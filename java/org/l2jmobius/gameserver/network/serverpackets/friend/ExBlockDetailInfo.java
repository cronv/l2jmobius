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

import java.util.Calendar;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet provides detailed information about a blocked player.<br>
 * It is used to inform the client about specific details regarding an excluded user.
 * @author Atronic
 */
public class ExBlockDetailInfo extends ServerPacket
{
	private final int _objectId;
	private final Player _friend;
	private final String _name;
	private final int _lastAccess;
	private final boolean _isOnline;
	private final int _friendObjectId;
	private final int _level;
	private final int _classId;
	private final int _clanId;
	private final int _clanCrestId;
	private final String _clanName;
	private final int _allyId;
	private final int _allyCrestId;
	private final String _allyName;
	private final Calendar _createDate;
	private final int _lastAccessDelay;
	private final String _friendMemo;
	
	/**
	 * Creates a new {@code ExBlockDetailInfo} packet for a specific player.<br>
	 * This constructor populates the friend details based on the provided name.<br>
	 * It handles both online and offline players to retrieve their status.
	 * @param player The {@code Player} object of the user sending the request.
	 * @param name The name of the friend whose information is being requested.
	 */
	public ExBlockDetailInfo(Player player, String name)
	{
		_objectId = player.getObjectId();
		_name = name;
		_friend = World.getPlayer(_name);
		_lastAccess = (_friend == null) || _friend.isBlocked(player) ? 0 : _friend.isOnline() ? (int) System.currentTimeMillis() : (int) (System.currentTimeMillis() - _friend.getLastAccess()) / 1000;
		
		final PlayerInfoTable charInfoTable = PlayerInfoTable.getInstance();
		if (_friend == null)
		{
			final int charId = charInfoTable.getIdByName(_name);
			_isOnline = false;
			_friendObjectId = charId;
			_level = charInfoTable.getLevelById(charId);
			_classId = charInfoTable.getClassIdById(charId);
			
			final Clan clan = ClanTable.getInstance().getClan(charInfoTable.getClanIdById(charId));
			if (clan != null)
			{
				_clanId = clan.getId();
				_clanCrestId = clan.getCrestId();
				_clanName = clan.getName();
				_allyId = clan.getAllyId();
				_allyCrestId = clan.getAllyCrestId();
				_allyName = clan.getAllyName();
			}
			else
			{
				_clanId = 0;
				_clanCrestId = 0;
				_clanName = "";
				_allyId = 0;
				_allyCrestId = 0;
				_allyName = "";
			}
			
			_createDate = charInfoTable.getCharacterCreationDate(charId);
			_lastAccessDelay = charInfoTable.getLastAccessDelay(charId);
			_friendMemo = charInfoTable.getFriendMemo(_objectId, charId);
		}
		else
		{
			_isOnline = _friend.isOnlineInt() == 1;
			final int objectId = _friend.getObjectId();
			_friendObjectId = objectId;
			_level = _friend.getLevel();
			_classId = _friend.getPlayerClass().getId();
			
			_clanId = _friend.getClanId();
			_clanCrestId = _friend.getClanCrestId();
			
			final Clan clan = _friend.getClan();
			_clanName = clan != null ? clan.getName() : "";
			_allyId = _friend.getAllyId();
			_allyCrestId = _friend.getAllyCrestId();
			_allyName = clan != null ? clan.getAllyName() : "";
			
			_createDate = _friend.getCreateDate();
			_lastAccessDelay = _lastAccess;
			_friendMemo = charInfoTable.getFriendMemo(_objectId, objectId);
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_FRIEND_DETAIL_INFO.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeString(_name);
		buffer.writeInt(_isOnline);
		buffer.writeInt(_friendObjectId);
		buffer.writeShort(_level);
		buffer.writeShort(_classId);
		buffer.writeInt(_clanId);
		buffer.writeInt(_clanCrestId);
		buffer.writeString(_clanName);
		buffer.writeInt(_allyId);
		buffer.writeInt(_allyCrestId);
		buffer.writeString(_allyName);
		buffer.writeByte(_createDate.get(Calendar.MONTH) + 1);
		buffer.writeByte(_createDate.get(Calendar.DAY_OF_MONTH));
		buffer.writeInt(_lastAccessDelay);
		buffer.writeString(_friendMemo);
	}
}
