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
package handlers.skill.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player is currently wearing a cloak with a specific enchantment.<br>
 * This condition is used to validate skill requirements based on equipped gear.
 * @author CostyKiller
 */
public class EquippedCloakEnchantSkillCondition implements ISkillCondition
{
	private final int _amount;
	
	/**
	 * Creates a new condition for checking cloak enchantments.<br>
	 * This constructor initializes the required amount from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public EquippedCloakEnchantSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount", 0);
	}
	
	/**
	 * Checks if the caster can use a specific skill based on their cloak enchantment.<br>
	 * The caster must be a player and have a cloak equipped.<br>
	 * The cloak's enchantment level must meet the required amount.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((caster == null) || !caster.isPlayer())
		{
			return false;
		}
		
		final Item cloak = caster.getInventory().getPaperdollItem(Inventory.PAPERDOLL_CLOAK);
		if (cloak == null)
		{
			return false;
		}
		
		return cloak.getEnchantLevel() >= _amount;
	}
}
