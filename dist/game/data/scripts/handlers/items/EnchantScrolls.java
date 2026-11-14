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
package handlers.items;

import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.EnchantItemRequest;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ChooseInventoryItem;

/**
 * This class handles the logic for using enchantment scrolls.<br>
 * It processes {@code EnchantItemRequest} actions to apply effects to items.<br>
 * It manages interactions between {@code Player} entities and their inventory.
 */
public class EnchantScrolls implements IItemHandler
{
	/**
	 * Handles the logic for when a player uses an item.<br>
	 * It checks if the user is allowed to use the item based on their current state.<br>
	 * If successful, it initiates the enchantment request process.
	 * @param playable The entity attempting to use the item.
	 * @param item The specific {@code Item} instance being used.
	 * @param forceUse A boolean indicating if the action should be forced.
	 * @return {@code true} if the item was successfully used, otherwise {@code false}.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		final Player player = playable.asPlayer();
		if (player.isMounted())
		{
			player.sendPacket(SystemMessageId.INAPPROPRIATE_ENCHANT_CONDITIONS);
			return false;
		}
		
		if (player.isCastingNow())
		{
			return false;
		}
		
		if (player.hasItemRequest())
		{
			player.sendPacket(SystemMessageId.ANOTHER_ENCHANTMENT_IS_IN_PROGRESS_PLEASE_COMPLETE_THE_PREVIOUS_TASK_THEN_TRY_AGAIN);
			return false;
		}
		
		player.addRequest(new EnchantItemRequest(player, item.getObjectId()));
		player.sendPacket(new ChooseInventoryItem(item.getId()));
		player.sendPacket(SystemMessageId.SELECT_AN_ITEM_FOR_ENCHANTING);
		return true;
	}
}
