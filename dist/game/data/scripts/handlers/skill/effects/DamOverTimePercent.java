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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements a damage over time effect based on a percentage.<br>
 * It calculates periodic damage as a portion of the target's stats.
 * @author Adry_85
 */
public class DamOverTimePercent extends AbstractEffect
{
	private final boolean _canKill;
	private final double _power;
	
	/**
	 * Creates a new {@code DamOverTimePercent} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the damage power, tick count, and whether the effect can kill the target.
	 * @param params The {@code StatSet} containing all required configuration values.
	 */
	public DamOverTimePercent(StatSet params)
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
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this specific effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DMG_OVER_TIME_PERCENT;
	}
	
	/**
	 * This method handles the periodic damage logic for a Damage Over Time effect.<br>
	 * It calculates damage based on the current health of the {@code effected} creature.<br>
	 * It ensures that non-lethal skills do not kill the target by capping the damage.<br>
	 * The method returns whether the skill is currently active as a toggle.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the damage.
	 * @param skill The {@code Skill} object used to cast the effect.
	 * @param item The {@code Item} associated with the action, if any.
	 * @return {@code true} if the skill is a toggle and remains active, otherwise {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return false;
		}
		
		double damage = effected.getCurrentHp() * _power * getTicksMultiplier();
		if (damage >= (effected.getCurrentHp() - 1))
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
				if (effected.getCurrentHp() <= 1)
				{
					return skill.isToggle();
				}
				
				damage = effected.getCurrentHp() - 1;
			}
		}
		
		effector.doAttack(damage, effected, skill, true, false, false, false);
		return skill.isToggle();
	}
}
