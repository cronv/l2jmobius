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
 * Handles the calculation of dispel damage based on magical abnormal types.<br>
 * This class implements a specific logic for applying damage when an effect is dispelled.
 * @author Nik
 */
public class MagicalAbnormalDispelDamage extends AbstractEffect
{
	private final double _power;
	private final AbnormalType _abnormalType;
	
	/**
	 * Creates a new instance of the {@code MagicalAbnormalDispelDamage} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the power and the specific {@code AbnormalType} for the dispel logic.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public MagicalAbnormalDispelDamage(StatSet params)
	{
		_power = params.getDouble("power", 0);
		_abnormalType = AbnormalType.getAbnormalType(params.getString("abnormalType", null));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the effect successfully hits the target.<br>
	 * This method checks if the {@code effector} fails to evade the {@code skill}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect hits, and {@code false} if it is evaded.
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
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a magical dispel.<br>
	 * This method removes an abnormal status and applies damage to the target.<br>
	 * It handles special cases like fake death and spirit shots.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// First dispells the effect, then does damage. Sometimes the damage is evaded, but debuff is still dispelled.
		if (effector.isAlikeDead() || (_abnormalType == AbnormalType.NONE) || !effected.getEffectList().stopEffects(_abnormalType))
		{
			return;
		}
		
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		final boolean sps = skill.useSpiritShot() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
		final double damage = Formulas.calcMagicDam(effector, effected, skill, effector.getMAtk(), _power, effected.getMDef(), sps, bss, mcrit);
		
		effector.doAttack(damage, effected, skill, false, false, mcrit, false);
	}
}
