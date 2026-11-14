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
 * Provides the default implementation for formatting HTML bypasses.<br>
 * This class handles standard text processing when no specific rules are applied.
 * @author UnAfraid
 */
public class DefaultFormatter implements IBypassFormatter
{
	public static final DefaultFormatter INSTANCE = new DefaultFormatter();
	
	/**
	 * Formats a bypass string with its corresponding page number.<br>
	 * This method joins the two values into a single {@code String}.
	 * @param bypass The original bypass text to be formatted.
	 * @param page The page number associated with the bypass.
	 * @return A formatted {@code String} containing both the bypass and the page.
	 */
	@Override
	public String formatBypass(String bypass, int page)
	{
		return bypass + " " + page;
	}
}
