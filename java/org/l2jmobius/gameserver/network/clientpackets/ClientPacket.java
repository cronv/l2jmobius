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

import org.l2jmobius.commons.network.packet.ReadablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;

/**
 * This class serves as the base class for all packets sent from the client to the server.<br>
 * It provides a foundation for handling incoming network data and identifying specific client actions.
 * @author Mobius
 */
public abstract class ClientPacket extends ReadablePacket<GameClient>
{
	/**
	 * Reads the data from the incoming packet.<br>
	 * This method calls {@code readImpl()} to process the content.<br>
	 * It catches any exceptions and logs a warning if reading fails.
	 * @return {@code true} if the packet was read successfully, or {@code false} otherwise.
	 */
	@Override
	public boolean read()
	{
		try
		{
			readImpl();
			return true;
		}
		catch (Exception e)
		{
			PacketLogger.warning("Client: " + getClient() + " - Failed reading: " + getClass().getSimpleName() + " ; " + e.getMessage());
			PacketLogger.warning(TraceUtil.getStackTrace(e));
		}
		
		return false;
	}
	
	protected abstract void readImpl();
	
	@Override
	public void run()
	{
		try
		{
			runImpl();
		}
		catch (Exception e)
		{
			final GameClient client = getClient();
			PacketLogger.warning("Client: " + client + " - Failed running: " + getClass().getSimpleName() + " ; " + e.getMessage());
			PacketLogger.warning(TraceUtil.getStackTrace(e));
			
			// In case of EnterWorld error kick player from game.
			if (this instanceof EnterWorld)
			{
				client.closeNow();
			}
		}
	}
	
	protected abstract void runImpl();
	
	/**
	 * Retrieves the {@code Player} associated with this packet.<br>
	 * This method fetches the player from the current {@code GameClient}.
	 * @return the {@code Player} object.
	 */
	protected Player getPlayer()
	{
		return getClient().getPlayer();
	}
}
