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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.BoatActionHolder;
import org.l2jmobius.gameserver.data.holders.BoatHolder;
import org.l2jmobius.gameserver.data.holders.BoatStationHolder;
import org.l2jmobius.gameserver.data.xml.BoatData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.entity.actor.instance.Boat;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Spawns the boats defined in {@code data/BoatData.xml} and drives them along their station routes.<br>
 * A boat stops at every station, executes the announcement actions, waits the departure delay and moves on with the station speed.<br>
 * Stations with a dock id are occupied by the approaching boat, other boats wait until they are freed again.
 * @author Mobius
 */
public class BoatManager
{
	private static final Logger LOGGER = Logger.getLogger(BoatManager.class.getName());
	
	private static final int BOAT_TALKER_ID = 801;
	private static final int BUSY_RETRY_DELAY = 5000;
	private static final int BUSY_SHOUT_RESET = 35;
	
	private final Map<Integer, Boat> _boats = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _docks = new ConcurrentHashMap<>();
	
	protected BoatManager()
	{
		final List<BoatHolder> boats = BoatData.getInstance().getBoats();
		if (boats.isEmpty())
		{
			return;
		}
		
		for (BoatHolder holder : boats)
		{
			final BoatStationHolder spawnStation = holder.getStations().get(0);
			final Boat boat = createBoat(holder.getId(), spawnStation, holder.getHeading());
			_boats.put(boat.getObjectId(), boat);
			
			if (spawnStation.getDockId() > 0)
			{
				_docks.put(spawnStation.getDockId(), boat.getObjectId());
			}
			
			if (spawnStation.getKickLocation() != null)
			{
				boat.setOustLoc(spawnStation.getKickLocation());
			}
			
			boat.registerEngine(new BoatRoute(boat, holder));
			boat.runEngine(holder.getInitialDelay());
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Spawned " + _boats.size() + " boats.");
	}
	
	/**
	 * Creates and spawns a new {@code Boat}.
	 * @param id The boat template id.
	 * @param location The spawn {@code Location}.
	 * @param heading The spawn heading.
	 * @return The spawned {@code Boat}.
	 */
	private Boat createBoat(int id, Location location, int heading)
	{
		final StatSet npcDat = new StatSet();
		npcDat.set("npcId", id);
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
		npcDat.set("baseHpMax", 50000);
		npcDat.set("baseHpReg", 3.e-3f);
		npcDat.set("baseMpReg", 3.e-3f);
		npcDat.set("basePDef", 100);
		npcDat.set("baseMDef", 100);
		
		final Boat boat = new Boat(new CreatureTemplate(npcDat));
		boat.setHeading(heading);
		boat.setXYZInvisible(location.getX(), location.getY(), location.getZ());
		boat.spawnMe();
		return boat;
	}
	
	/**
	 * Retrieves a spawned {@code Boat} by its object id.
	 * @param objectId The object id of the boat.
	 * @return The {@code Boat} with the given object id, or {@code null} if none exists.
	 */
	public Boat getBoat(int objectId)
	{
		return _boats.get(objectId);
	}
	
	/**
	 * Executes the current station of a boat route.<br>
	 * Runs the pending announcement actions, waits the departure delay, waits for the next dock to become free and finally departs to the next station.
	 * @param route The {@code BoatRoute} being advanced.
	 */
	private void executeStation(BoatRoute route)
	{
		final Boat boat = route.getBoat();
		final BoatHolder holder = route.getHolder();
		final List<BoatStationHolder> stations = holder.getStations();
		final BoatStationHolder station = stations.get(route.getStation());
		
		// Arrival handling.
		if (route.isArriving())
		{
			route.setArriving(false);
			if (station.getKickLocation() != null)
			{
				boat.setOustLoc(station.getKickLocation());
			}
		}
		
		// Announcement actions.
		final List<BoatActionHolder> actions = station.getActions();
		while (route.getAction() < actions.size())
		{
			final BoatActionHolder action = actions.get(route.getAction());
			route.incrementAction();
			broadcastAction(boat, holder, station, action);
			if (action.getWait() > 0)
			{
				ThreadPool.schedule(route, action.getWait());
				return;
			}
		}
		
		// Departure delay.
		if (!route.hasWaitedDepartDelay() && (station.getDepartDelay() > 0))
		{
			route.setWaitedDepartDelay(true);
			ThreadPool.schedule(route, station.getDepartDelay());
			return;
		}
		
		// Wait until the next dock is freed by the other boat.
		final BoatStationHolder next = stations.get((route.getStation() + 1) % stations.size());
		final Integer occupant = next.getDockId() > 0 ? _docks.get(next.getDockId()) : null;
		if ((occupant != null) && (occupant.intValue() != boat.getObjectId()))
		{
			if ((route.incrementShoutCount() == 1) && (station.getBusyMessageId() > 0))
			{
				broadcastPackets(holder, new CreatureSay(ChatType.BOAT, BOAT_TALKER_ID, SystemMessageId.getSystemMessageId(station.getBusyMessageId())));
			}
			
			ThreadPool.schedule(route, BUSY_RETRY_DELAY);
			return;
		}
		
		// Depart to the next station.
		route.resetShoutCount();
		if (station.getDockId() > 0)
		{
			_docks.remove(station.getDockId(), boat.getObjectId());
		}
		
		if (next.getDockId() > 0)
		{
			_docks.put(next.getDockId(), boat.getObjectId());
		}
		
		if (station.getTicketId() > 0)
		{
			final Location ticketLocation = station.getTicketLocation();
			boat.payForRide(station.getTicketId(), 1, ticketLocation.getX(), ticketLocation.getY(), ticketLocation.getZ());
		}
		else if (station.getKickLocation() != null)
		{
			final Location kickLocation = station.getKickLocation();
			boat.payForRide(0, 1, kickLocation.getX(), kickLocation.getY(), kickLocation.getZ());
		}
		
		route.departTo((route.getStation() + 1) % stations.size());
		boat.executePath(new VehiclePathPoint[]
		{
			new VehiclePathPoint(next.getX(), next.getY(), next.getZ(), station.getMoveSpeed(), station.getRotationSpeed())
		});
	}
	
	/**
	 * Broadcasts an announcement action at the boat broadcast points.
	 * @param boat The {@code Boat} the announcement belongs to.
	 * @param holder The {@code BoatHolder} providing the broadcast points.
	 * @param station The {@code BoatStationHolder} providing the sound position.
	 * @param action The {@code BoatActionHolder} to broadcast.
	 */
	private void broadcastAction(Boat boat, BoatHolder holder, BoatStationHolder station, BoatActionHolder action)
	{
		final List<ServerPacket> packets = new ArrayList<>(3);
		if (action.getSound() != null)
		{
			packets.add(new PlaySound(0, action.getSound(), 1, boat.getObjectId(), station.getX(), station.getY(), station.getZ()));
		}
		
		packets.add(new CreatureSay(ChatType.BOAT, BOAT_TALKER_ID, SystemMessageId.getSystemMessageId(action.getMessageId())));
		if (action.getMessageId2() > 0)
		{
			packets.add(new CreatureSay(ChatType.BOAT, BOAT_TALKER_ID, SystemMessageId.getSystemMessageId(action.getMessageId2())));
		}
		
		broadcastPackets(holder, packets.toArray(new ServerPacket[packets.size()]));
	}
	
	/**
	 * Sends packets to all players near the broadcast points of a boat.
	 * @param holder The {@code BoatHolder} providing the broadcast points.
	 * @param packets The {@code ServerPacket} array to send.
	 */
	private void broadcastPackets(BoatHolder holder, ServerPacket... packets)
	{
		final List<Location> points = holder.getBroadcastPoints();
		final ILocational point1 = points.get(0);
		final ILocational point2 = points.size() > 1 ? points.get(1) : point1;
		final double radiusSq = (double) GeneralConfig.BOAT_BROADCAST_RADIUS * GeneralConfig.BOAT_BROADCAST_RADIUS;
		for (Player player : World.getPlayers())
		{
			final int px = player.getX();
			final int py = player.getY();
			final double dx1 = px - point1.getX();
			final double dy1 = py - point1.getY();
			final double dx2 = px - point2.getX();
			final double dy2 = py - point2.getY();
			if ((((dx1 * dx1) + (dy1 * dy1)) < radiusSq) || (((dx2 * dx2) + (dy2 * dy2)) < radiusSq))
			{
				for (ServerPacket packet : packets)
				{
					player.sendPacket(packet);
				}
			}
		}
	}
	
	private class BoatRoute implements Runnable
	{
		private final Boat _boat;
		private final BoatHolder _holder;
		private int _station;
		private int _action;
		private boolean _arriving;
		private boolean _waitedDepartDelay;
		private int _shoutCount;
		
		protected BoatRoute(Boat boat, BoatHolder holder)
		{
			_boat = boat;
			_holder = holder;
		}
		
		public Boat getBoat()
		{
			return _boat;
		}
		
		public BoatHolder getHolder()
		{
			return _holder;
		}
		
		public int getStation()
		{
			return _station;
		}
		
		public int getAction()
		{
			return _action;
		}
		
		public void incrementAction()
		{
			_action++;
		}
		
		public boolean isArriving()
		{
			return _arriving;
		}
		
		public void setArriving(boolean value)
		{
			_arriving = value;
		}
		
		public boolean hasWaitedDepartDelay()
		{
			return _waitedDepartDelay;
		}
		
		public void setWaitedDepartDelay(boolean value)
		{
			_waitedDepartDelay = value;
		}
		
		/**
		 * Resets the route state for the movement to the given station.
		 * @param station The index of the next station.
		 */
		public void departTo(int station)
		{
			_station = station;
			_action = 0;
			_arriving = true;
			_waitedDepartDelay = false;
		}
		
		/**
		 * Counts a busy dock shout attempt, restarting the count after {@code BUSY_SHOUT_RESET} attempts.
		 * @return The current attempt count.
		 */
		public int incrementShoutCount()
		{
			_shoutCount++;
			if (_shoutCount > BUSY_SHOUT_RESET)
			{
				_shoutCount = 1;
			}
			
			return _shoutCount;
		}
		
		public void resetShoutCount()
		{
			_shoutCount = 0;
		}
		
		@Override
		public void run()
		{
			try
			{
				executeStation(this);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error executing boat " + _holder.getId() + " station " + _station + ": " + e.getMessage());
			}
		}
	}
	
	public static BoatManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BoatManager INSTANCE = new BoatManager();
	}
}
