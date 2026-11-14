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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Vehicle;

/**
 * Manages the periodic stuck checks for moving vehicles.<br>
 * Registered {@code Vehicle} entities are verified every second.<br>
 * A vehicle that stopped advancing resumes its path or broadcasts its info.
 * @author Mobius
 */
public class VehicleTaskManager implements Runnable
{
	private static final Set<Vehicle> VEHICLES = ConcurrentHashMap.newKeySet();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes the {@code VehicleTaskManager}.<br>
	 * This constructor schedules the task to run periodically.<br>
	 * It sets up the execution frequency for vehicle movement checks.
	 */
	protected VehicleTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 1000, 1000);
	}
	
	@Override
	public void run()
	{
		if (!WORKING.compareAndSet(false, true))
		{
			return;
		}
		
		try
		{
			for (Vehicle vehicle : VEHICLES)
			{
				vehicle.monitorMovement();
			}
		}
		finally
		{
			WORKING.set(false);
		}
	}
	
	/**
	 * Adds a {@code Vehicle} to the active task list.<br>
	 * This method registers the vehicle for movement monitoring.
	 * @param vehicle The {@code Vehicle} object to add.
	 */
	public void add(Vehicle vehicle)
	{
		VEHICLES.add(vehicle);
	}
	
	/**
	 * Removes a {@code Vehicle} from the active task manager.<br>
	 * This method updates the internal set of monitored vehicles.
	 * @param vehicle The {@code Vehicle} object to be removed.
	 */
	public void remove(Vehicle vehicle)
	{
		VEHICLES.remove(vehicle);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code VehicleTaskManager}.<br>
	 * Use this method to access the global task manager.
	 * @return The active {@code VehicleTaskManager} instance.
	 */
	public static VehicleTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final VehicleTaskManager INSTANCE = new VehicleTaskManager();
	}
}
