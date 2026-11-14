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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.Henna;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and management of {@code Henna} data from XML files.<br>
 * It maps specific attributes to {@link Henna} objects for use in the game server.
 * @author Zoey76, Mobius
 */
public class HennaData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(HennaData.class.getName());
	
	private final Map<Integer, Henna> _hennaList = new HashMap<>();
	
	/**
	 * Initializes a new instance of the {@code HennaData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected HennaData()
	{
		load();
	}
	
	/**
	 * Loads the henna data from the XML file.<br>
	 * This method clears the existing list and parses the new data.<br>
	 * It logs the total number of items loaded to the console.
	 */
	@Override
	public void load()
	{
		_hennaList.clear();
		parseDatapackFile("data/stats/hennaList.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _hennaList.size() + " henna data.");
	}
	
	/**
	 * Parses the provided {@code Document} to load henna data.<br>
	 * This method iterates through all nodes in the XML file.<br>
	 * It identifies and processes every {@code henna} entry found within a {@code list}.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equals(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("henna".equals(d.getNodeName()))
					{
						parseHenna(d);
					}
				}
			}
		}
	}
	
	/**
	 * Parses a {@code Node} to create and store a new {@code Henna} object.<br>
	 * This method extracts attributes for stats, wear costs, and skills from the XML structure.<br>
	 * The resulting {@code Henna} is added to the internal list using its unique ID.
	 * @param d The {@code Node} representing the henna data in the XML document.
	 */
	private void parseHenna(Node d)
	{
		final StatSet set = new StatSet();
		final List<PlayerClass> wearClassIds = new ArrayList<>();
		final List<Skill> skills = new ArrayList<>();
		NamedNodeMap attrs = d.getAttributes();
		Node attr;
		int attrsLen = attrs.getLength();
		for (int i = 0; i < attrsLen; i++)
		{
			attr = attrs.item(i);
			set.set(attr.getNodeName(), attr.getNodeValue());
		}
		
		for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
		{
			final String name = c.getNodeName();
			attrs = c.getAttributes();
			switch (name)
			{
				case "stats":
				{
					attrsLen = attrs.getLength();
					for (int i = 0; i < attrsLen; i++)
					{
						attr = attrs.item(i);
						set.set(attr.getNodeName(), attr.getNodeValue());
					}
					break;
				}
				case "wear":
				{
					attr = attrs.getNamedItem("count");
					set.set("wear_count", attr.getNodeValue());
					attr = attrs.getNamedItem("fee");
					set.set("wear_fee", attr.getNodeValue());
					break;
				}
				case "cancel":
				{
					attr = attrs.getNamedItem("count");
					set.set("cancel_count", attr.getNodeValue());
					attr = attrs.getNamedItem("fee");
					set.set("cancel_fee", attr.getNodeValue());
					break;
				}
				case "duration":
				{
					attr = attrs.getNamedItem("time"); // in minutes
					set.set("duration", attr.getNodeValue());
					break;
				}
				case "skill":
				{
					skills.add(SkillData.getInstance().getSkill(parseInteger(attrs, "id"), parseInteger(attrs, "level")));
					break;
				}
				case "classId":
				{
					wearClassIds.add(PlayerClass.getPlayerClass(Integer.parseInt(c.getTextContent())));
					break;
				}
			}
		}
		
		final Henna henna = new Henna(set);
		henna.setSkills(skills);
		henna.setWearClassIds(wearClassIds);
		_hennaList.put(henna.getDyeId(), henna);
	}
	
	/**
	 * Retrieves a {@code Henna} object based on its unique identifier.<br>
	 * This method looks up the data in the internal list.
	 * @param id The unique integer ID of the henna to retrieve.
	 * @return The {@code Henna} object associated with the given ID, or {@code null} if not found.
	 */
	public Henna getHenna(int id)
	{
		return _hennaList.get(id);
	}
	
	/**
	 * Retrieves a list of {@code Henna} items for a specific player class.<br>
	 * This method filters the available data based on the provided {@code PlayerClass}.
	 * @param playerClass The {@code PlayerClass} to filter the results by.
	 * @return A {@code List} of {@code Henna} objects allowed for the given class.
	 */
	public List<Henna> getHennaList(PlayerClass playerClass)
	{
		final List<Henna> list = new ArrayList<>();
		for (Henna henna : _hennaList.values())
		{
			if (henna.isAllowedClass(playerClass))
			{
				list.add(henna);
			}
		}
		
		return list;
	}
	
	/**
	 * Provides access to the singleton instance of {@code HennaData}.<br>
	 * Use this method to get the global data handler for henna items.
	 * @return The single instance of {@code HennaData}.
	 */
	public static HennaData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HennaData INSTANCE = new HennaData();
	}
}
