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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.holders.RelicDataHolder;
import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.entity.actor.holders.player.PlayerRelicData;
import org.l2jmobius.gameserver.entity.actor.holders.player.RelicList;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionUpdate;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsList;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsUpdateList;

/**
 * Handles administrative commands for managing relics.<br>
 * This class provides the logic for the admin relic menu interface.
 * @author CostyKiller
 */
public class AdminRelics implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_relic_menu",
		"admin_give_relic",
		"admin_relic_page"
	};
	
	// Track current page per grade
	private static final Map<String, Integer> CURRENT_PAGES = new HashMap<>();
	
	/**
	 * Processes admin commands related to relics.<br>
	 * Validates the target player before executing any action.<br>
	 * Handles menu navigation and relic distribution.
	 * @param command The command string sent by the administrator.
	 * @param activeChar The {@code Player} object of the administrator sending the command.
	 * @return {@code true} if the command was processed successfully, {@code false} otherwise.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final WorldObject activeCharTarget = activeChar.getTarget();
		if ((activeCharTarget == null) || !activeCharTarget.isPlayer())
		{
			activeChar.sendMessage("Invalid target!");
			return false;
		}
		Player target = activeCharTarget.asPlayer();
		
		if (command.startsWith("admin_relic_menu"))
		{
			// Reset all pages.
			String playerKey = activeChar.getName();
			CURRENT_PAGES.put(playerKey + "_1", 0);
			CURRENT_PAGES.put(playerKey + "_2", 0);
			CURRENT_PAGES.put(playerKey + "_3", 0);
			CURRENT_PAGES.put(playerKey + "_4", 0);
			CURRENT_PAGES.put(playerKey + "_5", 0);
			showRelicMenu(activeChar, target);
		}
		else if (command.startsWith("admin_relic_page "))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			int grade = Integer.parseInt(st.nextToken());
			String direction = st.nextToken(); // "next" or "prev"
			
			String playerKey = activeChar.getName() + "_" + grade;
			int currentPage = CURRENT_PAGES.getOrDefault(playerKey, 0);
			
			// Get total pages for this grade (1 relic per page).
			List<RelicDataHolder> gradeRelics = getRelicsByGrade(grade);
			int maxPages = gradeRelics.size();
			
			if ("next".equals(direction) && (currentPage < (maxPages - 1)))
			{
				currentPage++;
			}
			else if ("prev".equals(direction) && (currentPage > 0))
			{
				currentPage--;
			}
			
			CURRENT_PAGES.put(playerKey, currentPage);
			showRelicMenu(activeChar, target);
		}
		else if (command.startsWith("admin_give_relic "))
		{
			// Parse the command - format: "admin_give_relic relicId count"
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (st.countTokens() < 2)
			{
				activeChar.sendMessage("Usage: //give_relic <relicId> <count>");
				return false;
			}
			
			int relicId = 0;
			int count = 0;
			
			try
			{
				relicId = Integer.parseInt(st.nextToken());
				count = Integer.parseInt(st.nextToken());
			}
			catch (Exception e)
			{
				activeChar.sendMessage("Error parsing command: " + e.getMessage());
				return false;
			}
			
			if ((relicId <= 0) || (count <= 0))
			{
				activeChar.sendMessage("Invalid relic ID or count!");
				return false;
			}
			
			RelicDataHolder relic = RelicData.getInstance().getRelic(relicId);
			if (relic == null)
			{
				activeChar.sendMessage("Invalid relic ID: " + relicId);
				return false;
			}
			
			// Give the relic to target.
			final List<PlayerRelicData> updatedRelics = new ArrayList<>();
			final RelicList relicList = target.getRelicList();
			Collection<PlayerRelicData> storedRelics = relicList.getRelics();
			
			PlayerRelicData existingRelic = null;
			for (PlayerRelicData relicData : storedRelics)
			{
				if (relicData.getRelicId() == relicId)
				{
					existingRelic = relicData;
					break;
				}
			}
			
			if (existingRelic != null)
			{
				existingRelic.setRelicCount(existingRelic.getRelicCount() + count);
				updatedRelics.add(existingRelic);
			}
			else
			{
				PlayerRelicData newRelic = new PlayerRelicData(relicId, 0, count, 0, 0);
				storedRelics.add(newRelic);
				updatedRelics.add(newRelic);
			}
			
			relicList.store();
			target.sendPacket(new ExRelicsList(target)); // Update confirmed relic list relics count.
			
			if (!updatedRelics.isEmpty())
			{
				target.sendPacket(new ExRelicsUpdateList(updatedRelics));
			}
			
			if (!relicList.isRegistered(relicId, 0))
			{
				// Auto-Add to relic collections on summon.
				target.sendPacket(new ExRelicsCollectionUpdate(target, relicId, 0)); // Update collection list.
			}
			
			String relicName = sanitizeText(relic.getName());
			if ((relicName == null) || relicName.trim().isEmpty())
			{
				relicName = "Relic #" + relicId;
			}
			
			activeChar.sendMessage("You gave " + count + "x " + relicName + " to " + target.getName());
			if (activeChar != target)
			{
				target.sendMessage("Admin gave you " + count + "x " + relicName);
			}
			
			showRelicMenu(activeChar, target);
		}
		
		return true;
	}
	
	/**
	 * Retrieves a list of relics based on the provided grade.<br>
	 * This method converts the integer to a {@code RelicGrade}.<br>
	 * It returns an empty list if the grade is invalid.<br>
	 * The resulting list is sorted by relic ID.
	 * @param grade The numeric grade used to filter the relics.
	 * @return A sorted {@code List} of {@code RelicDataHolder} objects.
	 */
	private List<RelicDataHolder> getRelicsByGrade(int grade)
	{
		final RelicGrade relicGrade = getRelicGrade(grade);
		if (relicGrade == null)
		{
			return new ArrayList<>();
		}
		
		final List<RelicDataHolder> result = new ArrayList<>(RelicData.getInstance().getRelicsByGrade(relicGrade));
		result.sort((a, b) -> Integer.compare(a.getRelicId(), b.getRelicId()));
		return result;
	}
	
	/**
	 * Converts a numeric grade into its corresponding {@code RelicGrade} enum value.<br>
	 * This method maps specific integers to the correct relic tier.
	 * @param grade The integer value representing the relic grade.
	 * @return The matching {@code RelicGrade} or {@code null} if the input is invalid.
	 */
	private RelicGrade getRelicGrade(int grade)
	{
		switch (grade)
		{
			case 1:
			{
				return RelicGrade.NOGRADE;
			}
			case 2:
			{
				return RelicGrade.DGRADE;
			}
			case 3:
			{
				return RelicGrade.CGRADE;
			}
			case 4:
			{
				return RelicGrade.BGRADE;
			}
			case 5:
			{
				return RelicGrade.AGRADE;
			}
			default:
			{
				return null;
			}
		}
	}
	
	/**
	 * Generates the HTML content for a specific relic grade section.<br>
	 * This method handles pagination and displays one relic per page.<br>
	 * It includes navigation buttons and an input field to give relics.
	 * @param admin The {@code Player} who is viewing the menu.
	 * @param grade The integer representing the relic grade to display.
	 * @return A {@code String} containing the formatted HTML table.
	 */
	private String generateRelicSectionByGrade(Player admin, int grade)
	{
		StringBuilder html = new StringBuilder();
		String playerKey = admin.getName() + "_" + grade;
		int currentPage = CURRENT_PAGES.getOrDefault(playerKey, 0);
		
		// Get relics for this grade
		List<RelicDataHolder> gradeRelics = getRelicsByGrade(grade);
		
		// Pagination - 1 relic per page
		final int totalPages = gradeRelics.size();
		
		// Make sure currentPage is valid
		if (currentPage >= totalPages)
		{
			currentPage = 0;
			CURRENT_PAGES.put(playerKey, 0);
		}
		
		html.append("<table width=270 bgcolor=282828 cellpadding=0 cellspacing=1>");
		
		// Header with pagination
		html.append("<tr><td colspan=3 align=center>");
		html.append("<table width=155><tr>");
		
		// Previous button
		if (currentPage > 0)
		{
			html.append("<td width=30><button value=\"<\" action=\"bypass -h admin_relic_page ").append(grade).append(" prev\" width=25 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		}
		else
		{
			html.append("<td width=30></td>");
		}
		
		// Grade name and page indicator
		html.append("<td align=center><font color=\"LEVEL\">").append(getGradeName(grade)).append(" (").append(currentPage + 1).append('/').append(totalPages).append(")</font></td>");
		
		// Next button
		if (currentPage < (totalPages - 1))
		{
			html.append("<td width=30><button value=\">\" action=\"bypass -h admin_relic_page ").append(grade).append(" next\" width=25 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
		}
		else
		{
			html.append("<td width=30></td>");
		}
		
		html.append("</tr></table>");
		html.append("</td></tr>");
		
		// Display current relic (1 per page)
		if (gradeRelics.isEmpty())
		{
			html.append("<tr><td colspan=3 align=center><font color=\"FF0000\">No relics available</font></td></tr>");
		}
		else if (currentPage < totalPages)
		{
			RelicDataHolder relic = gradeRelics.get(currentPage);
			int relicId = relic.getRelicId();
			String relicName = sanitizeHtmlText(relic.getName());
			
			// Fallback to ID if name is empty
			if ((relicName == null) || relicName.trim().isEmpty())
			{
				relicName = "Relic #" + relicId;
			}
			
			html.append("<tr>");
			html.append("<td width=160 align=center><font color=\"LEVEL\">").append(relicName).append("</font></td>");
			html.append("<td width=50><edit var=\"countGrade").append(grade).append("\" width=45 height=15 type=\"number\"></td>");
			html.append("<td width=60><button value=\"Give\" action=\"bypass -h admin_give_relic ").append(relicId).append(" $countGrade").append(grade).append("\" width=55 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			html.append("</tr>");
		}
		
		html.append("</table>");
		
		return html.toString();
	}
	
	/**
	 * Displays the admin relic menu to a specific player.<br>
	 * It loads an HTML template and replaces placeholders with dynamic data.<br>
	 * The final message is sent as an {@code NpcHtmlMessage}.
	 * @param activeChar The player who will view the menu.
	 * @param target The player whose name will be displayed in the menu.
	 */
	private void showRelicMenu(Player activeChar, Player target)
	{
		String content = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/relics_menu.htm");
		
		content = content.replace("%targetName%", target.getName());
		content = content.replace("%relicSectionGrade1%", generateRelicSectionByGrade(activeChar, 1));
		content = content.replace("%relicSectionGrade2%", generateRelicSectionByGrade(activeChar, 2));
		content = content.replace("%relicSectionGrade3%", generateRelicSectionByGrade(activeChar, 3));
		content = content.replace("%relicSectionGrade4%", generateRelicSectionByGrade(activeChar, 4));
		content = content.replace("%relicSectionGrade5%", generateRelicSectionByGrade(activeChar, 5));
		
		activeChar.sendPacket(new NpcHtmlMessage(0, 1, content));
	}
	
	/**
	 * Converts a numeric grade into its corresponding display name.<br>
	 * This method maps specific integers to strings like {@code D-Grade}.<br>
	 * It returns {@code Unknown Grade} if the input does not match known values.
	 * @param grade The integer value representing the relic grade.
	 * @return The string name of the grade.
	 */
	private String getGradeName(int grade)
	{
		switch (grade)
		{
			case 1:
			{
				return "NoGrade";
			}
			case 2:
			{
				return "D-Grade";
			}
			case 3:
			{
				return "C-Grade";
			}
			case 4:
			{
				return "B-Grade";
			}
			case 5:
			{
				return "A-Grade";
			}
			default:
			{
				return "Unknown Grade";
			}
		}
	}
	
	/**
	 * Cleans the input string to prevent HTML injection.<br>
	 * It replaces special characters and removes invisible control characters.<br>
	 * The method also trims whitespace from both ends of the result.
	 * @param text The raw string to be cleaned.
	 * @return A sanitized version of the input string or an empty string if {@code null}.
	 */
	private String sanitizeHtmlText(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return "";
		}
		
		// Replace HTML special characters
		text = text.replace("&", "&amp;");
		text = text.replace("<", "&lt;");
		text = text.replace(">", "&gt;");
		text = text.replace("\"", "&quot;");
		
		// Remove control characters and other problematic characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		
		// Remove zero-width spaces and other invisible characters
		text = text.replaceAll("[\u200B-\u200D\uFEFF]", "");
		
		return text.trim();
	}
	
	/**
	 * Cleans the input string by removing hidden or control characters.<br>
	 * It also removes leading and trailing whitespace from the result.<br>
	 * If the input is {@code null} or empty, it returns an empty string.
	 * @param text The raw string to be cleaned.
	 * @return The sanitized version of the input string.
	 */
	private String sanitizeText(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return "";
		}
		
		// Remove control characters and other problematic characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		
		// Remove zero-width spaces and other invisible characters
		text = text.replaceAll("[\u200B-\u200D\uFEFF]", "");
		
		return text.trim();
	}
	
	/**
	 * Retrieves the list of available admin relic commands.<br>
	 * This method returns all registered command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}