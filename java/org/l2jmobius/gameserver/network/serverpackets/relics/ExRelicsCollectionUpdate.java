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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet updates the client with the current state of a player's relic collection.<br>
 * It synchronizes the data held in {@code PlayerRelicCollectionData} to ensure the UI reflects the correct information.
 * @author CostyKiller
 */
public class ExRelicsCollectionUpdate extends ServerPacket
{
	private final int _relicId;
	private final int _relicLevel;
	private final int _completedCollectionId;
	
	/**
	 * Updates the relic collection for a specific player.<br>
	 * This packet sends information about a relic's level to the client.
	 * @param player The {@code Player} receiving the update.
	 * @param relicId The unique identifier of the relic.
	 * @param relicLevel The current level of the relic.
	 */
	public ExRelicsCollectionUpdate(Player player, int relicId, int relicLevel)
	{
		_relicId = relicId;
		_relicLevel = relicLevel;
		
		// Registering the collection touches the relic list, the database and the player, so it happens here rather than while the packet is written on the network thread.
		_completedCollectionId = player.getRelicList().updateCollection(relicId);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RELICS_COLLECTION_UPDATE.writeId(this, buffer);
		if (_completedCollectionId == 0)
		{
			return;
		}
		
		buffer.writeInt(1); // Collection array size.
		buffer.writeInt(_completedCollectionId); // Collection id.
		buffer.writeByte(true); // Collection is complete?
		buffer.writeInt(1); // Registered relics in collection size.
		buffer.writeInt(1); // Array position.
		buffer.writeInt(_relicId); // Relic id.
		buffer.writeInt(_relicLevel); // Relic level.
	}
}
