/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.tools.dashboard.runtime;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.tools.dashboard.bridge.RuntimeManagementAgent;
import org.l2jmobius.tools.dashboard.logging.ConsoleMessage;
import org.l2jmobius.tools.dashboard.logging.ConsoleMessageBuffer;
import org.l2jmobius.tools.dashboard.logging.ConsoleMessageBuffer.ConsoleCounts;
import org.l2jmobius.tools.dashboard.logging.ConsoleMessageBuffer.ConsoleEntry;
import org.l2jmobius.tools.dashboard.logging.ConsoleMessageParser;
import org.l2jmobius.tools.dashboard.logging.ControlCenterLog;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.telemetry.RuntimeJmxGateway;
import org.l2jmobius.tools.dashboard.telemetry.RuntimeSnapshot;

/**
 * Supervises one Login Server or Game Server process and publishes its state to visible Dashboard observers. <br>
 * Process/port health and JVM telemetry use independent schedules, while console history is retained once in a central bounded buffer.
 * @author BazookaRpm
 */
public final class ServiceSupervisor implements AutoCloseable
{
	private static final int PORT_PROBE_TIMEOUT_MILLIS = 250;
	private static final int TERMINATION_GRACE_SECONDS = 5;
	private final ControlCenterSettings _settings;
	private final ServiceRole _role;
	private final CopyOnWriteArrayList<ServiceObserver> _observers = new CopyOnWriteArrayList<>();
	private final AtomicInteger _telemetryObservers = new AtomicInteger();
	private final ScheduledExecutorService _monitor;
	private final RuntimeJmxGateway _jmx;
	private final ConsoleMessageBuffer _console;
	private volatile DashboardWorkloadCoordinator _workloadCoordinator;
	private volatile ServicePhase _phase = ServicePhase.OFFLINE;
	private volatile Process _ownedProcess;
	private volatile ProcessHandle _processHandle;
	private volatile boolean _restartRequested;
	private volatile boolean _closing;
	private volatile long _stopWatchPid = -1;
	private volatile LifecycleCountdown _countdown;
	private volatile RuntimeSnapshot _lastSnapshot = RuntimeSnapshot.unavailable();
	private volatile int _servicePort;
	
	public ServiceSupervisor(ControlCenterSettings settings, ServiceRole role)
	{
		_settings = settings;
		_role = role;
		_servicePort = ServicePortResolver.resolve(settings, role);
		_jmx = new RuntimeJmxGateway(settings.getControlToken());
		_console = new ConsoleMessageBuffer(settings.getConsoleLineLimit());
		final AtomicInteger monitorThreadSequence = new AtomicInteger();
		_monitor = Executors.newScheduledThreadPool(2, runnable ->
		{
			final Thread thread = new Thread(runnable, "Dashboard-" + role.roleId() + "-Monitor-" + monitorThreadSequence.incrementAndGet());
			thread.setDaemon(true);
			return thread;
		});
		attachRecordedProcess();
		_monitor.scheduleWithFixedDelay(this::monitorHealthSafely, 0, Math.max(250, settings.getHealthCheckIntervalMillis()), TimeUnit.MILLISECONDS);
		_monitor.scheduleWithFixedDelay(this::pollTelemetrySafely, 0, Math.max(500, settings.getTelemetryIntervalMillis()), TimeUnit.MILLISECONDS);
	}
	
	public void setWorkloadCoordinator(DashboardWorkloadCoordinator coordinator)
	{
		_workloadCoordinator = coordinator;
	}
	
	public ServiceRole getRole()
	{
		return _role;
	}
	
	public ServicePhase getPhase()
	{
		return _phase;
	}
	
	public boolean isRunning()
	{
		final ProcessHandle handle = _processHandle;
		return (handle != null) && handle.isAlive();
	}
	
	/**
	 * @return the running countdown as one consistent snapshot, or {@code null} when no action is scheduled
	 */
	public LifecycleCountdown getLifecycleCountdown()
	{
		return _countdown;
	}
	
	public RuntimeSnapshot getLastSnapshot()
	{
		return _lastSnapshot;
	}
	
	public int getServicePort()
	{
		return _servicePort;
	}
	
	public List<ConsoleEntry> getConsoleSnapshot()
	{
		return _console.snapshot();
	}
	
	public ConsoleCounts getConsoleCounts()
	{
		return _console.counts();
	}
	
	public long getLatestConsoleSequence()
	{
		return _console.latestSequence();
	}
	
	public void clearConsoleHistory()
	{
		_console.clear();
	}
	
	public void addObserver(ServiceObserver observer)
	{
		if ((observer == null) || !_observers.addIfAbsent(observer))
		{
			return;
		}
		
		if (observer.requiresTelemetry())
		{
			_telemetryObservers.incrementAndGet();
		}
		
		observer.phaseChanged(_phase, "Current state");
		final ProcessHandle handle = _processHandle;
		observer.runtimeChanged((handle != null) && handle.isAlive() ? handle.pid() : -1, (handle != null) && handle.isAlive() ? uptimeMillis(handle) : 0, _servicePort);
		notifyLifecycleAction(List.of(observer));
		if (observer.requiresTelemetry())
		{
			observer.telemetryChanged(_lastSnapshot);
		}
	}
	
	public void removeObserver(ServiceObserver observer)
	{
		if ((observer != null) && _observers.remove(observer) && observer.requiresTelemetry())
		{
			_telemetryObservers.updateAndGet(value -> Math.max(0, value - 1));
		}
	}
	
	public synchronized void start()
	{
		if (_closing || isRunning())
		{
			return;
		}
		
		final DashboardWorkloadCoordinator coordinator = _workloadCoordinator;
		if ((coordinator != null) && !coordinator.beginServiceStart())
		{
			notifyPhaseDetail(coordinator.serviceStartBlockReason());
			return;
		}
		
		try
		{
			final Path jar = _settings.getServiceJar(_role);
			final Path workDirectory = _settings.getServiceDirectory(_role);
			if (!Files.isRegularFile(jar))
			{
				changePhase(ServicePhase.FAILED, "Missing server JAR: " + jar);
				return;
			}
			
			if (!Files.isDirectory(workDirectory))
			{
				changePhase(ServicePhase.FAILED, "Missing service directory: " + workDirectory);
				return;
			}
			
			try
			{
				_servicePort = ServicePortResolver.resolve(_settings, _role);
				final ProcessBuilder builder = new ProcessBuilder(buildCommand());
				builder.directory(workDirectory.toFile());
				builder.environment().put("L2J_DASHBOARD_TOKEN", _settings.getControlToken());
				changePhase(ServicePhase.LAUNCHING, "Starting " + _role.label() + ".");
				final Process process = builder.start();
				_ownedProcess = process;
				_processHandle = process.toHandle();
				ManagedProcessRecord.store(_settings.getProcessRecord(_role), _processHandle, jar);
				readPipe(process.getInputStream());
				readPipe(process.getErrorStream());
				watchExit(process);
				ControlCenterLog.write(_role.label() + " started with PID " + process.pid() + ".");
			}
			catch (Exception e)
			{
				_ownedProcess = null;
				_processHandle = null;
				final String reason = e.getMessage();
				changePhase(ServicePhase.FAILED, "Launch failed: " + reason);
				ControlCenterLog.write(_role.label() + " launch failed: " + reason);
			}
		}
		finally
		{
			if (coordinator != null)
			{
				coordinator.endServiceStart();
			}
		}
	}
	
	public void startAsync()
	{
		newThread(this::start).start();
	}
	
	public synchronized void stop(boolean restart)
	{
		if (!isRunning())
		{
			if (restart)
			{
				start();
			}
			return;
		}
		
		clearScheduledLifecycleAction();
		_restartRequested = restart;
		changePhase(ServicePhase.STOPPING, restart ? "Restart requested." : "Shutdown requested.");
		final ProcessHandle handle = _processHandle;
		if (handle == null)
		{
			return;
		}
		
		final boolean accepted = _jmx.requestStop(handle.pid(), restart, 0);
		if (!accepted && handle.isAlive())
		{
			handle.destroy();
			ControlCenterLog.write(_role.label() + " did not accept local JMX shutdown; process termination was requested.");
		}
		
		startStopWatchdog(handle, accepted);
	}
	
	public void restart()
	{
		stop(true);
	}
	
	public void scheduleLifecycleActionAsync(boolean restart, int seconds)
	{
		newThread(() -> scheduleLifecycleAction(restart, seconds)).start();
	}
	
	public synchronized void scheduleLifecycleAction(boolean restart, int seconds)
	{
		if (seconds == 0)
		{
			stop(restart);
			return;
		}
		
		final ProcessHandle handle = _processHandle;
		if ((_countdown != null) || (handle == null) || !handle.isAlive())
		{
			return;
		}
		
		if (!_jmx.requestStop(handle.pid(), restart, seconds))
		{
			ControlCenterLog.write(_role.label() + " did not accept the scheduled " + (restart ? "restart" : "shutdown") + " request.");
			notifyPhaseDetail("Unable to schedule " + (restart ? "restart." : "shutdown."));
			return;
		}
		
		// The tick waits for this monitor, so it never runs before the countdown is assigned.
		_countdown = new LifecycleCountdown(restart, seconds, handle.pid(), _monitor.scheduleAtFixedRate(this::tickLifecycleCountdown, 1, 1, TimeUnit.SECONDS));
		notifyLifecycleAction(_observers);
	}
	
	public void cancelScheduledLifecycleActionAsync()
	{
		newThread(this::cancelScheduledLifecycleAction).start();
	}
	
	public synchronized void cancelScheduledLifecycleAction()
	{
		final LifecycleCountdown countdown = _countdown;
		if (countdown == null)
		{
			return;
		}
		
		final ProcessHandle handle = _processHandle;
		if ((handle == null) || !handle.isAlive() || (handle.pid() != countdown._processId))
		{
			clearScheduledLifecycleAction();
			return;
		}
		
		final String action = countdown._restart ? "restart" : "shutdown";
		final boolean cancelled = _jmx.cancelLifecycle(handle.pid());
		
		// A refusal means no countdown is pending any more: it finished and the process exits, or it was aborted in game.
		clearScheduledLifecycleAction();
		_restartRequested = false;
		if (!cancelled)
		{
			ControlCenterLog.write(_role.label() + " had no pending " + action + " to cancel.");
			notifyPhaseDetail("No pending " + action + " to cancel, it already finished or was aborted in game.");
			return;
		}
		
		notifyPhaseDetail("Scheduled " + action + " cancelled.");
	}
	
	private synchronized void tickLifecycleCountdown()
	{
		final LifecycleCountdown countdown = _countdown;
		if (countdown == null)
		{
			return;
		}
		
		final ProcessHandle handle = _processHandle;
		if ((handle == null) || !handle.isAlive() || (handle.pid() != countdown._processId))
		{
			clearScheduledLifecycleAction();
			return;
		}
		
		final int secondsRemaining = countdown._secondsRemaining - 1;
		if (secondsRemaining > 0)
		{
			_countdown = new LifecycleCountdown(countdown._restart, secondsRemaining, countdown._processId, countdown._task);
			notifyLifecycleAction(_observers);
			return;
		}
		
		// The service runs its own countdown and exits by itself, and an in-game abort keeps it alive, so nothing is terminated here.
		clearScheduledLifecycleAction();
		notifyPhaseDetail((countdown._restart ? "Restart" : "Shutdown") + " countdown completed, waiting for the service to exit.");
	}
	
	private void clearScheduledLifecycleAction()
	{
		final LifecycleCountdown countdown = _countdown;
		if (countdown == null)
		{
			return;
		}
		
		_countdown = null;
		countdown._task.cancel(false);
		notifyLifecycleAction(_observers);
	}
	
	public String readThreadDump()
	{
		final ProcessHandle handle = _processHandle;
		return (handle == null) || !handle.isAlive() ? "Thread dump unavailable: managed process is not running." : _jmx.threadDump(handle.pid());
	}
	
	public boolean stopAndWaitForExit()
	{
		final ProcessHandle handle = _processHandle;
		if ((handle == null) || !handle.isAlive())
		{
			return true;
		}
		
		stop(false);
		final long waitSeconds = _settings.getShutdownTimeoutSeconds() + (TERMINATION_GRACE_SECONDS * 2L) + 2L;
		return waitForExit(handle, TimeUnit.SECONDS.toMillis(waitSeconds));
	}
	
	private void startStopWatchdog(ProcessHandle handle, boolean gracefulRequestAccepted)
	{
		final long processId = handle.pid();
		synchronized (this)
		{
			if (_stopWatchPid == processId)
			{
				return;
			}
			
			_stopWatchPid = processId;
		}
		
		final Thread thread = newThread(() ->
		{
			try
			{
				final long gracefulWaitMillis = gracefulRequestAccepted ? TimeUnit.SECONDS.toMillis(_settings.getShutdownTimeoutSeconds()) : TimeUnit.SECONDS.toMillis(TERMINATION_GRACE_SECONDS);
				if (waitForExit(handle, gracefulWaitMillis))
				{
					return;
				}
				
				ControlCenterLog.write(_role.label() + " shutdown timeout expired; process termination is being escalated.");
				handle.destroy();
				if (waitForExit(handle, TimeUnit.SECONDS.toMillis(TERMINATION_GRACE_SECONDS)))
				{
					return;
				}
				
				ControlCenterLog.write(_role.label() + " remained alive after process termination; forcing process exit.");
				handle.destroyForcibly();
				if (!waitForExit(handle, TimeUnit.SECONDS.toMillis(TERMINATION_GRACE_SECONDS)))
				{
					ControlCenterLog.write(_role.label() + " process " + processId + " is still alive after forced termination.");
				}
			}
			finally
			{
				synchronized (ServiceSupervisor.this)
				{
					if (_stopWatchPid == processId)
					{
						_stopWatchPid = -1;
					}
				}
			}
		});
		thread.start();
	}
	
	private static boolean waitForExit(ProcessHandle handle, long timeoutMillis)
	{
		final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(Math.max(0, timeoutMillis));
		while (handle.isAlive() && (System.nanoTime() < deadline))
		{
			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				return !handle.isAlive();
			}
		}
		
		return !handle.isAlive();
	}
	
	private List<String> buildCommand() throws Exception
	{
		final List<String> command = new ArrayList<>();
		command.add(JavaRuntimeResolver.resolve(_settings).toString());
		command.add("--enable-native-access=ALL-UNNAMED");
		command.addAll(JvmOptionReader.read(_settings.getJavaOptionsFile(_role)));
		command.add("-D" + RuntimeManagementAgent.MANAGED_PROPERTY + "=true");
		final ProcessHandle parent = ProcessHandle.current();
		command.add("-D" + RuntimeManagementAgent.PARENT_PID_PROPERTY + "=" + parent.pid());
		command.add("-D" + RuntimeManagementAgent.PARENT_START_PROPERTY + "=" + parent.info().startInstant().map(Instant::toEpochMilli).orElse(0L));
		command.add("-jar");
		command.add(_settings.getServiceJar(_role).toString());
		return command;
	}
	
	private void attachRecordedProcess()
	{
		final Optional<ProcessHandle> recorded = ManagedProcessRecord.find(_settings.getProcessRecord(_role), _settings.getServiceJar(_role));
		if (recorded.isPresent())
		{
			_processHandle = recorded.get();
			changePhase(ServicePhase.LAUNCHING, "Attached to recorded process.");
			return;
		}
		
		ManagedProcessRecord.remove(_settings.getProcessRecord(_role));
		if (TcpEndpointProbe.acceptsConnections(_servicePort, PORT_PROBE_TIMEOUT_MILLIS))
		{
			changePhase(ServicePhase.EXTERNAL, "Service port is open without a managed PID.");
		}
	}
	
	private void monitorHealthSafely()
	{
		try
		{
			monitorHealth();
		}
		catch (Exception e)
		{
			ControlCenterLog.write(_role.label() + " health monitor error: " + e.getMessage());
		}
	}
	
	private void pollTelemetrySafely()
	{
		try
		{
			pollTelemetry();
		}
		catch (Exception e)
		{
			ControlCenterLog.write(_role.label() + " telemetry monitor error: " + e.getMessage());
		}
	}
	
	private void monitorHealth()
	{
		final ProcessHandle handle = _processHandle;
		if ((handle == null) || !handle.isAlive())
		{
			if ((_ownedProcess == null) && TcpEndpointProbe.acceptsConnections(_servicePort, PORT_PROBE_TIMEOUT_MILLIS))
			{
				changePhase(ServicePhase.EXTERNAL, "Service port is open without a managed PID.");
			}
			else if ((_phase != ServicePhase.OFFLINE) && (_phase != ServicePhase.FAILED))
			{
				changePhase(ServicePhase.OFFLINE, "Process is not running.");
			}
			
			if (_lastSnapshot.available())
			{
				_lastSnapshot = RuntimeSnapshot.unavailable();
				notifyTelemetry(_lastSnapshot);
			}
			
			notifyRuntime(-1, 0);
			return;
		}
		
		final boolean portOpen = TcpEndpointProbe.acceptsConnections(_servicePort, PORT_PROBE_TIMEOUT_MILLIS);
		if (portOpen && (_phase != ServicePhase.STOPPING))
		{
			changePhase(ServicePhase.ONLINE, "Service port is accepting connections.");
		}
		else if ((_phase == ServicePhase.OFFLINE) || (_phase == ServicePhase.FAILED))
		{
			changePhase(ServicePhase.LAUNCHING, "Process is running and waiting for its service port.");
		}
		
		notifyRuntime(handle.pid(), uptimeMillis(handle));
	}
	
	private void pollTelemetry()
	{
		if ((_telemetryObservers.get() <= 0) || (_phase != ServicePhase.ONLINE))
		{
			return;
		}
		
		final ProcessHandle handle = _processHandle;
		if ((handle == null) || !handle.isAlive())
		{
			return;
		}
		
		final RuntimeSnapshot snapshot = _jmx.read(handle.pid());
		_lastSnapshot = snapshot;
		notifyTelemetry(snapshot);
	}
	
	private long uptimeMillis(ProcessHandle handle)
	{
		final Instant started = handle.info().startInstant().orElse(null);
		return (started == null) ? 0 : Math.max(0, System.currentTimeMillis() - started.toEpochMilli());
	}
	
	private void readPipe(InputStream stream)
	{
		final Thread thread = newThread(() ->
		{
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8)))
			{
				String line;
				while ((line = reader.readLine()) != null)
				{
					final ConsoleMessage message = ConsoleMessageParser.parse(_role, line);
					final ConsoleEntry entry = _console.append(message);
					for (ServiceObserver observer : _observers)
					{
						observer.consoleMessage(entry);
					}
				}
			}
			catch (Exception e)
			{
				if (!_closing)
				{
					ControlCenterLog.write(_role.label() + " console reader closed: " + e.getMessage());
				}
			}
		});
		thread.start();
	}
	
	private void watchExit(Process process)
	{
		final Thread thread = newThread(() ->
		{
			int exitCode = -1;
			try
			{
				exitCode = process.waitFor();
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
			}
			
			handleExit(process, exitCode);
		});
		thread.start();
	}
	
	private synchronized void handleExit(Process process, int exitCode)
	{
		if (_ownedProcess != process)
		{
			return;
		}
		
		_ownedProcess = null;
		_processHandle = null;
		clearScheduledLifecycleAction();
		ManagedProcessRecord.remove(_settings.getProcessRecord(_role));
		_jmx.close();
		_lastSnapshot = RuntimeSnapshot.unavailable();
		notifyTelemetry(_lastSnapshot);
		final boolean restart = !_closing && (_restartRequested || (exitCode == 2));
		_restartRequested = false;
		changePhase(ServicePhase.OFFLINE, "Process exited with code " + exitCode + ".");
		ControlCenterLog.write(_role.label() + " exited with code " + exitCode + ".");
		if (restart)
		{
			_monitor.schedule(this::start, 1, TimeUnit.SECONDS);
		}
	}
	
	private void changePhase(ServicePhase phase, String detail)
	{
		if ((_phase == phase) && ((phase == ServicePhase.ONLINE) || (phase == ServicePhase.OFFLINE) || (phase == ServicePhase.EXTERNAL)))
		{
			return;
		}
		
		_phase = phase;
		notifyPhaseDetail(detail);
	}
	
	private void notifyPhaseDetail(String detail)
	{
		for (ServiceObserver observer : _observers)
		{
			observer.phaseChanged(_phase, detail);
		}
	}
	
	private void notifyRuntime(long processId, long uptimeMillis)
	{
		for (ServiceObserver observer : _observers)
		{
			observer.runtimeChanged(processId, uptimeMillis, _servicePort);
		}
	}
	
	private void notifyLifecycleAction(Iterable<ServiceObserver> observers)
	{
		final LifecycleCountdown countdown = _countdown;
		final boolean restart = (countdown != null) && countdown._restart;
		final int secondsRemaining = countdown == null ? -1 : countdown._secondsRemaining;
		for (ServiceObserver observer : observers)
		{
			observer.lifecycleActionChanged(countdown != null, restart, secondsRemaining);
		}
	}
	
	private void notifyTelemetry(RuntimeSnapshot snapshot)
	{
		for (ServiceObserver observer : _observers)
		{
			if (observer.requiresTelemetry())
			{
				observer.telemetryChanged(snapshot);
			}
		}
	}
	
	public Thread newThread(Runnable runnable)
	{
		final Thread thread = new Thread(runnable, "Dashboard-" + _role.roleId() + "-Worker");
		thread.setDaemon(true);
		return thread;
	}
	
	@Override
	public synchronized void close()
	{
		_closing = true;
		_restartRequested = false;
		clearScheduledLifecycleAction();
		final ProcessHandle handle = _processHandle;
		if ((handle != null) && handle.isAlive())
		{
			ControlCenterLog.write(_role.label() + " remained alive while its supervisor was closing; forcing process exit.");
			handle.destroyForcibly();
			
			// The kill is asynchronous, so the record only goes once the process is really gone.
			if (waitForExit(handle, TimeUnit.SECONDS.toMillis(TERMINATION_GRACE_SECONDS)))
			{
				ManagedProcessRecord.remove(_settings.getProcessRecord(_role));
			}
		}
		
		_monitor.shutdownNow();
		_jmx.close();
	}
	
	/**
	 * Immutable snapshot of one scheduled stop or restart, replaced on every countdown tick.
	 */
	public static final class LifecycleCountdown
	{
		private final boolean _restart;
		private final int _secondsRemaining;
		private final long _processId;
		private final ScheduledFuture<?> _task;
		
		private LifecycleCountdown(boolean restart, int secondsRemaining, long processId, ScheduledFuture<?> task)
		{
			_restart = restart;
			_secondsRemaining = secondsRemaining;
			_processId = processId;
			_task = task;
		}
		
		public boolean isRestart()
		{
			return _restart;
		}
		
		public int getSecondsRemaining()
		{
			return _secondsRemaining;
		}
	}
}
