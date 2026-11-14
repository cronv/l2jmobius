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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.network.packet.WritablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;

/**
 * This is the base class for all packets sent from the server to the client.<br>
 * It provides the foundation for handling network communication and data serialization.
 * @author Mobius
 */
public abstract class ServerPacket extends WritablePacket<GameClient>
{
	/**
	 * Writes an integer to the {@code WriteBuffer} using a variable length format.<br>
	 * If the value is smaller than {@code Short.MAX_VALUE}, it is written as a short.<br>
	 * If the value is larger, it writes the maximum short followed by the full integer.
	 * @param value The integer value to write.
	 * @param buffer The buffer where the data will be stored.
	 */
	protected void writeOptionalInt(int value, WriteBuffer buffer)
	{
		if (value >= Short.MAX_VALUE)
		{
			buffer.writeShort(Short.MAX_VALUE);
			buffer.writeInt(value);
		}
		else
		{
			buffer.writeShort(value);
		}
	}
	
	/**
	 * Writes a {@code CharSequence} to the {@code WriteBuffer} as a field of a fixed width.<br>
	 * Each character is written as a 2-byte {@code short} and the rest of the field is padded with null characters.<br>
	 * The text is truncated where it would leave no room for a terminator, so the field always ends with at least one null.
	 * @param text The {@code CharSequence} to write.
	 * @param length The width of the field, in characters.
	 * @param buffer The buffer where the data will be stored.
	 */
	protected void writeFixedString(CharSequence text, int length, WriteBuffer buffer)
	{
		final CharSequence value = text == null ? "" : text;
		final int count = Math.min(value.length(), length - 1);
		for (int i = 0; i < count; i++)
		{
			buffer.writeChar(value.charAt(i));
		}
		
		for (int i = count; i < length; i++)
		{
			buffer.writeChar('\000');
		}
	}
	
	/**
	 * Writes the packet data to the specified client.<br>
	 * This method checks if the {@code GameClient} is active before sending.<br>
	 * It catches any exceptions during the writing process and logs them.
	 * @param client The {@code GameClient} receiving the packet.
	 * @param buffer The {@code WriteBuffer} used to store the data.
	 * @return {@code true} if the write was successful or the client is disconnected, and {@code false} if an error occurred.
	 */
	@Override
	protected boolean write(GameClient client, WriteBuffer buffer)
	{
		final GameClient c = client;
		if ((c == null) || c.isDetached() || (c.getConnectionState() == ConnectionState.DISCONNECTED))
		{
			return true; // Disconnected client.
		}
		
		try
		{
			writeImpl(c, buffer);
			return true;
		}
		catch (Exception e)
		{
			PacketLogger.warning("Error writing packet " + this + " to client (" + e.getMessage() + ") " + c + "]]");
			PacketLogger.warning(TraceUtil.getStackTrace(e));
		}
		
		return false;
	}
	
	/**
	 * Executes the core logic for this packet.<br>
	 * This method is called to process the server-side actions.
	 * @param player The {@code Player} object associated with this request.
	 */
	public void runImpl(Player player)
	{
	}
	
	protected abstract void writeImpl(GameClient client, WriteBuffer buffer) throws Exception;
}
