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
package org.l2jmobius.tools.editor.item.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.l2jmobius.tools.editor.item.model.ItemCapsuleRecord;
import org.l2jmobius.tools.editor.item.model.ItemConditionRecord;
import org.l2jmobius.tools.editor.item.model.ItemSetProperty;
import org.l2jmobius.tools.editor.item.model.ItemSetProperty.Category;
import org.l2jmobius.tools.editor.item.model.ItemSetProperty.Kind;
import org.l2jmobius.tools.editor.item.model.ItemSkillRecord;
import org.l2jmobius.tools.editor.item.model.ItemStatRecord;
import org.l2jmobius.tools.editor.npc.util.NpcXmlUtil;

/**
 * Inspects and surgically updates one item XML fragment without rebuilding unrelated source text.
 * @author BazookaRpm
 */
public final class ItemStructuredEditor
{
	private static final List<String> MATERIAL_TYPES = values("STEEL,FINE_STEEL,COTTON,BLOOD_STEEL,BRONZE,SILVER,GOLD,MITHRIL,ORIHARUKON,PAPER,WOOD,CLOTH,LEATHER,BONE,HORN,DAMASCUS,ADAMANTAITE,CHRYSOLITE,CRYSTAL,LIQUID,SCALE_OF_DRAGON,DYESTUFF,COBWEB,SEED,FISH,RUNE_XP,RUNE_SP,RUNE_REMOVE_PENALTY");
	private static final List<String> CRYSTAL_TYPES = values("NONE,D,C,B,A,S,S80,S84,R,R95,R99,R110,L,EVENT");
	private static final List<String> ACTION_TYPES = values("CALC,CALL_SKILL,CAPSULE,CREATE_MPCC,DICE,EQUIP,FISHINGSHOT,HARVEST,HIDE_NAME,KEEP_EXP,NICK_COLOR,NONE,PEEL,RECIPE,SEED,SHOW_ADVENTURER_GUIDE_BOOK,SHOW_HTML,SHOW_SSQ_STATUS,SKILL_MAINTAIN,SKILL_REDUCE,SOULSHOT,SPIRITSHOT,START_QUEST,SUMMON_SOULSHOT,SUMMON_SPIRITSHOT,XMAS_OPEN,SKILL_REDUCE_ON_SKILL_SUCCESS,SHOW_TUTORIAL");
	private static final List<String> WEAPON_TYPES = values("NONE,SWORD,BLUNT,DAGGER,POLE,DUALFIST,BOW,ETC,DUAL,FIST,FISHINGROD,RAPIER,CROSSBOW,ANCIENTSWORD,FLAG,DUALDAGGER,OWNTHING,TWOHANDCROSSBOW,DUALBLUNT");
	private static final List<String> ARMOR_TYPES = values("NONE,LIGHT,HEAVY,MAGIC,SIGIL,SHIELD");
	private static final List<String> ETC_ITEM_TYPES = values("NONE,SCROLL,ARROW,POTION,SPELLBOOK,RECIPE,MATERIAL,PET_COLLAR,CASTLE_GUARD,DYE,SEED,SEED2,HARVEST,LOTTO,RACE_TICKET,TICKET_OF_LORD,LURE,CROP,MATURECROP,ENCHT_WP,ENCHT_AM,ENCHT_AM_DOWN,ENCHT_WP_DOWN,GIANT_ENCHT_WP,GIANT_ENCHT_AM,BLESS_ENCHT_WP,BLESS_ENCHT_AM,BLESS_ENCHT_AM_DOWN,COUPON,ELIXIR,ENCHT_ATTR,ENCHT_ATTR_CURSED,BOLT,ENCHT_ATTR_INC_PROP_ENCHT_WP,ENCHT_ATTR_INC_PROP_ENCHT_AM,BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_WP,BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_AM,BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP,BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM,GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP,GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM,ENCHT_ATTR_CRYSTAL_ENCHANT_AM,ENCHT_ATTR_CRYSTAL_ENCHANT_WP,ENCHT_ATTR_ANCIENT_CRYSTAL_ENCHANT_AM,ENCHT_ATTR_ANCIENT_CRYSTAL_ENCHANT_WP,ENCHT_ATTR_RUNE,ENCHT_ATTRT_RUNE_SELECT,TELEPORTBOOKMARK,CHANGE_ATTR,SOULSHOT,SHAPE_SHIFTING_WP,BLESS_SHAPE_SHIFTING_WP,SHAPE_SHIFTING_WP_FIXED,SHAPE_SHIFTING_AM,BLESS_SHAPE_SHIFTING_AM,SHAPE_SHIFTING_AM_FIXED,SHAPE_SHIFTING_HAIRACC,BLESS_SHAPE_SHIFTING_HAIRACC,SHAPE_SHIFTING_HAIRACC_FIXED,RESTORE_SHAPE_SHIFTING_WP,RESTORE_SHAPE_SHIFTING_AM,RESTORE_SHAPE_SHIFTING_HAIRACC,RESTORE_SHAPE_SHIFTING_ALLITEM,BLESS_INC_PROP_ENCHT_WP,BLESS_INC_PROP_ENCHT_AM,CARD_EVENT,SHAPE_SHIFTING_ALLITEM_FIXED,MULTI_ENCHT_WP,MULTI_ENCHT_AM,MULTI_INC_PROB_ENCHT_WP,MULTI_INC_PROB_ENCHT_AM,SOUL_CRYSTAL,ENSOUL_STONE");
	private static final List<String> BODY_PARTS = values("none,underwear,shirt,rear,rbracelet,lear,rear;lear,neck,rfinger,lfinger,rfinger;lfinger,head,rhand,lhand,gloves,chest,legs,feet,back,lrhand,fullarmor,onepiece,hair,alldress,hair2,hairall,dhair,lbracelet,deco1,talisman,belt,waist,brooch,brooch_jewel,agathion,artifactbook,artifact,wolf,hatchling,strider,babypet,greatwolf");
	private static final List<String> ITEM_SKILL_TYPES = values("NORMAL,ON_ENCHANT,ON_EQUIP,ON_UNEQUIP,ON_CRITICAL_SKILL,ON_MAGIC_SKILL");
	private static final List<String> COMMISSION_ITEM_TYPES = values("ONE_HAND_SWORD,ONE_HAND_MAGIC_SWORD,DAGGER,RAPIER,TWO_HAND_SWORD,ANCIENT_SWORD,DUALSWORD,DUAL_DAGGER,BLUNT_WEAPON,ONE_HAND_MAGIC_BLUNT_WEAPON,TWO_HAND_BLUNT_WEAPON,TWO_HAND_MAGIC_BLUNT_WEAPON,DUAL_BLUNT_WEAPON,BOW,CROSSBOW,FIST_WEAPON,SPEAR,OTHER_WEAPON,HELMET,ARMOR_TOP,ARMOR_PANTS,FULL_BODY,GLOVES,FEET,SHIELD,SIGIL,UNDERWEAR,CLOAK,RING,EARRING,NECKLACE,BELT,BRACELET,AGATHION,HAIR_ACCESSORY,BROOCH_JEWEL,ARTIFACT,POTION,SCROLL_ENCHANT_WEAPON,SCROLL_ENCHANT_ARMOR,SCROLL_OTHER,SOULSHOT,SPIRITSHOT,OTHER_SUPPLIES,PET_EQUIPMENT,PET_SUPPLIES,CRYSTAL,RECIPE,MAJOR_CRAFTING_INGREDIENTS,LIFE_STONE,SOUL_CRYSTAL,ATTRIBUTE_STONE,WEAPON_ENCHANT_STONE,ARMOR_ENCHANT_STONE,SPELLBOOK,GEMSTONE,POUCH,PIN,MAGIC_RUNE_CLIP,MAGIC_ORNAMENT,DYES,OTHER_ITEM");
	private static final Set<String> BOOLEAN_KEYS = Set.of("allow_self_resurrection", "blessed", "element_enabled", "enchant_enabled", "ex_immediate_effect", "for_npc", "immediate_effect", "isAppearanceable", "isAttackWeapon", "isForceEquip", "is_clan_depositable", "is_commissionable", "is_cursed_weapon", "is_depositable", "is_destroyable", "is_dragon_weapon", "is_dropable", "is_event_restricted", "is_freightable", "is_infinite", "is_magic_weapon", "is_oly_restricted", "is_premium", "is_private_storeable", "is_questitem", "is_questusable", "is_sellable", "is_stackable", "is_tradable", "useWeaponSkillsOnly");
	private static final Set<String> INTEGER_KEYS = Set.of("artifactSlot", "attack_range", "auto_destroy_time", "change_weaponId", "crystal_count", "displayId", "duration", "enchanted", "enchant_limit", "ensoulNormalSlots", "ensoulSpecialSlots", "equip_reuse_delay", "extractableCountMax", "extractableCountMin", "gearScore", "mp_consume", "recipe_id", "reuse_delay", "shared_reuse_group", "soulshots", "spiritshots", "time", "useSkillDisTime", "weight");
	private static final Set<String> EQUIPMENT_KEYS = Set.of("bodypart", "equip_reuse_delay", "duration", "time", "crystal_type", "crystal_count", "element_enabled", "enchant_enabled", "enchant_limit", "ensoulNormalSlots", "ensoulSpecialSlots", "artifactSlot", "useSkillDisTime");
	private static final Set<String> TYPE_KEYS = Set.of("weapon_type", "armor_type", "etcitem_type", "handler", "commissionItemType", "is_magic_weapon", "is_dragon_weapon", "is_cursed_weapon", "soulshots", "spiritshots", "mp_consume", "attack_range", "damage_range", "reduced_soulshot", "reduced_mp_consume", "change_weaponId", "isForceEquip", "isAttackWeapon", "useWeaponSkillsOnly");
	private static final Set<String> GENERAL_KEYS = Set.of("displayId", "name", "additionalName", "icon", "weight", "material", "price", "default_action", "commissionItemType");
	private static final Map<String, List<String>> ENUMS = createEnums();
	
	public String getRootAttribute(String xml, String name)
	{
		return safe(ItemXmlSpans.attribute(ItemXmlSpans.rootItem(xml).getOpeningTag(xml), name));
	}
	
	public String updateRootAttribute(String xml, String name, String value) throws Exception
	{
		final ItemXmlSpans.ElementSpan root = ItemXmlSpans.rootItem(xml);
		final String result = replace(xml, root.getStart(), root.getContentStart(), ItemXmlSpans.replaceAttribute(root.getOpeningTag(xml), name, value, false));
		NpcXmlUtil.parse(result);
		return result;
	}
	
	public List<ItemSetProperty> inspectSetProperties(String xml) throws Exception
	{
		final Element root = NpcXmlUtil.parse(xml).getDocumentElement();
		final List<ItemSetProperty> result = new ArrayList<>();
		int ordinal = 0;
		for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ((node.getNodeType() != Node.ELEMENT_NODE) || !"set".equals(node.getNodeName()))
			{
				continue;
			}
			
			final Element element = (Element) node;
			final String name = element.getAttribute("name");
			final String value = element.getAttribute("val");
			result.add(new ItemSetProperty(ordinal++, name, value, category(name), kind(name), options(name, value)));
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public String updateSetProperty(String xml, ItemSetProperty property, String value) throws Exception
	{
		final int row = property.getOrdinal();
		int ordinal = 0;
		for (ItemXmlSpans.ElementSpan span : ItemXmlSpans.directChildren(ItemXmlSpans.spans(xml), "item", "set"))
		{
			if (ordinal++ != row)
			{
				continue;
			}
			
			final String opening = span.getOpeningTag(xml);
			final String name = ItemXmlSpans.attribute(opening, "name");
			final String expected = property.getName();
			if (!expected.equals(name))
			{
				throw new IllegalArgumentException("Item set row " + row + " changed from '" + expected + "' to '" + name + "'.");
			}
			
			final String result = replace(xml, span.getStart(), span.getContentStart(), ItemXmlSpans.replaceAttribute(opening, "val", value, false));
			NpcXmlUtil.parse(result);
			return result;
		}
		
		throw new IllegalArgumentException("Could not locate item set row " + row + ".");
	}
	
	public List<ItemStatRecord> inspectStats(String xml)
	{
		final List<ItemXmlSpans.ElementSpan> spans = ItemXmlSpans.spans(xml);
		final List<ItemStatRecord> result = new ArrayList<>();
		int ordinal = 0;
		for (ItemXmlSpans.ElementSpan parent : ItemXmlSpans.directChildren(spans, "item", "stats"))
		{
			for (ItemXmlSpans.ElementSpan span : ItemXmlSpans.childrenOfSpan(spans, parent, "stat"))
			{
				final String opening = span.getOpeningTag(xml);
				final String type = safe(ItemXmlSpans.attribute(opening, "type"));
				final String elementXml = span.getXml(xml);
				final int textEnd = elementXml.lastIndexOf("</");
				result.add(new ItemStatRecord(ordinal++, type, textEnd < 0 ? "" : elementXml.substring(opening.length(), textEnd).trim()));
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public String updateStat(String xml, ItemStatRecord record, String value) throws Exception
	{
		final List<ItemXmlSpans.ElementSpan> spans = ItemXmlSpans.spans(xml);
		final int row = record.getOrdinal();
		int ordinal = 0;
		for (ItemXmlSpans.ElementSpan parent : ItemXmlSpans.directChildren(spans, "item", "stats"))
		{
			for (ItemXmlSpans.ElementSpan span : ItemXmlSpans.childrenOfSpan(spans, parent, "stat"))
			{
				if (ordinal++ == row)
				{
					final String elementXml = span.getXml(xml);
					final int textEnd = elementXml.lastIndexOf("</");
					if (textEnd < 0)
					{
						throw new IllegalArgumentException("Stat row " + row + " is not a normal element.");
					}
					
					final String result = replace(xml, span.getStart(), span.getEnd(), span.getOpeningTag(xml) + ItemXmlSpans.escapeText(value) + elementXml.substring(textEnd));
					NpcXmlUtil.parse(result);
					return result;
				}
			}
		}
		
		throw new IllegalArgumentException("Could not locate stat row " + row + ".");
	}
	
	public List<ItemSkillRecord> inspectSkills(String xml)
	{
		final List<ItemXmlSpans.ElementSpan> spans = ItemXmlSpans.spans(xml);
		final List<ItemSkillRecord> result = new ArrayList<>();
		for (String container : List.of("skills", "unequip_skills"))
		{
			int ordinal = 0;
			for (ItemXmlSpans.ElementSpan parent : ItemXmlSpans.directChildren(spans, "item", container))
			{
				for (ItemXmlSpans.ElementSpan span : ItemXmlSpans.childrenOfSpan(spans, parent, "skill"))
				{
					final String opening = span.getOpeningTag(xml);
					final int id = intValue(ItemXmlSpans.attribute(opening, "id"));
					final int level = intValue(ItemXmlSpans.attribute(opening, "level"));
					final int subLevel = intValue(ItemXmlSpans.attribute(opening, "subLevel"));
					final String type = safe(ItemXmlSpans.attribute(opening, "type"));
					result.add(new ItemSkillRecord(container, ordinal++, id, level, subLevel, type.isBlank() ? "NORMAL" : type, safe(ItemXmlSpans.attribute(opening, "type_chance")), safe(ItemXmlSpans.attribute(opening, "type_value"))));
				}
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public String updateSkill(String xml, ItemSkillRecord record, String attribute, String value) throws Exception
	{
		if ((record == null) || (attribute == null))
		{
			throw new IllegalArgumentException("An item skill row and attribute are required.");
		}
		
		if (!"id".equals(attribute) && !"level".equals(attribute) && !"subLevel".equals(attribute) && !"type".equals(attribute) && !"type_chance".equals(attribute) && !"type_value".equals(attribute))
		{
			throw new IllegalArgumentException("Main item skills support ID, level, subLevel, type, type_chance, and type_value edits.");
		}
		
		final ItemXmlSpans.ElementSpan span = findNestedOrdinal(xml, record.getContainer(), "skill", record.getOrdinal());
		final boolean optional = "subLevel".equals(attribute) || "type".equals(attribute) || "type_chance".equals(attribute) || "type_value".equals(attribute);
		final String storedValue = "type".equals(attribute) && "NORMAL".equals(value) ? "" : value;
		final String result = replace(xml, span.getStart(), span.getContentStart(), ItemXmlSpans.replaceAttribute(span.getOpeningTag(xml), attribute, storedValue, optional && ((storedValue == null) || storedValue.isBlank())));
		NpcXmlUtil.parse(result);
		return result;
	}
	
	public List<ItemCapsuleRecord> inspectCapsules(String xml)
	{
		final List<ItemXmlSpans.ElementSpan> spans = ItemXmlSpans.spans(xml);
		final List<ItemCapsuleRecord> result = new ArrayList<>();
		int ordinal = 0;
		for (ItemXmlSpans.ElementSpan parent : ItemXmlSpans.directChildren(spans, "item", "capsuled_items"))
		{
			for (ItemXmlSpans.ElementSpan span : ItemXmlSpans.childrenOfSpan(spans, parent, "item"))
			{
				final String opening = span.getOpeningTag(xml);
				result.add(new ItemCapsuleRecord(ordinal++, intValue(ItemXmlSpans.attribute(opening, "id")), safe(ItemXmlSpans.attribute(opening, "min")), safe(ItemXmlSpans.attribute(opening, "max")), safe(ItemXmlSpans.attribute(opening, "chance")), safe(ItemXmlSpans.attribute(opening, "minEnchant")), safe(ItemXmlSpans.attribute(opening, "maxEnchant"))));
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public String updateCapsule(String xml, ItemCapsuleRecord record, String attribute, String value) throws Exception
	{
		final ItemXmlSpans.ElementSpan span = findNestedOrdinal(xml, "capsuled_items", "item", record.getOrdinal());
		final String result = replace(xml, span.getStart(), span.getContentStart(), ItemXmlSpans.replaceAttribute(span.getOpeningTag(xml), attribute, value, ("minEnchant".equals(attribute) || "maxEnchant".equals(attribute)) && ((value == null) || value.isBlank())));
		NpcXmlUtil.parse(result);
		return result;
	}
	
	public List<ItemConditionRecord> inspectConditions(String xml)
	{
		final List<ItemConditionRecord> result = new ArrayList<>();
		int ordinal = 0;
		for (ItemXmlSpans.ElementSpan span : ItemXmlSpans.directChildren(ItemXmlSpans.spans(xml), "item", "conditions"))
		{
			result.add(new ItemConditionRecord(ordinal, "Conditions " + (ordinal + 1), span.getXml(xml)));
			ordinal++;
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public String updateCondition(String xml, ItemConditionRecord record, String conditionXml) throws Exception
	{
		final Document wrapper = NpcXmlUtil.parse("<item>" + conditionXml + "</item>");
		int elements = 0;
		for (Node node = wrapper.getDocumentElement().getFirstChild(); node != null; node = node.getNextSibling())
		{
			if (node.getNodeType() == Node.ELEMENT_NODE)
			{
				elements++;
				if (!"conditions".equals(node.getNodeName()))
				{
					throw new IllegalArgumentException("Condition editor accepts exactly one <conditions> block.");
				}
			}
		}
		
		if (elements != 1)
		{
			throw new IllegalArgumentException("Condition editor accepts exactly one <conditions> block.");
		}
		
		final List<ItemXmlSpans.ElementSpan> conditions = ItemXmlSpans.directChildren(ItemXmlSpans.spans(xml), "item", "conditions");
		final int row = record.getOrdinal();
		if ((row < 0) || (row >= conditions.size()))
		{
			throw new IllegalArgumentException("Could not locate conditions block " + (row + 1) + ".");
		}
		
		final ItemXmlSpans.ElementSpan span = conditions.get(row);
		final String result = replace(xml, span.getStart(), span.getEnd(), conditionXml.trim());
		NpcXmlUtil.parse(result);
		return result;
	}
	
	public List<String> summarizeChanges(String originalXml, String currentXml, int maximum) throws Exception
	{
		if (originalXml.equals(currentXml))
		{
			return Collections.emptyList();
		}
		
		final List<String> result = new ArrayList<>();
		for (String attribute : List.of("name", "additionalName"))
		{
			addChange(result, attribute, getRootAttribute(originalXml, attribute), getRootAttribute(currentXml, attribute), maximum);
		}
		
		addIndexedChanges(result, "set", setValues(inspectSetProperties(originalXml)), setValues(inspectSetProperties(currentXml)), maximum);
		if (result.size() >= maximum)
		{
			return Collections.unmodifiableList(result);
		}
		
		addIndexedChanges(result, "stat", statValues(inspectStats(originalXml)), statValues(inspectStats(currentXml)), maximum);
		addIndexedChanges(result, "skill", skillValues(inspectSkills(originalXml)), skillValues(inspectSkills(currentXml)), maximum);
		addIndexedChanges(result, "capsule", capsuleValues(inspectCapsules(originalXml)), capsuleValues(inspectCapsules(currentXml)), maximum);
		if ((result.size() < maximum) && !conditionValues(inspectConditions(originalXml)).equals(conditionValues(inspectConditions(currentXml))))
		{
			result.add("conditions: modified");
		}
		
		if (result.isEmpty())
		{
			result.add("Raw XML changed");
		}
		
		return Collections.unmodifiableList(result);
	}
	
	public static List<String> itemSkillTypes()
	{
		return ITEM_SKILL_TYPES;
	}
	
	public static List<String> enumOptions(String name)
	{
		return ENUMS.getOrDefault(name, Collections.emptyList());
	}
	
	private static Category category(String name)
	{
		if (BOOLEAN_KEYS.contains(name))
		{
			return Category.FLAGS;
		}
		
		if (TYPE_KEYS.contains(name))
		{
			return Category.TYPE_SPECIFIC;
		}
		
		if (EQUIPMENT_KEYS.contains(name))
		{
			return Category.EQUIPMENT;
		}
		
		if (GENERAL_KEYS.contains(name))
		{
			return Category.GENERAL;
		}
		
		return Category.OTHER;
	}
	
	private static Kind kind(String name)
	{
		if (BOOLEAN_KEYS.contains(name))
		{
			return Kind.BOOLEAN;
		}
		
		if (ENUMS.containsKey(name))
		{
			return Kind.ENUM;
		}
		
		if ("damage_range".equals(name))
		{
			return Kind.DAMAGE_RANGE;
		}
		
		if ("reduced_soulshot".equals(name) || "reduced_mp_consume".equals(name))
		{
			return Kind.INTEGER_PAIR;
		}
		
		if (INTEGER_KEYS.contains(name))
		{
			return Kind.INTEGER;
		}
		
		if ("price".equals(name))
		{
			return Kind.LONG;
		}
		
		return Kind.TEXT;
	}
	
	private static List<String> options(String name, String current)
	{
		final List<String> source = ENUMS.get(name);
		if (source == null)
		{
			return Collections.emptyList();
		}
		
		if ((current == null) || current.isBlank() || source.contains(current))
		{
			return source;
		}
		
		final List<String> result = new ArrayList<>(source.size() + 1);
		result.add(current);
		result.addAll(source);
		return Collections.unmodifiableList(result);
	}
	
	private static Map<String, List<String>> createEnums()
	{
		final Map<String, List<String>> result = new HashMap<>();
		result.put("material", MATERIAL_TYPES);
		result.put("crystal_type", CRYSTAL_TYPES);
		result.put("default_action", ACTION_TYPES);
		result.put("weapon_type", WEAPON_TYPES);
		result.put("armor_type", ARMOR_TYPES);
		result.put("etcitem_type", ETC_ITEM_TYPES);
		result.put("bodypart", BODY_PARTS);
		result.put("commissionItemType", COMMISSION_ITEM_TYPES);
		return Collections.unmodifiableMap(result);
	}
	
	private static ItemXmlSpans.ElementSpan findNestedOrdinal(String xml, String parentName, String childName, int ordinal)
	{
		final List<ItemXmlSpans.ElementSpan> spans = ItemXmlSpans.spans(xml);
		int index = 0;
		for (ItemXmlSpans.ElementSpan parent : ItemXmlSpans.directChildren(spans, "item", parentName))
		{
			for (ItemXmlSpans.ElementSpan span : ItemXmlSpans.childrenOfSpan(spans, parent, childName))
			{
				if (index++ == ordinal)
				{
					return span;
				}
			}
		}
		
		throw new IllegalArgumentException("Could not locate " + parentName + "/" + childName + " row " + ordinal + ".");
	}
	
	private static String replace(String source, int start, int end, String value)
	{
		return source.substring(0, start) + value + source.substring(end);
	}
	
	private static int intValue(String value)
	{
		try
		{
			return Integer.parseInt(safe(value));
		}
		catch (NumberFormatException e)
		{
			return 0;
		}
	}
	
	private static void addChange(List<String> result, String name, String before, String after, int maximum)
	{
		if ((result.size() < maximum) && !safe(before).equals(safe(after)))
		{
			result.add(name + ": " + display(before) + " -> " + display(after));
		}
	}
	
	private static void addIndexedChanges(List<String> result, String label, List<String> before, List<String> after, int maximum)
	{
		final int count = Math.max(before.size(), after.size());
		for (int index = 0; (index < count) && (result.size() < maximum); index++)
		{
			final String left = index < before.size() ? before.get(index) : "";
			final String right = index < after.size() ? after.get(index) : "";
			if (!left.equals(right))
			{
				result.add(label + "[" + (index + 1) + "]: " + display(left) + " -> " + display(right));
			}
		}
	}
	
	private static List<String> setValues(List<ItemSetProperty> properties)
	{
		final List<String> result = new ArrayList<>();
		for (ItemSetProperty property : properties)
		{
			result.add(property.getName() + "=" + property.getValue());
		}
		
		return result;
	}
	
	private static List<String> statValues(List<ItemStatRecord> records)
	{
		final List<String> result = new ArrayList<>();
		for (ItemStatRecord record : records)
		{
			result.add(record.getType() + "=" + record.getValue());
		}
		
		return result;
	}
	
	private static List<String> skillValues(List<ItemSkillRecord> records)
	{
		final List<String> result = new ArrayList<>();
		for (ItemSkillRecord record : records)
		{
			result.add(record.getContainer() + ":" + record.getId() + ":" + record.getLevel() + ":" + record.getSubLevel() + ":" + record.getType() + ":" + record.getChance() + ":" + record.getValue());
		}
		
		return result;
	}
	
	private static List<String> capsuleValues(List<ItemCapsuleRecord> records)
	{
		final List<String> result = new ArrayList<>();
		for (ItemCapsuleRecord record : records)
		{
			result.add(record.getItemId() + ":" + record.getMin() + ":" + record.getMax() + ":" + record.getChance() + ":" + record.getMinEnchant() + ":" + record.getMaxEnchant());
		}
		
		return result;
	}
	
	private static List<String> conditionValues(List<ItemConditionRecord> records)
	{
		final List<String> result = new ArrayList<>();
		for (ItemConditionRecord record : records)
		{
			result.add(record.getXml());
		}
		
		return result;
	}
	
	private static String display(String value)
	{
		final String safe = safe(value);
		return safe.isBlank() ? "<empty>" : safe;
	}
	
	private static String safe(String value)
	{
		return value == null ? "" : value;
	}
	
	private static List<String> values(String csv)
	{
		return Collections.unmodifiableList(Arrays.asList(csv.split(",")));
	}
}
