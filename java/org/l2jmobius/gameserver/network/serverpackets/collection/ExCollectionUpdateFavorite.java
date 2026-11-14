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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet updates the favorite status of an item in a collection.<br>
 * It informs the client to refresh the visual state of the user's favorites.
 * @author Berezkin Nikolay
 */
public class ExCollectionUpdateFavorite extends ServerPacket
{
	private final int _isAdd;
	private final int _collectionId;
	
	/**
	 * Updates the favorite status of a specific collection.<br>
	 * This packet handles adding or removing items from favorites.
	 * @param isAdd The value to determine if the item is being added or removed.
	 * @param collectionId The unique identifier for the collection.
	 */
	public ExCollectionUpdateFavorite(int isAdd, int collectionId)
	{
		_isAdd = isAdd;
		_collectionId = collectionId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_COLLECTION_UPDATE_FAVORITE.writeId(this, buffer);
		buffer.writeByte(_isAdd);
		buffer.writeShort(_collectionId);
	}
}
