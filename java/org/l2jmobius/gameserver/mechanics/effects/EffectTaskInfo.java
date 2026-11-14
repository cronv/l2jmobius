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

import java.util.concurrent.ScheduledFuture;

/**
 * This class serves as a Data Transfer Object (DTO) for effect task information.<br>
 * It holds the necessary data required to manage and identify specific tasks associated with effects.
 * @author Zoey76
 */
public class EffectTaskInfo
{
	private final EffectTickTask _effectTask;
	private final ScheduledFuture<?> _scheduledFuture;
	
	/**
	 * Creates a new {@code EffectTaskInfo} object.<br>
	 * This constructor initializes the task and its schedule.
	 * @param effectTask The {@code EffectTickTask} to be stored.
	 * @param scheduledFuture The {@code ScheduledFuture} associated with this task.
	 */
	public EffectTaskInfo(EffectTickTask effectTask, ScheduledFuture<?> scheduledFuture)
	{
		_effectTask = effectTask;
		_scheduledFuture = scheduledFuture;
	}
	
	/**
	 * Retrieves the {@code EffectTickTask} associated with this info object.<br>
	 * This task handles the logic for effect ticks.
	 * @return the {@code EffectTickTask} instance.
	 */
	public EffectTickTask getEffectTask()
	{
		return _effectTask;
	}
	
	/**
	 * Retrieves the {@code ScheduledFuture} associated with this effect task.<br>
	 * This allows you to manage or cancel the scheduled execution.
	 * @return The {@code ScheduledFuture<?>} object.
	 */
	public ScheduledFuture<?> getScheduledFuture()
	{
		return _scheduledFuture;
	}
}
