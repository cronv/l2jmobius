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
package ai.areas.GainakUnderground.Lezonne;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class defines the AI behavior for NPCs located in the {@code Lezonne} area.<br>
 * It handles specific script logic and interactions for entities within this zone.
 * @author Notorion
 */
public class Lezonne extends Script
{
	// NPC
	private static final int LEZONNE = 33834;
	
	// Items
	private static final int ADENA = 57;
	
	// Skills
	private static final SkillHolder[] DONATE_BUFFS =
	{
		new SkillHolder(11517, 1), // Horn Melody
		new SkillHolder(11518, 1), // Drum Melody
		new SkillHolder(11519, 1), // Pipe Organ Melody
		new SkillHolder(11520, 1), // Guitar Melody
		new SkillHolder(30812, 1), // Fantasia Harmony
	};
	
	/**
	 * Private constructor for the {@code Lezonne} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Lezonne()
	{
		addStartNpc(LEZONNE);
		addTalkId(LEZONNE);
		addFirstTalkId(LEZONNE);
	}
	
	/**
	 * Handles specific events for the {@code Lezonne} script.<br>
	 * It processes interactions like viewing information or donating items.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A {@code String} representing the HTML file to display.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "index":
			{
				htmltext = npc.getId() + ".html";
				break;
			}
			
			case "donate_adena":
			{
				if (getQuestItemsCount(player, ADENA) >= 200000)
				{
					takeItems(player, ADENA, 200000);
					for (SkillHolder holder : DONATE_BUFFS)
					{
						SkillCaster.triggerCast(npc, player, holder.getSkill());
					}
				}
				else
				{
					htmltext = "noItems.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which HTML file to display based on the NPC's unique ID.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML file to be displayed.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Lezonne} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Lezonne();
	}
}
