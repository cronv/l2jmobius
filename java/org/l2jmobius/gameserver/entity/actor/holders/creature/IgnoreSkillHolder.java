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
package org.l2jmobius.gameserver.entity.actor.holders.creature;

import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class manages the list of skills that a creature is configured to ignore.<br>
 * It allows for specific mechanics to bypass certain skill effects on a per-creature basis.
 * @author UnAfraid
 */
public class IgnoreSkillHolder extends SkillHolder
{
	private final AtomicInteger _instances = new AtomicInteger(1);
	
	/**
	 * Creates a new {@code IgnoreSkillHolder} instance.<br>
	 * This constructor initializes the holder with specific skill data.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill to be ignored.
	 */
	public IgnoreSkillHolder(int skillId, int skillLevel)
	{
		super(skillId, skillLevel);
	}
	
	/**
	 * Creates a new {@code IgnoreSkillHolder} based on an existing {@code SkillHolder}.<br>
	 * This constructor copies the skill information from the provided holder.
	 * @param holder The {@code SkillHolder} to use as a template.
	 */
	public IgnoreSkillHolder(SkillHolder holder)
	{
		super(holder.getSkill());
	}
	
	/**
	 * Returns the current number of instances for this skill.<br>
	 * This value is managed by an {@code AtomicInteger}.
	 * @return The total count of active instances.
	 */
	public int getInstances()
	{
		return _instances.get();
	}
	
	/**
	 * Increases the number of active instances for this holder.<br>
	 * This method updates the internal {@code AtomicInteger} counter.
	 * @return The new count of instances after the increase.
	 */
	public int increaseInstances()
	{
		return _instances.incrementAndGet();
	}
	
	/**
	 * Reduces the number of active instances by one.<br>
	 * This method updates the internal counter and returns the new value.
	 * @return The updated count of instances as an {@code int}.
	 */
	public int decreaseInstances()
	{
		return _instances.decrementAndGet();
	}
}
