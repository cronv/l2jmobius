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
import java.util.List;

import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class acts as a data container for clan mastery information.<br>
 * It stores the progression and requirements associated with {@code Skill} masteries for clans.
 * @author Mobius
 */
public class ClanMasteryHolder
{
	private final int _id;
	private final List<Skill> _skills = new ArrayList<>();
	private final int _clanLevel;
	private final int _clanReputation;
	private final int _previousMastery;
	private final int _previousMasteryAlt;
	
	/**
	 * Creates a new {@code ClanMasteryHolder} instance.<br>
	 * This constructor initializes the clan data and its associated skills.
	 * @param id The unique identifier for this holder.
	 * @param skill1 The first required {@code Skill}.
	 * @param skill2 The second optional {@code Skill}.
	 * @param skill3 The third optional {@code Skill}.
	 * @param skill4 The fourth optional {@code Skill}.
	 * @param clanLevel The current level of the clan.
	 * @param clanReputation The reputation score of the clan.
	 * @param previousMastery The primary previous mastery value.
	 * @param previousMasteryAlt The alternative previous mastery value.
	 */
	public ClanMasteryHolder(int id, Skill skill1, Skill skill2, Skill skill3, Skill skill4, int clanLevel, int clanReputation, int previousMastery, int previousMasteryAlt)
	{
		_id = id;
		_clanLevel = clanLevel;
		_clanReputation = clanReputation;
		_previousMastery = previousMastery;
		_previousMasteryAlt = previousMasteryAlt;
		_skills.add(skill1);
		if (skill2 != null)
		{
			_skills.add(skill2);
		}
		
		if (skill3 != null)
		{
			_skills.add(skill3);
		}
		
		if (skill4 != null)
		{
			_skills.add(skill4);
		}
	}
	
	/**
	 * Retrieves the unique identifier for this {@code ClanMasteryHolder}.<br>
	 * This value is used to distinguish between different mastery records.
	 * @return The unique integer ID of the holder.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the list of skills associated with this clan mastery.
	 * @return a {@code List} containing all {@code Skill} objects.
	 */
	public List<Skill> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Retrieves the current level of the clan.<br>
	 * This value represents the progression status of the clan.
	 * @return The current {@code int} level of the clan.
	 */
	public int getClanLevel()
	{
		return _clanLevel;
	}
	
	/**
	 * Retrieves the current reputation of the clan.<br>
	 * This value represents the total points earned by the clan.
	 * @return The current {@code int} reputation value.
	 */
	public int getClanReputation()
	{
		return _clanReputation;
	}
	
	/**
	 * Retrieves the previous mastery value.<br>
	 * This value is stored in the {@code ClanMasteryHolder}.
	 * @return The integer value of the previous mastery.
	 */
	public int getPreviousMastery()
	{
		return _previousMastery;
	}
	
	/**
	 * Retrieves the alternative previous mastery value.<br>
	 * This value is stored within the {@code ClanMasteryHolder}.
	 * @return The integer value of the previous mastery alternative.
	 */
	public int getPreviousMasteryAlt()
	{
		return _previousMasteryAlt;
	}
}
