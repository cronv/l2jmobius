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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the effect of granting a magical attack bonus.<br>
 * It is triggered when the casting speed exceeds the defined limits by a specific percentage.
 * @author SaltyMike
 */
public class ExceedingMagicalAttackSpeed extends AbstractEffect
{
	private final int _percent;
	
	/**
	 * Creates a new instance of {@code ExceedingMagicalAttackSpeed}.<br>
	 * This constructor initializes the effect using provided statistics.<br>
	 * It sets the percentage value from the {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ExceedingMagicalAttackSpeed(StatSet params)
	{
		_percent = params.getInt("percent", 100);
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the magical attack speed effect immediately.<br>
	 * This method calculates and updates stats for the {@code effector}.<br>
	 * It handles capping the magic attack speed based on configuration limits.
	 * @param effector The creature receiving the stat changes.
	 * @param effected The creature targeted by the skill.
	 * @param skill The skill being cast.
	 * @param item The item used to trigger the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final double currentMAtkSpd = effector.getMAtkSpd();
		final double bonusSpeed = (currentMAtkSpd * _percent) / 100.0;
		final double newMAtkSpd = currentMAtkSpd + bonusSpeed;
		if (newMAtkSpd > PlayerConfig.MAX_MATK_SPEED)
		{
			final double bonusSpeedToCap = PlayerConfig.MAX_MATK_SPEED - currentMAtkSpd;
			final double excessBonusSpeed = bonusSpeed - bonusSpeedToCap;
			effector.getStat().mergeAdd(Stat.MAGIC_ATTACK_SPEED, bonusSpeedToCap);
			
			final double currentMAtk = effector.getMAtk();
			final double bonusMAtk = currentMAtk + excessBonusSpeed;
			effector.getStat().mergeMul(Stat.MAGIC_ATTACK, bonusMAtk);
		}
		else
		{
			effector.getStat().mergeAdd(Stat.MAGIC_ATTACK_SPEED, bonusSpeed);
		}
	}
}
