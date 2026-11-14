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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet provides information regarding the {@code ExDethrone} event to the client.<br>
 * It is used to synchronize server-side state with the game client during this specific activity.
 * @author CostyKiller
 */
public class ExDethroneServerInfo extends ServerPacket
{
	private final long _serverPoints;
	private final long _serverSoulOrbs;
	private final boolean _adenCastleOwner;
	private final boolean _connected;
	
	/**
	 * Creates a new {@code ExDethroneServerInfo} packet.<br>
	 * This constructor initializes the server status data for the client.
	 * @param serverPoints The current number of points on the server.
	 * @param serverSoulOrbs The current amount of soul orbs available.
	 * @param adenCastleOwner Indicates if the castle is currently owned by Aden.
	 * @param connected Indicates whether the connection is active.
	 */
	public ExDethroneServerInfo(long serverPoints, long serverSoulOrbs, boolean adenCastleOwner, boolean connected)
	{
		_serverPoints = serverPoints;
		_serverSoulOrbs = serverSoulOrbs;
		_adenCastleOwner = adenCastleOwner;
		_connected = connected;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DETHRONE_SERVER_INFO.writeId(this, buffer);
		
		// Receiving Server Points
		buffer.writeInt(1); // array size (only one server atm)
		
		buffer.writeInt(1); // server rank
		buffer.writeInt(ServerConfig.SERVER_ID); // server id (off 15)
		buffer.writeLong(_serverPoints); // server points
		
		// buffer.writeInt(2); // server rank 2
		// buffer.writeInt(2); // server id 2 (off 16)
		// buffer.writeLong(123456789); // server points 2
		
		// Soul Orb Status
		buffer.writeInt(1); // array size (only one server atm)
		
		// buffer.writeInt(1); // server rank 1
		// buffer.writeInt(1); // server id 1
		// buffer.writeLong(1234); // server score 1
		//
		// buffer.writeInt(2); // server rank 2
		// buffer.writeInt(2); // server id 2
		// buffer.writeLong(12345); // server score 2
		//
		// buffer.writeInt(3); // server rank 3
		// buffer.writeInt(3); // server id 3
		// buffer.writeLong(123456); // server score 3
		
		buffer.writeInt(1); // server rank 1
		buffer.writeInt(ServerConfig.SERVER_ID); // server id 4
		buffer.writeLong(_serverSoulOrbs); // server score 4
		
		// The connection list array size must match the number of servers in both arrays, where the count comes from the Soul Orb status array of connectable servers (0 means no connections are available and the Connect to Conquest button is disabled).
		buffer.writeInt(1); // array size
		// buffer.writeInt(_bConnected ? 1 : 0); // connection status on/off for server id 1
		// buffer.writeInt(_bConnected ? 2 : 0); // connection status on/off for server id 2
		// buffer.writeInt(_bConnected ? 3 : 0); // connection status on/off for server id 3
		buffer.writeInt(_connected ? ServerConfig.SERVER_ID : 0); // connection status on/off for server id 4
		
		// Connection button status
		buffer.writeInt(_adenCastleOwner ? 1 : 0); // 0 - connecting to conquest button is grey (disabled) / 1 - connecting to conquest button is green (enabled)
		
		buffer.writeInt(ServerConfig.SERVER_ID); // nDethroneWorldID
	}
}
