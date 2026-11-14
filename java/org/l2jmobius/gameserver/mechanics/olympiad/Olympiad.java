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
package org.l2jmobius.gameserver.mechanics.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadInfo;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the core logic and lifecycle of the Olympiad system.<br>
 * This class handles event registration, competition rules, and player participation.<br>
 * It coordinates interactions between players, categories, and rewards.
 * @author godson, Mobius
 */
public class Olympiad extends ListenersContainer
{
	protected static final Logger LOGGER = Logger.getLogger(Olympiad.class.getName());
	protected static final Logger LOGGER_OLYMPIAD = Logger.getLogger("olympiad");
	
	private static final Map<Integer, StatSet> NOBLES = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> NOBLES_RANK = new HashMap<>();
	
	public static final String OLYMPIAD_HTML_PATH = "data/html/olympiad/";
	private static final String OLYMPIAD_LOAD_DATA = "SELECT current_cycle, period, olympiad_end, validation_end, next_weekly_change FROM olympiad_data WHERE id = 0";
	private static final String OLYMPIAD_SAVE_DATA = "INSERT INTO olympiad_data (id, current_cycle, period, olympiad_end, validation_end, next_weekly_change) VALUES (0,?,?,?,?,?) ON DUPLICATE KEY UPDATE current_cycle=?, period=?, olympiad_end=?, validation_end=?, next_weekly_change=?";
	private static final String OLYMPIAD_LOAD_NOBLES = "SELECT olympiad_nobles.charId, olympiad_nobles.class_id, characters.char_name, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_done, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn, olympiad_nobles.competitions_done_week FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId";
	private static final String OLYMPIAD_SAVE_NOBLES = "INSERT INTO olympiad_nobles (`charId`,`class_id`,`olympiad_points`,`competitions_done`,`competitions_won`,`competitions_lost`,`competitions_drawn`, `competitions_done_week`) VALUES (?,?,?,?,?,?,?,?)";
	private static final String OLYMPIAD_UPDATE_NOBLES = "UPDATE olympiad_nobles SET olympiad_points = ?, competitions_done = ?, competitions_won = ?, competitions_lost = ?, competitions_drawn = ?, competitions_done_week = ? WHERE charId = ?";
	private static final String OLYMPIAD_GET_HEROS = "SELECT olympiad_nobles.charId, characters.char_name FROM olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= " + OlympiadConfig.OLYMPIAD_MIN_MATCHES + " AND olympiad_nobles.competitions_won > 0 ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_won DESC, (olympiad_nobles.competitions_won / olympiad_nobles.competitions_done) DESC";
	private static final String OLYMPIAD_GET_LEGEND = "SELECT olympiad_nobles.charId FROM olympiad_nobles WHERE olympiad_nobles.competitions_done >=" + OlympiadConfig.OLYMPIAD_MIN_MATCHES + " ORDER BY olympiad_nobles.olympiad_points DESC LIMIT 1";
	private static final String GET_ALL_CLASSIFIED_NOBLESS = "SELECT charId from olympiad_nobles_eom WHERE competitions_done >= " + OlympiadConfig.OLYMPIAD_MIN_MATCHES + " ORDER BY olympiad_points DESC, competitions_done DESC, competitions_won DESC";
	private static final String GET_EACH_CLASS_LEADER = "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND olympiad_nobles_eom.class_id = ? AND olympiad_nobles_eom.competitions_done >= " + OlympiadConfig.OLYMPIAD_MIN_MATCHES + " ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_CURRENT = "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND olympiad_nobles.class_id = ? AND olympiad_nobles.competitions_done >= " + OlympiadConfig.OLYMPIAD_MIN_MATCHES + " ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_SOULHOUND = "SELECT characters.char_name from olympiad_nobles_eom, characters WHERE characters.charId = olympiad_nobles_eom.charId AND (olympiad_nobles_eom.class_id = ? OR olympiad_nobles_eom.class_id = " + PlayerClass.FEMALE_SOUL_HOUND.getId() + ") AND olympiad_nobles_eom.competitions_done >= " + OlympiadConfig.OLYMPIAD_MIN_MATCHES + " ORDER BY olympiad_nobles_eom.olympiad_points DESC, olympiad_nobles_eom.competitions_done DESC, olympiad_nobles_eom.competitions_won DESC LIMIT 10";
	private static final String GET_EACH_CLASS_LEADER_CURRENT_SOULHOUND = "SELECT characters.char_name from olympiad_nobles, characters WHERE characters.charId = olympiad_nobles.charId AND (olympiad_nobles.class_id = ? OR olympiad_nobles.class_id = " + PlayerClass.FEMALE_SOUL_HOUND.getId() + ") AND olympiad_nobles.competitions_done >= " + OlympiadConfig.OLYMPIAD_MIN_MATCHES + " ORDER BY olympiad_nobles.olympiad_points DESC, olympiad_nobles.competitions_done DESC, olympiad_nobles.competitions_won DESC LIMIT 10";
	
	private static final String REMOVE_UNCLAIMED_POINTS = "DELETE FROM character_variables WHERE charId=? AND var=?";
	private static final String INSERT_UNCLAIMED_POINTS = "INSERT INTO character_variables (charId, var, val) VALUES (?, ?, ?)";
	public static final String UNCLAIMED_OLYMPIAD_POINTS_VAR = "UNCLAIMED_OLYMPIAD_POINTS";
	
	private static final String OLYMPIAD_DELETE_ALL = "TRUNCATE olympiad_nobles";
	private static final String OLYMPIAD_MONTH_CLEAR = "TRUNCATE olympiad_nobles_eom";
	private static final String OLYMPIAD_MONTH_CREATE = "INSERT INTO olympiad_nobles_eom SELECT charId, class_id, olympiad_points, competitions_done, competitions_won, competitions_lost, competitions_drawn FROM olympiad_nobles";
	
	private static final Set<Integer> HERO_IDS = CategoryData.getInstance().getCategoryByType(CategoryType.SIXTH_CLASS_GROUP);
	
	private static final int COMP_START = OlympiadConfig.OLYMPIAD_START_TIME; // 6PM
	private static final int COMP_MIN = OlympiadConfig.OLYMPIAD_MIN; // 00 mins
	private static final long COMP_PERIOD = OlympiadConfig.OLYMPIAD_CPERIOD; // 6 hours
	protected static final long WEEKLY_PERIOD = OlympiadConfig.OLYMPIAD_WPERIOD; // 1 week
	protected static final long VALIDATION_PERIOD = OlympiadConfig.OLYMPIAD_VPERIOD; // 24 hours
	
	public static final int DEFAULT_POINTS = OlympiadConfig.OLYMPIAD_START_POINTS;
	
	public static final String CHAR_ID = "charId";
	public static final String CLASS_ID = "class_id";
	public static final String CHAR_NAME = "char_name";
	public static final String POINTS = "olympiad_points";
	public static final String COMP_DONE = "competitions_done";
	public static final String COMP_WON = "competitions_won";
	public static final String COMP_LOST = "competitions_lost";
	public static final String COMP_DRAWN = "competitions_drawn";
	public static final String COMP_DONE_WEEK = "competitions_done_week";
	
	protected long _olympiadEnd;
	protected long _validationEnd;
	
	/**
	 * The current period of the olympiad.<br>
	 * <b>0 -</b> Competition period<br>
	 * <b>1 -</b> Validation Period
	 */
	protected int _period;
	protected long _nextWeeklyChange;
	protected int _currentCycle;
	private long _compEnd;
	private Calendar _compStart;
	protected static boolean _inCompPeriod;
	protected static boolean _compStarted = false;
	protected ScheduledFuture<?> _scheduledCompStart;
	protected ScheduledFuture<?> _scheduledCompEnd;
	protected ScheduledFuture<?> _scheduledOlympiadEnd;
	protected ScheduledFuture<?> _scheduledWeeklyTask;
	protected ScheduledFuture<?> _scheduledValdationTask;
	protected ScheduledFuture<?> _gameManager = null;
	protected ScheduledFuture<?> _gameAnnouncer = null;
	
	/**
	 * Initializes the {@code Olympiad} system.<br>
	 * This constructor checks if olympiads are enabled in the configuration.<br>
	 * If enabled, it loads data and registers necessary event listeners.<br>
	 * It also triggers the initial setup if no period is currently active.
	 */
	protected Olympiad()
	{
		if (OlympiadConfig.OLYMPIAD_ENABLED)
		{
			load();
			AntiFeedManager.getInstance().registerEvent(AntiFeedManager.OLYMPIAD_ID);
			
			if (_period == 0)
			{
				init();
			}
		}
		else
		{
			LOGGER.log(Level.INFO, "Disabled.");
		}
	}
	
	/**
	 * Loads the olympiad data from the database.<br>
	 * This method initializes the current cycle, period, and end times.<br>
	 * It also populates the noble statistics into the {@code NOBLES} map.<br>
	 * Finally, it logs the remaining time until the next event.
	 */
	private void load()
	{
		NOBLES.clear();
		
		boolean loaded = false;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_DATA);
			ResultSet rset = statement.executeQuery())
		{
			while (rset.next())
			{
				_currentCycle = rset.getInt("current_cycle");
				_period = rset.getInt("period");
				_olympiadEnd = rset.getLong("olympiad_end");
				_validationEnd = rset.getLong("validation_end");
				_nextWeeklyChange = rset.getLong("next_weekly_change");
				loaded = true;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Olympiad System: Error loading olympiad data from database: ", e);
		}
		
		if (!loaded)
		{
			_currentCycle = 1;
			_period = 0;
			_olympiadEnd = 0;
			_validationEnd = 0;
			_nextWeeklyChange = 0;
		}
		
		final long currentTime = System.currentTimeMillis();
		switch (_period)
		{
			case 0:
			{
				if ((_olympiadEnd == 0) || (_olympiadEnd < currentTime))
				{
					setNewOlympiadEnd();
				}
				else
				{
					scheduleWeeklyChange();
				}
				break;
			}
			case 1:
			{
				if (_validationEnd > currentTime)
				{
					loadNoblesRank();
					_scheduledValdationTask = ThreadPool.schedule(new ValidationEndTask(), getMillisToValidationEnd());
				}
				else
				{
					_currentCycle++;
					_period = 0;
					deleteNobles();
					setNewOlympiadEnd();
				}
				break;
			}
			default:
			{
				LOGGER.warning("Olympiad System: Omg something went wrong in loading!! Period = " + _period);
				return;
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_LOAD_NOBLES);
			ResultSet rset = statement.executeQuery())
		{
			StatSet statData;
			while (rset.next())
			{
				statData = new StatSet();
				statData.set(CLASS_ID, rset.getInt(CLASS_ID));
				statData.set(CHAR_NAME, rset.getString(CHAR_NAME));
				final int compDone = rset.getInt(COMP_DONE);
				statData.set(POINTS, Math.clamp(rset.getInt(POINTS), 0, OlympiadConfig.OLYMPIAD_MAX_POINTS * compDone));
				statData.set(COMP_DONE, compDone);
				statData.set(COMP_WON, rset.getInt(COMP_WON));
				statData.set(COMP_LOST, rset.getInt(COMP_LOST));
				statData.set(COMP_DRAWN, rset.getInt(COMP_DRAWN));
				statData.set(COMP_DONE_WEEK, rset.getInt(COMP_DONE_WEEK));
				statData.set("to_save", false);
				
				addNobleStats(rset.getInt(CHAR_ID), statData);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database: ", e);
		}
		
		LOGGER.info("Olympiad System: Loading....");
		if (_period == 0)
		{
			LOGGER.info("Olympiad System: Currently in Olympiad Period");
		}
		else
		{
			LOGGER.info("Olympiad System: Currently in Validation Period");
		}
		
		long milliToEnd;
		if (_period == 0)
		{
			milliToEnd = getMillisToOlympiadEnd();
		}
		else
		{
			milliToEnd = getMillisToValidationEnd();
		}
		
		final double numSecs = (milliToEnd / 1000) % 60;
		double countDown = ((milliToEnd / 1000.) - numSecs) / 60;
		final int numMins = (int) (countDown % 60);
		countDown = (countDown - numMins) / 60;
		final int numHours = (int) (countDown % 24);
		final int numDays = (int) ((countDown - numHours) / 24);
		
		LOGGER.info("Olympiad System: " + numDays + " days, " + numHours + " hours and " + numMins + " mins until period ends.");
		
		if (_period == 0)
		{
			milliToEnd = getMillisToWeekChange();
			final double numSecs2 = (milliToEnd / 1000) % 60;
			double countDown2 = ((milliToEnd / 1000.) - numSecs2) / 60;
			final int numMins2 = (int) (countDown % 60);
			countDown2 = (countDown2 - numMins) / 60;
			final int numHours2 = (int) (countDown2 % 24);
			final int numDays2 = (int) ((countDown2 - numHours) / 24);
			
			LOGGER.info("Olympiad System: Next weekly change is in " + numDays2 + " days, " + numHours2 + " hours and " + numMins2 + " mins.");
		}
		
		LOGGER.info("Olympiad System: Loaded " + NOBLES.size() + " Nobles");
	}
	
	/**
	 * Retrieves the current rank of a player in the Olympiad.<br>
	 * It looks up the rank using the unique object ID of the {@code Player}.<br>
	 * If the player does not have a rank, it returns 0.
	 * @param player The {@code Player} whose rank is being requested.
	 * @return The integer rank of the player or 0 if no rank exists.
	 */
	public int getOlympiadRank(Player player)
	{
		return NOBLES_RANK.getOrDefault(player.getObjectId(), 0);
	}
	
	/**
	 * Loads the ranking data for nobles from the database.<br>
	 * This method populates the {@code NOBLES_RANK} map based on character positions.<br>
	 * It also updates and stores unclaimed olympiad points for all active nobles.
	 */
	public void loadNoblesRank()
	{
		NOBLES_RANK.clear();
		final Map<Integer, Integer> tmpPlace = new HashMap<>();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_ALL_CLASSIFIED_NOBLESS);
			ResultSet rset = statement.executeQuery())
		{
			int place = 1;
			while (rset.next())
			{
				tmpPlace.put(rset.getInt(CHAR_ID), place++);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Olympiad System: Error loading noblesse data from database for Ranking: ", e);
		}
		
		final int tmpPlaceSize = tmpPlace.size();
		int rank1 = (int) Math.round(tmpPlaceSize * 0.01);
		int rank2 = (int) Math.round(tmpPlaceSize * 0.10);
		int rank3 = (int) Math.round(tmpPlaceSize * 0.25);
		int rank4 = (int) Math.round(tmpPlaceSize * 0.50);
		if (rank1 == 0)
		{
			rank1 = 1;
			rank2++;
			rank3++;
			rank4++;
		}
		
		for (Entry<Integer, Integer> chr : tmpPlace.entrySet())
		{
			final Integer key = chr.getKey();
			if (chr.getValue() <= rank1)
			{
				NOBLES_RANK.put(key, 1);
			}
			else if (tmpPlace.get(key) <= rank2)
			{
				NOBLES_RANK.put(key, 2);
			}
			else if (tmpPlace.get(key) <= rank3)
			{
				NOBLES_RANK.put(key, 3);
			}
			else if (tmpPlace.get(key) <= rank4)
			{
				NOBLES_RANK.put(key, 4);
			}
			else
			{
				NOBLES_RANK.put(key, 5);
			}
		}
		
		// Store remaining hero reward points to player variables.
		final Map<Integer, Integer> absent = new HashMap<>();
		for (int noblesId : NOBLES.keySet())
		{
			final int points = getOlympiadTradePoint(noblesId);
			if (points <= 0)
			{
				continue;
			}
			
			final Player player = World.getPlayer(noblesId);
			if (player == null)
			{
				absent.put(Integer.valueOf(noblesId), Integer.valueOf(points));
				continue;
			}
			
			player.getVariables().set(UNCLAIMED_OLYMPIAD_POINTS_VAR, points);
		}
		
		if (absent.isEmpty())
		{
			return;
		}
		
		// One connection carries every noble who is away, where each of them used to take two of its own.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement remove = con.prepareStatement(REMOVE_UNCLAIMED_POINTS);
			PreparedStatement insert = con.prepareStatement(INSERT_UNCLAIMED_POINTS))
		{
			for (Entry<Integer, Integer> entry : absent.entrySet())
			{
				// The previous record goes before the new value takes its place.
				remove.setInt(1, entry.getKey().intValue());
				remove.setString(2, UNCLAIMED_OLYMPIAD_POINTS_VAR);
				remove.execute();
				
				insert.setInt(1, entry.getKey().intValue());
				insert.setString(2, UNCLAIMED_OLYMPIAD_POINTS_VAR);
				insert.setString(3, String.valueOf(entry.getValue()));
				insert.execute();
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Olympiad System: Couldn't store unclaimed olympiad points to DB!");
		}
	}
	
	/**
	 * Initializes the competition period settings.<br>
	 * This method calculates the start and end times for the olympiad.<br>
	 * It schedules the {@code OlympiadEndTask} if it is not already running.<br>
	 * It also updates the current competition status.
	 */
	protected void init()
	{
		if (_period == 1)
		{
			return;
		}
		
		_compStart = Calendar.getInstance();
		final int currentDay = _compStart.get(Calendar.DAY_OF_WEEK);
		boolean dayFound = false;
		int dayCounter = 0;
		for (int i = currentDay; i < 8; i++)
		{
			if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i))
			{
				dayFound = true;
				break;
			}
			
			dayCounter++;
		}
		
		if (!dayFound)
		{
			for (int i = 1; i < 8; i++)
			{
				if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i))
				{
					break;
				}
				
				dayCounter++;
			}
		}
		
		if (dayCounter > 0)
		{
			_compStart.add(Calendar.DAY_OF_MONTH, dayCounter);
		}
		
		_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
		_compStart.set(Calendar.MINUTE, COMP_MIN);
		_compEnd = _compStart.getTimeInMillis() + COMP_PERIOD;
		
		if (_scheduledOlympiadEnd != null)
		{
			_scheduledOlympiadEnd.cancel(true);
		}
		
		_scheduledOlympiadEnd = ThreadPool.schedule(new OlympiadEndTask(), getMillisToOlympiadEnd());
		
		updateCompStatus();
	}
	
	protected class OlympiadEndTask implements Runnable
	{
		@Override
		public void run()
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.ROUND_S1_OF_THE_OLYMPIAD_HAS_NOW_ENDED);
			sm.addInt(_currentCycle);
			
			World.broadcastToAllOnlinePlayers(sm);
			
			if (_scheduledWeeklyTask != null)
			{
				_scheduledWeeklyTask.cancel(true);
			}
			
			saveNobleData();
			
			_period = 1;
			final List<StatSet> heroesToBe = sortHerosToBe();
			Hero.getInstance().resetData();
			Hero.getInstance().computeNewHeroes(heroesToBe);
			
			saveOlympiadStatus();
			updateMonthlyData();
			
			final Calendar validationEnd = Calendar.getInstance();
			_validationEnd = validationEnd.getTimeInMillis() + VALIDATION_PERIOD;
			
			loadNoblesRank();
			_scheduledValdationTask = ThreadPool.schedule(new ValidationEndTask(), getMillisToValidationEnd());
		}
	}
	
	protected class ValidationEndTask implements Runnable
	{
		@Override
		public void run()
		{
			World.broadcastToAllOnlinePlayers("Olympiad Validation Period has ended");
			_period = 0;
			_currentCycle++;
			deleteNobles();
			setNewOlympiadEnd();
			init();
		}
	}
	
	/**
	 * Retrieves the total number of nobles currently registered.<br>
	 * This method returns the size of the {@code NOBLES} map.
	 * @return The current count of nobles as an {@code int}.
	 */
	protected static int getNobleCount()
	{
		return NOBLES.size();
	}
	
	/**
	 * Retrieves the {@code StatSet} for a specific noble player.<br>
	 * This method looks up the stats using the provided {@code playerId}.<br>
	 * It returns {@code null} if no data exists for that ID.
	 * @param playerId The unique identifier of the player to look up.
	 * @return The {@code StatSet} associated with the player, or {@code null}.
	 */
	public static StatSet getNobleStats(int playerId)
	{
		return NOBLES.get(playerId);
	}
	
	/**
	 * Removes the noble status and statistics for a specific player.<br>
	 * This method clears the data from both memory and the database.
	 * @param playerId The unique identifier of the player to remove.
	 */
	public static void removeNobleStats(int playerId)
	{
		NOBLES.remove(playerId);
		NOBLES_RANK.remove(playerId);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement("DELETE FROM olympiad_nobles WHERE charId=?");
			PreparedStatement ps2 = con.prepareStatement("DELETE FROM olympiad_nobles_eom WHERE charId=?"))
		{
			ps1.setInt(1, playerId);
			ps2.setInt(1, playerId);
			ps1.execute();
			ps2.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Olympiad System: Error removing noblesse data from database: ", e);
		}
	}
	
	/**
	 * Updates the competition status and schedules related tasks.<br>
	 * This method calculates the time until the next competition starts.<br>
	 * It schedules the start of the Olympiad games and their corresponding end events.<br>
	 * It also handles broadcasting system messages to all online players.
	 */
	private void updateCompStatus()
	{
		// _compStarted = false;
		
		final long milliToStart = getMillisToCompBegin();
		final double numSecs = (milliToStart / 1000) % 60;
		double countDown = ((milliToStart / 1000.) - numSecs) / 60;
		final int numMins = (int) (countDown % 60);
		countDown = (countDown - numMins) / 60;
		final int numHours = (int) (countDown % 24);
		final int numDays = (int) ((countDown - numHours) / 24);
		LOGGER.info("Olympiad System: Competition Period Starts in " + numDays + " days, " + numHours + " hours and " + numMins + " mins.");
		LOGGER.info("Olympiad System: Event starts/started: " + _compStart.getTime());
		
		_scheduledCompStart = ThreadPool.schedule(() ->
		{
			if (isOlympiadEnd() || !OlympiadConfig.OLYMPIAD_ENABLED)
			{
				return;
			}
			
			_inCompPeriod = true;
			
			for (Player player : World.getPlayers())
			{
				player.sendPacket(new ExOlympiadInfo(1));
			}
			
			World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_OLYMPIAD_HAS_BEGUN));
			LOGGER.info("Olympiad System: Olympiad Games have started.");
			LOGGER_OLYMPIAD.info("Result,Player1,Player2,Player1 HP,Player2 HP,Player1 Damage,Player2 Damage,Points,Classed");
			
			_gameManager = ThreadPool.scheduleAtFixedRate(OlympiadGameManager.getInstance(), 30000, 30000);
			if (OlympiadConfig.OLYMPIAD_ANNOUNCE_GAMES)
			{
				_gameAnnouncer = ThreadPool.scheduleAtFixedRate(new OlympiadAnnouncer(), 30000, 500);
			}
			
			final long regEnd = getMillisToCompEnd() - 1200000;
			if (regEnd > 0)
			{
				ThreadPool.schedule(() -> World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_OLYMPIAD_REGISTRATION_PERIOD_HAS_ENDED)), regEnd);
			}
			
			_scheduledCompEnd = ThreadPool.schedule(() ->
			{
				if (isOlympiadEnd())
				{
					return;
				}
				
				_inCompPeriod = false;
				
				for (Player player : World.getPlayers())
				{
					player.sendPacket(new ExOlympiadInfo(0));
				}
				
				World.broadcastToAllOnlinePlayers(new SystemMessage(SystemMessageId.THE_OLYMPIAD_IS_OVER));
				LOGGER.info("Olympiad System: Olympiad games have ended.");
				
				while (OlympiadGameManager.getInstance().isBattleStarted()) // cleared in game manager
				{
					try
					{
						// wait 1 minutes for end of pending games
						Thread.sleep(60000);
					}
					catch (Exception e)
					{
						// Ignore.
					}
				}
				
				if (_gameManager != null)
				{
					_gameManager.cancel(false);
					_gameManager = null;
				}
				
				if (_gameAnnouncer != null)
				{
					_gameAnnouncer.cancel(false);
					_gameAnnouncer = null;
				}
				
				saveOlympiadStatus();
				
				init();
			}, getMillisToCompEnd());
		}, getMillisToCompBegin());
	}
	
	/**
	 * Calculates the remaining time until the olympiad ends.<br>
	 * This method returns the difference between the end timestamp and the current system time.
	 * @return The number of milliseconds left before the olympiad finishes.
	 */
	public long getMillisToOlympiadEnd()
	{
		// if (_olympiadEnd > System.currentTimeMillis())
		return _olympiadEnd - System.currentTimeMillis();
		// return 10;
	}
	
	/**
	 * Manually triggers the selection of heroes.<br>
	 * This method cancels any existing scheduled end tasks.<br>
	 * It then schedules a new {@code OlympiadEndTask} to run immediately.
	 */
	public void manualSelectHeroes()
	{
		if (_scheduledOlympiadEnd != null)
		{
			_scheduledOlympiadEnd.cancel(true);
		}
		
		_scheduledOlympiadEnd = ThreadPool.schedule(new OlympiadEndTask(), 0);
	}
	
	/**
	 * Calculates the remaining time until the validation period ends.<br>
	 * Returns the difference in milliseconds between the current time and {@code _validationEnd}.<br>
	 * If the validation period has already passed, it returns a default value of {@code 10}.
	 * @return The number of milliseconds remaining until validation ends.
	 */
	protected long getMillisToValidationEnd()
	{
		final long currentTime = System.currentTimeMillis();
		if (_validationEnd > currentTime)
		{
			return _validationEnd - currentTime;
		}
		
		return 10;
	}
	
	/**
	 * Checks if the current Olympiad period is active.<br>
	 * Returns {@code true} if the period is not {@code 0}.<br>
	 * Returns {@code false} if no period is currently active.
	 * @return {@code true} if the Olympiad has ended or is active, {@code false} otherwise.
	 */
	public boolean isOlympiadEnd()
	{
		return _period != 0;
	}
	
	/**
	 * Updates the end time for the current Olympiad cycle.<br>
	 * It broadcasts a system message to all online players.<br>
	 * The method calculates the new end time based on {@code OlympiadConfig} settings.<br>
	 * It also schedules the next weekly change.
	 */
	protected void setNewOlympiadEnd()
	{
		final SystemMessage sm = new SystemMessage(SystemMessageId.ROUND_S1_OF_THE_OLYMPIAD_GAMES_HAS_STARTED);
		sm.addInt(_currentCycle);
		World.broadcastToAllOnlinePlayers(sm);
		
		final Calendar currentTime = Calendar.getInstance();
		currentTime.set(Calendar.AM_PM, Calendar.AM);
		currentTime.set(Calendar.HOUR_OF_DAY, 12);
		currentTime.set(Calendar.MINUTE, 0);
		currentTime.set(Calendar.SECOND, 0);
		
		final Calendar nextChange = Calendar.getInstance();
		final long nextChangeTimeInMillis = nextChange.getTimeInMillis();
		
		switch (OlympiadConfig.OLYMPIAD_PERIOD)
		{
			case "DAY":
			{
				currentTime.add(Calendar.DAY_OF_MONTH, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
				currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
				
				if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER >= 14)
				{
					_nextWeeklyChange = nextChangeTimeInMillis + WEEKLY_PERIOD;
				}
				else if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER >= 7)
				{
					_nextWeeklyChange = nextChangeTimeInMillis + (WEEKLY_PERIOD / 2);
				}
				else
				{
					LOGGER.warning("Invalid config value for Config.OLYMPIAD_PERIOD_MULTIPLIER, must be >= 7");
				}
				break;
			}
			case "WEEK":
			{
				currentTime.add(Calendar.WEEK_OF_MONTH, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
				currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
				
				if (OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER > 1)
				{
					_nextWeeklyChange = nextChangeTimeInMillis + WEEKLY_PERIOD;
				}
				else
				{
					_nextWeeklyChange = nextChangeTimeInMillis + (WEEKLY_PERIOD / 2);
				}
				break;
			}
			case "MONTH":
			{
				currentTime.add(Calendar.MONTH, OlympiadConfig.OLYMPIAD_PERIOD_MULTIPLIER);
				currentTime.add(Calendar.DAY_OF_MONTH, -1); // last day is for validation
				
				_nextWeeklyChange = nextChangeTimeInMillis + WEEKLY_PERIOD;
				break;
			}
		}
		
		_olympiadEnd = currentTime.getTimeInMillis();
		
		scheduleWeeklyChange();
	}
	
	/**
	 * Checks if the current time falls within the competition period.
	 * @return {@code true} if the server is currently in a competition period, {@code false} otherwise.
	 */
	public boolean inCompPeriod()
	{
		return _inCompPeriod;
	}
	
	/**
	 * Calculates the remaining time until the competition begins.<br>
	 * Returns a fixed value of {@code 10} if the competition is currently active.<br>
	 * If the competition has not started, it returns the difference between the start time and current time.<br>
	 * Otherwise, it triggers a reset of the competition start time.
	 * @return The number of milliseconds remaining until the competition starts.
	 */
	private long getMillisToCompBegin()
	{
		final long currentTime = System.currentTimeMillis();
		if ((_compStart.getTimeInMillis() < currentTime) && (_compEnd > currentTime))
		{
			return 10;
		}
		
		final long compStartTime = _compStart.getTimeInMillis();
		if (compStartTime > currentTime)
		{
			return compStartTime - currentTime;
		}
		
		return setNewCompBegin();
	}
	
	/**
	 * Calculates and sets the start time for the next competition period.<br>
	 * This method updates the internal schedule based on configuration settings.<br>
	 * It handles custom day logic and calculates the remaining milliseconds until the start.
	 * @return The number of milliseconds remaining until the next competition begins.
	 */
	private long setNewCompBegin()
	{
		_compStart = Calendar.getInstance();
		
		int currentDay = _compStart.get(Calendar.DAY_OF_WEEK);
		_compStart.set(Calendar.HOUR_OF_DAY, COMP_START);
		_compStart.set(Calendar.MINUTE, COMP_MIN);
		
		// Today's competitions ended, start checking from next day.
		if (currentDay == _compStart.get(Calendar.DAY_OF_WEEK))
		{
			if (currentDay == Calendar.SATURDAY)
			{
				currentDay = Calendar.SUNDAY;
			}
			else
			{
				currentDay++;
			}
		}
		
		boolean dayFound = false;
		int dayCounter = 0;
		for (int i = currentDay; i < 8; i++)
		{
			if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i))
			{
				dayFound = true;
				break;
			}
			
			dayCounter++;
		}
		
		if (!dayFound)
		{
			for (int i = 1; i < 8; i++)
			{
				if (OlympiadConfig.OLYMPIAD_COMPETITION_DAYS.contains(i))
				{
					break;
				}
				
				dayCounter++;
			}
		}
		
		if (dayCounter > 0)
		{
			_compStart.add(Calendar.DAY_OF_MONTH, dayCounter);
		}
		
		_compStart.add(Calendar.HOUR_OF_DAY, 24);
		final long timeInMillis = _compStart.getTimeInMillis();
		_compEnd = timeInMillis + COMP_PERIOD;
		
		LOGGER.info("Olympiad System: New Schedule @ " + _compStart.getTime());
		
		return timeInMillis - System.currentTimeMillis();
	}
	
	/**
	 * Calculates the remaining time until the competition ends.<br>
	 * This value is returned in milliseconds.
	 * @return The number of milliseconds left before the competition finishes.
	 */
	public long getMillisToCompEnd()
	{
		// if (_compEnd > System.currentTimeMillis())
		return _compEnd - System.currentTimeMillis();
		// return 10;
	}
	
	/**
	 * Calculates the remaining time until the next weekly change.<br>
	 * It returns the difference in milliseconds between the current time and the scheduled change.<br>
	 * If the change has already occurred, it returns a default value of {@code 10}.
	 * @return The number of milliseconds remaining until the next weekly change.
	 */
	public long getMillisToWeekChange()
	{
		final long currentTime = System.currentTimeMillis();
		if (_nextWeeklyChange > currentTime)
		{
			return _nextWeeklyChange - currentTime;
		}
		
		return 10;
	}
	
	/**
	 * Schedules a recurring task to handle weekly updates.<br>
	 * This method uses {@code ThreadPool} to run every {@code WEEKLY_PERIOD}.<br>
	 * It calls {@code addWeeklyPoints()} and {@code resetWeeklyMatches()}.
	 */
	private void scheduleWeeklyChange()
	{
		_scheduledWeeklyTask = ThreadPool.scheduleAtFixedRate(() ->
		{
			resetWeeklyMatches();
			LOGGER.info("Olympiad System: Reset weekly matches to nobles");
			
			_nextWeeklyChange = System.currentTimeMillis() + WEEKLY_PERIOD;
		}, getMillisToWeekChange(), WEEKLY_PERIOD);
	}
	
	/**
	 * Resets the weekly competition statistics for all nobles.<br>
	 * This method clears values like {@code COMP_DONE_WEEK} and related stats.<br>
	 * It only executes if the current period is not equal to 1.
	 */
	protected void resetWeeklyMatches()
	{
		if (_period == 1)
		{
			return;
		}
		
		for (StatSet nobleInfo : NOBLES.values())
		{
			nobleInfo.set(COMP_DONE_WEEK, 0);
		}
	}
	
	/**
	 * Retrieves the current olympiad cycle.
	 * @return The current cycle as an {@code int}.
	 */
	public int getCurrentCycle()
	{
		return _currentCycle;
	}
	
	/**
	 * Retrieves the current olympiad period.
	 * @return The current period as an {@code int}.
	 */
	public int getPeriod()
	{
		return _period;
	}
	
	/**
	 * Checks if the specified player is currently inside an olympiad stadium.<br>
	 * This method queries the {@code ZoneManager} to verify the player's location.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the player is in a stadium, {@code false} otherwise.
	 */
	public boolean playerInStadia(Player player)
	{
		return ZoneManager.getInstance().getOlympiadStadium(player) != null;
	}
	
	/**
	 * Saves the current noble statistics to the database.<br>
	 * This method iterates through all entries in the {@code NOBLES} map.<br>
	 * It determines whether to insert or update records based on the {@code to_save} flag.<br>
	 * Errors during database operations are logged as severe errors.
	 */
	protected synchronized void saveNobleData()
	{
		if (NOBLES.isEmpty())
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection())
		{
			final List<Entry<Integer, StatSet>> nobleList = new ArrayList<>(NOBLES.entrySet());
			for (Entry<Integer, StatSet> entry : nobleList)
			{
				final StatSet nobleInfo = entry.getValue();
				if (nobleInfo == null)
				{
					continue;
				}
				
				final int charId = entry.getKey();
				final int classId = nobleInfo.getInt(CLASS_ID);
				final int points = nobleInfo.getInt(POINTS);
				final int compDone = nobleInfo.getInt(COMP_DONE);
				final int compWon = nobleInfo.getInt(COMP_WON);
				final int compLost = nobleInfo.getInt(COMP_LOST);
				final int compDrawn = nobleInfo.getInt(COMP_DRAWN);
				final int compDoneWeek = nobleInfo.getInt(COMP_DONE_WEEK);
				final boolean toSave = nobleInfo.getBoolean("to_save");
				
				try (PreparedStatement statement = con.prepareStatement(toSave ? OLYMPIAD_SAVE_NOBLES : OLYMPIAD_UPDATE_NOBLES))
				{
					if (toSave)
					{
						statement.setInt(1, charId);
						statement.setInt(2, classId);
						statement.setInt(3, points);
						statement.setInt(4, compDone);
						statement.setInt(5, compWon);
						statement.setInt(6, compLost);
						statement.setInt(7, compDrawn);
						statement.setInt(8, compDoneWeek);
						
						nobleInfo.set("to_save", false);
					}
					else
					{
						statement.setInt(1, points);
						statement.setInt(2, compDone);
						statement.setInt(3, compWon);
						statement.setInt(4, compLost);
						statement.setInt(5, compDrawn);
						statement.setInt(6, compDoneWeek);
						statement.setInt(7, charId);
					}
					
					statement.execute();
					statement.close();
				}
				catch (SQLException e)
				{
					LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save noble data for charId " + charId, e);
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save noblesse data to database: ", e);
		}
	}
	
	/**
	 * Saves the current state of the Olympiad system to the database.<br>
	 * This method calls {@code saveNobleData()} before persisting cycle and period information.<br>
	 * It handles potential {@code SQLException} by logging a severe error message.
	 */
	public void saveOlympiadStatus()
	{
		saveNobleData();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_SAVE_DATA))
		{
			statement.setInt(1, _currentCycle);
			statement.setInt(2, _period);
			statement.setLong(3, _olympiadEnd);
			statement.setLong(4, _validationEnd);
			statement.setLong(5, _nextWeeklyChange);
			statement.setInt(6, _currentCycle);
			statement.setInt(7, _period);
			statement.setLong(8, _olympiadEnd);
			statement.setLong(9, _validationEnd);
			statement.setLong(10, _nextWeeklyChange);
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Olympiad System: Failed to save olympiad data to database: ", e);
		}
		// @formatter:off
		/*
		Properties OlympiadProperties = new Properties();
		try (FileOutputStream fos = new FileOutputStream(new File("./" + OLYMPIAD_DATA_FILE)))
		{
			OlympiadProperties.setProperty("CurrentCycle", String.valueOf(_currentCycle));
			OlympiadProperties.setProperty("Period", String.valueOf(_period));
			OlympiadProperties.setProperty("OlympiadEnd", String.valueOf(_olympiadEnd));
			OlympiadProperties.setProperty("ValdationEnd", String.valueOf(_validationEnd));
			OlympiadProperties.setProperty("NextWeeklyChange", String.valueOf(_nextWeeklyChange));
			OlympiadProperties.store(fos, "Olympiad Properties");
		}
		catch (Exception e)
		{
			LOGGER.warning("Olympiad System: Unable to save olympiad properties to file: ", e);
		}
		*/
		// @formatter:on
	}
	
	/**
	 * Updates the monthly noble data in the database.<br>
	 * This method executes the SQL queries defined by {@code OLYMPIAD_MONTH_CLEAR} and {@code OLYMPIAD_MONTH_CREATE}.<br>
	 * It handles any {@code SQLException} by logging a severe error message.
	 */
	protected void updateMonthlyData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps1 = con.prepareStatement(OLYMPIAD_MONTH_CLEAR);
			PreparedStatement ps2 = con.prepareStatement(OLYMPIAD_MONTH_CREATE))
		{
			ps1.execute();
			ps2.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "Olympiad System: Failed to update monthly noblese data: ", e);
		}
	}
	
	/**
	 * Retrieves and prepares a list of heroes for the olympiad.<br>
	 * This method only executes if the current period is {@code 1}.<br>
	 * It fetches hero data from the database based on predefined IDs.
	 * @return A {@code List} of {@code StatSet} objects containing hero information.
	 */
	protected List<StatSet> sortHerosToBe()
	{
		if (_period != 1)
		{
			return Collections.emptyList();
		}
		
		if (NOBLES != null)
		{
			LOGGER_OLYMPIAD.info("Noble,charid,classid,compDone,points");
			StatSet nobleInfo;
			for (Entry<Integer, StatSet> entry : NOBLES.entrySet())
			{
				nobleInfo = entry.getValue();
				if (nobleInfo == null)
				{
					continue;
				}
				
				final int charId = entry.getKey();
				final int classId = nobleInfo.getInt(CLASS_ID);
				final String charName = nobleInfo.getString(CHAR_NAME);
				final int points = nobleInfo.getInt(POINTS);
				final int compDone = nobleInfo.getInt(COMP_DONE);
				
				LOGGER_OLYMPIAD.info(charName + "," + charId + "," + classId + "," + compDone + "," + points);
			}
		}
		
		final List<StatSet> heroesToBe = new ArrayList<>();
		
		int legendId = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_GET_LEGEND))
		{
			try (ResultSet rset = statement.executeQuery())
			{
				if (rset.next())
				{
					legendId = rset.getInt("charId");
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Olympiad System: Could not load legend from DB");
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_GET_HEROS))
		{
			StatSet hero;
			for (int element : HERO_IDS)
			{
				statement.setInt(1, element);
				
				try (ResultSet rset = statement.executeQuery())
				{
					if (rset.next())
					{
						hero = new StatSet();
						final int charId = rset.getInt(CHAR_ID);
						hero.set(CLASS_ID, element);
						hero.set(CHAR_ID, charId);
						hero.set(CHAR_NAME, rset.getString(CHAR_NAME));
						hero.set("LEGEND", charId == legendId ? 1 : 0);
						
						LOGGER_OLYMPIAD.info("Hero " + hero.getString(CHAR_NAME) + "," + charId + "," + hero.getInt(CLASS_ID));
						heroesToBe.add(hero);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Olympiad System: Could not load heros from DB");
		}
		
		return heroesToBe;
	}
	
	/**
	 * Retrieves a list of character names for the class leaderboard.<br>
	 * This method fetches winners based on the current configuration settings.<br>
	 * It handles specific logic for different player classes.
	 * @param classId The unique identifier for the player class.
	 * @return A {@code List} of strings containing the names of the leaders.
	 */
	public List<String> getClassLeaderBoard(int classId)
	{
		final List<String> names = new ArrayList<>();
		final String query = OlympiadConfig.OLYMPIAD_SHOW_MONTHLY_WINNERS ? ((classId == PlayerClass.MALE_SOUL_HOUND.getId()) ? GET_EACH_CLASS_LEADER_SOULHOUND : GET_EACH_CLASS_LEADER) : ((classId == PlayerClass.MALE_SOUL_HOUND.getId()) ? GET_EACH_CLASS_LEADER_CURRENT_SOULHOUND : GET_EACH_CLASS_LEADER_CURRENT);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(query))
		{
			ps.setInt(1, classId);
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					names.add(rset.getString(CHAR_NAME));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Olympiad System: Couldn't load olympiad leaders from DB!");
		}
		
		// The top ten are picked by points but listed by name, so the ranking order stays hidden.
		names.sort(String.CASE_INSENSITIVE_ORDER);
		return names;
	}
	
	/**
	 * Calculates the total trade points for a specific noble.<br>
	 * It checks if the current period is active and if the object exists in the rank map.<br>
	 * The method adds bonuses based on hero status, rank position, and competition results.<br>
	 * It resets the noble's point value to 0 after calculation.
	 * @param objectId The unique identifier of the noble.
	 * @return The total calculated trade points as an {@code int}.
	 */
	private int getOlympiadTradePoint(int objectId)
	{
		if ((_period != 1) || NOBLES_RANK.isEmpty())
		{
			return 0;
		}
		
		if (!NOBLES_RANK.containsKey(objectId))
		{
			return 0;
		}
		
		final StatSet noble = NOBLES.get(objectId);
		if ((noble == null) || (noble.getInt(POINTS) == 0))
		{
			return 0;
		}
		
		// Hero point bonus
		int points = Hero.getInstance().isHero(objectId) || Hero.getInstance().isUnclaimedHero(objectId) ? OlympiadConfig.OLYMPIAD_HERO_POINTS : 0;
		
		// Rank point bonus
		switch (NOBLES_RANK.get(objectId))
		{
			case 1:
			{
				points += OlympiadConfig.OLYMPIAD_RANK1_POINTS;
				break;
			}
			case 2:
			{
				points += OlympiadConfig.OLYMPIAD_RANK2_POINTS;
				break;
			}
			case 3:
			{
				points += OlympiadConfig.OLYMPIAD_RANK3_POINTS;
				break;
			}
			case 4:
			{
				points += OlympiadConfig.OLYMPIAD_RANK4_POINTS;
				break;
			}
			default:
			{
				points += OlympiadConfig.OLYMPIAD_RANK5_POINTS;
			}
		}
		
		// Win/no win matches point bonus.
		points += getCompetitionWon(objectId) > 0 ? 10 : 0;
		
		// This is a one time calculation.
		noble.set(POINTS, 0);
		
		return points;
	}
	
	/**
	 * Retrieves the current noble points for a specific player.<br>
	 * If the player does not have any stats recorded, new stats are initialized with default values.
	 * @param player The {@code Player} object to check.
	 * @return The number of points held by the player.
	 */
	public int getNoblePoints(Player player)
	{
		final int objectId = player.getObjectId();
		if (!NOBLES.containsKey(objectId))
		{
			final StatSet statDat = new StatSet();
			statDat.set(CLASS_ID, player.getBaseClass());
			statDat.set(CHAR_NAME, player.getName());
			statDat.set(POINTS, DEFAULT_POINTS);
			statDat.set(COMP_DONE, 0);
			statDat.set(COMP_WON, 0);
			statDat.set(COMP_LOST, 0);
			statDat.set(COMP_DRAWN, 0);
			statDat.set(COMP_DONE_WEEK, 0);
			statDat.set("to_save", true);
			addNobleStats(objectId, statDat);
		}
		
		return NOBLES.get(objectId).getInt(POINTS);
	}
	
	/**
	 * Retrieves the last recorded Olympiad points for a specific character.<br>
	 * This method queries the database using the provided {@code objId}.<br>
	 * It returns 0 if no record is found or an error occurs.
	 * @param objId The unique identifier of the character.
	 * @return The number of points earned in the last Olympiad.
	 */
	public int getLastNobleOlympiadPoints(int objId)
	{
		int result = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT olympiad_points FROM olympiad_nobles_eom WHERE charId = ?"))
		{
			ps.setInt(1, objId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.first())
				{
					result = rs.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Olympiad System: Could not load last olympiad points:", e);
		}
		
		return result;
	}
	
	/**
	 * Retrieves the completion status of a competition for a specific object.<br>
	 * It checks if the object exists in the {@code NOBLES} map.<br>
	 * If it does not exist, it returns {@code 0}.<br>
	 * Otherwise, it returns the value associated with the {@code COMP_DONE} stat.
	 * @param objId The unique identifier of the object to check.
	 * @return The competition completion status as an {@code int}.
	 */
	public int getCompetitionDone(int objId)
	{
		if ((NOBLES == null) || !NOBLES.containsKey(objId))
		{
			return 0;
		}
		
		return NOBLES.get(objId).getInt(COMP_DONE);
	}
	
	/**
	 * Retrieves the number of competitions won by a specific noble.<br>
	 * It checks if the noble exists in the {@code NOBLES} map.<br>
	 * If the noble is not found, it returns {@code 0}.
	 * @param objId The unique identifier of the noble.
	 * @return The total count of competitions won by the noble.
	 */
	public int getCompetitionWon(int objId)
	{
		if ((NOBLES == null) || !NOBLES.containsKey(objId))
		{
			return 0;
		}
		
		return NOBLES.get(objId).getInt(COMP_WON);
	}
	
	/**
	 * Retrieves the number of lost competitions for a specific noble.<br>
	 * It checks if the noble exists in the {@code NOBLES} map.<br>
	 * If the noble is not found, it returns {@code 0}.<br>
	 * Otherwise, it returns the value associated with {@code COMP_LOST}.
	 * @param objId The unique identifier of the noble.
	 * @return The total count of lost competitions.
	 */
	public int getCompetitionLost(int objId)
	{
		if ((NOBLES == null) || !NOBLES.containsKey(objId))
		{
			return 0;
		}
		
		return NOBLES.get(objId).getInt(COMP_LOST);
	}
	
	/**
	 * Retrieves the number of completed competition weeks for a specific noble.<br>
	 * Returns {@code 0} if the object ID is not found in the records.
	 * @param objId The unique identifier of the noble.
	 * @return The count of completed competition weeks.
	 */
	public int getCompetitionDoneWeek(int objId)
	{
		if ((NOBLES == null) || !NOBLES.containsKey(objId))
		{
			return 0;
		}
		
		return NOBLES.get(objId).getInt(COMP_DONE_WEEK);
	}
	
	/**
	 * Calculates the number of matches remaining for a specific object.<br>
	 * It subtracts the completed matches from the maximum allowed weekly limit.<br>
	 * The result will never be less than {@code 0}.
	 * @param objId The unique identifier of the object.
	 * @return The count of remaining weekly matches.
	 */
	public int getRemainingWeeklyMatches(int objId)
	{
		return Math.max(OlympiadConfig.OLYMPIAD_MAX_WEEKLY_MATCHES - getCompetitionDoneWeek(objId), 0);
	}
	
	/**
	 * Removes all noble data from the database.<br>
	 * Clears the {@code NOBLES} map in memory.<br>
	 * Logs a warning if a {@code SQLException} occurs during deletion.
	 */
	protected void deleteNobles()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(OLYMPIAD_DELETE_ALL))
		{
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Olympiad System: Couldn't delete nobles from DB!");
		}
		
		NOBLES.clear();
	}
	
	/**
	 * Adds a new set of noble statistics to the internal map.<br>
	 * This method associates the provided {@code StatSet} with a specific character ID.<br>
	 * If the {@code charId} already exists, it will be updated with the new data.
	 * @param charId The unique identifier for the character.
	 * @param data The {@code StatSet} containing the noble statistics to store.
	 * @return The previous {@code StatSet} associated with the {@code charId}, or {@code null} if there was no previous mapping.
	 */
	public static StatSet addNobleStats(int charId, StatSet data)
	{
		return NOBLES.put(charId, data);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code Olympiad} class.<br>
	 * This method provides a global access point to the olympiad manager.
	 * @return The active {@code Olympiad} instance.
	 */
	public static Olympiad getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Olympiad INSTANCE = new Olympiad();
	}
}
