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

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.holders.MailMessage;

/**
 * This packet is sent to the client to display a list of outgoing mail messages.<br>
 * It contains the data required to populate the "Sent" tab in the mail interface.
 * @author Migi, DS
 */
public class ExShowSentPostList extends ServerPacket
{
	private final List<MailMessage> _outbox;
	
	/**
	 * Creates a new {@code ExShowSentPostList} packet.<br>
	 * It retrieves the list of sent mail messages for a specific player.
	 * @param objectId The unique identifier of the player.
	 */
	public ExShowSentPostList(int objectId)
	{
		_outbox = MailManager.getInstance().getOutbox(objectId);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_SENT_POST_LIST.writeId(this, buffer);
		buffer.writeInt((int) (System.currentTimeMillis() / 1000));
		if ((_outbox != null) && !_outbox.isEmpty())
		{
			buffer.writeInt(_outbox.size());
			for (MailMessage msg : _outbox)
			{
				buffer.writeInt(msg.getId());
				buffer.writeString(msg.getSubject());
				buffer.writeString(msg.getReceiverName());
				buffer.writeInt(msg.isLocked());
				buffer.writeInt(msg.getExpirationSeconds());
				buffer.writeInt(msg.isUnread());
				buffer.writeInt(1);
				buffer.writeInt(msg.hasAttachments());
				buffer.writeInt(0);
			}
		}
		else
		{
			buffer.writeInt(0);
		}
	}
}
