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
package org.l2jmobius.gameserver.network.clientpackets.augmentation;

import java.util.Arrays;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.EnchantItemAttributeRequest;
import org.l2jmobius.gameserver.entity.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.options.AugmentationFee;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * This class serves as a base for all network packets related to the item refining system.<br>
 * It provides common functionality for handling refinement requests between the client and the server.
 */
public abstract class AbstractRefinePacket extends ClientPacket
{
	/**
	 * Validates the requirements for an augmentation process.<br>
	 * This method checks if the player, items, and fees are all valid.<br>
	 * It ensures the fee item belongs to the player and is in their inventory.
	 * @param player The {@code Player} performing the action.
	 * @param item The primary {@code Item} being modified.
	 * @param mineralItem The {@code Item} used as a mineral.
	 * @param feeItem The {@code Item} provided as a fee.
	 * @param fee The {@code AugmentationFee} configuration for this process.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	protected static boolean isValid(Player player, Item item, Item mineralItem, Item feeItem, AugmentationFee fee)
	{
		if (fee == null)
		{
			return false;
		}
		
		if (!isValid(player, item, mineralItem))
		{
			return false;
		}
		
		if (feeItem != null)
		{
			// GemStones must belong to owner.
			if (feeItem.getOwnerId() != player.getObjectId())
			{
				return false;
			}
			
			// .. and located in inventory
			if (feeItem.getItemLocation() != ItemLocation.INVENTORY)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Validates the requirements for a refinement process.<br>
	 * This method checks if the {@code Player}, {@code Item}, and {@code mineralItem} are valid.<br>
	 * It ensures the {@code mineralItem} belongs to the player and is in their inventory.
	 * @param player The {@code Player} performing the action.
	 * @param item The primary {@code Item} being refined.
	 * @param mineralItem The {@code Item} used as a mineral or lifestone.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	protected static boolean isValid(Player player, Item item, Item mineralItem)
	{
		if (!isValid(player, item))
		{
			return false;
		}
		
		// Item must belong to owner.
		if (mineralItem.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		// Lifestone must be located in inventory.
		if (mineralItem.getItemLocation() != ItemLocation.INVENTORY)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if a {@code Player} can perform an action on a specific {@code Item}.<br>
	 * This method verifies ownership, item type restrictions, and location.<br>
	 * It also checks against the augmentation blacklist.
	 * @param player The {@code Player} attempting the action.
	 * @param item The {@code Item} being checked for validity.
	 * @return {@code true} if the operation is valid, {@code false} otherwise.
	 */
	protected static boolean isValid(Player player, Item item)
	{
		if (!isValid(player))
		{
			return false;
		}
		
		// Item must belong to owner.
		if (item.getOwnerId() != player.getObjectId())
		{
			return false;
		}
		
		if (item.isHeroItem())
		{
			return false;
		}
		
		if (item.isShadowItem())
		{
			return false;
		}
		
		if (item.isCommonItem())
		{
			return false;
		}
		
		if (item.isEtcItem())
		{
			return false;
		}
		
		if (item.isTimeLimitedItem())
		{
			return false;
		}
		
		if (item.isPvp() && !PlayerConfig.ALT_ALLOW_AUGMENT_PVP_ITEMS)
		{
			return false;
		}
		
		// Source item can be equipped or in inventory.
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				break;
			}
			default:
			{
				return false;
			}
		}
		
		final ItemTemplate template = item.getTemplate();
		if (!(template instanceof Weapon) && !(template instanceof Armor))
		{
			return false; // Neither weapon nor armor?
		}
		
		// blacklist check
		if (Arrays.binarySearch(PlayerConfig.AUGMENTATION_BLACKLIST, item.getId()) >= 0)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the {@code Player} is in a valid state to perform an augmentation.<br>
	 * This method verifies that the player is not dead, trading, fishing, or sitting.<br>
	 * It also ensures the player is not currently processing other transactions.
	 * @param player The {@code Player} object to validate.
	 * @return {@code true} if the player can proceed with the action, {@code false} otherwise.
	 */
	protected static boolean isValid(Player player)
	{
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP_IS_IN_OPERATION);
			return false;
		}
		
		if (player.getActiveTradeList() != null)
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_ENGAGED_IN_TRADE_ACTIVITIES);
			return false;
		}
		
		if (player.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_DEAD);
			return false;
		}
		
		if (player.hasBlockActions() && player.hasAbnormalType(AbnormalType.PARALYZE))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_PARALYZED);
			return false;
		}
		
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_FISHING);
			return false;
		}
		
		if (player.isSitting())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_AUGMENT_ITEMS_WHILE_SITTING);
			return false;
		}
		
		if (player.isCursedWeaponEquipped())
		{
			return false;
		}
		
		if (player.hasRequest(EnchantItemRequest.class, EnchantItemAttributeRequest.class) || player.isProcessingTransaction())
		{
			return false;
		}
		
		return true;
	}
}
