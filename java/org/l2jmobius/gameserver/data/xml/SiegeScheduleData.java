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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.mechanics.siege.SiegeScheduleDate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of siege schedule data from XML files.<br>
 * It provides the necessary structure to manage scheduled events within the game world.<br>
 * The schedules are published as an immutable snapshot only after the whole file parses, so a reload drops entries removed from the config.
 * @author UnAfraid, BazookaRpm
 */
public class SiegeScheduleData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SiegeScheduleData.class.getName());
	
	private volatile Map<Integer, SiegeScheduleDate> _scheduleData = Collections.emptyMap();
	
	/**
	 * This is the default constructor for {@code SiegeScheduleData}.<br>
	 * It initializes the object and calls the {@code load()} method.<br>
	 * Use this to create a new instance with loaded data.
	 */
	protected SiegeScheduleData()
	{
		load();
	}
	
	/**
	 * Loads the siege schedule data from the configuration file.<br>
	 * The active schedules are replaced only when the file parses successfully, so a failed reload keeps the sieges already scheduled.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, SiegeScheduleDate> previous = _scheduleData;
		parseDatapackFile("config/SiegeSchedule.xml");
		if (_scheduleData == previous)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not load siege schedules. Keeping the active schedules.");
			return;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _scheduleData.size() + " siege schedulers.");
	}
	
	/**
	 * Parses the provided {@code Document} and publishes the siege schedule data.<br>
	 * This method reads XML nodes and maps them to {@code SiegeScheduleDate} objects.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot parse a null XML document.");
			return;
		}
		
		final Map<Integer, SiegeScheduleDate> scheduleData = new HashMap<>();
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node cd = n.getFirstChild(); cd != null; cd = cd.getNextSibling())
				{
					switch (cd.getNodeName())
					{
						case "schedule":
						{
							final StatSet set = new StatSet();
							final NamedNodeMap attrs = cd.getAttributes();
							for (int i = 0; i < attrs.getLength(); i++)
							{
								final Node node = attrs.item(i);
								final String key = node.getNodeName();
								String val = node.getNodeValue();
								if ("day".equals(key) && !StringUtil.isNumeric(val))
								{
									val = Integer.toString(getDayOfWeek(val));
								}
								
								set.set(key, val);
							}
							
							scheduleData.put(set.getInt("castleId"), new SiegeScheduleDate(set));
							break;
						}
					}
				}
			}
		}
		
		if (scheduleData.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": No siege schedules were found in " + (file != null ? file.getAbsolutePath() : "<unknown>") + ".");
		}
		
		_scheduleData = Collections.unmodifiableMap(scheduleData);
	}
	
	/**
	 * Translates a day name into its {@code Calendar} day of week value.<br>
	 * It returns -1 for an unknown name, matching the previous reflective lookup.
	 * @param day The name of the day, such as {@code SUNDAY}.
	 * @return The matching {@code Calendar} constant, or -1 if the name is not a day.
	 */
	private int getDayOfWeek(String day)
	{
		switch (day)
		{
			case "SUNDAY":
			{
				return Calendar.SUNDAY;
			}
			case "MONDAY":
			{
				return Calendar.MONDAY;
			}
			case "TUESDAY":
			{
				return Calendar.TUESDAY;
			}
			case "WEDNESDAY":
			{
				return Calendar.WEDNESDAY;
			}
			case "THURSDAY":
			{
				return Calendar.THURSDAY;
			}
			case "FRIDAY":
			{
				return Calendar.FRIDAY;
			}
			case "SATURDAY":
			{
				return Calendar.SATURDAY;
			}
			default:
			{
				LOGGER.warning(getClass().getSimpleName() + ": Could not get value for day " + day + ".");
				return -1;
			}
		}
	}
	
	/**
	 * Retrieves the siege schedule date for a specific castle.<br>
	 * This method looks up the data using the provided {@code castleId}.
	 * @param castleId The unique identifier of the castle.
	 * @return The {@code SiegeScheduleDate} associated with the castle, or {@code null} if not found.
	 */
	public SiegeScheduleDate getScheduleDateForCastleId(int castleId)
	{
		return _scheduleData.get(castleId);
	}
	
	/**
	 * Gets the global instance of {@code SiegeScheduleData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the loaded siege schedules throughout the game server.
	 * @return The single instance of {@code SiegeScheduleData}.
	 */
	public static SiegeScheduleData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SiegeScheduleData INSTANCE = new SiegeScheduleData();
	}
}
