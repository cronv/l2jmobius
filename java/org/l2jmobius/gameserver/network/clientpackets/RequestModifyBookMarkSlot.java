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

/**
 * Handles a request from the client to modify a specific bookmark slot.<br>
 * This packet allows players to update their saved locations or bookmarks.
 * @author ShanSoft, Mobius
 * @structure chddSdS
 */
public class RequestModifyBookMarkSlot extends ClientPacket
{
	private int _id;
	private int _icon;
	private String _name;
	private String _tag;
	
	@Override
	protected void readImpl()
	{
		_id = readInt();
		_name = readString();
		_icon = readInt();
		_tag = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		player.getTeleportBookmarkList().modify(_id, _icon, _tag, _name);
	}
}
