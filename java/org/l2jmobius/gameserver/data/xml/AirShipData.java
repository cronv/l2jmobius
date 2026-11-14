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
import org.l2jmobius.gameserver.data.holders.AirShipHolder;
import org.l2jmobius.gameserver.data.holders.AirShipPortHolder;
import org.l2jmobius.gameserver.data.holders.AirShipStepHolder;
import org.l2jmobius.gameserver.entity.Location;

/**
 * Loads the scheduled airship definitions from {@code data/AirShipData.xml}.<br>
 * Holds the spawn locations and route steps used by the {@code AirShipManager}.
 * @author Mobius
 */
public class AirShipData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AirShipData.class.getName());
	
	private final List<AirShipHolder> _airships = new ArrayList<>();
	
	protected AirShipData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_airships.clear();
		parseDatapackFile("data/AirShipData.xml");
		
		final int size = _airships.size();
		if (size > 0)
		{
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + size + " airships.");
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (!d.getNodeName().equals("airship"))
			{
				continue;
			}
			
			// The start location must be known before the airship is created.
			Location start = null;
			for (Node a = d.getFirstChild(); a != null; a = a.getNextSibling())
			{
				if (a.getNodeName().equals("start"))
				{
					final NamedNodeMap attrs = a.getAttributes();
					start = new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"), parseInteger(attrs, "heading", 0));
				}
			}
			
			if (start == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Airship needs a start location.");
				continue;
			}
			
			final NamedNodeMap dAttrs = d.getAttributes();
			final AirShipHolder airship = new AirShipHolder(parseInteger(dAttrs, "id"), parseInteger(dAttrs, "moveSpeed"), parseInteger(dAttrs, "rotationSpeed"), parseInteger(dAttrs, "initialDelay"), start);
			int portCount = 0;
			for (Node a = d.getFirstChild(); a != null; a = a.getNextSibling())
			{
				final String aNodeName = a.getNodeName();
				if (aNodeName.equals("point"))
				{
					final NamedNodeMap attrs = a.getAttributes();
					airship.addStep(new AirShipStepHolder(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"), -1, null));
				}
				else if (aNodeName.equals("teleport"))
				{
					final NamedNodeMap attrs = a.getAttributes();
					airship.addStep(new AirShipStepHolder(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"), parseInteger(attrs, "heading", 0), null));
				}
				else if (aNodeName.equals("port"))
				{
					// The oust location must be known before the port is created.
					Location oustLocation = null;
					for (Node p = a.getFirstChild(); p != null; p = p.getNextSibling())
					{
						if (p.getNodeName().equals("oust"))
						{
							final NamedNodeMap attrs = p.getAttributes();
							oustLocation = new Location(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"));
						}
					}
					
					final NamedNodeMap attrs = a.getAttributes();
					final AirShipPortHolder port = new AirShipPortHolder(parseInteger(attrs, "dockId"), parseInteger(attrs, "stopover"), parseInteger(attrs, "announcerNpcId"), parseInteger(attrs, "arriveNpcStringId"), parseInteger(attrs, "departNpcStringId"), oustLocation);
					airship.addStep(new AirShipStepHolder(parseInteger(attrs, "x"), parseInteger(attrs, "y"), parseInteger(attrs, "z"), -1, port));
					portCount++;
				}
			}
			
			if (portCount < 1)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Airship " + airship.getId() + " needs at least one port.");
				continue;
			}
			
			_airships.add(airship);
		}
	}
	
	/**
	 * @return The scheduled airship definitions.
	 */
	public List<AirShipHolder> getAirShips()
	{
		return _airships;
	}
	
	public static AirShipData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AirShipData INSTANCE = new AirShipData();
	}
}
