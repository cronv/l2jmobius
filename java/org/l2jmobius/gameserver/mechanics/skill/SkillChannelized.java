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
package org.l2jmobius.gameserver.mechanics.skill;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Creature;

/**
 * This class handles the logic for skills that are channelized.<br>
 * It manages how a {@code Creature} maintains a continuous effect over time.
 * @author UnAfraid
 */
public class SkillChannelized
{
	private final Map<Integer, Map<Integer, Creature>> _channelizers = new ConcurrentHashMap<>();
	
	/**
	 * Adds a {@code Creature} as a channelizer for a specific skill.<br>
	 * This method links the creature to the provided {@code skillId}.<br>
	 * It updates the internal mapping of active channelizers.
	 * @param skillId The unique identifier for the skill.
	 * @param channelizer The {@code Creature} instance performing the action.
	 */
	public void addChannelizer(int skillId, Creature channelizer)
	{
		_channelizers.computeIfAbsent(skillId, _ -> new ConcurrentHashMap<>()).put(channelizer.getObjectId(), channelizer);
	}
	
	/**
	 * Removes a specific {@code Creature} from the list of channelizers for a given skill.<br>
	 * This method updates the internal mapping for the provided {@code skillId}.
	 * @param skillId The unique identifier of the skill.
	 * @param channelizer The {@code Creature} to be removed.
	 */
	public void removeChannelizer(int skillId, Creature channelizer)
	{
		getChannelizers(skillId).remove(channelizer.getObjectId());
	}
	
	/**
	 * Returns the number of creatures currently channelizing a specific skill.<br>
	 * This method uses the {@code skillId} to look up the active channelizers.
	 * @param skillId The unique identifier for the skill.
	 * @return The total count of channelizers for the given {@code skillId}.
	 */
	public int getChannerlizersSize(int skillId)
	{
		return getChannelizers(skillId).size();
	}
	
	/**
	 * Retrieves all creatures currently channelizing a specific skill.<br>
	 * This method returns an empty map if no creatures are found for the given {@code skillId}.
	 * @param skillId The unique identifier of the skill to check.
	 * @return A map where keys are IDs and values are {@code Creature} objects.
	 */
	public Map<Integer, Creature> getChannelizers(int skillId)
	{
		return _channelizers.getOrDefault(skillId, Collections.emptyMap());
	}
	
	/**
	 * Stops all active skill channelizations.<br>
	 * This method calls {@code abortCast()} on every {@code Creature} currently channeling a skill.<br>
	 * It then clears the internal list of all channelizers.
	 */
	public void abortChannelization()
	{
		for (Map<Integer, Creature> map : _channelizers.values())
		{
			for (Creature channelizer : map.values())
			{
				channelizer.abortCast();
			}
		}
		
		_channelizers.clear();
	}
	
	/**
	 * Checks if any skills are currently being channelized.<br>
	 * It looks through all active skill maps to see if they contain any creatures.
	 * @return {@code true} if at least one creature is currently channelizing a skill, otherwise {@code false}.
	 */
	public boolean isChannelized()
	{
		for (Map<Integer, Creature> map : _channelizers.values())
		{
			if (!map.isEmpty())
			{
				return true;
			}
		}
		
		return false;
	}
}
