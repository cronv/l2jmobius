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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Checks if the actor is currently using a magic weapon.<br>
 * This condition verifies that the {@code Creature} holds an item with the magic property.<br>
 * It is used to restrict or enable skills based on the type of equipment held.
 * @author Mobius
 */
public class ConditionUsingMagicWeapon extends Condition
{
	private final boolean _value;
	
	/**
	 * Creates a new {@code ConditionUsingMagicWeapon} instance.<br>
	 * This constructor sets the required magic weapon state.
	 * @param value The boolean value to assign to this condition.
	 */
	public ConditionUsingMagicWeapon(boolean value)
	{
		_value = value;
	}
	
	/**
	 * Checks if the target is using a magic weapon.<br>
	 * This method validates the weapon type of the {@code effected} creature.<br>
	 * It returns {@code false} if the target is not a player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the interaction.
	 * @return {@code true} if the weapon matches the required magic status, otherwise {@code false}.
	 */
	@Override
	public boolean testImpl(Creature effector, Creature effected, Skill skill, ItemTemplate item)
	{
		if ((effected == null) || !effected.isPlayer())
		{
			return false;
		}
		
		final ItemTemplate weapon = effected.getActiveWeaponItem();
		return (weapon != null) && (weapon.isMagicWeapon() == _value);
	}
}
