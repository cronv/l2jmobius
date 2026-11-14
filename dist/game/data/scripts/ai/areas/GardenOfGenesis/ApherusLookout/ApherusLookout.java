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
package ai.areas.GardenOfGenesis.ApherusLookout;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * This class handles the script logic for the {@code ApherusLookout} area.<br>
 * It manages specific NPC interactions and world events within this location.
 * @author Gigi
 */
public class ApherusLookout extends Script
{
	private static final int APHERUS_LOOKOUT = 22964;
	private static final int APHERUS_PACKAGE = 19001;
	private static final int APHERUS_PACKAGE1 = 19002;
	private static final int APHERUS_PACKAGE2 = 19003;
	private static final int APERUS_KEY = 17373;
	
	/**
	 * Initializes the {@code ApherusLookout} script.<br>
	 * Registers the required NPC IDs for events and interactions.
	 */
	public ApherusLookout()
	{
		addKillId(APHERUS_LOOKOUT);
		addFirstTalkId(APHERUS_PACKAGE, APHERUS_PACKAGE1, APHERUS_PACKAGE2);
	}
	
	/**
	 * Handles specific events triggered by the {@code ApherusLookout} script.<br>
	 * This method processes logic such as opening bags and giving items.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A {@code String} containing HTML text, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = null;
		if (event.equals("open_bag"))
		{
			if (getRandom(100) < 7)
			{
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.MOVED_TO_APHERUS_DIMENSION, ExShowScreenMessage.TOP_CENTER, 3000, true));
				giveItems(player, APERUS_KEY, 1);
			}
			
			npc.deleteMe();
		}
		
		return htmltext;
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the logic for spawning new items after a kill.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who performed the kill.
	 * @param isPet A boolean indicating if the kill was made by a pet.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		final int npcX = npc.getX();
		final int npcY = npc.getY();
		final int npcZ = npc.getZ();
		final Npc aPackage = addSpawn(APHERUS_PACKAGE, npcX, npcY, npcZ, 0, true, 120000, false);
		aPackage.setImmobilized(true);
		final Npc bPackage = addSpawn(APHERUS_PACKAGE1, npcX, npcY, npcZ, 0, true, 120000, false);
		bPackage.setImmobilized(true);
		final Npc cPackage = addSpawn(APHERUS_PACKAGE2, npcX, npcY, npcZ, 0, true, 120000, false);
		cPackage.setImmobilized(true);
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method determines which dialogue screen to show first.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A string representing the ID of the HTML file to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "19001.html";
	}
	
	/**
	 * This is the entry point for the {@code ApherusLookout} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new ApherusLookout();
	}
}
