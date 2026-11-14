/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.network.clientpackets.friend;

import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This packet is sent by the client to request a list of friends.<br>
 * It allows the server to identify which player's friend list needs to be retrieved.
 */
public class RequestFriendList extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		SystemMessage sm;
		
		// ======<Friend List>======
		player.sendPacket(SystemMessageId.FRIENDS_LIST);
		
		Player friend = null;
		for (int id : player.getFriends().getIds())
		{
			// int friendId = rset.getInt("friendId");
			final String friendName = PlayerInfoTable.getInstance().getNameById(id);
			if (friendName == null)
			{
				continue;
			}
			
			friend = World.getPlayer(friendName);
			if ((friend == null) || !friend.isOnline())
			{
				// (Currently: Offline)
				sm = new SystemMessage(SystemMessageId.S1_OFFLINE);
				sm.addString(friendName);
			}
			else
			{
				// (Currently: Online)
				sm = new SystemMessage(SystemMessageId.S1_CURRENTLY_ONLINE);
				sm.addString(friendName);
			}
			
			player.sendPacket(sm);
		}
		
		// =========================
		player.sendPacket(SystemMessageId.EMPTY_3);
	}
}
