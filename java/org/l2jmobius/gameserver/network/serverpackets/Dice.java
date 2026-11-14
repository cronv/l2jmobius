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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the transmission of dice roll results to the client.<br>
 * It is used to synchronize random outcomes during gameplay events.
 */
public class Dice extends ServerPacket
{
	private final int _objectId;
	private final int _itemId;
	private final int _number;
	private final int _x;
	private final int _y;
	private final int _z;
	
	/**
	 * Creates a new {@code Dice} packet.<br>
	 * This constructor initializes the data for a dice roll event.
	 * @param charObjId The unique identifier of the character object.
	 * @param itemId The item ID associated with the dice.
	 * @param number The result of the dice roll.
	 * @param x The X coordinate of the dice position.
	 * @param y The Y coordinate of the dice position.
	 * @param z The Z coordinate of the dice position.
	 */
	public Dice(int charObjId, int itemId, int number, int x, int y, int z)
	{
		_objectId = charObjId;
		_itemId = itemId;
		_number = number;
		_x = x;
		_y = y;
		_z = z;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.DICE.writeId(this, buffer);
		buffer.writeInt(_objectId); // object id of player
		buffer.writeInt(_itemId); // item id of dice (spade) 4625,4626,4627,4628
		buffer.writeInt(_number); // number rolled
		buffer.writeInt(_x); // x
		buffer.writeInt(_y); // y
		buffer.writeInt(_z); // z
	}
}
