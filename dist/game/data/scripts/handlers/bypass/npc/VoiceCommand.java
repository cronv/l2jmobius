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
package handlers.bypass.npc;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;

/**
 * Handles voice commands issued to NPCs via the bypass system.<br>
 * This class processes specific vocal inputs to trigger corresponding actions.<br>
 * It implements {@code IBypassHandler} to integrate with the game's command logic.
 * @author DS
 */
public class VoiceCommand implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"voice"
	};
	
	/**
	 * Processes a voice command issued by a {@code Player}.<br>
	 * It checks if the {@code command} starts with the required prefix.<br>
	 * If valid, it delegates the action to the appropriate {@code VoicedCommandHandler}.
	 * @param command The full string input from the player.
	 * @param player The {@code Player} who sent the command.
	 * @param target The {@code Creature} being targeted by the command.
	 * @return {@code true} if the command was executed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		// only voice commands allowed
		if ((command.length() > 7) && (command.charAt(6) == '.'))
		{
			final String vc;
			final String vparams;
			final int endOfCommand = command.indexOf(' ', 7);
			if (endOfCommand > 0)
			{
				vc = command.substring(7, endOfCommand).trim();
				vparams = command.substring(endOfCommand).trim();
			}
			else
			{
				vc = command.substring(7).trim();
				vparams = null;
			}
			
			if (!vc.isEmpty())
			{
				final IVoicedCommandHandler vch = VoicedCommandHandler.getInstance().getHandler(vc);
				if (vch != null)
				{
					return vch.onCommand(vc, player, vparams);
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the list of available voice commands.<br>
	 * This method returns all valid strings that can be processed by this handler.
	 * @return a {@code String[]} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
