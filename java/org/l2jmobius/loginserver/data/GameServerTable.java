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
package org.l2jmobius.loginserver.data;

import java.io.File;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.LoginServer;
import org.l2jmobius.loginserver.network.LoginClient;
import org.l2jmobius.loginserver.network.gameserverpackets.receive.ServerStatus;
import org.l2jmobius.loginserver.util.Subnet;

/**
 * This class is responsible for loading game server names from configuration files.<br>
 * It initializes the internal data structures used to manage the list of available game servers.
 * @author KenM
 */
public class GameServerTable implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(GameServerTable.class.getName());
	
	// Map for storing server names with their corresponding IDs.
	private static final Map<Integer, String> SERVER_NAMES = new HashMap<>();
	
	// Table for storing game server information.
	private static final Map<Integer, GameServerInfo> GAME_SERVER_TABLE = new HashMap<>();
	
	// Configuration for RSA key pairs.
	private static final int KEYS_SIZE = 10;
	private KeyPair[] _keyPairs;
	
	/**
	 * Initializes a new instance of the {@code GameServerTable} class.<br>
	 * This constructor calls {@code load} and {@code loadRegisteredGameServers}.<br>
	 * It also initializes the RSA keys for secure communication.
	 */
	public GameServerTable()
	{
		load();
		
		loadRegisteredGameServers();
		LOGGER.info("Loaded " + GAME_SERVER_TABLE.size() + " registered Game Servers.");
		
		initRSAKeys();
		LOGGER.info("Cached " + _keyPairs.length + " RSA keys for Game Server communication.");
	}
	
	/**
	 * Loads the game server names from the configuration file.<br>
	 * This method clears existing data and parses {@code data/servername.xml}.<br>
	 * It logs the total number of servers loaded to the logger.
	 */
	@Override
	public void load()
	{
		SERVER_NAMES.clear();
		parseDatapackFile("data/servername.xml");
		LOGGER.info("Loaded " + SERVER_NAMES.size() + " server names.");
	}
	
	/**
	 * Parses the server information from an XML document.<br>
	 * This method populates the internal server name map.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} source of the XML document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		final NodeList servers = document.getElementsByTagName("server");
		for (int s = 0; s < servers.getLength(); s++)
		{
			final NamedNodeMap attrs = servers.item(s).getAttributes();
			SERVER_NAMES.put(parseInteger(attrs, "id"), parseString(attrs, "name"));
		}
	}
	
	/**
	 * Initializes the {@code KeyPair} array with new RSA keys.<br>
	 * These keys are used for secure communication between servers.<br>
	 * It generates a specific number of keys based on the {@code KEYS_SIZE} constant.
	 */
	private void initRSAKeys()
	{
		try
		{
			final KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(new RSAKeyGenParameterSpec(512, RSAKeyGenParameterSpec.F4));
			_keyPairs = new KeyPair[KEYS_SIZE];
			for (int i = 0; i < KEYS_SIZE; i++)
			{
				_keyPairs[i] = keyGen.genKeyPair();
			}
		}
		catch (Exception e)
		{
			LOGGER.severe("Error loading RSA keys for Game Server communication!");
		}
	}
	
	/**
	 * Loads all registered game servers from the database.<br>
	 * This method populates the {@code GAME_SERVER_TABLE} map.<br>
	 * It executes a SQL query to fetch server IDs and hex IDs.
	 */
	private void loadRegisteredGameServers()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT * FROM gameservers"))
		{
			int id;
			while (rs.next())
			{
				id = rs.getInt("server_id");
				GAME_SERVER_TABLE.put(id, new GameServerInfo(id, stringToHex(rs.getString("hexid"))));
			}
		}
		catch (Exception e)
		{
			LOGGER.severe("Error loading registered game servers!");
		}
	}
	
	/**
	 * Retrieves all game servers currently registered in the system.<br>
	 * The data is returned as a map where the key is the server ID.
	 * @return A {@code Map} containing integer IDs and their corresponding {@code GameServerInfo} objects.
	 */
	public Map<Integer, GameServerInfo> getRegisteredGameServers()
	{
		return GAME_SERVER_TABLE;
	}
	
	/**
	 * Retrieves a {@code GameServerInfo} object using its unique identifier.<br>
	 * This method looks up the server in the internal registry.
	 * @param id The unique integer ID of the game server.
	 * @return The {@code GameServerInfo} associated with the provided ID, or {@code null} if not found.
	 */
	public GameServerInfo getRegisteredGameServerById(int id)
	{
		return GAME_SERVER_TABLE.get(id);
	}
	
	/**
	 * Checks if a game server is registered with the given ID.<br>
	 * This method looks up the {@code id} in the internal registry.
	 * @param id The unique identifier of the game server to check.
	 * @return {@code true} if the server exists, {@code false} otherwise.
	 */
	public boolean hasRegisteredGameServerOnId(int id)
	{
		return GAME_SERVER_TABLE.containsKey(id);
	}
	
	/**
	 * Registers a new game server using the first available ID found in the system.<br>
	 * This method ensures that no two servers share the same identifier.
	 * @param gsi The {@code GameServerInfo} object to register.
	 * @return {@code true} if the registration was successful, or {@code false} if no IDs are available.
	 */
	public boolean registerWithFirstAvailableId(GameServerInfo gsi)
	{
		// Avoid two servers registering with the same "free" id.
		synchronized (GAME_SERVER_TABLE)
		{
			for (Integer serverId : SERVER_NAMES.keySet())
			{
				if (!GAME_SERVER_TABLE.containsKey(serverId))
				{
					GAME_SERVER_TABLE.put(serverId, gsi);
					gsi.setId(serverId);
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Registers a new game server with the specified ID.<br>
	 * This method ensures that no two servers share the same ID.
	 * @param id The unique identifier for the game server.
	 * @param gsi The {@code GameServerInfo} object containing server details.
	 * @return {@code true} if the registration was successful, or {@code false} if the ID is already taken.
	 */
	public boolean register(int id, GameServerInfo gsi)
	{
		// Avoid two servers registering with the same id.
		synchronized (GAME_SERVER_TABLE)
		{
			if (!GAME_SERVER_TABLE.containsKey(id))
			{
				GAME_SERVER_TABLE.put(id, gsi);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Saves the game server information to the database.<br>
	 * This method uses the data provided in the {@code GameServerInfo} object.
	 * @param gsi The {@code GameServerInfo} object containing the server details.
	 */
	public void registerServerOnDB(GameServerInfo gsi)
	{
		registerServerOnDB(gsi.getHexId(), gsi.getId(), gsi.getExternalHost());
	}
	
	/**
	 * Saves a new game server to the database.<br>
	 * This method registers the server in memory and inserts its details into the table.
	 * @param hexId The unique identifier for the server in hexadecimal format.
	 * @param id The numerical ID assigned to the server.
	 * @param externalHost The network address where the server is hosted.
	 */
	public void registerServerOnDB(byte[] hexId, int id, String externalHost)
	{
		register(id, new GameServerInfo(id, hexId));
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO gameservers (hexid,server_id,host) values (?,?,?)"))
		{
			ps.setString(1, hexToString(hexId));
			ps.setInt(2, id);
			ps.setString(3, externalHost);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.severe("Error while saving gameserver!");
		}
	}
	
	/**
	 * Retrieves the name of a game server using its unique identifier.<br>
	 * This method looks up the {@code id} in the internal server map.
	 * @param id The unique identification number for the game server.
	 * @return The name of the server as a {@code String}, or {@code null} if not found.
	 */
	public String getServerNameById(int id)
	{
		return SERVER_NAMES.get(id);
	}
	
	/**
	 * Retrieves a map of all server names.<br>
	 * The keys in the {@code Map} are the unique server IDs.<br>
	 * The values are the corresponding server names.
	 * @return A {@code Map<Integer, String>} containing all registered server names.
	 */
	public Map<Integer, String> getServerNames()
	{
		return SERVER_NAMES;
	}
	
	/**
	 * Retrieves a random {@code KeyPair} from the internal collection.<br>
	 * This method uses {@code Rnd} to select an index between 0 and 9.
	 * @return A randomly selected {@code KeyPair}.
	 */
	public KeyPair getKeyPair()
	{
		return _keyPairs[Rnd.get(10)];
	}
	
	/**
	 * Converts a hexadecimal string into a byte array.<br>
	 * The input {@code string} must be a valid hex representation.
	 * @param string The hexadecimal string to convert.
	 * @return A {@code byte[]} representing the hex value.
	 */
	private byte[] stringToHex(String string)
	{
		return new BigInteger(string, 16).toByteArray();
	}
	
	/**
	 * Converts a byte array into its hexadecimal string representation.<br>
	 * This method uses {@code BigInteger} to perform the conversion.<br>
	 * It returns the literal {@code null} as a string if the input is {@code null}.
	 * @param hex The byte array to be converted.
	 * @return The resulting hexadecimal string.
	 */
	private String hexToString(byte[] hex)
	{
		if (hex == null)
		{
			return "null";
		}
		
		return new BigInteger(hex).toString(16);
	}
	
	/**
	 * The Class GameServerInfo.
	 */
	public static class GameServerInfo
	{
		// Authentication fields.
		private int _id;
		private final byte[] _hexId;
		private boolean _isAuthed;
		
		// Status information.
		private GameServerThread _gst;
		private int _status;
		
		// Network configuration.
		private final List<GameServerAddress> _addrs = new ArrayList<>(5);
		private int _port;
		
		// Configuration settings.
		private static final boolean IS_PVP = true;
		private int _serverType;
		private int _ageLimit;
		private boolean _isShowingBrackets;
		private int _maxPlayers;
		
		/**
		 * Instantiates a new game server info.
		 * @param id the id
		 * @param hexId the hex id
		 * @param gst the gst
		 */
		public GameServerInfo(int id, byte[] hexId, GameServerThread gst)
		{
			_id = id;
			_hexId = hexId;
			_gst = gst;
			_status = ServerStatus.STATUS_DOWN;
		}
		
		/**
		 * Instantiates a new game server info.
		 * @param id the id
		 * @param hexId the hex id
		 */
		public GameServerInfo(int id, byte[] hexId)
		{
			this(id, hexId, null);
		}
		
		/**
		 * Sets the id.
		 * @param id the new id
		 */
		public void setId(int id)
		{
			_id = id;
		}
		
		/**
		 * Gets the id.
		 * @return the id
		 */
		public int getId()
		{
			return _id;
		}
		
		/**
		 * Gets the hex id.
		 * @return the hex id
		 */
		public byte[] getHexId()
		{
			return _hexId;
		}
		
		public String getName()
		{
			// This value can't be stored in a private variable because the ID can be changed by setId().
			return getInstance().getServerNameById(_id);
		}
		
		/**
		 * Sets the authed.
		 * @param isAuthed the new authed
		 */
		public void setAuthed(boolean isAuthed)
		{
			_isAuthed = isAuthed;
		}
		
		/**
		 * Checks if is authed.
		 * @return true, if is authed
		 */
		public boolean isAuthed()
		{
			return _isAuthed;
		}
		
		/**
		 * Sets the game server thread.
		 * @param gst the new game server thread
		 */
		public void setGameServerThread(GameServerThread gst)
		{
			_gst = gst;
		}
		
		/**
		 * Gets the game server thread.
		 * @return the game server thread
		 */
		public GameServerThread getGameServerThread()
		{
			return _gst;
		}
		
		/**
		 * Sets the status.
		 * @param status the new status
		 */
		public void setStatus(int status)
		{
			final int loginServerStatus = LoginServer.getInstance().getStatus();
			if (loginServerStatus == ServerStatus.STATUS_DOWN)
			{
				_status = ServerStatus.STATUS_DOWN;
			}
			else if (loginServerStatus == ServerStatus.STATUS_GM_ONLY)
			{
				_status = ServerStatus.STATUS_GM_ONLY;
			}
			else
			{
				_status = status;
			}
		}
		
		/**
		 * Gets the status.
		 * @return the status
		 */
		public int getStatus()
		{
			return _status;
		}
		
		public String getStatusName()
		{
			switch (_status)
			{
				case 0:
				{
					return "Auto";
				}
				case 1:
				{
					return "Good";
				}
				case 2:
				{
					return "Normal";
				}
				case 3:
				{
					return "Full";
				}
				case 4:
				{
					return "Down";
				}
				case 5:
				{
					return "GM Only";
				}
				default:
				{
					return "Unknown";
				}
			}
		}
		
		/**
		 * Gets the current player count.
		 * @return the current player count
		 */
		public int getCurrentPlayerCount()
		{
			if (_gst == null)
			{
				return 0;
			}
			
			return _gst.getPlayerCount();
		}
		
		public boolean canLogin(LoginClient client)
		{
			// DOWN status doesn't allow anyone to login.
			if (_status == ServerStatus.STATUS_DOWN)
			{
				return false;
			}
			
			// GM_ONLY status or full server only allows superior access levels accounts to login.
			if ((_status == ServerStatus.STATUS_GM_ONLY) || (getCurrentPlayerCount() >= getMaxPlayers()))
			{
				return client.getAccessLevel() > 0;
			}
			
			// Otherwise, any positive access level account can login.
			return client.getAccessLevel() >= 0;
		}
		
		/**
		 * Gets the external host.
		 * @return the external host
		 */
		public String getExternalHost()
		{
			try
			{
				return getServerAddress(InetAddress.getByName("0.0.0.0"));
			}
			catch (Exception e)
			{
				// Ignore.
			}
			
			return null;
		}
		
		/**
		 * Gets the port.
		 * @return the port
		 */
		public int getPort()
		{
			return _port;
		}
		
		/**
		 * Sets the port.
		 * @param port the new port
		 */
		public void setPort(int port)
		{
			_port = port;
		}
		
		/**
		 * Sets the max players.
		 * @param maxPlayers the new max players
		 */
		public void setMaxPlayers(int maxPlayers)
		{
			_maxPlayers = maxPlayers;
		}
		
		/**
		 * Gets the max players.
		 * @return the max players
		 */
		public int getMaxPlayers()
		{
			return _maxPlayers;
		}
		
		/**
		 * Checks if is pvp.
		 * @return true, if is pvp
		 */
		public boolean isPvp()
		{
			return IS_PVP;
		}
		
		/**
		 * Sets the age limit.
		 * @param value the new age limit
		 */
		public void setAgeLimit(int value)
		{
			_ageLimit = value;
		}
		
		/**
		 * Gets the age limit.
		 * @return the age limit
		 */
		public int getAgeLimit()
		{
			return _ageLimit;
		}
		
		/**
		 * Sets the server type.
		 * @param value the new server type
		 */
		public void setServerType(int value)
		{
			_serverType = value;
		}
		
		/**
		 * Gets the server type.
		 * @return the server type
		 */
		public int getServerType()
		{
			return _serverType;
		}
		
		/**
		 * Sets the showing brackets.
		 * @param value the new showing brackets
		 */
		public void setShowingBrackets(boolean value)
		{
			_isShowingBrackets = value;
		}
		
		/**
		 * Checks if is showing brackets.
		 * @return true, if is showing brackets
		 */
		public boolean isShowingBrackets()
		{
			return _isShowingBrackets;
		}
		
		/**
		 * Sets the down.
		 */
		public void setDown()
		{
			setAuthed(false);
			setPort(0);
			setGameServerThread(null);
			setStatus(ServerStatus.STATUS_DOWN);
		}
		
		/**
		 * Adds the server address.
		 * @param subnet the subnet
		 * @param addr the addr
		 * @throws UnknownHostException the unknown host exception
		 */
		public void addServerAddress(String subnet, String addr) throws UnknownHostException
		{
			_addrs.add(new GameServerAddress(subnet, addr));
		}
		
		/**
		 * Gets the server address.
		 * @param addr the addr
		 * @return the server address
		 */
		@SuppressWarnings("unlikely-arg-type")
		public String getServerAddress(InetAddress addr)
		{
			for (GameServerAddress a : _addrs)
			{
				if (a.equals(addr))
				{
					return a.getServerAddress();
				}
			}
			
			return null; // Should not happen.
		}
		
		/**
		 * Gets the server addresses.
		 * @return the server addresses
		 */
		public String[] getServerAddresses()
		{
			final String[] result = new String[_addrs.size()];
			for (int i = 0; i < result.length; i++)
			{
				result[i] = _addrs.get(i).toString();
			}
			
			return result;
		}
		
		/**
		 * Clear server addresses.
		 */
		public void clearServerAddresses()
		{
			_addrs.clear();
		}
		
		/**
		 * The Class GameServerAddress.
		 */
		private class GameServerAddress extends Subnet
		{
			private final String _serverAddress;
			
			/**
			 * Instantiates a new game server address.
			 * @param subnet the subnet
			 * @param address the address
			 * @throws UnknownHostException the unknown host exception
			 */
			public GameServerAddress(String subnet, String address) throws UnknownHostException
			{
				super(subnet);
				_serverAddress = address;
			}
			
			/**
			 * Gets the server address.
			 * @return the server address
			 */
			public String getServerAddress()
			{
				return _serverAddress;
			}
			
			@Override
			public String toString()
			{
				return _serverAddress + "/" + super.toString();
			}
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code GameServerTable}.<br>
	 * This provides a global access point to the game server data.
	 * @return The single instance of {@code GameServerTable}.
	 */
	public static GameServerTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * The Class SingletonHolder.
	 */
	private static class SingletonHolder
	{
		protected static final GameServerTable INSTANCE = new GameServerTable();
	}
}
