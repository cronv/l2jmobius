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
package ai.areas.Conquest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.ConquestConfig;
import org.l2jmobius.gameserver.data.xml.DethroneShopData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.ConquestZone;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDailyReset;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerPvPKill;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.dethrone.ExDethroneSeasonInfo;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Manages the core logic and mechanics for the conquest system.<br>
 * This class handles zone ownership, rewards, and related events within {@code ConquestZone} areas.
 * @author CostyKiller, Mobius
 */
public class ConquestEngine extends Script
{
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
	private static final ConquestPointData CONQUEST_POINT_DATA = ConquestPointData.getInstance();
	private static final int BLOODY_COIN = 80306;
	private static final ConquestZone CONQUEST_ZONE = ZoneManager.getInstance().getZoneByName("conquest", ConquestZone.class);
	private static final boolean DEBUG = false; // Set it true to display point related messages into server console.
	
	// Queries
	private static final String CLEAR_CONQUEST_PREVIOUS_RANKLIST = "DELETE FROM conquest_prev_season_ranklist";
	private static final String SAVE_CONQUEST_PREVIOUS_RANKLIST = "INSERT INTO conquest_prev_season_ranklist (charId, char_name, personal_points) SELECT a.charId, a.val, b.val FROM character_variables a, character_variables b WHERE a.var=\"CONQUEST_NAME\" AND b.var=\"CONQUEST_PERSONAL_POINTS\" AND a.charId=b.charId";
	private static final String CLEAR_CONQUEST_PREVIOUS_PLAYER_DATA = "DELETE FROM character_variables WHERE var=? OR var=?";
	
	// Variables
	private static final String CONQUEST_AVAILABLE_VAR = "CONQUEST_AVAILABLE";
	private static final String CONQUEST_CONNECTED_VAR = "CONQUEST_CONNECTED";
	private static final String CONQUEST_SERVER_POINTS_VAR = "CONQUEST_SERVER_POINTS";
	private static final String CONQUEST_PREV_SEASON_SERVER_POINTS_VAR = "CONQUEST_PREV_SEASON_SERVER_POINTS";
	private static final String CONQUEST_SERVER_SOUL_ORBS_VAR = "CONQUEST_SOUL_ORBS";
	private static final String CONQUEST_PREV_SEASON_SERVER_SOUL_ORBS_VAR = "CONQUEST_PREV_SEASON_SERVER_SOUL_ORBS";
	private static final String CONQUEST_CURRENT_CYCLE_VAR = "CONQUEST_CURRENT_CYCLE";
	private static final String CONQUEST_SEASON_END_VAR = "CONQUEST_SEASON_END";
	
	// Zone Name Vars used only for reset points
	private static final String CONQUEST_ZONE_ASA_VAR = "CONQUEST_ZONE_ASA_POINTS";
	private static final String CONQUEST_ZONE_ANIMA_VAR = "CONQUEST_ZONE_ANIMA_POINTS";
	private static final String CONQUEST_ZONE_NOX_VAR = "CONQUEST_ZONE_NOX_POINTS";
	private static final String CONQUEST_ZONE_VITA_VAR = "CONQUEST_ZONE_VITA_POINTS";
	private static final String CONQUEST_ZONE_IGNIS_VAR = "CONQUEST_ZONE_IGNIS_POINTS";
	
	// Skills
	private static final int FIRE_SOURCE_CENTER_SKILL_ID = 34490;
	
	protected static boolean _isConquestAvailable = GlobalVariablesManager.getInstance().getBoolean(CONQUEST_AVAILABLE_VAR, false);
	protected int _currentCycle;
	protected long _conquestSeasonEnd;
	
	// Tasks
	protected ScheduledFuture<?> _scheduledOpenAccessStartTask;
	protected ScheduledFuture<?> _scheduledOpenAccessEndTask;
	
	/**
	 * Initializes the {@code ConquestEngine} instance.<br>
	 * This constructor checks if the conquest system is enabled in the configuration.<br>
	 * If enabled, it calls {@code init()} and {@code load()} to start the engine.<br>
	 * If disabled, it logs a message indicating that the system is inactive.
	 */
	public ConquestEngine()
	{
		if (ConquestConfig.CONQUEST_SYSTEM_ENABLED)
		{
			init();
			load();
			DethroneShopData.getInstance();
		}
		else
		{
			String s = "=[ Conquest ]";
			while (s.length() < 61)
			{
				s = "-" + s;
			}
			
			LOGGER.info(s);
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest is disabled.");
		}
	}
	
	/**
	 * Loads the initial conquest data from the database or configuration.<br>
	 * This method initializes the current cycle and season end values.<br>
	 * It also logs the current access status and point totals to the logger.
	 */
	private void load()
	{
		// Current Conquest Status
		long millisToEnd = _isConquestAvailable ? getMillisUntilOpenAccessEnd() : getMillisUntilOpenAccessStart();
		final long numDays = millisToEnd / 86400000;
		millisToEnd %= 86400000;
		final long numHours = millisToEnd / 3600000;
		millisToEnd %= 3600000;
		final long numMins = millisToEnd / 60000;
		String status = _isConquestAvailable ? "is Open, Closes in: " : "is Closed, Opens in: ";
		
		final String simpleName = ConquestEngine.class.getSimpleName();
		LOGGER.info(simpleName + ": Conquest Access: " + status + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
		LOGGER.info(simpleName + ": Conquest Season End: " + DATE_FORMAT.format(Instant.ofEpochMilli(System.currentTimeMillis() + getMillisToConquestSeasonEnd()).atZone(java.time.ZoneId.systemDefault())));
		
		// Current Cycle Status
		boolean loaded = false;
		_currentCycle = GlobalVariablesManager.getInstance().getInt(CONQUEST_CURRENT_CYCLE_VAR, 0);
		_conquestSeasonEnd = GlobalVariablesManager.getInstance().getLong(CONQUEST_SEASON_END_VAR, 0);
		if ((_currentCycle != 0) && (_conquestSeasonEnd != 0))
		{
			loaded = true;
			LOGGER.info(simpleName + ": Conquest Cycle data loaded from database.");
		}
		
		if (!loaded)
		{
			_currentCycle = ConquestConfig.CONQUEST_CURRENT_CYCLE;
			_conquestSeasonEnd = ConquestConfig.CONQUEST_SEASON_END;
			LOGGER.info(simpleName + ": Conquest Cycle data loaded from ConquestConfig.");
		}
		
		LOGGER.info(simpleName + ": Conquest Current cycle: " + _currentCycle);
		setNewConquestSeasonEnd();
		
		// Current Points
		addKillId(CONQUEST_POINT_DATA.getPointsInfo().keySet());
		LOGGER.info(simpleName + ": Conquest Server Points: " + getServerPoints());
		LOGGER.info(simpleName + ": Conquest " + getZoneNameById(1) + " Zone points: " + getZonePoints(1));
		LOGGER.info(simpleName + ": Conquest " + getZoneNameById(2) + " Zone points: " + getZonePoints(2));
		LOGGER.info(simpleName + ": Conquest " + getZoneNameById(3) + " Zone points: " + getZonePoints(3));
		LOGGER.info(simpleName + ": Conquest " + getZoneNameById(4) + " Zone points: " + getZonePoints(4));
		LOGGER.info(simpleName + ": Conquest " + getZoneNameById(5) + " Zone points: " + getZonePoints(5));
	}
	
	/**
	 * Updates the conquest season end timestamp.<br>
	 * This method checks if a new season end needs to be calculated based on the current time or configuration.<br>
	 * It handles cycle transitions and broadcasts messages to all players when a cycle ends.<br>
	 * Database records are updated whenever the season end is refreshed.
	 */
	protected void setNewConquestSeasonEnd()
	{
		_conquestSeasonEnd = GlobalVariablesManager.getInstance().getLong(CONQUEST_SEASON_END_VAR, 0);
		_currentCycle = GlobalVariablesManager.getInstance().getInt(CONQUEST_CURRENT_CYCLE_VAR, 0);
		
		// Get the current date and time.
		LocalDateTime now = LocalDateTime.now();
		final LocalDateTime resetTime = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_RESET_TIME));
		
		// Check if conquest season end is on initial config value and set new value
		if (_conquestSeasonEnd == 0)
		{
			_conquestSeasonEnd = System.currentTimeMillis() + getMillisToConquestSeasonEnd();
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Season End was updated.");
			
			// Update db
			saveConquestCycleData();
		}
		// Check current day is first day of month or the day set in config
		else if (now.getDayOfMonth() == Integer.valueOf(ConquestConfig.CONQUEST_RESET_DAY))
		{
			// Reset Conquest Cycle data and set conquest end if conquest season end has passed
			if (now.isAfter(resetTime) && (_conquestSeasonEnd < System.currentTimeMillis()))
			{
				// Current Cycle messages
				final SystemMessage sm = new SystemMessage(SystemMessageId.THE_CONQUEST_CYCLE_S1_IS_OVER);
				sm.addInt(_currentCycle);
				World.broadcastToAllOnlinePlayers(sm);
				
				final String simpleName = ConquestEngine.class.getSimpleName();
				LOGGER.info(simpleName + ": Conquest Cycle: " + _currentCycle + " is now over.");
				
				// Update Conquest Season End
				_conquestSeasonEnd = System.currentTimeMillis() + getMillisToConquestSeasonEnd();
				LOGGER.info(simpleName + ": Conquest Season End was updated.");
				
				// Update db
				saveConquestCycleData();
				
				// Next Cycle messages
				int _nextCycle = _currentCycle + 1;
				_currentCycle = _nextCycle;
				GlobalVariablesManager.getInstance().set(CONQUEST_CURRENT_CYCLE_VAR, _nextCycle);
				GlobalVariablesManager.getInstance().storeMe();
				final SystemMessage sm2 = new SystemMessage(SystemMessageId.THE_CONQUEST_CYCLE_S1_HAS_BEGUN);
				sm2.addInt(_nextCycle);
				World.broadcastToAllOnlinePlayers(sm2);
				LOGGER.info(simpleName + ": Conquest Cycle: " + _nextCycle + " has begun.");
				resetConquestData();
			}
		}
	}
	
	/**
	 * Calculates the time remaining until the next conquest season ends.<br>
	 * This method compares the current system time against the scheduled reset date and time.<br>
	 * It accounts for custom configuration values and month-end logic.
	 * @return The number of milliseconds remaining until the season ends.
	 */
	private long getMillisToConquestSeasonEnd()
	{
		// Get the current date and time.
		LocalDateTime now = LocalDateTime.now();
		
		// Find the time of the next season end.
		long millisUntilSeasonEnd = 0;
		final int year = now.getYear();
		final int monthValue = now.getMonthValue();
		String customConfigDate = String.format("%d-%02d-%02d", year, monthValue, Integer.parseInt(ConquestConfig.CONQUEST_RESET_DAY));
		String defaultConfigDate = String.format("%d-%02d-%02d", year, monthValue, now.toLocalDate().lengthOfMonth());
		
		// default config adds one day to last day of month
		if (Integer.valueOf(ConquestConfig.CONQUEST_RESET_DAY) == 01)
		{
			final LocalDateTime endTime = LocalDateTime.of(LocalDate.parse(defaultConfigDate.trim()).plusDays(1), LocalTime.parse(ConquestConfig.CONQUEST_RESET_TIME));
			millisUntilSeasonEnd = ChronoUnit.MILLIS.between(now, endTime);
		}
		// Config value of day < current day adds a month to season end
		else if (Integer.valueOf(ConquestConfig.CONQUEST_RESET_DAY) < now.getDayOfWeek().getValue())
		{
			final LocalDateTime endTime = LocalDateTime.of(LocalDate.parse(customConfigDate.trim()).plusMonths(1), LocalTime.parse(ConquestConfig.CONQUEST_RESET_TIME));
			millisUntilSeasonEnd = ChronoUnit.MILLIS.between(now, endTime);
		}
		else
		{
			final LocalDateTime endTime = LocalDateTime.of(LocalDate.parse(customConfigDate.trim()), LocalTime.parse(ConquestConfig.CONQUEST_RESET_TIME));
			millisUntilSeasonEnd = ChronoUnit.MILLIS.between(now, endTime);
		}
		
		return millisUntilSeasonEnd;
	}
	
	/**
	 * Calculates the time remaining until the next open access window begins.<br>
	 * This method checks the current day and time against the configured conquest schedules.<br>
	 * It returns 0 if the current time is already within an active window.
	 * @return The number of milliseconds until the start of the next available window.
	 */
	private long getMillisUntilOpenAccessStart()
	{
		// Get the current date and time.
		LocalDateTime now = LocalDateTime.now();
		
		// Find the closest start time of the next available window.
		long millisUntilOpenAccessStart = 0;
		final int dayOfWeekValue = now.getDayOfWeek().getValue();
		if (ConquestConfig.CONQUEST_AVAILABLE_DAYS1.contains(dayOfWeekValue))
		{
			final LocalDateTime startTime1 = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_START_HOUR1));
			final LocalDateTime endTime1 = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_END_HOUR1));
			final LocalDateTime startTime2 = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_START_HOUR2));
			
			// Added 1 day to endTime2 for proper millis calculation
			final LocalDateTime endTime2 = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.parse(ConquestConfig.CONQUEST_END_HOUR2));
			
			if ((now.isAfter(startTime1) && now.isBefore(endTime1)) || (now.isAfter(startTime2) && now.isBefore(endTime2)))
			{
				millisUntilOpenAccessStart = 0;
			}
			else if (now.isAfter(endTime1) && now.isBefore(startTime2))
			{
				millisUntilOpenAccessStart = ChronoUnit.MILLIS.between(now, startTime2);
			}
			else if (now.isBefore(startTime1))
			{
				millisUntilOpenAccessStart = ChronoUnit.MILLIS.between(now, startTime1);
			}
		}
		else if (ConquestConfig.CONQUEST_AVAILABLE_DAYS2.contains(dayOfWeekValue))
		{
			final LocalDateTime startTime3 = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_START_HOUR3));
			
			// Added 1 day to endTime3 for proper millis calculation
			final LocalDateTime endTime3 = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.parse(ConquestConfig.CONQUEST_END_HOUR3));
			if (now.isBefore(startTime3))
			{
				millisUntilOpenAccessStart = ChronoUnit.MILLIS.between(now, startTime3);
			}
			else if (now.isAfter(startTime3) && now.isBefore(endTime3))
			{
				millisUntilOpenAccessStart = 0;
			}
		}
		
		return millisUntilOpenAccessStart;
	}
	
	/**
	 * Calculates the remaining time until the current open access window ends.<br>
	 * This method checks the current day and hour against the configuration settings.<br>
	 * It returns the difference in milliseconds between now and the end of the active period.<br>
	 * If no active window is found or if the time has already passed, it returns 0.
	 * @return the number of milliseconds remaining until the open access period ends.
	 */
	private long getMillisUntilOpenAccessEnd()
	{
		// Get the current date and time.
		LocalDateTime now = LocalDateTime.now();
		
		// Find the closest end time of the next available window.
		long millisUntilOpenAccessEnd = 0;
		final int dayOfWeekValue = now.getDayOfWeek().getValue();
		if (ConquestConfig.CONQUEST_AVAILABLE_DAYS1.contains(dayOfWeekValue))
		{
			final LocalDateTime startTime1 = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_START_HOUR1));
			final LocalDateTime endTime1 = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_END_HOUR1));
			final LocalDateTime startTime2 = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_START_HOUR2));
			final LocalDateTime endTime2 = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.parse(ConquestConfig.CONQUEST_END_HOUR2));
			
			if (now.isAfter(startTime1) && now.isBefore(endTime1))
			{
				millisUntilOpenAccessEnd = ChronoUnit.MILLIS.between(now, endTime1);
			}
			else if (now.isAfter(startTime2) && now.isBefore(endTime2))
			{
				millisUntilOpenAccessEnd = ChronoUnit.MILLIS.between(now, endTime2);
			}
			else if ((now.isAfter(endTime1) && now.isBefore(startTime2)) || (now.isBefore(startTime1)))
			{
				millisUntilOpenAccessEnd = 0;
			}
		}
		else if (ConquestConfig.CONQUEST_AVAILABLE_DAYS2.contains(dayOfWeekValue))
		{
			final LocalDateTime startTime3 = LocalDateTime.of(now.toLocalDate(), LocalTime.parse(ConquestConfig.CONQUEST_START_HOUR3));
			final LocalDateTime endTime3 = LocalDateTime.of(now.toLocalDate().plusDays(1), LocalTime.parse(ConquestConfig.CONQUEST_END_HOUR3));
			if (now.isBefore(startTime3))
			{
				millisUntilOpenAccessEnd = 0;
			}
			else if (now.isAfter(startTime3) && now.isBefore(endTime3))
			{
				millisUntilOpenAccessEnd = ChronoUnit.MILLIS.between(now, endTime3);
			}
		}
		
		return millisUntilOpenAccessEnd;
	}
	
	/**
	 * Initializes the conquest availability status.<br>
	 * This method checks if the conquest schedule is enabled in {@code ConquestConfig}.<br>
	 * If disabled, it sets the availability to {@code true}.<br>
	 * Otherwise, it updates the current access status.
	 */
	protected void init()
	{
		// Schedule not enabled means conquest is available all time
		if (!ConquestConfig.CONQUEST_SCHEDULE_ENABLED)
		{
			_isConquestAvailable = true;
			GlobalVariablesManager.getInstance().set(CONQUEST_AVAILABLE_VAR, true);
			if (!GlobalVariablesManager.getInstance().hasVariable(CONQUEST_CONNECTED_VAR))
			{
				GlobalVariablesManager.getInstance().set(CONQUEST_CONNECTED_VAR, false);
			}
			return;
		}
		
		// Start / Close Access
		updateOpenAccessStatus();
	}
	
	/**
	 * Schedules tasks to manage the opening and closing of conquest access.<br>
	 * It updates global variables and notifies all players when status changes.<br>
	 * This method also handles player teleportation when access ends.
	 */
	private synchronized void updateOpenAccessStatus()
	{
		_scheduledOpenAccessStartTask = ThreadPool.schedule(() ->
		{
			if (!ConquestConfig.CONQUEST_SYSTEM_ENABLED)
			{
				return;
			}
			
			_isConquestAvailable = true;
			GlobalVariablesManager.getInstance().set(CONQUEST_AVAILABLE_VAR, true);
			
			// Current Conquest Status
			long millisToEnd = getMillisUntilOpenAccessEnd();
			final long numDays = millisToEnd / 86400000;
			millisToEnd %= 86400000;
			final long numHours = millisToEnd / 3600000;
			millisToEnd %= 3600000;
			final long numMins = millisToEnd / 60000;
			
			// Message on console when is open
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Access: is Open, Closes in: " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
			for (Player player : World.getPlayers())
			{
				player.sendPacket(new ExDethroneSeasonInfo(true));
			}
			
			World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_PATH_TO_THE_CONQUEST_WORLD_IS_OPEN_YOU_CAN_GET_THERE_ON_MONDAYS_TUESDAYS_WEDNESDAYS_AND_THURSDAYS_FROM_10_00_TILL_12_00_AND_FROM_22_00_TILL_00_00_AND_ON_FRIDAYS_SATURDAYS_AND_SUNDAYS_FROM_20_00_TILL_00_00_PVP_IS_DISABLED_FROM_20_00_TILL_22_00_BECAUSE_THE_NEW_WORLD_EXPLORATION_IS_UNDER_WAY));
			_scheduledOpenAccessEndTask = ThreadPool.schedule(() ->
			{
				_isConquestAvailable = false;
				GlobalVariablesManager.getInstance().set(CONQUEST_AVAILABLE_VAR, false);
				if (!GlobalVariablesManager.getInstance().hasVariable(CONQUEST_CONNECTED_VAR))
				{
					GlobalVariablesManager.getInstance().set(CONQUEST_CONNECTED_VAR, false);
				}
				
				// Current Conquest Status
				long millisToEnd2 = getMillisUntilOpenAccessStart();
				final long numDays2 = millisToEnd2 / 86400000;
				millisToEnd2 %= 86400000;
				final long numHours2 = millisToEnd2 / 3600000;
				millisToEnd2 %= 3600000;
				final long numMins2 = millisToEnd2 / 60000;
				
				// Message on console when is closed
				LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Access: is Closed, Opens in: " + numDays2 + " days, " + numHours2 + " hours and " + numMins2 + " mins.");
				for (Player player : World.getPlayers())
				{
					player.sendPacket(new ExDethroneSeasonInfo(false));
				}
				
				World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_PATH_TO_THE_CONQUEST_WORLD_IS_CLOSED_YOU_CAN_GET_THERE_ON_MONDAYS_TUESDAYS_WEDNESDAYS_AND_THURSDAYS_FROM_10_00_TILL_12_00_AND_FROM_22_00_TILL_00_00_AND_ON_FRIDAYS_SATURDAYS_AND_SUNDAYS_FROM_20_00_TILL_00_00_PVP_IS_DISABLED_FROM_20_00_TILL_22_00_BECAUSE_THE_NEW_WORLD_EXPLORATION_IS_UNDER_WAY));
				
				// Teleport players to origin location if Open Access ends.
				for (Player player : CONQUEST_ZONE.getPlayersInside())
				{
					if (!player.isGM())
					{
						final PlayerVariables vars = player.getVariables();
						Location location = new Location(147458, 26903, -2206); // Aden center if no location stored
						if (vars.contains(PlayerVariables.CONQUEST_ORIGIN))
						{
							final int[] loc = vars.getIntArray(PlayerVariables.CONQUEST_ORIGIN, ";");
							if ((loc != null) && (loc.length == 3))
							{
								location = new Location(loc[0], loc[1], loc[2]);
							}
							
							player.teleToLocation(location);
							vars.remove(PlayerVariables.CONQUEST_ORIGIN);
						}
						else
						{
							player.teleToLocation(location);
						}
					}
				}
				
				init();
				
				// Reset Cycle when access closes if conditions are met
				setNewConquestSeasonEnd();
			}, getMillisUntilOpenAccessEnd());
		}, getMillisUntilOpenAccessStart());
	}
	
	/**
	 * Resets all conquest related data for the current cycle.<br>
	 * This method saves existing season data before clearing previous records.<br>
	 * It ensures that old player and server statistics are wiped from memory.
	 */
	public void resetConquestData()
	{
		saveConquestCycleData();
		saveConquestPrevSeasonRanklistData();
		saveConquestPrevSeasonServerData();
		resetConquestPrevSeasonServerData();
		resetConquestPrevSeasonPlayerData();
	}
	
	/**
	 * Saves the current conquest cycle information to the database.<br>
	 * This method updates global variables and persists the {@code ConquestEngine} state.
	 */
	public void saveConquestCycleData()
	{
		GlobalVariablesManager.getInstance().set(CONQUEST_CURRENT_CYCLE_VAR, _currentCycle);
		GlobalVariablesManager.getInstance().set(CONQUEST_SEASON_END_VAR, _conquestSeasonEnd);
		GlobalVariablesManager.getInstance().storeMe();
		LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Cycle data has been saved.");
	}
	
	/**
	 * Saves the ranking data from the previous conquest season to the database.<br>
	 * This method clears existing temporary records and persists the final results.<br>
	 * It handles {@code SQLException} by logging a severe error message.
	 */
	public void saveConquestPrevSeasonRanklistData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(CLEAR_CONQUEST_PREVIOUS_RANKLIST);
			PreparedStatement ps2 = con.prepareStatement(SAVE_CONQUEST_PREVIOUS_RANKLIST);)
		{
			ps1.execute();
			ps2.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, ConquestEngine.class.getSimpleName() + ": Failed to save previous season rank list data to database: ", e);
		}
		
		LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Previous Season ranklist data has been saved.");
	}
	
	/**
	 * Saves the server points and soul orbs from the previous season.<br>
	 * This method updates the global variables for historical tracking.
	 */
	public void saveConquestPrevSeasonServerData()
	{
		// Save previous season Server Points and Soul Orbs
		GlobalVariablesManager.getInstance().set(CONQUEST_PREV_SEASON_SERVER_POINTS_VAR, GlobalVariablesManager.getInstance().getLong(CONQUEST_SERVER_POINTS_VAR, 0));
		GlobalVariablesManager.getInstance().set(CONQUEST_PREV_SEASON_SERVER_SOUL_ORBS_VAR, GlobalVariablesManager.getInstance().getLong(CONQUEST_SERVER_SOUL_ORBS_VAR, 0));
		LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Previous Season server data has been saved.");
	}
	
	/**
	 * Resets the server-wide conquest data for the next season.<br>
	 * This method sets {@code CONQUEST_SERVER_POINTS_VAR} and {@code CONQUEST_SERVER_SOUL_ORBS_VAR} to {@code 0}.<br>
	 * It also resets the points for specific zones like ASA, Anima, and Nox.<br>
	 * Finally, it saves the updated values using {@code GlobalVariablesManager}.
	 */
	public void resetConquestPrevSeasonServerData()
	{
		// Reset Server Points and Soul Orbs for next season
		GlobalVariablesManager.getInstance().set(CONQUEST_SERVER_POINTS_VAR, 0);
		GlobalVariablesManager.getInstance().set(CONQUEST_SERVER_SOUL_ORBS_VAR, 0);
		
		// Reset Zone Points for next season
		GlobalVariablesManager.getInstance().set(CONQUEST_ZONE_ASA_VAR, 0);
		GlobalVariablesManager.getInstance().set(CONQUEST_ZONE_ANIMA_VAR, 0);
		GlobalVariablesManager.getInstance().set(CONQUEST_ZONE_NOX_VAR, 0);
		GlobalVariablesManager.getInstance().set(CONQUEST_ZONE_VITA_VAR, 0);
		GlobalVariablesManager.getInstance().set(CONQUEST_ZONE_IGNIS_VAR, 0);
		GlobalVariablesManager.getInstance().storeMe();
		LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Previous Season server data has been reset.");
	}
	
	/**
	 * Resets the personal points and rewards received for all players from the previous season.<br>
	 * This method updates offline player data in the database.<br>
	 * It also clears these variables for all currently online players.
	 */
	public void resetConquestPrevSeasonPlayerData()
	{
		// Update data for offline players.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(CLEAR_CONQUEST_PREVIOUS_PLAYER_DATA);)
		
		{
			ps.setString(1, PlayerVariables.CONQUEST_PERSONAL_POINTS);
			ps.setString(2, PlayerVariables.CONQUEST_REWARDS_RECEIVED);
			ps.execute();
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Previous Season offline players data has been reset.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not reset Conquest Previous Season player data: " + e);
		}
		
		// Update data for online players.
		for (Player player : World.getPlayers())
		{
			player.getVariables().remove(PlayerVariables.CONQUEST_PERSONAL_POINTS);
			player.getVariables().remove(PlayerVariables.CONQUEST_REWARDS_RECEIVED);
			player.getVariables().storeMe();
		}
		
		LOGGER.info(ConquestEngine.class.getSimpleName() + ": Conquest Previous Season online players data has been reset.");
	}
	
	/**
	 * Updates the conquest attack points for a specific player.<br>
	 * This method adds the provided amount to the current total.
	 * @param player The {@code Player} whose points are being updated.
	 * @param attackPoints The number of points to add to the player's total.
	 */
	public void setAttackPoints(Player player, int attackPoints)
	{
		if (DEBUG)
		{
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Attack Points updated for player: " + player.getName() + " from " + getAttackPoints(player) + " to " + (getAttackPoints(player) + attackPoints) + ".");
		}
		
		player.getVariables().set(PlayerVariables.CONQUEST_ATTACK_POINTS, getAttackPoints(player) + attackPoints);
	}
	
	/**
	 * Updates the conquest life points for a specific player.<br>
	 * This method adds the provided value to the current life points.
	 * @param player The {@code Player} object to update.
	 * @param lifePoints The amount of life points to add.
	 */
	public void setLifePoints(Player player, int lifePoints)
	{
		if (DEBUG)
		{
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Life Points updated for player: " + player.getName() + " from " + getLifePoints(player) + " to " + (getLifePoints(player) + lifePoints) + ".");
		}
		
		player.getVariables().set(PlayerVariables.CONQUEST_LIFE_POINTS, getLifePoints(player) + lifePoints);
	}
	
	/**
	 * Updates the personal points for a specific {@code Player}.<br>
	 * This method adds the provided {@code personalPoints} to the current total.
	 * @param player The {@code Player} whose points are being updated.
	 * @param personalPoints The amount of points to add to the player's total.
	 */
	public void setPersonalPoints(Player player, long personalPoints)
	{
		if (DEBUG)
		{
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Personal Points updated from " + getPersonalPoints(player) + " to " + (getPersonalPoints(player) + personalPoints) + ".");
		}
		
		player.getVariables().set(PlayerVariables.CONQUEST_PERSONAL_POINTS, getPersonalPoints(player) + personalPoints);
	}
	
	/**
	 * Updates the total server points for a specific player.<br>
	 * This method adds the provided amount to the current global server points.
	 * @param player The {@code Player} object receiving the update.
	 * @param serverPoints The amount of points to add to the total.
	 */
	public void setServerPoints(Player player, long serverPoints)
	{
		if (DEBUG)
		{
			final long currentServerPoints = getServerPoints();
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Server Points updated from " + currentServerPoints + " to " + (currentServerPoints + serverPoints) + ".");
		}
		
		GlobalVariablesManager.getInstance().set(CONQUEST_SERVER_POINTS_VAR, getServerPoints() + serverPoints);
	}
	
	/**
	 * Updates the total amount of server soul orbs.<br>
	 * This method adds the specified value to the current global count.
	 * @param player The {@code Player} object associated with this action.
	 * @param serverSoulOrbs The amount of soul orbs to add to the total.
	 */
	public void setServerSoulOrbs(Player player, long serverSoulOrbs)
	{
		if (DEBUG)
		{
			final long currentServerSoulOrbs = getServerSoulOrbs();
			LOGGER.info(ConquestEngine.class.getSimpleName() + ": Soul Orbs updated from " + currentServerSoulOrbs + " to " + (currentServerSoulOrbs + serverSoulOrbs) + ".");
		}
		
		GlobalVariablesManager.getInstance().set(CONQUEST_SERVER_SOUL_ORBS_VAR, getServerSoulOrbs() + serverSoulOrbs);
	}
	
	/**
	 * Retrieves the name of a conquest zone based on its unique identifier.<br>
	 * This method maps specific {@code int} IDs to their corresponding string names.<br>
	 * It returns {@code "NONE"} if the provided ID does not match any known zone.
	 * @param zoneId The unique identifier for the zone.
	 * @return The name of the zone as a {@code String}.
	 */
	public String getZoneNameById(int zoneId)
	{
		switch (zoneId)
		{
			case 1:
			{
				return "ASA";
			}
			case 2:
			{
				return "ANIMA";
			}
			case 3:
			{
				return "NOX";
			}
			case 4:
			{
				return "VITA";
			}
			case 5:
			{
				return "IGNIS";
			}
			default:
			{
				return "NONE";
			}
		}
	}
	
	/**
	 * Updates the points for a specific conquest zone.<br>
	 * This method adds the specified amount to the current zone total.<br>
	 * It ignores updates if the zone name is {@code NONE}.
	 * @param zoneId The unique identifier of the zone.
	 * @param zonePoints The number of points to add to the zone.
	 */
	public void setZonePoints(int zoneId, int zonePoints)
	{
		// exclusion for monsters with no zone id
		if (!getZoneNameById(zoneId).equals("NONE"))
		{
			GlobalVariablesManager.getInstance().set("CONQUEST_ZONE_" + getZoneNameById(zoneId) + "_POINTS", getZonePoints(zoneId) + zonePoints);
			if (DEBUG)
			{
				LOGGER.info(ConquestEngine.class.getSimpleName() + ": Zone Points updated for zone: " + getZoneNameById(zoneId) + " from " + getZonePoints(zoneId) + " to " + (getZonePoints(zoneId) + zonePoints) + ".");
			}
		}
	}
	
	/**
	 * Updates the conquest points for a specific player and zone.<br>
	 * This method applies optional multipliers based on server configuration.<br>
	 * It ensures that updates are thread-safe using synchronization.
	 * @param player The {@code Player} object to receive point updates.
	 * @param personalPoints The amount of personal points to add.
	 * @param serverPoints The amount of server points to add.
	 * @param serverSoulOrbs The amount of server soul orbs to add.
	 * @param zonePoints The amount of points to add to the specific zone.
	 * @param zoneId The unique identifier for the {@code ZoneId}.
	 * @param useRates A boolean flag that determines if configuration multipliers should be applied.
	 */
	public synchronized void updatePoints(Player player, int personalPoints, int serverPoints, int serverSoulOrbs, int zonePoints, int zoneId, boolean useRates)
	{
		if (!_isConquestAvailable)
		{
			return;
		}
		
		long finalPersonalPoints = personalPoints;
		long finalServerPoints = serverPoints;
		long finalServerSoulOrbs = serverSoulOrbs;
		int finalZonePoints = zonePoints;
		if (useRates)
		{
			finalPersonalPoints = personalPoints * ConquestConfig.CONQUEST_RATE_PERSONAL_POINTS;
			finalServerPoints = serverPoints * ConquestConfig.CONQUEST_RATE_SERVER_POINTS;
			finalServerSoulOrbs = serverSoulOrbs * ConquestConfig.CONQUEST_RATE_SERVER_SOUL_ORBS;
			finalZonePoints = zonePoints * ConquestConfig.CONQUEST_RATE_ZONE_POINTS;
		}
		
		setPersonalPoints(player, finalPersonalPoints);
		setServerPoints(player, finalServerPoints);
		setServerSoulOrbs(player, finalServerSoulOrbs);
		setZonePoints(zoneId, finalZonePoints);
	}
	
	/**
	 * Retrieves the current attack points for a specific {@code Player}.<br>
	 * It looks up the value from the player's variables.<br>
	 * If no value is found, it returns the default amount from {@code ConquestConfig}.
	 * @param player The {@code Player} whose attack points are being retrieved.
	 * @return The current number of attack points as an {@code int}.
	 */
	public int getAttackPoints(Player player)
	{
		return player.getVariables().getInt(PlayerVariables.CONQUEST_ATTACK_POINTS, ConquestConfig.CONQUEST_ATTACK_POINTS);
	}
	
	/**
	 * Retrieves the current life points for a specific {@code Player}.<br>
	 * It looks up the value from the player's variables.<br>
	 * If no value is found, it returns the default amount from {@code ConquestConfig}.
	 * @param player The {@code Player} whose life points are being retrieved.
	 * @return The current life points as an {@code int}.
	 */
	public int getLifePoints(Player player)
	{
		return player.getVariables().getInt(PlayerVariables.CONQUEST_LIFE_POINTS, ConquestConfig.CONQUEST_LIFE_POINTS);
	}
	
	/**
	 * Retrieves the personal conquest points for a specific player.<br>
	 * It looks up the value from the {@code PlayerVariables} collection.<br>
	 * If no value is found, it returns {@code 0}.
	 * @param player The {@code Player} whose points are being retrieved.
	 * @return The number of personal conquest points as a {@code long}.
	 */
	public long getPersonalPoints(Player player)
	{
		return player.getVariables().getLong(PlayerVariables.CONQUEST_PERSONAL_POINTS, 0);
	}
	
	/**
	 * Retrieves the current server points.<br>
	 * This value is fetched from the {@code GlobalVariablesManager}.<br>
	 * It returns a default value of {@code 0} if no data exists.
	 * @return The total number of server points as a {@code long}.
	 */
	public long getServerPoints()
	{
		return GlobalVariablesManager.getInstance().getLong(CONQUEST_SERVER_POINTS_VAR, 0);
	}
	
	/**
	 * Retrieves the current amount of server soul orbs.<br>
	 * This value is fetched from the global variables manager.
	 * @return The total number of server soul orbs as a {@code long}.
	 */
	public long getServerSoulOrbs()
	{
		return GlobalVariablesManager.getInstance().getLong(CONQUEST_SERVER_SOUL_ORBS_VAR, 0);
	}
	
	/**
	 * Retrieves the current points for a specific zone.<br>
	 * This method looks up the value from the global variables manager.<br>
	 * It returns 0 if no points are found.
	 * @param zoneId The unique identifier of the zone to check.
	 * @return The number of points associated with the zone.
	 */
	public int getZonePoints(int zoneId)
	{
		return GlobalVariablesManager.getInstance().getInt("CONQUEST_ZONE_" + getZoneNameById(zoneId) + "_POINTS", 0);
	}
	
	/**
	 * This method handles the logic when an {@code Npc} is killed.<br>
	 * It updates conquest points for the {@code Player} who dealt the killing blow.<br>
	 * Points are also awarded to nearby party members if they are alive.<br>
	 * The logic only executes if conquest is currently available.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who killed the {@code Npc}.
	 * @param isSummon Indicates whether the kill was from a summoned entity.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (_isConquestAvailable)
		{
			final int npcId = npc.getId();
			final Party party = killer.getParty();
			if (party == null)
			{
				updatePoints(killer, CONQUEST_POINT_DATA.getPersonalPointsAmount(npcId), CONQUEST_POINT_DATA.getServerPointsAmount(npcId), 1, CONQUEST_POINT_DATA.getZonePointsAmount(npcId), CONQUEST_POINT_DATA.getZoneId(npcId), true);
			}
			else
			{
				for (Player member : party.getMembers())
				{
					if (!member.isDead() && LocationUtil.checkIfInRange(1000, member, killer, true))
					{
						updatePoints(member, CONQUEST_POINT_DATA.getPersonalPointsAmount(npcId), CONQUEST_POINT_DATA.getServerPointsAmount(npcId), 1, CONQUEST_POINT_DATA.getZonePointsAmount(npcId), CONQUEST_POINT_DATA.getZoneId(npcId), true);
					}
				}
			}
		}
	}
	
	/**
	 * Handles the logic for player versus player kills within conquest zones.<br>
	 * This method awards points and items based on the target's level.<br>
	 * It also sends system messages to both the winner and the loser.
	 * @param event The {@code OnPlayerPvPKill} event containing attacker and target details.
	 */
	@RegisterEvent(EventType.ON_PLAYER_PVP_KILL)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerPvPKill(OnPlayerPvPKill event)
	{
		if (_isConquestAvailable && event.getPlayer().isInsideZone(ZoneId.CONQUEST) && event.getTarget().isInsideZone(ZoneId.CONQUEST) && !event.getPlayer().getEffectList().isAffectedBySkill(FIRE_SOURCE_CENTER_SKILL_ID))
		{
			final Player attackerPlayer = event.getPlayer();
			final Player targetPlayer = event.getTarget();
			final int targetLevel = targetPlayer.getLevel();
			if (CONQUEST_POINT_DATA.getPvpPointsInfo().containsKey(targetLevel))
			{
				updatePoints(attackerPlayer, CONQUEST_POINT_DATA.getPvpPersonalPointsAmount(targetLevel), CONQUEST_POINT_DATA.getPvpServerPointsAmount(targetLevel), 0, 0, 0, true);
				if ((getAttackPoints(attackerPlayer) >= 1) && (getLifePoints(targetPlayer) >= 1))
				{
					final int coinsAmount = CONQUEST_POINT_DATA.getCoinsAmount(targetLevel) * ConquestConfig.CONQUEST_RATE_BLOODY_COINS;
					attackerPlayer.addItem(ItemProcessType.REWARD, BLOODY_COIN, coinsAmount, attackerPlayer, true);
					setAttackPoints(attackerPlayer, -1);
					setLifePoints(targetPlayer, -1);
					
					// Looser Message
					SystemMessage sm1 = new SystemMessage(SystemMessageId.C1_HAS_DEFEATED_YOU_YOU_HAVE_LOST_1_VITALITY_POINT_CHARACTERS_GET_BLOODY_COINS_FOR_A_VICTORY_ONLY_IF_THEY_HAVE_AT_LEAST_1_VITALITY_POINT_TO_CHECK_THEIR_CURRENT_AMOUNT_ENTER_BLOODYCOIN_IN_YOUR_CHAT_WINDOW);
					sm1.addString(attackerPlayer.getName());
					targetPlayer.sendPacket(sm1);
					
					// Winner Message
					SystemMessage sm2 = new SystemMessage(SystemMessageId.YOU_HAVE_DEFEATED_C1_AND_GOT_BLOODY_COINS_X_S2_PERSONAL_CONQUEST_POINTS_X_S3_SERVER_CONQUEST_POINTS_X_S4_ATTACK_POINTS_1_TO_CHECK_THEIR_CURRENT_AMOUNT_ENTER_BLOODYCOIN_IN_YOUR_CHAT_WINDOW);
					sm2.addString(targetPlayer.getName());
					sm2.addString(Integer.toString(coinsAmount));
					sm2.addString(Long.toString(CONQUEST_POINT_DATA.getPvpPersonalPointsAmount(targetLevel)));
					sm2.addString(Long.toString(CONQUEST_POINT_DATA.getPvpServerPointsAmount(targetLevel)));
					attackerPlayer.sendPacket(sm2);
				}
			}
		}
	}
	
	/**
	 * Handles the daily reset of conquest data.<br>
	 * This method updates points for both online and offline players.<br>
	 * It also clears ability counters from the database and active sessions.
	 * @param event The {@code OnDailyReset} event object.
	 */
	@RegisterEvent(EventType.ON_DAILY_RESET)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDailyReset(OnDailyReset event)
	{
		// Update data for offline players.
		// Attack Points
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE character_variables SET val = ? WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
		{
			ps.setInt(1, ConquestConfig.CONQUEST_ATTACK_POINTS);
			ps.setString(2, PlayerVariables.CONQUEST_ATTACK_POINTS);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not reset Conquest Attack Points entries: " + e.getMessage());
		}
		
		// Life Points
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE character_variables SET val = ? WHERE var = ? AND charId IN (SELECT charId FROM characters WHERE online = 0)"))
		{
			ps.setInt(1, ConquestConfig.CONQUEST_LIFE_POINTS);
			ps.setString(2, PlayerVariables.CONQUEST_LIFE_POINTS);
			ps.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not reset Conquest Life Points entries: " + e.getMessage());
		}
		
		// Abilities Counters
		try (Connection con = DatabaseFactory.getConnection())
		{
			try (PreparedStatement ps = con.prepareStatement("DELETE FROM character_variables WHERE var IN (?, ?, ?, ?, ?, ?, ?, ?)"))
			{
				ps.setString(1, PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_RESET);
				ps.setString(2, PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_RESET);
				ps.setString(3, PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_RESET);
				ps.setString(4, PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_RESET);
				ps.setString(5, PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES);
				ps.setString(6, PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES);
				ps.setString(7, PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES);
				ps.setString(8, PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES);
				ps.execute();
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not reset Ability counter entries: " + e.getMessage());
		}
		
		// Update data for online players.
		// Attack and Life Points
		for (Player player : World.getPlayers())
		{
			final PlayerVariables vars = player.getVariables();
			vars.set(PlayerVariables.CONQUEST_ATTACK_POINTS, ConquestConfig.CONQUEST_ATTACK_POINTS);
			vars.set(PlayerVariables.CONQUEST_LIFE_POINTS, ConquestConfig.CONQUEST_LIFE_POINTS);
		}
		
		final String className = getClass().getSimpleName();
		LOGGER.info(className + ": Player points entries have been reset.");
		
		// Abilities Counters
		for (Player player : World.getPlayers())
		{
			final PlayerVariables vars = player.getVariables();
			vars.remove(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_RESET);
			vars.remove(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_RESET);
			vars.remove(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_RESET);
			vars.remove(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_RESET);
			vars.remove(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES);
			vars.remove(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES);
			vars.remove(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES);
			vars.remove(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES);
		}
		
		LOGGER.info(className + ": Ability counter entries have been reset.");
	}
	
	/**
	 * This is the main entry point for the application.<br>
	 * It initializes a new instance of {@code ConquestEngine}.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new ConquestEngine();
	}
}
