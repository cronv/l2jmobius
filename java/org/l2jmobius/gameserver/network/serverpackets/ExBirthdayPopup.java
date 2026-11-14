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
 * This packet is sent to the client to display a special birthday popup.<br>
 * It handles the visual notification for player birthdays.
 * @author Gnacik
 */
public class ExBirthdayPopup extends ServerPacket
{
	public static final ExBirthdayPopup STATIC_PACKET = new ExBirthdayPopup();
	
	/**
	 * Private constructor for the {@code ExBirthdayPopup} class.<br>
	 * This prevents other classes from creating new instances of this packet.<br>
	 * Use {@code ExBirthdayPopup.STATIC_PACKET} to access the shared instance.
	 */
	private ExBirthdayPopup()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_NOTIFY_BIRTHDAY.writeId(this, buffer);
	}
}
