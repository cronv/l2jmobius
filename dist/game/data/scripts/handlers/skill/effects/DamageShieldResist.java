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
 * Handles the application of a damage shield resistance effect.<br>
 * This class modifies the {@code Stat} values associated with damage reduction.<br>
 * It allows characters to mitigate incoming damage based on specific shield properties.
 * @author Sdw
 */
public class DamageShieldResist extends AbstractStatAddEffect
{
	/**
	 * Creates a new {@code DamageShieldResist} effect.<br>
	 * This constructor initializes the effect using the provided statistics.<br>
	 * It applies the values to the {@code Stat.REFLECT_DAMAGE_PERCENT_DEFENSE} stat.
	 * @param params The set of statistics used to configure this effect.
	 */
	public DamageShieldResist(StatSet params)
	{
		super(params, Stat.REFLECT_DAMAGE_PERCENT_DEFENSE);
	}
}
