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
package org.l2jmobius.gameserver.managers;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.holders.AirShipHolder;
import org.l2jmobius.gameserver.data.holders.AirShipPortHolder;
import org.l2jmobius.gameserver.data.holders.AirShipStepHolder;
import org.l2jmobius.gameserver.data.xml.AirShipData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.entity.actor.instance.ControllableAirShip;
import org.l2jmobius.gameserver.entity.actor.stat.VehicleStat;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.ExAirShipTeleportList;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Spawns the scheduled airships defined in {@code data/AirShipData.xml} and drives them along their route steps.<br>
 * The route is split into legs ending at a port or a teleport point.<br>
 * At ports the airship docks, ousts the passengers and waits the stopover, announcing through the nearby controller npc.<br>
 * Also manages the private airships, their licenses, fuel and dock teleport lists.
 * @author Mobius
 */
public class AirShipManager
{
	private static final Logger LOGGER = Logger.getLogger(AirShipManager.class.getName());
	
	private static final int TELEPORT_DELAY = 5000;
	private static final int ANNOUNCE_RANGE = 600;
	
	private static final String LOAD_DB = "SELECT * FROM airships";
	private static final String ADD_DB = "INSERT INTO airships (owner_id,fuel) VALUES (?,?)";
	private static final String UPDATE_DB = "UPDATE airships SET fuel=? WHERE owner_id=?";
	
	private final Map<Integer, AirShip> _airships = new ConcurrentHashMap<>();
	private CreatureTemplate _airShipTemplate = null;
	private final Map<Integer, StatSet> _airShipsInfo = new HashMap<>();
	private final Map<Integer, AirShip> _privateAirShips = new HashMap<>();
	private final Map<Integer, AirShipTeleportList> _teleports = new HashMap<>();
	
	protected AirShipManager()
	{
		final StatSet npcDat = new StatSet();
		npcDat.set("npcId", 9);
		npcDat.set("level", 0);
		npcDat.set("jClass", "boat");
		npcDat.set("baseSTR", 0);
		npcDat.set("baseCON", 0);
		npcDat.set("baseDEX", 0);
		npcDat.set("baseINT", 0);
		npcDat.set("baseWIT", 0);
		npcDat.set("baseMEN", 0);
		npcDat.set("baseShldDef", 0);
		npcDat.set("baseShldRate", 0);
		npcDat.set("baseAccCombat", 38);
		npcDat.set("baseEvasRate", 38);
		npcDat.set("baseCritRate", 38);
		npcDat.set("collision_radius", 0);
		npcDat.set("collision_height", 0);
		npcDat.set("sex", "male");
		npcDat.set("type", "");
		npcDat.set("baseAtkRange", 0);
		npcDat.set("baseMpMax", 0);
		npcDat.set("baseCpMax", 0);
		npcDat.set("rewardExp", 0);
		npcDat.set("rewardSp", 0);
		npcDat.set("basePAtk", 0);
		npcDat.set("baseMAtk", 0);
		npcDat.set("basePAtkSpd", 0);
		npcDat.set("aggroRange", 0);
		npcDat.set("baseMAtkSpd", 0);
		npcDat.set("rhand", 0);
		npcDat.set("lhand", 0);
		npcDat.set("armor", 0);
		npcDat.set("baseWalkSpd", 0);
		npcDat.set("baseRunSpd", 0);
		npcDat.set("name", "AirShip");
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);
		_airShipTemplate = new CreatureTemplate(npcDat);
		
		load();
		
		final List<AirShipHolder> airships = AirShipData.getInstance().getAirShips();
		if (airships.isEmpty())
		{
			return;
		}
		
		for (AirShipHolder holder : airships)
		{
			final List<AirShipLeg> legs = buildLegs(holder);
			if (legs.isEmpty())
			{
				LOGGER.warning(getClass().getSimpleName() + ": Airship " + holder.getId() + " has no route legs.");
				continue;
			}
			
			final AirShip airship = createAirShip(holder.getStart());
			_airships.put(holder.getId(), airship);
			airship.setOustLoc(findNextPortOust(holder.getSteps(), -1));
			airship.registerEngine(new AirShipRoute(airship, holder.getId(), legs));
			airship.runEngine(holder.getInitialDelay());
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Spawned " + _airships.size() + " airships.");
	}
	
	/**
	 * Creates and spawns a new scheduled {@code AirShip}.
	 * @param location The spawn {@code Location} including the spawn heading.
	 * @return The spawned {@code AirShip}.
	 */
	private AirShip createAirShip(Location location)
	{
		final AirShip airship = new AirShip(_airShipTemplate);
		airship.setHeading(location.getHeading());
		airship.setXYZInvisible(location.getX(), location.getY(), location.getZ());
		airship.spawnMe();
		final VehicleStat airshipStat = airship.getStat();
		airshipStat.setMoveSpeed(280);
		airshipStat.setRotationSpeed(2000);
		return airship;
	}
	
	/**
	 * Retrieves a spawned scheduled {@code AirShip} by its airship id.
	 * @param id The airship id from AirShipData.xml.
	 * @return The {@code AirShip} with the given id, or {@code null} if none exists.
	 */
	public AirShip getAirShip(int id)
	{
		return _airships.get(id);
	}
	
	/**
	 * Creates or retrieves an {@code AirShip} for a specific owner.<br>
	 * This method initializes the ship with default stats if it does not exist.<br>
	 * It then updates the position and heading before spawning the entity.
	 * @param x The X coordinate of the spawn location.
	 * @param y The Y coordinate of the spawn location.
	 * @param z The Z coordinate of the spawn location.
	 * @param heading The initial rotation angle of the ship.
	 * @param ownerId The unique identifier of the player who owns the ship.
	 * @return The spawned {@code AirShip} instance or {@code null} if the owner has no data.
	 */
	public AirShip getNewAirShip(int x, int y, int z, int heading, int ownerId)
	{
		final StatSet info = _airShipsInfo.get(ownerId);
		if (info == null)
		{
			return null;
		}
		
		AirShip airShip = _privateAirShips.get(ownerId);
		if (airShip != null)
		{
			airShip.refreshId();
		}
		else
		{
			airShip = new ControllableAirShip(_airShipTemplate, ownerId);
			_privateAirShips.put(ownerId, airShip);
			
			airShip.setMaxFuel(600);
			airShip.setFuel(info.getInt("fuel"));
			final VehicleStat airShipStat = airShip.getStat();
			airShipStat.setMoveSpeed(280);
			airShipStat.setRotationSpeed(2000);
		}
		
		airShip.setHeading(heading);
		airShip.setXYZInvisible(x, y, z);
		airShip.spawnMe();
		return airShip;
	}
	
	/**
	 * Removes an {@code AirShip} from the active task manager.<br>
	 * This method updates the database and current fuel stats for the owner.
	 * @param ship The {@code AirShip} object to be removed.
	 */
	public void removeAirShip(AirShip ship)
	{
		final int shipOwnerId = ship.getOwnerId();
		if (shipOwnerId != 0)
		{
			storeInDb(shipOwnerId);
			final StatSet info = _airShipsInfo.get(shipOwnerId);
			if (info != null)
			{
				info.set("fuel", ship.getFuel());
			}
		}
	}
	
	/**
	 * Checks if a specific owner has an airship license.<br>
	 * This method looks up the {@code ownerId} in the internal records.
	 * @param ownerId The unique identifier of the player or entity.
	 * @return {@code true} if the license exists, {@code false} otherwise.
	 */
	public boolean hasAirShipLicense(int ownerId)
	{
		return _airShipsInfo.containsKey(ownerId);
	}
	
	/**
	 * Registers a new airship license for a specific owner.<br>
	 * This method initializes the fuel level to 600 and saves it to the database.<br>
	 * It checks if the {@code ownerId} already exists before creating a new entry.
	 * @param ownerId The unique identifier of the player who will own the license.
	 */
	public void registerLicense(int ownerId)
	{
		if (!_airShipsInfo.containsKey(ownerId))
		{
			final StatSet info = new StatSet();
			info.set("fuel", 600);
			_airShipsInfo.put(ownerId, info);
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(ADD_DB))
			{
				ps.setInt(1, ownerId);
				ps.setInt(2, info.getInt("fuel"));
				ps.executeUpdate();
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not add new airship license: ", e);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while initializing: ", e);
			}
		}
	}
	
	/**
	 * Checks if a specific owner currently has an active airship.<br>
	 * An airship is considered active if it is spawned or teleporting.
	 * @param ownerId The unique identifier of the player owner.
	 * @return {@code true} if the owner has an active ship, {@code false} otherwise.
	 */
	public boolean hasAirShip(int ownerId)
	{
		final AirShip ship = _privateAirShips.get(ownerId);
		return (ship != null) && (ship.isSpawned() || ship.isTeleporting());
	}
	
	/**
	 * Registers a list of teleport destinations for a specific dock.<br>
	 * This method maps the {@code locationId} and path points to a {@code dockId}.<br>
	 * It ensures that the number of paths matches the fuel consumption data.
	 * @param dockId The unique identifier for the airship dock.
	 * @param locationId The destination ID for the teleport.
	 * @param tp A 2D array containing the path points for the teleport.
	 * @param fuelConsumption An array containing the fuel costs for each path.
	 */
	public void registerAirShipTeleportList(int dockId, int locationId, VehiclePathPoint[][] tp, int[] fuelConsumption)
	{
		if (tp.length != fuelConsumption.length)
		{
			return;
		}
		
		_teleports.put(dockId, new AirShipTeleportList(locationId, fuelConsumption, tp));
	}
	
	/**
	 * Sends the available teleport list to a player.<br>
	 * This method checks if the {@code Player} is currently in an {@code AirShip}.<br>
	 * It verifies that the player is the captain and the ship is docked.<br>
	 * If all conditions are met, it sends the {@code ExAirShipTeleportList} packet.
	 * @param player The {@code Player} who will receive the teleport list.
	 */
	public void sendAirShipTeleportList(Player player)
	{
		if ((player == null) || !player.isInAirShip())
		{
			return;
		}
		
		final AirShip ship = player.getAirShip();
		if (!ship.isCaptain(player) || !ship.isInDock() || ship.isMoving())
		{
			return;
		}
		
		final int dockId = ship.getDockId();
		if (!_teleports.containsKey(dockId))
		{
			return;
		}
		
		final AirShipTeleportList all = _teleports.get(dockId);
		player.sendPacket(new ExAirShipTeleportList(all.getLocation(), all.getRoute(), all.getFuel()));
	}
	
	/**
	 * Retrieves the destination point for a specific teleport from a dock.<br>
	 * This method looks up the route associated with the provided {@code dockId}.<br>
	 * It returns the path point at the specified {@code index}.
	 * @param dockId The unique identifier of the dock.
	 * @param index The position in the teleport route to retrieve.
	 * @return An array of {@code VehiclePathPoint} representing the destination, or {@code null} if not found.
	 */
	public VehiclePathPoint[] getTeleportDestination(int dockId, int index)
	{
		final AirShipTeleportList all = _teleports.get(dockId);
		if (all == null)
		{
			return null;
		}
		
		final VehiclePathPoint[][] route = all.getRoute();
		if ((index < -1) || (index >= route.length))
		{
			return null;
		}
		
		return route[index + 1];
	}
	
	/**
	 * Retrieves the fuel cost for a specific teleport destination.<br>
	 * This method looks up the data based on the provided dock and index.<br>
	 * It returns 0 if the dock or index is invalid.
	 * @param dockId The unique identifier of the dock.
	 * @param index The position of the teleport in the list.
	 * @return The amount of fuel consumed for the destination.
	 */
	public int getFuelConsumption(int dockId, int index)
	{
		final AirShipTeleportList all = _teleports.get(dockId);
		if (all == null)
		{
			return 0;
		}
		
		final int[] fuel = all.getFuel();
		if ((index < -1) || (index >= fuel.length))
		{
			return 0;
		}
		
		return fuel[index + 1];
	}
	
	/**
	 * Loads all airship data from the database.<br>
	 * This method populates the internal map with owner information and fuel levels.<br>
	 * It handles {@code SQLException} by logging a warning.
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement();
			ResultSet rs = s.executeQuery(LOAD_DB))
		{
			StatSet info;
			while (rs.next())
			{
				info = new StatSet();
				info.set("fuel", rs.getInt("fuel"));
				_airShipsInfo.put(rs.getInt("owner_id"), info);
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load airships table: ", e);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while initializing: ", e);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _airShipsInfo.size() + " private airships");
	}
	
	/**
	 * Saves the current airship fuel information to the database.<br>
	 * This method updates the record for a specific owner.<br>
	 * It logs an error if the database update fails.
	 * @param ownerId The unique identifier of the player who owns the airship.
	 */
	private void storeInDb(int ownerId)
	{
		final StatSet info = _airShipsInfo.get(ownerId);
		if (info == null)
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(UPDATE_DB))
		{
			ps.setInt(1, info.getInt("fuel"));
			ps.setInt(2, ownerId);
			ps.executeUpdate();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not update airships table: ", e);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error while save: ", e);
		}
	}
	
	/**
	 * Splits the route steps of an airship into legs ending at a port or a teleport point.
	 * @param holder The {@code AirShipHolder} providing the route steps.
	 * @return The route legs in travel order.
	 */
	private List<AirShipLeg> buildLegs(AirShipHolder holder)
	{
		final List<AirShipLeg> legs = new ArrayList<>();
		final List<AirShipStepHolder> steps = holder.getSteps();
		final int moveSpeed = holder.getMoveSpeed();
		final int rotationSpeed = holder.getRotationSpeed();
		List<VehiclePathPoint> points = new ArrayList<>();
		for (int i = 0; i < steps.size(); i++)
		{
			final AirShipStepHolder step = steps.get(i);
			if (step.isTeleport())
			{
				points.add(new VehiclePathPoint(step.getX(), step.getY(), step.getZ(), 0, step.getTeleportHeading()));
				legs.add(new AirShipLeg(points.toArray(new VehiclePathPoint[points.size()]), null, findNextPortOust(steps, i)));
				points = new ArrayList<>();
			}
			else if (step.getPort() != null)
			{
				points.add(new VehiclePathPoint(step.getX(), step.getY(), step.getZ(), moveSpeed, rotationSpeed));
				legs.add(new AirShipLeg(points.toArray(new VehiclePathPoint[points.size()]), step.getPort(), null));
				points = new ArrayList<>();
			}
			else
			{
				points.add(new VehiclePathPoint(step.getX(), step.getY(), step.getZ(), moveSpeed, rotationSpeed));
			}
		}
		
		// Steps after the last port or teleport continue into the first leg.
		if (!points.isEmpty() && !legs.isEmpty())
		{
			final AirShipLeg first = legs.get(0);
			points.addAll(Arrays.asList(first.getPath()));
			legs.set(0, new AirShipLeg(points.toArray(new VehiclePathPoint[points.size()]), first.getPort(), first.getTeleportOust()));
		}
		
		return legs;
	}
	
	/**
	 * Finds the oust location of the next port after the given step index, wrapping around the route.
	 * @param steps The route steps of the airship.
	 * @param fromIndex The step index to search after, or -1 for the first port.
	 * @return The oust {@code Location} of the next port, or {@code null} if the route has no port.
	 */
	private Location findNextPortOust(List<AirShipStepHolder> steps, int fromIndex)
	{
		final int size = steps.size();
		for (int i = 1; i <= size; i++)
		{
			final AirShipPortHolder port = steps.get((fromIndex + i) % size).getPort();
			if (port != null)
			{
				return port.getOustLocation();
			}
		}
		
		return null;
	}
	
	/**
	 * Shouts an announcement through the controller npc found near the airship.
	 * @param airship The {@code AirShip} the announcement belongs to.
	 * @param port The {@code AirShipPortHolder} providing the announcer npc id.
	 * @param npcStringId The npc string id to shout.
	 */
	private void broadcastAnnouncement(AirShip airship, AirShipPortHolder port, int npcStringId)
	{
		final Npc announcer = World.getFirstVisibleObjectInRange(airship, Npc.class, ANNOUNCE_RANGE, npc -> npc.getId() == port.getAnnouncerNpcId());
		if (announcer != null)
		{
			announcer.broadcastPacket(new NpcSay(announcer.getObjectId(), ChatType.NPC_SHOUT, announcer.getId(), NpcStringId.getNpcStringId(npcStringId)));
		}
	}
	
	private static class AirShipTeleportList
	{
		private final int _location;
		private final int[] _fuel;
		private final VehiclePathPoint[][] _routes;
		
		public AirShipTeleportList(int locationValue, int[] fuelValue, VehiclePathPoint[][] routesValue)
		{
			_location = locationValue;
			_fuel = fuelValue;
			_routes = routesValue;
		}
		
		public int getLocation()
		{
			return _location;
		}
		
		public int[] getFuel()
		{
			return _fuel;
		}
		
		public VehiclePathPoint[][] getRoute()
		{
			return _routes;
		}
	}
	
	private static class AirShipLeg
	{
		private final VehiclePathPoint[] _path;
		private final AirShipPortHolder _port;
		private final Location _teleportOust;
		
		/**
		 * Creates a new {@code AirShipLeg}.
		 * @param path The path points of the leg, ending at the port or teleport point.
		 * @param port The {@code AirShipPortHolder} the leg ends at, or {@code null} when it ends at a teleport.
		 * @param teleportOust The oust {@code Location} applied after the ending teleport, or {@code null}.
		 */
		protected AirShipLeg(VehiclePathPoint[] path, AirShipPortHolder port, Location teleportOust)
		{
			_path = path;
			_port = port;
			_teleportOust = teleportOust;
		}
		
		public VehiclePathPoint[] getPath()
		{
			return _path;
		}
		
		public AirShipPortHolder getPort()
		{
			return _port;
		}
		
		public Location getTeleportOust()
		{
			return _teleportOust;
		}
	}
	
	private class AirShipRoute implements Runnable
	{
		private final AirShip _airship;
		private final int _id;
		private final List<AirShipLeg> _legs;
		private int _leg;
		private boolean _arriving;
		
		protected AirShipRoute(AirShip airship, int id, List<AirShipLeg> legs)
		{
			_airship = airship;
			_id = id;
			_legs = legs;
			_leg = legs.size() - 1;
		}
		
		@Override
		public void run()
		{
			try
			{
				final AirShipLeg leg = _legs.get(_leg);
				if (_arriving)
				{
					_arriving = false;
					final AirShipPortHolder port = leg.getPort();
					if (port != null)
					{
						broadcastAnnouncement(_airship, port, port.getArriveNpcStringId());
						_airship.setInDock(port.getDockId());
						_airship.oustPlayers();
						ThreadPool.schedule(this, port.getStopover());
					}
					else
					{
						_airship.setOustLoc(leg.getTeleportOust());
						ThreadPool.schedule(this, TELEPORT_DELAY);
					}
					
					return;
				}
				
				final AirShipPortHolder port = leg.getPort();
				if (port != null)
				{
					broadcastAnnouncement(_airship, port, port.getDepartNpcStringId());
					_airship.setInDock(0);
				}
				
				_leg = (_leg + 1) % _legs.size();
				_arriving = true;
				_airship.executePath(_legs.get(_leg).getPath());
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error executing airship " + _id + " leg " + _leg + ": " + e.getMessage());
			}
		}
	}
	
	public static AirShipManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AirShipManager INSTANCE = new AirShipManager();
	}
}
