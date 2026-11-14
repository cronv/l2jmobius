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

import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the application of magic critical damage effects.<br>
 * This class modifies how {@code Stat} values are calculated for magical attacks.<br>
 * It allows for specific scaling of critical hits based on magic attributes.
 * @author Sdw
 */
public class MagicCriticalDamage extends AbstractStatEffect
{
	/**
	 * Creates a new {@code MagicCriticalDamage} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public MagicCriticalDamage(StatSet params)
	{
		super(params, Stat.MAGIC_CRITICAL_DAMAGE, Stat.MAGIC_CRITICAL_DAMAGE_ADD);
	}
}
