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

import java.security.interfaces.RSAPrivateKey;
import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.commons.network.Client;
import org.l2jmobius.commons.network.Connection;
import org.l2jmobius.commons.network.buffer.ReadBuffer;
import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.loginserver.controller.LoginController;
import org.l2jmobius.loginserver.controller.ScrambledKeyPair;
import org.l2jmobius.loginserver.controller.SessionKey;
import org.l2jmobius.loginserver.enums.AccountKickedReason;
import org.l2jmobius.loginserver.enums.LoginFailReason;
import org.l2jmobius.loginserver.enums.PlayFailReason;
import org.l2jmobius.loginserver.network.serverpackets.AccountKicked;
import org.l2jmobius.loginserver.network.serverpackets.Init;
import org.l2jmobius.loginserver.network.serverpackets.LoginFail;
import org.l2jmobius.loginserver.network.serverpackets.LoginServerPacket;
import org.l2jmobius.loginserver.network.serverpackets.PlayFail;

/**
 * Represents a client connected to the {@code LoginServer}.<br>
 * This class manages session state, cryptographic contexts for RSA/Blowfish encryption, and account metadata.<br>
 * It handles lifecycle hooks for connections and tracks access levels for each active session.
 * @author KenM, BazookaRpm
 */
public class LoginClient extends Client<Connection<LoginClient>>
{
	private static final String DEFAULT_IP = "N/A";
	private static final int DISCONNECT_GRACE_MS = 1000;
	
	private final LoginEncryption _encryption;
	private final ScrambledKeyPair _scrambledPair;
	private final byte[] _blowfishKey;
	
	private String _ip = DEFAULT_IP;
	private final int _sessionId;
	private final long _connectionStartTime;
	
	private String _account;
	private int _accessLevel;
	private int _lastServer;
	private SessionKey _sessionKey;
	private boolean _joinedGS;
	private ConnectionState _connectionState = ConnectionState.CONNECTED;
	
	private Map<Integer, Integer> _charsOnServers;
	private Map<Integer, long[]> _charsToDelete;
	
	/**
	 * Initializes a new {@code LoginClient} instance.<br>
	 * This constructor sets up the session state and encryption keys.<br>
	 * It also checks if the remote IP address is banned.
	 * @param connection The network connection associated with this client.
	 */
	public LoginClient(Connection<LoginClient> connection)
	{
		super(connection);
		
		_scrambledPair = LoginController.getInstance().getScrambledRSAKeyPair();
		_blowfishKey = LoginController.getInstance().getBlowfishKey();
		_ip = connection.getRemoteAddress();
		_sessionId = Rnd.nextInt();
		_connectionStartTime = System.currentTimeMillis();
		
		_encryption = new LoginEncryption();
		_encryption.setKey(_blowfishKey);
		
		if (LoginController.getInstance().isBannedAddress(_ip))
		{
			close(LoginFailReason.REASON_NOT_AUTHED);
		}
	}
	
	/**
	 * Encrypts a specific portion of the provided buffer.<br>
	 * This method uses the current session encryption context.
	 * @param data The {@code WriteBuffer} containing the data to be encrypted.
	 * @param offset The starting position in the buffer.
	 * @param size The number of bytes to encrypt.
	 * @return {@code true} if the encryption was successful, {@code false} otherwise.
	 */
	@Override
	public boolean encrypt(WriteBuffer data, int offset, int size)
	{
		return _encryption.encrypt(data, offset, size);
	}
	
	/**
	 * Decrypts a specific portion of the provided buffer.<br>
	 * This method uses the internal encryption context to process the data.<br>
	 * If decryption fails, the connection will be closed automatically.
	 * @param data The {@code ReadBuffer} containing the encrypted information.
	 * @param offset The starting position within the buffer.
	 * @param size The number of bytes to decrypt.
	 * @return {@code true} if the data was decrypted successfully, or {@code false} otherwise.
	 */
	@Override
	public boolean decrypt(ReadBuffer data, int offset, int size)
	{
		final boolean decrypted = _encryption.decrypt(data, offset, size);
		if (!decrypted)
		{
			close();
		}
		
		return decrypted;
	}
	
	/**
	 * This method is called when a client successfully connects.<br>
	 * It sends an {@code Init} packet to the client.
	 */
	@Override
	public void onConnected()
	{
		sendPacket(new Init(this));
	}
	
	/**
	 * Handles the logic when a client disconnects from the server.<br>
	 * It removes the authenticated client if they have not joined a game server.<br>
	 * This method also includes a brief delay before finishing the cleanup.
	 */
	@Override
	public void onDisconnection()
	{
		if (!_joinedGS)
		{
			LoginController.getInstance().removeAuthedLoginClient(_account);
			try
			{
				Thread.sleep(DISCONNECT_GRACE_MS);
			}
			catch (InterruptedException e)
			{
			}
		}
	}
	
	/**
	 * Retrieves the Blowfish encryption key for this client.<br>
	 * This key is used to secure communication during the session.
	 * @return a {@code byte[]} containing the Blowfish key.
	 */
	public byte[] getBlowfishKey()
	{
		return _blowfishKey;
	}
	
	/**
	 * Retrieves the scrambled modulus from the current session.<br>
	 * This value is used during the cryptographic handshake process.
	 * @return a {@code byte[]} containing the scrambled modulus.
	 */
	public byte[] getScrambledModulus()
	{
		return _scrambledPair.getScrambledModulus();
	}
	
	/**
	 * Retrieves the private RSA key for this client.<br>
	 * This key is used during the cryptographic handshake process.
	 * @return the {@code RSAPrivateKey} associated with the current session.
	 */
	public RSAPrivateKey getRSAPrivateKey()
	{
		return (RSAPrivateKey) _scrambledPair.getPrivateKey();
	}
	
	/**
	 * Retrieves the IP address of the connected client.
	 * @return The {@code String} representation of the client's IP address.
	 */
	public String getIp()
	{
		return _ip;
	}
	
	/**
	 * Retrieves the unique identifier for the current session.<br>
	 * This ID is used to track the client connection.
	 * @return The {@code int} value of the session ID.
	 */
	public int getSessionId()
	{
		return _sessionId;
	}
	
	/**
	 * Retrieves the timestamp when the connection was established.<br>
	 * This value is returned in milliseconds.
	 * @return The start time of the connection as a {@code long}.
	 */
	public long getConnectionStartTime()
	{
		return _connectionStartTime;
	}
	
	/**
	 * Retrieves the account name associated with this client.
	 * @return The {@code String} representing the account name.
	 */
	public String getAccount()
	{
		return _account;
	}
	
	/**
	 * Sets the unique identifier for the user account.<br>
	 * This updates the internal {@code _account} field.
	 * @param account The name of the account to set.
	 */
	public void setAccount(String account)
	{
		_account = account;
	}
	
	/**
	 * Updates the current access level for this client.<br>
	 * The value is stored in the internal {@code _accessLevel} field.
	 * @param accessLevel The new integer value to set as the access level.
	 */
	public void setAccessLevel(int accessLevel)
	{
		_accessLevel = accessLevel;
	}
	
	/**
	 * Retrieves the current access level of the client.<br>
	 * This value determines what actions the user is allowed to perform.
	 * @return The integer representing the current {@code accessLevel}.
	 */
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	/**
	 * Updates the ID of the last server the client connected to.<br>
	 * This value is used to track the user's previous session location.
	 * @param lastServer The unique identifier for the last server.
	 */
	public void setLastServer(int lastServer)
	{
		_lastServer = lastServer;
	}
	
	/**
	 * Retrieves the ID of the last server the client connected to.
	 * @return The {@code int} identifier of the last server.
	 */
	public int getLastServer()
	{
		return _lastServer;
	}
	
	/**
	 * Checks if the client has joined a GameServer.
	 * @return {@code true} if the client is currently in a GameServer, {@code false} otherwise.
	 */
	public boolean hasJoinedGS()
	{
		return _joinedGS;
	}
	
	/**
	 * Updates the status of whether the client has joined a GameServer.<br>
	 * This method sets the internal {@code _joinedGS} flag to the provided value.
	 * @param value The new join status to set.
	 */
	public void setJoinedGS(boolean value)
	{
		_joinedGS = value;
	}
	
	/**
	 * Sets the {@code SessionKey} for this client.<br>
	 * This key is used to manage the secure session state.
	 * @param sessionKey The {@code SessionKey} to assign.
	 */
	public void setSessionKey(SessionKey sessionKey)
	{
		_sessionKey = sessionKey;
	}
	
	/**
	 * Retrieves the current session key for this client.<br>
	 * This key is used to identify the unique session during communication.
	 * @return the {@code SessionKey} associated with this connection.
	 */
	public SessionKey getSessionKey()
	{
		return _sessionKey;
	}
	
	/**
	 * Updates the number of characters associated with a specific server.<br>
	 * This method stores the character count in the internal map.
	 * @param servId The unique identifier for the server.
	 * @param chars The number of characters on that server.
	 */
	public void setCharsOnServ(int servId, int chars)
	{
		if (_charsOnServers == null)
		{
			_charsOnServers = new HashMap<>();
		}
		_charsOnServers.put(servId, chars);
	}
	
	/**
	 * Retrieves the current character counts for all servers.<br>
	 * This method returns a map of server IDs to their respective character counts.
	 * @return A {@code Map<Integer, Integer>} where keys are server IDs and values are character counts.
	 */
	public Map<Integer, Integer> getCharsOnServ()
	{
		return _charsOnServers;
	}
	
	/**
	 * Updates the list of characters waiting to be deleted for a specific server.<br>
	 * This method stores the provided character IDs in the internal tracking map.
	 * @param servId The unique identifier of the server.
	 * @param charsToDel An array containing the IDs of the characters to be deleted.
	 */
	public void serCharsWaitingDelOnServ(int servId, long[] charsToDel)
	{
		if (_charsToDelete == null)
		{
			_charsToDelete = new HashMap<>();
		}
		_charsToDelete.put(servId, charsToDel);
	}
	
	/**
	 * Retrieves the characters currently waiting to be deleted from a server.<br>
	 * The map uses the server ID as the key.
	 * @return A {@code Map} where each entry contains a server ID and an array of character IDs to delete.
	 */
	public Map<Integer, long[]> getCharsWaitingDelOnServ()
	{
		return _charsToDelete;
	}
	
	/**
	 * Retrieves the current state of the connection.<br>
	 * This provides information about the status of the client session.
	 * @return the current {@code ConnectionState}
	 */
	public ConnectionState getConnectionState()
	{
		return _connectionState;
	}
	
	/**
	 * Updates the current state of the network connection.<br>
	 * This method sets the {@code _connectionState} field to the provided value.
	 * @param connectionState The new {@code ConnectionState} to apply.
	 */
	public void setConnectionState(ConnectionState connectionState)
	{
		_connectionState = connectionState;
	}
	
	/**
	 * Sends a {@code LoginServerPacket} to the connected client.<br>
	 * This method handles the transmission of data over the network.
	 * @param packet The {@code LoginServerPacket} object to be sent.
	 */
	public void sendPacket(LoginServerPacket packet)
	{
		writePacket(packet);
	}
	
	/**
	 * Closes the connection and notifies the client of a failure.<br>
	 * This method sends a {@code LoginFail} packet to the user.
	 * @param reason The specific {@code LoginFailReason} for the closure.
	 */
	public void close(LoginFailReason reason)
	{
		sendPacket(new LoginFail(reason));
	}
	
	/**
	 * Closes the connection and sends a failure packet to the client.<br>
	 * This method handles the cleanup of the session resources.
	 * @param reason The specific {@code PlayFailReason} for closing the connection.
	 */
	public void close(PlayFailReason reason)
	{
		close(new PlayFail(reason));
	}
	
	/**
	 * Closes the current connection and kicks the account.<br>
	 * Sends an {@code AccountKicked} packet to the client.
	 * @param reason The specific {@code AccountKickedReason} for closing the connection.
	 */
	public void close(AccountKickedReason reason)
	{
		close(new AccountKicked(reason));
	}
	
	/**
	 * Returns a string representation of the current client.<br>
	 * This includes the class name, account information, and IP address.
	 * @return A formatted {@code String} describing the client state.
	 */
	@Override
	public String toString()
	{
		final String ip = getHostAddress();
		final StringBuilder sb = new StringBuilder();
		sb.append(getClass().getSimpleName());
		sb.append(" [");
		if (_account != null)
		{
			sb.append("Account: ");
			sb.append(_account);
		}
		if (ip != null)
		{
			if (_account != null)
			{
				sb.append(" - ");
			}
			sb.append("IP: ");
			sb.append(ip.isEmpty() ? "disconnected" : ip);
		}
		sb.append(']');
		return sb.toString();
	}
}
