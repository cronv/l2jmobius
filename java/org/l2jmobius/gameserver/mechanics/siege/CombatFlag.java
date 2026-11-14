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
package org.l2jmobius.gameserver.mechanics.siege;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.ItemManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a flag used to manage specific combat behaviors during siege events.<br>
 * This class helps determine how entities interact with each other in a siege context.
 */
public class CombatFlag
{
	private Player _player = null;
	private int _playerId = 0;
	private Item _item = null;
	private Item _itemInstance;
	private final Location _location;
	private final int _itemId;
	@SuppressWarnings("unused")
	private final int _fortId;
	
	/**
	 * Creates a new {@code CombatFlag} instance.<br>
	 * This constructor initializes the flag with its position and item data.
	 * @param fortId The unique identifier for the fortress.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param heading The rotation angle of the flag.
	 * @param itemId The item ID used to represent the flag.
	 */
	public CombatFlag(int fortId, int x, int y, int z, int heading, int itemId)
	{
		_fortId = fortId;
		_location = new Location(x, y, z, heading);
		_itemId = itemId;
	}
	
	/**
	 * Creates a new {@code Item} instance for this flag.<br>
	 * This method places the item into the game world at the stored {@code Location}.<br>
	 * It uses {@code ItemProcessType.QUEST} to initialize the object.
	 */
	public synchronized void spawnMe()
	{
		// Init the dropped ItemInstance and add it in the world as a visible object at the position where mob was last.
		_itemInstance = ItemManager.createItem(ItemProcessType.QUEST, _itemId, 1, null, null);
		_itemInstance.dropMe(null, _location.getX(), _location.getY(), _location.getZ());
	}
	
	/**
	 * Removes this combat flag from the game world.<br>
	 * If a {@code Player} is holding it, the item will be dropped.<br>
	 * If an {@code Item} instance exists, it will be decayed.
	 */
	public synchronized void unSpawnMe()
	{
		if (_player != null)
		{
			dropIt();
		}
		
		if (_itemInstance != null)
		{
			_itemInstance.decayMe();
		}
	}
	
	/**
	 * Equips a specific item for the given {@code Player}.<br>
	 * This method checks if the player is mounted before proceeding.<br>
	 * It updates the inventory and sets the combat flag status to {@code true}.
	 * @param player The {@code Player} who will equip the item.
	 * @param item The {@code Item} to be equipped by the player.
	 * @return {@code true} if the item was successfully equipped, or {@code false} otherwise.
	 */
	public boolean activate(Player player, Item item)
	{
		if (player.isMounted())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_MEET_THE_REQUIRED_CONDITION_TO_EQUIP_THAT_ITEM);
			return false;
		}
		
		// Player holding it data.
		_player = player;
		_playerId = _player.getObjectId();
		_itemInstance = null;
		
		// Equip with the weapon.
		_item = item;
		_player.getInventory().equipItem(_item);
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_EQUIPPED);
		sm.addItemName(_item);
		_player.sendPacket(sm);
		
		// Refresh inventory
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addItem(_item);
		_player.sendInventoryUpdate(iu);
		
		// Refresh player stats
		_player.broadcastUserInfo();
		_player.setCombatFlagEquipped(true);
		return true;
	}
	
	/**
	 * Removes the combat flag from the player.<br>
	 * This method clears all associated stats and destroys the item.<br>
	 * It resets the internal state of this {@code CombatFlag} instance.
	 */
	public void dropIt()
	{
		// Reset player stats
		_player.setCombatFlagEquipped(false);
		final BodyPart bodyPart = BodyPart.fromItem(_item);
		_player.getInventory().unEquipItemInBodySlot(bodyPart);
		_player.destroyItem(ItemProcessType.DESTROY, _item, null, true);
		_item = null;
		_player.broadcastUserInfo();
		_player = null;
		_playerId = 0;
	}
	
	/**
	 * Retrieves the unique identifier for the player associated with this {@code CombatFlag}.<br>
	 * This ID is stored in the internal {@code _playerId} field.
	 * @return The {@code int} ID of the player.
	 */
	public int getPlayerObjectId()
	{
		return _playerId;
	}
	
	/**
	 * Retrieves the {@code Item} instance associated with this combat flag.<br>
	 * This method returns the specific item object used for the flag.
	 * @return The {@code Item} instance or {@code null} if it has not been set.
	 */
	public Item getCombatFlagInstance()
	{
		return _itemInstance;
	}
}
