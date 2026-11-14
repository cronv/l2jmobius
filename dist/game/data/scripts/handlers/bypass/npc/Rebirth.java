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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.managers.RebirthManager;

/**
 * Handles rebirth-related bypass commands and delegates actions to the {@code RebirthManager}.<br>
 * This class manages requests for menu displays, confirmations, skill selections, previews, acquisitions, and resets.<br>
 * It validates player references and parses skill identifiers from command parameters.
 * @author BazookaRpm
 */
public final class Rebirth implements IBypassHandler
{
	// Bypass commands.
	private static final String[] COMMANDS =
	{
		"rebirth_openmenu",
		"rebirth_confirmrequest",
		"rebirth_selectskills",
		"rebirth_previewSkill",
		"rebirth_confirmSkill",
		"rebirth_resetSkill"
	};
	
	/**
	 * Processes a rebirth-related bypass command.<br>
	 * This method identifies the action and delegates it to the {@code RebirthManager}.<br>
	 * It returns {@code true} if the command is recognized and handled.
	 * @param command The string name of the command to execute.
	 * @param player The {@code Player} who sent the command.
	 * @param bypassOrigin The {@code Creature} that triggered the bypass, or {@code null}.
	 * @return {@code true} if the command was successfully processed, {@code false} otherwise.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature bypassOrigin)
	{
		if ((command == null) || (player == null))
		{
			return false;
		}
		
		final int objectId = (bypassOrigin != null) ? bypassOrigin.getObjectId() : 0;
		
		switch (command)
		{
			case "rebirth_openmenu":
			{
				RebirthManager.getInstance().displayMainWindow(player, objectId);
				return true;
			}
			case "rebirth_confirmrequest":
			{
				RebirthManager.getInstance().requestRebirth(player);
				return true;
			}
			case "rebirth_selectskills":
			{
				RebirthManager.getInstance().displaySkillSelectionWindow(player, objectId);
				return true;
			}
			default:
			{
				if (command.startsWith("rebirth_previewSkill"))
				{
					final Integer skillId = parseSkillId(command);
					if (skillId == null)
					{
						player.sendMessage("Invalid skill.");
						return true;
					}
					
					RebirthManager.getInstance().displaySkillPreviewWindow(player, objectId, skillId.intValue());
					return true;
				}
				else if (command.startsWith("rebirth_confirmSkill"))
				{
					final Integer skillId = parseSkillId(command);
					if (skillId == null)
					{
						player.sendMessage("Invalid skill.");
						return true;
					}
					
					RebirthManager.getInstance().acquireRebirthSkill(player, objectId, skillId.intValue());
					return true;
				}
				else if (command.startsWith("rebirth_resetSkill"))
				{
					final Integer skillId = parseSkillId(command);
					if (skillId == null)
					{
						player.sendMessage("Invalid skill.");
						return true;
					}
					
					RebirthManager.getInstance().resetRebirthSkill(player, objectId, skillId.intValue());
					return true;
				}
				break;
			}
		}
		
		return false;
	}
	
	/**
	 * Extracts a skill identifier from a command string.<br>
	 * It looks for the first space and parses the following value as an {@code Integer}.<br>
	 * Returns {@code null} if no ID is found or if parsing fails.
	 * @param command The raw command string to parse.
	 * @return The parsed skill ID or {@code null}.
	 */
	private Integer parseSkillId(String command)
	{
		try
		{
			final int spaceIndex = command.indexOf(' ');
			if (spaceIndex > 0)
			{
				return Integer.valueOf(Integer.parseInt(command.substring(spaceIndex + 1).trim()));
			}
		}
		catch (Exception e)
		{
		}
		
		return null;
	}
	
	/**
	 * Retrieves the list of supported rebirth commands.<br>
	 * This method returns all valid strings that this handler can process.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}