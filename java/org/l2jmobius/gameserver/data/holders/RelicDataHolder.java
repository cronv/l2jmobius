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
package org.l2jmobius.gameserver.data.holders;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicType;

/**
 * This class serves as a data container for {@code Relic} information.<br>
 * It stores and manages the properties associated with specific relics in the game world.
 * @author CostyKiller
 */
public class RelicDataHolder
{
	private final int _relicId;
	private final String _name;
	private final int _parentRelicId;
	private final RelicGrade _grade;
	private final RelicType _type;
	private final List<RelicEnchantHolder> _enchantHolder;
	private final long _summonChance;
	private final float _compoundChanceModifier;
	private final float _compoundUpGradeChanceModifier;
	
	/**
	 * Creates a new {@code RelicDataHolder} object.<br>
	 * This constructor initializes all data fields for a specific relic.
	 * @param relicId The unique identifier for the relic.
	 * @param name The display name of the relic.
	 * @param parentRelicId The ID of the parent relic, if applicable.
	 * @param grade The {@code RelicGrade} assigned to this relic.
	 * @param type The {@code RelicType} category for this relic.
	 * @param summonChance The base chance to summon this relic.
	 * @param enchantHolder A list of {@code RelicEnchantHolder} objects associated with the relic.
	 * @param compoundChanceModifier The modifier applied to the compound chance.
	 * @param compoundUpGradeChanceModifier The modifier applied to the compound upgrade chance.
	 */
	public RelicDataHolder(int relicId, String name, int parentRelicId, RelicGrade grade, RelicType type, long summonChance, List<RelicEnchantHolder> enchantHolder, float compoundChanceModifier, float compoundUpGradeChanceModifier)
	{
		_relicId = relicId;
		_name = name;
		_parentRelicId = parentRelicId;
		_grade = grade;
		_type = type;
		_summonChance = summonChance;
		_enchantHolder = enchantHolder;
		_compoundChanceModifier = compoundChanceModifier;
		_compoundUpGradeChanceModifier = compoundUpGradeChanceModifier;
	}
	
	/**
	 * Retrieves the unique identifier for this relic.<br>
	 * This value is used to identify the specific relic in the database.
	 * @return The {@code int} ID of the relic.
	 */
	public int getRelicId()
	{
		return _relicId;
	}
	
	/**
	 * Retrieves the name of the relic.<br>
	 * This method returns the {@code String} value stored in the holder.
	 * @return The name of the relic.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the unique identifier of the parent relic.<br>
	 * This value is used to determine the progression hierarchy.
	 * @return the {@code int} ID of the parent relic.
	 */
	public int getParentRelicId()
	{
		return _parentRelicId;
	}
	
	/**
	 * Retrieves the grade of the relic.<br>
	 * This method returns the {@code RelicGrade} associated with this holder.
	 * @return The {@code RelicGrade} of the relic.
	 */
	public RelicGrade getGrade()
	{
		return _grade;
	}
	
	/**
	 * Retrieves the numerical position of the {@code RelicGrade}.<br>
	 * This value corresponds to the order defined in the {@code RelicGrade} enum.
	 * @return The integer ordinal of the current grade.
	 */
	public int getGradeOrdinal()
	{
		return _grade.ordinal();
	}
	
	/**
	 * Retrieves the type of the relic.<br>
	 * This method returns the {@code RelicType} associated with this holder.
	 * @return the {@code RelicType} of the relic.
	 */
	public RelicType getType()
	{
		return _type;
	}
	
	/**
	 * Checks if the relic type is shining.<br>
	 * This method compares the current {@code RelicType} against the {@code SHINING} value.
	 * @return {@code true} if the relic is a shining type, {@code false} otherwise.
	 */
	public boolean isShining()
	{
		return _type == RelicType.SHINING;
	}
	
	/**
	 * Retrieves the chance to summon this relic.<br>
	 * The value is stored as a {@code long}.
	 * @return the summon chance of the relic.
	 */
	public long getSummonChance()
	{
		return _summonChance;
	}
	
	/**
	 * Retrieves the list of enchantment holders for this relic.<br>
	 * This method returns all enchantments associated with the {@code RelicDataHolder}.
	 * @return a {@code List} of {@code RelicEnchantHolder} objects.
	 */
	public List<RelicEnchantHolder> getEnchantHoldders()
	{
		return _enchantHolder;
	}
	
	/**
	 * Finds a specific {@code RelicEnchantHolder} based on the provided enchant level.<br>
	 * If no exact match is found, it returns the first holder in the list.
	 * @param enchant The enchantment level to search for.
	 * @return The matching {@code RelicEnchantHolder} or the first available one.
	 */
	public RelicEnchantHolder getEnchantHolderByEnchant(int enchant)
	{
		for (RelicEnchantHolder holder : _enchantHolder)
		{
			if (enchant == holder.getEnchantLevel())
			{
				return holder;
			}
		}
		
		return _enchantHolder.getFirst();
	}
	
	/**
	 * Retrieves the compound chance modifier for this relic.<br>
	 * This value is used to adjust the overall probability of a specific outcome.
	 * @return The {@code float} value of the compound chance modifier.
	 */
	public float getCompoundChanceModifier()
	{
		return _compoundChanceModifier;
	}
	
	/**
	 * Retrieves the modifier for compound upgrade chances.<br>
	 * This value adjusts how likely a relic is to upgrade when compounded.
	 * @return the {@code float} value of the compound upgrade chance modifier.
	 */
	public float getCompoundUpGradeChanceModifier()
	{
		return _compoundUpGradeChanceModifier;
	}
}
