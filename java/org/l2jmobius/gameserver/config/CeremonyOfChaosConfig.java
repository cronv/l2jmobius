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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.util.ItemHolderParser;

/**
 * This class loads all configuration settings related to the Ceremony of Chaos.<br>
 * It provides easy access to these values throughout the game server.
 * @author Mobius
 */
public class CeremonyOfChaosConfig
{
	// File
	private static final String CEREMONY_OF_CHAOS_CONFIG_FILE = "./config/CeremonyOfChaos.ini";
	
	// Constants
	public static List<Integer> COC_COMPETITION_DAYS;
	public static int COC_START_TIME;
	public static int COC_MIN_PLAYERS;
	public static int COC_MAX_PLAYERS;
	public static int COC_MAX_ARENAS;
	public static List<ItemHolder> COC_WINNER_REWARDS;
	public static List<ItemHolder> COC_BEST_KILLER_REWARDS;
	public static List<ItemHolder> COC_MONTHLY_WINNER_REWARDS;
	public static int COC_MONTHLY_WINNER_FAME;
	public static int COC_MONTHLY_WINNER_CLAN_REPUTATION;
	
	/**
	 * Loads the configuration data from the file.<br>
	 * This method populates all static fields in {@code CeremonyOfChaosConfig}.
	 */
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CEREMONY_OF_CHAOS_CONFIG_FILE);
		COC_COMPETITION_DAYS = new ArrayList<>();
		for (String s : config.getString("CoCCompetitionDays", "2,3,4,5").split(","))
		{
			COC_COMPETITION_DAYS.add(Integer.parseInt(s));
		}
		COC_START_TIME = config.getInt("CoCStartTime", 20);
		COC_MIN_PLAYERS = config.getInt("CoCMinPlayers", 2);
		COC_MAX_PLAYERS = config.getInt("CoCMaxPlayers", 18);
		COC_MAX_ARENAS = config.getInt("CoCMaxArenas", 5);
		COC_WINNER_REWARDS = ItemHolderParser.parseItemHolders(config, "CoCWinnerRewards", "45584,5;36333,1");
		COC_BEST_KILLER_REWARDS = ItemHolderParser.parseItemHolders(config, "CoCBestKillerRewards", "35982,1");
		COC_MONTHLY_WINNER_REWARDS = ItemHolderParser.parseItemHolders(config, "CoCMonthlyWinnerRewards", "35565,1;35564,1");
		COC_MONTHLY_WINNER_FAME = config.getInt("CoCMonthlyWinnerFame", 5000);
		COC_MONTHLY_WINNER_CLAN_REPUTATION = config.getInt("CoCMonthlyWinnerClanReputation", 150000);
	}
}
