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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;

/**
 * Handles the admin command to toggle invulnerability for a target creature.<br>
 * This allows administrators to make a {@code Player} or {@code Creature} immune to damage.
 * @author Skache
 */
public class AdminInvul implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_invul",
		"admin_setinvul",
		"admin_undying",
		"admin_setundying"
	};
	
	/**
	 * Processes administrative commands for toggling invulnerability and undying states.<br>
	 * This method handles specific command strings to affect the sender or a targeted entity.
	 * @param command The string name of the command being executed.
	 * @param activeChar The {@code Player} who is currently executing the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		switch (command)
		{
			case "admin_invul":
			{
				toggleInvul(activeChar, activeChar);
				break;
			}
			case "admin_setinvul":
			{
				final WorldObject target = activeChar.getTarget();
				if ((target != null) && target.isPlayer())
				{
					toggleInvul(target.asPlayer(), activeChar);
				}
				else
				{
					activeChar.sendSysMessage("You must target a player.");
				}
				break;
			}
			case "admin_undying":
			{
				toggleUndying(activeChar, activeChar);
				AdminHtml.showAdminHtml(activeChar, "gm_menu.htm");
				break;
			}
			case "admin_setundying":
			{
				final WorldObject target = activeChar.getTarget();
				if ((target != null) && target.isCreature())
				{
					toggleUndying(target.asCreature(), activeChar);
				}
				else
				{
					activeChar.sendSysMessage("You must target a creature.");
				}
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Toggles the invulnerability status of a player.<br>
	 * This method switches the {@code isInvul} state between {@code true} and {@code false}.<br>
	 * It sends a confirmation message to both the target and the admin.
	 * @param target The {@code Player} whose invulnerability will be changed.
	 * @param admin The {@code Player} who executed the command.
	 */
	private void toggleInvul(Player target, Player admin)
	{
		final boolean invul = !target.isInvul();
		target.setInvul(invul);
		
		// Notify target.
		target.sendSysMessage("You are now " + (invul ? "invulnerable." : "mortal."));
		
		// Notify admin.
		if (admin != target)
		{
			admin.sendSysMessage("You made " + target.getName() + " " + (invul ? "invulnerable." : "mortal."));
		}
	}
	
	/**
	 * Toggles the undying status of a specific creature.<br>
	 * This method switches the state between mortal and undying.<br>
	 * It sends a system message to both the target and the admin.
	 * @param target The {@code Creature} whose status is being changed.
	 * @param admin The {@code Player} who executed the command.
	 */
	private void toggleUndying(Creature target, Player admin)
	{
		final boolean undying = !target.isUndying();
		target.setUndying(undying);
		
		// Notify target if it's a player.
		if (target.isPlayer())
		{
			target.asPlayer().sendSysMessage("You are now " + (undying ? "undying." : "mortal."));
		}
		
		// Notify admin.
		if (admin != target)
		{
			admin.sendSysMessage("You made " + target.getName() + " " + (undying ? "undying." : "mortal."));
		}
	}
	
	/**
	 * Retrieves the list of available administrative commands.<br>
	 * This method returns all command strings associated with this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
