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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.attributechange.ExChangeAttributeItemList;

/**
 * Handles the logic for using a {@code ChangeAttributeCrystal} item.<br>
 * This class processes attribute changes when a player interacts with the crystal.<br>
 * It ensures that the correct attributes are modified on the {@code Player} entity.
 * @author Mobius
 */
public class ChangeAttributeCrystal implements IItemHandler
{
	/**
	 * Handles the logic for using an item to change attributes.<br>
	 * It checks if the user is a {@code Player} and not in store mode.<br>
	 * It identifies compatible weapons in the inventory to display for selection.
	 * @param playable The entity attempting to use the item.
	 * @param item The specific item being used to trigger the change.
	 * @param forceUse A flag indicating if the action should be forced.
	 * @return {@code true} if the operation was successful, otherwise {@code false}.
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
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_CHANGE_AN_ATTRIBUTE_WHILE_USING_A_PRIVATE_STORE_OR_WORKSHOP);
			return false;
		}
		
		final List<ItemInfo> itemList = new ArrayList<>();
		for (Item i : player.getInventory().getItems())
		{
			if (i.isWeapon() && i.hasAttributes() && (i.getTemplate().getCrystalTypePlus() == item.getTemplate().getCrystalTypePlus()))
			{
				itemList.add(new ItemInfo(i));
			}
		}
		
		if (itemList.isEmpty())
		{
			player.sendPacket(SystemMessageId.THE_ITEM_FOR_CHANGING_AN_ATTRIBUTE_DOES_NOT_EXIST);
			return false;
		}
		
		final int itemId = item.getId();
		player.sendPacket(new ExChangeAttributeItemList(1, itemId, itemList));
		player.sendPacket(new ExChangeAttributeItemList(0, itemId, itemList));
		return true;
	}
}
