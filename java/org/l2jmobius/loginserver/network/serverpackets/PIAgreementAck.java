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
 * This packet acknowledges that the client has accepted the End User License Agreement.<br>
 * It is sent by the server to confirm the user's agreement during the login process.
 * @author UnAfraid
 */
public class PIAgreementAck extends LoginServerPacket
{
	private final int _accountId;
	private final int _status;
	
	/**
	 * Creates a new {@code PIAgreementAck} packet.<br>
	 * This packet acknowledges the agreement status for a specific account.
	 * @param accountId The unique identifier of the account.
	 * @param status The result of the agreement check.
	 */
	public PIAgreementAck(int accountId, int status)
	{
		_accountId = accountId;
		_status = status;
	}
	
	@Override
	protected void writeImpl(LoginClient client, WriteBuffer buffer)
	{
		buffer.writeByte(0x12);
		buffer.writeInt(_accountId);
		buffer.writeByte(_status);
	}
}
