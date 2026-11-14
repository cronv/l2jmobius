/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.areas.IvoryTower.AvantGarde;

import java.util.List;

import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.RequestAcquireSkill;
import org.l2jmobius.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the AI logic for the {@code AvantGarde} NPC.<br>
 * This script manages transformation skill learning and the selling of transformation scrolls.
 * @author Zoey76
 */
public class AvantGarde extends Script
{
	// NPC
	private static final int AVANT_GARDE = 32323;
	
	/**
	 * Initializes the {@code AvantGarde} script.<br>
	 * This constructor registers the necessary NPC and talk IDs.<br>
	 * It sets up the requirements for skill acquisition and interaction.
	 */
	public AvantGarde()
	{
		addStartNpc(AVANT_GARDE);
		addTalkId(AVANT_GARDE);
		addFirstTalkId(AVANT_GARDE);
		addAcquireSkillId(AVANT_GARDE);
	}
	
	/**
	 * Handles the logic when a player attempts to acquire a skill.<br>
	 * This method checks if the acquisition type is {@code AcquireSkillType.TRANSFORM}.<br>
	 * If it is, it displays the transformation skill list to the player.
	 * @param npc The {@code Npc} interacting with the player.
	 * @param player The {@code Player} attempting to learn a skill.
	 * @param skill The {@code Skill} being acquired.
	 * @param type The {@code AcquireSkillType} of the acquisition.
	 */
	@Override
	public void onAcquireSkill(Npc npc, Player player, Skill skill, AcquireSkillType type)
	{
		if (type == AcquireSkillType.TRANSFORM)
		{
			showTransformSkillList(player);
		}
	}
	
	/**
	 * Handles specific events for the Avant-Garde NPC.<br>
	 * It manages skill learning and item sales based on the provided event name.
	 * @param event The unique identifier or HTML file path for the current event.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "32323-02.html":
			case "32323-02a.html":
			case "32323-02b.html":
			case "32323-02c.html":
			case "32323-05.html":
			case "32323-05no.html":
			case "32323-06.html":
			case "32323-06no.html":
			{
				htmltext = event;
				break;
			}
			case "LearnTransformationSkill":
			{
				if (RequestAcquireSkill.canTransform(player))
				{
					showTransformSkillList(player);
				}
				else
				{
					htmltext = "32323-03.html";
				}
				break;
			}
			case "BuyTransformationItems":
			{
				if (RequestAcquireSkill.canTransform(player))
				{
					MultisellData.getInstance().separateAndSend(32323001, player, npc, false);
				}
				else
				{
					htmltext = "32323-04.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method triggers when the player speaks to the NPC for the first time.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the HTML file path to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "32323-01.html";
	}
	
	/**
	 * Handles the interaction when a {@code Player} talks to an {@code Npc}.<br>
	 * It returns the path to the HTML file for the dialogue.
	 * @param npc The {@code Npc} being interacted with.
	 * @param talker The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the filename of the dialogue.
	 */
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		return "32323-01.html";
	}
	
	/**
	 * Displays the list of available transformation skills to a player.<br>
	 * It checks if any skills can be learned based on the current level.<br>
	 * If no skills are available, it sends a message regarding the required level.<br>
	 * Otherwise, it sends the list of acquirable transform skills.
	 * @param player The {@code Player} who will receive the skill list or notification.
	 */
	public static void showTransformSkillList(Player player)
	{
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableTransformSkills(player);
		if (skills.isEmpty())
		{
			final int minlevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, SkillTreeData.getInstance().getTransformSkillTree());
			if (minlevel > 0)
			{
				// No more skills to learn, come back when you level.
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				sm.addInt(minlevel);
				player.sendPacket(sm);
			}
			else
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
			}
		}
		else
		{
			player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.TRANSFORM));
		}
	}
	
	/**
	 * This is the entry point for the {@code AvantGarde} class.<br>
	 * It initializes a new instance of the {@code AvantGarde} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new AvantGarde();
	}
}
