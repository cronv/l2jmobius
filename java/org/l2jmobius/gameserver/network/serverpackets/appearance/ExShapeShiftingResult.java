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
package org.l2jmobius.gameserver.network.serverpackets.appearance;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to communicate the result of a shape-shifting action.<br>
 * It informs the player whether their transformation was successful or failed.
 * @author UnAfraid
 */
public class ExShapeShiftingResult extends ServerPacket
{
	public static final int RESULT_FAILED = 0;
	public static final int RESULT_SUCCESS = 1;
	public static final int RESULT_CLOSE = 2;
	
	public static final ExShapeShiftingResult FAILED = new ExShapeShiftingResult(RESULT_FAILED, 0, 0);
	public static final ExShapeShiftingResult CLOSE = new ExShapeShiftingResult(RESULT_CLOSE, 0, 0);
	
	private final int _result;
	private final int _targetItemId;
	private final int _extractItemId;
	
	/**
	 * Creates a new {@code ExShapeShiftingResult} packet.<br>
	 * This constructor initializes the result status and item IDs.
	 * @param result The outcome of the shape shifting action.
	 * @param targetItemId The ID of the target item involved in the process.
	 * @param extractItemId The ID of the extracted item from the process.
	 */
	public ExShapeShiftingResult(int result, int targetItemId, int extractItemId)
	{
		_result = result;
		_targetItemId = targetItemId;
		_extractItemId = extractItemId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHAPE_SHIFTING_RESULT.writeId(this, buffer);
		buffer.writeInt(_result);
		buffer.writeInt(_targetItemId);
		buffer.writeInt(_extractItemId);
	}
}
