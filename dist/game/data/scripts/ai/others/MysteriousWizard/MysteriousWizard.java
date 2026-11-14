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
package ai.others.MysteriousWizard;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code MysteriousWizard} NPC.<br>
 * It handles the specific actions and logic executed by this entity within the game world.
 * @author Mobius
 */
public class MysteriousWizard extends Script
{
	private static final int MYSTERIOUS_WIZARD = 31522;
	
	/**
	 * Private constructor for the {@code MysteriousWizard} class.<br>
	 * It initializes the script with the required first talk ID.<br>
	 * This constructor should not be called from outside the class.
	 */
	private MysteriousWizard()
	{
		addFirstTalkId(MYSTERIOUS_WIZARD);
		addStartNpc(MYSTERIOUS_WIZARD);
		addTalkId(MYSTERIOUS_WIZARD);
	}
	
	/**
	 * Determines the correct dialogue file to display when a player talks to the NPC for the first time.<br>
	 * It checks if the player is in the Fortress of the Dead instance.<br>
	 * It also checks the current progress of the {@code Q10751_WindsOfFateEncounters} quest.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return The name of the HTML file to be displayed as a {@code String}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			final int templateId = world.getTemplateId();
			if (templateId == 254) // Fortress of the Dead
			{
				return "31522-01.html";
			}
			else if (templateId == 255) // Chamber of Prophecies
			{
				return "31522.html";
			}
		}
		
		return null;
	}
	
	/**
	 * This is the entry point for the {@code MysteriousWizard} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new MysteriousWizard();
	}
}
