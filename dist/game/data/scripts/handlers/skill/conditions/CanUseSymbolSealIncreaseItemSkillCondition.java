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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player meets the requirements to use a skill that increases item symbols.<br>
 * This condition validates specific criteria for {@code SymbolSealIncreaseItem} skills.
 * @author CostyKiller
 */
public class CanUseSymbolSealIncreaseItemSkillCondition implements ISkillCondition
{
	private final int _amount;
	
	/**
	 * Creates a new condition to check if an item's seal can be increased.<br>
	 * It initializes the required amount from the provided configuration.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public CanUseSymbolSealIncreaseItemSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount", 0);
	}
	
	/**
	 * Checks if the {@code caster} can use a specific {@code skill}.<br>
	 * This method verifies that the {@code caster} is a player.<br>
	 * It also ensures the player does not exceed the maximum symbol seal points after using the skill.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being attempted.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isPlayer() && ((caster.asPlayer().getSymbolSealPoints() + ((PlayerConfig.MAX_SYMBOL_SEAL_POINTS * _amount) / 100)) <= PlayerConfig.MAX_SYMBOL_SEAL_POINTS);
	}
}
