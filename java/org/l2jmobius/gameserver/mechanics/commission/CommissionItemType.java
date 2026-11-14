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

import java.util.HashMap;
import java.util.Map;

/**
 * Defines the types of items that can be used or required for commissions.<br>
 * This enum helps categorize different item behaviors within the commission system.
 * @author NosBit, Mobius
 */
public enum CommissionItemType
{
	// Weapon
	ONE_HAND_SWORD(1),
	ONE_HAND_MAGIC_SWORD(2),
	DAGGER(3),
	RAPIER(4),
	TWO_HAND_SWORD(5),
	ANCIENT_SWORD(6),
	DUALSWORD(7),
	DUAL_DAGGER(8),
	BLUNT_WEAPON(9),
	ONE_HAND_MAGIC_BLUNT_WEAPON(10),
	TWO_HAND_BLUNT_WEAPON(11),
	TWO_HAND_MAGIC_BLUNT_WEAPON(12),
	DUAL_BLUNT_WEAPON(13),
	BOW(14),
	CROSSBOW(15),
	FIST_WEAPON(16),
	SPEAR(17),
	OTHER_WEAPON(18),
	// Armor
	HELMET(19),
	ARMOR_TOP(20),
	ARMOR_PANTS(21),
	FULL_BODY(22),
	GLOVES(23),
	FEET(24),
	SHIELD(25),
	SIGIL(26),
	UNDERWEAR(27),
	CLOAK(28),
	// Accessory
	RING(29),
	EARRING(30),
	NECKLACE(31),
	BELT(32),
	BRACELET(33),
	AGATHION(62),
	HAIR_ACCESSORY(34),
	BROOCH_JEWEL(63),
	ARTIFACT(64),
	// Supplies
	POTION(35),
	SCROLL_ENCHANT_WEAPON(36),
	SCROLL_ENCHANT_ARMOR(37),
	SCROLL_OTHER(38),
	SOULSHOT(39),
	SPIRITSHOT(40),
	OTHER_SUPPLIES(41),
	// Pet Goods
	PET_EQUIPMENT(42),
	PET_SUPPLIES(43),
	// Misc.
	CRYSTAL(44),
	RECIPE(45),
	MAJOR_CRAFTING_INGREDIENTS(46),
	LIFE_STONE(47),
	SOUL_CRYSTAL(48),
	ATTRIBUTE_STONE(49),
	WEAPON_ENCHANT_STONE(50),
	ARMOR_ENCHANT_STONE(51),
	SPELLBOOK(52),
	GEMSTONE(53),
	POUCH(54),
	PIN(55),
	MAGIC_RUNE_CLIP(56),
	MAGIC_ORNAMENT(57),
	DYES(58),
	OTHER_ITEM(59);
	
	private final int _clientId;
	
	/**
	 * Creates a new {@code CommissionItemType} with a specific ID.<br>
	 * This constructor assigns the provided integer to the internal client identifier.
	 * @param clientId The unique identification number for the item type.
	 */
	CommissionItemType(int clientId)
	{
		_clientId = clientId;
	}
	
	/**
	 * Retrieves the unique identifier for this item type.<br>
	 * This value is used to identify the item in the game client.
	 * @return The {@code int} ID of the commission item type.
	 */
	public int getClientId()
	{
		return _clientId;
	}
	
	private static final Map<Integer, CommissionItemType> BY_CLIENT_ID = new HashMap<>(values().length);
	static
	{
		for (CommissionItemType value : values())
		{
			BY_CLIENT_ID.put(value.getClientId(), value);
		}
	}
	
	/**
	 * Retrieves a {@code CommissionItemType} based on its unique ID.<br>
	 * This method searches the internal mapping for the provided {@code clientId}.
	 * @param clientId The unique identifier of the commission item.
	 * @return The corresponding {@code CommissionItemType} or {@code null} if not found.
	 */
	public static CommissionItemType findByClientId(int clientId)
	{
		return BY_CLIENT_ID.get(clientId);
	}
}
