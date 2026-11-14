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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class holds the configuration settings for a player's auto-play feature.<br>
 * It stores state information such as whether auto-play is enabled and its current status.
 * @author Mobius
 */
public class AutoPlaySettingsHolder
{
	private final AtomicInteger _options = new AtomicInteger();
	private final AtomicBoolean _pickup = new AtomicBoolean();
	private final AtomicInteger _nextTargetMode = new AtomicInteger();
	private final AtomicBoolean _shortRange = new AtomicBoolean();
	private final AtomicBoolean _respectfulHunting = new AtomicBoolean();
	private final AtomicInteger _autoPotionPercent = new AtomicInteger();
	private final AtomicInteger _macroIndex = new AtomicInteger();
	
	/**
	 * Creates a new instance of {@code AutoPlaySettingsHolder}.<br>
	 * This initializes the default settings for auto-play behavior.
	 */
	public AutoPlaySettingsHolder()
	{
	}
	
	/**
	 * Retrieves the current configuration options.<br>
	 * This method returns the integer value stored in the {@code _options} field.
	 * @return The current integer value of the options.
	 */
	public int getOptions()
	{
		return _options.get();
	}
	
	/**
	 * Updates the configuration options for this holder.<br>
	 * This method sets the internal {@code _options} value.
	 * @param options The new integer value to set for the options.
	 */
	public void setOptions(int options)
	{
		_options.set(options);
	}
	
	/**
	 * Checks if the auto-pickup feature is enabled.<br>
	 * This method retrieves the current state of the {@code _pickup} flag.
	 * @return {@code true} if items can be picked up automatically, {@code false} otherwise.
	 */
	public boolean doPickup()
	{
		return _pickup.get();
	}
	
	/**
	 * Sets whether the character should automatically pick up items.<br>
	 * Use {@code true} to enable this feature.<br>
	 * Use {@code false} to disable this feature.
	 * @param value The boolean state for the pickup behavior.
	 */
	public void setPickup(boolean value)
	{
		_pickup.set(value);
	}
	
	/**
	 * Retrieves the current target mode for auto-play.<br>
	 * This value determines how the system selects the next target.
	 * @return The current {@code int} value of the next target mode.
	 */
	public int getNextTargetMode()
	{
		return _nextTargetMode.get();
	}
	
	/**
	 * Updates the mode for the next target.<br>
	 * This method sets the internal state of {@code _nextTargetMode}.
	 * @param nextTargetMode The new mode value to set.
	 */
	public void setNextTargetMode(int nextTargetMode)
	{
		_nextTargetMode.set(nextTargetMode);
	}
	
	/**
	 * Checks if the short range setting is enabled.<br>
	 * This determines if the auto-play behavior uses a limited distance.
	 * @return {@code true} if short range is active, {@code false} otherwise.
	 */
	public boolean isShortRange()
	{
		return _shortRange.get();
	}
	
	/**
	 * Sets whether the auto-play system uses short range targeting.<br>
	 * This updates the {@code _shortRange} state within the holder.
	 * @param value The boolean value to set for short range mode.
	 */
	public void setShortRange(boolean value)
	{
		_shortRange.set(value);
	}
	
	/**
	 * Checks if the respectful hunting setting is enabled.<br>
	 * This determines if the player avoids attacking certain targets.
	 * @return {@code true} if respectful hunting is active, {@code false} otherwise.
	 */
	public boolean isRespectfulHunting()
	{
		return _respectfulHunting.get();
	}
	
	/**
	 * Sets whether the auto-play system follows respectful hunting rules.<br>
	 * This determines if the character avoids attacking certain targets.
	 * @param value The boolean value to set for respectful hunting.
	 */
	public void setRespectfulHunting(boolean value)
	{
		_respectfulHunting.set(value);
	}
	
	/**
	 * Retrieves the current auto-potion percentage.<br>
	 * This value determines how often the player automatically uses potions.
	 * @return The current potion percentage as an {@code int}.
	 */
	public int getAutoPotionPercent()
	{
		return _autoPotionPercent.get();
	}
	
	/**
	 * Sets the percentage for automatic potion usage.<br>
	 * This value determines how often the character uses a potion.
	 * @param value The new percentage to set.
	 */
	public void setAutoPotionPercent(int value)
	{
		_autoPotionPercent.set(value);
	}
	
	/**
	 * Sets the current index for the macro system.<br>
	 * This updates the internal {@code _macroIndex} value.
	 * @param value The new index to set.
	 */
	public void setMacroIndex(int value)
	{
		_macroIndex.set(value);
	}
	
	/**
	 * Retrieves the current index of the macro.<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return The current macro index as an {@code int}.
	 */
	public int getMacroIndex()
	{
		return _macroIndex.get();
	}
}
