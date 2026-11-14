/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the exchange of pledge emblems between players.<br>
 * It is used to synchronize emblem data across the network.
 * @author Sdw
 */
public class ExPledgeEmblem extends ServerPacket
{
	private static final int TOTAL_SIZE = 65664;
	
	private final int _crestId;
	private final int _clanId;
	private final byte[] _data;
	private final int _chunkId;
	
	/**
	 * Creates a new {@code ExPledgeEmblem} packet.<br>
	 * This constructor initializes the emblem data for the client.
	 * @param crestId The unique identifier for the crest.
	 * @param chunkedData The raw byte array containing the emblem data.
	 * @param clanId The identifier of the clan owning the emblem.
	 * @param chunkId The specific chunk index for this piece of data.
	 */
	public ExPledgeEmblem(int crestId, byte[] chunkedData, int clanId, int chunkId)
	{
		_crestId = crestId;
		_data = chunkedData;
		_clanId = clanId;
		_chunkId = chunkId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PLEDGE_EMBLEM.writeId(this, buffer);
		buffer.writeInt(ServerConfig.SERVER_ID);
		buffer.writeInt(_clanId);
		buffer.writeInt(_crestId);
		buffer.writeInt(_chunkId);
		buffer.writeInt(TOTAL_SIZE);
		if (_data != null)
		{
			buffer.writeInt(_data.length);
			buffer.writeBytes(_data);
		}
		else
		{
			buffer.writeInt(0);
		}
	}
}
