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
package org.l2jmobius.loginserver.util;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.loginserver.config.LoginConfig;

/**
 * Listens for incoming login connections and applies basic IP-based flood protection.<br>
 * This class maintains connection states per address and delegates accepted sockets to subclasses.<br>
 * It uses limits from {@code LoginConfig} to mitigate abusive connection patterns.
 * @author BazookaRpm
 */
public abstract class FloodProtectorListener extends Thread
{
	private static final Logger LOGGER = Logger.getLogger(FloodProtectorListener.class.getName());
	
	// Server socket backlog size.
	private static final int SERVER_SOCKET_BACKLOG = 50;
	
	// Flood protection state per remote IP.
	private final Map<String, ForeignConnection> _floodProtection = new ConcurrentHashMap<>();
	
	// Login server listening socket.
	private final ServerSocket _serverSocket;
	
	/**
	 * Initializes a new {@code FloodProtectorListener} instance.<br>
	 * This constructor binds the server to a specific network address and port.<br>
	 * It sets up the underlying {@code ServerSocket} for incoming connections.
	 * @param listenIp The IP address to bind to, or use {@code "*"} to listen on all interfaces.
	 * @param port The port number to open for listening.
	 * @throws IOException If an error occurs while creating the server socket.
	 */
	public FloodProtectorListener(String listenIp, int port) throws IOException
	{
		// If listening IP is "*", listen on all interfaces.
		if ("*".equals(listenIp))
		{
			_serverSocket = new ServerSocket(port);
		}
		else
		{
			// Listen on the specified IP with a fixed backlog size.
			_serverSocket = new ServerSocket(port, SERVER_SOCKET_BACKLOG, InetAddress.getByName(listenIp));
		}
	}
	
	@Override
	public void run()
	{
		Socket clientSocket = null;
		while (!isInterrupted())
		{
			try
			{
				// Accept incoming connections.
				clientSocket = _serverSocket.accept();
				
				if (LoginConfig.FLOOD_PROTECTION)
				{
					// Check flood protection for this remote address.
					final String clientAddress = clientSocket.getInetAddress().getHostAddress();
					ForeignConnection foreignConnection = _floodProtection.get(clientAddress);
					if (foreignConnection != null)
					{
						foreignConnection.connectionNumber += 1;
						
						final long currentTime = System.currentTimeMillis();
						if (((foreignConnection.connectionNumber > LoginConfig.FAST_CONNECTION_LIMIT) && ((currentTime - foreignConnection.lastConnection) < LoginConfig.NORMAL_CONNECTION_TIME)) || ((currentTime - foreignConnection.lastConnection) < LoginConfig.FAST_CONNECTION_TIME) || (foreignConnection.connectionNumber > LoginConfig.MAX_CONNECTION_PER_IP))
						{
							foreignConnection.lastConnection = currentTime;
							clientSocket.close();
							foreignConnection.connectionNumber -= 1;
							
							if (!foreignConnection.isFlooding)
							{
								LOGGER.warning("Potential flood from address " + clientAddress + ".");
							}
							
							foreignConnection.isFlooding = true;
							continue;
						}
						
						// If the connection was previously considered flooding but now passes the check.
						if (foreignConnection.isFlooding)
						{
							foreignConnection.isFlooding = false;
							LOGGER.info("Address " + clientAddress + " is no longer considered as flooding.");
						}
						
						foreignConnection.lastConnection = currentTime;
					}
					else
					{
						// Initialize flood protection state for the new remote address.
						foreignConnection = new ForeignConnection(System.currentTimeMillis());
						_floodProtection.put(clientAddress, foreignConnection);
					}
				}
				
				// Add client connection for further processing (implementation in subclasses).
				addClient(clientSocket);
			}
			catch (Exception e)
			{
				// Handle exceptions and potential thread interruption.
				if (isInterrupted())
				{
					// Close server socket and break the loop on thread interruption.
					try
					{
						_serverSocket.close();
					}
					catch (IOException io)
					{
						LOGGER.log(Level.INFO, "Error while closing server socket on interruption in " + getClass().getSimpleName() + ".", io);
					}
					break;
				}
				
				LOGGER.log(Level.WARNING, "Unexpected exception in " + getClass().getSimpleName() + " main loop.", e);
			}
		}
	}
	
	/**
	 * Represents flood protection state for a single remote IP address.
	 */
	protected static class ForeignConnection
	{
		public int connectionNumber;
		public long lastConnection;
		public boolean isFlooding = false;
		
		/**
		 * Creates a new foreign connection record with an initial timestamp.
		 * @param time The timestamp of the first connection.
		 */
		public ForeignConnection(long time)
		{
			// Initialize a new foreign connection.
			lastConnection = time;
			connectionNumber = 1;
		}
	}
	
	/**
	 * Adds a client connection for further processing in subclasses.
	 * @param socket The accepted client socket.
	 */
	public abstract void addClient(Socket socket);
	
	/**
	 * Removes or updates the flood protection status for a specific IP address.<br>
	 * This method checks if {@code LoginConfig.FLOOD_PROTECTION} is enabled first.<br>
	 * It decrements the connection count for the provided {@code ip}.<br>
	 * If the count reaches zero, it removes the entry from the internal map.
	 * @param ip The IP address to update or remove.
	 */
	public void removeFloodProtection(String ip)
	{
		// Only proceed if flood protection is enabled.
		if (!LoginConfig.FLOOD_PROTECTION)
		{
			return;
		}
		
		// Decrement connection count or remove if no more connections exist.
		final ForeignConnection foreignConnection = _floodProtection.get(ip);
		if (foreignConnection != null)
		{
			foreignConnection.connectionNumber -= 1;
			if (foreignConnection.connectionNumber == 0)
			{
				_floodProtection.remove(ip);
			}
		}
		else
		{
			LOGGER.warning("Attempted to remove flood protection for unknown IP " + ip + ".");
		}
	}
	
	/**
	 * Closes the server socket used for listening to connections.<br>
	 * This method handles any {@code IOException} that occurs during closing.<br>
	 * It logs a warning if an error happens while shutting down.
	 */
	public void close()
	{
		try
		{
			_serverSocket.close();
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": " + e.getMessage());
		}
	}
}
