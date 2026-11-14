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
import org.l2jmobius.gameserver.network.serverpackets.ShowCalculator;

/**
 * Handles the logic for the calculator item.<br>
 * It processes interactions between players and the {@code Item} instance.<br>
 * This class manages sending the {@code ShowCalculator} packet to the client.
 * @author Zoey76
 */
public class Calculator implements IItemHandler
{
	/**
	 * Handles the logic when a {@code Playable} entity uses an {@code Item}.<br>
	 * It checks if the user is a player before opening the calculator.<br>
	 * If successful, it sends a {@code ShowCalculator} packet to the player.
	 * @param playable The entity attempting to use the item.
	 * @param item The specific item instance being used.
	 * @param forceUse Whether the use action is forced by the system.
	 * @return {@code true} if the item was successfully used, {@code false} otherwise.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		playable.sendPacket(new ShowCalculator(item.getId()));
		return true;
	}
}
