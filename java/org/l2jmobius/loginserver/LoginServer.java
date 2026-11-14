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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetSocketAddress;
import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.DatabaseConfig;
import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.database.DatabaseBackup;
import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.network.ConnectionManager;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.loginserver.config.LoginConfig;
import org.l2jmobius.loginserver.controller.LoginController;
import org.l2jmobius.loginserver.data.GameServerTable;
import org.l2jmobius.loginserver.network.LoginClient;
import org.l2jmobius.loginserver.network.LoginPacketHandler;
import org.l2jmobius.loginserver.network.gameserverpackets.receive.ServerStatus;
import org.l2jmobius.loginserver.ui.Gui;
import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.bridge.RuntimeManagementAgent;
import org.l2jmobius.tools.dashboard.bridge.RuntimeManagementBean;

/**
 * Bootstraps and drives the {@code LoginServer} lifecycle.<br>
 * Coordinates configuration loading, core service initialization, and network listener startup. This class initializes logging, database connections, thread pools, and registers IP bans.
 * @author BazookaRpm
 */
public class LoginServer
{
	// Logger.
	public static final Logger LOGGER = Logger.getLogger(LoginServer.class.getName());
	
	// Configuration paths.
	private static final String LOG_DIRECTORY_NAME = "log";
	private static final String LOG_CONFIGURATION_PATH = "./log.cfg";
	private static final String BANNED_IPS_CONFIGURATION_PATH = "./banned_ip.cfg";
	
	// Time constants.
	private static final int MILLISECONDS_PER_HOUR = 3600000;
	
	// Process exit codes.
	private static final int EXIT_CODE_NORMAL_SHUTDOWN = 0;
	private static final int EXIT_CODE_RESTART_REQUEST = 2;
	private static final int EXIT_CODE_FATAL_ERROR = 1;
	
	// Protocol.
	public static final int PROTOCOL_REV = 0x0106;
	
	// Singleton instance.
	private static LoginServer _instance;
	
	// Network listeners.
	private GameServerListener _gameServerListener;
	private ScheduledFuture<?> _dashboardLifecycleTask;
	
	// Login server status.
	private static volatile int _loginStatus = ServerStatus.STATUS_NORMAL;
	
	/**
	 * Private constructor for the {@code LoginServer} class.<br>
	 * It initializes all core components and services during startup.
	 */
	private LoginServer()
	{
		initializeInterfaceLayer();
		initializeLoggingLayer();
		initializeCoreServices();
		initializeSecurityAndBans();
		configureScheduledRestart();
		initializeNetworkListeners();
		RuntimeManagementAgent.publish(new RuntimeManagementBean("LoginServer", () -> "Login Server", () -> -1, () -> -1, GameServerListener::getGameServerCount, () -> LoginController.getInstance().getAuthenticatedClientCount(), seconds -> scheduleDashboardLifecycleAction(seconds, false), seconds -> scheduleDashboardLifecycleAction(seconds, true), this::cancelDashboardLifecycleAction), LOGGER);
	}
	
	/**
	 * Sets up the graphical user interface for the login server.<br>
	 * It loads the {@code InterfaceConfig} and starts a new {@code Gui} instance if enabled.
	 */
	private void initializeInterfaceLayer()
	{
		InterfaceConfig.load();
		
		if (RuntimeManagementAgent.isManagedProcess())
		{
			System.out.println("LoginServer: Running under " + DashboardProfile.MANAGEMENT_LABEL + " management.");
			return;
		}
		
		if (!InterfaceConfig.ENABLE_GUI)
		{
			return;
		}
		
		System.out.println("LoginServer: Running in GUI mode.");
		new Gui();
	}
	
	/**
	 * Sets up the server logging system.<br>
	 * Creates the log directory if it does not exist.<br>
	 * Loads the configuration from {@code LOG_CONFIGURATION_PATH}.<br>
	 * Falls back to default settings if the file is missing or invalid.
	 */
	private void initializeLoggingLayer()
	{
		final File logDirectory = new File(".", LOG_DIRECTORY_NAME);
		if (!logDirectory.exists() && !logDirectory.mkdir())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Unable to create log directory at " + logDirectory.getAbsolutePath() + ".");
		}
		
		final File logConfigurationFile = new File(LOG_CONFIGURATION_PATH);
		if (!logConfigurationFile.exists())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Logging configuration file is missing at " + logConfigurationFile.getAbsolutePath() + ". Using default logging settings.");
			return;
		}
		
		if (logConfigurationFile.isDirectory())
		{
			LOGGER.warning(getClass().getSimpleName() + ": Logging configuration path points to a directory (" + logConfigurationFile.getAbsolutePath() + "). Using default logging settings.");
			return;
		}
		
		try (InputStream logConfigurationInputStream = new FileInputStream(logConfigurationFile))
		{
			LogManager.getLogManager().readConfiguration(logConfigurationInputStream);
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed to apply logging configuration from " + logConfigurationFile.getAbsolutePath() + ". Reason: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Sets up the essential services required for the server to run.<br>
	 * This method loads configurations and initializes core components like databases and thread pools.
	 */
	private void initializeCoreServices()
	{
		LoginConfig.load();
		
		DatabaseFactory.init();
		
		ThreadPool.init();
		
		try
		{
			LoginController.load();
		}
		catch (GeneralSecurityException e)
		{
			LOGGER.log(Level.SEVERE, "FATAL: LoginController initialization failed due to security configuration error. Reason: " + e.getMessage(), e);
			System.exit(EXIT_CODE_FATAL_ERROR);
		}
		
		GameServerTable.getInstance();
	}
	
	/**
	 * Loads the ban files and cleans up expired security restrictions.<br>
	 * This method removes old IP bans and temporary account bans from the database.<br>
	 * It ensures that only active security records remain after startup.
	 */
	private void initializeSecurityAndBans()
	{
		loadBanFile();
		
		// Cleanup expired IP bans and ban_temp records on startup.
		final long startupTime = System.currentTimeMillis();
		LoginController.getInstance().getBannedIps().entrySet().removeIf(entry ->
		{
			final long value = entry.getValue();
			if ((value > 0) && (value < startupTime))
			{
				LOGGER.info("Removed expired IP ban: " + entry.getKey());
				return true;
			}
			return false;
		});
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM account_data WHERE var='ban_temp' AND value <= ?"))
		{
			ps.setString(1, Long.toString(startupTime));
			final int deleted = ps.executeUpdate();
			if (deleted > 0)
			{
				LOGGER.info("Removed " + deleted + " expired ban_temp record(s) from account_data.");
			}
		}
		catch (Exception e)
		{
			LOGGER.warning("Could not purge expired ban_temp records: " + e.getMessage());
		}
	}
	
	/**
	 * Configures the automatic restart of the login server.<br>
	 * This method checks if scheduled restarts are enabled in {@code LoginConfig}.<br>
	 * It calculates the delay based on the configured hours and schedules a shutdown task.
	 */
	private void configureScheduledRestart()
	{
		if (!LoginConfig.LOGIN_SERVER_SCHEDULE_RESTART)
		{
			return;
		}
		
		final long restartDelayHours = LoginConfig.LOGIN_SERVER_SCHEDULE_RESTART_TIME;
		final long restartDelayMillis = restartDelayHours * MILLISECONDS_PER_HOUR;
		if (restartDelayMillis <= 0)
		{
			LOGGER.warning("Login server restart scheduling is enabled but the computed delay is invalid. Configured hours: " + restartDelayHours + ".");
			return;
		}
		
		LOGGER.info("Login server restart scheduled in " + restartDelayHours + " hour(s). Computed delay: " + restartDelayMillis + " ms.");
		ThreadPool.schedule(() -> shutdown(true), restartDelayMillis);
	}
	
	/**
	 * Sets up the network listeners for the server.<br>
	 * This method starts both the game server and login client listeners.
	 */
	private void initializeNetworkListeners()
	{
		startGameServerListener();
		startLoginClientListener();
	}
	
	/**
	 * Starts the game server listener.<br>
	 * This method creates a new {@code GameServerListener} instance and begins its execution.<br>
	 * It logs a success message if the listener starts correctly.<br>
	 * If an {@code IOException} occurs, it logs a fatal error and shuts down the application.
	 */
	private void startGameServerListener()
	{
		try
		{
			_gameServerListener = new GameServerListener();
			_gameServerListener.start();
			
			LOGGER.info("Game server listener is listening on " + LoginConfig.GAME_SERVER_LOGIN_HOST + ":" + LoginConfig.GAME_SERVER_LOGIN_PORT + ".");
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "FATAL: Game server listener could not be started on " + LoginConfig.GAME_SERVER_LOGIN_HOST + ":" + LoginConfig.GAME_SERVER_LOGIN_PORT + ". Reason: " + e.getMessage(), e);
			System.exit(EXIT_CODE_FATAL_ERROR);
		}
	}
	
	/**
	 * Starts the network listener for login clients.<br>
	 * This method initializes the {@code ConnectionManager} using settings from {@code LoginConfig}.<br>
	 * It handles potential {@code IOException} by logging a fatal error and exiting the application.
	 */
	private void startLoginClientListener()
	{
		try
		{
			new ConnectionManager<>(new InetSocketAddress(LoginConfig.LOGIN_BIND_ADDRESS, LoginConfig.PORT_LOGIN), LoginClient::new, new LoginPacketHandler());
			LOGGER.info(getClass().getSimpleName() + ": Login client listener started on " + LoginConfig.LOGIN_BIND_ADDRESS + ":" + LoginConfig.PORT_LOGIN + ".");
		}
		catch (IOException e)
		{
			LOGGER.log(Level.SEVERE, "FATAL: Login client listener could not be started on " + LoginConfig.LOGIN_BIND_ADDRESS + ":" + LoginConfig.PORT_LOGIN + ". Reason: " + e.getMessage(), e);
			System.exit(EXIT_CODE_FATAL_ERROR);
		}
	}
	
	/**
	 * Retrieves the current {@code GameServerListener}.<br>
	 * This listener handles communication with the game server.
	 * @return The active {@code GameServerListener} instance.
	 */
	public GameServerListener getGameServerListener()
	{
		return _gameServerListener;
	}
	
	/**
	 * Loads the list of banned IP addresses from a configuration file.<br>
	 * This method reads each line from the specified path and processes it.<br>
	 * If the file is missing or unreadable, it logs a warning and stops.
	 */
	public void loadBanFile()
	{
		final File bannedIpConfigurationFile = new File(BANNED_IPS_CONFIGURATION_PATH);
		if (!bannedIpConfigurationFile.exists() || !bannedIpConfigurationFile.isFile())
		{
			LOGGER.warning("IP bans configuration file (" + bannedIpConfigurationFile.getAbsolutePath() + ") does not exist or is not a regular file. No IP bans have been loaded.");
			return;
		}
		
		try (FileInputStream bannedIpFileInputStream = new FileInputStream(bannedIpConfigurationFile);
			InputStreamReader bannedIpReader = new InputStreamReader(bannedIpFileInputStream);
			LineNumberReader bannedIpLineReader = new LineNumberReader(bannedIpReader))
		{
			String rawLine;
			while ((rawLine = bannedIpLineReader.readLine()) != null)
			{
				processBanLine(bannedIpConfigurationFile, bannedIpLineReader, rawLine);
			}
		}
		catch (IOException e)
		{
			LOGGER.log(Level.WARNING, "Error while reading IP bans configuration file (" + bannedIpConfigurationFile.getAbsolutePath() + "). Details: " + e.getMessage(), e);
		}
		
		LOGGER.info("Loaded " + LoginController.getInstance().getBannedIps().size() + " IP Bans.");
	}
	
	/**
	 * Parses a single line from the ban file to register an IP address.<br>
	 * It validates the format and extracts the IP and duration.<br>
	 * If valid, it adds the ban via {@code LoginController}.
	 * @param sourceFile The {@code File} object representing the ban list.
	 * @param lineReader The {@code LineNumberReader} used to track the current line number.
	 * @param rawLine The {@code String} content of the current line being processed.
	 */
	private void processBanLine(File sourceFile, LineNumberReader lineReader, String rawLine)
	{
		if (rawLine == null)
		{
			return;
		}
		
		final String trimmedLine = rawLine.trim();
		if (trimmedLine.isEmpty())
		{
			return;
		}
		
		if (trimmedLine.charAt(0) == '#')
		{
			return;
		}
		
		final int commentIndex = trimmedLine.indexOf('#');
		final String definitionPart = (commentIndex >= 0) ? trimmedLine.substring(0, commentIndex).trim() : trimmedLine;
		if (definitionPart.isEmpty())
		{
			LOGGER.warning("Skipped IP ban entry with empty definition in file (" + sourceFile.getAbsolutePath() + ") at line " + lineReader.getLineNumber() + ".");
			return;
		}
		
		final String[] tokens = definitionPart.split("\\s+");
		if (tokens.length == 0)
		{
			LOGGER.warning("Skipped IP ban entry with no address token in file (" + sourceFile.getAbsolutePath() + ") at line " + lineReader.getLineNumber() + ".");
			return;
		}
		
		final String ipAddress = tokens[0];
		long durationMillis = 0;
		
		if (tokens.length > 1)
		{
			final String durationToken = tokens[1];
			try
			{
				durationMillis = Long.parseLong(durationToken);
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("Skipped IP ban entry due to invalid duration token '" + durationToken + "' in file (" + sourceFile.getAbsolutePath() + ") at line " + lineReader.getLineNumber() + ".");
				return;
			}
		}
		
		try
		{
			LoginController.getInstance().addBanForAddress(ipAddress, durationMillis);
		}
		catch (Exception e)
		{
			LOGGER.warning("Skipped IP ban registration for address '" + ipAddress + "' from file (" + sourceFile.getAbsolutePath() + ") at line " + lineReader.getLineNumber() + ". Reason: " + e.getMessage() + ".");
		}
	}
	
	private synchronized void scheduleDashboardLifecycleAction(int seconds, boolean restart)
	{
		cancelDashboardLifecycleAction();
		if (seconds <= 0)
		{
			ThreadPool.execute(() -> shutdown(restart));
			return;
		}
		
		LOGGER.warning("Dashboard requested Login Server " + (restart ? "restart" : "shutdown") + " in " + seconds + " second(s).");
		_dashboardLifecycleTask = ThreadPool.schedule(() -> shutdown(restart), TimeUnit.SECONDS.toMillis(seconds));
	}
	
	private synchronized boolean cancelDashboardLifecycleAction()
	{
		final ScheduledFuture<?> task = _dashboardLifecycleTask;
		if (task == null)
		{
			return false;
		}
		
		_dashboardLifecycleTask = null;
		if (!task.cancel(false))
		{
			return false;
		}
		
		LOGGER.info("Dashboard Login Server lifecycle request cancelled.");
		return true;
	}
	
	/**
	 * Shuts down the server and performs a database backup.<br>
	 * It then exits the application based on the restart flag.
	 * @param restart Set to {@code true} to request a restart, or {@code false} for a normal shutdown.
	 */
	public void shutdown(boolean restart)
	{
		if (DatabaseConfig.BACKUP_DATABASE)
		{
			DatabaseBackup.performBackup("login");
		}
		
		Runtime.getRuntime().exit(restart ? EXIT_CODE_RESTART_REQUEST : EXIT_CODE_NORMAL_SHUTDOWN);
	}
	
	/**
	 * Retrieves the current operational status of the server.<br>
	 * This value represents the internal state of the {@code LoginServer}.
	 * @return The current status as an {@code int}.
	 */
	public int getStatus()
	{
		return _loginStatus;
	}
	
	/**
	 * Updates the current status of the server.<br>
	 * This method sets the internal {@code _loginStatus} value.
	 * @param status The new status code to assign to the server.
	 */
	public void setStatus(int status)
	{
		_loginStatus = status;
	}
	
	/**
	 * Retrieves the singleton instance of the {@code LoginServer}.<br>
	 * This method provides a global access point to the server core.
	 * @return The active {@code LoginServer} instance.
	 */
	public static LoginServer getInstance()
	{
		return _instance;
	}
	
	/**
	 * The main entry point for the {@code LoginServer} application.<br>
	 * This method initializes and starts the login server instance.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		_instance = new LoginServer();
	}
}
