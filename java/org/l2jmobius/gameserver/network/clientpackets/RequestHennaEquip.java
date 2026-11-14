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

import java.util.Set;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.HennaList;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.HennaEquipList;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;

/**
 * Handles the client request to equip a specific {@code Henna} item.<br>
 * This packet triggers the logic for applying henna effects to the player.<br>
 * It validates the request and sends back an {@code InventoryUpdate} or {@code ActionFailed} response.
 * @author Mobius
 */
public class RequestHennaEquip extends ClientPacket
{
	// Symbol makers served by the SymbolMaker script.
	private static final Set<Integer> SYMBOL_MAKERS = Set.of(31046, 31047, 31048, 31049, 31050, 31051, 31052, 31053, 31264, 31308, 31953);
	
	private int _symbolId;
	
	@Override
	protected void readImpl()
	{
		_symbolId = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (!getClient().getFloodProtectors().canPerformTransaction())
		{
			return;
		}
		
		if (!isAtSymbolMaker(player))
		{
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final Henna henna = HennaData.getInstance().getHenna(_symbolId);
		if (henna == null)
		{
			PacketLogger.warning("Invalid Henna Id: " + _symbolId + " from " + player);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final HennaList hennas = player.getHennaList();
		if (henna.isPremium())
		{
			if ((PremiumSystemConfig.PREMIUM_HENNA_SLOT_ENABLED_FOR_ALL || player.hasPremiumStatus()) && PremiumSystemConfig.PREMIUM_HENNA_SLOT_ENABLED && (player.getPlayerClass().level() > 1))
			{
				if (hennas.get(4) != null)
				{
					player.sendPacket(SystemMessageId.YOU_HAVE_NO_FREE_TATTOO_SLOTS);
					player.sendPacket(ActionFailed.STATIC_PACKET);
					return;
				}
			}
			else
			{
				player.sendPacket(ActionFailed.STATIC_PACKET);
				return;
			}
		}
		else if (hennas.getEmptySlots() == 0)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_NO_FREE_TATTOO_SLOTS);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final long count = player.getInventory().getInventoryItemCount(henna.getDyeItemId(), -1);
		if (henna.isAllowedClass(player.getPlayerClass()) && (count >= henna.getWearCount()) && (player.getAdena() >= henna.getWearFee()) && hennas.add(henna))
		{
			player.destroyItemByItemId(ItemProcessType.FEE, henna.getDyeItemId(), henna.getWearCount(), player, true);
			
			// Keep the item reference, paying the exact amount destroys the stack.
			final Item adenaItem = player.getInventory().getAdenaInstance();
			player.getInventory().reduceAdena(ItemProcessType.FEE, henna.getWearFee(), player, player.getLastFolkNPC());
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItem(adenaItem);
			player.sendInventoryUpdate(iu);
			player.sendPacket(new HennaEquipList(player));
			player.updateSymbolSealSkills();
			player.sendPacket(SystemMessageId.A_TATTOO_IS_ADDED);
		}
		else
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_MAKE_A_TATTOO);
			if (!player.isGM() && !henna.isAllowedClass(player.getPlayerClass()))
			{
				PunishmentManager.handleIllegalPlayerAction(player, "Exploit attempt: Character " + player.getName() + " of account " + player.getAccountName() + " tryed to add a forbidden henna.", GeneralConfig.DEFAULT_PUNISH);
			}
			
			player.sendPacket(ActionFailed.STATIC_PACKET);
		}
	}
	
	/**
	 * Checks that the player talks to a symbol maker in reach and is not in the Olympiad, so a crafted packet cannot draw or remove symbols anywhere.
	 * @param player The {@code Player} sending the henna packet.
	 * @return {@code true} if the henna request may be handled.
	 */
	static boolean isAtSymbolMaker(Player player)
	{
		if (player.isInOlympiadMode())
		{
			return false;
		}
		
		final Npc npc = player.getLastFolkNPC();
		return (npc != null) && SYMBOL_MAKERS.contains(npc.getId()) && (npc.canInteract(player) || player.isGM());
	}
}
