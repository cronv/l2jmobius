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
package custom.SellBuff;

import org.l2jmobius.gameserver.config.custom.SellBuffsConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.managers.SellBuffsManager;

/**
 * Handles the voice commands for selling buffs.<br>
 * This class processes {@code IVoicedCommandHandler} requests related to the buff selling system.
 * @author St3eT
 */
public class SellBuffCommandHandler implements IVoicedCommandHandler
{
	private static final String[] COMMANDS =
	{
		"sellbuff",
		"sellbuffs",
	};
	
	/**
	 * Private constructor for the {@code SellBuffCommandHandler} class.<br>
	 * This prevents other classes from creating new instances of this handler.<br>
	 * It ensures that only the intended registration logic is executed.
	 */
	private SellBuffCommandHandler()
	{
		if (SellBuffsConfig.SELLBUFF_ENABLED)
		{
			VoicedCommandHandler.getInstance().registerHandler(this);
		}
	}
	
	/**
	 * Handles the execution of voiced commands for selling buffs.<br>
	 * It checks if the {@code command} matches the allowed list.<br>
	 * If it matches, it opens the sell menu for the {@code player}.
	 * @param command The name of the command typed by the user.
	 * @param player The {@code Player} who executed the command.
	 * @param params Additional parameters provided with the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, String params)
	{
		switch (command)
		{
			case "sellbuff":
			case "sellbuffs":
			{
				SellBuffsManager.getInstance().sendSellMenu(player);
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of supported voiced commands.<br>
	 * This method returns all valid strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	/**
	 * This is the entry point for the {@code SellBuffCommandHandler} class.<br>
	 * It initializes a new instance of the handler to start the application.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new SellBuffCommandHandler();
	}
}
