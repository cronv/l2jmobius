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
package handlers.items;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.holders.RecipeHolder;
import org.l2jmobius.gameserver.data.xml.RecipeData;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.RecipeBook;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles the logic for crafting items using recipes.<br>
 * It manages how {@code Item} objects are processed based on defined {@code RecipeData}.<br>
 * It ensures that players can correctly combine materials to create new items.
 * @author Zoey76
 */
public class Recipes implements IItemHandler
{
	/**
	 * Handles the logic for using an item to register a new recipe.<br>
	 * Checks if the player meets all requirements such as crafting level and limits.<br>
	 * Destroys the item upon successful registration.
	 * @param playable The {@code Playable} entity attempting to use the item.
	 * @param item The {@code Item} being used for registration.
	 * @param forceUse A boolean indicating if the action should be forced.
	 * @return {@code true} if the recipe was successfully registered, {@code false} otherwise.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		if (!PlayerConfig.IS_CRAFTING_ENABLED)
		{
			playable.sendMessage("Crafting is disabled, you cannot register this recipe.");
			return false;
		}
		
		final Player player = playable.asPlayer();
		if (player.isCrafting())
		{
			player.sendPacket(SystemMessageId.YOU_MAY_NOT_ALTER_YOUR_RECIPE_BOOK_WHILE_ENGAGED_IN_MANUFACTURING);
			return false;
		}
		
		final RecipeHolder rp = RecipeData.getInstance().getRecipeByRecipeItemId(item.getId());
		if (rp == null)
		{
			player.sendPacket(SystemMessageId.THE_RECIPE_IS_INCORRECT);
			return false;
		}
		
		final RecipeBook recipeBook = player.getRecipeBook();
		if (recipeBook.has(rp.getId()))
		{
			player.sendPacket(SystemMessageId.THAT_RECIPE_IS_ALREADY_REGISTERED);
			return false;
		}
		
		boolean canCraft = false;
		boolean recipeLevel = false;
		boolean recipeLimit = false;
		if (rp.isDwarvenRecipe())
		{
			final int createItemLevel = player.getCreateItemLevel();
			canCraft = createItemLevel > 0;
			recipeLevel = (rp.getLevel() > createItemLevel);
			recipeLimit = (recipeBook.getDwarven().size() >= recipeBook.getDwarfLimit());
		}
		else
		{
			final int createCommonItemLevel = player.getCreateCommonItemLevel();
			canCraft = createCommonItemLevel > 0;
			recipeLevel = (rp.getLevel() > createCommonItemLevel);
			recipeLimit = (recipeBook.getCommon().size() >= recipeBook.getCommonLimit());
		}
		
		if (!canCraft)
		{
			player.sendPacket(SystemMessageId.THE_RECIPE_CANNOT_BE_REGISTERED_YOU_DO_NOT_HAVE_THE_ABILITY_TO_CREATE_ITEMS);
			return false;
		}
		
		if (recipeLevel)
		{
			player.sendPacket(SystemMessageId.THE_LEVEL_OF_CREATE_ITEM_IS_TOO_LOW_FOR_REGISTERING_THE_RECIPE);
			return false;
		}
		
		if (recipeLimit)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.UP_TO_S1_RECIPES_CAN_BE_REGISTERED);
			sm.addInt(rp.isDwarvenRecipe() ? recipeBook.getDwarfLimit() : recipeBook.getCommonLimit());
			player.sendPacket(sm);
			return false;
		}
		
		if (rp.isDwarvenRecipe())
		{
			recipeBook.registerDwarven(rp, true);
		}
		else
		{
			recipeBook.registerCommon(rp, true);
		}
		
		player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, null, false);
		final SystemMessage sm = new SystemMessage(SystemMessageId.S1_ADDED);
		sm.addItemName(item);
		player.sendPacket(sm);
		return true;
	}
}
