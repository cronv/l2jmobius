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
package org.l2jmobius.loginserver.network;

import java.util.logging.Logger;

import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.packet.PacketHandler;
import org.l2jmobius.commons.network.packet.ReadablePacket;
import org.l2jmobius.loginserver.enums.LoginFailReason;

/**
 * This class handles incoming network packets from the {@code LoginClient}.<br>
 * It processes login-related requests and manages the communication between the client and the server.<br>
 * It implements the {@code PacketHandler} interface to route specific data to the correct logic.
 * @author Mobius
 */
public class LoginPacketHandler implements PacketHandler<LoginClient>
{
	private static final Logger LOGGER = Logger.getLogger(LoginPacketHandler.class.getName());
	
	/**
	 * Processes an incoming network packet for a login client.<br>
	 * This method reads the packet ID from the {@code ReadBuffer}.<br>
	 * It validates the packet against the current connection state.<br>
	 * Returns a new {@code ReadablePacket} if the request is valid.
	 * @param buffer The buffer containing the incoming data.
	 * @param client The client instance associated with the connection.
	 * @return A new {@code ReadablePacket} or {@code null} if the packet is invalid.
	 */
	@Override
	public ReadablePacket<LoginClient> handle(ReadBuffer buffer, LoginClient client)
	{
		// Read packet id.
		final int packetId;
		try
		{
			packetId = Byte.toUnsignedInt(buffer.readByte());
		}
		catch (Exception e)
		{
			LOGGER.warning("LoginPacketHandler: Problem receiving packet id from " + client);
			// Usually a port scanner or bot that sent fewer than 3 bytes, no need to log the full stack trace.
			// LOGGER.warning(TraceUtil.getStackTrace(e));
			client.close(LoginFailReason.REASON_ACCESS_FAILED);
			return null;
		}
		
		// Check if packet id is within valid range.
		if ((packetId < 0) || (packetId >= LoginClientPackets.PACKET_ARRAY.length))
		{
			return null;
		}
		
		// Find packet enum.
		final LoginClientPackets packetEnum = LoginClientPackets.PACKET_ARRAY[packetId];
		if (packetEnum == null)
		{
			return null;
		}
		
		// Check connection state.
		if (!packetEnum.getConnectionStates().contains(client.getConnectionState()))
		{
			return null;
		}
		
		// Create new LoginClientPacket.
		return packetEnum.newPacket();
	}
}
