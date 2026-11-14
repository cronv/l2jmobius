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
 * This packet handles the loading of the top rankers for the Halloween event.<br>
 * It sends the ranking data from the server to the client.
 */
public class ExBrLoadEventTopRankers extends ServerPacket
{
	private final int _eventId;
	private final int _day;
	private final int _count;
	private final int _bestScore;
	private final int _myScore;
	
	/**
	 * Constructs a new {@code ExBrLoadEventTopRankers} packet.<br>
	 * This packet handles the ranking data for a specific event day.
	 * @param eventId The unique identifier for the event.
	 * @param day The current day of the event.
	 * @param count The number of top rankers to display.
	 * @param bestScore The highest score achieved in the event.
	 * @param myScore The score belonging to the current player.
	 */
	public ExBrLoadEventTopRankers(int eventId, int day, int count, int bestScore, int myScore)
	{
		_eventId = eventId;
		_day = day;
		_count = count;
		_bestScore = bestScore;
		_myScore = myScore;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BR_LOAD_EVENT_TOP_RANKERS_ACK.writeId(this, buffer);
		buffer.writeInt(_eventId);
		buffer.writeInt(_day);
		buffer.writeInt(_count);
		buffer.writeInt(_bestScore);
		buffer.writeInt(_myScore);
	}
}
