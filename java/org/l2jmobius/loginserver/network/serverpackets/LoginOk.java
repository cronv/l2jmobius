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
package org.l2jmobius.loginserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.loginserver.controller.SessionKey;
import org.l2jmobius.loginserver.network.LoginClient;

/**
 * This packet is sent to the client after a successful authentication.<br>
 * It serializes two {@code LoginOk} integers and fills the protocol-reserved fields.
 */
public class LoginOk extends LoginServerPacket
{
	// Constants.
	private static final int OPCODE_LOGIN_OK = 0x03;
	private static final int RESERVED_ZERO = 0x00;
	private static final int RESERVED_VALUE_3EA = 0x000003ea;
	private static final int RESERVED_BYTES_LEN = 16;
	private static final byte[] RESERVED_BYTES_16 = new byte[RESERVED_BYTES_LEN];
	
	// Session key parts.
	private final int _loginOkPart1;
	private final int _loginOkPart2;
	
	/**
	 * Creates a new {@code LoginOk} packet.<br>
	 * This constructor extracts the required IDs from the provided {@code SessionKey}.
	 * @param sessionKey The {@code SessionKey} containing the login identifiers.
	 */
	public LoginOk(SessionKey sessionKey)
	{
		_loginOkPart1 = sessionKey.getLoginOkID1();
		_loginOkPart2 = sessionKey.getLoginOkID2();
	}
	
	@Override
	protected void writeImpl(LoginClient client, WriteBuffer buffer)
	{
		buffer.writeByte(OPCODE_LOGIN_OK);
		buffer.writeInt(_loginOkPart1);
		buffer.writeInt(_loginOkPart2);
		buffer.writeInt(RESERVED_ZERO);
		buffer.writeInt(RESERVED_ZERO);
		buffer.writeInt(RESERVED_VALUE_3EA);
		buffer.writeInt(RESERVED_ZERO);
		buffer.writeInt(RESERVED_ZERO);
		buffer.writeInt(RESERVED_ZERO);
		buffer.writeBytes(RESERVED_BYTES_16);
	}
}
