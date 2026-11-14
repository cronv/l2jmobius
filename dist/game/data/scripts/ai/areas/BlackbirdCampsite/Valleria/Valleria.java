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
package ai.areas.BlackbirdCampsite.Valleria;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Faction;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for NPCs in the {@code Valleria} area.<br>
 * It handles specific actions and logic for entities within this location.
 * @author CostyKiller
 */
public class Valleria extends Script
{
	// NPC
	private static final int VALLERIA = 34435;
	
	// Items
	private static final int MEDAL = 48516; // Medal of Honor
	private static final int GRAND_MEDAL = 48517; // Grand Medal of Honor
	
	// Misc
	private static final int MEDAL_POINTS = 100;
	private static final int GRAND_MEDAL_POINTS = 1000;
	private static final int MIN_LEVEL = 99;
	
	/**
	 * Private constructor for the {@code Valleria} class.<br>
	 * This constructor initializes the NPC IDs and script events.<br>
	 * It prevents other classes from creating new instances of this class.
	 */
	private Valleria()
	{
		addStartNpc(VALLERIA);
		addFirstTalkId(VALLERIA);
		addTalkId(VALLERIA);
	}
	
	/**
	 * Handles various script events for the Valleria NPC.<br>
	 * It processes medal exchanges and returns the appropriate HTML response.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The name of the HTML file to display as a result.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34435.html":
			case "34435-01.html":
			case "34435-02.html":
			case "34435-03.html":
			case "34435-04.html":
			{
				htmltext = event;
				break;
			}
			case "medal_for_blackbird":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, MEDAL))
					{
						takeItems(player, 1, MEDAL);
						player.addFactionPoints(Faction.BLACKBIRD_CLAN, MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "medal_for_mother":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, MEDAL))
					{
						takeItems(player, 1, MEDAL);
						player.addFactionPoints(Faction.MOTHER_TREE_GUARDIANS, MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "medal_for_giant":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, MEDAL))
					{
						takeItems(player, 1, MEDAL);
						player.addFactionPoints(Faction.GIANT_TRACKERS, MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "medal_for_unworldly":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, MEDAL))
					{
						takeItems(player, 1, MEDAL);
						player.addFactionPoints(Faction.UNWORLDLY_VISITORS, MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "medal_for_kingdom":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, MEDAL))
					{
						takeItems(player, 1, MEDAL);
						player.addFactionPoints(Faction.KINGDOM_ROYAL_GUARDS, MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "grand_medal_for_blackbird":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, GRAND_MEDAL))
					{
						takeItems(player, 1, GRAND_MEDAL);
						player.addFactionPoints(Faction.BLACKBIRD_CLAN, GRAND_MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "grand_medal_for_mother":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, GRAND_MEDAL))
					{
						takeItems(player, 1, GRAND_MEDAL);
						player.addFactionPoints(Faction.MOTHER_TREE_GUARDIANS, GRAND_MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "grand_medal_for_giant":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, GRAND_MEDAL))
					{
						takeItems(player, 1, GRAND_MEDAL);
						player.addFactionPoints(Faction.GIANT_TRACKERS, GRAND_MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "grand_medal_for_unworldly":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, GRAND_MEDAL))
					{
						takeItems(player, 1, GRAND_MEDAL);
						player.addFactionPoints(Faction.UNWORLDLY_VISITORS, GRAND_MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
			case "grand_medal_for_kingdom":
			{
				if (player.getLevel() < MIN_LEVEL)
				{
					htmltext = "no_level.html";
				}
				else
				{
					if (hasAtLeastOneQuestItem(player, GRAND_MEDAL))
					{
						takeItems(player, 1, GRAND_MEDAL);
						player.addFactionPoints(Faction.KINGDOM_ROYAL_GUARDS, GRAND_MEDAL_POINTS);
						htmltext = "success.html";
					}
					else
					{
						htmltext = "no_medal.html";
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It returns the name of the HTML file to display.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the filename of the dialogue to show.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34435.html";
	}
	
	/**
	 * This is the main entry point for the {@code Valleria} class.<br>
	 * It initializes a new instance of the {@code Valleria} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Valleria();
	}
}
