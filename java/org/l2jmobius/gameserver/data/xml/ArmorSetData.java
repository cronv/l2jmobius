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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ArmorsetSkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;

/**
 * Loads armor-set definitions from the datapack XML directory.<br>
 * This class provides constant-time lookups by chest item ID and ensures that only successful loads update the active catalog.
 * @author BazookaRpm, Mobius
 */
public class ArmorSetData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ArmorSetData.class.getName());
	
	// Data Paths.
	private static final String DATA_DIRECTORY = "data/stats/armorsets";
	
	// XML Nodes.
	private static final String LIST_NODE = "list";
	private static final String SET_NODE = "set";
	private static final String REQUIRED_ITEMS_NODE = "requiredItems";
	private static final String OPTIONAL_ITEMS_NODE = "optionalItems";
	private static final String ITEM_NODE = "item";
	private static final String SKILLS_NODE = "skills";
	private static final String SKILL_NODE = "skill";
	private static final String STATS_NODE = "stats";
	private static final String STAT_NODE = "stat";
	
	// XML Attributes.
	private static final String ID_ATTRIBUTE = "id";
	private static final String MINIMUM_PIECES_ATTRIBUTE = "minimumPieces";
	private static final String VISUAL_ATTRIBUTE = "visual";
	private static final String LEVEL_ATTRIBUTE = "level";
	private static final String MINIMUM_ENCHANT_ATTRIBUTE = "minimumEnchant";
	private static final String OPTIONAL_ATTRIBUTE = "optional";
	private static final String SLOT_MASK_ATTRIBUTE = "slotMask";
	private static final String BOOK_SLOT_ATTRIBUTE = "bookSlot";
	private static final String TYPE_ATTRIBUTE = "type";
	private static final String VALUE_ATTRIBUTE = "val";
	
	// Catalog State.
	private static final ArmorSetState EMPTY_STATE = new ArmorSetState(new ArmorSet[0], newItemArray(0), 0);
	private final Map<Integer, ArmorSet> _armorSets = new ConcurrentHashMap<>();
	private volatile ArmorSetState _state = EMPTY_STATE;
	
	/**
	 * Initializes a new instance of the {@code ArmorSetData} class.<br>
	 * This constructor automatically triggers the {@code load()} method to populate data.
	 */
	protected ArmorSetData()
	{
		load();
	}
	
	/**
	 * Loads all armor set definitions from the data directory.<br>
	 * This method parses all XML files into a new internal state.<br>
	 * If no armor sets are loaded, the current active catalog is preserved.
	 */
	@Override
	public void load()
	{
		_armorSets.clear();
		parseDatapackDirectory(DATA_DIRECTORY, false);
		if (_armorSets.isEmpty())
		{
			LOGGER.warning("ArmorSetData: No armor sets were loaded. Keeping the active armor-set catalog.");
			return;
		}
		
		_state = createState(_armorSets);
		_armorSets.clear();
		LOGGER.info("ArmorSetData: Loaded " + _state.getCount() + " armor sets.");
	}
	
	/**
	 * Parses an XML document and adds its armor set definitions to the load buffer.<br>
	 * The buffer is converted into the active state once the whole directory is parsed.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning("ArmorSetData: Cannot parse a null XML document.");
			return;
		}
		
		parseArmorSets(document, file, _armorSets);
	}
	
	/**
	 * Retrieves an {@code ArmorSet} based on its unique set ID.<br>
	 * This method searches the current data state for a matching set.
	 * @param setId The unique identifier for the armor set.
	 * @return The corresponding {@code ArmorSet} object, or {@code null} if not found.
	 */
	public ArmorSet getSet(int setId)
	{
		return _state.getSet(setId);
	}
	
	/**
	 * Retrieves all {@code ArmorSet} objects associated with a specific item ID.<br>
	 * This method searches the current data state for matching sets.
	 * @param itemId The unique identifier of the item to search for.
	 * @return A {@code List} of {@code ArmorSet} objects matching the given ID, or an empty list.
	 */
	public List<ArmorSet> getSets(int itemId)
	{
		return _state.getSets(itemId);
	}
	
	/**
	 * Parses armor set definitions from an XML document.<br>
	 * Validates the root node and iterates through all set nodes.<br>
	 * Adds each valid {@code ArmorSet} to the provided map using its set ID as the key.
	 * @param document The {@code Document} containing the XML data.
	 * @param file The {@code File} associated with the current document.
	 * @param armorSets The map where parsed {@code ArmorSet} objects are stored.
	 */
	private void parseArmorSets(Document document, File file, Map<Integer, ArmorSet> armorSets)
	{
		final Node rootNode = document.getDocumentElement();
		if ((rootNode == null) || !LIST_NODE.equalsIgnoreCase(rootNode.getNodeName()))
		{
			throw new IllegalArgumentException("ArmorSetData: Invalid root node in " + getFilePath(file) + ". Expected " + LIST_NODE + ".");
		}
		
		for (Node setNode = rootNode.getFirstChild(); setNode != null; setNode = setNode.getNextSibling())
		{
			if (!SET_NODE.equalsIgnoreCase(setNode.getNodeName()))
			{
				continue;
			}
			
			final ArmorSet armorSet = parseArmorSet(setNode, file);
			if (armorSets.putIfAbsent(armorSet.getId(), armorSet) != null)
			{
				LOGGER.warning("ArmorSetData: Duplicate set entry with id " + armorSet.getId() + " in " + getFilePath(file) + ".");
			}
		}
	}
	
	/**
	 * Converts an XML node into an {@code ArmorSet} object.<br>
	 * This method reads the required items, optional items, skills and stats of a single set.
	 * @param setNode The XML node representing a single armor set.
	 * @param file The source file where the data is located.
	 * @return A new {@code ArmorSet} populated with data from the node.
	 */
	private ArmorSet parseArmorSet(Node setNode, File file)
	{
		final NamedNodeMap setAttrs = setNode.getAttributes();
		final int id = parseInteger(setAttrs, ID_ATTRIBUTE);
		final int minimumPieces = parseInteger(setAttrs, MINIMUM_PIECES_ATTRIBUTE, 0);
		final boolean isVisual = parseBoolean(setAttrs, VISUAL_ATTRIBUTE, false);
		final Set<Integer> requiredItems = new LinkedHashSet<>();
		final Set<Integer> optionalItems = new LinkedHashSet<>();
		final List<ArmorsetSkillHolder> skills = new ArrayList<>();
		final Map<BaseStat, Double> stats = new LinkedHashMap<>();
		for (Node innerSetNode = setNode.getFirstChild(); innerSetNode != null; innerSetNode = innerSetNode.getNextSibling())
		{
			switch (innerSetNode.getNodeName())
			{
				case REQUIRED_ITEMS_NODE:
				{
					forEach(innerSetNode, b -> ITEM_NODE.equals(b.getNodeName()), node ->
					{
						final NamedNodeMap attrs = node.getAttributes();
						final int itemId = parseInteger(attrs, ID_ATTRIBUTE);
						final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
						if (item == null)
						{
							LOGGER.warning("ArmorSetData: Attempting to register non existing required item: " + itemId + " to a set: " + file.getName());
						}
						else if (!requiredItems.add(itemId))
						{
							LOGGER.warning("ArmorSetData: Attempting to register duplicate required item " + item + " to a set: " + file.getName());
						}
					});
					break;
				}
				case OPTIONAL_ITEMS_NODE:
				{
					forEach(innerSetNode, b -> ITEM_NODE.equals(b.getNodeName()), node ->
					{
						final NamedNodeMap attrs = node.getAttributes();
						final int itemId = parseInteger(attrs, ID_ATTRIBUTE);
						final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
						if (item == null)
						{
							LOGGER.warning("ArmorSetData: Attempting to register non existing optional item: " + itemId + " to a set: " + file.getName());
						}
						else if (!optionalItems.add(itemId))
						{
							LOGGER.warning("ArmorSetData: Attempting to register duplicate optional item " + item + " to a set: " + file.getName());
						}
					});
					break;
				}
				case SKILLS_NODE:
				{
					forEach(innerSetNode, b -> SKILL_NODE.equals(b.getNodeName()), node ->
					{
						final NamedNodeMap attrs = node.getAttributes();
						final int skillId = parseInteger(attrs, ID_ATTRIBUTE);
						final int skillLevel = parseInteger(attrs, LEVEL_ATTRIBUTE);
						final int minPieces = parseInteger(attrs, MINIMUM_PIECES_ATTRIBUTE, minimumPieces);
						final int minEnchant = parseInteger(attrs, MINIMUM_ENCHANT_ATTRIBUTE, 0);
						final boolean isOptional = parseBoolean(attrs, OPTIONAL_ATTRIBUTE, false);
						final int artifactSlotMask = parseInteger(attrs, SLOT_MASK_ATTRIBUTE, 0);
						final int artifactBookSlot = parseInteger(attrs, BOOK_SLOT_ATTRIBUTE, 0);
						skills.add(new ArmorsetSkillHolder(skillId, skillLevel, minPieces, minEnchant, isOptional, artifactSlotMask, artifactBookSlot));
					});
					break;
				}
				case STATS_NODE:
				{
					forEach(innerSetNode, b -> STAT_NODE.equals(b.getNodeName()), node ->
					{
						final NamedNodeMap attrs = node.getAttributes();
						stats.put(parseEnum(attrs, BaseStat.class, TYPE_ATTRIBUTE), parseDouble(attrs, VALUE_ATTRIBUTE));
					});
					break;
				}
			}
		}
		
		return new ArmorSet(id, minimumPieces, isVisual, requiredItems, optionalItems, skills, stats);
	}
	
	/**
	 * Creates a new {@code ArmorSetState} from the loaded map.<br>
	 * This method validates that the map is not empty.<br>
	 * It builds the set-ID index and the item-ID reverse index.
	 * @param armorSets A map containing the loaded {@code ArmorSet} objects.
	 * @return A new {@code ArmorSetState} instance.
	 */
	private ArmorSetState createState(Map<Integer, ArmorSet> armorSets)
	{
		if (armorSets.isEmpty())
		{
			throw new IllegalStateException("ArmorSetData: No valid armor sets were loaded.");
		}
		
		int highestSetId = 0;
		int highestItemId = -1;
		for (Entry<Integer, ArmorSet> entry : armorSets.entrySet())
		{
			final int setId = entry.getKey().intValue();
			if (setId > highestSetId)
			{
				highestSetId = setId;
			}
			
			final ArmorSet set = entry.getValue();
			for (int itemId : set.getRequiredItems())
			{
				if (itemId > highestItemId)
				{
					highestItemId = itemId;
				}
			}
			
			for (int itemId : set.getOptionalItems())
			{
				if (itemId > highestItemId)
				{
					highestItemId = itemId;
				}
			}
		}
		
		final ArmorSet[] armorSetsBySetId = new ArmorSet[highestSetId + 1];
		for (Entry<Integer, ArmorSet> entry : armorSets.entrySet())
		{
			armorSetsBySetId[entry.getKey().intValue()] = entry.getValue();
		}
		
		final List<ArmorSet>[] armorSetsByItemId = newItemArray(highestItemId + 1);
		for (ArmorSet set : armorSetsBySetId)
		{
			if (set == null)
			{
				continue;
			}
			
			for (int itemId : set.getRequiredItems())
			{
				addToItemIndex(armorSetsByItemId, itemId, set);
			}
			
			for (int itemId : set.getOptionalItems())
			{
				addToItemIndex(armorSetsByItemId, itemId, set);
			}
		}
		
		return new ArmorSetState(armorSetsBySetId, armorSetsByItemId, armorSets.size());
	}
	
	/**
	 * Registers an armor set under a specific item ID in the reverse index.<br>
	 * The bucket list is created on first use.
	 * @param armorSetsByItemId The reverse index array to update.
	 * @param itemId The item ID acting as the index.
	 * @param set The {@code ArmorSet} to register.
	 */
	private void addToItemIndex(List<ArmorSet>[] armorSetsByItemId, int itemId, ArmorSet set)
	{
		List<ArmorSet> sets = armorSetsByItemId[itemId];
		if (sets == null)
		{
			sets = new ArrayList<>();
			armorSetsByItemId[itemId] = sets;
		}
		
		sets.add(set);
	}
	
	/**
	 * Creates a typed array of armor-set lists.<br>
	 * This centralizes the unchecked generic array allocation.
	 * @param size The length of the array to create.
	 * @return A new array of {@code List<ArmorSet>} references.
	 */
	@SuppressWarnings("unchecked")
	private static List<ArmorSet>[] newItemArray(int size)
	{
		return new List[size];
	}
	
	/**
	 * Converts a {@code File} object into its absolute path string.<br>
	 * Returns the string {@code <unknown>} if the input is {@code null}.
	 * @param file The {@code File} to convert.
	 * @return The absolute path of the file as a {@code String}.
	 */
	private String getFilePath(File file)
	{
		return file != null ? file.getAbsolutePath() : "<unknown>";
	}
	
	/**
	 * Stores a stable lookup index for armor-set readers.<br>
	 * Publishes complete arrays and the registered entry count together.
	 * <ul>
	 * <li>Provides constant-time lookup by set ID.</li>
	 * <li>Provides constant-time reverse lookup by item ID.</li>
	 * <li>Rejects negative and out-of-range IDs.</li>
	 * </ul>
	 */
	private static final class ArmorSetState
	{
		private final ArmorSet[] _armorSets;
		private final List<ArmorSet>[] _itemSets;
		private final int _count;
		
		/**
		 * Creates an armor-set lookup state.
		 * @param armorSets
		 * @param itemSets
		 * @param count
		 */
		private ArmorSetState(ArmorSet[] armorSets, List<ArmorSet>[] itemSets, int count)
		{
			_armorSets = armorSets;
			_itemSets = itemSets;
			_count = count;
		}
		
		/**
		 * Finds an armor set by set ID.
		 * @param setId
		 * @return the armor set, or {@code null} when absent.
		 */
		private ArmorSet getSet(int setId)
		{
			if ((setId < 0) || (setId >= _armorSets.length))
			{
				return null;
			}
			
			return _armorSets[setId];
		}
		
		/**
		 * Finds all armor sets registered under an item ID.
		 * @param itemId
		 * @return the armor sets, or an empty list when absent.
		 */
		private List<ArmorSet> getSets(int itemId)
		{
			if ((itemId < 0) || (itemId >= _itemSets.length))
			{
				return Collections.emptyList();
			}
			
			final List<ArmorSet> sets = _itemSets[itemId];
			return sets != null ? sets : Collections.emptyList();
		}
		
		/**
		 * Gets the number of armor sets in this state.
		 * @return the armor-set count.
		 */
		private int getCount()
		{
			return _count;
		}
	}
	
	/**
	 * Retrieves the singleton instance of {@code ArmorSetData}.<br>
	 * This provides a global access point to the loaded armor set data.
	 * @return The single instance of {@code ArmorSetData}.
	 */
	public static ArmorSetData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Holds the lazily initialized singleton instance.<br>
	 * Uses class initialization for thread-safe publication.
	 * <ul>
	 * <li>Creates the loader on first access.</li>
	 * <li>Publishes exactly one instance.</li>
	 * </ul>
	 */
	private static class SingletonHolder
	{
		protected static final ArmorSetData INSTANCE = new ArmorSetData();
	}
}
