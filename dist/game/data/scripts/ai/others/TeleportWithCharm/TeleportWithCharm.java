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
package ai.others.TeleportWithCharm;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class implements the {@code TeleportWithCharm} AI script.<br>
 * It handles the logic for NPCs that perform a teleport action while using a charm.
 * @author Plim
 */
public class TeleportWithCharm extends Script
{
	// NPCs
	private static final int WHIRPY = 30540;
	private static final int TAMIL = 30576;
	
	// Items
	private static final int ORC_GATEKEEPER_CHARM = 1658;
	private static final int DWARF_GATEKEEPER_TOKEN = 1659;
	
	// Locations
	private static final Location ORC_TELEPORT = new Location(-80826, 149775, -3043);
	private static final Location DWARF_TELEPORT = new Location(-80826, 149775, -3043);
	
	/**
	 * Initializes the {@code TeleportWithCharm} script.<br>
	 * Registers the required NPCs and talk IDs for this script.
	 */
	private TeleportWithCharm()
	{
		addStartNpc(WHIRPY, TAMIL);
		addTalkId(WHIRPY, TAMIL);
	}
	
	/**
	 * Handles the interaction when a {@code Player} talks to an {@code Npc}.<br>
	 * It checks for specific quest items and teleports the player if requirements are met.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the HTML file to display or a default response.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		switch (npc.getId())
		{
			case WHIRPY:
			{
				if (hasQuestItems(player, DWARF_GATEKEEPER_TOKEN))
				{
					takeItems(player, DWARF_GATEKEEPER_TOKEN, 1);
					player.teleToLocation(DWARF_TELEPORT);
				}
				else
				{
					return "30540-01.htm";
				}
				break;
			}
			case TAMIL:
			{
				if (hasQuestItems(player, ORC_GATEKEEPER_CHARM))
				{
					takeItems(player, ORC_GATEKEEPER_CHARM, 1);
					player.teleToLocation(ORC_TELEPORT);
				}
				else
				{
					return "30576-01.htm";
				}
				break;
			}
		}
		
		return super.onTalk(npc, player);
	}
	
	/**
	 * This is the main entry point for the {@code TeleportWithCharm} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new TeleportWithCharm();
	}
}
