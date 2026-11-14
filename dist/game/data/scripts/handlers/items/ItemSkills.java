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
package handlers.items;

import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class handles the logic for items that grant specific skills.<br>
 * It ensures that item skills are disabled during {@code Olympiad} events.
 */
public class ItemSkills extends ItemSkillsTemplate
{
	/**
	 * Checks if a {@code Playable} entity can use a specific {@code Item}.<br>
	 * This method prevents players from using items during an Olympiad match.<br>
	 * If the action is blocked, it sends a system message to the player.
	 * @param playable The {@code Playable} entity attempting to use the item.
	 * @param item The {@code Item} instance being used.
	 * @param forceUse A boolean indicating if the use should be forced.
	 * @return {@code true} if the item was successfully used, or {@code false} otherwise.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		final Player player = playable.asPlayer();
		if ((player != null) && player.isInOlympiadMode() && item.isOlyRestrictedItem())
		{
			player.sendPacket(SystemMessageId.THE_ITEM_CANNOT_BE_USED_IN_THE_OLYMPIAD);
			return false;
		}
		
		return super.onItemUse(playable, item, forceUse);
	}
}
