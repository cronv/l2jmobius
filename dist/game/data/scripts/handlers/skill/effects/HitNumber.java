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
 * Handles the visual display of hit numbers when a character takes damage.<br>
 * This class manages how numerical values are shown on screen during combat.
 * @author Sdw
 */
public class HitNumber extends AbstractStatEffect
{
	/**
	 * Creates a new {@code HitNumber} effect.<br>
	 * This constructor initializes the effect using the provided statistics.<br>
	 * It sets the target stat to {@code Stat.ATTACK_COUNT_MAX}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public HitNumber(StatSet params)
	{
		super(params, Stat.ATTACK_COUNT_MAX);
	}
}
