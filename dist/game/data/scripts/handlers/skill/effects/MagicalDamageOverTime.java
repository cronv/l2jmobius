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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the application of magical attack damage over time.<br>
 * It implements an {@code AbstractEffect} to deal periodic damage based on magical stats.
 * @author Nik
 */
public class MagicalDamageOverTime extends AbstractEffect
{
	private final double _power;
	private final boolean _canKill;
	
	/**
	 * Creates a new instance of {@code MagicalDamageOverTime}.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the damage power, tick count, and kill permission.
	 * @param params The {@code StatSet} containing all required configuration values.
	 */
	public MagicalDamageOverTime(StatSet params)
	{
		_power = params.getDouble("power", 0);
		_canKill = params.getBoolean("canKill", false);
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for magical damage over time.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MAGICAL_DMG_OVER_TIME;
	}
	
	/**
	 * This method handles the periodic damage logic for a magical damage over time effect.<br>
	 * It calculates the damage based on the {@code effector} and {@code effected} stats.<br>
	 * It ensures that the target is not killed if the skill configuration forbids it.
	 * @param effector The creature casting or applying the effect.
	 * @param effected The creature receiving the damage from the effect.
	 * @param skill The {@code Skill} object associated with this effect.
	 * @param item The {@code Item} used to trigger the skill, if any.
	 * @return {@code true} if the skill is a toggle and should continue, otherwise {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Creature creature = effector;
		final Creature target = effected;
		
		if (target.isDead())
		{
			return false;
		}
		
		double damage = Formulas.calcMagicDam(creature, target, skill, creature.getMAtk(), _power, target.getMDef(), false, false, false); // In retail spiritshots change nothing.
		damage *= getTicksMultiplier();
		
		if (target.getCurrentHp() > 1)
		{
			final double currentHp = target.getCurrentHp();
			if (damage >= (currentHp - 1))
			{
				if (skill.isToggle())
				{
					target.sendPacket(SystemMessageId.YOUR_SKILL_HAS_BEEN_CANCELLED_DUE_TO_LACK_OF_HP);
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
		}
		
		effector.doAttack(damage, effected, skill, true, false, false, false);
		return skill.isToggle();
	}
}
