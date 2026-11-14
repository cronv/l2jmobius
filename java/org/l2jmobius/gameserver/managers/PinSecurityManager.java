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
package org.l2jmobius.gameserver.managers;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.custom.PinSecurityConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerBypass;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.AbstractEventListener;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.AbstractEventReturn;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.network.ConnectionState;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Manages in-game PIN security that blocks gameplay until a player sets up or verifies a valid PIN.
 * @author BazookaRpm
 */
public class PinSecurityManager
{
	private static final Logger LOGGER = Logger.getLogger(PinSecurityManager.class.getName());
	
	// SQL Queries.
	private static final String SELECT_PIN = "SELECT pin_hash,pin_salt,failed_attempts,locked_until FROM pin_security WHERE pin_scope=? AND account_name=? AND character_id=?";
	private static final String INSERT_PIN = "INSERT INTO pin_security (pin_scope,account_name,character_id,pin_hash,pin_salt,failed_attempts,locked_until,created_at,updated_at,last_attempt) VALUES (?,?,?,?,?,0,0,?,?,0)";
	private static final String UPDATE_ATTEMPTS = "UPDATE pin_security SET failed_attempts=?,locked_until=?,last_attempt=?,updated_at=? WHERE pin_scope=? AND account_name=? AND character_id=?";
	private static final String RESET_ATTEMPTS = "UPDATE pin_security SET failed_attempts=0,locked_until=0,last_attempt=?,updated_at=? WHERE pin_scope=? AND account_name=? AND character_id=?";
	
	// Commands.
	private static final String COMMAND_ENTER = "pinsecurity_enter";
	private static final String COMMAND_SETUP = "pinsecurity_setup";
	private static final String COMMAND_REFRESH = "pinsecurity_refresh";
	private static final String COMMAND_PREFIX = "pinsecurity_";
	
	// HTML.
	private static final String ENTER_HTML = "data/html/mods/PinSecurity/enter.htm";
	private static final String LOCKED_HTML = "data/html/mods/PinSecurity/locked.htm";
	private static final String SETUP_HTML = "data/html/mods/PinSecurity/setup.htm";
	
	// Security.
	private static final int SALT_BYTES = 24;
	private static final int HASH_BITS = 256;
	private static final long HTML_RESEND_DELAY = 2000;
	private static final TerminateReturn TERMINATE_RETURN = new TerminateReturn(true, true, true);
	private static final SecureRandom RANDOM = new SecureRandom();
	
	// Runtime sessions.
	private final Map<Integer, PinSecuritySession> _lockedPlayers = new ConcurrentHashMap<>();
	
	/**
	 * Creates the {@code PinSecurityManager}, registering event listeners only when {@code PinSecurityConfig.ENABLED} is set.
	 */
	protected PinSecurityManager()
	{
		if (!PinSecurityConfig.ENABLED)
		{
			return;
		}
		
		registerListeners();
	}
	
	/**
	 * Registers the {@code ON_PLAYER_BYPASS} and {@code ON_PLAYER_LOGOUT} listeners on the global container.
	 */
	private void registerListeners()
	{
		final AbstractEventListener bypassListener = new AbstractEventListener(Containers.Global(), EventType.ON_PLAYER_BYPASS, this)
		{
			@Override
			public <R extends AbstractEventReturn> R executeEvent(IBaseEvent event, Class<R> returnBackClass)
			{
				final TerminateReturn result = onPlayerBypass((OnPlayerBypass) event);
				return (result == null) || (returnBackClass == null) ? null : returnBackClass.cast(result);
			}
		};
		bypassListener.setPriority(10000);
		
		final ListenersContainer global = Containers.Global();
		global.addListener(bypassListener);
		global.addListener(new ConsumerEventListener(global, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
	}
	
	/**
	 * Runs the entry security check, locking the player to set up a new PIN or verify an existing one.
	 * @param player The {@code Player} entering the world.
	 */
	public void onEnterWorld(Player player)
	{
		if (!PinSecurityConfig.ENABLED || (player == null))
		{
			return;
		}
		
		final GameClient client = player.getClient();
		if (client == null)
		{
			return;
		}
		
		try
		{
			final PinSecurityKey key = createKey(player);
			final PinSecurityData pinData = loadPinData(key);
			if (pinData == null)
			{
				lockPlayer(player, key, null, true, "Create a security PIN to unlock your character.");
				return;
			}
			
			lockPlayer(player, key, pinData, false, "Enter your security PIN to unlock your character.");
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, "Could not load PIN security data for player " + player.getObjectId() + ".", e);
			lockPlayer(player, createKey(player), null, false, "PIN security database is unavailable. Contact an administrator.");
		}
	}
	
	/**
	 * Checks whether a player currently holds an active lock session.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is locked, {@code false} otherwise.
	 */
	public boolean isLocked(Player player)
	{
		return (player != null) && _lockedPlayers.containsKey(player.getObjectId());
	}
	
	/**
	 * Re-shows the security window for a locked player, throttled by {@code HTML_RESEND_DELAY}.
	 * @param player The {@code Player} to show the window to.
	 */
	public void showLockWindow(Player player)
	{
		final PinSecuritySession session = getSession(player);
		if (session == null)
		{
			return;
		}
		
		final long now = System.currentTimeMillis();
		if ((now - session.getLastHtmlTime()) < HTML_RESEND_DELAY)
		{
			return;
		}
		
		session.setLastHtmlTime(now);
		showSessionWindow(player, session, session.getMessage());
	}
	
	/**
	 * Routes a bypass event through {@code handleBypass}, returning the terminate result when handled.
	 * @param event The {@code OnPlayerBypass} event.
	 * @return The shared {@code TerminateReturn} if the command was handled, {@code null} otherwise.
	 */
	private TerminateReturn onPlayerBypass(OnPlayerBypass event)
	{
		return handleBypass(event.getPlayer(), event.getCommand()) ? TERMINATE_RETURN : null;
	}
	
	/**
	 * Processes a bypass command, blocking non-PIN commands while the player is locked and routing PIN commands.
	 * @param player The {@code Player} sending the command.
	 * @param command The raw command {@code String}.
	 * @return {@code true} if the command was handled or blocked by security, {@code false} otherwise.
	 */
	public boolean handleBypass(Player player, String command)
	{
		if ((player == null) || (command == null))
		{
			return false;
		}
		
		final boolean pinCommand = command.startsWith(COMMAND_PREFIX);
		if (!isLocked(player))
		{
			return pinCommand;
		}
		
		if (!pinCommand)
		{
			showLockWindow(player);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return true;
		}
		
		if (command.startsWith(COMMAND_ENTER))
		{
			handleEnterCommand(player, command);
			return true;
		}
		
		if (command.startsWith(COMMAND_SETUP))
		{
			handleSetupCommand(player, command);
			return true;
		}
		
		if (command.equals(COMMAND_REFRESH))
		{
			showSessionWindow(player, getSession(player), "Enter your security PIN to unlock your character.");
			return true;
		}
		
		showLockWindow(player);
		return true;
	}
	
	/**
	 * Unlocks the player on logout without restoring state.
	 * @param event The {@code OnPlayerLogout} event.
	 */
	private void onPlayerLogout(OnPlayerLogout event)
	{
		unlockPlayer(event.getPlayer(), false);
	}
	
	/**
	 * Validates the entered PIN, unlocking the player on success or recording a failed attempt otherwise.
	 * @param player The {@code Player} entering the PIN.
	 * @param command The raw command {@code String} carrying the PIN.
	 */
	private void handleEnterCommand(Player player, String command)
	{
		final PinSecuritySession session = getSession(player);
		if ((session == null) || (session.getPinData() == null))
		{
			showSessionWindow(player, session, "PIN security is unavailable. Contact an administrator.");
			return;
		}
		
		final long now = System.currentTimeMillis();
		if (session.getPinData().getLockedUntil() > now)
		{
			showSessionWindow(player, session, "Too many failed attempts. Try again later.");
			return;
		}
		
		final String pin = getCommandParameter(command, COMMAND_ENTER);
		if (!isValidPin(pin))
		{
			showSessionWindow(player, session, "Invalid PIN format.");
			return;
		}
		
		try
		{
			if (isPinValid(pin, session.getPinData()))
			{
				resetAttempts(session.getKey());
				unlockPlayer(player, true);
				player.sendMessage("Security PIN accepted.");
				return;
			}
			
			registerFailedAttempt(session);
			showSessionWindow(player, session, getFailedAttemptMessage(session));
		}
		catch (GeneralSecurityException | SQLException e)
		{
			LOGGER.log(Level.WARNING, "Could not validate PIN security data for player " + player.getObjectId() + ".", e);
			showSessionWindow(player, session, "PIN security validation failed. Contact an administrator.");
		}
	}
	
	/**
	 * Validates a new PIN and its confirmation, creating it and unlocking the player on success.
	 * @param player The {@code Player} setting up the PIN.
	 * @param command The raw command {@code String} carrying the new PIN and confirmation.
	 */
	private void handleSetupCommand(Player player, String command)
	{
		final PinSecuritySession session = getSession(player);
		if (session == null)
		{
			return;
		}
		
		if (!session.isSetupRequired())
		{
			showSessionWindow(player, session, "Enter your security PIN to unlock your character.");
			return;
		}
		
		final StringTokenizer tokenizer = new StringTokenizer(getCommandParameter(command, COMMAND_SETUP));
		if (tokenizer.countTokens() != 2)
		{
			showSessionWindow(player, session, "Enter and confirm your new PIN.");
			return;
		}
		
		final String pin = tokenizer.nextToken();
		final String confirmation = tokenizer.nextToken();
		if (!pin.equals(confirmation))
		{
			showSessionWindow(player, session, "PIN confirmation does not match.");
			return;
		}
		
		if (!isValidPin(pin))
		{
			showSessionWindow(player, session, "Invalid PIN format.");
			return;
		}
		
		try
		{
			createPin(session.getKey(), pin);
			unlockPlayer(player, true);
			player.sendMessage("Security PIN created.");
		}
		catch (GeneralSecurityException | SQLException e)
		{
			LOGGER.log(Level.WARNING, "Could not create PIN security data for player " + player.getObjectId() + ".", e);
			showSessionWindow(player, session, "PIN security setup failed. Contact an administrator.");
		}
	}
	
	/**
	 * Locks a player by creating a session, disabling the client connection, and immobilizing them.
	 * @param player The {@code Player} to lock.
	 * @param key The {@code PinSecurityKey} for the session.
	 * @param pinData The {@code PinSecurityData} for the session, or {@code null}.
	 * @param setupRequired {@code true} if a new PIN setup is required.
	 * @param message The message to display.
	 */
	private void lockPlayer(Player player, PinSecurityKey key, PinSecurityData pinData, boolean setupRequired, String message)
	{
		final GameClient client = player.getClient();
		if (client == null)
		{
			return;
		}
		
		final PinSecuritySession session = new PinSecuritySession(key, pinData, setupRequired, message);
		_lockedPlayers.put(player.getObjectId(), session);
		client.setConnectionState(ConnectionState.DISABLED);
		player.abortAttack();
		player.abortCast();
		player.stopMove(null);
		player.setBlockActions(true);
		player.setImmobilized(true);
		showSessionWindow(player, session, message);
	}
	
	/**
	 * Removes the player from the locked list and restores the {@code IN_GAME} state.
	 * @param player The {@code Player} to unlock.
	 * @param restoreState {@code true} to restore movement and actions.
	 */
	private void unlockPlayer(Player player, boolean restoreState)
	{
		if (player == null)
		{
			return;
		}
		
		final PinSecuritySession session = _lockedPlayers.remove(player.getObjectId());
		final GameClient client = player.getClient();
		if (client != null)
		{
			client.setConnectionState(ConnectionState.IN_GAME);
		}
		
		if ((session != null) && restoreState)
		{
			player.setBlockActions(false);
			player.setImmobilized(false);
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Sends the security HTML window, substituting the message, length bounds, and remaining lock time.
	 * @param player The {@code Player} to receive the window.
	 * @param session The {@code PinSecuritySession} with current data.
	 * @param message The text to display.
	 */
	private void showSessionWindow(Player player, PinSecuritySession session, String message)
	{
		if ((player == null) || (session == null))
		{
			return;
		}
		
		session.setLastHtmlTime(System.currentTimeMillis());
		final String file = getHtmlFile(session);
		final NpcHtmlMessage html = new NpcHtmlMessage(0);
		html.setFile(player, file);
		html.replace("%message%", message == null ? "" : message);
		html.replace("%min%", PinSecurityConfig.MIN_LENGTH);
		html.replace("%max%", PinSecurityConfig.MAX_LENGTH);
		html.replace("%remaining%", getRemainingLockSeconds(session));
		player.sendPacket(html);
	}
	
	/**
	 * Selects the locked, enter, or setup HTML file for the current session state.
	 * @param session The {@code PinSecuritySession} to inspect.
	 * @return The HTML file path as a {@code String}.
	 */
	private String getHtmlFile(PinSecuritySession session)
	{
		if (session.getPinData() != null)
		{
			if (session.getPinData().getLockedUntil() > System.currentTimeMillis())
			{
				return LOCKED_HTML;
			}
			return ENTER_HTML;
		}
		
		return session.isSetupRequired() ? SETUP_HTML : LOCKED_HTML;
	}
	
	/**
	 * Builds the failed-entry message, reporting either a lockout or the remaining attempts.
	 * @param session The {@code PinSecuritySession} to inspect.
	 * @return The error message as a {@code String}.
	 */
	private String getFailedAttemptMessage(PinSecuritySession session)
	{
		final PinSecurityData pinData = session.getPinData();
		if (pinData.getLockedUntil() > System.currentTimeMillis())
		{
			return "Too many failed attempts. Try again later.";
		}
		
		return "Wrong PIN. Remaining attempts: " + Math.max(0, PinSecurityConfig.MAX_ATTEMPTS - pinData.getFailedAttempts()) + ".";
	}
	
	/**
	 * Computes the seconds remaining until the lock expires, or 0 if expired or unavailable.
	 * @param session The {@code PinSecuritySession} to inspect.
	 * @return The remaining time in seconds as a {@code long}.
	 */
	private long getRemainingLockSeconds(PinSecuritySession session)
	{
		if ((session == null) || (session.getPinData() == null))
		{
			return 0;
		}
		
		final long remaining = session.getPinData().getLockedUntil() - System.currentTimeMillis();
		return TimeUtil.roundUpToSecond(remaining) / 1000;
	}
	
	/**
	 * Strips the command name from a command string and returns the trimmed remainder.
	 * @param command The full command {@code String}.
	 * @param commandName The command name to remove.
	 * @return The extracted parameter, or an empty {@code String}.
	 */
	private String getCommandParameter(String command, String commandName)
	{
		final int commandNameLength = commandName.length();
		if (command.length() <= commandNameLength)
		{
			return "";
		}
		
		return command.substring(commandNameLength).trim();
	}
	
	/**
	 * Checks that the PIN is non-null, numeric, and within the configured length bounds.
	 * @param pin The PIN {@code String} to validate.
	 * @return {@code true} if the PIN is valid, {@code false} otherwise.
	 */
	private boolean isValidPin(String pin)
	{
		return (pin != null) && (pin.length() >= PinSecurityConfig.MIN_LENGTH) && (pin.length() <= PinSecurityConfig.MAX_LENGTH) && StringUtil.isNumeric(pin);
	}
	
	/**
	 * Looks up the lock session for a player by object id.
	 * @param player The {@code Player} to look up.
	 * @return The {@code PinSecuritySession}, or {@code null} if not locked.
	 */
	private PinSecuritySession getSession(Player player)
	{
		return player == null ? null : _lockedPlayers.get(player.getObjectId());
	}
	
	/**
	 * Builds a {@code PinSecurityKey} from the configured scope, the lowercased account name, and an optional character id.
	 * @param player The {@code Player} to derive the key from.
	 * @return A new {@code PinSecurityKey}.
	 */
	private PinSecurityKey createKey(Player player)
	{
		final String scope = PinSecurityConfig.SCOPE;
		final String accountName = player.getAccountName().toLowerCase(Locale.ROOT);
		final int characterId = PinSecurityConfig.SCOPE_CHARACTER.equals(scope) ? player.getObjectId() : 0;
		return new PinSecurityKey(scope, accountName, characterId);
	}
	
	/**
	 * Loads the stored PIN record for a key from the database.
	 * @param key The {@code PinSecurityKey} to query.
	 * @return A {@code PinSecurityData}, or {@code null} if no record exists.
	 * @throws SQLException If a database access error occurs.
	 */
	private PinSecurityData loadPinData(PinSecurityKey key) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_PIN))
		{
			setKey(statement, key);
			try (ResultSet result = statement.executeQuery())
			{
				if (result.next())
				{
					return new PinSecurityData(result.getString("pin_hash"), result.getString("pin_salt"), result.getInt("failed_attempts"), result.getLong("locked_until"));
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Hashes the PIN with a fresh salt and inserts a new record into the database.
	 * @param key The {@code PinSecurityKey} for the record.
	 * @param pin The plain-text PIN {@code String} to hash and store.
	 * @throws GeneralSecurityException If hashing fails.
	 * @throws SQLException If a database access error occurs.
	 */
	private void createPin(PinSecurityKey key, String pin) throws GeneralSecurityException, SQLException
	{
		final long now = System.currentTimeMillis();
		final String salt = createSalt();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(INSERT_PIN))
		{
			setKey(statement, key);
			statement.setString(4, hashPin(pin, salt));
			statement.setString(5, salt);
			statement.setLong(6, now);
			statement.setLong(7, now);
			statement.executeUpdate();
		}
	}
	
	/**
	 * Increments the failure count, applying a lockout once {@code MAX_ATTEMPTS} is reached, and persists it.
	 * @param session The {@code PinSecuritySession} to update.
	 * @throws SQLException If a database access error occurs.
	 */
	private void registerFailedAttempt(PinSecuritySession session) throws SQLException
	{
		final PinSecurityData pinData = session.getPinData();
		final long now = System.currentTimeMillis();
		final int failedAttempts = pinData.getFailedAttempts() + 1;
		final long lockedUntil = failedAttempts >= PinSecurityConfig.MAX_ATTEMPTS ? now + (PinSecurityConfig.LOCK_MINUTES * 60000L) : 0;
		pinData.setFailedAttempts(failedAttempts);
		pinData.setLockedUntil(lockedUntil);
		updateAttempts(session.getKey(), failedAttempts, lockedUntil, now);
	}
	
	/**
	 * Persists the failed-attempt count and lock timestamp for a key.
	 * @param key The {@code PinSecurityKey} to update.
	 * @param failedAttempts The failed attempt count.
	 * @param lockedUntil The unlock timestamp.
	 * @param now The current time used for the update.
	 * @throws SQLException If a database access error occurs.
	 */
	private void updateAttempts(PinSecurityKey key, int failedAttempts, long lockedUntil, long now) throws SQLException
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(UPDATE_ATTEMPTS))
		{
			statement.setInt(1, failedAttempts);
			statement.setLong(2, lockedUntil);
			statement.setLong(3, now);
			statement.setLong(4, now);
			setKey(statement, key, 5);
			statement.executeUpdate();
		}
	}
	
	/**
	 * Clears the failed-attempt count and lock for a key in the database.
	 * @param key The {@code PinSecurityKey} to reset.
	 * @throws SQLException If a database access error occurs.
	 */
	private void resetAttempts(PinSecurityKey key) throws SQLException
	{
		final long now = System.currentTimeMillis();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(RESET_ATTEMPTS))
		{
			statement.setLong(1, now);
			statement.setLong(2, now);
			setKey(statement, key, 3);
			statement.executeUpdate();
		}
	}
	
	/**
	 * Binds the key fields to a statement starting at index 1.
	 * @param statement The {@code PreparedStatement} to bind.
	 * @param key The {@code PinSecurityKey} to bind.
	 * @throws SQLException If a database access error occurs.
	 */
	private void setKey(PreparedStatement statement, PinSecurityKey key) throws SQLException
	{
		setKey(statement, key, 1);
	}
	
	/**
	 * Binds the scope, account name, and character id to a statement from the given index.
	 * @param statement The {@code PreparedStatement} to bind.
	 * @param key The {@code PinSecurityKey} to bind.
	 * @param startIndex The first parameter index.
	 * @throws SQLException If a database access error occurs.
	 */
	private void setKey(PreparedStatement statement, PinSecurityKey key, int startIndex) throws SQLException
	{
		statement.setString(startIndex, key.getScope());
		statement.setString(startIndex + 1, key.getAccountName());
		statement.setInt(startIndex + 2, key.getCharacterId());
	}
	
	/**
	 * Hashes the PIN with the stored salt and compares it to the stored hash in constant time.
	 * @param pin The plain-text PIN {@code String} to verify.
	 * @param pinData The {@code PinSecurityData} holding the salt and hash.
	 * @return {@code true} if the PIN matches, {@code false} otherwise.
	 * @throws GeneralSecurityException If hashing fails.
	 */
	private boolean isPinValid(String pin, PinSecurityData pinData) throws GeneralSecurityException
	{
		return MessageDigest.isEqual(hashPin(pin, pinData.getSalt()).getBytes(StandardCharsets.UTF_8), pinData.getHash().getBytes(StandardCharsets.UTF_8));
	}
	
	/**
	 * Generates a random {@code SALT_BYTES}-byte salt encoded as Base64.
	 * @return The Base64-encoded salt {@code String}.
	 */
	private String createSalt()
	{
		final byte[] salt = new byte[SALT_BYTES];
		RANDOM.nextBytes(salt);
		return Base64.getEncoder().encodeToString(salt);
	}
	
	/**
	 * Derives a Base64 PBKDF2 hash from the PIN and salt using the configured iteration count.
	 * @param pin The plain-text PIN {@code String}.
	 * @param salt The Base64-encoded salt {@code String}.
	 * @return The Base64-encoded hash {@code String}.
	 * @throws GeneralSecurityException If hashing fails.
	 */
	private String hashPin(String pin, String salt) throws GeneralSecurityException
	{
		final char[] password = pin.toCharArray();
		final byte[] decodedSalt = Base64.getDecoder().decode(salt);
		final PBEKeySpec spec = new PBEKeySpec(password, decodedSalt, PinSecurityConfig.HASH_ITERATIONS, HASH_BITS);
		try
		{
			return Base64.getEncoder().encodeToString(getSecretKeyFactory().generateSecret(spec).getEncoded());
		}
		finally
		{
			spec.clearPassword();
		}
	}
	
	/**
	 * Returns a PBKDF2 {@code SecretKeyFactory}, preferring HmacSHA256 and falling back to HmacSHA1.
	 * @return A {@code SecretKeyFactory} instance.
	 * @throws NoSuchAlgorithmException If neither algorithm is available.
	 */
	private SecretKeyFactory getSecretKeyFactory() throws NoSuchAlgorithmException
	{
		try
		{
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
		}
		catch (NoSuchAlgorithmException e)
		{
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
		}
	}
	
	private static class PinSecurityKey
	{
		private final String _scope;
		private final String _accountName;
		private final int _characterId;
		
		PinSecurityKey(String scope, String accountName, int characterId)
		{
			_scope = scope;
			_accountName = accountName;
			_characterId = characterId;
		}
		
		String getScope()
		{
			return _scope;
		}
		
		String getAccountName()
		{
			return _accountName;
		}
		
		int getCharacterId()
		{
			return _characterId;
		}
	}
	
	private static class PinSecurityData
	{
		private final String _hash;
		private final String _salt;
		private int _failedAttempts;
		private long _lockedUntil;
		
		PinSecurityData(String hash, String salt, int failedAttempts, long lockedUntil)
		{
			_hash = hash;
			_salt = salt;
			_failedAttempts = failedAttempts;
			_lockedUntil = lockedUntil;
		}
		
		String getHash()
		{
			return _hash;
		}
		
		String getSalt()
		{
			return _salt;
		}
		
		int getFailedAttempts()
		{
			return _failedAttempts;
		}
		
		void setFailedAttempts(int failedAttempts)
		{
			_failedAttempts = failedAttempts;
		}
		
		long getLockedUntil()
		{
			return _lockedUntil;
		}
		
		void setLockedUntil(long lockedUntil)
		{
			_lockedUntil = lockedUntil;
		}
	}
	
	private static class PinSecuritySession
	{
		private final PinSecurityKey _key;
		private final PinSecurityData _pinData;
		private final boolean _setupRequired;
		private final String _message;
		private volatile long _lastHtmlTime;
		
		PinSecuritySession(PinSecurityKey key, PinSecurityData pinData, boolean setupRequired, String message)
		{
			_key = key;
			_pinData = pinData;
			_setupRequired = setupRequired;
			_message = message;
		}
		
		PinSecurityKey getKey()
		{
			return _key;
		}
		
		PinSecurityData getPinData()
		{
			return _pinData;
		}
		
		boolean isSetupRequired()
		{
			return _setupRequired;
		}
		
		String getMessage()
		{
			return _message;
		}
		
		long getLastHtmlTime()
		{
			return _lastHtmlTime;
		}
		
		void setLastHtmlTime(long lastHtmlTime)
		{
			_lastHtmlTime = lastHtmlTime;
		}
	}
	
	/**
	 * Retrieves the singleton {@code PinSecurityManager} instance.
	 * @return The single {@code PinSecurityManager} instance.
	 */
	public static PinSecurityManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PinSecurityManager INSTANCE = new PinSecurityManager();
	}
}
