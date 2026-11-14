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
package handlers.actions.click;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.handler.IActionClickHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the interaction when a player clicks on a {@code StaticObject}.<br>
 * It processes the click event to trigger appropriate actions or display messages.<br>
 * This class implements the {@code IActionClickHandler} interface.
 */
public class StaticObjectClick implements IActionClickHandler
{
	/**
	 * Handles the logic when a {@code Player} interacts with a {@code WorldObject}.<br>
	 * This method updates the player's target and processes specific actions based on the object type.
	 * @param player The {@code Player} who is performing the action.
	 * @param target The {@code WorldObject} being interacted with.
	 * @param interact A boolean indicating if the interaction was triggered by a click.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onAction(Player player, WorldObject target, boolean interact)
	{
		final StaticObject staticObject = (StaticObject) target;
		final int objectType = staticObject.getType();
		if (objectType < 0)
		{
			LOGGER.info("StaticObject: StaticObject with invalid type! StaticObjectId: " + staticObject.getId());
		}
		
		// Check if the Player already target the Npc.
		if (player.getTarget() != staticObject)
		{
			// Set the target of the Player player.
			player.setTarget(staticObject);
		}
		else if (interact)
		{
			// Calculate the distance between the Player and the Npc.
			if (!player.isInsideRadius2D(staticObject, Npc.INTERACTION_DISTANCE))
			{
				// Notify the Player AI with INTERACT.
				player.getAI().setIntentionInteract(staticObject);
			}
			else if (objectType == 2)
			{
				final String filename = (staticObject.getId() == 24230101) ? "data/html/signboard/tomb_of_crystalgolem.htm" : "data/html/signboard/pvp_signboard.htm";
				final String content = HtmCache.getInstance().getHtm(player, filename);
				final NpcHtmlMessage html = new NpcHtmlMessage(staticObject.getObjectId());
				
				if (content == null)
				{
					html.setHtml("<html><body>Signboard is missing:<br>" + filename + "</body></html>");
				}
				else
				{
					html.setHtml(content);
				}
				
				player.sendPacket(html);
			}
			else if (objectType == 0)
			{
				player.sendPacket(staticObject.getMap());
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of instance for this object.<br>
	 * This method returns a constant value from {@code InstanceType}.
	 * @return the {@code InstanceType} associated with this action.
	 */
	@Override
	public InstanceType getInstanceType()
	{
		return InstanceType.StaticObject;
	}
}
