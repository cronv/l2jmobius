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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class stores the configuration settings for a player's auto-use features.<br>
 * It manages data such as active skills, items, and their respective cooldowns.
 * @author Mobius
 */
public class AutoUseSettingsHolder
{
	private final Collection<Integer> _autoSupplyItems = ConcurrentHashMap.newKeySet();
	private final Collection<Integer> _autoActions = ConcurrentHashMap.newKeySet();
	private final Collection<Integer> _autoBuffs = ConcurrentHashMap.newKeySet();
	private final List<Integer> _autoSkills = new CopyOnWriteArrayList<>();
	private final AtomicInteger _autoPotionItem = new AtomicInteger();
	private int _skillIndex = 0;
	
	/**
	 * Creates a new instance of {@code AutoUseSettingsHolder}.<br>
	 * This initializes the default settings for automatic actions.
	 */
	public AutoUseSettingsHolder()
	{
	}
	
	/**
	 * Retrieves the collection of items configured for auto-supply.<br>
	 * This returns a set of item IDs that the system should automatically provide.
	 * @return A {@code Collection<Integer>} containing the auto-supply item IDs.
	 */
	public Collection<Integer> getAutoSupplyItems()
	{
		return _autoSupplyItems;
	}
	
	/**
	 * Retrieves the collection of automated actions.<br>
	 * This returns all action IDs currently set for automatic execution.
	 * @return a {@code Collection<Integer>} containing the auto action IDs.
	 */
	public Collection<Integer> getAutoActions()
	{
		return _autoActions;
	}
	
	/**
	 * Retrieves the collection of active auto-buff IDs.<br>
	 * This method returns all buffs currently set for automatic use.
	 * @return a {@code Collection<Integer>} containing the auto-buff IDs.
	 */
	public Collection<Integer> getAutoBuffs()
	{
		return _autoBuffs;
	}
	
	/**
	 * Retrieves the list of skills configured for automatic use.<br>
	 * This method returns a {@code List<Integer>} containing all skill IDs.
	 * @return A {@code List<Integer>} of auto-use skill IDs.
	 */
	public List<Integer> getAutoSkills()
	{
		return _autoSkills;
	}
	
	/**
	 * Retrieves the ID of the item used for automatic potion consumption.<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return The unique identifier of the auto-potion item.
	 */
	public int getAutoPotionItem()
	{
		return _autoPotionItem.get();
	}
	
	/**
	 * Sets the item ID for automatic potion use.<br>
	 * This updates the {@code _autoPotionItem} value.
	 * @param itemId The unique identifier of the potion item.
	 */
	public void setAutoPotionItem(int itemId)
	{
		_autoPotionItem.set(itemId);
	}
	
	/**
	 * Checks if a specific skill is set for automatic use.<br>
	 * This method returns {@code true} if the ID exists in either the auto skills or auto buffs lists.
	 * @param skillId The unique identifier of the skill to check.
	 * @return {@code true} if the skill is automated, otherwise {@code false}.
	 */
	public boolean isAutoSkill(int skillId)
	{
		return _autoSkills.contains(skillId) || _autoBuffs.contains(skillId);
	}
	
	/**
	 * Retrieves the next {@code Integer} ID from the auto-skill list.<br>
	 * It resets the index to {@code 0} if it exceeds the list size.<br>
	 * If an error occurs, it calls {@code resetSkillOrder}.
	 * @return The next skill ID or {@code Integer.MIN_VALUE} if no skills are found.
	 */
	public Integer getNextSkillId()
	{
		if (_skillIndex >= _autoSkills.size())
		{
			_skillIndex = 0;
		}
		
		Integer skillId = Integer.MIN_VALUE;
		try
		{
			skillId = _autoSkills.get(_skillIndex);
		}
		catch (Exception e)
		{
			resetSkillOrder();
		}
		
		return skillId;
	}
	
	/**
	 * Increases the current index of the skill order.<br>
	 * This method moves to the next skill in the sequence.
	 */
	public void incrementSkillOrder()
	{
		_skillIndex++;
	}
	
	/**
	 * Resets the current skill index to {@code 0}.<br>
	 * This method restarts the sequence of skills used in auto-use.
	 */
	public void resetSkillOrder()
	{
		_skillIndex = 0;
	}
	
	/**
	 * Checks if the holder is empty.<br>
	 * It returns {@code true} if all auto-use collections and the potion item are empty.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if there are no settings configured, {@code false} otherwise.
	 */
	public boolean isEmpty()
	{
		return _autoSupplyItems.isEmpty() && (_autoPotionItem.get() == 0) && _autoSkills.isEmpty() && _autoBuffs.isEmpty() && _autoActions.isEmpty();
	}
}
