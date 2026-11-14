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
package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;

/**
 * Represents a specific location point within the Underground Coliseum.<br>
 * This class is used to manage spatial data for mechanics related to the coliseum area.
 */
public class UCPoint
{
	private final Location _loc;
	private final List<Door> _doors;
	private final List<Player> _players = new ArrayList<>();
	
	/**
	 * Creates a new {@code UCPoint} instance.<br>
	 * This constructor initializes the point with specific doors and a location.
	 * @param doors The list of {@code Door} objects associated with this point.
	 * @param loc The {@code Location} where this point is situated.
	 */
	public UCPoint(List<Door> doors, Location loc)
	{
		_doors = doors;
		_loc = loc;
	}
	
	/**
	 * Moves a {@code Player} to the location of this point.<br>
	 * Saves the current coordinates into the {@code RESTORE_LOCATION} variable.<br>
	 * Resets the player if they are currently dead.<br>
	 * Adds the {@code Player} to the internal list of players.
	 * @param player The {@code Player} object to teleport.
	 */
	public void teleportPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		player.getVariables().set(PlayerVariables.RESTORE_LOCATION, player.getLocation().getX() + ";" + player.getLocation().getY() + ";" + player.getLocation().getZ());
		
		if (player.isDead())
		{
			UCTeam.resPlayer(player);
		}
		
		player.teleToLocation(_loc, true);
		_players.add(player);
	}
	
	/**
	 * Updates the state of all doors associated with this point.<br>
	 * It iterates through the {@code _doors} list to trigger an action.
	 * @param open Set to {@code true} to open the doors, or {@code false} to close them.
	 */
	public void actionDoors(boolean open)
	{
		if (_doors.isEmpty())
		{
			return;
		}
		
		for (Door door : _doors)
		{
			if (open)
			{
				door.openMe();
			}
			else
			{
				door.closeMe();
			}
		}
	}
	
	/**
	 * Retrieves the current location of this point.<br>
	 * This method returns the {@code Location} object stored in the private field.
	 * @return The {@code Location} associated with this {@code UCPoint}.
	 */
	public Location getLocation()
	{
		return _loc;
	}
	
	/**
	 * Retrieves the list of all players currently at this point.<br>
	 * This method returns a {@code List} containing {@code Player} objects.
	 * @return A {@code List} of {@code Player} instances.
	 */
	public List<Player> getPlayers()
	{
		return _players;
	}
	
	/**
	 * Checks if a specific {@code Player} is currently inside this point.<br>
	 * If the player is found, it opens the doors and updates their state.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player was found, otherwise {@code false}.
	 */
	public boolean checkPlayer(Player player)
	{
		if (_players.contains(player))
		{
			actionDoors(true);
			for (Player pl : _players)
			{
				if (pl != null)
				{
					pl.setUCState(Player.UC_STATE_ARENA);
				}
			}
			
			return true;
		}
		
		return false;
	}
}
