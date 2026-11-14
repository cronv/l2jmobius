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
 * This packet sends a dialog containing an input field to the client.<br>
 * It is used for changing either a character name or a clan name.<br>
 * The type {@code 0} represents a character name change, while type {@code 1} represents a clan name change.
 * @author JIV
 */
public class ExNeedToChangeName extends ServerPacket
{
	private final int _type;
	private final int _subType;
	private final String _name;
	
	/**
	 * Creates a new {@code ExNeedToChangeName} packet.<br>
	 * This packet is used to display a dialog for changing names.
	 * @param type The category of the name being changed.
	 * @param subType The specific subtype of the name.
	 * @param name The current name value associated with this request.
	 */
	public ExNeedToChangeName(int type, int subType, String name)
	{
		_type = type;
		_subType = subType;
		_name = name;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_NEED_TO_CHANGE_NAME.writeId(this, buffer);
		buffer.writeInt(_type);
		buffer.writeInt(_subType);
		buffer.writeString(_name);
	}
}
