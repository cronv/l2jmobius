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
 * This packet is sent to the client to display the adventurer's guide book.<br>
 * It handles the visual representation of the player's progression and achievements.
 * @author KenM
 */
public class ExShowAdventurerGuideBook extends ServerPacket
{
	public static final ExShowAdventurerGuideBook STATIC_PACKET = new ExShowAdventurerGuideBook();
	
	/**
	 * This is a private constructor for the {@code ExShowAdventurerGuideBook} class.<br>
	 * It is used to initialize the static packet instance.<br>
	 * This method does not take any parameters.
	 */
	private ExShowAdventurerGuideBook()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_ADVENTURER_GUIDE_BOOK.writeId(this, buffer);
	}
}
