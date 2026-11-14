/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.mechanics.stats;

import java.io.File;
import java.util.NoSuchElementException;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Creature;

/**
 * Represents the fundamental base statistics for entities in the game.<br>
 * This enum defines the core attributes used to calculate various mechanics and properties.
 * @author DS
 */
public enum BaseStat
{
	STR(Stat.STAT_STR),
	INT(Stat.STAT_INT),
	DEX(Stat.STAT_DEX),
	WIT(Stat.STAT_WIT),
	CON(Stat.STAT_CON),
	MEN(Stat.STAT_MEN),
	CHA(Stat.STAT_CHA),
	LUC(Stat.STAT_LUC);
	
	private static final BaseStat[] VALUES = BaseStat.values();
	
	public static final int MAX_STAT_VALUE = 201;
	
	private final double[] _bonus = new double[MAX_STAT_VALUE];
	private final Stat _stat;
	
	/**
	 * Creates a new instance of {@code BaseStat}.<br>
	 * This constructor assigns the underlying {@code Stat} to this enum constant.
	 * @param stat The {@code Stat} type associated with this base statistic.
	 */
	BaseStat(Stat stat)
	{
		_stat = stat;
	}
	
	/**
	 * Retrieves the {@code Stat} associated with this {@code BaseStat}.
	 * @return The {@code Stat} value.
	 */
	public Stat getStat()
	{
		return _stat;
	}
	
	/**
	 * Calculates the current value of a specific stat for a given creature.<br>
	 * It retrieves the value from the {@code Creature} object.<br>
	 * If either the {@code Creature} or the internal {@code _stat} is null, it returns 0.
	 * @param creature The {@code Creature} to calculate the stat for.
	 * @return The integer value of the stat, or 0 if inputs are invalid.
	 */
	public int calcValue(Creature creature)
	{
		if ((creature != null) && (_stat != null))
		{
			// return (int) Math.min(_stat.finalize(creature, Optional.empty()), MAX_STAT_VALUE - 1);
			return (int) creature.getStat().getValue(_stat);
		}
		
		return 0;
	}
	
	/**
	 * Calculates the bonus value for a specific {@code BaseStat}.<br>
	 * It retrieves the stat value from the provided {@code Creature}.<br>
	 * If the value is less than {@code 1}, it returns {@code 1}.<br>
	 * Returns {@code 1} if the {@code creature} parameter is {@code null}.
	 * @param creature The {@code Creature} to calculate the bonus for.
	 * @return The calculated bonus as a {@code double}.
	 */
	public double calcBonus(Creature creature)
	{
		if (creature != null)
		{
			final int value = calcValue(creature);
			if (value < 1)
			{
				return 1;
			}
			
			return _bonus[value];
		}
		
		return 1;
	}
	
	/**
	 * Updates the bonus value at a specific position.<br>
	 * This method modifies the internal array of this {@code BaseStat}.
	 * @param index The position in the array to update.
	 * @param value The new double value to store.
	 */
	void setValue(int index, double value)
	{
		_bonus[index] = value;
	}
	
	/**
	 * Retrieves the bonus value for a specific index.<br>
	 * This method accesses the internal {@code _bonus} array.
	 * @param index The position in the array to retrieve.
	 * @return The double value stored at the given {@code index}.
	 */
	public double getValue(int index)
	{
		return _bonus[index];
	}
	
	/**
	 * Converts a {@code Stat} into its corresponding {@code BaseStat} enum constant.<br>
	 * This method searches through all available values to find a match.<br>
	 * It throws a {@code NoSuchElementException} if the provided stat is not recognized.
	 * @param stat The {@code Stat} value to convert.
	 * @return The matching {@code BaseStat} instance.
	 */
	public static BaseStat valueOf(Stat stat)
	{
		for (BaseStat baseStat : VALUES)
		{
			if (baseStat.getStat() == stat)
			{
				return baseStat;
			}
		}
		
		throw new NoSuchElementException("Unknown base stat '" + stat + "' for enum BaseStats");
	}
	
	static
	{
		new IXmlReader()
		{
			final Logger LOGGER = Logger.getLogger(BaseStat.class.getName());
			
			@Override
			public void load()
			{
				parseDatapackFile("data/stats/statBonus.xml");
			}
			
			@Override
			public void parseDocument(Document document, File file)
			{
				forEach(document, "list", listNode -> forEach(listNode, IXmlReader::isNode, statNode ->
				{
					final BaseStat baseStat;
					try
					{
						baseStat = valueOf(statNode.getNodeName());
					}
					catch (Exception e)
					{
						LOGGER.severe("Invalid base stats type: " + statNode.getNodeValue() + ", skipping");
						return;
					}
					
					forEach(statNode, "stat", statValue ->
					{
						final NamedNodeMap attrs = statValue.getAttributes();
						final int val = parseInteger(attrs, "value");
						final double bonus = parseDouble(attrs, "bonus");
						baseStat.setValue(val, bonus);
					});
				}));
			}
		}.load();
	}
}
