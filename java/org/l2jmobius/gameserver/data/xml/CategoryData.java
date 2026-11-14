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
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.enums.CategoryType;

/**
 * This class handles the loading and storage of category data from XML files.<br>
 * It maps {@code CategoryType} values to their respective configurations within the game server.<br>
 * The catalog is published only after the whole file parses, so a reload never exposes an empty or partially filled map to the membership checks that read it.
 * @author BazookaRpm
 */
public class CategoryData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CategoryData.class.getName());
	
	private volatile Map<CategoryType, Set<Integer>> _categories = Collections.emptyMap();
	
	/**
	 * Initializes a new instance of {@code CategoryData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the internal data structures for use.
	 */
	protected CategoryData()
	{
		load();
	}
	
	/**
	 * Loads the category data from the XML file.<br>
	 * The active categories are replaced only when the file parses successfully.
	 */
	@Override
	public void load()
	{
		final Map<CategoryType, Set<Integer>> previous = _categories;
		parseDatapackFile("data/CategoryData.xml");
		if (_categories == previous)
		{
			LOGGER.warning("CategoryData: Could not load categories. Keeping the active data.");
			return;
		}
		
		LOGGER.info("CategoryData: Loaded " + _categories.size() + " categories.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate category data.<br>
	 * It reads entries from the XML file and maps them to {@code CategoryType}.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} source used for identifying the data context.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning("CategoryData: Cannot parse a null XML document.");
			return;
		}
		
		final Node rootNode = document.getDocumentElement();
		if ((rootNode == null) || !"list".equalsIgnoreCase(rootNode.getNodeName()))
		{
			throw new IllegalArgumentException("CategoryData: Invalid root node in " + getFilePath(file) + ". Expected list.");
		}
		
		final Map<CategoryType, Set<Integer>> categories = new EnumMap<>(CategoryType.class);
		for (Node categoryNode = rootNode.getFirstChild(); categoryNode != null; categoryNode = categoryNode.getNextSibling())
		{
			if (!"category".equalsIgnoreCase(categoryNode.getNodeName()))
			{
				continue;
			}
			
			final String name = parseString(categoryNode.getAttributes(), "name");
			if (name == null)
			{
				throw new IllegalArgumentException("CategoryData: Missing category name in " + getFilePath(file) + ".");
			}
			
			final CategoryType categoryType = CategoryType.findByName(name);
			if (categoryType == null)
			{
				LOGGER.warning("CategoryData: Can't find category by name: " + name);
				continue;
			}
			
			final Set<Integer> ids = new HashSet<>();
			for (Node idNode = categoryNode.getFirstChild(); idNode != null; idNode = idNode.getNextSibling())
			{
				if (!"id".equalsIgnoreCase(idNode.getNodeName()))
				{
					continue;
				}
				
				ids.add(Integer.valueOf(idNode.getTextContent()));
			}
			
			// Kept as last wins, matching the tolerance shown to unknown category names above.
			if (categories.put(categoryType, Collections.unmodifiableSet(ids)) != null)
			{
				LOGGER.warning("CategoryData: Duplicate category " + categoryType + " in " + getFilePath(file) + ". The last definition is used.");
			}
		}
		
		if (categories.isEmpty())
		{
			throw new IllegalArgumentException("CategoryData: No categories were found in " + getFilePath(file) + ".");
		}
		
		_categories = categories;
	}
	
	/**
	 * Checks if a specific ID belongs to a given category.<br>
	 * This method looks up the set of IDs associated with the {@code CategoryType}.<br>
	 * It returns {@code true} if the ID is found in that set.
	 * @param type The {@code CategoryType} to check against.
	 * @param id The unique identifier to verify.
	 * @return {@code true} if the ID exists in the category, otherwise {@code false}.
	 */
	public boolean isInCategory(CategoryType type, int id)
	{
		final Set<Integer> category = getCategoryByType(type);
		if (category == null)
		{
			LOGGER.warning("CategoryData: Can't find category type: " + type);
			return false;
		}
		
		return category.contains(id);
	}
	
	/**
	 * Retrieves all category IDs associated with a specific type.<br>
	 * This method looks up the data in the internal map.
	 * @param type The {@code CategoryType} to filter by.
	 * @return An unmodifiable {@code Set<Integer>} containing the matching IDs, or {@code null} if no matches exist.
	 */
	public Set<Integer> getCategoryByType(CategoryType type)
	{
		return _categories.get(type);
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
	 * Retrieves the global instance of {@code CategoryData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access category data throughout the application.
	 * @return The single instance of {@code CategoryData}.
	 */
	public static CategoryData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CategoryData INSTANCE = new CategoryData();
	}
}
