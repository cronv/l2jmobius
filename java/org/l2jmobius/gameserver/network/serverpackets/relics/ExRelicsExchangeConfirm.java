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
package org.l2jmobius.gameserver.network.serverpackets.relics;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet confirms the successful exchange of relics between players.<br>
 * It is sent to the client to notify them that the transaction has been completed.
 * @author CostyKiller
 */
public class ExRelicsExchangeConfirm extends ServerPacket
{
	private final int _index;
	private final int _relicId;
	
	/**
	 * Confirms the exchange of a specific relic for a player.<br>
	 * This packet handles the final step of the relic exchange process.
	 * @param player The {@code Player} who is performing the exchange.
	 * @param index The position or index of the relic in the inventory.
	 * @param relicId The unique identifier for the relic being exchanged.
	 */
	public ExRelicsExchangeConfirm(Player player, int index, int relicId)
	{
		_index = index;
		_relicId = relicId;
		
		// Confirming touches the relic list, the database and the account, so it happens here rather than while the packet is written on the network thread.
		player.getRelicList().confirmExchange(relicId);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RELICS_EXCHANGE_CONFIRM.writeId(this, buffer);
		
		buffer.writeInt(_index);
		buffer.writeByte(1);
		buffer.writeInt(_relicId);
	}
}
