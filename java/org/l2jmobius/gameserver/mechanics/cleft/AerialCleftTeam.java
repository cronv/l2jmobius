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
package org.l2jmobius.gameserver.mechanics.cleft;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class represents one of the two teams fighting over the Aerial Cleft.<br>
 * It keeps the participants, the score and the Combat Aerial Target of the team.
 * @author Altur
 */
public class AerialCleftTeam
{
	private final int _teamId;
	private final Map<Integer, CleftPlayer> _players = new ConcurrentHashMap<>();
	private final List<Location> _startPoints = new ArrayList<>();
	private final List<Location> _exitPoints = new ArrayList<>();
	private int _points;
	private CleftPlayer _combatAerialTarget;
	
	/**
	 * Creates a new team.
	 * @param teamId The team identifier, {@code 0} for the blue team and {@code 1} for the red team.
	 */
	public AerialCleftTeam(int teamId)
	{
		_teamId = teamId;
	}
	
	/**
	 * Gets the identifier of this team.
	 * @return {@code 0} for the blue team, {@code 1} for the red team.
	 */
	public int getId()
	{
		return _teamId;
	}
	
	/**
	 * Registers a player in this team.
	 * @param player The {@code Player} to add.
	 * @return The created {@code CleftPlayer} record.
	 */
	public CleftPlayer addPlayer(Player player)
	{
		final CleftPlayer cleftPlayer = new CleftPlayer(player, System.currentTimeMillis());
		_players.put(player.getObjectId(), cleftPlayer);
		return cleftPlayer;
	}
	
	/**
	 * Removes a player from this team.
	 * @param objectId The object id of the player to remove.
	 * @return The removed {@code CleftPlayer} record, or {@code null} if the player was not registered.
	 */
	public CleftPlayer removePlayer(int objectId)
	{
		final CleftPlayer cleftPlayer = _players.remove(objectId);
		if ((cleftPlayer != null) && (_combatAerialTarget == cleftPlayer))
		{
			_combatAerialTarget = null;
		}
		
		return cleftPlayer;
	}
	
	/**
	 * Verifies if a player is registered in this team.
	 * @param objectId The object id of the player to look for.
	 * @return {@code true} if the player belongs to this team, {@code false} otherwise.
	 */
	public boolean containsPlayer(int objectId)
	{
		return _players.containsKey(objectId);
	}
	
	/**
	 * Gets the battle record of a player of this team.
	 * @param objectId The object id of the player to look for.
	 * @return The {@code CleftPlayer} record, or {@code null} if the player does not belong to this team.
	 */
	public CleftPlayer getPlayer(int objectId)
	{
		return _players.get(objectId);
	}
	
	/**
	 * Gets the battle records of every player of this team.
	 * @return A {@code Collection} of {@code CleftPlayer} records.
	 */
	public Collection<CleftPlayer> getPlayers()
	{
		return _players.values();
	}
	
	/**
	 * Gets the amount of players registered in this team.
	 * @return The player count as an {@code int}.
	 */
	public int getPlayerCount()
	{
		return _players.size();
	}
	
	/**
	 * Gets the score of this team.
	 * @return The amount of points as an {@code int}.
	 */
	public int getPoints()
	{
		return _points;
	}
	
	/**
	 * Adds points to the score of this team.
	 * @param count The amount of points to add.
	 */
	public void addPoints(int count)
	{
		_points += count;
	}
	
	/**
	 * Adds a location players of this team are teleported to when they enter the battlefield.
	 * @param location The {@code Location} to add.
	 */
	public void addStartPoint(Location location)
	{
		_startPoints.add(location);
	}
	
	/**
	 * Adds a location players of this team are teleported to when they leave the battlefield.
	 * @param location The {@code Location} to add.
	 */
	public void addExitPoint(Location location)
	{
		_exitPoints.add(location);
	}
	
	/**
	 * Gets a random location to enter the battlefield.
	 * @return The chosen {@code Location}, or {@code null} if the zone defines none.
	 */
	public Location getStartPoint()
	{
		if (_startPoints.isEmpty())
		{
			return null;
		}
		
		return _startPoints.get(Rnd.get(_startPoints.size()));
	}
	
	/**
	 * Gets a random location to leave the battlefield.
	 * @return The chosen {@code Location}, or {@code null} if the zone defines none.
	 */
	public Location getExitPoint()
	{
		if (_exitPoints.isEmpty())
		{
			return null;
		}
		
		return _exitPoints.get(Rnd.get(_exitPoints.size()));
	}
	
	/**
	 * Verifies if the zone provided this team with the locations it needs.
	 * @return {@code true} if both start and exit locations are known, {@code false} otherwise.
	 */
	public boolean hasLocations()
	{
		return !_startPoints.isEmpty() && !_exitPoints.isEmpty();
	}
	
	/**
	 * Gets the current Combat Aerial Target of this team.
	 * @return The {@code CleftPlayer} record of the target, or {@code null} if the team has none.
	 */
	public CleftPlayer getCombatAerialTarget()
	{
		return _combatAerialTarget;
	}
	
	/**
	 * Picks a random participant as the Combat Aerial Target of this team.<br>
	 * The previous target, if any, loses the mark.
	 * @return The chosen {@code CleftPlayer} record, or {@code null} if the team is empty.
	 */
	public CleftPlayer selectCombatAerialTarget()
	{
		if (_combatAerialTarget != null)
		{
			_combatAerialTarget.setCombatAerialTarget(false);
			_combatAerialTarget = null;
		}
		
		if (_players.isEmpty())
		{
			return null;
		}
		
		final List<CleftPlayer> candidates = new ArrayList<>(_players.values());
		final CleftPlayer chosen = candidates.get(Rnd.get(candidates.size()));
		chosen.setCombatAerialTarget(true);
		_combatAerialTarget = chosen;
		return chosen;
	}
	
	/**
	 * Clears the participants and the score of this team.<br>
	 * The locations read from the zone are kept.
	 */
	public void clean()
	{
		_players.clear();
		_points = 0;
		_combatAerialTarget = null;
	}
}
