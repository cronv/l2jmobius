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
package org.l2jmobius.gameserver.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.data.xml.SecondaryAuthData;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.Ex2ndPasswordAck;
import org.l2jmobius.gameserver.network.serverpackets.Ex2ndPasswordCheck;
import org.l2jmobius.gameserver.network.serverpackets.Ex2ndPasswordVerify;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

/**
 * Handles the authentication process for secondary passwords.<br>
 * It verifies user credentials against the database and manages the associated network packets.
 * @author mrTJO
 */
public class SecondaryPasswordAuth
{
	private static final Logger LOGGER = Logger.getLogger(SecondaryPasswordAuth.class.getName());
	private final GameClient _activeClient;
	
	private String _password;
	private int _wrongAttempts;
	private boolean _authed;
	
	private static final String VAR_PWD = "secauth_pwd";
	private static final String VAR_WTE = "secauth_wte";
	
	private static final String SELECT_PASSWORD = "SELECT var, value FROM account_gsdata WHERE account_name=? AND var LIKE 'secauth_%'";
	private static final String INSERT_PASSWORD = "INSERT INTO account_gsdata VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value=?";
	private static final String UPDATE_PASSWORD = "UPDATE account_gsdata SET value=? WHERE account_name=? AND var=?";
	
	private static final String INSERT_ATTEMPT = "INSERT INTO account_gsdata VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE value=?";
	
	/**
	 * Initializes a new instance of the {@code SecondaryPasswordAuth} class.<br>
	 * It sets up the initial state for the authentication process.<br>
	 * This constructor also triggers the loading of the password from the database.
	 * @param activeClient The {@code GameClient} currently performing the authentication.
	 */
	public SecondaryPasswordAuth(GameClient activeClient)
	{
		_activeClient = activeClient;
		_password = null;
		_wrongAttempts = 0;
		_authed = false;
		loadPassword();
	}
	
	/**
	 * Loads the secondary password and wrong attempt count from the database.<br>
	 * It retrieves data for the current account using {@code _activeClient}.<br>
	 * If an error occurs, it logs a severe message.
	 */
	private void loadPassword()
	{
		String var = null;
		String value = null;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PASSWORD))
		{
			statement.setString(1, _activeClient.getAccountName());
			try (ResultSet rs = statement.executeQuery())
			{
				while (rs.next())
				{
					var = rs.getString("var");
					value = rs.getString("value");
					if (var.equals(VAR_PWD))
					{
						_password = value;
					}
					else if (var.equals(VAR_WTE))
					{
						_wrongAttempts = Integer.parseInt(value);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error while reading password.", e);
		}
	}
	
	/**
	 * Saves the provided secondary password to the database.<br>
	 * This method validates the input and checks if a password already exists.<br>
	 * It disconnects the client if a conflict is detected.
	 * @param value The plain text password to be saved.
	 * @return {@code true} if the password was successfully saved, {@code false} otherwise.
	 */
	public boolean savePassword(String value)
	{
		if (passwordExist())
		{
			LOGGER.warning("[SecondaryPasswordAuth]" + _activeClient.getAccountName() + " forced savePassword");
			Disconnection.of(_activeClient).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			return false;
		}
		
		if (!validatePassword(value))
		{
			_activeClient.sendPacket(new Ex2ndPasswordAck(0, Ex2ndPasswordAck.WRONG_PATTERN));
			return false;
		}
		
		final String password = cryptPassword(value);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_PASSWORD))
		{
			statement.setString(1, _activeClient.getAccountName());
			statement.setString(2, VAR_PWD);
			statement.setString(3, password);
			statement.setString(4, password);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error while writing password.", e);
			return false;
		}
		
		_password = password;
		return true;
	}
	
	/**
	 * Records the number of failed login attempts in the database.<br>
	 * This method updates the {@code secauth_wte} variable for the current account.<br>
	 * It returns {@code true} if the operation succeeds.<br>
	 * It returns {@code false} if a database error occurs.
	 * @param attempts The number of failed attempts to save.
	 * @return {@code true} if successful, {@code false} otherwise.
	 */
	public boolean insertWrongAttempt(int attempts)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_ATTEMPT))
		{
			statement.setString(1, _activeClient.getAccountName());
			statement.setString(2, VAR_WTE);
			statement.setString(3, Integer.toString(attempts));
			statement.setString(4, Integer.toString(attempts));
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error while writing wrong attempts.", e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Updates the secondary password for the current account.<br>
	 * This method verifies the old password and validates the new one before saving it to the database.<br>
	 * It returns {@code false} if any validation fails or an error occurs.
	 * @param oldPassword The current secondary password used for verification.
	 * @param newPassword The new secondary password to be set.
	 * @return {@code true} if the password was successfully changed, otherwise {@code false}.
	 */
	public boolean changePassword(String oldPassword, String newPassword)
	{
		if (!passwordExist())
		{
			LOGGER.warning("[SecondaryPasswordAuth]" + _activeClient.getAccountName() + " forced changePassword");
			Disconnection.of(_activeClient).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
			return false;
		}
		
		if (!checkPassword(oldPassword, true))
		{
			return false;
		}
		
		if (!validatePassword(newPassword))
		{
			_activeClient.sendPacket(new Ex2ndPasswordAck(2, Ex2ndPasswordAck.WRONG_PATTERN));
			return false;
		}
		
		final String password = cryptPassword(newPassword);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_PASSWORD))
		{
			statement.setString(1, password);
			statement.setString(2, _activeClient.getAccountName());
			statement.setString(3, VAR_PWD);
			statement.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Error while reading password.", e);
			return false;
		}
		
		_password = password;
		_authed = false;
		return true;
	}
	
	/**
	 * Verifies the provided password against the stored value.<br>
	 * It handles failed attempts and potential account bans.
	 * @param value The plain text password to check.
	 * @param skipAuth If {@code true}, it skips updating the authentication status.
	 * @return {@code true} if the password is correct, {@code false} otherwise.
	 */
	public boolean checkPassword(String value, boolean skipAuth)
	{
		final String password = cryptPassword(value);
		if (!password.equals(_password))
		{
			_wrongAttempts++;
			final int maxAttempts = SecondaryAuthData.getInstance().getMaxAttempts();
			if (_wrongAttempts < maxAttempts)
			{
				_activeClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_WRONG, _wrongAttempts));
				insertWrongAttempt(_wrongAttempts);
			}
			else
			{
				final String accountName = _activeClient.getAccountName();
				final String ip = _activeClient.getIp();
				final int banTime = SecondaryAuthData.getInstance().getBanTime();
				LoginServerThread.getInstance().sendTempBan(accountName, ip, banTime);
				LoginServerThread.getInstance().sendMail(accountName, "SATempBan", ip, Integer.toString(maxAttempts), Long.toString(banTime), SecondaryAuthData.getInstance().getRecoveryLink());
				LOGGER.warning(accountName + " - (" + ip + ") has inputted the wrong password " + _wrongAttempts + " times in row.");
				insertWrongAttempt(0);
				_activeClient.close(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_BAN, maxAttempts));
			}
			
			return false;
		}
		
		if (!skipAuth)
		{
			_authed = true;
			_activeClient.sendPacket(new Ex2ndPasswordVerify(Ex2ndPasswordVerify.PASSWORD_OK, _wrongAttempts));
		}
		
		insertWrongAttempt(0);
		return true;
	}
	
	/**
	 * Checks if a secondary password has been set for the account.<br>
	 * It verifies if the {@code _password} field is not {@code null}.
	 * @return {@code true} if a password exists, {@code false} otherwise.
	 */
	public boolean passwordExist()
	{
		return _password != null;
	}
	
	/**
	 * Opens the secondary password dialog for the player.<br>
	 * It checks if a password exists to determine which prompt to send.<br>
	 * The method sends an {@code Ex2ndPasswordCheck} packet to the {@code _activeClient}.
	 */
	public void openDialog()
	{
		if (passwordExist())
		{
			_activeClient.sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_PROMPT));
		}
		else
		{
			_activeClient.sendPacket(new Ex2ndPasswordCheck(Ex2ndPasswordCheck.PASSWORD_NEW));
		}
	}
	
	/**
	 * Checks if the secondary authentication was successful.<br>
	 * Returns {@code true} if the user is authenticated.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating the authentication status.
	 */
	public boolean isAuthed()
	{
		return _authed;
	}
	
	/**
	 * Encrypts a plain text password using the {@code SHA} algorithm.<br>
	 * The result is returned as a {@code Base64} encoded string.<br>
	 * Returns {@code null} if the hashing algorithm is not found.
	 * @param password The plain text password to encrypt.
	 * @return The encrypted password string or {@code null}.
	 */
	private String cryptPassword(String password)
	{
		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA");
			final byte[] raw = password.getBytes(StandardCharsets.UTF_8);
			final byte[] hash = md.digest(raw);
			return Base64.getEncoder().encodeToString(hash);
		}
		catch (NoSuchAlgorithmException e)
		{
			LOGGER.severe("[SecondaryPasswordAuth]Unsupported Algorythm");
		}
		
		return null;
	}
	
	/**
	 * Checks if the provided password meets the security requirements.<br>
	 * The password must contain only numbers.<br>
	 * The length must be between 6 and 8 characters inclusive.<br>
	 * It also checks if the password is in the forbidden list.
	 * @param password The password string to validate.
	 * @return {@code true} if the password is valid, otherwise {@code false}.
	 */
	private boolean validatePassword(String password)
	{
		if (!StringUtil.isNumeric(password))
		{
			return false;
		}
		
		final int length = password.length();
		if ((length < 6) || (length > 8))
		{
			return false;
		}
		
		return !SecondaryAuthData.getInstance().isForbiddenPassword(password);
	}
}
