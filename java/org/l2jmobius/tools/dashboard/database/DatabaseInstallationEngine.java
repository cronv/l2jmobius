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
package org.l2jmobius.tools.dashboard.database;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * Executes the official Interlude Login and Game SQL installation scripts independently from any user interface.<br>
 * Operator decisions, progress reporting, cancellation, and preventive backup creation are supplied by callbacks.
 * @author BazookaRpm
 */
public final class DatabaseInstallationEngine
{
	/** Defines how an existing target schema is handled. */
	public enum ExistingDatabaseAction
	{
		RECREATE,
		USE_EXISTING,
		CANCEL
	}
	
	/** Defines how installation proceeds after a SQL statement fails. */
	public enum ScriptErrorAction
	{
		CONTINUE,
		ABORT
	}
	
	/** Defines the final state of an installation request. */
	public enum ResultStatus
	{
		SUCCESS,
		COMPLETED_WITH_ERRORS,
		CANCELLED,
		ABORTED
	}
	
	/** Defines the severity of an installer progress event. */
	public enum ProgressLevel
	{
		INFO,
		SUCCESS,
		WARNING,
		ERROR
	}
	
	/**
	 * Immutable database connection settings used by the installation engine.
	 * @param driverClass optional JDBC driver class name
	 * @param jdbcUrl target MySQL JDBC URL including the database schema
	 * @param user database username
	 * @param password database password
	 */
	public record ConnectionSettings(String driverClass, String jdbcUrl, String user, String password)
	{
	}
	
	/**
	 * Installer progress event.
	 * @param level event severity
	 * @param message operator-facing message
	 * @param completedScripts number of completed SQL files
	 * @param totalScripts total SQL files selected for installation
	 */
	public record ProgressEvent(ProgressLevel level, String message, int completedScripts, int totalScripts)
	{
	}
	
	/**
	 * Final installation result.
	 * @param status final operation status
	 * @param scriptsExecuted number of SQL files fully processed
	 * @param statementErrors number of SQL statement errors accepted by the operator
	 * @param safetyBackup preventive backup artifact created before recreation, or {@code null}
	 */
	public record InstallationResult(ResultStatus status, int scriptsExecuted, int statementErrors, Path safetyBackup)
	{
	}
	
	/** Receives progress events from the engine. */
	@FunctionalInterface
	public interface ProgressListener
	{
		/**
		 * Receives one progress event.
		 * @param event current progress event
		 */
		void onProgress(ProgressEvent event);
	}
	
	/** Resolves how an existing target database should be handled. */
	@FunctionalInterface
	public interface ExistingDatabaseDecision
	{
		/**
		 * Selects the action for an existing database.
		 * @param databaseName existing database name
		 * @return selected action
		 * @throws Exception if the decision cannot be collected
		 */
		ExistingDatabaseAction choose(String databaseName) throws Exception;
	}
	
	/** Resolves how installation proceeds after a SQL statement error. */
	@FunctionalInterface
	public interface ScriptErrorDecision
	{
		/**
		 * Selects whether to continue after one SQL error.
		 * @param script SQL file being executed
		 * @param exception database error raised by the statement
		 * @return selected action
		 * @throws Exception if the decision cannot be collected
		 */
		ScriptErrorAction choose(Path script, SQLException exception) throws Exception;
	}
	
	/** Creates a preventive backup before a destructive database recreation. */
	@FunctionalInterface
	public interface SafetyBackup
	{
		/**
		 * Creates the preventive backup.
		 * @return backup artifact path, or {@code null} when the implementation has no path representation
		 * @throws Exception if the backup cannot be completed
		 */
		Path create() throws Exception;
	}
	
	private static final String LOGIN_DIRECTORY = "login";
	private static final String GAME_DIRECTORY = "game";
	private static final String MYSQL_PREFIX = "jdbc:mysql://";
	private final Path _installerDirectory;
	private final Path _sqlDirectory;
	
	/**
	 * Creates an engine bound to the official {@code dist/db_installer} directory.
	 * @param installerDirectory database installer directory containing {@code sql/login} and {@code sql/game}
	 */
	public DatabaseInstallationEngine(Path installerDirectory)
	{
		_installerDirectory = installerDirectory.toAbsolutePath().normalize();
		_sqlDirectory = _installerDirectory.resolve("sql");
	}
	
	/**
	 * Returns the database installer directory used by this engine.
	 * @return normalized installer directory
	 */
	public Path installerDirectory()
	{
		return _installerDirectory;
	}
	
	/**
	 * Returns the number of official Login SQL files.
	 * @return Login script count
	 * @throws IOException if the SQL directory cannot be read
	 */
	public int loginScriptCount() throws IOException
	{
		return listScripts(LOGIN_DIRECTORY).size();
	}
	
	/**
	 * Returns the number of official Game SQL files.
	 * @return Game script count
	 * @throws IOException if the SQL directory cannot be read
	 */
	public int gameScriptCount() throws IOException
	{
		return listScripts(GAME_DIRECTORY).size();
	}
	
	/**
	 * Validates that both official SQL groups exist and contain scripts.
	 * @throws IOException if required resources are missing
	 */
	public void validateResources() throws IOException
	{
		if (!Files.isDirectory(_installerDirectory))
		{
			throw new IOException("Database installer directory was not found: " + _installerDirectory);
		}
		
		if (listScripts(LOGIN_DIRECTORY).isEmpty())
		{
			throw new IOException("Login SQL directory does not contain installation scripts.");
		}
		
		if (listScripts(GAME_DIRECTORY).isEmpty())
		{
			throw new IOException("Game SQL directory does not contain installation scripts.");
		}
	}
	
	/**
	 * Tests access to the MySQL server without requiring the target schema to exist.
	 * @param settings connection settings
	 * @throws Exception if driver loading or connection validation fails
	 */
	public void testServerConnection(ConnectionSettings settings) throws Exception
	{
		final JdbcAddress address = validateSettings(settings);
		loadDriver(settings);
		DriverManager.setLoginTimeout(10);
		try (Connection connection = DriverManager.getConnection(address.serverUrl(), settings.user(), normalizedPassword(settings.password())))
		{
			if (!connection.isValid(5))
			{
				throw new SQLException("The MySQL connection opened but failed validation.");
			}
		}
	}
	
	/**
	 * Checks whether the configured target schema exists.
	 * @param settings connection settings
	 * @return {@code true} when the target schema exists
	 * @throws Exception if driver loading or server access fails
	 */
	public boolean databaseExists(ConnectionSettings settings) throws Exception
	{
		final JdbcAddress address = validateSettings(settings);
		loadDriver(settings);
		try (Connection connection = DriverManager.getConnection(address.serverUrl(), settings.user(), normalizedPassword(settings.password())))
		{
			return schemaExists(connection, address.database());
		}
	}
	
	/**
	 * Installs selected official SQL groups into the configured schema.
	 * @param settings database connection settings
	 * @param installLogin whether Login SQL files are installed
	 * @param installGame whether Game SQL files are installed
	 * @param existingDatabaseDecision callback used when the target schema already exists
	 * @param scriptErrorDecision callback used after SQL statement failures
	 * @param safetyBackup callback required before destructive database recreation
	 * @param cancelled cancellation state checked between safe boundaries
	 * @param progress progress event receiver
	 * @return final installation result
	 * @throws Exception if validation, backup, connection, or SQL preparation fails
	 */
	public InstallationResult install(ConnectionSettings settings, boolean installLogin, boolean installGame, ExistingDatabaseDecision existingDatabaseDecision, ScriptErrorDecision scriptErrorDecision, SafetyBackup safetyBackup, BooleanSupplier cancelled, ProgressListener progress) throws Exception
	{
		validateResources();
		final JdbcAddress address = validateSettings(settings);
		if (!installLogin && !installGame)
		{
			throw new IllegalArgumentException("Select Login, Game, or both database groups.");
		}
		
		if (existingDatabaseDecision == null)
		{
			throw new IllegalArgumentException("Existing database decision callback is required.");
		}
		
		if (scriptErrorDecision == null)
		{
			throw new IllegalArgumentException("SQL error decision callback is required.");
		}
		
		final BooleanSupplier cancellation = cancelled == null ? () -> false : cancelled;
		loadDriver(settings);
		final List<Path> scripts = selectedScripts(installLogin, installGame);
		final int totalScripts = scripts.size();
		Path safetyBackupPath = null;
		int completedScripts = 0;
		int statementErrors = 0;
		
		progress(progress, ProgressLevel.INFO, "Testing MySQL server connection...", 0, totalScripts);
		testServerConnection(settings);
		if (cancellation.getAsBoolean())
		{
			return result(ResultStatus.CANCELLED, completedScripts, statementErrors, safetyBackupPath, progress, totalScripts, "Installation cancelled before database preparation.");
		}
		
		try (Connection serverConnection = DriverManager.getConnection(address.serverUrl(), settings.user(), normalizedPassword(settings.password()));
			Statement serverStatement = serverConnection.createStatement())
		{
			if (schemaExists(serverConnection, address.database()))
			{
				progress(progress, ProgressLevel.WARNING, "Database '" + address.database() + "' already exists.", 0, totalScripts);
				final ExistingDatabaseAction action = existingDatabaseDecision.choose(address.database());
				if (action == ExistingDatabaseAction.CANCEL)
				{
					return result(ResultStatus.CANCELLED, completedScripts, statementErrors, safetyBackupPath, progress, totalScripts, "Installation cancelled. Existing database was not modified.");
				}
				
				if (action == ExistingDatabaseAction.RECREATE)
				{
					if (safetyBackup == null)
					{
						throw new IllegalStateException("A preventive backup provider is required before recreating an existing database.");
					}
					
					progress(progress, ProgressLevel.INFO, "Creating preventive backup before database recreation...", 0, totalScripts);
					safetyBackupPath = safetyBackup.create();
					progress(progress, ProgressLevel.SUCCESS, safetyBackupMessage(safetyBackupPath), 0, totalScripts);
					if (cancellation.getAsBoolean())
					{
						return result(ResultStatus.CANCELLED, completedScripts, statementErrors, safetyBackupPath, progress, totalScripts, "Installation cancelled before destructive database changes.");
					}
					
					serverStatement.execute("DROP DATABASE " + quoteIdentifier(address.database()));
					progress(progress, ProgressLevel.INFO, "Existing database deleted.", 0, totalScripts);
					createSchema(serverStatement, address.database());
					progress(progress, ProgressLevel.SUCCESS, "Database recreated using utf8mb4_unicode_ci.", 0, totalScripts);
				}
				else
				{
					progress(progress, ProgressLevel.INFO, "Installing on the existing database without deleting current data.", 0, totalScripts);
				}
			}
			else
			{
				createSchema(serverStatement, address.database());
				progress(progress, ProgressLevel.SUCCESS, "Database '" + address.database() + "' created using utf8mb4_unicode_ci.", 0, totalScripts);
			}
		}
		
		if (cancellation.getAsBoolean())
		{
			return result(ResultStatus.CANCELLED, completedScripts, statementErrors, safetyBackupPath, progress, totalScripts, "Installation cancelled before SQL execution.");
		}
		
		try (Connection schemaConnection = DriverManager.getConnection(settings.jdbcUrl(), settings.user(), normalizedPassword(settings.password()));
			Statement statement = schemaConnection.createStatement())
		{
			for (Path script : scripts)
			{
				if (cancellation.getAsBoolean())
				{
					return result(ResultStatus.CANCELLED, completedScripts, statementErrors, safetyBackupPath, progress, totalScripts, "Cancellation requested. No additional SQL files will be executed.");
				}
				
				progress(progress, ProgressLevel.INFO, "Installing " + script.getFileName() + "...", completedScripts, totalScripts);
				for (String sql : parseSqlStatements(script))
				{
					if (cancellation.getAsBoolean())
					{
						return result(ResultStatus.CANCELLED, completedScripts, statementErrors, safetyBackupPath, progress, totalScripts, "Cancellation requested. SQL execution stopped between statements.");
					}
					
					try
					{
						statement.execute(sql);
					}
					catch (SQLException e)
					{
						statementErrors++;
						progress(progress, ProgressLevel.ERROR, script.getFileName() + ": " + sanitize(e.getMessage()), completedScripts, totalScripts);
						if (scriptErrorDecision.choose(script, e) == ScriptErrorAction.ABORT)
						{
							return result(ResultStatus.ABORTED, completedScripts, statementErrors, safetyBackupPath, progress, totalScripts, "Installation aborted after an SQL error.");
						}
					}
				}
				
				completedScripts++;
				progress(progress, ProgressLevel.SUCCESS, "Completed " + script.getFileName() + ".", completedScripts, totalScripts);
			}
		}
		
		if (statementErrors > 0)
		{
			return result(ResultStatus.COMPLETED_WITH_ERRORS, completedScripts, statementErrors, safetyBackupPath, progress, totalScripts, "Installation completed with " + statementErrors + " accepted SQL error(s).");
		}
		
		return result(ResultStatus.SUCCESS, completedScripts, 0, safetyBackupPath, progress, totalScripts, "Database installation completed successfully.");
	}
	
	/**
	 * Parses one SQL file into executable statements while preserving semicolons inside quoted values.
	 * @param script SQL file
	 * @return ordered SQL statements
	 * @throws IOException if the file cannot be read
	 */
	public static List<String> parseSqlStatements(Path script) throws IOException
	{
		final String content = Files.readString(script, StandardCharsets.UTF_8);
		final List<String> statements = new ArrayList<>();
		final StringBuilder current = new StringBuilder(content.length());
		boolean singleQuote = false;
		boolean doubleQuote = false;
		boolean backtickQuote = false;
		boolean lineComment = false;
		boolean blockComment = false;
		boolean escaped = false;
		for (int index = 0; index < content.length(); index++)
		{
			final char character = content.charAt(index);
			final char next = (index + 1) < content.length() ? content.charAt(index + 1) : '\0';
			final char afterNext = (index + 2) < content.length() ? content.charAt(index + 2) : '\0';
			if (lineComment)
			{
				if ((character == '\n') || (character == '\r'))
				{
					lineComment = false;
					current.append(' ');
				}
				continue;
			}
			
			if (blockComment)
			{
				if ((character == '*') && (next == '/'))
				{
					blockComment = false;
					index++;
					current.append(' ');
				}
				continue;
			}
			
			if (singleQuote || doubleQuote || backtickQuote)
			{
				current.append(character);
				if (escaped)
				{
					escaped = false;
					continue;
				}
				
				if ((character == '\\') && !backtickQuote)
				{
					escaped = true;
					continue;
				}
				
				if (singleQuote && (character == '\''))
				{
					if (next == '\'')
					{
						current.append(next);
						index++;
					}
					else
					{
						singleQuote = false;
					}
				}
				else if (doubleQuote && (character == '"'))
				{
					doubleQuote = false;
				}
				else if (backtickQuote && (character == '`'))
				{
					backtickQuote = false;
				}
				continue;
			}
			
			if ((character == '-') && (next == '-') && ((afterNext == '\0') || Character.isWhitespace(afterNext)))
			{
				lineComment = true;
				index++;
				continue;
			}
			
			if (character == '#')
			{
				lineComment = true;
				continue;
			}
			
			if ((character == '/') && (next == '*'))
			{
				blockComment = true;
				index++;
				continue;
			}
			
			if (character == '\'')
			{
				singleQuote = true;
				current.append(character);
				continue;
			}
			
			if (character == '"')
			{
				doubleQuote = true;
				current.append(character);
				continue;
			}
			
			if (character == '`')
			{
				backtickQuote = true;
				current.append(character);
				continue;
			}
			
			if (character == ';')
			{
				final String statement = current.toString().trim();
				if (!statement.isEmpty())
				{
					statements.add(statement);
				}
				
				current.setLength(0);
				continue;
			}
			
			current.append(character);
		}
		
		final String trailing = current.toString().trim();
		if (!trailing.isEmpty())
		{
			statements.add(trailing);
		}
		
		return statements;
	}
	
	private List<Path> selectedScripts(boolean installLogin, boolean installGame) throws IOException
	{
		final List<Path> scripts = new ArrayList<>();
		if (installLogin)
		{
			scripts.addAll(listScripts(LOGIN_DIRECTORY));
		}
		
		if (installGame)
		{
			scripts.addAll(listScripts(GAME_DIRECTORY));
		}
		
		return scripts;
	}
	
	private List<Path> listScripts(String group) throws IOException
	{
		final Path directory = _sqlDirectory.resolve(group);
		if (!Files.isDirectory(directory))
		{
			return List.of();
		}
		
		final List<Path> scripts = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.sql"))
		{
			for (Path script : stream)
			{
				if (Files.isRegularFile(script))
				{
					scripts.add(script);
				}
			}
		}
		
		scripts.sort(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
		return scripts;
	}
	
	private static JdbcAddress validateSettings(ConnectionSettings settings)
	{
		if (settings == null)
		{
			throw new IllegalArgumentException("Database connection settings are required.");
		}
		
		if ((settings.user() == null) || settings.user().isBlank())
		{
			throw new IllegalArgumentException("Database username is required.");
		}
		
		return JdbcAddress.parse(settings.jdbcUrl());
	}
	
	private static void loadDriver(ConnectionSettings settings) throws ClassNotFoundException
	{
		if ((settings.driverClass() != null) && !settings.driverClass().isBlank())
		{
			Class.forName(settings.driverClass());
		}
	}
	
	private static boolean schemaExists(Connection connection, String databaseName) throws SQLException
	{
		try (PreparedStatement statement = connection.prepareStatement("SELECT SCHEMA_NAME FROM INFORMATION_SCHEMA.SCHEMATA WHERE SCHEMA_NAME = ?"))
		{
			statement.setString(1, databaseName);
			try (ResultSet result = statement.executeQuery())
			{
				return result.next();
			}
		}
	}
	
	private static void createSchema(Statement statement, String databaseName) throws SQLException
	{
		statement.execute("CREATE DATABASE " + quoteIdentifier(databaseName) + " CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
	}
	
	private static String quoteIdentifier(String databaseName)
	{
		validateDatabaseName(databaseName);
		return "`" + databaseName + "`";
	}
	
	private static void validateDatabaseName(String databaseName)
	{
		if ((databaseName == null) || !databaseName.matches("[A-Za-z0-9_$-]+"))
		{
			throw new IllegalArgumentException("Database name may contain only letters, numbers, underscore, dollar sign, or hyphen.");
		}
	}
	
	private static InstallationResult result(ResultStatus status, int scriptsExecuted, int statementErrors, Path safetyBackup, ProgressListener progress, int totalScripts, String message)
	{
		final ProgressLevel level;
		if (status == ResultStatus.SUCCESS)
		{
			level = ProgressLevel.SUCCESS;
		}
		else if (status == ResultStatus.ABORTED)
		{
			level = ProgressLevel.ERROR;
		}
		else
		{
			level = ProgressLevel.WARNING;
		}
		
		progress(progress, level, message, scriptsExecuted, totalScripts);
		return new InstallationResult(status, scriptsExecuted, statementErrors, safetyBackup);
	}
	
	private static void progress(ProgressListener listener, ProgressLevel level, String message, int completedScripts, int totalScripts)
	{
		if (listener != null)
		{
			listener.onProgress(new ProgressEvent(level, message, completedScripts, totalScripts));
		}
	}
	
	private static String safetyBackupMessage(Path safetyBackup)
	{
		return safetyBackup == null ? "Preventive backup completed." : "Preventive backup created: " + safetyBackup.getFileName();
	}
	
	private static String normalizedPassword(String password)
	{
		return password == null ? "" : password;
	}
	
	private static String sanitize(String message)
	{
		if (message == null)
		{
			return "SQL operation failed.";
		}
		
		return message.replace('\r', ' ').replace('\n', ' ').trim();
	}
	
	private record JdbcAddress(String host, int port, String database, String query)
	{
		static JdbcAddress parse(String jdbcUrl)
		{
			if ((jdbcUrl == null) || !jdbcUrl.regionMatches(true, 0, MYSQL_PREFIX, 0, MYSQL_PREFIX.length()))
			{
				throw new IllegalArgumentException("Expected a MySQL JDBC URL.");
			}
			
			final String body = jdbcUrl.substring(MYSQL_PREFIX.length());
			final int queryStart = body.indexOf('?');
			final String authorityAndSchema = queryStart >= 0 ? body.substring(0, queryStart) : body;
			final String query = queryStart >= 0 ? body.substring(queryStart + 1) : "";
			final int slash = authorityAndSchema.indexOf('/');
			if (slash <= 0)
			{
				throw new IllegalArgumentException("JDBC URL does not contain a database schema.");
			}
			
			final String authority = authorityAndSchema.substring(0, slash);
			final String database = authorityAndSchema.substring(slash + 1);
			final int colon = authority.lastIndexOf(':');
			final String host = colon > 0 ? authority.substring(0, colon) : authority;
			final int port = colon > 0 ? Integer.parseInt(authority.substring(colon + 1)) : 3306;
			if (host.isBlank() || database.isBlank())
			{
				throw new IllegalArgumentException("JDBC host and schema must not be empty.");
			}
			
			if ((port < 1) || (port > 65535))
			{
				throw new IllegalArgumentException("Database port must be between 1 and 65535.");
			}
			
			validateDatabaseName(database);
			return new JdbcAddress(host, port, database, query);
		}
		
		String serverUrl()
		{
			final StringBuilder result = new StringBuilder(MYSQL_PREFIX).append(host).append(':').append(port).append('/');
			if (!query.isEmpty())
			{
				result.append('?').append(query);
			}
			
			return result.toString();
		}
	}
}
