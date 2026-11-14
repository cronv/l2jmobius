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
package events.EveTheFortuneTeller;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.enums.LuckyGameBettingType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.network.serverpackets.luckygame.ExStartLuckyGame;

/**
 * Handles the logic for the "Eve the Fortune Teller Returns" event.<br>
 * This class manages the interaction and mechanics associated with this specific long-term game event.
 * @author Mobius
 */
public class EveTheFortuneTeller extends LongTimeEvent
{
	// NPCs
	private static final int EVE = 8542;
	private static final int JAYCE = 8540;
	
	// Items
	private static final int FORTUNE_READING_TICKET = 23767;
	private static final int LUXURY_FORTUNE_READING_TICKET = 23768;
	
	// Misc
	private static final NpcStringId[] JAYCE_TEXT =
	{
		NpcStringId.I_LOOK_WEST,
		NpcStringId.EVE_WILL_BRING_YOU_GREAT_FORTUNE,
		NpcStringId.YOU_WILL_ONE_DAY_ASK_ME_FOR_GUIDANCE_IN_YOUR_PATH
	};
	
	/**
	 * Initializes the {@code EveTheFortuneTeller} event.<br>
	 * This constructor sets up the required NPCs and talk IDs.<br>
	 * It registers {@code EVE} as a primary NPC for the event.
	 */
	private EveTheFortuneTeller()
	{
		addStartNpc(EVE);
		addFirstTalkId(EVE);
		addTalkId(EVE);
		addSpawnId(JAYCE);
	}
	
	/**
	 * Handles specific events triggered by the {@code EveTheFortuneTeller} event.<br>
	 * It processes HTML text, lucky game starts, and NPC shouts.
	 * @param event The unique identifier for the event to be processed.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the event.
	 * @return The HTML text string if applicable, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "8542.htm":
			case "8542-1.htm":
			{
				htmltext = event;
				break;
			}
			case "FortuneReadingGame":
			{
				if (!player.isInventoryUnder80(false))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SPACE_IN_THE_INVENTORY_UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
					break;
				}
				
				player.sendPacket(new ExStartLuckyGame(LuckyGameBettingType.NORMAL, player.getInventory().getInventoryItemCount(FORTUNE_READING_TICKET, -1)));
				break;
			}
			case "LuxuryFortuneReadingGame":
			{
				if (!player.isInventoryUnder80(false))
				{
					player.sendPacket(SystemMessageId.NOT_ENOUGH_SPACE_IN_THE_INVENTORY_UNABLE_TO_PROCESS_THIS_REQUEST_UNTIL_YOUR_INVENTORY_S_WEIGHT_AND_SLOT_COUNT_ARE_LESS_THAN_80_PERCENT_OF_CAPACITY);
					break;
				}
				
				player.sendPacket(new ExStartLuckyGame(LuckyGameBettingType.PREMIUM, player.getInventory().getInventoryItemCount(LUXURY_FORTUNE_READING_TICKET, -1)));
				break;
			}
			case "JAYCE_SHOUT":
			{
				World.broadcastToVisiblePlayersInRange(npc, new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), getRandomEntry(JAYCE_TEXT)), 1000);
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the logic when a player talks to an {@code Npc} for the first time.<br>
	 * This method determines which dialogue file should be displayed.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "8542.htm";
	}
	
	/**
	 * This method is called when the {@code Npc} spawns.<br>
	 * It starts a quest timer for the NPC.
	 * @param npc The {@code Npc} instance that was spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("JAYCE_SHOUT", 45000, npc, null, true);
	}
	
	/**
	 * This is the main entry point for the {@code EveTheFortuneTeller} class.<br>
	 * It initializes a new instance of the event handler.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new EveTheFortuneTeller();
	}
}
