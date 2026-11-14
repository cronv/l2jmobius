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
package handlers.actions.user;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListBuy;
import org.l2jmobius.gameserver.network.serverpackets.PrivateStoreManageListSell;
import org.l2jmobius.gameserver.network.serverpackets.RecipeShopManageList;

/**
 * Handles the actions for opening and closing a {@code PrivateStore}.<br>
 * This class manages user interactions with private stores in the game world.
 * @author Nik
 */
public class PrivateStore implements IActionUserHandler
{
	private static final Logger LOGGER = Logger.getLogger(PrivateStore.class.getName());
	
	/**
	 * Handles the logic for opening or closing a private store.<br>
	 * This method checks if the {@code Player} is allowed to interact with stores.<br>
	 * It updates the player's store type and sends the appropriate packets based on the action.
	 * @param player The {@code Player} who triggered the action.
	 * @param data The {@code ActionDataHolder} containing the action details.
	 * @param ctrlPressed The state of the {@code Ctrl} key.
	 * @param shiftPressed The state of the {@code Shift} key.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		final PrivateStoreType type = PrivateStoreType.findById(data.getOptionId());
		if (type == null)
		{
			LOGGER.warning("Incorrect private store type: " + data.getOptionId());
			return;
		}
		
		// Player shouldn't be able to set stores if he/she is alike dead (dead or fake death).
		if (!player.canOpenPrivateStore())
		{
			if (player.isInsideZone(ZoneId.NO_STORE))
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_OPEN_A_PRIVATE_STORE_HERE);
			}
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		switch (type)
		{
			case SELL:
			case SELL_MANAGE:
			case PACKAGE_SELL:
			{
				final PrivateStoreType privateStoreType = player.getPrivateStoreType();
				if ((privateStoreType == PrivateStoreType.SELL) || (privateStoreType == PrivateStoreType.SELL_MANAGE) || (privateStoreType == PrivateStoreType.PACKAGE_SELL))
				{
					player.setPrivateStoreType(PrivateStoreType.NONE);
					player.getSellList().clear();
				}
				break;
			}
			case BUY:
			case BUY_MANAGE:
			{
				final PrivateStoreType privateStoreType = player.getPrivateStoreType();
				if ((privateStoreType == PrivateStoreType.BUY) || (privateStoreType == PrivateStoreType.BUY_MANAGE))
				{
					player.setPrivateStoreType(PrivateStoreType.NONE);
				}
				break;
			}
			case MANUFACTURE:
			{
				player.setPrivateStoreType(PrivateStoreType.NONE);
				player.broadcastUserInfo();
				break;
			}
		}
		
		if (player.getPrivateStoreType() == PrivateStoreType.NONE)
		{
			if (player.isSitting())
			{
				player.standUp();
			}
			
			switch (type)
			{
				case SELL:
				case SELL_MANAGE:
				case PACKAGE_SELL:
				{
					player.setPrivateStoreType(PrivateStoreType.SELL_MANAGE);
					player.sendPacket(new PrivateStoreManageListSell(1, player, type == PrivateStoreType.PACKAGE_SELL));
					player.sendPacket(new PrivateStoreManageListSell(2, player, type == PrivateStoreType.PACKAGE_SELL));
					break;
				}
				case BUY:
				case BUY_MANAGE:
				{
					player.setPrivateStoreType(PrivateStoreType.BUY_MANAGE);
					player.sendPacket(new PrivateStoreManageListBuy(1, player));
					player.sendPacket(new PrivateStoreManageListBuy(2, player));
					break;
				}
				case MANUFACTURE:
				{
					player.sendPacket(new RecipeShopManageList(player, true));
					break;
				}
			}
		}
	}
}
