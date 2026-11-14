/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.loginserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.network.packet.WritablePacket;
import org.l2jmobius.loginserver.network.LoginClient;

/**
 * This is the base class for all network packets sent from the login server to the client.<br>
 * It provides a foundation for handling communication between {@code LoginClient} and the server.
 * @author KenM
 */
public abstract class LoginServerPacket extends WritablePacket<LoginClient>
{
	// public static final Logger LOGGER = Logger.getLogger(LoginServerPacket.class.getName());
	
	/**
	 * This method writes the packet data to the provided buffer.<br>
	 * It calls the internal {@code writeImpl} method to perform the operation.<br>
	 * It returns {@code true} if the writing was successful.<br>
	 * It returns {@code false} if an exception occurred during the process.
	 * @param client The {@code LoginClient} receiving the packet.
	 * @param buffer The {@code WriteBuffer} used to store the outgoing data.
	 * @return A boolean indicating whether the write operation succeeded.
	 */
	@Override
	protected boolean write(LoginClient client, WriteBuffer buffer)
	{
		try
		{
			writeImpl(client, buffer);
			return true;
		}
		catch (Exception e)
		{
			// LOGGER.error(e.getMessage(), e);
		}
		
		return false;
	}
	
	protected abstract void writeImpl(LoginClient client, WriteBuffer buffer);
}
