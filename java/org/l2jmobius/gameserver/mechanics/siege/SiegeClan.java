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
package org.l2jmobius.gameserver.mechanics.siege;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Npc;

/**
 * Represents a clan participating in siege warfare.<br>
 * This class manages the state and data for a specific group during a siege event.
 */
public class SiegeClan
{
	private int _clanId = 0;
	private final Set<Npc> _flags = ConcurrentHashMap.newKeySet();
	private SiegeClanType _type;
	
	/**
	 * Creates a new instance of {@code SiegeClan}.<br>
	 * This constructor initializes the clan with a specific ID and type.
	 * @param clanId The unique identifier for the clan.
	 * @param type The {@code SiegeClanType} assigned to this clan.
	 */
	public SiegeClan(int clanId, SiegeClanType type)
	{
		_clanId = clanId;
		_type = type;
	}
	
	/**
	 * Returns the total number of flags currently assigned to this clan.<br>
	 * This count includes all {@code Npc} objects stored in the internal flag set.
	 * @return The size of the flags collection as an {@code int}.
	 */
	public int getNumFlags()
	{
		return _flags.size();
	}
	
	/**
	 * Adds a new {@code Npc} to the clan's flag collection.<br>
	 * This method updates the internal set of flags.
	 * @param flag The {@code Npc} object to be added as a flag.
	 */
	public void addFlag(Npc flag)
	{
		_flags.add(flag);
	}
	
	/**
	 * Removes a specific {@code Npc} from the clan flags.<br>
	 * This method deletes the {@code Npc} entity if it exists.<br>
	 * It returns {@code true} if the flag was successfully removed.<br>
	 * It returns {@code false} if the provided {@code flag} is {@code null}.
	 * @param flag The {@code Npc} to be removed from the flags.
	 * @return {@code true} if the removal was successful, otherwise {@code false}.
	 */
	public boolean removeFlag(Npc flag)
	{
		if (flag == null)
		{
			return false;
		}
		
		flag.deleteMe();
		
		return _flags.remove(flag);
	}
	
	/**
	 * Removes all flags associated with this {@code SiegeClan}.<br>
	 * It iterates through the internal set of {@code Npc} objects.<br>
	 * Each flag is removed using the {@code removeFlag} method.
	 */
	public void removeFlags()
	{
		for (Npc flag : _flags)
		{
			removeFlag(flag);
		}
	}
	
	/**
	 * Retrieves the unique identifier for this clan.<br>
	 * This value is assigned when the {@code SiegeClan} is created.
	 * @return The {@code int} ID of the clan.
	 */
	public int getClanId()
	{
		return _clanId;
	}
	
	/**
	 * Retrieves the collection of flags associated with this siege clan.<br>
	 * The returned {@code Set} contains all {@code Npc} objects currently acting as flags.
	 * @return A {@code Set} of {@code Npc} objects representing the flags.
	 */
	public Set<Npc> getFlag()
	{
		return _flags;
	}
	
	/**
	 * Retrieves the type of the siege clan.<br>
	 * This method returns the {@code SiegeClanType} associated with this instance.
	 * @return the current {@code SiegeClanType}
	 */
	public SiegeClanType getType()
	{
		return _type;
	}
	
	/**
	 * Updates the type of the siege clan.<br>
	 * This method sets the {@code _type} field to the provided value.
	 * @param setType The new {@code SiegeClanType} to assign.
	 */
	public void setType(SiegeClanType setType)
	{
		_type = setType;
	}
}
