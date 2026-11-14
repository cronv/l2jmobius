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
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.homunculus.HomunculusCreationTemplate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the parsing of XML data for homunculus creation templates.<br>
 * It maps configuration values to {@code HomunculusCreationTemplate} objects.
 * @author Index
 */
public class HomunculusCreationData implements IXmlReader
{
	private static final List<HomunculusCreationTemplate> TEMPLATES = new ArrayList<>();
	
	private HomunculusCreationTemplate _defaultTemplate;
	
	/**
	 * Initializes the {@code HomunculusCreationData} object.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected HomunculusCreationData()
	{
		load();
	}
	
	/**
	 * Loads the homunculus creation data from the XML file.<br>
	 * This method clears existing templates and initializes the default template.<br>
	 * It reads the configuration from {@code data/HomunculusCreationData.xml}.
	 */
	@Override
	public void load()
	{
		TEMPLATES.clear();
		_defaultTemplate = null;
		parseDatapackFile("data/HomunculusCreationData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + TEMPLATES.size() + " templates.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate the creation templates.<br>
	 * This method reads data from the XML file and stores it in the internal list.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		StatSet set;
		Node att;
		NamedNodeMap attrs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("homunculusCreation".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						set = new StatSet();
						for (int i = 0; i < attrs.getLength(); i++)
						{
							att = attrs.item(i);
							set.set(att.getNodeName(), att.getNodeValue());
						}
						
						final int slotId = set.getInt("slotId", 0);
						final Boolean isEnabled = set.getBoolean("isEnabled", false);
						final int grade = set.getInt("grade", 0);
						final Boolean isEvent = set.getBoolean("event", false);
						List<ItemHolder> itemFees = Collections.emptyList();
						Integer[] hpFee = new Integer[2];
						Long[] spFee = new Long[2];
						Integer[] vpFee = new Integer[2];
						long time = 0;
						List<Double[]> chances = Collections.emptyList();
						for (Node b = d.getFirstChild(); b != null; b = b.getNextSibling())
						{
							final String bName = b.getNodeName();
							if ("itemFees".equalsIgnoreCase(bName))
							{
								itemFees = getItemList(b);
							}
							else if ("hpFee".equalsIgnoreCase(bName))
							{
								final NamedNodeMap hpAttrs = b.getAttributes();
								hpFee[0] = Integer.parseInt(hpAttrs.getNamedItem("count").getNodeValue());
								hpFee[1] = Integer.parseInt(hpAttrs.getNamedItem("byUse").getNodeValue());
							}
							else if ("spFee".equalsIgnoreCase(bName))
							{
								final NamedNodeMap spAttrs = b.getAttributes();
								spFee[0] = Long.parseLong(spAttrs.getNamedItem("count").getNodeValue());
								spFee[1] = Long.parseLong(spAttrs.getNamedItem("byUse").getNodeValue());
							}
							else if ("vpFee".equalsIgnoreCase(bName))
							{
								final NamedNodeMap vpAttrs = b.getAttributes();
								vpFee[0] = Integer.parseInt(vpAttrs.getNamedItem("count").getNodeValue());
								vpFee[1] = Integer.parseInt(vpAttrs.getNamedItem("byUse").getNodeValue());
							}
							else if ("time".equalsIgnoreCase(bName))
							{
								time = Long.parseLong(b.getAttributes().getNamedItem("count").getNodeValue());
							}
							else if ("chance".equalsIgnoreCase(bName))
							{
								chances = getChanceList(b);
							}
						}
						
						final HomunculusCreationTemplate template = new HomunculusCreationTemplate(slotId, isEnabled, grade, isEvent, itemFees, hpFee, spFee, vpFee, time, chances);
						TEMPLATES.add(template);
						if (_defaultTemplate == null)
						{
							_defaultTemplate = template;
						}
					}
				}
			}
		}
	}
	
	/**
	 * This method parses a {@code Node} to find all item entries.<br>
	 * It extracts the ID and count for each item found in the XML structure.<br>
	 * The results are stored as a list of {@code ItemHolder} objects.
	 * @param c The {@code Node} containing the item data.
	 * @return A {@code List} of {@code ItemHolder} objects.
	 */
	private List<ItemHolder> getItemList(Node c)
	{
		final List<ItemHolder> items = new ArrayList<>();
		for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
		{
			if ("item".equalsIgnoreCase(b.getNodeName()))
			{
				final NamedNodeMap bAttrs = b.getAttributes();
				final int itemId = Integer.parseInt(bAttrs.getNamedItem("id").getNodeValue());
				final long itemCount = Long.parseLong(bAttrs.getNamedItem("count").getNodeValue());
				items.add(new ItemHolder(itemId, itemCount));
			}
		}
		
		return items;
	}
	
	/**
	 * Retrieves a list of creation chances from the XML node.<br>
	 * It parses the {@code id} and {@code creationChance} attributes for each child node.
	 * @param c The {@code Node} containing the homunculus data.
	 * @return A {@code List} of {@code Double[]} arrays containing the ID and chance values.
	 */
	private List<Double[]> getChanceList(Node c)
	{
		final List<Double[]> chanceList = new ArrayList<>();
		for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
		{
			if ("homunculus".equalsIgnoreCase(b.getNodeName()))
			{
				final NamedNodeMap bAttrs = b.getAttributes();
				final Double[] feeArray = new Double[2];
				feeArray[0] = Double.parseDouble(bAttrs.getNamedItem("id").getNodeValue());
				feeArray[1] = Double.parseDouble(bAttrs.getNamedItem("creationChance").getNodeValue());
				chanceList.add(feeArray);
			}
		}
		
		return chanceList;
	}
	
	/**
	 * Retrieves the default template for homunculus creation.<br>
	 * This is used when no specific item ID is provided.
	 * @return the {@code HomunculusCreationTemplate} used as the default.
	 */
	public HomunculusCreationTemplate getDefaultTemplate()
	{
		return _defaultTemplate;
	}
	
	/**
	 * Retrieves a {@code HomunculusCreationTemplate} based on a specific item ID.<br>
	 * This method searches through all loaded templates to find a match.<br>
	 * It returns the first template that contains the given coupon.
	 * @param itemId The unique identifier of the item to search for.
	 * @return The matching {@code HomunculusCreationTemplate} or {@code null} if no match is found.
	 */
	public HomunculusCreationTemplate getTemplateByItemId(int itemId)
	{
		for (HomunculusCreationTemplate template : TEMPLATES)
		{
			if (template.isInstanceHaveCoupon(itemId))
			{
				return template;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves all loaded homunculus creation templates.<br>
	 * This method returns the internal list of {@code HomunculusCreationTemplate} objects.
	 * @return A {@code Collection} containing all available {@code HomunculusCreationTemplate} instances.
	 */
	public Collection<HomunculusCreationTemplate> getTemplates()
	{
		return TEMPLATES;
	}
	
	/**
	 * Provides access to the singleton instance of {@code HomunculusCreationData}.<br>
	 * Use this method to retrieve the global data container for homunculus creation.
	 * @return The single instance of {@code HomunculusCreationData}.
	 */
	public static HomunculusCreationData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HomunculusCreationData INSTANCE = new HomunculusCreationData();
	}
}
