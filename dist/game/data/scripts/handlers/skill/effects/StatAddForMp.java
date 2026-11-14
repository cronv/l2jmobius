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
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the application of a stat bonus specifically for Maximum MP.<br>
 * It modifies the {@code Stat} value of a {@code Creature} when an effect is active.
 * @author Mobius
 */
public class StatAddForMp extends AbstractEffect
{
	private final int _mp;
	private final Stat _stat;
	private final double _amount;
	
	/**
	 * Creates a new {@code StatAddForMp} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the MP value, the target stat, and the amount.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public StatAddForMp(StatSet params)
	{
		_mp = params.getInt("mp", 0);
		_stat = params.getEnum("stat", Stat.class);
		_amount = params.getDouble("amount", 0);
	}
	
	/**
	 * Checks if the effect should be delayed before pumping.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the pump is not delayed.
	 */
	@Override
	public boolean delayPump()
	{
		return true;
	}
	
	/**
	 * Applies a stat modification to a {@code Creature}.<br>
	 * This method checks if the target has enough maximum MP.<br>
	 * If the requirement is met, it merges the specified amount into the target's stats.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		if (effected.getMaxMp() >= _mp)
		{
			effected.getStat().mergeAdd(_stat, _amount);
		}
	}
}
