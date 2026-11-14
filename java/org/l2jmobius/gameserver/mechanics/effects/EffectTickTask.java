/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.effects;

import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;

/**
 * This class handles the periodic execution of effects that occur over time.<br>
 * It manages the logic for ticking specific mechanics on a scheduled basis.
 * @author Zoey76
 */
public class EffectTickTask implements Runnable
{
	private final BuffInfo _info;
	private final AbstractEffect _effect;
	
	/**
	 * Creates a new {@code EffectTickTask} instance.<br>
	 * This constructor initializes the task with specific buff and effect data.
	 * @param info The {@code BuffInfo} associated with this effect tick.
	 * @param effect The {@code AbstractEffect} to be processed during the tick.
	 */
	public EffectTickTask(BuffInfo info, AbstractEffect effect)
	{
		_info = info;
		_effect = effect;
	}
	
	/**
	 * Retrieves the {@code BuffInfo} associated with this task.<br>
	 * This method returns the internal information for the effect.
	 * @return the {@code BuffInfo} object.
	 */
	public BuffInfo getBuffInfo()
	{
		return _info;
	}
	
	/**
	 * Retrieves the {@code AbstractEffect} associated with this task.<br>
	 * This method returns the internal effect object used by the tick.
	 * @return the {@code AbstractEffect} instance.
	 */
	public AbstractEffect getEffect()
	{
		return _effect;
	}
	
	@Override
	public void run()
	{
		_info.onTick(_effect);
	}
}
