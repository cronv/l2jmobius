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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.logging.Logger;

import org.l2jmobius.commons.time.TimeUtil;

/**
 * Reads and provides typed access to configuration properties loaded from a file.
 * @author Mobius
 */
public class ConfigReader
{
	private static final Logger LOGGER = Logger.getLogger(ConfigReader.class.getName());
	
	private final Properties _properties = new Properties();
	private final File _file;
	
	/**
	 * Loads properties from the file at the given path.
	 * @param filePath The configuration file path.
	 */
	public ConfigReader(String filePath)
	{
		_file = new File(filePath);
		
		if (!Files.exists(_file.toPath()))
		{
			LOGGER.warning("Configuration file not found: " + _file.getAbsolutePath());
			return;
		}
		
		try (InputStream input = Files.newInputStream(_file.toPath());
			InputStreamReader reader = new InputStreamReader(input, Charset.defaultCharset()))
		{
			_properties.load(reader);
		}
		catch (IOException e)
		{
			LOGGER.warning("Failed to load configurations from " + _file.getName() + ": " + e.getMessage());
		}
	}
	
	/**
	 * Checks whether the given configuration key exists.
	 * @param config The configuration key.
	 * @return {@code true} if found, {@code false} otherwise.
	 */
	public boolean containsKey(String config)
	{
		return _properties.containsKey(config);
	}
	
	/**
	 * Retrieves the raw value mapped to the given key.
	 * @param config The configuration key.
	 * @return The value, or {@code null} if not found.
	 */
	public String getValue(String config)
	{
		return _properties.getProperty(config);
	}
	
	/**
	 * Retrieves all property names with a string value.
	 * @return A {@code Collection} of property names.
	 */
	public Collection<String> getStringPropertyNames()
	{
		return _properties.stringPropertyNames();
	}
	
	/**
	 * Retrieves a {@code boolean} value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The value returned when the key is missing.
	 * @return The parsed {@code boolean}, or {@code defaultValue}.
	 */
	public boolean getBoolean(String config, boolean defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Boolean.parseBoolean(value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid boolean for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return defaultValue;
	}
	
	/**
	 * Retrieves a {@code byte} value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The value returned when missing or unparseable.
	 * @return The parsed {@code byte}, or {@code defaultValue}.
	 */
	public byte getByte(String config, byte defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Byte.parseByte(value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid byte for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return defaultValue;
	}
	
	/**
	 * Retrieves a {@code short} value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The value returned when missing or unparseable.
	 * @return The parsed {@code short}, or {@code defaultValue}.
	 */
	public short getShort(String config, short defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Short.parseShort(value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid short for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return defaultValue;
	}
	
	/**
	 * Retrieves an {@code int} value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The value returned when missing or unparseable.
	 * @return The parsed {@code int}, or {@code defaultValue}.
	 */
	public int getInt(String config, int defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Integer.parseInt(value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid int for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return defaultValue;
	}
	
	/**
	 * Retrieves a {@code long} value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The value returned when missing or unparseable.
	 * @return The parsed {@code long}, or {@code defaultValue}.
	 */
	public long getLong(String config, long defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Long.parseLong(value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid long for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return defaultValue;
	}
	
	/**
	 * Retrieves a {@code float} value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The value returned when missing or unparseable.
	 * @return The parsed {@code float}, or {@code defaultValue}.
	 */
	public float getFloat(String config, float defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Float.parseFloat(value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid float for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return defaultValue;
	}
	
	/**
	 * Retrieves a {@code double} value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The value returned when missing or unparseable.
	 * @return The parsed {@code double}, or {@code defaultValue}.
	 */
	public double getDouble(String config, double defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Double.parseDouble(value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid double for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return defaultValue;
	}
	
	/**
	 * Retrieves a {@code String} value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The value returned when the key is missing.
	 * @return The value, or {@code defaultValue}.
	 */
	public String getString(String config, String defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value == null)
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			return defaultValue;
		}
		
		return value;
	}
	
	/**
	 * Retrieves the enum constant mapped to the given key.
	 * @param <T> The enum type.
	 * @param config The configuration key.
	 * @param clazz The enum class.
	 * @param defaultValue The value returned when missing or unparseable.
	 * @return The matching enum constant, or {@code defaultValue}.
	 */
	public <T extends Enum<T>> T getEnum(String config, Class<T> clazz, T defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Enum.valueOf(clazz, value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid enum for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return defaultValue;
	}
	
	/**
	 * Retrieves a {@code Duration} parsed from the value for the given key.
	 * @param config The configuration key.
	 * @param defaultValue The default value parsed when missing or unparseable.
	 * @return The parsed {@code Duration}.
	 */
	public Duration getDuration(String config, String defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return TimeUtil.parseDuration(value);
			}
			catch (Exception e)
			{
				LOGGER.warning("Invalid duration for config '" + config + "' in file '" + _file.getName() + "', using default: " + defaultValue + ".");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default: " + defaultValue + ".");
		}
		
		return TimeUtil.parseDuration(defaultValue);
	}
	
	/**
	 * Retrieves an {@code int} array parsed from the value, split on the delimiter.
	 * @param config The configuration key.
	 * @param delimiter The value delimiter.
	 * @param defaultValue The fallback value parsed when missing or unparseable.
	 * @return The parsed {@code int} array, or an empty array.
	 */
	public int[] getIntArray(String config, String delimiter, String defaultValue)
	{
		final String value = _properties.getProperty(config);
		if (value != null)
		{
			try
			{
				return Arrays.stream(value.split(delimiter)).map(String::trim).mapToInt(Integer::parseInt).toArray();
			}
			catch (NumberFormatException e)
			{
				LOGGER.warning("Invalid int array for config '" + config + "' in file '" + _file.getName() + "', using default values.");
			}
		}
		else
		{
			LOGGER.warning("Config '" + config + "' not found in file '" + _file.getName() + "', using default values.");
		}
		
		try
		{
			return Arrays.stream(defaultValue.split(delimiter)).map(String::trim).mapToInt(Integer::parseInt).toArray();
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("Invalid default values for config '" + config + "' in file '" + _file.getName() + "', using empty array.");
		}
		
		return new int[0];
	}
}
