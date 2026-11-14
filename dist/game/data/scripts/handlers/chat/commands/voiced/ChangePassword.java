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
package handlers.chat.commands.voiced;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the voice command for changing a player's password.<br>
 * It validates the new password and updates it in the system.
 * @author Nik
 */
public class ChangePassword implements IVoicedCommandHandler
{
	private static final Logger LOGGER = Logger.getLogger(ChangePassword.class.getName());
	
	private static final String[] VOICED_COMMANDS =
	{
		"changepassword"
	};
	
	/**
	 * Processes the voiced command to change a player's password.<br>
	 * It validates the current and new passwords provided in the target string.<br>
	 * If no target is provided, it displays an HTML help page.
	 * @param command The name of the command being executed.
	 * @param activeChar The {@code Player} who sent the command.
	 * @param target The arguments containing the old and new passwords.
	 * @return {@code true} if the command was processed successfully, {@code false} otherwise.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar, String target)
	{
		if (target != null)
		{
			final StringTokenizer st = new StringTokenizer(target);
			try
			{
				String curpass = null;
				String newpass = null;
				String repeatnewpass = null;
				if (st.hasMoreTokens())
				{
					curpass = st.nextToken();
				}
				
				if (st.hasMoreTokens())
				{
					newpass = st.nextToken();
				}
				
				if (st.hasMoreTokens())
				{
					repeatnewpass = st.nextToken();
				}
				
				if (!((curpass == null) || (newpass == null) || (repeatnewpass == null)))
				{
					if (!newpass.equals(repeatnewpass))
					{
						activeChar.sendMessage("The new password doesn't match with the repeated one!");
						return false;
					}
					
					final int newpassLength = newpass.length();
					if (newpassLength < 3)
					{
						activeChar.sendMessage("The new password is shorter than 3 chars! Please try with a longer one.");
						return false;
					}
					
					if (newpassLength > 30)
					{
						activeChar.sendMessage("The new password is longer than 30 chars! Please try with a shorter one.");
						return false;
					}
					
					LoginServerThread.getInstance().sendChangePassword(activeChar.getAccountName(), activeChar.getName(), curpass, newpass);
				}
				else
				{
					activeChar.sendMessage("Invalid password data! You have to fill all boxes.");
					return false;
				}
			}
			catch (Exception e)
			{
				activeChar.sendMessage("A problem occurred while changing password!");
				LOGGER.log(Level.WARNING, "", e);
			}
		}
		else
		{
			// showHTML(activeChar);
			String html = HtmCache.getInstance().getHtm(null, "data/html/mods/ChangePassword.htm");
			if (html == null)
			{
				html = "<html><body><br><br><center><font color=LEVEL>404:</font> File Not Found</center></body></html>";
			}
			
			activeChar.sendPacket(new NpcHtmlMessage(html));
			return true;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of supported voice commands.<br>
	 * This method returns all valid command strings for this handler.
	 * @return an array of {@code String} containing the available commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return VOICED_COMMANDS;
	}
}
