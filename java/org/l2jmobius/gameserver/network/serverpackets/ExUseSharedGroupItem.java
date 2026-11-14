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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client when a shared group item is used.<br>
 * It handles the synchronization of shared items between players in a group.
 * @author KenM
 */
public class ExUseSharedGroupItem extends ServerPacket
{
	private final int _itemId;
	private final int _grpId;
	private final int _remainingTime;
	private final int _totalTime;
	
	/**
	 * Creates a new {@code ExUseSharedGroupItem} packet.<br>
	 * This packet is used to notify the client about shared group item usage.<br>
	 * It converts time values from milliseconds to seconds.
	 * @param itemId The unique identifier for the item.
	 * @param grpId The unique identifier for the group.
	 * @param remainingTime The amount of time left in milliseconds.
	 * @param totalTime The total duration of the effect in milliseconds.
	 */
	public ExUseSharedGroupItem(int itemId, int grpId, long remainingTime, int totalTime)
	{
		_itemId = itemId;
		_grpId = grpId;
		_remainingTime = (int) (remainingTime / 1000);
		_totalTime = totalTime / 1000;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_USE_SHARED_GROUP_ITEM.writeId(this, buffer);
		buffer.writeInt(_itemId);
		buffer.writeInt(_grpId);
		buffer.writeInt(_remainingTime);
		buffer.writeInt(_totalTime);
	}
}
