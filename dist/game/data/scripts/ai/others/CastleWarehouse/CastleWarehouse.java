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
package ai.others.CastleWarehouse;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for the {@code CastleWarehouse} keeper.<br>
 * It defines the behaviors and logic for this specific NPC script.
 * @author malyelfik
 */
public class CastleWarehouse extends Script
{
	// NPCs
	private static final int[] NPCS =
	{
		35099, // Warehouse Keeper (Gludio)
		35141, // Warehouse Keeper (Dion)
		35183, // Warehouse Keeper (Giran)
		35225, // Warehouse Keeper (Oren)
		35273, // Warehouse Keeper (Aden)
		35315, // Warehouse Keeper (Inadril)
		35362, // Warehouse Keeper (Goddard)
		35508, // Warehouse Keeper (Rune)
		35554, // Warehouse Keeper (Schuttgart)
	};
	
	// Items
	private static final int BLOOD_OATH = 9910;
	private static final int BLOOD_ALLIANCE = 9911;
	
	/**
	 * Private constructor for the {@code CastleWarehouse} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private CastleWarehouse()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	/**
	 * Handles specific events for the Castle Warehouse NPC.<br>
	 * This method determines which HTML response to show based on player actions and clan status.
	 * @param event The name of the event or HTML file requested.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The name of the HTML file to display, or {@code null} if no event matches.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = event;
		final Clan clan = player.getClan();
		final boolean isMyLord = player.isClanLeader() && (clan.getCastleId() == (npc.getCastle() != null ? npc.getCastle().getResidenceId() : -1));
		switch (event)
		{
			case "warehouse-01.html":
			case "warehouse-02.html":
			case "warehouse-03.html":
			{
				break;
			}
			case "warehouse-04.html":
			{
				htmltext = (!isMyLord) ? "warehouse-no.html" : getHtm(player, "warehouse-04.html").replace("%blood%", Integer.toString(clan.getBloodAllianceCount()));
				break;
			}
			case "Receive":
			{
				if (!isMyLord)
				{
					htmltext = "warehouse-no.html";
				}
				else if (clan.getBloodAllianceCount() == 0)
				{
					htmltext = "warehouse-05.html";
				}
				else
				{
					giveItems(player, BLOOD_ALLIANCE, clan.getBloodAllianceCount());
					clan.resetBloodAllianceCount();
					htmltext = "warehouse-06.html";
				}
				break;
			}
			case "Exchange":
			{
				if (!isMyLord)
				{
					htmltext = "warehouse-no.html";
				}
				else if (!hasQuestItems(player, BLOOD_ALLIANCE))
				{
					htmltext = "warehouse-08.html";
				}
				else
				{
					takeItems(player, BLOOD_ALLIANCE, 1);
					giveItems(player, BLOOD_OATH, 30);
					htmltext = "warehouse-07.html";
				}
				break;
			}
			default:
			{
				htmltext = null;
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the initial interaction when a {@code Player} speaks to an {@code Npc}.<br>
	 * This method determines which dialogue file to display first.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the name of the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "warehouse-01.html";
	}
	
	/**
	 * This is the entry point for the {@code CastleWarehouse} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CastleWarehouse();
	}
}
