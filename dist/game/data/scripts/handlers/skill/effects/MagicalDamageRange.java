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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the application of magical damage over a specific range.<br>
 * It serves as an implementation of {@code AbstractEffect} for skill-based magic damage.
 * @author Adry_85
 */
public class MagicalDamageRange extends AbstractEffect
{
	private final double _power;
	private final double _shieldDefPercent;
	
	/**
	 * Creates a new instance of {@code MagicalDamageRange}.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts the required power and shield defense percentages.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public MagicalDamageRange(StatSet params)
	{
		_power = params.getDouble("power");
		_shieldDefPercent = params.getDouble("shieldDefPercent", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the magical damage effect successfully hits the target.<br>
	 * It checks if the {@code effected} creature fails to evade the attack.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the hit is successful, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcSkillEvasion(effector, effected, skill);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
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
	 * Applies the instant magical damage effect to a target.<br>
	 * This method calculates damage based on magic defense and shield stats.<br>
	 * It also handles special cases like fake death and spirit shots.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		double mDef = effected.getMDef();
		switch (Formulas.calcShldUse(effector, effected))
		{
			case Formulas.SHIELD_DEFENSE_SUCCEED:
			{
				mDef += ((effected.getShldDef() * _shieldDefPercent) / 100);
				break;
			}
			case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK:
			{
				mDef = -1;
				break;
			}
		}
		
		double damage = 1;
		final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
		
		if (mDef != -1)
		{
			final boolean spiritShot = skill.useSpiritShot();
			final boolean sps = spiritShot && effector.isChargedShot(ShotType.SPIRITSHOTS);
			final boolean bss = spiritShot && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
			
			damage = Formulas.calcMagicDam(effector, effected, skill, effector.getMAtk(), _power, mDef, sps, bss, mcrit);
		}
		
		effector.doAttack(damage, effected, skill, false, false, mcrit, false);
	}
}
