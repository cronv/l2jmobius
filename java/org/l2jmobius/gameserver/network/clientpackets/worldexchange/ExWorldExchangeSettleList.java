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
package org.l2jmobius.gameserver.network.clientpackets.worldexchange;

import java.util.Collections;

import org.l2jmobius.gameserver.config.WorldExchangeConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExAdenaInvenCount;
import org.l2jmobius.gameserver.network.serverpackets.limitshop.ExBloodyCoinCount;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeInfo;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeItemList;
import org.l2jmobius.gameserver.network.serverpackets.worldexchange.WorldExchangeSettleList;

/**
 * This packet handles the settlement of items in the World Exchange.<br>
 * It contains a list of {@code WorldExchangeItemList} objects to be processed by the server.
 * @author Index
 */
public class ExWorldExchangeSettleList extends ClientPacket
{
	private int _weType;
	
	@Override
	protected void readImpl()
	{
		_weType = readByte(); // 557
	}
	
	@Override
	protected void runImpl()
	{
		if (!WorldExchangeConfig.ENABLE_WORLD_EXCHANGE)
		{
			return;
		}
		
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		player.sendPacket(new ExAdenaInvenCount(player));
		player.sendPacket(new ExBloodyCoinCount(player));
		player.sendPacket(new WorldExchangeInfo(_weType));
		player.sendPacket(new WorldExchangeItemList(Collections.emptyList(), null, 0, _weType));
		player.sendPacket(new WorldExchangeSettleList(player, _weType));
	}
}
