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

import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;

import org.l2jmobius.gameserver.mechanics.html.formatters.DefaultFormatter;
import org.l2jmobius.gameserver.mechanics.html.pagehandlers.DefaultPageHandler;
import org.l2jmobius.gameserver.mechanics.html.styles.DefaultStyle;

/**
 * Provides a fluent API for constructing and configuring HTML pages.<br>
 * It simplifies the creation of complex page structures by chaining various configuration methods.
 * @author UnAfraid
 * @param <T>
 */
public class PageBuilder<T>
{
	private final Collection<T> _elements;
	private final int _elementsPerPage;
	private final String _bypass;
	private int _currentPage = 0;
	private IPageHandler _pageHandler = DefaultPageHandler.INSTANCE;
	private IBypassFormatter _formatter = DefaultFormatter.INSTANCE;
	private IHtmlStyle _style = DefaultStyle.INSTANCE;
	private IBodyHandler<T> _bodyHandler;
	
	/**
	 * Creates a new instance of {@code PageBuilder}.<br>
	 * This constructor initializes the internal data for page construction.
	 * @param elements The collection of items to be displayed on the pages.
	 * @param elementsPerPage The maximum number of items allowed per page.
	 * @param bypass A string used to determine if certain formatting rules should be skipped.
	 */
	private PageBuilder(Collection<T> elements, int elementsPerPage, String bypass)
	{
		_elements = elements;
		_elementsPerPage = elementsPerPage;
		_bypass = bypass;
	}
	
	/**
	 * Sets the current page number for the builder.<br>
	 * The value will be at least {@code 0}.
	 * @param currentPage The page number to set.
	 * @return This {@code PageBuilder} instance.
	 */
	public PageBuilder<T> currentPage(int currentPage)
	{
		_currentPage = Math.max(currentPage, 0);
		return this;
	}
	
	/**
	 * Sets the handler for processing the body of the page.<br>
	 * This method assigns a custom {@code IBodyHandler} to the builder.<br>
	 * The provided {@code bodyHandler} must not be {@code null}.
	 * @param bodyHandler The handler used to process the body content.
	 * @return The current {@code PageBuilder} instance for chaining.
	 */
	public PageBuilder<T> bodyHandler(IBodyHandler<T> bodyHandler)
	{
		Objects.requireNonNull(bodyHandler, "Body Handler cannot be null!");
		_bodyHandler = bodyHandler;
		return this;
	}
	
	/**
	 * Sets the custom handler for managing pages.<br>
	 * This method updates the internal {@code IPageHandler} instance.<br>
	 * It returns the current {@code PageBuilder} object to allow for method chaining.
	 * @param pageHandler The new {@code IPageHandler} implementation to use.
	 * @return The current {@code PageBuilder} instance.
	 */
	public PageBuilder<T> pageHandler(IPageHandler pageHandler)
	{
		Objects.requireNonNull(pageHandler, "Page Handler cannot be null!");
		_pageHandler = pageHandler;
		return this;
	}
	
	/**
	 * Sets the custom formatter for handling bypasses.<br>
	 * This method updates the internal {@code _formatter} field.<br>
	 * It returns the current {@code PageBuilder} instance to allow for method chaining.
	 * @param formatter The {@code I BypassFormatter} to use for formatting.
	 * @return The current {@code PageBuilder} instance.
	 */
	public PageBuilder<T> formatter(IBypassFormatter formatter)
	{
		Objects.requireNonNull(formatter, "Formatter cannot be null!");
		_formatter = formatter;
		return this;
	}
	
	/**
	 * Sets the visual style for the page.<br>
	 * This method updates the internal {@code IHtmlStyle} object.<br>
	 * It returns the current {@code PageBuilder} instance to allow for method chaining.
	 * @param style The new {@code IHtmlStyle} to apply to the page.
	 * @return The current {@code PageBuilder} instance.
	 */
	public PageBuilder<T> style(IHtmlStyle style)
	{
		Objects.requireNonNull(style, "Style cannot be null!");
		_style = style;
		return this;
	}
	
	/**
	 * Constructs the final {@code PageResult} based on current settings.<br>
	 * It calculates total pages and applies the pagination template.<br>
	 * The method ensures the current page is within valid bounds.<br>
	 * It uses the body handler to generate the content string.
	 * @return A new {@code PageResult} containing the generated HTML components.
	 */
	public PageResult build()
	{
		Objects.requireNonNull(_bodyHandler, "Body was not set!");
		
		final StringBuilder pagerTemplate = new StringBuilder();
		final int elementSize = _elements.size();
		final int pages = (elementSize / _elementsPerPage) + ((elementSize % _elementsPerPage) > 0 ? 1 : 0);
		if (pages > 1)
		{
			_pageHandler.apply(_bypass, _currentPage, pages - 1, pagerTemplate, _formatter, _style);
		}
		
		if (_currentPage > pages)
		{
			_currentPage = pages - 1;
		}
		
		final StringBuilder sb = new StringBuilder();
		final int start = Math.max(_elementsPerPage * _currentPage, 0);
		_bodyHandler.create(_elements, pages, start, _elementsPerPage, sb);
		
		return new PageResult(pages, pagerTemplate, sb);
	}
	
	/**
	 * Creates a new {@code PageBuilder} instance.<br>
	 * This method initializes the builder with a collection of items.<br>
	 * It sets the number of items to show per page and the bypass string.
	 * @param <T> The type of elements in the collection.
	 * @param elements The collection of items to be displayed on the pages.
	 * @param elementsPerPage The maximum number of items allowed on a single page.
	 * @param bypass A string used to identify specific bypass rules.
	 * @return A new {@code PageBuilder} instance configured with the provided values.
	 */
	public static <T> PageBuilder<T> newBuilder(Collection<T> elements, int elementsPerPage, String bypass)
	{
		return new PageBuilder<>(elements, elementsPerPage, bypass.trim());
	}
	
	/**
	 * Creates a new {@code PageBuilder} instance.<br>
	 * This method initializes the builder with an array of items.<br>
	 * It sets the number of items to show per page and the bypass string.
	 * @param <T> The type of elements in the list.
	 * @param elements An array of objects to be displayed on the pages.
	 * @param elementsPerPage The maximum number of elements allowed on a single page.
	 * @param bypass A string used to identify specific bypass rules.
	 * @return A new {@code PageBuilder} instance configured with the provided data.
	 */
	public static <T> PageBuilder<T> newBuilder(T[] elements, int elementsPerPage, String bypass)
	{
		return new PageBuilder<>(Arrays.asList(elements), elementsPerPage, bypass.trim());
	}
}
