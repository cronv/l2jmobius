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
package org.l2jmobius.gameserver.data.holders;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;

/**
 * This class acts as a container for managing active punishments within the game server.<br>
 * It stores and tracks {@code PunishmentTask} objects to ensure they are processed correctly.
 * @author UnAfraid
 */
public class PunishmentHolder
{
	private final Map<String, Map<PunishmentType, PunishmentTask>> _holder = new ConcurrentHashMap<>();
	
	/**
	 * Adds a {@code PunishmentTask} to the internal storage.<br>
	 * This method checks if the task is expired before saving it.<br>
	 * It organizes tasks by their unique key and type.
	 * @param task The {@code PunishmentTask} to be added.
	 */
	public void addPunishment(PunishmentTask task)
	{
		if (!task.isExpired())
		{
			_holder.computeIfAbsent(String.valueOf(task.getKey()), _ -> new ConcurrentHashMap<>()).put(task.getType(), task);
		}
	}
	
	/**
	 * Stops a specific punishment task.<br>
	 * This method calls the {@code stopPunishment()} method on the provided {@code PunishmentTask}.<br>
	 * It also removes the task from the internal storage map.
	 * @param task The {@code PunishmentTask} to be stopped.
	 */
	public void stopPunishment(PunishmentTask task)
	{
		final String key = String.valueOf(task.getKey());
		final Map<PunishmentType, PunishmentTask> punishments = _holder.get(key);
		if (punishments != null)
		{
			task.stopPunishment();
			punishments.remove(task.getType());
			if (punishments.isEmpty())
			{
				_holder.remove(key);
			}
		}
	}
	
	/**
	 * Stops all active punishments of a specific type.<br>
	 * This method searches through the internal holder to find matching tasks.<br>
	 * It calls {@code stopPunishment()} on each task and removes them from the collection.
	 * @param type The {@code PunishmentType} to be stopped.
	 */
	public void stopPunishment(PunishmentType type)
	{
		for (Map<PunishmentType, PunishmentTask> punishments : _holder.values())
		{
			for (PunishmentTask task : punishments.values())
			{
				final PunishmentType taskType = task.getType();
				if (taskType == type)
				{
					task.stopPunishment();
					final String key = String.valueOf(task.getKey());
					punishments.remove(taskType);
					if (punishments.isEmpty())
					{
						_holder.remove(key);
					}
				}
			}
		}
	}
	
	/**
	 * Checks if a specific punishment exists for a given key.<br>
	 * This method returns {@code true} if the task is found.<br>
	 * It returns {@code false} if no such punishment exists.
	 * @param key The unique identifier for the entity or object.
	 * @param type The category of the punishment to check.
	 * @return {@code true} if the punishment exists, otherwise {@code false}.
	 */
	public boolean hasPunishment(String key, PunishmentType type)
	{
		return getPunishment(key, type) != null;
	}
	
	/**
	 * Retrieves a specific punishment task based on its unique identifier and type.<br>
	 * This method looks up the {@code key} in the internal storage.<br>
	 * It then finds the task associated with the provided {@code type}.
	 * @param key The unique string identifier for the entity being punished.
	 * @param type The specific category of punishment to retrieve.
	 * @return The {@code PunishmentTask} if found, or {@code null} if it does not exist.
	 */
	public PunishmentTask getPunishment(String key, PunishmentType type)
	{
		return _holder.getOrDefault(key, Collections.emptyMap()).get(type);
	}
}
