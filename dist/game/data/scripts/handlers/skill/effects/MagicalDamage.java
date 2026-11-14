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

import java.util.EnumSet;
import java.util.Set;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the application of magical damage effects.<br>
 * It calculates and applies damage to a {@code Creature} based on magical stats.
 * @author Adry_85, Mobius
 */
public class MagicalDamage extends AbstractEffect
{
	private final double _power;
	private final boolean _overHit;
	private final double _debuffModifier;
	private final double _raceModifier;
	private final Set<Race> _races = EnumSet.noneOf(Race.class);
	
	/**
	 * Creates a new instance of {@code MagicalDamage}.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts power, overhit status, and various modifiers.
	 * @param params The {@code StatSet} containing all configuration data.
	 */
	public MagicalDamage(StatSet params)
	{
		_power = params.getDouble("power", 0);
		_overHit = params.getBoolean("overHit", false);
		_debuffModifier = params.getDouble("debuffModifier", 1);
		_raceModifier = params.getDouble("raceModifier", 1);
		if (params.contains("races"))
		{
			for (String race : params.getString("races", "").split(";"))
			{
				_races.add(Race.valueOf(race));
			}
		}
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
		
		if (params.contains("mode"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should not have mode.");
		}
	}
	
	/**
	 * Determines if the magical damage effect successfully hits the target.<br>
	 * This method checks if the {@code effector} fails to evade the attack from the {@code skill}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used for the calculation.
	 * @return {@code true} if the hit is successful, or {@code false} if it was evaded.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcSkillEvasion(effector, effected, skill);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns the {@code MAGICAL_ATTACK} constant.
	 * @return the {@code EffectType} for this magical damage effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MAGICAL_ATTACK;
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
	 * Applies the magical damage effect instantly to a target.<br>
	 * This method calculates the final damage based on various modifiers and applies it to the {@code effected} creature.<br>
	 * It also handles specific mechanics like fake death, overhit status, and spirit shots.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the damage.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		if (_overHit && effected.isAttackable())
		{
			effected.asAttackable().overhitEnabled(true);
		}
		
		final boolean sps = skill.useSpiritShot() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
		double damage = Formulas.calcMagicDam(effector, effected, skill, effector.getMAtk(), _power, effected.getMDef(), sps, bss, mcrit);
		
		// Apply debuff modifier.
		if (effected.getEffectList().getDebuffCount() > 0)
		{
			damage *= _debuffModifier;
		}
		
		// Apply race modifier.
		if (_races.contains(effected.getRace()))
		{
			damage *= _raceModifier;
		}
		
		effector.doAttack(damage, effected, skill, false, false, mcrit, false);
	}
}
