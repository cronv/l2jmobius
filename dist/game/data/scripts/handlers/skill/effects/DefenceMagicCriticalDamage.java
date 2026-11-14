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
 * Handles the calculation for magic critical damage against a target's defense.<br>
 * This effect modifies how {@code Stat} values are applied during magical critical hits.
 * @author Sdw
 */
public class DefenceMagicCriticalDamage extends AbstractStatEffect
{
	/**
	 * Creates a new instance of {@code DefenceMagicCriticalDamage}.<br>
	 * This effect handles the magic critical damage defense stat.<br>
	 * It uses the provided statistics to initialize the effect.
	 * @param params The {@code StatSet} containing the required values for this effect.
	 */
	public DefenceMagicCriticalDamage(StatSet params)
	{
		super(params, Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE, Stat.DEFENCE_MAGIC_CRITICAL_DAMAGE_ADD);
	}
}
