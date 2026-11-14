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
package org.l2jmobius.gameserver.network.clientpackets;

import java.util.Calendar;
import java.util.Date;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SiegeInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles a client request to manually set the siege time for a castle.<br>
 * This packet allows administrators or authorized users to update the {@code Castle} schedule.
 * @author UnAfraid, Mobius
 */
public class RequestSetCastleSiegeTime extends ClientPacket
{
	private int _castleId;
	private long _time;
	
	@Override
	protected void readImpl()
	{
		_castleId = readInt();
		_time = readInt();
		_time *= 1000;
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		final Castle castle = CastleManager.getInstance().getCastleById(_castleId);
		if ((player == null) || (castle == null))
		{
			PacketLogger.warning(getClass().getSimpleName() + ": activeChar: " + player + " castle: " + castle + " castleId: " + _castleId);
			return;
		}
		
		final int ownerId = castle.getOwnerId();
		// Only the castle lord can set the siege time, nobody can for a castle owned by NPCs.
		if ((ownerId == 0) || (ownerId != player.getClanId()))
		{
			PacketLogger.warning(getClass().getSimpleName() + ": activeChar: " + player + " castle: " + castle + " castleId: " + _castleId + " is trying to change siege date of not his own castle!");
		}
		else if (!player.isClanLeader())
		{
			PacketLogger.warning(getClass().getSimpleName() + ": activeChar: " + player + " castle: " + castle + " castleId: " + _castleId + " is trying to change siege date but is not clan leader!");
		}
		else if (!castle.isTimeRegistrationOver())
		{
			if (isSiegeTimeValid(castle.getSiegeDate().getTimeInMillis(), _time))
			{
				castle.getSiegeDate().setTimeInMillis(_time);
				castle.setTimeRegistrationOver(true);
				castle.getSiege().saveSiegeDate();
				final SystemMessage msg = new SystemMessage(SystemMessageId.S1_HAS_ANNOUNCED_THE_NEXT_CASTLE_SIEGE_TIME);
				msg.addCastleId(_castleId);
				World.broadcastToAllOnlinePlayers(msg);
				player.sendPacket(new SiegeInfo(castle, player));
			}
			else
			{
				PacketLogger.warning(getClass().getSimpleName() + ": activeChar: " + player + " castle: " + castle + " castleId: " + _castleId + " is trying to an invalid time (" + new Date(_time) + " !");
			}
		}
		else
		{
			PacketLogger.warning(getClass().getSimpleName() + ": activeChar: " + player + " castle: " + castle + " castleId: " + _castleId + " is trying to change siege date but currently not possible!");
		}
	}
	
	/**
	 * Checks if a specific date and time is valid for a siege.<br>
	 * It compares the {@code choosenDate} against allowed hours in the configuration.
	 * @param siegeDate The base date for the siege event.
	 * @param choosenDate The specific timestamp to validate.
	 * @return {@code true} if the time is valid, {@code false} otherwise.
	 */
	private static boolean isSiegeTimeValid(long siegeDate, long choosenDate)
	{
		final Calendar cal1 = Calendar.getInstance();
		cal1.setTimeInMillis(siegeDate);
		cal1.set(Calendar.MINUTE, 0);
		cal1.set(Calendar.SECOND, 0);
		
		final Calendar cal2 = Calendar.getInstance();
		cal2.setTimeInMillis(choosenDate);
		
		for (int hour : FeatureConfig.SIEGE_HOUR_LIST)
		{
			cal1.set(Calendar.HOUR_OF_DAY, hour);
			if (isEqual(cal1, cal2, Calendar.YEAR, Calendar.MONTH, Calendar.DAY_OF_MONTH, Calendar.HOUR, Calendar.MINUTE, Calendar.SECOND))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Compares specific fields of two {@code Calendar} objects.<br>
	 * Returns {@code true} if all specified fields match.<br>
	 * Returns {@code false} if any field differs.
	 * @param cal1 The first {@code Calendar} to compare.
	 * @param cal2 The second {@code Calendar} to compare.
	 * @param fields The specific {@code Calendar} fields to check.
	 * @return {@code true} if all specified fields are equal, otherwise {@code false}.
	 */
	private static boolean isEqual(Calendar cal1, Calendar cal2, int... fields)
	{
		for (int field : fields)
		{
			if (cal1.get(field) != cal2.get(field))
			{
				return false;
			}
		}
		
		return true;
	}
}
