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
package ai.bosses.Anakim;

import java.util.Calendar;
import java.util.Collection;
import java.util.List;

import org.l2jmobius.gameserver.config.GrandBossConfig;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.GrandBoss;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.QuestTimer;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the artificial intelligence for the {@code Anakim} boss.<br>
 * It manages combat behaviors and special mechanics for this specific entity.
 * @author LasTravel, NviX
 * @URL http://boards.lineage2.com/showpost.php?p=3386784&postcount=6<br>
 * @video http://www.youtube.com/watch?v=LecymFTJQzQ
 */
public class Anakim extends Script
{
	// Status
	private static final int ALIVE = 0;
	private static final int FIGHTING = 1;
	private static final int DEAD = 2;
	
	// NPCs
	private static final int ANAKIM = 29348;
	private static final int EXIST_CUBIC = 31109;
	private static final int ANAKIM_CUBIC = 31101;
	// @formatter:off
    private static final int[] ANAKIM_MINIONS = {29349, 29350, 29351};
    // @formatter:on
	private static final int[] ALL_MOBS =
	{
		ANAKIM,
		ANAKIM_MINIONS[0],
		ANAKIM_MINIONS[1],
		ANAKIM_MINIONS[2],
	};
	
	// Misc
	private static final Location ENTER_ANAKIM_LOC = new Location(184569, -12134, -5499);
	private static final ZoneType BOSS_ZONE = ZoneManager.getInstance().getZoneById(12003);
	
	// Vars
	private static long _lastAction;
	private static Npc _anakimBoss;
	private static GrandBoss _anakimTemp;
	
	/**
	 * Initializes the {@code Anakim} boss entity.<br>
	 * Registers all necessary event IDs and NPC interactions.<br>
	 * Checks the current status of the boss to handle respawning logic.
	 */
	public Anakim()
	{
		addTalkId(EXIST_CUBIC, ANAKIM_CUBIC);
		addStartNpc(EXIST_CUBIC, ANAKIM_CUBIC);
		addFirstTalkId(EXIST_CUBIC, ANAKIM_CUBIC);
		addAttackId(ALL_MOBS);
		addKillId(ALL_MOBS);
		addSkillSeeId(ALL_MOBS);
		
		// Unlock
		final StatSet info = GrandBossManager.getInstance().getStatSet(ANAKIM);
		final int status = GrandBossManager.getInstance().getStatus(ANAKIM);
		if (status == DEAD)
		{
			final long time = info.getLong("respawn_time") - System.currentTimeMillis();
			if (time > 0)
			{
				startQuestTimer("unlock_anakim", time, null, null);
			}
			else
			{
				_anakimTemp = (GrandBoss) addSpawn(ANAKIM, -126920, -234182, -15563, 0, false, 0);
				GrandBossManager.getInstance().addBoss(_anakimTemp);
				GrandBossManager.getInstance().setStatus(ANAKIM, ALIVE);
			}
		}
		else
		{
			_anakimTemp = (GrandBoss) addSpawn(ANAKIM, -126920, -234182, -15563, 0, false, 0);
			GrandBossManager.getInstance().addBoss(_anakimTemp);
			GrandBossManager.getInstance().setStatus(ANAKIM, ALIVE);
		}
	}
	
	/**
	 * Handles specific game events for the Anakim boss.<br>
	 * This method processes logic such as unlocking the boss, checking activity tasks, and managing spawns.<br>
	 * It also handles teleporting players and clearing timers based on the provided event string.
	 * @param event The name of the event to be processed.
	 * @param npc The {@code Npc} instance associated with the event.
	 * @param player The {@code Player} instance associated with the event.
	 * @return The result of the superclass {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "unlock_anakim":
			{
				_anakimTemp = (GrandBoss) addSpawn(ANAKIM, -126920, -234182, -15563, 0, false, 0);
				GrandBossManager.getInstance().addBoss(_anakimTemp);
				GrandBossManager.getInstance().setStatus(ANAKIM, ALIVE);
				break;
			}
			case "check_activity_task":
			{
				if ((_lastAction + 900000) < System.currentTimeMillis())
				{
					GrandBossManager.getInstance().setStatus(ANAKIM, ALIVE);
					for (Creature creature : BOSS_ZONE.getCharactersInside())
					{
						if (creature != null)
						{
							if (creature.isNpc())
							{
								creature.deleteMe();
							}
							else if (creature.isPlayer())
							{
								creature.teleToLocation(MapRegionData.getInstance().getTeleToLocation(creature, TeleportWhereType.TOWN));
							}
						}
					}
					
					startQuestTimer("end_anakim", 2000, null, null);
				}
				else
				{
					startQuestTimer("check_activity_task", 60000, null, null);
				}
				break;
			}
			case "cancel_timers":
			{
				QuestTimer activityTimer = getQuestTimer("check_activity_task", null, null);
				if (activityTimer != null)
				{
					activityTimer.cancel();
				}
				
				QuestTimer forceEnd = getQuestTimer("end_anakim", null, null);
				if (forceEnd != null)
				{
					forceEnd.cancel();
				}
				break;
			}
			case "end_anakim":
			{
				notifyEvent("cancel_timers", null, null);
				if (_anakimBoss != null)
				{
					_anakimBoss.deleteMe();
				}
				
				BOSS_ZONE.oustAllPlayers();
				if (GrandBossManager.getInstance().getStatus(ANAKIM) != DEAD)
				{
					GrandBossManager.getInstance().setStatus(ANAKIM, ALIVE);
				}
				break;
			}
			case "exist":
			{
				player.teleToLocation(TeleportWhereType.TOWN);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the interaction logic when a {@code Player} talks to an {@code Npc}.<br>
	 * This method checks for party requirements and level constraints before teleporting players.<br>
	 * It also manages the spawning of the boss based on current game status.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the HTML message or a custom response, or {@code null}.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		if (npcId == ANAKIM_CUBIC)
		{
			final int _anakimStatus = GrandBossManager.getInstance().getStatus(ANAKIM);
			if (_anakimStatus > ALIVE)
			{
				return "31101-01.html";
			}
			
			if (!player.isInParty())
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
				packet.setHtml(getHtm(player, "31101-02.html"));
				packet.replace("%min%", Integer.toString(GrandBossConfig.ANAKIM_MIN_PLAYERS));
				player.sendPacket(packet);
				return null;
			}
			
			final Party party = player.getParty();
			final boolean isInCC = party.isInCommandChannel();
			final boolean isPartyLeader = (isInCC) ? party.getCommandChannel().isLeader(player) : party.isLeader(player);
			if (!isPartyLeader)
			{
				return "31101-03.html";
			}
			
			final List<Player> members = (isInCC) ? party.getCommandChannel().getMembers() : party.getMembers();
			
			final int memberSize = members.size();
			if ((memberSize < GrandBossConfig.ANAKIM_MIN_PLAYERS) || (memberSize > GrandBossConfig.ANAKIM_MAX_PLAYERS))
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
				packet.setHtml(getHtm(player, "31101-02.html"));
				packet.replace("%min%", Integer.toString(GrandBossConfig.ANAKIM_MIN_PLAYERS));
				player.sendPacket(packet);
				return null;
			}
			
			for (Player member : members)
			{
				if (member.getLevel() < GrandBossConfig.ANAKIM_MIN_PLAYER_LEVEL)
				{
					final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
					packet.setHtml(getHtm(player, "31101-04.html"));
					packet.replace("%minLevel%", Integer.toString(GrandBossConfig.ANAKIM_MIN_PLAYER_LEVEL));
					player.sendPacket(packet);
					return null;
				}
			}
			
			for (Player member : members)
			{
				if (member.isInsideRadius3D(npc, 1000) && (npcId == ANAKIM_CUBIC))
				{
					member.teleToLocation(ENTER_ANAKIM_LOC, true);
				}
			}
			
			if ((_anakimStatus == ALIVE) && (npcId == ANAKIM_CUBIC))
			{
				GrandBossManager.getInstance().setStatus(ANAKIM, FIGHTING);
				
				// Spawn the rb
				_anakimBoss = addSpawn(ANAKIM, 185080, -12613, -5499, 16550, false, 0);
				GrandBossManager.getInstance().addBoss((GrandBoss) _anakimBoss);
				startQuestTimer("end_anakim", 60 * 60000, null, null); // 1h
			}
		}
		
		return super.onTalk(npc, player);
	}
	
	/**
	 * Determines the HTML message to display when a player talks to an {@code Npc} for the first time.<br>
	 * It returns the ID of the {@code Npc} followed by the .html extension.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * Handles logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks for out-of-zone attacks and handles specific boss behaviors.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isPet Whether the attack was performed by a pet.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isPet)
	{
		_lastAction = System.currentTimeMillis();
		if (npc.isMinion() || npc.isRaid())// Anakim and minions
		{
			// Anti BUGGERS
			if (!BOSS_ZONE.isInsideZone(attacker)) // Character attacking out of zone
			{
				attacker.doDie(null);
			}
			
			if (!BOSS_ZONE.isInsideZone(npc)) // Npc moved out of the zone
			{
				final Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					npc.teleToLocation(spawn.getX(), spawn.getY(), spawn.getZ());
				}
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the logic for resetting boss states and spawning new entities.<br>
	 * It updates timers and manages specific quest progression based on the kill.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isPet Whether the killing blow was dealt by a pet.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isPet)
	{
		if (npc.getId() == ANAKIM)
		{
			notifyEvent("cancel_timers", null, null);
			addSpawn(EXIST_CUBIC, 185082, -12606, -5499, 6133, false, 900000); // 15min
			
			GrandBossManager.getInstance().setStatus(ANAKIM, DEAD);
			final long respawnTime = getRespawnTime();
			final StatSet info = GrandBossManager.getInstance().getStatSet(ANAKIM);
			info.set("respawn_time", System.currentTimeMillis() + respawnTime);
			GrandBossManager.getInstance().setStatSet(ANAKIM, info);
			
			startQuestTimer("unlock_anakim", respawnTime, null, null);
			startQuestTimer("end_anakim", 900000, null, null);
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} sees a skill being cast.<br>
	 * This method checks if specific minions should target the {@code caster}.<br>
	 * It updates the {@code Npc} aggression and intention based on the skill type.
	 * @param npc The {@code Npc} entity performing the action.
	 * @param caster The {@code Player} who cast the skill.
	 * @param skill The {@code Skill} object being used.
	 * @param targets A collection of {@code WorldObject} entities targeted by the skill.
	 * @param isPet Whether the skill was cast by a pet.
	 */
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isPet)
	{
		if (ArrayUtil.contains(ANAKIM_MINIONS, npc.getId()) && getRandomBoolean())
		{
			if (skill.getAbnormalType() == AbnormalType.HP_RECOVER)
			{
				final WorldObject target = npc.getTarget();
				if (!npc.isCastingNow() && (target != npc) && (target != caster) && (target != _anakimBoss)) // Don't call minions if are healing Anakim
				{
					npc.asAttackable().clearAggroList();
					npc.setTarget(caster);
					npc.asAttackable().addDamageHate(caster, 500, 99999);
					npc.getAI().setIntentionAttack(caster);
				}
			}
		}
	}
	
	/**
	 * Calculates the time required for the boss to respawn.<br>
	 * It uses a specific schedule based on days and hours.
	 * @return The total respawn time in seconds as an {@code int}.
	 */
	private int getRespawnTime()
	{
		return (int) calcReuseFromDays(0, 21, Calendar.TUESDAY, 0, 16, Calendar.SATURDAY);
	}
	
	/**
	 * Calculates the remaining time until a specific reuse event.<br>
	 * It compares two different time points and returns the difference in milliseconds.<br>
	 * The method ensures that if a date has already passed this week, it moves to the next week.
	 * @param day1Minute The minute for the first time point.
	 * @param day1Hour The hour for the first time point.
	 * @param day1Day The day of the week for the first time point.
	 * @param day2Minute The minute for the second time point.
	 * @param day2Hour The hour for the second time point.
	 * @param day2Day The day of the week for the second time point.
	 * @return The number of milliseconds remaining until the earliest valid time point.
	 */
	private long calcReuseFromDays(int day1Minute, int day1Hour, int day1Day, int day2Minute, int day2Hour, int day2Day)
	{
		Calendar now = Calendar.getInstance();
		Calendar day1 = (Calendar) now.clone();
		day1.set(Calendar.MINUTE, day1Minute);
		day1.set(Calendar.HOUR_OF_DAY, day1Hour);
		day1.set(Calendar.DAY_OF_WEEK, day1Day);
		
		Calendar day2 = (Calendar) day1.clone();
		day2.set(Calendar.MINUTE, day2Minute);
		day2.set(Calendar.HOUR_OF_DAY, day2Hour);
		day2.set(Calendar.DAY_OF_WEEK, day2Day);
		
		if (now.after(day1))
		{
			day1.add(Calendar.WEEK_OF_MONTH, 1);
		}
		
		if (now.after(day2))
		{
			day2.add(Calendar.WEEK_OF_MONTH, 1);
		}
		
		Calendar reenter = day1;
		if (day2.before(day1))
		{
			reenter = day2;
		}
		
		return reenter.getTimeInMillis() - System.currentTimeMillis();
	}
	
	/**
	 * This is the entry point for the {@code Anakim} class.<br>
	 * It initializes a new instance of the {@code Anakim} object.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Anakim();
	}
}
