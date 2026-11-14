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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet informs the client of the number of unread mail messages.<br>
 * It is used to update the mail notification count in the user interface.
 * @author Sdw
 */
public class ExUnReadMailCount extends ServerPacket
{
	private final int _mailUnreadCount;
	
	/**
	 * This packet sends the number of unread mails to a player.<br>
	 * It retrieves the count from the {@code MailManager}.
	 * @param player The {@code Player} who will receive the mail count.
	 */
	public ExUnReadMailCount(Player player)
	{
		_mailUnreadCount = (int) MailManager.getInstance().getUnreadCount(player);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_UNREADMAIL_COUNT.writeId(this, buffer);
		buffer.writeInt(_mailUnreadCount);
	}
}
