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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.SocialClass;
import org.l2jmobius.gameserver.entity.actor.enums.player.SubclassType;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class loads and manages the skill trees for characters and pledges from XML data.<br>
 * It handles various categories including class-specific, common, and clan-related skills.<br>
 * The {@code SkillTreeData} class allows for custom progression paths by defining parent IDs within the configuration.
 * @author Zoey76, Mobius
 */
public class SkillTreeData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SkillTreeData.class.getName());
	
	// ClassId, Map of Skill Hash Code, SkillLearn
	private static final Map<PlayerClass, Map<Long, SkillLearn>> _classSkillTrees = new ConcurrentHashMap<>();
	private static final Map<PlayerClass, Map<Long, SkillLearn>> _completeClassSkillTree = new HashMap<>();
	private static final Map<PlayerClass, Map<Long, SkillLearn>> _transferSkillTrees = new ConcurrentHashMap<>();
	private static final Map<Race, Map<Long, SkillLearn>> _raceSkillTree = new ConcurrentHashMap<>();
	private static final Map<SubclassType, Map<Long, SkillLearn>> _revelationSkillTree = new ConcurrentHashMap<>();
	private static final Map<PlayerClass, Set<Integer>> _awakeningSaveSkillTree = new ConcurrentHashMap<>();
	
	// Skill Hash Code, SkillLearn
	private static final Map<Long, SkillLearn> _collectSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _fishingSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _pledgeSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _subClassSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _subPledgeSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _transformSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _commonSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _abilitySkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _alchemySkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _dualClassSkillTree = new ConcurrentHashMap<>();
	
	// Other skill trees
	private static final Map<Long, SkillLearn> _nobleSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _heroSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _gameMasterSkillTree = new ConcurrentHashMap<>();
	private static final Map<Long, SkillLearn> _gameMasterAuraSkillTree = new ConcurrentHashMap<>();
	
	// Remove skill tree
	private static final Map<PlayerClass, Set<Integer>> _removeSkillCache = new ConcurrentHashMap<>();
	
	// Checker, sorted arrays of hash codes
	private Map<Integer, long[]> _skillsByClassIdHashCodes; // Occupation skills
	private Map<Integer, long[]> _skillsByRaceHashCodes; // Race-specific Transformations
	private long[] _allSkillsHashCodes; // Fishing, Collection, Transformations, Common Skills.
	
	/** Parent class Ids are read from XML and stored in this map, to allow easy customization. */
	private static final Map<PlayerClass, PlayerClass> _parentClassMap = new ConcurrentHashMap<>();
	
	private boolean _loading = true;
	
	/**
	 * Initializes a new instance of the {@code SkillTreeData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected SkillTreeData()
	{
		load();
	}
	
	/**
	 * Loads the skill tree data from the configuration files.<br>
	 * This method clears all existing caches and parses the datapack directory.<br>
	 * It also generates the complete class skill trees and check arrays.
	 */
	@Override
	public void load()
	{
		_loading = true;
		_parentClassMap.clear();
		_classSkillTrees.clear();
		_collectSkillTree.clear();
		_fishingSkillTree.clear();
		_pledgeSkillTree.clear();
		_subClassSkillTree.clear();
		_subPledgeSkillTree.clear();
		_transferSkillTrees.clear();
		_transformSkillTree.clear();
		_nobleSkillTree.clear();
		_abilitySkillTree.clear();
		_alchemySkillTree.clear();
		_heroSkillTree.clear();
		_gameMasterSkillTree.clear();
		_gameMasterAuraSkillTree.clear();
		_raceSkillTree.clear();
		_revelationSkillTree.clear();
		_dualClassSkillTree.clear();
		_removeSkillCache.clear();
		_awakeningSaveSkillTree.clear();
		
		// Load files.
		parseDatapackDirectory("data/stats/players/skillTrees/", true);
		
		// Cache the complete class skill trees.
		_completeClassSkillTree.clear();
		for (Entry<PlayerClass, Map<Long, SkillLearn>> entry : _classSkillTrees.entrySet())
		{
			final Map<Long, SkillLearn> skillTree = new HashMap<>();
			
			// Add all skills that belong to all classes.
			skillTree.putAll(_commonSkillTree);
			final PlayerClass entryPlayerClass = entry.getKey();
			PlayerClass currentPlayerClass = entryPlayerClass;
			while ((currentPlayerClass != null) && (_classSkillTrees.get(currentPlayerClass) != null))
			{
				skillTree.putAll(_classSkillTrees.get(currentPlayerClass));
				currentPlayerClass = _parentClassMap.get(currentPlayerClass);
			}
			
			_completeClassSkillTree.put(entryPlayerClass, skillTree);
		}
		
		// Generate check arrays.
		generateCheckArrays();
		
		// Logs a report with skill trees info.
		report();
		
		_loading = false;
	}
	
	/**
	 * Parses the skill tree data from an XML document.<br>
	 * This method reads the {@code Document} and populates various internal skill maps.<br>
	 * It identifies different types of trees such as class, race, and transfer skills.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		Node attr;
		String type = null;
		Race race = null;
		SubclassType subType = null;
		int cId = -1;
		int parentClassId = -1;
		PlayerClass playerClass = null;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skillTree".equalsIgnoreCase(d.getNodeName()))
					{
						final Map<Long, SkillLearn> classSkillTree = new HashMap<>();
						final Map<Long, SkillLearn> transferSkillTree = new HashMap<>();
						final Map<Long, SkillLearn> raceSkillTree = new HashMap<>();
						final Map<Long, SkillLearn> revelationSkillTree = new HashMap<>();
						final NamedNodeMap dAttrs = d.getAttributes();
						type = dAttrs.getNamedItem("type").getNodeValue();
						attr = dAttrs.getNamedItem("classId");
						if (attr != null)
						{
							cId = Integer.parseInt(attr.getNodeValue());
							playerClass = PlayerClass.getPlayerClass(cId);
						}
						else
						{
							cId = -1;
						}
						
						attr = dAttrs.getNamedItem("race");
						if (attr != null)
						{
							race = parseEnum(attr, Race.class);
						}
						
						attr = dAttrs.getNamedItem("subType");
						if (attr != null)
						{
							subType = parseEnum(attr, SubclassType.class);
						}
						
						attr = dAttrs.getNamedItem("parentClassId");
						if (attr != null)
						{
							parentClassId = Integer.parseInt(attr.getNodeValue());
							if ((cId > -1) && (cId != parentClassId) && (parentClassId > -1) && !_parentClassMap.containsKey(playerClass))
							{
								_parentClassMap.put(playerClass, PlayerClass.getPlayerClass(parentClassId));
							}
						}
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							if ("skill".equalsIgnoreCase(c.getNodeName()))
							{
								final StatSet learnSkillSet = new StatSet();
								attrs = c.getAttributes();
								for (int i = 0; i < attrs.getLength(); i++)
								{
									attr = attrs.item(i);
									learnSkillSet.set(attr.getNodeName(), attr.getNodeValue());
								}
								
								final SkillLearn skillLearn = new SkillLearn(learnSkillSet);
								
								// Test if skill exists.
								SkillData.getInstance().getSkill(skillLearn.getSkillId(), skillLearn.getSkillLevel());
								for (Node b = c.getFirstChild(); b != null; b = b.getNextSibling())
								{
									attrs = b.getAttributes();
									switch (b.getNodeName())
									{
										case "item":
										{
											final List<ItemHolder> itemList = new ArrayList<>(1);
											final int count = parseInteger(attrs, "count");
											for (String id : parseString(attrs, "id").split(","))
											{
												itemList.add(new ItemHolder(Integer.parseInt(id), count));
											}
											
											skillLearn.addRequiredItem(itemList);
											break;
										}
										case "preRequisiteSkill":
										{
											skillLearn.addPreReqSkill(new SkillHolder(parseInteger(attrs, "id"), parseInteger(attrs, "lvl")));
											break;
										}
										case "race":
										{
											skillLearn.addRace(Race.valueOf(b.getTextContent()));
											break;
										}
										case "residenceId":
										{
											skillLearn.addResidenceId(Integer.parseInt(b.getTextContent()));
											break;
										}
										case "socialClass":
										{
											skillLearn.setSocialClass(Enum.valueOf(SocialClass.class, b.getTextContent()));
											break;
										}
										case "removeSkill":
										{
											final int removeSkillId = parseInteger(attrs, "id");
											skillLearn.addRemoveSkills(removeSkillId);
											if (!parseBoolean(attrs, "onlyReplaceByLearn", false).booleanValue())
											{
												_removeSkillCache.computeIfAbsent(playerClass, _ -> new HashSet<>()).add(removeSkillId);
											}
											break;
										}
									}
								}
								
								final long skillHashCode = SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel());
								switch (type)
								{
									case "classSkillTree":
									{
										if (cId != -1)
										{
											classSkillTree.put(skillHashCode, skillLearn);
										}
										else
										{
											_commonSkillTree.put(skillHashCode, skillLearn);
										}
										break;
									}
									case "transferSkillTree":
									{
										transferSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "collectSkillTree":
									{
										_collectSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "raceSkillTree":
									{
										raceSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "revelationSkillTree":
									{
										revelationSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "fishingSkillTree":
									{
										_fishingSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "pledgeSkillTree":
									{
										_pledgeSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "subClassSkillTree":
									{
										_subClassSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "subPledgeSkillTree":
									{
										_subPledgeSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "transformSkillTree":
									{
										_transformSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "nobleSkillTree":
									{
										_nobleSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "abilitySkillTree":
									{
										_abilitySkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "alchemySkillTree":
									{
										_alchemySkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "heroSkillTree":
									{
										_heroSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "gameMasterSkillTree":
									{
										_gameMasterSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "gameMasterAuraSkillTree":
									{
										_gameMasterAuraSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "dualClassSkillTree":
									{
										_dualClassSkillTree.put(skillHashCode, skillLearn);
										break;
									}
									case "awakeningSaveSkillTree":
									{
										_awakeningSaveSkillTree.computeIfAbsent(playerClass, _ -> new HashSet<>()).add(skillLearn.getSkillId());
										break;
									}
									default:
									{
										LOGGER.warning(getClass().getSimpleName() + ": Unknown Skill Tree type: " + type + "!");
									}
								}
							}
						}
						
						if (type.equals("transferSkillTree"))
						{
							_transferSkillTrees.put(playerClass, transferSkillTree);
						}
						else if (type.equals("classSkillTree") && (cId > -1))
						{
							final Map<Long, SkillLearn> classSkillTrees = _classSkillTrees.get(playerClass);
							if (classSkillTrees == null)
							{
								_classSkillTrees.put(playerClass, classSkillTree);
							}
							else
							{
								classSkillTrees.putAll(classSkillTree);
							}
						}
						else if (type.equals("raceSkillTree") && (race != null))
						{
							final Map<Long, SkillLearn> raceSkillTrees = _raceSkillTree.get(race);
							if (raceSkillTrees == null)
							{
								_raceSkillTree.put(race, raceSkillTree);
							}
							else
							{
								raceSkillTrees.putAll(raceSkillTree);
							}
						}
						else if (type.equals("revelationSkillTree") && (subType != null))
						{
							final Map<Long, SkillLearn> revelationSkillTrees = _revelationSkillTree.get(subType);
							if (revelationSkillTrees == null)
							{
								_revelationSkillTree.put(subType, revelationSkillTree);
							}
							else
							{
								revelationSkillTrees.putAll(revelationSkillTree);
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the full skill tree for a specific player class.<br>
	 * This method returns all skills associated with the provided {@code PlayerClass}.<br>
	 * If no data exists for the class, it returns an empty map.
	 * @param playerClass The {@code PlayerClass} to look up.
	 * @return A {@code Map} where keys are skill IDs and values are {@code SkillLearn} objects.
	 */
	public Map<Long, SkillLearn> getCompleteClassSkillTree(PlayerClass playerClass)
	{
		return _completeClassSkillTree.getOrDefault(playerClass, Collections.emptyMap());
	}
	
	/**
	 * Retrieves the skill tree associated with a specific player class.<br>
	 * This method returns the transfer skills for the provided {@code PlayerClass}.
	 * @param playerClass The {@code PlayerClass} to look up.
	 * @return A {@code Map} containing the skill data, or {@code null} if not found.
	 */
	public Map<Long, SkillLearn> getTransferSkillTree(PlayerClass playerClass)
	{
		return _transferSkillTrees.get(playerClass);
	}
	
	/**
	 * Retrieves the skill tree associated with a specific race.<br>
	 * This method returns all skills that belong to the provided {@code Race}.<br>
	 * If no skills are found for the race, it returns an empty collection.
	 * @param race The {@code Race} type to look up in the skill tree.
	 * @return A {@code Collection} of {@code SkillLearn} objects for the given race.
	 */
	public Collection<SkillLearn> getRaceSkillTree(Race race)
	{
		return _raceSkillTree.containsKey(race) ? _raceSkillTree.get(race).values() : Collections.emptyList();
	}
	
	/**
	 * Retrieves the skill tree for common skills.<br>
	 * This map contains all skills shared across different classes.
	 * @return a {@code Map} where the key is the skill ID and the value is a {@code SkillLearn} object.
	 */
	public Map<Long, SkillLearn> getCommonSkillTree()
	{
		return _commonSkillTree;
	}
	
	/**
	 * Retrieves the collection skill tree data.<br>
	 * This method returns all skills associated with the collection category.
	 * @return A {@code Map} where the key is a {@code Long} ID and the value is a {@code SkillLearn} object.
	 */
	public Map<Long, SkillLearn> getCollectSkillTree()
	{
		return _collectSkillTree;
	}
	
	/**
	 * Retrieves the skill tree for fishing.<br>
	 * This method returns a map of all skills associated with the fishing category.
	 * @return A {@code Map} where the key is the skill ID and the value is a {@code SkillLearn} object.
	 */
	public Map<Long, SkillLearn> getFishingSkillTree()
	{
		return _fishingSkillTree;
	}
	
	/**
	 * Retrieves the skill tree associated with the Pledge system.<br>
	 * The skills are mapped by their unique identifier.
	 * @return a {@code Map} containing the pledge skills and their learn data.
	 */
	public Map<Long, SkillLearn> getPledgeSkillTree()
	{
		return _pledgeSkillTree;
	}
	
	/**
	 * Retrieves the skill tree associated with subclasses.<br>
	 * This method returns a map of all subclass skills.
	 * @return A {@code Map} where the key is the skill ID and the value is a {@code SkillLearn} object.
	 */
	public Map<Long, SkillLearn> getSubClassSkillTree()
	{
		return _subClassSkillTree;
	}
	
	/**
	 * Retrieves the skill tree for sub-pledge classes.<br>
	 * This method returns a map of skills associated with specific sub-pledges.
	 * @return A {@code Map} where the key is a {@code Long} ID and the value is a {@code SkillLearn} object.
	 */
	public Map<Long, SkillLearn> getSubPledgeSkillTree()
	{
		return _subPledgeSkillTree;
	}
	
	/**
	 * Retrieves the skill tree associated with transformations.<br>
	 * This method returns a map of skills for transformation types.
	 * @return A {@code Map} where the key is a {@code Long} ID and the value is a {@code SkillLearn} object.
	 */
	public Map<Long, SkillLearn> getTransformSkillTree()
	{
		return _transformSkillTree;
	}
	
	/**
	 * Retrieves the skill tree associated with abilities.<br>
	 * This method returns a map of all ability skills.
	 * @return A {@code Map} where the key is a {@code Long} ID and the value is a {@code SkillLearn} object.
	 */
	public Map<Long, SkillLearn> getAbilitySkillTree()
	{
		return _abilitySkillTree;
	}
	
	/**
	 * Retrieves the skill tree for alchemy.<br>
	 * This method returns a map of all skills associated with the alchemy category.
	 * @return A {@code Map} where the key is the skill ID and the value is a {@code SkillLearn} object.
	 */
	public Map<Long, SkillLearn> getAlchemySkillTree()
	{
		return _alchemySkillTree;
	}
	
	/**
	 * Retrieves the list of skills belonging to the noble skill tree.<br>
	 * This method iterates through all noble skill entries and fetches their data.
	 * @return a {@code List} of {@code Skill} objects.
	 */
	public List<Skill> getNobleSkillTree()
	{
		final List<Skill> result = new ArrayList<>();
		for (SkillLearn skill : _nobleSkillTree.values())
		{
			result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of noble skills that are automatically acquired.<br>
	 * This method filters the {@code _nobleSkillTree} for entries where {@code isAutoGet()} is true.<br>
	 * It returns the corresponding {@code Skill} objects from the {@code SkillData} instance.
	 * @return A {@code List} of {@code Skill} objects that are automatically granted to nobles.
	 */
	public List<Skill> getNobleSkillAutoGetTree()
	{
		final List<Skill> result = new ArrayList<>();
		for (SkillLearn skill : _nobleSkillTree.values())
		{
			if (skill.isAutoGet())
			{
				result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of skills for the hero skill tree.<br>
	 * This method iterates through all learned skills in the hero tree.<br>
	 * It returns a list of {@code Skill} objects based on their IDs and levels.
	 * @return A {@code List} of {@code Skill} objects belonging to the hero tree.
	 */
	public List<Skill> getHeroSkillTree()
	{
		final List<Skill> result = new ArrayList<>();
		for (SkillLearn skill : _heroSkillTree.values())
		{
			result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of skills for the Game Master skill tree.<br>
	 * This method iterates through all {@code SkillLearn} entries in the internal map.<br>
	 * It fetches each corresponding {@code Skill} object using its ID and level.
	 * @return A {@code List} containing all {@code Skill} objects in the GMSkillTree.
	 */
	public List<Skill> getGMSkillTree()
	{
		final List<Skill> result = new ArrayList<>();
		for (SkillLearn skill : _gameMasterSkillTree.values())
		{
			result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of skills for the Game Master Aura tree.<br>
	 * This method iterates through all defined {@code SkillLearn} entries in the aura tree.<br>
	 * It returns a list of {@code Skill} objects based on their IDs and levels.
	 * @return A {@code List} containing all {@code Skill} objects for the Game Master Aura.
	 */
	public List<Skill> getGMAuraSkillTree()
	{
		final List<Skill> result = new ArrayList<>();
		for (SkillLearn skill : _gameMasterAuraSkillTree.values())
		{
			result.add(SkillData.getInstance().getSkill(skill.getSkillId(), skill.getSkillLevel()));
		}
		
		return result;
	}
	
	/**
	 * Checks if the {@code Player} can learn any new skills for their {@code PlayerClass}.<br>
	 * It evaluates all available skills in the class tree.<br>
	 * It ignores skills that are auto-obtained, learned by followers, or above the player's level.<br>
	 * It returns {@code true} if at least one skill is ready to be learned.
	 * @param player The {@code Player} object to check.
	 * @param playerClass The {@code PlayerClass} of the character.
	 * @return {@code true} if a new skill is available, otherwise {@code false}.
	 */
	public boolean hasAvailableSkills(Player player, PlayerClass playerClass)
	{
		final Map<Long, SkillLearn> skills = getCompleteClassSkillTree(playerClass);
		for (SkillLearn skill : skills.values())
		{
			final int skillId = skill.getSkillId();
			if ((skillId == CommonSkill.DIVINE_INSPIRATION.getId()) || skill.isAutoGet() || skill.isLearnedByFS() || (skill.getGetLevel() > player.getLevel()))
			{
				continue;
			}
			
			final Skill oldSkill = player.getKnownSkill(skillId);
			if ((oldSkill != null) && (oldSkill.getLevel() == (skill.getSkillLevel() - 1)))
			{
				return true;
			}
			else if ((oldSkill == null) && (skill.getSkillLevel() == 1))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves a list of skills that the specified {@code Player} is allowed to learn.<br>
	 * This method filters skills based on class requirements and various configuration flags.
	 * @param player The {@code Player} object used to check current skill levels.
	 * @param playerClass The {@code PlayerClass} of the character.
	 * @param includeByFs Whether to include skills granted by specific factors.
	 * @param includeByFp Whether to include skills granted by other specific factors.
	 * @param includeAutoGet Whether to include skills that are automatically acquired.
	 * @return A {@code Collection} of {@code SkillLearn} objects representing available skills.
	 */
	public Collection<SkillLearn> getAvailableSkills(Player player, PlayerClass playerClass, boolean includeByFs, boolean includeByFp, boolean includeAutoGet)
	{
		return getAvailableSkills(player, playerClass, includeByFs, includeByFp, includeAutoGet, true, player.getSkills());
	}
	
	/**
	 * Retrieves a list of skills that the player is currently able to learn.<br>
	 * This method filters the class skill tree based on various requirements and flags.<br>
	 * It also handles logic for auto-learned skills, item requirements, and skill replacements.
	 * @param player The {@code Player} object representing the current character.
	 * @param playerClass The {@code PlayerClass} of the character.
	 * @param includeByFs Whether to include skills learned by Forgotten Scrolls.
	 * @param includeByFp Whether to include skills learned by Forgotten Powers.
	 * @param includeAutoGet Whether to include skills that are automatically granted.
	 * @param includeRequiredItems Whether to include skills that require specific items.
	 * @param existingSkills A map of the {@code Skill} IDs and their current levels for the player.
	 * @return A {@code Collection} of {@code SkillLearn} objects available for learning.
	 */
	private Collection<SkillLearn> getAvailableSkills(Player player, PlayerClass playerClass, boolean includeByFs, boolean includeByFp, boolean includeAutoGet, boolean includeRequiredItems, Map<Integer, Skill> existingSkills)
	{
		final Set<SkillLearn> result = new LinkedHashSet<>();
		final Map<Long, SkillLearn> skills = getCompleteClassSkillTree(playerClass);
		if (skills.isEmpty())
		{
			// The Skill Tree for this class is undefined.
			LOGGER.warning(getClass().getSimpleName() + ": Skilltree for class " + playerClass + " is not defined!");
			return result;
		}
		
		final boolean isAwaken = player.isInCategory(CategoryType.SIXTH_CLASS_GROUP) && ((player.getRace() != Race.ERTHEIA) || player.isDualClassActive());
		for (Entry<Long, SkillLearn> entry : skills.entrySet())
		{
			final SkillLearn skill = entry.getValue();
			final int skillId = skill.getSkillId();
			if (((skillId == CommonSkill.DIVINE_INSPIRATION.getId()) && (!PlayerConfig.AUTO_LEARN_DIVINE_INSPIRATION && includeAutoGet) && !player.isGM()) || (!includeAutoGet && skill.isAutoGet()) || (!includeByFs && skill.isLearnedByFS()) || (!includeByFp && (skillId > 11399) && (skillId < 11405)) || isRemoveSkill(playerClass, skillId))
			{
				continue;
			}
			
			// Forgotten Scroll and Forgotten Power requirements checked above.
			if (!includeRequiredItems && !skill.getRequiredItems().isEmpty() && !skill.isLearnedByFS() && ((skillId < 11400) || (skillId > 11404)))
			{
				continue;
			}
			
			if (isAwaken && !isCurrentClassSkillNoParent(playerClass, entry.getKey()) && !isAwakenSaveSkill(player.getPlayerClass(), skillId))
			{
				continue;
			}
			
			if (player.getLevel() >= skill.getGetLevel())
			{
				final int skillLevel = skill.getSkillLevel();
				if (skillLevel > SkillData.getInstance().getMaxLevel(skillId))
				{
					LOGGER.severe(getClass().getSimpleName() + ": SkillTreesData found learnable skill " + skillId + " with level higher than max skill level!");
					continue;
				}
				
				final Skill oldSkill = existingSkills.get(skillId);
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skillLevel - 1))
					{
						result.add(skill);
					}
				}
				else if (skillLevel == 1)
				{
					result.add(skill);
				}
			}
		}
		
		// Manage skill unlearn for player skills.
		for (Skill knownSkill : player.getSkillList())
		{
			final int knownId = knownSkill.getId();
			final int knownLevel = knownSkill.getLevel();
			SkillLearn skillLearn = getClassSkill(player.getOriginalSkill(knownId), knownLevel, playerClass);
			if (skillLearn == null)
			{
				continue;
			}
			
			Set<Integer> removeSkills = skillLearn.getRemoveSkills();
			if (removeSkills.isEmpty())
			{
				if (knownLevel > 1)
				{
					// Check first skill level for removed skills.
					skillLearn = getClassSkill(knownId, 1, playerClass);
					if (skillLearn == null)
					{
						continue;
					}
					
					removeSkills = skillLearn.getRemoveSkills();
					if (removeSkills.isEmpty())
					{
						continue;
					}
				}
				else
				{
					continue;
				}
			}
			
			for (int removeId : removeSkills)
			{
				SEARCH: for (SkillLearn knownLearn : result)
				{
					if (knownLearn.getSkillId() == removeId)
					{
						result.remove(knownLearn);
						break SEARCH;
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all skills that a player is currently able to learn.<br>
	 * This method recursively checks for new skills that become available after learning others.<br>
	 * It also handles the removal of skills if specific requirements are no longer met.
	 * @param player The {@code Player} object representing the current character.
	 * @param playerClass The {@code PlayerClass} of the character.
	 * @param includeByFs Whether to include skills based on Force stats.
	 * @param includeByFp Whether to include skills based on Prowess stats.
	 * @param includeAutoGet Whether to include skills that are automatically granted.
	 * @param includeRequiredItems Whether to check for required items to learn a skill.
	 * @return A {@code Collection} of all available {@code Skill} objects.
	 */
	public Collection<Skill> getAllAvailableSkills(Player player, PlayerClass playerClass, boolean includeByFs, boolean includeByFp, boolean includeAutoGet, boolean includeRequiredItems)
	{
		final Map<Integer, Skill> result = new HashMap<>();
		for (Skill skill : player.getSkills().values())
		{
			// Adding only skills that can be learned by the player.
			if (isSkillAllowed(player, skill))
			{
				result.put(skill.getId(), skill);
			}
		}
		
		// Keep learning skills until no more are available.
		final Set<Integer> removed = new HashSet<>();
		Collection<SkillLearn> learnableSkills;
		Collection<SkillLearn> previousLearnableSkills = null;
		while (true)
		{
			learnableSkills = getAvailableSkills(player, playerClass, includeByFs, includeByFp, includeAutoGet, includeRequiredItems, result);
			if (learnableSkills.isEmpty())
			{
				break;
			}
			
			// Break if no new skills became learnable since last iteration.
			if (learnableSkills.equals(previousLearnableSkills))
			{
				break;
			}
			
			// All remaining skills have been removed.
			boolean allRemoved = true;
			SEARCH: for (SkillLearn skillLearn : learnableSkills)
			{
				if (!removed.contains(skillLearn.getSkillId()))
				{
					allRemoved = false;
					break SEARCH;
				}
			}
			
			if (allRemoved)
			{
				break;
			}
			
			for (SkillLearn skillLearn : learnableSkills)
			{
				// Cleanup skills that has to be removed.
				for (int skillId : skillLearn.getRemoveSkills())
				{
					// Mark skill as removed, so it doesn't gets added.
					removed.add(skillId);
					
					// Remove skill from player's skill list or existing skill list.
					final Skill playerSkillToRemove = player.getKnownSkill(skillId);
					final Skill holderSkillToRemove = result.get(skillId);
					
					// If player has the skill remove it.
					if (playerSkillToRemove != null)
					{
						player.removeSkill(playerSkillToRemove);
					}
					
					// If result already contains the skill remove it.
					if (holderSkillToRemove != null)
					{
						result.remove(skillId);
					}
				}
				
				final int currentSkillLearnId = skillLearn.getSkillId();
				if (!removed.contains(currentSkillLearnId))
				{
					final Skill skill = SkillData.getInstance().getSkill(currentSkillLearnId, skillLearn.getSkillLevel());
					result.put(skill.getId(), skill);
				}
			}
			
			// Store reference for convergence check.
			previousLearnableSkills = learnableSkills;
		}
		
		return result.values();
	}
	
	/**
	 * Retrieves a list of skills that the player can automatically learn.<br>
	 * This method checks class, race, and level requirements for auto-get skills.<br>
	 * It also handles skill removal logic based on current known skills.
	 * @param player The {@code Player} object to check for available skills.
	 * @return A {@code List} of {@code SkillLearn} objects that the player can currently acquire.
	 */
	public List<SkillLearn> getAvailableAutoGetSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final PlayerClass playerClass = player.getPlayerClass();
		final Map<Long, SkillLearn> skills = getCompleteClassSkillTree(playerClass);
		if (skills.isEmpty())
		{
			// The Skill Tree for this class is undefined, so we return an empty list.
			LOGGER.warning(getClass().getSimpleName() + ": Skill Tree for this class Id(" + playerClass + ") is not defined!");
			return result;
		}
		
		final Race race = player.getRace();
		final boolean isAwaken = player.isInCategory(CategoryType.SIXTH_CLASS_GROUP);
		
		// Race skills
		if (isAwaken)
		{
			for (SkillLearn skill : getRaceSkillTree(race))
			{
				if (player.getKnownSkill(skill.getSkillId()) == null)
				{
					result.add(skill);
				}
			}
		}
		
		for (SkillLearn skill : skills.values())
		{
			if (!skill.isAutoGet())
			{
				continue;
			}
			
			if ((player.getLevel() < skill.getGetLevel()))
			{
				continue;
			}
			
			final Set<Race> skillRaces = skill.getRaces();
			if (!skillRaces.isEmpty() && !skillRaces.contains(race))
			{
				continue;
			}
			
			final int skillId = skill.getSkillId();
			final Skill oldSkill = player.getKnownSkill(skillId);
			if (oldSkill != null)
			{
				if (oldSkill.getLevel() < skill.getSkillLevel())
				{
					result.add(skill);
				}
			}
			else
			{
				final int maxLevel = SkillData.getInstance().getMaxLevel(skillId);
				final long hashCode = SkillData.getSkillHashCode(skillId, maxLevel);
				if (!isAwaken || isCurrentClassSkillNoParent(playerClass, hashCode) || isAwakenSaveSkill(playerClass, skillId))
				{
					result.add(skill);
				}
			}
		}
		
		// Manage skill unlearn for player skills.
		for (Skill knownSkill : player.getSkillList())
		{
			final int knownId = knownSkill.getId();
			final int knownLevel = knownSkill.getLevel();
			SkillLearn skillLearn = getClassSkill(player.getOriginalSkill(knownId), knownLevel, playerClass);
			if (skillLearn == null)
			{
				continue;
			}
			
			Set<Integer> removeSkills = skillLearn.getRemoveSkills();
			if (removeSkills.isEmpty())
			{
				if (knownLevel > 1)
				{
					// Check first skill level for removed skills.
					skillLearn = getClassSkill(knownId, 1, playerClass);
					if (skillLearn == null)
					{
						continue;
					}
					
					removeSkills = skillLearn.getRemoveSkills();
					if (removeSkills.isEmpty())
					{
						continue;
					}
				}
				else
				{
					continue;
				}
			}
			
			for (int removeId : removeSkills)
			{
				SEARCH: for (SkillLearn knownLearn : result)
				{
					if (knownLearn.getSkillId() == removeId)
					{
						result.remove(knownLearn);
						break SEARCH;
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of fishing skills that the player is eligible to learn.<br>
	 * This method filters the global fishing skill tree based on the player's race and current level.<br>
	 * It also checks if the player meets the requirements for NPC-learned skills.
	 * @param player The {@code Player} object used to check race, level, and current skills.
	 * @return A {@code List} of {@code SkillLearn} objects available to the player.
	 */
	public List<SkillLearn> getAvailableFishingSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final Race playerRace = player.getRace();
		for (SkillLearn skill : _fishingSkillTree.values())
		{
			// If skill is Race specific and the player's race isn't allowed, skip it.
			final Set<Race> skillRaces = skill.getRaces();
			if (!skillRaces.isEmpty() && !skillRaces.contains(playerRace))
			{
				continue;
			}
			
			if (skill.isLearnedByNpc() && (player.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = player.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of revelation skills available for a specific player and subclass.<br>
	 * This method filters out skills that the {@code Player} already possesses.
	 * @param player The {@code Player} object to check for existing skills.
	 * @param type The {@code SubclassType} used to determine which skill tree to query.
	 * @return A {@code List} of {@code SkillLearn} objects that the player can still learn.
	 */
	public List<SkillLearn> getAvailableRevelationSkills(Player player, SubclassType type)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final Map<Long, SkillLearn> revelationSkills = _revelationSkillTree.get(type);
		for (SkillLearn skill : revelationSkills.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getSkillId());
			if (oldSkill == null)
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all revelation skills for a specific subclass.<br>
	 * This method returns the list of {@code SkillLearn} objects associated with the provided {@code SubclassType}.<br>
	 * If no skills are found, it returns an empty collection.
	 * @param player The {@code Player} object requesting the skills.
	 * @param type The {@code SubclassType} to filter the revelation skills by.
	 * @return A {@code Collection} of {@code SkillLearn} objects for the given subclass.
	 */
	public Collection<SkillLearn> getAllRevelationSkills(Player player, SubclassType type)
	{
		final Map<Long, SkillLearn> revelationSkills = _revelationSkillTree.get(type);
		return revelationSkills == null ? Collections.emptyList() : revelationSkills.values();
	}
	
	/**
	 * Retrieves a list of alchemy skills that the player is eligible to learn.<br>
	 * It checks if the player meets the required level and has the correct previous skill level.
	 * @param player The {@code Player} object used to check current levels and learned skills.
	 * @return A {@code List} of {@code SkillLearn} objects representing available alchemy skills.
	 */
	public List<SkillLearn> getAvailableAlchemySkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (SkillLearn skill : _alchemySkillTree.values())
		{
			if (skill.isLearnedByNpc() && (player.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = player.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of collect skills that a player can currently learn.<br>
	 * It checks if the player already knows the previous level or has no knowledge of the skill.
	 * @param player The {@code Player} object to check for available skills.
	 * @return A {@code List} of {@code SkillLearn} objects representing learnable skills.
	 */
	public List<SkillLearn> getAvailableCollectSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (SkillLearn skill : _collectSkillTree.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getSkillId());
			if (oldSkill != null)
			{
				if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
				{
					result.add(skill);
				}
			}
			else if (skill.getSkillLevel() == 1)
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of transfer skills available to a specific player.<br>
	 * It checks the skill tree associated with the player's {@code PlayerClass}.<br>
	 * Only skills that the player does not already know are included in the result.
	 * @param player The {@code Player} object used to check current known skills and class type.
	 * @return A {@code List} of {@code SkillLearn} objects representing available transfer skills.
	 */
	public List<SkillLearn> getAvailableTransferSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final PlayerClass playerClass = player.getPlayerClass();
		if (!_transferSkillTrees.containsKey(playerClass))
		{
			return result;
		}
		
		for (SkillLearn skill : _transferSkillTrees.get(playerClass).values())
		{
			// If player doesn't know this transfer skill:
			if (player.getKnownSkill(skill.getSkillId()) == null)
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of transform skills that the player is eligible to learn.<br>
	 * It checks if the player meets the required level and race requirements.<br>
	 * It also verifies if the player has already learned the previous skill level.
	 * @param player The {@code Player} object used to check current stats and skills.
	 * @return A {@code List} of {@code SkillLearn} objects available for the player.
	 */
	public List<SkillLearn> getAvailableTransformSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		final Race race = player.getRace();
		for (SkillLearn skill : _transformSkillTree.values())
		{
			final Set<Race> skillRaces = skill.getRaces();
			if ((player.getLevel() >= skill.getGetLevel()) && (skillRaces.isEmpty() || skillRaces.contains(race)))
			{
				final Skill oldSkill = player.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if (oldSkill.getLevel() == (skill.getSkillLevel() - 1))
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of skills available for a specific clan to learn.<br>
	 * It filters out residential skills and checks if the clan meets the required level.<br>
	 * Only skills that are ready to be learned based on current skill levels are included.
	 * @param clan The {@code Clan} object used to check requirements and current skills.
	 * @return A {@code List} of {@code SkillLearn} objects available for the clan.
	 */
	public List<SkillLearn> getAvailablePledgeSkills(Clan clan)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (!skill.isResidencialSkill() && (clan.getLevel() >= skill.getGetLevel()))
			{
				final Skill oldSkill = clan.getSkills().get(skill.getSkillId());
				if (oldSkill != null)
				{
					if ((oldSkill.getLevel() + 1) == skill.getSkillLevel())
					{
						result.add(skill);
					}
				}
				else if (skill.getSkillLevel() == 1)
				{
					result.add(skill);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the maximum pledge skills for a specific clan.<br>
	 * It checks if the clan meets the required level to learn each skill.<br>
	 * The method also considers sub-pledge skills if requested.
	 * @param clan The {@code Clan} object to check for skills.
	 * @param includeSquad Whether to include sub-pledge skills in the result.
	 * @return A map of skill IDs and their corresponding {@code SkillLearn} data.
	 */
	public Map<Integer, SkillLearn> getMaxPledgeSkills(Clan clan, boolean includeSquad)
	{
		final Map<Integer, SkillLearn> result = new HashMap<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (!skill.isResidencialSkill() && (clan.getLevel() >= skill.getGetLevel()))
			{
				final Integer skillId = skill.getSkillId();
				final Skill oldSkill = clan.getSkills().get(skillId);
				if ((oldSkill == null) || (oldSkill.getLevel() < skill.getSkillLevel()))
				{
					result.put(skillId, skill);
				}
			}
		}
		
		if (includeSquad)
		{
			for (SkillLearn skill : _subPledgeSkillTree.values())
			{
				if ((clan.getLevel() >= skill.getGetLevel()))
				{
					final Integer skillId = skill.getSkillId();
					final Skill oldSkill = clan.getSkills().get(skillId);
					if ((oldSkill == null) || (oldSkill.getLevel() < skill.getSkillLevel()))
					{
						result.put(skillId, skill);
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of sub-pledge skills available for a specific clan.<br>
	 * It checks if the {@code Clan} level meets the required skill level.<br>
	 * It also verifies if the skill is learnable based on current conditions.
	 * @param clan The {@code Clan} object to check for available skills.
	 * @return A {@code List} of {@code SkillLearn} objects that the clan can currently learn.
	 */
	public List<SkillLearn> getAvailableSubPledgeSkills(Clan clan)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (SkillLearn skill : _subPledgeSkillTree.values())
		{
			if ((clan.getLevel() >= skill.getGetLevel()) && clan.isLearnableSubSkill(skill.getSkillId(), skill.getSkillLevel()))
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of subclass skills that a player can currently learn.<br>
	 * It checks if the skill is at level 1 and not yet learned, or if the player has already learned the previous level.
	 * @param player The {@code Player} object to check for available skills.
	 * @return A {@code List} of {@code SkillLearn} objects that are ready to be learned.
	 */
	public List<SkillLearn> getAvailableSubClassSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (SkillLearn skill : _subClassSkillTree.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getSkillId());
			final int skillLevel = skill.getSkillLevel();
			if (((oldSkill == null) && (skillLevel == 1)) || ((oldSkill != null) && (oldSkill.getLevel() == (skillLevel - 1))))
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the list of dual class skills available for a specific player.<br>
	 * It checks if the player can learn the next level of each skill in the tree.<br>
	 * The results are sorted by their unique skill ID.
	 * @param player The {@code Player} object to check for skill eligibility.
	 * @return A {@code List} of {@code SkillLearn} objects that the player can currently learn.
	 */
	public List<SkillLearn> getAvailableDualClassSkills(Player player)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (SkillLearn skill : _dualClassSkillTree.values())
		{
			final Skill oldSkill = player.getSkills().get(skill.getSkillId());
			final int skillLevel = skill.getSkillLevel();
			if (((oldSkill == null) && (skillLevel == 1)) || ((oldSkill != null) && (oldSkill.getLevel() == (skillLevel - 1))))
			{
				result.add(skill);
			}
		}
		
		result.sort(Comparator.comparingInt(SkillLearn::getSkillId));
		return result;
	}
	
	/**
	 * Retrieves a list of residential skills for a specific residence.<br>
	 * This method filters the pledge skill tree to find skills that are marked as residential.<br>
	 * It checks if the provided {@code residenceId} is associated with each skill.
	 * @param residenceId The unique identifier of the residence to check.
	 * @return A {@code List} of {@code SkillLearn} objects matching the criteria.
	 */
	public List<SkillLearn> getAvailableResidentialSkills(int residenceId)
	{
		final List<SkillLearn> result = new ArrayList<>();
		for (SkillLearn skill : _pledgeSkillTree.values())
		{
			if (skill.isResidencialSkill() && skill.getResidenceIds().contains(residenceId))
			{
				result.add(skill);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a specific {@code SkillLearn} object based on the provided type and parameters.<br>
	 * This method identifies the correct skill tree category to search for the requested data.
	 * @param skillType The category of the skill to retrieve.
	 * @param id The unique identifier for the skill.
	 * @param lvl The level of the skill.
	 * @param player The {@code Player} instance used to determine class-specific requirements.
	 * @return The corresponding {@code SkillLearn} object or {@code null} if not found.
	 */
	public SkillLearn getSkillLearn(AcquireSkillType skillType, int id, int lvl, Player player)
	{
		SkillLearn sl = null;
		switch (skillType)
		{
			case CLASS:
			{
				sl = getClassSkill(id, lvl, player.getPlayerClass());
				break;
			}
			case TRANSFORM:
			{
				sl = getTransformSkill(id, lvl);
				break;
			}
			case FISHING:
			{
				sl = getFishingSkill(id, lvl);
				break;
			}
			case PLEDGE:
			{
				sl = getPledgeSkill(id, lvl);
				break;
			}
			case SUBPLEDGE:
			{
				sl = getSubPledgeSkill(id, lvl);
				break;
			}
			case TRANSFER:
			{
				sl = getTransferSkill(id, lvl, player.getPlayerClass());
				break;
			}
			case SUBCLASS:
			{
				sl = getSubClassSkill(id, lvl);
				break;
			}
			case COLLECT:
			{
				sl = getCollectSkill(id, lvl);
				break;
			}
			case REVELATION:
			{
				sl = getRevelationSkill(SubclassType.BASECLASS, id, lvl);
				break;
			}
			case REVELATION_DUALCLASS:
			{
				sl = getRevelationSkill(SubclassType.DUALCLASS, id, lvl);
				break;
			}
			case ALCHEMY:
			{
				sl = getAlchemySkill(id, lvl);
				break;
			}
			case DUALCLASS:
			{
				sl = getDualClassSkill(id, lvl);
				break;
			}
		}
		
		return sl;
	}
	
	/**
	 * Retrieves a specific transform skill based on its unique identifier and level.<br>
	 * This method looks up the data in the internal transformation skill tree map.
	 * @param id The unique identifier of the skill.
	 * @param lvl The current level of the skill.
	 * @return The {@code SkillLearn} object corresponding to the provided ID and level, or {@code null} if not found.
	 */
	private SkillLearn getTransformSkill(int id, int lvl)
	{
		return _transformSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific skill from the ability skill tree.<br>
	 * It uses the unique hash code generated by the provided ID and level.
	 * @param id The unique identifier for the skill.
	 * @param lvl The current level of the skill.
	 * @return The {@code SkillLearn} object corresponding to the given parameters, or {@code null} if not found.
	 */
	public SkillLearn getAbilitySkill(int id, int lvl)
	{
		return _abilitySkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific alchemy skill based on its unique identifier and level.<br>
	 * This method looks up the data in the internal alchemy skill tree map.
	 * @param id The unique identifier for the skill.
	 * @param lvl The current level of the skill.
	 * @return The {@code SkillLearn} object corresponding to the provided parameters, or {@code null} if not found.
	 */
	private SkillLearn getAlchemySkill(int id, int lvl)
	{
		return _alchemySkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific skill learn object based on the provided parameters.<br>
	 * This method looks up the skill within the complete class tree for the given {@code PlayerClass}.
	 * @param id The unique identifier of the skill.
	 * @param lvl The level of the skill.
	 * @param playerClass The class of the player.
	 * @return The {@code SkillLearn} object corresponding to the skill, or {@code null} if not found.
	 */
	public SkillLearn getClassSkill(int id, int lvl, PlayerClass playerClass)
	{
		return getCompleteClassSkillTree(playerClass).get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific fishing skill from the loaded tree.<br>
	 * It uses the unique hash code generated by the provided ID and level.
	 * @param id The unique identifier for the skill.
	 * @param lvl The level of the skill.
	 * @return The {@code SkillLearn} object corresponding to the input parameters.
	 */
	private SkillLearn getFishingSkill(int id, int lvl)
	{
		return _fishingSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific skill from the pledge skill tree.<br>
	 * It uses the provided ID and level to find the correct entry.
	 * @param id The unique identifier for the skill.
	 * @param lvl The level of the skill.
	 * @return The {@code SkillLearn} object associated with the given parameters, or {@code null} if not found.
	 */
	public SkillLearn getPledgeSkill(int id, int lvl)
	{
		return _pledgeSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific skill from the sub-pledge tree.<br>
	 * It uses the unique hash code generated by the provided ID and level.
	 * @param id The unique identifier for the skill.
	 * @param lvl The level of the skill.
	 * @return The {@code SkillLearn} object associated with the given parameters, or {@code null} if not found.
	 */
	public SkillLearn getSubPledgeSkill(int id, int lvl)
	{
		return _subPledgeSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific skill from the transfer tree for a given class.<br>
	 * This method checks if the {@code PlayerClass} has an associated transfer tree.<br>
	 * It then looks up the skill based on its unique hash code.
	 * @param id The unique identifier of the skill.
	 * @param lvl The level of the skill.
	 * @param playerClass The class of the player.
	 * @return The {@code SkillLearn} object if found, or {@code null} if it does not exist.
	 */
	private SkillLearn getTransferSkill(int id, int lvl, PlayerClass playerClass)
	{
		if (_transferSkillTrees.get(playerClass) != null)
		{
			return _transferSkillTrees.get(playerClass).get(SkillData.getSkillHashCode(id, lvl));
		}
		
		return null;
	}
	
	/**
	 * Retrieves a specific {@code SkillLearn} based on its ID and level for a given race.<br>
	 * This method searches through the entire race skill tree.<br>
	 * It returns {@code null} if no matching skill is found.
	 * @param id The unique identifier of the skill.
	 * @param lvl The specific level of the skill to retrieve.
	 * @param race The {@code Race} type associated with the skill tree.
	 * @return The matching {@code SkillLearn} object or {@code null}.
	 */
	private SkillLearn getRaceSkill(int id, int lvl, Race race)
	{
		for (SkillLearn skill : getRaceSkillTree(race))
		{
			if ((skill.getSkillId() == id) && (skill.getSkillLevel() == lvl))
			{
				return skill;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a specific skill from the subclass skill tree.<br>
	 * It uses the unique hash code generated by the provided ID and level.
	 * @param id The unique identifier for the skill.
	 * @param lvl The level of the skill.
	 * @return The {@code SkillLearn} object associated with the given parameters.
	 */
	private SkillLearn getSubClassSkill(int id, int lvl)
	{
		return _subClassSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific skill from the dual class skill tree.<br>
	 * It uses the unique hash code generated by the provided ID and level.
	 * @param id The unique identifier for the skill.
	 * @param lvl The level of the skill.
	 * @return The {@code SkillLearn} object associated with the given parameters, or {@code null} if not found.
	 */
	public SkillLearn getDualClassSkill(int id, int lvl)
	{
		return _dualClassSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific skill from the common skill tree.<br>
	 * It uses the unique hash code generated by the provided ID and level.
	 * @param id The unique identifier for the skill.
	 * @param lvl The level of the skill.
	 * @return The {@code SkillLearn} object associated with the given parameters, or {@code null} if not found.
	 */
	public SkillLearn getCommonSkill(int id, int lvl)
	{
		return _commonSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific skill from the collection tree.<br>
	 * This method looks up the skill using its unique hash code.
	 * @param id The unique identifier for the skill.
	 * @param lvl The level of the skill.
	 * @return The {@code SkillLearn} object corresponding to the provided ID and level.
	 */
	public SkillLearn getCollectSkill(int id, int lvl)
	{
		return _collectSkillTree.get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Retrieves a specific revelation skill based on the subclass and its unique identifier.<br>
	 * This method looks up the skill within the {@code _revelationSkillTree}.
	 * @param type The {@code SubclassType} of the character.
	 * @param id The unique identification number for the skill.
	 * @param lvl The level of the skill to retrieve.
	 * @return The corresponding {@code SkillLearn} object.
	 */
	public SkillLearn getRevelationSkill(SubclassType type, int id, int lvl)
	{
		return _revelationSkillTree.get(type).get(SkillData.getSkillHashCode(id, lvl));
	}
	
	/**
	 * Calculates the minimum level required to learn a new skill.<br>
	 * It checks all skills in the provided tree against the current player level.<br>
	 * The method returns the lowest required level that is currently higher than the player's level.
	 * @param player The {@code Player} object used to check the current character level.
	 * @param skillTree A {@code Map} containing all {@code SkillLearn} data for the tree.
	 * @return The minimum required level as an {@code int}. Returns 0 if no new skills are available.
	 */
	public int getMinLevelForNewSkill(Player player, Map<Long, SkillLearn> skillTree)
	{
		int minLevel = 0;
		if (skillTree.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": SkillTree is not defined for getMinLevelForNewSkill!");
		}
		else
		{
			final int playerLevel = player.getLevel();
			for (SkillLearn s : skillTree.values())
			{
				final int getLevel = s.getGetLevel();
				if ((playerLevel < getLevel) && ((minLevel == 0) || (minLevel > getLevel)))
				{
					minLevel = getLevel;
				}
			}
		}
		
		return minLevel;
	}
	
	/**
	 * Retrieves a collection of skills that the player is eligible to learn next.<br>
	 * This method filters skills based on level requirements and specific configuration flags.<br>
	 * It also handles logic for removing skills that are lost when others are learned.
	 * @param player The {@code Player} object representing the current character.
	 * @param playerClass The {@code PlayerClass} of the character.
	 * @param includeByFs Whether to include skills learned by FS.
	 * @param includeByFp Whether to include skills learned by FP.
	 * @param includeAutoGet Whether to include skills that are automatically acquired.
	 * @return A {@code Collection} of {@code SkillLearn} objects available for the player.
	 */
	public Collection<SkillLearn> getNextAvailableSkills(Player player, PlayerClass playerClass, boolean includeByFs, boolean includeByFp, boolean includeAutoGet)
	{
		final Map<Long, SkillLearn> completeClassSkillTree = getCompleteClassSkillTree(playerClass);
		final Set<SkillLearn> result = new LinkedHashSet<>();
		if (completeClassSkillTree.isEmpty())
		{
			return result;
		}
		
		final int minLevelForNewSkill = getMinLevelForNewSkill(player, completeClassSkillTree);
		if (minLevelForNewSkill > 0)
		{
			for (SkillLearn skill : completeClassSkillTree.values())
			{
				final int skillGetLevel = skill.getGetLevel();
				if (skillGetLevel > PlayerConfig.PLAYER_MAXIMUM_LEVEL)
				{
					continue;
				}
				
				if ((!includeAutoGet && skill.isAutoGet()) || (!includeByFs && skill.isLearnedByFS()) || (!includeByFp && (skill.getSkillId() > 11399) && (skill.getSkillId() < 11405)))
				{
					continue;
				}
				
				if (minLevelForNewSkill <= skillGetLevel)
				{
					final Skill oldSkill = player.getKnownSkill(skill.getSkillId());
					final int skillLevel = skill.getSkillLevel();
					if (oldSkill != null)
					{
						if (oldSkill.getLevel() == (skillLevel - 1))
						{
							result.add(skill);
						}
					}
					else if (skillLevel == 1)
					{
						result.add(skill);
					}
				}
			}
		}
		
		// Manage skill unlearn for player skills.
		for (Skill knownSkill : player.getSkillList())
		{
			final int knownId = knownSkill.getId();
			final int knownLevel = knownSkill.getLevel();
			SkillLearn skillLearn = getClassSkill(player.getOriginalSkill(knownId), knownLevel, playerClass);
			if (skillLearn == null)
			{
				continue;
			}
			
			Set<Integer> removeSkills = skillLearn.getRemoveSkills();
			if (removeSkills.isEmpty())
			{
				if (knownLevel > 1)
				{
					// Check first skill level for removed skills.
					skillLearn = getClassSkill(knownId, 1, playerClass);
					if (skillLearn == null)
					{
						continue;
					}
					
					removeSkills = skillLearn.getRemoveSkills();
					if (removeSkills.isEmpty())
					{
						continue;
					}
				}
				else
				{
					continue;
				}
			}
			
			for (int removeId : removeSkills)
			{
				SEARCH: for (SkillLearn knownLearn : result)
				{
					if (knownLearn.getSkillId() == removeId)
					{
						result.remove(knownLearn);
						break SEARCH;
					}
				}
			}
		}
		
		// Manage skill unlearn for player replaced skills.
		for (int skillId : player.getReplacedSkills())
		{
			final SkillLearn skillLearn = getClassSkill(skillId, 1, playerClass);
			if (skillLearn != null)
			{
				final Set<Integer> removeSkills = skillLearn.getRemoveSkills();
				if (removeSkills != null)
				{
					for (int removeId : removeSkills)
					{
						SEARCH: for (SkillLearn knownLearn : result)
						{
							if (knownLearn.getSkillId() == removeId)
							{
								result.remove(knownLearn);
								break SEARCH;
							}
						}
					}
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Removes skills from a {@code Player} that are no longer valid for their current class.<br>
	 * This method checks the player's current and parent classes to identify obsolete skills.<br>
	 * Skills provided by items in the inventory are preserved during this process.
	 * @param player The {@code Player} whose skills need to be cleaned.
	 */
	public void cleanSkillUponChangeClass(Player player)
	{
		PlayerClass currentClass = player.getPlayerClass();
		for (Skill skill : player.getAllSkills())
		{
			final int skillId = skill.getId();
			final int maxLevel = SkillData.getInstance().getMaxLevel(skillId);
			final long hashCode = SkillData.getSkillHashCode(skillId, maxLevel);
			if (!isCurrentClassSkillNoParent(currentClass, hashCode) && !isRemoveSkill(currentClass, skillId) && !isAwakenSaveSkill(currentClass, skillId) && !isAlchemySkill(skillId, skill.getLevel()))
			{
				// Do not remove equipped item skills.
				boolean isItemSkill = false;
				SEARCH: for (Item item : player.getInventory().getItems())
				{
					final List<ItemSkillHolder> itemSkills = item.getTemplate().getAllSkills();
					if (itemSkills != null)
					{
						for (ItemSkillHolder itemSkillHolder : itemSkills)
						{
							if (itemSkillHolder.getSkillId() == skillId)
							{
								isItemSkill = true;
								break SEARCH;
							}
						}
					}
				}
				
				if (!isItemSkill)
				{
					player.removeSkill(skill, true, true);
				}
			}
		}
		
		// Check previous classes as well, in case classes where skipped.
		PlayerClass parentClass;
		while ((parentClass = currentClass.getParent()) != null)
		{
			final Set<Integer> removedList = _removeSkillCache.get(currentClass);
			if (removedList != null)
			{
				for (Integer skillId : removedList)
				{
					final int currentLevel = player.getSkillLevel(skillId);
					if (currentLevel > 0)
					{
						player.removeSkill(SkillData.getInstance().getSkill(skillId, currentLevel));
					}
				}
			}
			
			currentClass = parentClass;
		}
	}
	
	/**
	 * Checks if a specific skill belongs to the alchemy tree.<br>
	 * It uses the {@code skillId} and {@code skillLevel} to generate a unique hash code.<br>
	 * The method returns {@code true} if the hash exists in the alchemy data map.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The current level of the skill.
	 * @return {@code true} if it is an alchemy skill, otherwise {@code false}.
	 */
	public boolean isAlchemySkill(int skillId, int skillLevel)
	{
		return _alchemySkillTree.containsKey(SkillData.getSkillHashCode(skillId, skillLevel));
	}
	
	/**
	 * Checks if a specific skill belongs to the hero skill tree.<br>
	 * It verifies the existence of the skill in the internal data map.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The current level of the skill.
	 * @return {@code true} if the skill is a hero skill, {@code false} otherwise.
	 */
	public boolean isHeroSkill(int skillId, int skillLevel)
	{
		return _heroSkillTree.containsKey(SkillData.getSkillHashCode(skillId, skillLevel));
	}
	
	/**
	 * Checks if a specific skill belongs to the Game Master skill tree.<br>
	 * This method verifies both standard skills and aura skills.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The current level of the skill.
	 * @return {@code true} if the skill is part of a Game Master tree, otherwise {@code false}.
	 */
	public boolean isGMSkill(int skillId, int skillLevel)
	{
		final long hashCode = SkillData.getSkillHashCode(skillId, skillLevel);
		return _gameMasterSkillTree.containsKey(hashCode) || _gameMasterAuraSkillTree.containsKey(hashCode);
	}
	
	/**
	 * Checks if a specific skill belongs to the pledge or sub-pledge tree.<br>
	 * This method verifies the skill by its unique hash code.
	 * @param skillId The unique identifier of the skill.
	 * @param skillLevel The current level of the skill.
	 * @return {@code true} if the skill is part of a pledge tree, {@code false} otherwise.
	 */
	public boolean isClanSkill(int skillId, int skillLevel)
	{
		final long hashCode = SkillData.getSkillHashCode(skillId, skillLevel);
		return _pledgeSkillTree.containsKey(hashCode) || _subPledgeSkillTree.containsKey(hashCode);
	}
	
	/**
	 * Checks if a specific skill is removed for a given player class.<br>
	 * This method looks up the {@code skillId} in the removal cache for the provided {@code PlayerClass}.
	 * @param playerClass The {@code PlayerClass} to check.
	 * @param skillId The unique identifier of the skill.
	 * @return {@code true} if the skill is removed, {@code false} otherwise.
	 */
	public boolean isRemoveSkill(PlayerClass playerClass, int skillId)
	{
		return _removeSkillCache.getOrDefault(playerClass, Collections.emptySet()).contains(skillId);
	}
	
	/**
	 * Checks if a specific skill belongs to the primary class tree.<br>
	 * It verifies if the {@code hashCode} exists within the map for the given {@code PlayerClass}.
	 * @param playerClass The {@code PlayerClass} to check against.
	 * @param hashCode The unique identifier of the skill.
	 * @return {@code true} if the skill is part of the class tree, {@code false} otherwise.
	 */
	public boolean isCurrentClassSkillNoParent(PlayerClass playerClass, Long hashCode)
	{
		return _classSkillTrees.getOrDefault(playerClass, Collections.emptyMap()).containsKey(hashCode);
	}
	
	/**
	 * Checks if a specific skill is part of the awakening save tree for a class.<br>
	 * This method verifies if the {@code skillId} exists in the saved skills for the given {@code PlayerClass}.
	 * @param playerClass The class of the player to check.
	 * @param skillId The unique identifier of the skill.
	 * @return {@code true} if the skill is in the awakening save tree, {@code false} otherwise.
	 */
	public boolean isAwakenSaveSkill(PlayerClass playerClass, int skillId)
	{
		return _awakeningSaveSkillTree.getOrDefault(playerClass, Collections.emptySet()).contains(skillId);
	}
	
	/**
	 * Adds specific skills to a {@code Player} object.<br>
	 * This method grants skills based on the Game Master skill trees.<br>
	 * It determines which tree to use based on the {@code auraSkills} flag.
	 * @param gmchar The {@code Player} receiving the skills.
	 * @param auraSkills If {@code true}, uses the GM Aura skill tree; otherwise, uses the standard GM skill tree.
	 */
	public void addSkills(Player gmchar, boolean auraSkills)
	{
		final Collection<SkillLearn> skills = auraSkills ? _gameMasterAuraSkillTree.values() : _gameMasterSkillTree.values();
		final SkillData st = SkillData.getInstance();
		for (SkillLearn sl : skills)
		{
			gmchar.addSkill(st.getSkill(sl.getSkillId(), sl.getSkillLevel()), false); // Don't Save GM skills to database.
		}
	}
	
	/**
	 * Initializes the internal hash code arrays for skills.<br>
	 * This method populates {@code _skillsByClassIdHashCodes}, {@code _skillsByRaceHashCodes}, and {@code _allSkillsHashCodes}.<br>
	 * It processes class-specific, race-specific, and general skill trees to organize them by hash codes.
	 */
	private void generateCheckArrays()
	{
		int index;
		long[] skillHashes;
		
		// Class-specific skills.
		Map<Long, SkillLearn> skillLearnMap;
		final Set<PlayerClass> playerClassSet = _classSkillTrees.keySet();
		_skillsByClassIdHashCodes = new HashMap<>(playerClassSet.size());
		for (PlayerClass playerClass : playerClassSet)
		{
			index = 0;
			skillLearnMap = new HashMap<>(getCompleteClassSkillTree(playerClass));
			skillHashes = new long[skillLearnMap.size()];
			for (long skillHash : skillLearnMap.keySet())
			{
				skillHashes[index++] = skillHash;
			}
			
			skillLearnMap.clear();
			Arrays.sort(skillHashes);
			_skillsByClassIdHashCodes.put(playerClass.getId(), skillHashes);
		}
		
		// Race-specific skills from Fishing and Transformation skill trees.
		final List<Long> skillHashList = new ArrayList<>();
		_skillsByRaceHashCodes = new HashMap<>(Race.values().length);
		for (Race race : Race.values())
		{
			for (SkillLearn skillLearn : _fishingSkillTree.values())
			{
				if (skillLearn.getRaces().contains(race))
				{
					skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
				}
			}
			
			for (SkillLearn skillLearn : _transformSkillTree.values())
			{
				if (skillLearn.getRaces().contains(race))
				{
					skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
				}
			}
			
			index = 0;
			skillHashes = new long[skillHashList.size()];
			for (long skillHash : skillHashList)
			{
				skillHashes[index++] = skillHash;
			}
			
			Arrays.sort(skillHashes);
			_skillsByRaceHashCodes.put(race.ordinal(), skillHashes);
			skillHashList.clear();
		}
		
		// Skills available for all classes and races.
		for (SkillLearn skillLearn : _commonSkillTree.values())
		{
			if (skillLearn.getRaces().isEmpty())
			{
				skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
			}
		}
		
		for (SkillLearn skillLearn : _fishingSkillTree.values())
		{
			if (skillLearn.getRaces().isEmpty())
			{
				skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
			}
		}
		
		for (SkillLearn skillLearn : _transformSkillTree.values())
		{
			if (skillLearn.getRaces().isEmpty())
			{
				skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
			}
		}
		
		for (SkillLearn skillLearn : _collectSkillTree.values())
		{
			skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
		}
		
		for (SkillLearn skillLearn : _abilitySkillTree.values())
		{
			skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
		}
		
		for (SkillLearn skillLearn : _alchemySkillTree.values())
		{
			skillHashList.add(SkillData.getSkillHashCode(skillLearn.getSkillId(), skillLearn.getSkillLevel()));
		}
		
		_allSkillsHashCodes = new long[skillHashList.size()];
		int hashIndex = 0;
		for (long skillHash : skillHashList)
		{
			_allSkillsHashCodes[hashIndex++] = skillHash;
		}
		
		Arrays.sort(_allSkillsHashCodes);
	}
	
	/**
	 * Checks if a specific skill is allowed for the given player.<br>
	 * This method verifies if the skill belongs to the player's class, race, or other valid categories.<br>
	 * It also handles special cases like GM skills and excluded skills.
	 * @param player The {@code Player} object to check.
	 * @param skill The {@code Skill} object to validate.
	 * @return {@code true} if the skill is allowed, otherwise {@code false}.
	 */
	public boolean isSkillAllowed(Player player, Skill skill)
	{
		if (skill.isExcludedFromCheck())
		{
			return true;
		}
		
		if (player.isGM() && skill.isGMSkill())
		{
			return true;
		}
		
		// Prevent accidental skill remove during reload.
		if (_loading)
		{
			return true;
		}
		
		final int skillId = skill.getId();
		final int maxLevel = SkillData.getInstance().getMaxLevel(skillId);
		final int safeLevel = Math.min(skill.getLevel(), maxLevel);
		final long hashCode = SkillData.getSkillHashCode(skillId, safeLevel);
		final PlayerClass playerClass = player.getPlayerClass();
		if (Arrays.binarySearch(_skillsByClassIdHashCodes.get(playerClass.getId()), hashCode) >= 0)
		{
			return true;
		}
		
		final Race race = player.getRace();
		if (Arrays.binarySearch(_skillsByRaceHashCodes.get(race.ordinal()), hashCode) >= 0)
		{
			return true;
		}
		
		if (Arrays.binarySearch(_allSkillsHashCodes, hashCode) >= 0)
		{
			return true;
		}
		
		// Exclude Transfer Skills from this check.
		if (getTransferSkill(skillId, safeLevel, playerClass) != null)
		{
			return true;
		}
		
		// Exclude Race skills from this check.
		if (getRaceSkill(skillId, safeLevel, race) != null)
		{
			return true;
		}
		
		return false;
	}
	
	/**
	 * Logs the total count of loaded skills for all tree types.<br>
	 * This method provides a summary of data loaded into the {@code SkillTreeData} class.<br>
	 * It prints information about classes, races, and special skill categories to the logger.
	 */
	private void report()
	{
		int classSkillTreeCount = 0;
		for (Map<Long, SkillLearn> classSkillTree : _classSkillTrees.values())
		{
			classSkillTreeCount += classSkillTree.size();
		}
		
		int transferSkillTreeCount = 0;
		for (Map<Long, SkillLearn> trasferSkillTree : _transferSkillTrees.values())
		{
			transferSkillTreeCount += trasferSkillTree.size();
		}
		
		int raceSkillTreeCount = 0;
		for (Map<Long, SkillLearn> raceSkillTree : _raceSkillTree.values())
		{
			raceSkillTreeCount += raceSkillTree.size();
		}
		
		int revelationSkillTreeCount = 0;
		for (Map<Long, SkillLearn> revelationSkillTree : _revelationSkillTree.values())
		{
			revelationSkillTreeCount += revelationSkillTree.size();
		}
		
		int dwarvenOnlyFishingSkillCount = 0;
		for (SkillLearn fishSkill : _fishingSkillTree.values())
		{
			if (fishSkill.getRaces().contains(Race.DWARF))
			{
				dwarvenOnlyFishingSkillCount++;
			}
		}
		
		int resSkillCount = 0;
		for (SkillLearn pledgeSkill : _pledgeSkillTree.values())
		{
			if (pledgeSkill.isResidencialSkill())
			{
				resSkillCount++;
			}
		}
		
		final String className = getClass().getSimpleName();
		LOGGER.info(className + ": Loaded " + classSkillTreeCount + " Class skills for " + _classSkillTrees.size() + " class skill trees.");
		
		LOGGER.info(className + ": Loaded " + _subClassSkillTree.size() + " sub-class skills.");
		
		LOGGER.info(className + ": Loaded " + _dualClassSkillTree.size() + " dual-class skills.");
		
		LOGGER.info(className + ": Loaded " + transferSkillTreeCount + " transfer skills for " + _transferSkillTrees.size() + " transfer skill trees.");
		
		LOGGER.info(className + ": Loaded " + raceSkillTreeCount + " race skills for " + _raceSkillTree.size() + " race skill trees.");
		
		LOGGER.info(className + ": Loaded " + _fishingSkillTree.size() + " fishing skills, " + dwarvenOnlyFishingSkillCount + " Dwarven only fishing skills.");
		
		LOGGER.info(className + ": Loaded " + _collectSkillTree.size() + " collect skills.");
		
		LOGGER.info(className + ": Loaded " + _pledgeSkillTree.size() + " clan skills, " + (_pledgeSkillTree.size() - resSkillCount) + " for clan and " + resSkillCount + " residential.");
		
		LOGGER.info(className + ": Loaded " + _subPledgeSkillTree.size() + " sub-pledge skills.");
		
		LOGGER.info(className + ": Loaded " + _transformSkillTree.size() + " transform skills.");
		
		LOGGER.info(className + ": Loaded " + _nobleSkillTree.size() + " noble skills.");
		
		LOGGER.info(className + ": Loaded " + _heroSkillTree.size() + " hero skills.");
		
		LOGGER.info(className + ": Loaded " + _gameMasterSkillTree.size() + " game master skills.");
		
		LOGGER.info(className + ": Loaded " + _gameMasterAuraSkillTree.size() + " game master aura skills.");
		
		LOGGER.info(className + ": Loaded " + _abilitySkillTree.size() + " ability skills.");
		
		LOGGER.info(className + ": Loaded " + _alchemySkillTree.size() + " alchemy skills.");
		
		LOGGER.info(className + ": Loaded " + _awakeningSaveSkillTree.size() + " class awaken save skills.");
		
		LOGGER.info(className + ": Loaded " + revelationSkillTreeCount + " Revelation skills.");
		
		final int commonSkills = _commonSkillTree.size();
		if (commonSkills > 0)
		{
			LOGGER.info(className + ": Loaded " + commonSkills + " common skills.");
		}
	}
	
	/**
	 * Retrieves the singleton instance of {@code SkillTreeData}.<br>
	 * This method provides a global access point to the skill tree data.
	 * @return The single instance of {@code SkillTreeData}.
	 */
	public static SkillTreeData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Singleton holder for the SkillTreesData class.
	 */
	private static class SingletonHolder
	{
		protected static final SkillTreeData INSTANCE = new SkillTreeData();
	}
}
