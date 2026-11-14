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
package handlers.chat.commands.admin;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the admin command to allow a player to ride a specific mount.<br>
 * This class processes the request and updates the {@code Player} state accordingly.
 */
public class AdminRide implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_ride_horse",
		"admin_ride_bike",
		"admin_ride_wyvern",
		"admin_ride_strider",
		"admin_unride_wyvern",
		"admin_unride_strider",
		"admin_unride",
		"admin_ride_wolf",
		"admin_unride_wolf",
	};
	
	private static final int PURPLE_MANED_HORSE_TRANSFORMATION_ID = 106;
	
	private static final int JET_BIKE_TRANSFORMATION_ID = 20001;
	
	/**
	 * Processes admin commands for mounting and unmounting players.<br>
	 * This method handles specific ride types like horses, bikes, wyverns, striders, and wolves.<br>
	 * It checks if the target player is eligible for the requested action before executing it.
	 * @param command The command string sent by the administrator.
	 * @param activeChar The {@code Player} object who is executing the command.
	 * @return {@code true} if the command was processed successfully, or {@code false} otherwise.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final Player player = getRideTarget(activeChar);
		if (player == null)
		{
			return false;
		}
		
		if (command.startsWith("admin_ride"))
		{
			if (player.isMounted() || player.hasSummon())
			{
				activeChar.sendSysMessage("Target already have a summon.");
				return false;
			}
			
			int petRideId;
			if (command.startsWith("admin_ride_wyvern"))
			{
				petRideId = 12621;
			}
			else if (command.startsWith("admin_ride_strider"))
			{
				petRideId = 12526;
			}
			else if (command.startsWith("admin_ride_wolf"))
			{
				petRideId = 16041;
			}
			else if (command.startsWith("admin_ride_horse")) // handled using transformation
			{
				if (player.isTransformed())
				{
					activeChar.sendPacket(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
				}
				else
				{
					player.transform(PURPLE_MANED_HORSE_TRANSFORMATION_ID, true);
				}
				
				return true;
			}
			else if (command.startsWith("admin_ride_bike")) // handled using transformation
			{
				if (player.isTransformed())
				{
					activeChar.sendPacket(SystemMessageId.YOU_ALREADY_POLYMORPHED_AND_CANNOT_POLYMORPH_AGAIN);
				}
				else
				{
					player.transform(JET_BIKE_TRANSFORMATION_ID, true);
				}
				
				return true;
			}
			else
			{
				activeChar.sendSysMessage("Command '" + command + "' not recognized");
				return false;
			}
			
			player.mount(petRideId, 0, false);
			return false;
		}
		else if (command.startsWith("admin_unride"))
		{
			final int transformationId = player.getTransformationId();
			if (transformationId == PURPLE_MANED_HORSE_TRANSFORMATION_ID)
			{
				player.untransform();
			}
			
			if (transformationId == JET_BIKE_TRANSFORMATION_ID)
			{
				player.untransform();
			}
			else
			{
				player.dismount();
			}
		}
		
		return true;
	}
	
	/**
	 * Determines the target {@code Player} for a ride command.<br>
	 * It checks if the {@code activeChar} has a valid target.<br>
	 * If the target is invalid or not a {@code Player}, it returns the {@code activeChar}.<br>
	 * Otherwise, it returns the targeted {@code Player}.
	 * @param activeChar The player executing the command.
	 * @return The {@code Player} object to be affected by the ride action.
	 */
	private Player getRideTarget(Player activeChar)
	{
		Player player = null;
		if ((activeChar.getTarget() == null) || (activeChar.getTarget().getObjectId() == activeChar.getObjectId()) || !activeChar.getTarget().isPlayer())
		{
			player = activeChar;
		}
		else
		{
			player = activeChar.getTarget().asPlayer();
		}
		
		return player;
	}
	
	/**
	 * Retrieves the list of available admin ride commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
