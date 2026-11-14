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
package org.l2jmobius.gameserver.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * Utility methods for common operations on {@code Map} objects.
 * @author Mobius
 */
public class MapUtil
{
	/**
	 * Returns a new map sorted by value in ascending or descending order.
	 * @param <K> the type of keys in the map
	 * @param <V> the type of values, which must be comparable
	 * @param map source map to sort
	 * @param descending whether to sort in descending order
	 * @return a new {@code Map} sorted by value
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, boolean descending)
	{
		if (descending)
		{
			return map.entrySet().stream().sorted(Entry.comparingByValue(Collections.reverseOrder())).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
		}
		
		return map.entrySet().stream().sorted(Entry.comparingByValue()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
	}
	
	/**
	 * Returns a new map sorted by value in ascending order.
	 * @param <K> the type of keys in the map
	 * @param <V> the type of values, which must be comparable
	 * @param map source map to sort
	 * @return a new {@code Map} sorted by value
	 */
	public static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map)
	{
		return map.entrySet().stream().sorted(Entry.comparingByValue()).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
	}
}
