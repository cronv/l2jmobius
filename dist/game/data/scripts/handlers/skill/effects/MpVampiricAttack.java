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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the effect for skills that restore {@code MP} based on damage dealt.<br>
 * It calculates the amount of mana recovered by the attacker during a combat action.
 * @author Mobius
 */
public class MpVampiricAttack extends AbstractEffect
{
	private final double _amount;
	private final double _sum;
	
	/**
	 * Creates a new {@code MpVampiricAttack} effect.<br>
	 * This constructor initializes the attack values from a provided set of statistics.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public MpVampiricAttack(StatSet params)
	{
		_amount = params.getDouble("amount");
		_sum = _amount * params.getDouble("chance", 100);
	}
	
	/**
	 * Applies the effect to a specific creature.<br>
	 * This method updates the stats of the {@code effected} target based on the provided {@code skill}.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		final CreatureStat stat = effected.getStat();
		stat.mergeAdd(Stat.ABSORB_MANA_DAMAGE_PERCENT, _amount / 100);
		stat.addToMpVampiricSum(_sum);
	}
}
