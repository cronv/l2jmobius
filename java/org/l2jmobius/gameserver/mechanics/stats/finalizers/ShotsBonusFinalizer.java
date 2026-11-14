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
package org.l2jmobius.gameserver.mechanics.stats.finalizers;

import java.util.OptionalDouble;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class calculates the final bonus for shots based on specific character stats.<br>
 * It ensures that shot-related modifiers are correctly applied to a {@code Creature}.
 * @author UnAfraid
 */
public class ShotsBonusFinalizer implements IStatFunction
{
	/**
	 * Calculates the final value for a specific statistic.<br>
	 * This method applies bonuses from weapons and jewels.<br>
	 * It checks if the {@code creature} is a {@code Player}.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base The {@code OptionalDouble} containing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated double value.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		
		double baseValue = 1;
		final Player player = creature.asPlayer();
		if (player != null)
		{
			final Item weapon = player.getActiveWeaponInstance();
			if ((weapon != null) && weapon.isEnchanted())
			{
				baseValue += (weapon.getEnchantLevel() * 0.7) / 100;
			}
			
			if (isMageCaster(player))
			{
				if ((player.getActiveSapphireJewel() != null))
				{
					baseValue += player.getActiveSapphireJewel().getBonus();
				}
			}
			else if (player.getActiveRubyJewel() != null)
			{
				baseValue += player.getActiveRubyJewel().getBonus();
			}
		}
		
		return Stat.defaultValue(creature, stat, baseValue);
	}
	
	/**
	 * Checks if a {@code Player} is considered a mage caster.<br>
	 * This method excludes certain classes and the {@code Race}.ORC race.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is a mage caster, {@code false} otherwise.
	 */
	private boolean isMageCaster(Player player)
	{
		// Iss classes considered fighters.
		final int classId = player.getActiveClass();
		if ((classId >= PlayerClass.ISS_HIEROPHANT.getId()) && (classId <= PlayerClass.ISS_DOOMCRYER.getId()))
		{
			return false;
		}
		
		return player.isMageClass() && (player.getRace() != Race.ORC);
	}
}
