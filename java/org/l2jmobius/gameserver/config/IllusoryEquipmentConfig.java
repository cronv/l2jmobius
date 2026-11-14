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

import java.util.Arrays;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class handles the loading of all configuration data related to illusory equipment.<br>
 * It provides a centralized way to manage these settings within the {@code IllusoryEquipmentConfig} class.
 * @author Mobius
 */
public class IllusoryEquipmentConfig
{
	private static final Logger LOGGER = Logger.getLogger(IllusoryEquipmentConfig.class.getName());
	
	// File
	private static final String ILLUSORY_EQUIPMENT_CONFIG_FILE = "./config/IllusoryEquipment.ini";
	
	/** The highest character level the point limits are read for. */
	private static final int MAX_LEVEL = 200;
	
	// Constants
	public static boolean ILLUSORY_EQUIPMENT_ENABLED;
	public static int ILLUSORY_EQUIPMENT_EVENT_DURATION;
	public static boolean ILLUSORY_EQUIPMENT_EVENT_DEBUG_ENABLED;
	public static String ILLUSORY_EQUIPMENT_EVENT_POINTS_LIMIT_BY_LEVEL;
	public static int ILLUSORY_EQUIPMENT_POINTS_BONUS_LIMIT;
	public static boolean ILLUSORY_EQUIPMENT_KEEP_BONUS_POINTS;
	
	/** The amount of points a character may spend, by character level. */
	private static final int[] POINTS_LIMIT_BY_LEVEL = new int[MAX_LEVEL + 1];
	
	/**
	 * Loads the configuration settings from the file.<br>
	 * This method populates all static variables in {@code IllusoryEquipmentConfig}.<br>
	 * It reads values for enabled status, duration, debug mode, and point limits.
	 */
	public static void load()
	{
		final ConfigReader config = new ConfigReader(ILLUSORY_EQUIPMENT_CONFIG_FILE);
		ILLUSORY_EQUIPMENT_ENABLED = config.getBoolean("Enabled", false);
		ILLUSORY_EQUIPMENT_EVENT_DURATION = config.getInt("Duration", 5);
		ILLUSORY_EQUIPMENT_EVENT_DEBUG_ENABLED = config.getBoolean("Debug", false);
		ILLUSORY_EQUIPMENT_EVENT_POINTS_LIMIT_BY_LEVEL = config.getString("PointsLimitByLevel", "1-109,500;110-115,300;116-119,100;120-200,0");
		ILLUSORY_EQUIPMENT_POINTS_BONUS_LIMIT = config.getInt("PointsBonusLimit", 0);
		ILLUSORY_EQUIPMENT_KEEP_BONUS_POINTS = config.getBoolean("KeepBonusPointsOnSeasonEnd", false);
		parsePointsLimit();
	}
	
	/**
	 * Fills the point limit of every character level from the configured ranges.<br>
	 * A level no range covers keeps a limit of zero.
	 */
	private static void parsePointsLimit()
	{
		Arrays.fill(POINTS_LIMIT_BY_LEVEL, 0);
		
		final StringTokenizer ranges = new StringTokenizer(ILLUSORY_EQUIPMENT_EVENT_POINTS_LIMIT_BY_LEVEL, ";");
		while (ranges.hasMoreTokens())
		{
			final String range = ranges.nextToken().trim();
			final int comma = range.indexOf(',');
			final int dash = range.indexOf('-');
			if ((dash < 1) || (comma < dash))
			{
				LOGGER.warning("IllusoryEquipmentConfig: Skipped the malformed point limit range \"" + range + "\".");
				continue;
			}
			
			try
			{
				final int fromLevel = Integer.parseInt(range.substring(0, dash).trim());
				final int toLevel = Integer.parseInt(range.substring(dash + 1, comma).trim());
				final int points = Integer.parseInt(range.substring(comma + 1).trim());
				for (int level = Math.max(fromLevel, 0); level <= Math.min(toLevel, MAX_LEVEL); level++)
				{
					POINTS_LIMIT_BY_LEVEL[level] = points;
				}
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("IllusoryEquipmentConfig: Skipped the unreadable point limit range \"" + range + "\".");
			}
		}
	}
	
	/**
	 * Retrieves the amount of points a character of the given level may spend.<br>
	 * Items raising the limit are counted on top of this by the caller.
	 * @param level The level of the character.
	 * @return The configured point limit of that level.
	 */
	public static int getPointsLimit(int level)
	{
		if (level < 0)
		{
			return 0;
		}
		
		if (level > MAX_LEVEL)
		{
			return POINTS_LIMIT_BY_LEVEL[MAX_LEVEL];
		}
		
		return POINTS_LIMIT_BY_LEVEL[level];
	}
}
