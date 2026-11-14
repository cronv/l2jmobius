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

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemTalk;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the logic for processing bypass commands from items.<br>
 * This class allows players to skip specific dialogues or actions when interacting with an {@code Item}.<br>
 * It ensures that the game flow continues correctly when a user chooses to ignore certain prompts.
 * @author JIV
 */
public class Bypass implements IItemHandler
{
	/**
	 * Handles the logic when a {@code Playable} entity uses an {@code Item}.<br>
	 * This method checks if the user is a {@code Player}.<br>
	 * It sends a specific HTML message based on the item's ID.
	 * @param playable The entity attempting to use the item.
	 * @param item The item being used by the entity.
	 * @param forceUse A flag indicating if the use is forced.
	 * @return {@code true} if the action was successful, {@code false} otherwise.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			return false;
		}
		
		final Player player = playable.asPlayer();
		final int itemId = item.getId();
		final String filename = "data/html/item/" + itemId + ".htm";
		final String content = HtmCache.getInstance().getHtm(player, filename);
		final NpcHtmlMessage html = new NpcHtmlMessage(itemId);
		if (content == null)
		{
			html.setHtml("<html><body>My Text is missing:<br>" + filename + "</body></html>");
			player.sendPacket(html);
		}
		else
		{
			html.setHtml(content);
			html.replace("%itemId%", String.valueOf(item.getObjectId()));
			player.sendPacket(html);
		}
		
		// Notify events.
		final ItemTemplate itemTemplate = item.getTemplate();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_ITEM_TALK, itemTemplate))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnItemTalk(item, player), itemTemplate);
		}
		
		return true;
	}
}
