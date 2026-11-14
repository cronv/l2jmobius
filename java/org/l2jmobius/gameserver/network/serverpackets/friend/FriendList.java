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
package org.l2jmobius.gameserver.network.serverpackets.friend;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Supports the "Chat with Friends" dialog functionality.<br>
 * This {@code FriendList} packet is sent to the client only during the login process.
 * @author mrTJO, UnAfraid, Mobius
 */
public class FriendList extends ServerPacket
{
	private final List<FriendInfo> _info = new ArrayList<>();
	
	private static class FriendInfo
	{
		int _objId;
		String _name;
		boolean _online;
		int _classid;
		int _level;
		
		public FriendInfo(int objId, String name, boolean online, int classid, int level)
		{
			_objId = objId;
			_name = name;
			_online = online;
			_classid = classid;
			_level = level;
		}
	}
	
	/**
	 * Creates a new {@code FriendList} for the specified player.<br>
	 * This constructor populates the friend list with names, levels, and online statuses.<br>
	 * It retrieves data from both the active world and the database.
	 * @param player The {@code Player} object whose friends should be loaded.
	 */
	public FriendList(Player player)
	{
		final Set<Integer> absent = new HashSet<>();
		for (int objId : player.getFriends().getIds())
		{
			final Player friend = World.getPlayer(objId);
			if (friend == null)
			{
				absent.add(Integer.valueOf(objId));
				continue;
			}
			
			_info.add(new FriendInfo(objId, PlayerInfoTable.getInstance().getNameById(objId), friend.isOnline(), friend.getPlayerClass().getId(), friend.getLevel()));
		}
		
		if (absent.isEmpty())
		{
			return;
		}
		
		// One connection carries every friend who is not in the world, where each of them used to take one of its own.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT char_name, online, classid, level FROM characters WHERE charId = ?"))
		{
			for (int objId : absent)
			{
				statement.setInt(1, objId);
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						_info.add(new FriendInfo(objId, rset.getString(1), rset.getInt(2) == 1, rset.getInt(3), rset.getInt(4)));
					}
				}
			}
		}
		catch (Exception e)
		{
			// Who cares?
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.FRIEND_LIST.writeId(this, buffer);
		buffer.writeInt(_info.size());
		for (FriendInfo info : _info)
		{
			buffer.writeInt(info._objId); // character id
			buffer.writeString(info._name);
			buffer.writeInt(info._online); // online
			buffer.writeInt(info._online ? info._objId : 0); // object id if online
			buffer.writeInt(info._classid);
			buffer.writeInt(info._level);
		}
	}
}
