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

import java.util.List;

import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a specialized merchant NPC that handles fishing mechanics.<br>
 * This class extends {@code Merchant} to provide unique behaviors for players interacting with fishing spots.
 */
public class Fisherman extends Merchant
{
	/**
	 * Creates a new {@code Fisherman} instance.<br>
	 * This constructor initializes the actor using a provided template.<br>
	 * It sets the {@code InstanceType} to {@code Fisherman}.
	 * @param template The {@code NpcTemplate} used to create this fisherman.
	 */
	public Fisherman(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Fisherman);
	}
	
	/**
	 * Retrieves the file path for a specific fisherman HTML template.<br>
	 * It constructs a path based on the provided NPC ID and value.
	 * @param npcId The unique identifier of the NPC.
	 * @param value An additional value used to differentiate templates.
	 * @param player The {@code Player} object associated with the request.
	 * @return A {@code String} representing the full path to the .htm file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom = "";
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		return "data/html/fisherman/" + pom + ".htm";
	}
	
	/**
	 * Handles feedback from a bypass command sent by a player.<br>
	 * It checks if the {@code command} is "FishSkillList".<br>
	 * If it matches, it displays the fish skill list to the {@code player}.<br>
	 * Otherwise, it passes the request to the superclass method.
	 * @param player The {@code Player} who sent the command.
	 * @param command The string content of the bypass command.
	 */
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		if (command.equalsIgnoreCase("FishSkillList"))
		{
			showFishSkillList(player);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	/**
	 * Displays the list of available fishing skills to a player.<br>
	 * It checks if the {@code Player} has any new skills to learn.<br>
	 * If no skills are available, it sends a message regarding the required level.<br>
	 * Otherwise, it sends the list of acquirable skills.
	 * @param player The {@code Player} who will view the skill list.
	 */
	public static void showFishSkillList(Player player)
	{
		final List<SkillLearn> skills = SkillTreeData.getInstance().getAvailableFishingSkills(player);
		if (skills.isEmpty())
		{
			final int minlLevel = SkillTreeData.getInstance().getMinLevelForNewSkill(player, SkillTreeData.getInstance().getFishingSkillTree());
			if (minlLevel > 0)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_DO_NOT_HAVE_ANY_FURTHER_SKILLS_TO_LEARN_COME_BACK_WHEN_YOU_HAVE_REACHED_LEVEL_S1);
				sm.addInt(minlLevel);
				player.sendPacket(sm);
			}
			else
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
			}
		}
		else
		{
			player.sendPacket(new ExAcquirableSkillListByClass(skills, AcquireSkillType.FISHING));
		}
	}
}
