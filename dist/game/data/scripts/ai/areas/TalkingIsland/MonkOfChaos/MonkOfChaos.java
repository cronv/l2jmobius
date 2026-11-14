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
package ai.areas.TalkingIsland.MonkOfChaos;

import java.util.List;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.SubclassType;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAcquirableSkillListByClass;

/**
 * This class handles the artificial intelligence for the {@code MonkOfChaos} NPC.<br>
 * It defines the behaviors and logic specific to this entity within the Talking Island area.
 * @author Sdw, Mobius
 */
public class MonkOfChaos extends Script
{
	private static final int MONK_OF_CHAOS = 33880;
	private static final int MIN_LEVEL = 85;
	private static final int CANCEL_FEE = 100000000;
	private static final int CHAOS_POMANDER = 37374;
	private static final int CHAOS_POMANDER_DUALCLASS = 37375;
	private static final String[] REVELATION_VAR_NAMES =
	{
		PlayerVariables.REVELATION_SKILL_1_MAIN_CLASS,
		PlayerVariables.REVELATION_SKILL_2_MAIN_CLASS,
	};
	
	private static final String[] DUALCLASS_REVELATION_VAR_NAMES =
	{
		PlayerVariables.REVELATION_SKILL_1_DUAL_CLASS,
		PlayerVariables.REVELATION_SKILL_2_DUAL_CLASS
	};
	
	/**
	 * Initializes the {@code MonkOfChaos} script.<br>
	 * Registers the NPC ID and talk IDs to the system.
	 */
	private MonkOfChaos()
	{
		addStartNpc(MONK_OF_CHAOS);
		addTalkId(MONK_OF_CHAOS);
		addFirstTalkId(MONK_OF_CHAOS);
	}
	
	/**
	 * Handles specific events for the Monk of Chaos script.<br>
	 * It processes skill learning and cancellation logic based on player status.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The name of the HTML file to display as a result.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "33880-1.html":
			case "33880-2.html":
			{
				htmltext = event;
				break;
			}
			case "LearnRevelationSkills":
			{
				if ((player.getLevel() < MIN_LEVEL) || !player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
				{
					htmltext = "no-learn.html";
					break;
				}
				
				if (player.isSubClassActive() && !player.isDualClassActive())
				{
					htmltext = "no-subclass.html";
					break;
				}
				
				if (player.isDualClassActive())
				{
					final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableRevelationSkills(player, SubclassType.DUALCLASS);
					if (!skills.isEmpty())
					{
						player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.REVELATION_DUALCLASS));
					}
					else
					{
						player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
					}
				}
				else
				{
					final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableRevelationSkills(player, SubclassType.BASECLASS);
					if (!skills.isEmpty())
					{
						player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.REVELATION));
					}
					else
					{
						player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
					}
				}
				break;
			}
			case "CancelRevelationSkills":
			{
				if (player.isSubClassActive() && !player.isDualClassActive())
				{
					htmltext = "no-subclass.html";
					break;
				}
				
				final boolean isDualClassActive = player.isDualClassActive();
				final String[] varNames = isDualClassActive ? DUALCLASS_REVELATION_VAR_NAMES : REVELATION_VAR_NAMES;
				int count = 0;
				for (String varName : varNames)
				{
					if (player.getVariables().getInt(varName, 0) > 0)
					{
						count++;
					}
				}
				
				if ((player.getLevel() < MIN_LEVEL) || !player.isInCategory(CategoryType.SIXTH_CLASS_GROUP) || (count == 0))
				{
					htmltext = "no-cancel.html";
					break;
				}
				
				if (player.getAdena() < CANCEL_FEE)
				{
					htmltext = "no-adena.html";
					break;
				}
				
				takeItems(player, 57, CANCEL_FEE);
				
				for (SkillLearn skillLearn : SkillTreeData.getInstance().getAllRevelationSkills(player, isDualClassActive ? SubclassType.DUALCLASS : SubclassType.BASECLASS))
				{
					final Skill skill = player.getKnownSkill(skillLearn.getSkillId());
					if (skill != null)
					{
						player.removeSkill(skill);
					}
				}
				
				for (String varName : varNames)
				{
					player.getVariables().remove(varName);
				}
				
				giveItems(player, isDualClassActive ? CHAOS_POMANDER_DUALCLASS : CHAOS_POMANDER, count);
				htmltext = "canceled.html";
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method determines which dialogue file to load.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A string representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code MonkOfChaos} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new MonkOfChaos();
	}
}
