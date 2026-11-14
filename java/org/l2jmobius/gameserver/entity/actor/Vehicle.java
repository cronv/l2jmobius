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
package org.l2jmobius.gameserver.entity.actor;

import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.entity.actor.stat.VehicleStat;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.GetOffVehicle;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;
import org.l2jmobius.gameserver.taskmanagers.MovementTaskManager;
import org.l2jmobius.gameserver.taskmanagers.VehicleTaskManager;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Represents a mountable or rideable entity within the game world.<br>
 * This class provides the base functionality for all types of vehicles.<br>
 * It handles movement, stats, and interaction logic for creatures that can be ridden.
 * @author DS, Skache
 */
public abstract class Vehicle extends Creature
{
	protected int _dockId = 0;
	protected final Set<Player> _passengers = ConcurrentHashMap.newKeySet();
	protected Location _oustLoc = null;
	private Runnable _engine = null;
	
	protected VehiclePathPoint[] _currentPath = null;
	protected int _runState = 0;
	private final Location _monitorLocation = new Location(this);
	
	/**
	 * Creates a new {@code Vehicle} instance based on the provided template.<br>
	 * This constructor initializes the vehicle with the correct {@code InstanceType}.<br>
	 * It also sets the flying status to {@code true} by default.
	 * @param template The {@code CreatureTemplate} used to define the vehicle properties.
	 */
	public Vehicle(CreatureTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Vehicle);
		setFlying(true);
	}
	
	/**
	 * Checks if the vehicle is a boat.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the vehicle is a boat, otherwise {@code false}.
	 */
	public boolean isBoat()
	{
		return false;
	}
	
	/**
	 * Checks if the vehicle is an airship.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the vehicle is an airship, {@code false} otherwise.
	 */
	public boolean isAirShip()
	{
		return false;
	}
	
	/**
	 * Checks if the vehicle can be manually controlled by a player.<br>
	 * This returns {@code true} if there is no active engine assigned to the vehicle.<br>
	 * It returns {@code false} if an engine is currently running.
	 * @return {@code true} if the vehicle is controllable, {@code false} otherwise.
	 */
	public boolean canBeControlled()
	{
		return _engine == null;
	}
	
	/**
	 * Registers a task to be executed by the vehicle engine.<br>
	 * This sets the {@code Runnable} that handles the vehicle's logic.
	 * @param r The {@code Runnable} task to register.
	 */
	public void registerEngine(Runnable r)
	{
		_engine = r;
	}
	
	/**
	 * Starts the vehicle engine task.<br>
	 * This method schedules the {@code _engine} runnable to execute after a specific time.<br>
	 * If no engine is registered, this method does nothing.
	 * @param delay The amount of time to wait before running the engine.
	 */
	public void runEngine(int delay)
	{
		if (_engine != null)
		{
			ThreadPool.schedule(_engine, delay);
		}
	}
	
	/**
	 * Executes a movement path for the vehicle.<br>
	 * This method sets the current path and updates the move speed and rotation speed based on the first point.<br>
	 * If the path is empty or null, it sets the AI to an active state.
	 * @param path An array of {@code VehiclePathPoint} objects defining the route.
	 */
	public void executePath(VehiclePathPoint[] path)
	{
		_runState = 0;
		_currentPath = path;
		if ((_currentPath != null) && (_currentPath.length > 0))
		{
			final VehiclePathPoint point = _currentPath[0];
			final int moveSpeed = point.getMoveSpeed();
			if (moveSpeed > 0)
			{
				getStat().setMoveSpeed(moveSpeed);
			}
			
			final int rotationSpeed = point.getRotationSpeed();
			if (rotationSpeed > 0)
			{
				getStat().setRotationSpeed(rotationSpeed);
			}
			
			getAI().setIntentionMoveTo(new Location(point.getX(), point.getY(), point.getZ(), 0));
			return;
		}
		
		getAI().setIntentionActive();
	}
	
	/**
	 * Advances the vehicle to the next point in its current path.<br>
	 * It updates movement speeds, headings, and registers the move with the {@code MovementTaskManager}.<br>
	 * If no valid path exists or movement is disabled, it runs the engine instead.
	 * @return {@code true} if the vehicle successfully started moving to a new point, otherwise {@code false}.
	 */
	@Override
	public boolean moveToNextRoutePoint()
	{
		_move = null;
		if (_currentPath != null)
		{
			_runState++;
			if (_runState < _currentPath.length)
			{
				final VehiclePathPoint point = _currentPath[_runState];
				if (!isMovementDisabled())
				{
					final int moveSpeed = point.getMoveSpeed();
					if (moveSpeed == 0)
					{
						point.setHeading(point.getRotationSpeed());
						teleToLocation(point, false);
						VehicleTaskManager.getInstance().remove(this);
						_currentPath = null;
					}
					else
					{
						if (moveSpeed > 0)
						{
							getStat().setMoveSpeed(moveSpeed);
						}
						
						final int rotationSpeed = point.getRotationSpeed();
						if (rotationSpeed > 0)
						{
							getStat().setRotationSpeed(rotationSpeed);
						}
						
						final MoveData m = new MoveData();
						m.disregardingGeodata = false;
						m.onGeodataPathIndex = -1;
						final int pointX = point.getX();
						final int pointY = point.getY();
						m.xDestination = pointX;
						m.yDestination = pointY;
						m.zDestination = point.getZ();
						m.heading = 0;
						
						final double dx = pointX - getX();
						final double dy = pointY - getY();
						final double distance = Math.sqrt((dx * dx) + (dy * dy));
						if (distance > 1)
						{
							setHeading(LocationUtil.calculateHeadingFrom(getX(), getY(), pointX, pointY));
						}
						
						m.moveStartTime = GameTimeTaskManager.getInstance().getGameTicks();
						_move = m;
						MovementTaskManager.getInstance().registerMovingObject(this);
						
						// Make sure vehicle is not stuck.
						VehicleTaskManager.getInstance().add(this);
						
						return true;
					}
				}
			}
			else
			{
				VehicleTaskManager.getInstance().remove(this);
				_currentPath = null;
			}
		}
		
		runEngine(10);
		return false;
	}
	
	/**
	 * Verifies that the vehicle is advancing along its current path.<br>
	 * Called every second by the {@code VehicleTaskManager} while the vehicle is moving.<br>
	 * A vehicle that stopped advancing resumes its path or broadcasts its info.
	 */
	public void monitorMovement()
	{
		if (!isInDock() && (calculateDistance3D(_monitorLocation) == 0))
		{
			if (_currentPath != null)
			{
				if (_runState < _currentPath.length)
				{
					_runState = Math.max(0, _runState - 1);
					moveToNextRoutePoint();
				}
				else
				{
					broadcastInfo();
				}
			}
		}
		else
		{
			_monitorLocation.setXYZ(this);
		}
	}
	
	/**
	 * Retrieves the statistics for this vehicle.<br>
	 * This method calls the parent {@code Creature} class to get the stats.<br>
	 * The result is cast to a {@code VehicleStat} object.
	 * @return the {@code VehicleStat} associated with this entity.
	 */
	@Override
	public VehicleStat getStat()
	{
		return (VehicleStat) super.getStat();
	}
	
	/**
	 * Initializes the {@code VehicleStat} for this vehicle.<br>
	 * This method creates a new {@code VehicleStat} instance and assigns it to the current entity.
	 */
	@Override
	public void initCharStat()
	{
		setStat(new VehicleStat(this));
	}
	
	/**
	 * Checks if the vehicle is currently inside a dock.<br>
	 * It returns {@code true} if the {@code _dockId} is greater than {@code 0}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if in a dock, {@code false} otherwise.
	 */
	public boolean isInDock()
	{
		return _dockId > 0;
	}
	
	/**
	 * Retrieves the unique identifier of the dock. <br>
	 * This value is used to identify which specific dock the vehicle is associated with.
	 * @return The {@code int} ID of the dock.
	 */
	public int getDockId()
	{
		return _dockId;
	}
	
	/**
	 * Sets the unique identifier for the dock.<br>
	 * This value is used to determine which dock the vehicle is currently at.
	 * @param d The {@code int} ID of the dock.
	 */
	public void setInDock(int d)
	{
		_dockId = d;
	}
	
	/**
	 * Sets the location where passengers are sent when they are ousted.<br>
	 * This updates the internal {@code _oustLoc} field with the provided {@code Location}.
	 * @param loc The new destination for ousted players.
	 */
	public void setOustLoc(Location loc)
	{
		_oustLoc = loc;
	}
	
	/**
	 * Retrieves the location where passengers are sent when they are ousted.<br>
	 * If no specific location is set, it returns a default town location.
	 * @return the {@code Location} of the oust point or a default town location.
	 */
	public Location getOustLoc()
	{
		return _oustLoc != null ? _oustLoc : MapRegionData.getInstance().getTeleToLocation(this, TeleportWhereType.TOWN);
	}
	
	/**
	 * Removes all passengers from the vehicle.<br>
	 * This method iterates through the {@code _passengers} set.<br>
	 * It calls {@code oustPlayer(player)} for each passenger found.
	 */
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
	 * Removes a specific {@code Player} from the vehicle.<br>
	 * This method clears the player's vehicle status and position.<br>
	 * It also removes the player from the internal passenger list.
	 * @param player The {@code Player} to remove from the vehicle.
	 */
	public void oustPlayer(Player player)
	{
		player.setVehicle(null);
		player.setInVehiclePosition(null);
		removePassenger(player);
	}
	
	/**
	 * Adds a {@code Player} to the vehicle as a passenger.<br>
	 * This method checks if the player is valid and not already on this vehicle.<br>
	 * It also ensures the player is not currently riding another vehicle.
	 * @param player The {@code Player} to add to the vehicle.
	 * @return {@code true} if the player was successfully added, {@code false} otherwise.
	 */
	public boolean addPassenger(Player player)
	{
		if ((player == null) || _passengers.contains(player))
		{
			return false;
		}
		
		// Already in other vehicle.
		final Vehicle currentVehicle = player.getVehicle();
		if ((currentVehicle != null) && (currentVehicle != this))
		{
			return false;
		}
		
		_passengers.add(player);
		return true;
	}
	
	/**
	 * Removes a specific player from the vehicle's passenger list.<br>
	 * This method handles any exceptions that occur during removal.
	 * @param player The {@code Player} to be removed.
	 */
	public void removePassenger(Player player)
	{
		try
		{
			_passengers.remove(player);
		}
		catch (Exception e)
		{
		}
	}
	
	/**
	 * Checks if the vehicle has any passengers.<br>
	 * Returns {@code true} if there are no players in the passenger set.<br>
	 * Returns {@code false} if there is at least one player present.
	 * @return a boolean value indicating if the vehicle is empty.
	 */
	public boolean isEmpty()
	{
		return _passengers.isEmpty();
	}
	
	/**
	 * Retrieves all players currently riding this vehicle.
	 * @return a {@code Set} of {@code Player} objects.
	 */
	public Set<Player> getPassengers()
	{
		return _passengers;
	}
	
	/**
	 * Sends a network packet to all players currently riding this vehicle.<br>
	 * It iterates through the {@code _passengers} set and sends the data.<br>
	 * The method ensures that only non-{@code null} players receive the packet.
	 * @param packet The {@code ServerPacket} to be sent to each passenger.
	 */
	public void broadcastToPassengers(ServerPacket packet)
	{
		for (Player player : _passengers)
		{
			if (player != null)
			{
				player.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Processes the payment for passengers to board this vehicle.<br>
	 * It checks if players have the required tickets and removes them from their inventory.<br>
	 * If a player lacks a valid ticket, they are teleported to a specified location.
	 * @param itemId The unique identifier of the item required as a ticket.
	 * @param count The number of items to consume for each passenger.
	 * @param oustX The X coordinate where players without tickets will be sent.
	 * @param oustY The Y coordinate where players without tickets will be sent.
	 * @param oustZ The Z coordinate where players without tickets will be sent.
	 */
	public void payForRide(int itemId, int count, int oustX, int oustY, int oustZ)
	{
		World.forEachVisibleObjectInRange(this, Player.class, 1000, player ->
		{
			if (player.isInBoat() && (player.getBoat() == this))
			{
				if (itemId > 0)
				{
					final Item ticket = player.getInventory().getItemByItemId(itemId);
					if ((ticket == null) || !player.destroyItem(ItemProcessType.FEE, ticket, count, this, true))
					{
						player.sendPacket(SystemMessageId.YOU_DO_NOT_POSSESS_THE_CORRECT_TICKET_TO_BOARD_THE_BOAT);
						
						// The client still counts the player as aboard unless it is told the player got off.
						player.broadcastPacket(new GetOffVehicle(player.getObjectId(), getObjectId(), oustX, oustY, oustZ));
						player.teleToLocation(new Location(oustX, oustY, oustZ), true);
						return;
					}
				}
				
				addPassenger(player);
			}
		});
	}
	
	/**
	 * Updates the position of the vehicle and its passengers.<br>
	 * This method calls the parent {@code updatePosition()} logic.<br>
	 * It then synchronizes the coordinates for all players currently on the vehicle.
	 * @return {@code true} if the position was updated successfully, otherwise {@code false}.
	 */
	@Override
	public boolean updatePosition()
	{
		final boolean result = super.updatePosition();
		for (Player player : _passengers)
		{
			if ((player != null) && (player.getVehicle() == this))
			{
				player.setXYZ(getX(), getY(), getZ());
				player.revalidateZone(false);
			}
		}
		
		return result;
	}
	
	/**
	 * Teleports this vehicle and all its passengers to a specific location.<br>
	 * It stops any current movement before performing the teleport.<br>
	 * The heading of the vehicle is updated to match the destination if it is not {@code 0}.
	 * @param loc The destination where the vehicle will be moved.
	 * @param allowRandomOffset Determines whether a random offset should be applied to the destination.
	 */
	@Override
	public void teleToLocation(ILocational loc, boolean allowRandomOffset)
	{
		if (isMoving())
		{
			stopMove(null);
		}
		
		setTeleporting(true);
		
		getAI().setIntentionActive();
		
		for (Player player : _passengers)
		{
			if (player != null)
			{
				player.teleToLocation(loc, false);
			}
		}
		
		decayMe();
		setXYZ(loc);
		
		// Temporary fix for heading on teleports.
		final int locHeading = loc.getHeading();
		if (locHeading != 0)
		{
			setHeading(locHeading);
		}
		
		onTeleported();
		revalidateZone(true);
	}
	
	/**
	 * Stops the current movement of the vehicle.<br>
	 * Sets the internal move state to {@code null}.<br>
	 * Updates the position and heading if a location is provided.
	 * @param loc The destination where the vehicle should stop.
	 */
	@Override
	public void stopMove(Location loc)
	{
		_move = null;
		if (loc != null)
		{
			setXYZ(loc);
			setHeading(loc.getHeading());
			revalidateZone(true);
		}
	}
	
	/**
	 * Removes this vehicle from the game world.<br>
	 * This method stops movement, removes passengers, and handles decay.<br>
	 * It also ensures the entity is removed from its current zone region.
	 * @return {@code true} if the deletion was successful, otherwise {@code false}.
	 */
	@Override
	public boolean deleteMe()
	{
		_engine = null;
		VehicleTaskManager.getInstance().remove(this);
		
		try
		{
			if (isMoving())
			{
				stopMove(null);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed stopMove().", e);
		}
		
		try
		{
			oustPlayers();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed oustPlayers().", e);
		}
		
		final ZoneRegion oldZoneRegion = ZoneManager.getInstance().getRegion(this);
		
		try
		{
			decayMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed decayMe().", e);
		}
		
		oldZoneRegion.removeFromZones(this);
		
		return super.deleteMe();
	}
	
	/**
	 * Retrieves the current weapon instance being used by the vehicle.<br>
	 * This method returns {@code null} if no weapon is currently active.
	 * @return The active {@code Item} instance or {@code null}.
	 */
	@Override
	public Item getActiveWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the weapon currently held by this vehicle.<br>
	 * This method returns {@code null} if no weapon is equipped.
	 * @return The current {@code Weapon} object or {@code null}.
	 */
	@Override
	public Weapon getActiveWeaponItem()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon instance of the vehicle.<br>
	 * This method currently returns {@code null}.
	 * @return The {@code Item} representing the secondary weapon, or {@code null}.
	 */
	@Override
	public Item getSecondaryWeaponInstance()
	{
		return null;
	}
	
	/**
	 * Retrieves the secondary weapon of the vehicle.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code Weapon} object if it exists, otherwise {@code null}
	 */
	@Override
	public Weapon getSecondaryWeaponItem()
	{
		return null;
	}
	
	/**
	 * Returns the current level of the vehicle.<br>
	 * This method currently returns a default value of {@code 0}.
	 * @return The integer level of the vehicle.
	 */
	@Override
	public int getLevel()
	{
		return 0;
	}
	
	/**
	 * Checks if the specified creature can attack this vehicle.<br>
	 * This method currently always returns {@code false}.
	 * @param attacker The {@code Creature} attempting to perform an attack.
	 * @return {@code false} regardless of the input.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Removes the AI logic from this {@code Vehicle}.<br>
	 * This method stops all automated behaviors for the entity.
	 */
	@Override
	public void detachAI()
	{
	}
	
	/**
	 * Checks if the current entity is a vehicle.<br>
	 * This method always returns {@code true} because this class extends {@code Vehicle}.
	 * @return {@code true} if the entity is a vehicle.
	 */
	@Override
	public boolean isVehicle()
	{
		return true;
	}
}
