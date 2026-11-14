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
 * Handles the generation of pagination links for HTML content.<br>
 * It creates a pager that displays specific page numbers, such as {@code 1 2 3 | 9 10 | 998 | 999}.
 * @author UnAfraid
 */
public class DefaultPageHandler implements IPageHandler
{
	public static final DefaultPageHandler INSTANCE = new DefaultPageHandler(2);
	protected final int _pagesOffset;
	
	/**
	 * Initializes a new instance of the {@code DefaultPageHandler}.<br>
	 * Sets the starting offset for page navigation.
	 * @param pagesOffset The number of pages to skip before showing links.
	 */
	public DefaultPageHandler(int pagesOffset)
	{
		_pagesOffset = pagesOffset;
	}
	
	/**
	 * This method generates HTML pagination links.<br>
	 * It appends the formatted page numbers to a {@code StringBuilder}.<br>
	 * The output includes the current page, nearby pages, and the first or last pages.
	 * @param bypass The prefix string for the bypass command.
	 * @param currentPage The index of the page currently being viewed.
	 * @param pages The index of the last available page.
	 * @param sb The {@code StringBuilder} where the HTML content will be added.
	 * @param bypassFormatter The formatter used to create the bypass text.
	 * @param style The style object used to apply CSS and separators.
	 */
	@Override
	public void apply(String bypass, int currentPage, int pages, StringBuilder sb, IBypassFormatter bypassFormatter, IHtmlStyle style)
	{
		// The last page index is given, while the loops below work with the page count.
		final int pageCount = pages + 1;
		final int pagerStart = Math.max(currentPage - _pagesOffset, 0);
		final int pagerFinish = Math.min(currentPage + _pagesOffset + 1, pageCount);
		
		// Show the initial pages in case we are in the middle or at the end.
		if (pagerStart > _pagesOffset)
		{
			for (int i = 0; i < _pagesOffset; i++)
			{
				sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, i), String.valueOf(i + 1), currentPage == i));
			}
			
			// Separator
			sb.append(style.applySeparator());
		}
		
		// Show current pages
		for (int i = pagerStart; i < pagerFinish; i++)
		{
			sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, i), String.valueOf(i + 1), currentPage == i));
		}
		
		// Show the last pages.
		if (pageCount > pagerFinish)
		{
			// Separator
			sb.append(style.applySeparator());
			
			for (int i = pageCount - _pagesOffset; i < pageCount; i++)
			{
				sb.append(style.applyBypass(bypassFormatter.formatBypass(bypass, i), String.valueOf(i + 1), currentPage == i));
			}
		}
	}
}
