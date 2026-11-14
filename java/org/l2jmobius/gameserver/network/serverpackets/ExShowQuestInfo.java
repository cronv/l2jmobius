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
 * This packet is used to send quest information to the client.<br>
 * It allows the game client to display specific details about a quest.
 * @author Luca Baldi
 */
public class ExShowQuestInfo extends ServerPacket
{
	public static final ExShowQuestInfo STATIC_PACKET = new ExShowQuestInfo();
	
	/**
	 * Private constructor for the {@code ExShowQuestInfo} class.<br>
	 * This prevents other classes from creating new instances of this packet.<br>
	 * The {@code STATIC_PACKET} instance is used instead.
	 */
	private ExShowQuestInfo()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_QUEST_INFO.writeId(this, buffer);
	}
}
