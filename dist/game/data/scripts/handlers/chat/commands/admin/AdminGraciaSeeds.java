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

import java.util.StringTokenizer;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.GraciaSeedsConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.GraciaSeedsManager;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the admin commands for viewing and managing the Gracia seeds.<br>
 * Covers the Seed of Destruction state and Tiat kills.<br>
 * Every command ends by showing the status menu.
 */
public class AdminGraciaSeeds implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_gracia_seeds",
		"admin_kill_tiat",
		"admin_set_sodstate"
	};
	
	/**
	 * Processes administrative commands related to Gracia Seeds.<br>
	 * Validates every numeric argument before touching the seed manager.<br>
	 * It always displays the status menu to the player after execution.
	 * @param command The full command string entered by the user.
	 * @param activeChar The {@code Player} who executed the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken(); // Get actual command
		if (actualCommand.equalsIgnoreCase("admin_kill_tiat"))
		{
			if (GraciaSeedsManager.getInstance().getSoDState() == 1)
			{
				GraciaSeedsManager.getInstance().increaseSoDTiatKilled();
			}
			else
			{
				activeChar.sendSysMessage("Tiat kills only count while the Seed of Destruction is in state 1.");
			}
		}
		else if (actualCommand.equalsIgnoreCase("admin_set_sodstate"))
		{
			final int state = parseValue(st, 1, 2);
			if (state > 0)
			{
				GraciaSeedsManager.getInstance().setSoDState(state, true);
			}
			else
			{
				activeChar.sendSysMessage("Usage: //set_sodstate <1-2>");
			}
		}
		
		showMenu(activeChar);
		return true;
	}
	
	/**
	 * Reads the next token as a whole number within the given bounds.
	 * @param st The tokenizer to read from.
	 * @param min The lowest accepted value.
	 * @param max The highest accepted value.
	 * @return The parsed value, or {@code -1} when the token is missing, not numeric or out of bounds.
	 */
	private int parseValue(StringTokenizer st, int min, int max)
	{
		if (!st.hasMoreTokens())
		{
			return -1;
		}
		
		final String token = st.nextToken();
		if (!StringUtil.isNumeric(token) || (token.length() > 9))
		{
			return -1;
		}
		
		final int value = Integer.parseInt(token);
		if ((value < min) || (value > max))
		{
			return -1;
		}
		
		return value;
	}
	
	/**
	 * Displays the Gracia Seeds admin menu to a player.<br>
	 * Fills the HTML with the live state of the Seed of Destruction, the Tiat kills against their configured threshold and the pending timer.
	 * @param activeChar The {@code Player} who will receive the menu packet.
	 */
	private void showMenu(Player activeChar)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(activeChar, "data/html/admin/graciaseeds.htm");
		final GraciaSeedsManager sod = GraciaSeedsManager.getInstance();
		final int sodState = sod.getSoDState();
		html.replace("%sodstate%", sodState + " (" + getSoDStateName(sodState) + ")");
		html.replace("%sodtiatkill%", sod.getSoDTiatKilled() + " / " + GraciaSeedsConfig.SOD_TIAT_KILL_COUNT);
		html.replace("%sodlastchange%", TimeUtil.getDateTimeString(sod.getSoDLastStateChangeDate().getTimeInMillis()));
		final long sodTimeForNextStateChange = sod.getSoDTimeForNextStateChange();
		if (sodTimeForNextStateChange > 0)
		{
			html.replace("%sodtime%", TimeUtil.getDateTimeString(System.currentTimeMillis() + sodTimeForNextStateChange));
		}
		else
		{
			html.replace("%sodtime%", "-");
		}
		
		activeChar.sendPacket(html);
	}
	
	/**
	 * Names a Seed of Destruction state for display.
	 * @param state The state value from {@code GraciaSeedsManager}.
	 * @return The readable state name.
	 */
	private String getSoDStateName(int state)
	{
		switch (state)
		{
			case 1:
			{
				return "Attack";
			}
			case 2:
			{
				return "Conquest Complete";
			}
			case 3:
			{
				return "Defense";
			}
			default:
			{
				return "Unknown";
			}
		}
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
