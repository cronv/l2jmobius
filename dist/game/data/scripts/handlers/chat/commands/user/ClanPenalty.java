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
package handlers.chat.commands.user;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.IUserCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the {@code ClanPenalty} user command.<br>
 * This class allows players to view or manage penalties associated with a clan.
 * @author Tempy, Mobius
 */
public class ClanPenalty implements IUserCommandHandler
{
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final int[] COMMAND_IDS =
	{
		100
	};
	
	/**
	 * Handles the execution of a clan penalty command.<br>
	 * This method checks for various clan-related restrictions and sends an HTML message to the player.
	 * @param id The unique identifier for the command.
	 * @param player The {@code Player} who executed the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(int id, Player player)
	{
		if (id != COMMAND_IDS[0])
		{
			return false;
		}
		
		final StringBuilder htmlContent = new StringBuilder(500);
		htmlContent.append("<html><body><center><table width=270 border=0 bgcolor=111111><tr><td width=170>Penalty</td><td width=100 align=center>Expiration Date</td></tr></table><table width=270 border=0>");
		final int headerLength = htmlContent.length();
		final long currentTime = System.currentTimeMillis();
		final Clan clan = player.getClan();
		appendPenalty(htmlContent, "Unable to join a clan.", player.getClanJoinExpiryTime(), currentTime);
		if (clan != null)
		{
			appendPenalty(htmlContent, "Unable to invite a clan member.", clan.getCharPenaltyExpiryTime(), currentTime);
		}
		
		appendPenalty(htmlContent, "Unable to create a clan.", player.getClanCreateExpiryTime(), currentTime);
		if (clan != null)
		{
			final int allyPenaltyType = clan.getAllyPenaltyType();
			if ((allyPenaltyType == Clan.PENALTY_TYPE_CLAN_LEAVED) || (allyPenaltyType == Clan.PENALTY_TYPE_CLAN_DISMISSED))
			{
				appendPenalty(htmlContent, "Unable to join an alliance.", clan.getAllyPenaltyExpiryTime(), currentTime);
			}
			else if (allyPenaltyType == Clan.PENALTY_TYPE_DISMISS_CLAN)
			{
				appendPenalty(htmlContent, "Unable to invite a new alliance member.", clan.getAllyPenaltyExpiryTime(), currentTime);
			}
			else if (allyPenaltyType == Clan.PENALTY_TYPE_DISSOLVE_ALLY)
			{
				appendPenalty(htmlContent, "Unable to create an alliance.", clan.getAllyPenaltyExpiryTime(), currentTime);
			}
			
			appendPenalty(htmlContent, "The request to dissolve the clan is currently being processed. (Restrictions are now going to be imposed on the use of clan functions)", clan.getDissolvingExpiryTime(), currentTime);
		}
		
		// Nothing was appended after the header.
		if (htmlContent.length() == headerLength)
		{
			htmlContent.append("<tr><td width=170>No penalty is imposed.</td><td width=100 align=center></td></tr>");
		}
		
		htmlContent.append("</table><img src=\"L2UI.SquareWhite\" width=270 height=1></center></body></html>");
		
		final NpcHtmlMessage penaltyHtml = new NpcHtmlMessage();
		penaltyHtml.setHtml(htmlContent.toString());
		player.sendPacket(penaltyHtml);
		
		return true;
	}
	
	/**
	 * Appends one penalty row while the penalty is still running.
	 * @param html The window being built.
	 * @param penalty The penalty description.
	 * @param expiryTime The time the penalty ends.
	 * @param currentTime The current time.
	 */
	private void appendPenalty(StringBuilder html, String penalty, long expiryTime, long currentTime)
	{
		if (expiryTime > currentTime)
		{
			html.append("<tr><td width=170>");
			html.append(penalty);
			html.append("</td><td width=100 align=center>");
			html.append(DATE_FORMAT.format(Instant.ofEpochMilli(expiryTime).atZone(ZoneId.systemDefault())));
			html.append("</td></tr>");
		}
	}
	
	/**
	 * Retrieves the list of available command IDs.<br>
	 * This method returns the internal {@code COMMAND_IDS} array.
	 * @return an {@code int[]} containing all valid command identifiers.
	 */
	@Override
	public int[] getCommandList()
	{
		return COMMAND_IDS;
	}
}
