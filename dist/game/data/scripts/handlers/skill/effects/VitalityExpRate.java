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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the effect of modifying a creature's experience gain rate based on their {@code Vitality} stat.<br>
 * This class calculates the percentage change applied to the experience earned by the target.
 * @author Mobius
 */
public class VitalityExpRate extends AbstractStatPercentEffect
{
	/**
	 * Creates a new {@code VitalityExpRate} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public VitalityExpRate(StatSet params)
	{
		super(params, Stat.VITALITY_EXP_RATE);
	}
	
	/**
	 * Applies the vitality experience rate boost to a creature.<br>
	 * This method updates the {@code Stat.VITALITY_EXP_RATE} and {@code Stat.VITALITY_SKILLS} stats.<br>
	 * If the target is a {@code Player}, it triggers a user boost notification.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		effected.getStat().mergeMul(Stat.VITALITY_EXP_RATE, (_amount / 100) + 1);
		effected.getStat().mergeAdd(Stat.VITALITY_SKILLS, 1d);
		
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		player.sendUserBoostStat();
	}
}
