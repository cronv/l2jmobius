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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.l2jmobius.tools.editor.item.model.ItemCapsuleRecord;
import org.l2jmobius.tools.editor.item.model.ItemHandlerCatalog;
import org.l2jmobius.tools.editor.item.model.ItemMetadataIndex;
import org.l2jmobius.tools.editor.item.model.ItemProjectLayout;
import org.l2jmobius.tools.editor.item.model.ItemSetProperty;
import org.l2jmobius.tools.editor.item.model.ItemSetProperty.Kind;
import org.l2jmobius.tools.editor.item.model.ItemSkillRecord;
import org.l2jmobius.tools.editor.item.model.ItemStatRecord;
import org.l2jmobius.tools.editor.item.model.ItemValidationIssue;
import org.l2jmobius.tools.editor.item.model.ItemValidationIssue.Severity;
import org.l2jmobius.tools.editor.item.model.ItemValidationReport;
import org.l2jmobius.tools.editor.npc.model.NpcSkillInfo;
import org.l2jmobius.tools.editor.npc.model.NpcSkillLookup;
import org.l2jmobius.tools.editor.npc.service.LazySkillLookup;
import org.l2jmobius.tools.editor.npc.util.NpcXmlUtil;

/**
 * Validates item semantics against the Main Chronicle item/skill indexes and registered item handlers. Historical unsupported references are warnings when left untouched, but newly introduced invalid references are errors.
 * @author BazookaRpm
 */
public final class ItemSemanticValidator
{
	private static final Pattern CLASS_ENTRY = Pattern.compile("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*\\.class\\s*,?");
	private static final Set<String> BOOLEAN_VALUES = Set.of("true", "false");
	private final ItemStructuredEditor _structuredEditor = new ItemStructuredEditor();
	private NpcSkillLookup _skills;
	private Path _catalogPath;
	private long _catalogSize = -1;
	private long _catalogModified = -1;
	private ItemHandlerCatalog _catalog = new ItemHandlerCatalog(Collections.emptySet(), false);
	
	public ItemValidationReport validate(ItemProjectLayout layout, ItemMetadataIndex itemIndex, String originalXml, String currentXml) throws Exception
	{
		NpcXmlUtil.parse(currentXml);
		final List<ItemValidationIssue> issues = new ArrayList<>();
		final List<ItemSetProperty> originalProperties = _structuredEditor.inspectSetProperties(originalXml);
		final Map<String, String> originalSets = toSetMap(originalProperties);
		final List<ItemSetProperty> currentSets = _structuredEditor.inspectSetProperties(currentXml);
		validateSetProperties(currentSets, originalProperties, issues);
		validateHandler(layout, currentSets, originalSets, issues);
		validateStats(_structuredEditor.inspectStats(currentXml), issues);
		validateSkills(layout, _structuredEditor.inspectSkills(originalXml), _structuredEditor.inspectSkills(currentXml), issues);
		validateCapsules(itemIndex, _structuredEditor.inspectCapsules(originalXml), _structuredEditor.inspectCapsules(currentXml), issues);
		return new ItemValidationReport(issues);
	}
	
	public void validateOrThrow(ItemProjectLayout layout, ItemMetadataIndex itemIndex, String originalXml, String currentXml) throws Exception
	{
		final ItemValidationReport report = validate(layout, itemIndex, originalXml, currentXml);
		if (report.hasErrors())
		{
			final StringBuilder message = new StringBuilder("Item semantic validation failed:");
			for (ItemValidationIssue issue : report.getIssues())
			{
				if (issue.getSeverity() == Severity.ERROR)
				{
					message.append(System.lineSeparator()).append("- ").append(issue.getMessage());
				}
			}
			
			throw new IllegalArgumentException(message.toString());
		}
	}
	
	public synchronized ItemHandlerCatalog loadHandlerCatalog(ItemProjectLayout layout) throws Exception
	{
		final Path file = layout == null ? null : layout.getMasterHandler();
		if ((file == null) || !Files.isRegularFile(file))
		{
			return new ItemHandlerCatalog(Collections.emptySet(), false);
		}
		
		final Path normalized = file.toAbsolutePath().normalize();
		final BasicFileAttributes attributes = Files.readAttributes(normalized, BasicFileAttributes.class);
		if (normalized.equals(_catalogPath) && (_catalogSize == attributes.size()) && (_catalogModified == attributes.lastModifiedTime().toMillis()))
		{
			return _catalog;
		}
		
		final List<String> lines = Files.readAllLines(normalized);
		final Set<String> handlers = new LinkedHashSet<>();
		boolean inItemHandlers = false;
		for (String line : lines)
		{
			if (line.contains("// Item Handlers"))
			{
				inItemHandlers = true;
				continue;
			}
			
			if (inItemHandlers && line.trim().startsWith("},"))
			{
				break;
			}
			
			if (inItemHandlers)
			{
				final Matcher matcher = CLASS_ENTRY.matcher(line);
				if (matcher.find())
				{
					handlers.add(matcher.group(1));
				}
			}
		}
		
		_catalogPath = normalized;
		_catalogSize = attributes.size();
		_catalogModified = attributes.lastModifiedTime().toMillis();
		_catalog = new ItemHandlerCatalog(handlers, true);
		return _catalog;
	}
	
	private void validateSetProperties(List<ItemSetProperty> properties, List<ItemSetProperty> original, List<ItemValidationIssue> issues)
	{
		for (ItemSetProperty property : properties)
		{
			final String name = property.getName();
			final String value = property.getValue();
			final Kind kind = property.getKind();
			if (kind == Kind.ENUM)
			{
				if (!ItemStructuredEditor.enumOptions(name).contains(value))
				{
					addLegacyAware(issues, name + " has unsupported value '" + value + "'.", valueAtOrdinal(original, property), value);
				}
				continue;
			}
			
			final String message = setPropertyError(kind, name, value);
			if (message != null)
			{
				issues.add(error(message));
			}
		}
		
		final Integer min = integerValue(value(properties, "extractableCountMin"));
		final Integer max = integerValue(value(properties, "extractableCountMax"));
		if ((min != null) && (max != null) && (min > max))
		{
			final boolean unchanged = safeEquals(value(original, "extractableCountMin"), Integer.toString(min)) && safeEquals(value(original, "extractableCountMax"), Integer.toString(max));
			issues.add(new ItemValidationIssue(unchanged ? Severity.WARNING : Severity.ERROR, "extractableCountMin cannot be greater than extractableCountMax." + (unchanged ? " Existing unchanged values were preserved." : "")));
		}
	}
	
	/**
	 * Checks one set value against its kind, shared by the editor table and the full validation pass.
	 * @param kind the property kind
	 * @param name the property name
	 * @param value the raw value
	 * @return the error message, or {@code null} when the value fits the kind (enum values are checked by the caller)
	 */
	public static String setPropertyError(Kind kind, String name, String value)
	{
		switch (kind)
		{
			case INTEGER:
			{
				return isInteger(value) ? null : name + " must be an integer.";
			}
			case LONG:
			{
				return longValue(value) != null ? null : name + " must be an integer.";
			}
			case BOOLEAN:
			{
				return BOOLEAN_VALUES.contains(value.toLowerCase(Locale.ROOT)) ? null : name + " must be true or false.";
			}
			case INTEGER_PAIR:
			{
				return isIntegerList(value, ",", 2) ? null : name + " must contain exactly two comma-separated integers.";
			}
			case DAMAGE_RANGE:
			{
				return isIntegerList(value, ";", 4) ? null : "damage_range must contain exactly four semicolon-separated integers.";
			}
			default:
			{
				return null;
			}
		}
	}
	
	private void validateHandler(ItemProjectLayout layout, List<ItemSetProperty> current, Map<String, String> original, List<ItemValidationIssue> issues) throws Exception
	{
		final String handler = value(current, "handler");
		if (handler.isBlank())
		{
			return;
		}
		
		final ItemHandlerCatalog catalog = loadHandlerCatalog(layout);
		if (!catalog.isAvailable())
		{
			issues.add(new ItemValidationIssue(Severity.WARNING, "Item handler registry could not be inspected; handler '" + handler + "' was not verified."));
			return;
		}
		
		if (!catalog.contains(handler))
		{
			addLegacyAware(issues, "Item handler '" + handler + "' is not registered in MasterHandler.", original.get("handler"), handler);
		}
	}
	
	private void validateStats(List<ItemStatRecord> stats, List<ItemValidationIssue> issues)
	{
		for (ItemStatRecord stat : stats)
		{
			try
			{
				Double.parseDouble(stat.getValue());
			}
			catch (NumberFormatException e)
			{
				issues.add(error("Stat " + stat.getType() + " must have a numeric value."));
			}
		}
	}
	
	private void validateSkills(ItemProjectLayout layout, List<ItemSkillRecord> original, List<ItemSkillRecord> current, List<ItemValidationIssue> issues) throws Exception
	{
		if ((layout == null) || !Files.isDirectory(layout.getSkillRoot()))
		{
			issues.add(new ItemValidationIssue(Severity.WARNING, "Skill datapack was not available; item skill references were not verified."));
			return;
		}
		
		final NpcSkillLookup skills = loadSkills(layout);
		for (int index = 0; index < current.size(); index++)
		{
			final ItemSkillRecord record = current.get(index);
			final ItemSkillRecord before = index < original.size() ? original.get(index) : null;
			final int skillId = record.getId();
			if (skillId <= 0)
			{
				issues.add(error("Item skill reference must use a positive skill ID."));
				continue;
			}
			
			final NpcSkillInfo skill = skills.getSkill(skillId);
			if (skill == null)
			{
				addLegacyAware(issues, "Referenced skill " + skillId + " does not exist in the skill datapack.", before == null ? null : Integer.toString(before.getId()), Integer.toString(skillId));
			}
			else
			{
				final int level = record.getLevel();
				final int minLevel = skill.getMinLevel();
				final int maxLevel = skill.getMaxLevel();
				if ((level < minLevel) || (level > maxLevel))
				{
					addLegacyAware(issues, "Skill " + skillId + " level " + level + " is outside available range " + minLevel + "-" + maxLevel + ".", before == null ? null : before.getId() + ":" + before.getLevel(), skillId + ":" + level);
				}
			}
			
			if (record.getSubLevel() < 0)
			{
				issues.add(error("Item skill subLevel cannot be negative."));
			}
			
			final String type = record.getType();
			if (!ItemStructuredEditor.itemSkillTypes().contains(type))
			{
				issues.add(error("Unsupported item skill type '" + type + "'."));
			}
			
			final String chance = record.getChance();
			if (!chance.isBlank() && !isPositiveInteger(chance))
			{
				issues.add(error("Item skill type_chance must be a positive integer when present."));
			}
			
			final String value = record.getValue();
			if (!value.isBlank() && !isPositiveInteger(value))
			{
				issues.add(error("Item skill type_value must be a positive integer when present."));
			}
		}
	}
	
	private void validateCapsules(ItemMetadataIndex items, List<ItemCapsuleRecord> original, List<ItemCapsuleRecord> current, List<ItemValidationIssue> issues)
	{
		for (int index = 0; index < current.size(); index++)
		{
			final ItemCapsuleRecord record = current.get(index);
			final ItemCapsuleRecord before = index < original.size() ? original.get(index) : null;
			final int itemId = record.getItemId();
			if ((items != null) && !items.contains(itemId))
			{
				addLegacyAware(issues, "Capsuled item " + itemId + " does not exist in the item datapack.", before == null ? null : Integer.toString(before.getItemId()), Integer.toString(itemId));
			}
			
			final String minValue = record.getMin();
			final String maxValue = record.getMax();
			final Long min = longValue(minValue);
			final Long max = longValue(maxValue);
			if ((min == null) || (min <= 0))
			{
				issues.add(error("Capsule min must be a positive integer."));
			}
			
			if ((max == null) || (max <= 0))
			{
				issues.add(error("Capsule max must be a positive integer."));
			}
			
			if ((min != null) && (max != null) && (min > max))
			{
				final boolean unchanged = (before != null) && safeEquals(before.getMin(), minValue) && safeEquals(before.getMax(), maxValue);
				issues.add(new ItemValidationIssue(unchanged ? Severity.WARNING : Severity.ERROR, "Capsule min cannot be greater than max." + (unchanged ? " Existing unchanged values were preserved." : "")));
			}
			
			try
			{
				final double chance = Double.parseDouble(record.getChance());
				if (!Double.isFinite(chance) || (chance <= 0) || (chance > 100))
				{
					issues.add(error("Capsule chance must be greater than 0 and at most 100."));
				}
			}
			catch (NumberFormatException e)
			{
				issues.add(error("Capsule chance must be numeric."));
			}
			
			final String minEnchantValue = record.getMinEnchant();
			final String maxEnchantValue = record.getMaxEnchant();
			validateOptionalEnchant("minEnchant", minEnchantValue, issues);
			validateOptionalEnchant("maxEnchant", maxEnchantValue, issues);
			final Integer minEnchant = integerValue(minEnchantValue);
			final Integer maxEnchant = integerValue(maxEnchantValue);
			if ((minEnchant != null) && (maxEnchant != null) && (minEnchant > maxEnchant))
			{
				issues.add(error("Capsule minEnchant cannot be greater than maxEnchant."));
			}
		}
	}
	
	/**
	 * Replaces the cached skill lookup, so skill datapack changes show after Reload Data.
	 * @param layout the project layout
	 * @throws Exception when the skill directory cannot be read
	 */
	public synchronized void reloadSkills(ItemProjectLayout layout) throws Exception
	{
		final Path skillRoot = layout.getSkillRoot();
		_skills = Files.isDirectory(skillRoot) ? new LazySkillLookup(skillRoot) : null;
	}
	
	private synchronized NpcSkillLookup loadSkills(ItemProjectLayout layout) throws Exception
	{
		if (_skills == null)
		{
			_skills = new LazySkillLookup(layout.getSkillRoot());
		}
		
		return _skills;
	}
	
	private static void validateOptionalEnchant(String name, String value, List<ItemValidationIssue> issues)
	{
		// The items.xsd schema declares enchant bounds as unsignedByte, so zero is a valid bound.
		final Integer number = integerValue(value);
		if (!value.isBlank() && ((number == null) || (number < 0)))
		{
			issues.add(error("Capsule " + name + " must be a non-negative integer when present."));
		}
	}
	
	private static void addLegacyAware(List<ItemValidationIssue> issues, String message, String original, String current)
	{
		if ((original != null) && original.equals(current))
		{
			issues.add(new ItemValidationIssue(Severity.WARNING, message + " Existing unchanged value was preserved."));
		}
		else
		{
			issues.add(error(message));
		}
	}
	
	private static Map<String, String> toSetMap(List<ItemSetProperty> properties)
	{
		final Map<String, String> result = new LinkedHashMap<>();
		for (ItemSetProperty property : properties)
		{
			result.put(property.getName(), property.getValue());
		}
		
		return result;
	}
	
	private static String value(List<ItemSetProperty> properties, String name)
	{
		String result = "";
		for (ItemSetProperty property : properties)
		{
			if (name.equals(property.getName()))
			{
				result = property.getValue();
			}
		}
		
		return result;
	}
	
	private static String valueAtOrdinal(List<ItemSetProperty> properties, ItemSetProperty current)
	{
		for (ItemSetProperty property : properties)
		{
			if ((property.getOrdinal() == current.getOrdinal()) && property.getName().equals(current.getName()))
			{
				return property.getValue();
			}
		}
		
		return null;
	}
	
	private static boolean isInteger(String value)
	{
		return integerValue(value) != null;
	}
	
	private static boolean isPositiveInteger(String value)
	{
		final Integer number = integerValue(value);
		return (number != null) && (number > 0);
	}
	
	private static Integer integerValue(String value)
	{
		if ((value == null) || value.isBlank())
		{
			return null;
		}
		
		try
		{
			return Integer.valueOf(value.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
	
	private static Long longValue(String value)
	{
		if ((value == null) || value.isBlank())
		{
			return null;
		}
		
		try
		{
			return Long.valueOf(value.trim());
		}
		catch (NumberFormatException e)
		{
			return null;
		}
	}
	
	private static boolean isIntegerList(String value, String separator, int expected)
	{
		if ((value == null) || value.isBlank())
		{
			return false;
		}
		
		final String[] parts = value.split(Pattern.quote(separator), -1);
		if (parts.length != expected)
		{
			return false;
		}
		
		for (String part : parts)
		{
			if (!isInteger(part.trim()))
			{
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean safeEquals(String first, String second)
	{
		return (first == null ? "" : first).equals(second == null ? "" : second);
	}
	
	private static ItemValidationIssue error(String message)
	{
		return new ItemValidationIssue(Severity.ERROR, message);
	}
}
