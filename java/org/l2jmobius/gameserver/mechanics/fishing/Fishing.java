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
package org.l2jmobius.gameserver.mechanics.fishing;

import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.FishingData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.FishingZone;
import org.l2jmobius.gameserver.entity.zone.type.WaterZone;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerFishing;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.fishing.ExFishingEnd;
import org.l2jmobius.gameserver.network.serverpackets.fishing.ExFishingStart;
import org.l2jmobius.gameserver.network.serverpackets.fishing.ExUserInfoFishing;
import org.l2jmobius.gameserver.util.LocationUtil;

/**
 * Handles the core logic for the fishing mechanic in the game.<br>
 * This class manages player interactions with {@code WaterZone} and {@code FishingZone} areas.<br>
 * It processes fishing actions, rewards, and related events for {@code Player} entities.
 * @author bit
 */
public class Fishing
{
	protected static final Logger LOGGER = Logger.getLogger(Fishing.class.getName());
	private ILocational _baitLocation = new Location(0, 0, 0);
	
	private final Player _player;
	private ScheduledFuture<?> _reelInTask;
	private ScheduledFuture<?> _startFishingTask;
	private boolean _isFishing = false;
	
	/**
	 * Initializes a new {@code Fishing} instance for a specific player.<br>
	 * This constructor links the {@code Player} to the fishing mechanic.
	 * @param player The {@code Player} who will perform the fishing action.
	 */
	public Fishing(Player player)
	{
		_player = player;
	}
	
	/**
	 * Checks if the current actor is currently engaged in a fishing action.<br>
	 * This method is thread-safe and returns {@code true} if fishing is active.<br>
	 * It returns {@code false} otherwise.
	 * @return {@code true} if the actor is fishing, {@code false} otherwise.
	 */
	public synchronized boolean isFishing()
	{
		return _isFishing;
	}
	
	/**
	 * Checks if the player is currently in a valid area for fishing.<br>
	 * This method verifies if the player is inside a zone designated as {@code ZoneId.FISHING}.
	 * @return {@code true} if the location is valid, {@code false} otherwise.
	 */
	public boolean isAtValidLocation()
	{
		// TODO: implement checking direction
		// if (calculateBaitLocation() == null)
		// {
		// return false;
		// }
		
		return _player.isInsideZone(ZoneId.FISHING);
	}
	
	/**
	 * Checks if the player is allowed to start fishing.<br>
	 * This method verifies that the player is alive and not in a restricted state.<br>
	 * It ensures the player is not sitting or currently performing other actions.
	 * @return {@code true} if the player can fish, {@code false} otherwise.
	 */
	public boolean canFish()
	{
		return !_player.isDead() && !_player.isAlikeDead() && !_player.hasBlockActions() && !_player.isSitting();
	}
	
	/**
	 * Retrieves the data for the bait currently held by the player.<br>
	 * It checks the item in the left hand of the {@code Player}.<br>
	 * If no item is found, it returns {@code null}.
	 * @return The {@code FishingData} associated with the bait item or {@code null}.
	 */
	private FishingBait getCurrentBaitData()
	{
		final Item bait = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
		return bait != null ? FishingData.getInstance().getBaitData(bait.getId()) : null;
	}
	
	/**
	 * Stops all active background tasks for the fishing mechanic.<br>
	 * This method cancels both the {@code _reelInTask} and the {@code _startFishingTask}.<br>
	 * It sets these task references to {@code null} after cancellation.
	 */
	private void cancelTasks()
	{
		if (_reelInTask != null)
		{
			_reelInTask.cancel(false);
			_reelInTask = null;
		}
		
		if (_startFishingTask != null)
		{
			_startFishingTask.cancel(false);
			_startFishingTask = null;
		}
	}
	
	/**
	 * Starts the fishing process for the player.<br>
	 * This method sets the {@code _isFishing} flag to {@code true}.<br>
	 * It then calls the {@code castLine()} method to begin the action.
	 */
	public synchronized void startFishing()
	{
		if (_isFishing)
		{
			return;
		}
		
		_isFishing = true;
		castLine();
	}
	
	/**
	 * Validates all requirements for casting a fishing line.<br>
	 * Checks player status, bait data, and equipment.<br>
	 * Cancels existing tasks before starting the new fishing process.<br>
	 * Schedules the reel-in task if all conditions are met.
	 */
	private void castLine()
	{
		if (!GeneralConfig.ALLOW_FISHING && !_player.isGM())
		{
			_player.sendMessage("Fishing is disabled.");
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		cancelTasks();
		
		if (!canFish())
		{
			if (_isFishing)
			{
				_player.sendPacket(SystemMessageId.YOUR_ATTEMPT_AT_FISHING_HAS_BEEN_CANCELLED);
			}
			
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		final FishingBait baitData = getCurrentBaitData();
		if (baitData == null)
		{
			_player.sendPacket(SystemMessageId.YOU_MUST_PUT_BAIT_ON_YOUR_HOOK_BEFORE_YOU_CAN_FISH);
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		if (PremiumSystemConfig.PREMIUM_SYSTEM_ENABLED)
		{
			final boolean hasPremiumStatus = _player.hasPremiumStatus();
			if (PremiumSystemConfig.PREMIUM_ONLY_FISHING && !hasPremiumStatus)
			{
				_player.sendPacket(SystemMessageId.FISHING_IS_AVAILABLE_TO_PREMIUM_USERS_ONLY);
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				stopFishing(FishingEndType.ERROR);
				return;
			}
			
			if (baitData.isPremiumOnly() && !hasPremiumStatus)
			{
				_player.sendPacket(SystemMessageId.FAILED_PLEASE_TRY_AGAIN_USING_THE_CORRECT_BAIT);
				_player.sendPacket(ActionFailed.STATIC_PACKET);
				stopFishing(FishingEndType.ERROR);
				return;
			}
		}
		
		final int minPlayerLevel = baitData.getMinPlayerLevel();
		final int maxPLayerLevel = baitData.getMaxPlayerLevel();
		final int playerLevel = _player.getLevel();
		if ((playerLevel < minPlayerLevel) || (playerLevel > maxPLayerLevel))
		{
			_player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_FISHING_LEVEL_REQUIREMENTS);
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		final Item rod = _player.getActiveWeaponInstance();
		if ((rod == null) || (rod.getItemType() != WeaponType.FISHINGROD))
		{
			_player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_FISHING_ROD);
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		final FishingRod rodData = FishingData.getInstance().getRodData(rod.getId());
		if (rodData == null)
		{
			_player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_FISHING_ROD);
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		if (_player.isTransformed() || _player.isInBoat())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHEN_TRANSFORMED_OR_WHILE_RIDING_AS_A_PASSENGER_OF_A_BOAT_IT_S_AGAINST_THE_RULES);
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		if (_player.isCrafting() || _player.isInStoreMode())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHILE_USING_A_RECIPE_BOOK_PRIVATE_WORKSHOP_OR_PRIVATE_STORE);
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		if (_player.isInsideZone(ZoneId.WATER) || _player.isInWater())
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_WHILE_UNDER_WATER);
			_player.sendPacket(ActionFailed.STATIC_PACKET);
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		_baitLocation = calculateBaitLocation();
		if (!_player.isInsideZone(ZoneId.FISHING) || (_baitLocation == null))
		{
			if (_isFishing)
			{
				// _player.sendPacket(SystemMessageId.YOUR_ATTEMPT_AT_FISHING_HAS_BEEN_CANCELLED);
				_player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			else
			{
				_player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_HERE_AS_YOU_ARE_NOT_FACING_WATER_OR_THIS_IS_NOT_A_FISHING_PLACE);
				_player.sendPacket(ActionFailed.STATIC_PACKET);
			}
			
			stopFishing(FishingEndType.ERROR);
			return;
		}
		
		if (!_player.isChargedShot(ShotType.FISH_SOULSHOTS) || !_player.isChargedShot(ShotType.GOLD_FISH_SOULSHOTS))
		{
			_player.rechargeShots(false, false, true);
		}
		
		final long fishingTime = Math.max(Rnd.get(baitData.getTimeMin(), baitData.getTimeMax()) - rodData.getReduceFishingTime(), 1000);
		final long fishingWaitTime = Rnd.get(baitData.getWaitMin(), baitData.getWaitMax());
		_reelInTask = ThreadPool.schedule(() ->
		{
			final Fishing fishing = _player.getFishing();
			fishing.reelInWithReward();
			_startFishingTask = ThreadPool.schedule(() -> fishing.castLine(), fishingWaitTime);
		}, fishingTime);
		_player.stopMove(null);
		_player.broadcastPacket(new ExFishingStart(_player, -1, _baitLocation));
		_player.sendPacket(new ExUserInfoFishing(_player, true, _baitLocation));
		_player.sendPacket(new PlaySound(1, "sf_p_01", 0, 0, 0, 0, 0));
		_player.sendPacket(SystemMessageId.YOU_CAST_YOUR_LINE_AND_START_TO_FISH);
	}
	
	/**
	 * Completes the fishing action and determines if a reward is granted.<br>
	 * It checks the bait data to calculate the success chance.<br>
	 * If the player uses a {@code FISH_SOULSHOTS}, the success chance is doubled.<br>
	 * The method then calls {@code reelIn} based on the random result.
	 */
	public void reelInWithReward()
	{
		// Fish may or may not eat the hook. If it does - it consumes fishing bait and fishing shot.
		// Then player may or may not catch the fish. Using fishing shots increases chance to win.
		final FishingBait baitData = getCurrentBaitData();
		if (baitData == null)
		{
			reelIn(FishingEndReason.LOSE, false);
			LOGGER.warning("Player " + _player + " is fishing with unhandled bait: " + _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND));
			return;
		}
		
		double chance = baitData.getChance();
		if (_player.isChargedShot(ShotType.FISH_SOULSHOTS))
		{
			chance *= 2;
		}
		else if (_player.isChargedShot(ShotType.GOLD_FISH_SOULSHOTS))
		{
			chance = 100;
		}
		
		if (Rnd.get(100) <= chance)
		{
			reelIn(FishingEndReason.WIN, true);
		}
		else
		{
			reelIn(FishingEndReason.LOSE, true);
		}
	}
	
	/**
	 * Completes the fishing action based on a specific result.<br>
	 * This method handles rewards for winning or loss messages for losing.<br>
	 * It also manages bait consumption and notifies relevant listeners.
	 * @param reasonValue The {@code FishingEndReason} that triggered the end of fishing.
	 * @param consumeBait Whether to remove one bait item from the player's hand.
	 */
	private void reelIn(FishingEndReason reasonValue, boolean consumeBait)
	{
		if (!_isFishing)
		{
			return;
		}
		
		cancelTasks();
		
		FishingEndReason reason = reasonValue;
		try
		{
			final Item bait = _player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_LHAND);
			if (consumeBait && ((bait == null) || !_player.getInventory().updateItemCount(null, bait, -1, _player, null)))
			{
				reason = FishingEndReason.LOSE; // no bait - no reward
				return;
			}
			
			if ((reason == FishingEndReason.WIN) && (bait != null))
			{
				final int baitId = bait.getId();
				final FishingBait baitData = FishingData.getInstance().getBaitData(baitId);
				final FishingCatch fishingCatchData = baitData.getRandom();
				if (fishingCatchData != null)
				{
					final FishingData fishingData = FishingData.getInstance();
					final double lvlModifier = (Math.pow(_player.getLevel(), 2.2) * fishingCatchData.getMultiplier());
					final PlayerStat stat = _player.getStat();
					final long xp = (long) (Rnd.get(fishingData.getExpRateMin(), fishingData.getExpRateMax()) * lvlModifier * stat.getMul(Stat.FISHING_EXP_SP_BONUS, 1));
					final long sp = (long) (Rnd.get(fishingData.getSpRateMin(), fishingData.getSpRateMax()) * lvlModifier * stat.getMul(Stat.FISHING_EXP_SP_BONUS, 1));
					_player.addExpAndSp(xp, sp, true);
					_player.getInventory().addItem(ItemProcessType.PICKUP, fishingCatchData.getItemId(), 1, _player, null);
					final SystemMessage msg = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_2);
					msg.addItemName(fishingCatchData.getItemId());
					_player.sendPacket(msg);
					if (_player.isChargedShot(ShotType.GOLD_FISH_SOULSHOTS))
					{
						_player.unchargeShot(ShotType.GOLD_FISH_SOULSHOTS);
					}
					else
					{
						_player.unchargeShot(ShotType.FISH_SOULSHOTS);
					}
					
					_player.rechargeShots(false, false, true);
				}
				else
				{
					LOGGER.warning("Could not find fishing rewards for bait " + baitId);
				}
			}
			else if (reason == FishingEndReason.LOSE)
			{
				_player.sendPacket(SystemMessageId.THE_BAIT_HAS_BEEN_LOST_BECAUSE_THE_FISH_GOT_AWAY);
			}
			
			if (consumeBait && EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_FISHING, _player))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnPlayerFishing(_player, reason), _player);
			}
		}
		finally
		{
			_player.broadcastPacket(new ExFishingEnd(_player, reason));
			_player.sendPacket(new ExUserInfoFishing(_player, false));
		}
	}
	
	/**
	 * Stops the current fishing action for the player.<br>
	 * This method triggers a manual stop by the user.<br>
	 * It cleans up active tasks and updates the player state.
	 */
	public void stopFishing()
	{
		stopFishing(FishingEndType.PLAYER_STOP);
	}
	
	/**
	 * Stops the current fishing action.<br>
	 * This method handles reeling in the line and updating the player state.<br>
	 * It sends specific messages based on the provided {@code FishingEndType}.
	 * @param endType The reason why the fishing activity is ending.
	 */
	public synchronized void stopFishing(FishingEndType endType)
	{
		if (_isFishing)
		{
			reelIn(FishingEndReason.STOP, false);
			_isFishing = false;
			switch (endType)
			{
				case PLAYER_STOP:
				{
					_player.sendPacket(SystemMessageId.YOU_REEL_YOUR_LINE_IN_AND_STOP_FISHING);
					break;
				}
				case PLAYER_CANCEL:
				{
					_player.sendPacket(SystemMessageId.YOUR_ATTEMPT_AT_FISHING_HAS_BEEN_CANCELLED);
					break;
				}
			}
		}
	}
	
	/**
	 * Retrieves the current location of the bait.<br>
	 * This is used to determine where the bait is positioned in the world.
	 * @return the {@code ILocational} object representing the bait's position.
	 */
	public ILocational getBaitLocation()
	{
		return _baitLocation;
	}
	
	/**
	 * Calculates a random position for the fishing bait.<br>
	 * It determines a distance based on {@code FishingData} and uses the player's heading.<br>
	 * The method also validates if the target area is within a valid {@code FishingZone} or {@code WaterZone}.<br>
	 * Returns {@code null} if the calculated location is invalid.
	 * @return A new {@code Location} object for the bait, or {@code null} if fishing is impossible at that spot.
	 */
	private Location calculateBaitLocation()
	{
		// calculate a position in front of the player with a random distance
		final int distMin = FishingData.getInstance().getBaitDistanceMin();
		final int distMax = FishingData.getInstance().getBaitDistanceMax();
		final int distance = Rnd.get(distMin, distMax);
		final double angle = LocationUtil.convertHeadingToDegree(_player.getHeading());
		final double radian = Math.toRadians(angle);
		final double sin = Math.sin(radian);
		final double cos = Math.cos(radian);
		final int baitX = (int) (_player.getX() + (cos * distance));
		final int baitY = (int) (_player.getY() + (sin * distance));
		
		// search for fishing zone
		FishingZone fishingZone = null;
		for (ZoneType zone : ZoneManager.getInstance().getZones(_player))
		{
			if (zone instanceof FishingZone)
			{
				fishingZone = (FishingZone) zone;
				break;
			}
		}
		
		// search for water zone
		WaterZone waterZone = null;
		for (ZoneType zone : ZoneManager.getInstance().getZones(baitX, baitY))
		{
			if (zone instanceof WaterZone)
			{
				waterZone = (WaterZone) zone;
				break;
			}
		}
		
		final int baitZ = computeBaitZ(_player, baitX, baitY, fishingZone, waterZone);
		if (baitZ == Integer.MIN_VALUE)
		{
			_player.sendPacket(SystemMessageId.YOU_CANNOT_FISH_HERE_AS_YOU_ARE_NOT_FACING_WATER_OR_THIS_IS_NOT_A_FISHING_PLACE);
			return null;
		}
		
		return new Location(baitX, baitY, baitZ);
	}
	
	/**
	 * Calculates the correct Z coordinate for a fishing bait.<br>
	 * This method validates the height against the {@code WaterZone} and geometry.<br>
	 * It returns {@code Integer.MIN_VALUE} if the location is invalid or obstructed.
	 * @param player The {@code Player} performing the action.
	 * @param baitX The X coordinate of the bait.
	 * @param baitY The Y coordinate of the bait.
	 * @param fishingZone The {@code FishingZone} where the player is fishing.
	 * @param waterZone The {@code WaterZone} containing the bait.
	 * @return The calculated Z coordinate or {@code Integer.MIN_VALUE} if invalid.
	 */
	private static int computeBaitZ(Player player, int baitX, int baitY, FishingZone fishingZone, WaterZone waterZone)
	{
		if (fishingZone == null)
		{
			return Integer.MIN_VALUE;
		}
		
		if (waterZone == null)
		{
			return Integer.MIN_VALUE;
		}
		
		// always use water zone, fishing zone high z is high in the air...
		final int baitZ = waterZone.getWaterZ();
		
		// if (!GeoEngine.getInstance().canSeeTarget(player.getX(), player.getY(), player.getZ(), baitX, baitY, baitZ))
		//
		// return Integer.MIN_VALUE;
		// }
		
		if (GeoEngine.getInstance().hasGeo(baitX, baitY))
		{
			if (GeoEngine.getInstance().getHeight(baitX, baitY, baitZ) > baitZ)
			{
				return Integer.MIN_VALUE;
			}
			
			if (GeoEngine.getInstance().getHeight(baitX, baitY, player.getZ()) > baitZ)
			{
				return Integer.MIN_VALUE;
			}
		}
		
		return baitZ;
	}
}
