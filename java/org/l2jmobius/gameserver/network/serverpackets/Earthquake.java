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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to trigger a screen shake effect.<br>
 * It informs the game client that an earthquake event has occurred.
 */
public class Earthquake extends ServerPacket
{
	private final int _x;
	private final int _y;
	private final int _z;
	private final int _intensity;
	private final int _duration;
	
	/**
	 * Creates a new {@code Earthquake} packet.<br>
	 * This constructor initializes the earthquake with a specific location and magnitude.
	 * @param location The {@code ILocational} object defining where the earthquake starts.
	 * @param intensity The strength of the earthquake as an {@code int}.
	 * @param duration The length of time the earthquake lasts in seconds.
	 */
	public Earthquake(ILocational location, int intensity, int duration)
	{
		_x = location.getX();
		_y = location.getY();
		_z = location.getZ();
		_intensity = intensity;
		_duration = duration;
	}
	
	/**
	 * Creates a new {@code Earthquake} packet.<br>
	 * This constructor sets the coordinates and magnitude of the earthquake.
	 * @param x The X coordinate of the earthquake.
	 * @param y The Y coordinate of the earthquake.
	 * @param z The Z coordinate of the earthquake.
	 * @param intensity The strength of the earthquake.
	 * @param duration The length of time the earthquake lasts.
	 */
	public Earthquake(int x, int y, int z, int intensity, int duration)
	{
		_x = x;
		_y = y;
		_z = z;
		_intensity = intensity;
		_duration = duration;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EARTHQUAKE.writeId(this, buffer);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
		buffer.writeInt(_intensity);
		buffer.writeInt(_duration);
		buffer.writeInt(0); // Unknown
	}
}
