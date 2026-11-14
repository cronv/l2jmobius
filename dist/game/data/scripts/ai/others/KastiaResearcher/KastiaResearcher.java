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
package ai.others.KastiaResearcher;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Handles the logic for the {@code KastiaResearcher} NPC script.<br>
 * This class manages interactions and behaviors specific to this entity.
 * @author Mobius
 */
public class KastiaResearcher extends Script
{
	// NPC
	private static final int RESEARCHER = 34566;
	
	/**
	 * Initializes the {@code KastiaResearcher} script.<br>
	 * This constructor registers the required NPC IDs for events and dialogue.
	 */
	public KastiaResearcher()
	{
		addStartNpc(RESEARCHER);
		addTalkId(RESEARCHER);
		addFirstTalkId(RESEARCHER);
	}
	
	/**
	 * Handles specific events triggered by the {@code KastiaResearcher} script.<br>
	 * This method processes interactions between an {@code Npc} and a {@code Player}.<br>
	 * It checks for the {@code "exit"} event to close instances.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return A {@code null} value or a string response.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("exit"))
		{
			final Instance world = player.getInstanceWorld();
			if (world != null)
			{
				world.finishInstance(0);
			}
		}
		
		return null;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which HTML file to display based on the NPC's ID.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the interaction.
	 * @return A {@code String} representing the path to the HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code KastiaResearcher} application.<br>
	 * It initializes a new instance of the {@code KastiaResearcher} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new KastiaResearcher();
	}
}
