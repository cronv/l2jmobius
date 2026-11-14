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
package custom.events.Elpies;

import java.io.File;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.SchedulingPattern;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.EventMonster;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.script.Event;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Elpies} game event.<br>
 * It manages the spawning and behavior of specific monsters during the event period.
 */
public class Elpies extends Event
{
	// NPC
	private static final int ELPY = 900100;
	
	// Amount of Elpies to spawn when the event starts.
	private static final int ELPY_AMOUNT = 100;
	
	// Event duration in minutes
	private static final int EVENT_DURATION_MINUTES = 2;
	private static final int[][] DROPLIST_CONSUMABLES =
	{
		// @formatter:off
		// itemId, chance, min amount, max amount
		{  1540, 80, 10, 15 },	// Quick Healing Potion
		{  1538, 60,  5, 10 },	// Blessed Scroll of Escape
		{  3936, 40,  5, 10 },	// Blessed Scroll of Ressurection
		{  6387, 25,  5, 10 },	// Blessed Scroll of Ressurection Pets
		{ 22025, 15,  5, 10 },	// Powerful Healing Potion
		{  6622, 10,  1, 1 },	// Giant's Codex
		{ 20034,  5,  1, 1 },	// Revita Pop
		{ 20004,  1,  1, 1 },	// Energy Ginseng
		{ 20004,  0,  1, 1 }	// Energy Ginseng
	};
	
	private static final int[][] DROPLIST_CRYSTALS =
	{
		{ 1458, 80, 50, 100 },	// Crystal D-Grade
		{ 1459, 60, 40,  80 },	// Crystal C-Grade
		{ 1460, 40, 30,  60 },	// Crystal B-Grade
		{ 1461, 20, 20,  30 },	// Crystal A-Grade
		{ 1462,  0, 10,  20 }	// Crystal S-Grade
		// @formatter:on
	};
	
	// Non-final variables
	private static boolean EVENT_ACTIVE = false;
	private static int CURRENT_ELPY_COUNT = 0;
	private ScheduledFuture<?> _eventTask = null;
	
	/**
	 * Private constructor for the {@code Elpies} class.<br>
	 * This constructor initializes the event by registering spawn and kill IDs.<br>
	 * It also triggers the configuration loading process.
	 */
	private Elpies()
	{
		addSpawnId(ELPY);
		addKillId(ELPY);
		
		loadConfig();
	}
	
	/**
	 * Loads the configuration for this event from an XML file.<br>
	 * It parses the data located in {@code data/scripts/custom/events/Elpies/config.xml}.<br>
	 * This method initializes the event schedules and parameters.
	 */
	private void loadConfig()
	{
		new IXmlReader()
		{
			@Override
			public void load()
			{
				parseDatapackFile("data/scripts/custom/events/Elpies/config.xml");
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
	 * This method checks if the {@code event} string starts with {@code Schedule}.<br>
	 * If it does, it starts the event and schedules the next occurrence.
	 * @param event The name or type of the event to process.
	 * @param params The configuration parameters for the event.
	 * @param npc The {@code Npc} associated with this timer.
	 * @param player The {@code Player} associated with this timer.
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
	 * Checks if a {@code Player} is allowed to bypass the event.<br>
	 * This method currently always returns {@code false}.
	 * @param player The {@code Player} attempting to bypass the event.
	 * @param bypass The unique identifier for the bypass action.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean eventBypass(Player player, String bypass)
	{
		return false;
	}
	
	/**
	 * Starts the Elpy invasion event.<br>
	 * Checks if the custom NPC table is enabled before spawning mobs.<br>
	 * Broadcasts a message to all online players when the event begins.
	 * @param eventMaker The {@code Player} who initiated the event.
	 * @return {@code true} if the event started successfully, or {@code false} otherwise.
	 */
	@Override
	public boolean eventStart(Player eventMaker)
	{
		if (EVENT_ACTIVE)
		{
			return false;
		}
		
		// Check Custom Table - we use custom NPCs.
		if (!GeneralConfig.CUSTOM_NPC_DATA)
		{
			final String name = getName();
			LOGGER.info(name + ": Event can't be started because custom NPC table is disabled!");
			eventMaker.sendMessage("Event " + name + " can't be started because custom NPC table is disabled!");
			return false;
		}
		
		EVENT_ACTIVE = true;
		
		final EventLocation randomLoc = getRandomEntry(EventLocation.values());
		CURRENT_ELPY_COUNT = 0;
		final long despawnDelay = EVENT_DURATION_MINUTES * 60000;
		for (int i = 0; i < ELPY_AMOUNT; i++)
		{
			addSpawn(ELPY, randomLoc.getRandomX(), randomLoc.getRandomY(), randomLoc.getZ(), 0, true, despawnDelay);
			CURRENT_ELPY_COUNT++;
		}
		
		World.broadcastToAllOnlinePlayers("*Squeak Squeak*");
		World.broadcastToAllOnlinePlayers("Elpy invasion in " + randomLoc.getName());
		World.broadcastToAllOnlinePlayers("Help us exterminate them!");
		World.broadcastToAllOnlinePlayers("You have " + EVENT_DURATION_MINUTES + " minutes!");
		_eventTask = ThreadPool.schedule(() ->
		{
			World.broadcastToAllOnlinePlayers("Time is up!");
			eventStop();
		}, despawnDelay);
		return true;
	}
	
	/**
	 * Stops the current Elpies event.<br>
	 * This method sets {@code EVENT_ACTIVE} to {@code false}.<br>
	 * It cancels any active tasks and removes all spawned NPCs.<br>
	 * A broadcast message is sent to all online players.
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
		if (_eventTask != null)
		{
			_eventTask.cancel(true);
			_eventTask = null;
		}
		
		for (Spawn spawn : SpawnTable.getInstance().getSpawns(ELPY))
		{
			final Npc npc = spawn.getLastSpawn();
			if (npc != null)
			{
				npc.deleteMe();
			}
		}
		
		World.broadcastToAllOnlinePlayers("*Squeak Squeak*");
		World.broadcastToAllOnlinePlayers("Elpy Event finished!");
		return true;
	}
	
	/**
	 * This method is called when an {@code Npc} is killed during the event.<br>
	 * It handles item drops and updates the remaining count of active enemies.<br>
	 * If no more enemies remain, it broadcasts a message and stops the event.
	 * @param npc The {@code Npc} that was just killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates whether the killed entity was a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (EVENT_ACTIVE)
		{
			dropItem(npc, killer, DROPLIST_CONSUMABLES);
			dropItem(npc, killer, DROPLIST_CRYSTALS);
			CURRENT_ELPY_COUNT--;
			
			if (CURRENT_ELPY_COUNT <= 0)
			{
				World.broadcastToAllOnlinePlayers("All elpies have been killed!");
				eventStop();
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It configures the {@code EventMonster} to drop items on the ground.<br>
	 * It also prevents the monster from being hit by offensive skills.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		((EventMonster) npc).eventSetDropOnGround(true);
		((EventMonster) npc).eventSetBlockOffensiveSkills(true);
	}
	
	private enum EventLocation
	{
		ADEN("Aden", 146558, 148341, 26622, 28560, -2200),
		DION("Dion", 18564, 19200, 144377, 145782, -3081),
		GLUDIN("Gludin", -84040, -81420, 150257, 151175, -3125),
		HV("Hunters Village", 116094, 117141, 75776, 77072, -2700),
		OREN("Oren", 82048, 82940, 53240, 54126, -1490);
		
		private final String _name;
		private final int _minX;
		private final int _maxX;
		private final int _minY;
		private final int _maxY;
		private final int _z;
		
		EventLocation(String name, int minX, int maxX, int minY, int maxY, int z)
		{
			_name = name;
			_minX = minX;
			_maxX = maxX;
			_minY = minY;
			_maxY = maxY;
			_z = z;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public int getRandomX()
		{
			return getRandom(_minX, _maxX);
		}
		
		public int getRandomY()
		{
			return getRandom(_minY, _maxY);
		}
		
		public int getZ()
		{
			return _z;
		}
	}
	
	/**
	 * Handles the logic for dropping items from a defeated mob.<br>
	 * It selects one item from the {@code droplist} based on random chance.<br>
	 * The method then calls {@code mob.dropItem} to give the reward to the player.
	 * @param mob The {@code Npc} that was killed.
	 * @param player The {@code Player} who received the loot.
	 * @param droplist A 2D array containing item IDs, chances, and amount ranges.
	 */
	private void dropItem(Npc mob, Player player, int[][] droplist)
	{
		final int chance = getRandom(100);
		for (int[] drop : droplist)
		{
			if (chance >= drop[1])
			{
				mob.dropItem(player, drop[0], getRandom(drop[2], drop[3]));
				break;
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code Elpies} class.<br>
	 * It initializes a new instance of the event.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new Elpies();
	}
}
