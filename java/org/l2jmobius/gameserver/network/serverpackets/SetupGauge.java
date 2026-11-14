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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the initial configuration of the gauge system for the client.<br>
 * It ensures that the client is properly synchronized with the server's gauge settings.
 */
public class SetupGauge extends ServerPacket
{
	public static final int BLUE = 0;
	public static final int RED = 1;
	public static final int CYAN = 2;
	
	private final int _dat1;
	private final int _time;
	private final int _time2;
	private final int _objectId;
	
	/**
	 * Initializes a new {@code SetupGauge} packet.<br>
	 * This method sets the initial values for the gauge object.
	 * @param objectId The unique identifier of the object.
	 * @param dat1 The color value for the gauge.
	 * @param time The current time value for the gauge.
	 */
	public SetupGauge(int objectId, int dat1, int time)
	{
		_objectId = objectId;
		_dat1 = dat1; // color 0-blue 1-red 2-cyan 3-green
		_time = time;
		_time2 = time;
	}
	
	/**
	 * This constructor initializes a new {@code SetupGauge} packet.<br>
	 * It sets the visual properties and timing for a gauge object.
	 * @param objectId The unique identifier of the object.
	 * @param color The color value for the gauge.
	 * @param currentTime The current progress time.
	 * @param maxTime The maximum duration of the gauge.
	 */
	public SetupGauge(int objectId, int color, int currentTime, int maxTime)
	{
		_objectId = objectId;
		_dat1 = color; // color 0-blue 1-red 2-cyan 3-green
		_time = currentTime;
		_time2 = maxTime;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.SETUP_GAUGE.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_dat1);
		buffer.writeInt(_time);
		buffer.writeInt(_time2);
	}
}
