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
package ai.bosses.MightyChaosGolem;

import java.util.Calendar;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code MightyChaosGolem} boss.<br>
 * It handles the logic for its actions, movements, and combat patterns during gameplay.
 * @author Tanatos
 */
public class MightyChaosGolem extends Script
{
	private static final int MIGHTY_CHAOS_GOLEM = 29436;
	private static final Location[] MIGHTY_CHAOS_GOLEM_SPAWN_LOC =
	{
		new Location(116942, -149931, -2328),
		new Location(119666, -162360, -1336),
		new Location(103981, -155303, -1800),
		new Location(101943, -148981, -3152),
		new Location(107592, -159375, -1800)
	};
	private static final long DESPAWN_DELAY = 1_200_000;
	
	private static final SpawnEvent[] SPAWN_EVENTS =
	{
		new SpawnEvent("spawnFirstGolem", 13, 0),
		new SpawnEvent("spawnSecondGolem", 16, 0),
		new SpawnEvent("spawnThirdGolem", 20, 0)
	};
	
	private int _location;
	
	/**
	 * Initializes the {@code MightyChaosGolem} script.<br>
	 * This constructor schedules all required spawn events.<br>
	 * It logs the scheduled spawn times to the server console.
	 */
	private MightyChaosGolem()
	{
		for (SpawnEvent event : SPAWN_EVENTS)
		{
			scheduleSpawn(event);
		}
		
		LOGGER.info("Mighty Chaos Golem will spawn at 13:00, 16:00 and 20:00.");
	}
	
	/**
	 * Handles specific events for the {@code MightyChaosGolem} script.<br>
	 * This method checks if the provided {@code event} matches a known spawn trigger.<br>
	 * If a match is found, it spawns a new boss at a random location and schedules the next event.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the superclass {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		for (SpawnEvent spawnEvent : SPAWN_EVENTS)
		{
			if (spawnEvent.name.equals(event))
			{
				_location = getRandom(4);
				addSpawn(MIGHTY_CHAOS_GOLEM, MIGHTY_CHAOS_GOLEM_SPAWN_LOC[_location].getX(), MIGHTY_CHAOS_GOLEM_SPAWN_LOC[_location].getY(), MIGHTY_CHAOS_GOLEM_SPAWN_LOC[_location].getZ(), 0, false, DESPAWN_DELAY, false);
				
				scheduleSpawn(spawnEvent);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Schedules a new entity to appear at a specific time.<br>
	 * This method calculates the delay based on the {@code SpawnEvent} data.<br>
	 * It then starts a quest timer to trigger the spawn.
	 * @param spawnEvent The event containing the name and time for the spawn.
	 */
	private void scheduleSpawn(SpawnEvent spawnEvent)
	{
		final Calendar time = TimeUtil.getNextTime(spawnEvent.hour, spawnEvent.minute);
		startQuestTimer(spawnEvent.name, time.getTimeInMillis() - System.currentTimeMillis(), null, null);
	}
	
	private static class SpawnEvent
	{
		String name;
		int hour;
		int minute;
		
		SpawnEvent(String name, int hour, int minute)
		{
			this.name = name;
			this.hour = hour;
			this.minute = minute;
		}
	}
	
	/**
	 * This is the entry point for the {@code MightyChaosGolem} class.<br>
	 * It initializes a new instance of the script to start the process.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new MightyChaosGolem();
	}
}
