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
 * This class handles the logic for modifying a player's world chat points.<br>
 * It extends {@code AbstractStatEffect} to apply changes to specific character statistics.
 * @author Nik
 */
public class WorldChatPoints extends AbstractStatEffect
{
	/**
	 * Creates a new {@code WorldChatPoints} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the initial values.
	 */
	public WorldChatPoints(StatSet params)
	{
		super(params, Stat.WORLD_CHAT_POINTS);
	}
}
