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
package org.l2jmobius.loginserver;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.crypt.NewCrypt;
import org.l2jmobius.commons.network.packet.SimpleWritablePacket;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.loginserver.controller.LoginController;
import org.l2jmobius.loginserver.controller.ScrambledKeyPair;
import org.l2jmobius.loginserver.data.GameServerTable;
import org.l2jmobius.loginserver.data.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.network.GameServerPacketHandler;
import org.l2jmobius.loginserver.network.GameServerPacketHandler.GameServerState;
import org.l2jmobius.loginserver.network.gameserverpackets.send.ChangePasswordResponse;
import org.l2jmobius.loginserver.network.gameserverpackets.send.InitLS;
import org.l2jmobius.loginserver.network.gameserverpackets.send.KickPlayer;
import org.l2jmobius.loginserver.network.gameserverpackets.send.LoginServerFail;
import org.l2jmobius.loginserver.network.gameserverpackets.send.RequestCharacters;

/**
 * Handles the connection and communication between the login server and a single game server.<br>
 * This class manages authentication, packet exchange, and player account tracking for each active connection.
 * @author -Wooden-, KenM, Mobius
 */
public class GameServerThread extends Thread
{
	protected static final Logger LOGGER = Logger.getLogger(GameServerThread.class.getName());
	
	// Constants.
	private static final int PACKET_HEADER_SIZE = 2;
	private static final int BLOWFISH_BLOCK_SIZE = 8;
	private static final int HIGH_BYTE_MULTIPLIER = 256;
	private static final String DEFAULT_BLOWFISH_KEY = "_;v.]05-31!|+-%xT!^[$\00";
	
	// Player Account Management.
	private final Set<String> _accountsOnGameServer = ConcurrentHashMap.newKeySet();
	
	// Network Communication.
	private final Socket _socket;
	private InputStream _inputStream;
	private OutputStream _outputStream;
	
	// Cryptography.
	private final RSAPublicKey _publicKey;
	private final RSAPrivateKey _privateKey;
	private NewCrypt _blowfishCipher;
	
	// Connection State Management.
	private final String _connectionIp;
	private String _connectionIpAddress;
	private GameServerInfo _gameServerInfo;
	private GameServerState _loginConnectionState = GameServerState.CONNECTED;
	
	/**
	 * Initializes a new {@code GameServerThread} for a specific connection.<br>
	 * This constructor sets up the network streams and encryption keys.<br>
	 * It also starts the thread automatically.
	 * @param socket The {@code Socket} object representing the connection to the game server.
	 */
	public GameServerThread(Socket socket)
	{
		_socket = socket;
		_connectionIp = socket.getInetAddress().getHostAddress();
		
		try
		{
			// Initialize network streams for communication.
			_inputStream = _socket.getInputStream();
			_outputStream = new BufferedOutputStream(_socket.getOutputStream());
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to initialize network streams - " + e.getMessage());
		}
		
		// Retrieve RSA key pair for secure initial communication.
		final ScrambledKeyPair keyPair = LoginController.getInstance().getScrambledRSAKeyPair();
		_privateKey = (RSAPrivateKey) keyPair.getPrivateKey();
		_publicKey = (RSAPublicKey) keyPair.getPublicKey();
		
		// Initialize Blowfish cipher with default key for packet encryption.
		_blowfishCipher = new NewCrypt(DEFAULT_BLOWFISH_KEY);
		
		// Set thread name for easier debugging and monitoring.
		// Java 18: setName(getClass().getSimpleName() + "-" + getId() + "@" + _connectionIp);
		// Java 19+: Using threadId() method.
		setName(getClass().getSimpleName() + "-" + threadId() + "@" + _connectionIp);
		
		// Start the thread to begin processing.
		start();
	}
	
	@Override
	public void run()
	{
		// Resolve and store the connection IP address.
		_connectionIpAddress = _socket.getInetAddress().getHostAddress();
		
		// Check if the connecting IP is banned.
		if (isBannedGameserverIP(_connectionIpAddress))
		{
			LOGGER.info("GameServerRegistration: IP Address " + _connectionIpAddress + " is on the banned IP list. Connection rejected.");
			forceClose(LoginServerFail.REASON_IP_BANNED);
			return; // Terminate processing for banned IP.
		}
		
		// Send initial packet with RSA public key for secure key exchange.
		final InitLS initializationPacket = new InitLS(_publicKey.getModulus().toByteArray());
		try
		{
			sendPacket(initializationPacket);
			
			// Main packet processing loop.
			int lengthHighByte = 0;
			int lengthLowByte = 0;
			int packetLength = 0;
			boolean checksumValid = false;
			
			while (true)
			{
				// Read packet length header (2 bytes: low byte, high byte).
				lengthLowByte = _inputStream.read();
				lengthHighByte = _inputStream.read();
				packetLength = (lengthHighByte * HIGH_BYTE_MULTIPLIER) + lengthLowByte;
				
				// Check for connection termination.
				if ((lengthHighByte < 0) || _socket.isClosed())
				{
					LOGGER.finer("GameServerThread: Game server terminated the connection gracefully.");
					break;
				}
				
				// Reject lengths that cannot hold an encrypted packet (Blowfish works on whole 8 byte blocks).
				final int dataLength = packetLength - PACKET_HEADER_SIZE;
				if ((dataLength < 8) || ((dataLength % 8) != 0))
				{
					LOGGER.warning("Invalid packet length " + packetLength + " received from game server (" + _connectionIpAddress + "). Closing connection.");
					break;
				}
				
				// Allocate buffer for packet data (excluding header).
				final byte[] packetData = new byte[packetLength - PACKET_HEADER_SIZE];
				int totalBytesReceived = 0;
				int bytesRead = 0;
				int remainingBytes = packetLength - PACKET_HEADER_SIZE;
				
				// Read the complete packet data.
				while ((bytesRead != -1) && (totalBytesReceived < (packetLength - PACKET_HEADER_SIZE)))
				{
					bytesRead = _inputStream.read(packetData, totalBytesReceived, remainingBytes);
					totalBytesReceived += bytesRead;
					remainingBytes -= bytesRead;
				}
				
				// Validate that we received the complete packet.
				final int expectedLength = packetLength - PACKET_HEADER_SIZE;
				if (totalBytesReceived != expectedLength)
				{
					LOGGER.warning("Incomplete packet received from game server. Expected " + expectedLength + " bytes, got " + totalBytesReceived + ". Closing connection.");
					break;
				}
				
				// Decrypt the packet data using Blowfish cipher.
				_blowfishCipher.decrypt(packetData, 0, packetData.length);
				
				// Verify packet integrity using checksum.
				checksumValid = NewCrypt.verifyChecksum(packetData);
				if (!checksumValid)
				{
					LOGGER.warning("Packet checksum verification failed. Possible data corruption or tampering detected. Closing connection.");
					return;
				}
				
				// Forward the validated packet to the packet handler.
				GameServerPacketHandler.handle(packetData, this);
			}
		}
		catch (IOException e)
		{
			// Log connection loss with server identification.
			final int serverId = getServerId();
			final String serverIdentification = serverId != -1 ? "[" + serverId + "] " + GameServerTable.getInstance().getServerNameById(serverId) : "(" + _connectionIpAddress + ")";
			final String connectionLostMessage = "GameServer " + serverIdentification + ": Connection lost - " + e.getMessage();
			LOGGER.info(connectionLostMessage);
		}
		finally
		{
			// Cleanup operations when thread terminates.
			if (isAuthed())
			{
				// Mark the game server as offline.
				if (_gameServerInfo != null)
				{
					_gameServerInfo.setDown();
				}
				
				final int serverId = getServerId();
				LOGGER.info("Server [" + serverId + "] " + GameServerTable.getInstance().getServerNameById(serverId) + " is now marked as disconnected.");
			}
			
			// Remove this game server from active connections and cleanup flood protection.
			LoginServer.getInstance().getGameServerListener().removeGameServer(this);
			LoginServer.getInstance().getGameServerListener().removeFloodProtection(_connectionIp);
		}
	}
	
	/**
	 * Checks if a specific account is currently connected to this game server.<br>
	 * This method looks up the {@code accountName} in the internal tracking set.
	 * @param accountName The name of the account to check.
	 * @return {@code true} if the account exists on this server, {@code false} otherwise.
	 */
	public boolean hasAccountOnGameServer(String accountName)
	{
		return _accountsOnGameServer.contains(accountName);
	}
	
	/**
	 * Retrieves the current number of players on this game server.<br>
	 * This method counts the entries in the internal account set.
	 * @return The total count of accounts currently on the game server as an {@code int}.
	 */
	public int getPlayerCount()
	{
		return _accountsOnGameServer.size();
	}
	
	/**
	 * Links the current thread to a specific {@code GameServerInfo} object.<br>
	 * This method configures the network settings and player capacity for the server.<br>
	 * It also marks the server as authenticated.
	 * @param gameServerInfo The {@code GameServerInfo} object to associate with this thread.
	 * @param port The network port used by the game server.
	 * @param hosts An array of hostnames or IP addresses for the server.
	 * @param maxPlayers The maximum number of players allowed on the server.
	 */
	public void attachGameServerInfo(GameServerInfo gameServerInfo, int port, String[] hosts, int maxPlayers)
	{
		// Establish bidirectional relationship between thread and server info.
		setGameServerInfo(gameServerInfo);
		gameServerInfo.setGameServerThread(this);
		
		// Configure server network settings.
		gameServerInfo.setPort(port);
		setGameHosts(hosts);
		
		// Set player capacity and mark as authenticated.
		gameServerInfo.setMaxPlayers(maxPlayers);
		gameServerInfo.setAuthed(true);
	}
	
	/**
	 * Closes the connection to the game server immediately.<br>
	 * This method sends a {@code LoginServerFail} packet before closing the socket.<br>
	 * It handles any {@code IOException} that occurs during the closure process.
	 * @param reasonCode The integer code representing the reason for the forced closure.
	 */
	public void forceClose(int reasonCode)
	{
		// Send failure notification to the game server.
		sendPacket(new LoginServerFail(reasonCode));
		
		try
		{
			// Close the socket connection immediately.
			_socket.close();
		}
		catch (IOException e)
		{
			LOGGER.finer("GameServerThread: Failed to close socket for banned server. Socket may already be closed - " + e.getMessage());
		}
	}
	
	/**
	 * Checks if the provided IP address is on the banned list.<br>
	 * @param ipAddress The IP address to check.
	 * @return {@code true} if the IP is banned, otherwise {@code false}.
	 */
	public static boolean isBannedGameserverIP(String ipAddress)
	{
		return LoginController.getInstance().isBannedAddress(ipAddress);
	}
	
	/**
	 * Sends a network packet to the connected game server.<br>
	 * This method handles padding, checksum calculation, and encryption.<br>
	 * It ensures thread-safe transmission over the output stream.
	 * @param packet The {@code SimpleWritablePacket} to be sent.
	 */
	public void sendPacket(SimpleWritablePacket packet)
	{
		// Verify connection is still valid.
		if ((_blowfishCipher == null) || (_socket == null) || _socket.isClosed())
		{
			return;
		}
		
		try
		{
			// Prepare packet data.
			packet.write(); // Write the packet content.
			packet.writeInt(0); // Reserve space for checksum.
			
			int dataSize = packet.getLength() - PACKET_HEADER_SIZE; // Size without header.
			final int paddingNeeded = dataSize % BLOWFISH_BLOCK_SIZE; // Calculate padding for 8-byte blocks.
			
			// Add padding bytes to align with Blowfish block size.
			if (paddingNeeded != 0)
			{
				for (int i = paddingNeeded; i < BLOWFISH_BLOCK_SIZE; i++)
				{
					packet.writeByte(0);
				}
			}
			
			// Get the complete packet data including header.
			final byte[] packetData = packet.getSendableBytes();
			dataSize = packetData.length - PACKET_HEADER_SIZE; // Recalculate size after padding.
			
			// Thread-safe packet transmission.
			synchronized (_outputStream)
			{
				// Append checksum for integrity verification.
				NewCrypt.appendChecksum(packetData, PACKET_HEADER_SIZE, dataSize);
				
				// Encrypt the data portion (excluding header).
				_blowfishCipher.crypt(packetData, PACKET_HEADER_SIZE, dataSize);
				
				// Send the complete packet.
				_outputStream.write(packetData);
				try
				{
					_outputStream.flush();
				}
				catch (IOException e)
				{
					// Game server might have terminated connection during flush.
					LOGGER.finer("GameServerThread: Failed to flush output stream. Game server may have disconnected - " + e.getMessage());
				}
			}
		}
		catch (IOException e)
		{
			LOGGER.severe("GameServerThread: IOException occurred while sending packet " + packet.getClass().getSimpleName() + " - " + e.getMessage());
			LOGGER.severe(TraceUtil.getStackTrace(e));
		}
	}
	
	/**
	 * Disconnects a specific player from the game server.<br>
	 * This method sends a {@code KickPlayer} packet to the target user.
	 * @param accountName The name of the account to be kicked.
	 */
	public void kickPlayer(String accountName)
	{
		sendPacket(new KickPlayer(accountName));
	}
	
	/**
	 * Sends a request to the game server for character data.<br>
	 * This method uses the {@code accountName} to identify the user.<br>
	 * It creates and sends a new {@code RequestCharacters} packet.
	 * @param accountName The name of the account to request characters for.
	 */
	public void requestCharacters(String accountName)
	{
		sendPacket(new RequestCharacters(accountName));
	}
	
	/**
	 * Sends a password change result to the game server.<br>
	 * This method creates and sends a {@code ChangePasswordResponse} packet.
	 * @param characterName The name of the character attempting to change the password.
	 * @param responseMessage The message to display to the user regarding the result.
	 */
	public void changePasswordResponse(String characterName, String responseMessage)
	{
		sendPacket(new ChangePasswordResponse(characterName, responseMessage));
	}
	
	/**
	 * Updates the list of game server host addresses.<br>
	 * This method clears existing addresses and processes new pairs from the provided array.<br>
	 * Each pair consists of a hostname followed by a subnet.
	 * @param hosts An array of strings containing host and subnet pairs.
	 */
	public void setGameHosts(String[] hosts)
	{
		final int serverId = getServerId();
		LOGGER.info("Updated Gameserver [" + serverId + "] " + GameServerTable.getInstance().getServerNameById(serverId) + " IP's:");
		
		// Clear existing server addresses.
		_gameServerInfo.clearServerAddresses();
		
		// Process host pairs (host, subnet).
		for (int i = 0; i < hosts.length; i += 2)
		{
			try
			{
				_gameServerInfo.addServerAddress(hosts[i], hosts[i + 1]);
			}
			catch (Exception e)
			{
				LOGGER.warning("Failed to resolve hostname \"" + hosts[i] + "\" - " + e.getMessage());
			}
		}
		
		// Log all configured server addresses.
		for (String serverAddress : _gameServerInfo.getServerAddresses())
		{
			LOGGER.info(serverAddress);
		}
	}
	
	/**
	 * Checks if the game server is currently authenticated.<br>
	 * This method returns {@code true} only if the game server information exists and is authenticated.
	 * @return {@code true} if the server is authenticated, {@code false} otherwise.
	 */
	public boolean isAuthed()
	{
		if (_gameServerInfo == null)
		{
			return false;
		}
		
		return _gameServerInfo.isAuthed();
	}
	
	/**
	 * Updates the internal game server information.<br>
	 * This method assigns a new {@code GameServerInfo} object to the current instance.
	 * @param gameServerInfo The {@code GameServerInfo} data to set.
	 */
	public void setGameServerInfo(GameServerInfo gameServerInfo)
	{
		_gameServerInfo = gameServerInfo;
	}
	
	/**
	 * Retrieves the current information for this game server.<br>
	 * This method returns the {@code GameServerInfo} object associated with this instance.
	 * @return The {@code GameServerInfo} of the current game server.
	 */
	public GameServerInfo getGameServerInfo()
	{
		return _gameServerInfo;
	}
	
	/**
	 * Retrieves the IP address of the current connection.
	 * @return The {@code String} representing the connection IP address.
	 */
	public String getConnectionIpAddress()
	{
		return _connectionIpAddress;
	}
	
	/**
	 * Retrieves the unique identifier for this game server.<br>
	 * It returns the ID from {@code GameServerInfo}.<br>
	 * If no information is available, it returns -1.
	 * @return The integer ID of the server or -1 if not set.
	 */
	public int getServerId()
	{
		if (_gameServerInfo != null)
		{
			return _gameServerInfo.getId();
		}
		
		return -1;
	}
	
	/**
	 * Retrieves the private RSA key.<br>
	 * This key is used for secure communication.
	 * @return the {@code RSAPrivateKey} object.
	 */
	public RSAPrivateKey getPrivateKey()
	{
		return _privateKey;
	}
	
	/**
	 * Sets the {@code NewCrypt} instance used for BlowFish encryption.<br>
	 * This method updates the internal cipher object.
	 * @param blowfishCipher The {@code NewCrypt} object to use for encryption.
	 */
	public void setBlowFish(NewCrypt blowfishCipher)
	{
		_blowfishCipher = blowfishCipher;
	}
	
	/**
	 * Adds a specific account to the list of active accounts on this game server.<br>
	 * This method also removes any existing authenticated login clients for that account.
	 * @param accountName The name of the account to add.
	 */
	public void addAccountOnGameServer(String accountName)
	{
		_accountsOnGameServer.add(accountName);
		LoginController.getInstance().removeAuthedLoginClient(accountName);
	}
	
	/**
	 * Removes an account from the current game server.<br>
	 * This method updates the internal tracking of active accounts.<br>
	 * It also notifies the {@code LoginController} to remove the authenticated client.
	 * @param accountName The name of the account to remove.
	 */
	public void removeAccountOnGameServer(String accountName)
	{
		_accountsOnGameServer.remove(accountName);
		LoginController.getInstance().removeAuthedLoginClient(accountName);
	}
	
	/**
	 * Retrieves the current connection state of the game server.<br>
	 * This is used to check if the login server is connected properly.
	 * @return The current {@code GameServerState}.
	 */
	public GameServerState getLoginConnectionState()
	{
		return _loginConnectionState;
	}
	
	/**
	 * Updates the current connection status of the game server.<br>
	 * This method sets the internal {@code _loginConnectionState} variable.
	 * @param state The new {@code GameServerState} to apply.
	 */
	public void setLoginConnectionState(GameServerState state)
	{
		_loginConnectionState = state;
	}
}
