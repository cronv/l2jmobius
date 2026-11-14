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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.buylist.ProductList;
import org.l2jmobius.gameserver.mechanics.siege.CastleTaxType;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExBuySellList;

/**
 * Represents a merchant NPC that allows players to trade items.<br>
 * This class handles the logic for buying and selling products from a {@code ProductList}.
 */
public class Merchant extends Folk
{
	/**
	 * Creates a new {@code Merchant} instance.<br>
	 * This constructor initializes the merchant using a provided template.<br>
	 * It also sets the {@code InstanceType} to {@code Merchant}.
	 * @param template The {@code NpcTemplate} used to create this merchant.
	 */
	public Merchant(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Merchant);
	}
	
	/**
	 * Checks if the merchant can be attacked by a specific creature.<br>
	 * Monsters are always allowed to attack this merchant.<br>
	 * Other creatures follow the default rules of the parent class.
	 * @param attacker The {@code Creature} attempting to perform an attack.
	 * @return {@code true} if the attack is permitted, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		if (attacker.isMonster())
		{
			return true;
		}
		
		return super.isAutoAttackable(attacker);
	}
	
	/**
	 * Retrieves the file path for a merchant's HTML template.<br>
	 * The path is constructed based on the NPC ID and an optional value.
	 * @param npcId The unique identifier of the NPC.
	 * @param value An additional numeric value used to differentiate templates.
	 * @param player The {@code Player} object interacting with the merchant.
	 * @return A {@code String} representing the full path to the .htm file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom;
		if (value == 0)
		{
			pom = Integer.toString(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		return "data/html/merchant/" + pom + ".htm";
	}
	
	/**
	 * Opens the purchase window for a specific {@code Player}.<br>
	 * This method displays the items available for sale.<br>
	 * It uses the provided {@code value} to determine which list to show.
	 * @param player The {@code Player} who will see the window.
	 * @param value The ID used to identify the specific buy list.
	 */
	public void showBuyWindow(Player player, int value)
	{
		showBuyWindow(player, value, true);
	}
	
	/**
	 * Opens the purchase window for a specific {@code Player}.<br>
	 * It retrieves the correct product list based on the provided {@code value}.<br>
	 * The method also handles whether to apply a castle tax.
	 * @param player The {@code Player} who will view the shop window.
	 * @param value The unique identifier for the buy list to display.
	 * @param applyCastleTax Set to {@code true} to include taxes, or {@code false} to ignore them.
	 */
	public void showBuyWindow(Player player, int value, boolean applyCastleTax)
	{
		final ProductList buyList = BuyListData.getInstance().getBuyList(value);
		if (buyList == null)
		{
			LOGGER.warning("BuyList not found! BuyListId:" + value);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		final int id = getId();
		if (!buyList.isNpcAllowed(id))
		{
			LOGGER.warning("Npc not allowed in BuyList! BuyListId:" + value + " NpcId:" + id);
			player.sendPacket(ActionFailed.STATIC_PACKET);
			return;
		}
		
		player.setInventoryBlockingStatus(true);
		
		player.sendPacket(new ExBuySellList(buyList, player, (applyCastleTax) ? getCastleTaxRate(CastleTaxType.BUY) : 0));
		player.sendPacket(new ExBuySellList(player, false));
		player.sendPacket(new ExBuySellList(getCastle(), applyCastleTax));
	}
}
