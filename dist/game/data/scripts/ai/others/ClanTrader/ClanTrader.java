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
package ai.others.ClanTrader;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles the artificial intelligence for the {@code ClanTrader} NPC.<br>
 * It manages the logic for trading interactions between players and the trader.
 * @author St3eT
 */
public class ClanTrader extends Script
{
	// NPCs
	private static final int[] CLAN_TRADER =
	{
		32024, // Mulia
		32025, // Ilia
	};
	
	// Items
	private static final int BLOOD_ALLIANCE = 9911; // Blood Alliance
	private static final int BLOOD_ALLIANCE_COUNT = 1; // Blood Alliance Count
	private static final int BLOOD_OATH = 9910; // Blood Oath
	private static final int BLOOD_OATH_COUNT = 10; // Blood Oath Count
	private static final int KNIGHTS_EPAULETTE = 9912; // Knight's Epaulette
	private static final int KNIGHTS_EPAULETTE_COUNT = 100; // Knight's Epaulette Count
	
	/**
	 * Private constructor for the {@code ClanTrader} class.<br>
	 * This constructor initializes the NPC IDs and talk IDs.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private ClanTrader()
	{
		addStartNpc(CLAN_TRADER);
		addTalkId(CLAN_TRADER);
		addFirstTalkId(CLAN_TRADER);
	}
	
	/**
	 * This method handles the logic for granting clan reputation.<br>
	 * It checks if a {@code Player} has enough items to complete a task.<br>
	 * If successful, it removes the items and adds points to the clan.<br>
	 * It also sends a system message to the player.
	 * @param npc The {@code Npc} interacting with the player.
	 * @param player The {@code Player} who is performing the action.
	 * @param count The amount of reputation points to award.
	 * @param itemId The unique ID of the item required.
	 * @param itemCount The quantity of items needed for the task.
	 * @return A string representing the HTML file path to display next.
	 */
	private String giveReputation(Npc npc, Player player, int count, int itemId, int itemCount)
	{
		if (getQuestItemsCount(player, itemId) >= itemCount)
		{
			takeItems(player, itemId, itemCount);
			player.getClan().addReputationScore(count);
			
			final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_REPUTATION_POINTS_S1);
			sm.addInt(count);
			player.sendPacket(sm);
			return npc.getId() + "-04.html";
		}
		
		return npc.getId() + "-03.html";
	}
	
	/**
	 * Handles specific events for the {@code ClanTrader} script.<br>
	 * It processes requests for reputation information and item exchanges.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A string containing the HTML text to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32024.html":
			case "32024-02.html":
			case "32025.html":
			case "32025-02.html":
			{
				htmltext = event;
				break;
			}
			case "repinfo":
			{
				final int npcId = npc.getId();
				htmltext = (player.getClan().getLevel() > 4) ? npcId + "-02.html" : npcId + "-05.html";
				break;
			}
			case "exchange-ba":
			{
				htmltext = giveReputation(npc, player, FeatureConfig.BLOODALLIANCE_POINTS, BLOOD_ALLIANCE, BLOOD_ALLIANCE_COUNT);
				break;
			}
			case "exchange-bo":
			{
				htmltext = giveReputation(npc, player, FeatureConfig.BLOODOATH_POINTS, BLOOD_OATH, BLOOD_OATH_COUNT);
				break;
			}
			case "exchange-ke":
			{
				htmltext = giveReputation(npc, player, FeatureConfig.KNIGHTSEPAULETTE_POINTS, KNIGHTS_EPAULETTE, KNIGHTS_EPAULETTE_COUNT);
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the dialogue file for a player's first interaction with an {@code Npc}.<br>
	 * It checks if the {@code Player} belongs to a clan and their specific permissions.<br>
	 * The method returns the filename based on the {@code Npc} ID and the player's status.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the path to the dialogue HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code ClanTrader} application.<br>
	 * It initializes a new instance of the {@code ClanTrader} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new ClanTrader();
	}
}
