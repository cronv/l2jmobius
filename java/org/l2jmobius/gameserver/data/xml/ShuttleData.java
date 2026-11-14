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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.holders.creature.VehiclePathPoint;
import org.l2jmobius.gameserver.entity.actor.instance.Shuttle;
import org.l2jmobius.gameserver.entity.actor.stat.VehicleStat;
import org.l2jmobius.gameserver.entity.actor.templates.CreatureTemplate;
import org.l2jmobius.gameserver.entity.shuttle.ShuttleDataHolder;
import org.l2jmobius.gameserver.entity.shuttle.ShuttleEngine;
import org.l2jmobius.gameserver.entity.shuttle.ShuttleStop;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of shuttle data from XML files.<br>
 * It provides configuration details for {@code Shuttle} entities within the game world.
 * @author UnAfraid, BazookaRpm
 */
public class ShuttleData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ShuttleData.class.getName());
	
	private final Map<Integer, ShuttleDataHolder> _shuttles = new ConcurrentHashMap<>();
	private final Map<Integer, Shuttle> _shuttleInstances = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of {@code ShuttleData}.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected ShuttleData()
	{
		load();
	}
	
	/**
	 * Loads the shuttle data from the configuration file.<br>
	 * This method clears existing instances before parsing the XML data.<br>
	 * It then initializes the internal shuttle maps.
	 */
	@Override
	public synchronized void load()
	{
		if (!_shuttleInstances.isEmpty())
		{
			for (Shuttle shuttle : _shuttleInstances.values())
			{
				shuttle.deleteMe();
			}
			
			_shuttleInstances.clear();
		}
		
		_shuttles.clear();
		parseDatapackFile("data/ShuttleData.xml");
		init();
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _shuttles.size() + " shuttles.");
	}
	
	/**
	 * Parses the shuttle configuration from an XML document.<br>
	 * This method populates the internal shuttle data maps.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} object representing the source of the XML.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("shuttle".equalsIgnoreCase(d.getNodeName()))
					{
						final ShuttleDataHolder data = new ShuttleDataHolder(new StatSet(parseAttributes(d)));
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							final String bName = b.getNodeName();
							if ("doors".equalsIgnoreCase(bName))
							{
								for (Node a = b.getFirstChild(); a != null; a = a.getNextSibling())
								{
									if ("door".equalsIgnoreCase(a.getNodeName()))
									{
										data.addDoor(parseInteger(a.getAttributes(), "id"));
									}
								}
							}
							else if ("stops".equalsIgnoreCase(bName))
							{
								for (Node a = b.getFirstChild(); a != null; a = a.getNextSibling())
								{
									if ("stop".equalsIgnoreCase(a.getNodeName()))
									{
										final ShuttleStop stop = new ShuttleStop(parseInteger(a.getAttributes(), "id"));
										for (Node z = a.getFirstChild(); z != null; z = z.getNextSibling())
										{
											if ("dimension".equalsIgnoreCase(z.getNodeName()))
											{
												final NamedNodeMap dimensionAttrs = z.getAttributes();
												stop.addDimension(new Location(parseInteger(dimensionAttrs, "x"), parseInteger(dimensionAttrs, "y"), parseInteger(dimensionAttrs, "z")));
											}
										}
										
										data.addStop(stop);
									}
								}
							}
							else if ("routes".equalsIgnoreCase(bName))
							{
								for (Node a = b.getFirstChild(); a != null; a = a.getNextSibling())
								{
									if ("route".equalsIgnoreCase(a.getNodeName()))
									{
										final List<Location> locs = new ArrayList<>();
										for (Node z = a.getFirstChild(); z != null; z = z.getNextSibling())
										{
											if ("loc".equalsIgnoreCase(z.getNodeName()))
											{
												final NamedNodeMap locAttrs = z.getAttributes();
												locs.add(new Location(parseInteger(locAttrs, "x"), parseInteger(locAttrs, "y"), parseInteger(locAttrs, "z")));
											}
										}
										
										final VehiclePathPoint[] route = new VehiclePathPoint[locs.size()];
										int i = 0;
										for (Location loc : locs)
										{
											route[i++] = new VehiclePathPoint(loc);
										}
										
										data.addRoute(route);
									}
								}
							}
						}
						
						_shuttles.put(data.getId(), data);
					}
				}
			}
		}
	}
	
	/**
	 * Initializes the shuttle instances based on loaded data.<br>
	 * Creates new {@code Shuttle} objects for every entry in the {@code _shuttles} map.<br>
	 * Configures their stats, locations, and engines before spawning them.
	 */
	private void init()
	{
		for (ShuttleDataHolder data : _shuttles.values())
		{
			final Shuttle shuttle = new Shuttle(new CreatureTemplate(new StatSet()));
			shuttle.setData(data);
			final Location shuttleLocation = data.getLocation();
			shuttle.setHeading(shuttleLocation.getHeading());
			shuttle.setLocationInvisible(shuttleLocation);
			shuttle.spawnMe();
			final VehicleStat shuttleStat = shuttle.getStat();
			shuttleStat.setMoveSpeed(300);
			shuttleStat.setRotationSpeed(0);
			shuttle.registerEngine(new ShuttleEngine(data, shuttle));
			shuttle.runEngine(1000);
			_shuttleInstances.put(shuttle.getObjectId(), shuttle);
		}
	}
	
	/**
	 * Retrieves a {@code Shuttle} instance based on its unique identifier.<br>
	 * This method searches through all active shuttle instances.<br>
	 * It returns the first match found for either the object ID or the internal ID.
	 * @param id The unique identifier to search for.
	 * @return The matching {@code Shuttle} object, or {@code null} if no match is found.
	 */
	public Shuttle getShuttle(int id)
	{
		for (Shuttle shuttle : _shuttleInstances.values())
		{
			if ((shuttle.getObjectId() == id) || (shuttle.getId() == id))
			{
				return shuttle;
			}
		}
		
		return null;
	}
	
	/**
	 * Provides access to the singleton instance of {@code ShuttleData}.<br>
	 * Use this method to get the global data handler for shuttles.
	 * @return The single instance of {@code ShuttleData}.
	 */
	public static ShuttleData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ShuttleData INSTANCE = new ShuttleData();
	}
}
