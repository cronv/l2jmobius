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
 * Handles the physical attack and defense bonus for PvE raids.<br>
 * This effect applies a percentage-based modifier to relevant stats during raid encounters.
 * @author Sdw
 */
public class PveRaidPhysicalAttackDefenceBonus extends AbstractStatPercentEffect
{
	/**
	 * Creates a new instance of {@code PveRaidPhysicalAttackDefenceBonus}.<br>
	 * This constructor initializes the effect using a provided set of statistics.
	 * @param params The {@code StatSet} containing the values for this bonus.
	 */
	public PveRaidPhysicalAttackDefenceBonus(StatSet params)
	{
		super(params, Stat.PVE_RAID_PHYSICAL_ATTACK_DEFENCE);
	}
}
