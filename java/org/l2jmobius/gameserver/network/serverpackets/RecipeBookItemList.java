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

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.RecipeBook;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to provide a list of items for a specific recipe.<br>
 * It contains the data required to display the necessary components in the {@code RecipeBook}.
 */
public class RecipeBookItemList extends ServerPacket
{
	private final Collection<RecipeHolder> _recipes;
	private final boolean _isDwarvenCraft;
	private final int _maxMp;
	
	/**
	 * Creates a new {@code RecipeBookItemList} for a specific player.<br>
	 * This constructor determines which recipe book to use based on the craft type.<br>
	 * It also stores the maximum MP of the player.
	 * @param player The {@code Player} object whose data will be used.
	 * @param isDwarvenCraft Set to {@code true} for Dwarven recipes, or {@code false} for common recipes.
	 */
	public RecipeBookItemList(Player player, boolean isDwarvenCraft)
	{
		_isDwarvenCraft = isDwarvenCraft;
		_maxMp = player.getMaxMp();
		final RecipeBook recipeBook = player.getRecipeBook();
		_recipes = (isDwarvenCraft ? recipeBook.getDwarven() : recipeBook.getCommon());
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.RECIPE_BOOK_ITEM_LIST.writeId(this, buffer);
		buffer.writeInt(!_isDwarvenCraft); // 0 = Dwarven - 1 = Common
		buffer.writeInt(_maxMp);
		if (_recipes == null)
		{
			buffer.writeInt(0);
		}
		else
		{
			buffer.writeInt(_recipes.size()); // number of items in recipe book
			int count = 1;
			for (RecipeHolder recipe : _recipes)
			{
				buffer.writeInt(recipe.getId());
				buffer.writeInt(count++);
			}
		}
	}
}
