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
 * Handles the logic for applying a damage shield effect to an entity.<br>
 * This class modifies stats to provide protection against incoming damage.
 * @author Sdw
 */
public class DamageShield extends AbstractStatAddEffect
{
	/**
	 * Creates a new {@code DamageShield} effect.<br>
	 * This constructor initializes the shield using the provided statistics.<br>
	 * It applies the effect to the {@code Stat.REFLECT_DAMAGE_PERCENT} stat.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public DamageShield(StatSet params)
	{
		super(params, Stat.REFLECT_DAMAGE_PERCENT);
	}
}
