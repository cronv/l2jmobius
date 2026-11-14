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
 * This packet handles the transmission of information regarding {@code ExNevitAdventPoint} data.<br>
 * It is used to synchronize specific point details between the server and the client.
 * @author mochitto
 */
public class ExNevitAdventPointInfoPacket extends ServerPacket
{
	private final int _points;
	
	/**
	 * Creates a new {@code ExNevitAdventPointInfoPacket} instance.<br>
	 * This packet stores the number of advent points for a player.
	 * @param points The amount of points to set in the packet.
	 */
	public ExNevitAdventPointInfoPacket(int points)
	{
		_points = points;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_BR_AGATHION_ENERGY_INFO.writeId(this, buffer);
		buffer.writeInt(_points); // 72 = 1%, max 7200 = 100%
	}
}
