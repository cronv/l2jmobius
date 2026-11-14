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

import org.l2jmobius.gameserver.data.xml.AugmentationData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.request.AugmentationRequest;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.options.Augmentation;
import org.l2jmobius.gameserver.mechanics.options.AugmentationFee;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.augmentation.ExVariationResult;

/**
 * Handles the client request to refine an item.<br>
 * This packet initiates the refinement process for a specific {@code Item}.<br>
 * It is used by the client to communicate with the server when a player attempts to upgrade gear.
 * @author -Wooden-, Index
 */
public class RequestRefine extends AbstractRefinePacket
{
	private int _targetItemObjId;
	private int _mineralItemObjId;
	
	@Override
	protected void readImpl()
	{
		_targetItemObjId = readInt();
		_mineralItemObjId = readInt();
		readByte(); // is event
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item targetItem = player.getInventory().getItemByObjectId(_targetItemObjId);
		if (targetItem == null)
		{
			return;
		}
		
		final Item mineralItem = player.getInventory().getItemByObjectId(_mineralItemObjId);
		if (mineralItem == null)
		{
			return;
		}
		
		final AugmentationFee fee = AugmentationData.getInstance().getFee(targetItem.getId(), mineralItem.getId());
		if (fee == null)
		{
			return;
		}
		
		final Item feeItem = player.getInventory().getItemByItemId(fee.getItemId());
		if ((feeItem == null) && (fee.getItemId() != 0))
		{
			PacketLogger.warning(getClass().getSimpleName() + ": " + player.getName() + " does not have required fee item (ID: " + fee.getItemId() + ") for mineral ID: " + mineralItem.getId());
			player.sendPacket(ExVariationResult.FAIL);
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		if (!isValid(player, targetItem, mineralItem, feeItem, fee))
		{
			player.sendPacket(ExVariationResult.FAIL);
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		if (fee.getAdenaFee() <= 0)
		{
			player.sendPacket(ExVariationResult.FAIL);
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		final long adenaFee = fee.getAdenaFee();
		if ((adenaFee > 0) && (player.getAdena() < adenaFee))
		{
			player.sendPacket(ExVariationResult.FAIL);
			player.sendPacket(SystemMessageId.AUGMENTATION_FAILED_DUE_TO_INAPPROPRIATE_CONDITIONS);
			return;
		}
		
		final Augmentation augmentation = AugmentationData.getInstance().getAugmentation(mineralItem.getId(), targetItem);
		if (augmentation == null)
		{
			player.sendPacket(ExVariationResult.FAIL);
			return;
		}
		
		AugmentationInstance augment = AugmentationData.getInstance().generateRandomAugmentation(augmentation, targetItem);
		if (augment == null)
		{
			player.sendPacket(ExVariationResult.FAIL);
			return;
		}
		
		final int option1 = augment.getOption1Id();
		final int option2 = augment.getOption2Id();
		final int option3 = augment.getOption3Id();
		
		final AugmentationInstance oldAugment = targetItem.getAugmentation();
		if (oldAugment != null)
		{
			final int newOption1;
			final int newOption2;
			final int newOption3;
			
			if (targetItem.getTemplate().getBodyPart() == BodyPart.BACK)
			{
				// Preserve the existing slot if the new mineral didn't provide a value for it.
				newOption1 = (option1 > 0) ? option1 : oldAugment.getOption1Id();
				newOption2 = (option2 > 0) ? option2 : oldAugment.getOption2Id();
				newOption3 = 0; // Cloaks never use option3.
			}
			else
			{
				// Non-cloaks: no fallback to old options, replace entirely.
				newOption1 = (option1 > 0) ? option1 : 0;
				newOption2 = (option2 > 0) ? option2 : 0;
				newOption3 = (option3 > 0) ? option3 : 0;
			}
			
			augment = new AugmentationInstance(augment.getMineralId(), newOption1, newOption2, newOption3);
		}
		
		player.sendPacket(new ExVariationResult(augment.getOption1Id(), augment.getOption2Id(), augment.getOption3Id(), true));
		
		// Consume the life stone.
		player.destroyItem(ItemProcessType.FEE, mineralItem, 1, null, false);
		
		// Consume the gemstones.
		if (feeItem != null)
		{
			player.destroyItem(ItemProcessType.FEE, feeItem, fee.getItemCount(), null, false);
		}
		
		// Consume Adena.
		if (adenaFee > 0)
		{
			player.reduceAdena(ItemProcessType.FEE, adenaFee, player, false);
		}
		
		// Request (continues at ExApplyVariationOption).
		if (player.getRequest(AugmentationRequest.class) == null)
		{
			player.addRequest(new AugmentationRequest(player));
		}
		
		final AugmentationRequest request = player.getRequest(AugmentationRequest.class);
		request.setAugmentedItem(_targetItemObjId);
		request.setAugment(augment);
	}
}
