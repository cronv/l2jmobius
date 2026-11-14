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
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a creature meets the required base statistic for a skill.<br>
 * This condition compares the current {@code BaseStat} of an actor against a specified value.
 * @author Mobius
 */
public class OpBaseStatSkillCondition implements ISkillCondition
{
	private final BaseStat _stat;
	private final int _min;
	private final int _max;
	
	/**
	 * Creates a new skill condition based on base statistics.<br>
	 * This constructor extracts the required values from a {@code StatSet}.<br>
	 * It sets the target {@code BaseStat}, minimum value, and maximum value.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpBaseStatSkillCondition(StatSet params)
	{
		_stat = params.getEnum("stat", BaseStat.class);
		_min = params.getInt("min", 0);
		_max = params.getInt("max", 2147483647);
	}
	
	/**
	 * Checks if the {@code caster} meets the required base stat requirements.<br>
	 * It verifies if the current stat value falls between the defined minimum and maximum limits.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @return {@code true} if the stat is within the required range, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		int currentValue = 0;
		switch (_stat)
		{
			case STR:
			{
				currentValue = caster.getSTR();
				break;
			}
			case INT:
			{
				currentValue = caster.getINT();
				break;
			}
			case DEX:
			{
				currentValue = caster.getDEX();
				break;
			}
			case WIT:
			{
				currentValue = caster.getWIT();
				break;
			}
			case CON:
			{
				currentValue = caster.getCON();
				break;
			}
			case MEN:
			{
				currentValue = caster.getMEN();
				break;
			}
		}
		
		return (currentValue >= _min) && (currentValue <= _max);
	}
}
