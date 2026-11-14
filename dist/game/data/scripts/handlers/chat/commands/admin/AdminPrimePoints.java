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

import java.util.Collection;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.FormatUtil;

/**
 * This class handles the logic for {@code AdminPrimePoints} commands.<br>
 * It allows administrators to manage and interact with prime points within the game.
 * @author St3eT
 */
public class AdminPrimePoints implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_primepoints",
	};
	
	/**
	 * Processes admin commands related to Prime Points.<br>
	 * This method handles setting, increasing, decreasing, or rewarding points to players.
	 * @param command The full command string entered by the administrator.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		if (actualCommand.equals("admin_primepoints"))
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
						target.setPrimePoints(value);
						target.sendMessage("Admin set your Prime Point(s) to " + value + "!");
						activeChar.sendSysMessage("You set " + value + " Prime Point(s) to player " + target.getName());
						break;
					}
					case "increase":
					{
						final int primePoints = target.getPrimePoints();
						if (primePoints == Integer.MAX_VALUE)
						{
							showMenuHtml(activeChar);
							activeChar.sendMessage(target.getName() + " already have max count of Prime Points!");
							return false;
						}
						
						int primeCount = Math.min((primePoints + value), Integer.MAX_VALUE);
						if (primeCount < 0)
						{
							primeCount = Integer.MAX_VALUE;
						}
						
						target.setPrimePoints(primeCount);
						target.sendMessage("Admin increase your Prime Point(s) by " + value + "!");
						activeChar.sendSysMessage("You increased Prime Point(s) of " + target.getName() + " by " + value);
						break;
					}
					case "decrease":
					{
						final int primePoints = target.getPrimePoints();
						if (primePoints == 0)
						{
							showMenuHtml(activeChar);
							activeChar.sendMessage(target.getName() + " already have min count of Prime Points!");
							return false;
						}
						
						final int primeCount = Math.max(primePoints - value, 0);
						target.setPrimePoints(primeCount);
						target.sendMessage("Admin decreased your Prime Point(s) by " + value + "!");
						activeChar.sendSysMessage("You decreased Prime Point(s) of " + target.getName() + " by " + value);
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
							activeChar.sendSysMessage("You increased Prime Point(s) of all online players (" + count + ") by " + value + ".");
						}
						else if (range > 0)
						{
							final int count = increaseForAll(World.getVisibleObjectsInRange(activeChar, Player.class, range), value);
							activeChar.sendSysMessage("You increased Prime Point(s) of all players (" + count + ") in range " + range + " by " + value + ".");
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
	 * Increases the prime points for all online players in a list.<br>
	 * It checks if the player is currently online and not at the maximum limit.<br>
	 * The method ensures the new value stays within valid integer bounds.
	 * @param playerList The collection of {@code Player} objects to process.
	 * @param value The amount of points to add to each player.
	 * @return The total number of players who received the point increase.
	 */
	private int increaseForAll(Collection<Player> playerList, int value)
	{
		int counter = 0;
		for (Player temp : playerList)
		{
			if ((temp != null) && (temp.isOnlineInt() == 1))
			{
				final int primePoints = temp.getPrimePoints();
				if (primePoints == Integer.MAX_VALUE)
				{
					continue;
				}
				
				int primeCount = Math.min((primePoints + value), Integer.MAX_VALUE);
				if (primeCount < 0)
				{
					primeCount = Integer.MAX_VALUE;
				}
				
				temp.setPrimePoints(primeCount);
				temp.sendMessage("Admin increase your Prime Point(s) by " + value + "!");
				counter++;
			}
		}
		
		return counter;
	}
	
	/**
	 * Finds the player currently targeted by the active character.<br>
	 * If no target exists, it returns the {@code activeChar} itself.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @return The {@code Player} being targeted or the {@code activeChar}.
	 */
	private Player getTarget(Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		final Player targetPlayer = target != null ? target.asPlayer() : null;
		return targetPlayer != null ? targetPlayer : activeChar;
	}
	
	/**
	 * Displays the admin prime points menu to a player.<br>
	 * This method fetches the HTML template from {@code HtmCache}.<br>
	 * It replaces placeholders with the target's name and point count.<br>
	 * The final message is sent as a packet to the {@code activeChar}.
	 * @param activeChar The {@code Player} who will receive the menu.
	 */
	private void showMenuHtml(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		final Player target = getTarget(activeChar);
		final int points = target.getPrimePoints();
		html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/primepoints.htm"));
		html.replace("%points%", FormatUtil.formatAdena(points));
		html.replace("%targetName%", target.getName());
		activeChar.sendPacket(html);
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns the internal {@code ADMIN_COMMANDS} array.
	 * @return An array of {@code String} objects containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
