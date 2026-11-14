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
package org.l2jmobius.commons.database;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.l2jmobius.commons.config.DatabaseConfig;

/**
 * This class manages the database connection pool using {@code HikariCP}.<br>
 * The configuration is optimized for high-load environments.
 * @author Mobius
 */
public class DatabaseFactory
{
	private static final Logger LOGGER = Logger.getLogger(DatabaseFactory.class.getName());
	
	private static volatile HikariDataSource DATABASE_POOL;
	
	/**
	 * Private constructor to prevent instantiation.
	 */
	private DatabaseFactory()
	{
	}
	
	/**
	 * Initializes the database connection pool.<br>
	 * This method loads configurations and sets up the {@code HikariDataSource}.<br>
	 * It ensures that the pool is only created if it is currently {@code null} or closed.
	 */
	public static synchronized void init()
	{
		if ((DATABASE_POOL != null) && !DATABASE_POOL.isClosed())
		{
			LOGGER.warning("Database: Connection pool is already initialized.");
			return;
		}
		
		// Load configurations.
		DatabaseConfig.load();
		
		try
		{
			final HikariConfig config = new HikariConfig();
			config.setDriverClassName(DatabaseConfig.DATABASE_DRIVER);
			config.setJdbcUrl(DatabaseConfig.DATABASE_URL);
			config.setUsername(DatabaseConfig.DATABASE_LOGIN);
			config.setPassword(DatabaseConfig.DATABASE_PASSWORD);
			
			// Pool size configuration.
			config.setMaximumPoolSize(determineMaxPoolSize(DatabaseConfig.DATABASE_MAX_CONNECTIONS));
			config.setMinimumIdle(determineMinimumIdle(DatabaseConfig.DATABASE_MAX_CONNECTIONS));
			
			// Connection lifetime settings.
			config.setConnectionTimeout(60000); // Wait up to 1 minute for a free connection.
			config.setIdleTimeout(600000); // Retire connections idle for 10 minutes.
			config.setMaxLifetime(1800000); // Recycle connections after 30 minutes.
			config.setKeepaliveTime(120000); // Ping idle connections every 2 minutes to survive wait_timeout and firewall cuts.
			config.setValidationTimeout(5000); // Fail validation pings after 5 seconds.
			
			// Leak detection.
			config.setLeakDetectionThreshold(600000); // Warn when a connection is held for over 10 minutes.
			
			// Start even when the database is down and reconnect automatically.
			config.setInitializationFailTimeout(-1);
			
			// Pool name for identification.
			config.setPoolName("L2JMobiusPool");
			
			// Register MBeans for monitoring.
			config.setRegisterMbeans(true);
			
			// MySQL driver optimizations for high query throughput.
			config.addDataSourceProperty("cachePrepStmts", "true"); // Cache prepared statements on the client.
			config.addDataSourceProperty("prepStmtCacheSize", "500"); // Cache up to 500 statements per connection.
			config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048"); // Cache statements up to 2048 characters long.
			config.addDataSourceProperty("useServerPrepStmts", "true"); // Use server side prepared statements.
			config.addDataSourceProperty("rewriteBatchedStatements", "true"); // Collapse batched writes into multi-value statements.
			config.addDataSourceProperty("cacheServerConfiguration", "true"); // Cache server variables per connection.
			config.addDataSourceProperty("useLocalSessionState", "true"); // Trust local session state to avoid round trips.
			config.addDataSourceProperty("elideSetAutoCommits", "true"); // Skip redundant autocommit calls.
			config.addDataSourceProperty("maintainTimeStats", "false"); // Disable driver time statistics gathering.
			
			// Initialize HikariDataSource.
			DATABASE_POOL = new HikariDataSource(config);
			
			LOGGER.info("Database: HikariCP pool initialized successfully.");
			
			// Check server variables, cap the pool size to the server connection limit and warn for known low defaults. Failures are logged and ignored.
			try (Connection connection = DATABASE_POOL.getConnection();
				Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("SHOW VARIABLES WHERE Variable_name IN ('max_connections', 'max_user_connections', 'max_allowed_packet', 'innodb_buffer_pool_size', 'innodb_log_file_size', 'innodb_redo_log_capacity', 'innodb_flush_log_at_trx_commit', 'wait_timeout', 'default_storage_engine')"))
			{
				long logFileSize = -1;
				long redoLogCapacity = -1;
				while (result.next())
				{
					final String name = result.getString(1);
					final String value = result.getString(2);
					switch (name)
					{
						case "max_connections":
						{
							// Leave headroom for external tools such as phpMyAdmin and backups.
							final int usableConnections = (int) Long.parseLong(value) - 10;
							if (DATABASE_POOL.getMaximumPoolSize() > usableConnections)
							{
								final int newMaxPoolSize = Math.max(usableConnections, 4);
								LOGGER.warning("Database: Server max_connections (" + value + ") is lower than the configured pool size. Capping pool to " + newMaxPoolSize + ". Increase max_connections in my.ini for a larger pool.");
								DATABASE_POOL.setMaximumPoolSize(newMaxPoolSize);
								DATABASE_POOL.setMinimumIdle(determineMinimumIdle(newMaxPoolSize));
							}
							break;
						}
						case "max_user_connections":
						{
							// Zero means unlimited. Leave headroom for other processes using the same account.
							final int userLimit = (int) Long.parseLong(value);
							final int usableUserConnections = userLimit - 10;
							if ((userLimit > 0) && (DATABASE_POOL.getMaximumPoolSize() > usableUserConnections))
							{
								final int newMaxPoolSize = Math.max(usableUserConnections, 4);
								LOGGER.warning("Database: Server max_user_connections (" + value + ") is lower than the configured pool size. Capping pool to " + newMaxPoolSize + ". Increase max_user_connections in my.ini for a larger pool.");
								DATABASE_POOL.setMaximumPoolSize(newMaxPoolSize);
								DATABASE_POOL.setMinimumIdle(determineMinimumIdle(newMaxPoolSize));
							}
							break;
						}
						case "max_allowed_packet":
						{
							final long packetSize = Long.parseLong(value);
							if (packetSize < 16777216) // 16MB.
							{
								LOGGER.warning("Database: max_allowed_packet is " + (packetSize / 1048576) + "MB. Set max_allowed_packet=128M in my.ini for batched writes under load.");
							}
							break;
						}
						case "innodb_buffer_pool_size":
						{
							final long bufferPoolSize = Long.parseLong(value);
							if (bufferPoolSize < 1073741824) // 1GB.
							{
								LOGGER.warning("Database: innodb_buffer_pool_size is " + (bufferPoolSize / 1048576) + "MB. Set innodb_buffer_pool_size=2G or higher in my.ini for high population servers.");
							}
							break;
						}
						case "innodb_log_file_size":
						{
							logFileSize = Long.parseLong(value);
							break;
						}
						case "innodb_redo_log_capacity":
						{
							redoLogCapacity = Long.parseLong(value);
							break;
						}
						case "innodb_flush_log_at_trx_commit":
						{
							if (Long.parseLong(value) == 1)
							{
								LOGGER.warning("Database: innodb_flush_log_at_trx_commit is 1. Set innodb_flush_log_at_trx_commit=2 in my.ini for faster commits, at the cost of up to 1 second of writes on operating system crash.");
							}
							break;
						}
						case "wait_timeout":
						{
							if (Long.parseLong(value) < 300) // 5 minutes.
							{
								LOGGER.warning("Database: wait_timeout is " + value + " seconds. Set wait_timeout=28800 in my.ini so idle connections outlive keepalive pings.");
							}
							break;
						}
						case "default_storage_engine":
						{
							if (!"InnoDB".equalsIgnoreCase(value))
							{
								LOGGER.warning("Database: Default storage engine is " + value + ". Set default-storage-engine=InnoDB in my.ini.");
							}
							break;
						}
					}
				}
				
				// Redo log sizing. MySQL 8.0.30 and later use innodb_redo_log_capacity and ignore innodb_log_file_size. Older MySQL and MariaDB use innodb_log_file_size. Prefer the modern variable when the server reports it.
				final long redoLogSize = (redoLogCapacity >= 0) ? redoLogCapacity : logFileSize;
				if ((redoLogSize >= 0) && (redoLogSize < 268435456)) // 256MB.
				{
					final String variableName = (redoLogCapacity >= 0) ? "innodb_redo_log_capacity" : "innodb_log_file_size";
					LOGGER.warning("Database: " + variableName + " is " + (redoLogSize / 1048576) + "MB. Set " + variableName + "=256M in my.ini to avoid checkpoint stalls under heavy writes.");
				}
			}
			catch (SQLException | NumberFormatException e)
			{
				LOGGER.log(Level.WARNING, "Database: Could not verify server configuration.", e);
			}
			
			// Warn when MyISAM tables remain. InnoDB row level locking is required for concurrent access.
			try (Connection connection = DATABASE_POOL.getConnection();
				Statement statement = connection.createStatement();
				ResultSet result = statement.executeQuery("SELECT table_name FROM information_schema.tables WHERE table_schema = DATABASE() AND ENGINE = 'MyISAM' AND TABLE_TYPE = 'BASE TABLE' ORDER BY table_name"))
			{
				final List<String> myIsamTables = new ArrayList<>();
				while (result.next())
				{
					myIsamTables.add(result.getString(1));
				}
				
				if (!myIsamTables.isEmpty())
				{
					LOGGER.warning("Database: " + myIsamTables.size() + " tables still use MyISAM (" + String.join(", ", myIsamTables) + "). Convert them to InnoDB for row level locking and crash recovery.");
				}
			}
			catch (SQLException e)
			{
				LOGGER.log(Level.WARNING, "Database: Could not check table engines.", e);
			}
			
			if (DatabaseConfig.DATABASE_TEST_CONNECTIONS)
			{
				// Test that the pool can open every connection it is allowed to create.
				final List<Connection> connections = new ArrayList<>();
				int successfulConnections = 0;
				try
				{
					LOGGER.info("Database: Testing database connections...");
					
					final int maxPoolSize = DATABASE_POOL.getMaximumPoolSize();
					for (int i = 0; i < maxPoolSize; i++)
					{
						try
						{
							connections.add(DATABASE_POOL.getConnection());
							successfulConnections++;
						}
						catch (SQLException e)
						{
							LOGGER.log(Level.SEVERE, "Database: Failed to open connection " + (i + 1) + "!", e);
							break;
						}
					}
					
					if (successfulConnections == maxPoolSize)
					{
						LOGGER.info("Database: Initialized with a total of " + successfulConnections + " connections.");
					}
					else
					{
						LOGGER.warning("Database: Only " + successfulConnections + " out of " + maxPoolSize + " connections were successful. Adjusting pool size.");
						
						// Calculate new pool size, reducing in steps to find a stable number.
						int newConnectionCount = successfulConnections;
						if (successfulConnections > 100)
						{
							newConnectionCount = (successfulConnections / 100) * 100;
						}
						else if (successfulConnections > 50)
						{
							newConnectionCount = (successfulConnections / 50) * 50;
						}
						
						// Ensure a minimum pool size of 20.
						newConnectionCount = Math.max(newConnectionCount, 20);
						
						// Update pool configuration.
						try
						{
							DATABASE_POOL.setMaximumPoolSize(newConnectionCount);
							DATABASE_POOL.setMinimumIdle(determineMinimumIdle(newConnectionCount));
							LOGGER.info("Database: Reinitialized pool size to " + newConnectionCount + ".");
						}
						catch (Exception e)
						{
							LOGGER.log(Level.SEVERE, "Database: Failed to adjust pool size.", e);
						}
					}
				}
				finally // Close all opened connections.
				{
					for (Connection connection : connections)
					{
						try
						{
							connection.close();
						}
						catch (SQLException e)
						{
							LOGGER.log(Level.SEVERE, "Database: Error closing connection.", e);
						}
					}
				}
			}
			else
			{
				// Test that a single valid connection can be established.
				try (Connection connection = DATABASE_POOL.getConnection())
				{
					if (connection.isValid(5))
					{
						LOGGER.info("Database: Initialized with a valid connection.");
					}
					else
					{
						LOGGER.warning("Database: Connection is not valid.");
					}
				}
				catch (SQLException e)
				{
					LOGGER.log(Level.SEVERE, "Database: Problem initializing connection pool.", e);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Database: Failed to initialize HikariCP pool.", e);
		}
	}
	
	/**
	 * Clamps the configured maximum pool size between {@code 4} and {@code 1000}.
	 * @param configuredMax The maximum pool size provided in the configuration.
	 * @return The adjusted maximum pool size.
	 */
	private static int determineMaxPoolSize(int configuredMax)
	{
		return Math.clamp(configuredMax, 4, 1000);
	}
	
	/**
	 * Calculates the minimum number of idle connections, a tenth of the maximum pool size and at least {@code 2}.
	 * @param configuredMax The maximum number of connections allowed.
	 * @return The minimum idle connection count.
	 */
	private static int determineMinimumIdle(int configuredMax)
	{
		return Math.max(determineMaxPoolSize(configuredMax) / 10, 2);
	}
	
	/**
	 * Retrieves a connection from the database pool.<br>
	 * Wraps any {@code SQLException} in a {@code RuntimeException}.
	 * @return A {@code Connection} object to the database.
	 */
	public static Connection getConnection()
	{
		final HikariDataSource pool = DATABASE_POOL;
		if (pool == null)
		{
			throw new IllegalStateException("Database: Connection pool is not initialized.");
		}
		
		try
		{
			return pool.getConnection();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Database: Could not get a connection.", e);
			throw new RuntimeException("Unable to obtain a database connection.", e);
		}
	}
	
	/**
	 * Closes the database connection pool.<br>
	 * This method checks if the pool is active before attempting to shut it down.
	 */
	public static synchronized void close()
	{
		if ((DATABASE_POOL != null) && !DATABASE_POOL.isClosed())
		{
			try
			{
				DATABASE_POOL.close();
				LOGGER.info("Database: HikariCP pool closed successfully.");
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "Database: There was a problem closing the data source.", e);
			}
		}
		
		DATABASE_POOL = null;
	}
}
