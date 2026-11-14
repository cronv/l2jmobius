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
package ai.others.AdventurersGuide;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.DeathPenalty;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class handles the artificial intelligence for the {@code AdventurersGuide} NPC.<br>
 * It defines the behaviors and actions performed by this specific guide script.
 * @author St3eT, Mobius
 */
public class AdventurersGuide extends Script
{
	// NPC
	private static final int[] ADVENTURERS_GUIDE =
	{
		32327,
		33950,
	};
	
	// Skills
	private static final SkillHolder BLESS_PROTECTION = new SkillHolder(5182, 1); // Blessing of Protection
	private static final SkillHolder FANTASIA = new SkillHolder(62074, 1); // Fantasia Harmony - Adventurer
	private static final SkillHolder[] GROUP_BUFFS =
	{
		new SkillHolder(62072, 1), // Fever Melody - Adventurer
		new SkillHolder(62073, 1), // Drumbeat of Serenity - Adventurer
	};
	
	// Misc
	private static final int MAX_LEVEL_BUFFS = 105;
	private static final int MIN_LEVEL_PROTECTION = 40;
	
	/**
	 * Private constructor for the {@code AdventurersGuide} class.<br>
	 * This constructor initializes the NPC IDs and talk IDs.<br>
	 * It registers the required data for the script to function.
	 */
	private AdventurersGuide()
	{
		addStartNpc(ADVENTURERS_GUIDE);
		addTalkId(ADVENTURERS_GUIDE);
		addFirstTalkId(ADVENTURERS_GUIDE);
	}
	
	/**
	 * Handles specific events triggered by the Adventurers Guide.<br>
	 * It determines which HTML content to display or applies buffs based on the event type.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The name of the HTML file to display or a success message.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "guide-01.html":
			case "guide-05.html":
			case "guide-06.html":
			case "guide-07.html":
			case "guide-08.html":
			{
				htmltext = event;
				break;
			}
			case "index":
			{
				htmltext = npc.getId() + ".html";
				break;
			}
			case "weakenBreath":
			{
				final DeathPenalty deathPenalty = player.getDeathPenalty();
				if (deathPenalty.getLevel() < 3)
				{
					htmltext = "guide-noBreath.html";
					break;
				}
				
				deathPenalty.setLevel(2);
				htmltext = "guide-cleanedBreath.html";
				break;
			}
			case "fantasia":
			{
				if (player.getLevel() > MAX_LEVEL_BUFFS)
				{
					return "guide-noBuffs.html";
				}
				
				for (SkillHolder holder : GROUP_BUFFS)
				{
					SkillCaster.triggerCast(npc, player, holder.getSkill());
				}
				
				htmltext = applyBuffs(npc, player, FANTASIA.getSkill());
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Applies various buffs to a {@code Player} from an {@code Npc}.<br>
	 * It checks the level of the player before applying group and specific skills.<br>
	 * If requirements are met, it triggers the necessary skill casts.
	 * @param npc The {@code Npc} casting the skills.
	 * @param player The {@code Player} receiving the buffs.
	 * @param skill The primary {@code Skill} to be applied.
	 * @return A string path to an HTML file if the level is too high, or {@code null} otherwise.
	 */
	private String applyBuffs(Npc npc, Player player, Skill skill)
	{
		for (SkillHolder holder : GROUP_BUFFS)
		{
			SkillCaster.triggerCast(npc, player, holder.getSkill());
		}
		
		SkillCaster.triggerCast(npc, player, skill);
		if ((player.getLevel() < MIN_LEVEL_PROTECTION) && (player.getPlayerClass().level() <= 1))
		{
			SkillCaster.triggerCast(npc, player, BLESS_PROTECTION.getSkill());
		}
		
		return null;
	}
	
	/**
	 * Handles the initial interaction when a {@code Player} talks to an {@code Npc}.<br>
	 * It returns the filename for the first dialogue.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code AdventurersGuide} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new AdventurersGuide();
	}
}
