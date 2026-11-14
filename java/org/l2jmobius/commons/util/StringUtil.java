/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.commons.util;

import java.util.StringJoiner;
import java.util.StringTokenizer;

/**
 * Utility for {@code String} building, parsing and formatting.
 * @author Mobius
 */
public class StringUtil
{
	/**
	 * Appends a {@code String} to a {@code StringBuilder}.
	 * @param sb The {@code StringBuilder} to modify.
	 * @param arg The {@code String} to append.
	 */
	public static void append(StringBuilder sb, String arg)
	{
		sb.append(arg);
	}
	
	/**
	 * Appends multiple strings to a {@code StringBuilder}, pre-sizing capacity and counting {@code null} as length 4.
	 * @param sb The {@code StringBuilder} to modify.
	 * @param args The strings to append.
	 */
	public static void append(StringBuilder sb, String... args)
	{
		// Directly calculate the required capacity and ensure it in one step.
		int totalLength = sb.length();
		for (String arg : args)
		{
			totalLength += (arg != null ? arg.length() : 4);
		}
		
		sb.ensureCapacity(totalLength);
		
		// Append each argument.
		for (String arg : args)
		{
			sb.append(arg);
		}
	}
	
	/**
	 * Appends multiple objects to a {@code StringBuilder} via their {@code String} forms, pre-sizing capacity.
	 * @param sb The {@code StringBuilder} to modify.
	 * @param args The objects to append.
	 */
	public static void append(StringBuilder sb, Object... args)
	{
		// Calculate the total length and store converted strings.
		int totalLength = sb.length();
		final String[] strings = new String[args.length];
		for (int i = 0; i < args.length; i++)
		{
			strings[i] = String.valueOf(args[i]);
			totalLength += strings[i].length();
		}
		
		sb.ensureCapacity(totalLength);
		
		// Append each stored string.
		for (String string : strings)
		{
			sb.append(string);
		}
	}
	
	/**
	 * Joins multiple strings into one, counting {@code null} as length 4.
	 * @param args The strings to join.
	 * @return The concatenated {@code String}.
	 */
	public static String concat(String... args)
	{
		// Calculate the total length of all strings.
		int totalLength = 0;
		for (String arg : args)
		{
			totalLength += (arg != null ? arg.length() : 4);
		}
		
		// Append each argument.
		final StringBuilder sb = new StringBuilder(totalLength);
		for (String arg : args)
		{
			sb.append(arg);
		}
		
		return sb.toString();
	}
	
	/**
	 * Joins multiple objects into one via their {@code String} forms.
	 * @param args The objects to concatenate.
	 * @return The concatenated {@code String}.
	 */
	public static String concat(Object... args)
	{
		// Calculate the total length and store converted strings.
		int totalLength = 0;
		final String[] strings = new String[args.length];
		for (int i = 0; i < args.length; i++)
		{
			strings[i] = String.valueOf(args[i]);
			totalLength += strings[i].length();
		}
		
		// Append each stored string.
		final StringBuilder sb = new StringBuilder(totalLength);
		for (String string : strings)
		{
			sb.append(string);
		}
		
		return sb.toString();
	}
	
	/**
	 * Joins all elements of an {@code Iterable} separated by a delimiter.
	 * @param <T> The element type.
	 * @param items The elements to join.
	 * @param delimiter The separator.
	 * @return The joined {@code String}.
	 */
	public static <T> String implode(Iterable<T> items, String delimiter)
	{
		final StringJoiner joiner = new StringJoiner(delimiter);
		for (T item : items)
		{
			joiner.add(item.toString());
		}
		
		return joiner.toString();
	}
	
	/**
	 * Joins all elements of an array separated by a delimiter.
	 * @param <T> The element type.
	 * @param array The elements to join.
	 * @param delimiter The separator.
	 * @return The joined {@code String}.
	 */
	public static <T> String implode(T[] array, String delimiter)
	{
		final StringJoiner joiner = new StringJoiner(delimiter);
		for (T element : array)
		{
			joiner.add(element.toString());
		}
		
		return joiner.toString();
	}
	
	/**
	 * Capitalizes the first character and lowercases the rest, returning the input unchanged if {@code null} or empty.
	 * @param text The string to format.
	 * @return The formatted string.
	 */
	public static String capitalizeFirst(String text)
	{
		// Return the original if it's null or empty.
		if ((text == null) || text.isEmpty())
		{
			return text;
		}
		
		// Capitalize the first letter and set the remaining letters to lowercase.
		return Character.toUpperCase(text.charAt(0)) + text.substring(1).toLowerCase();
	}
	
	/**
	 * Inserts a space at each lowercase-to-uppercase transition, returning the input unchanged if {@code null} or empty.
	 * @param text The string to process.
	 * @return The spaced string.
	 */
	public static String separateWords(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return text;
		}
		
		final StringBuilder result = new StringBuilder();
		final char[] chars = text.toCharArray();
		for (int i = 0; i < chars.length; i++)
		{
			final char current = chars[i];
			
			// Check if the current character is uppercase and it's not the first character.
			if (Character.isUpperCase(current) && (i > 0) && Character.isLowerCase(chars[i - 1]))
			{
				result.append(' ');
			}
			
			result.append(current);
		}
		
		return result.toString();
	}
	
	/**
	 * Converts an {@code Enum} constant to title case, replacing underscores with spaces.
	 * @param enumeration The {@code Enum} constant.
	 * @return The formatted string.
	 */
	public static String enumToString(Enum<?> enumeration)
	{
		final String name = enumeration.name().toLowerCase();
		final int nameLength = name.length();
		final StringBuilder sb = new StringBuilder(nameLength);
		
		boolean capitalizeNext = true;
		for (int i = 0; i < nameLength; i++)
		{
			char c = name.charAt(i);
			if (c == '_')
			{
				sb.append(' ');
				capitalizeNext = true;
			}
			else if (capitalizeNext)
			{
				sb.append(Character.toUpperCase(c));
				capitalizeNext = false;
			}
			else
			{
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Parses a string into a {@code Boolean}, {@code Long}, {@code Integer}, {@code Float} or {@code Double}, falling back to the trimmed string.
	 * @param value The string to parse.
	 * @return The parsed object, or {@code null} if the input is {@code null}.
	 */
	public static Object parseValue(String value)
	{
		if (value == null)
		{
			return null;
		}
		
		final String val = value.trim();
		if (val.equalsIgnoreCase("true"))
		{
			return Boolean.TRUE;
		}
		else if (val.equalsIgnoreCase("false"))
		{
			return Boolean.FALSE;
		}
		else
		{
			try
			{
				// Try Long first (to avoid Integer parsing of larger numbers).
				return Long.valueOf(val);
			}
			catch (NumberFormatException e1)
			{
				try
				{
					// Try Integer.
					return Integer.valueOf(val);
				}
				catch (NumberFormatException e2)
				{
					try
					{
						// Try Float before Double to get single-precision when possible.
						return Float.valueOf(val);
					}
					catch (NumberFormatException e3)
					{
						try
						{
							// Try Double.
							return Double.valueOf(val);
						}
						catch (NumberFormatException e4)
						{
							return val;
						}
					}
				}
			}
		}
	}
	
	/**
	 * Parses a string into an {@code int}, returning a default on failure.
	 * @param text The string to parse.
	 * @param defaultValue The value returned if parsing fails.
	 * @return The parsed int, or the default.
	 */
	public static int parseInt(String text, int defaultValue)
	{
		try
		{
			return Integer.parseInt(text);
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Parses a string into a {@code long}, returning a default on failure.
	 * @param text The string to parse.
	 * @param defaultValue The value returned if parsing fails.
	 * @return The parsed long, or the default.
	 */
	public static long parseLong(String text, long defaultValue)
	{
		try
		{
			return Long.parseLong(text);
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Parses the next integer token, returning a default if absent or unparseable.
	 * @param tokenizer The {@code StringTokenizer} to read.
	 * @param defaultValue The value returned on error.
	 * @return The parsed int, or the default.
	 */
	public static int parseNextInt(StringTokenizer tokenizer, int defaultValue)
	{
		if (tokenizer.hasMoreTokens())
		{
			try
			{
				final String value = tokenizer.nextToken().trim();
				return Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{
				// Parsing failed, fall back to default.
			}
		}
		
		return defaultValue;
	}
	
	/**
	 * Checks whether a string contains only letters and digits.
	 * @param text The string to check.
	 * @return {@code true} if all characters are alphanumeric, {@code false} if {@code null}, empty or otherwise.
	 */
	public static boolean isAlphaNumeric(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		for (int i = 0; i < text.length(); i++)
		{
			if (!Character.isLetterOrDigit(text.charAt(i)))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks whether a string contains only digits.
	 * @param text The string to check.
	 * @return {@code true} if all characters are digits, {@code false} if {@code null}, empty or otherwise.
	 */
	public static boolean isNumeric(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		for (int i = 0; i < text.length(); i++)
		{
			if (!Character.isDigit(text.charAt(i)))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Checks whether a string parses as an {@code int}.
	 * @param text The string to check.
	 * @return {@code true} if a valid integer, {@code false} if {@code null}, empty or otherwise.
	 */
	public static boolean isInteger(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		try
		{
			Integer.parseInt(text);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Checks whether a string parses as a {@code float}.
	 * @param text The string to check.
	 * @return {@code true} if a valid float, {@code false} if {@code null}, empty or otherwise.
	 */
	public static boolean isFloat(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		try
		{
			Float.parseFloat(text);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Checks whether a string parses as a {@code double}.
	 * @param text The string to check.
	 * @return {@code true} if a valid double, {@code false} if {@code null}, empty or otherwise.
	 */
	public static boolean isDouble(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return false;
		}
		
		try
		{
			Double.parseDouble(text);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Checks whether a string names a valid constant of the given enum type.
	 * @param <T> The enum type.
	 * @param name The string to check.
	 * @param enumType The enum class.
	 * @return {@code true} if a valid enum constant, {@code false} if {@code null}, empty or otherwise.
	 */
	public static <T extends Enum<T>> boolean isEnum(String name, Class<T> enumType)
	{
		if ((name == null) || name.isEmpty())
		{
			return false;
		}
		
		try
		{
			Enum.valueOf(enumType, name);
			return true;
		}
		catch (IllegalArgumentException e)
		{
			return false;
		}
	}
}
