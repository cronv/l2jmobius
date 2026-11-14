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
 * This packet handles a request from the client to change the color of their nickname.<br>
 * It allows for customization of the player's visual identity in the game world.
 * @author Gnacik
 */
public class ExRequestChangeNicknameColor extends ServerPacket
{
	private final int _itemId;
	
	/**
	 * This packet is sent to the server to request a nickname color change.<br>
	 * It contains the unique identifier for the item used in this action.
	 * @param itemId The {@code int} ID of the item being used.
	 */
	public ExRequestChangeNicknameColor(int itemId)
	{
		_itemId = itemId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CHANGE_NICKNAME_COLOR.writeId(this, buffer);
		buffer.writeInt(_itemId);
	}
}
