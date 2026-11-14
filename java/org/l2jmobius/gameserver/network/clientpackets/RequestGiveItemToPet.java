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

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.PetItemList;

/**
 * Handles the client request to give an {@code Item} to a {@code Pet}.<br>
 * This packet processes the transfer of items from the player's inventory to their pet.<br>
 * It validates the action and updates the relevant inventories accordingly.
 */
public class RequestGiveItemToPet extends ClientPacket
{
	private int _objectId;
	private long _amount;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_amount = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if ((_amount <= 0) || (player == null) || !player.hasPet())
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			player.sendMessage("You are giving items to pet too fast.");
			return;
		}
		
		if (player.hasItemRequest())
		{
			return;
		}
		
		// Alt game - Karma punishment
		if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_TRADE && (player.getReputation() < 0))
		{
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendMessage("You cannot give items while trading.");
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_objectId);
		if (item == null)
		{
			return;
		}
		
		final long itemCount = item.getCount();
		if (_amount > itemCount)
		{
			PunishmentManager.handleIllegalPlayerAction(player, getClass().getSimpleName() + ": Character " + player.getName() + " of account " + player.getAccountName() + " tried to give item with oid " + _objectId + " to pet but has invalid count " + _amount + " item count: " + itemCount, GeneralConfig.DEFAULT_PUNISH);
			return;
		}
		
		if (item.isAugmented())
		{
			return;
		}
		
		if (item.isHeroItem() || !item.isDropable() || !item.isDestroyable() || !item.isTradeable())
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return;
		}
		
		// Pets cannot carry arrows, bolts or shots of any kind.
		final ItemType itemType = item.getItemType();
		final ActionType action = item.getTemplate().getDefaultAction();
		if ((itemType == EtcItemType.ARROW) || (itemType == EtcItemType.BOLT) || (action == ActionType.SOULSHOT) || (action == ActionType.SPIRITSHOT) || (action == ActionType.SUMMON_SOULSHOT) || (action == ActionType.SUMMON_SPIRITSHOT) || (action == ActionType.FISHINGSHOT))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return;
		}
		
		final Pet pet = player.getPet();
		if (pet.isDead())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_TRANSFER_AN_ITEM_TO_A_DEAD_PET);
			return;
		}
		
		if (!pet.getInventory().validateCapacity(item))
		{
			player.sendPacket(SystemMessageId.YOUR_PET_S_INVENTORY_IS_FULL_REMOVE_SOMETHING_FROM_IT_AND_TRY_AGAIN);
			return;
		}
		
		if (!pet.getInventory().validateWeight(item, _amount))
		{
			player.sendPacket(SystemMessageId.THE_PET_S_INVENTORY_IS_FULL);
			return;
		}
		
		final Item transferedItem = player.transferItem(ItemProcessType.TRANSFER, _objectId, _amount, pet.getInventory(), pet);
		if (transferedItem != null)
		{
			player.sendPacket(new PetItemList(pet.getInventory().getItems()));
		}
		else
		{
			PacketLogger.warning("Invalid item transfer request: " + pet.getName() + "(pet) --> " + player.getName());
		}
	}
}
