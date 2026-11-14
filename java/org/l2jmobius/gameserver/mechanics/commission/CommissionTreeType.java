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
package org.l2jmobius.gameserver.mechanics.commission;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Defines the different types of commission trees available in the game.<br>
 * This enum is used to categorize and identify specific commission structures.
 * @author NosBit, Mobius
 */
public enum CommissionTreeType
{
	WEAPON(0, CommissionItemType.ONE_HAND_SWORD, CommissionItemType.ONE_HAND_MAGIC_SWORD, CommissionItemType.DAGGER, CommissionItemType.RAPIER, CommissionItemType.TWO_HAND_SWORD, CommissionItemType.ANCIENT_SWORD, CommissionItemType.DUALSWORD, CommissionItemType.DUAL_DAGGER, CommissionItemType.BLUNT_WEAPON, CommissionItemType.ONE_HAND_MAGIC_BLUNT_WEAPON, CommissionItemType.TWO_HAND_BLUNT_WEAPON, CommissionItemType.TWO_HAND_MAGIC_BLUNT_WEAPON, CommissionItemType.DUAL_BLUNT_WEAPON, CommissionItemType.BOW, CommissionItemType.CROSSBOW, CommissionItemType.FIST_WEAPON, CommissionItemType.SPEAR, CommissionItemType.OTHER_WEAPON),
	ARMOR(1, CommissionItemType.HELMET, CommissionItemType.ARMOR_TOP, CommissionItemType.ARMOR_PANTS, CommissionItemType.FULL_BODY, CommissionItemType.GLOVES, CommissionItemType.FEET, CommissionItemType.SHIELD, CommissionItemType.SIGIL, CommissionItemType.UNDERWEAR, CommissionItemType.CLOAK),
	ACCESSORY(2, CommissionItemType.RING, CommissionItemType.EARRING, CommissionItemType.NECKLACE, CommissionItemType.BELT, CommissionItemType.BRACELET, CommissionItemType.AGATHION, CommissionItemType.HAIR_ACCESSORY, CommissionItemType.BROOCH_JEWEL, CommissionItemType.ARTIFACT),
	SUPPLIES(3, CommissionItemType.POTION, CommissionItemType.SCROLL_ENCHANT_WEAPON, CommissionItemType.SCROLL_ENCHANT_ARMOR, CommissionItemType.SCROLL_OTHER, CommissionItemType.SOULSHOT, CommissionItemType.SPIRITSHOT, CommissionItemType.OTHER_SUPPLIES),
	PET_GOODS(4, CommissionItemType.PET_EQUIPMENT, CommissionItemType.PET_SUPPLIES),
	MISC(5, CommissionItemType.CRYSTAL, CommissionItemType.RECIPE, CommissionItemType.MAJOR_CRAFTING_INGREDIENTS, CommissionItemType.LIFE_STONE, CommissionItemType.SOUL_CRYSTAL, CommissionItemType.ATTRIBUTE_STONE, CommissionItemType.WEAPON_ENCHANT_STONE, CommissionItemType.ARMOR_ENCHANT_STONE, CommissionItemType.SPELLBOOK, CommissionItemType.GEMSTONE, CommissionItemType.POUCH, CommissionItemType.PIN, CommissionItemType.MAGIC_RUNE_CLIP, CommissionItemType.MAGIC_ORNAMENT, CommissionItemType.DYES, CommissionItemType.OTHER_ITEM);
	
	private final int _clientId;
	private final Set<CommissionItemType> _commissionItemTypes;
	
	/**
	 * Creates a new {@code CommissionTreeType} instance.<br>
	 * This constructor maps specific item types to a unique client ID.
	 * @param clientId The unique identifier for the commission tree.
	 * @param commissionItemTypes The list of {@code CommissionItemType} values associated with this tree.
	 */
	CommissionTreeType(int clientId, CommissionItemType... commissionItemTypes)
	{
		_clientId = clientId;
		_commissionItemTypes = Set.copyOf(Arrays.asList(commissionItemTypes));
	}
	
	/**
	 * Retrieves the unique identifier for this commission tree type.<br>
	 * This value is used to distinguish between different categories of items.
	 * @return The {@code int} ID associated with this {@code CommissionTreeType}.
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	/**
	 * Retrieves the set of item types associated with this commission tree.<br>
	 * This method returns all {@code CommissionItemType} values defined for this category.
	 * @return a {@code Set} containing the {@code CommissionItemType} objects.
	 */
	public Set<CommissionItemType> getCommissionItemTypes()
	{
		return _commissionItemTypes;
	}
	
	private static final Map<Integer, CommissionTreeType> BY_CLIENT_ID = new HashMap<>(values().length);
	static
	{
		for (CommissionTreeType value : values())
		{
			BY_CLIENT_ID.put(value.getClientId(), value);
		}
	}
	
	/**
	 * Finds a {@code CommissionTreeType} based on its unique ID.<br>
	 * This method searches the internal map for the provided {@code clientId}.
	 * @param clientId The unique identifier of the commission tree type.
	 * @return The matching {@code CommissionTreeType} or {@code null} if not found.
	 */
	public static CommissionTreeType findByClientId(int clientId)
	{
		return BY_CLIENT_ID.get(clientId);
	}
}
