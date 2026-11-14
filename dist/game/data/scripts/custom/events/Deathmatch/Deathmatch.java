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
package custom.events.Deathmatch;

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
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
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
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.MapUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for a {@code Deathmatch} game event.<br>
 * This class manages the lifecycle and rules of the combat encounter.<br>
 * It coordinates player interactions, scoring, and victory conditions.
 * @author NasSeKa
 */
public class Deathmatch extends Event
{
	// NPC
	private static final int MANAGER = 70011;
	
	// Skills
	private static final SkillHolder GHOST_WALKING = new SkillHolder(100000, 1); // Custom Ghost Walking
	private static final SkillHolder[] GROUP_BUFFS =
	{
		new SkillHolder(62072, 1), // Fever Melody - Adventurer
		new SkillHolder(62073, 1), // Drumbeat of Serenity - Adventurer
		new SkillHolder(62074, 1), // Fantasia Harmony - Adventurer
	};
	
	// Others
	private static final int INSTANCE_ID = 3049;
	private static final int BLUE_DOOR_ID = 24190002;
	private static final int RED_DOOR_ID = 24190003;
	private static final Location MANAGER_SPAWN_LOC = new Location(83425, 148585, -3406, 32938);
	private static final ZoneForm SPAWN_1 = ZoneManager.getInstance().getZoneByName("colosseum_battle1").getZone();
	private static final ZoneForm SPAWN_2 = ZoneManager.getInstance().getZoneByName("colosseum_battle2").getZone();
	private static final ZoneForm SPAWN_3 = ZoneManager.getInstance().getZoneByName("colosseum_battle3").getZone();
	
	// Settings
	private static final int REGISTRATION_TIME = 1; // Minutes
	private static final int WAIT_TIME = 20; // Seconds
	private static final int FIGHT_TIME = 3; // Minutes
	private static final int INACTIVITY_TIME = 2; // Minutes
	private static final int MINIMUM_PARTICIPANT_LEVEL = 85;
	private static final int MAXIMUM_PARTICIPANT_LEVEL = 200;
	private static final int MINIMUM_PARTICIPANT_COUNT = 4;
	private static final int MAXIMUM_PARTICIPANT_COUNT = 24; // Scoreboard has 25 slots
	private static final ItemHolder REWARD = new ItemHolder(57, 1000000); // Adena
	
	// Misc
	private static final Map<Player, Integer> PLAYER_SCORES = new ConcurrentHashMap<>();
	private static final Set<Player> PLAYER_LIST = ConcurrentHashMap.newKeySet();
	private static Instance PVP_WORLD = null;
	private static Npc MANAGER_NPC_INSTANCE = null;
	private static boolean EVENT_ACTIVE = false;
	
	/**
	 * Private constructor for the {@code Deathmatch} class.<br>
	 * It initializes the required talk IDs and loads the configuration files.
	 */
	private Deathmatch()
	{
		addTalkId(MANAGER);
		addFirstTalkId(MANAGER);
		
		loadConfig();
	}
	
	/**
	 * Loads the configuration for deathmatch events.<br>
	 * This method parses the {@code config.xml} file from the datapack.<br>
	 * It initializes event schedules and timers based on the XML data.
	 */
	private void loadConfig()
	{
		new IXmlReader()
		{
			@Override
			public void load()
			{
				parseDatapackFile("data/scripts/custom/events/Deathmatch/config.xml");
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
	 * Handles specific events triggered by a timer.<br>
	 * This method checks if the {@code event} string starts with {@code Schedule}.<br>
	 * If it does, it initiates the event and adds it to the internal timer list.
	 * @param event The name or type of the timer event.
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
	 * Handles specific event logic for the Deathmatch system.<br>
	 * This method processes actions like registration, teleportation, and fight management.<br>
	 * It returns an HTML file name to be displayed to the player based on the outcome of the event.
	 * @param event The unique identifier for the event type to execute.
	 * @param npc The {@code Npc} entity triggering or involved in the event.
	 * @param player The {@code Player} interacting with the event.
	 * @return A {@code String} containing the name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!EVENT_ACTIVE)
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
					World.broadcastToAllOnlinePlayers("Deathmatch Event: Event was canceled, not enough participants.");
					for (Player participant : PLAYER_LIST)
					{
						removeListeners(participant);
						participant.setRegisteredOnEvent(false);
					}
					
					EVENT_ACTIVE = false;
					return null;
				}
				
				// Create the instance.
				final InstanceManager manager = InstanceManager.getInstance();
				final InstanceTemplate template = manager.getInstanceTemplate(INSTANCE_ID);
				PVP_WORLD = manager.createInstance(template, null);
				
				// Make sure doors are closed.
				PVP_WORLD.getDoors().forEach(Door::closeMe);
				
				// Randomize player list.
				final List<Player> playerList = new ArrayList<>(PLAYER_LIST.size());
				playerList.addAll(PLAYER_LIST);
				Collections.shuffle(playerList);
				PLAYER_LIST.clear();
				PLAYER_LIST.addAll(playerList);
				
				for (Player participant : PLAYER_LIST)
				{
					participant.setOnEvent(true);
					participant.setOnSoloEvent(true);
					participant.setRegisteredOnEvent(false);
					PVP_WORLD.addAllowed(participant);
					participant.leaveParty();
					RANDOM: switch (getRandom(1, 3))
					{
						case 1:
						{
							participant.teleToLocation(SPAWN_1.getRandomPoint(), false, PVP_WORLD);
							break RANDOM;
						}
						case 2:
						{
							participant.teleToLocation(SPAWN_2.getRandomPoint(), false, PVP_WORLD);
							break RANDOM;
						}
						case 3:
						{
							participant.teleToLocation(SPAWN_3.getRandomPoint(), false, PVP_WORLD);
							break RANDOM;
						}
					}
					
					participant.setInvul(true);
					participant.setImmobilized(true);
					participant.disableAllSkills();
					for (Summon summon : participant.getServitors().values())
					{
						summon.setInvul(true);
						summon.setImmobilized(true);
						summon.disableAllSkills();
					}
					
					addDeathListener(participant);
				}
				
				// Spawn managers.
				// addSpawn(MANAGER, BLUE_BUFFER_SPAWN_LOC, false, (WAIT_TIME + FIGHT_TIME) * 60000, false, PVP_WORLD.getId());
				// addSpawn(MANAGER, RED_BUFFER_SPAWN_LOC, false, (WAIT_TIME + FIGHT_TIME) * 60000, false, PVP_WORLD.getId());
				// Initialize scores.
				// BLUE_SCORE = 0;
				// RED_SCORE = 0;
				// Initialize scoreboard.
				PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.INITIALIZE, MapUtil.sortByValue(PLAYER_SCORES, true)));
				
				// Schedule start.
				startQuestTimer("5", (WAIT_TIME * 1000) - 5000, null, null);
				startQuestTimer("4", (WAIT_TIME * 1000) - 4000, null, null);
				startQuestTimer("3", (WAIT_TIME * 1000) - 3000, null, null);
				startQuestTimer("2", (WAIT_TIME * 1000) - 2000, null, null);
				startQuestTimer("1", (WAIT_TIME * 1000) - 1000, null, null);
				startQuestTimer("StartFight", WAIT_TIME * 1000, null, null);
				break;
			}
			case "StartFight":
			{
				// Open doors.
				// closeDoor(BLUE_DOOR_ID, PVP_WORLD.getId());
				// closeDoor(RED_DOOR_ID, PVP_WORLD.getId());
				// Send message.
				broadcastScreenMessageWithEffect("The fight has began!", 5);
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
				
				Player winner = Collections.max(PLAYER_SCORES.entrySet(), Map.Entry.comparingByValue()).getKey();
				winner.addItem(ItemProcessType.REWARD, REWARD, player, true);
				
				startQuestTimer("ScoreBoard", 3500, null, null);
				startQuestTimer("TeleportOut", 7000, null, null);
				break;
			}
			case "ScoreBoard":
			{
				PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.FINISH, MapUtil.sortByValue(PLAYER_SCORES, true)));
				break;
			}
			case "TeleportOut":
			{
				// Remove event listeners.
				for (Player participant : PLAYER_LIST)
				{
					removeListeners(participant);
					participant.setOnEvent(false);
					participant.setOnSoloEvent(false);
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
				
				EVENT_ACTIVE = false;
				break;
			}
			case "ResurrectPlayer":
			{
				if (player.isDead() && player.isOnEvent())
				{
					player.setIsPendingRevive(true);
					RANDOM: switch (getRandom(1, 3))
					{
						case 1:
						{
							player.teleToLocation(SPAWN_1.getRandomPoint(), false, PVP_WORLD);
							break RANDOM;
						}
						case 2:
						{
							player.teleToLocation(SPAWN_2.getRandomPoint(), false, PVP_WORLD);
							break RANDOM;
						}
						case 3:
						{
							player.teleToLocation(SPAWN_3.getRandomPoint(), false, PVP_WORLD);
							break RANDOM;
						}
					}
					
					resetActivityTimers(player);
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
				html.setFile(player, "data/scripts/custom/events/Deathmatch/manager-cancel.html");
				html.replace("%player_numbers%", String.valueOf(PLAYER_LIST.size()));
				player.sendPacket(html);
				break;
			}
			case "manager-register":
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(npc.getObjectId());
				html.setFile(player, "data/scripts/custom/events/Deathmatch/manager-register.html");
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
				PVP_WORLD.ejectPlayer(player);
				PLAYER_LIST.remove(player);
				PLAYER_SCORES.remove(player);
				player.setOnEvent(false);
				player.setOnSoloEvent(false);
				removeListeners(player);
				player.sendMessage("You have been kicked for been inactive.");
				if (PVP_WORLD != null)
				{
					broadcastScreenMessageWithEffect("Player " + player.getName() + " was kicked for been inactive!", 7);
				}
			}
		}
		
		if (event.startsWith("RegistrationWarn:"))
		{
			final int minutesLeft = Integer.parseInt(event.split(":")[1]);
			World.broadcastToAllOnlinePlayers("Deathmatch Event: Registration opened for " + minutesLeft + " minutes.");
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the interaction when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method determines which dialogue page to show based on the player's registration status.<br>
	 * It also manages quest timers for registering or canceling participation in the event.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} entity initiating the conversation.
	 * @return A {@code String} representing the name of the HTML dialogue file to display, or {@code null} if the event is inactive.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		// Event not active.
		if (!EVENT_ACTIVE)
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
	 * Handles logic when a {@code Creature} leaves a specific {@code ZoneType}.<br>
	 * This method checks if the creature is a {@code Player} currently participating in an event.<br>
	 * If so, it cancels specific quest timers and removes any active ghost walking effects.
	 * @param creature The {@code Creature} that exited the zone.
	 * @param zone The type of the zone being exited.
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
	 * It also ensures the player is not already in another activity.
	 * @param player The {@code Player} to check.
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
	 * Sends a temporary message to the player's screen.<br>
	 * The message appears in the top center of the display.
	 * @param player The {@code Player} who will receive the message.
	 * @param message The text content to display on the screen.
	 * @param duration The time in seconds for which the message should remain visible.
	 */
	private void sendScreenMessage(Player player, String message, int duration)
	{
		player.sendPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, false));
	}
	
	/**
	 * Sends a message to all players in the {@code PVP_WORLD}.<br>
	 * The message appears at the top center of the screen.
	 * @param message The text content to display to the players.
	 * @param duration The time in seconds that the message should remain visible.
	 */
	private void broadcastScreenMessage(String message, int duration)
	{
		PVP_WORLD.broadcastPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, false));
	}
	
	/**
	 * Sends a message to all players in the {@code PVP_WORLD}.<br>
	 * This method includes a visual effect for the display.
	 * @param message The text content to be displayed on the screen.
	 * @param duration The time in milliseconds that the message should remain visible.
	 */
	private void broadcastScreenMessageWithEffect(String message, int duration)
	{
		PVP_WORLD.broadcastPacket(new ExShowScreenMessage(message, ExShowScreenMessage.TOP_CENTER, duration * 1000, 0, true, true));
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
	 * Registers a listener for death events for a specific player.<br>
	 * This allows the system to trigger {@code onPlayerDeath} when a creature dies.
	 * @param player The {@code Player} instance to attach the listener to.
	 */
	private void addDeathListener(Player player)
	{
		player.addListener(new ConsumerEventListener(player, EventType.ON_CREATURE_DEATH, (OnCreatureDeath event) -> onPlayerDeath(event), this));
	}
	
	/**
	 * Removes the event listeners associated with a specific player.<br>
	 * This method unregisters listeners for {@code ON_PLAYER_LOGOUT} and {@code ON_CREATURE_DEATH}.<br>
	 * It only removes listeners that belong to this instance.
	 * @param player The {@code Player} from whom the listeners should be removed.
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
	 * This method cancels existing kick warnings and starts new ones.<br>
	 * The duration depends on whether the door in {@code PVP_WORLD} is open.
	 * @param player The {@code Player} whose timers need to be reset.
	 */
	private void resetActivityTimers(Player player)
	{
		final int objectId = player.getObjectId();
		cancelQuestTimer("KickPlayer" + objectId, null, player);
		cancelQuestTimer("KickPlayerWarning" + objectId, null, player);
		startQuestTimer("KickPlayer" + objectId, PVP_WORLD.getDoor(BLUE_DOOR_ID).isOpen() ? INACTIVITY_TIME * 60000 : (INACTIVITY_TIME * 60000) + (WAIT_TIME * 1000), null, player);
		startQuestTimer("KickPlayerWarning" + objectId, PVP_WORLD.getDoor(BLUE_DOOR_ID).isOpen() ? (INACTIVITY_TIME / 2) * 60000 : ((INACTIVITY_TIME / 2) * 60000) + (WAIT_TIME * 1000), null, player);
	}
	
	/**
	 * Handles the logic when a player logs out of the game.<br>
	 * This method removes the {@code Player} from active tracking lists.<br>
	 * It clears the player's data from {@code PLAYER_LIST} and {@code PLAYER_SCORES}.
	 * @param event The {@code OnPlayerLogout} event containing the player details.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		
		// Remove player from lists.
		PLAYER_LIST.remove(player);
		PLAYER_SCORES.remove(player);
	}
	
	/**
	 * This method handles the logic when a player dies.<br>
	 * It updates scores if the killer is also a player.<br>
	 * It broadcasts the updated score rankings to the world.<br>
	 * It starts a resurrection timer for the killed player.
	 * @param event The {@code OnCreatureDeath} event containing death details.
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	public void onPlayerDeath(OnCreatureDeath event)
	{
		if (event.getTarget().isPlayer())
		{
			final Player killedPlayer = event.getTarget().asPlayer();
			final Player killer = event.getAttacker().asPlayer();
			
			// Confirm player kill.
			PLAYER_SCORES.put(killer, PLAYER_SCORES.get(killer) + 1);
			PVP_WORLD.broadcastPacket(new ExPVPMatchCCRecord(ExPVPMatchCCRecord.UPDATE, MapUtil.sortByValue(PLAYER_SCORES, true)));
			
			// Auto release after 10 seconds.
			startQuestTimer("ResurrectPlayer", 10000, null, killedPlayer);
		}
	}
	
	/**
	 * Starts the Deathmatch event.<br>
	 * This method initializes the event state and spawns the manager NPC.<br>
	 * It also clears previous timers and player data.
	 * @param eventMaker The {@code Player} who initiated the event start.
	 * @return {@code true} if the event started successfully, or {@code false} if it is already active.
	 */
	@Override
	public boolean eventStart(Player eventMaker)
	{
		if (EVENT_ACTIVE)
		{
			return false;
		}
		
		EVENT_ACTIVE = true;
		
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
		
		// Spawn event manager.
		MANAGER_NPC_INSTANCE = addSpawn(MANAGER, MANAGER_SPAWN_LOC, false, REGISTRATION_TIME * 60000);
		MANAGER_NPC_INSTANCE.setTitle("Deathmatch Event");
		MANAGER_NPC_INSTANCE.broadcastStatusUpdate();
		startQuestTimer("TeleportToArena", REGISTRATION_TIME * 60000, null, null);
		
		// Send message to players.
		World.broadcastToAllOnlinePlayers("Deathmatch Event: Registration opened for " + REGISTRATION_TIME + " minutes.");
		World.broadcastToAllOnlinePlayers("Deathmatch Event: You can register at Giran Event Manager.");
		
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
	 * Stops the current deathmatch event.<br>
	 * This method deactivates the event and cleans up all active timers and participants.<br>
	 * It also removes special status effects from players and destroys the PVP world if it exists.
	 * @return {@code true} if the event was successfully stopped, or {@code false} if it was not active.
	 */
	@Override
	public boolean eventStop()
	{
		if (!EVENT_ACTIVE)
		{
			return false;
		}
		
		EVENT_ACTIVE = false;
		
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
			participant.setRegisteredOnEvent(false);
			participant.setOnEvent(false);
			participant.setOnSoloEvent(false);
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
		
		if (PVP_WORLD != null)
		{
			PVP_WORLD.destroy();
			PVP_WORLD = null;
		}
		
		// Send message to players.
		World.broadcastToAllOnlinePlayers("Deathmatch Event: Event was canceled.");
		return true;
	}
	
	/**
	 * Checks if a {@code Player} is allowed to bypass an event.<br>
	 * This method currently always returns {@code false}.
	 * @param player The {@code Player} attempting the bypass.
	 * @param bypass The unique identifier for the event being bypassed.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		return false;
	}
	
	/**
	 * This is the main entry point for the {@code Deathmatch} class.<br>
	 * It initializes a new instance of the {@code Deathmatch} object.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Deathmatch();
	}
}
