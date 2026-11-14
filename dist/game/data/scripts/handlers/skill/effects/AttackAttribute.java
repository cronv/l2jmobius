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
package handlers.skill.effects;

import java.util.EnumSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles effects that modify the {@code Stat} attributes related to attacking.<br>
 * It allows for the dynamic adjustment of various attack-related properties on a {@code Creature}.
 * @author Mobius
 */
public class AttackAttribute extends AbstractEffect
{
	private final double _amount;
	private final Stat _singleStat;
	private final Set<Stat> _multipleStats;
	
	/**
	 * Creates a new {@code AttackAttribute} effect.<br>
	 * This constructor initializes the attack values from a provided set of parameters.<br>
	 * It determines whether to apply a single stat or multiple stats based on the input string.
	 * @param params The {@code StatSet} containing the configuration for this attribute.
	 */
	public AttackAttribute(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
		final String attributes = params.getString("attribute", "FIRE");
		if (attributes.contains(","))
		{
			_singleStat = null;
			_multipleStats = EnumSet.noneOf(Stat.class);
			for (String attribute : attributes.split(","))
			{
				_multipleStats.add(Stat.valueOf(attribute + "_POWER"));
			}
		}
		else
		{
			_singleStat = Stat.valueOf(attributes + "_POWER");
			_multipleStats = null;
		}
	}
	
	/**
	 * Applies the attribute changes to a target creature.<br>
	 * This method updates the stats of the {@code effected} entity based on the defined effect amount.
	 * @param effected The {@code Creature} receiving the stat modification.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		if (_singleStat != null)
		{
			effected.getStat().mergeAdd(_singleStat, _amount);
			return;
		}
		
		for (Stat stat : _multipleStats)
		{
			effected.getStat().mergeAdd(stat, _amount);
		}
	}
}
