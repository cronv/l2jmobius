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
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.BoatActionHolder;
import org.l2jmobius.gameserver.data.holders.BoatHolder;
import org.l2jmobius.gameserver.data.holders.BoatStationHolder;
import org.l2jmobius.gameserver.entity.Location;

/**
 * Loads the scheduled boat definitions from {@code data/BoatData.xml}.<br>
 * Holds the boat broadcast points and station routes used by the {@code BoatManager}.
 * @author Mobius
 */
public class BoatData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(BoatData.class.getName());
	
	private final List<BoatHolder> _boats = new ArrayList<>();
	
	protected BoatData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_boats.clear();
		parseDatapackFile("data/BoatData.xml");
		
		final int size = _boats.size();
		if (size > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + size + " boats.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (!d.getNodeName().equals("boat"))
			{
				continue;
			}
			
			final NamedNodeMap dAttrs = d.getAttributes();
			final BoatHolder boat = new BoatHolder(parseInteger(dAttrs, "id"), parseString(dAttrs, "name"), parseInteger(dAttrs, "heading"), parseInteger(dAttrs, "initialDelay"));
			for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
			{
				final String bNodeName = b.getNodeName();
				if (bNodeName.equals("broadcastPoint"))
				{
					final NamedNodeMap attrs = b.getAttributes();
					boat.addBroadcastPoint(new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z")));
				}
				else if (bNodeName.equals("station"))
				{
					// Children must be known before the station is created.
					Location kickLocation = null;
					int ticketId = 0;
					Location ticketLocation = null;
					final List<BoatActionHolder> actions = new ArrayList<>();
					for (Node s = b.getFirstChild(); s != null; s = s.getNextSibling())
					{
						final String sNodeName = s.getNodeName();
						if (sNodeName.equals("kick"))
						{
							final NamedNodeMap attrs = s.getAttributes();
							kickLocation = new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"));
						}
						else if (sNodeName.equals("action"))
						{
							final NamedNodeMap attrs = s.getAttributes();
							actions.add(new BoatActionHolder(parseString(attrs, "sound", null), parseInteger(attrs, "messageId"), parseInteger(attrs, "messageId2", 0), parseInteger(attrs, "wait", 0)));
						}
						else if (sNodeName.equals("ticket"))
						{
							final NamedNodeMap attrs = s.getAttributes();
							ticketId = parseInteger(attrs, "itemId");
							ticketLocation = new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"));
						}
					}
					
					final NamedNodeMap attrs = b.getAttributes();
					final BoatStationHolder station = new BoatStationHolder(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"), parseInteger(attrs, "dockId", 0), parseInteger(attrs, "moveSpeed"), parseInteger(attrs, "rotationSpeed"), parseInteger(attrs, "departDelay", 0), parseInteger(attrs, "busyMessageId", 0), kickLocation, ticketId, ticketLocation);
					for (BoatActionHolder action : actions)
					{
						station.addAction(action);
					}
					
					boat.addStation(station);
				}
			}
			
			if (boat.getStations().size() < 2)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Boat " + boat.getId() + " needs at least two stations.");
				continue;
			}
			
			_boats.add(boat);
		}
	}
	
	/**
	 * @return The scheduled boat definitions.
	 */
	public List<BoatHolder> getBoats()
	{
		return _boats;
	}
	
	public static BoatData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final BoatData INSTANCE = new BoatData();
	}
}
