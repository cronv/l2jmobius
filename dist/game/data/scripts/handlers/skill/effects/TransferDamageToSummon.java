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
 * Handles the logic for transferring damage from a player to their summoned units.<br>
 * This effect ensures that damage dealt by the player is redirected to an active summon.
 * @author Sdw
 */
public class TransferDamageToSummon extends AbstractStatAddEffect
{
	/**
	 * This constructor initializes the effect with specific statistics.<br>
	 * It uses the {@code StatSet} to define how damage is transferred.<br>
	 * The effect targets the {@code Stat.TRANSFER_DAMAGE_SUMMON_PERCENT} stat.
	 * @param params The set of statistics used to configure this effect.
	 */
	public TransferDamageToSummon(StatSet params)
	{
		super(params, Stat.TRANSFER_DAMAGE_SUMMON_PERCENT);
	}
}
