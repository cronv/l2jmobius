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
package ai.areas.Heine;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for a {@code WarpGate}.<br>
 * It manages how NPCs interact with and process teleportation logic within this area.
 * @author Gigi
 */
public class WrapGate extends Script
{
	// NPC
	private static final int WRAP_GATE = 33900;
	
	// Location
	private static final Location TELEPORT_LOC = new Location(-28575, 255984, -2195);
	
	/**
	 * Private constructor for the {@code WrapGate} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private WrapGate()
	{
		addStartNpc(WRAP_GATE);
		addFirstTalkId(WRAP_GATE);
	}
	
	/**
	 * Handles specific events triggered by the {@code WrapGate} script.<br>
	 * It checks for the {@code enter_hellbound} event to trigger a movie and teleport the player.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance that triggered the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return Always returns {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if ("enter_hellbound".equals(event))
		{
			// final QuestState qs = player.getQuestState(Q10455_ElikiasLetter.class.getSimpleName());
			// if ((qs != null) && qs.isCond(1))
			// {
			// playMovie(player, Movie.SC_HELLBOUND);
			// }
			
			player.teleToLocation(TELEPORT_LOC);
		}
		
		return null;
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method returns the name of the HTML file to display.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the filename of the dialogue.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "33900.html";
	}
	
	/**
	 * This is the main entry point for the {@code WrapGate} class.<br>
	 * It initializes a new instance of the {@code WrapGate} object.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new WrapGate();
	}
}
