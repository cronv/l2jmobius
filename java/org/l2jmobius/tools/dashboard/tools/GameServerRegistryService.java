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

import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.l2jmobius.tools.dashboard.database.DatabaseEndpoint;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Registers Interlude GameServer IDs directly from the Dashboard without starting the standalone utility.
 * @author BazookaRpm
 */
public final class GameServerRegistryService
{
	public record ServerRecord(int id, String name, boolean registered)
	{
	}
	
	private final ControlCenterSettings _settings;
	private final SecureRandom _random = new SecureRandom();
	
	public GameServerRegistryService(ControlCenterSettings settings)
	{
		_settings = settings;
	}
	
	public List<ServerRecord> list(DatabaseEndpoint endpoint) throws Exception
	{
		final Map<Integer, String> names = loadServerNames();
		final Map<Integer, Boolean> registered = loadRegistered(endpoint);
		final List<ServerRecord> records = new ArrayList<>();
		for (Entry<Integer, String> entry : names.entrySet())
		{
			records.add(new ServerRecord(entry.getKey(), entry.getValue(), registered.containsKey(entry.getKey())));
		}
		
		records.sort((left, right) -> Integer.compare(left.id(), right.id()));
		return records;
	}
	
	public Path register(DatabaseEndpoint endpoint, int serverId) throws Exception
	{
		final Map<Integer, String> names = loadServerNames();
		if (!names.containsKey(serverId))
		{
			throw new IllegalArgumentException("Unknown GameServer ID: " + serverId + ".");
		}
		
		final byte[] hexId = new byte[16];
		_random.nextBytes(hexId);
		final String hex = new BigInteger(hexId).toString(16);
		try (Connection connection = open(endpoint))
		{
			try (PreparedStatement check = connection.prepareStatement("SELECT COUNT(*) FROM gameservers WHERE server_id = ?"))
			{
				check.setInt(1, serverId);
				try (ResultSet result = check.executeQuery())
				{
					if (result.next() && (result.getInt(1) > 0))
					{
						throw new IllegalStateException("GameServer ID " + serverId + " is already registered.");
					}
				}
			}
			
			try (PreparedStatement insert = connection.prepareStatement("INSERT INTO gameservers (hexid, server_id, host) VALUES (?, ?, ?)"))
			{
				insert.setString(1, hex);
				insert.setInt(2, serverId);
				insert.setString(3, "");
				insert.executeUpdate();
			}
		}
		
		final Path output = _settings.getDistributionRoot().resolve("game/config/hexid.txt");
		Files.createDirectories(output.getParent());
		final Properties values = new Properties();
		values.setProperty("ServerID", String.valueOf(serverId));
		values.setProperty("HexID", hex);
		try (OutputStream stream = Files.newOutputStream(output))
		{
			values.store(stream, "The HexId to Auth into LoginServer");
		}
		
		return output;
	}
	
	public void unregister(DatabaseEndpoint endpoint, int serverId) throws Exception
	{
		try (Connection connection = open(endpoint);
			PreparedStatement statement = connection.prepareStatement("DELETE FROM gameservers WHERE server_id = ?"))
		{
			statement.setInt(1, serverId);
			statement.executeUpdate();
		}
	}
	
	public int unregisterAll(DatabaseEndpoint endpoint) throws Exception
	{
		try (Connection connection = open(endpoint);
			PreparedStatement statement = connection.prepareStatement("DELETE FROM gameservers"))
		{
			return statement.executeUpdate();
		}
	}
	
	private Map<Integer, String> loadServerNames() throws Exception
	{
		final Path file = _settings.getDistributionRoot().resolve("login/data/servername.xml");
		if (!Files.isRegularFile(file))
		{
			throw new IllegalStateException("Server name list was not found: " + file + ".");
		}
		
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		final NodeList nodes = factory.newDocumentBuilder().parse(file.toFile()).getElementsByTagName("server");
		final Map<Integer, String> result = new HashMap<>();
		for (int index = 0; index < nodes.getLength(); index++)
		{
			final Element server = (Element) nodes.item(index);
			result.put(Integer.parseInt(server.getAttribute("id")), server.getAttribute("name"));
		}
		
		return result;
	}
	
	private static Map<Integer, Boolean> loadRegistered(DatabaseEndpoint endpoint) throws Exception
	{
		final Map<Integer, Boolean> result = new HashMap<>();
		try (Connection connection = open(endpoint);
			PreparedStatement statement = connection.prepareStatement("SELECT server_id FROM gameservers");
			ResultSet rows = statement.executeQuery())
		{
			while (rows.next())
			{
				result.put(rows.getInt(1), Boolean.TRUE);
			}
		}
		
		return result;
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
}
