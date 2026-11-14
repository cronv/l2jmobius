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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulFee;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulOption;
import org.l2jmobius.gameserver.mechanics.ensoul.EnsoulStone;

/**
 * This class handles the loading and storage of data for {@code EnsoulStone} objects from XML files.<br>
 * It provides configuration details such as fees, options, and types used by the ensoul mechanics.
 * @author UnAfraid, BazookaRpm
 */
public class EnsoulData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnsoulData.class.getName());
	
	private volatile Map<Integer, EnsoulFee> _ensoulFees = Collections.emptyMap();
	private volatile Map<Integer, EnsoulOption> _ensoulOptions = Collections.emptyMap();
	private volatile Map<Integer, EnsoulStone> _ensoulStones = Collections.emptyMap();
	
	private volatile Map<Integer, EnsoulFee> _loadingFees;
	private volatile Map<Integer, EnsoulOption> _loadingOptions;
	private volatile Map<Integer, EnsoulStone> _loadingStones;
	
	/**
	 * Initializes a new instance of {@code EnsoulData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the data for use by the game server.
	 */
	protected EnsoulData()
	{
		load();
	}
	
	/**
	 * Loads the ensoul data from the configuration files.<br>
	 * This method reads all necessary fees, options, and stones into memory.<br>
	 * It logs the total number of items loaded for each category.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, EnsoulFee> loadedFees = new ConcurrentHashMap<>();
		final Map<Integer, EnsoulOption> loadedOptions = new ConcurrentHashMap<>();
		final Map<Integer, EnsoulStone> loadedStones = new ConcurrentHashMap<>();
		_loadingFees = loadedFees;
		_loadingOptions = loadedOptions;
		_loadingStones = loadedStones;
		try
		{
			parseDatapackDirectory("data/stats/ensoul", true);
			if (loadedFees.isEmpty() || loadedOptions.isEmpty() || loadedStones.isEmpty())
			{
				LOGGER.warning(getClass().getSimpleName() + ": Incomplete ensoul data. Keeping previous data.");
				return;
			}
			
			_ensoulFees = loadedFees;
			_ensoulOptions = loadedOptions;
			_ensoulStones = loadedStones;
			
			final String simpleName = getClass().getSimpleName();
			LOGGER.info(simpleName + ": Loaded " + loadedFees.size() + " fees.");
			LOGGER.info(simpleName + ": Loaded " + loadedOptions.size() + " options.");
			LOGGER.info(simpleName + ": Loaded " + loadedStones.size() + " stones.");
		}
		finally
		{
			_loadingFees = null;
			_loadingOptions = null;
			_loadingStones = null;
		}
	}
	
	/**
	 * Parses the provided {@code Document} to load ensoul data.<br>
	 * This method iterates through all nodes in the XML file.<br>
	 * It identifies and processes fee, option, and stone entries.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, IXmlReader::isNode, ensoulNode ->
		{
			switch (ensoulNode.getNodeName())
			{
				case "fee":
				{
					parseFees(ensoulNode);
					break;
				}
				case "option":
				{
					parseOptions(ensoulNode);
					break;
				}
				case "stone":
				{
					parseStones(ensoulNode);
					break;
				}
			}
		}));
	}
	
	/**
	 * Parses the ensoul fees from an XML node.<br>
	 * It identifies the {@code CrystalType} and populates the corresponding {@code EnsoulFee}.<br>
	 * This method handles both normal and re-enchantment fee categories.
	 * @param ensoulNode The {@code Node} containing the fee data to be parsed.
	 */
	private void parseFees(Node ensoulNode)
	{
		final Integer stoneId = parseInteger(ensoulNode.getAttributes(), "stoneId");
		final EnsoulFee fee = new EnsoulFee(stoneId);
		forEach(ensoulNode, IXmlReader::isNode, feeNode ->
		{
			switch (feeNode.getNodeName())
			{
				case "first":
				{
					parseFee(feeNode, fee, 0);
					break;
				}
				case "secondary":
				{
					parseFee(feeNode, fee, 1);
					break;
				}
				case "third":
				{
					parseFee(feeNode, fee, 2);
					break;
				}
				case "reNormal":
				{
					parseReFee(feeNode, fee, 0);
					break;
				}
				case "reSecondary":
				{
					parseReFee(feeNode, fee, 1);
					break;
				}
				case "reThird":
				{
					parseReFee(feeNode, fee, 2);
					break;
				}
				case "remove":
				{
					parseRemove(feeNode, fee);
					break;
				}
			}
		});
	}
	
	/**
	 * Parses the attributes of an {@code ensoulNode} to set a specific fee.<br>
	 * It extracts the item ID and count from the XML node.<br>
	 * The resulting {@code ItemHolder} is saved into the {@code EnsoulFee} object.
	 * @param ensoulNode The XML node containing the fee data.
	 * @param fee The {@code EnsoulFee} object to update.
	 * @param index The position of the fee within the crystal type.
	 */
	private void parseFee(Node ensoulNode, EnsoulFee fee, int index)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "itemId");
		final int count = parseInteger(attrs, "count");
		fee.setEnsoul(index, new ItemHolder(id, count));
		final Map<Integer, EnsoulFee> loadingFees = _loadingFees;
		if (loadingFees == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Skipping ensoul fee outside loading context.");
			return;
		}
		
		loadingFees.put(fee.getStoneId(), fee);
	}
	
	/**
	 * Parses the re-ensoul fee from an XML node.<br>
	 * This method extracts the item ID and count to set a resoul value.
	 * @param ensoulNode The {@code Node} containing the fee data.
	 * @param fee The {@code EnsoulFee} object to update.
	 * @param index The specific index for the resoul entry.
	 */
	private void parseReFee(Node ensoulNode, EnsoulFee fee, int index)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "itemId");
		final int count = parseInteger(attrs, "count");
		fee.setResoul(index, new ItemHolder(id, count));
	}
	
	/**
	 * Parses the removal fee from an XML node.<br>
	 * It extracts the item ID and count to create a new {@code ItemHolder}.<br>
	 * The resulting holder is added to the provided {@code EnsoulFee} object.
	 * @param ensoulNode The {@code Node} containing the XML data.
	 * @param fee The {@code EnsoulFee} object where the removal fee will be stored.
	 */
	private void parseRemove(Node ensoulNode, EnsoulFee fee)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "itemId");
		final int count = parseInteger(attrs, "count");
		fee.addRemovalFee(new ItemHolder(id, count));
	}
	
	/**
	 * Parses the {@code ensoulNode} to create and store {@code EnsoulOption} objects.<br>
	 * It extracts attributes like {@code id}, {@code name}, and {@code skillId} from the XML node.<br>
	 * The resulting options are saved into the internal map.
	 * @param ensoulNode The {@code Node} containing the option data to be parsed.
	 */
	private void parseOptions(Node ensoulNode)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "id");
		final String name = parseString(attrs, "name");
		final String desc = parseString(attrs, "desc");
		final int skillId = parseInteger(attrs, "skillId");
		final int skillLevel = parseInteger(attrs, "skillLevel");
		final EnsoulOption option = new EnsoulOption(id, name, desc, skillId, skillLevel);
		final Map<Integer, EnsoulOption> loadingOptions = _loadingOptions;
		if (loadingOptions == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Skipping ensoul option outside loading context.");
			return;
		}
		
		loadingOptions.put(option.getId(), option);
	}
	
	/**
	 * This method parses the {@code ensoulStone} data from an XML node.<br>
	 * It extracts the unique ID and slot type to create a new {@code EnsoulStone}.<br>
	 * It also processes all associated options for the stone.<br>
	 * Finally, it stores the completed stone in the internal map.
	 * @param ensoulNode The {@code Node} containing the stone data to be parsed.
	 */
	private void parseStones(Node ensoulNode)
	{
		final NamedNodeMap attrs = ensoulNode.getAttributes();
		final int id = parseInteger(attrs, "id");
		final int slotType = parseInteger(attrs, "slotType");
		final EnsoulStone stone = new EnsoulStone(id, slotType);
		forEach(ensoulNode, "option", optionNode -> stone.addOption(parseInteger(optionNode.getAttributes(), "id")));
		final Map<Integer, EnsoulStone> loadingStones = _loadingStones;
		if (loadingStones == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Skipping ensoul stone outside loading context.");
			return;
		}
		
		loadingStones.put(stone.getId(), stone);
	}
	
	/**
	 * Retrieves the item required for an ensoul fee.<br>
	 * This method looks up a specific fee based on the stone ID and its index.<br>
	 * It returns {@code null} if no fee is found.
	 * @param stoneId The unique identifier of the stone.
	 * @param index The specific index of the ensoul option.
	 * @return The {@code ItemHolder} representing the required item, or {@code null}.
	 */
	public ItemHolder getEnsoulFee(int stoneId, int index)
	{
		final EnsoulFee fee = _ensoulFees.get(stoneId);
		return fee != null ? fee.getEnsoul(index) : null;
	}
	
	/**
	 * Retrieves the item required for a resolution fee.<br>
	 * This method looks up the fee based on the provided stone ID and index.<br>
	 * It returns {@code null} if no fee is found.
	 * @param stoneId The unique identifier of the stone.
	 * @param index The specific index of the fee to retrieve.
	 * @return The {@code ItemHolder} for the resolution fee or {@code null}.
	 */
	public ItemHolder getResoulFee(int stoneId, int index)
	{
		final EnsoulFee fee = _ensoulFees.get(stoneId);
		return fee != null ? fee.getResoul(index) : null;
	}
	
	/**
	 * Retrieves the list of items required as a removal fee for a specific stone.<br>
	 * This method looks up the {@code EnsoulFee} associated with the provided {@code stoneId}.<br>
	 * It returns an empty collection if no fee is found.
	 * @param stoneId The unique identifier of the stone.
	 * @return A {@code Collection} of {@code ItemHolder} objects representing the removal fee.
	 */
	public Collection<ItemHolder> getRemovalFee(int stoneId)
	{
		final EnsoulFee fee = _ensoulFees.get(stoneId);
		return fee != null ? fee.getRemovalFee() : Collections.emptyList();
	}
	
	/**
	 * Retrieves an {@code EnsoulOption} based on its unique identifier.<br>
	 * This method looks up the option in the internal data map.
	 * @param id The unique integer ID of the option to find.
	 * @return The {@code EnsoulOption} associated with the given ID, or {@code null} if not found.
	 */
	public EnsoulOption getOption(int id)
	{
		return _ensoulOptions.get(id);
	}
	
	/**
	 * Retrieves an {@code EnsoulStone} based on its unique identifier.<br>
	 * This method looks up the stone in the internal data map.
	 * @param id The unique ID of the stone to retrieve.
	 * @return The {@code EnsoulStone} object associated with the given {@code id}, or {@code null} if not found.
	 */
	public EnsoulStone getStone(int id)
	{
		return _ensoulStones.get(id);
	}
	
	/**
	 * Retrieves the unique ID of an {@code EnsoulStone} based on its type and option.<br>
	 * This method searches through all available stones to find a match.<br>
	 * It returns {@code 0} if no matching stone is found.
	 * @param type The slot type of the stone to search for.
	 * @param optionId The specific option ID within that stone type.
	 * @return The unique integer ID of the stone, or {@code 0} if not found.
	 */
	public int getStone(int type, int optionId)
	{
		for (EnsoulStone stone : _ensoulStones.values())
		{
			if (stone.getSlotType() == type)
			{
				for (int id : stone.getOptions())
				{
					if (id == optionId)
					{
						return stone.getId();
					}
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the global instance of {@code EnsoulData}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access ensoul configuration data throughout the game server.
	 * @return The single instance of {@code EnsoulData}.
	 */
	public static EnsoulData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnsoulData INSTANCE = new EnsoulData();
	}
}
