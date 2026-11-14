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
package ai.areas.Conquest.Flowers;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.conquest.OnConquestFlowerCollect;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the AI logic for Conquest Flowers within the Fire Area.<br>
 * This script manages requirements, rewards, and specific spawning rules for different flower types.<br>
 * It processes item drops and point gains when a player successfully collects a flower.
 * @author CostyKiller
 */
public class Flowers extends Script
{
	// NPCs
	private static final int FIRE_FLOWER = 34655;
	private static final int LIFE_FLOWER = 34656;
	private static final int POWER_FLOWER = 34657;
	
	// Gather flower requirements
	private static final int REQUIRED_HP = 1000;
	private static final int REQUIRED_SP = 500000;
	
	// Rewards
	private static final int PERSONAL_POINTS_AMOUNT = 267;
	private static final int SERVER_POINTS_AMOUNT = 500;
	private static final int SEED_OF_FIRE = 82616;
	private static final int SEALED_GHOST_SOUL_ORB = 82610;
	private static final int SEALED_FIRE_SOURCE = 82658;
	private static final int SACRED_FIRE_SUMMON_SCROLL = 82614;
	private static final int EIGIS_ARMOR_FRAGMENT = 82083;
	private static final int DIVINE_FIRE = 82615;
	private static final int DIVINE_FIRE_CHANCE = 15; // 15%
	
	/**
	 * Private constructor for the {@code Flowers} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Flowers()
	{
		addStartNpc(FIRE_FLOWER, LIFE_FLOWER, POWER_FLOWER);
		addFirstTalkId(FIRE_FLOWER, LIFE_FLOWER, POWER_FLOWER);
		addTalkId(FIRE_FLOWER, LIFE_FLOWER, POWER_FLOWER);
	}
	
	/**
	 * Handles events for the Conquest Flowers script.<br>
	 * This method processes flower collection and displays specific HTML content.<br>
	 * It checks player requirements, deducts resources, and distributes rewards.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the flower.
	 * @return The HTML text to be displayed or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34655.html":
			case "34656.html":
			case "34657.html":
			case "34655-01.html":
			case "34656-01.html":
			case "34657-01.html":
			{
				htmltext = event;
				break;
			}
			case "collectFlower":
			{
				final double currentHp = player.getCurrentHp();
				final long sp = player.getSp();
				if ((currentHp < REQUIRED_HP) && (sp < REQUIRED_SP))
				{
					player.sendMessage("You can't collect this flower, the requirements are not met.");
				}
				else
				{
					player.setCurrentHp(currentHp - REQUIRED_HP);
					player.setSp(sp - REQUIRED_SP);
					
					// Messages
					SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
					sm.addLong(REQUIRED_SP);
					SystemMessage sm2 = new SystemMessage(SystemMessageId.C1_HAS_DRAINED_YOU_OF_S2_HP);
					sm2.addString(npc.getName());
					sm2.addLong(REQUIRED_HP);
					player.sendPacket(sm);
					player.sendPacket(sm2);
					npc.onDecay();
					
					final int npcId = npc.getId();
					htmltext = npcId + "-01.html";
					
					// Notify to scripts.
					if (EventDispatcher.getInstance().hasListener(EventType.ON_CONQUEST_FLOWER_COLLECT))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnConquestFlowerCollect(player, npcId));
					}
					
					if (player.isInventoryUnder90(false))
					{
						final int random = getRandom(100);
						
						// Reward only from Fire Flower
						if ((random < DIVINE_FIRE_CHANCE) && (npcId == FIRE_FLOWER))
						{
							giveItems(player, DIVINE_FIRE, 1);
						}
						
						// Rewards from all flowers
						if (random < 5) // 5% chance
						{
							giveItems(player, SACRED_FIRE_SUMMON_SCROLL, 1);
						}
						else if (random < 10) // 10% chance
						{
							giveItems(player, SEALED_GHOST_SOUL_ORB, 1);
						}
						else if (random < 15) // 15% chance
						{
							giveItems(player, SEALED_FIRE_SOURCE, 1);
							// player.sendPacket(SystemMessageId.YOU_HAVE_RECEIVED_FIRE_SOURCE_POINTS);
						}
						else if (random < 20) // 20% chance
						{
							giveItems(player, SEED_OF_FIRE, 1);
						}
						else if (random < 25) // 25% chance
						{
							giveItems(player, EIGIS_ARMOR_FRAGMENT, 1);
						}
						else if (random < 30) // 30% chance
						{
							GlobalVariablesManager.getInstance().set("CONQUEST_SERVER_POINTS", GlobalVariablesManager.getInstance().getLong("CONQUEST_SERVER_POINTS", 0) + SERVER_POINTS_AMOUNT);
							player.sendPacket(SystemMessageId.YOU_HAVE_RECEIVED_SERVER_CONQUEST_POINTS);
						}
						else
						{
							final PlayerVariables vars = player.getVariables();
							vars.set(PlayerVariables.CONQUEST_PERSONAL_POINTS, vars.getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS, 0) + PERSONAL_POINTS_AMOUNT);
							player.sendPacket(SystemMessageId.YOU_HAVE_RECEIVED_PERSONAL_CONQUEST_POINTS);
						}
					}
					else
					{
						player.sendPacket(SystemMessageId.YOUR_INVENTORY_S_WEIGHT_SLOT_LIMIT_HAS_BEEN_EXCEEDED_SO_YOU_CANNOT_RECEIVE_THE_REWARD_PLEASE_FREE_UP_SOME_SPACE_AND_TRY_AGAIN);
					}
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It returns the filename for the dialogue to be displayed.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Flowers} script.<br>
	 * It initializes a new instance of the {@code Flowers} class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Flowers();
	}
}
