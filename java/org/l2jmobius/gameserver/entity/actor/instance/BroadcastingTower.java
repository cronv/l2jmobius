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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Represents a broadcasting tower NPC that displays messages to nearby players.<br>
 * This entity handles the logic for showing information in the game world.
 * @author NightMarez
 */
public class BroadcastingTower extends Npc
{
	/**
	 * Creates a new {@code BroadcastingTower} instance.<br>
	 * This constructor initializes the tower using a provided template.<br>
	 * It sets the specific instance type to {@code InstanceType.BroadcastingTower}.
	 * @param template The {@code NpcTemplate} used to define the tower's properties.
	 */
	public BroadcastingTower(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.BroadcastingTower);
	}
	
	/**
	 * Displays a specific chat window to a player.<br>
	 * The window shown depends on the player's location and the provided value.<br>
	 * It sends an {@code NpcHtmlMessage} packet containing the correct HTML file.
	 * @param player The {@code Player} who will receive the message.
	 * @param value An integer used to determine which specific HTML file to display.
	 */
	@Override
	public void showChatWindow(Player player, int value)
	{
		String filename = null;
		if (isInsideRadius2D(-79884, 86529, 0, 50) || isInsideRadius2D(-78858, 111358, 0, 50) || isInsideRadius2D(-76973, 87136, 0, 50) || isInsideRadius2D(-75850, 111968, 0, 50))
		{
			final int id = getId();
			if (value == 0)
			{
				filename = "data/html/observation/" + id + "-Oracle.htm";
			}
			else
			{
				filename = "data/html/observation/" + id + "-Oracle-" + value + ".htm";
			}
		}
		else if (value == 0)
		{
			filename = "data/html/observation/" + getId() + ".htm";
		}
		else
		{
			filename = "data/html/observation/" + getId() + "-" + value + ".htm";
		}
		
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(objectId));
		player.sendPacket(html);
	}
}
