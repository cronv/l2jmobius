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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code HpToOwner} effect.<br>
 * It applies health points to the owner of a skill.<br>
 * It is an implementation of {@code AbstractEffect}.
 * @author Sdw
 */
public class HpToOwner extends AbstractEffect
{
	private final double _power;
	private final int _stealAmount;
	
	/**
	 * Creates a new {@code HpToOwner} effect instance.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the power, steal amount, and tick count based on the provided parameters.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public HpToOwner(StatSet params)
	{
		_power = params.getDouble("power");
		_stealAmount = params.getInt("stealAmount");
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * This method handles the logic when an effect begins.<br>
	 * It checks if the {@code Skill} is a magic skill and not a toggle.<br>
	 * If a magic critical occurs, it applies extra damage to the {@code effected} creature.
	 * @param effector The creature that initiated the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
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
				final double damage = _power * 10; // Tests show that 10 times HP DOT is taken during magic critical.
				effected.reduceCurrentHp(damage, effector, skill, true, false, true, false);
			}
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DMG_OVER_TIME;
	}
	
	/**
	 * This method handles the logic that occurs during each tick of the effect.<br>
	 * It checks if the target is alive before applying damage to the effector.<br>
	 * If a steal amount is set, it heals the effector based on the damage dealt.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill that triggered this effect.
	 * @param item The item associated with the skill.
	 * @return {@code true} if the skill is a toggle, otherwise {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return false;
		}
		
		final double damage = _power * getTicksMultiplier();
		
		effector.doAttack(damage, effected, skill, true, false, false, false);
		if (_stealAmount > 0)
		{
			final double amount = (damage * _stealAmount) / 100;
			effector.setCurrentHp(effector.getCurrentHp() + amount);
			effector.setCurrentMp(effector.getCurrentMp() + amount);
		}
		
		return skill.isToggle();
	}
}
