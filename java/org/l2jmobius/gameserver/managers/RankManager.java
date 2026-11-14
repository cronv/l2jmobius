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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.olympiad.Hero;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the ranking system for various game activities.<br>
 * This class handles the logic for tracking and updating player positions in leaderboards.<br>
 * It provides methods to retrieve, update, and synchronize rank data across the server.
 * @author NviX
 */
public class RankManager
{
	private static final Logger LOGGER = Logger.getLogger(RankManager.class.getName());
	
	public static final Long TIME_LIMIT = 2592000000L; // 30 days in milliseconds
	public static final long CURRENT_TIME = System.currentTimeMillis();
	public static final int PLAYER_LIMIT = 500;
	public static final int CONQUEST_PLAYER_LIMIT = 100;
	
	private static final String SELECT_CHARACTERS = "SELECT charId,char_name,level,race,base_class, clanid FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 84 ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	private static final String SELECT_CHARACTERS_PVP = "SELECT charId,char_name,level,race,base_class, clanid, deaths, kills, pvpkills FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 84 ORDER BY kills DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	private static final String SELECT_CHARACTERS_BY_RACE = "SELECT charId FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 84 AND race = ? ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	
	private static final String GET_CURRENT_CYCLE_DATA = "SELECT characters.char_name, characters.level, characters.base_class, characters.clanid, olympiad_nobles.charId, olympiad_nobles.olympiad_points, olympiad_nobles.competitions_won, olympiad_nobles.competitions_lost, olympiad_nobles.competitions_drawn FROM characters, olympiad_nobles WHERE characters.charId = olympiad_nobles.charId ORDER BY olympiad_nobles.olympiad_points DESC LIMIT " + PLAYER_LIMIT;
	private static final String GET_HEROES = "SELECT characters.charId, characters.char_name, characters.race, characters.sex, characters.base_class, characters.level, characters.clanid, olympiad_nobles_eom.competitions_won, olympiad_nobles_eom.competitions_lost, olympiad_nobles_eom.competitions_drawn, olympiad_nobles_eom.olympiad_points, heroes.legend_count, heroes.count FROM heroes, characters, olympiad_nobles_eom WHERE characters.charId = heroes.charId AND characters.charId = olympiad_nobles_eom.charId AND heroes.played = 1 AND characters.accesslevel = 0 ORDER BY olympiad_nobles_eom.olympiad_points DESC, characters.base_class ASC LIMIT " + RankManager.PLAYER_LIMIT;
	private static final String GET_CHARACTERS_BY_CLASS = "SELECT charId FROM characters WHERE (" + CURRENT_TIME + " - cast(lastAccess as signed) < " + TIME_LIMIT + ") AND accesslevel = 0 AND level > 84 AND characters.base_class = ? ORDER BY exp DESC, onlinetime DESC LIMIT " + PLAYER_LIMIT;
	
	// Conquest Data
	private static final String GET_CURRENT_CONQUEST_CYCLE_DATA = "SELECT characters.charId, characters.char_name, character_variables.charId, character_variables.var, character_variables.val FROM characters, character_variables WHERE characters.charId = character_variables.charId AND val > 0 AND var IN ('" + PlayerVariables.CONQUEST_PERSONAL_POINTS + "') ORDER BY CONVERT(val, UNSIGNED INTEGER) DESC LIMIT " + CONQUEST_PLAYER_LIMIT;
	private static final String GET_PREVIOUS_CONQUEST_CYCLE_DATA = "SELECT * FROM conquest_prev_season_ranklist ORDER BY CONVERT(personal_points, UNSIGNED INTEGER) DESC LIMIT " + CONQUEST_PLAYER_LIMIT;
	
	private final Map<Integer, StatSet> _mainList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotList = new ConcurrentHashMap<>();
	private final Map<Integer, StatSet> _mainOlyList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotOlyList = new ConcurrentHashMap<>();
	private final List<HeroInfo> _mainHeroList = new ArrayList<>();
	private List<HeroInfo> _snapshotHeroList = new ArrayList<>();
	private final Map<Integer, StatSet> _mainPvpList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotPvpList = new ConcurrentHashMap<>();
	
	// Conquest Lists
	private final Map<Integer, StatSet> _currentConquestList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotCurrentConquestList = new ConcurrentHashMap<>();
	private final Map<Integer, StatSet> _previousConquestList = new ConcurrentHashMap<>();
	private Map<Integer, StatSet> _snapshotPreviousConquestList = new ConcurrentHashMap<>();
	private String _conquestTopRankName = "";
	
	// Cache maps for consulting global ranks.
	private final Map<Integer, Integer> _globalConsultingRankCache = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _raceConsultingRankCache = new ConcurrentHashMap<>();
	
	public class HeroInfo
	{
		public String charName;
		public String clanName;
		public int serverId;
		public int race;
		public boolean isMale;
		public int baseClass;
		public int level;
		public int legendCount;
		public int competitionsWon;
		public int competitionsLost;
		public int competitionsDrawn;
		public int olympiadPoints;
		public int clanLevel;
		public boolean isTopHero;
		
		HeroInfo(String charName, String clanName, int serverId, int race, boolean isMale, int baseClass, int level, int legendCount, int competitionsWon, int competitionsLost, int competitionsDrawn, int olympiadPoints, int clanLevel, boolean isTopHero)
		{
			this.charName = charName;
			this.clanName = clanName;
			this.serverId = serverId;
			this.race = race;
			this.isMale = isMale;
			this.baseClass = baseClass;
			this.level = level;
			this.legendCount = legendCount;
			this.competitionsWon = competitionsWon;
			this.competitionsLost = competitionsLost;
			this.competitionsDrawn = competitionsDrawn;
			this.olympiadPoints = olympiadPoints;
			this.clanLevel = clanLevel;
			this.isTopHero = isTopHero;
		}
	}
	
	/**
	 * Initializes a new instance of the {@code RankManager}.<br>
	 * This constructor starts a background task to update ranks periodically.
	 */
	protected RankManager()
	{
		ThreadPool.scheduleAtFixedRate(this::update, 0, 1800000);
	}
	
	/**
	 * Refreshes all ranking data from the database.<br>
	 * This method updates global, race, class, and olympiad ranks.<br>
	 * It also synchronizes snapshot lists and clears internal caches.
	 */
	private synchronized void update()
	{
		// Load charIds All.
		_snapshotList = _mainList;
		_mainList.clear();
		_snapshotOlyList = _mainOlyList;
		_mainOlyList.clear();
		_snapshotHeroList = _mainHeroList;
		_mainHeroList.clear();
		_snapshotPvpList = _mainPvpList;
		_mainPvpList.clear();
		_snapshotCurrentConquestList = _currentConquestList;
		_currentConquestList.clear();
		_snapshotPreviousConquestList = _previousConquestList;
		_previousConquestList.clear();
		
		_globalConsultingRankCache.clear();
		_raceConsultingRankCache.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHARACTERS))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					final int classId = result.getInt("base_class");
					stats.set("charId", charId);
					stats.set("name", result.getString("char_name"));
					stats.set("level", result.getInt("level"));
					stats.set("classId", result.getInt("base_class"));
					final int race = result.getInt("race");
					stats.set("race", race);
					loadRaceRank(charId, race, stats);
					loadClassRank(charId, classId, stats);
					
					final Clan clan = ClanTable.getInstance().getClan(result.getInt("clanid"));
					if (clan != null)
					{
						stats.set("clanName", clan.getName());
					}
					else
					{
						stats.set("clanName", "");
					}
					
					_mainList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		// Load olympiad data.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_CURRENT_CYCLE_DATA))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					stats.set("charId", charId);
					stats.set("name", result.getString("char_name"));
					
					final Clan clan = ClanTable.getInstance().getClan(result.getInt("clanid"));
					if (clan != null)
					{
						stats.set("clanName", clan.getName());
						stats.set("clanLevel", clan.getLevel());
					}
					else
					{
						stats.set("clanName", "");
						stats.set("clanLevel", 0);
					}
					
					stats.set("level", result.getInt("level"));
					final int classId = result.getInt("base_class");
					stats.set("classId", classId);
					stats.set("competitions_won", result.getInt("competitions_won"));
					stats.set("competitions_lost", result.getInt("competitions_lost"));
					stats.set("competitions_drawn", result.getInt("competitions_drawn"));
					stats.set("olympiad_points", result.getInt("olympiad_points"));
					
					if (Hero.getInstance().getCompleteHeroes().containsKey(charId))
					{
						final StatSet heroStats = Hero.getInstance().getCompleteHeroes().get(charId);
						stats.set("count", heroStats.getInt("count", 0));
						stats.set("legend_count", heroStats.getInt("legend_count", 0));
					}
					else
					{
						stats.set("count", 0);
						stats.set("legend_count", 0);
					}
					
					loadClassRank(charId, classId, stats);
					
					_mainOlyList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load olympiad total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		if (!Hero.getInstance().getHeroes().isEmpty())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement statement = con.prepareStatement(GET_HEROES);
				ResultSet result = statement.executeQuery())
			{
				boolean isFirstHero = true;
				while (result.next())
				{
					final String charName = result.getString("char_name");
					final int clanId = result.getInt("clanid");
					final String clanName = (clanId > 0) ? ClanTable.getInstance().getClan(clanId).getName() : "";
					final int race = result.getInt("race");
					final int baseClass = result.getInt("base_class");
					final boolean isMale = (baseClass == 235 /* Fix for Shinemaker icon. */) || (result.getInt("sex") != 1);
					final int level = result.getInt("level");
					final int legendCount = result.getInt("legend_count");
					final int competitionsWon = result.getInt("competitions_won");
					final int competitionsLost = result.getInt("competitions_lost");
					final int competitionsDrawn = result.getInt("competitions_drawn");
					final int olympiadPoints = result.getInt("olympiad_points");
					final int clanLevel = (clanId > 0) ? ClanTable.getInstance().getClan(clanId).getLevel() : 0;
					final boolean isTopHero = isFirstHero;
					_mainHeroList.add(new HeroInfo(charName, clanName, ServerConfig.SERVER_ID, race, isMale, baseClass, level, legendCount, competitionsWon, competitionsLost, competitionsDrawn, olympiadPoints, clanLevel, isTopHero));
					isFirstHero = false;
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Could not load Hero and Legend Info rank data: " + this + " - " + e.getMessage(), e);
			}
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHARACTERS_PVP))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					stats.set("charId", charId);
					stats.set("name", result.getString("char_name"));
					stats.set("level", result.getInt("level"));
					stats.set("classId", result.getInt("base_class"));
					final int race = result.getInt("race");
					stats.set("race", race);
					stats.set("kills", result.getInt("kills"));
					stats.set("deaths", result.getInt("deaths"));
					stats.set("points", result.getInt("pvpkills"));
					loadRaceRank(charId, race, stats);
					
					final Clan clan = ClanTable.getInstance().getClan(result.getInt("clanid"));
					if (clan != null)
					{
						stats.set("clanName", clan.getName());
					}
					else
					{
						stats.set("clanName", "");
					}
					
					_mainPvpList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load pvp total rank data: " + this + " - " + e.getMessage(), e);
		}
		
		// load conquest data.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_CURRENT_CONQUEST_CYCLE_DATA))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					stats.set("charId", charId);
					stats.set("name", result.getString("char_name"));
					stats.set("conquestPersonalPoints", result.getLong("val"));
					if (i == 1)
					{
						loadConquestTopRank(charId);
					}
					
					_currentConquestList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load conquest rank data: " + this + " - " + e.getMessage(), e);
		}
		
		// load previous season conquest data.
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_PREVIOUS_CONQUEST_CYCLE_DATA))
		{
			try (ResultSet result = statement.executeQuery())
			{
				int i = 1;
				while (result.next())
				{
					final StatSet stats = new StatSet();
					final int charId = result.getInt("charId");
					stats.set("charId", charId);
					stats.set("conquest_name", result.getString("char_name"));
					stats.set("conquestPersonalPoints", result.getLong("personal_points"));
					
					_previousConquestList.put(i, stats);
					i++;
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load conquest previous season ranklist data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads the ranking for a specific character based on their class.<br>
	 * This method queries the database to determine the position of the character.<br>
	 * The result is stored in the provided {@code StatSet}.
	 * @param charId The unique identifier of the character.
	 * @param classId The identifier of the character's class.
	 * @param stats The {@code StatSet} where the rank will be saved.
	 */
	private void loadClassRank(int charId, int classId, StatSet stats)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(GET_CHARACTERS_BY_CLASS))
		{
			statement.setInt(1, classId);
			try (ResultSet result = statement.executeQuery())
			{
				int i = 0;
				while (result.next())
				{
					if (result.getInt("charId") == charId)
					{
						stats.set("classRank", i + 1);
					}
					
					i++;
				}
				
				if (i == 0)
				{
					stats.set("classRank", 0);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars classId olympiad rank data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads the race ranking for a specific character.<br>
	 * It queries the database to find the position of the character within their race.<br>
	 * The result is stored in the provided {@code StatSet}.
	 * @param charId The unique identifier of the character.
	 * @param race The race identifier used for ranking.
	 * @param stats The {@code StatSet} where the rank will be saved.
	 */
	private void loadRaceRank(int charId, int race, StatSet stats)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SELECT_CHARACTERS_BY_RACE))
		{
			statement.setInt(1, race);
			try (ResultSet result = statement.executeQuery())
			{
				int i = 0;
				while (result.next())
				{
					if (result.getInt("charId") == charId)
					{
						stats.set("raceRank", i + 1);
					}
					
					i++;
				}
				
				if (i == 0)
				{
					stats.set("raceRank", 0);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load chars race rank data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads the top rank name for a specific character from the database.<br>
	 * It retrieves the value associated with {@code PlayerVariables.CONQUEST_NAME}.
	 * @param charId The unique identifier of the character to load data for.
	 */
	private void loadConquestTopRank(int charId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("SELECT * FROM character_variables WHERE var='" + PlayerVariables.CONQUEST_NAME + "' AND charId=?"))
		{
			statement.setString(1, String.valueOf(charId));
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					_conquestTopRankName = result.getString("val");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Could not load conquest name data: " + this + " - " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the current ranking list of players.<br>
	 * The data is stored in a {@code Map} where the key is the character ID.
	 * @return A {@code Map} containing character IDs and their corresponding {@code StatSet}.
	 */
	public Map<Integer, StatSet> getRankList()
	{
		return _mainList;
	}
	
	/**
	 * Retrieves the current list of snapshots.<br>
	 * This method returns a map containing snapshot data.
	 * @return A {@code Map} where the key is an {@code Integer} and the value is a {@code StatSet}.
	 */
	public Map<Integer, StatSet> getSnapshotList()
	{
		return _snapshotList;
	}
	
	/**
	 * Retrieves the current ranking list for Olympiad.<br>
	 * The results are stored in a {@code Map}.
	 * @return A {@code Map} where the key is the character ID and the value is the {@code StatSet}.
	 */
	public Map<Integer, StatSet> getOlyRankList()
	{
		return _mainOlyList;
	}
	
	/**
	 * Retrieves the current snapshot of Olympiad rankings.<br>
	 * This method returns a map containing rank data for various heroes.
	 * @return A {@code Map} where the key is an {@code Integer} and the value is a {@code StatSet}.
	 */
	public Map<Integer, StatSet> getSnapshotOlyList()
	{
		return _snapshotOlyList;
	}
	
	/**
	 * Retrieves the current list of hero information snapshots.<br>
	 * This method returns a collection of {@code HeroInfo} objects.
	 * @return A {@code Collection} containing all snapshot hero data.
	 */
	public Collection<HeroInfo> getSnapshotHeroList()
	{
		return _snapshotHeroList;
	}
	
	/**
	 * Retrieves the current ranking list for PvP.<br>
	 * The data is stored in a {@code Map} where the key is the rank position.
	 * @return A {@code Map} containing the rank positions and their corresponding {@code StatSet}.
	 */
	public Map<Integer, StatSet> getPvpRankList()
	{
		return _mainPvpList;
	}
	
	/**
	 * Retrieves the current snapshot of the PvP rankings.<br>
	 * This method returns a pre-calculated map of ranks to avoid frequent database queries.
	 * @return A {@code Map} where the key is the rank and the value is the corresponding {@code StatSet}.
	 */
	public Map<Integer, StatSet> getSnapshotPvpRankList()
	{
		return _snapshotPvpList;
	}
	
	/**
	 * Retrieves the current ranking list for the conquest system.<br>
	 * The data is returned as a map where the key is the rank position.
	 * @return A {@code Map<Integer, StatSet>} containing the rankings.
	 */
	public Map<Integer, StatSet> getCurrentConquestRankList()
	{
		return _currentConquestList;
	}
	
	/**
	 * Retrieves the current snapshot of the conquest rankings.<br>
	 * This method returns a pre-calculated list of ranks for performance.
	 * @return A {@code Map} where the key is the character ID and the value is the {@code StatSet}.
	 */
	public Map<Integer, StatSet> getSnapshotCurrentConquestRankList()
	{
		return _snapshotCurrentConquestList;
	}
	
	/**
	 * Retrieves the ranking list from the previous conquest.<br>
	 * This method returns a map of character IDs to their corresponding {@code StatSet}.
	 * @return A {@code Map} containing the rankings for the previous conquest.
	 */
	public Map<Integer, StatSet> getPreviousConquestRankList()
	{
		return _previousConquestList;
	}
	
	/**
	 * Retrieves the snapshot of the previous conquest rank list.<br>
	 * This method returns a map containing character IDs and their corresponding statistics.
	 * @return A {@code Map} where the key is an {@code Integer} character ID and the value is a {@code StatSet}.
	 */
	public Map<Integer, StatSet> getSnapshotPreviousConquestRankList()
	{
		return _snapshotPreviousConquestList;
	}
	
	/**
	 * Retrieves the global rank of a specific player in the current conquest.<br>
	 * It searches through the {@code _currentConquestList} for the player's unique ID.
	 * @param player The {@code Player} object to check.
	 * @return The integer rank of the player, or 0 if they are not ranked.
	 */
	public int getPlayerConquestGlobalRank(Player player)
	{
		final int objectId = player.getObjectId();
		for (Entry<Integer, StatSet> entry : _currentConquestList.entrySet())
		{
			final StatSet stats = entry.getValue();
			if (stats.getInt("charId", 0) == objectId)
			{
				return entry.getKey();
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the global rank of a specific player.<br>
	 * This method checks if the {@code Player} belongs to the correct category.<br>
	 * It returns the cached rank or searches the main list for the position.<br>
	 * If no rank is found, it returns {@code 0}.
	 * @param player The {@code Player} object to check.
	 * @return The global rank of the player as an {@code int}.
	 */
	public int getPlayerGlobalRank(Player player)
	{
		if (!player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return 0;
		}
		
		final int objectId = player.getObjectId();
		final int cachedRank = _globalConsultingRankCache.getOrDefault(objectId, 0);
		if (cachedRank != 0)
		{
			return cachedRank;
		}
		
		for (Entry<Integer, StatSet> entry : _mainList.entrySet())
		{
			final StatSet stats = entry.getValue();
			if (stats.getInt("charId", 0) == objectId)
			{
				final int position = entry.getKey();
				_globalConsultingRankCache.put(objectId, position);
				
				return position;
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the rank of a player within their specific race.<br>
	 * This method checks if the {@code Player} belongs to the sixth class group.<br>
	 * It returns the cached rank or calculates it from the main list.<br>
	 * If no rank is found, it returns {@code 0}.
	 * @param player The {@code Player} object to check.
	 * @return The integer rank of the player or {@code 0} if not ranked.
	 */
	public int getPlayerRaceRank(Player player)
	{
		if (!player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return 0;
		}
		
		final int objectId = player.getObjectId();
		
		final int cachedRank = _raceConsultingRankCache.getOrDefault(objectId, 0);
		if (cachedRank != 0)
		{
			return cachedRank;
		}
		
		for (StatSet stats : _mainList.values())
		{
			if (stats.getInt("charId", 0) == objectId)
			{
				final int position = stats.getInt("raceRank", 0);
				_raceConsultingRankCache.put(objectId, position);
				return position;
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the class rank for a specific player.<br>
	 * This method checks if the {@code Player} belongs to the sixth class group.<br>
	 * It returns the rank from the main list or {@code 0} if not found.
	 * @param player The {@code Player} object to check.
	 * @return The integer rank of the player's class, or {@code 0} if they are not ranked.
	 */
	public int getPlayerClassRank(Player player)
	{
		if (!player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
		{
			return 0;
		}
		
		final int objectId = player.getObjectId();
		for (StatSet stats : _mainList.values())
		{
			if (stats.getInt("charId", 0) == objectId)
			{
				return stats.getInt("classRank", 0);
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the name of the top ranked player in conquest.
	 * @return The {@code String} name of the highest ranked conquest player.
	 */
	public String getConquestTopRankName()
	{
		return _conquestTopRankName;
	}
	
	/**
	 * Provides the global instance of the {@code RankManager}.<br>
	 * This method follows the singleton pattern to ensure only one manager exists.
	 * @return The single instance of {@code RankManager}.
	 */
	public static RankManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RankManager INSTANCE = new RankManager();
	}
}
