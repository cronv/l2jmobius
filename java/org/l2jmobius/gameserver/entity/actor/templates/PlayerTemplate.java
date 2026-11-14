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
package org.l2jmobius.gameserver.entity.actor.templates;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the base configuration and properties for a {@code Player} entity.<br>
 * It serves as a template used to initialize player-specific data such as stats, inventory, and classes.
 * @author mkizub, Zoey76
 */
public class PlayerTemplate extends CreatureTemplate
{
	private final PlayerClass _playerClass;
	
	private final float[] _baseHp;
	private final float[] _baseMp;
	private final float[] _baseCp;
	
	private final double[] _baseHpReg;
	private final double[] _baseMpReg;
	private final double[] _baseCpReg;
	
	private final float _fCollisionHeightFemale;
	private final float _fCollisionRadiusFemale;
	
	private final int _baseSafeFallHeight;
	
	private final List<Location> _creationPoints;
	private final Map<Integer, Integer> _baseSlotDef;
	
	/**
	 * Creates a new {@code PlayerTemplate} using the provided statistics and spawn locations.<br>
	 * This constructor initializes base stats, defense slots, and collision data from the {@code StatSet}.
	 * @param set The {@code StatSet} containing all player attributes and configuration values.
	 * @param creationPoints A {@code List} of {@code Location} objects where players can spawn.
	 */
	public PlayerTemplate(StatSet set, List<Location> creationPoints)
	{
		super(set);
		_playerClass = PlayerClass.getPlayerClass(set.getInt("classId"));
		setRace(_playerClass.getRace());
		final int maxLevel = ExperienceData.getInstance().getMaxLevel();
		_baseHp = new float[maxLevel];
		_baseMp = new float[maxLevel];
		_baseCp = new float[maxLevel];
		_baseHpReg = new double[maxLevel];
		_baseMpReg = new double[maxLevel];
		_baseCpReg = new double[maxLevel];
		_baseSlotDef = new HashMap<>(13);
		_baseSlotDef.put(Inventory.PAPERDOLL_CHEST, set.getInt("basePDefchest", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LEGS, set.getInt("basePDeflegs", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_HEAD, set.getInt("basePDefhead", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_FEET, set.getInt("basePDeffeet", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_GLOVES, set.getInt("basePDefgloves", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_UNDER, set.getInt("basePDefunderwear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_CLOAK, set.getInt("basePDefcloak", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_REAR, set.getInt("baseMDefrear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LEAR, set.getInt("baseMDeflear", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_RFINGER, set.getInt("baseMDefrfinger", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_LFINGER, set.getInt("baseMDefrfinger", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_NECK, set.getInt("baseMDefneck", 0));
		_baseSlotDef.put(Inventory.PAPERDOLL_HAIR, set.getInt("basePDefhair", 0));
		
		_fCollisionRadiusFemale = set.getFloat("collisionFemaleradius");
		_fCollisionHeightFemale = set.getFloat("collisionFemaleheight");
		_baseSafeFallHeight = set.getInt("baseSafeFall", 333);
		_creationPoints = creationPoints;
	}
	
	/**
	 * Retrieves the character class of the player.<br>
	 * This method returns the {@code PlayerClass} assigned to this template.
	 * @return the {@code PlayerClass} of the player.
	 */
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	/**
	 * Retrieves a random starting location for the player.<br>
	 * This method selects one point from the list of available creation points.
	 * @return The selected {@code Location} object.
	 */
	public Location getCreationPoint()
	{
		return _creationPoints.get(Rnd.get(_creationPoints.size()));
	}
	
	/**
	 * Sets a specific base statistic for a player at a given level.<br>
	 * This method updates values like {@code hp}, {@code mp}, or regeneration rates.
	 * @param paramName The name of the statistic to update.
	 * @param level The character level for which the value is set.
	 * @param value The numeric value to assign to the statistic.
	 */
	public void setUpgainValue(String paramName, int level, double value)
	{
		switch (paramName)
		{
			case "hp":
			{
				_baseHp[level] = (float) value;
				break;
			}
			case "mp":
			{
				_baseMp[level] = (float) value;
				break;
			}
			case "cp":
			{
				_baseCp[level] = (float) value;
				break;
			}
			case "hpRegen":
			{
				_baseHpReg[level] = value;
				break;
			}
			case "mpRegen":
			{
				_baseMpReg[level] = value;
				break;
			}
			case "cpRegen":
			{
				_baseCpReg[level] = value;
				break;
			}
		}
	}
	
	/**
	 * Retrieves the base maximum health for a specific level.<br>
	 * This value is pulled from the internal {@code _baseHp} array.
	 * @param level The character level to check.
	 * @return The base maximum health as a {@code float}.
	 */
	public float getBaseHpMax(int level)
	{
		return _baseHp[level];
	}
	
	/**
	 * Retrieves the base maximum mana for a specific level.<br>
	 * This value is used to calculate the player's starting mana pool.
	 * @param level The character level to check.
	 * @return The base maximum mana as a {@code float}.
	 */
	public float getBaseMpMax(int level)
	{
		return _baseMp[level];
	}
	
	/**
	 * Retrieves the base maximum CP for a specific level.<br>
	 * This value is used to determine the starting CP of a player.
	 * @param level The character level to check.
	 * @return The base maximum CP as a {@code float}.
	 */
	public float getBaseCpMax(int level)
	{
		return _baseCp[level];
	}
	
	/**
	 * Calculates the base health regeneration for a specific level.<br>
	 * This value is retrieved from the internal template data.
	 * @param level The character level to check.
	 * @return The base health regeneration amount as a {@code double}.
	 */
	public double getBaseHpRegen(int level)
	{
		return _baseHpReg[level];
	}
	
	/**
	 * Calculates the base Mana Point regeneration for a specific level.<br>
	 * This value is retrieved from the internal template data.
	 * @param level The character level to check.
	 * @return The base MP regeneration amount as a {@code double}.
	 */
	public double getBaseMpRegen(int level)
	{
		return _baseMpReg[level];
	}
	
	/**
	 * Calculates the base CP regeneration rate for a specific level.<br>
	 * This value is retrieved from the internal template data.
	 * @param level The character level to check.
	 * @return The base CP regeneration amount as a {@code double}.
	 */
	public double getBaseCpRegen(int level)
	{
		return _baseCpReg[level];
	}
	
	/**
	 * Retrieves the base defense value for a specific equipment slot.<br>
	 * It looks up the value using the provided {@code slotId}.<br>
	 * If the slot is not found, it returns {@code 0}.
	 * @param slotId The unique identifier of the equipment slot.
	 * @return The base defense value for the given slot.
	 */
	public int getBaseDefBySlot(int slotId)
	{
		return _baseSlotDef.getOrDefault(slotId, 0);
	}
	
	/**
	 * Returns the collision height for female characters.<br>
	 * This value is used to determine how high a character can stand or collide.
	 * @return The collision height as a {@code float}.
	 */
	public float getFCollisionHeightFemale()
	{
		return _fCollisionHeightFemale;
	}
	
	/**
	 * Gets the collision radius for female characters.<br>
	 * This value is used to determine the physical size of a female character in the game world.
	 * @return The collision radius as a {@code float}.
	 */
	public float getFCollisionRadiusFemale()
	{
		return _fCollisionRadiusFemale;
	}
	
	/**
	 * Returns the maximum height a player can fall from without taking damage.<br>
	 * This value is retrieved from the {@code _baseSafeFallHeight} field.
	 * @return The safe fall height as an {@code int}.
	 */
	public int getSafeFallHeight()
	{
		return _baseSafeFallHeight;
	}
}
