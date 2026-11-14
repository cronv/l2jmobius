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
package ai.areas.Gracia.KeucereusAllianceBase.Lekon;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.AirShipManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Defines the artificial intelligence behavior for the {@code Lekon} NPC.<br>
 * This class handles specific actions and logic for entities within the {@code Lekon} area.
 * @author St3eT
 */
public class Lekon extends Script
{
	// NPCs
	private static final int LEKON = 32557;
	
	// Items
	private static final int LICENCE = 13559; // Airship Summon License
	private static final int STONE = 13277; // Energy Star Stone
	
	// Misc
	private static final int MIN_CLAN_LV = 5;
	private static final int STONE_COUNT = 10;
	
	/**
	 * Initializes the {@code Lekon} script.<br>
	 * Registers the necessary IDs for NPCs and events.<br>
	 * Sets up the starting NPC for this script.
	 */
	public Lekon()
	{
		addFirstTalkId(LEKON);
		addTalkId(LEKON);
		addStartNpc(LEKON);
	}
	
	/**
	 * Handles specific events for the Lekon NPC.<br>
	 * It processes logic for license acquisition and quest items.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The name of the HTML file to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32557-01.html":
			{
				htmltext = event;
				break;
			}
			case "licence":
			{
				final Clan clan = player.getClan();
				if ((clan == null) || !player.isClanLeader() || (clan.getLevel() < MIN_CLAN_LV))
				{
					htmltext = "32557-02.html";
				}
				else if (hasAtLeastOneQuestItem(player, LICENCE))
				{
					htmltext = "32557-04.html";
				}
				else if (AirShipManager.getInstance().hasAirShipLicense(clan.getId()))
				{
					player.sendPacket(SystemMessageId.THE_AIRSHIP_SUMMON_LICENSE_HAS_ALREADY_BEEN_ACQUIRED);
				}
				else if (getQuestItemsCount(player, STONE) >= STONE_COUNT)
				{
					takeItems(player, STONE, STONE_COUNT);
					giveItems(player, LICENCE, 1);
				}
				else
				{
					htmltext = "32557-03.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which dialogue file to display based on the NPC's ID.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Lekon} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Lekon();
	}
}
