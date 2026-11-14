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
 * Handles the application of physical attack effects to a target.<br>
 * This class manages how {@code PhysicalAttack} modifies combat statistics.
 * @author Sdw
 */
public class PhysicalAttack extends AbstractConditionalHpEffect
{
	/**
	 * Creates a new {@code PhysicalAttack} effect.<br>
	 * This constructor initializes the physical attack stats from a provided set.<br>
	 * It ensures that the correct parameter names are used.
	 * @param params The {@code StatSet} containing the attributes for this effect.
	 */
	public PhysicalAttack(StatSet params)
	{
		super(params, Stat.PHYSICAL_ATTACK);
		
		if (params.contains("power"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use amount instead of power.");
		}
	}
}
