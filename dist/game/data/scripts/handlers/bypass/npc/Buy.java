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
package handlers.bypass.npc;

import java.util.StringTokenizer;
import java.util.logging.Level;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.handler.IBypassHandler;

/**
 * Handles the bypass command for purchasing items from an NPC.<br>
 * This class processes requests to interact with a {@code Merchant} entity.<br>
 * It ensures that players can successfully complete transactions through specific bypass triggers.
 */
public class Buy implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Buy"
	};
	
	/**
	 * Handles the command to open a purchase window.<br>
	 * This method checks if the {@code target} is a {@code Merchant}.<br>
	 * It parses the amount from the {@code command} string.<br>
	 * If successful, it opens the shop for the {@code player}.
	 * @param command The full command string sent by the player.
	 * @param player The {@code Player} who executed the command.
	 * @param target The {@code Creature} being interacted with.
	 * @return {@code true} if the window was opened successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!(target instanceof Merchant))
		{
			return false;
		}
		
		try
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			if (st.countTokens() < 1)
			{
				return false;
			}
			
			((Merchant) target).showBuyWindow(player, Integer.parseInt(st.nextToken()));
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in " + getClass().getSimpleName(), e);
		}
		
		return false;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * This method returns the {@code COMMANDS} array.
	 * @return an array of {@code String} objects containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
