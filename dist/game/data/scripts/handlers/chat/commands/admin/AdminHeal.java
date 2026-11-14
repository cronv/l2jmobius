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

/**
 * This class handles the {@code heal} admin command.<br>
 * It restores {@code HP}, {@code MP}, and {@code CP} to a target, specific name, or radius.
 */
public class AdminHeal implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_heal"
	};
	
	/**
	 * Processes an admin command sent by a player.<br>
	 * This method checks if the {@code command} is for healing.<br>
	 * It executes the appropriate heal logic based on the input string.
	 * @param command The raw command string entered by the user.
	 * @param activeChar The {@code Player} who executed the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if (command.equals("admin_heal"))
		{
			handleHeal(activeChar);
		}
		else if (command.startsWith("admin_heal"))
		{
			try
			{
				final String healTarget = command.substring(11);
				handleHeal(activeChar, healTarget);
			}
			catch (StringIndexOutOfBoundsException e)
			{
				activeChar.sendSysMessage("Incorrect target/radius specified.");
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all strings associated with this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Processes the healing command for a specific player.<br>
	 * This method restores health and mana to the target.<br>
	 * It is called when an admin uses the heal command.
	 * @param activeChar The {@code Player} who is executing the command.
	 */
	private void handleHeal(Player activeChar)
	{
		handleHeal(activeChar, null);
	}
	
	/**
	 * Processes the heal command for a specific target or radius.<br>
	 * It restores HP, MP, and CP to the selected entity.<br>
	 * If a number is provided, it heals all visible creatures in that radius.
	 * @param activeChar The {@code Player} executing the command.
	 * @param player The name of the target player or the radius value as a {@code String}.
	 */
	private void handleHeal(Player activeChar, String player)
	{
		WorldObject obj = activeChar.getTarget();
		if (player != null)
		{
			final Player plyr = World.getPlayer(player);
			if (plyr != null)
			{
				obj = plyr;
			}
			else
			{
				try
				{
					final int radius = Integer.parseInt(player);
					World.forEachVisibleObject(activeChar, Creature.class, character ->
					{
						character.setCurrentHpMp(character.getMaxHp(), character.getMaxMp());
						if (character.isPlayer())
						{
							character.setCurrentCp(character.getMaxCp());
						}
					});
					
					activeChar.sendSysMessage("Healed within " + radius + " unit radius.");
					return;
				}
				catch (NumberFormatException nbe)
				{
				}
			}
		}
		
		if (obj == null)
		{
			obj = activeChar;
		}
		
		if (obj.isCreature())
		{
			final Creature target = obj.asCreature();
			target.setCurrentHpMp(target.getMaxHp(), target.getMaxMp());
			if (target.isPlayer())
			{
				target.setCurrentCp(target.getMaxCp());
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
		}
	}
}
