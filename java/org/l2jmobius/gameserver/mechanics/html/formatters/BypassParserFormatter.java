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
package org.l2jmobius.gameserver.mechanics.html.formatters;

import org.l2jmobius.gameserver.mechanics.html.IBypassFormatter;

/**
 * This class handles the formatting of bypass commands within HTML content.<br>
 * It parses and processes specific bypass strings to ensure they are displayed correctly.
 * @author UnAfraid
 */
public class BypassParserFormatter implements IBypassFormatter
{
	public static final BypassParserFormatter INSTANCE = new BypassParserFormatter();
	
	/**
	 * This method formats a bypass string with its corresponding page number.<br>
	 * It combines the {@code bypass} text and the {@code page} integer into one string.
	 * @param bypass The original bypass message to format.
	 * @param page The current page number of the bypass.
	 * @return A formatted string containing both the bypass and the page.
	 */
	@Override
	public String formatBypass(String bypass, int page)
	{
		return bypass + " page=" + page;
	}
}
