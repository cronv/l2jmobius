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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory.ItemSkillsListener;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.network.serverpackets.ExRepairAllEquipment;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * @author Galagard
 */
public class ExRequestRepairAllEquipment extends ClientPacket
{
	private static final int REPAIR_FEE_ITEM_ID = 3031; // Spirit Ore.
	private static final long REPAIR_WEAPON_FEE_COUNT = 100;
	
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerInventory inventory = player.getInventory();
		final int damagedWeapons = inventory.getDamagedWeaponsCount();
		final int damagedArmor = inventory.getDamagedArmorCount();
		
		if ((damagedWeapons == 0) && (damagedArmor == 0))
		{
			return;
		}
		
		long repairArmorFeeCount = 0;
		if (damagedArmor == 1)
		{
			repairArmorFeeCount = 3;
		}
		else if (damagedArmor == 2)
		{
			repairArmorFeeCount = 6;
		}
		else if (damagedArmor == 3)
		{
			repairArmorFeeCount = 13;
		}
		else if (damagedArmor == 4)
		{
			repairArmorFeeCount = 20;
		}
		else if (damagedArmor >= 5)
		{
			repairArmorFeeCount = 30;
		}
		
		final long repairAllFeeCount = (damagedWeapons * REPAIR_WEAPON_FEE_COUNT) + repairArmorFeeCount;
		
		if (!player.destroyItemByItemId(ItemProcessType.FEE, REPAIR_FEE_ITEM_ID, repairAllFeeCount, null, true))
		{
			player.sendPacket(new ExRepairAllEquipment(0)); // Fail.
			return;
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		
		for (Item item : inventory.getItems())
		{
			if (item.isDamaged())
			{
				final boolean equipped = item.isEquipped();
				if (equipped)
				{
					ItemSkillsListener.getInstance().notifyUnequiped(item.getLocationSlot(), item, inventory);
				}
				
				item.setDamaged(false);
				
				if (equipped)
				{
					ItemSkillsListener.getInstance().notifyEquiped(item.getLocationSlot(), item, inventory);
				}
				
				iu.addModifiedItem(item);
			}
		}
		
		player.getStat().recalculateStats(true);
		player.sendInventoryUpdate(iu);
		player.broadcastUserInfo();
		player.sendPacket(new ExRepairAllEquipment(1)); // Success.
	}
}
