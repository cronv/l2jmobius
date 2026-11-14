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
package org.l2jmobius.gameserver.entity.item.appearance;

import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class acts as a container for an entity's visual appearance data.<br>
 * It stores information regarding how items are displayed on different {@code BodyPart} locations.<br>
 * It helps manage the visual representation of equipment and cosmetics.
 * @author Sdw
 */
public class AppearanceHolder
{
	private final int _visualId;
	private final WeaponType _weaponType;
	private final ArmorType _armorType;
	private final AppearanceHandType _handType;
	private final AppearanceMagicType _magicType;
	private final AppearanceTargetType _targetType;
	private final BodyPart _bodyPart;
	
	/**
	 * Creates a new {@code AppearanceHolder} instance.<br>
	 * This constructor populates the visual properties from a {@code StatSet}.<br>
	 * It extracts values for IDs, types, and body parts.
	 * @param set The {@code StatSet} containing the appearance data.
	 */
	public AppearanceHolder(StatSet set)
	{
		_visualId = set.getInt("id", 0);
		_weaponType = set.getEnum("weaponType", WeaponType.class, WeaponType.NONE);
		_armorType = set.getEnum("armorType", ArmorType.class, ArmorType.NONE);
		_handType = set.getEnum("handType", AppearanceHandType.class, AppearanceHandType.NONE);
		_magicType = set.getEnum("magicType", AppearanceMagicType.class, AppearanceMagicType.NONE);
		_targetType = set.getEnum("targetType", AppearanceTargetType.class, AppearanceTargetType.NONE);
		_bodyPart = BodyPart.fromName(set.getString("bodyPart", "none"));
	}
	
	/**
	 * Retrieves the weapon type associated with this appearance.<br>
	 * This value determines how the item is categorized as a weapon.
	 * @return the {@code WeaponType} of the appearance.
	 */
	public WeaponType getWeaponType()
	{
		return _weaponType;
	}
	
	/**
	 * Retrieves the armor type associated with this appearance.<br>
	 * This value determines how the item is categorized as armor.
	 * @return the {@code ArmorType} of the appearance.
	 */
	public ArmorType getArmorType()
	{
		return _armorType;
	}
	
	/**
	 * Retrieves the hand type associated with this appearance.<br>
	 * This value determines how the item is held or positioned in the character's hand.
	 * @return the {@code AppearanceHandType} of this holder.
	 */
	public AppearanceHandType getHandType()
	{
		return _handType;
	}
	
	/**
	 * Retrieves the magic type associated with this appearance.<br>
	 * This value determines how the item behaves in a magical context.
	 * @return the {@code AppearanceMagicType} of the holder.
	 */
	public AppearanceMagicType getMagicType()
	{
		return _magicType;
	}
	
	/**
	 * Retrieves the target type for this appearance.<br>
	 * This identifies which part of the entity the appearance applies to.
	 * @return the {@code AppearanceTargetType} associated with this holder.
	 */
	public AppearanceTargetType getTargetType()
	{
		return _targetType;
	}
	
	/**
	 * Retrieves the body part associated with this appearance.<br>
	 * This method returns the {@code BodyPart} value stored in the holder.
	 * @return The {@code BodyPart} of the appearance.
	 */
	public BodyPart getBodyPart()
	{
		return _bodyPart;
	}
	
	/**
	 * Retrieves the unique visual identifier for this appearance.<br>
	 * This ID is used to determine how the item looks in the game world.
	 * @return The {@code int} value of the visual ID.
	 */
	public int getVisualId()
	{
		return _visualId;
	}
}
