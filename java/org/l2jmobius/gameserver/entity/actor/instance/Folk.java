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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.status.FolkStatus;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a special NPC used for the skill tree system.<br>
 * It allows players to interact with and acquire new skills based on their character's class.<br>
 * This entity handles the logic for displaying and learning skills from {@code SkillTreeData}.
 */
public class Folk extends Npc
{
	/**
	 * Creates a new {@code Folk} instance based on the provided template.<br>
	 * This constructor initializes the entity with specific folk properties.
	 * @param template The {@code NpcTemplate} used to define the initial stats and data.
	 */
	public Folk(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Folk);
		setInvul(false);
	}
	
	/**
	 * Retrieves the current status of the {@code Folk}.<br>
	 * This method returns the status as a {@code FolkStatus} type.
	 * @return the current {@code FolkStatus} of this entity.
	 */
	@Override
	public FolkStatus getStatus()
	{
		return (FolkStatus) super.getStatus();
	}
	
	/**
	 * Initializes the status of the {@code Folk} instance.<br>
	 * This method creates and sets a new {@code FolkStatus} object for this entity.
	 */
	@Override
	public void initCharStatus()
	{
		setStatus(new FolkStatus(this));
	}
	
	/**
	 * Displays the list of available skills to a player.<br>
	 * This method checks if the player can learn new skills based on their class and level.<br>
	 * It sends a packet with the skill list or a system message if no skills are available.
	 * @param player The {@code Player} who will receive the skill information.
	 * @param npc The {@code Npc} that triggers the skill display.
	 * @param playerClass The {@code PlayerClass} of the player to determine eligible skills.
	 */
	public static void showSkillList(Player player, Npc npc, PlayerClass playerClass)
	{
		final int npcId = npc.getTemplate().getId();
		if (npcId == 32611) // Tolonis (Officer)
		{
			final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableCollectSkills(player);
			if (skills.isEmpty()) // No more skills to learn, come back when you level.
			{
				final int minLevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, SkillTreeData.getInstance().getCollectSkillTree());
				if (minLevel > 0)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
					sm.addInt(minLevel);
					player.sendPacket(sm);
				}
				else
				{
					player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
				}
			}
			else
			{
				player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.COLLECT));
			}
			return;
		}
		
		// Normal skills, No LearnedByFS, No LearnedByFP, no AutoGet skills.
		final Collection<SkillLearn> skills = SkillTreeData.getInstance().getAvailableSkills(player, playerClass, false, false, false);
		if (skills.isEmpty())
		{
			final Map<Long, SkillLearn> skillTree = SkillTreeData.getInstance().getCompleteClassSkillTree(playerClass);
			final int minLevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, skillTree);
			if (minLevel > 0)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				sm.addInt(minLevel);
				player.sendPacket(sm);
			}
			else if (player.getPlayerClass().level() == 1)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN_PLEASE_COME_BACK_AFTER_S1ND_CLASS_CHANGE);
				sm.addInt(2);
				player.sendPacket(sm);
			}
			else
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
			}
		}
		else
		{
			player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.CLASS));
		}
	}
}
