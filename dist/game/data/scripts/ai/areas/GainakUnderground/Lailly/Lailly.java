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
package ai.areas.GainakUnderground.Lailly;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * This class defines the artificial intelligence behavior for the {@code Lailly} NPC.<br>
 * It handles the logic and interactions for this specific entity within its designated area.
 * @author Stayway
 */
public class Lailly extends Script
{
	// NPCs
	private static final int LAILLY = 34181;
	
	// Instances
	private static final int INSTANCE_TAUTI = 261;
	private static final int INSTANCE_KELBIM = 262;
	private static final int INSTANCE_FREYA = 263;
	
	/**
	 * Private constructor for the {@code Lailly} class.<br>
	 * This constructor initializes the required spawn and talk IDs.
	 */
	private Lailly()
	{
		addSpawnId(LAILLY);
		addFirstTalkId(LAILLY);
	}
	
	/**
	 * Handles specific events triggered by the {@code Lailly} script.<br>
	 * This method processes interactions like dialogue, spam text, and instance teleportation.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} entity involved in the event.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return A string representing the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34181.html":
			{
				htmltext = event;
				break;
			}
			case "spam_text":
			{
				npc.broadcastPacket(new NpcSay(npc.getObjectId(), ChatType.NPC_GENERAL, npc.getId(), NpcStringId.READY_TO_LISTEN_TO_A_STORY_COME_NOW));
				break;
			}
			case "okay":
			{
				final Instance instance = InstanceManager.getInstance().getPlayerInstance(player, false);
				if ((instance != null) && (instance.getEndTime() > System.currentTimeMillis()))
				{
					switch (instance.getTemplateId())
					{
						case INSTANCE_TAUTI:
						case INSTANCE_KELBIM:
						case INSTANCE_FREYA:
						{
							player.teleToLocation(instance.getEnterLocation(), instance);
							break;
						}
					}
					break;
				}
				
				htmltext = "34181-1.html";
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It initializes the quest timer for the NPC.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("spam_text", 180000, npc, null, true);
	}
	
	/**
	 * Handles the dialogue when a {@code Player} speaks to an {@code Npc} for the first time.<br>
	 * This method returns the identifier for the initial conversation script.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the ID of the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34181.html";
	}
	
	/**
	 * This is the entry point for the {@code Lailly} class.<br>
	 * It initializes a new instance of the {@code Lailly} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Lailly();
	}
}
