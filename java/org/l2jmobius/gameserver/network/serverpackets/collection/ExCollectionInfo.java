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
package org.l2jmobius.gameserver.network.serverpackets.collection;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.CollectionList;
import org.l2jmobius.gameserver.entity.actor.holders.player.PlayerCollectionData;
import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends information about a player's collection to the client.<br>
 * It contains data regarding completed items and progress within the collection system.
 * @author Mobius
 */
public class ExCollectionInfo extends ServerPacket
{
	final Player _player;
	final int _category;
	final Set<Integer> _collectionIds = new HashSet<>();
	final List<Integer> _favoriteIds;
	final List<CollectionHolder> _collectionHolders = new ArrayList<>();
	
	/**
	 * Creates a new {@code ExCollectionInfo} packet for a specific player.<br>
	 * This constructor filters collections based on the provided category.<br>
	 * It also populates favorite IDs and collection holder data.
	 * @param player The {@code Player} who owns the collections.
	 * @param category The integer ID of the category to filter by.
	 */
	public ExCollectionInfo(Player player, int category)
	{
		_player = player;
		_category = category;
		final CollectionList collectionList = player.getCollectionList();
		for (PlayerCollectionData collection : collectionList.getCollections())
		{
			final int collectionId = collection.getCollectionId();
			final CollectionDataHolder collectionData = CollectionData.getInstance().getCollection(collectionId);
			if (collectionData == null)
			{
				PacketLogger.warning(ExCollectionInfo.class.getSimpleName() + ": Player " + player + " holds collection " + collectionId + ", which the data no longer defines.");
				continue;
			}
			
			if (collectionData.getCategory() == category)
			{
				_collectionIds.add(collectionId);
			}
		}
		
		_favoriteIds = collectionList.getFavorites();
		
		for (int id : _collectionIds)
		{
			final CollectionHolder holder = new CollectionHolder(id);
			final List<ItemEnchantHolder> items = CollectionData.getInstance().getCollection(id).getItems();
			for (PlayerCollectionData collection : collectionList.getCollections())
			{
				if (collection.getCollectionId() != id)
				{
					continue;
				}
				
				final int index = collection.getIndex();
				if ((index < 0) || (index >= items.size()))
				{
					PacketLogger.warning(ExCollectionInfo.class.getSimpleName() + ": Player " + player + " holds collection " + id + " slot " + index + ", but the data defines only " + items.size() + " items for it.");
					continue;
				}
				
				holder.addCollectionData(collection, items.get(index).getEnchantLevel());
			}
			
			_collectionHolders.add(holder);
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_COLLECTION_INFO.writeId(this, buffer);
		
		// Write collectionHolders data
		buffer.writeInt(_collectionHolders.size()); // size
		for (CollectionHolder holder : _collectionHolders)
		{
			final List<CollectionEntry> collectionDataList = holder.getCollectionData();
			buffer.writeInt(collectionDataList.size());
			for (CollectionEntry dataHolder : collectionDataList)
			{
				final PlayerCollectionData data = dataHolder.getCollectionData();
				buffer.writeByte(data.getIndex());
				buffer.writeInt(data.getItemId());
				buffer.writeByte(dataHolder.getEnchantLevel()); // enchant level
				buffer.writeByte(0); // bless
				buffer.writeByte(0); // bless Condition
				buffer.writeInt(1); // amount
			}
			
			buffer.writeShort(holder.getCollectionId());
		}
		
		// favoriteList
		buffer.writeInt(_favoriteIds.size());
		for (int id : _favoriteIds)
		{
			buffer.writeShort(id);
		}
		
		// rewardList
		buffer.writeInt(0);
		
		buffer.writeByte(_category);
		buffer.writeShort(0);
	}
	
	private class CollectionHolder
	{
		private final int _collectionId;
		private final List<CollectionEntry> _collectionData;
		
		public CollectionHolder(int collectionId)
		{
			_collectionId = collectionId;
			_collectionData = new ArrayList<>();
		}
		
		public int getCollectionId()
		{
			return _collectionId;
		}
		
		public List<CollectionEntry> getCollectionData()
		{
			return _collectionData;
		}
		
		public void addCollectionData(PlayerCollectionData data, int enchantLevel)
		{
			_collectionData.add(new CollectionEntry(data, enchantLevel));
		}
	}
	
	private class CollectionEntry
	{
		private final PlayerCollectionData _collectionData;
		private final int _enchantLevel;
		
		public CollectionEntry(PlayerCollectionData collectionData, int enchantLevel)
		{
			_collectionData = collectionData;
			_enchantLevel = enchantLevel;
		}
		
		public PlayerCollectionData getCollectionData()
		{
			return _collectionData;
		}
		
		public int getEnchantLevel()
		{
			return _enchantLevel;
		}
	}
}
