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

/**
 * This class serves as a container for {@code Builder} objects.<br>
 * It helps manage and organize various builder instances within the client strings system.
 * @author Forsaiken
 */
public class BuilderContainer extends Builder
{
	private final Builder[] _builders;
	
	/**
	 * Creates a new {@code BuilderContainer} instance.<br>
	 * This constructor initializes the container with an array of {@code Builder} objects.
	 * @param builders An array of {@code Builder} objects to store in the container.
	 */
	BuilderContainer(Builder[] builders)
	{
		_builders = builders;
	}
	
	/**
	 * Converts the container content into a string representation.<br>
	 * This method accepts a single {@code Object} as an argument.<br>
	 * It wraps the input and calls the varargs version of this method.
	 * @param param The object to be included in the resulting string.
	 * @return A formatted string containing the provided object.
	 */
	@Override
	public String toString(Object param)
	{
		return toString(new Object[]
		{
			param
		});
	}
	
	/**
	 * Converts the container contents into a single string.<br>
	 * It combines the results of all {@code Builder} objects in reverse order.<br>
	 * If parameters are provided, they are used to populate specific builders based on their index.
	 * @param params A variable number of objects used to fill builder values.
	 * @return The concatenated string representation of all builders.
	 */
	@Override
	public String toString(Object... params)
	{
		final int buildersLength = _builders.length;
		final int paramsLength = params.length;
		final String[] builds = new String[buildersLength];
		Builder builder;
		String build;
		int i;
		int paramIndex;
		int buildTextLen = 0;
		if (paramsLength != 0)
		{
			for (i = buildersLength; i-- > 0;)
			{
				builder = _builders[i];
				paramIndex = builder.getIndex();
				build = (paramIndex != -1) && (paramIndex < paramsLength) ? builder.toString(params[paramIndex]) : builder.toString();
				buildTextLen += build.length();
				builds[i] = build;
			}
		}
		else
		{
			for (i = buildersLength; i-- > 0;)
			{
				build = _builders[i].toString();
				buildTextLen += build.length();
				builds[i] = build;
			}
		}
		
		final StringBuilder sb = new StringBuilder(buildTextLen);
		for (i = 0; i < buildersLength; i++)
		{
			sb.append(builds[i]);
		}
		
		return sb.toString();
	}
	
	/**
	 * Returns the index of this item.<br>
	 * This method currently always returns {@code -1}.
	 * @return The current index as an {@code int}.
	 */
	@Override
	public int getIndex()
	{
		return -1;
	}
}
