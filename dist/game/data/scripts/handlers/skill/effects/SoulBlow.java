/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code SoulBlow} skill effect.<br>
 * It defines how the effect interacts with targets and applies its specific mechanics.
 * @author Adry_85
 */
public class SoulBlow extends AbstractEffect
{
	private final double _power;
	private final double _chanceBoost;
	private final boolean _overHit;
	
	/**
	 * Creates a new {@code SoulBlow} effect instance.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts power, chance boost, and over hit settings.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public SoulBlow(StatSet params)
	{
		_power = params.getDouble("power");
		_chanceBoost = params.getDouble("chanceBoost");
		_overHit = params.getBoolean("overHit", false);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the effect successfully hits the target.<br>
	 * It checks if the attack bypasses evasion and meets the success chance.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the hit is successful, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcSkillEvasion(effector, effected, skill) && Formulas.calcBlowSuccess(effector, effected, skill, _chanceBoost);
	}
	
	/**
	 * Retrieves the category of this effect.<br>
	 * This method identifies how the effect behaves in the game engine.
	 * @return The {@code EffectType} associated with this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHYSICAL_ATTACK;
	}
	
	/**
	 * Checks if this effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant damage logic for a Soul Blow effect.<br>
	 * This method calculates and applies damage to the target creature.<br>
	 * It handles special conditions like overhits and soul consumption.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (_overHit && effected.isAttackable())
		{
			effected.asAttackable().overhitEnabled(true);
		}
		
		final boolean ss = skill.useSoulShot() && (effector.isChargedShot(ShotType.SOULSHOTS) || effector.isChargedShot(ShotType.BLESSED_SOULSHOTS));
		final byte shld = Formulas.calcShldUse(effector, effected);
		double damage = Formulas.calcBlowDamage(effector, effected, skill, false, _power, shld, ss);
		if (effector.isPlayer() && (skill.getMaxSoulConsumeCount() > 0))
		{
			// Souls Formula (each soul increase +4.8%).
			final int maxSoulConsumeCount = skill.getMaxSoulConsumeCount();
			final int chargedSouls = (effector.asPlayer().getChargedSouls() <= maxSoulConsumeCount) ? effector.asPlayer().getChargedSouls() : maxSoulConsumeCount;
			damage *= 1 + (chargedSouls * 0.048);
		}
		
		effector.doAttack(damage, effected, skill, false, false, true, false);
	}
}
