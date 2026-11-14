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
package custom.FakePlayers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.data.xml.ArmorSetData;
import org.l2jmobius.gameserver.data.xml.CategoryData;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.Armor;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemGrade;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.commission.CommissionItemType;

/**
 * Picks random grade appropriate gear for fake players.<br>
 * Given an {@link ItemGrade} and a {@link PlayerClass}, it returns a coherent set of armor pieces plus a matching weapon and, when suitable, a shield.<br>
 * Armor type follows the weapon: mages wear {@link ArmorType#MAGIC} robe, dagger, bow and crossbow users wear {@link ArmorType#LIGHT} armor, everyone else wears {@link ArmorType#HEAVY} armor.<br>
 * Cursed weapons and any other {@code BANNED_ITEMS} are never handed out.<br>
 * Weapons respect race and class rules. Race: rapier, ancient sword and crossbows are Kamael only, so other races never receive them, while a {@code Race.KAMAEL} draws from those Kamael signature weapons; a {@code Race.ERTHEIA} always uses a magic two hand blunt staff and never melee, wearing robe. Class weapon rules are honored by the {@link #canEquip(PlayerClass, ItemTemplate)} check that mirrors {@code Player.canUseEquipment}, so a class only ever gets the weapons and shields it can actually wear.<br>
 * Armor selection anchors on a chest piece and pulls its real {@link ArmorSet} when one exists (via {@code ArmorSetData.getSets(int)}); any slot the set leaves empty is filled with another same grade piece.<br>
 * A one handed sword loadout also gets a shield, taken from the chest armor set when it defines one, otherwise a random same grade shield.
 * @author Mobius
 */
public class RandomEquipmentGenerator
{
	// Body slots we equip, one item per slot. FULL_ARMOR chests count as CHEST and also cover LEGS.
	private enum ArmorSlot
	{
		HEAD,
		CHEST,
		LEGS,
		GLOVES,
		FEET
	}
	
	// [grade] -> [armor type] -> [slot] -> item ids.
	private static final Map<ItemGrade, Map<ArmorType, Map<ArmorSlot, List<Integer>>>> ARMOR_POOLS = new EnumMap<>(ItemGrade.class);
	// [grade] -> [isMage] -> weapon ids usable by any race (excludes Kamael only types).
	private static final Map<ItemGrade, Map<Boolean, List<Integer>>> WEAPON_POOLS = new EnumMap<>(ItemGrade.class);
	// [grade] -> Kamael signature weapon ids (rapier, ancient sword, crossbows).
	private static final Map<ItemGrade, List<Integer>> KAMAEL_WEAPON_POOLS = new EnumMap<>(ItemGrade.class);
	// [grade] -> shield ids.
	private static final Map<ItemGrade, List<Integer>> SHIELD_POOLS = new EnumMap<>(ItemGrade.class);
	// Item ids never handed to a fake player (cursed weapons: Zariche, Akamanah, Kamex items).
	private static final Set<Integer> BANNED_ITEMS = Set.of(8190, 8689, 1295, 1296, 1297, 1298, 1299, 1300, 1301, 1302, 1303, 1304, 1305, 1306, 1307, 1308, 1309, 1310, 1311, 1312, 1313, 1314, 1315, 1316, 1317, 1318, 1319, 1320, 1321, 1322, 1323, 1324, 1325, 1326, 1327, 1328, 1329, 1330, 1331, 1332, 431, 459);
	private static volatile boolean _initialized = false;
	
	// Weapon type predicates for class specific weapon rules.
	private static final Predicate<Weapon> STAFF_WEAPON = weapon -> (weapon.getItemType() == WeaponType.BLUNT) && (weapon.getBodyPart() == BodyPart.LR_HAND);
	private static final Predicate<Weapon> FIST_WEAPON = weapon -> (weapon.getItemType() == WeaponType.FIST) || (weapon.getItemType() == WeaponType.DUALFIST);
	private static final Predicate<Weapon> TWO_HAND_SWORD = weapon -> (weapon.getItemType() == WeaponType.SWORD) && (weapon.getBodyPart() == BodyPart.LR_HAND);
	
	/**
	 * Builds the grade and type pools from the loaded item templates on first use.
	 */
	private static synchronized void initialize()
	{
		if (_initialized)
		{
			return;
		}
		
		final ItemData itemData = ItemData.getInstance();
		for (Armor armor : itemData.getAllArmors())
		{
			final ItemGrade grade = armor.getItemGrade();
			if ((grade == null) || isForbidden(armor) || BANNED_ITEMS.contains(armor.getId()))
			{
				continue;
			}
			
			final ArmorType type = armor.getItemType();
			if (type == ArmorType.SHIELD)
			{
				SHIELD_POOLS.computeIfAbsent(grade, _ -> new ArrayList<>()).add(armor.getId());
				continue;
			}
			
			if ((type != ArmorType.MAGIC) && (type != ArmorType.LIGHT) && (type != ArmorType.HEAVY))
			{
				continue; // Skip NONE, SIGIL and anything not a wearable body armor.
			}
			
			final ArmorSlot slot = toSlot(armor.getBodyPart());
			if (slot == null)
			{
				continue;
			}
			
			ARMOR_POOLS.computeIfAbsent(grade, _ -> new EnumMap<>(ArmorType.class)).computeIfAbsent(type, _ -> new EnumMap<>(ArmorSlot.class)).computeIfAbsent(slot, _ -> new ArrayList<>()).add(armor.getId());
		}
		
		for (Weapon weapon : itemData.getAllWeapons())
		{
			final ItemGrade grade = weapon.getItemGrade();
			if ((grade == null) || isForbidden(weapon) || BANNED_ITEMS.contains(weapon.getId()))
			{
				continue;
			}
			
			final WeaponType weaponType = weapon.getItemType();
			if (isKamaelWeapon(weaponType))
			{
				KAMAEL_WEAPON_POOLS.computeIfAbsent(grade, _ -> new ArrayList<>()).add(weapon.getId());
			}
			
			// Kamael weapon types are left out of the shared pool so other races never get them.
			if (!isKamaelWeapon(weaponType))
			{
				WEAPON_POOLS.computeIfAbsent(grade, _ -> new HashMap<>()).computeIfAbsent(weapon.isMagicWeapon(), _ -> new ArrayList<>()).add(weapon.getId());
			}
		}
		
		_initialized = true;
	}
	
	/**
	 * Generates a coherent set of armor item ids of the given grade for a mage or a fighter.<br>
	 * A chest is chosen first, its armor set fills as many slots as it can, and remaining slots are filled with other same grade pieces.
	 * @param grade the item grade to pull pieces from
	 * @param type the armor type to pull ({@link ArmorType#MAGIC}, {@link ArmorType#LIGHT} or {@link ArmorType#HEAVY})
	 * @return the armor piece item ids, possibly missing a slot if the grade has no piece for it
	 */
	public static List<Integer> generateArmor(ItemGrade grade, ArmorType type)
	{
		initialize();
		
		final List<Integer> pieces = new ArrayList<>();
		final Map<ArmorSlot, List<Integer>> pool = armorPool(grade, type);
		if (pool.isEmpty())
		{
			return pieces;
		}
		
		final Set<ArmorSlot> filled = EnumSet.noneOf(ArmorSlot.class);
		boolean chestIsFull = false;
		
		// Anchor on a chest whose armor set also covers the legs (or a full armor that covers them itself), so the top and bottom match.
		// A few random tries; if no complete set exists at this grade, fall back to any chest and let the gap fill dress the rest.
		final List<Integer> chestPool = pool.get(ArmorSlot.CHEST);
		Integer chestId = null;
		ArmorSet chestSet = null;
		if ((chestPool != null) && !chestPool.isEmpty())
		{
			final ItemData itemData = ItemData.getInstance();
			for (int i = 0; i < 12; i++)
			{
				final Integer candidate = chestPool.get(Rnd.get(chestPool.size()));
				if (chestId == null)
				{
					chestId = candidate; // Remember the first pick as a fallback.
				}
				
				if (itemData.getTemplate(candidate.intValue()).getBodyPart() == BodyPart.FULL_ARMOR)
				{
					chestId = candidate; // A full armor covers the legs by itself.
					break;
				}
				
				for (ArmorSet set : ArmorSetData.getInstance().getSets(candidate.intValue()))
				{
					if (setCoversLegs(set))
					{
						chestId = candidate;
						chestSet = set;
						break;
					}
				}
				if (chestSet != null)
				{
					break;
				}
			}
		}
		
		if (chestId != null)
		{
			pieces.add(chestId);
			filled.add(ArmorSlot.CHEST);
			chestIsFull = ItemData.getInstance().getTemplate(chestId.intValue()).getBodyPart() == BodyPart.FULL_ARMOR;
			
			// Wear the chosen legs covering set, otherwise any set the chest belongs to, then the gap fill dresses whatever the set left empty.
			if (chestSet == null)
			{
				final List<ArmorSet> sets = ArmorSetData.getInstance().getSets(chestId.intValue());
				if (!sets.isEmpty())
				{
					chestSet = sets.get(Rnd.get(sets.size()));
				}
			}
			if (chestSet != null)
			{
				addSetPieces(chestSet.getRequiredItems(), pieces, filled, chestIsFull);
				addSetPieces(chestSet.getOptionalItems(), pieces, filled, chestIsFull);
			}
		}
		
		// Fill any still empty slot with a random same grade piece.
		for (ArmorSlot slot : ArmorSlot.values())
		{
			if (filled.contains(slot) || ((slot == ArmorSlot.LEGS) && chestIsFull))
			{
				continue;
			}
			
			final Integer id = randomFrom(pool.get(slot));
			if (id != null)
			{
				pieces.add(id);
				filled.add(slot);
			}
		}
		
		return pieces;
	}
	
	/**
	 * Picks a random weapon id of the given grade that the class can actually equip.<br>
	 * A {@code Race.KAMAEL} receives one of its signature weapons (rapier, ancient sword, crossbow), falling back to a shared weapon only when the grade has no usable Kamael weapon.<br>
	 * A {@code Race.ERTHEIA} always receives a magic two hand blunt staff and never melee.<br>
	 * Any other race is never given a Kamael only weapon, and class weapon rules from {@link #canEquip(PlayerClass, ItemTemplate)} are always honored.
	 * @param grade the item grade to pull the weapon from
	 * @param playerClass the wielder class, providing race, mage flag and class restrictions
	 * @return a weapon item id, or {@code 0} if the grade has no matching weapon
	 */
	public static int generateWeapon(ItemGrade grade, PlayerClass playerClass)
	{
		initialize();
		
		if (playerClass.getRace() == Race.KAMAEL)
		{
			final int kamaelWeapon = pickWeapon(KAMAEL_WEAPON_POOLS.get(grade), playerClass, null);
			if (kamaelWeapon > 0)
			{
				return kamaelWeapon;
			}
			// No usable Kamael weapon at this grade, fall back to a shared weapon below.
		}
		
		final Map<Boolean, List<Integer>> pool = WEAPON_POOLS.get(grade);
		if (pool == null)
		{
			return 0;
		}
		
		// Some classes are limited to a specific weapon type.
		final boolean isMage = playerClass.isMage();
		boolean useMagePool = isMage;
		Predicate<Weapon> weaponFilter = null;
		if (CategoryData.getInstance().isInCategory(CategoryType.SHINE_MAKER_ALL_CLASS, playerClass.getId()))
		{
			useMagePool = false; // Two hand swords are physical, despite the Shine Maker mage flag.
			weaponFilter = TWO_HAND_SWORD;
		}
		else if (CategoryData.getInstance().isInCategory(CategoryType.WARG_ALL_CLASS, playerClass.getId()))
		{
			weaponFilter = FIST_WEAPON;
		}
		else if (playerClass.getRace() == Race.ERTHEIA)
		{
			useMagePool = true; // Ertheia use magic two hand blunt staves only, never melee.
			weaponFilter = STAFF_WEAPON;
		}
		
		return pickWeapon(pool.get(useMagePool), playerClass, weaponFilter);
	}
	
	/**
	 * Generates a full loadout: the race and class appropriate weapon of {@link #generateWeapon(ItemGrade, PlayerClass)}, armor that matches that weapon and, for a one handed sword, a shield the class can use.<br>
	 * The weapon is chosen first, then the armor type follows it: robe for mages, light for daggers, bows and crossbows, heavy otherwise ({@link #armorTypeForWeapon(int)}).
	 * @param grade the item grade to pull gear from
	 * @param playerClass the wearer class, providing race, mage flag and class restrictions
	 * @return every item id to equip, weapon and shield included when available
	 */
	public static List<Integer> generateEquipment(ItemGrade grade, PlayerClass playerClass)
	{
		// Pick the weapon first, then wear armor that matches it.
		final int weaponId = generateWeapon(grade, playerClass);
		final boolean kamael = playerClass.getRace() == Race.KAMAEL;
		final ArmorType armorType = kamael ? ArmorType.LIGHT : playerClass.isMage() ? ArmorType.MAGIC : armorTypeForWeapon(weaponId); // Kamael wear only light (leather) armor.
		List<Integer> items = generateArmor(grade, armorType);
		if (items.isEmpty() && (armorType != ArmorType.MAGIC) && !kamael)
		{
			// Fall back to the other fighter armor when the matching type has nothing at this grade (never onto Kamael, who are light only).
			items = generateArmor(grade, armorType == ArmorType.LIGHT ? ArmorType.HEAVY : ArmorType.LIGHT);
		}
		
		if (weaponId > 0)
		{
			items.add(Integer.valueOf(weaponId));
			
			// A one handed sword pairs with a shield, unless the class cannot use one.
			if (isOneHandedSword(weaponId) && !hasShield(items))
			{
				final int shieldId = pickShield(grade, items);
				if ((shieldId > 0) && canEquip(playerClass, ItemData.getInstance().getTemplate(shieldId)))
				{
					items.add(Integer.valueOf(shieldId));
				}
			}
		}
		
		return items;
	}
	
	/**
	 * Maps a weapon to the armor type its wielder wears: light for daggers, bows and crossbows, heavy otherwise.
	 * @param weaponId the weapon item id, or {@code 0}
	 * @return {@link ArmorType#LIGHT} or {@link ArmorType#HEAVY}
	 */
	private static ArmorType armorTypeForWeapon(int weaponId)
	{
		if ((weaponId > 0) && (ItemData.getInstance().getTemplate(weaponId) instanceof Weapon weapon))
		{
			if (weapon.isMagicWeapon())
			{
				return ArmorType.MAGIC; // Magic weapon wielders wear robe.
			}
			
			switch (weapon.getItemType())
			{
				case DAGGER:
				case DUALDAGGER:
				case BOW:
				case CROSSBOW:
				case TWOHANDCROSSBOW:
				case RAPIER:
				{
					return ArmorType.LIGHT;
				}
				default:
				{
					return ArmorType.HEAVY;
				}
			}
		}
		
		return ArmorType.HEAVY;
	}
	
	/**
	 * Checks whether a class may equip an item, mirroring {@code Player.canUseEquipment} without needing a live player.<br>
	 * It enforces class specific shield and weapon restrictions.
	 * @param playerClass the class to test
	 * @param item the item template to test
	 * @return {@code true} if the class can equip the item
	 */
	private static boolean canEquip(PlayerClass playerClass, ItemTemplate item)
	{
		if (item == null)
		{
			return false;
		}
		
		final int classId = playerClass.getId();
		final CategoryData categoryData = CategoryData.getInstance();
		if (item.isArmor())
		{
			// Some classes cannot use shields.
			if ((item.getItemType() == ArmorType.SHIELD) && (categoryData.isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, classId) || categoryData.isInCategory(CategoryType.SHINE_MAKER_ALL_CLASS, classId) || categoryData.isInCategory(CategoryType.SAMURAI_ALL_CLASS, classId)))
			{
				return false;
			}
		}
		else if (item.isWeapon())
		{
			// Allow equipping Sword Cursed - version Prelude of War 2019.
			final int itemId = item.getId();
			if ((itemId == 8190) || (itemId == 8689))
			{
				return true;
			}
			
			// Fishing rods are enabled for all players.
			final ItemType itemType = item.getItemType();
			if (itemType == WeaponType.FISHINGROD)
			{
				return item.isEquipable();
			}
			
			// Some classes can only equip one hand swords.
			if (categoryData.isInCategory(CategoryType.DEATH_KNIGHT_ALL_CLASS, classId))
			{
				if ((itemType != WeaponType.SWORD) || (item.getBodyPart() == BodyPart.LR_HAND))
				{
					return false;
				}
			}
			// Some classes can only equip two hand swords or bows.
			else if (categoryData.isInCategory(CategoryType.SHINE_MAKER_ALL_CLASS, classId))
			{
				if ((itemType != WeaponType.BOW) && ((itemType != WeaponType.SWORD) || (item.getBodyPart() != BodyPart.LR_HAND)))
				{
					return false;
				}
			}
			// Some classes can only equip swords.
			else if (categoryData.isInCategory(CategoryType.SAMURAI_ALL_CLASS, classId))
			{
				if (itemType != WeaponType.SWORD)
				{
					return false;
				}
			}
		}
		
		return item.isEquipable();
	}
	
	/**
	 * Picks a random weapon the class can equip from the candidate ids, optionally restricted to a weapon type filter.
	 * @param candidates the candidate weapon ids, may be {@code null}
	 * @param playerClass the wielder class
	 * @param filter an extra weapon filter for class specific weapon rules, or {@code null} for none
	 * @return an equippable weapon id, or {@code 0} if none qualifies
	 */
	private static int pickWeapon(List<Integer> candidates, PlayerClass playerClass, Predicate<Weapon> filter)
	{
		if ((candidates == null) || candidates.isEmpty())
		{
			return 0;
		}
		
		final ItemData itemData = ItemData.getInstance();
		// A few random tries for variety, then a full scan to honor class and race restrictions.
		for (int i = 0; i < 10; i++)
		{
			final int id = candidates.get(Rnd.get(candidates.size())).intValue();
			if (acceptsWeapon(itemData.getTemplate(id), playerClass, filter))
			{
				return id;
			}
		}
		
		for (Integer candidate : candidates)
		{
			final int id = candidate.intValue();
			if (acceptsWeapon(itemData.getTemplate(id), playerClass, filter))
			{
				return id;
			}
		}
		
		return 0;
	}
	
	/**
	 * Checks whether a weapon template passes both the class equip rules and the optional weapon filter.
	 * @param template the weapon template, may be {@code null}
	 * @param playerClass the wielder class
	 * @param filter an extra weapon filter, or {@code null} for none
	 * @return {@code true} if the weapon qualifies
	 */
	private static boolean acceptsWeapon(ItemTemplate template, PlayerClass playerClass, Predicate<Weapon> filter)
	{
		if (!canEquip(playerClass, template))
		{
			return false;
		}
		
		return (filter == null) || ((template instanceof Weapon weapon) && filter.test(weapon));
	}
	
	/**
	 * Adds set pieces to the loadout, one per still empty body slot.
	 * @param itemIds the set item ids to consider
	 * @param pieces the loadout being built
	 * @param filled the slots already taken
	 * @param chestIsFull {@code true} when the chest is a full armor and already covers the legs
	 */
	private static void addSetPieces(int[] itemIds, List<Integer> pieces, Set<ArmorSlot> filled, boolean chestIsFull)
	{
		final ItemData itemData = ItemData.getInstance();
		for (int itemId : itemIds)
		{
			final ItemTemplate template = itemData.getTemplate(itemId);
			if (template == null)
			{
				continue;
			}
			
			final ArmorSlot slot = toSlot(template.getBodyPart());
			if ((slot == null) || filled.contains(slot) || ((slot == ArmorSlot.LEGS) && chestIsFull))
			{
				continue;
			}
			
			pieces.add(Integer.valueOf(itemId));
			filled.add(slot);
		}
	}
	
	/**
	 * Checks whether an armor set provides a legs piece, so anchoring on it yields a matching top and bottom.
	 * @param set the armor set
	 * @return {@code true} if the set defines a legs item
	 */
	private static boolean setCoversLegs(ArmorSet set)
	{
		return setHasLegs(set.getRequiredItems()) || setHasLegs(set.getOptionalItems());
	}
	
	/**
	 * Checks whether any of the given set item ids is a legs piece.
	 * @param itemIds the set item ids to scan
	 * @return {@code true} if a legs item is present
	 */
	private static boolean setHasLegs(int[] itemIds)
	{
		final ItemData itemData = ItemData.getInstance();
		for (int itemId : itemIds)
		{
			final ItemTemplate template = itemData.getTemplate(itemId);
			if ((template != null) && (toSlot(template.getBodyPart()) == ArmorSlot.LEGS))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks whether the weapon is a one handed sword, which can be paired with a shield.
	 * @param weaponId the weapon item id
	 * @return {@code true} if the weapon is a one handed sword
	 */
	private static boolean isOneHandedSword(int weaponId)
	{
		return (ItemData.getInstance().getTemplate(weaponId) instanceof Weapon weapon) && (weapon.getBodyPart() == BodyPart.R_HAND) && (weapon.getItemType() == WeaponType.SWORD);
	}
	
	/**
	 * Checks whether the loadout already contains a shield.
	 * @param items the loadout item ids
	 * @return {@code true} if a shield is present
	 */
	private static boolean hasShield(List<Integer> items)
	{
		final ItemData itemData = ItemData.getInstance();
		for (int itemId : items)
		{
			if ((itemData.getTemplate(itemId) instanceof Armor armor) && (armor.getItemType() == ArmorType.SHIELD))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Picks a shield for the loadout, preferring one defined by the chest's armor set and falling back to a random same grade shield.
	 * @param grade the item grade for the random fallback
	 * @param items the loadout item ids, used to find the chest and its set
	 * @return a shield item id, or {@code 0} if none is available
	 */
	private static int pickShield(ItemGrade grade, List<Integer> items)
	{
		final ItemData itemData = ItemData.getInstance();
		for (int itemId : items)
		{
			final ItemTemplate template = itemData.getTemplate(itemId);
			if ((template == null) || (toSlot(template.getBodyPart()) != ArmorSlot.CHEST))
			{
				continue;
			}
			
			for (ArmorSet set : ArmorSetData.getInstance().getSets(itemId))
			{
				final int required = firstShield(set.getRequiredItems(), itemData);
				if (required > 0)
				{
					return required;
				}
				
				final int optional = firstShield(set.getOptionalItems(), itemData);
				if (optional > 0)
				{
					return optional;
				}
			}
			
			break; // Only one chest in a loadout.
		}
		
		final Integer id = randomFrom(SHIELD_POOLS.get(grade));
		return id == null ? 0 : id.intValue();
	}
	
	/**
	 * Returns the first shield id found among the given item ids.
	 * @param itemIds the item ids to scan
	 * @param itemData the item data lookup
	 * @return a shield item id, or {@code 0} if none is found
	 */
	private static int firstShield(int[] itemIds, ItemData itemData)
	{
		for (int itemId : itemIds)
		{
			if ((itemData.getTemplate(itemId) instanceof Armor armor) && (armor.getItemType() == ArmorType.SHIELD))
			{
				return itemId;
			}
		}
		
		return 0;
	}
	
	/**
	 * Checks whether the weapon type is a Kamael only weapon, usable by the Kamael race and no other.
	 * @param weaponType the weapon type
	 * @return {@code true} for rapier, ancient sword and crossbows
	 */
	private static boolean isKamaelWeapon(WeaponType weaponType)
	{
		return (weaponType == WeaponType.RAPIER) || (weaponType == WeaponType.ANCIENTSWORD) || (weaponType == WeaponType.CROSSBOW) || (weaponType == WeaponType.TWOHANDCROSSBOW);
	}
	
	/**
	 * Checks whether an item must never be handed to a fake player: pet equipment or an npc only item ("Monster Only" in its name).
	 * @param item the item template
	 * @return {@code true} if the item is forbidden
	 */
	private static boolean isForbidden(ItemTemplate item)
	{
		return (item.getCommissionItemType() == CommissionItemType.PET_EQUIPMENT) || item.getName().contains("onster" /* Monster Only - For monsters */);
	}
	
	/**
	 * Maps an item body part to the wearable slot we track, or {@code null} when it is not a body armor slot.
	 * @param bodyPart the item body part
	 * @return the matching {@link ArmorSlot} or {@code null}
	 */
	private static ArmorSlot toSlot(BodyPart bodyPart)
	{
		switch (bodyPart)
		{
			case HEAD:
			{
				return ArmorSlot.HEAD;
			}
			case CHEST:
			case FULL_ARMOR:
			{
				return ArmorSlot.CHEST;
			}
			case LEGS:
			{
				return ArmorSlot.LEGS;
			}
			case GLOVES:
			{
				return ArmorSlot.GLOVES;
			}
			case FEET:
			{
				return ArmorSlot.FEET;
			}
			default:
			{
				return null;
			}
		}
	}
	
	/**
	 * Returns the per slot armor pool for a grade and armor type, or an empty map when none exists.
	 * @param grade the item grade
	 * @param type the armor type
	 * @return the slot to item id map, never {@code null}
	 */
	private static Map<ArmorSlot, List<Integer>> armorPool(ItemGrade grade, ArmorType type)
	{
		final Map<ArmorType, Map<ArmorSlot, List<Integer>>> byType = ARMOR_POOLS.get(grade);
		if (byType == null)
		{
			return Collections.emptyMap();
		}
		
		return byType.getOrDefault(type, Collections.emptyMap());
	}
	
	/**
	 * Returns a random element from the list, or {@code null} when the list is missing or empty.
	 * @param list the list to pick from
	 * @return a random element or {@code null}
	 */
	private static Integer randomFrom(List<Integer> list)
	{
		if ((list == null) || list.isEmpty())
		{
			return null;
		}
		
		return list.get(Rnd.get(list.size()));
	}
}
