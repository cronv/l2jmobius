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
package custom.events.Race;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.script.Event;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for race-related events within the game.<br>
 * It manages specific behaviors and interactions triggered by these custom events.
 * @author Gnacik
 */
public class Race extends Event
{
	// 5 min for register
	private static final int REGISTER_TIME = 5;
	
	// 5 min for race
	private static final int RACE_TIME = 10;
	
	// NPCs
	private static final int START_NPC = 900103;
	private static final int STOP_NPC = 900104;
	
	// Locations
	private static final String[] LOCATIONS =
	{
		"Heretic catacomb enterance",
		"Dion castle bridge",
		"Floran village enterance",
		"Floran fort gate"
	};
	
	// Event NPCs list
	private List<Npc> _npclist;
	
	// Npc
	private Npc _npc;
	
	// Player list
	private Collection<Player> _players;
	
	// Event Task
	ScheduledFuture<?> _eventTask = null;
	
	// Event state
	private static boolean _isactive = false;
	
	// Race state
	private static boolean _isRaceStarted = false;
	
	// Skills (Frog by default)
	private static int _skill = 6201;
	
	// We must keep second NPC spawn for radar.
	private static int[] _randspawn = null;
	
	private static final int[][] COORDS =
	{
		// @formatter:off
		// x, y, z, heading
		{ 39177, 144345, -3650, 0 },
		{ 22294, 155892, -2950, 0 },
		{ 16537, 169937, -3500, 0 },
		{  7644, 150898, -2890, 0 }
	};
	private static final int[][] REWARDS =
	{
		{ 6622, 2 }, // Giant's Codex
		{ 9625, 2 }, // Giant's Codex -
		{ 9626, 2 }, // Giant's Codex -
		{ 9627, 2 }, // Giant's Codex -
		{ 9546, 5 }, // Attr stones
		{ 9547, 5 },
		{ 9548, 5 },
		{ 9549, 5 },
		{ 9550, 5 },
		{ 9551, 5 },
		{ 9574, 3 }, // Mid-Grade Life Stone: level 80
		{ 9575, 2 }, // High-Grade Life Stone: level 80
		{ 9576, 1 }, // Top-Grade Life Stone: level 80
		{ 20034,1 }  // Revita pop
		// @formatter:on
	};
	
	/**
	 * Initializes the {@code Race} event.<br>
	 * Registers the required NPCs and loads the configuration data.
	 */
	private Race()
	{
		addStartNpc(START_NPC);
		addFirstTalkId(START_NPC);
		addTalkId(START_NPC);
		addStartNpc(STOP_NPC);
		addFirstTalkId(STOP_NPC);
		addTalkId(STOP_NPC);
		
		loadConfig();
	}
	
	/**
	 * Loads the configuration for this event from an XML file.<br>
	 * It parses the data and schedules the events based on the defined patterns.
	 */
	private void loadConfig()
	{
		new IXmlReader()
		{
			@Override
			public void load()
			{
				parseDatapackFile("data/scripts/custom/events/Race/config.xml");
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
	 * Handles logic when a timer event is triggered.<br>
	 * This method checks if the {@code event} name starts with {@code Schedule}.<br>
	 * If it does, it starts the event and adds a new timer to the schedule.
	 * @param event The name of the event being processed.
	 * @param params The parameters associated with the event.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} involved in the event.
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
	 * Starts the race event if it is not already active.<br>
	 * Checks if the custom NPC table is enabled before proceeding.<br>
	 * Initializes the player list and spawns the manager NPC.<br>
	 * Broadcasts a start message to all online players.
	 * @param eventMaker The {@code Player} who initiated the event.
	 * @return {@code true} if the event started successfully, {@code false} otherwise.
	 */
	@Override
	public boolean eventStart(Player eventMaker)
	{
		// Don't start event if it is active.
		if (_isactive)
		{
			return false;
		}
		
		// Check Custom Table - we use custom NPCs.
		if (!GeneralConfig.CUSTOM_NPC_DATA)
		{
			final String name = getName();
			LOGGER.info(name + ": Event can't be started, because custom npc table is disabled!");
			eventMaker.sendMessage("Event " + name + " can't be started because custom NPC table is disabled!");
			return false;
		}
		
		// Initialize list
		_npclist = new ArrayList<>();
		_players = ConcurrentHashMap.newKeySet();
		
		// Set Event active
		_isactive = true;
		
		// Spawn Manager
		_npc = recordSpawn(START_NPC, 18429, 145861, -3090, 35000, false, 0);
		
		// Announce event start
		World.broadcastToAllOnlinePlayers("* Race Event started! *");
		World.broadcastToAllOnlinePlayers("Visit Event Manager in Dion village and signup, you have " + REGISTER_TIME + " min before Race Start...");
		
		// Schedule Event end
		_eventTask = ThreadPool.schedule(this::StartRace, REGISTER_TIME * 60 * 1000);
		return true;
	}
	
	/**
	 * Initializes the race event and prepares all participants.<br>
	 * Checks if any players have signed up to join.<br>
	 * Selects a random finish location and spawns the required NPC.<br>
	 * Notifies online players and schedules the completion timer.
	 */
	protected void StartRace()
	{
		// Abort race if no players signup.
		if (_players.isEmpty())
		{
			World.broadcastToAllOnlinePlayers("Race aborted, nobody signup.");
			eventStop();
			return;
		}
		
		// Set state
		_isRaceStarted = true;
		
		// Announce
		World.broadcastToAllOnlinePlayers("Race started!");
		
		// Get random Finish
		final int location = getRandom(0, LOCATIONS.length - 1);
		_randspawn = COORDS[location];
		
		// And spawn NPC
		recordSpawn(STOP_NPC, _randspawn[0], _randspawn[1], _randspawn[2], _randspawn[3], false, 0);
		
		// Transform players and send message.
		for (Player player : _players)
		{
			if ((player != null) && player.isOnline())
			{
				if (player.isInsideRadius2D(_npc, 500))
				{
					sendMessage(player, "Race started! Go find Finish NPC as fast as you can... He is located near " + LOCATIONS[location]);
					transformPlayer(player);
					player.getRadar().addMarker(_randspawn[0], _randspawn[1], _randspawn[2]);
				}
				else
				{
					sendMessage(player, "I told you stay near me right? Distance was too high, you are excluded from race");
					_players.remove(player);
				}
			}
		}
		
		// Schedule timeup for Race.
		_eventTask = ThreadPool.schedule(this::timeUp, RACE_TIME * 60 * 1000);
	}
	
	/**
	 * Stops the current race event if it is active.<br>
	 * This method resets all internal states and clears participants.<br>
	 * It also removes NPCs from the world and teleports players back.
	 * @return {@code true} if the event was successfully stopped, or {@code false} if it was already inactive.
	 */
	@Override
	public boolean eventStop()
	{
		// Don't stop inactive event.
		if (!_isactive)
		{
			return false;
		}
		
		// Set inactive
		_isactive = false;
		_isRaceStarted = false;
		
		// Cancel task if any.
		if (_eventTask != null)
		{
			_eventTask.cancel(true);
			_eventTask = null;
		}
		
		// Untransform players teleport to the event start point.
		for (Player player : _players)
		{
			if ((player != null) && player.isOnline())
			{
				player.untransform();
				player.teleToLocation(_npc, true);
			}
		}
		
		// Despawn NPCs
		for (Npc npc : _npclist)
		{
			if (npc != null)
			{
				npc.deleteMe();
			}
		}
		
		_npclist.clear();
		_players.clear();
		
		// Announce event end
		World.broadcastToAllOnlinePlayers("* Race Event finished *");
		return true;
	}
	
	/**
	 * Checks if a player wants to perform a specific action via a bypass command.<br>
	 * This method handles skill changes and teleportation requests.<br>
	 * It returns {@code true} after processing the request or showing the menu.
	 * @param player The {@code Player} who is sending the bypass command.
	 * @param bypass The string command sent by the player to trigger an action.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		if (bypass.startsWith("skill"))
		{
			if (_isRaceStarted)
			{
				player.sendMessage("Race already started, you cannot change transform skill now");
			}
			else
			{
				final int number = Integer.parseInt(bypass.substring(5));
				final Skill skill = SkillData.getInstance().getSkill(number, 1);
				if (skill != null)
				{
					_skill = number;
					player.sendMessage("Transform skill set to:");
					player.sendMessage(skill.getName());
				}
				else
				{
					player.sendMessage("Error while changing transform skill");
				}
			}
		}
		else if (bypass.startsWith("tele"))
		{
			if ((Integer.parseInt(bypass.substring(4)) > 0) && (_randspawn != null))
			{
				player.teleToLocation(_randspawn[0], _randspawn[1], _randspawn[2]);
			}
			else
			{
				player.teleToLocation(18429, 145861, -3090);
			}
		}
		
		showMenu(player);
		return true;
	}
	
	/**
	 * Handles specific event logic for the race system.<br>
	 * It processes actions like transforming, signing up, and finishing the race.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} performing the action.
	 * @return The HTML text to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String htmltext = event;
		final QuestState qs = getQuestState(player, false);
		if (qs == null)
		{
			return null;
		}
		
		if (event.equalsIgnoreCase("transform"))
		{
			transformPlayer(player);
			return null;
		}
		else if (event.equalsIgnoreCase("untransform"))
		{
			player.untransform();
			return null;
		}
		else if (event.equalsIgnoreCase("showfinish"))
		{
			player.getRadar().addMarker(_randspawn[0], _randspawn[1], _randspawn[2]);
			return null;
		}
		else if (event.equalsIgnoreCase("signup"))
		{
			if (_players.contains(player))
			{
				return "900103-onlist.htm";
			}
			
			_players.add(player);
			return "900103-signup.htm";
		}
		else if (event.equalsIgnoreCase("quit"))
		{
			player.untransform();
			_players.remove(player);
			return "900103-quit.htm";
		}
		else if (event.equalsIgnoreCase("finish"))
		{
			if (player.isAffectedBySkill(_skill))
			{
				winRace(player);
				return "900104-winner.htm";
			}
			
			return "900104-notrans.htm";
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the correct HTML file to display when a player talks to an {@code Npc} for the first time.<br>
	 * It checks if the race has started and whether the player is currently racing.<br>
	 * The returned string follows a specific naming convention based on the NPC ID and race status.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML file to be shown.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		getQuestState(player, true);
		final int npcId = npc.getId();
		if (npcId == START_NPC)
		{
			if (_isRaceStarted)
			{
				return START_NPC + "-started-" + isRacing(player) + ".htm";
			}
			
			return START_NPC + "-" + isRacing(player) + ".htm";
		}
		else if ((npcId == STOP_NPC) && _isRaceStarted)
		{
			return STOP_NPC + "-" + isRacing(player) + ".htm";
		}
		
		return npcId + ".htm";
	}
	
	/**
	 * Checks if a specific {@code Player} is currently participating in the race.<br>
	 * Returns {@code 1} if the player is in the active list.<br>
	 * Returns {@code 0} if the player is not participating.
	 * @param player The {@code Player} to check.
	 * @return An integer representing the participation status.
	 */
	private int isRacing(Player player)
	{
		return _players.contains(player) ? 1 : 0;
	}
	
	/**
	 * Spawns a new {@code Npc} into the world and adds it to the internal list.<br>
	 * This method handles the creation of the entity at the specified coordinates.
	 * @param npcId The unique identifier for the NPC type.
	 * @param x The X coordinate for spawning.
	 * @param y The Y coordinate for spawning.
	 * @param z The Z coordinate for spawning.
	 * @param heading The direction the NPC faces.
	 * @param randomOffSet Whether to apply a random position offset.
	 * @param despawnDelay The time in milliseconds before the NPC disappears.
	 * @return The created {@code Npc} object, or {@code null} if spawning failed.
	 */
	private Npc recordSpawn(int npcId, int x, int y, int z, int heading, boolean randomOffSet, long despawnDelay)
	{
		final Npc npc = addSpawn(npcId, x, y, z, heading, randomOffSet, despawnDelay);
		if (npc != null)
		{
			_npclist.add(npc);
		}
		
		return npc;
	}
	
	/**
	 * Updates the visual and status state of a {@code Player}.<br>
	 * This method handles transformation states and standing positions.<br>
	 * It also manages specific skill effects for the race event.
	 * @param player The {@code Player} to be transformed.
	 */
	private void transformPlayer(Player player)
	{
		if (player.isTransformed())
		{
			player.untransform();
		}
		
		if (player.isSitting())
		{
			player.standUp();
		}
		
		player.getEffectList().stopEffects(AbnormalType.SPEED_UP);
		player.stopSkillEffects(SkillFinishType.REMOVED, 268);
		player.stopSkillEffects(SkillFinishType.REMOVED, 298); // Rabbit Spirit Totem
		SkillData.getInstance().getSkill(_skill, 1).applyEffects(player, player);
	}
	
	/**
	 * Sends a chat message to a specific {@code Player}.<br>
	 * The message is sent as if it came from an NPC.
	 * @param player The {@code Player} who will receive the message.
	 * @param text The content of the message to be displayed.
	 */
	private void sendMessage(Player player, String text)
	{
		player.sendPacket(new CreatureSay(_npc, ChatType.MPCC_ROOM, _npc.getName(), text));
	}
	
	/**
	 * Displays the admin menu to a specific player.<br>
	 * It loads the content from {@code admin_menu.htm}.<br>
	 * The resulting HTML is sent as a packet to the player.
	 * @param player The {@code Player} who will receive the menu.
	 */
	private void showMenu(Player player)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage();
		final String content = getHtm(player, "admin_menu.htm");
		html.setHtml(content);
		player.sendPacket(html);
	}
	
	/**
	 * Handles the logic for when the race time expires.<br>
	 * It broadcasts a message to all online players.<br>
	 * It then calls {@code eventStop()} to end the event.
	 */
	protected void timeUp()
	{
		World.broadcastToAllOnlinePlayers("Time up, nobody wins!");
		eventStop();
	}
	
	/**
	 * Handles the logic for when a player wins the race.<br>
	 * It gives a random reward to the {@code Player}.<br>
	 * It broadcasts a victory message to all online players.<br>
	 * It stops the current event.
	 * @param player The {@code Player} who won the race.
	 */
	private void winRace(Player player)
	{
		final int[] reward = REWARDS[getRandom(REWARDS.length - 1)];
		player.addItem(ItemProcessType.REWARD, reward[0], reward[1], _npc, true);
		World.broadcastToAllOnlinePlayers(player.getName() + " is a winner!");
		eventStop();
	}
	
	/**
	 * This is the entry point for the {@code Race} class.<br>
	 * It initializes a new instance of the {@code Race} event.
	 * @param args The command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Race();
	}
}
