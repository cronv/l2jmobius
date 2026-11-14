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
package org.l2jmobius.gameserver.network.serverpackets.herobook;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to handle the charging process of an {@code ExHeroBook}.<br>
 * It provides necessary data for the client to display the correct animation or state during the charge.
 * @author Index
 */
public class ExHeroBookCharge extends ServerPacket
{
	private final boolean _success;
	
	/**
	 * This constructor initializes the {@code ExHeroBookCharge} packet.<br>
	 * It sets whether the hero book charge was successful.
	 * @param success The status of the charge operation. Use {@code true} for success and {@code false} for failure.
	 */
	public ExHeroBookCharge(boolean success)
	{
		_success = success;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_HERO_BOOK_CHARGE.writeId(this, buffer);
		buffer.writeByte(_success ? 1 : 0);
	}
}