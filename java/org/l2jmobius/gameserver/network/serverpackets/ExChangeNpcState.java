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
 * This packet is used to synchronize the state of an {@code NPC} between the server and the client.<br>
 * It ensures that visual elements like animations or positions are updated correctly for all players.
 * @author JIV
 */
public class ExChangeNpcState extends ServerPacket
{
	private final int _objId;
	private final int _state;
	
	/**
	 * This constructor creates a new {@code ExChangeNpcState} packet.<br>
	 * It sets the unique identifier and the current state of an NPC.
	 * @param objId The unique ID of the NPC object.
	 * @param state The new state to apply to the NPC.
	 */
	public ExChangeNpcState(int objId, int state)
	{
		_objId = objId;
		_state = state;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CHANGE_NPC_STATE.writeId(this, buffer);
		buffer.writeInt(_objId);
		buffer.writeInt(_state);
	}
}
