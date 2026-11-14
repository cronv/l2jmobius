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
package events.TrainingWithDandy;

import java.util.Calendar;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Handles the logic for the "Training with Dandy" event.<br>
 * This class manages the progression and mechanics of the training session.
 * @author Gigi
 * @date 2019-08-27 - [17:41:16]
 */
public class TrainingWithDandy extends LongTimeEvent
{
	// NPC
	private static final int DANDY = 33894;
	
	// Skill
	private static final SkillHolder DANDY_CH = new SkillHolder(17186, 1); // Dandy's Cheers
	
	// Misc
	private static final String GIVE_DANDI_BUFF_VAR = "GIVE_DANDI_BUFF";
	
	/**
	 * Initializes the {@code TrainingWithDandy} event.<br>
	 * Sets up the required NPC and talk IDs for the event.
	 */
	private TrainingWithDandy()
	{
		addStartNpc(DANDY);
		addFirstTalkId(DANDY);
		addTalkId(DANDY);
		addVariableToClear(GIVE_DANDI_BUFF_VAR);
	}
	
	/**
	 * Handles specific events for the Training with Dandy system.<br>
	 * It processes logic based on the provided {@code event} string.<br>
	 * This method determines which HTML file to display next.
	 * @param event The identifier of the current event.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The name of the HTML file to be displayed.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "33894-1.htm":
			{
				htmltext = event;
				break;
			}
			case "dandy_buff":
			{
				final Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.MINUTE, 30);
				calendar.set(Calendar.HOUR_OF_DAY, 6);
				final long resetTime = calendar.getTimeInMillis();
				final long previousDate = player.getVariables().getLong(GIVE_DANDI_BUFF_VAR, 0);
				if (previousDate < resetTime)
				{
					npc.setTarget(player);
					npc.doCast(DANDY_CH.getSkill());
					player.getVariables().set(GIVE_DANDI_BUFF_VAR, System.currentTimeMillis());
					player.broadcastStatusUpdate();
					htmltext = "33894-2.htm";
					break;
				}
				
				htmltext = "33894-3.htm";
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which HTML file to display for the dialogue.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the name of the HTML file to load.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + "-1.htm";
	}
	
	/**
	 * This is the main entry point for the {@code TrainingWithDandy} class.<br>
	 * It initializes a new instance of the event.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new TrainingWithDandy();
	}
}
