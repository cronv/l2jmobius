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
package custom.events.CaptureTheFlag;

import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.threads.ThreadPool;
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
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ItemManager;
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
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.MapUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the logic for a Capture The Flag game event.<br>
 * This class handles team interactions, flag mechanics, and win conditions within the {@code Event} framework.
 * @author BraveHeart
 * @inspired by Mobius Team vs Team
 */
public class CtF extends Event
{
	enum EventState
	{
		INACTIVE,
		PARTICIPATING,
		STARTING,
		STARTED
	}
	
	private static final String HTML_PATH = "data/scripts/custom/events/CaptureTheFlag/";
	
	// NPC
	private static final int MANAGER = 70012;
	
	// Skills
	private static final SkillHolder[] FIGHTER_BUFFS =
	{
		new SkillHolder(4322, 1), // Wind Walk
		new SkillHolder(4323, 1), // Shield
		new SkillHolder(5637, 1), // Magic Barrier
		new SkillHolder(4324, 1), // Bless the Body
		new SkillHolder(4325, 1), // Vampiric Rage
		new SkillHolder(4326, 1), // Regeneration
		new SkillHolder(5632, 1), // Haste
	};
	private static final SkillHolder[] MAGE_BUFFS =
	{
		new SkillHolder(4322, 1), // Wind Walk
		new SkillHolder(4323, 1), // Shield
		new SkillHolder(5637, 1), // Magic Barrier
		new SkillHolder(4328, 1), // Bless the Soul
		new SkillHolder(4329, 1), // Acumen
		new SkillHolder(4330, 1), // Concentration
		new SkillHolder(4331, 1), // Empower
	};
	
	private static final SkillHolder GHOST_WALKING = new SkillHolder(100000, 1); // Custom Ghost Walking
	/** The state of the Ctf. */
	private static EventState _state = EventState.INACTIVE;
	
	// Others
	private static final int INSTANCE_ID = 3050;
	private static final int BLUE_DOOR_ID = 24190002;
	private static final int RED_DOOR_ID = 24190003;
	/** Blue Flag NPC ID */
	private static final int BLUE_TEAM_HEADQUARTERS = 82016;
	/** Blue Flag x,y,z[,heading] Location. */
	private static final Location BLUE_TEAM_HEADQUARTERS_LOC = new Location(148487, 46703, -3414, 297);
	/** Blue Flag Item id */
	private static final int BLUE_TEAM_FLAG_ITEM_ID = 13531;
	/** Red Flag NPC ID */
	private static final int RED_TEAM_HEADQUARTERS = 82017;
	/** Red Flag x,y,z[,heading] location. */
	private static final Location RED_TEAM_HEADQUARTERS_LOC = new Location(150452, 46714, -3414, 34490);
	/** red Flag Item ID */
	private static final int RED_TEAM_FLAG_ITEM_ID = 13534;
	/** The NPC instance of Blue Flag. */
	private static Npc FLAG_BLUE_SPAWN = null;
	/** The Blue Team flag carrier Player. */
	private static Player BLUE_TEAM_CARRIER = null;
	/** The Blue Team flag carrier right hand item. */
	private static Item BLUE_TEAM_CARRIER_R_HAND = null;
	/** The Blue Team flag carrier left hand item. */
	private static Item BLUE_TEAM_CARRIER_L_HAND = null;
	/** The NPC instance of Red Flag */
	private static Npc FLAG_RED_SPAWN = null;
	/** The Red Team flag carrier Player. */
	private static Player RED_TEAM_CARRIER = null;
	/** The Red Team flag carrier right hand item. */
	private static Item RED_TEAM_CARRIER_R_HAND = null;
	/** The Red Team flag carrier left hand item. */
	private static Item RED_TEAM_CARRIER_L_HAND = null;
	private static final Location MANAGER_SPAWN_LOC = new Location(83425, 148585, -3406, 32938);
	private static final Location BLUE_BUFFER_SPAWN_LOC = new Location(147450, 46913, -3400, 49000);
	private static final ZoneForm BLUE_SPAWN_LOC = ZoneManager.getInstance().getZoneByName("blue_team_spawn").getZone();
	private static final ZoneType BLUE_PEACE_ZONE = ZoneManager.getInstance().getZoneByName("colosseum_peace1");
	private static final Location RED_BUFFER_SPAWN_LOC = new Location(151545, 46528, -3400, 16000);
	private static final ZoneForm RED_SPAWN_LOC = ZoneManager.getInstance().getZoneByName("red_team_spawn").getZone();
	private static final ZoneType RED_PEACE_ZONE = ZoneManager.getInstance().getZoneByName("colosseum_peace2");
	
	// Settings
	private static final int REGISTRATION_TIME = 10; // Minutes
	private static final int WAIT_TIME = 1; // Minutes
	private static final int FIGHT_TIME = 20; // Minutes
	private static final int INACTIVITY_TIME = 2; // Minutes
	private static final int MINIMUM_PARTICIPANT_LEVEL = 76;
	private static final int MAXIMUM_PARTICIPANT_LEVEL = 200;
	private static final int MINIMUM_PARTICIPANT_COUNT = 4;
	private static final int MAXIMUM_PARTICIPANT_COUNT = 24; // Scoreboard has 25 slots
	private static final int PARTY_MEMBER_COUNT = 7;
	private static final ItemHolder REWARD = new ItemHolder(57, 100000); // Adena
	
	// Misc
	private static final Map<Player, Integer> PLAYER_SCORES = new ConcurrentHashMap<>();
	private static final Set<Player> PLAYER_LIST = ConcurrentHashMap.newKeySet();
	private static final String BLUE_TEAM_NAME = "Blue";
	private static final Set<Player> BLUE_TEAM = ConcurrentHashMap.newKeySet();
	private static final String RED_TEAM_NAME = "Red";
	private static final Set<Player> RED_TEAM = ConcurrentHashMap.newKeySet();
	private static volatile int BLUE_SCORE;
	private static volatile int RED_SCORE;
	private static Instance PVP_WORLD = null;
	private static Npc MANAGER_NPC_INSTANCE = null;
	private static boolean TEAM_FORFEIT = false;
	private final Lock BLUE_LOCK = new ReentrantLock();
	private final Lock RED_LOCK = new ReentrantLock();
	
	/**
	 * Private constructor for the {@code CtF} class.<br>
	 * It initializes the required talk IDs and zone IDs.<br>
	 * It also calls {@code loadConfig()} to prepare the game settings.
	 */
	private CtF()
	{
		addTalkId(MANAGER, BLUE_TEAM_HEADQUARTERS, RED_TEAM_HEADQUARTERS);
		addFirstTalkId(MANAGER, BLUE_TEAM_HEADQUARTERS, RED_TEAM_HEADQUARTERS);
		final int BLUE_PEACE_ZONEId = BLUE_PEACE_ZONE.getId();
		final int RED_PEACE_ZONEId = RED_PEACE_ZONE.getId();
		addExitZoneId(BLUE_PEACE_ZONEId, RED_PEACE_ZONEId);
		addEnterZoneId(BLUE_PEACE_ZONEId, RED_PEACE_ZONEId);
		
		loadConfig();
	}
	
	/**
	 * Loads the configuration from the XML file.<br>
	 * This method parses the data and schedules events based on the patterns provided in the config.
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
	 * Handles events triggered by a timer.<br>
	 * This method checks if the {@code event} starts with {@code Schedule}.<br>
	 * If it does, it starts the event and adds a new timer to the system.
	 * @param event The name or type of the timer event.
	 * @param params The configuration parameters for the event.
	 * @param npc The {@code Npc} associated with this event.
	 * @param player The {@code Player} associated with this event.
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
	 * Handles specific game events for the Capture The Flag activity.<br>
	 * It processes logic for registration, team assignment, and combat initialization.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} interacting with the player.
	 * @param player The {@code Player} involved in the action.
	 * @return A string containing the HTML file name for the UI or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		// Event not participating, no starting or started return null.
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
						if (player.isMageClass())
						{
							for (SkillHolder skill : MAGE_BUFFS)
							{
								SkillCaster.triggerCast(npc, player, skill.getSkill());
							}
						}
						else
						{
							for (SkillHolder skill : FIGHTER_BUFFS)
							{
								SkillCaster.triggerCast(npc, player, skill.getSkill());
							}
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
					World.broadcastToAllOnlinePlayers("CtF Event: Event was canceled, not enough participants.");
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
						participant.teleToLocation(BLUE_SPAWN_LOC.getRandomPoint(), PVP_WORLD);
						participant.setTeam(Team.BLUE);
						team = false;
					}
					else
					{
						RED_TEAM.add(participant);
						PVP_WORLD.addAllowed(participant);
						participant.leaveParty();
						participant.teleToLocation(RED_SPAWN_LOC.getRandomPoint(), PVP_WORLD);
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
				
				// Spawn the flags
				FLAG_BLUE_SPAWN = addSpawn(BLUE_TEAM_HEADQUARTERS, BLUE_TEAM_HEADQUARTERS_LOC, false, (WAIT_TIME + FIGHT_TIME) * 60000, false, PVP_WORLD.getId());
				FLAG_RED_SPAWN = addSpawn(RED_TEAM_HEADQUARTERS, RED_TEAM_HEADQUARTERS_LOC, false, (WAIT_TIME + FIGHT_TIME) * 60000, false, PVP_WORLD.getId());
				
				// add event FIGHT_TIME
				for (Player participant : PLAYER_LIST)
				{
					participant.sendPacket(new ExSendUIEvent(participant, false, false, (int) MINUTES.toSeconds(FIGHT_TIME), 10, NpcStringId.TIME_LEFT));
				}
				
				// Send message.
				broadcastScreenMessageWithEffect("Capture The Flag Event Started - Go to flags!", 5);
				
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
				
				// Reset flag carriers
				if (BLUE_TEAM_CARRIER != null)
				{
					removeFlagCarrier(BLUE_TEAM_CARRIER);
				}
				
				if (RED_TEAM_CARRIER != null)
				{
					removeFlagCarrier(RED_TEAM_CARRIER);
				}
				
				// Remove Headquarters team Blue.
				if (FLAG_BLUE_SPAWN != null)
				{
					FLAG_BLUE_SPAWN.deleteMe();
					FLAG_BLUE_SPAWN = null;
				}
				
				// Remove Headquarters team Red.
				if (FLAG_RED_SPAWN != null)
				{
					FLAG_RED_SPAWN.deleteMe();
					FLAG_RED_SPAWN = null;
				}
				
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
						player.teleToLocation(BLUE_SPAWN_LOC.getRandomPoint(), false, player.getInstanceWorld());
						
						// Make player invulnerable for 30 seconds.
						GHOST_WALKING.getSkill().applyEffects(player, player);
						
						// Reset existing activity timers.
						resetActivityTimers(player); // In case player died in peace zone.
					}
					else if (RED_TEAM.contains(player))
					{
						player.setIsPendingRevive(true);
						player.teleToLocation(RED_SPAWN_LOC.getRandomPoint(), false, player.getInstanceWorld());
						
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
			int minutesLeft = Integer.parseInt(event.split(":")[1]);
			World.broadcastToAllOnlinePlayers("CtF Event: Registration opened for " + minutesLeft + " minutes.");
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method manages registration, flag capture, and team headquarters interactions.<br>
	 * It returns the HTML file path to be displayed to the player based on their current state.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the HTML file path, or {@code null} if no message is sent.
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
			final Instance world = npc.getInstanceWorld();
			final int npcId = npc.getId();
			if ((world != null) && (npcId == MANAGER))
			{
				return "manager-buffheal.html";
			}
			
			if (((world != null) && (npcId == BLUE_TEAM_HEADQUARTERS)) || (npcId == RED_TEAM_HEADQUARTERS))
			{
				final String flag = npc.getTemplate().getName();
				final String team = getParticipantTeamName(player);
				final String enemyteam = getParticipantEnemyTeamName(player);
				final int distance = (int) player.calculateDistance2D(npc);
				
				if (flag.equals(team))
				{
					if (player.equals(getTeamCarrier(player)))
					{
						if (distance > 70)
						{
							final String npcName = npc.getName();
							player.sendMessage(npcName + " : Your request cannot be completed.");
							player.sendMessage(npcName + " : you are not at the correct distance.");
						}
						else
						{
							// Player has returned with enemy flag.
							player.broadcastPacket(new MagicSkillUse(player, npc, 1034, 1, 1, 1));
							removeFlagCarrier(player);
							enemyTeamFlag(player);
							PVP_WORLD.broadcastPacket(new ExShowScreenMessage(player.getName() + " has captured the " + enemyteam + " flag!", 5 * 1000));
							
							increasePoints(player);
						}
					}
					else if (getEnemyCarrier(player) == null)
					{
						// Team flag is missing.
						final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
						html.setFile(player, HTML_PATH + "flag_friendly.html");
						html.replace("%enemyteam%", enemyteam);
						html.replace("%team%", team);
						html.replace("%player%", player.getName());
						player.sendPacket(html);
					}
					else
					{
						// Go get the flag.
						final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
						html.setFile(player, HTML_PATH + "flag_friendly_missing.html");
						html.replace("%enemyteam%", enemyteam);
						html.replace("%team%", team);
						html.replace("%player%", player.getName());
						player.sendPacket(html);
					}
				}
				else
				{
					if (getTeamCarrier(player) != null)
					{
						// enemy flag enemy is missing
						final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
						html.setFile(player, HTML_PATH + "flag_enemy_missing.html");
						html.replace("%enemyteam%", enemyteam);
						html.replace("%player%", CtF.getTeamCarrier(player).getName());
						player.sendPacket(html);
					}
					else
					{
						if (distance > 70)
						{
							final String npcName = npc.getName();
							player.sendMessage(npcName + " : Your request cannot be completed.");
							player.sendMessage(npcName + " : you are not at the correct distance.");
						}
						else
						{
							Lock locked = (BLUE_TEAM.contains(player) ? BLUE_LOCK : RED_LOCK);
							boolean lockAcquired = locked.tryLock();
							if (lockAcquired)
							{
								try
								{
									// player has flag
									final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
									html.setFile(player, HTML_PATH + "flag_enemy.html");
									html.replace("%enemyteam%", enemyteam);
									html.replace("%team%", team);
									html.replace("%player%", player.getName());
									player.sendPacket(html);
									
									// take flag
									final PlayerInventory inventory = player.getInventory();
									setCarrierUnequippedWeapons(player, inventory.getPaperdollItem(Inventory.PAPERDOLL_RHAND), inventory.getPaperdollItem(Inventory.PAPERDOLL_LHAND));
									inventory.equipItem(ItemManager.createItem(ItemProcessType.PICKUP, CtF.getEnemyTeamFlagId(player), 1, player, null));
									inventory.blockAllItems();
									player.broadcastUserInfo();
									setTeamCarrier(player);
									deleteTeamFlag(player);
									PVP_WORLD.broadcastPacket(new ExShowScreenMessage(player.getName() + " has taken the " + enemyteam + " flag!", 7000));
								}
								catch (Exception e)
								{
									LOGGER.warning(e.getMessage());
								}
								finally
								{
									locked.unlock();
								}
							}
							else
							{
								player.sendMessage("Cannot take the flag at this time.");
							}
						}
					}
				}
				
				return null;
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
	 * Players are teleported back if they enter enemy headquarters.<br>
	 * Activity timers are reset for players in their own base zones.<br>
	 * The flag carrier status is updated and messages are sent if the flag enters an enemy zone.
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
					creature.teleToLocation(RED_SPAWN_LOC.getRandomPoint(), creature.getInstanceWorld());
					sendScreenMessage(player, "Entering the enemy headquarters is prohibited!", 7);
				}
				
				if ((zone == RED_PEACE_ZONE) && (creature.getTeam() == Team.BLUE))
				{
					creature.teleToLocation(BLUE_SPAWN_LOC.getRandomPoint(), creature.getInstanceWorld());
					sendScreenMessage(player, "Entering the enemy headquarters is prohibited!", 7);
				}
				
				// Start inactivity check.
				if (creature.isPlayer() && //
					(((zone == BLUE_PEACE_ZONE) && (creature.getTeam() == Team.BLUE)) || //
						((zone == RED_PEACE_ZONE) && (creature.getTeam() == Team.RED))))
				{
					resetActivityTimers(player);
				}
				
				if (playerIsCarrier(player) && //
					(((zone == BLUE_PEACE_ZONE) && (creature.getTeam() == Team.RED)) || //
						((zone == RED_PEACE_ZONE) && (creature.getTeam() == Team.BLUE))))
				{
					removeFlagCarrier(player);
					enemyTeamFlag(player);
					
					for (Player activeChar : getParticipantEnemyTeam(player))
					{
						final String name = activeChar.getName();
						ThreadPool.schedule(() -> sendScreenMessage(activeChar, name + " The enemy has entered your zone with the flag", 4), 1000);
						ThreadPool.schedule(() -> sendScreenMessage(activeChar, name + " Your flag returned to your base!", 3), 4000);
					}
				}
			}
		}
	}
	
	/**
	 * Handles logic when a {@code Creature} leaves a specific {@code ZoneType}.<br>
	 * This method checks if the creature is a {@code Player} currently on an event.<br>
	 * It cancels specific quest timers and removes any active ghost walking effects.
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
	 * It validates level, inventory weight, and current status.<br>
	 * It also ensures the player is not already registered or in a restricted state.
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
	 * Retrieves the name of the team for a specific player.<br>
	 * It checks if the {@code Player} belongs to the blue or red team.<br>
	 * Returns an empty string if the player is not on either team.
	 * @param player The {@code Player} to check.
	 * @return The name of the team as a {@code String}.
	 */
	private static String getParticipantTeamName(Player player)
	{
		return (BLUE_TEAM.contains(player) ? BLUE_TEAM_NAME : (RED_TEAM.contains(player) ? RED_TEAM_NAME : ""));
	}
	
	/**
	 * Gets the name of the team that is opposing the player.<br>
	 * It checks if the {@code Player} belongs to the {@code BLUE_TEAM} or {@code RED_TEAM}.<br>
	 * If the player is not in either team, it returns an empty string.
	 * @param player The {@code Player} whose enemy team name needs to be retrieved.
	 * @return The name of the opposing team as a {@code String}.
	 */
	private static String getParticipantEnemyTeamName(Player player)
	{
		return (BLUE_TEAM.contains(player) ? RED_TEAM_NAME : (RED_TEAM.contains(player) ? BLUE_TEAM_NAME : ""));
	}
	
	/**
	 * Retrieves the set of players belonging to the same team as the given player.<br>
	 * It checks if the {@code Player} is on the blue or red team.
	 * @param player The {@code Player} whose team members need to be retrieved.
	 * @return A {@code Set} containing all players on the same team as the provided {@code Player}.
	 */
	private static Set<Player> getParticipantTeam(Player player)
	{
		return (player.getTeam() == Team.BLUE ? BLUE_TEAM : RED_TEAM);
	}
	
	/**
	 * Retrieves the set of players on the opposing team.<br>
	 * It checks if the {@code player} is on the {@code Team.BLUE} or {@code Team.RED} side.
	 * @param player The {@code Player} used to determine the enemy team.
	 * @return A {@code Set} of players belonging to the enemy team.
	 */
	private static Set<Player> getParticipantEnemyTeam(Player player)
	{
		return (player.getTeam() == Team.BLUE ? RED_TEAM : BLUE_TEAM);
	}
	
	/**
	 * Finds the current carrier for the player's team.<br>
	 * This method checks if the current carrier is still online and in the correct world.<br>
	 * If the carrier is missing, it destroys the flag item.
	 * @param player The {@code Player} whose team carrier needs to be found.
	 * @return The {@code Player} carrying the flag, or {@code null} if no valid carrier exists.
	 */
	private static Player getTeamCarrier(Player player)
	{
		// Check if team carrier has disconnected.
		final int PVP_WORLDId = PVP_WORLD.getId();
		if ((BLUE_TEAM.contains(player) && (BLUE_TEAM_CARRIER != null) && (!BLUE_TEAM_CARRIER.isOnline() || (BLUE_TEAM_CARRIER.getInstanceId() != PVP_WORLDId))) || (RED_TEAM.contains(player) && (RED_TEAM_CARRIER != null) && (!RED_TEAM_CARRIER.isOnline() || (RED_TEAM_CARRIER.getInstanceId() != PVP_WORLDId))))
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, getEnemyTeamFlagId(player), 1, player, false);
			return null;
		}
		
		// Return team carrier.
		return (BLUE_TEAM.contains(player) ? BLUE_TEAM_CARRIER : RED_TEAM_CARRIER);
	}
	
	/**
	 * Finds the current carrier for the team opposing the given {@code Player}.<br>
	 * This method also checks if the enemy carrier has disconnected.<br>
	 * If they are offline, it destroys the flag item and returns {@code null}.
	 * @param player The {@code Player} used to determine which team is the enemy.
	 * @return The {@code Player} carrying the enemy flag, or {@code null} if the carrier is offline.
	 */
	private static Player getEnemyCarrier(Player player)
	{
		// Check if enemy carrier has disconnected.
		final int PVP_WORLDId = PVP_WORLD.getId();
		if ((BLUE_TEAM.contains(player) && (RED_TEAM_CARRIER != null) && (!RED_TEAM_CARRIER.isOnline() || (RED_TEAM_CARRIER.getInstanceId() != PVP_WORLDId))) || (RED_TEAM.contains(player) && (BLUE_TEAM_CARRIER != null) && (!BLUE_TEAM_CARRIER.isOnline() || (BLUE_TEAM_CARRIER.getInstanceId() != PVP_WORLDId))))
		{
			player.destroyItemByItemId(ItemProcessType.DESTROY, getEnemyTeamFlagId(player), 1, player, false);
			return null;
		}
		
		// Return enemy carrier.
		return (BLUE_TEAM.contains(player) ? RED_TEAM_CARRIER : BLUE_TEAM_CARRIER);
	}
	
	/**
	 * Checks if the given {@code Player} is currently carrying a flag.<br>
	 * This method returns {@code true} if the player is either the red or blue team carrier.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is a carrier, {@code false} otherwise.
	 */
	private static boolean playerIsCarrier(Player player)
	{
		return (player == BLUE_TEAM_CARRIER) || (player == RED_TEAM_CARRIER);
	}
	
	/**
	 * Determines the unique ID of the flag for the opposing team.<br>
	 * It checks if the {@code player} belongs to the {@code BLUE_TEAM}.<br>
	 * If they are on the blue team, it returns the red team flag ID.<br>
	 * Otherwise, it returns the blue team flag ID.
	 * @param player The {@code Player} used to determine the current team.
	 * @return The integer ID of the enemy team's flag.
	 */
	private static int getEnemyTeamFlagId(Player player)
	{
		return (BLUE_TEAM.contains(player) ? RED_TEAM_FLAG_ITEM_ID : BLUE_TEAM_FLAG_ITEM_ID);
	}
	
	/**
	 * Updates the weapons held by the carrier for each team.<br>
	 * This method assigns the provided items to the specific team variables.
	 * @param player The {@code Player} who is currently carrying the flag.
	 * @param itemRight The {@code Item} held in the right hand.
	 * @param itemLeft The {@code Item} held in the left hand.
	 */
	private static void setCarrierUnequippedWeapons(Player player, Item itemRight, Item itemLeft)
	{
		if (BLUE_TEAM.contains(player))
		{
			BLUE_TEAM_CARRIER_R_HAND = itemRight;
			BLUE_TEAM_CARRIER_L_HAND = itemLeft;
		}
		else
		{
			RED_TEAM_CARRIER_R_HAND = itemRight;
			RED_TEAM_CARRIER_L_HAND = itemLeft;
		}
	}
	
	/**
	 * Removes the flag from a {@code Player}.<br>
	 * This method un-equips the flag and destroys it.<br>
	 * It also restores the player's original equipment.<br>
	 * Finally, it clears the carrier status for the team.
	 * @param player The {@code Player} who is currently carrying the flag.
	 */
	private static void removeFlagCarrier(Player player)
	{
		// Un-equip - destroy flag.
		player.getInventory().unEquipItemInSlot(Inventory.PAPERDOLL_RHAND);
		player.destroyItemByItemId(ItemProcessType.DESTROY, getEnemyTeamFlagId(player), 1, player, false);
		
		// Unblock inventory.
		player.getInventory().unblock();
		
		// Re-equip player items.
		final Item carrierRHand = BLUE_TEAM.contains(player) ? BLUE_TEAM_CARRIER_R_HAND : RED_TEAM_CARRIER_R_HAND;
		final Item carrierLHand = BLUE_TEAM.contains(player) ? BLUE_TEAM_CARRIER_L_HAND : RED_TEAM_CARRIER_L_HAND;
		if ((carrierRHand != null) && (player.getInventory().getItemByItemId(carrierRHand.getId()) != null))
		{
			player.getInventory().equipItem(carrierRHand);
		}
		
		if ((carrierLHand != null) && (player.getInventory().getItemByItemId(carrierLHand.getId()) != null))
		{
			player.getInventory().equipItem(carrierLHand);
		}
		
		setCarrierUnequippedWeapons(player, null, null);
		
		// Flag carrier removal.
		if (BLUE_TEAM.contains(player))
		{
			BLUE_TEAM_CARRIER = null;
		}
		else
		{
			RED_TEAM_CARRIER = null;
		}
		
		// Show re-equipped weapons.
		player.broadcastUserInfo();
	}
	
	/**
	 * Assigns a {@code Player} as the carrier for their respective team.<br>
	 * Checks if the {@code BLUE_TEAM} or {@code RED_TEAM} already has a carrier.<br>
	 * Sends a message to the player if the flag is already taken.
	 * @param player The {@code Player} attempting to pick up the flag.
	 */
	private static void setTeamCarrier(Player player)
	{
		if (BLUE_TEAM.contains(player))
		{
			if (BLUE_TEAM_CARRIER == null)
			{
				BLUE_TEAM_CARRIER = player;
			}
			else
			{
				player.sendMessage("The Blue flag has already been taken");
			}
		}
		
		if (RED_TEAM.contains(player))
		{
			if (RED_TEAM_CARRIER == null)
			{
				RED_TEAM_CARRIER = player;
			}
			else
			{
				player.sendMessage("The Red flag has already been taken");
			}
		}
	}
	
	/**
	 * Sends a temporary message to the player's screen.<br>
	 * The message appears in the top center of the display.
	 * @param player The {@code Player} who will receive the message.
	 * @param message The text content to be displayed.
	 * @param duration The time in seconds for which the message remains visible.
	 */
	private void sendScreenMessage(Player player, String message, int duration)
	{
		player.sendPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, false));
	}
	
	/**
	 * Sends a message to all players in the {@code PVP_WORLD}.<br>
	 * The message appears at the top center of the screen.
	 * @param message The text content to display on the screen.
	 * @param duration The time in seconds for which the message remains visible.
	 */
	private void broadcastScreenMessage(String message, int duration)
	{
		PVP_WORLD.broadcastPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, false));
	}
	
	/**
	 * Sends a message to all players in the {@code PVP_WORLD}.<br>
	 * This method displays the text on the screen with a visual effect.
	 * @param message The text content to display to the players.
	 * @param duration The time in seconds that the message should remain visible.
	 */
	private void broadcastScreenMessageWithEffect(String message, int duration)
	{
		PVP_WORLD.broadcastPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, true));
	}
	
	/**
	 * Sends a score update message to all players in the world.<br>
	 * This method displays the current scores for both teams on the screen.<br>
	 * It uses {@code PVP_WORLD} to broadcast the information.
	 */
	private static void broadcastScoreMessage()
	{
		PVP_WORLD.broadcastPacket(new ExShowScreenMessage("Blue: " + BLUE_SCORE + " - Red: " + RED_SCORE, ExShowScreenMessage.BOTTOM_RIGHT, 15000, 0, true, false));
	}
	
	/**
	 * Registers a listener for the {@code ON_PLAYER_LOGOUT} event.<br>
	 * This ensures that {@code onPlayerLogout} is called when a player leaves the game.
	 * @param player The {@code Player} object to attach the listener to.
	 */
	private void addLogoutListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
	}
	
	/**
	 * Registers a listener to track when the specified player dies.<br>
	 * This method handles {@code EventType.ON_CREATURE_DEATH} events.<br>
	 * It triggers the {@code onPlayerDeath} logic when the death occurs.
	 * @param player The {@code Player} instance to attach the listener to.
	 */
	private void addDeathListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onPlayerDeath(event), this));
	}
	
	/**
	 * Increases the score for the team that the {@code player} belongs to.<br>
	 * This method updates either the red or blue score based on the player's team.<br>
	 * It also broadcasts a message to all players showing the updated score.
	 * @param player The {@code Player} whose team score should be increased.
	 */
	public static void increasePoints(Player player)
	{
		if (BLUE_TEAM.contains(player))
		{
			BLUE_SCORE++;
			broadcastScoreMessage();
		}
		else if (RED_TEAM.contains(player))
		{
			RED_SCORE++;
			broadcastScoreMessage();
		}
	}
	
	/**
	 * Removes specific event listeners from a {@code Player}.<br>
	 * This method unregisters the current instance from logout and death events.
	 * @param player The {@code Player} object to modify.
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
	 * Resets the inactivity timers for a specific {@code Player}.<br>
	 * This method cancels existing kick warnings and starts new ones.<br>
	 * It uses the {@code INACTIVITY_TIME} and {@code WAIT_TIME} constants to set durations.
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
	 * It sets the {@code TEAM_FORFEIT} flag to {@code true}.<br>
	 * All active quest timers are cancelled.<br>
	 * A new {@code EndFight} timer is started and a message is broadcast.
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
	 * This method removes the player from active tracking lists.<br>
	 * It also checks if the player was carrying a flag and returns it to the base.<br>
	 * Finally, it updates the match status and manages potential forfeits.
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
		if (playerIsCarrier(player))
		{
			removeFlagCarrier(player);
			enemyTeamFlag(player);
			broadcastScreenMessage(player.getName() + " has logout, Flag returned to base", 7);
		}
		
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
	 * Handles the logic when a player dies during a Capture The Flag match.<br>
	 * This method updates scores for successful kills and handles flag returns.<br>
	 * It also initiates a resurrection timer for the deceased player.
	 * @param event The {@code OnCreatureDeath} event containing the victim and attacker details.
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
				PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.UPDATE, MapUtil.sortByValue(PLAYER_SCORES, true)));
			}
			
			// Confirm Red team kill.
			final String killerName = killer.getName();
			final String killedPlayerName = killedPlayer.getName();
			if ((killerTeam == Team.RED) && (killedTeam == Team.BLUE))
			{
				PLAYER_SCORES.put(killer, PLAYER_SCORES.get(killer) + 1);
				PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.UPDATE, MapUtil.sortByValue(PLAYER_SCORES, true)));
				
				final CreatureSay cs = new CreatureSay(killer, ChatType.WHISPER, killerName, "I have killed " + killedPlayerName + "!");
				for (Player activeChar : getParticipantTeam(killer))
				{
					if (activeChar != null)
					{
						activeChar.sendPacket(cs);
					}
				}
			}
			
			if (playerIsCarrier(killedPlayer))
			{
				removeFlagCarrier(killedPlayer);
				enemyTeamFlag(killedPlayer);
				broadcastScreenMessage(killerName + " has killed " + killedPlayerName + " Flag returned to base!", 7);
			}
			
			// Auto release after 10 seconds.
			startQuestTimer("ResurrectPlayer", 10000, null, killedPlayer);
		}
	}
	
	/**
	 * Starts the Capture The Flag event for a specific player.<br>
	 * This method initializes the event state and clears previous data.<br>
	 * It spawns the manager NPC and starts the registration timers.<br>
	 * Broadcasts messages to all online players regarding the start.
	 * @param eventMaker The {@code Player} who initiated the event.
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
		World.broadcastToAllOnlinePlayers("CtF Event: Registration opened for " + REGISTRATION_TIME + " minutes.");
		World.broadcastToAllOnlinePlayers("CtF Event: You can register at Giran CtF Event Manager.");
		
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
	 * Stops the current Capture The Flag event.<br>
	 * This method cleans up all active timers and removes participants from the game.<br>
	 * It also deletes headquarters and resets team statuses to their default values.
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
		
		// Reset flag carriers
		if (BLUE_TEAM_CARRIER != null)
		{
			removeFlagCarrier(BLUE_TEAM_CARRIER);
		}
		
		if (RED_TEAM_CARRIER != null)
		{
			removeFlagCarrier(RED_TEAM_CARRIER);
		}
		
		// Remove Headquarters team Blue.
		if (FLAG_BLUE_SPAWN != null)
		{
			FLAG_BLUE_SPAWN.deleteMe();
			FLAG_BLUE_SPAWN = null;
		}
		
		// Remove Headquarters team Red.
		if (FLAG_RED_SPAWN != null)
		{
			FLAG_RED_SPAWN.deleteMe();
			FLAG_RED_SPAWN = null;
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
		
		// Send message to players.
		World.broadcastToAllOnlinePlayers("CtF Event: Event was canceled.");
		
		// Set state PARTICIPATING
		setState(EventState.INACTIVE);
		return true;
	}
	
	/**
	 * Handles the spawning of flags for the enemy team.<br>
	 * This method checks if the {@code player} belongs to a specific team.<br>
	 * It then adds a new flag spawn at the opposite headquarters location.
	 * @param player The {@code Player} used to determine which team's flag needs to be spawned.
	 */
	private static void enemyTeamFlag(Player player)
	{
		if (BLUE_TEAM.contains(player))
		{
			FLAG_RED_SPAWN = addSpawn(RED_TEAM_HEADQUARTERS, RED_TEAM_HEADQUARTERS_LOC, false, (WAIT_TIME + FIGHT_TIME) * 60000, false, PVP_WORLD.getId());
		}
		
		if (RED_TEAM.contains(player))
		{
			FLAG_BLUE_SPAWN = addSpawn(BLUE_TEAM_HEADQUARTERS, BLUE_TEAM_HEADQUARTERS_LOC, false, (WAIT_TIME + FIGHT_TIME) * 60000, false, PVP_WORLD.getId());
		}
	}
	
	/**
	 * Removes the enemy team's spawn flag from the world.<br>
	 * This method checks if the {@code player} belongs to a specific team.<br>
	 * It then deletes and nullifies the corresponding opponent flag.
	 * @param player The {@code Player} who triggered the flag deletion.
	 */
	private static void deleteTeamFlag(Player player)
	{
		if (BLUE_TEAM.contains(player) && (FLAG_RED_SPAWN != null))
		{
			FLAG_RED_SPAWN.deleteMe();
			FLAG_RED_SPAWN = null;
		}
		
		if (RED_TEAM.contains(player) && (FLAG_BLUE_SPAWN != null))
		{
			FLAG_BLUE_SPAWN.deleteMe();
			FLAG_BLUE_SPAWN = null;
		}
	}
	
	/**
	 * Updates the current game state.<br>
	 * This method sets the internal {@code _state} variable to a new value.<br>
	 * It uses synchronization to ensure thread safety.
	 * @param state The new {@code EventState} to apply.
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
	 * This method verifies if the game is currently in the {@code PARTICIPATING} phase.
	 * @return {@code true} if the event is participating, {@code false} otherwise.
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
	 * Checks if the current event state is starting.<br>
	 * This method returns {@code true} if the game is in the {@code STARTING} phase.
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
	 * Checks if the Capture The Flag event has currently started.<br>
	 * It compares the current state against {@code EventState.STARTED}.
	 * @return {@code true} if the event is active, {@code false} otherwise.
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
	 * Checks if a specific event should be bypassed for a player.<br>
	 * This method currently always returns {@code false}.
	 * @param player The {@code Player} to check.
	 * @param bypass The name of the event to bypass.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		return false;
	}
	
	/**
	 * This is the main entry point for the {@code CtF} class.<br>
	 * It initializes a new instance of the {@code CtF} object.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CtF();
	}
}
