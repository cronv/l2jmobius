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
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * Server packet sent in response to a server-list request, providing the group IDs the current server belongs to.<br>
 * Each group is written with a single-entry world ID list containing {@code ServerConfig.SERVER_ID}, indicating that the current server is a member of that group.<br>
 * Filtering of which groups apply to this server is expected to be done by the caller before construction; this class does not filter {@code groupIds} itself.
 * @author Galagard
 */
public class ExBrServerIdList extends ServerPacket
{
	// @formatter:off
	private static final int[] MAIN_GROUPS = { 1, 100 };
	private static final int[] CLASSIC_GROUPS = { 1, 10 };
	private static final int[] ESSENCE_GROUPS = { 1, 4, 10, 113, 115, 200 };
	// @formatter:on
	
	private final int[] _groupIds;
	
	/**
	 * Constructs a new {@code ExBrServerIdList} packet.
	 */
	public ExBrServerIdList()
	{
		if (((ServerConfig.SERVER_LIST_TYPE & 0x2000) == 0x2000) || ((ServerConfig.SERVER_LIST_TYPE & 0x1000) == 0x1000))
		{
			_groupIds = ESSENCE_GROUPS;
		}
		else if ((ServerConfig.SERVER_LIST_TYPE & 0x400) == 0x400)
		{
			_groupIds = CLASSIC_GROUPS;
		}
		else
		{
			_groupIds = MAIN_GROUPS;
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BR_SERVER_ID_LIST.writeId(this, buffer);
		
		// vServerGroupList (array<_ServerGroupItem>)
		// Write the dynamic count of groups matching this specific server
		buffer.writeInt(_groupIds.length);
		
		for (int group : _groupIds)
		{
			// nGroupID (int)
			buffer.writeInt(group);
			
			// vWorldIDList (array<int>)
			buffer.writeInt(1);
			buffer.writeInt(ServerConfig.SERVER_ID);
		}
	}
}