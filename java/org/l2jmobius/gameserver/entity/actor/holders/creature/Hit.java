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
package org.l2jmobius.gameserver.entity.actor.holders.creature;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttackType;

/**
 * Represents a single instance of damage or healing dealt to an entity.<br>
 * It stores details such as the amount, type, and source of the {@code Hit}.
 * @author UnAfraid
 */
public class Hit
{
	private final WorldObject _target;
	private final int _targetId;
	private final int _damage;
	private final int _ssGrade;
	private int _flags = 0;
	
	/**
	 * Creates a new {@code Hit} object to represent an attack result.<br>
	 * This constructor initializes the target and damage values.<br>
	 * It also sets various hit flags based on the provided boolean conditions.
	 * @param target The {@code WorldObject} being attacked.
	 * @param damage The amount of damage dealt to the target.
	 * @param miss Set to {@code true} if the attack failed to hit.
	 * @param crit Set to {@code true} if the attack was a critical strike.
	 * @param shld The shield value used to determine if the hit was blocked.
	 * @param soulshot Set to {@code true} if a soulshot skill was used.
	 * @param ssGrade The grade of the soulshot used.
	 */
	public Hit(WorldObject target, int damage, boolean miss, boolean crit, byte shld, boolean soulshot, int ssGrade)
	{
		_target = target;
		_targetId = target.getObjectId();
		_damage = damage;
		_ssGrade = ssGrade;
		
		if (miss)
		{
			addMask(AttackType.MISSED);
			return;
		}
		
		if (crit)
		{
			addMask(AttackType.CRITICAL);
		}
		
		if (soulshot)
		{
			addMask(AttackType.SHOT_USED);
		}
		
		if ((target.isCreature() && target.asCreature().isHpBlocked()) || (shld > 0))
		{
			addMask(AttackType.BLOCKED);
		}
	}
	
	/**
	 * Updates the internal flags of this {@code Hit}.<br>
	 * It applies a bitmask based on the provided {@code AttackType}.
	 * @param type The {@code AttackType} to apply to the mask.
	 */
	private void addMask(AttackType type)
	{
		_flags |= type.getMask();
	}
	
	/**
	 * Retrieves the {@code WorldObject} that was hit.<br>
	 * This method returns the target of the current {@code Hit}.
	 * @return The {@code WorldObject} representing the target.
	 */
	public WorldObject getTarget()
	{
		return _target;
	}
	
	/**
	 * Retrieves the unique identifier of the target.<br>
	 * This value corresponds to the {@code _targetId} field.
	 * @return The integer ID of the target.
	 */
	public int getTargetId()
	{
		return _targetId;
	}
	
	/**
	 * Retrieves the amount of damage dealt by this {@code Hit}.<br>
	 * This value represents the final damage calculation.
	 * @return The integer value of the damage.
	 */
	public int getDamage()
	{
		return _damage;
	}
	
	/**
	 * Retrieves the current status flags for this {@code Hit}.<br>
	 * These flags represent various properties of the attack.
	 * @return The integer value representing all active flags.
	 */
	public int getFlags()
	{
		return _flags;
	}
	
	/**
	 * Retrieves the soulshot grade of the hit.<br>
	 * This value represents the specific level of the {@code ssGrade} property.
	 * @return The integer value of the soulshot grade.
	 */
	public int getGrade()
	{
		return _ssGrade;
	}
	
	/**
	 * Checks if the attack resulted in a miss.<br>
	 * It evaluates the internal flags of this {@code Hit}.
	 * @return {@code true} if the attack missed, {@code false} otherwise.
	 */
	public boolean isMiss()
	{
		return (AttackType.MISSED.getMask() & _flags) != 0;
	}
	
	/**
	 * Checks if the hit was a critical strike.<br>
	 * It evaluates the internal flags of this {@code Hit}.
	 * @return {@code true} if the hit is critical, {@code false} otherwise.
	 */
	public boolean isCritical()
	{
		return (AttackType.CRITICAL.getMask() & _flags) != 0;
	}
	
	/**
	 * Checks if a shot was used during this hit.<br>
	 * This method looks at the internal flags of the {@code Hit} object.
	 * @return {@code true} if a shot was used, {@code false} otherwise.
	 */
	public boolean isShotUsed()
	{
		return (AttackType.SHOT_USED.getMask() & _flags) != 0;
	}
	
	/**
	 * Checks if the hit was blocked by a shield.<br>
	 * This method evaluates the internal {@code _flags} field.
	 * @return {@code true} if the hit is blocked, {@code false} otherwise.
	 */
	public boolean isBlocked()
	{
		return (AttackType.BLOCKED.getMask() & _flags) != 0;
	}
}
