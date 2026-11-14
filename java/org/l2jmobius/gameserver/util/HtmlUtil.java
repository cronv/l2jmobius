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
package org.l2jmobius.gameserver.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.network.serverpackets.AbstractHtmlPacket;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;

/**
 * Helpers for building and sending HTML-based game-server messages.
 * @author NosBit, Mobius
 */
public class HtmlUtil
{
	private static final Logger LOGGER = Logger.getLogger(HtmlUtil.class.getName());
	
	/**
	 * Builds the HTML for a CP gauge of the given width.
	 * @param width Gauge width in pixels.
	 * @param current Current CP value.
	 * @param max Maximum CP value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @return The gauge HTML.
	 */
	public static String getCpGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_CP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_CP_Center", 17, -13);
	}
	
	/**
	 * Builds the HTML for an HP gauge of the given width.
	 * @param width Gauge width in pixels.
	 * @param current Current HP value.
	 * @param max Maximum HP value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @return The gauge HTML.
	 */
	public static String getHpGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_HP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_HP_Center", 21, -13);
	}
	
	/**
	 * Builds the HTML for a low-HP warning gauge of the given width.
	 * @param width Gauge width in pixels.
	 * @param current Current HP value.
	 * @param max Maximum HP value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @return The gauge HTML.
	 */
	public static String getHpWarnGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_HPWarn_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_HPWarn_Center", 17, -13);
	}
	
	/**
	 * Builds the HTML for an HP fill gauge of the given width.
	 * @param width Gauge width in pixels.
	 * @param current Current HP value.
	 * @param max Maximum HP value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @return The gauge HTML.
	 */
	public static String getHpFillGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_HPFill_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_HPFill_Center", 17, -13);
	}
	
	/**
	 * Builds the HTML for an MP gauge of the given width.
	 * @param width Gauge width in pixels.
	 * @param current Current MP value.
	 * @param max Maximum MP value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @return The gauge HTML.
	 */
	public static String getMpGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_MP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_MP_Center", 17, -13);
	}
	
	/**
	 * Builds the HTML for an experience gauge of the given width.
	 * @param width Gauge width in pixels.
	 * @param current Current experience value.
	 * @param max Maximum experience value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @return The gauge HTML.
	 */
	public static String getExpGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_EXP_bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_EXP_Center", 17, -13);
	}
	
	/**
	 * Builds the HTML for a food gauge of the given width.
	 * @param width Gauge width in pixels.
	 * @param current Current food value.
	 * @param max Maximum food value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @return The gauge HTML.
	 */
	public static String getFoodGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_Food_Bg_Center", "L2UI_CT1.Gauges.Gauge_DF_Large_Food_Center", 17, -13);
	}
	
	/**
	 * Builds the HTML for a weight gauge, deriving the style level from the current ratio.
	 * @param width Gauge width in pixels.
	 * @param current Current weight value.
	 * @param max Maximum weight value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @return The gauge HTML.
	 */
	public static String getWeightGauge(int width, long current, long max, boolean displayAsPercentage)
	{
		return getWeightGauge(width, current, max, displayAsPercentage, MathUtil.scaleToRange(current, 0, max, 1, 5));
	}
	
	/**
	 * Builds the HTML for a weight gauge using an explicit style level.
	 * @param width Gauge width in pixels.
	 * @param current Current weight value.
	 * @param max Maximum weight value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @param level Style level selecting the gauge appearance.
	 * @return The gauge HTML.
	 */
	public static String getWeightGauge(int width, long current, long max, boolean displayAsPercentage, long level)
	{
		return getGauge(width, current, max, displayAsPercentage, "L2UI_CT1.Gauges.Gauge_DF_Large_Weight_bg_Center" + level, "L2UI_CT1.Gauges.Gauge_DF_Large_Weight_Center" + level, 17, -13);
	}
	
	/**
	 * Builds the HTML table for a progress gauge with the given images and dimensions.
	 * @param width Gauge width in pixels.
	 * @param currentValue Current progress value, clamped to {@code max}.
	 * @param max Maximum progress value.
	 * @param displayAsPercentage {@code true} to show a percentage, {@code false} to show a fraction.
	 * @param backgroundImage Background image path.
	 * @param image Foreground fill image path.
	 * @param imageHeight Fill image height in pixels.
	 * @param top Cell spacing applied to the text table.
	 * @return The gauge HTML.
	 */
	private static String getGauge(int width, long currentValue, long max, boolean displayAsPercentage, String backgroundImage, String image, long imageHeight, long top)
	{
		final long current = Math.min(currentValue, max);
		final StringBuilder sb = new StringBuilder();
		sb.append("<table width=");
		sb.append(width);
		sb.append(" cellpadding=0 cellspacing=0>");
		sb.append("<tr>");
		sb.append("<td background=\"");
		sb.append(backgroundImage);
		sb.append("\">");
		sb.append("<img src=\"");
		sb.append(image);
		sb.append("\" width=");
		sb.append((long) (((double) current / max) * width));
		sb.append(" height=");
		sb.append(imageHeight);
		sb.append('>');
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("<tr>");
		sb.append("<td align=center>");
		sb.append("<table cellpadding=0 cellspacing=");
		sb.append(top);
		sb.append('>');
		sb.append("<tr>");
		sb.append("<td>");
		if (displayAsPercentage)
		{
			sb.append("<table cellpadding=0 cellspacing=2>");
			sb.append("<tr><td>");
			sb.append(String.format("%.2f%%", ((double) current / max) * 100));
			sb.append("</td></tr>");
			sb.append("</table>");
		}
		else
		{
			final int tdWidth = (width - 10) / 2;
			sb.append("<table cellpadding=0 cellspacing=0>");
			sb.append("<tr>");
			sb.append("<td width=");
			sb.append(tdWidth);
			sb.append(" align=right>");
			sb.append(current);
			sb.append("</td>");
			sb.append("<td width=10 align=center>/</td>");
			sb.append("<td width=");
			sb.append(tdWidth);
			sb.append('>');
			sb.append(max);
			sb.append("</td>");
			sb.append("</tr>");
			sb.append("</table>");
		}
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("</table>");
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Parses the HTML for bypass actions and registers them on the player under the given scope.
	 * @param player Player to register the actions on.
	 * @param scope Scope under which the actions are registered.
	 * @param html HTML content to parse.
	 */
	private static void buildHtmlBypassCache(Player player, HtmlActionScope scope, String html)
	{
		final String htmlLower = html.toLowerCase(Locale.ENGLISH);
		int bypassEnd = 0;
		int bypassStart = htmlLower.indexOf("=\"bypass ", bypassEnd);
		int bypassStartEnd;
		while (bypassStart != -1)
		{
			bypassStartEnd = bypassStart + 9;
			bypassEnd = htmlLower.indexOf("\"", bypassStartEnd);
			if (bypassEnd == -1)
			{
				break;
			}
			
			final int hParamPos = htmlLower.indexOf("-h ", bypassStartEnd);
			String bypass;
			if ((hParamPos != -1) && (hParamPos < bypassEnd))
			{
				bypass = html.substring(hParamPos + 3, bypassEnd).trim();
			}
			else
			{
				bypass = html.substring(bypassStartEnd, bypassEnd).trim();
			}
			
			final int firstParameterStart = bypass.indexOf(AbstractHtmlPacket.VAR_PARAM_START_CHAR);
			if (firstParameterStart != -1)
			{
				bypass = bypass.substring(0, firstParameterStart + 1);
			}
			
			if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG)
			{
				LOGGER.info("Cached html bypass(" + scope + "): '" + bypass + "'");
			}
			
			player.addHtmlAction(scope, bypass);
			bypassStart = htmlLower.indexOf("=\"bypass ", bypassEnd);
		}
	}
	
	/**
	 * Parses the HTML for link actions and registers valid ones on the player under the given scope.
	 * @param player Player to register the actions on.
	 * @param scope Scope under which the actions are registered.
	 * @param html HTML content to parse.
	 */
	private static void buildHtmlLinkCache(Player player, HtmlActionScope scope, String html)
	{
		final String htmlLower = html.toLowerCase(Locale.ENGLISH);
		int linkEnd = 0;
		int linkStart = htmlLower.indexOf("=\"link ", linkEnd);
		int linkStartEnd;
		while (linkStart != -1)
		{
			linkStartEnd = linkStart + 7;
			linkEnd = htmlLower.indexOf("\"", linkStartEnd);
			if (linkEnd == -1)
			{
				break;
			}
			
			final String htmlLink = html.substring(linkStartEnd, linkEnd).trim();
			if (htmlLink.isEmpty())
			{
				LOGGER.warning("Html link path is empty!");
				continue;
			}
			
			if (htmlLink.contains(".."))
			{
				LOGGER.warning("Html link path is invalid: " + htmlLink);
				continue;
			}
			
			if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG)
			{
				LOGGER.info("Cached html link(" + scope + "): '" + htmlLink + "'");
			}
			
			// Let's keep an action cache with "link " lowercase literal kept.
			player.addHtmlAction(scope, "link " + htmlLink);
			linkStart = htmlLower.indexOf("=\"link ", linkEnd);
		}
	}
	
	/**
	 * Sets the action origin object id and populates the bypass and link caches for the player.
	 * @param player Player whose action cache is built.
	 * @param scope Scope under which the actions are registered.
	 * @param npcObjId Origin NPC object id.
	 * @param html HTML content to cache.
	 * @throws IllegalArgumentException If any argument is null or {@code npcObjId} is negative.
	 */
	public static void buildHtmlActionCache(Player player, HtmlActionScope scope, int npcObjId, String html)
	{
		if ((player == null) || (scope == null) || (npcObjId < 0) || (html == null))
		{
			throw new IllegalArgumentException();
		}
		
		if (DevelopmentConfig.HTML_ACTION_CACHE_DEBUG)
		{
			LOGGER.info("Set html action npc(" + scope + "): " + npcObjId);
		}
		
		player.setHtmlActionOriginObjectId(scope, npcObjId);
		buildHtmlBypassCache(player, scope, html);
		buildHtmlLinkCache(player, scope, html);
	}
	
	/**
	 * Wraps the HTML in an {@code NpcHtmlMessage} and sends it to the player.
	 * @param player Recipient player.
	 * @param html HTML content to send.
	 */
	public static void sendHtml(Player player, String html)
	{
		final NpcHtmlMessage message = new NpcHtmlMessage();
		message.setHtml(html);
		player.sendPacket(message);
	}
	
	/**
	 * Sends community-board HTML to the player.
	 * @param player Recipient player.
	 * @param html HTML content to send.
	 */
	public static void sendCBHtml(Player player, String html)
	{
		sendCBHtml(player, html, 0);
	}
	
	/**
	 * Sends community-board HTML to the player for the given NPC.
	 * @param player Recipient player.
	 * @param html HTML content to send.
	 * @param npcObjId Associated NPC object id.
	 */
	public static void sendCBHtml(Player player, String html, int npcObjId)
	{
		sendCBHtml(player, html, null, npcObjId);
	}
	
	/**
	 * Sends community-board HTML to the player with multi-edit content.
	 * @param player Recipient player.
	 * @param html HTML content to send.
	 * @param fillMultiEdit Multi-edit fill content.
	 */
	public static void sendCBHtml(Player player, String html, String fillMultiEdit)
	{
		sendCBHtml(player, html, fillMultiEdit, 0);
	}
	
	/**
	 * Sends community-board HTML to the player, splitting long content across multiple {@code ShowBoard} packets.
	 * @param player Recipient player.
	 * @param html HTML content to send.
	 * @param fillMultiEdit Optional multi-edit fill content.
	 * @param npcObjId Associated NPC object id, or negative for none.
	 */
	public static void sendCBHtml(Player player, String html, String fillMultiEdit, int npcObjId)
	{
		if ((player == null) || (html == null))
		{
			return;
		}
		
		player.clearHtmlActions(HtmlActionScope.COMM_BOARD_HTML);
		
		if (npcObjId > -1)
		{
			buildHtmlActionCache(player, HtmlActionScope.COMM_BOARD_HTML, npcObjId, html);
		}
		
		if (fillMultiEdit != null)
		{
			player.sendPacket(new ShowBoard(html, "1001"));
			fillMultiEditContent(player, fillMultiEdit);
		}
		else if (html.length() < 16250)
		{
			player.sendPacket(new ShowBoard(html, "101"));
			player.sendPacket(new ShowBoard(null, "102"));
			player.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < (16250 * 2))
		{
			player.sendPacket(new ShowBoard(html.substring(0, 16250), "101"));
			player.sendPacket(new ShowBoard(html.substring(16250), "102"));
			player.sendPacket(new ShowBoard(null, "103"));
		}
		else if (html.length() < (16250 * 3))
		{
			player.sendPacket(new ShowBoard(html.substring(0, 16250), "101"));
			player.sendPacket(new ShowBoard(html.substring(16250, 16250 * 2), "102"));
			player.sendPacket(new ShowBoard(html.substring(16250 * 2), "103"));
		}
		else
		{
			player.sendPacket(new ShowBoard("<html><body><br><center>Error: HTML was too long!</center></body></html>", "101"));
			player.sendPacket(new ShowBoard(null, "102"));
			player.sendPacket(new ShowBoard(null, "103"));
		}
	}
	
	/**
	 * Sends a {@code ShowBoard} packet populating the multi-edit board with the given text.
	 * @param player Recipient player.
	 * @param text Board text whose {@code <br>} tags are replaced with line separators.
	 */
	public static void fillMultiEditContent(Player player, String text)
	{
		player.sendPacket(new ShowBoard(Arrays.asList("0", "0", "0", "0", "0", "0", player.getName(), Integer.toString(player.getObjectId()), player.getAccountName(), "9", " ", " ", text.replace("<br>", System.lineSeparator()), "0", "0", "0", "0")));
	}
	
	/**
	 * Computes the number of pages needed to display the items.
	 * @param totalItems Total item count.
	 * @param itemsPerPage Maximum items per page.
	 * @return The page count, or {@code 0} when {@code itemsPerPage} is not positive.
	 */
	public static int countPageNumber(int totalItems, int itemsPerPage)
	{
		if (itemsPerPage <= 0)
		{
			return 0;
		}
		
		return ((totalItems + itemsPerPage) - 1) / itemsPerPage;
	}
}
