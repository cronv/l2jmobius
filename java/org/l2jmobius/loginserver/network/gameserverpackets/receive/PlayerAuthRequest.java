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

package org.l2jmobius.loginserver.network.gameserverpackets.receive;

import org.l2jmobius.commons.network.packet.SimpleReadablePacket;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.controller.LoginController;
import org.l2jmobius.loginserver.controller.SessionKey;
import org.l2jmobius.loginserver.network.gameserverpackets.send.PlayerAuthResponse;

/**
 * This class handles authentication requests received from a game server.<br>
 * It validates the request against a stored {@code SessionKey} and sends a response to the client.<br>
 * Upon successful validation, it removes the login client from the authenticated list.
 * @author -Wooden-, BazookaRpm
 */
public class PlayerAuthRequest extends SimpleReadablePacket
{
	/**
	 * Initializes a new {@code PlayerAuthRequest} from decrypted data.<br>
	 * This constructor reads the account details and session keys from the packet.<br>
	 * It validates the keys against the stored session in the {@code LoginController}.<br>
	 * If valid, it removes the client from the authenticated list and sends a success response.
	 * @param decrypt The decrypted byte array containing the packet data.
	 * @param server The {@code GameServerThread} used to send responses back to the client.
	 */
	public PlayerAuthRequest(byte[] decrypt, GameServerThread server)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		final String account = readString();
		final int playKey1 = readInt();
		final int playKey2 = readInt();
		final int loginKey1 = readInt();
		final int loginKey2 = readInt();
		
		final SessionKey sessionKey = new SessionKey(loginKey1, loginKey2, playKey1, playKey2);
		final SessionKey storedKey = LoginController.getInstance().getKeyForAccount(account);
		
		if ((storedKey != null) && storedKey.equals(sessionKey))
		{
			LoginController.getInstance().removeAuthedLoginClient(account);
			server.sendPacket(new PlayerAuthResponse(account, true));
		}
		else
		{
			server.sendPacket(new PlayerAuthResponse(account, false));
		}
	}
}
