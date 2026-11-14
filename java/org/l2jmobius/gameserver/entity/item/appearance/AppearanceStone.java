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

import java.time.Duration;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a stone used to modify the visual appearance of an entity.<br>
 * It stores data regarding which {@code BodyPart} is affected and what specific changes are applied.
 * @author UnAfraid
 */
public class AppearanceStone
{
	private final int _id;
	private final int _cost;
	private final int _visualId;
	private final long _lifeTime;
	private final AppearanceType _type;
	private final WeaponType _weaponType;
	private final ArmorType _armorType;
	private final AppearanceHandType _handType;
	private final AppearanceMagicType _magicType;
	private Set<CrystalType> _crystalTypes;
	private Set<AppearanceTargetType> _targetTypes;
	private Set<BodyPart> _bodyParts;
	private Set<Race> _races;
	private Set<Race> _racesNot;
	private Set<AppearanceHolder> _allVisualIds;
	
	/**
	 * Creates a new {@code AppearanceStone} instance using the provided data.<br>
	 * This constructor initializes all stone properties from a {@code StatSet}.<br>
	 * It handles default values for types, costs, and life times.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	public AppearanceStone(StatSet set)
	{
		_id = set.getInt("id");
		_visualId = set.getInt("visualId", 0);
		_cost = set.getInt("cost", 0);
		_lifeTime = set.getDuration("lifeTime", Duration.ofSeconds(0)).toMillis();
		_type = set.getEnum("type", AppearanceType.class, AppearanceType.NONE);
		_weaponType = set.getEnum("weaponType", WeaponType.class, WeaponType.NONE);
		_armorType = set.getEnum("armorType", ArmorType.class, ArmorType.NONE);
		_handType = set.getEnum("handType", AppearanceHandType.class, AppearanceHandType.NONE);
		_magicType = set.getEnum("magicType", AppearanceMagicType.class, AppearanceMagicType.NONE);
		
		final AppearanceTargetType targetType = set.getEnum("targetType", AppearanceTargetType.class, AppearanceTargetType.NONE);
		if (targetType != AppearanceTargetType.NONE)
		{
			addTargetType(targetType);
		}
		
		// No grade items cannot change appearance, because client doesn't have No-Grade restoration stones.
		final CrystalType crystalType = set.getEnum("grade", CrystalType.class, null);
		
		// If no crystal type is defined, we must add all defaults.
		if (crystalType == null)
		{
			switch (targetType)
			{
				case ACCESSORY:
				case ALL:
				{
					addCrystalType(CrystalType.NONE);
					// Fallthrough.
				}
				case WEAPON:
				case ARMOR:
				{
					for (CrystalType cryType : CrystalType.values())
					{
						if ((cryType != CrystalType.NONE) && (cryType != CrystalType.EVENT))
						{
							addCrystalType(cryType);
						}
					}
					break;
				}
			}
		}
		else
		{
			addCrystalType(crystalType);
		}
		
		final BodyPart bodyPart = BodyPart.fromName(set.getString("bodyPart", "none"));
		if (bodyPart != BodyPart.NONE)
		{
			addBodyPart(bodyPart);
		}
		
		final Race race = set.getEnum("race", Race.class, Race.NONE);
		if (race != Race.NONE)
		{
			addRace(race);
		}
		
		final Race raceNot = set.getEnum("raceNot", Race.class, Race.NONE);
		if (raceNot != Race.NONE)
		{
			addRaceNot(raceNot);
		}
	}
	
	/**
	 * Retrieves the unique identifier for this {@code AppearanceStone}.
	 * @return The {@code int} ID of the stone.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the unique visual identifier for this appearance stone.<br>
	 * This value is used to identify how the item looks in the game world.
	 * @return The {@code int} value of the visual ID.
	 */
	public int getVisualId()
	{
		return _visualId;
	}
	
	/**
	 * Retrieves the cost of this {@code AppearanceStone}.
	 * @return The cost as an {@code int}.
	 */
	public int getCost()
	{
		return _cost;
	}
	
	/**
	 * Retrieves the total duration of the appearance effect.<br>
	 * The value is returned in milliseconds.
	 * @return the life time as a {@code long}.
	 */
	public long getLifeTime()
	{
		return _lifeTime;
	}
	
	/**
	 * Retrieves the appearance type of this stone.
	 * @return the {@code AppearanceType} associated with this stone.
	 */
	public AppearanceType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the weapon type associated with this appearance stone.
	 * @return the {@code WeaponType} of the stone.
	 */
	public WeaponType getWeaponType()
	{
		return _weaponType;
	}
	
	/**
	 * Retrieves the armor type associated with this appearance stone.
	 * @return the {@code ArmorType} of the stone.
	 */
	public ArmorType getArmorType()
	{
		return _armorType;
	}
	
	/**
	 * Retrieves the hand type associated with this appearance stone.
	 * @return the {@code AppearanceHandType} of the stone.
	 */
	public AppearanceHandType getHandType()
	{
		return _handType;
	}
	
	/**
	 * Retrieves the magic type associated with this appearance stone.
	 * @return the {@code AppearanceMagicType} of the stone.
	 */
	public AppearanceMagicType getMagicType()
	{
		return _magicType;
	}
	
	/**
	 * Adds a new {@code CrystalType} to the set of allowed crystal types.<br>
	 * This method initializes the internal set if it is currently {@code null}.
	 * @param type The {@code CrystalType} to add to this appearance stone.
	 */
	public void addCrystalType(CrystalType type)
	{
		if (_crystalTypes == null)
		{
			_crystalTypes = EnumSet.noneOf(CrystalType.class);
		}
		
		_crystalTypes.add(type);
	}
	
	/**
	 * Retrieves the set of crystal types associated with this stone.<br>
	 * Returns an empty {@code Set} if no types are defined.
	 * @return a {@code Set} of {@code CrystalType} objects.
	 */
	public Set<CrystalType> getCrystalTypes()
	{
		return _crystalTypes != null ? _crystalTypes : Collections.emptySet();
	}
	
	/**
	 * Adds a new target type to the collection of valid targets.<br>
	 * This method ensures that the {@code _targetTypes} set is initialized before adding.
	 * @param type The {@code AppearanceTargetType} to add.
	 */
	public void addTargetType(AppearanceTargetType type)
	{
		if (_targetTypes == null)
		{
			_targetTypes = EnumSet.noneOf(AppearanceTargetType.class);
		}
		
		_targetTypes.add(type);
	}
	
	/**
	 * Retrieves the set of target types associated with this stone.<br>
	 * Returns an empty {@code Set} if no target types are defined.
	 * @return a {@code Set} of {@code AppearanceTargetType} values.
	 */
	public Set<AppearanceTargetType> getTargetTypes()
	{
		return _targetTypes != null ? _targetTypes : Collections.emptySet();
	}
	
	/**
	 * Adds a specific body part to the set of allowed parts.<br>
	 * This method ensures that the {@code _bodyParts} collection is initialized before adding.
	 * @param bodyPart The {@code BodyPart} to be added.
	 */
	public void addBodyPart(BodyPart bodyPart)
	{
		if (_bodyParts == null)
		{
			_bodyParts = new HashSet<>();
		}
		
		_bodyParts.add(bodyPart);
	}
	
	/**
	 * Adds a new visual identifier to the collection.<br>
	 * This method ensures that the {@code _allVisualIds} set is initialized before adding.
	 * @param appearanceHolder The {@code AppearanceHolder} object to add.
	 */
	public void addVisualId(AppearanceHolder appearanceHolder)
	{
		if (_allVisualIds == null)
		{
			_allVisualIds = new HashSet<>();
		}
		
		_allVisualIds.add(appearanceHolder);
	}
	
	/**
	 * Retrieves the set of visual identifiers associated with this stone.<br>
	 * Returns an empty {@code Set} if no visual IDs are present.
	 * @return a {@code Set} of {@code AppearanceHolder} objects.
	 */
	public Set<AppearanceHolder> getVisualIds()
	{
		return _allVisualIds != null ? _allVisualIds : Collections.emptySet();
	}
	
	/**
	 * Retrieves the set of body parts associated with this appearance stone.<br>
	 * Returns an empty {@code Set} if no body parts are defined.
	 * @return a {@code Set} of {@code BodyPart} objects.
	 */
	public Set<BodyPart> getBodyParts()
	{
		return _bodyParts != null ? _bodyParts : Collections.emptySet();
	}
	
	/**
	 * Adds a specific race to the allowed list.<br>
	 * This method updates the internal set of {@code Race} types.
	 * @param race The {@code Race} to add to the collection.
	 */
	public void addRace(Race race)
	{
		if (_races == null)
		{
			_races = EnumSet.noneOf(Race.class);
		}
		
		_races.add(race);
	}
	
	/**
	 * Retrieves the set of races associated with this appearance stone.<br>
	 * Returns an empty {@code Set} if no races are defined.
	 * @return a {@code Set} of {@code Race} objects.
	 */
	public Set<Race> getRaces()
	{
		return _races != null ? _races : Collections.emptySet();
	}
	
	/**
	 * Adds a race to the exclusion list.<br>
	 * This prevents the appearance from being applied to specific races.
	 * @param race The {@code Race} type to exclude.
	 */
	public void addRaceNot(Race race)
	{
		if (_racesNot == null)
		{
			_racesNot = EnumSet.noneOf(Race.class);
		}
		
		_racesNot.add(race);
	}
	
	/**
	 * Retrieves the set of races that are excluded from this appearance.<br>
	 * Returns an empty {@code Set} if no races are excluded.
	 * @return a {@code Set} of {@code Race} types that are not allowed.
	 */
	public Set<Race> getRacesNot()
	{
		return _racesNot != null ? _racesNot : Collections.emptySet();
	}
	
	/**
	 * Validates if a {@code Player} can apply this appearance to a specific {@code Item}.<br>
	 * It checks various requirements such as item type, race restrictions, and body parts.<br>
	 * If any condition fails, it sends an appropriate system message to the player.
	 * @param player The player attempting to modify the item.
	 * @param targetItem The item being checked for appearance compatibility.
	 * @return {@code true} if all conditions are met; {@code false} otherwise.
	 */
	public boolean checkConditions(Player player, Item targetItem)
	{
		if (targetItem == null)
		{
			return false;
		}
		
		if (getTargetTypes().isEmpty())
		{
			return false;
		}
		
		if (targetItem.isEquipped() && (getRacesNot().contains(player.getRace()) || (!getRaces().isEmpty() && !getRaces().contains(player.getRace()))))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_MODIFY_AN_EQUIPPED_ITEM_INTO_THE_APPEARANCE_OF_AN_UNEQUIPPABLE_ITEM_PLEASE_CHECK_RACE_GENDER_RESTRICTIONS_YOU_CAN_MODIFY_THE_APPEARANCE_IF_YOU_UNEQUIP_THE_ITEM);
			return false;
		}
		
		switch (_type)
		{
			case RESTORE:
			{
				if (targetItem.getVisualId() == 0)
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_RESTORE_ITEMS_THAT_HAVE_NOT_BEEN_MODIFIED);
					return false;
				}
				
				final boolean isWeapon = targetItem.isWeapon();
				final boolean isArmor = targetItem.isArmor();
				final boolean isEtcItem = targetItem.isEtcItem();
				final BodyPart bodyPart = targetItem.getTemplate().getBodyPart();
				final boolean isHair = (bodyPart == BodyPart.HAIR) || (bodyPart == BodyPart.HAIR2) || (bodyPart == BodyPart.HAIRALL);
				
				if ((isWeapon && !getTargetTypes().contains(AppearanceTargetType.WEAPON)) || (isArmor && !getTargetTypes().contains(AppearanceTargetType.ARMOR) && !isHair) || (isEtcItem && !getTargetTypes().contains(AppearanceTargetType.ACCESSORY)))
				{
					player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
					return false;
				}
				
				if (isHair && !getTargetTypes().contains(AppearanceTargetType.ACCESSORY))
				{
					player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
					return false;
				}
				break;
			}
			default:
			{
				// Seems like in retail item with already changed appearance, can be changed again without being restored.
				
				final AppearanceTargetType targetType = getTargetTypes().stream().findFirst().get();
				switch (targetType)
				{
					case NONE:
					{
						return false;
					}
					case WEAPON:
					{
						if (!targetItem.isWeapon())
						{
							player.sendPacket(SystemMessageId.WEAPONS_ONLY);
							return false;
						}
						
						if (targetItem.getTemplate().getCrystalType() == CrystalType.NONE)
						{
							player.sendPacket(SystemMessageId.YOU_CANNOT_MODIFY_OR_RESTORE_NO_GRADE_ITEMS);
							return false;
						}
						break;
					}
					case ARMOR:
					{
						if (!targetItem.isArmor())
						{
							player.sendPacket(SystemMessageId.ARMOR_ONLY);
							return false;
						}
						
						if (targetItem.getTemplate().getCrystalType() == CrystalType.NONE)
						{
							player.sendPacket(SystemMessageId.YOU_CANNOT_MODIFY_OR_RESTORE_NO_GRADE_ITEMS);
							return false;
						}
						break;
					}
					case ACCESSORY:
					{
						final BodyPart bodyPart = targetItem.getTemplate().getBodyPart();
						if ((bodyPart != BodyPart.HAIR) && (bodyPart != BodyPart.HAIR2) && (bodyPart != BodyPart.HAIRALL))
						{
							player.sendPacket(SystemMessageId.HEAD_ACCESSORIES_ONLY);
							return false;
						}
						break;
					}
					case ALL:
					{
						if (!getCrystalTypes().isEmpty() && !getCrystalTypes().contains(targetItem.getTemplate().getCrystalType()))
						{
							player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
							return false;
						}
						
						if (findVisualChange(targetItem) == null)
						{
							player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
							return false;
						}
						
						return true;
					}
				}
				break;
			}
		}
		
		if (!getCrystalTypes().isEmpty() && !getCrystalTypes().contains(targetItem.getTemplate().getCrystalType()))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
			return false;
		}
		
		final BodyPart bodyPart = targetItem.getTemplate().getBodyPart();
		if (targetItem.isArmor() && !getBodyParts().isEmpty() && !getBodyParts().contains(bodyPart))
		{
			player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
			return false;
		}
		
		if (_weaponType != WeaponType.NONE)
		{
			final boolean isWeapon = targetItem.isWeapon();
			final ItemType itemType = targetItem.getItemType();
			if (!isWeapon || (itemType != _weaponType))
			{
				if (_weaponType != WeaponType.CROSSBOW)
				{
					player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_EXTRACTED);
					return false;
				}
				else if ((itemType != WeaponType.CROSSBOW) && (itemType != WeaponType.TWOHANDCROSSBOW))
				{
					player.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_EXTRACTED);
					return false;
				}
			}
			
			switch (_handType)
			{
				case ONE_HANDED:
				{
					if (bodyPart != BodyPart.R_HAND)
					{
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						return false;
					}
					break;
				}
				case TWO_HANDED:
				{
					if (bodyPart != BodyPart.LR_HAND)
					{
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						return false;
					}
					break;
				}
			}
			
			switch (_magicType)
			{
				case MAGICAL:
				{
					if (!targetItem.getTemplate().isMagicWeapon())
					{
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						return false;
					}
					break;
				}
				case PHYISICAL:
				{
					if (targetItem.getTemplate().isMagicWeapon())
					{
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						return false;
					}
				}
			}
		}
		
		if (_armorType != ArmorType.NONE)
		{
			switch (_armorType)
			{
				case SHIELD:
				{
					if (!targetItem.isArmor() || (targetItem.getItemType() != ArmorType.SHIELD))
					{
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						return false;
					}
					break;
				}
				case SIGIL:
				{
					if (!targetItem.isArmor() || (targetItem.getItemType() != ArmorType.SIGIL))
					{
						player.sendPacket(SystemMessageId.THIS_ITEM_DOES_NOT_MEET_REQUIREMENTS);
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Finds a matching {@code AppearanceHolder} for the given {@code Item}.<br>
	 * This method checks if the item matches the requirements of any visual IDs.<br>
	 * It validates body parts, weapon types, hand types, and magic properties.
	 * @param targetItem The {@code Item} to check for a visual change.
	 * @return The first matching {@code AppearanceHolder} or {@code null} if no match is found.
	 */
	public AppearanceHolder findVisualChange(Item targetItem)
	{
		for (AppearanceHolder holder : _allVisualIds)
		{
			final boolean isArmor = targetItem.isArmor();
			final BodyPart bodyPart = holder.getBodyPart();
			final BodyPart templateBodyPart = targetItem.getTemplate().getBodyPart();
			
			if (isArmor && (bodyPart != BodyPart.NONE) && (templateBodyPart != bodyPart))
			{
				continue;
			}
			
			final WeaponType weaponType = holder.getWeaponType();
			final ItemType itemType = targetItem.getItemType();
			if (weaponType != WeaponType.NONE)
			{
				final boolean isWeapon = targetItem.isWeapon();
				
				if (!isWeapon || (itemType != weaponType))
				{
					if (weaponType != WeaponType.CROSSBOW)
					{
						continue;
					}
					else if ((itemType != WeaponType.CROSSBOW) && (itemType != WeaponType.TWOHANDCROSSBOW))
					{
						continue;
					}
				}
				
				switch (holder.getHandType())
				{
					case ONE_HANDED:
					{
						if (templateBodyPart != BodyPart.R_HAND)
						{
							continue;
						}
						break;
					}
					case TWO_HANDED:
					{
						if (templateBodyPart != BodyPart.LR_HAND)
						{
							continue;
						}
						break;
					}
				}
				
				switch (holder.getMagicType())
				{
					case MAGICAL:
					{
						if (!targetItem.getTemplate().isMagicWeapon())
						{
							continue;
						}
						break;
					}
					case PHYISICAL:
					{
						if (targetItem.getTemplate().isMagicWeapon())
						{
							continue;
						}
						break;
					}
				}
			}
			
			final ArmorType armorType = holder.getArmorType();
			if (armorType != ArmorType.NONE)
			{
				switch (armorType)
				{
					case SHIELD:
					{
						if (!isArmor || (itemType != ArmorType.SHIELD))
						{
							continue;
						}
						break;
					}
					case SIGIL:
					{
						if (!isArmor || (itemType != ArmorType.SIGIL))
						{
							continue;
						}
						break;
					}
				}
			}
			
			return holder;
		}
		
		return null;
	}
}
