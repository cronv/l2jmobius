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
package events.BirthOfDraco;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;

/**
 * Handles the logic for the {@code BirthOfDraco} event.<br>
 * This class manages the progression and mechanics of the specific world event.
 * @URL http://www.lineage2.com/en/news/events/birth-of-draco.php
 * @author Mobius
 */
public class BirthOfDraco extends LongTimeEvent
{
	// NPCs
	private static final int ASLAN = 33687;
	private static final int LYN_DRACO = 33828;
	
	// Items
	private static final int SMALL_EGG = 34864;
	private static final int LARGE_EGG = 34865;
	private static final int SHATTERED_EGG = 34866;
	private static final int SHINY_GIFT = 34868;
	// private static final int AGATHION = 34869; // needs proper ExSendUIEvent
	
	/**
	 * Initializes the {@code BirthOfDraco} event.<br>
	 * Sets up the starting NPC and required dialogue IDs.
	 */
	private BirthOfDraco()
	{
		addStartNpc(ASLAN);
		addFirstTalkId(ASLAN);
		addTalkId(ASLAN);
	}
	
	/**
	 * Handles specific event logic for the Birth of Draco quest.<br>
	 * It processes item exchanges and spawns based on the provided {@code event} string.
	 * @param event The identifier for the current action to perform.
	 * @param npc The {@code Npc} involved in the interaction.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "33687-01.htm":
			case "33687-04.htm":
			case "33687-07.htm":
			case "33687-08.htm":
			case "33687-09.htm":
			case "howtohatch.htm": // custom info
			case "lyndraco.htm": // custom info
			case "rewards.htm": // custom info
			{
				htmltext = event;
				break;
			}
			case "giveFiftySmallEggs":
			{
				if (getQuestItemsCount(player, SMALL_EGG) >= 50)
				{
					takeItems(player, SMALL_EGG, 50);
					addSpawn(LYN_DRACO, player.getX(), player.getY(), player.getZ(), player.getHeading() + 32500, true, 180000);
				}
				else
				{
					htmltext = "33687-11.htm";
				}
				break;
			}
			case "getRandomReward":
			{
				if (getQuestItemsCount(player, SHATTERED_EGG) >= 50)
				{
					takeItems(player, SHATTERED_EGG, 50);
					if (getRandom(100) < 30)
					{
						giveItems(player, SHINY_GIFT, 1);
					}
					else if (getRandom(100) < 30)
					{
						giveItems(player, LARGE_EGG, 1);
					}
					else if (getRandom(100) < 30)
					{
						giveItems(player, SMALL_EGG, 1);
					}
					else
					{
						htmltext = "33687-05.htm";
					}
				}
				else
				{
					htmltext = "33687-06.htm";
				}
				break;
			}
			/*
			 * case "giveAgathion": { if (hasQuestItems(player, AGATHION)) { htmltext = "33687-03.htm"; } else { giveItems(player, AGATHION, 1); htmltext = "33687-02.htm"; } break; }
			 */
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which HTML file to display based on the NPC's ID.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the interaction.
	 * @return A {@code String} representing the path to the HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + "-01.htm";
	}
	
	/**
	 * This is the main entry point for the {@code BirthOfDraco} class.<br>
	 * It initializes a new instance of the event.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new BirthOfDraco();
	}
}
