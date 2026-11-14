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
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.handler.EffectHandler;
import org.l2jmobius.gameserver.handler.SkillConditionHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillConditionScope;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Loads skill definitions from XML and builds {@code Skill} objects.
 * @author NosBit, Mobius
 */
public class SkillData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SkillData.class.getName());
	
	private static final Pattern SUB_LEVEL_VALUES_PATTERN = Pattern.compile("subLevel\\d+Values");
	
	private final Map<Long, Skill> _skillsByHash = new ConcurrentHashMap<>();
	private final Map<Integer, Integer> _maxSkillLevels = new ConcurrentHashMap<>();
	
	private class NamedParamInfo
	{
		private final String _name;
		private final Integer _fromLevel;
		private final Integer _toLevel;
		private final Integer _fromSubLevel;
		private final Integer _toSubLevel;
		private final Map<Integer, Map<Integer, StatSet>> _info;
		
		public NamedParamInfo(String name, Integer fromLevel, Integer toLevel, Integer fromSubLevel, Integer toSubLevel, Map<Integer, Map<Integer, StatSet>> info)
		{
			_name = name;
			_fromLevel = fromLevel;
			_toLevel = toLevel;
			_fromSubLevel = fromSubLevel;
			_toSubLevel = toSubLevel;
			_info = info;
		}
		
		public String getName()
		{
			return _name;
		}
		
		public Integer getFromLevel()
		{
			return _fromLevel;
		}
		
		public Integer getToLevel()
		{
			return _toLevel;
		}
		
		public Integer getFromSubLevel()
		{
			return _fromSubLevel;
		}
		
		public Integer getToSubLevel()
		{
			return _toSubLevel;
		}
		
		public Map<Integer, Map<Integer, StatSet>> getInfo()
		{
			return _info;
		}
	}
	
	/**
	 * Creates the {@code SkillData} singleton and loads the skill data.
	 */
	protected SkillData()
	{
		load();
	}
	
	/**
	 * Indicates whether XML validation is enabled.
	 * @return {@code false}; validation is always disabled.
	 */
	@Override
	public boolean isValidating()
	{
		return false;
	}
	
	/**
	 * Clears existing data and parses the default and custom skill directories.
	 */
	@Override
	public synchronized void load()
	{
		_skillsByHash.clear();
		_maxSkillLevels.clear();
		
		parseDatapackDirectory("data/stats/skills/", false);
		if (GeneralConfig.CUSTOM_SKILLS_LOAD)
		{
			parseDatapackDirectory("data/stats/skills/custom", false);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _skillsByHash.size() + " Skills.");
	}
	
	/**
	 * Reloads skill data and refreshes the {@code SkillTreeData} instance.
	 */
	public void reload()
	{
		load();
		
		// Reload Skill Tree as well.
		SkillTreeData.getInstance().load();
	}
	
	/**
	 * Parses every skill in the document, resolving levels, sublevels, effects and conditions.
	 * @param document The {@code Document} to parse.
	 * @param file The source {@code File}.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ("list".equalsIgnoreCase(node.getNodeName()))
			{
				for (Node listNode = node.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(listNode.getNodeName()))
					{
						NamedNodeMap attributes = listNode.getAttributes();
						final Map<Integer, Set<Integer>> levels = new HashMap<>();
						final Map<Integer, Map<Integer, StatSet>> skillInfo = new HashMap<>();
						final StatSet generalSkillInfo = skillInfo.computeIfAbsent(-1, _ -> new HashMap<>()).computeIfAbsent(-1, _ -> new StatSet());
						parseAttributes(attributes, "", generalSkillInfo);
						
						final Map<String, Map<Integer, Map<Integer, Object>>> variableValues = new HashMap<>();
						final Map<EffectScope, List<NamedParamInfo>> effectParamInfo = new EnumMap<>(EffectScope.class);
						final Map<SkillConditionScope, List<NamedParamInfo>> conditionParamInfo = new EnumMap<>(SkillConditionScope.class);
						for (Node skillNode = listNode.getFirstChild(); skillNode != null; skillNode = skillNode.getNextSibling())
						{
							final String skillNodeName = skillNode.getNodeName();
							switch (skillNodeName.toLowerCase())
							{
								case "variable":
								{
									attributes = skillNode.getAttributes();
									final String name = "@" + parseString(attributes, "name");
									variableValues.put(name, parseValues(skillNode));
									break;
								}
								case "#text":
								{
									break;
								}
								default:
								{
									final EffectScope effectScope = EffectScope.findByXmlNodeName(skillNodeName);
									if (effectScope != null)
									{
										for (Node effectsNode = skillNode.getFirstChild(); effectsNode != null; effectsNode = effectsNode.getNextSibling())
										{
											if ("effect".equalsIgnoreCase(effectsNode.getNodeName()))
											{
												effectParamInfo.computeIfAbsent(effectScope, _ -> new ArrayList<>()).add(parseNamedParamInfo(effectsNode, variableValues));
											}
										}
										break;
									}
									
									final SkillConditionScope skillConditionScope = SkillConditionScope.findByXmlNodeName(skillNodeName);
									if (skillConditionScope != null)
									{
										for (Node conditionNode = skillNode.getFirstChild(); conditionNode != null; conditionNode = conditionNode.getNextSibling())
										{
											if ("condition".equalsIgnoreCase(conditionNode.getNodeName()))
											{
												conditionParamInfo.computeIfAbsent(skillConditionScope, _ -> new ArrayList<>()).add(parseNamedParamInfo(conditionNode, variableValues));
											}
										}
									}
									else
									{
										parseInfo(skillNode, variableValues, skillInfo);
									}
									break;
								}
							}
						}
						
						final int fromLevel = generalSkillInfo.getInt(".fromLevel", 1);
						final int toLevel = generalSkillInfo.getInt(".toLevel", 0);
						for (int i = fromLevel; i <= toLevel; i++)
						{
							levels.computeIfAbsent(i, _ -> new HashSet<>()).add(0);
						}
						
						skillInfo.forEach((level, subLevelMap) ->
						{
							if (level == -1)
							{
								return;
							}
							
							subLevelMap.forEach((subLevel, _) ->
							{
								if (subLevel == -1)
								{
									return;
								}
								
								levels.computeIfAbsent(level, _ -> new HashSet<>()).add(subLevel);
							});
						});
						
						Stream.concat(effectParamInfo.values().stream(), conditionParamInfo.values().stream()).forEach(namedParamInfos -> namedParamInfos.forEach(namedParamInfo ->
						{
							namedParamInfo.getInfo().forEach((level, subLevelMap) ->
							{
								if (level == -1)
								{
									return;
								}
								
								subLevelMap.forEach((subLevel, _) ->
								{
									if (subLevel == -1)
									{
										return;
									}
									
									levels.computeIfAbsent(level, _ -> new HashSet<>()).add(subLevel);
								});
							});
							
							// Handle level/sublevel combinations from namedParamInfo.
							// The _info map already contains all explicit level/sublevel combinations.
							namedParamInfo.getInfo().forEach((level, subLevelMap) ->
							{
								if (level == -1)
								{
									return;
								}
								
								subLevelMap.forEach((subLevel, _) ->
								{
									if (subLevel == -1)
									{
										return;
									}
									
									levels.computeIfAbsent(level, _ -> new HashSet<>()).add(subLevel);
								});
							});
							
							// Also handle standard level/sublevel ranges if provided.
							final Integer npFromLevel = namedParamInfo.getFromLevel();
							final Integer npToLevel = namedParamInfo.getToLevel();
							if ((npFromLevel != null) && (npToLevel != null))
							{
								final Integer npFromSubLevel = namedParamInfo.getFromSubLevel();
								final Integer npToSubLevel = namedParamInfo.getToSubLevel();
								for (int i = npFromLevel; i <= npToLevel; i++)
								{
									if ((npFromSubLevel != null) && (npToSubLevel != null))
									{
										// Standard level and sublevel ranges.
										for (int j = npFromSubLevel; j <= npToSubLevel; j++)
										{
											levels.computeIfAbsent(i, _ -> new HashSet<>()).add(j);
										}
									}
									else if (!namedParamInfo.getInfo().containsKey(i))
									{
										// Level range without sublevel range, and not already in _info - use default sublevel.
										levels.computeIfAbsent(i, _ -> new HashSet<>()).add(0);
									}
								}
							}
						}));
						
						levels.forEach((level, subLevels) -> subLevels.forEach(subLevel ->
						{
							final StatSet statSet = Optional.ofNullable(skillInfo.getOrDefault(level, Collections.emptyMap()).get(subLevel)).orElseGet(StatSet::new);
							final Map<String, Object> statSetMap = statSet.getSet();
							skillInfo.getOrDefault(level, Collections.emptyMap()).getOrDefault(-1, StatSet.EMPTY_STATSET).getSet().forEach(statSetMap::putIfAbsent);
							skillInfo.getOrDefault(-1, Collections.emptyMap()).getOrDefault(-1, StatSet.EMPTY_STATSET).getSet().forEach(statSetMap::putIfAbsent);
							statSet.set(".level", level);
							statSet.set(".subLevel", subLevel);
							final Skill skill = new Skill(statSet);
							forEachNamedParamInfoParam(effectParamInfo, level, subLevel, ((effectScope, params) ->
							{
								final String effectName = params.getString(".name");
								params.remove(".name");
								try
								{
									final Function<StatSet, AbstractEffect> effectFunction = EffectHandler.getInstance().getHandlerFactory(effectName);
									if (effectFunction != null)
									{
										skill.addEffect(effectScope, effectFunction.apply(params));
									}
									else
									{
										LOGGER.warning(getClass().getSimpleName() + ": Missing effect for Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "] Effect Scope[" + effectScope + "] Effect Name[" + effectName + "]");
									}
								}
								catch (Exception e)
								{
									LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading effect for Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "] Effect Scope[" + effectScope + "] Effect Name[" + effectName + "]", e);
								}
							}));
							
							forEachNamedParamInfoParam(conditionParamInfo, level, subLevel, ((skillConditionScope, params) ->
							{
								final String conditionName = params.getString(".name");
								params.remove(".name");
								try
								{
									final Function<StatSet, ISkillCondition> conditionFunction = SkillConditionHandler.getInstance().getHandlerFactory(conditionName);
									if (conditionFunction != null)
									{
										if (skill.isPassive())
										{
											if (skillConditionScope != SkillConditionScope.PASSIVE)
											{
												LOGGER.warning(getClass().getSimpleName() + ": Non passive condition for passive Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "]");
											}
										}
										else if (skillConditionScope == SkillConditionScope.PASSIVE)
										{
											LOGGER.warning(getClass().getSimpleName() + ": Passive condition for non passive Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "]");
										}
										
										skill.addCondition(skillConditionScope, conditionFunction.apply(params));
									}
									else
									{
										LOGGER.warning(getClass().getSimpleName() + ": Missing condition for Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "] Effect Scope[" + skillConditionScope + "] Effect Name[" + conditionName + "]");
									}
								}
								catch (Exception e)
								{
									LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Failed loading condition for Skill Id[" + statSet.getInt(".id") + "] Level[" + level + "] SubLevel[" + subLevel + "] Condition Scope[" + skillConditionScope + "] Condition Name[" + conditionName + "]", e);
								}
							}));
							
							_skillsByHash.put(getSkillHashCode(skill), skill);
							final Integer skillIdLocal = skill.getId();
							final Integer skillLevelLocal = skill.getLevel();
							final int skillSubLevel = skill.getSubLevel();
							_maxSkillLevels.merge(skillIdLocal, skillLevelLocal, Integer::max);
							if ((skillSubLevel % 1000) == 1)
							{
								EnchantSkillGroupsData.getInstance().addRouteForSkill(skillIdLocal, skillLevelLocal, skillSubLevel);
							}
						}));
					}
				}
			}
		}
	}
	
	/**
	 * Checks whether the level and sublevel match the parameter info by explicit entry or range.
	 * @param namedParamInfo The {@code NamedParamInfo} to check.
	 * @param level The current level.
	 * @param subLevel The current sublevel.
	 * @return {@code true} if matched, {@code false} otherwise.
	 */
	private boolean matchesLevelAndSubLevel(NamedParamInfo namedParamInfo, int level, int subLevel)
	{
		// First check if this exact level/sublevel combination exists in _info.
		final Map<Integer, Map<Integer, StatSet>> info = namedParamInfo.getInfo();
		final Map<Integer, StatSet> subLevelMap = info.get(level);
		if ((subLevelMap != null) && subLevelMap.containsKey(subLevel))
		{
			return true;
		}
		
		// If not found in _info, check the range-based matching.
		final Integer fromLevel = namedParamInfo.getFromLevel();
		final Integer toLevel = namedParamInfo.getToLevel();
		final boolean levelMatches = ((fromLevel == null) && (toLevel == null)) || ((fromLevel != null) && (toLevel != null) && (fromLevel <= level) && (toLevel >= level));
		if (!levelMatches)
		{
			return false;
		}
		
		// Check sublevel match within range.
		return ((namedParamInfo.getFromSubLevel() == null) && (namedParamInfo.getToSubLevel() == null)) || ((namedParamInfo.getFromSubLevel() != null) && (namedParamInfo.getToSubLevel() != null) && (namedParamInfo.getFromSubLevel() <= subLevel) && (namedParamInfo.getToSubLevel() >= subLevel));
	}
	
	/**
	 * Resolves a parameter value of a named entry without mutating its stored data.
	 * @param namedParamInfo The {@code NamedParamInfo} to read.
	 * @param level The current level.
	 * @param subLevel The current sublevel.
	 * @param key The parameter key to resolve.
	 * @return The resolved value, {@code null} when the parameter is not present.
	 */
	private Object getNamedParamValue(NamedParamInfo namedParamInfo, int level, int subLevel, String key)
	{
		final Map<Integer, Map<Integer, StatSet>> info = namedParamInfo.getInfo();
		final Map<Integer, StatSet> levelMap = info.getOrDefault(level, Collections.emptyMap());
		final Map<String, Object> exactSet = levelMap.getOrDefault(subLevel, StatSet.EMPTY_STATSET).getSet();
		if (exactSet.containsKey(key))
		{
			return exactSet.get(key);
		}
		
		final Map<String, Object> levelDefaultSet = levelMap.getOrDefault(-1, StatSet.EMPTY_STATSET).getSet();
		if (levelDefaultSet.containsKey(key))
		{
			return levelDefaultSet.get(key);
		}
		
		final Map<String, Object> globalDefaultSet = info.getOrDefault(-1, Collections.emptyMap()).getOrDefault(-1, StatSet.EMPTY_STATSET).getSet();
		if (globalDefaultSet.containsKey(key))
		{
			return globalDefaultSet.get(key);
		}
		
		return null;
	}
	
	/**
	 * Checks whether an enchanted entry replaces a generic entry of the same effect slot.
	 * @param current The generic effect entry.
	 * @param candidate The sublevel specific effect entry.
	 * @param level The current level.
	 * @param subLevel The current sublevel.
	 * @return {@code true} if the candidate replaces the current entry, {@code false} otherwise.
	 */
	private boolean isMatchingEnchantedEffectReplacement(NamedParamInfo current, NamedParamInfo candidate, int level, int subLevel)
	{
		final String effectName = current.getName();
		if (!effectName.equalsIgnoreCase(candidate.getName()))
		{
			return false;
		}
		
		// Summon variants replace the base summon even when the enchanted NPC id changes.
		if ("Summon".equalsIgnoreCase(effectName))
		{
			return true;
		}
		
		// Cubic enchants replace the base cubic with the same cubic id and enhanced power.
		if ("SummonCubic".equalsIgnoreCase(effectName))
		{
			final Object currentCubicId = getNamedParamValue(current, level, subLevel, "cubicId");
			final Object candidateCubicId = getNamedParamValue(candidate, level, subLevel, "cubicId");
			return (currentCubicId != null) && (candidateCubicId != null) && String.valueOf(currentCubicId).equals(String.valueOf(candidateCubicId));
		}
		
		// StatModify enchants carry the final enchanted value, adding the base value on top doubles the bonus.
		// Match by stat so unrelated StatModify entries of the same skill stay active.
		if ("StatModify".equalsIgnoreCase(effectName))
		{
			final Object currentStat = getNamedParamValue(current, level, subLevel, "stat");
			final Object candidateStat = getNamedParamValue(candidate, level, subLevel, "stat");
			return (currentStat != null) && (candidateStat != null) && String.valueOf(currentStat).equalsIgnoreCase(String.valueOf(candidateStat));
		}
		
		return false;
	}
	
	/**
	 * Checks whether a generic entry has a matching enchanted sublevel replacement.
	 * @param namedParamInfos All named parameter entries of the current scope.
	 * @param current The currently evaluated entry.
	 * @param level The current level.
	 * @param subLevel The current sublevel.
	 * @return {@code true} if the generic entry must be skipped, {@code false} otherwise.
	 */
	private boolean hasMatchingEnchantedEffectOverride(List<NamedParamInfo> namedParamInfos, NamedParamInfo current, int level, int subLevel)
	{
		if (subLevel <= 0)
		{
			return false;
		}
		
		// Only a generic entry is suppressed, a sublevel specific entry is already the replacement.
		if ((current.getFromSubLevel() != null) || (current.getToSubLevel() != null))
		{
			return false;
		}
		
		final String effectName = current.getName();
		if (!"Summon".equalsIgnoreCase(effectName) && !"SummonCubic".equalsIgnoreCase(effectName) && !"StatModify".equalsIgnoreCase(effectName))
		{
			return false;
		}
		
		for (NamedParamInfo candidate : namedParamInfos)
		{
			if (candidate == current)
			{
				continue;
			}
			
			// The replacement must explicitly target one or more sublevels.
			if ((candidate.getFromSubLevel() == null) && (candidate.getToSubLevel() == null))
			{
				continue;
			}
			
			if (matchesLevelAndSubLevel(candidate, level, subLevel) && isMatchingEnchantedEffectReplacement(current, candidate, level, subLevel))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Applies the consumer to each parameter entry matching the level and sublevel, merging defaults.
	 * @param <T> The scope key type.
	 * @param paramInfo The map of scopes to {@code NamedParamInfo} lists.
	 * @param level The current level.
	 * @param subLevel The current sublevel.
	 * @param consumer The {@code BiConsumer} processing the scope and resulting {@code StatSet}.
	 */
	private <T> void forEachNamedParamInfoParam(Map<T, List<NamedParamInfo>> paramInfo, int level, int subLevel, BiConsumer<T, StatSet> consumer)
	{
		paramInfo.forEach((scope, namedParamInfos) -> namedParamInfos.forEach(namedParamInfo ->
		{
			// Check if we have a match with level and sublevel ranges or table values.
			if (matchesLevelAndSubLevel(namedParamInfo, level, subLevel))
			{
				// An enchanted variant may declare a generic base entry plus a sublevel specific replacement.
				// Applying both stacks the base value on top of the enchanted one.
				if (hasMatchingEnchantedEffectOverride(namedParamInfos, namedParamInfo, level, subLevel))
				{
					return;
				}
				
				final Map<Integer, Map<Integer, StatSet>> info = namedParamInfo.getInfo();
				final Map<Integer, StatSet> levelMap = info.getOrDefault(level, Collections.emptyMap());
				final StatSet params = Optional.ofNullable(levelMap.get(subLevel)).orElseGet(StatSet::new);
				final Map<String, Object> paramsMap = params.getSet();
				levelMap.getOrDefault(-1, StatSet.EMPTY_STATSET).getSet().forEach(paramsMap::putIfAbsent);
				info.getOrDefault(-1, Collections.emptyMap()).getOrDefault(-1, StatSet.EMPTY_STATSET).getSet().forEach(paramsMap::putIfAbsent);
				params.set(".name", namedParamInfo.getName());
				consumer.accept(scope, params);
			}
		}));
	}
	
	/**
	 * Extracts named parameter info from a node, resolving level and sublevel ranges and value lists.
	 * @param node The {@code Node} to parse.
	 * @param variableValues The map of global variable values.
	 * @return A new {@code NamedParamInfo}.
	 */
	private NamedParamInfo parseNamedParamInfo(Node node, Map<String, Map<Integer, Map<Integer, Object>>> variableValues)
	{
		Node n = node;
		final NamedNodeMap attributes = n.getAttributes();
		final String name = parseString(attributes, "name");
		
		// Standard level attributes.
		final Integer level = parseInteger(attributes, "level");
		Integer fromLevel = parseInteger(attributes, "fromLevel", level);
		Integer toLevel = parseInteger(attributes, "toLevel", level);
		
		// Standard sublevel attributes.
		final Integer subLevel = parseInteger(attributes, "subLevel");
		Integer fromSubLevel = parseInteger(attributes, "fromSubLevel", subLevel);
		Integer toSubLevel = parseInteger(attributes, "toSubLevel", subLevel);
		
		// New table attributes for multiple levels/sublevels in one line.
		final String levelValuesStr = parseString(attributes, "levelValues");
		
		// Create the info map to store parameter data.
		final Map<Integer, Map<Integer, StatSet>> info = new HashMap<>();
		
		// Parse child nodes to extract parameter information.
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if (!n.getNodeName().equals("#text"))
			{
				parseInfo(n, variableValues, info);
			}
		}
		
		// Check for numbered subLevel*Values attributes.
		Integer[] parsedSubLevelValues = null;
		int subLevelCategory = 0;
		for (int i = 0; i < attributes.getLength(); i++)
		{
			final Node item = attributes.item(i);
			final String attrName = item.getNodeName();
			if (SUB_LEVEL_VALUES_PATTERN.matcher(attrName).matches())
			{
				// Extract category number.
				subLevelCategory = extractCategoryNumber(attrName);
				
				// Found a numbered subLevel attribute, parse it.
				final String subLevelValuesStr = item.getNodeValue();
				if ((subLevelValuesStr != null) && !subLevelValuesStr.isEmpty())
				{
					final String[] subLevelValues = subLevelValuesStr.trim().split("\\s+");
					parsedSubLevelValues = new Integer[subLevelValues.length];
					
					for (int j = 0; j < subLevelValues.length; j++)
					{
						try
						{
							parsedSubLevelValues[j] = Integer.parseInt(subLevelValues[j]);
						}
						catch (NumberFormatException e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": Invalid number format in " + attrName + ": " + subLevelValues[j]);
							parsedSubLevelValues = null;
							break;
						}
					}
				}
				break; // Only handle the first subLevel*Values attribute found.
			}
		}
		
		// If levelValues attribute is present, we'll use that instead of fromLevel/toLevel ranges.
		if ((levelValuesStr != null) && !levelValuesStr.isEmpty())
		{
			// Parse the space-separated values.
			final String[] levelValueStrings = levelValuesStr.trim().split("\\s+");
			final List<Integer> parsedLevelValues = new ArrayList<>();
			
			// Convert to integers.
			for (String valueStr : levelValueStrings)
			{
				try
				{
					parsedLevelValues.add(Integer.parseInt(valueStr));
				}
				catch (NumberFormatException e)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Invalid number format in levelValues: " + valueStr);
					// Return with standard level attributes as fallback.
					return new NamedParamInfo(name, fromLevel, toLevel, fromSubLevel, toSubLevel, info);
				}
			}
			
			// Determine the actual skill levels to use.
			// If we have fromLevel and toLevel, use that range.
			// Otherwise, assume levels start at 1.
			final int startLevel = (fromLevel != null) ? fromLevel : 1;
			final int endLevel = (toLevel != null) ? toLevel : ((startLevel + parsedLevelValues.size()) - 1);
			
			// Validate we have enough values for the level range.
			final int expectedCount = (endLevel - startLevel) + 1;
			final int currentParsedSize = parsedLevelValues.size();
			if (currentParsedSize != expectedCount)
			{
				LOGGER.warning(getClass().getSimpleName() + ": levelValues count (" + currentParsedSize + ") does not match level range " + startLevel + "-" + endLevel + " (" + expectedCount + " expected)");
				return new NamedParamInfo(name, fromLevel, toLevel, fromSubLevel, toSubLevel, info);
			}
			
			// Map each value to its corresponding level.
			for (int i = 0; i < currentParsedSize; i++)
			{
				final int actualLevel = startLevel + i;
				final Integer value = parsedLevelValues.get(i);
				
				// If we have subLevel*Values, populate all sublevel combinations.
				if (parsedSubLevelValues != null)
				{
					for (int j = 0; j < parsedSubLevelValues.length; j++)
					{
						final int actualSubLevel = (subLevelCategory * 1000) + j + 1;
						
						// Store the value for this level/sublevel combination.
						final StatSet statSet = new StatSet();
						statSet.set(name, value);
						info.computeIfAbsent(actualLevel, _ -> new HashMap<>()).put(actualSubLevel, statSet);
					}
					
					// Update sublevel range.
					fromSubLevel = (subLevelCategory * 1000) + 1;
					toSubLevel = (subLevelCategory * 1000) + parsedSubLevelValues.length;
				}
				else
				{
					// No sublevels, use default sublevel 0.
					final StatSet statSet = new StatSet();
					statSet.set(name, value);
					info.computeIfAbsent(actualLevel, _ -> new HashMap<>()).put(0, statSet);
					
					// Set default sublevel.
					if (fromSubLevel == null)
					{
						fromSubLevel = 0;
					}
					if (toSubLevel == null)
					{
						toSubLevel = 0;
					}
				}
			}
			
			// Update level range to match what we actually populated.
			fromLevel = startLevel;
			toLevel = endLevel;
		}
		else if (parsedSubLevelValues != null)
		{
			// We have subLevel*Values but no levelValues, use standard level range.
			// Populate info with level range and sublevel values.
			if ((fromLevel != null) && (toLevel != null))
			{
				for (int i = fromLevel; i <= toLevel; i++)
				{
					for (int j = 0; j < parsedSubLevelValues.length; j++)
					{
						// Calculate actual sublevel based on category.
						final int actualSubLevel = (subLevelCategory * 1000) + j + 1;
						
						// Add this level/sublevel combination to info if not already present.
						info.computeIfAbsent(i, _ -> new HashMap<>()).putIfAbsent(actualSubLevel, new StatSet());
					}
				}
			}
			
			// Update fromSubLevel and toSubLevel based on category.
			fromSubLevel = (subLevelCategory * 1000) + 1;
			toSubLevel = (subLevelCategory * 1000) + parsedSubLevelValues.length;
		}
		
		// Return with calculated from/to values.
		return new NamedParamInfo(name, fromLevel, toLevel, fromSubLevel, toSubLevel, info);
	}
	
	/**
	 * Parses a node into the info map, handling table values and resolving variable references.
	 * @param node The {@code Node} to parse.
	 * @param variableValues The map of global variable definitions.
	 * @param info The nested map where parsed {@code StatSet} data is stored.
	 */
	private void parseInfo(Node node, Map<String, Map<Integer, Map<Integer, Object>>> variableValues, Map<Integer, Map<Integer, StatSet>> info)
	{
		final NamedNodeMap attributes = node.getAttributes();
		final String nodeName = node.getNodeName();
		
		// Check for levelValues attribute.
		final String levelValuesStr = parseString(attributes, "levelValues");
		
		// Check for subLevel*Values attributes.
		String subLevelValuesStr = null;
		int subLevelCategory = 0;
		
		for (int i = 0; i < attributes.getLength(); i++)
		{
			final String attrName = attributes.item(i).getNodeName();
			if (SUB_LEVEL_VALUES_PATTERN.matcher(attrName).matches())
			{
				subLevelValuesStr = attributes.item(i).getNodeValue();
				subLevelCategory = extractCategoryNumber(attrName);
				break; // Use first one found.
			}
		}
		
		// If we have either levelValues or subLevel*Values, use table parsing.
		if (((levelValuesStr != null) && !levelValuesStr.isEmpty()) || ((subLevelValuesStr != null) && !subLevelValuesStr.isEmpty()))
		{
			// Get the parent skill's level range.
			Node skillNode = node.getParentNode();
			while ((skillNode != null) && !skillNode.getNodeName().equals("skill"))
			{
				skillNode = skillNode.getParentNode();
			}
			
			if (skillNode == null)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Could not find parent skill node for " + nodeName);
				return;
			}
			
			final NamedNodeMap skillAttrs = skillNode.getAttributes();
			final Integer skillFromLevel = parseInteger(skillAttrs, "fromLevel", parseInteger(skillAttrs, "level", 1));
			final Integer skillToLevel = parseInteger(skillAttrs, "toLevel", parseInteger(skillAttrs, "level", skillFromLevel));
			
			// Parse level values if present.
			final List<Integer> levelList = new ArrayList<>();
			final List<Object> levelValuesList = new ArrayList<>();
			if ((levelValuesStr != null) && !levelValuesStr.isEmpty())
			{
				final String[] valueStrings = levelValuesStr.trim().split("\\s+");
				for (int i = 0; i < valueStrings.length; i++)
				{
					final int level = skillFromLevel + i;
					if (level > skillToLevel)
					{
						break;
					}
					
					levelList.add(level);
					levelValuesList.add(StringUtil.parseValue(valueStrings[i]));
				}
			}
			else
			{
				// No levelValues, use full skill level range.
				for (int level = skillFromLevel; level <= skillToLevel; level++)
				{
					levelList.add(level);
				}
			}
			
			// Parse sublevel values if present.
			final List<Integer> subLevelList = new ArrayList<>();
			final List<Object> subLevelValuesList = new ArrayList<>();
			if ((subLevelValuesStr != null) && !subLevelValuesStr.isEmpty())
			{
				final String[] valueStrings = subLevelValuesStr.trim().split("\\s+");
				for (int j = 0; j < valueStrings.length; j++)
				{
					final int subLevel = (subLevelCategory * 1000) + j + 1;
					subLevelList.add(subLevel);
					subLevelValuesList.add(StringUtil.parseValue(valueStrings[j]));
				}
			}
			else
			{
				// No subLevelValues, use default sublevel 0.
				subLevelList.add(0);
			}
			
			// Now populate the info map with all combinations.
			if (!levelValuesList.isEmpty())
			{
				// We have levelValues - use those values.
				for (int i = 0; i < levelList.size(); i++)
				{
					final int level = levelList.get(i);
					final Object value = levelValuesList.get(i);
					for (int subLevel : subLevelList)
					{
						info.computeIfAbsent(level, _ -> new HashMap<>()).computeIfAbsent(subLevel, _ -> new StatSet()).set(nodeName, value);
					}
				}
			}
			else if (!subLevelValuesList.isEmpty())
			{
				// We have subLevelValues but no levelValues - use sublevel values.
				for (int level : levelList)
				{
					for (int j = 0; j < subLevelList.size(); j++)
					{
						final int subLevel = subLevelList.get(j);
						final Object value = subLevelValuesList.get(j);
						info.computeIfAbsent(level, _ -> new HashMap<>()).computeIfAbsent(subLevel, _ -> new StatSet()).set(nodeName, value);
					}
				}
			}
			else
			{
				// Neither has values? This shouldn't happen but handle it.
				for (int level : levelList)
				{
					for (int subLevel : subLevelList)
					{
						info.computeIfAbsent(level, _ -> new HashMap<>()).computeIfAbsent(subLevel, _ -> new StatSet());
					}
				}
			}
			
			return; // Done with table parsing.
		}
		
		// Standard parsing for non-table attributes.
		Map<Integer, Map<Integer, Object>> values = parseValues(node);
		final Object generalValue = values.getOrDefault(-1, Collections.emptyMap()).get(-1);
		if (generalValue != null)
		{
			final String stringGeneralValue = String.valueOf(generalValue);
			if (stringGeneralValue.startsWith("@"))
			{
				final Map<Integer, Map<Integer, Object>> variableValue = variableValues.get(stringGeneralValue);
				if (variableValue != null)
				{
					values = variableValue;
				}
				else
				{
					throw new IllegalArgumentException("undefined variable " + stringGeneralValue);
				}
			}
		}
		
		values.forEach((level, subLevelMap) -> subLevelMap.forEach((subLevel, value) -> info.computeIfAbsent(level, _ -> new HashMap<>()).computeIfAbsent(subLevel, _ -> new StatSet()).set(node.getNodeName(), value)));
	}
	
	/**
	 * Extracts the numeric category from a table name.
	 * @param tableName The table name.
	 * @return The category, or {@code 0} if extraction fails.
	 */
	private int extractCategoryNumber(String tableName)
	{
		try
		{
			// Extract digits from the middle of the string (after "subLevel" and before "Values").
			final String numberStr = tableName.replace("subLevel", "").replace("Values", "");
			if (!numberStr.isEmpty())
			{
				return Integer.parseInt(numberStr);
			}
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to extract category number from " + tableName);
		}
		
		return 0; // Return 0 if parsing fails.
	}
	
	/**
	 * Parses value nodes into a nested level/sublevel map.
	 * @param node The {@code Node} to parse.
	 * @return A nested {@code Map} of level and sublevel values.
	 */
	private Map<Integer, Map<Integer, Object>> parseValues(Node node)
	{
		Node n = node;
		final Map<Integer, Map<Integer, Object>> values = new HashMap<>();
		Object parsedValue = parseValue(n, true, false, Collections.emptyMap());
		if (parsedValue != null)
		{
			values.computeIfAbsent(-1, _ -> new HashMap<>()).put(-1, parsedValue);
		}
		else
		{
			for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
			{
				if (n.getNodeName().equalsIgnoreCase("value"))
				{
					final NamedNodeMap attributes = n.getAttributes();
					final Integer level = parseInteger(attributes, "level");
					if (level != null)
					{
						parsedValue = parseValue(n, false, false, Collections.emptyMap());
						if (parsedValue != null)
						{
							final Integer subLevel = parseInteger(attributes, "subLevel", -1);
							values.computeIfAbsent(level, _ -> new HashMap<>()).put(subLevel, parsedValue);
						}
					}
					else
					{
						final int fromLevel = parseInteger(attributes, "fromLevel");
						final int toLevel = parseInteger(attributes, "toLevel");
						final int fromSubLevel = parseInteger(attributes, "fromSubLevel", -1);
						final int toSubLevel = parseInteger(attributes, "toSubLevel", -1);
						for (int i = fromLevel; i <= toLevel; i++)
						{
							for (int j = fromSubLevel; j <= toSubLevel; j++)
							{
								final Map<Integer, Object> subValues = values.computeIfAbsent(i, _ -> new HashMap<>());
								final Map<String, Double> variables = new HashMap<>();
								variables.put("index", (i - fromLevel) + 1d);
								variables.put("subIndex", (j - fromSubLevel) + 1d);
								final Object base = values.getOrDefault(i, Collections.emptyMap()).get(-1);
								final String baseText = String.valueOf(base);
								if ((base != null) && !(base instanceof StatSet) && (!baseText.equalsIgnoreCase("true") && !baseText.equalsIgnoreCase("false")))
								{
									variables.put("base", Double.parseDouble(baseText));
								}
								
								parsedValue = parseValue(n, false, false, variables);
								if (parsedValue != null)
								{
									subValues.put(j, parsedValue);
								}
							}
						}
					}
				}
			}
		}
		
		return values;
	}
	
	/**
	 * Recursively parses a node into an object from its attributes, text and nested items.
	 * @param node The {@code Node} to parse.
	 * @param blockValue {@code true} to skip the value tag.
	 * @param parseAttributes {@code true} to parse attributes.
	 * @param variables The map of variable values.
	 * @return The parsed object, which may be a {@code StatSet}, a {@code List} or a {@code String}.
	 */
	protected Object parseValue(Node node, boolean blockValue, boolean parseAttributes, Map<String, Double> variables)
	{
		Node n = node;
		StatSet statSet = null;
		List<Object> list = null;
		Object text = null;
		if (parseAttributes && (!n.getNodeName().equals("value") || !blockValue) && (n.getAttributes().getLength() > 0))
		{
			statSet = new StatSet();
			parseAttributes(n.getAttributes(), "", statSet, variables);
		}
		
		for (n = n.getFirstChild(); n != null; n = n.getNextSibling())
		{
			final String nodeName = n.getNodeName();
			switch (nodeName)
			{
				case "#text":
				{
					final String value = n.getNodeValue().trim();
					if (!value.isEmpty())
					{
						text = parseNodeValue(value, variables);
					}
					break;
				}
				case "item":
				{
					if (list == null)
					{
						list = new ArrayList<>();
					}
					
					final Object value = parseValue(n, false, true, variables);
					if (value != null)
					{
						list.add(value);
					}
					break;
				}
				case "value":
				{
					if (blockValue)
					{
						break;
					}
					// Fallthrough.
				}
				default:
				{
					final Object value = parseValue(n, false, true, variables);
					if (value != null)
					{
						if (statSet == null)
						{
							statSet = new StatSet();
						}
						
						statSet.set(nodeName, value);
					}
					break;
				}
			}
		}
		
		if (list != null)
		{
			if (text != null)
			{
				throw new IllegalArgumentException("Text and list in same node are not allowed. Node[" + n + "]");
			}
			
			if (statSet != null)
			{
				statSet.set(".", list);
			}
			else
			{
				return list;
			}
		}
		
		if (text != null)
		{
			if (list != null)
			{
				throw new IllegalArgumentException("Text and list in same node are not allowed. Node[" + n + "]");
			}
			
			if (statSet != null)
			{
				statSet.set(".", text);
			}
			else
			{
				return text;
			}
		}
		
		return statSet;
	}
	
	/**
	 * Parses node attributes into a {@code StatSet} using the prefix.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param prefix The attribute key prefix.
	 * @param statSet The target {@code StatSet}.
	 */
	private void parseAttributes(NamedNodeMap attributes, String prefix, StatSet statSet)
	{
		parseAttributes(attributes, prefix, statSet, Collections.emptyMap());
	}
	
	/**
	 * Parses node attributes into a {@code StatSet}, prefixing keys and resolving variables.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param prefix The attribute key prefix.
	 * @param statSet The target {@code StatSet}.
	 * @param variables The map of variable values.
	 */
	private void parseAttributes(NamedNodeMap attributes, String prefix, StatSet statSet, Map<String, Double> variables)
	{
		for (int i = 0; i < attributes.getLength(); i++)
		{
			final Node attributeNode = attributes.item(i);
			statSet.set(prefix + "." + attributeNode.getNodeName(), parseNodeValue(attributeNode.getNodeValue(), variables));
		}
	}
	
	/**
	 * Parses a string value, evaluating it as an expression when wrapped in braces.
	 * @param value The string value.
	 * @param variables The map of variable values.
	 * @return The parsed object, or the original string.
	 */
	private Object parseNodeValue(String value, Map<String, Double> variables)
	{
		if (value.startsWith("{") && value.endsWith("}"))
		{
			final String expression = value.substring(1, value.length() - 1).trim();
			if (expression.isEmpty())
			{
				throw new IllegalArgumentException("Empty expression inside {}.");
			}
			
			return evaluateExpression(expression, variables);
		}
		
		return value;
	}
	
	/**
	 * Evaluates a mathematical expression by converting it to postfix.
	 * @param expression The expression.
	 * @param variables The map of variable values.
	 * @return The result as a {@code double}.
	 */
	private double evaluateExpression(String expression, Map<String, Double> variables)
	{
		final String postfix = toPostfix(expression, variables);
		return evaluatePostfix(postfix);
	}
	
	/**
	 * Converts an infix expression to postfix, substituting variables.
	 * @param expression The infix expression.
	 * @param variables The map of variable values.
	 * @return The postfix expression as a {@code String}.
	 */
	private String toPostfix(String expression, Map<String, Double> variables)
	{
		final Deque<String> operators = new ArrayDeque<>();
		final StringBuilder postfix = new StringBuilder();
		
		final StringTokenizer tokenizer = new StringTokenizer(expression, "+-*/() ", true);
		boolean expectNumber = true;
		
		while (tokenizer.hasMoreTokens())
		{
			String token = tokenizer.nextToken().trim();
			if (token.isEmpty())
			{
				continue;
			}
			
			final Object value = variables.get(token);
			if (value != null)
			{
				token = value.toString();
			}
			
			if (isNumeric(token))
			{
				postfix.append(token).append(' ');
				expectNumber = false;
			}
			else if (token.equals("-") && expectNumber)
			{
				final String nextToken = tokenizer.hasMoreTokens() ? tokenizer.nextToken().trim() : null;
				if ((nextToken != null) && isNumeric(nextToken))
				{
					postfix.append('-').append(nextToken).append(' ');
					expectNumber = false;
				}
				else
				{
					throw new IllegalArgumentException("Invalid syntax near '-' in expression.");
				}
			}
			else if (isOperator(token))
			{
				while (!operators.isEmpty() && (precedence(operators.peek()) >= precedence(token)))
				{
					postfix.append(operators.pop()).append(' ');
				}
				
				operators.push(token);
				expectNumber = true;
			}
			else if (token.equals("("))
			{
				operators.push(token);
				expectNumber = true;
			}
			else if (token.equals(")"))
			{
				while (!operators.isEmpty() && !operators.peek().equals("("))
				{
					postfix.append(operators.pop()).append(' ');
				}
				
				operators.pop();
				expectNumber = false;
			}
		}
		
		while (!operators.isEmpty())
		{
			postfix.append(operators.pop()).append(' ');
		}
		
		return postfix.toString().trim();
	}
	
	/**
	 * Evaluates a postfix expression.
	 * @param postfix The postfix expression.
	 * @return The result as a {@code double}.
	 */
	private double evaluatePostfix(String postfix)
	{
		final Deque<Double> stack = new ArrayDeque<>();
		final StringTokenizer tokenizer = new StringTokenizer(postfix);
		while (tokenizer.hasMoreTokens())
		{
			final String token = tokenizer.nextToken();
			if (isNumeric(token))
			{
				stack.push(Double.parseDouble(token));
			}
			else if (isOperator(token))
			{
				if (stack.size() < 2)
				{
					throw new IllegalStateException("Not enough operands for the operator: " + token);
				}
				
				final double b = stack.pop();
				final double a = stack.pop();
				switch (token)
				{
					case "+":
					{
						stack.push(a + b);
						break;
					}
					case "-":
					{
						stack.push(a - b);
						break;
					}
					case "*":
					{
						stack.push(a * b);
						break;
					}
					case "/":
					{
						stack.push(a / b);
						break;
					}
				}
			}
		}
		
		if (stack.size() != 1)
		{
			throw new IllegalStateException("The postfix expression did not evaluate to a single result.");
		}
		
		return stack.pop();
	}
	
	/**
	 * Checks whether the token parses as a number.
	 * @param token The token.
	 * @return {@code true} if numeric, {@code false} otherwise.
	 */
	private boolean isNumeric(String token)
	{
		try
		{
			Double.parseDouble(token);
			return true;
		}
		catch (NumberFormatException e)
		{
			return false;
		}
	}
	
	/**
	 * Checks whether the token is one of the {@code + - * /} operators.
	 * @param token The token.
	 * @return {@code true} if an operator, {@code false} otherwise.
	 */
	private boolean isOperator(String token)
	{
		return "+-*/".contains(token);
	}
	
	/**
	 * Retrieves the precedence of the operator.
	 * @param operator The operator.
	 * @return The precedence, or {@code -1} if unknown.
	 */
	private int precedence(String operator)
	{
		switch (operator)
		{
			case "+":
			case "-":
			{
				return 1;
			}
			case "*":
			case "/":
			{
				return 2;
			}
			default:
			{
				return -1;
			}
		}
	}
	
	/**
	 * Generates a hash code from the skill id, level and sublevel.
	 * @param skill The {@code Skill}.
	 * @return The hash code as a {@code long}.
	 */
	public static long getSkillHashCode(Skill skill)
	{
		return getSkillHashCode(skill.getId(), skill.getLevel(), skill.getSubLevel());
	}
	
	/**
	 * Generates a hash code for the skill id and level, defaulting the sublevel to {@code 0}.
	 * @param skillId The skill id.
	 * @param skillLevel The skill level.
	 * @return The hash code as a {@code long}.
	 */
	public static long getSkillHashCode(int skillId, int skillLevel)
	{
		return getSkillHashCode(skillId, skillLevel, 0);
	}
	
	/**
	 * Generates a hash code from the skill id, level and sublevel.
	 * @param skillId The skill id.
	 * @param skillLevel The skill level.
	 * @param subSkillLevel The skill sublevel.
	 * @return The hash code as a {@code long}.
	 */
	public static long getSkillHashCode(int skillId, int skillLevel, int subSkillLevel)
	{
		return (skillId * 4294967296L) + (subSkillLevel * 65536) + skillLevel;
	}
	
	/**
	 * Retrieves the skill for the id and level, defaulting the sublevel to {@code 0}.
	 * @param skillId The skill id.
	 * @param level The skill level.
	 * @return The matching {@code Skill}, or {@code null} if none.
	 */
	public Skill getSkill(int skillId, int level)
	{
		return getSkill(skillId, level, 0);
	}
	
	/**
	 * Retrieves the skill by id, level and sublevel, without any fallback or warning when there is no match.
	 * @param skillId The skill id.
	 * @param level The skill level.
	 * @param subLevel The skill sublevel.
	 * @return The matching {@code Skill}, or {@code null} if none.
	 */
	public Skill getSkillQuietly(int skillId, int level, int subLevel)
	{
		return _skillsByHash.get(getSkillHashCode(skillId, level, subLevel));
	}
	
	/**
	 * Retrieves the skill by id, level and sublevel, falling back to the max level when the level is too high.
	 * @param skillId The skill id.
	 * @param level The skill level.
	 * @param subLevel The skill sublevel.
	 * @return The matching {@code Skill}, or {@code null} if none.
	 */
	public Skill getSkill(int skillId, int level, int subLevel)
	{
		final Skill result = _skillsByHash.get(getSkillHashCode(skillId, level, subLevel));
		if (result != null)
		{
			return result;
		}
		
		// Skill/level not found, fix for transformation scripts.
		final int maxLevel = getMaxLevel(skillId);
		
		// Requested level too high.
		if ((maxLevel > 0) && (level > maxLevel))
		{
			LOGGER.warning(StringUtil.concat(getClass().getSimpleName(), ": Call to unexisting skill level id: ", String.valueOf(skillId), " requested level: ", String.valueOf(level), " max level: ", String.valueOf(maxLevel), ".", System.lineSeparator(), TraceUtil.getStackTrace(new Exception())));
			return _skillsByHash.get(getSkillHashCode(skillId, maxLevel));
		}
		
		LOGGER.warning(StringUtil.concat(getClass().getSimpleName(), ": No skill info found for skill id ", String.valueOf(skillId), " and skill level ", String.valueOf(level), ".", System.lineSeparator(), TraceUtil.getStackTrace(new Exception())));
		return null;
	}
	
	/**
	 * Retrieves the maximum level recorded for the skill.
	 * @param skillId The skill id.
	 * @return The maximum level, or {@code 0} if not found.
	 */
	public int getMaxLevel(int skillId)
	{
		final Integer maxLevel = _maxSkillLevels.get(skillId);
		return maxLevel != null ? maxLevel : 0;
	}
	
	/**
	 * Builds the list of siege skills, optionally including the advanced headquarters skill.
	 * @param addNoble {@code true} to include the advanced headquarters skill.
	 * @return A {@code List} of {@code Skill}.
	 */
	public List<Skill> getSiegeSkills(boolean addNoble)
	{
		final List<Skill> result = new ArrayList<>();
		result.add(_skillsByHash.get(getSkillHashCode(CommonSkill.IMPRIT_OF_LIGHT.getId(), 1)));
		result.add(_skillsByHash.get(getSkillHashCode(CommonSkill.IMPRIT_OF_DARKNESS.getId(), 1)));
		result.add(_skillsByHash.get(getSkillHashCode(247, 1))); // Build Headquarters
		if (addNoble)
		{
			result.add(_skillsByHash.get(getSkillHashCode(326, 1))); // Build Advanced Headquarters
		}
		
		// result.add(_skillsByHash.get(getSkillHashCode(15283, 1))); // Summon Jacquard
		// result.add(_skillsByHash.get(getSkillHashCode(15284, 1))); // Summon Brakel
		// result.add(_skillsByHash.get(getSkillHashCode(15285, 1))); // Summon Loken
		return result;
	}
	
	/**
	 * Retrieves the singleton {@code SkillData} instance.
	 * @return The single {@code SkillData} instance.
	 */
	public static SkillData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SkillData INSTANCE = new SkillData();
	}
}
