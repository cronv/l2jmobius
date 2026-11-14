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
package org.l2jmobius.gameserver.network.serverpackets.ensoul;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is used to display the Ensoul window to a client.<br>
 * It handles the visual representation of the ensoul interface for the player.
 * @author UnAfraid
 */
public class ExShowEnsoulWindow extends ServerPacket
{
	public static final ExShowEnsoulWindow STATIC_PACKET = new ExShowEnsoulWindow();
	
	/**
	 * Private constructor for the {@code ExShowEnsoulWindow} class.<br>
	 * This prevents other classes from creating new instances of this packet.<br>
	 * It is used to initialize the {@code STATIC_PACKET} constant.
	 */
	private ExShowEnsoulWindow()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_ENSOUL_WINDOW.writeId(this, buffer);
	}
}
