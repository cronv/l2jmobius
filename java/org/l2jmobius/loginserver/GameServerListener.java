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
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.loginserver;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.loginserver.config.LoginConfig;
import org.l2jmobius.loginserver.util.FloodProtectorListener;

/**
 * Listens for incoming game server connections on the configured login port.<br>
 * Delegates each accepted connection to a dedicated {@code GameServerThread} and tracks active handlers.<br>
 * Uses flood protection to mitigate abusive connection attempts.
 * @author BazookaRpm
 */
public class GameServerListener extends FloodProtectorListener
{
	private static final Logger LOGGER = Logger.getLogger(GameServerListener.class.getName());
	
	// Active game server handler threads.
	private static final Collection<GameServerThread> _gameServerThreads = ConcurrentHashMap.newKeySet();
	
	/**
	 * Initializes a new instance of the {@code GameServerListener}.<br>
	 * This constructor binds to the host and port defined in {@code LoginConfig}.<br>
	 * It sets the listener name based on the current configuration.
	 * @throws IOException If an error occurs while binding to the network socket.
	 */
	public GameServerListener() throws IOException
	{
		super(LoginConfig.GAME_SERVER_LOGIN_HOST, LoginConfig.GAME_SERVER_LOGIN_PORT);
		setName(getClass().getSimpleName() + "-" + LoginConfig.GAME_SERVER_LOGIN_HOST + ":" + LoginConfig.GAME_SERVER_LOGIN_PORT);
		
		// if (LOGGER.isLoggable(Level.INFO))
		// {
		// LOGGER.info(getClass().getSimpleName() + ": Listening for game servers on " + LoginConfig.GAME_SERVER_LOGIN_HOST + ":" + LoginConfig.GAME_SERVER_LOGIN_PORT + ".");
		// }
	}
	
	/**
	 * Registers a new game server connection.<br>
	 * This method creates a {@code GameServerThread} for the provided {@code Socket}.<br>
	 * It adds the new thread to the active collection and logs the registration.<br>
	 * If the socket is invalid or an error occurs, it handles the failure gracefully.
	 * @param socket The {@code Socket} representing the new game server connection.
	 */
	@Override
	public void addClient(Socket socket)
	{
		if ((socket == null) || socket.isClosed())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Attempted to add game server with null or closed socket.");
			return;
		}
		
		try
		{
			final GameServerThread gameServerThread = new GameServerThread(socket); // The constructor itself already does start().
			_gameServerThreads.add(gameServerThread);
			
			if (LOGGER.isLoggable(Level.FINE))
			{
				final String address = (socket.getInetAddress() != null) ? socket.getInetAddress().getHostAddress() : "unknown";
				LOGGER.fine(getClass().getSimpleName() + ": Registered new game server connection from " + address + ". Active game servers: " + _gameServerThreads.size() + ".");
			}
		}
		catch (Exception e)
		{
			// Ensure that we do not keep a broken connection or leak resources if handler creation fails.
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to register game server connection from " + socket.getRemoteSocketAddress() + ".", e);
			
			try
			{
				socket.close();
			}
			catch (IOException ioe)
			{
				LOGGER.log(Level.FINE, getClass().getSimpleName() + ": Failed to close socket after registration failure.", ioe);
			}
		}
	}
	
	/**
	 * Removes a specific game server from the active list.<br>
	 * This method updates the internal collection of threads.<br>
	 * It checks if the provided thread is {@code null} before attempting removal.
	 * @param gameServerThread The {@code GameServerThread} to be removed.
	 */
	public void removeGameServer(GameServerThread gameServerThread)
	{
		if (gameServerThread == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Attempted to remove null game server thread.");
			return;
		}
		
		final boolean removed = _gameServerThreads.remove(gameServerThread);
		
		if (LOGGER.isLoggable(Level.FINE))
		{
			LOGGER.fine(getClass().getSimpleName() + ": Removed game server thread " + gameServerThread + " (removed=" + removed + "). Active game servers: " + _gameServerThreads.size() + ".");
		}
	}
	
	/**
	 * Retrieves the total number of active game servers.<br>
	 * This method counts the entries in the internal thread collection.
	 * @return The current count of {@code GameServerThread} instances.
	 */
	public static int getGameServerCount()
	{
		return _gameServerThreads.size();
	}
	
	/**
	 * Retrieves all currently active game server threads.<br>
	 * This method returns the collection of {@code GameServerThread} instances managed by the listener.
	 * @return A {@code Collection} containing all active {@code GameServerThread} objects.
	 */
	public static Collection<GameServerThread> getGameServers()
	{
		return _gameServerThreads;
	}
}
