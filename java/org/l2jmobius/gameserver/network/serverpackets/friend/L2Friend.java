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
package org.l2jmobius.gameserver.network.serverpackets.friend;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This class handles the data packets for the "Chat with Friends" dialog.<br>
 * It manages actions such as adding a new friend or deleting an existing one.
 * @author JIV
 */
public class L2Friend extends ServerPacket
{
	private final boolean _action;
	private final boolean _online;
	private final int _objid;
	private final String _name;
	
	/**
	 * Creates a new {@code L2Friend} packet.<br>
	 * This packet handles friend list actions like adding or removing friends.
	 * @param action Set to {@code true} to add a friend and {@code false} to remove one.
	 * @param objId The unique identifier of the player object.
	 */
	public L2Friend(boolean action, int objId)
	{
		_action = action;
		_objid = objId;
		_name = PlayerInfoTable.getInstance().getNameById(objId);
		_online = World.getPlayer(objId) != null;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.L2_FRIEND.writeId(this, buffer);
		buffer.writeInt(_action ? 1 : 3); // 1-add 3-remove
		buffer.writeInt(_objid);
		buffer.writeString(_name);
		buffer.writeInt(_online);
		buffer.writeInt(_online ? _objid : 0);
	}
}
