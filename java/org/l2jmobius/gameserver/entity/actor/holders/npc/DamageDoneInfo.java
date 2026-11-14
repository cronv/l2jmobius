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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class stores information regarding damage dealt by an NPC to a {@code Player}.<br>
 * It is used to track and manage combat statistics during interactions.
 * @author xban1x
 */
public class DamageDoneInfo
{
	private final Player _attacker;
	private long _damage = 0;
	
	/**
	 * Creates a new {@code DamageDoneInfo} object.<br>
	 * This constructor sets the initial attacker for the damage record.
	 * @param attacker The {@code Player} who dealt the damage.
	 */
	public DamageDoneInfo(Player attacker)
	{
		_attacker = attacker;
	}
	
	/**
	 * Retrieves the {@code Player} who performed the attack.<br>
	 * This method returns the attacker associated with this damage information.
	 * @return the {@code Player} who initiated the attack
	 */
	public Player getAttacker()
	{
		return _attacker;
	}
	
	/**
	 * Adds a specific amount of damage to the total.<br>
	 * This updates the internal {@code _damage} field.
	 * @param damage The amount of damage to add.
	 */
	public void addDamage(long damage)
	{
		_damage += damage;
	}
	
	/**
	 * Retrieves the total amount of damage dealt.<br>
	 * This value is stored as a {@code long}.
	 * @return The current damage value.
	 */
	public long getDamage()
	{
		return _damage;
	}
	
	/**
	 * Compares this object with another object for equality.<br>
	 * Two objects are considered equal if they are the same instance.<br>
	 * They are also equal if both are of type {@code DamageDoneInfo} and have the same attacker.
	 * @param obj The object to compare this instance against.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		return (this == obj) || ((obj instanceof DamageDoneInfo) && (((DamageDoneInfo) obj).getAttacker() == _attacker));
	}
	
	/**
	 * Returns a hash code value for this object.<br>
	 * This value is used to identify the object in hash-based collections.<br>
	 * It is calculated based on the {@code Player} object's unique ID.
	 * @return The integer hash code of this instance.
	 */
	@Override
	public int hashCode()
	{
		return _attacker.getObjectId();
	}
}
