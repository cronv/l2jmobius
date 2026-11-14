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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class holds the base data and constant statistics for a pet.<br>
 * It manages identity information, per-level stat tables, learnable skills, and food registries.
 * @author JIV, Mobius
 */
public class PetDataHolder
{
	// Constants.
	private static final PetLevelData[] EMPTY_LEVEL_STATS = new PetLevelData[0];
	private static final int SKILL_LEVEL_THRESHOLD = 70;
	private static final int SKILL_LEVEL_DIVISOR_LOW = 10;
	private static final int SKILL_LEVEL_DIVISOR_HIGH = 5;
	private static final int SKILL_LEVEL_BASE_HIGH = 7;
	
	// Pet Identity.
	private final int _npcId;
	private final int _itemId;
	
	// Level Boundaries.
	private int _minLevel = Byte.MAX_VALUE;
	private int _maxLevel = 0;
	private boolean _syncLevel = false;
	
	// Stats and Capacity.
	private PetLevelData[] _levelStats = EMPTY_LEVEL_STATS;
	private int _load = 20000;
	private int _hungryLimit = 1;
	
	// Skills and Food.
	private final List<PetSkillLearn> _skills = new ArrayList<>();
	private final Set<Integer> _food = new HashSet<>();
	
	/**
	 * Creates a new instance of {@code PetDataHolder}.<br>
	 * This constructor initializes the pet with its unique identifiers.
	 * @param npcId The unique identifier for the NPC.
	 * @param itemId The unique identifier for the item.
	 */
	public PetDataHolder(int npcId, int itemId)
	{
		_npcId = npcId;
		_itemId = itemId;
	}
	
	/**
	 * Retrieves the unique identifier for the NPC.<br>
	 * This ID identifies the specific pet type in the game database.
	 * @return The {@code int} value of the NPC ID.
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * Retrieves the unique item identifier for this pet.<br>
	 * This value is used to identify the pet's visual representation.
	 * @return The {@code int} ID of the item.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Adds a new set of statistics for a specific pet level.<br>
	 * This method updates the minimum and maximum levels if necessary.<br>
	 * It also ensures the internal array is large enough to store the new data.
	 * @param level The integer level to associate with the data.
	 * @param data The {@code PetLevelData} object containing the stats.
	 */
	public void addNewStat(int level, PetLevelData data)
	{
		if (_minLevel > level)
		{
			_minLevel = level;
		}
		
		if (_maxLevel < level)
		{
			_maxLevel = level;
		}
		
		if (level >= _levelStats.length)
		{
			_levelStats = Arrays.copyOf(_levelStats, Math.max(level + 1, _levelStats.length * 2));
		}
		
		_levelStats[level] = data;
	}
	
	/**
	 * Retrieves the data for a specific pet level.<br>
	 * This method looks up the stats based on the provided level index.<br>
	 * It returns {@code null} if the level is out of bounds.
	 * @param petLevel The level to look up.
	 * @return The {@code PetLevelData} for the given level, or {@code null} if invalid.
	 */
	public PetLevelData getPetLevelData(int petLevel)
	{
		if ((petLevel < 0) || (petLevel >= _levelStats.length))
		{
			return null;
		}
		
		return _levelStats[petLevel];
	}
	
	/**
	 * Retrieves the current load value of the pet.
	 * @return The current {@code int} value for the load.
	 */
	public int getLoad()
	{
		return _load;
	}
	
	/**
	 * Retrieves the maximum hunger capacity for the pet.<br>
	 * This value determines how much food a pet can hold.
	 * @return The current {@code int} limit for hunger.
	 */
	public int getHungryLimit()
	{
		return _hungryLimit;
	}
	
	/**
	 * Checks if the level synchronization is enabled.<br>
	 * Returns {@code true} if synchronization is active.<br>
	 * Returns {@code false} otherwise.
	 * @return a boolean indicating the sync status.
	 */
	public boolean isSynchLevel()
	{
		return _syncLevel;
	}
	
	/**
	 * Retrieves the minimum level for this pet.<br>
	 * This value defines the lower boundary of the pet's level range.
	 * @return The minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Retrieves the maximum level allowed for a pet.<br>
	 * This value is stored in the {@code _maxLevel} field.
	 * @return The maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the set of food items for this pet.<br>
	 * This collection is used by the hunger system.
	 * @return a {@code Set<Integer>} containing the food IDs.
	 */
	public Set<Integer> getFood()
	{
		return _food;
	}
	
	/**
	 * Adds a new food item to the registry.<br>
	 * This method updates the internal set of allowed foods for the pet.
	 * @param foodId The unique identifier of the food item to add.
	 */
	public void addFood(Integer foodId)
	{
		_food.add(foodId);
	}
	
	/**
	 * Sets the current load value for the pet.<br>
	 * This updates the internal {@code _load} field.
	 * @param load The new load value to set.
	 */
	public void setLoad(int load)
	{
		_load = load;
	}
	
	/**
	 * Sets the maximum hunger capacity for the pet.<br>
	 * This value determines how much food a pet can hold.
	 * @param limit The new hunger limit to set.
	 */
	public void setHungryLimit(int limit)
	{
		_hungryLimit = limit;
	}
	
	/**
	 * Sets whether the level synchronization is enabled.<br>
	 * This updates the internal {@code _syncLevel} state.
	 * @param value The boolean value to set for synchronization.
	 */
	public void setSyncLevel(boolean value)
	{
		_syncLevel = value;
	}
	
	// Skills.
	
	/**
	 * Adds a new skill to the pet's learnable skills list.<br>
	 * This method creates a new {@code PetSkillLearn} object and stores it.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill being added.
	 * @param petLevel The minimum pet level required to learn this skill.
	 */
	public void addNewSkill(int skillId, int skillLevel, int petLevel)
	{
		_skills.add(new PetSkillLearn(skillId, skillLevel, petLevel));
	}
	
	/**
	 * Calculates the maximum learnable level for a specific skill based on the pet's current level.<br>
	 * This method checks if the skill is available and applies scaling logic or retrieves the defined level.
	 * @param skillId The unique identifier of the skill to check.
	 * @param petLevel The current level of the pet.
	 * @return The maximum level the pet can learn for the given skill, or 1 if it is available but has no specific scaling.
	 */
	public int getAvailableLevel(int skillId, int petLevel)
	{
		int level = 0;
		boolean found = false;
		for (PetSkillLearn skill : _skills)
		{
			if (skill.getSkillId() != skillId)
			{
				continue;
			}
			
			found = true;
			final int skillLevel = skill.getSkillLevel();
			if (skillLevel == 0)
			{
				if (petLevel < SKILL_LEVEL_THRESHOLD)
				{
					level = (petLevel / SKILL_LEVEL_DIVISOR_LOW);
					if (level <= 0)
					{
						level = 1;
					}
				}
				else
				{
					level = (SKILL_LEVEL_BASE_HIGH + ((petLevel - SKILL_LEVEL_THRESHOLD) / SKILL_LEVEL_DIVISOR_HIGH));
				}
				
				// Clamp to the skill's maximum defined level.
				final int maxLevel = SkillData.getInstance().getMaxLevel(skillId);
				if (level > maxLevel)
				{
					level = maxLevel;
				}
				break;
			}
			else if ((skill.getMinLevel() <= petLevel) && (skillLevel > level))
			{
				level = skillLevel;
			}
		}
		
		if (found && (level == 0))
		{
			return 1;
		}
		
		return level;
	}
	
	/**
	 * Retrieves the list of skills that a pet can learn.<br>
	 * This method returns all skill data associated with the pet.
	 * @return a {@code List} of {@code PetSkillLearn} objects.
	 */
	public List<PetSkillLearn> getAvailableSkills()
	{
		return _skills;
	}
	
	public static class PetSkillLearn extends SkillHolder
	{
		private final int _minLevel;
		
		/**
		 * @param id
		 * @param level
		 * @param minLevel
		 */
		public PetSkillLearn(int id, int level, int minLevel)
		{
			super(id, level);
			_minLevel = minLevel;
		}
		
		/**
		 * @return the minimum pet level required to acquire the skill.
		 */
		public int getMinLevel()
		{
			return _minLevel;
		}
	}
}
