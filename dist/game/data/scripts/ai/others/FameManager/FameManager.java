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
package ai.others.FameManager;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class handles the AI logic for the {@code FameManager}.<br>
 * It manages how fame-related mechanics are processed within the game world.
 * @author St3eT
 */
public class FameManager extends Script
{
	// Npc
	private static final int[] FAME_MANAGER =
	{
		36479, // Rapidus
		36480, // Scipio
	};
	
	// Misc
	private static final int MIN_LEVEL = 40;
	private static final int DECREASE_COST = 5000;
	private static final int REPUTATION_COST = 1000;
	private static final int MIN_CLAN_LEVEL = 5;
	private static final int CLASS_LEVEL = 2;
	
	/**
	 * Private constructor for the {@code FameManager} class.<br>
	 * This prevents other classes from creating new instances of this script.<br>
	 * It initializes the required NPC and talk IDs.
	 */
	private FameManager()
	{
		addStartNpc(FAME_MANAGER);
		addTalkId(FAME_MANAGER);
		addFirstTalkId(FAME_MANAGER);
	}
	
	/**
	 * Handles specific events for the Fame Manager NPC.<br>
	 * This method processes interactions like decreasing PK or gaining clan reputation.<br>
	 * It checks player requirements and returns the appropriate HTML file path.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The path to the HTML text to be displayed.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "36479.html":
			case "36479-02.html":
			case "36479-07.html":
			case "36480.html":
			case "36480-02.html":
			case "36480-07.html":
			{
				htmltext = event;
				break;
			}
			case "decreasePk":
			{
				final int pkKills = player.getPkKills();
				if (pkKills > 0)
				{
					final int fame = player.getFame();
					final int level = player.getLevel();
					if ((fame >= DECREASE_COST) && (level >= MIN_LEVEL) && (player.getPlayerClass().level() >= CLASS_LEVEL))
					{
						player.setFame(fame - DECREASE_COST);
						player.setPkKills(pkKills - 1);
						player.updateUserInfo();
						htmltext = npc.getId() + "-06.html";
					}
					else
					{
						htmltext = npc.getId() + "-01.html";
					}
				}
				else
				{
					htmltext = npc.getId() + "-05.html";
				}
				break;
			}
			case "clanRep":
			{
				final Clan clan = player.getClan();
				if ((clan != null) && (clan.getLevel() >= MIN_CLAN_LEVEL))
				{
					final int fame = player.getFame();
					final int level = player.getLevel();
					if ((fame >= REPUTATION_COST) && (level >= MIN_LEVEL) && (player.getPlayerClass().level() >= CLASS_LEVEL))
					{
						player.setFame(fame - REPUTATION_COST);
						clan.addReputationScore(50);
						player.updateUserInfo();
						player.sendPacket(SystemMessageId.YOU_VE_GAINED_50_CLAN_REPUTATION_POINTS);
						htmltext = npc.getId() + "-04.html";
					}
					else
					{
						htmltext = npc.getId() + "-01.html";
					}
				}
				else
				{
					htmltext = npc.getId() + "-03.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the correct dialogue file to show when a player talks to an {@code Npc} for the first time.<br>
	 * It checks if the {@code Player} meets specific level and class requirements.<br>
	 * If the requirements are met, it returns the standard dialogue.<br>
	 * Otherwise, it returns the introductory dialogue.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return ((player.getFame() > 0) && (player.getLevel() >= MIN_LEVEL) && (player.getPlayerClass().level() >= CLASS_LEVEL)) ? npc.getId() + ".html" : npc.getId() + "-01.html";
	}
	
	/**
	 * This is the main entry point for the {@code FameManager} class.<br>
	 * It initializes a new instance of the manager to start the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new FameManager();
	}
}
