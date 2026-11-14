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
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code HpDrain} effect logic.<br>
 * It manages how health is drained from a target over time.
 * @author Adry_85, Mobius
 */
public class HpDrain extends AbstractEffect
{
	private final double _power;
	private final double _percentage;
	
	/**
	 * Creates a new {@code HpDrain} effect.<br>
	 * This constructor initializes the drain power and percentage from the provided stats.<br>
	 * It will throw an exception if the {@code amount} key is found in the parameters.
	 * @param params The {@code StatSet} containing the configuration values for this effect.
	 */
	public HpDrain(StatSet params)
	{
		_power = params.getDouble("power", 0);
		_percentage = params.getDouble("percentage", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the effect successfully hits the target.<br>
	 * This method checks if the {@code effector} successfully bypasses the evasion of the {@code effected} creature.<br>
	 * It returns {@code true} if the skill is not evaded and {@code false} otherwise.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect succeeds, {@code false} if it fails.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcSkillEvasion(effector, effected, skill);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this specific effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HP_DRAIN;
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
	 * Applies the instant effect of an HP Drain.<br>
	 * This method calculates damage and heals the {@code effector} based on that damage.<br>
	 * It handles special conditions like Spirit Shots and Magic Criticals.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isAlikeDead() || effected.isInvul())
		{
			return;
		}
		
		final boolean sps = skill.useSpiritShot() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
		final double damage = Formulas.calcMagicDam(effector, effected, skill, effector.getMAtk(), _power, effected.getMDef(), sps, bss, mcrit);
		
		double drain = 0;
		final int cp = (int) effected.getCurrentCp();
		final int hp = (int) effected.getCurrentHp();
		
		// CP only takes the damage of players and summons, a monster drains what it takes from the HP.
		if ((cp > 0) && (effector.isPlayable() || effector.isFakePlayer()))
		{
			drain = (damage < cp) ? 0 : (damage - cp);
		}
		else if (damage > hp)
		{
			drain = hp;
		}
		else
		{
			drain = damage;
		}
		
		final double hpAdd = ((_percentage / 100) * drain);
		final double effectorCurrentHp = effector.getCurrentHp();
		final long effectorMaxHp = effector.getMaxHp();
		final double hpFinal = ((effectorCurrentHp + hpAdd) > effectorMaxHp ? effectorMaxHp : (effectorCurrentHp + hpAdd));
		effector.setCurrentHp(hpFinal);
		
		effector.doAttack(damage, effected, skill, false, false, mcrit, false);
	}
}
