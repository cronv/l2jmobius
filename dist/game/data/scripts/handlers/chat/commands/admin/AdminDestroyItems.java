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
package handlers.chat.commands.admin;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Handles the admin command to destroy specific items from a player's inventory.<br>
 * It removes the target {@code Item} and updates the {@code PlayerInventory} accordingly.
 * @author Mobius
 */
public class AdminDestroyItems implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_destroy_items",
		"admin_destroy_all_items",
		"admin_destroyitems",
		"admin_destroyallitems"
	};
	
	/**
	 * Processes the command to destroy items in a player's inventory.<br>
	 * It removes specific or all items based on the {@code command} string.<br>
	 * The method updates the player's inventory and sends a network update.
	 * @param command The command string used to determine which items to delete.
	 * @param activeChar The {@code Player} whose inventory will be modified.
	 * @return {@code true} if the command was executed successfully.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final PlayerInventory inventory = activeChar.getInventory();
		final InventoryUpdate iu = new InventoryUpdate();
		for (Item item : inventory.getItems())
		{
			if (item.isEquipped() && !command.contains("all"))
			{
				continue;
			}
			
			iu.addRemovedItem(item);
			inventory.destroyItem(ItemProcessType.DESTROY, item, activeChar, null);
		}
		
		activeChar.sendInventoryUpdate(iu);
		return true;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * This method returns all aliases associated with {@code AdminDestroyItems}.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
