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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.entity.actor.Creature;

/**
 * This class holds aggro-related information for a single aggressor against an NPC.<br>
 * It tracks independent counters for {@code Hate} and {@code Damage}.<br>
 * Values are capped to prevent uncontrolled growth and the class is designed to be lightweight and mutable.
 * @author BazookaRpm
 */
public class AggroInfo
{
	/**
	 * Upper cap applied to accumulated hate/damage.
	 */
	private static final long LIMIT = 1000000000000000L;
	
	/**
	 * The aggressor (attacker) this aggro entry belongs to.
	 */
	private final Creature _aggressor;
	
	/**
	 * Total accumulated damage credited to the aggressor.
	 */
	private long _damageTotal;
	
	/**
	 * Total accumulated hate credited to the aggressor.
	 */
	private long _hateTotal;
	
	/**
	 * Creates a new {@code AggroInfo} instance for a specific attacker.<br>
	 * This initializes the hate and damage totals to {@code 0}.
	 * @param aggressor The {@code Creature} that is attacking.
	 */
	public AggroInfo(Creature aggressor)
	{
		_aggressor = aggressor;
		_damageTotal = 0;
		_hateTotal = 0;
	}
	
	/**
	 * Retrieves the creature that initiated the attack.<br>
	 * This method returns the {@code Creature} associated with this aggro entry.
	 * @return the {@code Creature} who is attacking.
	 */
	public Creature getAttacker()
	{
		return _aggressor;
	}
	
	/**
	 * Retrieves the total damage accumulated by the aggressor.<br>
	 * This value is used for contribution tracking.
	 * @return the current {@code long} value of {@code _damageTotal}.
	 */
	public long getDamage()
	{
		return _damageTotal;
	}
	
	/**
	 * Increases the total damage for this aggro entry.<br>
	 * This method handles positive and negative values.<br>
	 * The total is capped at {@code LIMIT}.
	 * @param value the amount of damage to add
	 */
	public void addDamage(long value)
	{
		if (value == 0)
		{
			return;
		}
		
		if (value > 0)
		{
			final long current = _damageTotal;
			_damageTotal = (current >= (LIMIT - value)) ? LIMIT : (current + value);
			return;
		}
		
		_damageTotal += value;
	}
	
	/**
	 * Retrieves the total accumulated hate for this aggressor.<br>
	 * This value is used to determine target priority during threat calculations.
	 * @return the current {@code long} value of {@code _hateTotal}.
	 */
	public long getHate()
	{
		return _hateTotal;
	}
	
	/**
	 * Increases the total hate value for this aggressor.<br>
	 * This method handles both positive and negative increments.<br>
	 * The total value is capped at {@code LIMIT}.
	 * @param value the amount of hate to add
	 */
	public void addHate(long value)
	{
		if (value == 0)
		{
			return;
		}
		
		if (value > 0)
		{
			final long current = _hateTotal;
			_hateTotal = (current >= (LIMIT - value)) ? LIMIT : (current + value);
			return;
		}
		
		_hateTotal += value;
	}
	
	/**
	 * Resets the accumulated hate value to {@code 0}.<br>
	 * This method clears the threat history for the current aggressor.
	 */
	public void stopHate()
	{
		_hateTotal = 0;
	}
	
	/**
	 * Validates the hate value based on the current state of the attacker and owner.<br>
	 * This method resets {@code _hateTotal} to {@code 0} if the attacker is null, dead, or not spawned.<br>
	 * It also resets the value if the {@code owner} is not in the surrounding region of the attacker.
	 * @param owner The creature being targeted by the aggressor.
	 * @return The current hate total if all conditions are met, otherwise {@code 0}.
	 */
	public long checkHate(Creature owner)
	{
		final Creature attacker = _aggressor;
		if ((attacker == null) || (owner == null))
		{
			_hateTotal = 0;
			return 0;
		}
		
		if (attacker.isAlikeDead() && !(attacker.isPlayer() && attacker.asPlayer().isFakeDeathSeenBy(owner)))
		{
			_hateTotal = 0;
			return 0;
		}
		
		if (!attacker.isSpawned())
		{
			_hateTotal = 0;
			return 0;
		}
		
		if (!owner.isInSurroundingRegion(attacker))
		{
			_hateTotal = 0;
		}
		
		return _hateTotal;
	}
	
	/**
	 * Returns a hash code value for this object.<br>
	 * This method uses the {@code _aggressor} ID to generate the result.<br>
	 * It returns {@code 0} if the aggressor is {@code null}.
	 * @return The calculated hash code as an {@code int}.
	 */
	@Override
	public int hashCode()
	{
		final Creature c = _aggressor;
		return (c != null) ? c.getObjectId() : 0;
	}
	
	/**
	 * Compares this object with another object for equality.<br>
	 * Two {@code AggroInfo} objects are considered equal if they refer to the same aggressor.<br>
	 * This method returns {@code true} if both objects are the same instance or have the same attacker.<br>
	 * It returns {@code false} if the other object is {@code null}, not an instance of {@code AggroInfo}, or has a different attacker.
	 * @param other The object to compare this instance against.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object other)
	{
		if (other == this)
		{
			return true;
		}
		
		if (!(other instanceof AggroInfo))
		{
			return false;
		}
		
		return ((AggroInfo) other)._aggressor == _aggressor;
	}
	
	/**
	 * Returns a string representation of the {@code AggroInfo} object.<br>
	 * This includes the attacker, hate total, and damage total.
	 * @return A string describing the current state of this aggro entry.
	 */
	@Override
	public String toString()
	{
		final StringBuilder sb = new StringBuilder(96);
		sb.append("AggroInfo [attacker=");
		sb.append(_aggressor);
		sb.append(", hate=");
		sb.append(_hateTotal);
		sb.append(", damage=");
		sb.append(_damageTotal);
		sb.append(']');
		return sb.toString();
	}
}
