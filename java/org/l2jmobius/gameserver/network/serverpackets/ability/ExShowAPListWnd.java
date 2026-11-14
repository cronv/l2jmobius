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
package org.l2jmobius.gameserver.network.serverpackets.ability;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is used to display the Active Power list window to the client.<br>
 * It handles the synchronization of active power data for the user interface.
 * @author Sdw
 */
public class ExShowAPListWnd extends ServerPacket
{
	public static final ExShowAPListWnd STATIC_PACKET = new ExShowAPListWnd();
	
	/**
	 * Private constructor for the {@code ExShowAPListWnd} class.<br>
	 * This prevents other classes from creating new instances of this packet.<br>
	 * It is used to initialize the static instance.
	 */
	private ExShowAPListWnd()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_AP_LIST_WND.writeId(this, buffer);
	}
}
