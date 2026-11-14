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

import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ShowXMasSeal;

/**
 * Handles special logic for Christmas-themed items.<br>
 * This class manages unique behaviors triggered by specific holiday items in the game.
 * @author devScarlet, mrTJO
 */
public class SpecialXMas implements IItemHandler
{
	/**
	 * Handles the logic when a {@code Playable} entity uses an {@code Item}.<br>
	 * It checks if the user is a player before processing.<br>
	 * If successful, it sends a special visual effect to the player.
	 * @param playable The entity attempting to use the item.
	 * @param item The specific instance of the item being used.
	 * @param forceUse A flag indicating if the use is forced.
	 * @return {@code true} if the action was successful, otherwise {@code false}.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		playable.sendPacket(new ShowXMasSeal(item.getId()));
		return true;
	}
}
