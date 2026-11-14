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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.EnchantSkillHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillEnchantType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and management of enchantment skill groups from XML data.<br>
 * It maps specific skills to their respective categories for use in the game's enchantment system.
 * @author Micr0, BazookaRpm
 */
public class EnchantSkillGroupsData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(EnchantSkillGroupsData.class.getName());
	
	private final Map<Integer, EnchantSkillHolder> _enchantSkillHolders = new ConcurrentHashMap<>();
	private final Map<SkillHolder, Set<Integer>> _enchantSkillTrees = new ConcurrentHashMap<>();
	
	public static int MAX_ENCHANT_LEVEL;
	
	/**
	 * Initializes a new instance of {@code EnchantSkillGroupsData}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the data for use by loading necessary configurations.
	 */
	protected EnchantSkillGroupsData()
	{
		load();
	}
	
	/**
	 * This method initializes the data for enchantment skills.<br>
	 * It clears existing holders and parses the {@code data/EnchantSkillGroups.xml} file.<br>
	 * The {@code MAX_ENCHANT_LEVEL} is updated based on the loaded routes.
	 */
	@Override
	public void load()
	{
		_enchantSkillHolders.clear();
		parseDatapackFile("data/EnchantSkillGroups.xml");
		MAX_ENCHANT_LEVEL = _enchantSkillHolders.size();
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + MAX_ENCHANT_LEVEL + " enchant routes, max enchant set to " + MAX_ENCHANT_LEVEL + ".");
	}
	
	/**
	 * Parses the XML document to load enchantment skill data.<br>
	 * It populates the internal maps with skills, stats, and requirements.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "enchant", enchantNode ->
		{
			final EnchantSkillHolder enchantSkillHolder = new EnchantSkillHolder(new StatSet(parseAttributes(enchantNode)));
			forEach(enchantNode, "sps", spsNode -> forEach(spsNode, "sp", spNode ->
			{
				final NamedNodeMap spAttributes = spNode.getAttributes();
				enchantSkillHolder.addSp(parseEnum(spAttributes, SkillEnchantType.class, "type"), parseInteger(spAttributes, "amount"));
			}));
			forEach(enchantNode, "chances", chancesNode -> forEach(chancesNode, "chance", chanceNode ->
			{
				final NamedNodeMap chanceAttributes = chanceNode.getAttributes();
				enchantSkillHolder.addChance(parseEnum(chanceAttributes, SkillEnchantType.class, "type"), parseInteger(chanceAttributes, "value"));
			}));
			forEach(enchantNode, "items", itemsNode -> forEach(itemsNode, "item", itemNode -> enchantSkillHolder.addRequiredItem(parseEnum(itemNode.getAttributes(), SkillEnchantType.class, "type"), new ItemHolder(new StatSet(parseAttributes(itemNode))))));
			_enchantSkillHolders.put(parseInteger(enchantNode.getAttributes(), "level"), enchantSkillHolder);
		}));
	}
	
	/**
	 * Adds a specific route to a skill based on its ID and level.<br>
	 * This method updates the internal mapping for enchantment routes.
	 * @param skillId The unique identifier for the skill.
	 * @param level The current level of the skill.
	 * @param route The route value to assign to this skill and level.
	 */
	public void addRouteForSkill(int skillId, int level, int route)
	{
		addRouteForSkill(new SkillHolder(skillId, level), route);
	}
	
	/**
	 * Adds a specific route to the set of routes for a given skill.<br>
	 * This method updates the internal mapping between {@code SkillHolder} and its associated routes.
	 * @param holder The {@code SkillHolder} object that owns the skill.
	 * @param route The integer value representing the route to add.
	 */
	public void addRouteForSkill(SkillHolder holder, int route)
	{
		_enchantSkillTrees.computeIfAbsent(holder, _ -> new HashSet<>()).add(route);
	}
	
	/**
	 * Retrieves the set of routes associated with a specific skill and level.<br>
	 * This method looks up the data based on the provided {@code skillId}.<br>
	 * It defaults to a sub-level of {@code 0}.
	 * @param skillId The unique identifier for the skill.
	 * @param level The current level of the skill.
	 * @return A {@code Set} of integers representing the routes.
	 */
	public Set<Integer> getRouteForSkill(int skillId, int level)
	{
		return getRouteForSkill(skillId, level, 0);
	}
	
	/**
	 * Retrieves the set of routes associated with a specific skill.<br>
	 * This method uses the provided {@code skillId}, {@code level}, and {@code subLevel} to identify the correct route.
	 * @param skillId The unique identifier for the skill.
	 * @param level The current level of the skill.
	 * @param subLevel The specific sub-level of the skill.
	 * @return A {@code Set<Integer>} containing the associated routes.
	 */
	public Set<Integer> getRouteForSkill(int skillId, int level, int subLevel)
	{
		return getRouteForSkill(new SkillHolder(skillId, level, subLevel));
	}
	
	/**
	 * Retrieves the set of routes associated with a specific {@code SkillHolder}.<br>
	 * It returns an empty {@code Set} if no routes are found for the given holder.
	 * @param holder The {@code SkillHolder} to look up.
	 * @return A {@code Set} of integers representing the routes.
	 */
	public Set<Integer> getRouteForSkill(SkillHolder holder)
	{
		return _enchantSkillTrees.getOrDefault(holder, Collections.emptySet());
	}
	
	/**
	 * Checks if a specific {@code Skill} can be enchanted.<br>
	 * This method determines eligibility based on the skill ID and level.
	 * @param skill The {@code Skill} object to check.
	 * @return {@code true} if the skill is enchantable, {@code false} otherwise.
	 */
	public boolean isEnchantable(Skill skill)
	{
		return isEnchantable(new SkillHolder(skill.getId(), skill.getLevel()));
	}
	
	/**
	 * Checks if a specific skill can be enchanted.<br>
	 * This method looks up the {@code SkillHolder} in the internal tree map.
	 * @param holder The {@code SkillHolder} to check.
	 * @return {@code true} if the skill is enchantable, otherwise {@code false}.
	 */
	public boolean isEnchantable(SkillHolder holder)
	{
		return _enchantSkillTrees.containsKey(holder);
	}
	
	/**
	 * Retrieves the {@code EnchantSkillHolder} for a specific enchantment level.<br>
	 * This method looks up the holder based on the provided integer value.
	 * @param level The enchantment level to look up.
	 * @return The {@code EnchantSkillHolder} associated with the given level, or {@code null} if not found.
	 */
	public EnchantSkillHolder getEnchantSkillHolder(int level)
	{
		return _enchantSkillHolders.get(level);
	}
	
	/**
	 * Provides access to the singleton instance of {@code EnchantSkillGroupsData}.<br>
	 * Use this method to retrieve the global data holder for enchant skills.
	 * @return The single instance of {@code EnchantSkillGroupsData}.
	 */
	public static EnchantSkillGroupsData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final EnchantSkillGroupsData INSTANCE = new EnchantSkillGroupsData();
	}
}
