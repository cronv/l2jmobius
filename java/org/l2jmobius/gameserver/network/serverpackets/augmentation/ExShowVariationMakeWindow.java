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
package org.l2jmobius.gameserver.network.serverpackets.augmentation;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This class handles the {@code ExShowVariationMakeWindow} trigger packet.<br>
 * It is used to notify the client to display a specific variation window.
 * @author KenM
 */
public class ExShowVariationMakeWindow extends ServerPacket
{
	public static final ExShowVariationMakeWindow STATIC_PACKET = new ExShowVariationMakeWindow();
	
	/**
	 * Creates a new instance of the {@code ExShowVariationMakeWindow} packet.<br>
	 * This is used to send window information to the game client.
	 */
	public ExShowVariationMakeWindow()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_VARIATION_MAKE_WINDOW.writeId(this, buffer);
	}
}
