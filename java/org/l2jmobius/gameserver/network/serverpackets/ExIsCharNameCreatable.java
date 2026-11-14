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
 * This packet is used to check if a specific character name is available for creation.<br>
 * It allows the client to verify naming availability before a player attempts to register.<br>
 * The server responds with {@code true} or {@code false} based on whether the name is taken.
 * @author UnAfraid
 */
public class ExIsCharNameCreatable extends ServerPacket
{
	private final int _allowed;
	
	/**
	 * Creates a new {@code ExIsCharNameCreatable} packet.<br>
	 * This packet checks if a character name is valid for creation.<br>
	 * It stores the permission level required for this action.
	 * @param allowed The integer value representing the allowed status.
	 */
	public ExIsCharNameCreatable(int allowed)
	{
		_allowed = allowed;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CHECK_CHAR_NAME.writeId(this, buffer);
		buffer.writeInt(_allowed);
	}
}
