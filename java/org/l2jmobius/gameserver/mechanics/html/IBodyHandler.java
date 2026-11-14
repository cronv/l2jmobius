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

import java.util.Collection;

/**
 * Defines a functional interface for handling the body content of an {@code HTML} element.<br>
 * It allows for custom logic to be applied when processing specific parts of the game's web-based UI.
 * @author UnAfraid
 * @param <T>
 */
@FunctionalInterface
public interface IBodyHandler<T>
{
	void apply(int pages, T type, StringBuilder sb);
	
	/**
	 * This method processes a collection of elements to build an HTML body.<br>
	 * It skips the initial items based on the {@code start} index.<br>
	 * It limits the total number of processed items using {@code elementsPerPage}.<br>
	 * The results are appended to the provided {@code StringBuilder}.
	 * @param elements The collection of items to process.
	 * @param pages The total number of pages for pagination.
	 * @param start The index at which to begin processing elements.
	 * @param elementsPerPage The maximum number of elements to include in one page.
	 * @param sb The {@code StringBuilder} where the output will be stored.
	 */
	default void create(Collection<T> elements, int pages, int start, int elementsPerPage, StringBuilder sb)
	{
		int i = 0;
		for (T element : elements)
		{
			if (i++ < start)
			{
				continue;
			}
			
			apply(pages, element, sb);
			
			if (i >= (elementsPerPage + start))
			{
				break;
			}
		}
	}
}
