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
package handlers.actions.shiftclick;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IActionShiftHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the logic for when a player shift-clicks an {@code Item} in the game world.<br>
 * This class processes the interaction to determine if the item should be moved or used.<br>
 * It ensures that the action is performed correctly according to the server rules.
 */
public class ItemShiftClick implements IActionShiftHandler
{
	/**
	 * Handles the action when a player shift-clicks an object.<br>
	 * If the {@code Player} is a GM, it sends a packet with item details.<br>
	 * This method always returns {@code true}.
	 * @param player The {@code Player} performing the action.
	 * @param target The {@code WorldObject} being interacted with.
	 * @param interact A boolean indicating if the interaction was successful.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		if (player.isGM())
		{
			final NpcHtmlMessage html = new NpcHtmlMessage(0, 1, "<html><head><title>" + target.getName() + "</title></head><body><center><font color=\"LEVEL\">Item Info</font></center><br><table border=0><tr><td>Object ID: </td><td>" + target.getObjectId() + "</td></tr><tr><td>Item ID: </td><td>" + target.getId() + "</td></tr><tr><td>Owner ID: </td><td>" + ((Item) target).getOwnerId() + "</td></tr><tr><td>Location: </td><td>" + target.getLocation() + "</td></tr><tr><td><br></td></tr><tr><td>Class: </td><td>" + target.getClass().getSimpleName() + "</td></tr></table></body></html>");
			player.sendPacket(html);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance for this action.<br>
	 * This method returns a constant value from {@code InstanceType}.
	 * @return the {@code InstanceType} associated with this handler.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.Item;
	}
}
