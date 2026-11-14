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
package ai.areas.Aden.Tarti;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;

/**
 * This class defines the artificial intelligence behavior for NPCs in the {@code Tarti} area.<br>
 * It handles specific actions and logic for entities within this region.
 * @author Gigi
 * @date 2019-08-17 - [22:44:02]
 */
public class Tarti extends Script
{
	// NPC
	private static final int TARTI = 34360;
	
	// Misc
	private static final String[] TARTI_VOICE =
	{
		"Npcdialog1.tarti_ep50_greeting_8",
		"Npcdialog1.tarti_ep50_greeting_9",
		"Npcdialog1.tarti_ep50_greeting_10"
	};
	
	/**
	 * Private constructor for the {@code Tarti} class.<br>
	 * This constructor initializes the NPC data and talk IDs.
	 */
	private Tarti()
	{
		addStartNpc(TARTI);
		addFirstTalkId(TARTI);
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It plays a random voice sound for the NPC.<br>
	 * It returns the path to the dialogue file.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the name of the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		player.sendPacket(new PlaySound(3, TARTI_VOICE[getRandom(3)], 0, 0, 0, 0, 0));
		return "34360.html";
	}
	
	/**
	 * This is the entry point for the {@code Tarti} class.<br>
	 * It initializes a new instance of the {@code Tarti} object.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Tarti();
	}
}
