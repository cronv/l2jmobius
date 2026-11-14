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
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Backstab} skill effect.<br>
 * It calculates damage bonuses when an attack hits a target from behind.
 * @author Adry_85
 */
public class Backstab extends AbstractEffect
{
	private final double _power;
	private final double _chanceBoost;
	private final double _criticalChance;
	private final boolean _overHit;
	
	/**
	 * Creates a new {@code Backstab} effect instance.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts parameters like {@code power}, {@code chanceBoost}, and {@code overHit}.
	 * @param params The {@code StatSet} containing all required configuration values.
	 */
	public Backstab(StatSet params)
	{
		_power = params.getDouble("power");
		_chanceBoost = params.getDouble("chanceBoost");
		_criticalChance = params.getDouble("criticalChance", 0);
		_overHit = params.getBoolean("overHit", false);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the backstab effect is successfully applied.<br>
	 * It checks if the {@code effector} is behind the {@code effected}.<br>
	 * It also verifies that the attack is not evaded and succeeds based on chance.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @return {@code true} if all conditions for a successful backstab are met, otherwise {@code false}.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return (!effector.isInFrontOf(effected) || effector.hasAbnormalType(AbnormalType.GHOSTLY_WHISPERS)) && !Formulas.calcSkillEvasion(effector, effected, skill) && Formulas.calcBlowSuccess(effector, effected, skill, _chanceBoost);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this backstab effect.
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
	 * Applies the backstab effect immediately.<br>
	 * This method calculates damage based on power and critical chance.<br>
	 * It then executes an attack from the {@code effector} to the {@code effected}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used for the effect.
	 * @param item The item associated with the skill.
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
		double damage = Formulas.calcBlowDamage(effector, effected, skill, true, _power, shld, ss);
		
		if (Formulas.calcCrit(_criticalChance, effector, effected, skill))
		{
			damage *= 2;
		}
		
		effector.doAttack(damage, effected, skill, false, true, true, false);
	}
}
