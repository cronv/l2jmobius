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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the logic for PC Cafe points within the game server.<br>
 * This class handles point calculations and updates for players.<br>
 * It provides functionality to interact with {@code PremiumSystemConfig} settings.
 */
public class PcCafePointsManager
{
	/**
	 * Starts a recurring task to reward the player with PC Cafe points.<br>
	 * This method checks if the premium system is enabled for retail-like accounts.<br>
	 * It schedules the {@code giveRetailPcCafePoint} method at a fixed rate.
	 * @param player The {@code Player} who will receive the rewards.
	 */
	public void run(Player player)
	{
		// PC-points only premium accounts.
		if (!PremiumSystemConfig.PC_CAFE_ENABLED || !PremiumSystemConfig.PC_CAFE_RETAIL_LIKE || (!player.hasEnteredWorld()))
		{
			return;
		}
		
		ThreadPool.scheduleAtFixedRate(() -> giveRetailPcCafePoint(player), PremiumSystemConfig.PC_CAFE_REWARD_TIME, PremiumSystemConfig.PC_CAFE_REWARD_TIME);
	}
	
	/**
	 * Awards retail PC Cafe points to a player.<br>
	 * This method checks if the system and player meet all required conditions.<br>
	 * It calculates the points based on configuration settings.<br>
	 * The final point total is updated and sent to the player.
	 * @param player The {@code Player} who will receive the points.
	 */
	public void giveRetailPcCafePoint(Player player)
	{
		if (!PremiumSystemConfig.PC_CAFE_ENABLED || !PremiumSystemConfig.PC_CAFE_RETAIL_LIKE || (player.isOnlineInt() == 0) || (!player.hasPremiumStatus() && PremiumSystemConfig.PC_CAFE_ONLY_PREMIUM) || player.getOfflineMode().isEnabled())
		{
			return;
		}
		
		int points = PremiumSystemConfig.ACQUISITION_PC_CAFE_RETAIL_LIKE_POINTS;
		
		if (points >= PremiumSystemConfig.PC_CAFE_MAX_POINTS)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EARNED_THE_MAXIMUM_NUMBER_OF_PA_POINTS);
			return;
		}
		
		if (PremiumSystemConfig.PC_CAFE_RANDOM_POINT)
		{
			points = Rnd.get(points / 2, points);
		}
		
		SystemMessage message = null;
		if (PremiumSystemConfig.PC_CAFE_ENABLE_DOUBLE_POINTS && (Rnd.get(100) < PremiumSystemConfig.PC_CAFE_DOUBLE_POINTS_CHANCE))
		{
			points *= 2;
			message = new SystemMessage(SystemMessageId.DOUBLE_POINTS_YOU_HAVE_EARNED_S1_PA_POINTS);
		}
		else
		{
			message = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_PA_POINTS_X_S1);
		}
		
		final int pcCafePoints = player.getPcCafePoints();
		if ((pcCafePoints + points) > PremiumSystemConfig.PC_CAFE_MAX_POINTS)
		{
			points = PremiumSystemConfig.PC_CAFE_MAX_POINTS - pcCafePoints;
		}
		
		message.addLong(points);
		player.sendPacket(message);
		player.setPcCafePoints(pcCafePoints + points);
		player.sendPacket(new ExPCCafePointInfo(pcCafePoints + points, points, 1));
	}
	
	/**
	 * Awards PC Cafe points to a player based on the experience gained.<br>
	 * This method checks for various conditions like zone restrictions and premium status.<br>
	 * It calculates the points earned and updates the {@code Player} balance.
	 * @param player The {@code Player} who will receive the points.
	 * @param exp The amount of experience used to calculate the reward.
	 */
	public void givePcCafePoint(Player player, double exp)
	{
		if (PremiumSystemConfig.PC_CAFE_RETAIL_LIKE || !PremiumSystemConfig.PC_CAFE_ENABLED || player.isInsideZone(ZoneId.PEACE) || player.isInsideZone(ZoneId.PVP) || player.isInsideZone(ZoneId.SIEGE) || (player.isOnlineInt() == 0) || player.isJailed())
		{
			return;
		}
		
		// PC-points only premium accounts.
		if (PremiumSystemConfig.PC_CAFE_ONLY_PREMIUM && !player.hasPremiumStatus())
		{
			return;
		}
		
		final int pcCafeMaxPoints = PremiumSystemConfig.PC_CAFE_MAX_POINTS;
		final int currentPcCafePoints = player.getPcCafePoints();
		if (currentPcCafePoints >= pcCafeMaxPoints)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_EARNED_THE_MAXIMUM_NUMBER_OF_PA_POINTS);
			return;
		}
		
		int points = (int) (exp * 0.0001 * PremiumSystemConfig.PC_CAFE_POINT_RATE);
		if (PremiumSystemConfig.PC_CAFE_RANDOM_POINT)
		{
			points = Rnd.get(points / 2, points);
		}
		
		if ((points == 0) && (exp > 0) && PremiumSystemConfig.PC_CAFE_REWARD_LOW_EXP_KILLS && (Rnd.get(100) < PremiumSystemConfig.PC_CAFE_LOW_EXP_KILLS_CHANCE))
		{
			points = 1; // minimum points
		}
		
		if (points <= 0)
		{
			return;
		}
		
		SystemMessage message = null;
		if (PremiumSystemConfig.PC_CAFE_ENABLE_DOUBLE_POINTS && (Rnd.get(100) < PremiumSystemConfig.PC_CAFE_DOUBLE_POINTS_CHANCE))
		{
			points *= 2;
			message = new SystemMessage(SystemMessageId.DOUBLE_POINTS_YOU_HAVE_EARNED_S1_PA_POINTS);
		}
		else
		{
			message = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_PA_POINTS_X_S1);
		}
		
		if ((currentPcCafePoints + points) > pcCafeMaxPoints)
		{
			points = pcCafeMaxPoints - currentPcCafePoints;
		}
		
		message.addLong(points);
		player.sendPacket(message);
		player.setPcCafePoints(currentPcCafePoints + points);
		player.sendPacket(new ExPCCafePointInfo(currentPcCafePoints + points, points, 1));
	}
	
	/**
	 * Retrieves the singleton instance of the {@code PcCafePointsManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code PcCafePointsManager} instance.
	 */
	public static PcCafePointsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PcCafePointsManager INSTANCE = new PcCafePointsManager();
	}
}
