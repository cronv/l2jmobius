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
package org.l2jmobius.gameserver.data;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.CaptchaRequest;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.captcha.ReceiveBotCaptchaResult;

/**
 * This class manages the reporting and tracking of bot-like behavior in the game.<br>
 * It provides methods to identify, log, and handle suspicious activities based on predefined criteria.
 * @author BiggBoss
 */
public class BotReportTable
{
	// Zoey76: TODO: Split XML parsing from SQL operations, use IGameXmlReader instead of SAXParser.
	protected static final Logger LOGGER = Logger.getLogger(BotReportTable.class.getName());
	
	private static final int COLUMN_BOT_ID = 1;
	private static final int COLUMN_REPORTER_ID = 2;
	private static final int COLUMN_REPORT_TIME = 3;
	
	public static final int ATTACK_ACTION_BLOCK_ID = -1;
	public static final int TRADE_ACTION_BLOCK_ID = -2;
	public static final int PARTY_ACTION_BLOCK_ID = -3;
	public static final int ACTION_BLOCK_ID = -4;
	public static final int CHAT_BLOCK_ID = -5;
	
	private static final String SQL_LOAD_REPORTED_CHAR_DATA = "SELECT * FROM bot_reported_char_data";
	private static final String SQL_INSERT_REPORTED_CHAR_DATA = "INSERT INTO bot_reported_char_data VALUES (?,?,?)";
	private static final String SQL_CLEAR_REPORTED_CHAR_DATA = "DELETE FROM bot_reported_char_data";
	
	private Map<Integer, Long> _ipRegistry;
	private Map<Integer, ReporterCharData> _charRegistry;
	private Map<Integer, ReportedCharData> _reports;
	private Map<Integer, PunishHolder> _punishments;
	
	/**
	 * Initializes the {@code BotReportTable} instance.<br>
	 * This constructor checks if bot reporting is enabled in {@code GeneralConfig}.<br>
	 * It sets up internal registries and loads punishment data from an XML file.
	 */
	protected BotReportTable()
	{
		if (GeneralConfig.BOTREPORT_ENABLE)
		{
			_ipRegistry = new HashMap<>();
			_charRegistry = new ConcurrentHashMap<>();
			_reports = new ConcurrentHashMap<>();
			_punishments = new ConcurrentHashMap<>();
			
			try
			{
				final File punishments = new File("./config/BotReportPunishments.xml");
				if (!punishments.exists())
				{
					throw new FileNotFoundException(punishments.getName());
				}
				
				final SAXParser parser = SAXParserFactory.newInstance().newSAXParser();
				parser.parse(punishments, new PunishmentsLoader());
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load punishments from /config/BotReportPunishments.xml", e);
			}
			
			loadReportedCharData();
			scheduleResetPointTask();
		}
	}
	
	/**
	 * Loads the reported character data from the database.<br>
	 * This method populates the internal reports map and updates reporter points.<br>
	 * It calculates the last reset time based on the configuration settings.
	 */
	private void loadReportedCharData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery(SQL_LOAD_REPORTED_CHAR_DATA))
		{
			long lastResetTime = 0;
			try
			{
				final int hour = Integer.parseInt(GeneralConfig.BOTREPORT_RESETPOINT_HOUR[0]);
				final int minute = Integer.parseInt(GeneralConfig.BOTREPORT_RESETPOINT_HOUR[1]);
				final long currentTime = System.currentTimeMillis();
				final Calendar calendar = Calendar.getInstance();
				calendar.set(Calendar.HOUR_OF_DAY, hour);
				calendar.set(Calendar.MINUTE, minute);
				if (currentTime < calendar.getTimeInMillis())
				{
					calendar.set(Calendar.DAY_OF_YEAR, calendar.get(Calendar.DAY_OF_YEAR) - 1);
				}
				
				lastResetTime = calendar.getTimeInMillis();
			}
			catch (Exception e)
			{
				// Ignore.
			}
			
			while (rset.next())
			{
				final int botId = rset.getInt(COLUMN_BOT_ID);
				final int reporter = rset.getInt(COLUMN_REPORTER_ID);
				final long date = rset.getLong(COLUMN_REPORT_TIME);
				final ReportedCharData existing = _reports.get(botId);
				if (existing != null)
				{
					existing.addReporter(reporter, date);
				}
				else
				{
					final ReportedCharData rcd = new ReportedCharData();
					rcd.addReporter(reporter, date);
					_reports.put(botId, rcd);
				}
				
				if (date > lastResetTime)
				{
					ReporterCharData rcd = _charRegistry.get(reporter);
					if (rcd != null)
					{
						rcd.setPoints(rcd.getPointsLeft() - 1);
					}
					else
					{
						rcd = new ReporterCharData();
						rcd.setPoints(6);
						_charRegistry.put(reporter, rcd);
					}
				}
			}
			
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + _reports.size() + " bot reports");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not load reported char data!", e);
		}
	}
	
	/**
	 * Saves the current reported character data to the database.<br>
	 * This method clears existing records before inserting new ones.<br>
	 * It iterates through all reports and their respective reporters.
	 */
	public void saveReportedCharData()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement st = con.createStatement();
			PreparedStatement ps = con.prepareStatement(SQL_INSERT_REPORTED_CHAR_DATA))
		{
			st.execute(SQL_CLEAR_REPORTED_CHAR_DATA);
			
			for (Entry<Integer, ReportedCharData> entrySet : _reports.entrySet())
			{
				for (int reporterId : entrySet.getValue()._reporters.keySet())
				{
					ps.setInt(1, entrySet.getKey());
					ps.setInt(2, reporterId);
					ps.setLong(3, entrySet.getValue()._reporters.get(reporterId));
					ps.execute();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Could not update reported char data in database!", e);
		}
	}
	
	/**
	 * Reports a target character as a bot.<br>
	 * This method validates the reporter and the target based on game rules.<br>
	 * It checks for zone restrictions, clan status, and report limits.<br>
	 * If successful, it updates the report data and notifies the players.
	 * @param reporter The {@code Player} who is submitting the report.
	 * @return {@code true} if the report was successfully submitted, {@code false} otherwise.
	 */
	public boolean reportBot(Player reporter)
	{
		final WorldObject target = reporter.getTarget();
		if (target == null)
		{
			return false;
		}
		
		final Creature bot = target.asCreature();
		final int objectId = reporter.getObjectId();
		if ((!bot.isPlayer() && !bot.isFakePlayer()) || (bot.isFakePlayer() && !bot.asNpc().getTemplate().getFakePlayerInfo().isTalkable()) || (target.getObjectId() == objectId))
		{
			return false;
		}
		
		if (bot.isInsideZone(ZoneId.PEACE) || bot.isInsideZone(ZoneId.PVP))
		{
			reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_A_CHARACTER_IN_THIS_AREA);
			return false;
		}
		
		if (bot.isPlayer() && bot.asPlayer().isInOlympiadMode())
		{
			reporter.sendPacket(SystemMessageId.THIS_CHARACTER_CANNOT_MAKE_A_REPORT_YOU_CANNOT_MAKE_A_REPORT_WHILE_LOCATED_INSIDE_A_PEACE_ZONE_OR_A_BATTLEGROUND_WHILE_YOU_ARE_AN_OPPOSING_CLAN_MEMBER_DURING_A_CLAN_WAR_OR_WHILE_PARTICIPATING_IN_THE_OLYMPIAD);
			return false;
		}
		
		final Clan botClan = bot.getClan();
		final Clan reporterClan = reporter.getClan();
		if ((botClan != null) && (reporterClan != null) && botClan.isAtWarWith(reporterClan))
		{
			reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_WHEN_A_CLAN_WAR_HAS_BEEN_DECLARED);
			return false;
		}
		
		if (bot.isPlayer() && (bot.asPlayer().getExp() == bot.asPlayer().getStat().getStartingExp()))
		{
			reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_A_CHARACTER_WHO_HAS_NOT_ACQUIRED_ANY_XP_AFTER_CONNECTING);
			return false;
		}
		
		ReportedCharData rcd = _reports.get(bot.getObjectId());
		ReporterCharData rcdRep = _charRegistry.get(objectId);
		final int reporterId = objectId;
		
		synchronized (this)
		{
			if (_reports.containsKey(reporterId))
			{
				reporter.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REPORTED_AS_AN_ILLEGAL_PROGRAM_USER_AND_CANNOT_REPORT_OTHER_USERS);
				return false;
			}
			
			final int ip = hashIp(reporter);
			if (!timeHasPassed(_ipRegistry, ip))
			{
				reporter.sendPacket(SystemMessageId.THIS_CHARACTER_CANNOT_MAKE_A_REPORT_THE_TARGET_HAS_ALREADY_BEEN_REPORTED_BY_EITHER_YOUR_CLAN_OR_ALLIANCE_OR_HAS_ALREADY_BEEN_REPORTED_FROM_YOUR_CURRENT_IP);
				return false;
			}
			
			if (rcd != null)
			{
				if (rcd.alredyReportedBy(reporterId))
				{
					reporter.sendPacket(SystemMessageId.YOU_CANNOT_REPORT_THIS_PERSON_AGAIN_AT_THIS_TIME);
					return false;
				}
				
				if (!GeneralConfig.BOTREPORT_ALLOW_REPORTS_FROM_SAME_CLAN_MEMBERS && rcd.reportedBySameClan(reporterClan))
				{
					reporter.sendPacket(SystemMessageId.THIS_CHARACTER_CANNOT_MAKE_A_REPORT_THE_TARGET_HAS_ALREADY_BEEN_REPORTED_BY_EITHER_YOUR_CLAN_OR_ALLIANCE_OR_HAS_ALREADY_BEEN_REPORTED_FROM_YOUR_CURRENT_IP);
					return false;
				}
			}
			
			if (rcdRep != null)
			{
				final int pointsLeft = rcdRep.getPointsLeft();
				if (pointsLeft == 0)
				{
					reporter.sendPacket(SystemMessageId.YOU_SPENT_ALL_AVAILABLE_POINTS_THEY_WILL_BECOME_AVAILABLE_WHEN_THEY_ARE_RESET_AT_06_30_AM_EVERY_DAY);
					return false;
				}
				
				final long reuse = (System.currentTimeMillis() - rcdRep.getLastReporTime());
				if (reuse < GeneralConfig.BOTREPORT_REPORT_DELAY)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_CAN_MAKE_ANOTHER_REPORT_IN_S1_MIN_YOU_HAVE_S2_POINT_S_LEFT);
					sm.addInt((int) (reuse / 60000));
					sm.addInt(pointsLeft);
					reporter.sendPacket(sm);
					return false;
				}
			}
			
			final long curTime = System.currentTimeMillis();
			if (rcd == null)
			{
				rcd = new ReportedCharData();
				_reports.put(bot.getObjectId(), rcd);
			}
			
			rcd.addReporter(reporterId, curTime);
			if (rcdRep == null)
			{
				rcdRep = new ReporterCharData();
			}
			
			rcdRep.registerReport(curTime);
			
			_ipRegistry.put(ip, curTime);
			_charRegistry.put(reporterId, rcdRep);
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.C1_WAS_REPORTED_AS_A_BOT);
		final String botName = bot.getName();
		sm.addString(botName);
		reporter.sendPacket(sm);
		
		sm = new SystemMessage(SystemMessageId.YOU_HAVE_USED_A_REPORT_POINT_ON_C1_YOU_HAVE_S2_POINTS_REMAINING_ON_THIS_ACCOUNT);
		sm.addString(botName);
		sm.addInt(rcdRep.getPointsLeft());
		reporter.sendPacket(sm);
		
		if (bot.isPlayer())
		{
			handleReport(bot.asPlayer(), rcd);
		}
		
		return true;
	}
	
	/**
	 * Processes a report for a specific bot.<br>
	 * This method determines the appropriate punishment based on the report count.<br>
	 * It applies punishments from both fixed counts and defined ranges.
	 * @param bot The {@code Player} object representing the bot being reported.
	 * @param rcd The {@code ReportedCharData} containing information about the report.
	 */
	private void handleReport(Player bot, ReportedCharData rcd)
	{
		final Integer reportCount = rcd.getReportCount();
		// Report count punishment
		punishBot(bot, _punishments.get(reportCount));
		
		// Range punishments
		for (Entry<Integer, PunishHolder> entry : _punishments.entrySet())
		{
			final int key = entry.getKey();
			if ((key < 0) && (Math.abs(key) <= reportCount))
			{
				punishBot(bot, entry.getValue());
			}
		}
		
		// For every three reports request captcha (if player is not auto playing).
		// if (!bot.isAutoPlaying() && ((rcd.getReportCount() % 3) == 0))
		// {
		// final Captcha captcha = CaptchaGenerator.getInstance().next();
		// if (!bot.hasRequest(CaptchaRequest.class))
		// {
		// final CaptchaRequest request = new CaptchaRequest(bot, captcha);
		// bot.addRequest(request);
		// bot.sendPacket(new ReceiveBotCaptchaImage(captcha, request.getRemainingTime()));
		// bot.sendPacket(SystemMessageId.PLEASE_ENTER_THE_AUTHENTICATION_CODE_IN_TIME_TO_CONTINUE_PLAYING);
		// }
		// }
	}
	
	/**
	 * Applies punishments to a specific bot.<br>
	 * It executes the effects defined in the {@code PunishHolder}.<br>
	 * It also sends a system message if one is configured.
	 * @param bot The {@code Player} object representing the bot to be punished.
	 * @param ph The {@code PunishHolder} containing the punishment details.
	 */
	private void punishBot(Player bot, PunishHolder ph)
	{
		if (ph != null)
		{
			ph._punish.applyEffects(bot, bot);
			if (ph._systemMessageId > -1)
			{
				final SystemMessageId id = SystemMessageId.getSystemMessageId(ph._systemMessageId);
				if (id != null)
				{
					bot.sendPacket(id);
				}
			}
		}
	}
	
	/**
	 * Applies a penalty to a player who failed the captcha.<br>
	 * This method triggers the bot report status effect.<br>
	 * It removes any active {@code CaptchaRequest} from the player.<br>
	 * Finally, it sends a failure message and packet to the player.
	 * @param bot The {@code Player} object to be punished.
	 */
	public void punishBotDueUnsolvedCaptcha(Player bot)
	{
		CommonSkill.BOT_REPORT_STATUS.getSkill().applyEffects(bot, bot);
		bot.removeRequest(CaptchaRequest.class);
		final SystemMessage msg = new SystemMessage(SystemMessageId.IF_A_USER_ENTERS_A_WRONG_AUTHENTICATION_CODE_3_TIMES_IN_A_ROW_OR_DOES_NOT_ENTER_THE_CODE_IN_TIME_THE_SYSTEM_WILL_QUALIFY_HIM_AS_A_RULE_BREAKER_AND_CHARGE_HIS_ACCOUNT_WITH_A_PENALTY_S1);
		msg.addSkillName(CommonSkill.BOT_REPORT_STATUS.getId());
		bot.sendPacket(msg);
		bot.sendPacket(ReceiveBotCaptchaResult.FAILED);
	}
	
	/**
	 * Adds a new punishment to the system.<br>
	 * This method links a specific skill and message to a required number of reports.<br>
	 * It validates that the skill exists before adding it to the internal map.
	 * @param neededReports The number of reports required to trigger this punishment.
	 * @param skillId The unique identifier for the skill to be used as punishment.
	 * @param skillLevel The level of the skill to be used as punishment.
	 * @param sysMsg The system message ID to display when the punishment is triggered.
	 */
	void addPunishment(int neededReports, int skillId, int skillLevel, int sysMsg)
	{
		final Skill sk = SkillData.getInstance().getSkill(skillId, skillLevel);
		if (sk != null)
		{
			_punishments.put(neededReports, new PunishHolder(sk, sysMsg));
		}
		else
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not add punishment for " + neededReports + " report(s): Skill " + skillId + "-" + skillLevel + " does not exist!");
		}
	}
	
	/**
	 * Resets the points for all reported characters.<br>
	 * This method sets every character's points to {@code 7}.<br>
	 * It also triggers a new scheduled task for resetting points.
	 */
	void resetPointsAndSchedule()
	{
		synchronized (_charRegistry)
		{
			for (ReporterCharData rcd : _charRegistry.values())
			{
				rcd.setPoints(7);
			}
		}
		
		scheduleResetPointTask();
	}
	
	/**
	 * Schedules the {@code ResetPointTask} to run at a specific time.<br>
	 * It reads the reset hour and minute from {@code GeneralConfig}.<br>
	 * If the calculated time has already passed today, it schedules for tomorrow.<br>
	 * A fallback of 24 hours is used if an error occurs during scheduling.
	 */
	private void scheduleResetPointTask()
	{
		try
		{
			final int hour = Integer.parseInt(GeneralConfig.BOTREPORT_RESETPOINT_HOUR[0]);
			final int minute = Integer.parseInt(GeneralConfig.BOTREPORT_RESETPOINT_HOUR[1]);
			final long currentTime = System.currentTimeMillis();
			final Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.HOUR_OF_DAY, hour);
			calendar.set(Calendar.MINUTE, minute);
			if (calendar.getTimeInMillis() < currentTime)
			{
				calendar.add(Calendar.DAY_OF_YEAR, 1);
			}
			
			ThreadPool.schedule(new ResetPointTask(), calendar.getTimeInMillis() - currentTime);
		}
		catch (Exception e)
		{
			ThreadPool.schedule(new ResetPointTask(), 24 * 3600 * 1000);
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not properly schedule bot report points reset task. Scheduled in 24 hours.", e);
		}
	}
	
	/**
	 * Converts a player's IP address into a unique integer hash.<br>
	 * This method parses the IP string from the {@code Player} object.<br>
	 * It combines the four octets into a single bitwise value.
	 * @param player The {@code Player} whose IP will be hashed.
	 * @return The resulting integer representation of the IP address.
	 */
	private static int hashIp(Player player)
	{
		final String con = player.getClient().getIp();
		final String[] rawByte = con.split("\\.");
		final int[] rawIp = new int[4];
		for (int i = 0; i < 4; i++)
		{
			rawIp[i] = Integer.parseInt(rawByte[i]);
		}
		
		return rawIp[0] | (rawIp[1] << 8) | (rawIp[2] << 16) | (rawIp[3] << 24);
	}
	
	/**
	 * Checks if a specific amount of time has passed for an object.<br>
	 * It compares the current system time against the stored timestamp in the {@code Map}.<br>
	 * The method returns {@code true} if no timestamp exists or if the delay exceeds the configured limit.
	 * @param map A {@code Map} containing object IDs and their corresponding timestamps.
	 * @param objectId The unique identifier for the object to check.
	 * @return {@code true} if the time has passed, otherwise {@code false}.
	 */
	private static boolean timeHasPassed(Map<Integer, Long> map, int objectId)
	{
		final Long time = map.get(objectId);
		return (time == null) || ((System.currentTimeMillis() - time.longValue()) > GeneralConfig.BOTREPORT_REPORT_DELAY);
	}
	
	/**
	 * Represents the info about a reporter
	 */
	private class ReporterCharData
	{
		private long _lastReport;
		private byte _reportPoints;
		
		ReporterCharData()
		{
			_reportPoints = 7;
			_lastReport = 0;
		}
		
		void registerReport(long time)
		{
			_reportPoints -= 1;
			_lastReport = time;
		}
		
		long getLastReporTime()
		{
			return _lastReport;
		}
		
		byte getPointsLeft()
		{
			return _reportPoints;
		}
		
		void setPoints(int points)
		{
			_reportPoints = (byte) points;
		}
	}
	
	/**
	 * Represents the info about a reported character
	 */
	private class ReportedCharData
	{
		Map<Integer, Long> _reporters;
		
		ReportedCharData()
		{
			_reporters = new HashMap<>();
		}
		
		int getReportCount()
		{
			return _reporters.size();
		}
		
		boolean alredyReportedBy(int objectId)
		{
			return _reporters.containsKey(objectId);
		}
		
		void addReporter(int objectId, long reportTime)
		{
			_reporters.put(objectId, reportTime);
		}
		
		boolean reportedBySameClan(Clan clan)
		{
			if (clan == null)
			{
				return false;
			}
			
			for (int reporterId : _reporters.keySet())
			{
				if (clan.isMember(reporterId))
				{
					return true;
				}
			}
			
			return false;
		}
	}
	
	/**
	 * SAX loader to parse /config/BotReportPunishments.xml file
	 */
	private class PunishmentsLoader extends DefaultHandler
	{
		PunishmentsLoader()
		{
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attr)
		{
			if (qName.equals("punishment"))
			{
				int reportCount = -1;
				int skillId = -1;
				int skillLevel = 1;
				int sysMessage = -1;
				try
				{
					reportCount = Integer.parseInt(attr.getValue("neededReportCount"));
					skillId = Integer.parseInt(attr.getValue("skillId"));
					final String level = attr.getValue("skillLevel");
					final String systemMessageId = attr.getValue("sysMessageId");
					if (level != null)
					{
						skillLevel = Integer.parseInt(level);
					}
					
					if (systemMessageId != null)
					{
						sysMessage = Integer.parseInt(systemMessageId);
					}
				}
				catch (Exception e)
				{
					LOGGER.warning("Problem with BotReportTable: " + e.getMessage());
				}
				
				addPunishment(reportCount, skillId, skillLevel, sysMessage);
			}
		}
	}
	
	private class PunishHolder
	{
		final Skill _punish;
		final int _systemMessageId;
		
		public PunishHolder(Skill sk, int sysMsg)
		{
			_punish = sk;
			_systemMessageId = sysMsg;
		}
	}
	
	private class ResetPointTask implements Runnable
	{
		public ResetPointTask()
		{
		}
		
		@Override
		public void run()
		{
			resetPointsAndSchedule();
		}
	}
	
	/**
	 * Provides the singleton instance of the {@code BotReportTable}.<br>
	 * Use this method to access the global bot reporting system.
	 * @return The single instance of {@code BotReportTable}.
	 */
	public static BotReportTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		static final BotReportTable INSTANCE = new BotReportTable();
	}
}
