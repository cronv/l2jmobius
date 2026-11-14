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
 * Handles the logic for the {@code MpShield} skill effect.<br>
 * This class manages how the shield affects a character's statistics.<br>
 * It extends {@code AbstractStatAddEffect} to apply specific stat modifications.
 * @author Sdw
 */
public class MpShield extends AbstractStatAddEffect
{
	/**
	 * Creates a new {@code MpShield} effect.<br>
	 * This constructor initializes the shield based on provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public MpShield(StatSet params)
	{
		super(params, Stat.MANA_SHIELD_PERCENT);
	}
}
