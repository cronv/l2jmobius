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
package org.l2jmobius.gameserver.managers.events;

/**
 * Manages the logic and lifecycle of event shrines within the game world.<br>
 * This class handles the creation, updates, and removal of shrine objects.
 * @author Mobius
 */
public class EventShrineManager
{
	private static boolean ENABLE_SHRINES = false;
	
	/**
	 * Checks if the shrine events are currently active.<br>
	 * This method returns the current state of the {@code ENABLE_SHRINES} flag.
	 * @return {@code true} if shrines are enabled, {@code false} otherwise.
	 */
	public boolean areShrinesEnabled()
	{
		return ENABLE_SHRINES;
	}
	
	/**
	 * Updates the status of event shrines.<br>
	 * Sets whether shrines are currently active or inactive.
	 * @param enabled The {@code boolean} value to set for shrine activity.
	 */
	public void setEnabled(boolean enabled)
	{
		ENABLE_SHRINES = enabled;
	}
	
	/**
	 * Retrieves the global instance of the {@code EventShrineManager}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access shrine management features throughout the application.
	 * @return The single instance of {@code EventShrineManager}.
	 */
	public static EventShrineManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EventShrineManager INSTANCE = new EventShrineManager();
	}
}
