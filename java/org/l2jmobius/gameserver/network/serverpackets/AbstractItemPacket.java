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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.WarehouseItem;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.buylist.Product;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.network.enums.ItemListType;
import org.l2jmobius.gameserver.network.holders.TradeItem;

/**
 * This class serves as a base for all network packets related to items.<br>
 * It provides common functionality for handling {@code ItemListType} data.<br>
 * Subclasses should extend this to implement specific item-related communications.
 * @author Mobius
 */
public abstract class AbstractItemPacket extends AbstractComponentPacket<ItemListType>
{
	private static final byte[] MASKS =
	{
		0x00
	};
	
	/**
	 * Retrieves the bitmask array for this packet type.<br>
	 * This method is used to determine which fields are present in the data.
	 * @return a {@code byte[]} containing the masks.
	 */
	@Override
	protected byte[] getMasks()
	{
		return MASKS;
	}
	
	/**
	 * Writes a {@code TradeItem} and its quantity to the provided buffer.<br>
	 * This method converts the item into an {@code ItemInfo} object before writing.
	 * @param item The {@code TradeItem} to be written.
	 * @param count The number of items to write.
	 * @param buffer The {@code WriteBuffer} where the data will be stored.
	 */
	protected void writeItem(TradeItem item, long count, WriteBuffer buffer)
	{
		writeItem(new ItemInfo(item), count, buffer);
	}
	
	/**
	 * Writes the data of a {@code TradeItem} into the provided {@code WriteBuffer}.<br>
	 * This method converts the item to an {@code ItemInfo} object before writing.
	 * @param item The {@code TradeItem} to be written.
	 * @param buffer The {@code WriteBuffer} where the data will be stored.
	 */
	protected void writeItem(TradeItem item, WriteBuffer buffer)
	{
		writeItem(new ItemInfo(item), buffer);
	}
	
	/**
	 * Writes the data of a {@code WarehouseItem} into the provided {@code WriteBuffer}.<br>
	 * This method converts the item to an {@code ItemInfo} object before writing.
	 * @param item The {@code WarehouseItem} to be written.
	 * @param buffer The {@code WriteBuffer} where the data will be stored.
	 */
	protected void writeItem(WarehouseItem item, WriteBuffer buffer)
	{
		writeItem(new ItemInfo(item), buffer);
	}
	
	/**
	 * Writes the data of an {@code Item} into the provided {@code WriteBuffer}.<br>
	 * This method converts the {@code Item} into an {@code ItemInfo} object before writing.
	 * @param item The {@code Item} instance to be written.
	 * @param buffer The {@code WriteBuffer} where the data will be stored.
	 */
	protected void writeItem(Item item, WriteBuffer buffer)
	{
		writeItem(new ItemInfo(item), buffer);
	}
	
	/**
	 * Writes the details of a {@code Product} into the provided {@code WriteBuffer}.<br>
	 * This method converts the {@code Product} into an {@code ItemInfo} object before writing.
	 * @param item The {@code Product} object to be written.
	 * @param buffer The {@code WriteBuffer} used to store the data.
	 */
	protected void writeItem(Product item, WriteBuffer buffer)
	{
		writeItem(new ItemInfo(item), buffer);
	}
	
	/**
	 * Writes the data of an {@code ItemInfo} object into a {@code WriteBuffer}.<br>
	 * This method handles basic item properties and conditional attributes.
	 * @param item The {@code ItemInfo} object containing the item data to write.
	 * @param buffer The {@code WriteBuffer} used to store the serialized data.
	 */
	protected void writeItem(ItemInfo item, WriteBuffer buffer)
	{
		final int mask = calculateMask(item);
		final ItemTemplate template = item.getItem();
		buffer.writeShort(mask);
		buffer.writeInt(item.getObjectId()); // ObjectId
		buffer.writeInt(template.getDisplayId()); // ItemId
		buffer.writeByte(template.isQuestItem() || (item.getEquipped() == 1) ? 0xFF : item.getLocation()); // T1
		buffer.writeLong(item.getCount()); // Quantity
		buffer.writeByte(template.getType2()); // Item Type 2 : 00-weapon, 01-shield/armor, 02-ring/earring/necklace, 03-questitem, 04-adena, 05-item
		buffer.writeByte(item.getCustomType1()); // Filler (always 0)
		buffer.writeShort(item.getEquipped()); // Equipped : 00-No, 01-yes
		buffer.writeLong(template.getBodyPart().getMask()); // Slot : 0006-lr.ear, 0008-neck, 0030-lr.finger, 0040-head, 0100-l.hand, 0200-gloves, 0400-chest, 0800-pants, 1000-feet, 4000-r.hand, 8000-r.hand
		buffer.writeShort(item.getEnchantLevel()); // Enchant level (pet level shown in control item).
		buffer.writeInt(item.isDamaged() ? -1 : Math.max(0, item.getMana()));
		buffer.writeByte(0); // 270 protocol
		buffer.writeInt(item.getTime());
		buffer.writeByte(item.isAvailable()); // GOD Item enabled = 1 disabled (red) = 0
		buffer.writeShort(0); // 140 - locked
		
		if (containsMask(mask, ItemListType.AUGMENT_BONUS))
		{
			writeItemAugment(item, buffer);
		}
		
		if (containsMask(mask, ItemListType.ELEMENTAL_ATTRIBUTE))
		{
			writeItemElemental(item, buffer);
		}
		
		// 362 - Removed
		// if (containsMask(mask, ItemListType.ENCHANT_EFFECT))
		// {
		// buffer.writeItemEnchantEffect(item, buffer);
		// }
		
		if (containsMask(mask, ItemListType.VISUAL_ID))
		{
			buffer.writeInt(item.getVisualId()); // Item remodel visual ID
		}
		
		if (containsMask(mask, ItemListType.SOUL_CRYSTAL))
		{
			writeItemEnsoulOptions(item, buffer);
		}
		
		// TODO:
		// if (containsMask(mask, ItemListType.REUSE_DELAY))
		// {
		// final Player owner = item.getOwner();
		// buffer.writeInt(owner == null ? 0 : (int) (owner.getItemRemainingReuseTime(item.getObjectId()) / 1000));
		// }
		// Essence
		// if (containsMask(mask, ItemListType.BLESSED))
		// {
		// buffer.writeByte(1);
		// }
		
		if (containsMask(mask, ItemListType.VIRTUAL_ITEM))
		{
			buffer.writeByte(1); // The mark the client draws as system string 5247 on the tooltip.
		}
	}
	
	/**
	 * Writes the data for a specific {@code ItemInfo} object into the provided buffer.<br>
	 * This method handles the serialization of item properties including quantity and masks.<br>
	 * It also conditionally writes extra attributes like augments or elemental effects.
	 * @param item The {@code ItemInfo} object containing the item data to write.
	 * @param count The number of items to record in the buffer.
	 * @param buffer The {@code WriteBuffer} where the serialized data will be stored.
	 */
	protected void writeItem(ItemInfo item, long count, WriteBuffer buffer)
	{
		final int mask = calculateMask(item);
		final ItemTemplate template = item.getItem();
		buffer.writeShort(mask);
		buffer.writeInt(item.getObjectId()); // ObjectId
		buffer.writeInt(template.getDisplayId()); // ItemId
		buffer.writeByte(template.isQuestItem() || (item.getEquipped() == 1) ? 0xFF : item.getLocation()); // T1
		buffer.writeLong(count); // Quantity
		buffer.writeByte(template.getType2()); // Item Type 2 : 00-weapon, 01-shield/armor, 02-ring/earring/necklace, 03-questitem, 04-adena, 05-item
		buffer.writeByte(item.getCustomType1()); // Filler (always 0)
		buffer.writeShort(item.getEquipped()); // Equipped : 00-No, 01-yes
		buffer.writeLong(template.getBodyPart().getMask()); // Slot : 0006-lr.ear, 0008-neck, 0030-lr.finger, 0040-head, 0100-l.hand, 0200-gloves, 0400-chest, 0800-pants, 1000-feet, 4000-r.hand, 8000-r.hand
		buffer.writeShort(item.getEnchantLevel()); // Enchant level (pet level shown in control item).
		buffer.writeInt(item.isDamaged() ? -1 : Math.max(0, item.getMana()));
		buffer.writeByte(0); // 270 protocol
		buffer.writeInt(item.getTime());
		buffer.writeByte(item.isAvailable()); // GOD Item enabled = 1 disabled (red) = 0
		buffer.writeShort(0); // 140 - locked
		if (containsMask(mask, ItemListType.AUGMENT_BONUS))
		{
			writeItemAugment(item, buffer);
		}
		
		if (containsMask(mask, ItemListType.ELEMENTAL_ATTRIBUTE))
		{
			writeItemElemental(item, buffer);
		}
		
		// 362 - Removed
		// if (containsMask(mask, ItemListType.ENCHANT_EFFECT))
		// {
		// buffer.writeItemEnchantEffect(item, buffer);
		// }
		
		if (containsMask(mask, ItemListType.VISUAL_ID))
		{
			buffer.writeInt(item.getVisualId()); // Item remodel visual ID
		}
		
		if (containsMask(mask, ItemListType.SOUL_CRYSTAL))
		{
			writeItemEnsoulOptions(item, buffer);
		}
		
		// TODO:
		// if (containsMask(mask, ItemListType.REUSE_DELAY))
		// {
		// final Player owner = item.getOwner();
		// buffer.writeInt(owner == null ? 0 : (int) (owner.getItemRemainingReuseTime(item.getObjectId()) / 1000));
		// }
		// Essence
		// if (containsMask(mask, ItemListType.BLESSED))
		// {
		// buffer.writeByte(1);
		// }
		
		if (containsMask(mask, ItemListType.VIRTUAL_ITEM))
		{
			buffer.writeByte(1); // The mark the client draws as system string 5247 on the tooltip.
		}
	}
	
	/**
	 * Calculates the bitmask for an {@code ItemInfo} object.<br>
	 * This method determines which item properties need to be sent based on the item's attributes.<br>
	 * It checks for augmentations, elemental types, enchantments, and visual IDs.
	 * @param item The {@code ItemInfo} object to evaluate.
	 * @return The calculated integer mask representing the active properties of the item.
	 */
	protected static int calculateMask(ItemInfo item)
	{
		int mask = 0;
		if (item.getAugmentation() != null)
		{
			mask |= ItemListType.AUGMENT_BONUS.getMask();
		}
		
		if ((item.getAttackElementType() >= 0) || (item.getAttributeDefence(AttributeType.FIRE) > 0) || (item.getAttributeDefence(AttributeType.WATER) > 0) || (item.getAttributeDefence(AttributeType.WIND) > 0) || (item.getAttributeDefence(AttributeType.EARTH) > 0) || (item.getAttributeDefence(AttributeType.HOLY) > 0) || (item.getAttributeDefence(AttributeType.DARK) > 0))
		{
			mask |= ItemListType.ELEMENTAL_ATTRIBUTE.getMask();
		}
		
		// 362 - Removed
		// if (item.getEnchantOptions() != null)
		// {
		// for (int id : item.getEnchantOptions())
		// {
		// if (id > 0)
		// {
		// mask |= ItemListType.ENCHANT_EFFECT.getMask();
		// break;
		// }
		// }
		// }
		
		if (item.getVisualId() > 0)
		{
			mask |= ItemListType.VISUAL_ID.getMask();
		}
		
		if (((item.getSoulCrystalOptions() != null) && !item.getSoulCrystalOptions().isEmpty()) || ((item.getSoulCrystalSpecialOptions() != null) && !item.getSoulCrystalSpecialOptions().isEmpty()))
		{
			mask |= ItemListType.SOUL_CRYSTAL.getMask();
		}
		
		// TODO:
		// if (item.getReuseDelay() > 0)
		// {
		// mask |= ItemListType.REUSE_DELAY.getMask();
		// }
		// Essence
		// if (item.isBlessed())
		// {
		// mask |= ItemListType.BLESSED.getMask();
		// }
		
		if (item.isVirtual())
		{
			mask |= ItemListType.VIRTUAL_ITEM.getMask();
		}
		
		return mask;
	}
	
	/**
	 * Writes the augmentation data for a specific item to the buffer.<br>
	 * It checks if the {@code ItemInfo} contains an augmentation.<br>
	 * If it does, it writes three integer IDs to the {@code WriteBuffer}.<br>
	 * Otherwise, it writes three zeros as integers.
	 * @param item The {@code ItemInfo} object containing the item data.
	 * @param buffer The {@code WriteBuffer} used to store the outgoing packet data.
	 */
	protected void writeItemAugment(ItemInfo item, WriteBuffer buffer)
	{
		if ((item != null) && (item.getAugmentation() != null))
		{
			buffer.writeInt(item.getAugmentation().getOption1Id());
			buffer.writeInt(item.getAugmentation().getOption2Id());
			buffer.writeInt(item.getAugmentation().getOption3Id());
		}
		else
		{
			buffer.writeInt(0);
			buffer.writeInt(0);
			buffer.writeInt(0);
		}
	}
	
	/**
	 * Writes the elemental and enchantment data for an item to the buffer.<br>
	 * This method calls {@code writeItemElemental} and {@code writeItemEnchantEffect}.
	 * @param item The {@code ItemInfo} object containing the item details.
	 * @param buffer The {@code WriteBuffer} used to store the serialized data.
	 */
	protected void writeItemElementalAndEnchant(ItemInfo item, WriteBuffer buffer)
	{
		writeItemElemental(item, buffer);
		writeItemEnchantEffect(item, buffer);
	}
	
	/**
	 * Writes the elemental properties of an item to the buffer.<br>
	 * This includes attack element type, power, and various defense attributes.<br>
	 * If the provided {@code ItemInfo} is {@code null}, it writes default values.
	 * @param item The {@code ItemInfo} object containing the item data.
	 * @param buffer The {@code WriteBuffer} used to store the serialized data.
	 */
	protected void writeItemElemental(ItemInfo item, WriteBuffer buffer)
	{
		if (item != null)
		{
			buffer.writeShort(item.getAttackElementType());
			buffer.writeShort(item.getAttackElementPower());
			buffer.writeShort(item.getAttributeDefence(AttributeType.FIRE));
			buffer.writeShort(item.getAttributeDefence(AttributeType.WATER));
			buffer.writeShort(item.getAttributeDefence(AttributeType.WIND));
			buffer.writeShort(item.getAttributeDefence(AttributeType.EARTH));
			buffer.writeShort(item.getAttributeDefence(AttributeType.HOLY));
			buffer.writeShort(item.getAttributeDefence(AttributeType.DARK));
		}
		else
		{
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
			buffer.writeShort(0);
		}
	}
	
	/**
	 * Writes the enchantment effects of an item to the buffer.<br>
	 * This method iterates through all available enchant options for the given {@code ItemInfo}.<br>
	 * Each option is written as a {@code short} value into the {@code WriteBuffer}.
	 * @param item The {@code ItemInfo} object containing the enchantment data.
	 * @param buffer The {@code WriteBuffer} used to store the outgoing packet data.
	 */
	protected void writeItemEnchantEffect(ItemInfo item, WriteBuffer buffer)
	{
		// Enchant Effects
		for (int op : item.getEnchantOptions())
		{
			buffer.writeInt(op);
		}
	}
	
	/**
	 * Writes the soul crystal options for a specific item into the buffer.<br>
	 * This method handles both regular and special soul crystal options.<br>
	 * If the provided {@code ItemInfo} is {@code null}, it writes zero sizes for both types.
	 * @param item The {@code ItemInfo} object containing the item data to write.
	 * @param buffer The {@code WriteBuffer} used to store the serialized data.
	 */
	protected void writeItemEnsoulOptions(ItemInfo item, WriteBuffer buffer)
	{
		if (item != null)
		{
			buffer.writeByte(item.getSoulCrystalOptions().size()); // Size of regular soul crystal options.
			for (EnsoulOption option : item.getSoulCrystalOptions())
			{
				buffer.writeInt(option.getId()); // Regular Soul Crystal Ability ID.
			}
			
			buffer.writeByte(item.getSoulCrystalSpecialOptions().size()); // Size of special soul crystal options.
			for (EnsoulOption option : item.getSoulCrystalSpecialOptions())
			{
				buffer.writeInt(option.getId()); // Special Soul Crystal Ability ID.
			}
		}
		else
		{
			buffer.writeByte(0); // Size of regular soul crystal options.
			buffer.writeByte(0); // Size of special soul crystal options.
		}
	}
	
	/**
	 * Writes the inventory block data to the provided buffer.<br>
	 * This method checks if the {@code PlayerInventory} contains any blocked items.<br>
	 * It writes the count, client ID, and item IDs into the {@code WriteBuffer}.
	 * @param inventory The {@code PlayerInventory} containing the items to write.
	 * @param buffer The {@code WriteBuffer} used to store the packet data.
	 */
	protected void writeInventoryBlock(PlayerInventory inventory, WriteBuffer buffer)
	{
		if (inventory.hasInventoryBlock())
		{
			buffer.writeShort(inventory.getBlockItems().size());
			buffer.writeByte(inventory.getBlockMode().getClientId());
			for (int id : inventory.getBlockItems())
			{
				buffer.writeInt(id);
			}
		}
		else
		{
			buffer.writeShort(0);
		}
	}
	
	/**
	 * Calculates the total byte size of a packet for a specific item.<br>
	 * This method determines the size based on the {@code ItemInfo} properties and its associated masks.<br>
	 * It accounts for standard fields like ID, quantity, and slot, as well as optional data like augments or soul crystals.
	 * @param item The {@code ItemInfo} object to calculate the size for.
	 * @return The total size of the packet in bytes.
	 */
	protected int calculatePacketSize(ItemInfo item)
	{
		final int mask = calculateMask(item);
		int size = 0;
		size += 2; // buffer.writeShort(mask);
		size += 4; // buffer.writeInt(item.getObjectId()); // ObjectId
		size += 4; // buffer.writeInt(item.getItem().getDisplayId()); // ItemId
		size += 1; // buffer.writeByte(item.getItem().isQuestItem() || (item.getEquipped() == 1) ? 0xFF : item.getLocation()); // T1
		size += 8; // buffer.writeLong(item.getCount()); // Quantity
		size += 1; // buffer.writeByte(item.getItem().getType2()); // Item Type 2 : 00-weapon, 01-shield/armor, 02-ring/earring/necklace, 03-questitem, 04-adena, 05-item
		size += 1; // buffer.writeByte(item.getCustomType1()); // Filler (always 0)
		size += 2; // buffer.writeShort(item.getEquipped()); // Equipped : 00-No, 01-yes
		size += 8; // buffer.writeLong(item.getItem().getBodyPart()); // Slot : 0006-lr.ear, 0008-neck, 0030-lr.finger, 0040-head, 0100-l.hand, 0200-gloves, 0400-chest, 0800-pants, 1000-feet, 4000-r.hand, 8000-r.hand
		size += 2; // buffer.writeShort(item.getEnchantLevel()); // Enchant level (pet level shown in control item).
		size += 4; // buffer.writeInt(item.getMana());
		size += 1; // buffer.writeByte(0); // 270 protocol
		size += 4; // buffer.writeInt(item.getTime());
		size += 1; // buffer.writeByte(item.isAvailable()); // GOD Item enabled = 1 disabled (red) = 0
		size += 2; // buffer.writeShort(0); // 140 - locked
		
		if (containsMask(mask, ItemListType.AUGMENT_BONUS))
		{
			size += 12; // writeItemAugment writes three ints.
		}
		
		if (containsMask(mask, ItemListType.ELEMENTAL_ATTRIBUTE))
		{
			size += 16;
		}
		
		// 362 - Removed
		// if (containsMask(mask, ItemListType.ENCHANT_EFFECT))
		// {
		// size += (item.getEnchantOptions().length * 4);
		// }
		
		if (containsMask(mask, ItemListType.VISUAL_ID))
		{
			size += 4;
		}
		
		if (containsMask(mask, ItemListType.SOUL_CRYSTAL))
		{
			size += 1;
			size += (item.getSoulCrystalOptions().size() * 4);
			size += 1;
			size += (item.getSoulCrystalSpecialOptions().size() * 4);
		}
		
		// TODO:
		// if (containsMask(mask, ItemListType.REUSE_DELAY))
		// {
		// size += 4;
		// }
		// Essence
		// if (containsMask(mask, ItemListType.BLESSED))
		// {
		// size += 1;
		// }
		
		if (containsMask(mask, ItemListType.VIRTUAL_ITEM))
		{
			size += 1;
		}
		
		return size;
	}
}
