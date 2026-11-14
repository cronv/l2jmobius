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
package org.l2jmobius.gameserver.entity.item.enums;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;

/**
 * Defines the various body parts of an entity.<br>
 * This enumeration is used to identify specific locations for equipment and effects on a character.
 * @author Mobius
 */
public enum BodyPart
{
	// Items with no body part.
	NONE(0x0000, Inventory.PAPERDOLL_TOTALSLOTS, "none"),
	
	// Basic body parts.
	UNDERWEAR(0x0001, Inventory.PAPERDOLL_UNDER, "underwear", "shirt"),
	R_EAR(0x0002, Inventory.PAPERDOLL_REAR, "rear", "rbracelet"),
	L_EAR(0x0004, Inventory.PAPERDOLL_LEAR, "lear"),
	LR_EAR(0x0006, -1, "rear;lear"), // Special case, no direct paperdoll slot.
	NECK(0x0008, Inventory.PAPERDOLL_NECK, "neck"),
	R_FINGER(0x0010, Inventory.PAPERDOLL_RFINGER, "rfinger"),
	L_FINGER(0x0020, Inventory.PAPERDOLL_LFINGER, "lfinger"),
	LR_FINGER(0x0030, -1, "rfinger;lfinger"), // Special case, no direct paperdoll slot.
	HEAD(0x0040, Inventory.PAPERDOLL_HEAD, "head"),
	R_HAND(0x0080, Inventory.PAPERDOLL_RHAND, "rhand"),
	L_HAND(0x0100, Inventory.PAPERDOLL_LHAND, "lhand"),
	GLOVES(0x0200, Inventory.PAPERDOLL_GLOVES, "gloves"),
	CHEST(0x0400, Inventory.PAPERDOLL_CHEST, "chest"),
	LEGS(0x0800, Inventory.PAPERDOLL_LEGS, "legs"),
	FEET(0x1000, Inventory.PAPERDOLL_FEET, "feet"),
	BACK(0x2000, Inventory.PAPERDOLL_CLOAK, "back"),
	LR_HAND(0x4000, -1, "lrhand"), // Maps to RHAND slot.
	FULL_ARMOR(0x8000, -1, "fullarmor", "onepiece"), // Maps to CHEST slot.
	
	// Extended body parts.
	HAIR(0x010000, Inventory.PAPERDOLL_HAIR, "hair"),
	ALLDRESS(0x020000, -1, "alldress"), // Maps to CHEST slot.
	HAIR2(0x040000, Inventory.PAPERDOLL_HAIR2, "hair2"),
	HAIRALL(0x080000, -1, "hairall", "dhair"), // Maps to HAIR slot.
	R_BRACELET(0x100000, Inventory.PAPERDOLL_RBRACELET, "rbracelet"),
	L_BRACELET(0x200000, Inventory.PAPERDOLL_LBRACELET, "lbracelet"),
	DECO(0x400000, Inventory.PAPERDOLL_DECO1, "deco1", "talisman"), // First talisman slot.
	BELT(0x10000000, Inventory.PAPERDOLL_BELT, "belt", "waist"),
	BROOCH(0x20000000, Inventory.PAPERDOLL_BROOCH, "brooch"),
	BROOCH_JEWEL(0x40000000, Inventory.PAPERDOLL_BROOCH_JEWEL1, "brooch_jewel"),
	
	// 64-bit (long) values.
	AGATHION(0x3000000000L, Inventory.PAPERDOLL_AGATHION1, "agathion"),
	ARTIFACT_BOOK(0x20000000000L, Inventory.PAPERDOLL_ARTIFACT_BOOK, "artifactbook"),
	ARTIFACT(0x40000000000L, Inventory.PAPERDOLL_ARTIFACT1, "artifact"),
	
	// Special slots for pets.
	WOLF(-100, -1, "wolf"),
	HATCHLING(-101, -1, "hatchling"),
	STRIDER(-102, -1, "strider"),
	BABYPET(-103, -1, "babypet"),
	GREATWOLF(-104, -1, "greatwolf");
	
	private final long _mask;
	private final int _paperdollSlot;
	private final String[] _names;
	
	private static final Map<Integer, BodyPart> BODY_PARTS_BY_PAPERDOLL_SLOT = new HashMap<>();
	private static final Map<Long, BodyPart> BODY_PARTS_BY_MASK = new HashMap<>();
	private static final Map<String, BodyPart> BODY_PARTS_BY_NAME = new HashMap<>();
	static
	{
		// Special slots for agathions.
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_AGATHION1, AGATHION);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_AGATHION2, AGATHION);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_AGATHION3, AGATHION);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_AGATHION4, AGATHION);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_AGATHION5, AGATHION);
		
		// Special slots for talismans.
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_DECO1, DECO);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_DECO2, DECO);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_DECO3, DECO);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_DECO4, DECO);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_DECO5, DECO);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_DECO6, DECO);
		
		// Special slots for brooch jewels.
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_BROOCH_JEWEL1, BROOCH_JEWEL);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_BROOCH_JEWEL2, BROOCH_JEWEL);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_BROOCH_JEWEL3, BROOCH_JEWEL);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_BROOCH_JEWEL4, BROOCH_JEWEL);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_BROOCH_JEWEL5, BROOCH_JEWEL);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_BROOCH_JEWEL6, BROOCH_JEWEL);
		
		// Special slots for artifacts.
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT1, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT2, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT3, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT4, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT5, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT6, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT7, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT8, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT9, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT10, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT11, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT12, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT13, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT14, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT15, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT16, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT17, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT18, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT19, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT20, ARTIFACT);
		BODY_PARTS_BY_PAPERDOLL_SLOT.put(Inventory.PAPERDOLL_ARTIFACT21, ARTIFACT);
		
		for (BodyPart bodyPart : values())
		{
			// Only map paperdoll slots that are valid.
			if ((bodyPart._paperdollSlot >= 0) && (bodyPart._paperdollSlot < Inventory.PAPERDOLL_TOTALSLOTS))
			{
				BODY_PARTS_BY_PAPERDOLL_SLOT.put(bodyPart._paperdollSlot, bodyPart);
			}
			
			// Map all bodypart names.
			for (String name : bodyPart._names)
			{
				BODY_PARTS_BY_NAME.put(name, bodyPart);
			}
			
			// Map the mask the client names when it takes an item off.
			if (bodyPart._mask > 0)
			{
				BODY_PARTS_BY_MASK.put(Long.valueOf(bodyPart._mask), bodyPart);
			}
		}
	}
	
	/**
	 * Constructs a new {@code BodyPart} enum constant.<br>
	 * This constructor initializes the internal properties of the body part.
	 * @param mask The bitmask value associated with this body part.
	 * @param paperdollSlot The inventory slot index for the paperdoll system.
	 * @param names A variable number of strings representing the display names.
	 */
	private BodyPart(long mask, int paperdollSlot, String... names)
	{
		_mask = mask;
		_paperdollSlot = paperdollSlot;
		_names = names;
	}
	
	/**
	 * Retrieves the bitmask associated with this {@code BodyPart}.<br>
	 * This value is used to identify specific body parts in calculations.
	 * @return The {@code long} mask value.
	 */
	public long getMask()
	{
		return _mask;
	}
	
	/**
	 * Retrieves the paperdoll slot index for this body part.<br>
	 * This value is used to determine where an item appears on the character model.
	 * @return The integer ID of the paperdoll slot.
	 */
	public int getPaperdollSlot()
	{
		return _paperdollSlot;
	}
	
	/**
	 * Retrieves the correct paperdoll slot index for a given {@code BodyPart}.<br>
	 * This method handles special cases where multiple parts map to a single slot.
	 * @param bodyPart The {@code BodyPart} to look up.
	 * @return The integer index of the corresponding paperdoll slot.
	 */
	public static int getPaperdollIndex(BodyPart bodyPart)
	{
		switch (bodyPart) // Handle special cases.
		{
			case LR_HAND:
			{
				return Inventory.PAPERDOLL_RHAND;
			}
			case FULL_ARMOR:
			case ALLDRESS:
			{
				return Inventory.PAPERDOLL_CHEST;
			}
			case HAIRALL:
			{
				return Inventory.PAPERDOLL_HAIR;
			}
			default:
			{
				return bodyPart.getPaperdollSlot();
			}
		}
	}
	
	/**
	 * Converts a paperdoll slot index into its corresponding {@code BodyPart}.<br>
	 * This method looks up the body part based on the provided integer slot.
	 * @param slot The inventory slot index to check.
	 * @return The matching {@code BodyPart} or {@code null} if no match is found.
	 */
	public static BodyPart fromPaperdollSlot(int slot)
	{
		return BODY_PARTS_BY_PAPERDOLL_SLOT.get(slot);
	}
	
	/**
	 * Retrieves the body part carrying a given mask.
	 * @param mask The mask of the body part.
	 * @return The {@code BodyPart} of the given mask, or {@code null} when no body part carries it.
	 */
	public static BodyPart fromMask(long mask)
	{
		return BODY_PARTS_BY_MASK.get(Long.valueOf(mask));
	}
	
	/**
	 * Converts an {@code Item} into its corresponding {@code BodyPart}.<br>
	 * This method determines the body part based on the item's location slot.<br>
	 * It returns {@code NONE} if the provided {@code item} is {@code null}.
	 * @param item The {@code Item} to check for a body part.
	 * @return The {@code BodyPart} associated with the item, or {@code NONE}.
	 */
	public static BodyPart fromItem(Item item)
	{
		if (item == null)
		{
			return NONE;
		}
		
		final int slot = item.getLocationSlot();
		switch (slot)
		{
			case Inventory.PAPERDOLL_CHEST:
			{
				return item.getTemplate().getBodyPart(); // Special case for chest slots.
			}
			default:
			{
				return BODY_PARTS_BY_PAPERDOLL_SLOT.get(slot);
			}
		}
	}
	
	/**
	 * Converts a string name into its corresponding {@code BodyPart}.<br>
	 * This method looks up the name in the internal registry.
	 * @param name The name of the body part to look up.
	 * @return The matching {@code BodyPart} or {@code null} if not found.
	 */
	public static BodyPart fromName(String name)
	{
		return BODY_PARTS_BY_NAME.get(name);
	}
}
