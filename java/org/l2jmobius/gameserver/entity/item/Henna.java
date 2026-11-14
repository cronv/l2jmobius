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
package org.l2jmobius.gameserver.entity.item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents the {@code Henna} item object in the game.<br>
 * This class handles the data and logic associated with this specific item type.
 * @author Zoey76
 */
public class Henna
{
	private final int _dyeId;
	private final int _dyeItemId;
	private final boolean _isPremium;
	private final Map<BaseStat, Integer> _baseStats = new EnumMap<>(BaseStat.class);
	private final int _wearFee;
	private final int _wearCount;
	private final int _cancelFee;
	private final int _cancelCount;
	private final int _duration;
	private final List<Skill> _skills;
	private final Set<PlayerClass> _wearClass;
	
	/**
	 * Creates a new {@code Henna} instance using the provided data.<br>
	 * This constructor initializes all internal fields from a {@code StatSet}.<br>
	 * It sets values for IDs, stats, fees, and duration.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	public Henna(StatSet set)
	{
		_dyeId = set.getInt("dyeId");
		_dyeItemId = set.getInt("dyeItemId");
		_isPremium = set.getBoolean("isPremium", false);
		_baseStats.put(BaseStat.STR, set.getInt("str", 0));
		_baseStats.put(BaseStat.CON, set.getInt("con", 0));
		_baseStats.put(BaseStat.DEX, set.getInt("dex", 0));
		_baseStats.put(BaseStat.INT, set.getInt("int", 0));
		_baseStats.put(BaseStat.MEN, set.getInt("men", 0));
		_baseStats.put(BaseStat.WIT, set.getInt("wit", 0));
		_baseStats.put(BaseStat.LUC, set.getInt("luc", 0));
		_baseStats.put(BaseStat.CHA, set.getInt("cha", 0));
		_wearFee = set.getInt("wear_fee");
		_wearCount = set.getInt("wear_count");
		_cancelFee = set.getInt("cancel_fee");
		_cancelCount = set.getInt("cancel_count");
		_duration = set.getInt("duration", -1);
		_skills = new ArrayList<>();
		_wearClass = new HashSet<>();
	}
	
	/**
	 * Retrieves the unique identifier for the dye.<br>
	 * This value is used to identify which specific dye is being applied.
	 * @return the {@code int} ID of the dye.
	 */
	public int getDyeId()
	{
		return _dyeId;
	}
	
	/**
	 * Retrieves the unique identifier for the dye item.<br>
	 * This value is used to identify the specific item in the database.
	 * @return The {@code int} ID of the dye item.
	 */
	public int getDyeItemId()
	{
		return _dyeItemId;
	}
	
	/**
	 * Checks if this {@code Henna} object is a premium item.<br>
	 * Returns {@code true} if it is premium.<br>
	 * Returns {@code false} otherwise.
	 * @return the premium status of the item.
	 */
	public boolean isPremium()
	{
		return _isPremium;
	}
	
	/**
	 * Retrieves the base value for a specific statistic.<br>
	 * It returns {@code 0} if the statistic is not present in the map.
	 * @param stat The {@code BaseStat} type to look up.
	 * @return The integer value of the requested statistic.
	 */
	public int getBaseStats(BaseStat stat)
	{
		return !_baseStats.containsKey(stat) ? 0 : _baseStats.get(stat).intValue();
	}
	
	/**
	 * Retrieves the map of base statistics for this {@code Henna}.<br>
	 * The map contains {@code BaseStat} keys and their corresponding integer values.
	 * @return a {@code Map} containing all {@code BaseStat} entries.
	 */
	public Map<BaseStat, Integer> getBaseStats()
	{
		return _baseStats;
	}
	
	/**
	 * Retrieves the cost to wear this item.<br>
	 * This value is stored in the {@code _wearFee} field.
	 * @return The amount of currency required to wear the item.
	 */
	public int getWearFee()
	{
		return _wearFee;
	}
	
	/**
	 * Retrieves the total number of times this {@code Henna} can be worn.<br>
	 * This value represents the remaining uses available for the item.
	 * @return The current wear count as an {@code int}.
	 */
	public int getWearCount()
	{
		return _wearCount;
	}
	
	/**
	 * Retrieves the cost to cancel a henna effect.<br>
	 * This value is stored as an {@code int}.
	 * @return The cancellation fee amount.
	 */
	public int getCancelFee()
	{
		return _cancelFee;
	}
	
	/**
	 * Returns the total number of times this {@code Henna} has been cancelled.<br>
	 * This value is used to track usage limits.
	 * @return The current cancel count as an {@code int}.
	 */
	public int getCancelCount()
	{
		return _cancelCount;
	}
	
	/**
	 * Retrieves the total duration of the henna effect.<br>
	 * The value is returned as an {@code int}.
	 * @return The duration of the henna.
	 */
	public int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Adds a list of skills to the current object.<br>
	 * This method updates the internal skill collection.
	 * @param skillList The {@code List} of {@code Skill} objects to add.
	 */
	public void setSkills(List<Skill> skillList)
	{
		_skills.addAll(skillList);
	}
	
	/**
	 * Retrieves the list of skills associated with this {@code Henna}.<br>
	 * This method returns all active skills provided to the object.
	 * @return a {@code List} of {@code Skill} objects.
	 */
	public List<Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Retrieves the set of classes allowed to wear this item.<br>
	 * This method returns all {@code PlayerClass} types that have permission.
	 * @return a {@code Set} containing the allowed {@code PlayerClass} values.
	 */
	public Set<PlayerClass> getAllowedWearClass()
	{
		return _wearClass;
	}
	
	/**
	 * Checks if a specific class is allowed to wear this item.<br>
	 * It verifies the {@code playerClass} against the internal list of permitted classes.
	 * @param playerClass The {@code PlayerClass} to check.
	 * @return {@code true} if the class is allowed, {@code false} otherwise.
	 */
	public boolean isAllowedClass(PlayerClass playerClass)
	{
		return _wearClass.contains(playerClass);
	}
	
	/**
	 * Updates the list of allowed classes for this item.<br>
	 * It adds all provided classes to the internal set.
	 * @param wearClassIds The list of {@code PlayerClass} objects to add.
	 */
	public void setWearClassIds(List<PlayerClass> wearClassIds)
	{
		_wearClass.addAll(wearClassIds);
	}
}
