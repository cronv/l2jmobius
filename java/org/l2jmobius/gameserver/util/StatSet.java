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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Holds a collection of key/value pairs stored as {@code Object} and retrievable as any type via casting.
 * @author mkizub, Mobius
 */
public class StatSet
{
	private static final Logger LOGGER = Logger.getLogger(StatSet.class.getName());
	
	/** Static empty immutable map, used to avoid multiple null checks over the source. */
	public static final StatSet EMPTY_STATSET = new StatSet(Collections.emptyMap());
	
	private final Map<String, Object> _set;
	
	/**
	 * Constructs an empty {@code StatSet} backed by a {@code HashMap}.
	 */
	public StatSet()
	{
		this(HashMap::new);
	}
	
	/**
	 * Constructs a {@code StatSet} backed by the map produced by the given factory.
	 * @param mapFactory supplier producing the backing {@code Map<String,Object>}
	 */
	public StatSet(Supplier<Map<String, Object>> mapFactory)
	{
		this(mapFactory.get());
	}
	
	/**
	 * Constructs a {@code StatSet} backed by the given map.
	 * @param map the backing {@code Map<String,Object>} of initial key-value pairs
	 */
	public StatSet(Map<String, Object> map)
	{
		_set = map;
	}
	
	/**
	 * Returns the underlying map of all key-value pairs.
	 * @return the backing {@code Map<String,Object>}
	 */
	public Map<String, Object> getSet()
	{
		return _set;
	}
	
	/**
	 * Merges another set into this one, overwriting existing keys.
	 * @param newSet the {@code StatSet} whose entries are copied in
	 */
	public void merge(StatSet newSet)
	{
		_set.putAll(newSet.getSet());
	}
	
	/**
	 * Returns whether the set contains no entries.
	 * @return {@code true} if empty, {@code false} otherwise
	 */
	public boolean isEmpty()
	{
		return _set.isEmpty();
	}
	
	/**
	 * Returns the boolean value for the given key, parsing it from a {@code String} if needed.
	 * @param key the key to look up
	 * @return the boolean value
	 * @throws IllegalArgumentException if the key is missing or not a boolean
	 */
	public boolean getBoolean(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Boolean value required, but not specified");
		}
		
		if (val instanceof Boolean)
		{
			return ((Boolean) val).booleanValue();
		}
		
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Boolean value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the boolean value for the given key, or the default if missing or unparsable.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent or invalid
	 * @return the boolean value or the default
	 */
	public boolean getBoolean(String key, boolean defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Boolean)
		{
			return ((Boolean) val).booleanValue();
		}
		
		try
		{
			return Boolean.parseBoolean((String) val);
		}
		catch (Exception e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Returns the {@code byte} value for the given key, parsing it from a {@code String} if needed.
	 * @param key the key to look up
	 * @return the {@code byte} value
	 * @throws IllegalArgumentException if the key is missing or not a byte
	 */
	public byte getByte(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Byte value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).byteValue();
		}
		
		try
		{
			return Byte.parseByte((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Byte value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the {@code byte} value for the given key, or the default if missing.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent
	 * @return the {@code byte} value or the default
	 * @throws IllegalArgumentException if the value exists but is not a byte
	 */
	public byte getByte(String key, byte defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).byteValue();
		}
		
		try
		{
			return Byte.parseByte((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Byte value required, but found: " + val);
		}
	}
	
	/**
	 * Adds the given amount to the stored {@code byte} value and stores the result.
	 * @param key the key to update
	 * @param increaseWith the amount to add
	 * @return the new value as a {@code short}
	 */
	public short increaseByte(String key, byte increaseWith)
	{
		final byte newValue = (byte) (getByte(key) + increaseWith);
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Adds the given amount to the stored {@code byte} value (or the default if absent) and stores the result.
	 * @param key the key to update
	 * @param defaultValue the value used when the key is absent
	 * @param increaseWith the amount to add
	 * @return the new value as a {@code short}
	 */
	public short increaseByte(String key, byte defaultValue, byte increaseWith)
	{
		final byte newValue = (byte) (getByte(key, defaultValue) + increaseWith);
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Returns the value for the given key as a {@code byte} array, splitting a {@code String} value on the given regex.
	 * @param key the key to look up
	 * @param splitOn the regex used to split a string value
	 * @return the parsed {@code byte} array
	 * @throws IllegalArgumentException if the key is missing or a part is not a byte
	 */
	public byte[] getByteArray(String key, String splitOn)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Byte value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return new byte[]
			{
				((Number) val).byteValue()
			};
		}
		
		int c = 0;
		final String[] vals = ((String) val).split(splitOn);
		final byte[] result = new byte[vals.length];
		for (String v : vals)
		{
			try
			{
				result[c++] = Byte.parseByte(v);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("Byte value required, but found: " + val);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the value for the given key as a {@code List} of {@code Byte}, splitting a {@code String} value on the given regex.
	 * @param key the key to look up
	 * @param splitOn the regex used to split a string value
	 * @return the parsed {@code List<Byte>}
	 */
	public List<Byte> getByteList(String key, String splitOn)
	{
		final List<Byte> result = new ArrayList<>();
		for (Byte i : getByteArray(key, splitOn))
		{
			result.add(i);
		}
		
		return result;
	}
	
	/**
	 * Returns the {@code short} value for the given key, parsing it from a {@code String} if needed.
	 * @param key the key to look up
	 * @return the {@code short} value
	 * @throws IllegalArgumentException if the key is missing or not a short
	 */
	public short getShort(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Short value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).shortValue();
		}
		
		try
		{
			return Short.parseShort((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Short value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the {@code short} value for the given key, or the default if missing.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent
	 * @return the {@code short} value or the default
	 * @throws IllegalArgumentException if the value exists but is not a short
	 */
	public short getShort(String key, short defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).shortValue();
		}
		
		try
		{
			return Short.parseShort((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Short value required, but found: " + val);
		}
	}
	
	/**
	 * Adds the given amount to the stored {@code short} value and stores the result.
	 * @param key the key to update
	 * @param increaseWith the amount to add
	 * @return the new {@code short} value
	 */
	public short increaseShort(String key, short increaseWith)
	{
		final short newValue = (short) (getShort(key) + increaseWith);
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Adds the given amount to the stored {@code short} value (or the default if absent) and stores the result.
	 * @param key the key to update
	 * @param defaultValue the value used when the key is absent
	 * @param increaseWith the amount to add
	 * @return the new {@code short} value
	 */
	public short increaseShort(String key, short defaultValue, short increaseWith)
	{
		final short newValue = (short) (getShort(key, defaultValue) + increaseWith);
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Returns the {@code int} value for the given key, parsing it from a {@code String} if needed.
	 * @param key the key to look up
	 * @return the {@code int} value
	 * @throws IllegalArgumentException if the key is missing or not an integer
	 */
	public int getInt(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Integer value required, but not specified: " + key + "!");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).intValue();
		}
		
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val + "!");
		}
	}
	
	/**
	 * Returns the {@code int} value for the given key, or the default if missing.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent
	 * @return the {@code int} value or the default
	 * @throws IllegalArgumentException if the value exists but is not an integer
	 */
	public int getInt(String key, int defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).intValue();
		}
		
		try
		{
			return Integer.parseInt((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Integer value required, but found: " + val);
		}
	}
	
	/**
	 * Adds the given amount to the stored {@code int} value and stores the result.
	 * @param key the key to update
	 * @param increaseWith the amount to add
	 * @return the new {@code int} value
	 */
	public int increaseInt(String key, int increaseWith)
	{
		final int newValue = getInt(key) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Adds the given amount to the stored {@code int} value (or the default if absent) and stores the result.
	 * @param key the key to update
	 * @param defaultValue the value used when the key is absent
	 * @param increaseWith the amount to add
	 * @return the new {@code int} value
	 */
	public int increaseInt(String key, int defaultValue, int increaseWith)
	{
		final int newValue = getInt(key, defaultValue) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Returns the value for the given key as an {@code int} array, splitting a {@code String} value on the given regex.
	 * @param key the key to look up
	 * @param splitOn the regex used to split a string value
	 * @return the parsed {@code int} array
	 * @throws IllegalArgumentException if the key is missing or a part is not an integer
	 */
	public int[] getIntArray(String key, String splitOn)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Integer value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return new int[]
			{
				((Number) val).intValue()
			};
		}
		
		int c = 0;
		final String[] vals = ((String) val).split(splitOn);
		final int[] result = new int[vals.length];
		for (String v : vals)
		{
			try
			{
				result[c++] = Integer.parseInt(v);
			}
			catch (Exception e)
			{
				throw new IllegalArgumentException("Integer value required, but found: " + val);
			}
		}
		
		return result;
	}
	
	/**
	 * Returns the comma-separated value for the given key as a {@code List} of {@code Integer}, or an empty list if absent.
	 * @param key the key to look up
	 * @return the parsed {@code List<Integer>}
	 */
	public List<Integer> getIntegerList(String key)
	{
		final String val = getString(key, null);
		final List<Integer> result;
		if (val != null)
		{
			final String[] splitVal = val.split(",");
			result = new ArrayList<>(splitVal.length);
			for (String split : splitVal)
			{
				result.add(Integer.parseInt(split));
			}
		}
		else
		{
			result = new ArrayList<>(1);
		}
		
		return result;
	}
	
	/**
	 * Stores the given list as a comma-separated string, removing the key if the list is null or empty.
	 * @param key the key to store under
	 * @param list the {@code List<Integer>} to store
	 */
	public void setIntegerList(String key, List<Integer> list)
	{
		if (key == null)
		{
			return;
		}
		
		if ((list == null) || list.isEmpty())
		{
			remove(key);
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		for (int element : list)
		{
			sb.append(element);
			sb.append(',');
		}
		
		sb.deleteCharAt(sb.length() - 1); // Prettify value.
		
		set(key, sb.toString());
	}
	
	/**
	 * Returns the value for the given key parsed as a comma-separated map of key-value pairs, or an empty map if absent.
	 * @param key the key to look up
	 * @return the parsed {@code Map<Integer,Integer>}
	 */
	public Map<Integer, Integer> getIntegerMap(String key)
	{
		final String val = getString(key, null);
		final Map<Integer, Integer> result;
		if (val != null)
		{
			final String[] splitVal = val.split(",");
			result = new HashMap<>(splitVal.length + 1);
			for (String split : splitVal)
			{
				final String[] entry = split.split("-");
				result.put(Integer.parseInt(entry[0]), Integer.parseInt(entry[1]));
			}
		}
		else
		{
			result = new HashMap<>(1);
		}
		
		return result;
	}
	
	/**
	 * Stores the given map as a formatted string, removing the key if the map is null or empty.
	 * @param key the key to store under
	 * @param map the {@code Map<Integer,Integer>} to store
	 */
	public void setIntegerMap(String key, Map<Integer, Integer> map)
	{
		if (key == null)
		{
			return;
		}
		
		if ((map == null) || map.isEmpty())
		{
			remove(key);
			return;
		}
		
		final StringBuilder sb = new StringBuilder();
		for (Entry<Integer, Integer> entry : map.entrySet())
		{
			sb.append(entry.getKey());
			sb.append('-');
			sb.append(entry.getValue());
			sb.append(',');
		}
		
		sb.deleteCharAt(sb.length() - 1); // Prettify value.
		
		set(key, sb.toString());
	}
	
	/**
	 * Returns the {@code long} value for the given key, parsing it from a {@code String} if needed.
	 * @param key the key to look up
	 * @return the {@code long} value
	 * @throws IllegalArgumentException if the key is missing or not a long
	 */
	public long getLong(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Long value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).longValue();
		}
		
		try
		{
			return Long.parseLong((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Long value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the {@code long} value for the given key, or the default if missing.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent
	 * @return the {@code long} value or the default
	 * @throws IllegalArgumentException if the value exists but is not a long
	 */
	public long getLong(String key, long defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).longValue();
		}
		
		try
		{
			return Long.parseLong((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Long value required, but found: " + val);
		}
	}
	
	/**
	 * Adds the given amount to the stored {@code long} value and stores the result.
	 * @param key the key to update
	 * @param increaseWith the amount to add
	 * @return the new {@code long} value
	 */
	public long increaseLong(String key, long increaseWith)
	{
		final long newValue = getLong(key) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Adds the given amount to the stored {@code long} value (or the default if absent) and stores the result.
	 * @param key the key to update
	 * @param defaultValue the value used when the key is absent
	 * @param increaseWith the amount to add
	 * @return the new {@code long} value
	 */
	public long increaseLong(String key, long defaultValue, long increaseWith)
	{
		final long newValue = getLong(key, defaultValue) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Returns the {@code float} value for the given key, parsing it from a {@code String} if needed.
	 * @param key the key to look up
	 * @return the {@code float} value
	 * @throws IllegalArgumentException if the key is missing or not a float
	 */
	public float getFloat(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Float value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).floatValue();
		}
		
		try
		{
			return Float.parseFloat((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the {@code float} value for the given key, or the default if missing.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent
	 * @return the {@code float} value or the default
	 * @throws IllegalArgumentException if the value exists but is not a float
	 */
	public float getFloat(String key, float defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).floatValue();
		}
		
		try
		{
			return Float.parseFloat((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Float value required, but found: " + val);
		}
	}
	
	/**
	 * Adds the given amount to the stored {@code float} value and stores the result.
	 * @param key the key to update
	 * @param increaseWith the amount to add
	 * @return the new {@code float} value
	 */
	public float increaseFloat(String key, float increaseWith)
	{
		final float newValue = getFloat(key) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Adds the given amount to the stored {@code float} value (or the default if absent) and stores the result.
	 * @param key the key to update
	 * @param defaultValue the value used when the key is absent
	 * @param increaseWith the amount to add
	 * @return the new {@code float} value
	 */
	public float increaseFloat(String key, float defaultValue, float increaseWith)
	{
		final float newValue = getFloat(key, defaultValue) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Returns the {@code double} value for the given key, parsing it from a {@code String} if needed.
	 * @param key the key to look up
	 * @return the {@code double} value
	 * @throws IllegalArgumentException if the key is missing or not a double
	 */
	public double getDouble(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Double value required, but not specified");
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).doubleValue();
		}
		
		try
		{
			return Double.parseDouble((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Double value required, but found: " + val);
		}
	}
	
	/**
	 * Returns the {@code double} value for the given key, or the default if missing.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent
	 * @return the {@code double} value or the default
	 * @throws IllegalArgumentException if the value exists but is not a double
	 */
	public double getDouble(String key, double defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (val instanceof Number)
		{
			return ((Number) val).doubleValue();
		}
		
		try
		{
			return Double.parseDouble((String) val);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Double value required, but found: " + val);
		}
	}
	
	/**
	 * Adds the given amount to the stored {@code double} value and stores the result.
	 * @param key the key to update
	 * @param increaseWith the amount to add
	 * @return the new {@code double} value
	 */
	public double increaseDouble(String key, double increaseWith)
	{
		final double newValue = getDouble(key) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Adds the given amount to the stored {@code double} value (or the default if absent) and stores the result.
	 * @param key the key to update
	 * @param defaultValue the value used when the key is absent
	 * @param increaseWith the amount to add
	 * @return the new {@code double} value
	 */
	public double increaseDouble(String key, double defaultValue, double increaseWith)
	{
		final double newValue = getDouble(key, defaultValue) + increaseWith;
		set(key, newValue);
		return newValue;
	}
	
	/**
	 * Returns the value for the given key as a {@code String}.
	 * @param key the key to look up
	 * @return the string representation of the value
	 * @throws IllegalArgumentException if the key is missing
	 */
	public String getString(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("String value required, but not specified");
		}
		
		return String.valueOf(val);
	}
	
	/**
	 * Returns the value for the given key as a {@code String}, or the default if missing.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent
	 * @return the string representation of the value or the default
	 */
	public String getString(String key, String defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		return String.valueOf(val);
	}
	
	/**
	 * Returns the value for the given key parsed as a {@code Duration}.
	 * @param key the key to look up
	 * @return the parsed {@code Duration}
	 * @throws IllegalArgumentException if the key is missing
	 */
	public Duration getDuration(String key)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("String value required, but not specified");
		}
		
		return TimeUtil.parseDuration(String.valueOf(val));
	}
	
	/**
	 * Returns the value for the given key parsed as a {@code Duration}, or the default if missing.
	 * @param key the key to look up
	 * @param defaultValue the value returned when the key is absent
	 * @return the parsed {@code Duration} or the default
	 */
	public Duration getDuration(String key, Duration defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		return TimeUtil.parseDuration(String.valueOf(val));
	}
	
	/**
	 * Returns the value for the given key as an enum constant of the given class.
	 * @param <T> the enum type
	 * @param key the key to look up
	 * @param enumClass the enum class to resolve against
	 * @return the matching enum constant
	 * @throws IllegalArgumentException if the key is missing or the value is not a valid constant
	 */
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but not specified");
		}
		
		if (enumClass.isInstance(val))
		{
			return (T) val;
		}
		
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but found: " + val);
		}
	}
	
	/**
	 * Returns the value for the given key as an enum constant of the given class, or the default if missing.
	 * @param <T> the enum type
	 * @param key the key to look up
	 * @param enumClass the enum class to resolve against
	 * @param defaultValue the value returned when the key is absent
	 * @return the matching enum constant or the default
	 * @throws IllegalArgumentException if the value exists but is not a valid constant
	 */
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> T getEnum(String key, Class<T> enumClass, T defaultValue)
	{
		final Object val = _set.get(key);
		if (val == null)
		{
			return defaultValue;
		}
		
		if (enumClass.isInstance(val))
		{
			return (T) val;
		}
		
		try
		{
			return Enum.valueOf(enumClass, String.valueOf(val));
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Enum value of type " + enumClass.getName() + " required, but found: " + val);
		}
	}
	
	/**
	 * Returns the value for the given key cast to the given type, or {@code null} if absent or mismatched.
	 * @param <A> the expected return type
	 * @param name the key to look up
	 * @param type the expected class
	 * @return the value cast to {@code A}, or {@code null}
	 */
	@SuppressWarnings("unchecked")
	public <A> A getObject(String name, Class<A> type)
	{
		final Object obj = _set.get(name);
		if ((obj == null) || !type.isAssignableFrom(obj.getClass()))
		{
			return null;
		}
		
		return (A) obj;
	}
	
	/**
	 * Returns the value for the given key cast to the given type, or the default if absent or mismatched.
	 * @param <A> the expected return type
	 * @param name the key to look up
	 * @param type the expected class
	 * @param defaultValue the value returned when no match is found
	 * @return the value cast to {@code A}, or the default
	 */
	@SuppressWarnings("unchecked")
	public <A> A getObject(String name, Class<A> type, A defaultValue)
	{
		final Object obj = _set.get(name);
		if ((obj == null) || !type.isAssignableFrom(obj.getClass()))
		{
			return defaultValue;
		}
		
		return (A) obj;
	}
	
	/**
	 * Returns the value for the given key as a {@code SkillHolder}, or {@code null} if absent or mismatched.
	 * @param key the key to look up
	 * @return the {@code SkillHolder}, or {@code null}
	 */
	public SkillHolder getSkillHolder(String key)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof SkillHolder))
		{
			return null;
		}
		
		return (SkillHolder) obj;
	}
	
	/**
	 * Returns the value for the given key as a {@code Location}, or {@code null} if absent or mismatched.
	 * @param key the key to look up
	 * @return the {@code Location}, or {@code null}
	 */
	public Location getLocation(String key)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof Location))
		{
			return null;
		}
		
		return (Location) obj;
	}
	
	/**
	 * Returns the value for the given key as a {@code List} of {@code MinionHolder}, or an empty list if absent or mismatched.
	 * @param key the key to look up
	 * @return the {@code List<MinionHolder>}
	 */
	@SuppressWarnings("unchecked")
	public List<MinionHolder> getMinionList(String key)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof List<?>))
		{
			return Collections.emptyList();
		}
		
		return (List<MinionHolder>) obj;
	}
	
	/**
	 * Returns the value for the given key as a typed {@code List}, converting element types when possible.
	 * @param <T> the element type
	 * @param key the key to look up
	 * @param clazz the element class
	 * @return the {@code List<T>}, or {@code null} if absent or unconvertible
	 * @throws IllegalAccessError if the elements are enums; use getEnumList instead
	 */
	@SuppressWarnings("unchecked")
	public <T> List<T> getList(String key, Class<T> clazz)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof List<?>))
		{
			return null;
		}
		
		final List<Object> originalList = (List<Object>) obj;
		if (!originalList.isEmpty() && !originalList.stream().allMatch(clazz::isInstance))
		{
			if (clazz.getSuperclass() == Enum.class)
			{
				throw new IllegalAccessError("Please use getEnumList if you want to get list of Enums!");
			}
			
			// Attempt to convert the list.
			final List<T> convertedList = convertList(originalList, clazz);
			if (convertedList == null)
			{
				LOGGER.log(Level.WARNING, "getList(\"" + key + "\", " + clazz.getSimpleName() + ") requested with wrong generic type: " + obj.getClass().getGenericInterfaces()[0] + "!", new ClassNotFoundException());
				return null;
			}
			
			// Overwrite the existing list with proper generic type.
			_set.put(key, convertedList);
			return convertedList;
		}
		
		return (List<T>) obj;
	}
	
	/**
	 * Returns the value for the given key as a typed {@code List}, or the default if absent.
	 * @param <T> the element type
	 * @param key the key to look up
	 * @param clazz the element class
	 * @param defaultValue the value returned when no list is found
	 * @return the {@code List<T>} or the default
	 */
	public <T> List<T> getList(String key, Class<T> clazz, List<T> defaultValue)
	{
		final List<T> list = getList(key, clazz);
		return list == null ? defaultValue : list;
	}
	
	/**
	 * Returns the value for the given key as a {@code List} of enum constants of the given class.
	 * @param <T> the enum type
	 * @param key the key to look up
	 * @param clazz the enum class
	 * @return the {@code List<T>}, or {@code null} if absent or incompatible
	 */
	@SuppressWarnings("unchecked")
	public <T extends Enum<T>> List<T> getEnumList(String key, Class<T> clazz)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof List<?>))
		{
			return null;
		}
		
		final List<Object> originalList = (List<Object>) obj;
		if (!originalList.isEmpty() && (obj.getClass().getGenericInterfaces()[0] != clazz) && originalList.stream().allMatch(name -> StringUtil.isEnum(name.toString(), clazz)))
		{
			final List<T> convertedList = originalList.stream().map(Object::toString).map(name -> Enum.valueOf(clazz, name)).map(clazz::cast).toList();
			
			// Overwrite the existing list with proper generic type.
			_set.put(key, convertedList);
			return convertedList;
		}
		
		return (List<T>) obj;
	}
	
	/**
	 * Converts a list of objects to a list of the given type, supporting {@code Integer}, {@code Float} and {@code Double}.
	 * @param <T> the target element type
	 * @param originalList the source list to convert
	 * @param clazz the target element class
	 * @return the converted list, or {@code null} if conversion is unsupported or fails
	 */
	private <T> List<T> convertList(List<Object> originalList, Class<T> clazz)
	{
		final List<String> stringList = originalList.stream().map(Object::toString).toList();
		
		if (clazz == Integer.class)
		{
			if (stringList.stream().allMatch(StringUtil::isInteger))
			{
				return stringList.stream().map(Integer::valueOf).map(clazz::cast).toList();
			}
		}
		else if (clazz == Float.class)
		{
			if (stringList.stream().allMatch(StringUtil::isFloat))
			{
				return stringList.stream().map(Float::valueOf).map(clazz::cast).toList();
			}
		}
		else if (clazz == Double.class)
		{
			if (stringList.stream().allMatch(StringUtil::isDouble))
			{
				return stringList.stream().map(Double::valueOf).map(clazz::cast).toList();
			}
		}
		
		return null;
	}
	
	/**
	 * Returns the value for the given key as a typed {@code Map}, logging a warning on type mismatch.
	 * @param <K> the key type
	 * @param <V> the value type
	 * @param key the key to look up
	 * @param keyClass the key class
	 * @param valueClass the value class
	 * @return the {@code Map<K,V>}, or {@code null} if absent or not a map
	 */
	@SuppressWarnings("unchecked")
	public <K, V> Map<K, V> getMap(String key, Class<K> keyClass, Class<V> valueClass)
	{
		final Object obj = _set.get(key);
		if (!(obj instanceof Map<?, ?>))
		{
			return null;
		}
		
		final Map<?, ?> originalList = (Map<?, ?>) obj;
		if (!originalList.isEmpty() && ((!originalList.keySet().stream().allMatch(keyClass::isInstance)) || (!originalList.values().stream().allMatch(valueClass::isInstance))))
		{
			LOGGER.log(Level.WARNING, "getMap(\"" + key + "\", " + keyClass.getSimpleName() + ", " + valueClass.getSimpleName() + ") requested with wrong generic type: " + obj.getClass().getGenericInterfaces()[0] + "!", new ClassNotFoundException());
		}
		
		return (Map<K, V>) obj;
	}
	
	/**
	 * Stores the given object under the key, ignoring null values.
	 * @param name the key to store under
	 * @param value the object to store
	 */
	public void set(String name, Object value)
	{
		if (value == null)
		{
			return;
		}
		
		_set.put(name, value);
	}
	
	/**
	 * Stores the given boolean value under the key.
	 * @param name the key to store under
	 * @param value the boolean value to store
	 */
	public void set(String name, boolean value)
	{
		_set.put(name, Boolean.valueOf(value));
	}
	
	/**
	 * Stores the given {@code byte} value under the key.
	 * @param key the key to store under
	 * @param value the {@code byte} value to store
	 */
	public void set(String key, byte value)
	{
		_set.put(key, value);
	}
	
	/**
	 * Stores the given {@code short} value under the key.
	 * @param key the key to store under
	 * @param value the {@code short} value to store
	 */
	public void set(String key, short value)
	{
		_set.put(key, value);
	}
	
	/**
	 * Stores the given {@code int} value under the key.
	 * @param key the key to store under
	 * @param value the {@code int} value to store
	 */
	public void set(String key, int value)
	{
		_set.put(key, value);
	}
	
	/**
	 * Stores the given {@code long} value under the key.
	 * @param key the key to store under
	 * @param value the {@code long} value to store
	 */
	public void set(String key, long value)
	{
		_set.put(key, value);
	}
	
	/**
	 * Stores the given {@code float} value under the key.
	 * @param key the key to store under
	 * @param value the {@code float} value to store
	 */
	public void set(String key, float value)
	{
		_set.put(key, value);
	}
	
	/**
	 * Stores the given {@code double} value under the key.
	 * @param key the key to store under
	 * @param value the {@code double} value to store
	 */
	public void set(String key, double value)
	{
		_set.put(key, value);
	}
	
	/**
	 * Stores the given string under the key, ignoring null values.
	 * @param key the key to store under
	 * @param value the string to store
	 */
	public void set(String key, String value)
	{
		if (value == null)
		{
			return;
		}
		
		_set.put(key, value);
	}
	
	/**
	 * Stores the given enum value under the key, ignoring null values.
	 * @param key the key to store under
	 * @param value the enum value to store
	 */
	public void set(String key, Enum<?> value)
	{
		if (value == null)
		{
			return;
		}
		
		_set.put(key, value);
	}
	
	/**
	 * Removes the entry for the given key.
	 * @param key the key to remove
	 */
	public void remove(String key)
	{
		_set.remove(key);
	}
	
	/**
	 * Returns whether the set contains the given key.
	 * @param name the key to search for
	 * @return {@code true} if the key exists, {@code false} otherwise
	 */
	public boolean contains(String name)
	{
		return _set.containsKey(name);
	}
	
	/**
	 * Returns a string representation of the set for debugging.
	 * @return a string containing the contents of the backing map
	 */
	@Override
	public String toString()
	{
		return "StatSet{_set=" + _set + '}';
	}
}
