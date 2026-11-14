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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoom;
import org.l2jmobius.gameserver.managers.MatchingRoomManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to provide a list of players currently waiting in the matching queue.<br>
 * It helps update the user interface with the current status of the {@code MatchingRoom}.
 * @author Sdw, Galagard
 */
public class ExListMpccWaiting extends ServerPacket
{
	private static final int NUM_PER_PAGE = 64;
	
	private final int _size;
	private final List<MatchingRoom> _rooms = new ArrayList<>();
	
	/**
	 * Creates a packet containing a list of matching rooms.<br>
	 * It filters the rooms based on the provided location and level.<br>
	 * The results are paginated to return a specific subset of data.
	 * @param page The current page number to retrieve.
	 * @param location The map location used for filtering.
	 * @param level The character level used for filtering.
	 */
	public ExListMpccWaiting(int page, int location, int level)
	{
		final List<MatchingRoom> rooms = MatchingRoomManager.getInstance().getCCMathchingRooms(location, level);
		_size = rooms.size();
		final int startIndex = (Math.max(page, 1) - 1) * NUM_PER_PAGE;
		int chunkSize = _size - startIndex;
		if (chunkSize > NUM_PER_PAGE)
		{
			chunkSize = NUM_PER_PAGE;
		}
		
		for (int i = startIndex; i < (startIndex + chunkSize); i++)
		{
			_rooms.add(rooms.get(i));
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_LIST_MPCC_WAITING.writeId(this, buffer);
		buffer.writeInt(_size);
		buffer.writeInt(_rooms.size());
		for (MatchingRoom room : _rooms)
		{
			buffer.writeInt(room.getId());
			buffer.writeString(room.getTitle());
			buffer.writeInt(room.getMembersCount());
			buffer.writeInt(room.getMinLevel());
			buffer.writeInt(room.getMaxLevel());
			buffer.writeInt(room.getLocation());
			buffer.writeInt(room.getMaxMembers());
			buffer.writeString(room.getLeader().getName());
		}
	}
}
