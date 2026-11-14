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
package org.l2jmobius.gameserver.mechanics.html;

/**
 * Represents the result of an HTML page request.<br>
 * It holds the data needed to render a specific page or response.<br>
 * This class is used by {@code PageResult} to manage web-related content.
 * @author UnAfraid
 */
public class PageResult
{
	private final int _pages;
	private final StringBuilder _pagerTemplate;
	private final StringBuilder _bodyTemplate;
	
	/**
	 * Creates a new {@code PageResult} object.<br>
	 * This constructor initializes the page count and templates.
	 * @param pages The total number of pages.
	 * @param pagerTemplate The template used for the pager component.
	 * @param bodyTemplate The template used for the main content body.
	 */
	public PageResult(int pages, StringBuilder pagerTemplate, StringBuilder bodyTemplate)
	{
		_pages = pages;
		_pagerTemplate = pagerTemplate;
		_bodyTemplate = bodyTemplate;
	}
	
	/**
	 * Retrieves the total number of pages.<br>
	 * This value is stored in the {@code PageResult} instance.
	 * @return The total count of pages as an {@code int}.
	 */
	public int getPages()
	{
		return _pages;
	}
	
	/**
	 * Retrieves the template used for the page navigation bar.<br>
	 * This is returned as a {@code StringBuilder}.
	 * @return The {@code StringBuilder} containing the pager template.
	 */
	public StringBuilder getPagerTemplate()
	{
		return _pagerTemplate;
	}
	
	/**
	 * Retrieves the template used for the main body of the page.<br>
	 * This returns a {@code StringBuilder} object.
	 * @return The {@code StringBuilder} containing the body template.
	 */
	public StringBuilder getBodyTemplate()
	{
		return _bodyTemplate;
	}
}
