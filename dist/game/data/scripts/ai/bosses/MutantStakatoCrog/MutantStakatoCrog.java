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
package ai.bosses.MutantStakatoCrog;

import java.util.Calendar;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the AI behavior for the {@code MutantStakatoCrog} boss.<br>
 * It handles the logic for its unique actions and patterns during combat.
 * @author Tanatos
 */
public class MutantStakatoCrog extends Script
{
	private static final int STAKATO_CROG = 29426;
	private static final Location[] STAKATO_CROG_SPAWN_LOC =
	{
		new Location(73629, -57308, -3105),
		new Location(77761, -41481, -3228),
		new Location(71804, -43440, -3229)
	};
	private static final long DESPAWN_DELAY = 1_200_000;
	
	private static final SpawnEvent[] SPAWN_EVENTS =
	{
		new SpawnEvent("spawnFirstCrog", 13, 0),
		new SpawnEvent("spawnSecondCrog", 16, 0),
		new SpawnEvent("spawnThirdCrog", 20, 0)
	};
	
	private int _location;
	
	/**
	 * Private constructor for the {@code MutantStakatoCrog} class.<br>
	 * This constructor initializes the spawning schedule for the boss.<br>
	 * It schedules all events defined in the {@code SPAWN_EVENTS} array.
	 */
	private MutantStakatoCrog()
	{
		for (SpawnEvent event : SPAWN_EVENTS)
		{
			scheduleSpawn(event);
		}
		
		LOGGER.info("Mutant Stakato Crog will spawn at 13:00, 16:00 and 20:00.");
	}
	
	/**
	 * Handles specific events for the {@code MutantStakatoCrog} script.<br>
	 * This method checks if the {@code event} matches a known spawn trigger.<br>
	 * If matched, it spawns an NPC and schedules the next event.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} instance associated with the script.
	 * @param player The {@code Player} instance involved in the action.
	 * @return The result of the superclass {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		for (SpawnEvent spawnEvent : SPAWN_EVENTS)
		{
			if (spawnEvent.name.equals(event))
			{
				_location = getRandom(2);
				addSpawn(STAKATO_CROG, STAKATO_CROG_SPAWN_LOC[_location].getX(), STAKATO_CROG_SPAWN_LOC[_location].getY(), STAKATO_CROG_SPAWN_LOC[_location].getZ(), 0, false, DESPAWN_DELAY, false);
				
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
	 * This is the entry point for the {@code MutantStakatoCrog} class.<br>
	 * It initializes a new instance of the script to start the process.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new MutantStakatoCrog();
	}
}
