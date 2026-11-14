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
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.tools.dashboard.runtime.ServiceRole;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Reads, validates, tests, and atomically updates Interlude login and game database settings.
 * @author BazookaRpm
 */
public final class DatabaseConfigurationService
{
	private final ControlCenterSettings _settings;
	
	public DatabaseConfigurationService(ControlCenterSettings settings)
	{
		_settings = settings;
	}
	
	public DatabaseEndpoint loadLogin() throws IOException
	{
		return load(ServiceRole.LOGIN);
	}
	
	public DatabaseEndpoint loadGame() throws IOException
	{
		return load(ServiceRole.GAME);
	}
	
	public DatabaseEndpoint loadPrimary() throws IOException
	{
		final DatabaseEndpoint game = loadGame();
		return (game.jdbcUrl() == null) ? loadLogin() : game;
	}
	
	public void test(DatabaseEndpoint endpoint) throws Exception
	{
		if ((endpoint.driver() != null) && !endpoint.driver().isBlank())
		{
			Class.forName(endpoint.driver());
		}
		
		DriverManager.setLoginTimeout(10);
		try (Connection connection = DriverManager.getConnection(endpoint.jdbcUrl(), endpoint.user(), endpoint.password()))
		{
			if (!connection.isValid(5))
			{
				throw new IllegalStateException("The database connection was opened but failed validation.");
			}
		}
	}
	
	public void applyShared(String host, int port, String database, String user, String password) throws Exception
	{
		validate(host, port, database, user);
		final DatabaseEndpoint login = loadLogin();
		final DatabaseEndpoint game = loadGame();
		final Map<Path, byte[]> updates = new LinkedHashMap<>();
		updates.put(login.sourceFile(), update(login, host, port, database, user, password));
		updates.put(game.sourceFile(), update(game, host, port, database, user, password));
		for (Entry<Path, byte[]> entry : updates.entrySet())
		{
			atomicWrite(entry.getKey(), entry.getValue());
		}
	}
	
	private DatabaseEndpoint load(ServiceRole role) throws IOException
	{
		final Path file = _settings.getDatabaseConfig(role);
		final IniFileModel ini = IniFileModel.load(file);
		return new DatabaseEndpoint(file, ini.get("Driver", "com.mysql.cj.jdbc.Driver"), ini.get("URL", ""), ini.get("Login", "root"), ini.get("Password", ""), ini.get("MySqlBinLocation", ""));
	}
	
	private byte[] update(DatabaseEndpoint endpoint, String host, int port, String database, String user, String password) throws IOException
	{
		final IniFileModel ini = IniFileModel.load(endpoint.sourceFile());
		final Map<String, String> values = new LinkedHashMap<>();
		values.put("URL", endpoint.address().rebuild(host, port, database));
		values.put("Login", user);
		values.put("Password", password);
		return ini.withValues(values);
	}
	
	private static void validate(String host, int port, String database, String user)
	{
		if ((host == null) || host.isBlank() || (database == null) || database.isBlank() || (user == null) || user.isBlank())
		{
			throw new IllegalArgumentException("Host, database, and user are required.");
		}
		
		if ((port < 1) || (port > 65535))
		{
			throw new IllegalArgumentException("Database port must be between 1 and 65535.");
		}
		
		if (!database.matches("[A-Za-z0-9_$-]+"))
		{
			throw new IllegalArgumentException("Database name contains unsupported characters.");
		}
	}
	
	private static void atomicWrite(Path file, byte[] content) throws IOException
	{
		final Path name = file.getFileName();
		Files.copy(file, file.resolveSibling(name + ".dashboard.bak"), StandardCopyOption.REPLACE_EXISTING);
		final Path temporary = file.resolveSibling(name + ".dashboard.tmp");
		Files.write(temporary, content);
		try
		{
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException e)
		{
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
