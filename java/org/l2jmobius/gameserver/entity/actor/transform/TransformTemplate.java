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
package org.l2jmobius.gameserver.entity.actor.transform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a data template for defining the properties of a transformation.<br>
 * It stores configuration details such as required items, stats, and visual effects.<br>
 * Use this class to initialize {@code Transform} instances within the game world.
 * @author UnAfraid
 */
public class TransformTemplate
{
	private final Float _collisionRadius;
	private final Float _collisionHeight;
	private final WeaponType _baseAttackType;
	private List<SkillHolder> _skills;
	private List<AdditionalSkillHolder> _additionalSkills;
	private List<AdditionalItemHolder> _additionalItems;
	private Map<Integer, Integer> _baseDefense;
	private Map<Integer, Double> _baseStats;
	private int[] _actions;
	private final Map<Integer, TransformLevelData> _data = new LinkedHashMap<>(100);
	
	/**
	 * Creates a new {@code TransformTemplate} using the provided data.<br>
	 * This constructor initializes collision values and base attack types.<br>
	 * It also populates various stats and defenses based on the contents of the {@code StatSet}.
	 * @param set The {@code StatSet} containing all configuration values for this template.
	 */
	public TransformTemplate(StatSet set)
	{
		_collisionRadius = set.contains("radius") ? set.getFloat("radius") : null;
		_collisionHeight = set.contains("height") ? set.getFloat("height") : null;
		_baseAttackType = set.getEnum("attackType", WeaponType.class, null);
		if (set.contains("range"))
		{
			addStats(Stat.PHYSICAL_ATTACK_RANGE, set.getDouble("range", 0));
		}
		
		if (set.contains("randomDamage"))
		{
			addStats(Stat.RANDOM_DAMAGE, set.getDouble("randomDamage", 0));
		}
		
		if (set.contains("walk"))
		{
			addStats(Stat.WALK_SPEED, set.getDouble("walk", 0));
		}
		
		if (set.contains("run"))
		{
			addStats(Stat.RUN_SPEED, set.getDouble("run", 0));
		}
		
		if (set.contains("waterWalk"))
		{
			addStats(Stat.SWIM_WALK_SPEED, set.getDouble("waterWalk", 0));
		}
		
		if (set.contains("waterRun"))
		{
			addStats(Stat.SWIM_RUN_SPEED, set.getDouble("waterRun", 0));
		}
		
		if (set.contains("flyWalk"))
		{
			addStats(Stat.FLY_WALK_SPEED, set.getDouble("flyWalk", 0));
		}
		
		if (set.contains("flyRun"))
		{
			addStats(Stat.FLY_RUN_SPEED, set.getDouble("flyRun", 0));
		}
		
		if (set.contains("pAtk"))
		{
			addStats(Stat.PHYSICAL_ATTACK, set.getDouble("pAtk", 0));
		}
		
		if (set.contains("mAtk"))
		{
			addStats(Stat.MAGIC_ATTACK, set.getDouble("mAtk", 0));
		}
		
		if (set.contains("range"))
		{
			addStats(Stat.PHYSICAL_ATTACK_RANGE, set.getInt("range", 0));
		}
		
		if (set.contains("attackSpeed"))
		{
			addStats(Stat.PHYSICAL_ATTACK_SPEED, set.getInt("attackSpeed", 0));
		}
		
		if (set.contains("critRate"))
		{
			addStats(Stat.CRITICAL_RATE, set.getInt("critRate", 0));
		}
		
		if (set.contains("str"))
		{
			addStats(Stat.STAT_STR, set.getInt("str", 0));
		}
		
		if (set.contains("int"))
		{
			addStats(Stat.STAT_INT, set.getInt("int", 0));
		}
		
		if (set.contains("con"))
		{
			addStats(Stat.STAT_CON, set.getInt("con", 0));
		}
		
		if (set.contains("dex"))
		{
			addStats(Stat.STAT_DEX, set.getInt("dex", 0));
		}
		
		if (set.contains("wit"))
		{
			addStats(Stat.STAT_WIT, set.getInt("wit", 0));
		}
		
		if (set.contains("men"))
		{
			addStats(Stat.STAT_MEN, set.getInt("men", 0));
		}
		
		if (set.contains("chest"))
		{
			addDefense(Inventory.PAPERDOLL_CHEST, set.getInt("chest", 0));
		}
		
		if (set.contains("legs"))
		{
			addDefense(Inventory.PAPERDOLL_LEGS, set.getInt("legs", 0));
		}
		
		if (set.contains("head"))
		{
			addDefense(Inventory.PAPERDOLL_HEAD, set.getInt("head", 0));
		}
		
		if (set.contains("feet"))
		{
			addDefense(Inventory.PAPERDOLL_FEET, set.getInt("feet", 0));
		}
		
		if (set.contains("gloves"))
		{
			addDefense(Inventory.PAPERDOLL_GLOVES, set.getInt("gloves", 0));
		}
		
		if (set.contains("underwear"))
		{
			addDefense(Inventory.PAPERDOLL_UNDER, set.getInt("underwear", 0));
		}
		
		if (set.contains("cloak"))
		{
			addDefense(Inventory.PAPERDOLL_CLOAK, set.getInt("cloak", 0));
		}
		
		if (set.contains("rear"))
		{
			addDefense(Inventory.PAPERDOLL_REAR, set.getInt("rear", 0));
		}
		
		if (set.contains("lear"))
		{
			addDefense(Inventory.PAPERDOLL_LEAR, set.getInt("lear", 0));
		}
		
		if (set.contains("rfinger"))
		{
			addDefense(Inventory.PAPERDOLL_RFINGER, set.getInt("rfinger", 0));
		}
		
		if (set.contains("lfinger"))
		{
			addDefense(Inventory.PAPERDOLL_LFINGER, set.getInt("lfinger", 0));
		}
		
		if (set.contains("neck"))
		{
			addDefense(Inventory.PAPERDOLL_NECK, set.getInt("neck", 0));
		}
	}
	
	/**
	 * Adds a defense value to the base defense map.<br>
	 * This method updates or inserts a specific defense type.
	 * @param type The unique identifier for the defense category.
	 * @param value The numerical amount of defense to apply.
	 */
	private void addDefense(int type, int value)
	{
		if (_baseDefense == null)
		{
			_baseDefense = new HashMap<>();
		}
		
		_baseDefense.put(type, value);
	}
	
	/**
	 * Retrieves the defense value for a specific type.<br>
	 * It returns the default value if no entry exists.
	 * @param type The identifier for the defense category.
	 * @param defaultValue The value to return if the type is not found.
	 * @return The defense value associated with the given type.
	 */
	public int getDefense(int type, int defaultValue)
	{
		return (_baseDefense == null) ? defaultValue : _baseDefense.getOrDefault(type, defaultValue);
	}
	
	/**
	 * Adds a specific value to a {@code Stat}.<br>
	 * This method updates the internal map of base statistics.<br>
	 * It ensures the internal map is initialized before adding the data.
	 * @param stat The {@code Stat} type to update.
	 * @param value The numerical value to assign to the statistic.
	 */
	private void addStats(Stat stat, double value)
	{
		if (_baseStats == null)
		{
			_baseStats = new HashMap<>();
		}
		
		_baseStats.put(stat.ordinal(), value);
	}
	
	/**
	 * Retrieves the value of a specific statistic for this template.<br>
	 * If the statistic is not found, it returns the provided default value.
	 * @param stat The {@code Stat} type to look up.
	 * @param defaultValue The value to return if the statistic does not exist.
	 * @return The value of the requested statistic or the default value.
	 */
	public double getStats(Stat stat, double defaultValue)
	{
		return _baseStats == null ? defaultValue : _baseStats.getOrDefault(stat.ordinal(), defaultValue);
	}
	
	/**
	 * Gets the collision radius of this transform.<br>
	 * This value determines how large the physical hit area is.
	 * @return the {@code Float} value representing the collision radius.
	 */
	public Float getCollisionRadius()
	{
		return _collisionRadius;
	}
	
	/**
	 * Retrieves the collision height of the transform.<br>
	 * This value determines how tall the entity is for physics calculations.
	 * @return The {@code Float} height of the collision box.
	 */
	public Float getCollisionHeight()
	{
		return _collisionHeight;
	}
	
	/**
	 * Returns the primary attack type for this transform.<br>
	 * This value determines how the entity interacts with targets.
	 * @return the {@code WeaponType} of the base attack.
	 */
	public WeaponType getBaseAttackType()
	{
		return _baseAttackType;
	}
	
	/**
	 * Adds a new skill to the {@code TransformTemplate}.<br>
	 * This method initializes the internal list if it is currently {@code null}.<br>
	 * The provided {@code SkillHolder} is added to the collection.
	 * @param holder The {@code SkillHolder} object to be added.
	 */
	public void addSkill(SkillHolder holder)
	{
		if (_skills == null)
		{
			_skills = new ArrayList<>();
		}
		
		_skills.add(holder);
	}
	
	/**
	 * Retrieves the list of skills associated with this template.<br>
	 * Returns an empty {@code List} if no skills are present.
	 * @return a {@code List} of {@code SkillHolder} objects.
	 */
	public List<SkillHolder> getSkills()
	{
		return _skills != null ? _skills : Collections.emptyList();
	}
	
	/**
	 * Adds a new skill to the list of additional skills.<br>
	 * This method initializes the internal list if it is currently {@code null}.
	 * @param holder The {@code AdditionalSkillHolder} object to be added.
	 */
	public void addAdditionalSkill(AdditionalSkillHolder holder)
	{
		if (_additionalSkills == null)
		{
			_additionalSkills = new ArrayList<>();
		}
		
		_additionalSkills.add(holder);
	}
	
	/**
	 * Retrieves the list of additional skills for this transform.<br>
	 * Returns an empty {@code List} if no additional skills exist.
	 * @return a {@code List} of {@code AdditionalSkillHolder} objects.
	 */
	public List<AdditionalSkillHolder> getAdditionalSkills()
	{
		return _additionalSkills != null ? _additionalSkills : Collections.emptyList();
	}
	
	/**
	 * Adds a new item to the list of additional items.<br>
	 * This method initializes the internal list if it is currently {@code null}.<br>
	 * The provided {@code holder} is then added to the collection.
	 * @param holder The {@code AdditionalItemHolder} object to be added.
	 */
	public void addAdditionalItem(AdditionalItemHolder holder)
	{
		if (_additionalItems == null)
		{
			_additionalItems = new ArrayList<>();
		}
		
		_additionalItems.add(holder);
	}
	
	/**
	 * Retrieves the list of additional items for this transform.<br>
	 * Returns an empty {@code List} if no items are present.
	 * @return a {@code List} of {@code AdditionalItemHolder} objects.
	 */
	public List<AdditionalItemHolder> getAdditionalItems()
	{
		return _additionalItems != null ? _additionalItems : Collections.emptyList();
	}
	
	/**
	 * Sets the list of basic actions for this transform.<br>
	 * This method updates the internal {@code _actions} array.
	 * @param actions An array of integers representing the action IDs.
	 */
	public void setBasicActionList(int[] actions)
	{
		_actions = actions;
	}
	
	/**
	 * Retrieves the list of basic actions for this transform.<br>
	 * This method returns the internal {@code int[]} array of actions.
	 * @return an {@code int[]} array containing the basic action IDs.
	 */
	public int[] getBasicActionList()
	{
		return _actions;
	}
	
	/**
	 * Checks if the transform has a list of basic actions.<br>
	 * It returns {@code true} if the internal action array is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if an action list exists, {@code false} otherwise.
	 */
	public boolean hasBasicActionList()
	{
		return _actions != null;
	}
	
	/**
	 * Adds new level information to the template.<br>
	 * This method stores the {@code TransformLevelData} in the internal map.<br>
	 * The data is indexed by its level value.
	 * @param data The {@code TransformLevelData} object to be added.
	 */
	public void addLevelData(TransformLevelData data)
	{
		_data.put(data.getLevel(), data);
	}
	
	/**
	 * Retrieves the data for a specific transform level.<br>
	 * This method looks up the information based on the provided level number.
	 * @param level The level to retrieve data for.
	 * @return The {@code TransformLevelData} associated with the given level, or {@code null} if not found.
	 */
	public TransformLevelData getData(int level)
	{
		return _data.get(level);
	}
}
