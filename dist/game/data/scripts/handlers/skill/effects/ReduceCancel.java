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
 * This class handles the effect that reduces the chance of a skill being canceled.<br>
 * It modifies the {@code Stat} values to ensure skills execute more reliably.
 * @author Sdw
 */
public class ReduceCancel extends AbstractStatEffect
{
	/**
	 * Creates a new {@code ReduceCancel} effect.<br>
	 * This effect modifies the {@code ATTACK_CANCEL} stat.
	 * @param params The set of statistics used to configure this effect.
	 */
	public ReduceCancel(StatSet params)
	{
		super(params, Stat.ATTACK_CANCEL);
	}
}
