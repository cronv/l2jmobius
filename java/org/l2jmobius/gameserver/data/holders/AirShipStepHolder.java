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
 * Immutable step of a scheduled airship route, loaded from AirShipData.xml.<br>
 * A step is a plain path point, a teleport point or a port where the airship docks.
 * @author Mobius
 */
public class AirShipStepHolder extends Location
{
	private final int _teleportHeading;
	private final AirShipPortHolder _port;
	
	/**
	 * Creates a new {@code AirShipStepHolder}.
	 * @param x The X coordinate of the step.
	 * @param y The Y coordinate of the step.
	 * @param z The Z coordinate of the step.
	 * @param teleportHeading The heading applied after teleporting to this step, or -1 when the step is not a teleport.
	 * @param port The {@code AirShipPortHolder} of this step, or {@code null} when the step is not a port.
	 */
	public AirShipStepHolder(int x, int y, int z, int teleportHeading, AirShipPortHolder port)
	{
		super(x, y, z);
		_teleportHeading = teleportHeading;
		_port = port;
	}
	
	public boolean isTeleport()
	{
		return _teleportHeading > -1;
	}
	
	public int getTeleportHeading()
	{
		return _teleportHeading;
	}
	
	public AirShipPortHolder getPort()
	{
		return _port;
	}
}
