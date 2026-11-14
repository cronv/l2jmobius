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
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.alchemy.AlchemyCraftData;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and management of alchemy-related data from XML files.<br>
 * It parses configuration settings to define how various items are used in the alchemy system.<br>
 * Recipes are published as an immutable snapshot only after the whole file passes validation, so a failed reload keeps the active recipes instead of exposing an empty or partial map to conversion requests.
 * @author BazookaRpm
 */
public class AlchemyData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AlchemyData.class.getName());
	
	// Recipe State.
	private volatile Map<Long, AlchemyCraftData> _alchemy = Collections.emptyMap();
	
	/**
	 * Initializes a new instance of the {@code AlchemyData} class.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the internal data structures for use.
	 */
	protected AlchemyData()
	{
		load();
	}
	
	/**
	 * Loads the alchemy data from the XML file.<br>
	 * The active recipes are replaced only when the file parses and validates successfully.
	 */
	@Override
	public void load()
	{
		final Map<Long, AlchemyCraftData> previous = _alchemy;
		parseDatapackFile("data/AlchemyData.xml");
		if (_alchemy == previous)
		{
			LOGGER.warning("AlchemyData: No alchemy craft skills were loaded. Keeping the active recipes.");
			return;
		}
		
		LOGGER.info("AlchemyData: Loaded " + _alchemy.size() + " alchemy craft skills.");
	}
	
	/**
	 * Parses the provided {@code Document} and publishes a validated set of recipes.<br>
	 * Every recipe must hold a unique skill key, at least one ingredient and both production outcomes.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning("AlchemyData: Cannot parse a null XML document.");
			return;
		}
		
		final Node rootNode = document.getDocumentElement();
		if ((rootNode == null) || !"list".equalsIgnoreCase(rootNode.getNodeName()))
		{
			throw new IllegalArgumentException("AlchemyData: Invalid root node in " + getFilePath(file) + ". Expected list.");
		}
		
		final Map<Long, AlchemyCraftData> alchemy = new HashMap<>();
		for (Node skillNode = rootNode.getFirstChild(); skillNode != null; skillNode = skillNode.getNextSibling())
		{
			if (!"skill".equalsIgnoreCase(skillNode.getNodeName()))
			{
				continue;
			}
			
			final NamedNodeMap skillAttributes = skillNode.getAttributes();
			final StatSet set = new StatSet();
			for (int i = 0; i < skillAttributes.getLength(); i++)
			{
				final Node attribute = skillAttributes.item(i);
				set.set(attribute.getNodeName(), attribute.getNodeValue());
			}
			
			final AlchemyCraftData craftData = new AlchemyCraftData(set);
			for (Node sectionNode = skillNode.getFirstChild(); sectionNode != null; sectionNode = sectionNode.getNextSibling())
			{
				final String sectionName = sectionNode.getNodeName();
				if ("ingredients".equalsIgnoreCase(sectionName))
				{
					parseIngredients(craftData, sectionNode);
				}
				else if ("production".equalsIgnoreCase(sectionName))
				{
					parseProduction(craftData, sectionNode, file);
				}
			}
			
			final int skillId = set.getInt("id");
			final int skillLevel = set.getInt("level");
			if (craftData.getIngredients().isEmpty() || (craftData.getProductionSuccess() == null) || (craftData.getProductionFailure() == null))
			{
				throw new IllegalArgumentException("AlchemyData: Incomplete recipe for skill " + skillId + " level " + skillLevel + " in " + getFilePath(file) + ".");
			}
			
			if (alchemy.put(SkillData.getSkillHashCode(skillId, skillLevel), craftData) != null)
			{
				throw new IllegalArgumentException("AlchemyData: Duplicate recipe for skill " + skillId + " level " + skillLevel + " in " + getFilePath(file) + ".");
			}
		}
		
		if (alchemy.isEmpty())
		{
			throw new IllegalArgumentException("AlchemyData: No alchemy recipes were found in " + getFilePath(file) + ".");
		}
		
		_alchemy = Collections.unmodifiableMap(alchemy);
	}
	
	/**
	 * Retrieves the crafting data for a specific alchemy skill.<br>
	 * This method looks up the data based on the provided skill ID and level.
	 * @param skillId The unique identifier for the alchemy skill.
	 * @param skillLevel The current level of the skill being queried.
	 * @return The {@code AlchemyCraftData} object associated with the skill, or {@code null} if not found.
	 */
	public AlchemyCraftData getCraftData(int skillId, int skillLevel)
	{
		return _alchemy.get(SkillData.getSkillHashCode(skillId, skillLevel));
	}
	
	/**
	 * Reads every {@code item} child of an ingredients section into the recipe.
	 * @param craftData The recipe being filled.
	 * @param sectionNode The XML node holding the ingredient items.
	 */
	private void parseIngredients(AlchemyCraftData craftData, Node sectionNode)
	{
		for (Node itemNode = sectionNode.getFirstChild(); itemNode != null; itemNode = itemNode.getNextSibling())
		{
			if (!"item".equalsIgnoreCase(itemNode.getNodeName()))
			{
				continue;
			}
			
			final NamedNodeMap itemAttributes = itemNode.getAttributes();
			craftData.addIngredient(new ItemHolder(parseInteger(itemAttributes, "id"), parseInteger(itemAttributes, "count")));
		}
	}
	
	/**
	 * Reads every {@code item} child of a production section into the recipe.<br>
	 * An unsupported production type rejects the whole file.
	 * @param craftData The recipe being filled.
	 * @param sectionNode The XML node holding the production items.
	 * @param file The {@code File} source of the XML data.
	 */
	private void parseProduction(AlchemyCraftData craftData, Node sectionNode, File file)
	{
		for (Node itemNode = sectionNode.getFirstChild(); itemNode != null; itemNode = itemNode.getNextSibling())
		{
			if (!"item".equalsIgnoreCase(itemNode.getNodeName()))
			{
				continue;
			}
			
			final NamedNodeMap itemAttributes = itemNode.getAttributes();
			final String type = parseString(itemAttributes, "type");
			final boolean onSuccess = "ON_SUCCESS".equalsIgnoreCase(type);
			if (!onSuccess && !"ON_FAILURE".equalsIgnoreCase(type))
			{
				throw new IllegalArgumentException("AlchemyData: Unknown production type " + type + " in " + getFilePath(file) + ".");
			}
			
			final ItemHolder product = new ItemHolder(parseInteger(itemAttributes, "id"), parseInteger(itemAttributes, "count"));
			if (onSuccess)
			{
				craftData.setProductionSuccess(product);
			}
			else
			{
				craftData.setProductionFailure(product);
			}
		}
	}
	
	/**
	 * Converts a {@code File} object into its absolute path string.<br>
	 * Returns the string {@code <unknown>} if the input is {@code null}.
	 * @param file The {@code File} to describe.
	 * @return The absolute path, or {@code <unknown>} when the file is not available.
	 */
	private String getFilePath(File file)
	{
		return file != null ? file.getAbsolutePath() : "<unknown>";
	}
	
	/**
	 * Provides access to the singleton instance of {@code AlchemyData}.<br>
	 * Use this method to retrieve the global data handler.
	 * @return The single instance of {@code AlchemyData}.
	 */
	public static AlchemyData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AlchemyData INSTANCE = new AlchemyData();
	}
}
