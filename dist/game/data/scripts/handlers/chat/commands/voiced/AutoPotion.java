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
package handlers.chat.commands.voiced;

import org.l2jmobius.gameserver.config.custom.AutoPotionsConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.taskmanagers.AutoPotionTaskManager;

/**
 * Handles the {@code /autopotion} voiced command for players.<br>
 * This class allows users to toggle their automatic potion usage settings via voice chat.
 * @author Mobius, Gigi
 */
public class AutoPotion implements IVoicedCommandHandler
{
	private static final String[] VOICED_COMMANDS =
	{
		"apon",
		"apoff",
		"potionon",
		"potionoff"
	};
	
	/**
	 * Processes voiced commands for the auto potion system.<br>
	 * Checks if the player meets the required level and configuration settings.<br>
	 * Enables or disables the auto potion task for the specified character.
	 * @param command The string command sent by the player.
	 * @param activeChar The {@code Player} who issued the command.
	 * @param target The target of the command, if applicable.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar, String target)
	{
		if (!AutoPotionsConfig.AUTO_POTIONS_ENABLED || (activeChar == null))
		{
			return false;
		}
		
		if (activeChar.getLevel() < AutoPotionsConfig.AUTO_POTION_MIN_LEVEL)
		{
			activeChar.sendMessage("You need to be at least " + AutoPotionsConfig.AUTO_POTION_MIN_LEVEL + " to use auto potions.");
			return false;
		}
		
		switch (command)
		{
			case "apon":
			case "potionon":
			{
				AutoPotionTaskManager.getInstance().add(activeChar);
				activeChar.sendMessage("Auto potions is enabled.");
				break;
			}
			case "apoff":
			case "potionoff":
			{
				AutoPotionTaskManager.getInstance().remove(activeChar);
				activeChar.sendMessage("Auto potions is disabled.");
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of supported voice commands.<br>
	 * This method returns all valid strings for {@code AutoPotion}.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
