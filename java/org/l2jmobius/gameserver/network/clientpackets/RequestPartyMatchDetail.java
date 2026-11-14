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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoom;
import org.l2jmobius.gameserver.managers.MatchingRoomManager;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This packet is sent by the client to request specific details about a party match.<br>
 * It allows the server to provide information regarding a {@code MatchingRoom}.
 * @author Mobius
 */
public class RequestPartyMatchDetail extends ClientPacket
{
	private int _roomId;
	private int _location;
	
	@Override
	protected void readImpl()
	{
		_roomId = readInt();
		_location = readInt();
		readInt(); // The level filter combo index, not a character level, so the player's own level is used below.
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.isInMatchingRoom())
		{
			return;
		}
		
		final MatchingRoom room = _roomId > 0 ? MatchingRoomManager.getInstance().getPartyMathchingRoom(_roomId) : MatchingRoomManager.getInstance().getPartyMathchingRoom(_location, player.getLevel());
		if (room != null)
		{
			room.addMember(player);
		}
		else
		{
			player.sendPacket(SystemMessageId.THE_PARTY_ROOM_IS_NOT_FOUND);
		}
	}
}
