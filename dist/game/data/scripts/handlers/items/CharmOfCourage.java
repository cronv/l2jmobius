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
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the logic for the {@code CharmOfCourage} item.<br>
 * This class manages what happens when a player interacts with or uses this specific item.
 * @author Zealar, Mobius
 */
public class CharmOfCourage implements IItemHandler
{
	private static final long DURATION = 1200000; // 20 minutes (Kamael notes).
	
	/**
	 * Handles the logic for using a Charm of Courage item.<br>
	 * Checks if the player meets the required level to use the item.<br>
	 * Consumes the item and activates the charm effect if successful.
	 * @param playable The {@code Playable} entity attempting to use the item.
	 * @param item The {@code Item} instance being used.
	 * @param forceUse A boolean indicating if the use is forced.
	 * @return {@code true} if the item was successfully used, {@code false} otherwise.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}
		
		final Player player = playable.asPlayer();
		int level = player.getLevel();
		final int itemLevel = item.getTemplate().getCrystalType().getLevel();
		if (level < 20)
		{
			level = 0;
		}
		else if (level < 40)
		{
			level = 1;
		}
		else if (level < 52)
		{
			level = 2;
		}
		else if (level < 61)
		{
			level = 3;
		}
		else if (level < 76)
		{
			level = 4;
		}
		else
		{
			level = 5;
		}
		
		if (itemLevel < level)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addItemName(item.getId());
			player.sendPacket(sm);
			return false;
		}
		
		if (player.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, null, false))
		{
			player.startCharmOfCourage(DURATION);
			player.sendPacket(new EtcStatusUpdate(player));
			return true;
		}
		
		return false;
	}
}
