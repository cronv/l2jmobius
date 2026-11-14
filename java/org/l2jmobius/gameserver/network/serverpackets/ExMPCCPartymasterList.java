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

import java.util.Set;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet handles the transmission of the party master list to the client.<br>
 * It is used to synchronize and update the visible parties in the game world.
 * @author Sdw
 */
public class ExMPCCPartymasterList extends ServerPacket
{
	private final Set<String> _leadersName;
	
	/**
	 * Creates a new instance of {@code ExMPCCPartymasterList}.<br>
	 * This constructor initializes the list with specific leader names.
	 * @param leadersName A {@code Set} of strings containing the names of the party leaders.
	 */
	public ExMPCCPartymasterList(Set<String> leadersName)
	{
		_leadersName = leadersName;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_MPCC_PARTYMASTER_LIST.writeId(this, buffer);
		buffer.writeInt(_leadersName.size());
		_leadersName.forEach(buffer::writeString);
	}
}
