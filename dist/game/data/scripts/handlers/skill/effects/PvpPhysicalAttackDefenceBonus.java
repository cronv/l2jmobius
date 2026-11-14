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
 * Handles the application of a percentage bonus to physical attack and defense stats during PvP.<br>
 * This effect modifies the {@code Stat} values for players in combat scenarios.
 * @author Sdw
 */
public class PvpPhysicalAttackDefenceBonus extends AbstractStatPercentEffect
{
	/**
	 * Creates a new {@code PvpPhysicalAttackDefenceBonus} effect.<br>
	 * This constructor initializes the bonus using the provided statistics.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public PvpPhysicalAttackDefenceBonus(StatSet params)
	{
		super(params, Stat.PVP_PHYSICAL_ATTACK_DEFENCE);
	}
}
