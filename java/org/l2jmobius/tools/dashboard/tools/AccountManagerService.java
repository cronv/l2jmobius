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
package org.l2jmobius.tools.dashboard.tools;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import org.l2jmobius.tools.dashboard.database.DatabaseEndpoint;

/**
 * Provides account-management operations for the integrated Dashboard page.
 * @author BazookaRpm
 */
public final class AccountManagerService
{
	public record AccountRecord(String login, int accessLevel, String email, String lastIp, Timestamp created, long lastActive)
	{
	}
	
	public void test(DatabaseEndpoint endpoint) throws Exception
	{
		try (Connection connection = open(endpoint))
		{
			if (!connection.isValid(5))
			{
				throw new SQLException("Database connection validation failed.");
			}
		}
	}
	
	public int count(DatabaseEndpoint endpoint) throws Exception
	{
		try (Connection connection = open(endpoint);
			PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM accounts");
			ResultSet result = statement.executeQuery())
		{
			return result.next() ? result.getInt(1) : 0;
		}
	}
	
	public List<AccountRecord> list(DatabaseEndpoint endpoint, int limit, int offset) throws Exception
	{
		final List<AccountRecord> accounts = new ArrayList<>();
		try (Connection connection = open(endpoint);
			PreparedStatement statement = connection.prepareStatement("SELECT login, accessLevel, email, lastIP, created_time, lastactive FROM accounts ORDER BY login LIMIT ? OFFSET ?"))
		{
			statement.setInt(1, limit);
			statement.setInt(2, offset);
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					accounts.add(read(result));
				}
			}
		}
		
		return accounts;
	}
	
	public List<AccountRecord> search(DatabaseEndpoint endpoint, String text, int limit) throws Exception
	{
		final List<AccountRecord> accounts = new ArrayList<>();
		try (Connection connection = open(endpoint);
			PreparedStatement statement = connection.prepareStatement("SELECT login, accessLevel, email, lastIP, created_time, lastactive FROM accounts WHERE login LIKE ? ORDER BY login LIMIT ?"))
		{
			statement.setString(1, "%" + text + "%");
			statement.setInt(2, limit);
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					accounts.add(read(result));
				}
			}
		}
		
		return accounts;
	}
	
	public void create(DatabaseEndpoint endpoint, String username, char[] password, int accessLevel) throws Exception
	{
		validateUsername(username);
		if ((password == null) || (password.length == 0))
		{
			throw new IllegalArgumentException("Password is required.");
		}
		
		try (Connection connection = open(endpoint))
		{
			try (PreparedStatement check = connection.prepareStatement("SELECT COUNT(*) FROM accounts WHERE login = ?"))
			{
				check.setString(1, username);
				try (ResultSet result = check.executeQuery())
				{
					if (result.next() && (result.getInt(1) > 0))
					{
						throw new IllegalStateException("Account already exists.");
					}
				}
			}
			
			try (PreparedStatement insert = connection.prepareStatement("INSERT INTO accounts (login, password, accessLevel) VALUES (?, ?, ?)"))
			{
				insert.setString(1, username);
				insert.setString(2, hash(password));
				insert.setInt(3, accessLevel);
				insert.executeUpdate();
			}
		}
	}
	
	public void update(DatabaseEndpoint endpoint, String username, char[] password, int accessLevel, boolean updatePassword) throws Exception
	{
		validateUsername(username);
		final String sql = updatePassword ? "UPDATE accounts SET password = ?, accessLevel = ? WHERE login = ?" : "UPDATE accounts SET accessLevel = ? WHERE login = ?";
		try (Connection connection = open(endpoint);
			PreparedStatement statement = connection.prepareStatement(sql))
		{
			if (updatePassword)
			{
				if ((password == null) || (password.length == 0))
				{
					throw new IllegalArgumentException("New password is required when password update is enabled.");
				}
				
				statement.setString(1, hash(password));
				statement.setInt(2, accessLevel);
				statement.setString(3, username);
			}
			else
			{
				statement.setInt(1, accessLevel);
				statement.setString(2, username);
			}
			
			if (statement.executeUpdate() == 0)
			{
				throw new IllegalStateException("Account was not found.");
			}
		}
	}
	
	public void delete(DatabaseEndpoint endpoint, String username) throws Exception
	{
		validateUsername(username);
		try (Connection connection = open(endpoint);
			PreparedStatement statement = connection.prepareStatement("DELETE FROM accounts WHERE login = ?"))
		{
			statement.setString(1, username);
			if (statement.executeUpdate() == 0)
			{
				throw new IllegalStateException("Account was not found.");
			}
		}
	}
	
	private static AccountRecord read(ResultSet result) throws SQLException
	{
		return new AccountRecord(result.getString("login"), result.getInt("accessLevel"), result.getString("email"), result.getString("lastIP"), result.getTimestamp("created_time"), result.getLong("lastactive"));
	}
	
	private static Connection open(DatabaseEndpoint endpoint) throws Exception
	{
		if ((endpoint.driver() != null) && !endpoint.driver().isBlank())
		{
			Class.forName(endpoint.driver());
		}
		
		DriverManager.setLoginTimeout(10);
		return DriverManager.getConnection(endpoint.jdbcUrl(), endpoint.user(), endpoint.password());
	}
	
	private static String hash(char[] password) throws Exception
	{
		final MessageDigest digest = MessageDigest.getInstance("SHA");
		return Base64.getEncoder().encodeToString(digest.digest(new String(password).getBytes(StandardCharsets.UTF_8)));
	}
	
	private static void validateUsername(String username)
	{
		if ((username == null) || username.isBlank())
		{
			throw new IllegalArgumentException("Account name is required.");
		}
	}
}
