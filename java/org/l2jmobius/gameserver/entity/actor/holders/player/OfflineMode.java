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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.config.custom.OfflinePlayConfig;
import org.l2jmobius.gameserver.data.sql.OfflinePlayTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

/**
 * Manages the offline mode of a {@code Player}.<br>
 * It holds the detached client check shared by offline trade and offline play, the play flag with its start routine, and the start time of an offline shop.
 * @author Mobius
 */
public class OfflineMode
{
	private final Player _player;
	private boolean _playRunning = false;
	private long _shopStart = 0;
	
	/**
	 * Creates a new {@code OfflineMode} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public OfflineMode(Player player)
	{
		_player = player;
	}
	
	/**
	 * Checks if the player is currently in offline mode.<br>
	 * This occurs if the client is {@code null} or detached.
	 * @return {@code true} if the player is offline, {@code false} otherwise.
	 */
	public boolean isEnabled()
	{
		final GameClient client = _player.getClient();
		return (client == null) || client.isDetached();
	}
	
	/**
	 * Checks if the player is currently in offline play mode.
	 * @return {@code true} if the player is offline, {@code false} otherwise.
	 */
	public boolean isPlayRunning()
	{
		return _playRunning;
	}
	
	/**
	 * Sets whether the player can play while offline.<br>
	 * Use {@code true} to enable offline play.<br>
	 * Use {@code false} to disable offline play.
	 * @param value The boolean state for offline play.
	 */
	public void setPlayRunning(boolean value)
	{
		_playRunning = value;
	}
	
	/**
	 * Starts the offline play mode for the player.<br>
	 * This method checks if the player meets the requirements to enter offline status.<br>
	 * It handles IP limits, sends a leave packet, and updates the visual appearance.<br>
	 * The player is then marked as detached from the world.
	 */
	public void startPlay()
	{
		if (_player.hasPremiumStatus() && (DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OFFLINE_PLAY, _player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PREMIUM_PER_IP))
		{
			String limit = String.valueOf(AntiFeedManager.getInstance().getLimit(_player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP));
			_player.sendMessage("Only " + limit + " offline players allowed per IP.");
			return;
		}
		else if ((DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OFFLINE_PLAY, _player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP))
		{
			String limit = String.valueOf(AntiFeedManager.getInstance().getLimit(_player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OFFLINEPLAY_PER_IP));
			_player.sendMessage("Only " + limit + " offline players allowed per IP.");
			return;
		}
		
		AntiFeedManager.getInstance().removePlayer(AntiFeedManager.GAME_ID, _player);
		
		_player.sendPacket(LeaveWorld.STATIC_PACKET);
		
		if (OfflinePlayConfig.OFFLINE_PLAY_SET_NAME_COLOR)
		{
			_player.getAppearance().setNameColor(OfflinePlayConfig.OFFLINE_PLAY_NAME_COLOR);
		}
		
		if (!OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.isEmpty())
		{
			_player.getEffectList().startAbnormalVisualEffect(OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.get(Rnd.get(OfflinePlayConfig.OFFLINE_PLAY_ABNORMAL_EFFECTS.size())));
		}
		
		_player.broadcastUserInfo();
		
		_playRunning = true;
		_player.getClient().setDetached(true);
		
		if (OfflinePlayConfig.RESTORE_AUTO_PLAY_OFFLINERS)
		{
			OfflinePlayTable.getInstance().storeOfflinePlay(_player);
		}
	}
	
	/**
	 * Retrieves the timestamp when the offline shop was started.
	 * @return The start time as a {@code long}.
	 */
	public long getShopStartTime()
	{
		return _shopStart;
	}
	
	/**
	 * Sets the start time for the offline shop.<br>
	 * This value is stored in the {@code _shopStart} field.
	 * @param time The timestamp to set as the start time.
	 */
	public void setShopStartTime(long time)
	{
		_shopStart = time;
	}
}
