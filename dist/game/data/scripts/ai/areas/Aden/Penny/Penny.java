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
package ai.areas.Aden.Penny;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * This class defines the artificial intelligence behavior for NPCs in the {@code Aden} faction.<br>
 * It handles specific actions and logic for characters located within the {@code Penny} area.
 * @author NightBR
 * @date 2019-03-27
 */
public class Penny extends Script
{
	// NPC
	private static final int PENNY = 34413;
	
	// Misc
	private static final String[] RANDOM_VOICE =
	{
		"Npcdialog1.peny_ep50_greeting_7",
		"Npcdialog1.peny_ep50_greeting_8",
		"Npcdialog1.peny_ep50_greeting_9"
	};
	
	/**
	 * Private constructor for the {@code Penny} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Penny()
	{
		addStartNpc(PENNY);
		addTalkId(PENNY);
		addFirstTalkId(PENNY);
	}
	
	/**
	 * Handles specific events for the {@code Penny} NPC.<br>
	 * This method processes interactions like medals or reputation changes.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A {@code String} response or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "medal":
			{
				// Take medal / Give rep?
				return null;
			}
			case "grand_medal":
			{
				// Take medal / Give rep?
				return null;
			}
		}
		
		return null;
	}
	
	/**
	 * Handles the interaction when a player talks to this {@code Npc} for the first time.<br>
	 * It plays a random voice sound and returns the dialogue file.
	 * @param npc The {@code Npc} instance that is being interacted with.
	 * @param player The {@code Player} instance who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		player.sendPacket(new PlaySound(3, RANDOM_VOICE[getRandom(3)], 0, 0, 0, 0, 0));
		return "34413.html";
	}
	
	/**
	 * This is the main entry point for the {@code Penny} class.<br>
	 * It initializes a new instance of the {@code Penny} object.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Penny();
	}
}
