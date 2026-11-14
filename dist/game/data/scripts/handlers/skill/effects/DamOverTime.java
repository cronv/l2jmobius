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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the Damage Over Time (DoT) effect logic.<br>
 * It applies periodic damage to a target creature over a specified duration.
 */
public class DamOverTime extends AbstractEffect
{
	private final boolean _canKill;
	private final double _power;
	
	/**
	 * Creates a new {@code DamOverTime} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the damage power, tick count, and whether the effect can kill the target.
	 * @param params The {@code StatSet} containing all configuration values for this effect.
	 */
	public DamOverTime(StatSet params)
	{
		_canKill = params.getBoolean("canKill", false);
		_power = params.getDouble("power");
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Handles the initial logic when a damage over time effect begins.<br>
	 * It checks for magic critical hits and applies immediate damage if applicable.
	 * @param effector The {@code Creature} that triggered the effect.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to create the effect.
	 * @param item The {@code Item} associated with the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!skill.isToggle() && skill.isMagic())
		{
			// TODO: M.Crit can occur even if this skill is resisted. Only then m.crit damage is applied and not debuff
			final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
			if (mcrit)
			{
				double damage = _power * 10; // Tests show that 10 times HP DOT is taken during magic critical.
				
				final double currentHp = effected.getCurrentHp();
				if (!_canKill && (damage >= (currentHp - 1)))
				{
					damage = currentHp - 1;
				}
				
				effected.reduceCurrentHp(damage, effector, skill, true, false, true, false);
			}
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for damage over time.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DMG_OVER_TIME;
	}
	
	/**
	 * Handles the periodic damage logic for a Damage Over Time effect.<br>
	 * This method calculates damage and applies it to the target.<br>
	 * It checks if the target is alive before proceeding.<br>
	 * It also ensures that non-lethal skills do not kill the target.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} associated with the action, if any.
	 * @return {@code true} if the skill is a toggle and continues, {@code false} otherwise.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return false;
		}
		
		double damage = _power * getTicksMultiplier();
		final double currentHp = effected.getCurrentHp();
		if (damage >= (currentHp - 1))
		{
			if (skill.isToggle())
			{
				effected.sendPacket(SystemMessageId.YOUR_SKILL_HAS_BEEN_CANCELLED_DUE_TO_LACK_OF_HP);
				return false;
			}
			
			// For DOT skills that will not kill effected player.
			if (!_canKill)
			{
				// Fix for players dying by DOTs if HP < 1 since reduceCurrentHP method will kill them.
				if (currentHp <= 1)
				{
					return skill.isToggle();
				}
				
				damage = currentHp - 1;
			}
		}
		
		effector.doAttack(damage, effected, skill, true, false, false, false);
		return skill.isToggle();
	}
}
