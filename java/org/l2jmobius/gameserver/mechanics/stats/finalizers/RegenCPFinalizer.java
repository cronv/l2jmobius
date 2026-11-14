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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.IStatFunction;
import org.l2jmobius.gameserver.mechanics.stats.Stat;

/**
 * This class handles the final calculation for the {@code CP} regeneration stat.<br>
 * It ensures that the regeneration rate is correctly applied to a {@code Player}.
 * @author UnAfraid
 */
public class RegenCPFinalizer implements IStatFunction
{
	/**
	 * Calculates the CP regeneration value for a {@code Creature}.<br>
	 * This method applies multipliers based on movement and sitting status.<br>
	 * It returns 0 if the creature is not a {@code Player}.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @param base The {@code OptionalDouble} containing the base value.
	 * @param stat The {@code Stat} type being calculated.
	 * @return The final calculated regeneration value as a {@code double}.
	 */
	@Override
	public double calc(Creature creature, OptionalDouble base, Stat stat)
	{
		throwIfPresent(base);
		if (!creature.isPlayer())
		{
			return 0;
		}
		
		final Player player = creature.asPlayer();
		double baseValue = player.getTemplate().getBaseCpRegen(creature.getLevel()) * creature.getLevelMod() * BaseStat.CON.calcBonus(creature) * PlayerConfig.CP_REGEN_MULTIPLIER;
		if (player.isSitting())
		{
			baseValue *= 1.5; // Sitting
		}
		else if (!player.isMoving())
		{
			baseValue *= 1.1; // Staying
		}
		else if (player.isRunning())
		{
			baseValue *= 0.7; // Running
		}
		
		return Stat.defaultValue(player, stat, baseValue);
	}
}
