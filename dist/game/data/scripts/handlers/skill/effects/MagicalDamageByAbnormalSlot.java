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

import org.l2jmobius.gameserver.config.PlayerConfig;
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
 * This class handles the application of magical damage based on an abnormal slot.<br>
 * It calculates and applies damage to a target when specific {@code AbnormalType} conditions are met.
 * @author Sdw
 */
public class MagicalDamageByAbnormalSlot extends AbstractEffect
{
	private final double _power;
	private final Set<AbnormalType> _abnormals;
	
	/**
	 * Creates a new instance of {@code MagicalDamageByAbnormalSlot}.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts the power and the set of abnormal types.
	 * @param params The {@code StatSet} containing configuration data.
	 */
	public MagicalDamageByAbnormalSlot(StatSet params)
	{
		_power = params.getDouble("power", 0);
		
		final String abnormals = params.getString("abnormalType", null);
		if ((abnormals != null) && !abnormals.isEmpty())
		{
			_abnormals = new HashSet<>();
			for (String slot : abnormals.split(";"))
			{
				_abnormals.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_abnormals = Collections.<AbnormalType> emptySet();
		}
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the effect successfully hits the target.<br>
	 * This method checks if the {@code effected} creature fails to evade the attack.<br>
	 * It returns {@code true} if the hit is successful and {@code false} otherwise.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the attack hits, {@code false} if it is evaded.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcSkillEvasion(effector, effected, skill);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MAGICAL_ATTACK;
	}
	
	/**
	 * Checks if this effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the instant effect of a magical damage skill.<br>
	 * This method checks for abnormal types and calculates magic damage.<br>
	 * It also handles special cases like fake death and spirit shots.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		boolean hasAbnormalType = false;
		if (!_abnormals.isEmpty())
		{
			for (AbnormalType abnormal : _abnormals)
			{
				if (effected.hasAbnormalType(abnormal))
				{
					hasAbnormalType = true;
					break;
				}
			}
		}
		
		if (!hasAbnormalType)
		{
			return;
		}
		
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		final boolean useSpiritShot = skill.useSpiritShot();
		final boolean sps = useSpiritShot && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = useSpiritShot && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
		final double damage = Formulas.calcMagicDam(effector, effected, skill, effector.getMAtk(), _power, effected.getMDef(), sps, bss, mcrit);
		
		effector.doAttack(damage, effected, skill, false, false, mcrit, false);
	}
}
