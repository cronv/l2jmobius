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
package org.l2jmobius.gameserver.network.clientpackets.appearance;

import org.l2jmobius.gameserver.data.xml.AppearanceItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.ShapeShiftingItemRequest;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceStone;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceType;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemLocation;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.appearance.ExPutShapeShiftingExtractionItemResult;
import org.l2jmobius.gameserver.network.serverpackets.appearance.ExPutShapeShiftingTargetItemResult;

/**
 * Handles the client request to attempt placing an enchantment support item for shape-shifting.<br>
 * This packet processes the interaction between a {@code ShapeShiftingItemRequest} and its associated support items.
 * @author UnAfraid
 */
public class RequestExTryToPutShapeShiftingEnchantSupportItem extends ClientPacket
{
	private int _targetItemObjId;
	private int _extracItemObjId;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readInt();
		_extracItemObjId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final ShapeShiftingItemRequest request = player.getRequest(ShapeShiftingItemRequest.class);
		if ((request == null) || player.isInStoreMode() || player.isCrafting() || player.isProcessingRequest() || player.isProcessingTransaction())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_USE_THIS_SYSTEM_DURING_TRADING_PRIVATE_STORE_AND_WORKSHOP_SETUP);
			return;
		}
		
		final PlayerInventory inventory = player.getInventory();
		final Item targetItem = inventory.getItemByObjectId(_targetItemObjId);
		final Item extractItem = inventory.getItemByObjectId(_extracItemObjId);
		Item stone = request.getAppearanceStone();
		if ((targetItem == null) || (extractItem == null) || (stone == null))
		{
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		final int objectId = player.getObjectId();
		if ((stone.getOwnerId() != objectId) || (targetItem.getOwnerId() != objectId) || (extractItem.getOwnerId() != objectId))
		{
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if (!extractItem.getTemplate().isAppearanceable())
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
			player.sendPacket(ExPutShapeShiftingExtractionItemResult.FAILED);
			return;
		}
		
		final ItemLocation extractItemLocation = extractItem.getItemLocation();
		if ((extractItemLocation != ItemLocation.INVENTORY) && (extractItemLocation != ItemLocation.PAPERDOLL))
		{
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if ((stone = inventory.getItemByObjectId(stone.getObjectId())) == null)
		{
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		final AppearanceStone appearanceStone = AppearanceItemData.getInstance().getStone(stone.getId());
		if (appearanceStone == null)
		{
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if ((appearanceStone.getType() == AppearanceType.RESTORE) || (appearanceStone.getType() == AppearanceType.FIXED))
		{
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if (extractItem.getVisualId() > 0)
		{
			player.sendPacket(ExPutShapeShiftingExtractionItemResult.FAILED);
			player.sendPacket(SystemMessageId.YOU_CANNOT_EXTRACT_FROM_A_MODIFIED_ITEM);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if ((extractItemLocation != ItemLocation.INVENTORY) && (extractItemLocation != ItemLocation.PAPERDOLL))
		{
			player.sendPacket(ExPutShapeShiftingExtractionItemResult.FAILED);
			player.removeRequest(ShapeShiftingItemRequest.class);
			return;
		}
		
		if ((extractItem.getItemType() != targetItem.getItemType()) || (extractItem.getId() == targetItem.getId()) || (extractItem.getObjectId() == targetItem.getObjectId()))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
			player.sendPacket(ExPutShapeShiftingExtractionItemResult.FAILED);
			return;
		}
		
		final ItemTemplate extractTemplate = extractItem.getTemplate();
		final ItemTemplate targetTemplate = targetItem.getTemplate();
		final BodyPart extractBodyPart = extractTemplate.getBodyPart();
		final BodyPart targetBodyPart = targetTemplate.getBodyPart();
		if ((extractBodyPart != targetBodyPart) && ((extractBodyPart != BodyPart.FULL_ARMOR) || (targetBodyPart != BodyPart.CHEST)))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
			player.sendPacket(ExPutShapeShiftingExtractionItemResult.FAILED);
			return;
		}
		
		if (extractTemplate.getCrystalType().isGreater(targetTemplate.getCrystalType()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_EXTRACT_FROM_ITEMS_THAT_ARE_HIGHER_GRADE_THAN_ITEMS_TO_BE_MODIFIED);
			player.sendPacket(ExPutShapeShiftingExtractionItemResult.FAILED);
			return;
		}
		
		if (!appearanceStone.checkConditions(player, targetItem))
		{
			player.sendPacket(ExPutShapeShiftingTargetItemResult.FAILED);
			return;
		}
		
		request.setAppearanceExtractItem(extractItem);
		player.sendPacket(ExPutShapeShiftingExtractionItemResult.SUCCESS);
	}
}
