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

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Position;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * This class handles the loading and parsing of hit condition bonuses from XML files.<br>
 * It defines specific bonuses applied to creatures based on their current status or conditions.<br>
 * The configuration is published as a single snapshot only after the source XML passes validation, so a failed reload keeps the active bonuses instead of mixing old and new values.
 * @author BazookaRpm
 */
public class HitConditionBonusData implements IXmlReader
{
	// Bonus State.
	private volatile ConditionBonuses _bonuses = new ConditionBonuses(0, 0, 0, 0, 0, 0);
	
	/**
	 * Initializes a new instance of {@code HitConditionBonusData}.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected HitConditionBonusData()
	{
		load();
	}
	
	/**
	 * Loads the hit condition bonus data from the XML file.<br>
	 * The active bonuses are replaced only when the file parses and validates successfully.
	 */
	@Override
	public void load()
	{
		final ConditionBonuses previous = _bonuses;
		parseDatapackFile("data/stats/hitConditionBonus.xml");
		if (_bonuses == previous)
		{
			LOGGER.warning("HitConditionBonusData: No hit condition bonuses were loaded. Keeping the active configuration.");
			return;
		}
		
		LOGGER.info("HitConditionBonusData: Loaded hit condition bonuses.");
	}
	
	/**
	 * Parses the XML data from a {@code Document} and publishes a validated set of bonuses.<br>
	 * Every supported bonus must be present and carry a numeric value, otherwise the file is rejected.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning("HitConditionBonusData: Cannot parse a null XML document.");
			return;
		}
		
		final Node rootNode = document.getDocumentElement();
		if ((rootNode == null) || !"hitConditionBonus".equalsIgnoreCase(rootNode.getNodeName()))
		{
			throw new IllegalArgumentException("HitConditionBonusData: Invalid root node in " + getFilePath(file) + ". Expected hitConditionBonus.");
		}
		
		Integer front = null;
		Integer side = null;
		Integer back = null;
		Integer high = null;
		Integer low = null;
		Integer dark = null;
		for (Node bonusNode = rootNode.getFirstChild(); bonusNode != null; bonusNode = bonusNode.getNextSibling())
		{
			if (bonusNode.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			
			// A missing val attribute leaves the value null, which the completeness check below reports.
			final Integer value = parseInteger(bonusNode.getAttributes(), "val");
			switch (bonusNode.getNodeName())
			{
				case "front":
				{
					front = value;
					break;
				}
				case "side":
				{
					side = value;
					break;
				}
				case "back":
				{
					back = value;
					break;
				}
				case "high":
				{
					high = value;
					break;
				}
				case "low":
				{
					low = value;
					break;
				}
				case "dark":
				{
					dark = value;
					break;
				}
			}
		}
		
		if ((front == null) || (side == null) || (back == null) || (high == null) || (low == null) || (dark == null))
		{
			throw new IllegalArgumentException("HitConditionBonusData: Incomplete hit condition bonuses in " + getFilePath(file) + ".");
		}
		
		_bonuses = new ConditionBonuses(front.intValue(), side.intValue(), back.intValue(), high.intValue(), low.intValue(), dark.intValue());
	}
	
	/**
	 * Calculates the damage multiplier based on environmental and positional conditions.<br>
	 * This method checks height differences, time of day, and relative positions.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} receiving the attack.
	 * @return The final calculated bonus as a {@code double}.
	 */
	public double getConditionBonus(Creature attacker, Creature target)
	{
		final ConditionBonuses bonuses = _bonuses;
		double mod = 100d;
		
		// Apply height difference bonus.
		final int heightDifference = attacker.getZ() - target.getZ();
		if (heightDifference > 50)
		{
			mod += bonuses._high;
		}
		else if (heightDifference < -50)
		{
			mod += bonuses._low;
		}
		
		// Apply night-time bonus. There is no rain support yet.
		if (GameTimeTaskManager.getInstance().isNight())
		{
			mod += bonuses._dark;
		}
		
		// Apply positional bonus.
		switch (Position.getPosition(attacker, target))
		{
			case SIDE:
			{
				mod += bonuses._side;
				break;
			}
			case BACK:
			{
				mod += bonuses._back;
				break;
			}
			default:
			{
				mod += bonuses._front;
				break;
			}
		}
		
		// Return the final condition bonus multiplier, ensuring it is not less than zero.
		return Math.max(mod / 100d, 0);
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
	 * Retrieves the singleton instance of {@code HitConditionBonusData}.<br>
	 * This method provides a global access point to the shared data.
	 * @return The single instance of {@code HitConditionBonusData}.
	 */
	public static HitConditionBonusData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Stores one complete set of hit condition bonuses.<br>
	 * Readers take the whole set with a single field read, so they never observe a half-applied reload.
	 */
	private static class ConditionBonuses
	{
		private final int _front;
		private final int _side;
		private final int _back;
		private final int _high;
		private final int _low;
		private final int _dark;
		
		ConditionBonuses(int front, int side, int back, int high, int low, int dark)
		{
			_front = front;
			_side = side;
			_back = back;
			_high = high;
			_low = low;
			_dark = dark;
		}
	}
	
	private static class SingletonHolder
	{
		protected static final HitConditionBonusData INSTANCE = new HitConditionBonusData();
	}
}
