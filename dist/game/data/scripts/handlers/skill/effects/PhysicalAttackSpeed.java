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
 * Handles the application of physical attack speed effects to a character.<br>
 * This class modifies the {@code Stat} associated with physical attack speed.<br>
 * It is used by various skills and items to influence combat timing.
 * @author Sdw
 */
public class PhysicalAttackSpeed extends AbstractStatEffect
{
	/**
	 * Creates a new {@code PhysicalAttackSpeed} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public PhysicalAttackSpeed(StatSet params)
	{
		super(params, Stat.PHYSICAL_ATTACK_SPEED);
	}
}
