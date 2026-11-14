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
package org.l2jmobius.gameserver.mechanics.html.pagehandlers;

import org.l2jmobius.gameserver.mechanics.html.IBypassFormatter;
import org.l2jmobius.gameserver.mechanics.html.IHtmlStyle;
import org.l2jmobius.gameserver.mechanics.html.IPageHandler;

/**
 * This class handles the creation of pagination controls for HTML pages.<br>
 * It generates navigation links such as {@code <<}, {@code <}, {@code >}, and {@code >>}.<br>
 * It implements the {@code IPageHandler} interface to manage page transitions.
 * @author UnAfraid
 */
public class NextPrevPageHandler implements IPageHandler
{
	public static final NextPrevPageHandler INSTANCE = new NextPrevPageHandler();
	
	/**
	 * Adds pagination navigation links to the provided {@code StringBuilder}.<br>
	 * It generates symbols for first, previous, next, and last pages.<br>
	 * The method also displays the current page number relative to the total pages.
	 * @param bypass The bypass string used for formatting.
	 * @param currentPage The current page index starting from 0.
	 * @param pages The total number of pages available.
	 * @param sb The {@code StringBuilder} where the HTML content will be appended.
	 * @param bypassFormatter The formatter used to process the bypass string.
	 * @param style The style object used to apply CSS and separators.
	 */
	@Override
	public void apply(String bypass, int currentPage, int pages, StringBuilder sb, IBypassFormatter bypassFormatter, IHtmlStyle style)
	{
		// Beginning
		sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, 0), "<<", (currentPage - 1) < 0));
		
		// Separator
		sb.append(style.applySeparator());
		
		// Previous
		sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, currentPage - 1), "<", currentPage <= 0));
		sb.append(style.applySeparator());
		sb.append(String.format("<td align=\"center\">Page: %d/%d</td>", currentPage + 1, pages + 1));
		sb.append(style.applySeparator());
		
		// Next
		sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, currentPage + 1), ">", currentPage >= pages));
		
		// Separator
		sb.append(style.applySeparator());
		
		// End
		sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, pages), ">>", (currentPage + 1) > pages));
	}
}
