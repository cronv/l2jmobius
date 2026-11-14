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

import java.util.logging.Logger;

import org.l2jmobius.gameserver.ai.BoatAI;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Vehicle;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.network.serverpackets.VehicleDeparture;
import org.l2jmobius.gameserver.network.serverpackets.VehicleInfo;
import org.l2jmobius.gameserver.network.serverpackets.VehicleStarted;

/**
 * Represents a boat entity within the game world.<br>
 * This class handles specific behaviors and properties for water-based vehicles.<br>
 * It extends {@code Vehicle} to provide specialized functionality for maritime travel.
 * @author Maktakien, DS
 */
public class Boat extends Vehicle
{
	protected static final Logger LOGGER_BOAT = Logger.getLogger(Boat.class.getName());
	
	/**
	 * Creates a new {@code Boat} instance based on the provided template.<br>
	 * This constructor initializes the boat with its specific AI and type.
	 * @param template The {@code CreatureTemplate} used to define the boat's properties.
	 */
	public Boat(CreatureTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Boat);
		setAI(new BoatAI(this));
	}
	
	/**
	 * Checks if the current entity is a boat.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the object is a boat.
	 */
	@Override
	public boolean isBoat()
	{
		return true;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Boat}.<br>
	 * This value is used to distinguish between different boat instances.
	 * @return The integer ID of the boat.
	 */
	@Override
	public int getId()
	{
		return 0;
	}
	
	/**
	 * Moves the boat to its next destination in the current route.<br>
	 * This method updates the position and sends a {@code VehicleDeparture} packet if successful.
	 * @return {@code true} if the move was successful, {@code false} otherwise.
	 */
	@Override
	public boolean moveToNextRoutePoint()
	{
		final boolean result = super.moveToNextRoutePoint();
		if (result)
		{
			broadcastPacket(new VehicleDeparture(this));
		}
		
		return result;
	}
	
	/**
	 * Removes a {@code Player} from the boat.<br>
	 * Teleports the player to a new location if they are online.<br>
	 * Sets the player's coordinates to an invisible state if they are offline.
	 * @param player The {@code Player} to be removed from the vehicle.
	 */
	@Override
	public void oustPlayer(Player player)
	{
		super.oustPlayer(player);
		
		final Location loc = getOustLoc();
		if (player.isOnline())
		{
			player.teleToLocation(loc.getX(), loc.getY(), loc.getZ());
		}
		else
		{
			player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ()); // disconnects handling
		}
	}
	
	/**
	 * Stops the current movement of the boat.<br>
	 * Updates the position to the provided {@code Location}.<br>
	 * Sends a {@code VehicleStarted} and {@code VehicleInfo} packet to all nearby players.
	 * @param loc The destination where the boat should stop.
	 */
	@Override
	public void stopMove(Location loc)
	{
		super.stopMove(loc);
		
		broadcastPacket(new VehicleStarted(this, 0));
		broadcastPacket(new VehicleInfo(this));
	}
	
	/**
	 * Sends the boat information to a specific player.<br>
	 * This method creates and sends a {@code VehicleInfo} packet.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new VehicleInfo(this));
	}
}
