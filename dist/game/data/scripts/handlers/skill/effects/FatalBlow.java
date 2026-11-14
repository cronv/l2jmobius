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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

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
 * This class handles the {@code FatalBlow} skill effect.<br>
 * It manages the logic for applying fatal damage to a target creature.
 * @author Adry_85
 */
public class FatalBlow extends AbstractEffect
{
	private final double _power;
	private final double _chanceBoost;
	private final double _criticalChance;
	private final Set<AbnormalType> _abnormals;
	private final double _abnormalPower;
	private final boolean _overHit;
	
	/**
	 * Creates a new instance of the {@code FatalBlow} effect.<br>
	 * This constructor initializes all required statistics from a {@code StatSet}.<br>
	 * It validates that the {@code power} key is used instead of {@code amount}.
	 * @param params The {@code StatSet} containing configuration values for this effect.
	 */
	public FatalBlow(StatSet params)
	{
		_power = params.getDouble("power");
		_chanceBoost = params.getDouble("chanceBoost");
		_criticalChance = params.getDouble("criticalChance", 0);
		_overHit = params.getBoolean("overHit", false);
		
		final String abnormals = params.getString("abnormalType", null);
		if ((abnormals != null) && !abnormals.isEmpty())
		{
			_abnormals = new HashSet<>();
			for (String slot : abnormals.split(";"))
			{
				_abnormals.add(Enum.valueOf(AbnormalType.class, slot));
			}
		}
		else
		{
			_abnormals = Collections.<AbnormalType> emptySet();
		}
		
		_abnormalPower = params.getDouble("abnormalPower", 1);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the effect successfully hits the target.<br>
	 * It checks if the attack bypasses evasion and meets the success rate requirements.
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
	 * This method identifies how the effect interacts with the game world.
	 * @return The {@code EffectType} associated with this instance.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHYSICAL_ATTACK;
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
	 * Applies the fatal blow effect immediately.<br>
	 * This method calculates damage based on power and critical chance.<br>
	 * It then executes an attack from the {@code effector} to the {@code effected}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used for the effect.
	 * @param item The item associated with the effect.
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
		
		double power = _power;
		
		// Check if we apply an abnormal modifier.
		if (!_abnormals.isEmpty())
		{
			for (AbnormalType abnormal : _abnormals)
			{
				if (effected.hasAbnormalType(abnormal))
				{
					power += _abnormalPower;
					break;
				}
			}
		}
		
		final boolean ss = skill.useSoulShot() && (effector.isChargedShot(ShotType.SOULSHOTS) || effector.isChargedShot(ShotType.BLESSED_SOULSHOTS));
		final byte shld = Formulas.calcShldUse(effector, effected);
		double damage = Formulas.calcBlowDamage(effector, effected, skill, false, power, shld, ss);
		final boolean crit = Formulas.calcCrit(_criticalChance, effector, effected, skill);
		
		if (crit)
		{
			damage *= 2;
		}
		
		effector.doAttack(damage, effected, skill, false, false, true, false);
	}
}
