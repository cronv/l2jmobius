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
import org.l2jmobius.loginserver.network.LoginClient;

/**
 * This packet handles the verification of the user's agreement to the terms of service.<br>
 * It is sent by {@code LoginServer} to ensure the client has accepted the necessary legal requirements.
 * @author UnAfraid
 */
public class PIAgreementCheck extends LoginServerPacket
{
	private final int _accountId;
	private final int _status;
	
	/**
	 * Creates a new {@code PIAgreementCheck} packet.<br>
	 * This constructor initializes the agreement check for a specific user.
	 * @param accountId The unique identifier for the account.
	 * @param status The current status of the agreement.
	 */
	public PIAgreementCheck(int accountId, int status)
	{
		_accountId = accountId;
		_status = status;
	}
	
	@Override
	protected void writeImpl(LoginClient client, WriteBuffer buffer)
	{
		buffer.writeByte(0x11);
		buffer.writeInt(_accountId);
		buffer.writeByte(_status);
	}
}
