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
package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Stores skill and item reuse data so it survives a player relog.
 * @author Yesod, Zoey76, Mobius
 */
public class TimeStamp
{
	/** Item or skill ID. */
	private final int _id1;
	/** Item object ID or skill level. */
	private final int _id2;
	/** Skill level. */
	private final int _id3;
	/** Item or skill reuse time. */
	private final long _reuse;
	/** Time stamp. */
	private volatile long _stamp;
	/** Shared reuse group. */
	private final int _group;
	
	/**
	 * Creates a reuse timestamp for a skill.
	 * @param skill The {@code Skill} to track.
	 * @param reuse The reuse delay.
	 * @param systime The stamp time, or {@code <= 0} to use the current time plus the reuse delay.
	 */
	public TimeStamp(Skill skill, long reuse, long systime)
	{
		_id1 = skill.getId();
		_id2 = skill.getLevel();
		_id3 = skill.getSubLevel();
		_reuse = reuse;
		_stamp = systime > 0 ? systime : reuse != 0 ? System.currentTimeMillis() + reuse : 0;
		_group = skill.getReuseDelayGroup();
	}
	
	/**
	 * Creates a reuse timestamp for an item.
	 * @param item The {@code Item} to track.
	 * @param reuse The reuse delay.
	 * @param systime The stamp time, or {@code <= 0} to use the current time plus the reuse delay.
	 */
	public TimeStamp(Item item, long reuse, long systime)
	{
		_id1 = item.getId();
		_id2 = item.getObjectId();
		_id3 = 0;
		_reuse = reuse;
		_stamp = systime > 0 ? systime : reuse != 0 ? System.currentTimeMillis() + reuse : 0;
		_group = item.getSharedReuseGroup();
	}
	
	/**
	 * Retrieves the reuse timestamp.
	 * @return The stamp as a {@code long}.
	 */
	public long getStamp()
	{
		return _stamp;
	}
	
	/**
	 * Retrieves the item id.
	 * @return The item id as an {@code int}.
	 */
	public int getItemId()
	{
		return _id1;
	}
	
	/**
	 * Retrieves the item object id.
	 * @return The object id as an {@code int}.
	 */
	public int getItemObjectId()
	{
		return _id2;
	}
	
	/**
	 * Retrieves the skill id.
	 * @return The skill id as an {@code int}.
	 */
	public int getSkillId()
	{
		return _id1;
	}
	
	/**
	 * Retrieves the skill level.
	 * @return The skill level as an {@code int}.
	 */
	public int getSkillLevel()
	{
		return _id2;
	}
	
	/**
	 * Retrieves the skill sub-level.
	 * @return The sub-level as an {@code int}.
	 */
	public int getSkillSubLevel()
	{
		return _id3;
	}
	
	/**
	 * Retrieves the reuse delay.
	 * @return The reuse delay as a {@code long}.
	 */
	public long getReuse()
	{
		return _reuse;
	}
	
	/**
	 * Retrieves the shared reuse group id.
	 * @return The group id as an {@code int}.
	 */
	public int getSharedReuseGroup()
	{
		return _group;
	}
	
	/**
	 * Retrieves the time left before reuse, clearing the stamp once it elapses.
	 * @return The remaining milliseconds, or {@code 0} if already elapsed.
	 */
	public long getRemaining()
	{
		if (_stamp == 0)
		{
			return 0;
		}
		
		final long remainingTime = _stamp - System.currentTimeMillis();
		if (remainingTime <= 0)
		{
			_stamp = 0;
			return 0;
		}
		
		return remainingTime;
	}
	
	/**
	 * Indicates whether the reuse time has not yet elapsed, clearing the stamp once it has.
	 * @return {@code true} if still on cooldown, {@code false} otherwise.
	 */
	public boolean hasNotPassed()
	{
		if (_stamp == 0)
		{
			return false;
		}
		
		if (System.currentTimeMillis() >= _stamp)
		{
			_stamp = 0;
			return false;
		}
		
		return true;
	}
}
