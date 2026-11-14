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
package org.l2jmobius.gameserver.network.serverpackets.dethrone;

import java.util.Calendar;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet provides information about the current dethrone season to the client.<br>
 * It contains data such as the start time and duration of the active season.
 * @author Lefteris
 */
public class ExDethroneSeasonInfo extends ServerPacket
{
	private final int _seasonYear;
	private final int _seasonMonth;
	private final boolean _open;
	
	/**
	 * Creates a new {@code ExDethroneSeasonInfo} packet.<br>
	 * It automatically sets the current year and month.<br>
	 * The status of the season is determined by the provided boolean.
	 * @param open The status indicating if the season is currently open.
	 */
	public ExDethroneSeasonInfo(boolean open)
	{
		_seasonYear = Calendar.getInstance().get(Calendar.YEAR);
		_seasonMonth = Calendar.getInstance().get(Calendar.MONTH) + 1;
		_open = open;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DETHRONE_SEASON_INFO.writeId(this, buffer);
		buffer.writeInt(_seasonYear);
		buffer.writeInt(_seasonMonth);
		buffer.writeByte(_open);
	}
}
