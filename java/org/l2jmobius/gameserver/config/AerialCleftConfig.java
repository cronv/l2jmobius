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

import org.l2jmobius.commons.util.ConfigReader;

/**
 * This class loads all configuration settings related to the Aerial Cleft.<br>
 * It provides easy access to these values throughout the game server.
 * @author Altur
 */
public class AerialCleftConfig
{
	// File
	private static final String AERIAL_CLEFT_CONFIG_FILE = "./config/AerialCleft.ini";
	
	// Constants
	public static boolean CLEFT_ENABLED;
	public static int CLEFT_MIN_LEVEL;
	public static int CLEFT_MIN_TEAM_PLAYERS;
	public static int CLEFT_MAX_PLAYERS;
	public static int CLEFT_EVENT_TIME;
	public static int CLEFT_COLLECT_TIME;
	public static int CLEFT_RELOAD_TIME;
	public static int CLEFT_RESPAWN_DELAY;
	public static int CLEFT_LARGE_COMPRESSOR_POINTS;
	public static int CLEFT_SMALL_COMPRESSOR_POINTS;
	public static int CLEFT_CAT_POINTS;
	public static int CLEFT_PLAYER_POINTS;
	public static int CLEFT_REWARD_ID;
	public static int CLEFT_REWARD_COUNT_WINNER;
	public static int CLEFT_REWARD_COUNT_LOSER;
	public static int CLEFT_MIN_PARTICIPATION_TIME;
	
	/**
	 * Loads the configuration settings from the file.<br>
	 * This method populates all static variables in {@code AerialCleftConfig}.<br>
	 * It uses a {@code ConfigReader} to read values from the local config directory.
	 */
	public static void load()
	{
		final ConfigReader config = new ConfigReader(AERIAL_CLEFT_CONFIG_FILE);
		CLEFT_ENABLED = config.getBoolean("AerialCleftEnabled", true);
		CLEFT_MIN_LEVEL = config.getInt("MinimumLevel", 75);
		CLEFT_MIN_TEAM_PLAYERS = config.getInt("MinimumTeamPlayers", 9);
		CLEFT_MAX_PLAYERS = config.getInt("MaximumPlayers", 18);
		CLEFT_EVENT_TIME = config.getInt("EventTime", 25);
		CLEFT_COLLECT_TIME = config.getInt("CollectTime", 5);
		CLEFT_RELOAD_TIME = config.getInt("ReloadTime", 60);
		CLEFT_RESPAWN_DELAY = config.getInt("RespawnDelay", 10);
		CLEFT_LARGE_COMPRESSOR_POINTS = config.getInt("LargeCompressorPoints", 100);
		CLEFT_SMALL_COMPRESSOR_POINTS = config.getInt("SmallCompressorPoints", 40);
		CLEFT_CAT_POINTS = config.getInt("CatPoints", 10);
		CLEFT_PLAYER_POINTS = config.getInt("PlayerPoints", 1);
		CLEFT_REWARD_ID = config.getInt("RewardId", 13749);
		CLEFT_REWARD_COUNT_WINNER = config.getInt("RewardCountWinner", 50);
		CLEFT_REWARD_COUNT_LOSER = config.getInt("RewardCountLoser", 20);
		CLEFT_MIN_PARTICIPATION_TIME = config.getInt("MinimumParticipationTime", 15);
	}
}
