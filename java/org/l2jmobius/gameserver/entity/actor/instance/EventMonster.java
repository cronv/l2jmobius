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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

/**
 * Represents a monster that is part of a specific game event.<br>
 * This class extends {@code Monster} to provide specialized behavior for temporary activities.
 * @author Gnacik
 */
public class EventMonster extends Monster
{
	// Block offensive skills usage on event mobs.
	// Mainly for AoE skills, this prevents killing many event mobs with a single skill.
	public boolean block_skill_attack = false;
	
	// Event mobs should drop items to ground.
	// But item pickup must be protected to killer.
	// Todo: Some mobs need protect drop for spawner
	public boolean drop_on_ground = false;
	
	/**
	 * Creates a new {@code EventMonster} instance.<br>
	 * This constructor initializes the monster using a provided template.<br>
	 * It automatically sets the {@code InstanceType} to {@code EventMob}.
	 * @param template The {@code NpcTemplate} used to define the monster's properties.
	 */
	public EventMonster(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.EventMob);
	}
	
	/**
	 * Sets whether offensive skills are blocked for this event monster.<br>
	 * This is primarily used to prevent players from killing multiple mobs with one area of effect skill.
	 * @param value {@code true} to block skills, {@code false} to allow them.
	 */
	public void eventSetBlockOffensiveSkills(boolean value)
	{
		block_skill_attack = value;
	}
	
	/**
	 * Sets whether the monster drops items on the ground.<br>
	 * This updates the {@code drop_on_ground} field.
	 * @param value The boolean value to set for dropping items.
	 */
	public void eventSetDropOnGround(boolean value)
	{
		drop_on_ground = value;
	}
	
	/**
	 * Checks if the monster is allowed to drop items on the ground.<br>
	 * This value determines whether loot appears in the world after a kill.
	 * @return {@code true} if items drop on the ground, {@code false} otherwise.
	 */
	public boolean eventDropOnGround()
	{
		return drop_on_ground;
	}
	
	/**
	 * Checks if offensive skills are blocked for this event monster.<br>
	 * This is used to prevent players from killing multiple mobs with one skill.
	 * @return {@code true} if skills are blocked, {@code false} otherwise.
	 */
	public boolean eventSkillAttackBlocked()
	{
		return block_skill_attack;
	}
}
