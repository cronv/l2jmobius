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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class handles conditions related to an actor's inventory.<br>
 * It provides a base for checking if specific items are present or missing.<br>
 * Use {@code ConditionInventory} to implement requirements based on item ownership.
 * @author mkizub
 */
public abstract class ConditionInventory extends Condition
{
	protected final int _slot;
	
	/**
	 * Creates a new instance of {@code ConditionInventory}.<br>
	 * This constructor sets the specific inventory slot for the condition.
	 * @param slot The index of the inventory slot.
	 */
	public ConditionInventory(int slot)
	{
		_slot = slot;
	}
	
	/**
	 * Test impl.
	 * @return true, if successful
	 */
	@Override
	public abstract boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item);
}
