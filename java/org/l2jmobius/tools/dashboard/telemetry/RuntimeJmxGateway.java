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
package org.l2jmobius.tools.dashboard.telemetry;

import java.io.Closeable;
import java.io.IOException;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.List;

import javax.management.JMX;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;

import org.l2jmobius.tools.dashboard.bridge.RuntimeManagementAgent;
import org.l2jmobius.tools.dashboard.bridge.RuntimeManagementMXBean;
import org.l2jmobius.tools.dashboard.logging.ControlCenterLog;

/**
 * Attaches to a managed local JVM by PID and reads cached platform MXBean proxies over its local JMX connector. <br>
 * The connector and proxies are reused until the PID changes or a management operation fails.
 * @author BazookaRpm
 */
public final class RuntimeJmxGateway implements Closeable
{
	private static final String LOCAL_CONNECTOR_ADDRESS = "com.sun.management.jmxremote.localConnectorAddress";
	private static final ObjectName OPERATING_SYSTEM = operatingSystemName();
	private final String _token;
	private long _processId = -1;
	private JMXConnector _connector;
	private MBeanServerConnection _connection;
	private RuntimeManagementMXBean _management;
	private ThreadMXBean _threads;
	private MemoryMXBean _memory;
	private ClassLoadingMXBean _classes;
	private RuntimeMXBean _runtime;
	private List<GarbageCollectorMXBean> _collectors = List.of();
	private List<MemoryPoolMXBean> _memoryPools = List.of();
	
	public RuntimeJmxGateway(String token)
	{
		_token = token;
	}
	
	public synchronized RuntimeSnapshot read(long processId)
	{
		try
		{
			connect(processId);
			long collectionCount = 0;
			long collectionTime = 0;
			for (GarbageCollectorMXBean collector : _collectors)
			{
				final long count = collector.getCollectionCount();
				final long time = collector.getCollectionTime();
				if (count > 0)
				{
					collectionCount += count;
				}
				
				if (time > 0)
				{
					collectionTime += time;
				}
			}
			
			long metaspace = 0;
			for (MemoryPoolMXBean pool : _memoryPools)
			{
				if (pool.getName().toLowerCase().contains("metaspace") && (pool.getUsage() != null))
				{
					metaspace += pool.getUsage().getUsed();
				}
			}
			
			final double processCpu = doubleAttribute("ProcessCpuLoad");
			final double systemCpu = doubleAttribute("CpuLoad", "SystemCpuLoad");
			final long totalMemory = longAttribute("TotalMemorySize", "TotalPhysicalMemorySize");
			final long freeMemory = longAttribute("FreeMemorySize", "FreePhysicalMemorySize");
			final long usedMemory = ((totalMemory >= 0) && (freeMemory >= 0)) ? Math.max(0, totalMemory - freeMemory) : -1;
			final int[] population = _management.populationSnapshot();
			if ((population == null) || (population.length < 4))
			{
				throw new IOException("Invalid population snapshot returned by the managed server.");
			}
			
			return new RuntimeSnapshot(true, _management.runtimeName(), _runtime.getVmVersion(), population[0], population[1], population[2], population[3], _threads.getThreadCount(), _threads.getPeakThreadCount(), _threads.getDaemonThreadCount(), _classes.getLoadedClassCount(), _memory.getHeapMemoryUsage().getUsed(), _memory.getHeapMemoryUsage().getMax(), _memory.getNonHeapMemoryUsage().getUsed(), metaspace, collectionCount, collectionTime, _runtime.getUptime(), processCpu, systemCpu, usedMemory, totalMemory);
		}
		catch (Exception e)
		{
			ControlCenterLog.write("Telemetry poll failed for process " + processId + ": " + e);
			reset();
			return RuntimeSnapshot.unavailable();
		}
	}
	
	public synchronized String threadDump(long processId)
	{
		try
		{
			connect(processId);
			final StringBuilder output = new StringBuilder(16384);
			for (ThreadInfo info : _threads.dumpAllThreads(true, true))
			{
				output.append('"').append(info.getThreadName()).append("\" #").append(info.getThreadId()).append(' ').append(info.getThreadState()).append(System.lineSeparator());
				for (StackTraceElement element : info.getStackTrace())
				{
					output.append("\tat ").append(element).append(System.lineSeparator());
				}
				output.append(System.lineSeparator());
			}
			
			return output.toString();
		}
		catch (Exception e)
		{
			ControlCenterLog.write("Thread dump failed for process " + processId + ": " + e);
			reset();
			return "Thread dump unavailable: " + e.getMessage();
		}
	}
	
	public synchronized boolean requestStop(long processId, boolean restart, int seconds)
	{
		try
		{
			connect(processId);
			return _management.lifecycleRequest(_token, restart ? "restart" : "shutdown", seconds);
		}
		catch (Exception e)
		{
			ControlCenterLog.write("Lifecycle request failed for process " + processId + ": " + e);
			reset();
			return false;
		}
	}
	
	public synchronized boolean cancelLifecycle(long processId)
	{
		try
		{
			connect(processId);
			return _management.cancelLifecycleRequest(_token);
		}
		catch (Exception e)
		{
			ControlCenterLog.write("Lifecycle cancellation failed for process " + processId + ": " + e);
			reset();
			return false;
		}
	}
	
	private void connect(long processId) throws Exception
	{
		if ((processId <= 0) || ((_connection != null) && (_management != null) && (_processId == processId)))
		{
			if (processId <= 0)
			{
				throw new IOException("Managed process PID is unavailable.");
			}
			return;
		}
		
		reset();
		VirtualMachine virtualMachine = null;
		String connectorAddress;
		try
		{
			virtualMachine = VirtualMachine.attach(Long.toString(processId));
			connectorAddress = virtualMachine.getAgentProperties().getProperty(LOCAL_CONNECTOR_ADDRESS, "");
			if (connectorAddress.isBlank())
			{
				connectorAddress = virtualMachine.startLocalManagementAgent();
			}
		}
		finally
		{
			if (virtualMachine != null)
			{
				virtualMachine.detach();
			}
		}
		
		if ((connectorAddress == null) || connectorAddress.isBlank())
		{
			throw new IOException("The target JVM did not expose a local management connector.");
		}
		
		_connector = JMXConnectorFactory.connect(new JMXServiceURL(connectorAddress));
		_connection = _connector.getMBeanServerConnection();
		_management = JMX.newMXBeanProxy(_connection, new ObjectName(RuntimeManagementAgent.OBJECT_NAME), RuntimeManagementMXBean.class, true);
		_threads = ManagementFactory.newPlatformMXBeanProxy(_connection, ManagementFactory.THREAD_MXBEAN_NAME, ThreadMXBean.class);
		_memory = ManagementFactory.newPlatformMXBeanProxy(_connection, ManagementFactory.MEMORY_MXBEAN_NAME, MemoryMXBean.class);
		_classes = ManagementFactory.newPlatformMXBeanProxy(_connection, ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
		_runtime = ManagementFactory.newPlatformMXBeanProxy(_connection, ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
		_collectors = ManagementFactory.getPlatformMXBeans(_connection, GarbageCollectorMXBean.class);
		_memoryPools = ManagementFactory.getPlatformMXBeans(_connection, MemoryPoolMXBean.class);
		_processId = processId;
	}
	
	/**
	 * Builds the object name of the operating system bean.
	 * @return The {@code ObjectName} every attribute read goes through.
	 */
	private static ObjectName operatingSystemName()
	{
		try
		{
			return new ObjectName(ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("The operating system bean name is malformed.", e);
		}
	}
	
	private double doubleAttribute(String... names)
	{
		for (String name : names)
		{
			try
			{
				final Object value = _connection.getAttribute(OPERATING_SYSTEM, name);
				if (value instanceof Number)
				{
					return ((Number) value).doubleValue();
				}
			}
			catch (Exception e)
			{
				// Try the next platform-specific attribute name.
			}
		}
		
		return -1;
	}
	
	private long longAttribute(String... names)
	{
		for (String name : names)
		{
			try
			{
				final Object value = _connection.getAttribute(OPERATING_SYSTEM, name);
				if (value instanceof Number)
				{
					return ((Number) value).longValue();
				}
			}
			catch (Exception e)
			{
				// Try the next platform-specific attribute name.
			}
		}
		
		return -1;
	}
	
	private void reset()
	{
		if (_connector != null)
		{
			try
			{
				_connector.close();
			}
			catch (IOException e)
			{
				// The stale connector is discarded below.
			}
		}
		
		_processId = -1;
		_connector = null;
		_connection = null;
		_management = null;
		_threads = null;
		_memory = null;
		_classes = null;
		_runtime = null;
		_collectors = List.of();
		_memoryPools = List.of();
	}
	
	@Override
	public synchronized void close()
	{
		reset();
	}
}
