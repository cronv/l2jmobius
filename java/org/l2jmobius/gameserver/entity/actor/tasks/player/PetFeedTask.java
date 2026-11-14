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

import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This task handles the logic for feeding a player's pet.<br>
 * It processes the interaction between the {@code Player} and their pet entity.
 * @author UnAfraid
 */
public class PetFeedTask implements Runnable
{
	private static final Logger LOGGER = Logger.getLogger(PetFeedTask.class.getName());
	
	private final Player _player;
	
	/**
	 * Creates a new task to handle feeding a pet.<br>
	 * This constructor initializes the task with a specific {@code Player}.
	 * @param player The {@code Player} who owns the pet being fed.
	 */
	public PetFeedTask(Player player)
	{
		_player = player;
	}
	
	@Override
	public void run()
	{
		if (_player == null)
		{
			return;
		}
		
		try
		{
			final int mountNpcId = _player.getMountNpcId();
			if (!_player.isMounted() || (mountNpcId == 0) || (_player.getPetData(mountNpcId) == null))
			{
				_player.stopFeed();
				return;
			}
			
			final int currentFeed = _player.getCurrentFeed();
			final int feedConsume = _player.getFeedConsume();
			if (currentFeed > feedConsume)
			{
				// eat
				_player.setCurrentFeed(currentFeed - feedConsume);
			}
			else
			{
				_player.setCurrentFeed(0);
				_player.stopFeed();
				
				if (_player.isFlying() && (_player.getMountType() == MountType.WYVERN))
				{
					_player.dismount();
					_player.teleToLocation(TeleportWhereType.TOWN); // Teleport to nearest village.
				}
				else
				{
					_player.dismount();
				}
				
				_player.sendPacket(SystemMessageId.YOU_ARE_OUT_OF_FEED_MOUNT_STATUS_CANCELLED);
				return;
			}
			
			final Set<Integer> foodIds = _player.getPetData(mountNpcId).getFood();
			if (foodIds.isEmpty())
			{
				return;
			}
			
			Item food = null;
			for (int id : foodIds)
			{
				// TODO: possibly pet inv?
				food = _player.getInventory().getItemByItemId(id);
				if (food != null)
				{
					break;
				}
			}
			
			if ((food != null) && _player.isHungry())
			{
				final IItemHandler handler = ItemHandler.getInstance().getHandler(food.getEtcItem());
				if (handler != null)
				{
					handler.onItemUse(_player, food, false);
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_GUARDIAN_WAS_HUNGRY_SO_IT_HAS_EATEN_S1);
					sm.addItemName(food.getId());
					_player.sendPacket(sm);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Mounted Pet [NpcId: " + _player.getMountNpcId() + "] a feed task error has occurred", e);
		}
	}
}
