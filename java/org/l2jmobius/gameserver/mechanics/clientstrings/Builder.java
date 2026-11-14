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
package org.l2jmobius.gameserver.mechanics.clientstrings;

import java.util.ArrayList;
import java.util.List;

/**
 * This class serves as a base builder for constructing {@code ClientString} objects.<br>
 * It provides a structured way to define and assemble various client-side string properties.
 * @author Forsaiken, Zoey76
 */
public abstract class Builder
{
	public abstract String toString(Object param);
	
	public abstract String toString(Object... params);
	
	public abstract int getIndex();
	
	/**
	 * Creates a new {@code Builder} instance from a raw string.<br>
	 * This method parses the input text to identify components.<br>
	 * It handles both plain text and special parameters.
	 * @param text The raw string to be parsed into builders.
	 * @return A new {@code Builder} object representing the parsed content.
	 */
	public static Builder newBuilder(String text)
	{
		final List<Builder> builders = new ArrayList<>();
		
		int index1 = 0;
		int index2 = 0;
		int paramId;
		int subTextLen;
		
		final char[] array = text.toCharArray();
		final int arrayLength = array.length;
		
		char c;
		char c2;
		char c3;
		for (; index1 < arrayLength; index1++)
		{
			c = array[index1];
			if ((c == '$') && (index1 < (arrayLength - 2)))
			{
				c2 = array[index1 + 1];
				if ((c2 == 'c') || (c2 == 's') || (c2 == 'p') || (c2 == 'C') || (c2 == 'S') || (c2 == 'P'))
				{
					c3 = array[index1 + 2];
					if (Character.isDigit(c3))
					{
						paramId = Character.getNumericValue(c3);
						subTextLen = index1 - index2;
						if (subTextLen != 0)
						{
							builders.add(new BuilderText(new String(array, index2, subTextLen)));
						}
						
						builders.add(new BuilderObject(paramId));
						index1 += 2;
						index2 = index1 + 1;
					}
				}
			}
		}
		
		if (arrayLength >= index1)
		{
			subTextLen = index1 - index2;
			if (subTextLen != 0)
			{
				builders.add(new BuilderText(new String(array, index2, subTextLen)));
			}
		}
		
		if (builders.size() == 1)
		{
			return builders.get(0);
		}
		
		return new BuilderContainer(builders.toArray(new Builder[0]));
	}
}
