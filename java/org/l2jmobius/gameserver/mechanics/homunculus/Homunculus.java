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
package org.l2jmobius.gameserver.mechanics.homunculus;

/**
 * Represents a homunculus entity within the game world.<br>
 * This class handles the core logic and state for these companion creatures.
 * @author NviX
 */
public class Homunculus implements Comparable<Homunculus>
{
	private final HomunculusTemplate _template;
	private int _slot;
	private int _level;
	private int _exp;
	private int _skillLevel1;
	private int _skillLevel2;
	private int _skillLevel3;
	private int _skillLevel4;
	private int _skillLevel5;
	private boolean _isActive;
	
	/**
	 * Creates a new {@code Homunculus} instance.<br>
	 * This constructor initializes all basic stats and skills for the homunculus.
	 * @param template The {@code HomunculusTemplate} used to define the base properties.
	 * @param slot The specific slot index where this homunculus is placed.
	 * @param level The current experience level of the homunculus.
	 * @param exp The current experience points of the homunculus.
	 * @param skillLevel1 The proficiency level for the first skill.
	 * @param skillLevel2 The proficiency level for the second skill.
	 * @param skillLevel3 The proficiency level for the third skill.
	 * @param skillLevel4 The proficiency level for the fourth skill.
	 * @param skillLevel5 The proficiency level for the fifth skill.
	 * @param isActive The initial active state of the homunculus, set to {@code true} or {@code false}.
	 */
	public Homunculus(HomunculusTemplate template, int slot, int level, int exp, int skillLevel1, int skillLevel2, int skillLevel3, int skillLevel4, int skillLevel5, boolean isActive)
	{
		_template = template;
		_slot = slot;
		_level = level;
		_exp = exp;
		_skillLevel1 = skillLevel1;
		_skillLevel2 = skillLevel2;
		_skillLevel3 = skillLevel3;
		_skillLevel4 = skillLevel4;
		_skillLevel5 = skillLevel5;
		_isActive = isActive;
	}
	
	/**
	 * Retrieves the template associated with this {@code Homunculus}.<br>
	 * This provides access to the base data for the entity.
	 * @return the {@code HomunculusTemplate} object.
	 */
	public HomunculusTemplate getTemplate()
	{
		return _template;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Homunculus}.<br>
	 * This value is obtained from the associated {@code HomunculusTemplate}.
	 * @return The unique integer ID of the homunculus.
	 */
	public int getId()
	{
		return _template.getId();
	}
	
	/**
	 * Returns the name of the {@code Homunculus}.<br>
	 * This value is retrieved from the associated {@code HomunculusTemplate}.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _template.getName();
	}
	
	/**
	 * Retrieves the type of this {@code Homunculus}.<br>
	 * This value is fetched from the associated {@code HomunculusTemplate}.
	 * @return The integer type of the {@code Homunculus}.
	 */
	public int getType()
	{
		return _template.getType();
	}
	
	/**
	 * Sets the current slot for this {@code Homunculus}.<br>
	 * This updates the internal {@code _slot} value.
	 * @param slot The new slot index to assign.
	 */
	public void setSlot(int slot)
	{
		_slot = slot;
	}
	
	/**
	 * Retrieves the current slot index of this {@code Homunculus}.<br>
	 * This value identifies which position the homunculus occupies.
	 * @return The integer value representing the slot.
	 */
	public int getSlot()
	{
		return _slot;
	}
	
	/**
	 * Updates the current level of the {@code Homunculus}.<br>
	 * This method sets the internal level variable to a new value.
	 * @param level The new level to assign.
	 */
	public void setLevel(int level)
	{
		_level = level;
	}
	
	/**
	 * Retrieves the current level of the {@code Homunculus}.<br>
	 * This value represents the progression status of the entity.
	 * @return The current level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Updates the experience points of the {@code Homunculus}.<br>
	 * This method sets the internal {@code _exp} value.
	 * @param exp The new experience value to set.
	 */
	public void setExp(int exp)
	{
		_exp = exp;
	}
	
	/**
	 * Retrieves the current experience points of the {@code Homunculus}.<br>
	 * This value represents the total progress made by the entity.
	 * @return The current experience as an {@code int}.
	 */
	public int getExp()
	{
		return _exp;
	}
	
	/**
	 * Sets the first skill level for this {@code Homunculus}.<br>
	 * This updates the internal value of {@code _skillLevel1}.
	 * @param level The new integer value for the first skill level.
	 */
	public void setSkillLevel1(int level)
	{
		_skillLevel1 = level;
	}
	
	/**
	 * Retrieves the current level of the first skill.<br>
	 * This value is stored in the {@code _skillLevel1} field.
	 * @return The integer level of the first skill.
	 */
	public int getSkillLevel1()
	{
		return _skillLevel1;
	}
	
	/**
	 * Updates the second skill level of the {@code Homunculus}.<br>
	 * This method sets the internal value for {@code _skillLevel2}.
	 * @param level The new integer value for the second skill level.
	 */
	public void setSkillLevel2(int level)
	{
		_skillLevel2 = level;
	}
	
	/**
	 * Retrieves the current level of the second skill.<br>
	 * This value is stored in the {@code _skillLevel2} field.
	 * @return The integer level of the second skill.
	 */
	public int getSkillLevel2()
	{
		return _skillLevel2;
	}
	
	/**
	 * Updates the third skill level of the {@code Homunculus}.<br>
	 * This method sets the internal value for {@code _skillLevel3}.
	 * @param level The new integer value for the third skill level.
	 */
	public void setSkillLevel3(int level)
	{
		_skillLevel3 = level;
	}
	
	/**
	 * Retrieves the current level of the third skill.<br>
	 * This value is stored in the {@code _skillLevel3} field.
	 * @return The integer level of the third skill.
	 */
	public int getSkillLevel3()
	{
		return _skillLevel3;
	}
	
	/**
	 * Updates the skill level for the fourth skill.<br>
	 * This method sets the {@code _skillLevel4} field to a new value.
	 * @param level The new integer value for the fourth skill level.
	 */
	public void setSkillLevel4(int level)
	{
		_skillLevel4 = level;
	}
	
	/**
	 * Retrieves the current level of the fourth skill.<br>
	 * This value is stored in the {@code _skillLevel4} field.
	 * @return The integer level of the fourth skill.
	 */
	public int getSkillLevel4()
	{
		return _skillLevel4;
	}
	
	/**
	 * Sets the level for the fifth skill of the {@code Homunculus}.<br>
	 * This updates the internal value used to determine skill power.
	 * @param level The new integer value for the fifth skill.
	 */
	public void setSkillLevel5(int level)
	{
		_skillLevel5 = level;
	}
	
	/**
	 * Retrieves the level of the fifth skill.<br>
	 * This value is stored in the {@code _skillLevel5} field.
	 * @return The current level of the fifth skill as an {@code int}.
	 */
	public int getSkillLevel5()
	{
		return _skillLevel5;
	}
	
	/**
	 * Retrieves the current health points of the {@code Homunculus}.<br>
	 * The value is determined based on the current level.
	 * @return the current health points as an {@code int}.
	 */
	public int getHp()
	{
		switch (_level)
		{
			case 1:
			{
				return _template.getHpLevel1();
			}
			case 2:
			{
				return _template.getHpLevel2();
			}
			case 3:
			{
				return _template.getHpLevel3();
			}
			case 4:
			{
				return _template.getHpLevel4();
			}
			case 5:
			{
				return _template.getHpLevel5();
			}
			default:
			{
				return _template.getHpLevel1();
			}
		}
	}
	
	/**
	 * Retrieves the attack power of the {@code Homunculus}.<br>
	 * The value depends on the current level of the entity.
	 * @return the current attack power as an {@code int}.
	 */
	public int getAtk()
	{
		switch (_level)
		{
			case 1:
			{
				return _template.getAtkLevel1();
			}
			case 2:
			{
				return _template.getAtkLevel2();
			}
			case 3:
			{
				return _template.getAtkLevel3();
			}
			case 4:
			{
				return _template.getAtkLevel4();
			}
			case 5:
			{
				return _template.getAtkLevel5();
			}
		}
		
		return _template.getAtkLevel1();
	}
	
	/**
	 * Retrieves the defense value for this {@code Homunculus}.<br>
	 * The value is determined based on the current level.<br>
	 * It returns a value from the associated {@code HomunculusTemplate}.
	 * @return the defense value as an {@code int}
	 */
	public int getDef()
	{
		switch (_level)
		{
			case 1:
			{
				return _template.getDefLevel1();
			}
			case 2:
			{
				return _template.getDefLevel2();
			}
			case 3:
			{
				return _template.getDefLevel3();
			}
			case 4:
			{
				return _template.getDefLevel4();
			}
			case 5:
			{
				return _template.getDefLevel5();
			}
		}
		
		return _template.getDefLevel1();
	}
	
	/**
	 * Retrieves the critical hit rate of this {@code Homunculus}.<br>
	 * This value is fetched from the associated {@code HomunculusTemplate}.
	 * @return The critical hit rate as an {@code int}.
	 */
	public int getCritRate()
	{
		return _template.getCritRate();
	}
	
	/**
	 * Sets the active status of the {@code Homunculus}.<br>
	 * Use this to enable or disable the entity.
	 * @param active The new activity state to set. Set to {@code true} to activate and {@code false} to deactivate.
	 */
	public void setActive(boolean active)
	{
		_isActive = active;
	}
	
	/**
	 * Checks if the {@code Homunculus} is currently active.
	 * @return {@code true} if the homunculus is active, {@code false} otherwise.
	 */
	public boolean isActive()
	{
		return _isActive;
	}
	
	/**
	 * Returns a string representation of the {@code Homunculus}.<br>
	 * This includes the unique ID and the active status.
	 * @return A string containing the object details.
	 */
	@Override
	public String toString()
	{
		return "Homunculus[id=" + _template.getId() + ", isActive=" + _isActive + "]";
	}
	
	/**
	 * Compares this {@code Homunculus} with another {@code Homunculus} based on their IDs.<br>
	 * This method is used to determine the sorting order of homunculi.
	 * @param o The other {@code Homunculus} to compare against.
	 * @return A negative integer if this ID is smaller, a positive integer if larger, or zero if they are equal.
	 */
	@Override
	public int compareTo(Homunculus o)
	{
		return getId() - o.getId();
	}
}
