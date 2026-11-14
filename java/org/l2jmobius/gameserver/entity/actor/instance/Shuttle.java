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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.Iterator;
import java.util.List;

import org.l2jmobius.gameserver.ai.ShuttleAI;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Vehicle;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.entity.shuttle.ShuttleDataHolder;
import org.l2jmobius.gameserver.entity.shuttle.ShuttleStop;
import org.l2jmobius.gameserver.network.serverpackets.shuttle.ExShuttleGetOff;
import org.l2jmobius.gameserver.network.serverpackets.shuttle.ExShuttleGetOn;
import org.l2jmobius.gameserver.network.serverpackets.shuttle.ExShuttleInfo;

/**
 * Represents a shuttle vehicle that transports players between different locations.<br>
 * This class handles the logic for boarding, departing, and moving along defined routes.
 * @author UnAfraid
 */
public class Shuttle extends Vehicle
{
	private ShuttleDataHolder _shuttleData;
	
	/**
	 * Creates a new {@code Shuttle} instance based on the provided template.<br>
	 * This constructor initializes the shuttle with its specific AI and type.
	 * @param template The {@code CreatureTemplate} used to define the shuttle properties.
	 */
	public Shuttle(CreatureTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Shuttle);
		setAI(new ShuttleAI(this));
	}
	
	/**
	 * Retrieves the list of stops for this shuttle.<br>
	 * This method fetches data from the internal {@code ShuttleDataHolder}.
	 * @return a {@code List} of {@code ShuttleStop} objects.
	 */
	public List<ShuttleStop> getStops()
	{
		return _shuttleData.getStops();
	}
	
	/**
	 * Closes the door of a specific shuttle stop.<br>
	 * This method searches for a {@code ShuttleStop} by its unique identifier.<br>
	 * If the stop is found, it calls the {@code closeDoor()} method on that stop.
	 * @param id The unique identifier of the shuttle stop to close.
	 */
	public void closeDoor(int id)
	{
		for (ShuttleStop stop : _shuttleData.getStops())
		{
			if (stop.getId() == id)
			{
				stop.closeDoor();
				break;
			}
		}
	}
	
	/**
	 * Opens the door for a specific shuttle stop.<br>
	 * This method searches through all stops in the {@code ShuttleDataHolder}.<br>
	 * It calls the {@code openDoor()} method on the stop that matches the provided ID.
	 * @param id The unique identifier of the shuttle stop to open.
	 */
	public void openDoor(int id)
	{
		for (ShuttleStop stop : _shuttleData.getStops())
		{
			if (stop.getId() == id)
			{
				stop.openDoor();
				break;
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for this shuttle.<br>
	 * This value is obtained from the {@code ShuttleDataHolder}.
	 * @return The {@code int} ID of the shuttle.
	 */
	@Override
	public int getId()
	{
		return _shuttleData.getId();
	}
	
	/**
	 * Adds a {@code Player} to the shuttle as a passenger.<br>
	 * This method updates the player's position and vehicle status.<br>
	 * It returns {@code true} if the operation succeeded.<br>
	 * It returns {@code false} if the player could not be added.
	 * @param player The {@code Player} to add to the shuttle.
	 * @return {@code true} if successful, otherwise {@code false}.
	 */
	@Override
	public boolean addPassenger(Player player)
	{
		if (!super.addPassenger(player))
		{
			return false;
		}
		
		player.setVehicle(this);
		player.setInVehiclePosition(new Location(0, 0, 0));
		player.broadcastPacket(new ExShuttleGetOn(player, this));
		player.setXYZ(getX(), getY(), getZ());
		player.revalidateZone(true);
		return true;
	}
	
	/**
	 * Removes a {@code Player} from the shuttle.<br>
	 * This method updates the player's position to the specified coordinates.<br>
	 * It also sends an {@code ExShuttleGetOff} packet if the player is online.
	 * @param player The {@code Player} to remove from the vehicle.
	 * @param x The new X coordinate for the player.
	 * @param y The new Y coordinate for the player.
	 * @param z The new Z coordinate for the player.
	 */
	public void removePassenger(Player player, int x, int y, int z)
	{
		oustPlayer(player);
		if (player.isOnline())
		{
			player.broadcastPacket(new ExShuttleGetOff(player, this, x, y, z));
			player.setXYZ(x, y, z);
			player.revalidateZone(true);
		}
		else
		{
			player.setXYZInvisible(x, y, z);
		}
	}
	
	/**
	 * Removes all players from the shuttle.<br>
	 * This method iterates through the passenger list and calls {@code oustPlayer} for each one.
	 */
	@Override
	public void oustPlayers()
	{
		Player player;
		
		// Use iterator because oustPlayer will try to remove player from _passengers.
		final Iterator<Player> iter = _passengers.iterator();
		while (iter.hasNext())
		{
			player = iter.next();
			iter.remove();
			if (player != null)
			{
				oustPlayer(player);
			}
		}
	}
	
	/**
	 * Sends shuttle information to a specific player.<br>
	 * This method creates an {@code ExShuttleInfo} packet.<br>
	 * It then sends that packet to the provided {@code Player}.
	 * @param player The {@code Player} who will receive the shuttle data.
	 */
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new ExShuttleInfo(this));
	}
	
	/**
	 * Sends the shuttle information to all nearby players.<br>
	 * This method creates a new {@code ExShuttleInfo} packet.<br>
	 * It uses the current {@code Shuttle} instance to populate the data.
	 */
	public void broadcastShuttleInfo()
	{
		broadcastPacket(new ExShuttleInfo(this));
	}
	
	/**
	 * Updates the internal shuttle data.<br>
	 * This method assigns a {@code ShuttleDataHolder} object to the instance.
	 * @param data The {@code ShuttleDataHolder} to be stored.
	 */
	public void setData(ShuttleDataHolder data)
	{
		_shuttleData = data;
	}
	
	/**
	 * Retrieves the current shuttle data.<br>
	 * This method returns the {@code ShuttleDataHolder} associated with this shuttle.
	 * @return the {@code ShuttleDataHolder} object.
	 */
	public ShuttleDataHolder getShuttleData()
	{
		return _shuttleData;
	}
}
