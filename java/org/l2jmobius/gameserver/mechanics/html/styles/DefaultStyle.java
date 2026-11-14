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
package org.l2jmobius.gameserver.mechanics.html.styles;

import org.l2jmobius.gameserver.mechanics.html.IHtmlStyle;

/**
 * Provides the default styling for HTML elements within the game server.<br>
 * This class serves as the base implementation of {@code IHtmlStyle}.
 * @author UnAfraid
 */
public class DefaultStyle implements IHtmlStyle
{
	private static final String DEFAULT_PAGE_LINK_FORMAT = "<td><a action=\"%s\">%s</a></td>";
	private static final String DEFAULT_PAGE_TEXT_FORMAT = "<td>%s</td>";
	private static final String DEFAULT_PAGER_SEPARATOR = "<td align=center> | </td>";
	
	public static final DefaultStyle INSTANCE = new DefaultStyle();
	
	/**
	 * This method generates a formatted HTML string for a specific item.<br>
	 * It decides whether to show a link or plain text based on the {@code isEnabled} flag.
	 * @param bypass The URL path used if the feature is disabled.
	 * @param name The display name of the item.
	 * @param isEnabled A boolean indicating if the feature should be shown as plain text.
	 * @return A formatted HTML string representing the item.
	 */
	@Override
	public String applyBypass(String bypass, String name, boolean isEnabled)
	{
		if (isEnabled)
		{
			return String.format(DEFAULT_PAGE_TEXT_FORMAT, name);
		}
		
		return String.format(DEFAULT_PAGE_LINK_FORMAT, bypass, name);
	}
	
	/**
	 * Returns the default separator for pagination.<br>
	 * This method provides a formatted string used to separate page links.
	 * @return The {@code String} representing the default separator.
	 */
	@Override
	public String applySeparator()
	{
		return DEFAULT_PAGER_SEPARATOR;
	}
}
