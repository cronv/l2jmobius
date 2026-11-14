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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.mechanics.fishing.FishingBait;
import org.l2jmobius.gameserver.mechanics.fishing.FishingCatch;
import org.l2jmobius.gameserver.mechanics.fishing.FishingRod;

/**
 * This class handles the loading and parsing of fishing-related data from XML files.<br>
 * It provides configuration details for {@code FishingBait}, {@code FishingCatch}, and {@code FishingRod} objects.
 * @author BazookaRpm
 */
public class FishingData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(FishingData.class.getName());
	
	private volatile Map<Integer, FishingBait> _baitData = Collections.emptyMap();
	private volatile Map<Integer, FishingRod> _rodData = Collections.emptyMap();
	private int _baitDistanceMin;
	private int _baitDistanceMax;
	private double _expRateMin;
	private double _expRateMax;
	private double _spRateMin;
	private double _spRateMax;
	
	/**
	 * Initializes a new instance of the {@code FishingData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected FishingData()
	{
		load();
	}
	
	/**
	 * Loads the fishing configuration from the XML file.<br>
	 * The active configuration is replaced only when the file parses successfully, so a failed reload keeps the bait and rod data the fishing mechanics are already using.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, FishingBait> previous = _baitData;
		parseDatapackFile("data/Fishing.xml");
		if (_baitData == previous)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not load fishing data. Keeping the active configuration.");
			return;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _baitData.size() + " bait and " + _rodData.size() + " rod data.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate fishing data.<br>
	 * This method reads bait and rod configurations from an XML file.<br>
	 * It populates internal maps with {@code FishingBait} and {@code FishingRod} objects.
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
		
		final Map<Integer, FishingBait> baits = new HashMap<>();
		final Map<Integer, FishingRod> rods = new HashMap<>();
		int baitDistanceMin = _baitDistanceMin;
		int baitDistanceMax = _baitDistanceMax;
		double expRateMin = _expRateMin;
		double expRateMax = _expRateMax;
		double spRateMin = _spRateMin;
		double spRateMax = _spRateMax;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node listItem = n.getFirstChild(); listItem != null; listItem = listItem.getNextSibling())
				{
					switch (listItem.getNodeName())
					{
						case "baitDistance":
						{
							final NamedNodeMap bdAttrs = listItem.getAttributes();
							baitDistanceMin = parseInteger(bdAttrs, "min");
							baitDistanceMax = parseInteger(bdAttrs, "max");
							break;
						}
						case "xpRate":
						{
							final NamedNodeMap xpAttrs = listItem.getAttributes();
							expRateMin = parseDouble(xpAttrs, "min");
							expRateMax = parseDouble(xpAttrs, "max");
							break;
						}
						case "spRate":
						{
							final NamedNodeMap spAttrs = listItem.getAttributes();
							spRateMin = parseDouble(spAttrs, "min");
							spRateMax = parseDouble(spAttrs, "max");
							break;
						}
						case "baits":
						{
							for (Node bait = listItem.getFirstChild(); bait != null; bait = bait.getNextSibling())
							{
								if ("bait".equalsIgnoreCase(bait.getNodeName()))
								{
									final NamedNodeMap attrs = bait.getAttributes();
									final int itemId = parseInteger(attrs, "itemId");
									if (ItemData.getInstance().getTemplate(itemId) == null)
									{
										LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + itemId);
										continue;
									}
									
									final int level = parseInteger(attrs, "level", 1);
									final int minPlayerLevel = parseInteger(attrs, "minPlayerLevel");
									final int maxPlayerLevel = parseInteger(attrs, "maxPlayerLevel", PlayerConfig.PLAYER_MAXIMUM_LEVEL);
									final double chance = parseDouble(attrs, "chance");
									final int timeMin = parseInteger(attrs, "timeMin");
									final int timeMax = parseInteger(attrs, "timeMax", timeMin);
									final int waitMin = parseInteger(attrs, "waitMin");
									final int waitMax = parseInteger(attrs, "waitMax", waitMin);
									final boolean isPremiumOnly = parseBoolean(attrs, "isPremiumOnly", false);
									final FishingBait baitData = new FishingBait(itemId, level, minPlayerLevel, maxPlayerLevel, chance, timeMin, timeMax, waitMin, waitMax, isPremiumOnly);
									for (Node c = bait.getFirstChild(); c != null; c = c.getNextSibling())
									{
										if ("catch".equalsIgnoreCase(c.getNodeName()))
										{
											final NamedNodeMap cAttrs = c.getAttributes();
											final int cId = parseInteger(cAttrs, "itemId");
											if (ItemData.getInstance().getTemplate(cId) == null)
											{
												LOGGER.info(getClass().getSimpleName() + ": Could not find catch item with id " + cId);
												continue;
											}
											
											final float cChance = parseFloat(cAttrs, "chance");
											final float cMultiplier = parseFloat(cAttrs, "multiplier", 1f);
											
											baitData.addReward(new FishingCatch(cId, cChance, cMultiplier));
										}
									}
									
									baits.put(baitData.getItemId(), baitData);
								}
							}
							break;
						}
						case "rods":
						{
							for (Node rod = listItem.getFirstChild(); rod != null; rod = rod.getNextSibling())
							{
								if ("rod".equalsIgnoreCase(rod.getNodeName()))
								{
									final NamedNodeMap attrs = rod.getAttributes();
									final int itemId = parseInteger(attrs, "itemId");
									if (ItemData.getInstance().getTemplate(itemId) == null)
									{
										LOGGER.info(getClass().getSimpleName() + ": Could not find item with id " + itemId);
										continue;
									}
									
									final int reduceFishingTime = parseInteger(attrs, "reduceFishingTime", 0);
									final float xpMultiplier = parseFloat(attrs, "xpMultiplier", 1f);
									final float spMultiplier = parseFloat(attrs, "spMultiplier", 1f);
									rods.put(itemId, new FishingRod(itemId, reduceFishingTime, xpMultiplier, spMultiplier));
								}
							}
							break;
						}
					}
				}
			}
		}
		
		_baitDistanceMin = baitDistanceMin;
		_baitDistanceMax = baitDistanceMax;
		_expRateMin = expRateMin;
		_expRateMax = expRateMax;
		_spRateMin = spRateMin;
		_spRateMax = spRateMax;
		_rodData = Collections.unmodifiableMap(rods);
		_baitData = Collections.unmodifiableMap(baits);
	}
	
	/**
	 * Retrieves the data for a specific fishing bait.<br>
	 * This method looks up the bait by its unique ID.
	 * @param baitItemId The unique identifier of the bait to find.
	 * @return The {@code FishingBait} object associated with the ID, or {@code null} if not found.
	 */
	public FishingBait getBaitData(int baitItemId)
	{
		return _baitData.get(baitItemId);
	}
	
	/**
	 * Retrieves the data for a specific fishing rod.<br>
	 * This method looks up the {@code FishingRod} object using its unique ID.
	 * @param rodItemId The unique identifier of the fishing rod to find.
	 * @return The {@code FishingRod} object associated with the provided ID, or {@code null} if not found.
	 */
	public FishingRod getRodData(int rodItemId)
	{
		return _rodData.get(rodItemId);
	}
	
	/**
	 * Retrieves the minimum distance for bait.<br>
	 * This value is used to determine fishing range.
	 * @return the minimum bait distance as an {@code int}.
	 */
	public int getBaitDistanceMin()
	{
		return _baitDistanceMin;
	}
	
	/**
	 * Retrieves the maximum distance for bait. <br>
	 * This value is used to determine how far a player can cast with specific bait.
	 * @return The maximum bait distance as an {@code int}.
	 */
	public int getBaitDistanceMax()
	{
		return _baitDistanceMax;
	}
	
	/**
	 * Retrieves the minimum experience rate for fishing.<br>
	 * This value is used to calculate rewards.
	 * @return the minimum {@code double} experience rate.
	 */
	public double getExpRateMin()
	{
		return _expRateMin;
	}
	
	/**
	 * Retrieves the maximum experience rate for fishing.<br>
	 * This value is used to calculate the upper limit of experience gained.
	 * @return The maximum experience rate as a {@code double}.
	 */
	public double getExpRateMax()
	{
		return _expRateMax;
	}
	
	/**
	 * Retrieves the minimum skill point rate.<br>
	 * This value is used to calculate fishing rewards.
	 * @return The minimum {@code double} value for the skill point rate.
	 */
	public double getSpRateMin()
	{
		return _spRateMin;
	}
	
	/**
	 * Retrieves the maximum skill point rate.<br>
	 * This value is used to calculate fishing rewards.
	 * @return The maximum {@code double} value for the skill point rate.
	 */
	public double getSpRateMax()
	{
		return _spRateMax;
	}
	
	/**
	 * Retrieves the global instance of {@code FishingData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access fishing configuration data throughout the game server.
	 * @return The single instance of {@code FishingData}.
	 */
	public static FishingData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final FishingData INSTANCE = new FishingData();
	}
}
