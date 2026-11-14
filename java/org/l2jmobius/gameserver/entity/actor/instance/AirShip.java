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

import org.l2jmobius.gameserver.ai.AirShipAI;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Vehicle;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.managers.AirShipManager;
import org.l2jmobius.gameserver.network.serverpackets.ExAirShipInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExGetOffAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExGetOnAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExMoveToLocationAirShip;
import org.l2jmobius.gameserver.network.serverpackets.ExStopMoveAirShip;

/**
 * Represents a flying airship entity in the game world.<br>
 * This class functions similarly to {@code Boat} but is designed for aerial movement.
 * @author DrHouse, DS
 */
public class AirShip extends Vehicle
{
	/**
	 * Creates a new {@code AirShip} instance using the provided template.<br>
	 * This constructor initializes the ship with its specific type and AI.
	 * @param template The {@code CreatureTemplate} used to define the airship properties.
	 */
	public AirShip(CreatureTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.AirShip);
		setAI(new AirShipAI(this));
	}
	
	/**
	 * Checks if the current entity is an airship.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if this object is an airship.
	 */
	@Override
	public boolean isAirShip()
	{
		return true;
	}
	
	/**
	 * Checks if the specified {@code Player} is the owner of this airship.<br>
	 * This method currently always returns {@code false}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is the owner, otherwise {@code false}.
	 */
	public boolean isOwner(Player player)
	{
		return false;
	}
	
	/**
	 * Retrieves the unique identifier of the owner.<br>
	 * This value is used to identify which {@code Player} owns this airship.
	 * @return The {@code int} ID of the owner.
	 */
	public int getOwnerId()
	{
		return 0;
	}
	
	/**
	 * Checks if the specified {@code Player} is the captain of this airship.<br>
	 * This method currently always returns {@code false}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is the captain, otherwise {@code false}.
	 */
	public boolean isCaptain(Player player)
	{
		return false;
	}
	
	/**
	 * Retrieves the unique identifier of the airship captain.<br>
	 * Returns {@code 0} if no captain is currently assigned.
	 * @return The {@code int} ID of the captain.
	 */
	public int getCaptainId()
	{
		return 0;
	}
	
	/**
	 * Retrieves the unique identifier for the helm object.<br>
	 * This value is used to identify which specific item is being used to steer the airship.
	 * @return The {@code int} ID of the helm object.
	 */
	public int getHelmObjectId()
	{
		return 0;
	}
	
	/**
	 * Retrieves the unique identifier for the helm item.<br>
	 * This value is used to identify which specific item is currently at the helm.
	 * @return The {@code int} ID of the helm item.
	 */
	public int getHelmItemId()
	{
		return 0;
	}
	
	/**
	 * Sets the captain of this airship.<br>
	 * This method currently always returns {@code false}.
	 * @param player The {@code Player} to be set as the captain.
	 * @return Always returns {@code false}.
	 */
	public boolean setCaptain(Player player)
	{
		return false;
	}
	
	/**
	 * Retrieves the current fuel level of the airship.<br>
	 * This value represents how much fuel is currently available for use.
	 * @return The current amount of fuel as an {@code int}.
	 */
	public int getFuel()
	{
		return 0;
	}
	
	/**
	 * Sets the current fuel level of the airship.<br>
	 * This updates the amount of fuel available for movement.
	 * @param f The new fuel value to set.
	 */
	public void setFuel(int f)
	{
	}
	
	/**
	 * Retrieves the maximum fuel capacity of the airship.<br>
	 * This value represents the upper limit for the {@code getFuel()} method.
	 * @return The maximum amount of fuel the airship can hold.
	 */
	public int getMaxFuel()
	{
		return 0;
	}
	
	/**
	 * Sets the maximum fuel capacity for this {@code AirShip}.<br>
	 * This value determines the upper limit of fuel the ship can hold.
	 * @param mf The new maximum fuel amount.
	 */
	public void setMaxFuel(int mf)
	{
	}
	
	/**
	 * Retrieves the unique identifier for this {@code AirShip}.<br>
	 * This value is used to distinguish between different airship instances.
	 * @return The current ID of the airship.
	 */
	@Override
	public int getId()
	{
		return 0;
	}
	
	/**
	 * Moves the airship to its next scheduled route point.<br>
	 * This method updates the position and broadcasts the new location to players.
	 * @return {@code true} if the move was successful, or {@code false} otherwise.
	 */
	@Override
	public boolean moveToNextRoutePoint()
	{
		final boolean result = super.moveToNextRoutePoint();
		if (result)
		{
			broadcastPacket(new ExMoveToLocationAirShip(this));
		}
		
		return result;
	}
	
	/**
	 * Adds a {@code Player} to the airship as a passenger.<br>
	 * This method updates the player's position and vehicle status.<br>
	 * It returns {@code true} if the operation succeeds.<br>
	 * It returns {@code false} if the player cannot be added.
	 * @param player The {@code Player} to add to the airship.
	 * @return {@code true} if successful, {@code false} otherwise.
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
		player.broadcastPacket(new ExGetOnAirShip(player, this));
		player.setXYZ(getX(), getY(), getZ());
		player.revalidateZone(true);
		player.stopMove(null);
		return true;
	}
	
	/**
	 * Removes a {@code Player} from the airship.<br>
	 * The player is moved to a specific location and notified via packet.<br>
	 * If the player is offline, their coordinates are updated invisibly.
	 * @param player The {@code Player} to remove from the vehicle.
	 */
	@Override
	public void oustPlayer(Player player)
	{
		super.oustPlayer(player);
		final Location loc = getOustLoc();
		if (player.isOnline())
		{
			final int x = loc.getX();
			final int y = loc.getY();
			final int z = loc.getZ();
			player.broadcastPacket(new ExGetOffAirShip(player, this, x, y, z));
			player.teleToLocation(x, y, z);
		}
		else
		{
			player.setXYZInvisible(loc.getX(), loc.getY(), loc.getZ());
		}
	}
	
	/**
	 * Removes this airship from the game world.<br>
	 * It calls the parent {@code deleteMe()} method first.<br>
	 * It also removes the instance from the {@code AirShipManager}.
	 * @return {@code true} if the deletion was successful, otherwise {@code false}.
	 */
	@Override
	public boolean deleteMe()
	{
		if (!super.deleteMe())
		{
			return false;
		}
		
		AirShipManager.getInstance().removeAirShip(this);
		return true;
	}
	
	/**
	 * Stops the current movement of the airship.<br>
	 * The airship will come to a halt at the specified location.<br>
	 * This method also broadcasts a packet to all players.
	 * @param loc The {@code Location} where the airship should stop.
	 */
	@Override
	public void stopMove(Location loc)
	{
		super.stopMove(loc);
		
		broadcastPacket(new ExStopMoveAirShip(this));
	}
	
	/**
	 * Updates and synchronizes abnormal visual effects for the airship.<br>
	 * This method broadcasts the current state to all connected clients.<br>
	 * It ensures that visual glitches are corrected on the client side.
	 */
	@Override
	public void updateAbnormalVisualEffects()
	{
		broadcastPacket(new ExAirShipInfo(this));
	}
	
	/**
	 * Sends the current airship information to a specific player.<br>
	 * This method checks if the {@code Player} can see this airship before sending the packet.<br>
	 * It uses the {@code ExAirShipInfo} packet to transmit data.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		if (isVisibleFor(player))
		{
			player.sendPacket(new ExAirShipInfo(this));
		}
	}
}
