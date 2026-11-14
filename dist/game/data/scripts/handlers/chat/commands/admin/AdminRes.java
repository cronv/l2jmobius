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
package handlers.chat.commands.admin;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;

/**
 * This class handles the {@code res} admin command.<br>
 * It allows administrators to resurrect a target {@code Creature}.
 */
public class AdminRes implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_res",
		"admin_res_monster"
	};
	
	/**
	 * Processes admin commands for resurrecting players or monsters.<br>
	 * This method checks the {@code command} string to determine which action to take.<br>
	 * It returns {@code true} if a valid command was recognized.
	 * @param command The raw command string entered by the user.
	 * @param activeChar The {@code Player} who executed the command.
	 * @return {@code true} if the command was handled, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.startsWith("admin_res "))
		{
			handleRes(activeChar, command.split(" ")[1]);
		}
		else if (command.equals("admin_res"))
		{
			handleRes(activeChar);
		}
		else if (command.startsWith("admin_res_monster "))
		{
			handleNonPlayerRes(activeChar, command.split(" ")[1]);
		}
		else if (command.equals("admin_res_monster"))
		{
			handleNonPlayerRes(activeChar);
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all command strings handled by this class.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Processes the resurrection command for a player.<br>
	 * This method handles the logic when an admin uses the {@code res} command.<br>
	 * It identifies the target and triggers the resurrection process.
	 * @param activeChar The {@code Player} who is executing the command.
	 */
	private void handleRes(Player activeChar)
	{
		handleRes(activeChar, null);
	}
	
	/**
	 * Handles the resurrection logic for an admin command.<br>
	 * It determines which target to resurrect based on the provided parameter.<br>
	 * If {@code resParam} is a name, it targets that player.<br>
	 * If {@code resParam} is a number, it resurrects all players in that radius.
	 * @param activeChar The player who executed the command.
	 * @param resParam The optional target name or radius value.
	 */
	private void handleRes(Player activeChar, String resParam)
	{
		WorldObject obj = activeChar.getTarget();
		if (resParam != null)
		{
			// Check if a player name was specified as a param.
			final Player plyr = World.getPlayer(resParam);
			if (plyr != null)
			{
				obj = plyr;
			}
			else
			{
				// Otherwise, check if the param was a radius.
				try
				{
					final int radius = Integer.parseInt(resParam);
					World.forEachVisibleObjectInRange(activeChar, Player.class, radius, this::doResurrect);
					activeChar.sendSysMessage("Resurrected all players within a " + radius + " unit radius.");
					return;
				}
				catch (NumberFormatException e)
				{
					activeChar.sendSysMessage("Enter a valid player name or radius.");
					return;
				}
			}
		}
		
		if (obj == null)
		{
			obj = activeChar;
		}
		
		doResurrect(obj.asCreature());
	}
	
	/**
	 * This method handles the resurrection of a non-player creature.<br>
	 * It is called when the target is not a {@code Player}.<br>
	 * It uses an empty string for the radius parameter.
	 * @param activeChar The {@code Player} who executes the command.
	 */
	private void handleNonPlayerRes(Player activeChar)
	{
		handleNonPlayerRes(activeChar, "");
	}
	
	/**
	 * Resurrects non-player entities based on a target or radius.<br>
	 * If {@code radiusStr} is provided, it resurrects all {@code Creature} objects within that range.<br>
	 * Otherwise, it attempts to resurrect the specific target object.
	 * @param activeChar The {@code Player} executing the command.
	 * @param radiusStr A string representing the area radius for mass resurrection.
	 */
	private void handleNonPlayerRes(Player activeChar, String radiusStr)
	{
		final WorldObject obj = activeChar.getTarget();
		
		try
		{
			int radius = 0;
			if (!radiusStr.isEmpty())
			{
				radius = Integer.parseInt(radiusStr);
				World.forEachVisibleObjectInRange(activeChar, Creature.class, radius, knownChar ->
				{
					if (!knownChar.isPlayer())
					{
						doResurrect(knownChar);
					}
				});
				
				activeChar.sendSysMessage("Resurrected all non-players within a " + radius + " unit radius.");
			}
		}
		catch (NumberFormatException e)
		{
			activeChar.sendSysMessage("Enter a valid radius.");
			return;
		}
		
		if ((obj == null) || obj.isPlayer())
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		doResurrect(obj.asCreature());
	}
	
	/**
	 * Resurrects a dead {@code Creature}.<br>
	 * Checks if the target is actually dead before proceeding.<br>
	 * Restores experience for players or cancels decay for monsters.<br>
	 * Calls the {@code doRevive} method on the target.
	 * @param targetChar The {@code Creature} to be resurrected.
	 */
	private void doResurrect(Creature targetChar)
	{
		if (!targetChar.isDead())
		{
			return;
		}
		
		// If the target is a player, then restore the XP lost on death.
		if (targetChar.isPlayer())
		{
			targetChar.asPlayer().restoreExp(100.0);
		}
		else
		{
			DecayTaskManager.getInstance().cancel(targetChar);
		}
		
		targetChar.doRevive();
	}
}
