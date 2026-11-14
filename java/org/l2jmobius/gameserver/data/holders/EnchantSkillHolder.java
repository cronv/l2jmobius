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
package org.l2jmobius.gameserver.data.holders;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillEnchantType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class acts as a data container for skills associated with item enchantments.<br>
 * It stores and manages the relationship between {@code SkillEnchantType} and specific skill mechanics.
 * @author Sdw
 */
public class EnchantSkillHolder
{
	private final int _level;
	private final int _enchantFailLevel;
	private final Map<SkillEnchantType, Long> _sp = new EnumMap<>(SkillEnchantType.class);
	private final Map<SkillEnchantType, Integer> _chance = new EnumMap<>(SkillEnchantType.class);
	private final Map<SkillEnchantType, Set<ItemHolder>> _requiredItems = new EnumMap<>(SkillEnchantType.class);
	
	/**
	 * Creates a new {@code EnchantSkillHolder} instance.<br>
	 * This constructor initializes the holder using data from a {@code StatSet}.<br>
	 * It extracts the level and fail level values from the provided set.
	 * @param set The {@code StatSet} containing the required configuration values.
	 */
	public EnchantSkillHolder(StatSet set)
	{
		_level = set.getInt("level");
		_enchantFailLevel = set.getInt("enchantFailLevel");
	}
	
	/**
	 * Retrieves the current level of the enchantment skill.<br>
	 * This value is stored in the {@code _level} field.
	 * @return The integer level of the skill.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Returns the level at which an enchantment fails.<br>
	 * This value is used to determine the success threshold for skills.
	 * @return the current {@code enchantFailLevel}.
	 */
	public int getEnchantFailLevel()
	{
		return _enchantFailLevel;
	}
	
	/**
	 * Adds or updates the skill points for a specific enchantment type.<br>
	 * This method stores the value in the internal map.
	 * @param type The {@code SkillEnchantType} to modify.
	 * @param sp The amount of skill points to set.
	 */
	public void addSp(SkillEnchantType type, long sp)
	{
		_sp.put(type, sp);
	}
	
	/**
	 * Retrieves the skill points for a specific enchantment type.<br>
	 * It returns {@code 0} if no value is found for the given type.
	 * @param type The {@code SkillEnchantType} to check.
	 * @return The number of skill points as a {@code long}.
	 */
	public long getSp(SkillEnchantType type)
	{
		final Long val = _sp.get(type);
		return val != null ? val.longValue() : 0;
	}
	
	/**
	 * Adds or updates the success chance for a specific skill enchantment.<br>
	 * This method maps the {@code SkillEnchantType} to its corresponding integer value.
	 * @param type The type of the skill enchantment to modify.
	 * @param chance The numerical chance value to assign to this type.
	 */
	public void addChance(SkillEnchantType type, int chance)
	{
		_chance.put(type, chance);
	}
	
	/**
	 * Retrieves the success chance for a specific skill enchantment.<br>
	 * It returns the value stored in the internal map.<br>
	 * If no specific chance is defined, it defaults to {@code 100}.
	 * @param type The {@code SkillEnchantType} to check.
	 * @return The success chance as an {@code int}.
	 */
	public int getChance(SkillEnchantType type)
	{
		final Integer val = _chance.get(type);
		return val != null ? val.intValue() : 100;
	}
	
	/**
	 * Adds a specific item to the requirements for an enchantment type.<br>
	 * This method updates the internal collection of items needed for a {@code SkillEnchantType}.<br>
	 * If the type does not have any items yet, it creates a new set.
	 * @param type The {@code SkillEnchantType} that requires the item.
	 * @param item The {@code ItemHolder} to be added as a requirement.
	 */
	public void addRequiredItem(SkillEnchantType type, ItemHolder item)
	{
		_requiredItems.computeIfAbsent(type, _ -> new HashSet<>()).add(item);
	}
	
	/**
	 * Retrieves the set of items needed for a specific enchantment.<br>
	 * It looks up the requirements based on the provided {@code SkillEnchantType}.<br>
	 * If no items are required, it returns an empty {@code Set}.
	 * @param type The {@code SkillEnchantType} to check for requirements.
	 * @return A {@code Set} of {@code ItemHolder} objects required for the enchantment.
	 */
	public Set<ItemHolder> getRequiredItems(SkillEnchantType type)
	{
		return _requiredItems.getOrDefault(type, Collections.emptySet());
	}
}
