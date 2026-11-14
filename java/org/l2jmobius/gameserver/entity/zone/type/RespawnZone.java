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
package org.l2jmobius.gameserver.entity.zone.type;

import java.util.EnumMap;
import java.util.Map;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.zone.ZoneRespawn;

/**
 * This class represents a specific area where players can respawn.<br>
 * It provides the logic for handling entity rebirth within a {@code RespawnZone}.
 * @author Nyaran
 */
public class RespawnZone extends ZoneRespawn
{
	private final Map<Race, String> _raceRespawnPoint = new EnumMap<>(Race.class);
	
	/**
	 * Creates a new instance of {@code RespawnZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this respawn zone.
	 */
	public RespawnZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It handles any logic required for entering this specific area.
	 * @param creature The {@code Creature} that entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It handles any logic required for exiting the area.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
	}
	
	/**
	 * Adds a specific respawn location for a creature race.<br>
	 * This method maps a {@code Race} type to a coordinate string.
	 * @param race The name of the race as defined in the {@code Race} enum.
	 * @param point The coordinate string where the race will respawn.
	 */
	public void addRaceRespawnPoint(String race, String point)
	{
		_raceRespawnPoint.put(Race.valueOf(race), point);
	}
	
	/**
	 * Retrieves all respawn points for every race.<br>
	 * This method returns the internal map of {@code Race} to location strings.
	 * @return a {@code Map} containing all registered {@code Race} respawn points.
	 */
	public Map<Race, String> getAllRespawnPoints()
	{
		return _raceRespawnPoint;
	}
	
	/**
	 * Retrieves the specific respawn point for a given {@code Player}.<br>
	 * This method looks up the location based on the player's race.
	 * @param player The {@code Player} whose respawn point is being requested.
	 * @return The name of the respawn point as a {@code String}, or {@code null} if no point exists.
	 */
	public String getRespawnPoint(Player player)
	{
		return _raceRespawnPoint.get(player.getRace());
	}
}
