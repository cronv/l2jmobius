/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.others.ClanHallDoorManager;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Manages the artificial intelligence for doors within a {@code ClanHall}.<br>
 * This class handles how NPCs interact with and operate clan hall entrances.
 * @author St3eT, Mobius
 */
public class ClanHallDoorManager extends Script
{
	// NPCs
	// @formatter:off
	private static final int[] DOOR_MANAGERS =
	{
		35385, 35387, 35389, 35391, // Gludio
		35393, 35395, 35397, 35399, 35401, // Gludin
		35402, 35404, 35406, // Dion
		35440, 35442, 35444, 35446, 35448, 35450, // Aden
		35452, 35454, 35456, 35458, 35460, // Giran
		35462, 35464, 35466, 35468, // Goddard
		35567, 35569, 35571, 35573, 35575, 35577, 35579, // Rune
		35581, 35583, 35585, 35587, // Schuttgart
		36722, 36724, 36726, 36728, // Gludio Outskirts
		36730, 36732, 36734, 36736, // Dion Outskirts
		36738, 36740, // Floran Village		
	};
	// @formatter:on
	
	/**
	 * Initializes the {@code ClanHallDoorManager} script.<br>
	 * Registers the required NPCs and talk IDs for door management.
	 */
	private ClanHallDoorManager()
	{
		addStartNpc(DOOR_MANAGERS);
		addTalkId(DOOR_MANAGERS);
		addFirstTalkId(DOOR_MANAGERS);
	}
	
	/**
	 * Handles specific events for the clan hall door manager.<br>
	 * This method processes actions like indexing or managing doors.<br>
	 * It returns the path to an HTML file based on the action performed.
	 * @param event The event string containing the action to perform.
	 * @param npc The {@code Npc} entity triggering the event.
	 * @param player The {@code Player} entity interacting with the NPC.
	 * @return A {@code String} representing the HTML file path or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String action = st.nextToken();
		final ClanHall clanHall = npc.getClanHall();
		String htmltext = null;
		if (clanHall != null)
		{
			switch (action)
			{
				case "index":
				{
					htmltext = onFirstTalk(npc, player);
					break;
				}
				case "manageDoors":
				{
					if (isOwningClan(player, npc) && st.hasMoreTokens() && player.hasAccess(ClanAccess.HALL_OPEN_DOOR))
					{
						final boolean open = st.nextToken().equals("1");
						clanHall.openCloseDoors(open);
						htmltext = "ClanHallDoorManager-0" + (open ? "5" : "6") + ".html";
					}
					else
					{
						htmltext = "ClanHallDoorManager-04.html";
					}
					break;
				}
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the initial dialogue when a player speaks to a door manager.<br>
	 * It determines which HTML template to display based on the player's clan ownership.<br>
	 * The method returns the formatted HTML text for the NPC to speak.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return The resulting HTML string to be displayed in the chat window.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		final ClanHall clanHall = npc.getClanHall();
		if (isOwningClan(player, npc))
		{
			htmltext = getHtm(player, "ClanHallDoorManager-01.html");
			htmltext = htmltext.replace("%ownerClanName%", clanHall.getOwner().getName());
			htmltext = htmltext.replace("%wyvern%", npc.getTemplate().getParameters().getInt("Is_Wyvern", 0) == 1 ? "<br><button action=\"bypass -h Script WyvernManager Return\" value=\"Wyvern\" width=80 height=27 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "");
		}
		else if (clanHall.getOwnerId() <= 0)
		{
			htmltext = "ClanHallDoorManager-02.html";
		}
		else
		{
			htmltext = getHtm(player, "ClanHallDoorManager-03.html");
			htmltext = htmltext.replace("%ownerName%", clanHall.getOwner().getLeaderName());
			htmltext = htmltext.replace("%ownerClanName%", clanHall.getOwner().getName());
		}
		
		return htmltext;
	}
	
	/**
	 * Checks if the {@code Player} belongs to the clan that owns the hall associated with the {@code Npc}.<br>
	 * It verifies that the {@code ClanHall} exists and matches the player's clan ID.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} representing the door manager.
	 * @return {@code true} if the player belongs to the owning clan, {@code false} otherwise.
	 */
	private boolean isOwningClan(Player player, Npc npc)
	{
		final ClanHall clanHall = npc.getClanHall();
		return ((clanHall != null) && (clanHall.getOwnerId() == player.getClanId()) && (player.getClanId() != 0));
	}
	
	/**
	 * This is the entry point for the {@code ClanHallDoorManager} class.<br>
	 * It initializes a new instance of the manager to start the script.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new ClanHallDoorManager();
	}
}
