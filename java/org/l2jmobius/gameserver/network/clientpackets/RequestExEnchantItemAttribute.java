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
import org.l2jmobius.gameserver.data.holders.ElementalItemHolder;
import org.l2jmobius.gameserver.data.xml.ElementalAttributeData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.actor.request.EnchantItemAttributeRequest;
import org.l2jmobius.gameserver.entity.item.enchant.attribute.AttributeHolder;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAttributeEnchantResult;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the client request to enchant an item with a specific attribute.<br>
 * This packet processes the logic for applying extra attributes to items.<br>
 * It validates the request and sends back the result to the player.
 * @author Mobius
 */
public class RequestExEnchantItemAttribute extends ClientPacket
{
	private int _objectId;
	private long _count;
	
	@Override
	protected void readImpl()
	{
		_objectId = readInt();
		_count = readLong();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final EnchantItemAttributeRequest request = player.getRequest(EnchantItemAttributeRequest.class);
		if (request == null)
		{
			return;
		}
		
		request.setProcessing(true);
		
		if (_objectId == 0xFFFFFFFF)
		{
			// Player canceled enchant
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!player.isOnline())
		{
			player.removeRequest(request.getClass());
			return;
		}
		
		if (player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_ADD_ELEMENTAL_POWER_WHILE_OPERATING_A_PRIVATE_STORE_OR_PRIVATE_WORKSHOP);
			player.removeRequest(request.getClass());
			return;
		}
		
		// Restrict enchant during a trade (bug if enchant fails).
		if (player.getActiveRequester() != null)
		{
			// Cancel trade
			player.cancelActiveTrade();
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.YOU_CANNOT_DO_THAT_WHILE_TRADING);
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(_objectId);
		final Item stone = request.getEnchantingStone();
		if ((item == null) || (stone == null))
		{
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		// Quest Q10579_ContainingTheAttributePower check.
		final int stoneTemplateId = stone.getTemplate().getId();
		if ((item.getTemplate().getId() == 48168) && (stoneTemplateId != 48169))
		{
			player.removeRequest(request.getClass());
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			return;
		}
		
		if (!ElementalAttributeData.getInstance().isElementableWithStone(item, stoneTemplateId))
		{
			player.sendPacket(SystemMessageId.ELEMENTAL_POWER_ENHANCER_USAGE_REQUIREMENT_IS_NOT_SUFFICIENT);
			player.removeRequest(request.getClass());
			return;
		}
		
		if (_count < 1)
		{
			player.removeRequest(request.getClass());
			return;
		}
		
		switch (item.getItemLocation())
		{
			case INVENTORY:
			case PAPERDOLL:
			{
				if (item.getOwnerId() != player.getObjectId())
				{
					player.removeRequest(request.getClass());
					return;
				}
				break;
			}
			default:
			{
				player.removeRequest(request.getClass());
				PunishmentManager.handleIllegalPlayerAction(player, player + " tried to use enchant Exploit!", GeneralConfig.DEFAULT_PUNISH);
				return;
			}
		}
		
		final int stoneId = stone.getId();
		final long count = Math.min(stone.getCount(), _count);
		AttributeType elementToAdd = ElementalAttributeData.getInstance().getItemElement(stoneId);
		
		// Armors have the opposite element.
		final boolean itemIsArmor = item.isArmor();
		if (itemIsArmor)
		{
			elementToAdd = elementToAdd.getOpposite();
		}
		
		final AttributeHolder oldElement = item.getAttribute(elementToAdd);
		if ((item.isWeapon() && (oldElement != null) && (oldElement.getType() != elementToAdd) && (oldElement.getType() != AttributeType.NONE)) || (itemIsArmor && (item.getAttribute(elementToAdd) == null) && (item.getAttributes() != null) && (item.getAttributes().size() >= 3)))
		{
			player.sendPacket(SystemMessageId.ANOTHER_ELEMENTAL_POWER_HAS_ALREADY_BEEN_ADDED_THIS_ELEMENTAL_POWER_CANNOT_BE_ADDED);
			player.removeRequest(request.getClass());
			return;
		}
		
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		final AttributeType opositeElement = elementToAdd.getOpposite();
		final int limit = getLimit(item, stoneId);
		int powerToAdd = getPowerToAdd(stoneId, elementValue, item);
		if (itemIsArmor && (item.getAttributes() != null))
		{
			// Can't add opposite element.
			for (AttributeHolder attribute : item.getAttributes())
			{
				if (attribute.getType() == opositeElement)
				{
					player.removeRequest(request.getClass());
					PunishmentManager.handleIllegalPlayerAction(player, player + " tried to add oposite attribute to item!", GeneralConfig.DEFAULT_PUNISH);
					return;
				}
			}
		}
		
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		
		if (powerToAdd <= 0)
		{
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.removeRequest(request.getClass());
			return;
		}
		
		int usedStones = 0;
		int successfulAttempts = 0;
		int failedAttempts = 0;
		for (int i = 0; i < count; i++)
		{
			final int result = addElement(player, stone, item, elementToAdd);
			if (result == 1)
			{
				usedStones++;
				successfulAttempts++;
			}
			else if (result == 0)
			{
				usedStones++;
				failedAttempts++;
			}
			else
			{
				break;
			}
		}
		
		item.updateItemElementals();
		player.destroyItem(ItemProcessType.FEE, stone, usedStones, player, true);
		final AttributeHolder newElement = item.getAttribute(elementToAdd);
		final int newValue = newElement != null ? newElement.getValue() : 0;
		final AttributeType realElement = itemIsArmor ? opositeElement : elementToAdd;
		final InventoryUpdate iu = new InventoryUpdate();
		if (successfulAttempts > 0)
		{
			SystemMessage sm;
			final int enchantLevel = item.getEnchantLevel();
			if (enchantLevel == 0)
			{
				if (itemIsArmor)
				{
					sm = new SystemMessage(SystemMessageId.S1_ARE_NOW_IMBUED_WITH_S2_ATTRIBUTE_AND_YOUR_S3_RESISTANCE_HAS_INCREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S2_ATTRIBUTE_HAS_BEEN_ADDED_TO_S1);
				}
				
				sm.addItemName(item);
				sm.addAttribute(realElement.getClientId());
				if (itemIsArmor)
				{
					sm.addAttribute(realElement.getOpposite().getClientId());
				}
			}
			else
			{
				if (itemIsArmor)
				{
					sm = new SystemMessage(SystemMessageId.S3_POWER_HAS_BEEN_ADDED_TO_S1_S2_S4_RESISTANCE_IS_INCREASED);
				}
				else
				{
					sm = new SystemMessage(SystemMessageId.S1_S2_S3_ATTRIBUTE_POWER_IS_ENABLED);
				}
				
				sm.addInt(enchantLevel);
				sm.addItemName(item);
				sm.addAttribute(realElement.getClientId());
				if (itemIsArmor)
				{
					sm.addAttribute(realElement.getOpposite().getClientId());
				}
			}
			
			player.sendPacket(sm);
			
			// send packets
			iu.addModifiedItem(item);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_FAILED_TO_ADD_ELEMENTAL_POWER);
		}
		
		int result = 0;
		if (successfulAttempts == 0)
		{
			// Failed
			result = 2;
		}
		
		// Stone must be removed.
		if (stone.getCount() == 0)
		{
			iu.addRemovedItem(stone);
		}
		else
		{
			iu.addModifiedItem(stone);
		}
		
		player.removeRequest(request.getClass());
		player.sendPacket(new ExAttributeEnchantResult(result, item.isWeapon(), elementToAdd, elementValue, newValue, successfulAttempts, failedAttempts));
		player.updateUserInfo();
		player.sendInventoryUpdate(iu);
	}
	
	/**
	 * Adds an elemental attribute to a specific item.<br>
	 * This method calculates the new power value based on the stone used.<br>
	 * It checks if the addition is successful and updates the item attributes accordingly.
	 * @param player The {@code Player} who is performing the action.
	 * @param stone The {@code Item} being used as an enchantment stone.
	 * @param item The target {@code Item} to receive the attribute.
	 * @param elementToAdd The {@code AttributeType} of the element to add.
	 * @return Returns {@code 1} if the operation succeeded, {@code 0} if it failed, or {@code -1} if the action was cancelled.
	 */
	private int addElement(Player player, Item stone, Item item, AttributeType elementToAdd)
	{
		final int stoneId = stone.getId();
		final AttributeHolder oldElement = item.getAttribute(elementToAdd);
		final int elementValue = oldElement == null ? 0 : oldElement.getValue();
		final int limit = getLimit(item, stoneId);
		int powerToAdd = getPowerToAdd(stoneId, elementValue, item);
		int newPower = elementValue + powerToAdd;
		if (newPower > limit)
		{
			newPower = limit;
			powerToAdd = limit - elementValue;
		}
		
		if (powerToAdd <= 0)
		{
			player.sendPacket(SystemMessageId.ATTRIBUTE_ITEM_USAGE_HAS_BEEN_CANCELLED);
			player.removeRequest(EnchantItemAttributeRequest.class);
			return -1;
		}
		
		final boolean success = ElementalAttributeData.getInstance().isSuccess(item, stoneId);
		if (success)
		{
			item.setAttribute(new AttributeHolder(elementToAdd, newPower), false);
		}
		
		return success ? 1 : 0;
	}
	
	/**
	 * Calculates the maximum limit for an elemental attribute.<br>
	 * This value depends on the item type and the specific stone used.
	 * @param item The {@code Item} being modified.
	 * @param sotneId The unique identifier for the stone.
	 * @return The calculated limit as an {@code int}.
	 */
	public int getLimit(Item item, int sotneId)
	{
		final ElementalItemHolder elementItem = ElementalAttributeData.getInstance().getItemElemental(sotneId);
		if (elementItem == null)
		{
			return 0;
		}
		
		if (item.isWeapon())
		{
			return ElementalAttributeData.WEAPON_VALUES[elementItem.getType().getMaxLevel()];
		}
		
		return ElementalAttributeData.ARMOR_VALUES[elementItem.getType().getMaxLevel()];
	}
	
	/**
	 * Calculates the power value to add based on a stone and an item.<br>
	 * It checks for elemental data associated with the {@code stoneId}.<br>
	 * If no specific power is found, it returns bonuses based on the item type.
	 * @param stoneId The unique identifier of the stone.
	 * @param oldValue The current value of the attribute.
	 * @param item The {@code Item} being modified.
	 * @return The calculated power to add as an {@code int}.
	 */
	public int getPowerToAdd(int stoneId, int oldValue, Item item)
	{
		if (ElementalAttributeData.getInstance().getItemElement(stoneId) != AttributeType.NONE)
		{
			final int elementalPower = ElementalAttributeData.getInstance().getItemElemental(stoneId).getPower();
			if (elementalPower > 0)
			{
				return elementalPower;
			}
			
			if (item.isWeapon())
			{
				if (oldValue == 0)
				{
					return ElementalAttributeData.FIRST_WEAPON_BONUS;
				}
				
				return ElementalAttributeData.NEXT_WEAPON_BONUS;
			}
			else if (item.isArmor())
			{
				return ElementalAttributeData.ARMOR_BONUS;
			}
		}
		
		return 0;
	}
}
