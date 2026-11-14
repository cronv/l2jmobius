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

/**
 * Handles the logic for a player's run and walk actions.<br>
 * This {@code RunWalk} class manages movement states for the user.
 * @author Mobius
 */
public class RunWalk implements IActionUserHandler
{
	/**
	 * Handles the logic for toggling between running and walking.<br>
	 * It checks the current movement state of the {@code Player}.<br>
	 * The player will switch to walking if they are currently running.<br>
	 * Otherwise, the player will switch to running.
	 * @param player The {@code Player} performing the action.
	 * @param data The {@code ActionDataHolder} containing action details.
	 * @param ctrlPressed Whether the {@code Ctrl} key is pressed.
	 * @param shiftPressed Whether the {@code Shift} key is pressed.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (player.isRunning())
		{
			player.setWalking();
		}
		else
		{
			player.setRunning();
		}
	}
}
