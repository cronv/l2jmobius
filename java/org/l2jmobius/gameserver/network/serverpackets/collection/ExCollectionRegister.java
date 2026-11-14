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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet handles the registration of an {@code ExCollection} for a player.<br>
 * It informs the client about new collection data being added to their account.
 * @author Index
 */
public class ExCollectionRegister extends ServerPacket
{
	private final int _success;
	private final int _collectionId;
	private final int _index;
	private final ItemEnchantHolder _collectionInfo;
	
	/**
	 * Creates a new {@code ExCollectionRegister} packet.<br>
	 * This constructor initializes the success status and collection details.
	 * @param success The result of the registration attempt.
	 * @param collectionId The unique identifier for the collection.
	 * @param index The position or index within the collection.
	 * @param collectionInfo The data holder containing item enchantment information.
	 */
	public ExCollectionRegister(boolean success, int collectionId, int index, ItemEnchantHolder collectionInfo)
	{
		_success = success ? 1 : 0;
		_collectionId = collectionId;
		_index = index;
		_collectionInfo = collectionInfo;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_COLLECTION_REGISTER.writeId(this, buffer);
		buffer.writeShort(_collectionId);
		buffer.writeByte(_success); // success
		buffer.writeByte(0); // recursive reward
		buffer.writeShort(249); // 256 - size so far
		buffer.writeByte(_index); // slot index
		buffer.writeInt(_collectionInfo.getId()); // item classId
		buffer.writeShort(_collectionInfo.getEnchantLevel()); // enchant level
		buffer.writeByte(0); // is blessed
		buffer.writeByte(0); // blessed conditions
		buffer.writeInt((int) _collectionInfo.getCount()); // amount
	}
}
