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
package ai.areas.WallOfArgos.ElmoredenServantsGhost;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the artificial intelligence behavior for the {@code ElmoredenServantsGhost} NPC.<br>
 * It handles the specific logic and actions for this entity within its designated area.
 * @author St3eT
 */
public class ElmoredenServantsGhost extends Script
{
	// NPC
	private static final int GHOST = 31920; // Elmoreden Servant's Ghost
	
	// Items
	private static final int USED_GRAVE_PASS = 7261;
	private static final int ANTIQUE_BROOCH = 7262;
	
	/**
	 * Private constructor for the {@code ElmoredenServantsGhost} class.<br>
	 * This constructor initializes the NPC IDs and talk IDs required for the script.
	 */
	private ElmoredenServantsGhost()
	{
		addStartNpc(GHOST);
		addTalkId(GHOST);
		addFirstTalkId(GHOST);
	}
	
	/**
	 * Handles specific events for the Elmoreden Servant's Ghost.<br>
	 * This method manages teleportation logic based on quest items.<br>
	 * It checks if the {@code player} has required items before moving them.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A string containing HTML text or null if no message is needed.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.equals("teleport1") || event.equals("teleport2"))
		{
			if (!hasAtLeastOneQuestItem(player, USED_GRAVE_PASS, ANTIQUE_BROOCH))
			{
				htmltext = "31920-no.html";
			}
			else
			{
				takeItems(player, USED_GRAVE_PASS, 1);
				
				final Location loc;
				final StatSet npcParameters = npc.getParameters();
				if (event.equals("teleport1"))
				{
					loc = new Location(npcParameters.getInt("TelPos_X1", 0), npcParameters.getInt("TelPos_Y1", 0), npcParameters.getInt("TelPos_Z1", 0));
				}
				else
				{
					loc = new Location(npcParameters.getInt("TelPos_X2", 0), npcParameters.getInt("TelPos_Y2", 0), npcParameters.getInt("TelPos_Z2", 0));
				}
				
				player.teleToLocation(loc);
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the initial interaction when a {@code Player} talks to an {@code Npc}.<br>
	 * It determines which dialogue file to display based on the NPC's ID.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code ElmoredenServantsGhost} script.<br>
	 * It initializes a new instance of the class to start the AI logic.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new ElmoredenServantsGhost();
	}
}
