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
package events.LoversJubilee;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExBrBroadcastEventState;

/**
 * Handles the logic for the {@code LoversJubilee} long-term event.<br>
 * This class manages the progression and state of the jubilee activities.
 */
public class LoversJubilee extends LongTimeEvent
{
	// NPC
	private static final int ROSALIA = 4305;
	
	// Item
	private static final int ONE_RED_ROSE_BUD = 20905;
	private static final int ONE_BLUE_ROSE_BUD = 20906;
	private static final int ONE_WHILE_ROSE_BUD = 20907;
	private static final int DESELOPH_ROSE_NECKLACE = 20908;
	private static final int HYUM_ROSE_NECKLACE = 20909;
	private static final int REKANG_ROSE_NECKLACE = 20910;
	private static final int LILIAS_ROSE_NECKLACE = 20911;
	private static final int LAPHAM_ROSE_NECKLACE = 20912;
	private static final int MAFUM_ROSE_NECKLACE = 20913;
	private static final int IMPROVED_ROSE_SPIRIT_EXCHANGE_TICKET = 20914;
	private static final int IMPROVED_DESELOPH_ROSE_NECKLACE = 20915;
	private static final int IMPROVED_HYUM_ROSE_NECKLACE = 20916;
	private static final int IMPROVED_REKANG_ROSE_NECKLACE = 20917;
	private static final int IMPROVED_LILIAS_ROSE_NECKLACE = 20918;
	private static final int IMPROVED_LAPHAM_ROSE_NECKLACE = 20919;
	private static final int IMPROVED_MAFUM_ROSE_NECKLACE = 20920;
	private static final int SPIRIT_TEST_REPORT = 20921;
	
	// Misc
	private static final int ONE_ROSE_PRICE = 500;
	private static final int TEN_ROSES_PRICE = 5000;
	
	/**
	 * Initializes the {@code LoversJubilee} event.<br>
	 * Registers the starting NPC and required talk IDs.
	 */
	private LoversJubilee()
	{
		addStartNpc(ROSALIA);
		addFirstTalkId(ROSALIA);
		addTalkId(ROSALIA);
	}
	
	/**
	 * Handles specific event logic for the Lovers Jubilee quest.<br>
	 * It processes item rewards and checks player inventory based on the provided event ID.
	 * @param event The unique identifier for the quest action to perform.
	 * @param npc The {@code Npc} interacting with the player.
	 * @param player The {@code Player} performing the action.
	 * @return The filename of the HTML text to display to the player.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmtext = event;
		switch (event)
		{
			case "50020_1":
			{
				htmtext = ownsAtLeastOneItem(player, SPIRIT_TEST_REPORT) ? "4305-010.htm" : "4305-002.htm";
				break;
			}
			case "50020_2":
			case "50020_3":
			case "50020_4":
			{
				if (getQuestItemsCount(player, Inventory.ADENA_ID) < ONE_ROSE_PRICE)
				{
					htmtext = "4305-024.htm";
				}
				else
				{
					takeItems(player, Inventory.ADENA_ID, ONE_ROSE_PRICE);
					switch (event)
					{
						case "50020_2":
						{
							giveItems(player, ONE_RED_ROSE_BUD, 1);
							break;
						}
						case "50020_3":
						{
							giveItems(player, ONE_BLUE_ROSE_BUD, 1);
							break;
						}
						case "50020_4":
						{
							giveItems(player, ONE_WHILE_ROSE_BUD, 1);
							break;
						}
					}
					
					htmtext = "4305-023.htm";
				}
				break;
			}
			case "50020_5":
			case "50020_6":
			case "50020_7":
			{
				if (getQuestItemsCount(player, Inventory.ADENA_ID) < TEN_ROSES_PRICE)
				{
					htmtext = "4305-024.htm";
				}
				else
				{
					takeItems(player, Inventory.ADENA_ID, TEN_ROSES_PRICE);
					switch (event)
					{
						case "50020_5":
						{
							giveItems(player, ONE_RED_ROSE_BUD, 10);
							break;
						}
						case "50020_6":
						{
							giveItems(player, ONE_BLUE_ROSE_BUD, 10);
							break;
						}
						case "50020_7":
						{
							giveItems(player, ONE_WHILE_ROSE_BUD, 10);
							break;
						}
					}
					
					htmtext = "4305-023.htm";
				}
				break;
			}
			case "50020_8":
			{
				if (hasQuestItems(player, IMPROVED_ROSE_SPIRIT_EXCHANGE_TICKET))
				{
					htmtext = "4305-007.htm";
				}
				else
				{
					htmtext = "4305-008.htm";
				}
				break;
			}
			case "50020_9":
			case "50020_10":
			case "50020_11":
			case "50020_12":
			case "50020_13":
			case "50020_14":
			{
				giveItems(player, SPIRIT_TEST_REPORT, 1);
				switch (event)
				{
					case "50020_9":
					{
						giveItems(player, DESELOPH_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_10":
					{
						giveItems(player, HYUM_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_11":
					{
						giveItems(player, REKANG_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_12":
					{
						giveItems(player, LILIAS_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_13":
					{
						giveItems(player, LAPHAM_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_14":
					{
						giveItems(player, MAFUM_ROSE_NECKLACE, 1);
						break;
					}
				}
				
				htmtext = "4305-025.htm";
				break;
			}
			case "50020_15":
			case "50020_16":
			case "50020_17":
			case "50020_18":
			case "50020_19":
			case "50020_20":
			{
				takeItems(player, IMPROVED_ROSE_SPIRIT_EXCHANGE_TICKET, 1);
				switch (event)
				{
					case "50020_15":
					{
						giveItems(player, IMPROVED_DESELOPH_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_16":
					{
						giveItems(player, IMPROVED_HYUM_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_17":
					{
						giveItems(player, IMPROVED_REKANG_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_18":
					{
						giveItems(player, IMPROVED_LILIAS_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_19":
					{
						giveItems(player, IMPROVED_LAPHAM_ROSE_NECKLACE, 1);
						break;
					}
					case "50020_20":
					{
						giveItems(player, IMPROVED_MAFUM_ROSE_NECKLACE, 1);
						break;
					}
				}
				
				htmtext = "4305-026.htm";
				break;
			}
		}
		
		return htmtext;
	}
	
	/**
	 * Handles the logic for when a player talks to an {@code Npc} for the first time.<br>
	 * This method returns the filename of the dialogue to display.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "4305-001.htm";
	}
	
	/**
	 * This method handles the logic when a player logs into the game.<br>
	 * It checks if the event is currently active.<br>
	 * If active, it sends a broadcast packet to the player.
	 * @param event The {@code OnPlayerLogin} event object containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (isEventPeriod())
		{
			event.getPlayer().sendPacket(new ExBrBroadcastEventState(ExBrBroadcastEventState.LOVERS_JUBILEE, 1));
		}
	}
	
	/**
	 * This is the entry point for the {@code LoversJubilee} class.<br>
	 * It initializes a new instance of the event.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new LoversJubilee();
	}
}
