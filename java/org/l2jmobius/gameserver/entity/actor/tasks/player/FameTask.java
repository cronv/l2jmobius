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
package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.OfflineTradeConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This task rewards a {@code Player} with fame for standing within a siege zone.<br>
 * It handles the logic of granting reputation points based on specific area conditions.
 * @author UnAfraid
 */
public class FameTask implements Runnable
{
	private final Player _player;
	private final int _value;
	
	/**
	 * Creates a new {@code FameTask} for a specific player.<br>
	 * This task handles rewarding fame points.
	 * @param player The {@code Player} who will receive the reward.
	 * @param value The amount of fame to be awarded.
	 */
	public FameTask(Player player, int value)
	{
		_player = player;
		_value = value;
	}
	
	@Override
	public void run()
	{
		if ((_player == null) || (_player.isDead() && !PlayerConfig.FAME_FOR_DEAD_PLAYERS))
		{
			return;
		}
		
		if (((_player.getClient() == null) || _player.getClient().isDetached()) && !OfflineTradeConfig.OFFLINE_FAME)
		{
			return;
		}
		
		_player.setFame(_player.getFame() + _value);
		final SystemMessage sm = new SystemMessage(SystemMessageId.PERSONAL_REPUTATION_S1);
		sm.addInt(_value);
		_player.sendPacket(sm);
		_player.updateUserInfo();
	}
}
