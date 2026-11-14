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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;

/**
 * This class handles the loading and parsing of HP bonus data for enchanted items from XML files.<br>
 * It maps specific {@code CrystalType} effects to their corresponding health point increases.
 * @author BazookaRpm
 */
public class EnchantItemHPBonusData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnchantItemHPBonusData.class.getName());
	
	private static final float FULL_ARMOR_MODIFIER = 1.5f;
	
	private volatile Map<CrystalType, List<Integer>> _armorHPBonuses = Collections.emptyMap();
	
	/**
	 * This is the default constructor for {@code EnchantItemHPBonusData}.<br>
	 * It initializes the data by calling the {@code load()} method.
	 */
	protected EnchantItemHPBonusData()
	{
		load();
	}
	
	/**
	 * Loads the enchantment HP bonus data from the XML file.<br>
	 * The active table is replaced only when the file parses successfully, so a failed reload keeps the bonuses the max HP calculation is already using.
	 */
	@Override
	public void load()
	{
		final Map<CrystalType, List<Integer>> previous = _armorHPBonuses;
		parseDatapackFile("data/stats/enchantHPBonus.xml");
		if (_armorHPBonuses == previous)
		{
			LOGGER.warning("EnchantItemHPBonusData: Could not load enchant HP bonuses. Keeping the active table.");
			return;
		}
		
		LOGGER.info("EnchantItemHPBonusData: Loaded " + _armorHPBonuses.size() + " enchant HP bonuses.");
	}
	
	/**
	 * This method reads data from an XML file.<br>
	 * It populates the internal bonus maps using the provided {@code Document}.<br>
	 * The process identifies specific enchant nodes and their associated values.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} source used for identifying the data context.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning("EnchantItemHPBonusData: Cannot parse a null XML document.");
			return;
		}
		
		final Node rootNode = document.getDocumentElement();
		if ((rootNode == null) || !"list".equalsIgnoreCase(rootNode.getNodeName()))
		{
			throw new IllegalArgumentException("EnchantItemHPBonusData: Invalid root node in " + getFilePath(file) + ". Expected list.");
		}
		
		final Map<CrystalType, List<Integer>> armorHPBonuses = new EnumMap<>(CrystalType.class);
		for (Node gradeNode = rootNode.getFirstChild(); gradeNode != null; gradeNode = gradeNode.getNextSibling())
		{
			if (!"enchantHP".equalsIgnoreCase(gradeNode.getNodeName()))
			{
				continue;
			}
			
			final CrystalType grade = parseEnum(gradeNode.getAttributes(), CrystalType.class, "grade");
			if (grade == null)
			{
				throw new IllegalArgumentException("EnchantItemHPBonusData: Missing or unknown grade in " + getFilePath(file) + ".");
			}
			
			final List<Integer> bonuses = new ArrayList<>(12);
			for (Node bonusNode = gradeNode.getFirstChild(); bonusNode != null; bonusNode = bonusNode.getNextSibling())
			{
				if ("bonus".equalsIgnoreCase(bonusNode.getNodeName()))
				{
					bonuses.add(Integer.valueOf(bonusNode.getTextContent()));
				}
			}
			
			if (bonuses.isEmpty())
			{
				throw new IllegalArgumentException("EnchantItemHPBonusData: Grade " + grade + " has no bonus values in " + getFilePath(file) + ".");
			}
			
			// The datapack ships S80 twice with identical values, so only a conflicting repeat is worth reporting.
			final List<Integer> replaced = armorHPBonuses.put(grade, Collections.unmodifiableList(bonuses));
			if ((replaced != null) && !replaced.equals(bonuses))
			{
				LOGGER.warning("EnchantItemHPBonusData: Conflicting duplicate grade " + grade + " in " + getFilePath(file) + ". The last definition is used.");
			}
		}
		
		if (armorHPBonuses.isEmpty())
		{
			throw new IllegalArgumentException("EnchantItemHPBonusData: No enchant HP bonuses were found in " + getFilePath(file) + ".");
		}
		
		_armorHPBonuses = armorHPBonuses;
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
	 * Calculates the HP bonus provided by an enchanted item.<br>
	 * This method checks the crystal type and enchantment level of the {@code Item}.<br>
	 * It returns a value based on the defined bonuses for that specific item type.
	 * @param item The {@code Item} to check for bonuses.
	 * @return The total HP bonus as an {@code int}.
	 */
	public int getHPBonus(Item item)
	{
		final ItemTemplate template = item.getTemplate();
		final List<Integer> values = _armorHPBonuses.get(template.getCrystalTypePlus());
		if ((values == null) || values.isEmpty())
		{
			return 0;
		}
		
		final int olyEnchantLevel = item.getOlyEnchantLevel();
		if (olyEnchantLevel <= 0)
		{
			return 0;
		}
		
		final int bonus = values.get(Math.min(olyEnchantLevel, values.size()) - 1);
		if (template.getBodyPart() == BodyPart.FULL_ARMOR)
		{
			return (int) (bonus * FULL_ARMOR_MODIFIER);
		}
		
		return bonus;
	}
	
	/**
	 * Provides access to the singleton instance of this class.<br>
	 * Use this method to get the shared data for enchant item HP bonuses.
	 * @return The single {@code EnchantItemHPBonusData} instance.
	 */
	public static EnchantItemHPBonusData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantItemHPBonusData INSTANCE = new EnchantItemHPBonusData();
	}
}
