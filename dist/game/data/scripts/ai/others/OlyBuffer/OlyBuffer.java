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
package ai.others.OlyBuffer;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;

/**
 * This class handles the artificial intelligence for an Olympiad buffer.<br>
 * It manages how the NPC provides buffs to players during competition events.
 * @author St3eT, Mobius
 */
public class OlyBuffer extends Script
{
	// NPC
	private static final int OLYMPIAD_BUFFER = 36402;
	
	// Skills
	private static final CommonSkill[] BUFFS =
	{
		CommonSkill.OLYMPIAD_FEVER_MELODY,
		CommonSkill.OLYMPIAD_DRUMBEAT_OF_SERENITY,
		CommonSkill.OLYMPIAD_FANTASIA_HARMONY,
	};
	
	/**
	 * Private constructor for the {@code OlyBuffer} class.<br>
	 * This constructor initializes the script and registers necessary NPCs if Olympiad is enabled.
	 */
	private OlyBuffer()
	{
		if (OlympiadConfig.OLYMPIAD_ENABLED)
		{
			addStartNpc(OLYMPIAD_BUFFER);
			addFirstTalkId(OLYMPIAD_BUFFER);
			addTalkId(OLYMPIAD_BUFFER);
		}
	}
	
	/**
	 * Handles the initial interaction when a player talks to an {@code Npc}.<br>
	 * It determines which HTML file to display based on the NPC's script value.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} containing the name of the HTML file to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (npc.isScriptValue(0))
		{
			htmltext = "olympiad_master001.htm";
		}
		
		return htmltext;
	}
	
	/**
	 * Handles specific events for the Olympiad Buffer NPC.<br>
	 * It applies different buffs based on the provided event name.<br>
	 * The method also sets a script value and schedules the NPC to be deleted.
	 * @param event The type of event to trigger, such as "guardian", "berserker", or "magician".
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance receiving the buffs.
	 * @return A {@code String} representing the HTML file to display.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "buff":
			{
				applyBuffs(npc, player);
				break;
			}
		}
		
		npc.setScriptValue(1);
		getTimers().addTimer("DELETE_ME", 5000, evnt -> npc.deleteMe());
		return "olympiad_master003.htm";
	}
	
	/**
	 * This method applies specific buffs to a {@code Player}.<br>
	 * It iterates through the predefined list of {@code CommonSkill} skills.<br>
	 * The {@code SkillCaster} triggers each skill from the {@code Npc}.
	 * @param npc The {@code Npc} that will cast the skills.
	 * @param player The {@code Player} who will receive the buffs.
	 */
	private void applyBuffs(Npc npc, Player player)
	{
		for (CommonSkill holder : BUFFS)
		{
			SkillCaster.triggerCast(npc, player, holder.getSkill());
		}
	}
	
	/**
	 * This is the entry point for the {@code OlyBuffer} application.<br>
	 * It initializes a new instance of the {@code OlyBuffer} class.
	 * @param args The command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new OlyBuffer();
	}
}
