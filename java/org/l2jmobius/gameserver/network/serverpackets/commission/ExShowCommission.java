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
package org.l2jmobius.gameserver.network.serverpackets.commission;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to display commission information.<br>
 * It handles the visual representation of commissions for the player.
 * @author NosBit
 */
public class ExShowCommission extends ServerPacket
{
	public static final ExShowCommission STATIC_PACKET = new ExShowCommission();
	
	/**
	 * Private constructor for the {@code ExShowCommission} class.<br>
	 * This prevents other classes from creating new instances of this packet.<br>
	 * Use {@code ExShowCommission.STATIC_PACKET} to access the shared instance.
	 */
	private ExShowCommission()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_COMMISSION.writeId(this, buffer);
		buffer.writeInt(1);
	}
}
