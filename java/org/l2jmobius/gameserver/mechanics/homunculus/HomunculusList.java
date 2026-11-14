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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.HomunculusManager;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Manages a collection of {@code Homunculus} entities associated with a specific player.<br>
 * This class provides helper methods to track and manipulate the active homunculi for a user.
 * @author nexvill, Mobius
 */
public class HomunculusList
{
	private List<Homunculus> _homunculusList = Collections.emptyList();
	private int _hp;
	private int _atk;
	private int _def;
	private int _critRate;
	
	private final Player _owner;
	private final Map<Integer, SkillHolder> _skills = new ConcurrentHashMap<>();
	
	/**
	 * Creates a new {@code HomunculusList} for a specific player.<br>
	 * This list manages the homunculi owned by the provided {@code Player}.
	 * @param owner The {@code Player} who owns this list.
	 */
	public HomunculusList(Player owner)
	{
		_owner = owner;
	}
	
	/**
	 * Resets the internal list of homunculuses.<br>
	 * This method fetches all homunculuses belonging to the {@code _owner}.<br>
	 * It ensures the count does not exceed {@code PlayerConfig.MAX_HOMUNCULUS_COUNT}.<br>
	 * Finally, it calls {@code refreshStats(false)} to update values.
	 */
	public void restore()
	{
		_homunculusList = new ArrayList<>();
		
		for (Homunculus homunculus : HomunculusManager.getInstance().select(_owner))
		{
			_homunculusList.add(homunculus);
		}
		
		final int listSize = _homunculusList.size();
		if (listSize > PlayerConfig.MAX_HOMUNCULUS_COUNT)
		{
			for (int i = PlayerConfig.MAX_HOMUNCULUS_COUNT; i < listSize; i++)
			{
				_homunculusList.remove(i);
			}
		}
		
		refreshStats(false);
	}
	
	/**
	 * Retrieves a {@code Homunculus} from a specific slot.<br>
	 * It searches through the internal list for a matching index.<br>
	 * Returns {@code null} if no homunculus is found in that slot.
	 * @param slot The index of the slot to check.
	 * @return The {@code Homunculus} at the given slot, or {@code null}.
	 */
	public Homunculus get(int slot)
	{
		for (Homunculus homunculus : _homunculusList)
		{
			if (homunculus.getSlot() == slot)
			{
				return homunculus;
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if a specific homunculus exists in the list.<br>
	 * It searches for a match based on the provided template ID.
	 * @param templateId The unique identifier of the homunculus to find.
	 * @return {@code true} if the homunculus is found, otherwise {@code false}.
	 */
	public boolean hasHomunculus(int templateId)
	{
		for (Homunculus homunculus : _homunculusList)
		{
			if (homunculus.getId() == templateId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns the total number of homunculi in the list.<br>
	 * This count includes all occupied slots.
	 * @return The current size of the {@code HomunculusList}.
	 */
	public int size()
	{
		return _homunculusList.size();
	}
	
	/**
	 * Calculates the number of available slots for homunculi.<br>
	 * It subtracts the current {@code size()} from the maximum allowed count.<br>
	 * The result will never be less than {@code 0}.
	 * @return The number of free slots remaining.
	 */
	public int getFreeSize()
	{
		return Math.max(0, PlayerConfig.MAX_HOMUNCULUS_COUNT - size());
	}
	
	/**
	 * Checks if the list has no available slots.<br>
	 * It returns {@code true} if all positions are occupied.<br>
	 * It returns {@code false} if there is at least one free space.
	 * @return {@code true} if the list is full, otherwise {@code false}.
	 */
	public boolean isFull()
	{
		return getFreeSize() == 0;
	}
	
	/**
	 * Adds a {@code Homunculus} to the current list.<br>
	 * This method checks if there is enough space before adding.<br>
	 * It updates the owner's stats and sends the new skill list.
	 * @param homunculus The {@code Homunculus} object to add.
	 * @return {@code true} if the addition was successful, otherwise {@code false}.
	 */
	public boolean add(Homunculus homunculus)
	{
		if (isFull())
		{
			return false;
		}
		
		if (HomunculusManager.getInstance().insert(_owner, homunculus))
		{
			_homunculusList.add(homunculus);
			if (refreshStats(true))
			{
				_owner.sendSkillList();
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Updates the information for a specific {@code Homunculus}.<br>
	 * This method communicates with the {@code HomunculusManager} to apply changes.
	 * @param homunculus The {@code Homunculus} object to be updated.
	 * @return {@code true} if the update was successful, {@code false} otherwise.
	 */
	public boolean update(Homunculus homunculus)
	{
		return HomunculusManager.getInstance().update(_owner, homunculus);
	}
	
	/**
	 * Removes a specific {@code Homunculus} from the list.<br>
	 * This method also deletes the entity from the database and updates the remaining slots.
	 * @param homunculus The {@code Homunculus} object to remove.
	 * @return {@code true} if the removal was successful, {@code false} otherwise.
	 */
	public boolean remove(Homunculus homunculus)
	{
		// Remove from list.
		if (!_homunculusList.remove(homunculus))
		{
			return false;
		}
		
		// Remove from database.
		if (HomunculusManager.getInstance().delete(_owner, homunculus))
		{
			// Remove all.
			for (Homunculus homu : _homunculusList)
			{
				HomunculusManager.getInstance().delete(_owner, homu);
			}
			
			// Order.
			int slot = 0;
			for (int i = 0; i < PlayerConfig.MAX_HOMUNCULUS_COUNT; i++)
			{
				final Homunculus homu = get(i);
				if (homu != null)
				{
					homu.setSlot(slot++);
					HomunculusManager.getInstance().insert(_owner, homu);
				}
			}
			
			// Refresh stats.
			if (refreshStats(true))
			{
				_owner.sendSkillList();
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Recalculates the stats for all active homunculi in the list.<br>
	 * It updates the internal health, attack, defense, and skill values.
	 * @param send Determines whether to synchronize the updated stats with the client.
	 * @return {@code true} if the statistics were successfully refreshed.
	 */
	public boolean refreshStats(boolean send)
	{
		_hp = 0;
		_atk = 0;
		_def = 0;
		_critRate = 0;
		
		boolean updateSkillList = false;
		for (int skillId : _skills.keySet())
		{
			if (_owner.removeSkill(skillId, false) != null)
			{
				updateSkillList = true;
			}
		}
		
		_skills.clear();
		
		for (Homunculus homunculus : _homunculusList)
		{
			if (!homunculus.isActive())
			{
				continue;
			}
			
			switch (homunculus.getLevel())
			{
				case 1:
				{
					_hp = homunculus.getTemplate().getHpLevel1();
					_atk = homunculus.getTemplate().getAtkLevel1();
					_def = homunculus.getTemplate().getDefLevel1();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
				case 2:
				{
					_hp = homunculus.getTemplate().getHpLevel2();
					_atk = homunculus.getTemplate().getAtkLevel2();
					_def = homunculus.getTemplate().getDefLevel2();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel2() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId2(), homunculus.getSkillLevel2());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
				case 3:
				{
					_hp = homunculus.getTemplate().getHpLevel3();
					_atk = homunculus.getTemplate().getAtkLevel3();
					_def = homunculus.getTemplate().getDefLevel3();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel2() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId2(), homunculus.getSkillLevel2());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel3() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId3(), homunculus.getSkillLevel3());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
				case 4:
				{
					_hp = homunculus.getTemplate().getHpLevel4();
					_atk = homunculus.getTemplate().getAtkLevel4();
					_def = homunculus.getTemplate().getDefLevel4();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel2() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId2(), homunculus.getSkillLevel2());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel3() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId3(), homunculus.getSkillLevel3());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel4() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId4(), homunculus.getSkillLevel4());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
				case 5:
				{
					_hp = homunculus.getTemplate().getHpLevel5();
					_atk = homunculus.getTemplate().getAtkLevel5();
					_def = homunculus.getTemplate().getDefLevel5();
					if (homunculus.getSkillLevel1() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId1(), homunculus.getSkillLevel1());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel2() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId2(), homunculus.getSkillLevel2());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel3() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId3(), homunculus.getSkillLevel3());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel4() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId4(), homunculus.getSkillLevel4());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					
					if (homunculus.getSkillLevel5() > 0)
					{
						final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getSkillId5(), homunculus.getSkillLevel5());
						final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
						if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
						{
							_skills.put(skillEntry.getSkillId(), skillEntry);
						}
					}
					break;
				}
			}
			
			_critRate = homunculus.getTemplate().getCritRate();
			
			final SkillHolder skillEntry = new SkillHolder(homunculus.getTemplate().getBasicSkillId(), homunculus.getTemplate().getBasicSkillLevel());
			final SkillHolder tempSkillEntry = _skills.get(skillEntry.getSkillId());
			if ((tempSkillEntry == null) || (tempSkillEntry.getSkillLevel() < skillEntry.getSkillLevel()))
			{
				_skills.put(skillEntry.getSkillId(), skillEntry);
			}
		}
		
		for (SkillHolder skillEntry : _skills.values())
		{
			_owner.addSkill(skillEntry.getSkill(), false);
		}
		
		if (!_skills.isEmpty())
		{
			updateSkillList = true;
		}
		
		if (send)
		{
			_owner.broadcastUserInfo();
		}
		
		return updateSkillList;
	}
	
	/**
	 * Retrieves the current health points of the {@code HomunculusList}.<br>
	 * This value represents the total HP available.
	 * @return The current health points as an {@code int}.
	 */
	public int getHp()
	{
		return _hp;
	}
	
	/**
	 * Retrieves the attack power of the homunculus list.<br>
	 * This value represents the total attack stat.
	 * @return The current {@code int} value for attack.
	 */
	public int getAtk()
	{
		return _atk;
	}
	
	/**
	 * Retrieves the defense value of the homunculus list.<br>
	 * This value represents the total defense stat.
	 * @return The current {@code int} defense value.
	 */
	public int getDef()
	{
		return _def;
	}
	
	/**
	 * Retrieves the critical hit rate of the homunculus list.<br>
	 * This value represents the current percentage chance to land a critical strike.
	 * @return The current {@code int} value of the critical hit rate.
	 */
	public int getCritRate()
	{
		return _critRate;
	}
	
	/**
	 * Returns a string representation of the {@code HomunculusList}.<br>
	 * This includes the name of the owner.
	 * @return A string containing the list details.
	 */
	@Override
	public String toString()
	{
		return "HomunculusList[owner=" + _owner.getName() + "]";
	}
	
	/**
	 * Retrieves the currently active {@code Homunculus} from the list.<br>
	 * It searches through all available homunculi to find one where {@code isActive()} is true.<br>
	 * Returns {@code null} if no active homunculus is found.
	 * @return The first active {@code Homunculus} object or {@code null}.
	 */
	public Homunculus getActiveHomunculus()
	{
		for (Homunculus homunculus : _homunculusList)
		{
			if (homunculus.isActive())
			{
				return homunculus;
			}
		}
		
		return null;
	}
}
