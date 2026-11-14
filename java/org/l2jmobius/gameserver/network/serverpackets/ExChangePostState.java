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
 * This packet is sent to the client to update the state of an exchange post.<br>
 * It informs the player about changes made to a specific trade listing.
 * @author Migi
 */
public class ExChangePostState extends ServerPacket
{
	private final boolean _receivedBoard;
	private final int[] _changedMsgIds;
	private final int _changeId;
	
	/**
	 * Creates a new {@code ExChangePostState} packet.<br>
	 * This constructor initializes the state of an exchange post update.
	 * @param receivedBoard The status of whether the board was received.
	 * @param changedMsgIds An array of IDs for the messages that were changed.
	 * @param changeId The unique identifier for this specific change.
	 */
	public ExChangePostState(boolean receivedBoard, int[] changedMsgIds, int changeId)
	{
		_receivedBoard = receivedBoard;
		_changedMsgIds = changedMsgIds;
		_changeId = changeId;
	}
	
	/**
	 * Creates a new {@code ExChangePostState} packet.<br>
	 * This constructor handles cases where only one message ID is changed.<br>
	 * It initializes the internal state for the exchange process.
	 * @param receivedBoard The status of the board reception.
	 * @param changedMsgId The unique identifier for the modified message.
	 * @param changeId The unique identifier for the specific change.
	 */
	public ExChangePostState(boolean receivedBoard, int changedMsgId, int changeId)
	{
		_receivedBoard = receivedBoard;
		_changedMsgIds = new int[]
		{
			changedMsgId
		};
		_changeId = changeId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CHANGE_POST_STATE.writeId(this, buffer);
		buffer.writeInt(_receivedBoard);
		buffer.writeInt(_changedMsgIds.length);
		for (int postId : _changedMsgIds)
		{
			buffer.writeInt(postId); // postId
			buffer.writeInt(_changeId); // state
		}
	}
}
