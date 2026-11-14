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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet provides information about an item available in a recipe shop.<br>
 * It is sent to the client to display specific details for crafting recipes.
 * @author Mobius
 */
public class RecipeShopItemInfo extends ServerPacket
{
	private final Player _manufacturer;
	private final int _recipeId;
	private final Boolean _success;
	private final long _manufacturePrice;
	private final long _offeringMaximumAdena;
	private final double _craftRate;
	private final double _craftCritical;
	
	/**
	 * Creates a new {@code RecipeShopItemInfo} packet.<br>
	 * This constructor initializes the data for an item manufactured by a player.
	 * @param manufacturer The {@code Player} who created the item.
	 * @param recipeId The unique identifier for the crafting recipe.
	 * @param success Whether the manufacturing process was successful.
	 * @param manufacturePrice The cost required to manufacture the item.
	 * @param offeringMaximumAdena The maximum amount of adena offered for the item.
	 */
	public RecipeShopItemInfo(Player manufacturer, int recipeId, boolean success, long manufacturePrice, long offeringMaximumAdena)
	{
		_manufacturer = manufacturer;
		_recipeId = recipeId;
		_success = success;
		_manufacturePrice = manufacturePrice;
		_offeringMaximumAdena = offeringMaximumAdena;
		_craftRate = _manufacturer.getStat().getValue(Stat.CRAFT_RATE, 0);
		_craftCritical = _manufacturer.getStat().getValue(Stat.CRAFTING_CRITICAL, 0);
	}
	
	/**
	 * Creates a new {@code RecipeShopItemInfo} packet.<br>
	 * This constructor is used when the success status is not applicable.
	 * @param manufacturer The {@code Player} who created the item.
	 * @param recipeId The unique identifier for the recipe.
	 * @param manufacturePrice The cost required to manufacture the item.
	 * @param offeringMaximumAdena The maximum amount of adena offered for the item.
	 */
	public RecipeShopItemInfo(Player manufacturer, int recipeId, long manufacturePrice, long offeringMaximumAdena)
	{
		_manufacturer = manufacturer;
		_recipeId = recipeId;
		_success = null;
		_manufacturePrice = manufacturePrice;
		_offeringMaximumAdena = offeringMaximumAdena;
		_craftRate = _manufacturer.getStat().getValue(Stat.CRAFT_RATE, 0);
		_craftCritical = _manufacturer.getStat().getValue(Stat.CRAFTING_CRITICAL, 0);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.RECIPE_SHOP_ITEM_INFO.writeId(this, buffer);
		buffer.writeInt(_manufacturer.getObjectId());
		buffer.writeInt(_recipeId);
		buffer.writeInt((int) _manufacturer.getCurrentMp());
		buffer.writeInt(_manufacturer.getMaxMp());
		buffer.writeInt(_success == null ? -1 : (_success ? 1 : 0)); // item creation none/success/failed
		buffer.writeLong(_manufacturePrice);
		buffer.writeByte(_offeringMaximumAdena > 0); // Trigger offering window if 1
		buffer.writeLong(_offeringMaximumAdena);
		buffer.writeDouble(Math.min(_craftRate, 100.0));
		buffer.writeByte(_craftCritical > 0);
		buffer.writeDouble(Math.min(_craftCritical, 100.0));
		buffer.writeByte(0); // find me
	}
}
