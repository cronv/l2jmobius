/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerTakeHero;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a {@code Hero} entity within the Olympiad mechanics.<br>
 * This class handles the data and behavior for heroes used during events.
 * @author godson
 */
public class Hero
{
	private static final Logger LOGGER = Logger.getLogger(Hero.class.getName());
	
	private static final DateTimeFormatter DIARY_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH");
	private static final DateTimeFormatter FIGHT_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	
	private static final String GET_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.legend_count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId AND heroes.played = 1";
	private static final String GET_ALL_HEROES = "SELECT heroes.charId, characters.char_name, heroes.class_id, heroes.count, heroes.legend_count, heroes.played, heroes.claimed FROM heroes, characters WHERE characters.charId = heroes.charId";
	private static final String UPDATE_ALL = "UPDATE heroes SET played = 0";
	private static final String INSERT_HERO = "INSERT INTO heroes (charId, class_id, count, legend_count, played, claimed) VALUES (?,?,?,?,?,?)";
	private static final String UPDATE_HERO = "UPDATE heroes SET class_id = ?, count = ?, legend_count = ?, played = ?, claimed = ? WHERE charId = ?";
	private static final String GET_CLAN_ALLY = "SELECT characters.clanid AS clanid, coalesce(clan_data.ally_Id, 0) AS allyId FROM characters LEFT JOIN clan_data ON clan_data.clan_id = characters.clanid WHERE characters.charId = ?";
	private static final String DELETE_ITEMS = "DELETE FROM items WHERE item_id IN (30392, 30393, 30394, 30395, 30396, 30397, 30398, 30399, 30400, 30401, 30402, 30403, 30404, 30405, 30372, 30373, 6842, 6611, 6612, 6613, 6614, 6615, 6616, 6617, 6618, 6619, 6620, 6621, 9388, 9389, 9390, 48551, 48552, 48554, 48555, 48556, 48557, 48558, 48559, 48560, 48561, 48562, 48563, 48564, 48565, 48566, 48567) AND owner_id NOT IN (SELECT charId FROM characters WHERE accesslevel > 0)";
	
	private static final Map<Integer, StatSet> HEROES = new ConcurrentHashMap<>();
	private static final Map<Integer, StatSet> COMPLETE_HEROS = new ConcurrentHashMap<>();
	private static final Map<Integer, StatSet> HERO_COUNTS = new ConcurrentHashMap<>();
	private static final Map<Integer, List<StatSet>> HERO_FIGHTS = new ConcurrentHashMap<>();
	private static final Map<Integer, List<StatSet>> HERO_DIARY = new ConcurrentHashMap<>();
	private static final Map<Integer, String> HERO_MESSAGE = new ConcurrentHashMap<>();
	
	public static final String COUNT = "count";
	public static final String LEGEND_COUNT = "legend_count";
	public static final String PLAYED = "played";
	public static final String CLAIMED = "claimed";
	public static final String CLAN_NAME = "clan_name";
	public static final String CLAN_CREST = "clan_crest";
	public static final String ALLY_NAME = "ally_name";
	public static final String ALLY_CREST = "ally_crest";
	
	public static final int ACTION_RAID_KILLED = 1;
	public static final int ACTION_HERO_GAINED = 2;
	public static final int ACTION_CASTLE_TAKEN = 3;
	
	/**
	 * Initializes the {@code Hero} object.<br>
	 * This constructor checks if olympiad features are enabled in the configuration.<br>
	 * If enabled, it calls the internal {@code init()} method to load data.
	 */
	protected Hero()
	{
		if (OlympiadConfig.OLYMPIAD_ENABLED)
		{
			init();
		}
	}
	
	/**
	 * Initializes the hero system by clearing existing data.<br>
	 * Loads hero information from the database into memory.<br>
	 * Populates the {@code HEROES} and {@code COMPLETE_HEROES} maps.
	 */
	private void init()
	{
		HEROES.clear();
		COMPLETE_HEROS.clear();
		HERO_COUNTS.clear();
		HERO_FIGHTS.clear();
		HERO_DIARY.clear();
		HERO_MESSAGE.clear();
		
		try (Connection con = DatabaseFactory.getConnection();
			Statement s1 = con.createStatement();
			ResultSet rset = s1.executeQuery(GET_HEROES);
			PreparedStatement ps = con.prepareStatement(GET_CLAN_ALLY);
			Statement s2 = con.createStatement();
			ResultSet rset2 = s2.executeQuery(GET_ALL_HEROES))
		{
			while (rset.next())
			{
				final StatSet hero = new StatSet();
				final int charId = rset.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset.getInt(COUNT));
				hero.set(LEGEND_COUNT, rset.getInt(LEGEND_COUNT));
				hero.set(PLAYED, rset.getInt(PLAYED));
				hero.set(CLAIMED, Boolean.parseBoolean(rset.getString(CLAIMED)));
				loadFights(charId);
				loadDiary(charId);
				loadMessage(charId);
				processHeros(ps, charId, hero);
				HEROES.put(charId, hero);
			}
			
			while (rset2.next())
			{
				final StatSet hero = new StatSet();
				final int charId = rset2.getInt(Olympiad.CHAR_ID);
				hero.set(Olympiad.CHAR_NAME, rset2.getString(Olympiad.CHAR_NAME));
				hero.set(Olympiad.CLASS_ID, rset2.getInt(Olympiad.CLASS_ID));
				hero.set(COUNT, rset2.getInt(COUNT));
				hero.set(LEGEND_COUNT, rset2.getInt(LEGEND_COUNT));
				hero.set(PLAYED, rset2.getInt(PLAYED));
				hero.set(CLAIMED, Boolean.parseBoolean(rset2.getString(CLAIMED)));
				processHeros(ps, charId, hero);
				COMPLETE_HEROS.put(charId, hero);
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not load Heroes: " + e.getMessage());
		}
		
		LOGGER.info("Hero System: Loaded " + HEROES.size() + " Heroes.");
		LOGGER.info("Hero System: Loaded " + COMPLETE_HEROS.size() + " all time Heroes.");
	}
	
	/**
	 * Updates the {@code StatSet} with clan and ally information from the database.<br>
	 * It uses the provided {@code PreparedStatement} to fetch data for a specific character.<br>
	 * The results are mapped to the hero's crest and name fields.
	 * @param ps The {@code PreparedStatement} used to execute the query.
	 * @param charId The unique identifier of the character.
	 * @param hero The {@code StatSet} object where the data will be stored.
	 * @throws SQLException If a database access error occurs.
	 */
	private void processHeros(PreparedStatement ps, int charId, StatSet hero) throws SQLException
	{
		ps.setInt(1, charId);
		try (ResultSet rs = ps.executeQuery())
		{
			if (rs.next())
			{
				final int clanId = rs.getInt("clanid");
				final int allyId = rs.getInt("allyId");
				String clanName = "";
				String allyName = "";
				int clanCrest = 0;
				int allyCrest = 0;
				if (clanId > 0)
				{
					clanName = ClanTable.getInstance().getClan(clanId).getName();
					clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
					if (allyId > 0)
					{
						allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
						allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
					}
				}
				
				hero.set(CLAN_CREST, clanCrest);
				hero.set(CLAN_NAME, clanName);
				hero.set(ALLY_CREST, allyCrest);
				hero.set(ALLY_NAME, allyName);
			}
			
			ps.clearParameters();
		}
	}
	
	/**
	 * Converts a raw millisecond value into a formatted time string.<br>
	 * The result follows the {@code mm:ss} format.
	 * @param fightTimeValue The duration of the fight in milliseconds.
	 * @return A formatted string representing minutes and seconds.
	 */
	private String calcFightTime(long fightTimeValue)
	{
		final String format = String.format("%%0%dd", 2);
		final long fightTime = fightTimeValue / 1000;
		return String.format(format, (fightTime % 3600) / 60) + ":" + String.format(format, fightTime % 60);
	}
	
	/**
	 * Loads the custom message for a specific hero from the database.<br>
	 * The retrieved message is stored in the {@code HERO_MESSAGE} map using the provided character ID.<br>
	 * If an error occurs during the database query, a warning is logged.
	 * @param charId The unique identifier of the character to load the message for.
	 */
	public void loadMessage(int charId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT message FROM heroes WHERE charId=?"))
		{
			ps.setInt(1, charId);
			try (ResultSet rset = ps.executeQuery())
			{
				if (rset.next())
				{
					HERO_MESSAGE.put(charId, rset.getString("message"));
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not load Hero Message for CharId: " + charId + ": " + e.getMessage());
		}
	}
	
	/**
	 * Loads the hero diary entries from the database for a specific character.<br>
	 * It retrieves all actions performed by the hero and stores them in a list.<br>
	 * The data is then cached in the {@code HERO_DIARY} map.
	 * @param charId The unique identifier of the character to load.
	 */
	public void loadDiary(int charId)
	{
		final List<StatSet> diary = new ArrayList<>();
		int diaryentries = 0;
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM  heroes_diary WHERE charId=? ORDER BY time ASC"))
		{
			ps.setInt(1, charId);
			try (ResultSet rset = ps.executeQuery())
			{
				while (rset.next())
				{
					final StatSet diaryEntry = new StatSet();
					final long time = rset.getLong("time");
					final int action = rset.getInt("action");
					final int param = rset.getInt("param");
					final String date = DIARY_DATE_FORMAT.format(Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()));
					diaryEntry.set("date", date);
					if (action == ACTION_RAID_KILLED)
					{
						final NpcTemplate template = NpcData.getInstance().getTemplate(param);
						if (template != null)
						{
							diaryEntry.set("action", template.getName() + " was defeated");
						}
					}
					else if (action == ACTION_HERO_GAINED)
					{
						diaryEntry.set("action", "Gained Hero status");
					}
					else if (action == ACTION_CASTLE_TAKEN)
					{
						final Castle castle = CastleManager.getInstance().getCastleById(param);
						if (castle != null)
						{
							diaryEntry.set("action", castle.getName() + " Castle was successfuly taken");
						}
					}
					
					diary.add(diaryEntry);
					diaryentries++;
				}
			}
			
			HERO_DIARY.put(charId, diary);
			
			LOGGER.info("Hero System: Loaded " + diaryentries + " diary entries for Hero: " + PlayerInfoTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not load Hero Diary for CharId: " + charId + ": " + e.getMessage());
		}
	}
	
	/**
	 * Loads the fight history for a specific character from the database.<br>
	 * This method retrieves all matches involving the given {@code charId}.<br>
	 * It calculates total victories, losses, and draws for the hero.<br>
	 * The results are stored in the {@code HERO_COUNTS} and {@code HERO_FIGHTS} maps.
	 * @param charId The unique identifier of the character to load.
	 */
	public void loadFights(int charId)
	{
		final List<StatSet> fights = new ArrayList<>();
		final StatSet heroCountData = new StatSet();
		final Calendar data = Calendar.getInstance();
		data.set(Calendar.DAY_OF_MONTH, 1);
		data.set(Calendar.HOUR_OF_DAY, 0);
		data.set(Calendar.MINUTE, 0);
		data.set(Calendar.MILLISECOND, 0);
		
		final long from = data.getTimeInMillis();
		int numberOfFights = 0;
		int victories = 0;
		int losses = 0;
		int draws = 0;
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM olympiad_fights WHERE (charOneId=? OR charTwoId=?) AND start<? ORDER BY start ASC"))
		{
			ps.setInt(1, charId);
			ps.setInt(2, charId);
			ps.setLong(3, from);
			try (ResultSet rset = ps.executeQuery())
			{
				int charOneId;
				int charOneClass;
				int charTwoId;
				int charTwoClass;
				int winner;
				long start;
				long time;
				int classed;
				while (rset.next())
				{
					charOneId = rset.getInt("charOneId");
					charOneClass = rset.getInt("charOneClass");
					charTwoId = rset.getInt("charTwoId");
					charTwoClass = rset.getInt("charTwoClass");
					winner = rset.getInt("winner");
					start = rset.getLong("start");
					time = rset.getLong("time");
					classed = rset.getInt("classed");
					if (charId == charOneId)
					{
						final String name = PlayerInfoTable.getInstance().getNameById(charTwoId);
						final String cls = ClassListData.getInstance().getClass(charTwoClass).getClassName();
						if ((name != null) && (cls != null))
						{
							final StatSet fight = new StatSet();
							fight.set("oponent", name);
							fight.set("oponentclass", cls);
							fight.set("time", calcFightTime(time));
							final String date = FIGHT_DATE_FORMAT.format(Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()));
							fight.set("start", date);
							fight.set("classed", classed);
							switch (winner)
							{
								case 1:
								{
									fight.set("result", "<font color=\"00ff00\">victory</font>");
									victories++;
									break;
								}
								case 2:
								{
									fight.set("result", "<font color=\"ff0000\">loss</font>");
									losses++;
									break;
								}
								case 0:
								{
									fight.set("result", "<font color=\"ffff00\">draw</font>");
									draws++;
									break;
								}
							}
							
							fights.add(fight);
							numberOfFights++;
						}
					}
					else if (charId == charTwoId)
					{
						final String name = PlayerInfoTable.getInstance().getNameById(charOneId);
						final String cls = ClassListData.getInstance().getClass(charOneClass).getClassName();
						if ((name != null) && (cls != null))
						{
							final StatSet fight = new StatSet();
							fight.set("oponent", name);
							fight.set("oponentclass", cls);
							fight.set("time", calcFightTime(time));
							final String date = FIGHT_DATE_FORMAT.format(Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()));
							fight.set("start", date);
							fight.set("classed", classed);
							switch (winner)
							{
								case 1:
								{
									fight.set("result", "<font color=\"ff0000\">loss</font>");
									losses++;
									break;
								}
								case 2:
								{
									fight.set("result", "<font color=\"00ff00\">victory</font>");
									victories++;
									break;
								}
								case 0:
								{
									fight.set("result", "<font color=\"ffff00\">draw</font>");
									draws++;
									break;
								}
							}
							
							fights.add(fight);
							numberOfFights++;
						}
					}
				}
			}
			
			heroCountData.set("victory", victories);
			heroCountData.set("draw", draws);
			heroCountData.set("loss", losses);
			HERO_COUNTS.put(charId, heroCountData);
			HERO_FIGHTS.put(charId, fights);
			
			LOGGER.info("Hero System: Loaded " + numberOfFights + " fights for Hero: " + PlayerInfoTable.getInstance().getNameById(charId));
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not load Hero fights history for CharId: " + charId + ": " + e);
		}
	}
	
	/**
	 * Retrieves the collection of heroes. <br>
	 * This method returns a map where the key is the character ID and the value is the {@code StatSet}.
	 * @return A {@code Map} containing all hero data.
	 */
	public Map<Integer, StatSet> getHeroes()
	{
		return HEROES;
	}
	
	/**
	 * Retrieves the collection of completed heroes.<br>
	 * This method returns a map containing all hero data that has been finished.
	 * @return A {@code Map<Integer, StatSet>} where the key is the character ID and the value is the {@code StatSet}.
	 */
	public Map<Integer, StatSet> getCompleteHeroes()
	{
		return COMPLETE_HEROS;
	}
	
	/**
	 * Finds a hero based on their specific class ID.<br>
	 * It searches through the internal list of heroes.<br>
	 * Returns the unique identifier for the matching hero.
	 * @param classid The {@code int} value representing the class to search for.
	 * @return The {@code int} ID of the found hero, or 0 if no match is found.
	 */
	public int getHeroByClass(int classid)
	{
		for (Entry<Integer, StatSet> e : HEROES.entrySet())
		{
			if (e.getValue().getInt(Olympiad.CLASS_ID) == classid)
			{
				return e.getKey();
			}
		}
		
		return 0;
	}
	
	/**
	 * Clears all internal data structures for heroes.<br>
	 * This method empties the {@code HERO_DIARY}, {@code HERO_FIGHTS}, {@code HERO_COUNTS}, and {@code HERO_MESSAGE} maps.
	 */
	public void resetData()
	{
		HERO_DIARY.clear();
		HERO_FIGHTS.clear();
		HERO_COUNTS.clear();
		HERO_MESSAGE.clear();
	}
	
	/**
	 * Displays the hero diary to a specific player.<br>
	 * It retrieves the diary entries for a character and formats them into an HTML message.<br>
	 * The method handles pagination and replaces placeholders with actual data like dates and actions.
	 * @param player The {@code Player} who will receive the diary message.
	 * @param heroclass The class ID of the hero.
	 * @param charid The unique character ID for the hero.
	 * @param page The specific page number to display.
	 */
	public void showHeroDiary(Player player, int heroclass, int charid, int page)
	{
		final int perpage = 10;
		final List<StatSet> mainList = HERO_DIARY.get(charid);
		if (mainList != null)
		{
			final NpcHtmlMessage diaryReply = new NpcHtmlMessage();
			final String htmContent = HtmCache.getInstance().getHtm(player, "data/html/olympiad/herodiary.htm");
			final String heroMessage = HERO_MESSAGE.get(charid);
			if ((htmContent != null) && (heroMessage != null))
			{
				diaryReply.setHtml(htmContent);
				diaryReply.replace("%heroname%", PlayerInfoTable.getInstance().getNameById(charid));
				diaryReply.replace("%message%", heroMessage);
				diaryReply.disableValidation();
				
				if (!mainList.isEmpty())
				{
					final List<StatSet> list = new ArrayList<>(mainList);
					Collections.reverse(list);
					
					boolean color = true;
					final StringBuilder fList = new StringBuilder(500);
					int counter = 0;
					int breakat = 0;
					final int listSize = list.size();
					for (int i = (page - 1) * perpage; i < listSize; i++)
					{
						breakat = i;
						final StatSet diaryEntry = list.get(i);
						fList.append("<tr><td>");
						if (color)
						{
							fList.append("<table width=270 bgcolor=\"131210\">");
						}
						else
						{
							fList.append("<table width=270>");
						}
						fList.append("<tr><td width=270><font color=\"LEVEL\">" + diaryEntry.getString("date") + ":xx</font></td></tr>");
						fList.append("<tr><td width=270>" + diaryEntry.getString("action", "") + "</td></tr>");
						fList.append("<tr><td>&nbsp;</td></tr></table>");
						fList.append("</td></tr>");
						color = !color;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}
					
					if (breakat < (listSize - 1))
					{
						diaryReply.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						diaryReply.replace("%buttprev%", "");
					}
					
					if (page > 1)
					{
						diaryReply.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _diary?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						diaryReply.replace("%buttnext%", "");
					}
					
					diaryReply.replace("%list%", fList.toString());
				}
				else
				{
					diaryReply.replace("%list%", "");
					diaryReply.replace("%buttprev%", "");
					diaryReply.replace("%buttnext%", "");
				}
				
				player.sendPacket(diaryReply);
			}
		}
	}
	
	/**
	 * Displays the hero fight history to a specific player.<br>
	 * It retrieves records from the database and formats them into an HTML message.<br>
	 * The method handles pagination for long lists of fights.
	 * @param player The {@code Player} who will receive the message.
	 * @param heroclass The ID of the hero class to filter by.
	 * @param charid The unique identifier of the character whose history is being viewed.
	 * @param page The specific page number to display.
	 */
	public void showHeroFights(Player player, int heroclass, int charid, int page)
	{
		final int perpage = 20;
		int win = 0;
		int loss = 0;
		int draw = 0;
		
		final List<StatSet> heroFights = HERO_FIGHTS.get(charid);
		if (heroFights != null)
		{
			final NpcHtmlMessage fightReply = new NpcHtmlMessage();
			final String htmContent = HtmCache.getInstance().getHtm(player, "data/html/olympiad/herohistory.htm");
			if (htmContent != null)
			{
				fightReply.setHtml(htmContent);
				fightReply.replace("%heroname%", PlayerInfoTable.getInstance().getNameById(charid));
				final int heroFightSize = heroFights.size();
				if (!heroFights.isEmpty())
				{
					final StatSet heroCount = HERO_COUNTS.get(charid);
					if (heroCount != null)
					{
						win = heroCount.getInt("victory");
						loss = heroCount.getInt("loss");
						draw = heroCount.getInt("draw");
					}
					
					boolean color = true;
					final StringBuilder fList = new StringBuilder(500);
					int counter = 0;
					int breakat = 0;
					for (int i = (page - 1) * perpage; i < heroFightSize; i++)
					{
						breakat = i;
						final StatSet fight = heroFights.get(i);
						fList.append("<tr><td>");
						if (color)
						{
							fList.append("<table width=270 bgcolor=\"131210\">");
						}
						else
						{
							fList.append("<table width=270>");
						}
						fList.append("<tr><td width=220><font color=\"LEVEL\">" + fight.getString("start") + "</font>&nbsp;&nbsp;" + fight.getString("result") + "</td><td width=50 align=right>" + (fight.getInt("classed") > 0 ? "<font color=\"FFFF99\">cls</font>" : "<font color=\"999999\">non-cls<font>") + "</td></tr>");
						fList.append("<tr><td width=220>vs " + fight.getString("oponent") + " (" + fight.getString("oponentclass") + ")</td><td width=50 align=right>(" + fight.getString("time") + ")</td></tr>");
						fList.append("<tr><td colspan=2>&nbsp;</td></tr></table>");
						fList.append("</td></tr>");
						color = !color;
						counter++;
						if (counter >= perpage)
						{
							break;
						}
					}
					
					if (breakat < (heroFightSize - 1))
					{
						fightReply.replace("%buttprev%", "<button value=\"Prev\" action=\"bypass _match?class=" + heroclass + "&page=" + (page + 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						fightReply.replace("%buttprev%", "");
					}
					
					if (page > 1)
					{
						fightReply.replace("%buttnext%", "<button value=\"Next\" action=\"bypass _match?class=" + heroclass + "&page=" + (page - 1) + "\" width=60 height=25 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\">");
					}
					else
					{
						fightReply.replace("%buttnext%", "");
					}
					
					fightReply.replace("%list%", fList.toString());
				}
				else
				{
					fightReply.replace("%list%", "");
					fightReply.replace("%buttprev%", "");
					fightReply.replace("%buttnext%", "");
				}
				
				fightReply.replace("%win%", String.valueOf(win));
				fightReply.replace("%draw%", String.valueOf(draw));
				fightReply.replace("%loos%", String.valueOf(loss));
				player.sendPacket(fightReply);
			}
		}
	}
	
	/**
	 * Updates the hero data for all players in the world.<br>
	 * This method clears existing heroes and removes associated items from inventories.<br>
	 * It then populates the {@code HEROES} map with the provided new hero statistics.
	 * @param newHeroes The list of {@code StatSet} objects containing the new hero data to be processed.
	 */
	public synchronized void computeNewHeroes(List<StatSet> newHeroes)
	{
		updateHeroes(true);
		
		for (Integer objectId : HEROES.keySet())
		{
			final Player player = World.getPlayer(objectId);
			if (player == null)
			{
				continue;
			}
			
			player.setHero(false);
			if (player.isLegend())
			{
				player.setLegend(false);
			}
			
			for (int i = 0; i < Inventory.PAPERDOLL_TOTALSLOTS; i++)
			{
				final Item equippedItem = player.getInventory().getPaperdollItem(i);
				if ((equippedItem != null) && equippedItem.isHeroItem())
				{
					player.getInventory().unEquipItemInSlot(i);
				}
			}
			
			final InventoryUpdate iu = new InventoryUpdate();
			for (Item item : player.getInventory().getAvailableItems(false, false, false))
			{
				if ((item != null) && item.isHeroItem())
				{
					player.destroyItem(ItemProcessType.DESTROY, item, null, true);
					iu.addRemovedItem(item);
				}
			}
			
			if (!iu.getItems().isEmpty())
			{
				player.sendInventoryUpdate(iu);
			}
			
			player.broadcastUserInfo();
		}
		
		deleteItemsInDb();
		HEROES.clear();
		
		if (newHeroes.isEmpty())
		{
			return;
		}
		
		for (StatSet hero : newHeroes)
		{
			final int legendId = RankManager.getInstance().getOlyRankList().get(1).getInt("charId", 0);
			final int charId = hero.getInt(Olympiad.CHAR_ID);
			final StatSet oldHero = COMPLETE_HEROS.get(charId);
			if (oldHero != null)
			{
				final int count = oldHero.getInt(COUNT);
				oldHero.set(COUNT, count + 1);
				if (charId == legendId)
				{
					final int legendCount = oldHero.getInt(LEGEND_COUNT);
					oldHero.set(LEGEND_COUNT, legendCount + 1);
				}
				
				oldHero.set(PLAYED, 1);
				oldHero.set(CLAIMED, false);
				HEROES.put(charId, oldHero);
			}
			else
			{
				final StatSet newHero = new StatSet();
				newHero.set(Olympiad.CHAR_NAME, hero.getString(Olympiad.CHAR_NAME));
				newHero.set(Olympiad.CLASS_ID, hero.getInt(Olympiad.CLASS_ID));
				newHero.set(COUNT, 1);
				if (charId == legendId)
				{
					newHero.set(LEGEND_COUNT, 1);
				}
				
				newHero.set(PLAYED, 1);
				newHero.set(CLAIMED, false);
				HEROES.put(charId, newHero);
			}
		}
		
		updateHeroes(false);
	}
	
	/**
	 * Updates the hero data in the database.<br>
	 * If {@code setDefault} is {@code true}, it resets all heroes using a bulk update.<br>
	 * If {@code setDefault} is {@code false}, it synchronizes individual hero stats and clan information.
	 * @param setDefault Determines whether to perform a full reset or a detailed synchronization.
	 */
	public void updateHeroes(boolean setDefault)
	{
		try (Connection con = DatabaseFactory.getConnection())
		{
			if (setDefault)
			{
				try (Statement s = con.createStatement())
				{
					s.executeUpdate(UPDATE_ALL);
				}
			}
			else
			{
				StatSet hero;
				int heroId;
				for (Entry<Integer, StatSet> entry : HEROES.entrySet())
				{
					hero = entry.getValue();
					heroId = entry.getKey();
					if (!COMPLETE_HEROS.containsKey(heroId))
					{
						try (PreparedStatement insert = con.prepareStatement(INSERT_HERO))
						{
							insert.setInt(1, heroId);
							insert.setInt(2, hero.getInt(Olympiad.CLASS_ID));
							insert.setInt(3, hero.getInt(COUNT, 0));
							insert.setInt(4, hero.getInt(LEGEND_COUNT, 0));
							insert.setInt(5, hero.getInt(PLAYED, 0));
							insert.setString(6, String.valueOf(hero.getBoolean(CLAIMED, false)));
							insert.execute();
							insert.close();
						}
						
						try (PreparedStatement statement = con.prepareStatement(GET_CLAN_ALLY))
						{
							statement.setInt(1, heroId);
							try (ResultSet rset = statement.executeQuery())
							{
								if (rset.next())
								{
									final int clanId = rset.getInt("clanid");
									final int allyId = rset.getInt("allyId");
									String clanName = "";
									String allyName = "";
									int clanCrest = 0;
									int allyCrest = 0;
									if (clanId > 0)
									{
										clanName = ClanTable.getInstance().getClan(clanId).getName();
										clanCrest = ClanTable.getInstance().getClan(clanId).getCrestId();
										if (allyId > 0)
										{
											allyName = ClanTable.getInstance().getClan(clanId).getAllyName();
											allyCrest = ClanTable.getInstance().getClan(clanId).getAllyCrestId();
										}
									}
									
									hero.set(CLAN_CREST, clanCrest);
									hero.set(CLAN_NAME, clanName);
									hero.set(ALLY_CREST, allyCrest);
									hero.set(ALLY_NAME, allyName);
								}
							}
						}
						
						HEROES.put(heroId, hero);
						COMPLETE_HEROS.put(heroId, hero);
					}
					else
					{
						try (PreparedStatement statement = con.prepareStatement(UPDATE_HERO))
						{
							statement.setInt(1, hero.getInt(Olympiad.CLASS_ID));
							statement.setInt(2, hero.getInt(COUNT, 0));
							statement.setInt(3, hero.getInt(LEGEND_COUNT, 0));
							statement.setInt(4, hero.getInt(PLAYED, 0));
							statement.setString(5, String.valueOf(hero.getBoolean(CLAIMED, false)));
							statement.setInt(6, heroId);
							statement.execute();
						}
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Hero System: Could not update Heroes: " + e.getMessage());
		}
	}
	
	/**
	 * Updates the diary data for a specific character.<br>
	 * This method marks that a hero has been gained by the player.
	 * @param charId The unique identifier of the character.
	 */
	public void setHeroGained(int charId)
	{
		setDiaryData(charId, ACTION_HERO_GAINED, 0);
	}
	
	/**
	 * Records a Raid Boss kill for a specific character.<br>
	 * This method updates the hero diary with the defeat of an NPC.<br>
	 * It adds a new entry containing the date and the defeated monster name.
	 * @param charId The unique identifier of the player character.
	 * @param npcId The unique identifier of the Raid Boss NPC.
	 */
	public void setRBkilled(int charId, int npcId)
	{
		setDiaryData(charId, ACTION_RAID_KILLED, npcId);
		
		final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
		final List<StatSet> list = HERO_DIARY.get(charId);
		if ((list == null) || (template == null))
		{
			return;
		}
		
		// Prepare new data
		final StatSet diaryEntry = new StatSet();
		final String date = DIARY_DATE_FORMAT.format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()));
		diaryEntry.set("date", date);
		diaryEntry.set("action", template.getName() + " was defeated");
		
		// Add to old list.
		list.add(diaryEntry);
	}
	
	/**
	 * Records that a specific character has captured a castle.<br>
	 * This method updates the hero diary with a new entry.<br>
	 * It also adds a success message to the character's history.
	 * @param charId The unique identifier of the character.
	 * @param castleId The unique identifier of the castle taken.
	 */
	public void setCastleTaken(int charId, int castleId)
	{
		setDiaryData(charId, ACTION_CASTLE_TAKEN, castleId);
		
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		final List<StatSet> list = HERO_DIARY.get(charId);
		if ((list == null) || (castle == null))
		{
			return;
		}
		
		// Prepare new data
		final StatSet diaryEntry = new StatSet();
		final String date = DIARY_DATE_FORMAT.format(Instant.ofEpochMilli(System.currentTimeMillis()).atZone(ZoneId.systemDefault()));
		diaryEntry.set("date", date);
		diaryEntry.set("action", castle.getName() + " Castle was successfuly taken");
		
		// Add to old list.
		list.add(diaryEntry);
	}
	
	/**
	 * Saves a new entry into the hero diary database.<br>
	 * This method records an action performed by a character at the current time.
	 * @param charId The unique identifier of the character.
	 * @param action The type of action to record.
	 * @param param An additional parameter associated with the action.
	 */
	public void setDiaryData(int charId, int action, int param)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO heroes_diary (charId, time, action, param) values(?,?,?,?)"))
		{
			ps.setInt(1, charId);
			ps.setLong(2, System.currentTimeMillis());
			ps.setInt(3, action);
			ps.setInt(4, param);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.severe("SQL exception while saving DiaryData: " + e.getMessage());
		}
	}
	
	/**
	 * Sets a custom message for a specific {@code Player}.<br>
	 * This message is stored using the player's unique object ID.
	 * @param player The {@code Player} who will receive the message.
	 * @param message The {@code String} content to be displayed or stored.
	 */
	public void setHeroMessage(Player player, String message)
	{
		HERO_MESSAGE.put(player.getObjectId(), message);
	}
	
	/**
	 * Saves the hero message for a specific character to the database.<br>
	 * This method updates the {@code heroes} table using the {@code charId}.<br>
	 * It only performs an update if a message exists for that ID.
	 * @param charId The unique identifier of the character.
	 */
	public void saveHeroMessage(int charId)
	{
		if (!HERO_MESSAGE.containsKey(charId))
		{
			return;
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE heroes SET message=? WHERE charId=?;"))
		{
			ps.setString(1, HERO_MESSAGE.get(charId));
			ps.setInt(2, charId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.severe("SQL exception while saving HeroMessage:" + e.getMessage());
		}
	}
	
	/**
	 * Removes items from the database.<br>
	 * This method executes a SQL delete query to clear specific data.<br>
	 * It catches any {@code SQLException} and logs a warning message.
	 */
	private void deleteItemsInDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement s = con.createStatement())
		{
			s.executeUpdate(DELETE_ITEMS);
		}
		catch (SQLException e)
		{
			LOGGER.warning("Heroes: " + e.getMessage());
		}
	}
	
	/**
	 * Saves all hero messages to the database.<br>
	 * This method iterates through the {@code HERO_MESSAGE} map.<br>
	 * It calls {@code saveHeroMessage} for every entry.
	 */
	public void shutdown()
	{
		HERO_MESSAGE.keySet().forEach(this::saveHeroMessage);
	}
	
	/**
	 * Checks if a specific object is currently claimed as a hero.<br>
	 * It verifies the existence of the ID in the {@code HEROES} map.<br>
	 * It also checks if the {@code CLAIMED} status is true.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the object is a claimed hero, {@code false} otherwise.
	 */
	public boolean isHero(int objectId)
	{
		return HEROES.containsKey(objectId) && HEROES.get(objectId).getBoolean(CLAIMED);
	}
	
	/**
	 * Checks if a specific hero is currently available for claiming.<br>
	 * It verifies that the hero exists in the {@code HEROES} map and has not been claimed.
	 * @param objectId The unique identifier of the hero to check.
	 * @return {@code true} if the hero exists and is unclaimed, {@code false} otherwise.
	 */
	public boolean isUnclaimedHero(int objectId)
	{
		return HEROES.containsKey(objectId) && !HEROES.get(objectId).getBoolean(CLAIMED);
	}
	
	/**
	 * Marks a {@code Player} as a hero.<br>
	 * This method updates the player status and grants clan reputation points if applicable.<br>
	 * It also triggers animations and reloads relevant hero data.
	 * @param player The {@code Player} who is claiming the hero status.
	 */
	public void claimHero(Player player)
	{
		final int objectId = player.getObjectId();
		StatSet hero = HEROES.get(objectId);
		if (hero == null)
		{
			hero = new StatSet();
			HEROES.put(objectId, hero);
		}
		
		hero.set(CLAIMED, true);
		
		final Clan clan = player.getClan();
		if ((clan != null) && (clan.getLevel() >= 5))
		{
			clan.addReputationScore(FeatureConfig.HERO_POINTS);
			final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_MEMBER_C1_HAS_BECOME_THE_HERO_CLAN_REPUTATION_POINTS_S2);
			sm.addString(PlayerInfoTable.getInstance().getNameById(objectId));
			sm.addInt(FeatureConfig.HERO_POINTS);
			clan.broadcastToOnlineMembers(sm);
		}
		
		player.setHero(true);
		
		final StatSet legendRank = RankManager.getInstance().getOlyRankList().get(1);
		if (legendRank != null)
		{
			final int legendId = legendRank.getInt("charId", 0);
			if (objectId == legendId)
			{
				player.setLegend(true);
				player.getVariables().set(ALLY_NAME, false);
				if ((clan != null) && (clan.getLevel() >= 5))
				{
					clan.addReputationScore(100000);
					final SystemMessage sm = new SystemMessage(SystemMessageId.CLAN_MEMBER_C1_HAS_BECOME_THE_HERO_CLAN_REPUTATION_POINTS_S2);
					sm.addString(PlayerInfoTable.getInstance().getNameById(objectId));
					sm.addInt(FeatureConfig.HERO_POINTS);
					clan.broadcastToOnlineMembers(sm);
				}
			}
		}
		
		player.broadcastPacket(new SocialAction(objectId, 20016)); // Hero Animation
		player.broadcastUserInfo();
		
		// Set Gained hero and reload data.
		setHeroGained(objectId);
		loadFights(objectId);
		loadDiary(objectId);
		HERO_MESSAGE.put(objectId, "");
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_TAKE_HERO))
		{
			EventDispatcher.getInstance().notifyEvent(new OnPlayerTakeHero(player));
		}
		
		updateHeroes(false);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code Hero} class.<br>
	 * This method ensures that only one instance of {@code Hero} exists in memory.
	 * @return The single instance of {@code Hero}.
	 */
	public static Hero getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final Hero INSTANCE = new Hero();
	}
}
