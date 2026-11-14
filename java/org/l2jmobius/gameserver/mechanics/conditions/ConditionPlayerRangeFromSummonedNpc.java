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
package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.Set;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if the player is within a specific distance from an NPC they have summoned.<br>
 * This condition validates the proximity between the actor and their own summon.
 * @author Nik
 */
public class ConditionPlayerRangeFromSummonedNpc extends Condition
{
	/** NPC Ids. */
	private final Set<Integer> _npcIds;
	/** Radius to check. */
	private final int _radius;
	/** Expected value. */
	private final boolean _value;
	
	/**
	 * Creates a condition to check the distance between a player and summoned NPCs.<br>
	 * It verifies if the player is within a specific range of the provided NPC IDs.
	 * @param npcIds The set of {@code Integer} IDs for the summoned NPCs.
	 * @param radius The distance threshold to check.
	 * @param value The expected result to return as {@code true} or {@code false}.
	 */
	public ConditionPlayerRangeFromSummonedNpc(Set<Integer> npcIds, int radius, boolean value)
	{
		_npcIds = npcIds;
		_radius = radius;
		_value = value;
	}
	
	/**
	 * Checks if the {@code effector} is within a specific range of a summoned NPC.<br>
	 * This method verifies if an NPC with a matching ID exists near the player.<br>
	 * It compares the result against the expected {@code _value}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 * @return {@code true} if the condition matches the expected value, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		boolean existNpc = false;
		if (!_npcIds.isEmpty() && (_radius > 0))
		{
			existNpc = World.getFirstVisibleObjectInRange(effector, Npc.class, _radius, target -> _npcIds.contains(target.getId()) && (effector == target.getSummoner())) != null;
		}
		
		return existNpc == _value;
	}
}
