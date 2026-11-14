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

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Coordinates resource-intensive Control Center operations with managed server lifecycle activity. <br>
 * Heavy maintenance work and Login/Game service startup are mutually exclusive, and only one heavy operation may run at a time.
 * @author BazookaRpm
 */
public final class DashboardWorkloadCoordinator
{
	private final List<Runnable> _listeners = new CopyOnWriteArrayList<>();
	private ServiceSupervisor _login;
	private ServiceSupervisor _game;
	private WorkloadPermit _activeHeavyPermit;
	private int _serviceStarts;
	
	public void bindServices(ServiceSupervisor login, ServiceSupervisor game)
	{
		_login = login;
		_game = game;
		final ServiceObserver observer = new ServiceObserver()
		{
			@Override
			public void phaseChanged(ServicePhase phase, String detail)
			{
				notifyListeners();
			}
		};
		login.addObserver(observer);
		game.addObserver(observer);
		notifyListeners();
	}
	
	public WorkloadPermit tryAcquireHeavy(String operation)
	{
		final WorkloadPermit permit;
		synchronized (this)
		{
			if ((_activeHeavyPermit != null) || (_serviceStarts > 0) || servicesActive())
			{
				return null;
			}
			
			_activeHeavyPermit = new WorkloadPermit(this, normalize(operation));
			permit = _activeHeavyPermit;
		}
		
		notifyListeners();
		return permit;
	}
	
	public boolean beginServiceStart()
	{
		synchronized (this)
		{
			if (_activeHeavyPermit != null)
			{
				return false;
			}
			
			_serviceStarts++;
		}
		
		notifyListeners();
		return true;
	}
	
	public void endServiceStart()
	{
		synchronized (this)
		{
			if (_serviceStarts > 0)
			{
				_serviceStarts--;
			}
		}
		
		notifyListeners();
	}
	
	public synchronized boolean isMaintenanceAvailable()
	{
		return (_activeHeavyPermit == null) && (_serviceStarts == 0) && !servicesActive();
	}
	
	public synchronized boolean isHeavyWorkActive()
	{
		return _activeHeavyPermit != null;
	}
	
	public synchronized String activeHeavyOperation()
	{
		return _activeHeavyPermit == null ? "" : _activeHeavyPermit.operation();
	}
	
	public synchronized String maintenanceBlockReason()
	{
		if (_activeHeavyPermit != null)
		{
			return "Wait for " + _activeHeavyPermit.operation() + " to finish.";
		}
		
		if ((_serviceStarts > 0) || servicesActive())
		{
			return "Stop Login Server and Game Server before starting maintenance work.";
		}
		
		return "";
	}
	
	public synchronized String serviceStartBlockReason()
	{
		return _activeHeavyPermit == null ? "" : "Wait for " + _activeHeavyPermit.operation() + " to finish before starting a managed service.";
	}
	
	public void addListener(Runnable listener)
	{
		if ((listener != null) && !_listeners.contains(listener))
		{
			_listeners.add(listener);
			listener.run();
		}
	}
	
	public void removeListener(Runnable listener)
	{
		_listeners.remove(listener);
	}
	
	private void release(WorkloadPermit permit)
	{
		synchronized (this)
		{
			if (_activeHeavyPermit != permit)
			{
				return;
			}
			
			_activeHeavyPermit = null;
		}
		
		notifyListeners();
	}
	
	private boolean servicesActive()
	{
		return serviceActive(_login) || serviceActive(_game);
	}
	
	private static boolean serviceActive(ServiceSupervisor service)
	{
		return (service != null) && (service.isRunning() || service.getPhase().isProcessExpected());
	}
	
	/**
	 * Runs the registered listeners.<br>
	 * The monitor is released first, so listener code never runs while this coordinator is locked.
	 */
	private void notifyListeners()
	{
		for (Runnable listener : _listeners)
		{
			listener.run();
		}
	}
	
	private static String normalize(String operation)
	{
		if ((operation == null) || operation.isBlank())
		{
			return "the active maintenance task";
		}
		
		return operation.trim();
	}
	
	/**
	 * Releases one acquired heavy-work slot when the owning operation finishes.
	 * @author BazookaRpm
	 */
	public static final class WorkloadPermit implements AutoCloseable
	{
		private final DashboardWorkloadCoordinator _coordinator;
		private final String _operation;
		private boolean _closed;
		
		private WorkloadPermit(DashboardWorkloadCoordinator coordinator, String operation)
		{
			_coordinator = coordinator;
			_operation = operation;
		}
		
		public String operation()
		{
			return _operation;
		}
		
		@Override
		public synchronized void close()
		{
			if (_closed)
			{
				return;
			}
			
			_closed = true;
			_coordinator.release(this);
		}
	}
}
