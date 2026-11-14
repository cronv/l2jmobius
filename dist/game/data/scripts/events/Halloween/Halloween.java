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
package events.Halloween;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;

/**
 * This class manages the logic for the seasonal Halloween event.<br>
 * It handles all specific mechanics and behaviors associated with this long-term event.
 * @author Mobius
 */
public class Halloween extends LongTimeEvent
{
	// NPC
	private static final int PUMPKIN_GHOST = 13135;
	
	/**
	 * Private constructor for the {@code Halloween} class.<br>
	 * This constructor initializes the event settings.<br>
	 * It registers the required NPCs and talk IDs.
	 */
	private Halloween()
	{
		addStartNpc(PUMPKIN_GHOST);
		addFirstTalkId(PUMPKIN_GHOST);
		addTalkId(PUMPKIN_GHOST);
	}
	
	/**
	 * Handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method determines which dialogue file to display.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "13135-01.htm";
	}
	
	/**
	 * This is the entry point for the {@code Halloween} class.<br>
	 * It initializes a new instance of the event.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Halloween();
	}
}
