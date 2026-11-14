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
package org.l2jmobius.gameserver.mechanics.conditions;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if a {@code Player} currently has an active pet.<br>
 * This condition is used to verify the presence of a {@code Summon} entity associated with the player.
 */
public class ConditionPlayerHasPet extends Condition
{
	private final List<Integer> _controlItemIds;
	
	/**
	 * Creates a new condition to check if a player has a specific pet.<br>
	 * This constructor initializes the required item IDs for the check.
	 * @param itemIds A {@code List<Integer>} containing the IDs of the items that represent pets.
	 */
	public ConditionPlayerHasPet(List<Integer> itemIds)
	{
		if ((itemIds.size() == 1) && (itemIds.get(0) == 0))
		{
			_controlItemIds = null;
		}
		else
		{
			_controlItemIds = itemIds;
		}
	}
	
	/**
	 * Checks if the {@code effector} has a valid pet.<br>
	 * This method verifies that the player's pet is controlled by an item in the allowed list.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @param item The item template associated with the action.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if (_controlItemIds == null)
		{
			return true;
		}
		
		final Player player = effector.asPlayer();
		if (player == null)
		{
			return false;
		}
		
		final Summon pet = player.getPet();
		if (pet == null)
		{
			return false;
		}
		
		final Item controlItem = pet.asPet().getControlItem();
		return (controlItem != null) && _controlItemIds.contains(controlItem.getId());
	}
}
