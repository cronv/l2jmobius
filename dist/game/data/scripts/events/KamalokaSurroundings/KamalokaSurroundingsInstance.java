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
package events.KamalokaSurroundings;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Rim Kamaloka instance: 6 stages (Lv. 105-130) x Normal / Enchant type, 5 minute solo score attack.<br>
 * Entry is offered by the town Pathfinder Worker ({@link events.KamalokaSurroundings.KamalokaSurroundings}); the inner Pathfinder Worker hands out the reward.
 * @author Atronic
 */
public class KamalokaSurroundingsInstance extends InstanceScript
{
	// NPCs
	private static final int PATHFINDER_TOWN = 34520;
	private static final int PATHFINDER_INSIDE = 34521;
	
	// Stages: index 0-5 = Lv. 105, 110, 115, 120, 125, 130
	// @formatter:off
	protected static final int[] STAGE_LEVELS = {105, 110, 115, 120, 125, 130};
	private static final int[] TEMPLATES_NORMAL = {46, 47, 48, 49, 50, 51};
	private static final int[] TEMPLATES_ENCHANT = {52, 53, 54, 55, 56, 199};
	// @formatter:on
	
	// Monsters per stage: {Kanabion, Doppler, Void}
	// @formatter:off
	private static final int[][] MONSTERS_NORMAL =
	{
		{24525, 24526, 24527}, // White Skull Kanabion
		{24528, 24529, 24530}, // Fervent Kanabion
		{24531, 24532, 24533}, // Kanabion Swordsman
		{22452, 22453, 22454}, // Savage Kanabion
		{22455, 22456, 22457}, // Begrudged Kanabion
		{22458, 22459, 22460}, // Fallen Kanabion
	};
	private static final int[][] MONSTERS_ENCHANT =
	{
		{22461, 22462, 22463}, // Sturdy White Skull Kanabion
		{22464, 22465, 22466}, // Sturdy Fervent Kanabion
		{22467, 22468, 22469}, // Sturdy Kanabion Swordsman
		{22470, 22471, 22472}, // Sturdy Savage Kanabion
		{22473, 22474, 22475}, // Sturdy Begrudged Kanabion
		{22476, 22477, 22478}, // Sturdy Fallen Kanabion
	};
	// @formatter:on
	private static final int TYPE_KANABION = 0;
	private static final int TYPE_DOPPLER = 1;
	private static final int TYPE_VOID = 2;
	
	// Points
	// @formatter:off
	private static final int[] POINTS_BY_TYPE = {70, 80, 100};
	// @formatter:on
	private static final int ULTIMATE_BUFF_PENALTY = 10;
	private static final int[] GRADE_POINTS =
	{
		12500, // S
		7000, // A
		3500, // B
		2300, // C
		1500, // D
	};
	// @formatter:off
	private static final String[] GRADES = {"S", "A", "B", "C", "D", "F"};
	// @formatter:on
	private static final int GRADE_F = 5;
	
	// Doppler / Void appearance
	private static final int DOPPLER_CHANCE = 20; // % when a Kanabion dies
	private static final int VOID_CHANCE = 30; // % when a Doppler dies
	
	// Single spawn point in the center of the hall
	private static final int SPAWN_X = 42423;
	private static final int SPAWN_Y = -219779;
	private static final int SPAWN_Z = -8752;
	private static final int SPAWN_HEADING = 0;
	
	// Ultimate Buff (Void monsters channel it for 5 seconds; if not killed in time, -10 points)
	private static final SkillHolder ULTIMATE_BUFF = new SkillHolder(4340, 1);
	private static final int ULTIMATE_BUFF_CAST_TIME = 5000;
	
	// Rewards
	// @formatter:off
	private static final int[] MEDAL_NORMAL = {34593, 34594, 34595, 34596, 34597, 34598};
	private static final int[] MEDAL_ENCHANT = {34960, 34961, 34962, 34963, 34964, 34965};
	private static final long[] REWARD_EXP = {1861974522L, 2866182272L, 4393721330L, 6709919328L, 10211612468L, 15491002654L};
	private static final long[] REWARD_SP = {1675777L, 2579564L, 3954349L, 6038927L, 9190451L, 13941902L};
	// @formatter:on
	private static final int RIM_KAMALOKA_BOX = 81141;
	
	// Box count per stage, by grade S, A, B, C, D (F gets nothing)
	// @formatter:off
	private static final int[][] BOX_COUNT =
	{
		{4, 3, 2, 1, 1},
		{8, 7, 6, 5, 4},
		{10, 9, 8, 7, 6},
		{12, 11, 10, 9, 8},
		{14, 13, 12, 11, 10},
		{16, 15, 14, 13, 12},
	};
	// @formatter:on
	
	// Timing
	private static final int FIGHT_TIME = 300; // seconds
	private static final int START_DELAY = 3000; // ms after entering
	private static final int REWARD_TIME = 5; // minutes the Pathfinder waits after the battle
	private static final int DEATH_EJECT_TIME = 1; // minutes
	
	// Instance status
	private static final int STATUS_WAITING = 0;
	private static final int STATUS_FIGHTING = 1;
	private static final int STATUS_FINISHED = 2;
	private static final int STATUS_FAILED = 3;
	
	// Storage keys
	private static final String VAR_MEDAL = "RIM_KAMALOKA_MEDAL";
	private static final String GV_RANKING = "RIM_KAMALOKA_RANKING";
	private static final String TIMER_TEXT = "Remaining Time";
	
	// Class -> Protection of Light
	private static final Map<PlayerClass, Integer> PROTECTION_OF_LIGHT = new HashMap<>();
	static
	{
		PROTECTION_OF_LIGHT.put(PlayerClass.SIGEL_PHOENIX_KNIGHT, 34599);
		PROTECTION_OF_LIGHT.put(PlayerClass.SIGEL_HELL_KNIGHT, 34600);
		PROTECTION_OF_LIGHT.put(PlayerClass.SIGEL_EVA_TEMPLAR, 34601);
		PROTECTION_OF_LIGHT.put(PlayerClass.SIGEL_SHILLIEN_TEMPLAR, 34602);
		PROTECTION_OF_LIGHT.put(PlayerClass.TYRR_DUELIST, 34603);
		PROTECTION_OF_LIGHT.put(PlayerClass.TYRR_DREADNOUGHT, 34604);
		PROTECTION_OF_LIGHT.put(PlayerClass.TYRR_TITAN, 34605);
		PROTECTION_OF_LIGHT.put(PlayerClass.TYRR_GRAND_KHAVATARI, 34606);
		PROTECTION_OF_LIGHT.put(PlayerClass.TYRR_MAESTRO, 34607);
		PROTECTION_OF_LIGHT.put(PlayerClass.TYRR_DOOMBRINGER, 34608);
		PROTECTION_OF_LIGHT.put(PlayerClass.OTHELL_ADVENTURER, 34609);
		PROTECTION_OF_LIGHT.put(PlayerClass.OTHELL_WIND_RIDER, 34610);
		PROTECTION_OF_LIGHT.put(PlayerClass.OTHELL_GHOST_HUNTER, 34611);
		PROTECTION_OF_LIGHT.put(PlayerClass.OTHELL_FORTUNE_SEEKER, 34612);
		PROTECTION_OF_LIGHT.put(PlayerClass.YUL_SAGITTARIUS, 34613);
		PROTECTION_OF_LIGHT.put(PlayerClass.YUL_MOONLIGHT_SENTINEL, 34614);
		PROTECTION_OF_LIGHT.put(PlayerClass.YUL_GHOST_SENTINEL, 34615);
		PROTECTION_OF_LIGHT.put(PlayerClass.YUL_TRICKSTER, 34616);
		PROTECTION_OF_LIGHT.put(PlayerClass.FEOH_ARCHMAGE, 34617);
		PROTECTION_OF_LIGHT.put(PlayerClass.FEOH_SOULTAKER, 34618);
		PROTECTION_OF_LIGHT.put(PlayerClass.FEOH_MYSTIC_MUSE, 34619);
		PROTECTION_OF_LIGHT.put(PlayerClass.FEOH_STORM_SCREAMER, 34620);
		PROTECTION_OF_LIGHT.put(PlayerClass.FEOH_SOUL_HOUND, 34621);
		PROTECTION_OF_LIGHT.put(PlayerClass.ISS_HIEROPHANT, 34622);
		PROTECTION_OF_LIGHT.put(PlayerClass.ISS_SWORD_MUSE, 34623);
		PROTECTION_OF_LIGHT.put(PlayerClass.ISS_SPECTRAL_DANCER, 34624);
		PROTECTION_OF_LIGHT.put(PlayerClass.ISS_DOMINATOR, 34625);
		PROTECTION_OF_LIGHT.put(PlayerClass.ISS_DOOMCRYER, 34626);
		PROTECTION_OF_LIGHT.put(PlayerClass.WYNN_ARCANA_LORD, 34627);
		PROTECTION_OF_LIGHT.put(PlayerClass.WYNN_ELEMENTAL_MASTER, 34628);
		PROTECTION_OF_LIGHT.put(PlayerClass.WYNN_SPECTRAL_MASTER, 34629);
		PROTECTION_OF_LIGHT.put(PlayerClass.AEORE_CARDINAL, 34630);
		PROTECTION_OF_LIGHT.put(PlayerClass.AEORE_EVA_SAINT, 34631);
		PROTECTION_OF_LIGHT.put(PlayerClass.AEORE_SHILLIEN_SAINT, 34632);
		PROTECTION_OF_LIGHT.put(PlayerClass.EVISCERATOR, 34633);
		PROTECTION_OF_LIGHT.put(PlayerClass.SAYHA_SEER, 34634);
		PROTECTION_OF_LIGHT.put(PlayerClass.SIGEL_DEATH_KNIGHT, 34635);
		PROTECTION_OF_LIGHT.put(PlayerClass.SHINE_MAKER, 34959);
		PROTECTION_OF_LIGHT.put(PlayerClass.WARG_4, 34635);
		PROTECTION_OF_LIGHT.put(PlayerClass.SAMURAI, 34635);
	}
	
	private static final int[] ALL_MONSTERS;
	static
	{
		final List<Integer> ids = new ArrayList<>();
		for (int[][] set : new int[][][]
		{
			MONSTERS_NORMAL,
			MONSTERS_ENCHANT
		})
		{
			for (int[] stage : set)
			{
				for (int id : stage)
				{
					ids.add(id);
				}
			}
		}
		
		ALL_MONSTERS = new int[ids.size()];
		for (int i = 0; i < ids.size(); i++)
		{
			ALL_MONSTERS[i] = ids.get(i);
		}
	}
	
	// @formatter:off
	private static final int[] ALL_TEMPLATES = {46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 199};
	// @formatter:on
	
	public KamalokaSurroundingsInstance()
	{
		super(ALL_TEMPLATES);
		addStartNpc(PATHFINDER_TOWN);
		addTalkId(PATHFINDER_TOWN, PATHFINDER_INSIDE);
		addFirstTalkId(PATHFINDER_INSIDE);
		addSpawnId(ALL_MONSTERS);
		addCreatureSeeId(ALL_MONSTERS);
		addKillId(ALL_MONSTERS);
		addInstanceLeaveId(ALL_TEMPLATES);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.startsWith("enter_"))
		{
			// enter_<stage>_<n|e>
			final String[] parts = event.split("_");
			if ((parts.length != 3) || (player == null))
			{
				return null;
			}
			
			final int stage;
			try
			{
				stage = Integer.parseInt(parts[1]);
			}
			catch (NumberFormatException e)
			{
				return null;
			}
			
			if ((stage < 0) || (stage >= STAGE_LEVELS.length))
			{
				return null;
			}
			
			final int templateId = "e".equals(parts[2]) ? TEMPLATES_ENCHANT[stage] : TEMPLATES_NORMAL[stage];
			if (hasActiveReuse(player))
			{
				player.sendPacket(new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(player.getName()));
				return "condNoEnter.htm";
			}
			
			enterInstance(player, npc, templateId);
			return null;
		}
		
		switch (event)
		{
			case "start_fight":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world) && world.isStatus(STATUS_WAITING))
				{
					startFight(world, player);
				}
				
				return null;
			}
			case "end_fight":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world) && world.isStatus(STATUS_FIGHTING))
				{
					endFight(world, player);
				}
				
				return null;
			}
			case "ultimate_cast":
			{
				if (npc != null)
				{
					castUltimateBuff(npc);
				}
				
				return null;
			}
			case "ultimate_resolve":
			{
				if (npc != null)
				{
					resolveUltimateBuff(npc);
				}
				
				return null;
			}
			case "reward_main":
			{
				return resultPage(npc, player);
			}
			case "receive_reward":
			{
				return receiveReward(npc, player);
			}
			case "medal_again":
			{
				return requestMedalAgain(npc, player);
			}
			case "34521-03.htm":
			case "34521-05.htm":
			{
				return event;
			}
			case "leave_go":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				if (world.getParameters().getBoolean("REWARDED", false))
				{
					teleportPlayerOut(player, world);
					return null;
				}
				
				return "34521-04.htm";
			}
			case "leave_final":
			{
				final Instance world = player.getInstanceWorld();
				if (isInInstance(world))
				{
					teleportPlayerOut(player, world);
				}
				
				return null;
			}
			default:
			{
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	@Override
	protected void onEnter(Player player, Instance instance, boolean firstEnter)
	{
		super.onEnter(player, instance, firstEnter);
		if (firstEnter)
		{
			instance.setStatus(STATUS_WAITING);
			final StatSet parameters = instance.getParameters();
			parameters.set("POINTS", 0);
			parameters.set("REWARDED", false);
			parameters.set("CLOSING", false);
			startQuestTimer("start_fight", START_DELAY, null, player);
		}
	}
	
	private void startFight(Instance world, Player player)
	{
		world.setStatus(STATUS_FIGHTING);
		
		// Classes without their own Protection of Light get the one Warg and Samurai share.
		final Skill skill = new SkillHolder(PROTECTION_OF_LIGHT.getOrDefault(player.getPlayerClass(), 34635), 1).getSkill();
		if (skill != null)
		{
			SkillCaster.triggerCast(player, player, skill);
		}
		
		addDeathListener(player);
		player.sendPacket(new ExSendUIEvent(player, false, false, FIGHT_TIME, 0, TIMER_TEXT));
		startQuestTimer("end_fight", FIGHT_TIME * 1000, null, player);
		spawnNext(world, TYPE_KANABION);
	}
	
	/**
	 * Monsters come one at a time at the center of the hall; the next one appears as soon as the previous one dies.
	 * @param world the instance
	 * @param type {@code TYPE_KANABION}, {@code TYPE_DOPPLER} or {@code TYPE_VOID}
	 */
	private void spawnNext(Instance world, int type)
	{
		final Npc monster = addSpawn(stageMonsters(world)[type], SPAWN_X, SPAWN_Y, SPAWN_Z, SPAWN_HEADING, false, 0, false, world.getId());
		final Player player = world.getFirstPlayer();
		if ((monster == null) || (player == null) || player.isDead())
		{
			return;
		}
		
		if (monster.isAttackable())
		{
			monster.asAttackable().addDamageHate(player, 0, 1000);
		}
		
		addAttackDesire(monster, player);
	}
	
	private void endFight(Instance world, Player player)
	{
		synchronized (world.getParameters())
		{
			world.setStatus(STATUS_FINISHED);
			clearMonsters(world);
		}
		
		leaveCombatState(player);
		
		player.sendPacket(new ExShowScreenMessage("The Rim Kamaloka battle has ended. Get your reward through the Pathfinder.", ExShowScreenMessage.TOP_CENTER, 8000, 0, true, true));
		addSpawn(PATHFINDER_INSIDE, SPAWN_X, SPAWN_Y, SPAWN_Z, SPAWN_HEADING, false, 0, false, world.getId());
		world.setDuration(REWARD_TIME);
	}
	
	private void failFight(Instance world, Player player)
	{
		final StatSet parameters = world.getParameters();
		synchronized (parameters)
		{
			world.setStatus(STATUS_FAILED);
			parameters.set("POINTS", 0);
			clearMonsters(world);
		}
		
		cancelQuestTimer("end_fight", null, player);
		leaveCombatState(player);
		
		player.sendPacket(new ExShowScreenMessage("You have fallen. Your evaluation has been erased - you must restart Rim Kamaloka.", ExShowScreenMessage.TOP_CENTER, 8000, 0, true, true));
		world.setDuration(DEATH_EJECT_TIME);
	}
	
	private void clearMonsters(Instance world)
	{
		for (Npc monster : world.getAliveNpcs(ALL_MONSTERS))
		{
			monster.doDie(null);
		}
	}
	
	/**
	 * Removes everything that only exists while fighting: timer UI, Protection of Light and the death listener.
	 * @param player the player
	 */
	private void leaveCombatState(Player player)
	{
		player.sendPacket(new ExSendUIEvent(player, true, false, 0, 0, TIMER_TEXT));
		for (int skillId : PROTECTION_OF_LIGHT.values())
		{
			player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, skillId);
		}
		
		removeDeathListener(player);
	}
	
	private static int stageIndex(Instance world)
	{
		final int templateId = world.getTemplateId();
		for (int i = 0; i < STAGE_LEVELS.length; i++)
		{
			if ((TEMPLATES_NORMAL[i] == templateId) || (TEMPLATES_ENCHANT[i] == templateId))
			{
				return i;
			}
		}
		
		return 0;
	}
	
	private static boolean isEnchant(Instance world)
	{
		final int templateId = world.getTemplateId();
		for (int id : TEMPLATES_ENCHANT)
		{
			if (id == templateId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static int[] stageMonsters(Instance world)
	{
		return (isEnchant(world) ? MONSTERS_ENCHANT : MONSTERS_NORMAL)[stageIndex(world)];
	}
	
	/**
	 * @param world the instance
	 * @param npcId the monster
	 * @return {@code TYPE_KANABION}, {@code TYPE_DOPPLER}, {@code TYPE_VOID} or -1 if the monster does not belong to this stage
	 */
	private static int monsterType(Instance world, int npcId)
	{
		final int[] monsters = stageMonsters(world);
		for (int type = 0; type < monsters.length; type++)
		{
			if (monsters[type] == npcId)
			{
				return type;
			}
		}
		
		return -1;
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world) && (monsterType(world, npc.getId()) == TYPE_VOID))
		{
			startQuestTimer("ultimate_cast", 2000 + getRandom(3000), npc, null);
		}
		
		super.onSpawn(npc);
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world) || !creature.isPlayer() || !world.isStatus(STATUS_FIGHTING))
		{
			return;
		}
		
		if (npc.isAttackable())
		{
			npc.asAttackable().addDamageHate(creature, 0, 1000);
		}
		
		npc.getAI().setIntentionActive();
		addAttackDesire(npc, creature);
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final int type = monsterType(world, npc.getId());
		if (type < 0)
		{
			return;
		}
		
		// Dying always interrupts an Ultimate Buff channel, which only a Void monster runs.
		final StatSet parameters = world.getParameters();
		if (type == TYPE_VOID)
		{
			parameters.remove("UB_" + npc.getObjectId());
		}
		
		int next = TYPE_KANABION;
		if ((type == TYPE_KANABION) && (getRandom(100) < DOPPLER_CHANCE))
		{
			next = TYPE_DOPPLER;
		}
		else if ((type == TYPE_DOPPLER) && (getRandom(100) < VOID_CHANCE))
		{
			next = TYPE_VOID;
		}
		
		// The end timer can close the fight while a kill is counted, so the status is checked under the lock that closes it.
		synchronized (parameters)
		{
			if (!world.isStatus(STATUS_FIGHTING))
			{
				return;
			}
			
			addPoints(parameters, POINTS_BY_TYPE[type]);
			spawnNext(world, next);
		}
	}
	
	private void castUltimateBuff(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world) || !world.isStatus(STATUS_FIGHTING) || npc.isDead())
		{
			return;
		}
		
		world.getParameters().set("UB_" + npc.getObjectId(), true);
		npc.broadcastPacket(new MagicSkillUse(npc, npc, ULTIMATE_BUFF.getSkillId(), ULTIMATE_BUFF.getSkillLevel(), ULTIMATE_BUFF_CAST_TIME, 0));
		startQuestTimer("ultimate_resolve", ULTIMATE_BUFF_CAST_TIME, npc, null);
	}
	
	private void resolveUltimateBuff(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final StatSet parameters = world.getParameters();
		final String key = "UB_" + npc.getObjectId();
		if (!parameters.getBoolean(key, false))
		{
			return;
		}
		
		parameters.remove(key);
		synchronized (parameters)
		{
			if (npc.isDead() || !world.isStatus(STATUS_FIGHTING))
			{
				return;
			}
			
			SkillCaster.triggerCast(npc, npc, ULTIMATE_BUFF.getSkill());
			addPoints(parameters, -ULTIMATE_BUFF_PENALTY);
		}
	}
	
	/**
	 * Changes the score of the instance, never below zero.<br>
	 * Kills and the Ultimate Buff timer both change it, so callers hold the lock on the parameters while the fight is checked and the score written.
	 * @param parameters the parameters of the instance
	 * @param amount the points to add, negative to take them away
	 */
	private static void addPoints(StatSet parameters, int amount)
	{
		parameters.set("POINTS", Math.max(0, parameters.getInt("POINTS", 0) + amount));
	}
	
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (isInInstance(npc.getInstanceWorld()) && (npc.getId() == PATHFINDER_INSIDE))
		{
			return resultPage(npc, player);
		}
		
		player.sendPacket(ActionFailed.STATIC_PACKET);
		return null;
	}
	
	private static int gradeIndex(int points)
	{
		for (int i = 0; i < GRADE_POINTS.length; i++)
		{
			if (points >= GRADE_POINTS[i])
			{
				return i;
			}
		}
		
		return GRADE_F;
	}
	
	private String resultPage(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		if (!isInInstance(world) || (npc == null))
		{
			return null;
		}
		
		final StatSet parameters = world.getParameters();
		if (parameters.getBoolean("REWARDED", false))
		{
			return "34521-02.htm";
		}
		
		final int points = parameters.getInt("POINTS", 0);
		final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
		html.setHtml(getHtm(player, "34521-01.htm"));
		html.replace("%score%", String.valueOf(points));
		html.replace("%rank%", GRADES[gradeIndex(points)]);
		player.sendPacket(html);
		return null;
	}
	
	private String receiveReward(Npc npc, Player player)
	{
		final Instance world = player.getInstanceWorld();
		if ((npc == null) || !isInInstance(world) || !world.isStatus(STATUS_FINISHED))
		{
			return null;
		}
		
		final StatSet parameters = world.getParameters();
		if (parameters.getBoolean("REWARDED", false))
		{
			return "34521-02.htm";
		}
		
		parameters.set("REWARDED", true);
		
		final int stage = stageIndex(world);
		final boolean enchant = isEnchant(world);
		final int points = parameters.getInt("POINTS", 0);
		final int grade = gradeIndex(points);
		
		// Medal of Honor - every grade gets one (skill level 1-6 = F..S)
		final int medalId = (enchant ? MEDAL_ENCHANT : MEDAL_NORMAL)[stage];
		final int medalLevel = GRADES.length - grade;
		final Skill medal = new SkillHolder(medalId, medalLevel).getSkill();
		if (medal != null)
		{
			SkillCaster.triggerCast(npc, player, medal);
			player.getVariables().set(VAR_MEDAL, medalId + ";" + medalLevel + ";" + (System.currentTimeMillis() + 86400000L));
		}
		
		// EXP/SP and boxes - F grade gets nothing
		if (grade != GRADE_F)
		{
			player.getStat().addExpAndSp(REWARD_EXP[stage], REWARD_SP[stage], false);
			player.addItem(ItemProcessType.REWARD, RIM_KAMALOKA_BOX, BOX_COUNT[stage][grade], npc, true);
		}
		
		// Only the stage that was cleared gets a reuse time, hasActiveReuse makes it block every stage.
		world.setReenterTime(nextReset());
		submitScore(player.getName(), points);
		return "34521-02.htm";
	}
	
	private String requestMedalAgain(Npc npc, Player player)
	{
		final String[] parts = player.getVariables().getString(VAR_MEDAL, "").split(";");
		if ((npc == null) || (parts.length != 3) || !StringUtil.isInteger(parts[0]) || !StringUtil.isInteger(parts[1]) || (parts[2].length() > 18) || !StringUtil.isNumeric(parts[2]) || (Long.parseLong(parts[2]) <= System.currentTimeMillis()))
		{
			return "34521-07.htm";
		}
		
		final Skill medal = new SkillHolder(Integer.parseInt(parts[0]), Integer.parseInt(parts[1])).getSkill();
		if (medal == null)
		{
			return "34521-07.htm";
		}
		
		SkillCaster.triggerCast(npc, player, medal);
		return "34521-06.htm";
	}
	
	/**
	 * @param player the player
	 * @return {@code true} if any Rim Kamaloka stage (Normal or Enchant) is on reuse for this player
	 */
	private static boolean hasActiveReuse(Player player)
	{
		final long now = System.currentTimeMillis();
		for (int templateId : ALL_TEMPLATES)
		{
			if (InstanceManager.getInstance().getInstanceTime(player, templateId) > now)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static long nextReset()
	{
		final Calendar reset = Calendar.getInstance();
		reset.set(Calendar.HOUR_OF_DAY, 6);
		reset.set(Calendar.MINUTE, 30);
		reset.set(Calendar.SECOND, 0);
		reset.set(Calendar.MILLISECOND, 0);
		if (!reset.after(Calendar.getInstance()))
		{
			reset.add(Calendar.DAY_OF_MONTH, 1);
		}
		
		return reset.getTimeInMillis();
	}
	
	/**
	 * @return up to 3 entries of {name, score}, best first
	 */
	protected static synchronized List<String[]> getRanking()
	{
		final List<String[]> ranking = new ArrayList<>();
		final String[] parts = GlobalVariablesManager.getInstance().getString(GV_RANKING, "").split("\\|");
		if ((parts.length == 0) || parts[0].isEmpty())
		{
			return ranking;
		}
		
		try
		{
			if (Long.parseLong(parts[0]) <= System.currentTimeMillis())
			{
				GlobalVariablesManager.getInstance().set(GV_RANKING, "");
				return ranking;
			}
		}
		catch (NumberFormatException e)
		{
			GlobalVariablesManager.getInstance().set(GV_RANKING, "");
			return ranking;
		}
		
		for (int i = 1; i < parts.length; i++)
		{
			// An entry without a numeric score is skipped, so a damaged variable cannot break the reward that submits to it.
			final int split = parts[i].lastIndexOf(':');
			if (split < 1)
			{
				continue;
			}
			
			final String score = parts[i].substring(split + 1);
			if (StringUtil.isInteger(score))
			{
				ranking.add(new String[]
				{
					parts[i].substring(0, split),
					score
				});
			}
		}
		
		return ranking;
	}
	
	private static synchronized void submitScore(String name, int score)
	{
		final List<String[]> ranking = getRanking();
		for (int i = 0; i < ranking.size(); i++)
		{
			final String[] entry = ranking.get(i);
			if (entry[0].equals(name))
			{
				if (Integer.parseInt(entry[1]) >= score)
				{
					return;
				}
				
				ranking.remove(i);
				break;
			}
		}
		
		int pos = 0;
		while ((pos < ranking.size()) && (Integer.parseInt(ranking.get(pos)[1]) >= score))
		{
			pos++;
		}
		
		if (pos >= 3)
		{
			return;
		}
		
		ranking.add(pos, new String[]
		{
			name,
			String.valueOf(score)
		});
		
		final StringBuilder sb = new StringBuilder();
		sb.append(nextReset());
		for (int i = 0; (i < ranking.size()) && (i < 3); i++)
		{
			final String[] entry = ranking.get(i);
			sb.append('|').append(entry[0]).append(':').append(entry[1]);
		}
		
		GlobalVariablesManager.getInstance().set(GV_RANKING, sb.toString());
	}
	
	private void addDeathListener(Player player)
	{
		removeDeathListener(player);
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onPlayerDeath(event), this));
	}
	
	private void removeDeathListener(Player player)
	{
		for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_DEATH))
		{
			if (listener.getOwner() == this)
			{
				listener.unregisterMe();
			}
		}
	}
	
	private void onPlayerDeath(OnCreatureDeath event)
	{
		final Creature target = event.getTarget();
		if ((target == null) || !target.isPlayer())
		{
			return;
		}
		
		final Player player = target.asPlayer();
		final Instance world = player.getInstanceWorld();
		if (isInInstance(world) && world.isStatus(STATUS_FIGHTING))
		{
			failFight(world, player);
		}
	}
	
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		leaveCombatState(player);
		cancelQuestTimer("start_fight", null, player);
		cancelQuestTimer("end_fight", null, player);
		
		// Solo zone: once the player is out, the attempt is over (an unclaimed evaluation is erased).
		final StatSet parameters = instance.getParameters();
		if (parameters.getBoolean("CLOSING", false))
		{
			return;
		}
		
		parameters.set("CLOSING", true);
		final int instanceId = instance.getId();
		ThreadPool.schedule(() ->
		{
			final Instance world = InstanceManager.getInstance().getInstance(instanceId);
			if (world != null)
			{
				world.destroy();
			}
		}, 1000);
	}
	
	public static void main(String[] args)
	{
		new KamalokaSurroundingsInstance();
	}
}
