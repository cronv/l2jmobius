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
package org.l2jmobius.gameserver.network.clientpackets.virtualItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.IllusoryEquipmentConfig;
import org.l2jmobius.gameserver.data.holders.VirtualItemHolder;
import org.l2jmobius.gameserver.data.xml.VirtualItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.managers.IllusoryEquipmentManager;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoEquipSlot;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ItemList;
import org.l2jmobius.gameserver.network.serverpackets.virtualItem.ExVirtualItemSystem;

/**
 * Handles the client request to interact with the {@code ExVirtualItemSystem}.<br>
 * The window opens with type 1, applies a virtual item with type 2 and resets with type 3,<br>
 * a single slot when the selected category is given, every slot at once when it is zero.
 * @author CostyKiller
 */
public class RequestExVirtualItemSystem extends ClientPacket
{
	private static final Logger LOGGER = Logger.getLogger(RequestExVirtualItemSystem.class.getName());
	
	private static final int TYPE_OPEN = 1;
	private static final int TYPE_UPDATE = 2;
	private static final int TYPE_RESET = 3;
	
	// The client files an inventory change on its own tick, so a packet that makes the window redraw has to
	// arrive after it. A virtual item that leaves the paperdoll is destroyed one tick later still, so the
	// window has to wait for that second tick before the slot it left shows up empty.
	private static final int SLOT_REFRESH_DELAY = 100;
	private static final int WINDOW_REFRESH_DELAY = 250;
	
	private int _type;
	private int _selectIndexMain;
	private int _selectIndexSub;
	private int _selectSlot;
	private int _indexMain;
	private int _indexSub;
	private int _slot;
	private int _costVISPoint;
	
	@Override
	protected void readImpl()
	{
		_type = readByte();
		_selectIndexMain = readInt();
		_selectIndexSub = readInt();
		_selectSlot = readInt();
		readShort(); // Size of the block below, this short included, always 26.
		_indexMain = readInt();
		_indexSub = readInt();
		_slot = readInt();
		_costVISPoint = readInt(); // Cost, only compared against the server side data.
		readInt(); // Item or skill id, read from the server side data instead.
		readInt(); // Enchant, read from the server side data instead.
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_ENABLED)
		{
			return;
		}
		
		if (player.isInStoreMode() || player.isCrafting() || player.isProcessingRequest() || player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			return;
		}
		
		if (player.isFishing())
		{
			// You cannot mount, dismount, break and drop items while fishing.
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_FISHING_2);
			return;
		}
		
		if (player.isFlying())
		{
			return;
		}
		
		debug(() -> "request from " + player.getName() + ": type=" + _type + " selectIndexMain=" + _selectIndexMain + " selectIndexSub=" + _selectIndexSub + " selectSlot=" + _selectSlot + " indexMain=" + _indexMain + " indexSub=" + _indexSub + " slot=" + _slot + " cost=" + _costVISPoint);
		switch (_type)
		{
			case TYPE_OPEN:
			{
				sendState(player, true);
				break;
			}
			case TYPE_UPDATE:
			{
				update(player);
				break;
			}
			case TYPE_RESET:
			{
				reset(player);
				break;
			}
			default:
			{
				debug(() -> "unknown request type " + _type + ", nothing was done");
				break;
			}
		}
	}
	
	/**
	 * Applies the requested virtual item, replacing the one already taken from the same category.<br>
	 * The request is refused when the item is unknown, when regular equipment holds the body part it needs,<br>
	 * or when the player cannot pay for the resulting selection.
	 * @param player The {@code Player} applying the virtual item.
	 */
	private void update(Player player)
	{
		final VirtualItemHolder holder = VirtualItemData.getInstance().getVirtualItem(_indexMain, _indexSub);
		if (holder == null)
		{
			debug(() -> "update refused, no virtual item " + _indexMain + "-" + _indexSub + " in the data");
			sendState(player, false);
			return;
		}
		
		debug(() -> "update asks for " + describe(holder) + ", the window priced it at " + _costVISPoint);
		final int indexMain = holder.getIndexMain();
		final List<VirtualItemHolder> selection = IllusoryEquipmentManager.getInstance().getSelection(player, new HashMap<>());
		final List<VirtualItemHolder> wanted = new ArrayList<>(selection.size() + 1);
		for (VirtualItemHolder equipped : selection)
		{
			if (equipped.getIndexMain() != indexMain)
			{
				wanted.add(equipped);
			}
		}
		
		wanted.add(holder);
		
		final int usedPoints = IllusoryEquipmentManager.getInstance().getUsedPoints(wanted);
		final int availablePoints = getAvailablePoints(player);
		if (usedPoints > availablePoints)
		{
			debug(() -> "update refused, the wanted selection costs " + usedPoints + " points and only " + availablePoints + " are available");
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_ITEM_AS_YOU_DO_NOT_HAVE_ENOUGH_ILLUSORY_EQUIPMENT_POINTS);
			sendState(player, false);
			return;
		}
		
		if (!reenchant(player, selection, indexMain, holder))
		{
			removeCategory(player, selection, indexMain);
			apply(player, holder);
		}
		
		player.getVariables().set(PlayerVariables.ILLUSORY_POINTS_USED, usedPoints);
		debug(() -> "applied " + describe(holder) + ", used points " + usedPoints + " of " + availablePoints);
		player.sendPacket(SystemMessageId.THE_SELECTED_ILLUSORY_EFFECT_IS_APPLIED);
		sendState(player, true);
		
		// The window draws its slots from the equipped items the client holds, and the client files those on its
		// own tick, so the answer above lands too early to show the change. It is told once more once it caught up.
		ThreadPool.schedule(() -> sendState(player, true), WINDOW_REFRESH_DELAY);
	}
	
	/**
	 * Removes a single category from the player, or every category when no category is selected.
	 * @param player The {@code Player} resetting the virtual items.
	 */
	private void reset(Player player)
	{
		final PlayerVariables variables = player.getVariables();
		final List<VirtualItemHolder> selection = IllusoryEquipmentManager.getInstance().getSelection(player, new HashMap<>());
		if (_selectIndexMain == 0)
		{
			debug(() -> "reset of every slot, " + selection.size() + " entries to remove");
			for (VirtualItemHolder holder : selection)
			{
				remove(player, holder);
			}
			
			variables.set(PlayerVariables.ILLUSORY_POINTS_USED, 0);
			IllusoryEquipmentManager.getInstance().clearSkills(player);
			sendState(player, true);
			
			// The emptied slots only show once the client has filed the items leaving, which happens after this answer.
			ThreadPool.schedule(() -> refreshWindow(player), WINDOW_REFRESH_DELAY);
			return;
		}
		
		debug(() -> "reset of category " + _selectIndexMain);
		removeCategory(player, selection, _selectIndexMain);
		
		final IllusoryEquipmentManager manager = IllusoryEquipmentManager.getInstance();
		final int usedPoints = manager.getUsedPoints(manager.getSelection(player, new HashMap<>()));
		debug(() -> "used points after the reset " + usedPoints);
		variables.set(PlayerVariables.ILLUSORY_POINTS_USED, usedPoints);
		sendState(player, true);
		
		// The emptied slot only shows once the client has filed the item leaving, which happens after this answer.
		ThreadPool.schedule(() -> refreshWindow(player), WINDOW_REFRESH_DELAY);
	}
	
	/**
	 * Hands the client a fresh inventory and equipment view, then the state of the system.<br>
	 * The window draws its slots from the items the client holds, and the updates a removal files one by one are not enough to make it drop a slot, so the whole list goes over the way opening the inventory sends it. It goes out directly because the buffered helpers of the player hold it back, which would land it after the answer below and leave the window a step behind.
	 * @param player The {@code Player} to catch up.
	 */
	private void refreshWindow(Player player)
	{
		player.sendPacket(new ItemList(1, player));
		player.sendPacket(new ItemList(2, player));
		player.sendPacket(new ExUserInfoEquipSlot(player));
		player.broadcastUserInfo();
		sendState(player, true);
	}
	
	/**
	 * Retrieves the amount of points the player may spend.<br>
	 * The points collected are capped by the limit of the character level, which coupons raise.
	 * @param player The {@code Player} to read.
	 * @return The amount of points available to the player.
	 */
	private int getAvailablePoints(Player player)
	{
		final PlayerVariables variables = player.getVariables();
		final int acquiredPoints = variables.getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0);
		final int limit = IllusoryEquipmentConfig.getPointsLimit(player.getLevel()) + variables.getInt(PlayerVariables.ILLUSORY_POINTS_BONUS, 0);
		if (acquiredPoints > limit)
		{
			return limit;
		}
		
		return acquiredPoints;
	}
	
	/**
	 * Removes every virtual item of a category from the player.
	 * @param player The {@code Player} to strip.
	 * @param selection The virtual items the player currently carries.
	 * @param indexMain The category to remove.
	 */
	private void removeCategory(Player player, List<VirtualItemHolder> selection, int indexMain)
	{
		for (VirtualItemHolder holder : selection)
		{
			if (holder.getIndexMain() == indexMain)
			{
				remove(player, holder);
			}
		}
	}
	
	/**
	 * Raises or lowers the enchant of a virtual item the player already wears. Only the very same item on the very same body part is changed this way, anything else is applied anew.
	 * @param player The {@code Player} wearing the virtual item.
	 * @param selection The virtual items the player currently carries.
	 * @param indexMain The category the request belongs to.
	 * @param holder The virtual item that was asked for.
	 * @return {@code true} when the worn item was changed, {@code false} when it has to be applied anew.
	 */
	private boolean reenchant(Player player, List<VirtualItemHolder> selection, int indexMain, VirtualItemHolder holder)
	{
		final int itemId = holder.getItemId();
		final long slot = holder.getSlot();
		if (slot == 0)
		{
			return false;
		}
		
		boolean worn = false;
		for (VirtualItemHolder applied : selection)
		{
			if ((applied.getIndexMain() == indexMain) && (applied.getItemId() == itemId) && (applied.getSlot() == slot))
			{
				worn = true;
				break;
			}
		}
		
		if (!worn)
		{
			return false;
		}
		
		final Item item = findWornVirtualItem(player, itemId);
		if (item == null)
		{
			return false;
		}
		
		// The item keeps its place and its object, only the enchant changes, so the client has nothing to file away.
		final int enchant = holder.getEnchant();
		item.setEnchantLevel(enchant);
		item.updateDatabase();
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendInventoryUpdate(iu);
		player.broadcastUserInfo();
		
		IllusoryEquipmentManager.getInstance().rememberItem(player, holder, item.getLocationSlot());
		debug(() -> "changed item " + itemId + " object " + item.getObjectId() + " to +" + enchant + " without taking it off");
		return true;
	}
	
	/**
	 * Grants a virtual item to the player, as a skill when it has no body part, as an equipped item otherwise.
	 * @param player The {@code Player} receiving the virtual item.
	 * @param holder The virtual item to grant.
	 */
	private void apply(Player player, VirtualItemHolder holder)
	{
		final int itemId = holder.getItemId();
		final int enchant = holder.getEnchant();
		if (holder.getSlot() == 0)
		{
			// A skill of the system is a buff, and the buff slot the window picked has to be remembered.
			IllusoryEquipmentManager.getInstance().applySkill(player, holder, _slot);
			debug(() -> "granted skill " + itemId + " level " + enchant + " in buff slot " + _slot);
			return;
		}
		
		final Item item = new Item(itemId);
		item.setCount(1);
		item.setEnchantLevel(enchant);
		item.setVirtual(true);
		
		// A virtual item takes the body part it needs, whatever wore it before goes back to the inventory.
		final PlayerInventory inventory = player.getInventory();
		final List<Item> wornBefore = new ArrayList<>(inventory.getPaperdollItems());
		inventory.addItem(ItemProcessType.NONE, item, player, null);
		
		// The client has never seen this item, so it has to arrive in the inventory before it can be worn there.
		final InventoryUpdate received = new InventoryUpdate();
		received.addNewItem(item);
		player.sendInventoryUpdate(received);
		
		inventory.equipItem(item);
		
		// Every item the new one pushed off has to be told as well, the client draws the first worn item it holds for a slot.
		final InventoryUpdate worn = new InventoryUpdate();
		worn.addModifiedItem(item);
		for (Item displaced : wornBefore)
		{
			if (!displaced.isEquipped())
			{
				worn.addModifiedItem(displaced);
				debug(() -> "regular item " + displaced.getId() + " object " + displaced.getObjectId() + " was pushed off the body part");
			}
		}
		
		player.sendInventoryUpdate(worn);
		
		// The client takes a tick to file the change, so the slot refresh that makes the window redraw has to arrive after it.
		ThreadPool.schedule(() ->
		{
			player.sendPacket(new ExUserInfoEquipSlot(player));
			player.broadcastUserInfo();
		}, SLOT_REFRESH_DELAY);
		// A hard server stop must not leave the item behind unequipped, the window would draw that one instead.
		item.updateDatabase();
		
		final int locationSlot = item.getLocationSlot();
		IllusoryEquipmentManager.getInstance().rememberItem(player, holder, locationSlot);
		debug(() -> "equipped item " + itemId + " +" + enchant + " as object " + item.getObjectId() + " on paperdoll slot " + locationSlot);
	}
	
	/**
	 * Takes a virtual item back from the player, removing the skill or destroying the equipped item.
	 * @param player The {@code Player} losing the virtual item.
	 * @param holder The virtual item to take back.
	 */
	private void remove(Player player, VirtualItemHolder holder)
	{
		final int itemId = holder.getItemId();
		final int enchant = holder.getEnchant();
		if (holder.getSlot() == 0)
		{
			IllusoryEquipmentManager.getInstance().removeSkill(player, holder);
			debug(() -> "removed skill " + itemId + " level " + enchant);
			return;
		}
		
		final PlayerInventory inventory = player.getInventory();
		final Item item = findWornVirtualItem(player, itemId);
		if (item == null)
		{
			debug(() -> "virtual item " + itemId + " was not found on the paperdoll, nothing was removed");
			return;
		}
		
		final int objectId = item.getObjectId();
		
		// Unequipping it is enough, the inventory destroys a virtual item that leaves the paperdoll.
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addItems(inventory.unEquipItemInSlotAndRecord(item.getLocationSlot()));
		player.sendInventoryUpdate(iu);
		player.broadcastUserInfo();
		debug(() -> "removed item " + itemId + " object " + objectId);
	}
	
	/**
	 * Sends the current state of the system back to the client.
	 * @param player The {@code Player} to answer.
	 * @param result The result the client shows for the request.
	 */
	private void sendState(Player player, boolean result)
	{
		final Map<Integer, Integer> slotIndexes = new HashMap<>();
		final List<VirtualItemHolder> selection = IllusoryEquipmentManager.getInstance().getSelection(player, slotIndexes);
		debug(() -> "answering type " + _type + " with result " + result + " and " + selection.size() + " entries");
		player.sendPacket(new ExVirtualItemSystem(player, _type, result, _selectIndexMain, _selectIndexSub, _selectSlot, selection, slotIndexes));
	}
	
	/**
	 * Finds the virtual item of the given identifier a player wears.<br>
	 * A regular item of the same identifier may sit on the paperdoll as well, so the virtual mark decides.
	 * @param player The {@code Player} to look at.
	 * @param itemId The identifier of the virtual item to find.
	 * @return The worn virtual {@code Item}, {@code null} when the player wears none.
	 */
	private static Item findWornVirtualItem(Player player, int itemId)
	{
		final Collection<Item> worn = player.getInventory().getPaperdollItems(item -> (item.getId() == itemId) && item.isVirtual());
		if (worn.isEmpty())
		{
			return null;
		}
		
		return worn.iterator().next();
	}
	
	/**
	 * Spells out a virtual item for the debug lines.
	 * @param holder The virtual item to spell out.
	 * @return A single line describing the given virtual item.
	 */
	private String describe(VirtualItemHolder holder)
	{
		final long slot = holder.getSlot();
		return holder.getIndexMain() + "-" + holder.getIndexSub() + " body part " + slot + (slot == 0 ? " skill " : " item ") + holder.getItemId() + " +" + holder.getEnchant() + " cost " + holder.getCostVISPoint() + " total " + VirtualItemData.getInstance().getTotalCost(holder);
	}
	
	/**
	 * Writes a line to the game server console while the system runs with debug enabled.
	 * @param message The line to write.
	 */
	private void debug(Supplier<String> message)
	{
		if (IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_EVENT_DEBUG_ENABLED)
		{
			LOGGER.info("IllusoryEquipment: " + message.get());
		}
	}
}
