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
package org.l2jmobius.gameserver.data.holders;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;

/**
 * Immutable station of a boat route, loaded from BoatData.xml.<br>
 * The boat stops at every station, executes the announcement actions, waits the departure delay and moves on to the next station with the configured speed.<br>
 * Stations with a dock id are occupied by the approaching boat and block other boats until it departs.
 * @author Mobius
 */
public class BoatStationHolder extends Location
{
	private final int _dockId;
	private final int _moveSpeed;
	private final int _rotationSpeed;
	private final int _departDelay;
	private final int _busyMessageId;
	private final Location _kickLocation;
	private final int _ticketId;
	private final Location _ticketLocation;
	private final List<BoatActionHolder> _actions = new ArrayList<>();
	
	/**
	 * Creates a new {@code BoatStationHolder}.
	 * @param x The X coordinate of the station.
	 * @param y The Y coordinate of the station.
	 * @param z The Z coordinate of the station.
	 * @param dockId The dock id occupied while the boat is at or approaching this station, or 0 for none.
	 * @param moveSpeed The move speed used from this station to the next one.
	 * @param rotationSpeed The rotation speed used from this station to the next one.
	 * @param departDelay The delay in milliseconds before departing after the actions, or 0 for none.
	 * @param busyMessageId The system message id broadcast while the next dock is busy.
	 * @param kickLocation The {@code Location} passengers are ousted to, or {@code null} to keep the previous one.
	 * @param ticketId The ticket item id collected on departure, or 0 for none.
	 * @param ticketLocation The {@code Location} passengers without a ticket are teleported to, or {@code null}.
	 */
	public BoatStationHolder(int x, int y, int z, int dockId, int moveSpeed, int rotationSpeed, int departDelay, int busyMessageId, Location kickLocation, int ticketId, Location ticketLocation)
	{
		super(x, y, z);
		_dockId = dockId;
		_moveSpeed = moveSpeed;
		_rotationSpeed = rotationSpeed;
		_departDelay = departDelay;
		_busyMessageId = busyMessageId;
		_kickLocation = kickLocation;
		_ticketId = ticketId;
		_ticketLocation = ticketLocation;
	}
	
	public int getDockId()
	{
		return _dockId;
	}
	
	public int getMoveSpeed()
	{
		return _moveSpeed;
	}
	
	public int getRotationSpeed()
	{
		return _rotationSpeed;
	}
	
	public int getDepartDelay()
	{
		return _departDelay;
	}
	
	public int getBusyMessageId()
	{
		return _busyMessageId;
	}
	
	public Location getKickLocation()
	{
		return _kickLocation;
	}
	
	public int getTicketId()
	{
		return _ticketId;
	}
	
	public Location getTicketLocation()
	{
		return _ticketLocation;
	}
	
	public void addAction(BoatActionHolder action)
	{
		_actions.add(action);
	}
	
	public List<BoatActionHolder> getActions()
	{
		return _actions;
	}
}
