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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.PetDataHolder;
import org.l2jmobius.gameserver.data.holders.PetLevelData;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class loads pet definitions from XML files and provides constant-time lookups by NPC id and summoning item id.<br>
 * It caches per-pet data at startup and resolves per-level stats with a maximum-level fallback at runtime.
 * @author Zoey76, Mobius
 */
public class PetData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PetData.class.getName());
	
	// Pet Data Storage.
	private final Map<Integer, PetDataHolder> _pets = new ConcurrentHashMap<>();
	private final Map<Integer, PetDataHolder> _petsByItemId = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code PetData} class.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected PetData()
	{
		load();
	}
	
	/**
	 * Loads all pet data from the configuration files.<br>
	 * This method clears existing caches and parses the directory {@code data/stats/pets}.<br>
	 * It logs the total number of pets loaded into the system.
	 */
	@Override
	public void load()
	{
		_pets.clear();
		_petsByItemId.clear();
		parseDatapackDirectory("data/stats/pets", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _pets.size() + " pets.");
	}
	
	/**
	 * Parses pet data from a {@code Document} and stores it in the internal maps.<br>
	 * This method iterates through all {@code pet} nodes to extract attributes like skills and stats.<br>
	 * It populates both the NPC ID and item ID indexes for quick lookup later.
	 * @param document The {@code Document} containing the XML data to be parsed.
	 * @param file The {@code File} object representing the source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		for (Node petNode = document.getFirstChild().getFirstChild(); petNode != null; petNode = petNode.getNextSibling())
		{
			if (petNode.getNodeName().equals("pet"))
			{
				final NamedNodeMap petAttrs = petNode.getAttributes();
				final int npcId = parseInteger(petAttrs, "id");
				final int itemId = parseInteger(petAttrs, "itemId");
				
				// Index attribute is ignored for now.
				final PetDataHolder data = new PetDataHolder(npcId, itemId);
				for (Node propertyNode = petNode.getFirstChild(); propertyNode != null; propertyNode = propertyNode.getNextSibling())
				{
					final String propertyName = propertyNode.getNodeName();
					if (propertyName.equals("set"))
					{
						attrs = propertyNode.getAttributes();
						final String type = attrs.getNamedItem("name").getNodeValue();
						if ("food".equals(type))
						{
							for (String foodId : attrs.getNamedItem("val").getNodeValue().split(";"))
							{
								data.addFood(Integer.parseInt(foodId));
							}
						}
						else if ("load".equals(type))
						{
							data.setLoad(parseInteger(attrs, "val"));
						}
						else if ("hungry_limit".equals(type))
						{
							data.setHungryLimit(parseInteger(attrs, "val"));
						}
						else if ("sync_level".equals(type))
						{
							data.setSyncLevel(parseInteger(attrs, "val") == 1);
						}
						
						// Evolve attribute is ignored.
					}
					else if (propertyName.equals("skills"))
					{
						for (Node skillNode = propertyNode.getFirstChild(); skillNode != null; skillNode = skillNode.getNextSibling())
						{
							if (skillNode.getNodeName().equals("skill"))
							{
								attrs = skillNode.getAttributes();
								data.addNewSkill(parseInteger(attrs, "skillId"), parseInteger(attrs, "skillLevel"), parseInteger(attrs, "minLevel"));
							}
						}
					}
					else if (propertyName.equals("stats"))
					{
						for (Node statNode = propertyNode.getFirstChild(); statNode != null; statNode = statNode.getNextSibling())
						{
							if (statNode.getNodeName().equals("stat"))
							{
								final int level = Integer.parseInt(statNode.getAttributes().getNamedItem("level").getNodeValue());
								final StatSet set = new StatSet();
								for (Node beanNode = statNode.getFirstChild(); beanNode != null; beanNode = beanNode.getNextSibling())
								{
									if (beanNode.getNodeName().equals("set"))
									{
										attrs = beanNode.getAttributes();
										final String beanName = attrs.getNamedItem("name").getNodeValue();
										if ("speed_on_ride".equals(beanName))
										{
											set.set("walkSpeedOnRide", attrs.getNamedItem("walk").getNodeValue());
											set.set("runSpeedOnRide", attrs.getNamedItem("run").getNodeValue());
											set.set("slowSwimSpeedOnRide", attrs.getNamedItem("slowSwim").getNodeValue());
											set.set("fastSwimSpeedOnRide", attrs.getNamedItem("fastSwim").getNodeValue());
											final Node slowFly = attrs.getNamedItem("slowFly");
											if (slowFly != null)
											{
												set.set("slowFlySpeedOnRide", slowFly.getNodeValue());
											}
											
											final Node fastFly = attrs.getNamedItem("fastFly");
											if (fastFly != null)
											{
												set.set("fastFlySpeedOnRide", fastFly.getNodeValue());
											}
										}
										else
										{
											set.set(beanName, attrs.getNamedItem("val").getNodeValue());
										}
									}
								}
								
								data.addNewStat(level, new PetLevelData(set));
							}
						}
					}
				}
				
				_pets.put(npcId, data);
				_petsByItemId.put(itemId, data);
			}
		}
	}
	
	/**
	 * Retrieves the {@code PetDataHolder} associated with a specific item ID.<br>
	 * This method looks up the data in the internal cache.
	 * @param itemId The unique identifier of the summoning item.
	 * @return The {@code PetDataHolder} for the given item, or {@code null} if not found.
	 */
	public PetDataHolder getPetDataHolderByItemId(int itemId)
	{
		return _petsByItemId.get(itemId);
	}
	
	/**
	 * Retrieves the statistics for a specific pet at a given level.<br>
	 * If the requested {@code petLevel} exceeds the maximum allowed level, it returns data for the maximum level.
	 * @param petId The unique identifier of the pet.
	 * @param petLevel The level of the pet to retrieve data for.
	 * @return The {@code PetLevelData} for the requested pet and level, or {@code null} if the pet is not found.
	 */
	public PetLevelData getPetLevelData(int petId, int petLevel)
	{
		final PetDataHolder petData = _pets.get(petId);
		if (petData != null)
		{
			final int maxLevel = petData.getMaxLevel();
			if (petLevel > maxLevel)
			{
				return petData.getPetLevelData(maxLevel);
			}
			
			return petData.getPetLevelData(petLevel);
		}
		
		return null;
	}
	
	/**
	 * Retrieves the {@code PetDataHolder} associated with a specific pet ID.<br>
	 * This method looks up the data in the internal cache.<br>
	 * It logs an info message if no data is found for the given ID.
	 * @param petId The unique identifier of the pet to retrieve.
	 * @return The {@code PetDataHolder} object, or {@code null} if it does not exist.
	 */
	public PetDataHolder getPetData(int petId)
	{
		final PetDataHolder data = _pets.get(petId);
		if (data == null)
		{
			LOGGER.info(getClass().getSimpleName() + ": Missing pet data for NPC id: " + petId + ".");
		}
		
		return data;
	}
	
	/**
	 * Retrieves the minimum level required for a specific pet.<br>
	 * This method looks up the pet data using the provided {@code petId}.
	 * @param petId The unique identifier of the pet.
	 * @return The minimum level as an {@code int}.
	 */
	public int getPetMinLevel(int petId)
	{
		return _pets.get(petId).getMinLevel();
	}
	
	/**
	 * Retrieves the item ID associated with a specific NPC.<br>
	 * This method looks up the pet data using the provided {@code npcId}.
	 * @param npcId The unique identifier of the NPC.
	 * @return The integer item ID for the pet.
	 */
	public int getPetItemsByNpc(int npcId)
	{
		return _pets.get(npcId).getItemId();
	}
	
	/**
	 * Checks if a specific NPC can be used as a mount.<br>
	 * This method verifies the {@code npcId} against known mount types.
	 * @param npcId The unique identifier of the NPC to check.
	 * @return {@code true} if the NPC is mountable, otherwise {@code false}.
	 */
	public static boolean isMountable(int npcId)
	{
		return MountType.findByNpcId(npcId) != MountType.NONE;
	}
	
	/**
	 * Provides the singleton instance of {@code PetData}.<br>
	 * Use this method to access the global pet data manager.
	 * @return The singleton instance of {@code PetData}.
	 */
	public static PetData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetData INSTANCE = new PetData();
	}
}
