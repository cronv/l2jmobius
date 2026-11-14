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
 * This class handles the effect of granting a physical attack.<br>
 * It triggers when the {@code attackSpeed} exceeds its defined limits by a specific percentage.
 * @author SaltyMike
 */
public class ExceedingPhysicalAttackSpeed extends AbstractEffect
{
	private final int _percent;
	
	/**
	 * Creates a new instance of {@code ExceedingPhysicalAttackSpeed}.<br>
	 * This effect handles physical attack speed limits.<br>
	 * It initializes the percentage from the provided stats.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ExceedingPhysicalAttackSpeed(StatSet params)
	{
		_percent = params.getInt("percent", 100);
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the effect to a creature immediately.<br>
	 * This method calculates and updates physical attack speed and power stats.<br>
	 * It ensures that the final attack speed does not exceed the maximum limit defined in {@code PlayerConfig}.
	 * @param effector The {@code Creature} receiving the stat changes.
	 * @param effected The {@code Creature} targeted by the action.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final double currentPAtkSpd = effector.getPAtkSpd();
		final double bonusSpeed = (currentPAtkSpd * _percent) / 100.0;
		final double newPAtkSpd = currentPAtkSpd + bonusSpeed;
		if (newPAtkSpd > PlayerConfig.MAX_PATK_SPEED)
		{
			final double bonusSpeedToCap = PlayerConfig.MAX_PATK_SPEED - currentPAtkSpd;
			final double excessBonusSpeed = bonusSpeed - bonusSpeedToCap;
			effector.getStat().mergeAdd(Stat.PHYSICAL_ATTACK_SPEED, bonusSpeedToCap);
			
			final double currentPAtk = effector.getPAtk();
			final double bonusPAtk = currentPAtk + excessBonusSpeed;
			effector.getStat().mergeMul(Stat.PHYSICAL_ATTACK, bonusPAtk);
		}
		else
		{
			effector.getStat().mergeAdd(Stat.PHYSICAL_ATTACK_SPEED, bonusSpeed);
		}
	}
}
