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
 * This packet is used to hide the Game Master status from a specific client.<br>
 * It allows for administrative actions to be performed without being visible to other players.
 * @author Kerberos
 */
public class GMHide extends ServerPacket
{
	private final int _mode;
	
	/**
	 * Creates a new {@code GMHide} packet.<br>
	 * This packet is used to toggle the visibility of windows for Game Masters.
	 * @param mode The display mode where {@code 0} shows windows and {@code 1} hides them.
	 */
	public GMHide(int mode)
	{
		_mode = mode;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.GM_HIDE.writeId(this, buffer);
		buffer.writeInt(_mode);
	}
}
