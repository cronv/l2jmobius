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
package ai.areas.GainakUnderground;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.PeaceZone;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDeath;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.OnEventTrigger;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the logic for the siege events in the Gainak Underground area.<br>
 * This script manages specific behaviors and interactions related to {@code SiegeZone} mechanics.
 * @author LasTravel, Gigi, Mobius
 * @URL http://l2wiki.com/Gainak
 */
public class GainakSiege extends Script
{
	private static final int SIEGE_EFFECT = 20140700;
	private static final int SIEGE_DURATION = 30;
	private static final SiegeZone GAINAK_SIEGE_ZONE = ZoneManager.getInstance().getZoneById(60019, SiegeZone.class);
	private static final PeaceZone GAINAK_TOWN_ZONE = ZoneManager.getInstance().getZoneById(60020, PeaceZone.class);
	protected static final int[] ASSASSIN_IDS =
	{
		19471,
		19472,
		19473
	};
	private static final Location[] ASSASSIN_SPAWNS =
	{
		new Location(17085, -115385, -249, 41366),
		new Location(15452, -114531, -243, 5464),
		new Location(15862, -113121, -250, 53269)
	};
	private boolean _isInSiege = false;
	
	/**
	 * Initializes the {@code GainakSiege} logic.<br>
	 * It registers zone transitions and kill events.<br>
	 * This constructor also starts the siege quest timer.
	 */
	public GainakSiege()
	{
		addEnterZoneId(GAINAK_SIEGE_ZONE.getId(), GAINAK_TOWN_ZONE.getId());
		addKillId(ASSASSIN_IDS);
		startQuestTimer("GAINAK_WAR", getTimeBetweenSieges() * 60000, null, null);
	}
	
	/**
	 * Calculates the random time interval between siege events.<br>
	 * The result is a value between {@code 120} and {@code 180} minutes.<br>
	 * This corresponds to a duration of approximately 2 to 3 hours.
	 * @return The number of minutes until the next siege occurs.
	 */
	private int getTimeBetweenSieges()
	{
		return getRandom(120, 180); // 2 to 3 hours.
	}
	
	/**
	 * Handles logic when a {@code Creature} enters a specific {@code ZoneType}.<br>
	 * This method checks if the creature is a player and triggers siege effects.
	 * @param creature The {@code Creature} entering the zone.
	 * @param zone The {@code ZoneType} that was entered.
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (_isInSiege && creature.isPlayer())
		{
			creature.broadcastPacket(new OnEventTrigger(SIEGE_EFFECT, true));
		}
	}
	
	/**
	 * Handles specific game events for the Gainak area.<br>
	 * This method manages the transition between siege and peace states.<br>
	 * It updates zone statuses and broadcasts messages to all players.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance associated with the event.
	 * @param player The {@code Player} instance associated with the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equalsIgnoreCase("GAINAK_WAR"))
		{
			if (_isInSiege)
			{
				_isInSiege = false;
				GAINAK_TOWN_ZONE.setEnabled(true); // enable before broadcast
				GAINAK_TOWN_ZONE.broadcastPacket(new OnEventTrigger(SIEGE_EFFECT, false));
				GAINAK_TOWN_ZONE.broadcastPacket(new ExShowScreenMessage(NpcStringId.GAINAK_IN_PEACE, ExShowScreenMessage.TOP_CENTER, 5000, true));
				GAINAK_SIEGE_ZONE.setActive(false);
				GAINAK_SIEGE_ZONE.updateZoneStatusForCharactersInside();
				startQuestTimer("GAINAK_WAR", getTimeBetweenSieges() * 60000, null, null);
				if (PvpConfig.ANNOUNCE_GAINAK_SIEGE)
				{
					final SystemMessage s = new SystemMessage(SystemMessageId.PROGRESS_EVENT_DAY_S1);
					s.addString("Gainak is now in peace.");
					World.broadcastToAllOnlinePlayers(s);
				}
			}
			else
			{
				for (Location loc : ASSASSIN_SPAWNS)
				{
					addSpawn(getRandomEntry(ASSASSIN_IDS), loc, true, 1800000);
				}
				
				_isInSiege = true;
				GAINAK_TOWN_ZONE.broadcastPacket(new OnEventTrigger(SIEGE_EFFECT, true));
				GAINAK_TOWN_ZONE.broadcastPacket(new ExShowScreenMessage(NpcStringId.GAINAK_IN_WAR, ExShowScreenMessage.TOP_CENTER, 5000, true));
				GAINAK_TOWN_ZONE.setEnabled(false); // disable after broadcast
				GAINAK_SIEGE_ZONE.setActive(true);
				GAINAK_SIEGE_ZONE.updateZoneStatusForCharactersInside();
				startQuestTimer("GAINAK_WAR", SIEGE_DURATION * 60000, null, null);
				if (PvpConfig.ANNOUNCE_GAINAK_SIEGE)
				{
					final SystemMessage s = new SystemMessage(SystemMessageId.PROGRESS_EVENT_DAY_S1);
					s.addString("Gainak is now under siege.");
					World.broadcastToAllOnlinePlayers(s);
				}
				
				GAINAK_TOWN_ZONE.setEnabled(false);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles logic when an {@code Npc} is killed by a {@code Player}.<br>
	 * This method checks if the kill occurred in a specific active siege zone.<br>
	 * If conditions are met, it schedules a new assassin respawn.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who performed the kill.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final SiegeZone zone = ZoneManager.getInstance().getZone(npc, SiegeZone.class);
		if ((zone != null) && (zone.getId() == 60019) && zone.isActive())
		{
			ThreadPool.schedule(new RespawnNewAssassin(npc.getLocation()), 60000);
		}
	}
	
	private class RespawnNewAssassin implements Runnable
	{
		private final Location _loc;
		
		public RespawnNewAssassin(Location loc)
		{
			_loc = loc;
		}
		
		@Override
		public void run()
		{
			addSpawn(getRandomEntry(ASSASSIN_IDS), _loc, true, 1800000);
		}
	}
	
	/**
	 * Handles logic when a player dies during a siege.<br>
	 * This method updates the {@code pvpKills} count for the attacker.<br>
	 * It checks if both the attacker and target are players within the siege zone.
	 * @param event The {@code OnCreatureDeath} event containing death details.
	 */
	@RegisterEvent(EventType.ON_CREATURE_DEATH)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerDeath(OnCreatureDeath event)
	{
		if (_isInSiege && GAINAK_SIEGE_ZONE.getCharactersInside().contains(event.getTarget()) && event.getAttacker().isPlayer() && event.getTarget().isPlayer())
		{
			final Player attackerPlayer = event.getAttacker().asPlayer();
			attackerPlayer.setPvpKills(attackerPlayer.getPvpKills() + 1);
			attackerPlayer.updateUserInfo();
		}
	}
	
	/**
	 * This method serves as the entry point for the {@code GainakSiege} class.<br>
	 * It initializes a new instance of {@code GainakSiege}.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new GainakSiege();
	}
}
