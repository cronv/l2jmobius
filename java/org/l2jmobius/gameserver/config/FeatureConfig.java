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
package org.l2jmobius.gameserver.config;

import java.time.MonthDay;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeClientEffectInfo;

/**
 * This class is responsible for loading all feature-related configurations.<br>
 * It provides access to settings that control various game features.
 * @author Mobius
 */
public class FeatureConfig
{
	// File
	private static final String FEATURE_CONFIG_FILE = "./config/Feature.ini";
	
	/** Parses "MMM dd" strings such as "Dec 20" or "Feb 01". */
	private static final DateTimeFormatter MONTH_DAY_FMT = DateTimeFormatter.ofPattern("MMM dd", Locale.ENGLISH);
	
	// ------------------------------------------------------------------
	// Siege / Castle / Fortress constants (unchanged)
	// ------------------------------------------------------------------
	public static List<Integer> SIEGE_HOUR_LIST;
	public static int CASTLE_BUY_TAX_NEUTRAL;
	public static int CASTLE_BUY_TAX_LIGHT;
	public static int CASTLE_BUY_TAX_DARK;
	public static int CASTLE_SELL_TAX_NEUTRAL;
	public static int CASTLE_SELL_TAX_LIGHT;
	public static int CASTLE_SELL_TAX_DARK;
	public static long CS_TELE_FEE_RATIO;
	public static int CS_TELE1_FEE;
	public static int CS_TELE2_FEE;
	public static long CS_SUPPORT_FEE_RATIO;
	public static int CS_SUPPORT1_FEE;
	public static int CS_SUPPORT2_FEE;
	public static long CS_MPREG_FEE_RATIO;
	public static int CS_MPREG1_FEE;
	public static int CS_MPREG2_FEE;
	public static long CS_HPREG_FEE_RATIO;
	public static int CS_HPREG1_FEE;
	public static int CS_HPREG2_FEE;
	public static long CS_EXPREG_FEE_RATIO;
	public static int CS_EXPREG1_FEE;
	public static int CS_EXPREG2_FEE;
	public static int OUTER_DOOR_UPGRADE_PRICE2;
	public static int OUTER_DOOR_UPGRADE_PRICE3;
	public static int OUTER_DOOR_UPGRADE_PRICE5;
	public static int INNER_DOOR_UPGRADE_PRICE2;
	public static int INNER_DOOR_UPGRADE_PRICE3;
	public static int INNER_DOOR_UPGRADE_PRICE5;
	public static int WALL_UPGRADE_PRICE2;
	public static int WALL_UPGRADE_PRICE3;
	public static int WALL_UPGRADE_PRICE5;
	public static int TRAP_UPGRADE_PRICE1;
	public static int TRAP_UPGRADE_PRICE2;
	public static int TRAP_UPGRADE_PRICE3;
	public static int TRAP_UPGRADE_PRICE4;
	public static long FS_TELE_FEE_RATIO;
	public static int FS_TELE1_FEE;
	public static int FS_TELE2_FEE;
	public static long FS_SUPPORT_FEE_RATIO;
	public static int FS_SUPPORT1_FEE;
	public static int FS_SUPPORT2_FEE;
	public static long FS_MPREG_FEE_RATIO;
	public static int FS_MPREG1_FEE;
	public static int FS_MPREG2_FEE;
	public static long FS_HPREG_FEE_RATIO;
	public static int FS_HPREG1_FEE;
	public static int FS_HPREG2_FEE;
	public static long FS_EXPREG_FEE_RATIO;
	public static int FS_EXPREG1_FEE;
	public static int FS_EXPREG2_FEE;
	public static int FS_UPDATE_FRQ;
	public static int FS_BLOOD_OATH_COUNT;
	public static int FS_MAX_SUPPLY_LEVEL;
	public static int FS_FEE_FOR_CASTLE;
	public static int FS_MAX_OWN_TIME;
	public static int TAKE_FORT_POINTS;
	public static int LOOSE_FORT_POINTS;
	public static int TAKE_CASTLE_POINTS;
	public static int LOOSE_CASTLE_POINTS;
	public static int CASTLE_DEFENDED_POINTS;
	public static int FESTIVAL_WIN_POINTS;
	public static int HERO_POINTS;
	public static int ROYAL_GUARD_COST;
	public static int KNIGHT_UNIT_COST;
	public static int KNIGHT_REINFORCE_COST;
	public static int CLAN_CONTRIBUTION_REWARD_FOR_RAID;
	public static int CLAN_CONTRIBUTION_REWARD_FOR_ENEMY;
	public static int CLAN_CONTRIBUTION_REQUIRED;
	public static int CLAN_CONTRIBUTION_FAME_REWARD;
	public static boolean CLAN_WAR_KILL_MESSAGE;
	public static int BALLISTA_POINTS;
	public static int BLOODALLIANCE_POINTS;
	public static int BLOODOATH_POINTS;
	public static int KNIGHTSEPAULETTE_POINTS;
	public static int REPUTATION_SCORE_PER_KILL;
	public static int JOIN_ACADEMY_MIN_REP_SCORE;
	public static int JOIN_ACADEMY_MAX_REP_SCORE;
	public static boolean PK_PENALTY_LIST;
	public static int PK_PENALTY_LIST_MINIMUM_COUNT;
	
	// ------------------------------------------------------------------
	// Giran Zone Effect
	// ------------------------------------------------------------------
	
	/** Fallback day effect (used when no season/calendar override is active). */
	public static ExChangeClientEffectInfo GIRAN_DAY_EFFECT;
	/** Fallback night effect (used when no season/calendar override is active). */
	public static ExChangeClientEffectInfo GIRAN_NIGHT_EFFECT;
	
	/** When true, day/night effects are driven by the current meteorological season. */
	public static boolean GIRAN_SEASON_EFFECT_ENABLED;
	/** Day/night effects per season. Indexed by GiranSeason.ordinal(). */
	public static ExChangeClientEffectInfo[] GIRAN_SEASON_DAY_EFFECTS;
	public static ExChangeClientEffectInfo[] GIRAN_SEASON_NIGHT_EFFECTS;
	
	/** When true, matching calendar entries override the season (and fallback) effects. */
	public static boolean GIRAN_CALENDAR_EFFECT_ENABLED;
	/** Ordered list of custom date-range override entries. */
	public static List<GiranCalendarEntry> GIRAN_CALENDAR_ENTRIES;
	
	// ------------------------------------------------------------------
	// Wyvern / mounts
	// ------------------------------------------------------------------
	public static boolean ALLOW_WYVERN_ALWAYS;
	public static boolean ALLOW_WYVERN_DURING_SIEGE;
	public static boolean ALLOW_MOUNTS_DURING_SIEGE;
	
	// ------------------------------------------------------------------
	// Supporting types
	// ------------------------------------------------------------------
	
	/**
	 * Four meteorological seasons used for the Giran zone effect.<br>
	 * Boundaries are fixed calendar months (Northern Hemisphere): SPRING : Mar–May SUMMER : Jun–Aug AUTUMN : Sep–Nov WINTER : Dec–Feb
	 */
	public enum GiranSeason
	{
		SPRING,
		SUMMER,
		AUTUMN,
		WINTER;
		
		public ExChangeClientEffectInfo getDayEffect()
		{
			return GIRAN_SEASON_DAY_EFFECTS[ordinal()];
		}
		
		public ExChangeClientEffectInfo getNightEffect()
		{
			return GIRAN_SEASON_NIGHT_EFFECTS[ordinal()];
		}
	}
	
	/**
	 * A single custom calendar override entry.<br>
	 * Covers a date range [from, to] (inclusive, year-wrap supported) with separate day and night effects.
	 */
	public static class GiranCalendarEntry
	{
		private final MonthDay _from;
		private final MonthDay _to;
		private final ExChangeClientEffectInfo _dayEffect;
		private final ExChangeClientEffectInfo _nightEffect;
		
		public GiranCalendarEntry(MonthDay from, MonthDay to, ExChangeClientEffectInfo dayEffect, ExChangeClientEffectInfo nightEffect)
		{
			_from = from;
			_to = to;
			_dayEffect = dayEffect;
			_nightEffect = nightEffect;
		}
		
		public MonthDay getFrom()
		{
			return _from;
		}
		
		public MonthDay getTo()
		{
			return _to;
		}
		
		public ExChangeClientEffectInfo getDayEffect()
		{
			return _dayEffect;
		}
		
		public ExChangeClientEffectInfo getNightEffect()
		{
			return _nightEffect;
		}
	}
	
	// ------------------------------------------------------------------
	// Load
	// ------------------------------------------------------------------
	
	/**
	 * Loads all configuration values from the {@code FEATURE_CONFIG_FILE}.<br>
	 * It populates various static fields such as taxes, fees, and upgrade prices.<br>
	 * This method should be called during the server initialization process.
	 */
	public static void load()
	{
		final ConfigReader config = new ConfigReader(FEATURE_CONFIG_FILE);
		SIEGE_HOUR_LIST = new ArrayList<>();
		for (String hour : config.getString("SiegeHourList", "").split(","))
		{
			if (StringUtil.isNumeric(hour))
			{
				SIEGE_HOUR_LIST.add(Integer.parseInt(hour));
			}
		}
		CASTLE_BUY_TAX_NEUTRAL = config.getInt("BuyTaxForNeutralSide", 15);
		CASTLE_BUY_TAX_LIGHT = config.getInt("BuyTaxForLightSide", 0);
		CASTLE_BUY_TAX_DARK = config.getInt("BuyTaxForDarkSide", 30);
		CASTLE_SELL_TAX_NEUTRAL = config.getInt("SellTaxForNeutralSide", 0);
		CASTLE_SELL_TAX_LIGHT = config.getInt("SellTaxForLightSide", 0);
		CASTLE_SELL_TAX_DARK = config.getInt("SellTaxForDarkSide", 20);
		CS_TELE_FEE_RATIO = config.getLong("CastleTeleportFunctionFeeRatio", 604800000);
		CS_TELE1_FEE = config.getInt("CastleTeleportFunctionFeeLvl1", 1000);
		CS_TELE2_FEE = config.getInt("CastleTeleportFunctionFeeLvl2", 10000);
		CS_SUPPORT_FEE_RATIO = config.getLong("CastleSupportFunctionFeeRatio", 604800000);
		CS_SUPPORT1_FEE = config.getInt("CastleSupportFeeLvl1", 49000);
		CS_SUPPORT2_FEE = config.getInt("CastleSupportFeeLvl2", 120000);
		CS_MPREG_FEE_RATIO = config.getLong("CastleMpRegenerationFunctionFeeRatio", 604800000);
		CS_MPREG1_FEE = config.getInt("CastleMpRegenerationFeeLvl1", 45000);
		CS_MPREG2_FEE = config.getInt("CastleMpRegenerationFeeLvl2", 65000);
		CS_HPREG_FEE_RATIO = config.getLong("CastleHpRegenerationFunctionFeeRatio", 604800000);
		CS_HPREG1_FEE = config.getInt("CastleHpRegenerationFeeLvl1", 12000);
		CS_HPREG2_FEE = config.getInt("CastleHpRegenerationFeeLvl2", 20000);
		CS_EXPREG_FEE_RATIO = config.getLong("CastleExpRegenerationFunctionFeeRatio", 604800000);
		CS_EXPREG1_FEE = config.getInt("CastleExpRegenerationFeeLvl1", 63000);
		CS_EXPREG2_FEE = config.getInt("CastleExpRegenerationFeeLvl2", 70000);
		OUTER_DOOR_UPGRADE_PRICE2 = config.getInt("OuterDoorUpgradePriceLvl2", 3000000);
		OUTER_DOOR_UPGRADE_PRICE3 = config.getInt("OuterDoorUpgradePriceLvl3", 4000000);
		OUTER_DOOR_UPGRADE_PRICE5 = config.getInt("OuterDoorUpgradePriceLvl5", 5000000);
		INNER_DOOR_UPGRADE_PRICE2 = config.getInt("InnerDoorUpgradePriceLvl2", 750000);
		INNER_DOOR_UPGRADE_PRICE3 = config.getInt("InnerDoorUpgradePriceLvl3", 900000);
		INNER_DOOR_UPGRADE_PRICE5 = config.getInt("InnerDoorUpgradePriceLvl5", 1000000);
		WALL_UPGRADE_PRICE2 = config.getInt("WallUpgradePriceLvl2", 1600000);
		WALL_UPGRADE_PRICE3 = config.getInt("WallUpgradePriceLvl3", 1800000);
		WALL_UPGRADE_PRICE5 = config.getInt("WallUpgradePriceLvl5", 2000000);
		TRAP_UPGRADE_PRICE1 = config.getInt("TrapUpgradePriceLvl1", 3000000);
		TRAP_UPGRADE_PRICE2 = config.getInt("TrapUpgradePriceLvl2", 4000000);
		TRAP_UPGRADE_PRICE3 = config.getInt("TrapUpgradePriceLvl3", 5000000);
		TRAP_UPGRADE_PRICE4 = config.getInt("TrapUpgradePriceLvl4", 6000000);
		FS_TELE_FEE_RATIO = config.getLong("FortressTeleportFunctionFeeRatio", 604800000);
		FS_TELE1_FEE = config.getInt("FortressTeleportFunctionFeeLvl1", 1000);
		FS_TELE2_FEE = config.getInt("FortressTeleportFunctionFeeLvl2", 10000);
		FS_SUPPORT_FEE_RATIO = config.getLong("FortressSupportFunctionFeeRatio", 86400000);
		FS_SUPPORT1_FEE = config.getInt("FortressSupportFeeLvl1", 7000);
		FS_SUPPORT2_FEE = config.getInt("FortressSupportFeeLvl2", 17000);
		FS_MPREG_FEE_RATIO = config.getLong("FortressMpRegenerationFunctionFeeRatio", 86400000);
		FS_MPREG1_FEE = config.getInt("FortressMpRegenerationFeeLvl1", 6500);
		FS_MPREG2_FEE = config.getInt("FortressMpRegenerationFeeLvl2", 9300);
		FS_HPREG_FEE_RATIO = config.getLong("FortressHpRegenerationFunctionFeeRatio", 86400000);
		FS_HPREG1_FEE = config.getInt("FortressHpRegenerationFeeLvl1", 2000);
		FS_HPREG2_FEE = config.getInt("FortressHpRegenerationFeeLvl2", 3500);
		FS_EXPREG_FEE_RATIO = config.getLong("FortressExpRegenerationFunctionFeeRatio", 86400000);
		FS_EXPREG1_FEE = config.getInt("FortressExpRegenerationFeeLvl1", 9000);
		FS_EXPREG2_FEE = config.getInt("FortressExpRegenerationFeeLvl2", 10000);
		FS_UPDATE_FRQ = config.getInt("FortressPeriodicUpdateFrequency", 360);
		FS_BLOOD_OATH_COUNT = config.getInt("FortressBloodOathCount", 1);
		FS_MAX_SUPPLY_LEVEL = config.getInt("FortressMaxSupplyLevel", 6);
		FS_FEE_FOR_CASTLE = config.getInt("FortressFeeForCastle", 25000);
		FS_MAX_OWN_TIME = config.getInt("FortressMaximumOwnTime", 168);
		TAKE_FORT_POINTS = config.getInt("TakeFortPoints", 200);
		LOOSE_FORT_POINTS = config.getInt("LooseFortPoints", 0);
		TAKE_CASTLE_POINTS = config.getInt("TakeCastlePoints", 1500);
		LOOSE_CASTLE_POINTS = config.getInt("LooseCastlePoints", 3000);
		CASTLE_DEFENDED_POINTS = config.getInt("CastleDefendedPoints", 750);
		FESTIVAL_WIN_POINTS = config.getInt("FestivalOfDarknessWin", 200);
		HERO_POINTS = config.getInt("HeroPoints", 1000);
		ROYAL_GUARD_COST = config.getInt("CreateRoyalGuardCost", 5000);
		KNIGHT_UNIT_COST = config.getInt("CreateKnightUnitCost", 10000);
		KNIGHT_REINFORCE_COST = config.getInt("ReinforceKnightUnitCost", 5000);
		CLAN_CONTRIBUTION_REWARD_FOR_RAID = config.getInt("ClanContributionRewardForRaid", 1);
		CLAN_CONTRIBUTION_REWARD_FOR_ENEMY = config.getInt("ClanContributionRewardForEnemy", 1);
		CLAN_CONTRIBUTION_REQUIRED = config.getInt("ClanContributionRequired", 100);
		CLAN_CONTRIBUTION_FAME_REWARD = config.getInt("ClanContributionFameReward", 300);
		CLAN_WAR_KILL_MESSAGE = config.getBoolean("ClanWarKillMessage", false);
		BALLISTA_POINTS = config.getInt("KillBallistaPoints", 500);
		BLOODALLIANCE_POINTS = config.getInt("BloodAlliancePoints", 500);
		BLOODOATH_POINTS = config.getInt("BloodOathPoints", 200);
		KNIGHTSEPAULETTE_POINTS = config.getInt("KnightsEpaulettePoints", 20);
		REPUTATION_SCORE_PER_KILL = config.getInt("ReputationScorePerKill", 1);
		JOIN_ACADEMY_MIN_REP_SCORE = config.getInt("CompleteAcademyMinPoints", 190);
		JOIN_ACADEMY_MAX_REP_SCORE = config.getInt("CompleteAcademyMaxPoints", 650);
		PK_PENALTY_LIST = config.getBoolean("PkPenaltyList", true);
		PK_PENALTY_LIST_MINIMUM_COUNT = config.getInt("PkPenaltyMinimumCount", 9);
		
		// Giran fallback day/night effects.
		GIRAN_DAY_EFFECT = parseEffect(config.getString("GiranDayEffect", "Normal"));
		GIRAN_NIGHT_EFFECT = parseEffect(config.getString("GiranNightEffect", "Flowers"));
		
		// Giran season effects.
		GIRAN_SEASON_EFFECT_ENABLED = config.getBoolean("GiranSeasonEffect", false);
		GIRAN_SEASON_DAY_EFFECTS = new ExChangeClientEffectInfo[GiranSeason.values().length];
		GIRAN_SEASON_NIGHT_EFFECTS = new ExChangeClientEffectInfo[GiranSeason.values().length];
		GIRAN_SEASON_DAY_EFFECTS[GiranSeason.SPRING.ordinal()] = parseEffect(config.getString("GiranSpringDayEffect", "Flowers"));
		GIRAN_SEASON_NIGHT_EFFECTS[GiranSeason.SPRING.ordinal()] = parseEffect(config.getString("GiranSpringNightEffect", "Petals"));
		GIRAN_SEASON_DAY_EFFECTS[GiranSeason.SUMMER.ordinal()] = parseEffect(config.getString("GiranSummerDayEffect", "Water"));
		GIRAN_SEASON_NIGHT_EFFECTS[GiranSeason.SUMMER.ordinal()] = parseEffect(config.getString("GiranSummerNightEffect", "Normal"));
		GIRAN_SEASON_DAY_EFFECTS[GiranSeason.AUTUMN.ordinal()] = parseEffect(config.getString("GiranAutumnDayEffect", "Autumn"));
		GIRAN_SEASON_NIGHT_EFFECTS[GiranSeason.AUTUMN.ordinal()] = parseEffect(config.getString("GiranAutumnNightEffect", "Normal"));
		GIRAN_SEASON_DAY_EFFECTS[GiranSeason.WINTER.ordinal()] = parseEffect(config.getString("GiranWinterDayEffect", "Snow"));
		GIRAN_SEASON_NIGHT_EFFECTS[GiranSeason.WINTER.ordinal()] = parseEffect(config.getString("GiranWinterNightEffect", "Snow"));
		
		// Giran calendar override entries.
		GIRAN_CALENDAR_EFFECT_ENABLED = config.getBoolean("GiranCalendarEffect", false);
		GIRAN_CALENDAR_ENTRIES = parseCalendarEntries(config.getString("GiranCalendarEntries", ""));
		
		ALLOW_WYVERN_ALWAYS = config.getBoolean("AllowRideWyvernAlways", false);
		ALLOW_WYVERN_DURING_SIEGE = config.getBoolean("AllowRideWyvernDuringSiege", true);
		ALLOW_MOUNTS_DURING_SIEGE = config.getBoolean("AllowRideMountsDuringSiege", false);
	}
	
	// ------------------------------------------------------------------
	// Helpers
	// ------------------------------------------------------------------
	
	/**
	 * Converts a string value into an {@code ExChangeClientEffectInfo} object.<br>
	 * This method maps specific keywords to their corresponding effect types.<br>
	 * It returns {@code ExChangeClientEffectInfo.GIRAN_NORMAL} if no match is found.
	 * @param value The string representation of the effect to parse.
	 * @return The parsed {@code ExChangeClientEffectInfo} object.
	 */
	private static ExChangeClientEffectInfo parseEffect(String value)
	{
		switch (value.trim())
		{
			case "Petals":
			{
				return ExChangeClientEffectInfo.GIRAN_PETALS;
			}
			case "Snow":
			{
				return ExChangeClientEffectInfo.GIRAN_SNOW;
			}
			case "Flowers":
			{
				return ExChangeClientEffectInfo.GIRAN_FLOWERS;
			}
			case "Water":
			{
				return ExChangeClientEffectInfo.GIRAN_WATER;
			}
			case "Autumn":
			{
				return ExChangeClientEffectInfo.GIRAN_AUTUMN;
			}
			default:
			{
				return ExChangeClientEffectInfo.GIRAN_NORMAL;
			}
		}
	}
	
	/**
	 * Converts a raw configuration string into a list of {@code GiranCalendarEntry} objects.<br>
	 * The input string should contain entries separated by semicolons.<br>
	 * Each entry must follow the format "MMM dd-MMM dd=Effect/Effect".<br>
	 * Malformed entries are skipped and logged to the error stream.
	 * @param raw The raw configuration string to parse.
	 * @return A list of parsed {@code GiranCalendarEntry} objects or an empty list if the input is null or empty.
	 */
	private static List<GiranCalendarEntry> parseCalendarEntries(String raw)
	{
		if ((raw == null) || raw.trim().isEmpty())
		{
			return Collections.emptyList();
		}
		
		final List<GiranCalendarEntry> entries = new ArrayList<>();
		for (String token : raw.split(";"))
		{
			final String entry = token.trim();
			if (entry.isEmpty())
			{
				continue;
			}
			
			try
			{
				// Split on '=' to separate date range from effects.
				// e.g. "Dec 20-Jan 05" and "Snow/Snow".
				final int eqIdx = entry.indexOf('=');
				final String rangePart = entry.substring(0, eqIdx).trim(); // "Dec 20-Jan 05"
				final String effectPart = entry.substring(eqIdx + 1).trim(); // "Snow/Snow"
				
				// Split range on '-' but keep 3-char month + 2-char day groups.
				// Format is "MMM dd-MMM dd" so the dash sits at index 6.
				final String fromStr = rangePart.substring(0, 6).trim(); // "Dec 20"
				final String toStr = rangePart.substring(7).trim(); // "Jan 05"
				
				final MonthDay from = MonthDay.parse(fromStr, MONTH_DAY_FMT);
				final MonthDay to = MonthDay.parse(toStr, MONTH_DAY_FMT);
				
				// Split effects on '/'.
				final String[] effects = effectPart.split("/");
				final ExChangeClientEffectInfo dayEffect = parseEffect(effects[0].trim());
				final ExChangeClientEffectInfo nightEffect = effects.length > 1 ? parseEffect(effects[1].trim()) : dayEffect;
				
				entries.add(new GiranCalendarEntry(from, to, dayEffect, nightEffect));
			}
			catch (Exception e)
			{
				// Log and skip malformed entry so a typo never prevents server startup.
				System.err.println("[FeatureConfig] Skipping malformed GiranCalendarEntries entry: \"" + entry + "\" (" + e.getMessage() + ")");
			}
		}
		
		return Collections.unmodifiableList(entries);
	}
}