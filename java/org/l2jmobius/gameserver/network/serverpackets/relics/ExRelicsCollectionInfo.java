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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.RelicCollectionData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.PlayerRelicCollectionData;
import org.l2jmobius.gameserver.entity.actor.holders.player.RelicList;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends information about a player's relic collection to the client.<br>
 * It contains data retrieved from {@code PlayerRelicCollectionData}.
 * @author CostyKiller
 */
public class ExRelicsCollectionInfo extends ServerPacket
{
	private final Player _player;
	private final List<Integer> _relicCollectionIds = new ArrayList<>();
	
	/**
	 * Creates a new {@code ExRelicsCollectionInfo} packet for a specific player.<br>
	 * This constructor populates the list of unique relic collection IDs owned by the player.
	 * @param player The {@code Player} object used to retrieve relic data.
	 */
	public ExRelicsCollectionInfo(Player player)
	{
		_player = player;
		_relicCollectionIds.clear();
		for (PlayerRelicCollectionData relicCollection : player.getRelicList().getCollections())
		{
			final int relicId = relicCollection.getRelicCollectionId();
			if (!_relicCollectionIds.contains(relicId))
			{
				_relicCollectionIds.add(relicId);
			}
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RELICS_COLLECTION_INFO.writeId(this, buffer);
		
		buffer.writeInt(1); // Page index.
		buffer.writeInt(82); // Max page.
		final int relicCollectionIdsSize = _relicCollectionIds.size();
		buffer.writeInt(relicCollectionIdsSize);
		
		for (int i = 1; i <= relicCollectionIdsSize; i++)
		{
			final int collectionId = _relicCollectionIds.get(i - 1);
			buffer.writeInt(collectionId); // Collection array position.
			
			int completeCount = 0;
			final RelicList relicList = _player.getRelicList();
			for (PlayerRelicCollectionData coll : relicList.getCollections())
			{
				if (coll.getRelicCollectionId() == collectionId)
				{
					completeCount++;
				}
			}
			
			final int completedRelicsCount = RelicCollectionData.getInstance().getRelicCollection(collectionId).getCompleteCount();
			buffer.writeByte(completeCount == completedRelicsCount);
			buffer.writeInt(completeCount);
			
			for (PlayerRelicCollectionData collection : relicList.getCollections())
			{
				if (collection.getRelicCollectionId() == collectionId)
				{
					buffer.writeInt(collection.getRelicId()); // Relic id.
					buffer.writeInt(collection.getRelicLevel()); // Relic level.
				}
			}
		}
	}
}
