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

import org.l2jmobius.gameserver.entity.Location;

/**
 * Immutable port of a scheduled airship route, loaded from AirShipData.xml.<br>
 * The airship docks here, ousts the passengers and waits the stopover before departing.<br>
 * Announcements are shouted through the controller npc found near the airship.
 * @author Mobius
 */
public class AirShipPortHolder
{
	private final int _dockId;
	private final int _stopover;
	private final int _announcerNpcId;
	private final int _arriveNpcStringId;
	private final int _departNpcStringId;
	private final Location _oustLocation;
	
	/**
	 * Creates a new {@code AirShipPortHolder}.
	 * @param dockId The dock id occupied while the airship is at this port.
	 * @param stopover The delay in milliseconds before departing again.
	 * @param announcerNpcId The controller npc id shouting the announcements.
	 * @param arriveNpcStringId The npc string id shouted on arrival.
	 * @param departNpcStringId The npc string id shouted on departure.
	 * @param oustLocation The {@code Location} passengers are ousted to.
	 */
	public AirShipPortHolder(int dockId, int stopover, int announcerNpcId, int arriveNpcStringId, int departNpcStringId, Location oustLocation)
	{
		_dockId = dockId;
		_stopover = stopover;
		_announcerNpcId = announcerNpcId;
		_arriveNpcStringId = arriveNpcStringId;
		_departNpcStringId = departNpcStringId;
		_oustLocation = oustLocation;
	}
	
	public int getDockId()
	{
		return _dockId;
	}
	
	public int getStopover()
	{
		return _stopover;
	}
	
	public int getAnnouncerNpcId()
	{
		return _announcerNpcId;
	}
	
	public int getArriveNpcStringId()
	{
		return _arriveNpcStringId;
	}
	
	public int getDepartNpcStringId()
	{
		return _departNpcStringId;
	}
	
	public Location getOustLocation()
	{
		return _oustLocation;
	}
}
