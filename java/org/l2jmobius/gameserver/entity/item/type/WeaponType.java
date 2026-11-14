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
package org.l2jmobius.gameserver.entity.item.type;

import org.l2jmobius.gameserver.mechanics.stats.TraitType;

/**
 * This enum defines the different categories of weapons available in the game.<br>
 * It is used to identify and categorize various weapon types for mechanics and logic.
 * @author mkizub
 */
public enum WeaponType implements ItemType
{
	NONE(TraitType.NONE),
	SWORD(TraitType.SWORD),
	BLUNT(TraitType.BLUNT),
	DAGGER(TraitType.DAGGER),
	POLE(TraitType.POLE),
	DUALFIST(TraitType.DUALFIST),
	BOW(TraitType.BOW),
	ETC(TraitType.ETC),
	DUAL(TraitType.DUAL),
	FIST(TraitType.FIST), // 0 items with that type
	FISHINGROD(TraitType.NONE),
	RAPIER(TraitType.RAPIER),
	CROSSBOW(TraitType.CROSSBOW),
	ANCIENTSWORD(TraitType.ANCIENTSWORD),
	FLAG(TraitType.NONE), // 0 items with that type
	DUALDAGGER(TraitType.DUALDAGGER),
	OWNTHING(TraitType.NONE), // 0 items with that type
	TWOHANDCROSSBOW(TraitType.TWOHANDCROSSBOW),
	DUALBLUNT(TraitType.DUALBLUNT);
	
	private final int _mask;
	private final TraitType _traitType;
	
	/**
	 * Creates a new {@code WeaponType} instance.<br>
	 * This constructor initializes the internal mask and the associated {@code TraitType}.
	 * @param traitType The {@code TraitType} assigned to this weapon type.
	 */
	WeaponType(TraitType traitType)
	{
		_mask = 1 << ordinal();
		_traitType = traitType;
	}
	
	/**
	 * Returns the bitmask associated with this {@code WeaponType}.<br>
	 * This value is used for internal logic checks.
	 * @return The integer mask value.
	 */
	@Override
	public int mask()
	{
		return _mask;
	}
	
	/**
	 * Retrieves the {@code TraitType} associated with this weapon type.<br>
	 * This value determines how the item interacts with specific game mechanics.
	 * @return The {@code TraitType} of this weapon.
	 */
	public TraitType getTraitType()
	{
		return _traitType;
	}
	
	/**
	 * Checks if the weapon type is considered a ranged weapon.<br>
	 * It returns {@code true} for bows and crossbows.
	 * @return {@code true} if it is a ranged weapon, {@code false} otherwise.
	 */
	public boolean isRanged()
	{
		return (this == BOW) || (this == CROSSBOW) || (this == TWOHANDCROSSBOW);
	}
	
	/**
	 * Checks if the weapon type is a crossbow.<br>
	 * This includes both one-handed and two-handed variants.
	 * @return {@code true} if the item is a crossbow, {@code false} otherwise.
	 */
	public boolean isCrossbow()
	{
		return (this == CROSSBOW) || (this == TWOHANDCROSSBOW);
	}
	
	/**
	 * Checks if the weapon type is a dual-wielding type.<br>
	 * This includes types like {@code DUALFIST}, {@code DUAL}, {@code DUALDAGGER}, and {@code DUALBLUNT}.
	 * @return {@code true} if the weapon is a dual type, {@code false} otherwise.
	 */
	public boolean isDual()
	{
		return (this == DUALFIST) || (this == DUAL) || (this == DUALDAGGER) || (this == DUALBLUNT);
	}
}
