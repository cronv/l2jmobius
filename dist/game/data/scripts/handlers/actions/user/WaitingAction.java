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
package handlers.actions.user;

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

/**
 * Handles the logic for a player who is currently in a waiting state.<br>
 * This class manages the behavior and interactions of a {@code Player} while they are queued or idling.
 * @author Index
 */
public class WaitingAction implements IActionUserHandler
{
	/**
	 * Handles the logic for a player performing a waiting action.<br>
	 * It checks if the player belongs to a clan with an assigned castle.<br>
	 * The method sends a specific {@code SocialAction} packet based on leadership status.
	 * @param player The {@code Player} who is performing the action.
	 * @param data The {@code ActionDataHolder} containing details about the action.
	 * @param ctrlPressed Whether the {@code Ctrl} key is currently held down.
	 * @param shiftPressed Whether the {@code Shift} key is currently held down.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		final int actionId = (player.getClan() != null) && (CastleManager.getInstance().getCastleByOwner(player.getClan()) != null) ? player.isClanLeader() ? 100 : 101 : 0;
		if (actionId == 0)
		{
			player.sendPacket(SystemMessageId.YOU_HAVE_NOT_SET_A_LIST_OF_ACTIONS_FOR_THE_WAITING_TIME);
			return;
		}
		
		player.sendPacket(new SocialAction(player.getObjectId(), actionId));
	}
}
