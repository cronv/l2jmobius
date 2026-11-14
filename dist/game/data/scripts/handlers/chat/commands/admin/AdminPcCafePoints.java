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

import java.util.Collection;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.FormatUtil;

/**
 * This class handles administrative commands related to PC Cafe points.<br>
 * It allows administrators to manage and modify point values for players.
 */
public class AdminPcCafePoints implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_pccafepoints",
	};
	
	/**
	 * Processes admin commands related to PC Cafe points.<br>
	 * This method handles setting, increasing, decreasing, and rewarding points for players.<br>
	 * It validates the input values and sends feedback messages to both the administrator and the target player.
	 * @param command The full command string entered by the administrator.
	 * @param activeChar The {@code Player} object of the administrator executing the command.
	 * @return {@code true} if the command was processed successfully, or {@code false} if the input was invalid.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		if (actualCommand.equals("admin_pccafepoints"))
		{
			if (st.hasMoreTokens())
			{
				final String action = st.nextToken();
				final Player target = getTarget(activeChar);
				if ((target == null) || !st.hasMoreTokens())
				{
					return false;
				}
				
				int value = 0;
				try
				{
					value = Integer.parseInt(st.nextToken());
				}
				catch (Exception e)
				{
					showMenuHtml(activeChar);
					activeChar.sendSysMessage("Invalid Value!");
					return false;
				}
				
				switch (action)
				{
					case "set":
					{
						if (value > PremiumSystemConfig.PC_CAFE_MAX_POINTS)
						{
							showMenuHtml(activeChar);
							activeChar.sendSysMessage("You cannot set more than " + PremiumSystemConfig.PC_CAFE_MAX_POINTS + " PC points!");
							return false;
						}
						
						if (value < 0)
						{
							value = 0;
						}
						
						target.setPcCafePoints(value);
						target.sendMessage("Admin set your PC Cafe point(s) to " + value + "!");
						activeChar.sendSysMessage("You set " + value + " PC Cafe point(s) to player " + target.getName());
						target.sendPacket(new ExPCCafePointInfo(value, value, 1));
						break;
					}
					case "increase":
					{
						final int pcCafePoints = target.getPcCafePoints();
						if (pcCafePoints == PremiumSystemConfig.PC_CAFE_MAX_POINTS)
						{
							showMenuHtml(activeChar);
							activeChar.sendMessage(target.getName() + " already have max count of PC points!");
							return false;
						}
						
						int pcCafeCount = Math.min(pcCafePoints + value, PremiumSystemConfig.PC_CAFE_MAX_POINTS);
						if (pcCafeCount < 0)
						{
							pcCafeCount = PremiumSystemConfig.PC_CAFE_MAX_POINTS;
						}
						
						target.setPcCafePoints(pcCafeCount);
						target.sendMessage("Admin increased your PC Cafe point(s) by " + value + "!");
						activeChar.sendSysMessage("You increased PC Cafe point(s) of " + target.getName() + " by " + value);
						target.sendPacket(new ExPCCafePointInfo(pcCafeCount, value, 1));
						break;
					}
					case "decrease":
					{
						final int pcCafePoints = target.getPcCafePoints();
						if (pcCafePoints == 0)
						{
							showMenuHtml(activeChar);
							activeChar.sendMessage(target.getName() + " already have min count of PC points!");
							return false;
						}
						
						final int pcCafeCount = Math.max(pcCafePoints - value, 0);
						target.setPcCafePoints(pcCafeCount);
						target.sendMessage("Admin decreased your PC Cafe point(s) by " + value + "!");
						activeChar.sendSysMessage("You decreased PC Cafe point(s) of " + target.getName() + " by " + value);
						target.sendPacket(new ExPCCafePointInfo(pcCafePoints, -value, 1));
						break;
					}
					case "rewardOnline":
					{
						int range = 0;
						try
						{
							range = Integer.parseInt(st.nextToken());
						}
						catch (Exception e)
						{
						}
						
						if (range <= 0)
						{
							final int count = increaseForAll(World.getPlayers(), value);
							activeChar.sendSysMessage("You increased PC Cafe point(s) of all online players (" + count + ") by " + value + ".");
						}
						else if (range > 0)
						{
							final int count = increaseForAll(World.getVisibleObjectsInRange(activeChar, Player.class, range), value);
							activeChar.sendSysMessage("You increased PC Cafe point(s) of all players (" + count + ") in range " + range + " by " + value + ".");
						}
						break;
					}
				}
			}
			
			showMenuHtml(activeChar);
		}
		
		return true;
	}
	
	/**
	 * Increases the PC Cafe points for all players in a list.<br>
	 * It checks if the player is online and not at the maximum limit.<br>
	 * The method handles negative values by setting points to {@code Integer.MAX_VALUE}.
	 * @param playerList The collection of {@code Player} objects to update.
	 * @param value The amount to add to each player's points.
	 * @return The total number of players who received the point increase.
	 */
	private int increaseForAll(Collection<Player> playerList, int value)
	{
		int counter = 0;
		for (Player temp : playerList)
		{
			if ((temp != null) && (temp.isOnlineInt() == 1))
			{
				final int pcCafePoints = temp.getPcCafePoints();
				if (pcCafePoints == Integer.MAX_VALUE)
				{
					continue;
				}
				
				int pcCafeCount = Math.min(pcCafePoints + value, Integer.MAX_VALUE);
				if (pcCafeCount < 0)
				{
					pcCafeCount = Integer.MAX_VALUE;
				}
				
				temp.setPcCafePoints(pcCafeCount);
				temp.sendMessage("Admin increased your PC Cafe point(s) by " + value + "!");
				temp.sendPacket(new ExPCCafePointInfo(pcCafeCount, value, 1));
				counter++;
			}
		}
		
		return counter;
	}
	
	/**
	 * Finds the target player for a specific character.<br>
	 * It checks if the {@code activeChar} has a target object.<br>
	 * If no valid target is found, it returns the {@code activeChar} itself.
	 * @param activeChar The player who is executing the command.
	 * @return The target {@code Player} or the {@code activeChar} if no target exists.
	 */
	private Player getTarget(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		final Player targetPlayer = target != null ? target.asPlayer() : null;
		return targetPlayer != null ? targetPlayer : activeChar;
	}
	
	/**
	 * Displays the PC Cafe Points menu to a player.<br>
	 * This method fetches the HTML from {@code HtmCache}.<br>
	 * It replaces placeholders with the target's name and points.<br>
	 * The final message is sent to the {@code activeChar} as an {@code NpcHtmlMessage}.
	 * @param activeChar The player who will receive the menu.
	 */
	private void showMenuHtml(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		final Player target = getTarget(activeChar);
		final int points = target.getPcCafePoints();
		html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/pccafe.htm"));
		html.replace("%points%", FormatUtil.formatAdena(points));
		html.replace("%targetName%", target.getName());
		activeChar.sendPacket(html);
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all registered command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
