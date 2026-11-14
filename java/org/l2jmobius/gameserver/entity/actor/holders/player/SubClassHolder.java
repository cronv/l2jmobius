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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

/**
 * Definition of a character sub-class, storing its key state and progression data.
 * @author Tempy
 */
public class SubClassHolder
{
	private static final int MAX_LEVEL = PlayerConfig.MAX_SUBCLASS_LEVEL < ExperienceData.getInstance().getMaxLevel() ? PlayerConfig.MAX_SUBCLASS_LEVEL : ExperienceData.getInstance().getMaxLevel() - 1;
	
	private static final int MAX_VITALITY_POINTS = 140000;
	private static final int MIN_VITALITY_POINTS = 0;
	
	private PlayerClass _playerClass;
	private long _exp = ExperienceData.getInstance().getExpForLevel(PlayerConfig.BASE_SUBCLASS_LEVEL);
	private long _sp = 0;
	private int _level = PlayerConfig.BASE_SUBCLASS_LEVEL;
	private int _classIndex = 1;
	private int _vitalityPoints = 0;
	private boolean _dualClass = false;
	
	/**
	 * Creates a holder with all sub-class attributes set to their default values.
	 */
	public SubClassHolder()
	{
		// Used for specifying ALL attributes of a sub class directly,
		// using the preset default values.
	}
	
	/**
	 * Retrieves the current player class.
	 * @return The {@code PlayerClass} of this holder.
	 */
	public PlayerClass getPlayerClass()
	{
		return _playerClass;
	}
	
	/**
	 * Retrieves the player class id.
	 * @return The class id as an {@code int}.
	 */
	public int getId()
	{
		return _playerClass.getId();
	}
	
	/**
	 * Retrieves the current experience of the sub-class.
	 * @return The experience as a {@code long}.
	 */
	public long getExp()
	{
		return _exp;
	}
	
	/**
	 * Retrieves the current skill points of the sub-class.
	 * @return The skill points as a {@code long}.
	 */
	public long getSp()
	{
		return _sp;
	}
	
	/**
	 * Retrieves the current sub-class level.
	 * @return The level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the vitality points, clamped between {@code 0} and {@code 140000}.
	 * @return The vitality points as an {@code int}.
	 */
	public int getVitalityPoints()
	{
		return Math.clamp(_vitalityPoints, MIN_VITALITY_POINTS, MAX_VITALITY_POINTS);
	}
	
	/**
	 * Sets the vitality points, clamped between {@code 0} and {@code 140000}.
	 * @param value The new vitality point amount.
	 */
	public void setVitalityPoints(int value)
	{
		_vitalityPoints = Math.clamp(value, MIN_VITALITY_POINTS, MAX_VITALITY_POINTS);
	}
	
	/**
	 * Retrieves the class index of the sub-class.
	 * @return The class index as an {@code int}.
	 */
	public int getClassIndex()
	{
		return _classIndex;
	}
	
	/**
	 * Sets the player class resolved from the given id.
	 * @param id The player class id.
	 */
	public void setPlayerClass(int id)
	{
		_playerClass = PlayerClass.getPlayerClass(id);
	}
	
	/**
	 * Sets the experience, capping it at the maximum level requirement for a non-dual class.
	 * @param expValue The new experience value.
	 */
	public void setExp(long expValue)
	{
		if (!_dualClass && (expValue > (ExperienceData.getInstance().getExpForLevel(MAX_LEVEL + 1) - 1)))
		{
			_exp = ExperienceData.getInstance().getExpForLevel(MAX_LEVEL + 1) - 1;
			return;
		}
		
		_exp = expValue;
	}
	
	/**
	 * Sets the skill points of the sub-class.
	 * @param spValue The new skill point value.
	 */
	public void setSp(long spValue)
	{
		_sp = spValue;
	}
	
	/**
	 * Sets the class index of the sub-class.
	 * @param classIndex The new class index.
	 */
	public void setClassIndex(int classIndex)
	{
		_classIndex = classIndex;
	}
	
	/**
	 * Indicates whether the character has a dual class active.
	 * @return {@code true} if a dual class, {@code false} otherwise.
	 */
	public boolean isDualClass()
	{
		return _dualClass;
	}
	
	/**
	 * Sets whether the character has a dual class active.
	 * @param dualClass The dual class status.
	 */
	public void setDualClassActive(boolean dualClass)
	{
		_dualClass = dualClass;
	}
	
	/**
	 * Sets the sub-class level, capping at the maximum for a non-dual class and flooring at the base sub-class level.
	 * @param levelValue The new level.
	 */
	public void setLevel(int levelValue)
	{
		if (!_dualClass && (levelValue > MAX_LEVEL))
		{
			_level = MAX_LEVEL;
			return;
		}
		else if (levelValue < PlayerConfig.BASE_SUBCLASS_LEVEL)
		{
			_level = PlayerConfig.BASE_SUBCLASS_LEVEL;
			return;
		}
		
		_level = levelValue;
	}
}
