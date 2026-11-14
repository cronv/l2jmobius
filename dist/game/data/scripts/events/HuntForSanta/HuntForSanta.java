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
package events.HuntForSanta;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Manages the "Hunt for Santa" holiday event.<br>
 * This class handles the logic and progression for the seasonal activity.<br>
 * It provides mechanics for players to participate in finding Santa.
 * @author Mobius
 */
public class HuntForSanta extends LongTimeEvent
{
	// NPC
	private static final int NOELLE = 34008;
	
	// Skills
	private static final SkillHolder BUFF_STOCKING = new SkillHolder(16419, 1);
	private static final SkillHolder BUFF_TREE = new SkillHolder(16420, 1);
	private static final SkillHolder BUFF_SNOWMAN = new SkillHolder(16421, 1);
	
	// Item
	private static final int SANTAS_MARK = 40313;
	
	/**
	 * Initializes the event configuration.<br>
	 * Registers the starting {@code Npc} and required talk IDs.
	 */
	private HuntForSanta()
	{
		addStartNpc(NOELLE);
		addFirstTalkId(NOELLE);
		addTalkId(NOELLE);
	}
	
	/**
	 * Handles specific event logic for the Hunt for Santa activity.<br>
	 * It processes buff applications and rewards based on the provided {@code event} string.
	 * @param event The unique identifier or HTML file name for the current action.
	 * @param npc The {@code Npc} entity involved in the interaction.
	 * @param player The {@code Player} receiving the effects or rewards.
	 * @return A {@code String} containing the HTML text to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34008.htm":
			case "34008-1.htm":
			{
				htmltext = event;
				break;
			}
			case "receiveBuffStocking":
			{
				htmltext = applyBuff(npc, player, BUFF_STOCKING.getSkill());
				startQuestTimer("rewardBuffStocking" + player.getObjectId(), 7200000, null, player);
				break;
			}
			case "receiveBuffTree":
			{
				htmltext = applyBuff(npc, player, BUFF_TREE.getSkill());
				startQuestTimer("rewardBuffTree" + player.getObjectId(), 7200000, null, player);
				break;
			}
			case "receiveBuffSnowman":
			{
				htmltext = applyBuff(npc, player, BUFF_SNOWMAN.getSkill());
				startQuestTimer("rewardBuffSnowman" + player.getObjectId(), 7200000, null, player);
				break;
			}
			case "receiveBuffAll":
			{
				htmltext = applyAllBuffs(npc, player);
				final int objectId = player.getObjectId();
				startQuestTimer("rewardBuffStocking" + objectId, 7200000, null, player);
				startQuestTimer("rewardBuffTree" + objectId, 7200000, null, player);
				startQuestTimer("rewardBuffSnowman" + objectId, 7200000, null, player);
				break;
			}
			case "changeBuff":
			{
				removeBuffs(player);
				htmltext = "34008-1.htm";
				break;
			}
		}
		
		if (event.startsWith("rewardBuffStocking") //
			|| event.startsWith("rewardBuffSnowman") //
			|| event.startsWith("rewardBuffTree"))
		{
			if ((player != null) && (player.isOnlineInt() == 1))
			{
				giveItems(player, SANTAS_MARK, 1);
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the interaction when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method returns the name of the HTML file to display.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34008.htm";
	}
	
	/**
	 * Removes existing buffs from the {@code Player}.<br>
	 * Triggers a new {@code Skill} cast from the {@code Npc} to the {@code Player}.<br>
	 * Returns the filename for the success message.
	 * @param npc The {@code Npc} casting the skill.
	 * @param player The {@code Player} receiving the buff.
	 * @param skill The {@code Skill} to be applied.
	 * @return A {@code String} representing the HTML file name.
	 */
	private String applyBuff(Npc npc, Player player, Skill skill)
	{
		removeBuffs(player);
		SkillCaster.triggerCast(npc, player, skill);
		return "34008-2.htm";
	}
	
	/**
	 * Applies multiple buffs to party members near the {@code Npc}.<br>
	 * Checks if the {@code Player} is a leader of a large enough party.<br>
	 * Removes old buffs before applying new ones to nearby members.
	 * @param npc The {@code Npc} casting the skills.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return A string representing the HTML file to display.
	 */
	private String applyAllBuffs(Npc npc, Player player)
	{
		final Party party = player.getParty();
		if ((party != null) && (party.getLeader() == player) && ((party.getMemberCount() > 6) || (party.getRaceCount() > 2)))
		{
			for (Player member : party.getMembers())
			{
				if (LocationUtil.calculateDistance(npc, member, false, false) < 500)
				{
					removeBuffs(member);
					SkillCaster.triggerCast(npc, member, BUFF_STOCKING.getSkill());
					SkillCaster.triggerCast(npc, member, BUFF_TREE.getSkill());
					SkillCaster.triggerCast(npc, member, BUFF_SNOWMAN.getSkill());
				}
			}
			
			return "34008-2.htm";
		}
		else if (party == null)
		{
			return "34008-3.htm";
		}
		
		return "34008-4.htm";
	}
	
	/**
	 * Removes specific event buffs from the player.<br>
	 * This method stops the effects for stocking, tree, and snowman skills.<br>
	 * It also cancels the associated quest timers for these rewards.
	 * @param player The {@code Player} whose buffs need to be removed.
	 */
	private void removeBuffs(Player player)
	{
		final int objectId = player.getObjectId();
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, BUFF_STOCKING.getSkill());
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, BUFF_TREE.getSkill());
		player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, BUFF_SNOWMAN.getSkill());
		cancelQuestTimer("rewardBuffStocking" + objectId, null, player);
		cancelQuestTimer("rewardBuffTree" + objectId, null, player);
		cancelQuestTimer("rewardBuffSnowman" + objectId, null, player);
	}
	
	/**
	 * This method is called when a player logs into the game.<br>
	 * It checks for specific active buffs on the {@code Player}.<br>
	 * If found, it starts quest timers based on the buff duration.
	 * @param event The {@code OnPlayerLogin} event containing the login data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		final BuffInfo buffStocking = player.getEffectList().getBuffInfoBySkillId(BUFF_STOCKING.getSkillId());
		final BuffInfo buffTree = player.getEffectList().getBuffInfoBySkillId(BUFF_TREE.getSkillId());
		final BuffInfo buffSnowman = player.getEffectList().getBuffInfoBySkillId(BUFF_SNOWMAN.getSkillId());
		if (buffStocking != null)
		{
			final int objectId = player.getObjectId();
			cancelQuestTimer("rewardBuffStocking" + objectId, null, player);
			startQuestTimer("rewardBuffStocking" + objectId, buffStocking.getTime() * 1000, null, player);
		}
		
		if (buffTree != null)
		{
			final int objectId = player.getObjectId();
			cancelQuestTimer("rewardBuffTree" + objectId, null, player);
			startQuestTimer("rewardBuffTree" + objectId, buffTree.getTime() * 1000, null, player);
		}
		
		if (buffSnowman != null)
		{
			final int objectId = player.getObjectId();
			cancelQuestTimer("rewardBuffSnowman" + objectId, null, player);
			startQuestTimer("rewardBuffSnowman" + objectId, buffSnowman.getTime() * 1000, null, player);
		}
	}
	
	/**
	 * This is the entry point for the {@code HuntForSanta} class.<br>
	 * It initializes a new instance of the event.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new HuntForSanta();
	}
}
