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
package instances.SakuraIsle;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * Sakura Isle instance - kill 100 Yokai minions to unlock the Yokai General boss.<br>
 * Three difficulty levels (110 / 120 / 130) picked at entry by the party leader via Benusta.
 * @author Atronic
 */
public class SakuraIsle extends InstanceScript
{
	// NPCs
	private static final int BENUSTA = 31461;
	// Minions
	private static final int[] MINIONS =
	{
		26608,
		26609,
		26610, // Lv 110
		26612,
		26613,
		26614, // Lv 120
		26616,
		26617,
		26618, // Lv 130
	};
	// Yokai Generals.
	private static final int GENERAL_110 = 26611; // Ao Oni
	private static final int GENERAL_120 = 26615; // Kuro Oni
	private static final int GENERAL_130 = 26619; // Aka Oni
	
	// Buff shown on the General while minions are alive — also blocks damage.
	private static final SkillHolder MINIONS_PROTECTION = new SkillHolder(62456, 1);
	
	// Reward items
	private static final ItemHolder REWARD_BOX_110 = new ItemHolder(82453, 1); // Benusta's Reward Pack Lv. 1
	private static final ItemHolder REWARD_BOX_120 = new ItemHolder(82455, 1); // Benusta's Reward Pack Lv. 3
	private static final ItemHolder REWARD_BOX_130 = new ItemHolder(83729, 1); // Benusta's Reward Pack Lv. 5
	private static final ItemHolder WANDERER_TALES = new ItemHolder(83575, 2); // Novice Wanderer's Tales
	
	// Misc
	private static final int TEMPLATE_ID = 361;
	private static final int TOTAL_MINIONS = 100;
	private static final int IMPERTINENCE_AT = 98;
	
	// Count-message refresh: the message expires after its duration on the client,
	// so we re-broadcast periodically to keep it on screen until 100/100.
	private static final int MINION_COUNT_DURATION = 30000; // ms
	private static final int MINION_COUNT_REFRESH = 25000; // ms (slightly less than duration)
	
	// Instance status: 0 = waiting for difficulty pick, 1 = battle in progress.
	private static final int STATUS_WAITING = 0;
	private static final int STATUS_BATTLE = 1;
	
	public SakuraIsle()
	{
		super(TEMPLATE_ID);
		addStartNpc(BENUSTA);
		addFirstTalkId(BENUSTA);
		addKillId(MINIONS);
		addKillId(GENERAL_110, GENERAL_120, GENERAL_130);
		addSpawnId(MINIONS);
		addSpawnId(GENERAL_110, GENERAL_120, GENERAL_130);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			// Stay put — no random patrol; only react when attacked.
			npc.setRandomWalking(false);
			
			final int npcId = npc.getId();
			if ((npcId == GENERAL_110) || (npcId == GENERAL_120) || (npcId == GENERAL_130))
			{
				// Boss is invulnerable + shows Minions' Protection buff while minions are alive.
				npc.setInvul(true);
				MINIONS_PROTECTION.getSkill().applyEffects(npc, npc);
			}
		}
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (npc.getId() != BENUSTA)
		{
			return null;
		}
		
		final Instance world = player.getInstanceWorld();
		if (isInInstance(world))
		{
			// Inside the instance: only the party leader (or GM) gets the difficulty picker.
			if (player.isInParty() && !player.getParty().isLeader(player) && !player.isGM())
			{
				return "31461-2.html";
			}
			return "31461-1.html";
		}
		
		// Outside the instance: entry dialog.
		return "31461.html";
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				handleEnter(npc, player);
				break;
			}
			case "benusta_texts":
			{
				handleBenustaTexts(player);
				break;
			}
			case "intro_message":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					showOnScreenMsg(world, NpcStringId.YOKAI_GENERAL_IS_PROTECTED_BY_HIS_MINIONS_KILL_THEM_ALL, ExShowScreenMessage.TOP_CENTER, 6000, true);
				}
				break;
			}
			case "minion_count_refresh":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					break;
				}
				final int killed = world.getParameters().getInt("MINIONS_KILLED", 0);
				if ((killed > 0) && (killed < TOTAL_MINIONS))
				{
					broadcastMinionCount(world, killed);
				}
				if (killed < TOTAL_MINIONS)
				{
					startQuestTimer("minion_count_refresh", MINION_COUNT_REFRESH, null, player);
				}
				break;
			}
			case "level110":
			{
				startBattle(player, 110);
				break;
			}
			case "level120":
			{
				startBattle(player, 120);
				break;
			}
			case "level130":
			{
				startBattle(player, 130);
				break;
			}
		}
		return null;
	}
	
	private void handleEnter(Npc npc, Player player)
	{
		if (player.isInParty())
		{
			final Party party = player.getParty();
			if (!party.isLeader(player))
			{
				player.sendPacket(SystemMessageId.ONLY_A_PARTY_LEADER_CAN_MAKE_THE_REQUEST_TO_ENTER);
				return;
			}
			if (player.isInCommandChannel())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_ENTER_AS_YOU_DON_T_MEET_THE_REQUIREMENTS);
				return;
			}
			
			final List<Player> members = party.getMembers();
			for (Player member : members)
			{
				if (!member.isInsideRadius3D(npc, 1000))
				{
					player.sendMessage("Player " + member.getName() + " must come closer.");
					return;
				}
			}
			for (Player member : members)
			{
				enterInstance(member, npc, TEMPLATE_ID);
			}
		}
		else if (player.isGM())
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY_SO_YOU_CANNOT_ENTER);
			return;
		}
		
		final Instance world = player.getInstanceWorld();
		if (world != null)
		{
			world.spawnGroup("BENUSTA");
			startQuestTimer("benusta_texts", 5000, null, player);
		}
	}
	
	private void handleBenustaTexts(Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		// If Benusta is gone (difficulty picked), stop the loop.
		final Npc benusta = world.getNpc(BENUSTA);
		if (benusta == null)
		{
			return;
		}
		
		final int idx = world.getParameters().getInt("BENUSTA_TEXT", 0);
		final NpcStringId msg;
		switch (idx)
		{
			case 0:
			{
				msg = NpcStringId.THE_PARTY_LEADER_SHOULD_TALK_TO_ME_AND_SELECT_THE_DIFFICULTY_LEVEL;
				break;
			}
			case 1:
			{
				msg = NpcStringId.TAKE_INTO_ACCOUNT_THE_ABILITIES_OF_YOUR_PARTY_MEMBERS_WHEN_SELECTING_THE_DIFFICULTY;
				break;
			}
			default:
			{
				msg = NpcStringId.THE_SELECTED_DIFFICULTY_CANNOT_BE_CHANGED_SO_CHOOSE_WISELY;
				break;
			}
		}
		showOnScreenMsg(world, msg, ExShowScreenMessage.TOP_CENTER, 5000, true);
		benusta.broadcastSay(ChatType.NPC_GENERAL, msg);
		world.getParameters().set("BENUSTA_TEXT", (idx + 1) % 3);
		startQuestTimer("benusta_texts", 8000, null, player);
	}
	
	private void startBattle(Player player, int level)
	{
		final Instance world = player.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		// Only party leader (or GM) may pick.
		if (player.isInParty() && !player.getParty().isLeader(player) && !player.isGM())
		{
			return;
		}
		// Difficulty can only be chosen once.
		if (world.getStatus() != STATUS_WAITING)
		{
			return;
		}
		
		world.getParameters().set("INSTANCE_LEVEL", level);
		world.getParameters().set("MINIONS_KILLED", 0);
		world.setStatus(STATUS_BATTLE);
		
		switch (level)
		{
			case 110:
			{
				showOnScreenMsg(world, NpcStringId.LV_110_IS_SELECTED_PREPARE_FOR_BATTLE_2, ExShowScreenMessage.TOP_CENTER, 6000, true);
				break;
			}
			case 120:
			{
				showOnScreenMsg(world, NpcStringId.LV_120_IS_SELECTED_PREPARE_FOR_BATTLE_2, ExShowScreenMessage.TOP_CENTER, 6000, true);
				break;
			}
			case 130:
			{
				showOnScreenMsg(world, NpcStringId.LV_130_IS_SELECTED_PREPARE_FOR_BATTLE_2, ExShowScreenMessage.TOP_CENTER, 6000, true);
				break;
			}
		}
		
		// Despawn Benusta — difficulty is locked in.
		final Npc benusta = world.getNpc(BENUSTA);
		if (benusta != null)
		{
			benusta.deleteMe();
		}
		
		// Spawn the chosen minion + general group.
		world.spawnGroup("GROUP_" + level);
		
		// Intro message follows the "level chosen" message.
		startQuestTimer("intro_message", 6000, null, player);
		// Start the periodic refresh that keeps the kill-count message visible.
		startQuestTimer("minion_count_refresh", 12000, null, player);
	}
	
	private void broadcastMinionCount(Instance world, int killed)
	{
		showOnScreenMsg(world, NpcStringId.YOKAI_GENERAL_S_MINIONS_KILLED_S1_S2, ExShowScreenMessage.TOP_CENTER, MINION_COUNT_DURATION, true, String.valueOf(killed), String.valueOf(TOTAL_MINIONS));
	}
	
	private void grantBossRewards(Instance world, int generalId)
	{
		final ItemHolder rewardBox;
		
		switch (generalId)
		{
			case GENERAL_120:
			{
				rewardBox = REWARD_BOX_120;
				break;
			}
			case GENERAL_130:
			{
				rewardBox = REWARD_BOX_130;
				break;
			}
			default: // GENERAL_110
			{
				rewardBox = REWARD_BOX_110;
				break;
			}
		}
		
		// Per-member reward.
		for (Player member : world.getPlayers())
		{
			if ((member.getParty() != null) || member.isGM())
			{
				giveItems(member, rewardBox);
				giveItems(member, WANDERER_TALES);
			}
		}
	}
	
	private void releaseGeneral(Instance world)
	{
		for (int generalId : new int[]
		{
			GENERAL_110,
			GENERAL_120,
			GENERAL_130
		})
		{
			final Npc general = world.getNpc(generalId);
			if (general != null)
			{
				general.setInvul(false);
				general.stopSkillEffects(MINIONS_PROTECTION.getSkill());
			}
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final int npcId = npc.getId();
		if ((npcId == GENERAL_110) || (npcId == GENERAL_120) || (npcId == GENERAL_130))
		{
			// Boss down — hand out rewards then complete the instance.
			grantBossRewards(world, npcId);
			world.finishInstance();
			world.setReenterTime();
			return;
		}
		
		// Otherwise it's a minion — increment the kill counter.
		int killed = world.getParameters().getInt("MINIONS_KILLED", 0);
		if (killed >= TOTAL_MINIONS)
		{
			return;
		}
		killed++;
		world.getParameters().set("MINIONS_KILLED", killed);
		
		if (killed >= TOTAL_MINIONS)
		{
			// Final kill — replace the persistent count with the "now you can attack him" message.
			showOnScreenMsg(world, NpcStringId.YOU_HAVE_KILLED_ENOUGH_OF_YOKAI_GENERAL_S_MINIONS_AND_NOW_YOU_CAN_ATTACK_HIM, ExShowScreenMessage.TOP_CENTER, 10000, true);
			// Drop the General's invulnerability + buff icon.
			releaseGeneral(world);
		}
		else
		{
			// "Yokai General's minions killed: X/100" — persistent (refreshed by minion_count_refresh).
			broadcastMinionCount(world, killed);
			if (killed == IMPERTINENCE_AT)
			{
				// Boss taunt — separate position so it doesn't overwrite the count.
				world.broadcastPacket(new ExShowScreenMessage(NpcStringId.YOU_WILL_PAY_FOR_YOUR_IMPERTINENCE, ExShowScreenMessage.TOP_CENTER_2, 5000, true, ExShowScreenMessage.COLOR_RED));
			}
		}
	}
	
	public static void main(String[] args)
	{
		new SakuraIsle();
	}
}
