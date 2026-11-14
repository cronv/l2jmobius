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
package org.l2jmobius.loginserver.network.gameserverpackets.receive;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.packet.SimpleReadablePacket;
import org.l2jmobius.loginserver.controller.LoginController;

/**
 * Handles the request to temporarily ban a player account.<br>
 * This packet processes the incoming data to apply a time-limited restriction.<br>
 * It interacts with the database to update the account status.
 * @author mrTJO
 */
public class RequestTempBan extends SimpleReadablePacket
{
	private static final Logger LOGGER = Logger.getLogger(RequestTempBan.class.getName());
	
	private final String _accountName;
	private final String _ip;
	long _banTime;
	
	/**
	 * This constructor initializes a new {@code RequestTempBan} packet.<br>
	 * It decrypts the raw data and extracts account details.<br>
	 * The method then triggers the user ban process.
	 * @param decrypt The encrypted byte array received from the network.
	 */
	public RequestTempBan(byte[] decrypt)
	{
		super(decrypt);
		readByte(); // Packet id, it is already processed.
		
		_accountName = readString();
		_ip = readString();
		_banTime = readLong();
		final boolean haveReason = readByte() != 0;
		if (haveReason)
		{
			readString(); // _banReason
		}
		
		banUser();
	}
	
	/**
	 * This method processes a temporary ban for a user.<br>
	 * It updates the database with the new ban time.<br>
	 * It also adds an IP ban via {@code LoginController}.
	 */
	private void banUser()
	{
		// _banTime is a future timestamp (currentTimeMillis + duration), sent by the GameServer.
		final long banDuration = _banTime - System.currentTimeMillis();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO account_data VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value=?"))
		{
			ps.setString(1, _accountName);
			ps.setString(2, "ban_temp");
			ps.setString(3, Long.toString(_banTime));
			ps.setString(4, Long.toString(_banTime));
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
		
		// addBanForAddress expects a duration in ms, so derive it from the future timestamp.
		LoginController.getInstance().addBanForAddress(_ip, banDuration);
	}
}
