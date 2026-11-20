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
package custom.events.TeamVsTeam;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.mechanics.script.Event;
import org.l2jmobius.gameserver.mechanics.script.QuestTimer;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.MapUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the logic for a Team vs Team (TvT) competitive event.<br>
 * This class handles the mechanics, rules, and progression of matches between opposing groups.
 * @author Mobius
 */
public class TvT extends Event
{
	public enum EventState
	{
		INACTIVE,
		PARTICIPATING,
		STARTING,
		STARTED
	}
	
	private static final String HTML_PATH = "data/scripts/custom/events/TeamVsTeam/";
	
	// NPC
	private static final int MANAGER = 70010;
	
	// Skills
	private static final SkillHolder GHOST_WALKING = new SkillHolder(100000, 1); // Custom Ghost Walking
	private static final SkillHolder[] GROUP_BUFFS =
	{
		new SkillHolder(62072, 1), // Fever Melody - Adventurer
		new SkillHolder(62073, 1), // Drumbeat of Serenity - Adventurer
		new SkillHolder(62074, 1), // Fantasia Harmony - Adventurer
	};
	
	/** The state of the Ctf. */
	private static EventState _state = EventState.INACTIVE;
	
	// Others
	private static final int INSTANCE_ID = 3049;
	private static final int BLUE_DOOR_ID = 24190002;
	private static final int RED_DOOR_ID = 24190003;
	private static final Location MANAGER_SPAWN_LOC = new Location(83425, 148585, -3406, 32938);
	private static final Location BLUE_BUFFER_SPAWN_LOC = new Location(147450, 46913, -3400, 49000);
	private static final Location RED_BUFFER_SPAWN_LOC = new Location(151545, 46528, -3400, 16000);
	private static final Location BLUE_SPAWN_LOC = new Location(147447, 46722, -3416);
	private static final Location RED_SPAWN_LOC = new Location(151536, 46722, -3416);
	private static final ZoneType BLUE_PEACE_ZONE = ZoneManager.getInstance().getZoneByName("colosseum_peace1");
	private static final ZoneType RED_PEACE_ZONE = ZoneManager.getInstance().getZoneByName("colosseum_peace2");
	
	// Settings
	private static final int REGISTRATION_TIME = 10; // Minutes
	private static final int WAIT_TIME = 1; // Minutes
	private static final int FIGHT_TIME = 20; // Minutes
	private static final int INACTIVITY_TIME = 2; // Minutes
	private static final int MINIMUM_PARTICIPANT_LEVEL = 85;
	private static final int MAXIMUM_PARTICIPANT_LEVEL = 200;
	private static final int MINIMUM_PARTICIPANT_COUNT = 2;
	private static final int MAXIMUM_PARTICIPANT_COUNT = 24; // Scoreboard has 25 slots
	private static final int PARTY_MEMBER_COUNT = 7;
	private static final ItemHolder REWARD = new ItemHolder(57, 1000000); // Adena
	
	// Misc
	private static final Map<Player, Integer> PLAYER_SCORES = new ConcurrentHashMap<>();
	private static final Set<Player> PLAYER_LIST = ConcurrentHashMap.newKeySet();
	private static final Set<Player> BLUE_TEAM = ConcurrentHashMap.newKeySet();
	private static final Set<Player> RED_TEAM = ConcurrentHashMap.newKeySet();
	private static volatile int BLUE_SCORE;
	private static volatile int RED_SCORE;
	private static Instance PVP_WORLD = null;
	private static Npc MANAGER_NPC_INSTANCE = null;
	private static boolean TEAM_FORFEIT = false;
	
	/**
	 * Private constructor for the {@code TvT} class.<br>
	 * It initializes required talk IDs and zone IDs.<br>
	 * It also calls {@code loadConfig()} to prepare the system.
	 */
	private TvT()
	{
		addTalkId(MANAGER);
		addFirstTalkId(MANAGER);
		final int BLUE_PEACE_ZONEId = BLUE_PEACE_ZONE.getId();
		final int RED_PEACE_ZONEId = RED_PEACE_ZONE.getId();
		addExitZoneId(BLUE_PEACE_ZONEId, RED_PEACE_ZONEId);
		addEnterZoneId(BLUE_PEACE_ZONEId, RED_PEACE_ZONEId);
		
		loadConfig();
	}
	
	/**
	 * Loads the configuration from the XML file.<br>
	 * This method parses the event data and schedules them based on their patterns.<br>
	 * It initializes the timers for all events defined in the config.
	 */
	private void loadConfig()
	{
		new IXmlReader()
		{
			@Override
			public void load()
			{
				parseDatapackFile(HTML_PATH + "config.xml");
			}
			
			@Override
			public void parseDocument(Document document, File file)
			{
				final AtomicInteger count = new AtomicInteger(0);
				forEach(document, "event", eventNode ->
				{
					final StatSet att = new StatSet(parseAttributes(eventNode));
					final String name = att.getString("name");
					for (Node node = document.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling())
					{
						switch (node.getNodeName())
						{
							case "schedule":
							{
								final StatSet attributes = new StatSet(parseAttributes(node));
								final String pattern = attributes.getString("pattern");
								final SchedulingPattern schedulingPattern = new SchedulingPattern(pattern);
								final StatSet params = new StatSet();
								params.set("Name", name);
								params.set("SchedulingPattern", pattern);
								final long delay = schedulingPattern.getDelayToNextFromNow();
								getTimers().addTimer("Schedule" + count.incrementAndGet(), params, delay + 5000, null, null); // Added 5 seconds to prevent overlapping.
								LOGGER.info("Event " + name + " scheduled at " + TimeUtil.getDateTimeString(System.currentTimeMillis() + delay));
								break;
							}
						}
					}
				});
			}
		}.load();
	}
	
	/**
	 * Handles events triggered by the game timer.<br>
	 * This method checks if an event should be scheduled based on its name.<br>
	 * It calculates the delay and adds it to the internal timer system.
	 * @param event The unique identifier for the timer event.
	 * @param params A {@code StatSet} containing configuration data like patterns and names.
	 * @param npc The {@code Npc} associated with this event, if any.
	 * @param player The {@code Player} associated with this event, if any.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.startsWith("Schedule"))
		{
			eventStart(null);
			final SchedulingPattern schedulingPattern = new SchedulingPattern(params.getString("SchedulingPattern"));
			final long delay = schedulingPattern.getDelayToNextFromNow();
			getTimers().addTimer(event, params, delay + 5000, null, null); // Added 5 seconds to prevent overlapping.
			LOGGER.info("Event " + params.getString("Name") + " scheduled at " + TimeUtil.getDateTimeString(System.currentTimeMillis() + delay));
		}
	}
	
	/**
	 * Handles specific events for the Team Vs Team event.<br>
	 * It processes actions like registration, teleportation, and buffing.<br>
	 * Returns a {@code String} representing the HTML file to display to the player.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} interacting with the player.
	 * @param player The {@code Player} who triggered the event.
	 * @return A {@code String} containing the filename for the UI, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{ // Event not participating, no starting or started return null.
		if (!IS_PARTICIPATING() && !IS_STARTING() && !IS_STARTED())
		{
			return null;
		}
		
		String htmltext = null;
		switch (event)
		{
			case "Participate":
			{
				if (canRegister(player))
				{
					if ((DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP == 0) || AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.L2EVENT_ID, player, DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP))
					{
						PLAYER_LIST.add(player);
						PLAYER_SCORES.put(player, 0);
						player.setRegisteredOnEvent(true);
						addLogoutListener(player);
						htmltext = "registration-success.html";
					}
					else
					{
						htmltext = "registration-ip.html";
					}
				}
				else
				{
					htmltext = "registration-failed.html";
				}
				break;
			}
			case "CancelParticipation":
			{
				if (player.isOnEvent())
				{
					return null;
				}
				
				// Remove the player from the IP count.
				if (DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
				{
					AntiFeedManager.getInstance().removePlayer(AntiFeedManager.L2EVENT_ID, player);
				}
				
				PLAYER_LIST.remove(player);
				PLAYER_SCORES.remove(player);
				removeListeners(player);
				player.setRegisteredOnEvent(false);
				htmltext = "registration-canceled.html";
				break;
			}
			case "BuffHeal":
			{
				if (player.isOnEvent() || player.isGM())
				{
					if (player.isInCombat())
					{
						htmltext = "manager-combat.html";
					}
					else
					{
						for (SkillHolder holder : GROUP_BUFFS)
						{
							SkillCaster.triggerCast(npc, player, holder.getSkill());
						}
						
						player.fullRestore();
					}
				}
				break;
			}
			case "TeleportToArena":
			{
				// Set state to STARTING.
				setState(EventState.STARTING);
				
				TEAM_FORFEIT = false;
				
				// Remove offline players.
				for (Player participant : PLAYER_LIST)
				{
					if ((participant == null) || (participant.isOnlineInt() != 1))
					{
						PLAYER_LIST.remove(participant);
						PLAYER_SCORES.remove(participant);
					}
				}
				
				// Check if there are enough players to start the event.
				if (PLAYER_LIST.size() < MINIMUM_PARTICIPANT_COUNT)
				{
					World.broadcastToAllOnlinePlayers("TvT Event: Event was canceled, not enough participants.");
					for (Player participant : PLAYER_LIST)
					{
						removeListeners(participant);
						participant.setRegisteredOnEvent(false);
					}
					
					// Set state INACTIVE
					setState(EventState.INACTIVE);
					return null;
				}
				
				// Create the instance.
				final InstanceManager manager = InstanceManager.getInstance();
				final InstanceTemplate template = manager.getInstanceTemplate(INSTANCE_ID);
				PVP_WORLD = manager.createInstance(template, null);
				
				// Make sure doors are closed.
				PVP_WORLD.getDoors().forEach(Door::closeMe);
				
				// Randomize player list and separate teams.
				final List<Player> playerList = new ArrayList<>(PLAYER_LIST.size());
				playerList.addAll(PLAYER_LIST);
				Collections.shuffle(playerList);
				PLAYER_LIST.clear();
				PLAYER_LIST.addAll(playerList);
				boolean team = getRandomBoolean(); // If teams are not even, randomize where extra player goes.
				for (Player participant : PLAYER_LIST)
				{
					participant.setOnEvent(true);
					participant.setRegisteredOnEvent(false);
					if (team)
					{
						BLUE_TEAM.add(participant);
						PVP_WORLD.addAllowed(participant);
						participant.leaveParty();
						participant.teleToLocation(BLUE_SPAWN_LOC, PVP_WORLD);
						participant.setTeam(Team.BLUE);
						team = false;
					}
					else
					{
						RED_TEAM.add(participant);
						PVP_WORLD.addAllowed(participant);
						participant.leaveParty();
						participant.teleToLocation(RED_SPAWN_LOC, PVP_WORLD);
						participant.setTeam(Team.RED);
						team = true;
					}
					
					addDeathListener(participant);
				}
				
				// Make Blue CC.
				if (BLUE_TEAM.size() > 1)
				{
					CommandChannel blueCC = null;
					Party lastBlueParty = null;
					int blueParticipantCounter = 0;
					for (Player participant : BLUE_TEAM)
					{
						blueParticipantCounter++;
						if (blueParticipantCounter == 1)
						{
							lastBlueParty = new Party(participant, PartyDistributionType.FINDERS_KEEPERS);
							participant.joinParty(lastBlueParty);
							if (BLUE_TEAM.size() > PARTY_MEMBER_COUNT)
							{
								if (blueCC == null)
								{
									blueCC = new CommandChannel(participant);
								}
								else
								{
									blueCC.addParty(lastBlueParty);
								}
							}
						}
						else
						{
							participant.joinParty(lastBlueParty);
						}
						
						if (blueParticipantCounter == PARTY_MEMBER_COUNT)
						{
							blueParticipantCounter = 0;
						}
					}
				}
				
				// Make Red CC.
				if (RED_TEAM.size() > 1)
				{
					CommandChannel redCC = null;
					Party lastRedParty = null;
					int redParticipantCounter = 0;
					for (Player participant : RED_TEAM)
					{
						redParticipantCounter++;
						if (redParticipantCounter == 1)
						{
							lastRedParty = new Party(participant, PartyDistributionType.FINDERS_KEEPERS);
							participant.joinParty(lastRedParty);
							if (RED_TEAM.size() > PARTY_MEMBER_COUNT)
							{
								if (redCC == null)
								{
									redCC = new CommandChannel(participant);
								}
								else
								{
									redCC.addParty(lastRedParty);
								}
							}
						}
						else
						{
							participant.joinParty(lastRedParty);
						}
						
						if (redParticipantCounter == PARTY_MEMBER_COUNT)
						{
							redParticipantCounter = 0;
						}
					}
				}
				
				// Spawn managers.
				addSpawn(MANAGER, BLUE_BUFFER_SPAWN_LOC, false, (WAIT_TIME + FIGHT_TIME) * 60000, false, PVP_WORLD.getId());
				addSpawn(MANAGER, RED_BUFFER_SPAWN_LOC, false, (WAIT_TIME + FIGHT_TIME) * 60000, false, PVP_WORLD.getId());
				
				// Initialize scores.
				BLUE_SCORE = 0;
				RED_SCORE = 0;
				
				// Initialize scoreboard.
				PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.INITIALIZE, MapUtil.sortByValue(PLAYER_SCORES, true)));
				
				// Schedule start.
				startQuestTimer("5", (WAIT_TIME * 60000) - 5000, null, null);
				startQuestTimer("4", (WAIT_TIME * 60000) - 4000, null, null);
				startQuestTimer("3", (WAIT_TIME * 60000) - 3000, null, null);
				startQuestTimer("2", (WAIT_TIME * 60000) - 2000, null, null);
				startQuestTimer("1", (WAIT_TIME * 60000) - 1000, null, null);
				startQuestTimer("StartFight", WAIT_TIME * 60000, null, null);
				break;
			}
			case "StartFight":
			{
				// Set state STARTED
				setState(EventState.STARTED);
				
				// Open doors.
				openDoor(BLUE_DOOR_ID, PVP_WORLD.getId());
				openDoor(RED_DOOR_ID, PVP_WORLD.getId());
				
				// add event FIGHT_TIME
				for (Player participant : PLAYER_LIST)
				{
					participant.sendPacket(new ExSendUIEvent(participant, false, false, (int) MINUTES.toSeconds(FIGHT_TIME), 10, NpcStringId.TIME_LEFT));
				}
				
				// Send message.
				broadcastScreenMessageWithEffect("The fight has began!", 5);
				
				// Schedule finish.
				startQuestTimer("10", (FIGHT_TIME * 60000) - 10000, null, null);
				startQuestTimer("9", (FIGHT_TIME * 60000) - 9000, null, null);
				startQuestTimer("8", (FIGHT_TIME * 60000) - 8000, null, null);
				startQuestTimer("7", (FIGHT_TIME * 60000) - 7000, null, null);
				startQuestTimer("6", (FIGHT_TIME * 60000) - 6000, null, null);
				startQuestTimer("5", (FIGHT_TIME * 60000) - 5000, null, null);
				startQuestTimer("4", (FIGHT_TIME * 60000) - 4000, null, null);
				startQuestTimer("3", (FIGHT_TIME * 60000) - 3000, null, null);
				startQuestTimer("2", (FIGHT_TIME * 60000) - 2000, null, null);
				startQuestTimer("1", (FIGHT_TIME * 60000) - 1000, null, null);
				startQuestTimer("EndFight", FIGHT_TIME * 60000, null, null);
				break;
			}
			case "EndFight":
			{
				// Close doors.
				closeDoor(BLUE_DOOR_ID, PVP_WORLD.getId());
				closeDoor(RED_DOOR_ID, PVP_WORLD.getId());
				
				// Disable players.
				for (Player participant : PLAYER_LIST)
				{
					participant.setInvul(true);
					participant.setImmobilized(true);
					participant.disableAllSkills();
					for (Summon summon : participant.getServitors().values())
					{
						summon.setInvul(true);
						summon.setImmobilized(true);
						summon.disableAllSkills();
					}
				}
				
				// Make sure noone is dead.
				for (Player participant : PLAYER_LIST)
				{
					if (participant.isDead())
					{
						participant.doRevive();
					}
				}
				
				// Team wins by Forfeit.
				if (TEAM_FORFEIT)
				{
					Set<Player> TeamWinner = (BLUE_TEAM.isEmpty() && !RED_TEAM.isEmpty() ? RED_TEAM : BLUE_TEAM);
					
					final Skill skill = CommonSkill.FIREWORK.getSkill();
					broadcastScreenMessageWithEffect("Team " + (TeamWinner == BLUE_TEAM ? "Blue" : "Red") + " won the event by forfeit!", 7);
					for (Player participant : TeamWinner)
					{
						if ((participant != null) && (participant.getInstanceWorld() == PVP_WORLD))
						{
							participant.broadcastPacket(new MagicSkillUse(participant, participant, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
							participant.broadcastSocialAction(3);
							giveItems(participant, REWARD);
						}
					}
				}
				// Team Blue wins.
				else if ((BLUE_SCORE > RED_SCORE) && (!TEAM_FORFEIT))
				{
					final Skill skill = CommonSkill.FIREWORK.getSkill();
					broadcastScreenMessageWithEffect("Team Blue won the event!", 7);
					for (Player participant : BLUE_TEAM)
					{
						if ((participant != null) && (participant.getInstanceWorld() == PVP_WORLD))
						{
							participant.broadcastPacket(new MagicSkillUse(participant, participant, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
							participant.broadcastSocialAction(3);
							giveItems(participant, REWARD);
						}
					}
				}
				// Team Red wins.
				else if ((RED_SCORE > BLUE_SCORE) && (!TEAM_FORFEIT))
				{
					final Skill skill = CommonSkill.FIREWORK.getSkill();
					broadcastScreenMessageWithEffect("Team Red won the event!", 7);
					for (Player participant : RED_TEAM)
					{
						if ((participant != null) && (participant.getInstanceWorld() == PVP_WORLD))
						{
							participant.broadcastPacket(new MagicSkillUse(participant, participant, skill.getId(), skill.getLevel(), skill.getHitTime(), skill.getReuseDelay()));
							participant.broadcastSocialAction(3);
							giveItems(participant, REWARD);
						}
					}
				}
				// Tie.
				else
				{
					broadcastScreenMessageWithEffect("The event ended with a tie!", 7);
					for (Player participant : PLAYER_LIST)
					{
						participant.broadcastSocialAction(13);
					}
				}
				
				startQuestTimer("ScoreBoard", 3500, null, null);
				startQuestTimer("TeleportOut", 7000, null, null);
				break;
			}
			case "ScoreBoard":
			{
				PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.FINISH, MapUtil.sortByValue(PLAYER_SCORES, true)));
				
				// remove event FIGHT_TIME
				for (Player participant : PLAYER_LIST)
				{
					participant.sendPacket(new ExSendUIEvent(participant, false, false, 0, 0, NpcStringId.TIME_LEFT));
				}
				break;
			}
			case "TeleportOut":
			{
				TEAM_FORFEIT = false;
				
				// Remove event listeners.
				for (Player participant : PLAYER_LIST)
				{
					removeListeners(participant);
					participant.setTeam(Team.NONE);
					participant.setOnEvent(false);
					participant.leaveParty();
				}
				
				// Destroy world.
				if (PVP_WORLD != null)
				{
					PVP_WORLD.destroy();
					PVP_WORLD = null;
				}
				
				// Enable players.
				for (Player participant : PLAYER_LIST)
				{
					participant.setInvul(false);
					participant.setImmobilized(false);
					participant.enableAllSkills();
					for (Summon summon : participant.getServitors().values())
					{
						summon.setInvul(false);
						summon.setImmobilized(false);
						summon.enableAllSkills();
					}
				}
				
				// Set state INACTIVE
				setState(EventState.INACTIVE);
				break;
			}
			case "ResurrectPlayer":
			{
				if (player.isDead() && player.isOnEvent())
				{
					if (BLUE_TEAM.contains(player))
					{
						player.setIsPendingRevive(true);
						player.teleToLocation(BLUE_SPAWN_LOC, false, player.getInstanceWorld());
						
						// Make player invulnerable for 30 seconds.
						GHOST_WALKING.getSkill().applyEffects(player, player);
						
						// Reset existing activity timers.
						resetActivityTimers(player); // In case player died in peace zone.
					}
					else if (RED_TEAM.contains(player))
					{
						player.setIsPendingRevive(true);
						player.teleToLocation(RED_SPAWN_LOC, false, player.getInstanceWorld());
						
						// Make player invulnerable for 30 seconds.
						GHOST_WALKING.getSkill().applyEffects(player, player);
						
						// Reset existing activity timers.
						resetActivityTimers(player); // In case player died in peace zone.
					}
				}
				break;
			}
			case "10":
			case "9":
			case "8":
			case "7":
			case "6":
			case "5":
			case "4":
			case "3":
			case "2":
			case "1":
			{
				broadcastScreenMessage(event, 4);
				break;
			}
			case "manager-cancel":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, HTML_PATH + "manager-cancel.html");
				html.replace("%player_numbers%", String.valueOf(PLAYER_LIST.size()));
				player.sendPacket(html);
				break;
			}
			case "manager-register":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, HTML_PATH + "manager-register.html");
				html.replace("%player_numbers%", String.valueOf(PLAYER_LIST.size()));
				player.sendPacket(html);
				break;
			}
		}
		
		// Activity timer.
		if (event.startsWith("KickPlayer") && (player != null) && (player.getInstanceWorld() == PVP_WORLD))
		{
			if (event.contains("Warning"))
			{
				sendScreenMessage(player, "You have been marked as inactive!", 10);
			}
			else
			{
				player.setTeam(Team.NONE);
				PVP_WORLD.ejectPlayer(player);
				PLAYER_LIST.remove(player);
				PLAYER_SCORES.remove(player);
				BLUE_TEAM.remove(player);
				RED_TEAM.remove(player);
				player.setOnEvent(false);
				removeListeners(player);
				player.sendMessage("You have been kicked for been inactive.");
				if (PVP_WORLD != null)
				{
					// Manage forfeit.
					if ((BLUE_TEAM.isEmpty() && !RED_TEAM.isEmpty()) || //
						(RED_TEAM.isEmpty() && !BLUE_TEAM.isEmpty()))
					{
						manageForfeit();
					}
					else
					{
						broadcastScreenMessageWithEffect("Player " + player.getName() + " was kicked for been inactive!", 7);
					}
				}
				
				player.sendPacket(new ExSendUIEvent(player, false, false, 0, 0, NpcStringId.TIME_LEFT));
				player.sendPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.FINISH, MapUtil.sortByValue(PLAYER_SCORES, true)));
			}
		}
		
		if (event.startsWith("RegistrationWarn:"))
		{
			final int minutesLeft = Integer.parseInt(event.split(":")[1]);
			World.broadcastToAllOnlinePlayers("TvT Event: Registration opened for " + minutesLeft + " minutes.");
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * It checks if the player is registered and returns the appropriate HTML file.<br>
	 * If the player is not participating, it returns {@code null}.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the HTML file to display, or {@code null} if no action is taken.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// Event not participating, no starting or started return null.
		if (!IS_PARTICIPATING() && !IS_STARTING() && !IS_STARTED())
		{
			return null;
		}
		
		// Player has already registered.
		if (PLAYER_LIST.contains(player))
		{
			// Npc is in instance.
			if (npc.getInstanceWorld() != null)
			{
				return "manager-buffheal.html";
			}
			
			startQuestTimer("manager-cancel", 5, npc, player);
			return "manager-cancel.html";
		}
		
		// Player is not registered.
		startQuestTimer("manager-register", 5, npc, player);
		return "manager-register.html";
	}
	
	/**
	 * Handles logic when a {@code Creature} enters a specific {@code ZoneType}.<br>
	 * It checks if the creature is a player participating in an event.<br>
	 * Players are teleported back if they enter enemy peace zones.<br>
	 * Activity timers are reset for players entering their own team's peace zones.
	 * @param creature The {@code Creature} that entered the zone.
	 * @param zone The {@code ZoneType} that was entered.
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayable())
		{
			final Player player = creature.asPlayer();
			if (player.isOnEvent())
			{
				// Kick enemy players.
				if ((zone == BLUE_PEACE_ZONE) && (creature.getTeam() == Team.RED))
				{
					creature.teleToLocation(RED_SPAWN_LOC, creature.getInstanceWorld());
					sendScreenMessage(player, "Entering the enemy headquarters is prohibited!", 10);
				}
				
				if ((zone == RED_PEACE_ZONE) && (creature.getTeam() == Team.BLUE))
				{
					creature.teleToLocation(BLUE_SPAWN_LOC, creature.getInstanceWorld());
					sendScreenMessage(player, "Entering the enemy headquarters is prohibited!", 10);
				}
				
				// Start inactivity check.
				if (creature.isPlayer() && //
					(((zone == BLUE_PEACE_ZONE) && (creature.getTeam() == Team.BLUE)) || //
						((zone == RED_PEACE_ZONE) && (creature.getTeam() == Team.RED))))
				{
					resetActivityTimers(player);
				}
			}
		}
	}
	
	/**
	 * Handles logic when a {@code Creature} leaves a specific {@code ZoneType}.<br>
	 * This method checks if the creature is a {@code Player} participating in an event.<br>
	 * It cancels specific quest timers and removes the ghost walking effect for that player.
	 * @param creature The {@code Creature} that exited the zone.
	 * @param zone The {@code ZoneType} that was exited.
	 */
	@Override
	public void onExitZone(Creature creature, ZoneType zone)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (player.isOnEvent())
			{
				final int objectId = player.getObjectId();
				cancelQuestTimer("KickPlayer" + objectId, null, player);
				cancelQuestTimer("KickPlayerWarning" + objectId, null, player);
				
				// Removed invulnerability shield.
				if (player.isAffectedBySkill(GHOST_WALKING))
				{
					player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, GHOST_WALKING.getSkill());
				}
			}
		}
	}
	
	/**
	 * Checks if a {@code Player} meets all requirements to join the event.<br>
	 * It validates level, inventory weight, and current status like flying or dueling.<br>
	 * The method returns {@code false} if any condition is not met.
	 * @param player The {@code Player} to check for registration eligibility.
	 * @return {@code true} if the player can register, otherwise {@code false}.
	 */
	private boolean canRegister(Player player)
	{
		if (PLAYER_LIST.contains(player))
		{
			player.sendMessage("You are already registered on this event.");
			return false;
		}
		
		final int level = player.getLevel();
		if (level < MINIMUM_PARTICIPANT_LEVEL)
		{
			player.sendMessage("Your level is too low to participate.");
			return false;
		}
		
		if (level > MAXIMUM_PARTICIPANT_LEVEL)
		{
			player.sendMessage("Your level is too high to participate.");
			return false;
		}
		
		if (player.isRegisteredOnEvent() || (player.getBlockCheckerArena() > -1))
		{
			player.sendMessage("You are already registered on an event.");
			return false;
		}
		
		if (PLAYER_LIST.size() >= MAXIMUM_PARTICIPANT_COUNT)
		{
			player.sendMessage("There are too many players registered on the event.");
			return false;
		}
		
		if (player.isFlyingMounted())
		{
			player.sendMessage("You cannot register on the event while flying.");
			return false;
		}
		
		if (player.isTransformed())
		{
			player.sendMessage("You cannot register on the event while on a transformed state.");
			return false;
		}
		
		if (!player.isInventoryUnder80(false))
		{
			player.sendMessage("There are too many items in your inventory.");
			player.sendMessage("Try removing some items.");
			return false;
		}
		
		if ((player.getWeightPenalty() != 0))
		{
			player.sendMessage("Your invetory weight has exceeded the normal limit.");
			player.sendMessage("Try removing some items.");
			return false;
		}
		
		if (player.isCursedWeaponEquipped() || (player.getReputation() < 0))
		{
			player.sendMessage("People with bad reputation can't register.");
			return false;
		}
		
		if (player.isInDuel())
		{
			player.sendMessage("You cannot register while on a duel.");
			return false;
		}
		
		if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("You cannot participate while registered on the Olympiad.");
			return false;
		}
		
		if (player.isInInstance())
		{
			player.sendMessage("You cannot register while in an instance.");
			return false;
		}
		
		if (player.isInSiege() || player.isInsideZone(ZoneId.SIEGE))
		{
			player.sendMessage("You cannot register while on a siege.");
			return false;
		}
		
		if (player.isFishing())
		{
			player.sendMessage("You cannot register while fishing.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Sends a message to the player's screen.<br>
	 * The message appears in the center of the top area.
	 * @param player The {@code Player} who will receive the message.
	 * @param message The text content to display on the screen.
	 * @param duration The time in seconds for which the message should be visible.
	 */
	private void sendScreenMessage(Player player, String message, int duration)
	{
		player.sendPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, false));
	}
	
	/**
	 * Sends a message to the center of the screen for all players in {@code PVP_WORLD}.<br>
	 * The message will be displayed for a specific amount of time.
	 * @param message The text content to display on the screen.
	 * @param duration The time in seconds that the message should remain visible.
	 */
	private void broadcastScreenMessage(String message, int duration)
	{
		PVP_WORLD.broadcastPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, false));
	}
	
	/**
	 * Sends a visual message to all players in the {@code PVP_WORLD}.<br>
	 * This method includes a special visual effect.
	 * @param message The text content to display on the screen.
	 * @param duration The time in seconds that the message should remain visible.
	 */
	private void broadcastScreenMessageWithEffect(String message, int duration)
	{
		PVP_WORLD.broadcastPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, true));
	}
	
	/**
	 * Sends a score update message to all players in the world.<br>
	 * This method displays the current scores for both teams on the screen.<br>
	 * The message appears at the bottom right for 15000 milliseconds.
	 */
	private void broadcastScoreMessage()
	{
		PVP_WORLD.broadcastPacket(new ExShowScreenMessage("Blue: " + BLUE_SCORE + " - Red: " + RED_SCORE, ExShowScreenMessage.BOTTOM_RIGHT, 15000, 0, true, false));
	}
	
	/**
	 * Registers a listener for the {@code ON_PLAYER_LOGOUT} event.<br>
	 * This ensures that {@code onPlayerLogout} is called when the player leaves.
	 * @param player The {@code Player} to attach the listener to.
	 */
	private void addLogoutListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
	}
	
	/**
	 * Registers a listener to track when the specified player dies.<br>
	 * This allows the system to trigger {@code onPlayerDeath} logic.
	 * @param player The {@code Player} instance to monitor.
	 */
	private void addDeathListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onPlayerDeath(event), this));
	}
	
	/**
	 * Removes the event listeners associated with this class from a specific player.<br>
	 * It unregisters listeners for both {@code ON_PLAYER_LOGOUT} and {@code ON_CREATURE_DEATH} events.
	 * @param player The {@code Player} object to remove listeners from.
	 */
	private void removeListeners(Player player)
	{
		for (AbstractEventListener listener : player.getListeners(EventType.ON_PLAYER_LOGOUT))
		{
			if (listener.getOwner() == this)
			{
				listener.unregisterMe();
			}
		}
		
		for (AbstractEventListener listener : player.getListeners(EventType.ON_CREATURE_DEATH))
		{
			if (listener.getOwner() == this)
			{
				listener.unregisterMe();
			}
		}
	}
	
	/**
	 * Resets the activity timers for a specific {@code Player}.<br>
	 * This method cancels existing kick and warning timers.<br>
	 * It then restarts them based on current inactivity settings.
	 * @param player The {@code Player} whose timers need to be reset.
	 */
	private void resetActivityTimers(Player player)
	{
		final int objectId = player.getObjectId();
		cancelQuestTimer("KickPlayer" + objectId, null, player);
		cancelQuestTimer("KickPlayerWarning" + objectId, null, player);
		startQuestTimer("KickPlayer" + objectId, IS_STARTED() ? INACTIVITY_TIME * 60000 : (INACTIVITY_TIME * 60000) + (WAIT_TIME * 60000), null, player);
		startQuestTimer("KickPlayerWarning" + objectId, IS_STARTED() ? (INACTIVITY_TIME / 2) * 60000 : ((INACTIVITY_TIME / 2) * 60000) + (WAIT_TIME * 60000), null, player);
	}
	
	/**
	 * Handles the logic for when a team forfeits the match.<br>
	 * Sets the {@code TEAM_FORFEIT} flag to {@code true}.<br>
	 * Cancels all active countdown timers and starts the final end fight timer.<br>
	 * Broadcasts a forfeit message to all players.
	 */
	private void manageForfeit()
	{
		TEAM_FORFEIT = true;
		cancelQuestTimer("10", null, null);
		cancelQuestTimer("9", null, null);
		cancelQuestTimer("8", null, null);
		cancelQuestTimer("7", null, null);
		cancelQuestTimer("6", null, null);
		cancelQuestTimer("5", null, null);
		cancelQuestTimer("4", null, null);
		cancelQuestTimer("3", null, null);
		cancelQuestTimer("2", null, null);
		cancelQuestTimer("1", null, null);
		cancelQuestTimer("EndFight", null, null);
		startQuestTimer("EndFight", 10000, null, null);
		broadcastScreenMessageWithEffect("Enemy team forfeit!", 7);
	}
	
	/**
	 * Handles the logic when a player logs out of the game.<br>
	 * This method removes the player from all active team lists and scores.<br>
	 * It also checks if the remaining players trigger a forfeit condition.
	 * @param event The {@code OnPlayerLogout} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		
		// Remove player from lists.
		PLAYER_LIST.remove(player);
		PLAYER_SCORES.remove(player);
		BLUE_TEAM.remove(player);
		RED_TEAM.remove(player);
		
		if (IS_STARTED())
		{
			player.sendPacket(new ExSendUIEvent(player, false, false, 0, 0, NpcStringId.TIME_LEFT));
		}
		
		// Manage forfeit.
		final boolean blueTeamEmpty = BLUE_TEAM.isEmpty();
		final boolean redTeamEmpty = RED_TEAM.isEmpty();
		if ((blueTeamEmpty && !redTeamEmpty) || //
			(redTeamEmpty && !blueTeamEmpty))
		{
			manageForfeit();
		}
	}
	
	/**
	 * This method handles the logic when a player dies during a match.<br>
	 * It checks if the kill was between opposing teams.<br>
	 * If it was a valid team kill, it updates the scores and broadcasts the results.<br>
	 * It also starts a resurrection timer for the killed player.
	 * @param event The {@code OnCreatureDeath} event containing the attacker and target information.
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	public void onPlayerDeath(OnCreatureDeath event)
	{
		if (event.getTarget().isPlayer())
		{
			final Player killedPlayer = event.getTarget().asPlayer();
			final Player killer = event.getAttacker().asPlayer();
			final Team killerTeam = killer.getTeam();
			final Team killedTeam = killedPlayer.getTeam();
			
			// Confirm Blue team kill.
			if ((killerTeam == Team.BLUE) && (killedTeam == Team.RED))
			{
				PLAYER_SCORES.put(killer, PLAYER_SCORES.get(killer) + 1);
				BLUE_SCORE++;
				broadcastScoreMessage();
				PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.UPDATE, MapUtil.sortByValue(PLAYER_SCORES, true)));
			}
			
			// Confirm Red team kill.
			if ((killerTeam == Team.RED) && (killedTeam == Team.BLUE))
			{
				PLAYER_SCORES.put(killer, PLAYER_SCORES.get(killer) + 1);
				RED_SCORE++;
				broadcastScoreMessage();
				PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.UPDATE, MapUtil.sortByValue(PLAYER_SCORES, true)));
			}
			
			// Auto release after 10 seconds.
			startQuestTimer("ResurrectPlayer", 10000, null, killedPlayer);
		}
	}
	
	/**
	 * Starts the Team Vs Team event.<br>
	 * This method sets the state to {@code PARTICIPATING}.<br>
	 * It clears previous timers and player data.<br>
	 * It spawns the manager NPC and starts the registration timer.
	 * @param eventMaker The {@code Player} who initiated the event start.
	 * @return {@code true} if the event started successfully.
	 */
	@Override
	public boolean eventStart(Player eventMaker)
	{
		// Set state PARTICIPATING
		setState(EventState.PARTICIPATING);
		
		// Cancel timers. (In case event started immediately after another event was canceled.).
		for (List<QuestTimer> timers : getQuestTimers().values())
		{
			for (QuestTimer timer : timers)
			{
				timer.cancel();
			}
		}
		
		// Register the event at AntiFeedManager and clean it for just in case if the event is already registered.
		if (DualboxCheckConfig.DUALBOX_CHECK_MAX_L2EVENT_PARTICIPANTS_PER_IP > 0)
		{
			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.L2EVENT_ID);
			AntiFeedManager.getInstance().clear(AntiFeedManager.L2EVENT_ID);
		}
		
		// Clear player lists.
		PLAYER_LIST.clear();
		PLAYER_SCORES.clear();
		BLUE_TEAM.clear();
		RED_TEAM.clear();
		
		// Spawn event manager.
		MANAGER_NPC_INSTANCE = addSpawn(MANAGER, MANAGER_SPAWN_LOC, false, REGISTRATION_TIME * 60000);
		startQuestTimer("TeleportToArena", REGISTRATION_TIME * 60000, null, null);
		
		// Send message to players.
		World.broadcastToAllOnlinePlayers("TvT Event: Registration opened for " + REGISTRATION_TIME + " minutes.");
		World.broadcastToAllOnlinePlayers("TvT Event: You can register at Giran TvT Event Manager.");
		
		// @formatter:off
		final int[] warnings = {10, 5, 4, 3, 2, 1};
		// @formatter:on
		for (int warn : warnings)
		{
			if (REGISTRATION_TIME > warn)
			{
				final long delay = (REGISTRATION_TIME - warn) * 60000L;
				startQuestTimer("RegistrationWarn:" + warn, delay, null, null);
			}
		}
		
		return true;
	}
	
	/**
	 * Stops the current event and cleans up all resources.<br>
	 * This method deletes the manager NPC and cancels all active timers.<br>
	 * It also resets participant statuses and destroys the PVP world.
	 * @return {@code true} if the event was successfully stopped.
	 */
	@Override
	public boolean eventStop()
	{
		// Despawn event manager.
		MANAGER_NPC_INSTANCE.deleteMe();
		
		// Cancel timers.
		for (List<QuestTimer> timers : getQuestTimers().values())
		{
			for (QuestTimer timer : timers)
			{
				timer.cancel();
			}
		}
		
		// Remove participants.
		for (Player participant : PLAYER_LIST)
		{
			removeListeners(participant);
			participant.setTeam(Team.NONE);
			participant.setRegisteredOnEvent(false);
			participant.setOnEvent(false);
			participant.setInvul(false);
			participant.setImmobilized(false);
			participant.enableAllSkills();
			for (Summon summon : participant.getServitors().values())
			{
				summon.setInvul(false);
				summon.setImmobilized(false);
				summon.enableAllSkills();
			}
			
			if (IS_STARTED())
			{
				participant.sendPacket(new ExSendUIEvent(participant, false, false, 0, 0, NpcStringId.TIME_LEFT));
			}
		}
		
		if (IS_STARTED())
		{
			PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.FINISH, MapUtil.sortByValue(PLAYER_SCORES, true)));
		}
		
		if (PVP_WORLD != null)
		{
			PVP_WORLD.destroy();
			PVP_WORLD = null;
		}

		// Clear all lists.
        PLAYER_LIST.clear();
        PLAYER_SCORES.clear();
        BLUE_TEAM.clear();
        RED_TEAM.clear();

		// Send message to players.
		World.broadcastToAllOnlinePlayers("TvT Event: Event was canceled.");
		
		// Set state PARTICIPATING
		setState(EventState.INACTIVE);
		return true;
	}
	
	/**
	 * Updates the current state of the event.<br>
	 * This method uses a synchronized block to ensure thread safety.
	 * @param state The new {@code EventState} to set.
	 */
	public static void setState(EventState state)
	{
		synchronized (_state)
		{
			_state = state;
		}
	}
	
	/**
	 * Checks if the current event state is inactive.<br>
	 * This method returns {@code true} if the status is {@code INACTIVE}.
	 * @return {@code true} if the event is inactive, {@code false} otherwise.
	 */
	public static boolean IS_INACTIVE()
	{
		boolean isInactive;
		synchronized (_state)
		{
			isInactive = _state == EventState.INACTIVE;
		}
		
		return isInactive;
	}
	
	/**
	 * Checks if the current event state is active.<br>
	 * This method returns {@code true} if players are currently participating in the event.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if participating, {@code false} otherwise.
	 */
	public static boolean IS_PARTICIPATING()
	{
		boolean isParticipating;
		synchronized (_state)
		{
			isParticipating = _state == EventState.PARTICIPATING;
		}
		
		return isParticipating;
	}
	
	/**
	 * Checks if the event is currently in the starting phase.<br>
	 * This method accesses the internal state to verify if it matches {@code EventState.STARTING}.
	 * @return {@code true} if the event is starting, {@code false} otherwise.
	 */
	public static boolean IS_STARTING()
	{
		boolean isStarting;
		synchronized (_state)
		{
			isStarting = _state == EventState.STARTING;
		}
		
		return isStarting;
	}
	
	/**
	 * Checks if the event has currently started.<br>
	 * This method returns {@code true} if the state is {@code STARTED}.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the event is active, {@code false} if it is not.
	 */
	public static boolean IS_STARTED()
	{
		boolean isStarted;
		synchronized (_state)
		{
			isStarted = _state == EventState.STARTED;
		}
		
		return isStarted;
	}
	
	/**
	 * Checks if a specific player is allowed to bypass an event.<br>
	 * This method currently always returns {@code false}.
	 * @param player The {@code Player} object to check.
	 * @param bypass The name of the bypass to verify.
	 * @return {@code false} for all requests.
	 */
	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		onEvent(bypass, null, player);
        return true;
	}
	
	/**
	 * This is the main entry point for the application.<br>
	 * It initializes the {@code TvT} class to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new TvT();
	}

    public EventState getEventState()
    {
        return _state;
    }

    public int getRegisteredCount()
    {
        return PLAYER_LIST.size();
    }

    public boolean isPlayerRegistered(Player player)
    {
        return PLAYER_LIST.contains(player);
    }

    public int getMinimumParticipants()
    {
        return MINIMUM_PARTICIPANT_COUNT;
    }

    public int getMaximumParticipants()
    {
        return MAXIMUM_PARTICIPANT_COUNT;
    }

    public int getMinimumLevel()
    {
        return MINIMUM_PARTICIPANT_LEVEL;
    }

    public int getMaximumLevel()
    {
        return MAXIMUM_PARTICIPANT_LEVEL;
    }

    public int getBlueScore()
    {
        return BLUE_SCORE;
    }

    public int getRedScore()
    {
        return RED_SCORE;
    }
}
