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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.RecipeBookItemList;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code OpenCommonRecipeBook} effect.<br>
 * It allows a player to view common recipes within the game world.
 * @author Adry_85, Mobius
 */
public class OpenCommonRecipeBook extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code OpenCommonRecipeBook} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the necessary parameters for this effect.
	 */
	public OpenCommonRecipeBook(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect happens instantly.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for opening a recipe book.<br>
	 * This method checks if the {@code effector} is a {@code Player}.<br>
	 * It ensures the player is not currently engaged in a trade.<br>
	 * If valid, it requests to open the recipe book.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player casterPlayer = effector.asPlayer();
		if (casterPlayer == null)
		{
			return;
		}
		
		if (casterPlayer.getPrivateStoreType() == PrivateStoreType.MANUFACTURE)
		{
			casterPlayer.sendPacket(SystemMessageId.YOU_MAY_NOT_ALTER_YOUR_RECIPE_BOOK_WHILE_ENGAGED_IN_MANUFACTURING);
			return;
		}
		
		if (casterPlayer.isProcessingTransaction())
		{
			casterPlayer.sendPacket(SystemMessageId.ITEM_CREATION_IS_NOT_POSSIBLE_WHILE_ENGAGED_IN_A_TRADE);
			return;
		}
		
		casterPlayer.sendPacket(new RecipeBookItemList(casterPlayer, false));
	}
}
