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

import java.util.logging.Level;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.network.serverpackets.augmentation.ExShowVariationCancelWindow;
import org.l2jmobius.gameserver.network.serverpackets.augmentation.ExShowVariationMakeWindow;

/**
 * Handles the bypass logic for the {@code Augment} NPC interaction.<br>
 * This class manages the transition between different UI windows during the augmentation process.
 */
public class Augment implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"Augment"
	};
	
	/**
	 * Handles the Augment command for players.<br>
	 * This method checks if the target is an NPC.<br>
	 * It processes specific numeric arguments to open or close windows.
	 * @param command The command string sent by the player.
	 * @param player The {@code Player} instance who executed the command.
	 * @param target The {@code Creature} being targeted by the player.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		try
		{
			switch (Integer.parseInt(command.substring(8, 9).trim()))
			{
				case 1:
				{
					player.sendPacket(ExShowVariationMakeWindow.STATIC_PACKET);
					return true;
				}
				case 2:
				{
					player.sendPacket(ExShowVariationCancelWindow.STATIC_PACKET);
					return true;
				}
			}
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
