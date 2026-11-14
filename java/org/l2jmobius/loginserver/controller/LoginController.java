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
package org.l2jmobius.loginserver.controller;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Cipher;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.loginserver.GameServerThread;
import org.l2jmobius.loginserver.config.LoginConfig;
import org.l2jmobius.loginserver.data.GameServerTable;
import org.l2jmobius.loginserver.data.GameServerTable.GameServerInfo;
import org.l2jmobius.loginserver.enums.LoginFailReason;
import org.l2jmobius.loginserver.enums.LoginResult;
import org.l2jmobius.loginserver.network.LoginClient;

/**
 * Handles user authentication and session management between the login server and game servers.
 * @author Mobius
 */
public class LoginController
{
	protected static final Logger LOGGER = Logger.getLogger(LoginController.class.getName());
	
	// SQL Queries.
	private static final String USER_INFO_SELECT = "SELECT login, password, IF(CAST(? AS UNSIGNED) > CAST(value AS UNSIGNED) OR value IS NULL, accessLevel, -1) AS accessLevel, lastServer FROM accounts LEFT JOIN (account_data) ON (account_data.account_name=accounts.login AND account_data.var=\"ban_temp\") WHERE login=?";
	private static final String AUTOCREATE_ACCOUNTS_INSERT = "INSERT INTO accounts (login, password, lastactive, accessLevel, lastIP) values (?, ?, ?, ?, ?)";
	private static final String ACCOUNT_INFO_UPDATE = "UPDATE accounts SET lastactive = ?, lastIP = ? WHERE login = ?";
	private static final String ACCOUNT_LAST_SERVER_UPDATE = "UPDATE accounts SET lastServer = ? WHERE login = ?";
	private static final String ACCOUNT_ACCESS_LEVEL_UPDATE = "UPDATE accounts SET accessLevel = ? WHERE login = ?";
	private static final String ACCOUNT_IPS_UPDATE = "UPDATE accounts SET pcIp = ?, hop1 = ?, hop2 = ?, hop3 = ?, hop4 = ? WHERE login = ?";
	private static final String ACCOUNT_IPAUTH_SELECT = "SELECT * FROM accounts_ipauth WHERE login = ?";
	private static final String DELETE_EXPIRED_BAN = "DELETE FROM account_data WHERE account_name=? AND var='ban_temp' AND CAST(value AS UNSIGNED) <= CAST(? AS UNSIGNED)";
	
	/** Time before kicking the client if he didn't logged yet */
	public static final int LOGIN_TIMEOUT = 5 * 60 * 1000; // 5 minutes.
	
	private static final int BLOWFISH_KEYS = 20;
	protected byte[][] _blowfishKeys;
	protected ScrambledKeyPair[] _keyPairs;
	
	/** Authed Clients on LoginServer */
	protected Map<String, LoginClient> _loginServerClients = new ConcurrentHashMap<>();
	
	private final Map<String, Integer> _failedLoginAttemps = new HashMap<>();
	private final Map<String, Long> _bannedIps = new ConcurrentHashMap<>();
	
	private static LoginController INSTANCE;
	
	/**
	 * Generates RSA key pairs and Blowfish keys, then schedules the timed-out client purge task.
	 * @throws GeneralSecurityException if security key generation fails
	 */
	private LoginController() throws GeneralSecurityException
	{
		LOGGER.info("Loading LoginController...");
		_keyPairs = new ScrambledKeyPair[10];
		
		final KeyPairGenerator keygen = KeyPairGenerator.getInstance("RSA");
		final RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
		keygen.initialize(spec);
		
		// Generate the initial set of keys.
		for (int i = 0; i < 10; i++)
		{
			_keyPairs[i] = new ScrambledKeyPair(keygen.generateKeyPair());
		}
		
		LOGGER.info("Cached 10 KeyPairs for RSA communication.");
		
		testCipher((RSAPrivateKey) _keyPairs[0].getPrivateKey());
		
		// Store keys for blowfish communication.
		generateBlowFishKeys();
		
		// Start the client purge task.
		ThreadPool.scheduleAtFixedRate(this::purge, LOGIN_TIMEOUT, LOGIN_TIMEOUT);
	}
	
	/**
	 * Tests the {@code RSA/ECB/nopadding} cipher in decrypt mode to avoid worst-case execution.
	 * @param key the {@code RSAPrivateKey} used to initialize the cipher
	 * @throws GeneralSecurityException if cipher initialization fails
	 */
	private void testCipher(RSAPrivateKey key) throws GeneralSecurityException
	{
		// Avoid worst-case execution.
		final Cipher rsaCipher = Cipher.getInstance("RSA/ECB/nopadding");
		rsaCipher.init(Cipher.DECRYPT_MODE, key);
	}
	
	/**
	 * Populates the internal Blowfish key array with random bytes.
	 */
	private void generateBlowFishKeys()
	{
		_blowfishKeys = new byte[BLOWFISH_KEYS][16];
		
		for (int i = 0; i < BLOWFISH_KEYS; i++)
		{
			for (int j = 0; j < _blowfishKeys[i].length; j++)
			{
				_blowfishKeys[i][j] = (byte) (Rnd.get(0, 255));
			}
		}
		
		LOGGER.info("Stored " + _blowfishKeys.length + " keys for Blowfish communication.");
	}
	
	/**
	 * Returns a random Blowfish key from the internal pool.
	 * @return a {@code byte[]} containing the selected Blowfish key
	 */
	public byte[] getBlowfishKey()
	{
		return _blowfishKeys[(int) (Math.random() * BLOWFISH_KEYS)];
	}
	
	/**
	 * Creates a session key for the account and registers the client in the active list.
	 * @param account the account identifier
	 * @param client the connecting {@code LoginClient}
	 * @return the new {@code SessionKey} assigned to the session
	 */
	public SessionKey assignSessionKeyToClient(String account, LoginClient client)
	{
		final SessionKey key = new SessionKey(Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt(), Rnd.nextInt());
		_loginServerClients.put(account, client);
		return key;
	}
	
	/**
	 * Removes the authenticated client for the given account, ignoring a {@code null} account.
	 * @param account the account identifier to remove
	 */
	public void removeAuthedLoginClient(String account)
	{
		if (account == null)
		{
			return;
		}
		
		_loginServerClients.remove(account);
	}
	
	/**
	 * Returns the authenticated {@code LoginClient} for the given account.
	 * @param account the account identifier
	 * @return the {@code LoginClient} if found, {@code null} otherwise
	 */
	public LoginClient getAuthedClient(String account)
	{
		return _loginServerClients.get(account);
	}
	
	/**
	 * Returns the number of authenticated login clients currently tracked by the controller.
	 * @return the authenticated login client count
	 */
	public int getAuthenticatedClientCount()
	{
		return _loginServerClients.size();
	}
	
	/**
	 * Retrieves account info for the given credentials with auto-creation enabled.
	 * @param clientAddr the connecting client IP address
	 * @param login the account login name
	 * @param password the supplied password
	 * @return the {@code AccountInfo} for the account, or {@code null} if authentication fails
	 */
	public AccountInfo retriveAccountInfo(String clientAddr, String login, String password)
	{
		return retriveAccountInfo(clientAddr, login, password, true);
	}
	
	/**
	 * Increments the failed login count for the address and bans it once the limit is reached.
	 * @param addr the client IP address attempting to log in
	 */
	private void recordFailedLoginAttemp(String addr)
	{
		// We need to synchronize this!
		// When multiple connections from the same address fail to login at the same time, unexpected behavior can happen.
		Integer failedLoginAttemps;
		synchronized (_failedLoginAttemps)
		{
			failedLoginAttemps = _failedLoginAttemps.get(addr);
			if (failedLoginAttemps == null)
			{
				failedLoginAttemps = 1;
			}
			else
			{
				++failedLoginAttemps;
			}
			
			_failedLoginAttemps.put(addr, failedLoginAttemps);
		}
		
		if (failedLoginAttemps >= LoginConfig.LOGIN_TRY_BEFORE_BAN)
		{
			addBanForAddress(addr, LoginConfig.LOGIN_BLOCK_AFTER_BAN * 1000);
			
			// We need to clear the failed login attempts here, so after the IP ban is over the client has another 5 attempts.
			clearFailedLoginAttemps(addr);
			LOGGER.warning("Added banned address " + addr + "! Too many login attempts.");
		}
	}
	
	/**
	 * Clears the recorded failed login attempts for the given address.
	 * @param clientAddr the client IP address to clear
	 */
	private void clearFailedLoginAttemps(String clientAddr)
	{
		synchronized (_failedLoginAttemps)
		{
			_failedLoginAttemps.remove(clientAddr);
		}
	}
	
	/**
	 * Validates credentials, clears expired bans, optionally auto-creates the account, and records failures.
	 * @param clientAddr the connecting client IP address
	 * @param login the account login name
	 * @param password the plain text password
	 * @param autoCreateIfEnabled whether to create the account if it does not exist
	 * @return the {@code AccountInfo} if successful, or {@code null} if authentication fails
	 */
	private AccountInfo retriveAccountInfo(String clientAddr, String login, String password, boolean autoCreateIfEnabled)
	{
		// A field of nothing but spaces trims to empty, which would otherwise be hashed and auto created as an account.
		if (login.isEmpty() || password.isEmpty())
		{
			recordFailedLoginAttemp(clientAddr);
			return null;
		}
		
		try
		{
			final MessageDigest md = MessageDigest.getInstance("SHA");
			final byte[] raw = password.getBytes(StandardCharsets.UTF_8);
			final String hashBase64 = Base64.getEncoder().encodeToString(md.digest(raw));
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(USER_INFO_SELECT))
			{
				final long currentTime = System.currentTimeMillis();
				ps.setString(1, Long.toString(currentTime));
				ps.setString(2, login);
				try (ResultSet rset = ps.executeQuery())
				{
					if (rset.next())
					{
						final AccountInfo info = new AccountInfo(rset.getString("login"), rset.getString("password"), rset.getInt("accessLevel"), rset.getInt("lastServer"));
						
						// Clean up any expired ban_temp rows so they don't persist after the ban period ends.
						try (PreparedStatement psDel = con.prepareStatement(DELETE_EXPIRED_BAN))
						{
							psDel.setString(1, login);
							psDel.setString(2, Long.toString(currentTime));
							psDel.executeUpdate();
						}
						
						if (!info.checkPassHash(hashBase64))
						{
							// Wrong password.
							recordFailedLoginAttemp(clientAddr);
							return null;
						}
						
						clearFailedLoginAttemps(clientAddr);
						return info;
					}
				}
			}
			
			if (!autoCreateIfEnabled || !LoginConfig.AUTO_CREATE_ACCOUNTS)
			{
				// Account does not exist and auto create account is not desired.
				recordFailedLoginAttemp(clientAddr);
				return null;
			}
			
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(AUTOCREATE_ACCOUNTS_INSERT))
			{
				ps.setString(1, login);
				ps.setString(2, hashBase64);
				ps.setLong(3, System.currentTimeMillis());
				ps.setInt(4, 0);
				ps.setString(5, clientAddr);
				ps.execute();
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception while auto creating account for '" + login + "'!", e);
				return null;
			}
			
			LOGGER.info("Auto created account '" + login + "'.");
			return retriveAccountInfo(clientAddr, login, password, false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception while retriving account info for '" + login + "'!", e);
			return null;
		}
	}
	
	/**
	 * Authenticates the account and verifies it is not banned or already logged in on a login or game server.
	 * @param client the connecting {@code LoginClient}
	 * @param address the client IP address
	 * @param info the {@code AccountInfo} with user credentials and data
	 * @return a {@code LoginResult} indicating the authentication outcome
	 */
	public LoginResult tryCheckinAccount(LoginClient client, String address, AccountInfo info)
	{
		if (info.getAccessLevel() < 0)
		{
			return LoginResult.ACCOUNT_BANNED;
		}
		
		LoginResult ret = LoginResult.INVALID_PASSWORD;
		
		// Check auth.
		if (canCheckin(client, address, info))
		{
			// Login was successful, verify presence on Gameservers.
			ret = LoginResult.ALREADY_ON_GS;
			final String login = info.getLogin();
			if (!isAccountInAnyGameServer(login))
			{
				// Account is not on any GS verify LS itself.
				ret = LoginResult.ALREADY_ON_LS;
				if (_loginServerClients.putIfAbsent(login, client) == null)
				{
					ret = LoginResult.AUTH_SUCCESS;
				}
			}
		}
		
		return ret;
	}
	
	/**
	 * Bans the given IP address for the specified duration, or permanently when the duration is not positive.
	 * @param address the IP address to ban
	 * @param duration the time in milliseconds until the ban expires
	 */
	public void addBanForAddress(String address, long duration)
	{
		if (duration > 0)
		{
			_bannedIps.put(address, System.currentTimeMillis() + duration);
		}
		else // Permanent ban.
		{
			_bannedIps.put(address, Long.MAX_VALUE);
		}
	}
	
	/**
	 * Checks whether the IP address or one of its subnets is banned, removing the entry when expired.
	 * @param address the IP address to check
	 * @return {@code true} if the address is banned, {@code false} otherwise
	 */
	public boolean isBannedAddress(String address)
	{
		final String[] parts = address.split("\\.");
		Long bi = _bannedIps.get(address);
		if (bi == null)
		{
			bi = _bannedIps.get(parts[0] + "." + parts[1] + "." + parts[2] + ".0");
		}
		
		if (bi == null)
		{
			bi = _bannedIps.get(parts[0] + "." + parts[1] + ".0.0");
		}
		
		if (bi == null)
		{
			bi = _bannedIps.get(parts[0] + ".0.0.0");
		}
		
		if (bi != null)
		{
			if ((bi > 0) && (bi < System.currentTimeMillis()))
			{
				_bannedIps.remove(address);
				LOGGER.info("Removed expired ip address ban " + address + ".");
				return false;
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Returns the current banned IP addresses mapped to their expiry timestamps.
	 * @return a {@code Map<String,Long>} of IP addresses to ban expiry values
	 */
	public Map<String, Long> getBannedIps()
	{
		return _bannedIps;
	}
	
	/**
	 * Removes the given IP address from the ban list.
	 * @param address the IP address to remove from the ban list
	 * @return {@code true} if the address was removed, {@code false} otherwise
	 */
	public boolean removeBanForAddress(String address)
	{
		return _bannedIps.remove(address) != null;
	}
	
	/**
	 * Returns the session key of the active client for the given account.
	 * @param account the account identifier to look up
	 * @return the {@code SessionKey} if a client is found, {@code null} otherwise
	 */
	public SessionKey getKeyForAccount(String account)
	{
		final LoginClient client = _loginServerClients.get(account);
		if (client != null)
		{
			return client.getSessionKey();
		}
		
		return null;
	}
	
	/**
	 * Checks whether the account is logged into any registered game server.
	 * @param account the account identifier to check
	 * @return {@code true} if the account is found on any server, {@code false} otherwise
	 */
	public boolean isAccountInAnyGameServer(String account)
	{
		final Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			final GameServerThread gst = gsi.getGameServerThread();
			if ((gst != null) && gst.hasAccountOnGameServer(account))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Finds the game server on which the account is currently logged in.
	 * @param account the account login name to search for
	 * @return the matching {@code GameServerInfo}, or {@code null} if not found
	 */
	public GameServerInfo getAccountOnGameServer(String account)
	{
		final Collection<GameServerInfo> serverList = GameServerTable.getInstance().getRegisteredGameServers().values();
		for (GameServerInfo gsi : serverList)
		{
			final GameServerThread gst = gsi.getGameServerThread();
			if ((gst != null) && gst.hasAccountOnGameServer(account))
			{
				return gsi;
			}
		}
		
		return null;
	}
	
	/**
	 * Requests the account characters from every authenticated game server.
	 * @param account the account identifier
	 */
	public void getCharactersOnAccount(String account)
	{
		for (GameServerInfo gsi : GameServerTable.getInstance().getRegisteredGameServers().values())
		{
			if (gsi.isAuthed())
			{
				gsi.getGameServerThread().requestCharacters(account);
			}
		}
	}
	
	/**
	 * Checks server status and capacity for the client, updating the last used server on success.
	 * @param client the connecting {@code LoginClient}
	 * @param serverId the target game server identifier
	 * @return {@code true} if login is allowed, {@code false} otherwise
	 */
	public boolean isLoginPossible(LoginClient client, int serverId)
	{
		final GameServerInfo gsi = GameServerTable.getInstance().getRegisteredGameServerById(serverId);
		if ((gsi != null) && gsi.isAuthed())
		{
			final boolean loginOk = gsi.canLogin(client);
			if (loginOk && (client.getLastServer() != serverId))
			{
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement ps = con.prepareStatement(ACCOUNT_LAST_SERVER_UPDATE))
				{
					ps.setInt(1, serverId);
					ps.setString(2, client.getAccount());
					ps.executeUpdate();
				}
				catch (Exception e)
				{
					LOGGER.log(Level.WARNING, "Could not set lastServer: " + e.getMessage(), e);
				}
			}
			
			return loginOk;
		}
		
		return false;
	}
	
	/**
	 * Updates the access level of the given account in the database.
	 * @param account the account identifier to update
	 * @param banLevel the new access level or ban value
	 */
	public void setAccountAccessLevel(String account, int banLevel)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(ACCOUNT_ACCESS_LEVEL_UPDATE))
		{
			ps.setInt(1, banLevel);
			ps.setString(2, account);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not set accessLevel: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Saves the player IP address and first four network hops for the account in the database.
	 * @param account the account identifier
	 * @param pcIp the public IP address of the player computer
	 * @param hop1 the first network hop
	 * @param hop2 the second network hop
	 * @param hop3 the third network hop
	 * @param hop4 the fourth network hop
	 */
	public void setAccountLastTracert(String account, String pcIp, String hop1, String hop2, String hop3, String hop4)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(ACCOUNT_IPS_UPDATE))
		{
			ps.setString(1, pcIp);
			ps.setString(2, hop1);
			ps.setString(3, hop2);
			ps.setString(4, hop3);
			ps.setString(5, hop4);
			ps.setString(6, account);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not set last tracert: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Updates the character count and deletion times for the account on the given game server.
	 * @param account the account identifier
	 * @param charsNum the number of characters on this account
	 * @param timeToDel timestamps for characters scheduled for deletion
	 * @param serverId the target game server identifier
	 */
	public void setCharactersOnServer(String account, int charsNum, long[] timeToDel, int serverId)
	{
		final LoginClient client = _loginServerClients.get(account);
		if (client == null)
		{
			return;
		}
		
		if (charsNum > 0)
		{
			client.setCharsOnServ(serverId, charsNum);
		}
		
		if (timeToDel.length > 0)
		{
			client.serCharsWaitingDelOnServ(serverId, timeToDel);
		}
	}
	
	/**
	 * Returns a random RSA key pair from the internal collection.
	 * @return the selected {@code ScrambledKeyPair}
	 */
	public ScrambledKeyPair getScrambledRSAKeyPair()
	{
		return _keyPairs[Rnd.get(10)];
	}
	
	/**
	 * Validates the client address against the account IP whitelist and blacklist, updating access level and last server on success.
	 * @param client the connecting {@code LoginClient}
	 * @param address the client IP address
	 * @param info the {@code AccountInfo} with account details
	 * @return {@code true} if the checkin succeeds, {@code false} otherwise
	 */
	public boolean canCheckin(LoginClient client, String address, AccountInfo info)
	{
		try
		{
			final List<String> ipWhiteList = new ArrayList<>();
			final List<String> ipBlackList = new ArrayList<>();
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(ACCOUNT_IPAUTH_SELECT))
			{
				ps.setString(1, info.getLogin());
				try (ResultSet rset = ps.executeQuery())
				{
					String ip;
					String type;
					while (rset.next())
					{
						ip = rset.getString("ip");
						type = rset.getString("type");
						if (!isValidIPAddress(ip))
						{
							continue;
						}
						
						if (type.equals("allow"))
						{
							ipWhiteList.add(ip);
						}
						else if (type.equals("deny"))
						{
							ipBlackList.add(ip);
						}
					}
				}
			}
			
			// Check IP.
			final boolean whiteEmpty = ipWhiteList.isEmpty();
			final boolean blackEmpty = ipBlackList.isEmpty();
			if (!whiteEmpty || !blackEmpty)
			{
				if (!whiteEmpty && !ipWhiteList.contains(address))
				{
					LOGGER.warning("Account checkin attemp from address(" + address + ") not present on whitelist for account '" + info.getLogin() + "'.");
					return false;
				}
				
				if (!blackEmpty && ipBlackList.contains(address))
				{
					LOGGER.warning("Account checkin attemp from address(" + address + ") on blacklist for account '" + info.getLogin() + "'.");
					return false;
				}
			}
			
			client.setAccessLevel(info.getAccessLevel());
			client.setLastServer(info.getLastServer());
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(ACCOUNT_INFO_UPDATE))
			{
				ps.setLong(1, System.currentTimeMillis());
				ps.setString(2, address);
				ps.setString(3, info.getLogin());
				ps.execute();
			}
			
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not finish login process!", e);
			return false;
		}
	}
	
	/**
	 * Checks whether the string is a valid IPv4 address of four parts between 0 and 255.
	 * @param ipAddress the string to validate
	 * @return {@code true} if the string is a valid IP address, {@code false} otherwise
	 */
	public boolean isValidIPAddress(String ipAddress)
	{
		final String[] parts = ipAddress.split("\\.");
		if (parts.length != 4)
		{
			return false;
		}
		
		for (String s : parts)
		{
			final int i = Integer.parseInt(s);
			if ((i < 0) || (i > 255))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Closes and removes clients that have timed out without joining a game server or have disconnected.
	 */
	private void purge()
	{
		if (_loginServerClients.isEmpty())
		{
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		final Iterator<Entry<String, LoginClient>> iterator = _loginServerClients.entrySet().iterator();
		while (iterator.hasNext())
		{
			final LoginClient client = iterator.next().getValue();
			if ((!client.hasJoinedGS() && ((client.getConnectionStartTime() + LOGIN_TIMEOUT) <= currentTime)) || !client.isConnected())
			{
				client.close(LoginFailReason.REASON_ACCESS_FAILED);
				iterator.remove();
			}
		}
	}
	
	/**
	 * Initializes the singleton instance, allowing it to be loaded only once.
	 * @throws GeneralSecurityException if security key generation fails
	 */
	public static void load() throws GeneralSecurityException
	{
		synchronized (LoginController.class)
		{
			if (INSTANCE == null)
			{
				INSTANCE = new LoginController();
			}
			else
			{
				throw new IllegalStateException("LoginController can only be loaded a single time.");
			}
		}
	}
	
	/**
	 * Returns the singleton {@code LoginController} instance.
	 * @return the single {@code LoginController} instance
	 */
	public static LoginController getInstance()
	{
		return INSTANCE;
	}
}
