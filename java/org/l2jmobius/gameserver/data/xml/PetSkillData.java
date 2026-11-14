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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class handles the loading and parsing of pet skills from XML configuration files.<br>
 * It maps data to {@code Skill} objects for use by {@link org.l2jmobius.gameserver.entity.actor.Summon} entities.
 * @author Mobius
 */
public class PetSkillData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(PetSkillData.class.getName());
	private final Map<Integer, Map<Long, SkillHolder>> _skillTrees = new HashMap<>();
	
	/**
	 * Initializes a new instance of the {@code PetSkillData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected PetSkillData()
	{
		load();
	}
	
	/**
	 * Loads the pet skill data from the configuration file.<br>
	 * This method clears existing data and parses {@code data/PetSkillData.xml}.<br>
	 * It logs the total number of skills loaded into the {@code _skillTrees} map.
	 */
	@Override
	public void load()
	{
		_skillTrees.clear();
		parseDatapackFile("data/PetSkillData.xml");
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _skillTrees.size() + " skills.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate the internal skill trees.<br>
	 * This method reads XML data from a specific file structure.<br>
	 * It maps skills to their respective NPCs based on the attributes found in the file.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} source of the XML data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("skill".equalsIgnoreCase(d.getNodeName()))
					{
						final NamedNodeMap attrs = d.getAttributes();
						final int npcId = parseInteger(attrs, "npcId");
						final int skillId = parseInteger(attrs, "skillId");
						final int skillLevel = parseInteger(attrs, "skillLevel");
						Map<Long, SkillHolder> skillTree = _skillTrees.get(npcId);
						if (skillTree == null)
						{
							skillTree = new HashMap<>();
							_skillTrees.put(npcId, skillTree);
						}
						
						if (SkillData.getInstance().getSkill(skillId, skillLevel == 0 ? 1 : skillLevel) != null)
						{
							skillTree.put(SkillData.getSkillHashCode(skillId, skillLevel + 1), new SkillHolder(skillId, skillLevel));
						}
						else
						{
							LOGGER.info(getClass().getSimpleName() + ": Could not find skill with id " + skillId + ", level " + skillLevel + " for NPC " + npcId + ".");
						}
					}
				}
			}
		}
	}
	
	/**
	 * Calculates the current level of a specific skill for a given pet.<br>
	 * This method checks the pet's skill tree and applies the correct leveling logic.<br>
	 * It considers both the pet's level and any existing skill levels.
	 * @param pet The {@code Summon} object representing the pet.
	 * @param skillId The unique identifier for the skill to check.
	 * @return The calculated level of the skill as an {@code int}.
	 */
	public int getAvailableLevel(Summon pet, int skillId)
	{
		int level = 0;
		final int petId = pet.getId();
		final Map<Long, SkillHolder> skillTree = _skillTrees.get(petId);
		if (skillTree == null)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": Pet id " + petId + " does not have any skills assigned.");
			return level;
		}
		
		for (SkillHolder skillHolder : skillTree.values())
		{
			final int skillHolderId = skillHolder.getSkillId();
			if (skillHolderId != skillId)
			{
				continue;
			}
			
			final int skillHolderLevel = skillHolder.getSkillLevel();
			final int petLevel = pet.getLevel();
			if (skillHolderLevel == 0)
			{
				if (petLevel < 70)
				{
					level = petLevel / 10;
					if (level <= 0)
					{
						level = 1;
					}
				}
				else
				{
					level = 7 + ((petLevel - 70) / 5);
				}
				
				// formula usable for skill that have 10 or more skill levels
				final int maxLevel = SkillData.getInstance().getMaxLevel(skillHolderId);
				if (level > maxLevel)
				{
					level = maxLevel;
				}
				break;
			}
			else if ((1 <= petLevel) && (skillHolderLevel > level))
			{
				level = skillHolderLevel;
			}
		}
		
		return level;
	}
	
	/**
	 * Retrieves a list of all skills available to a specific pet.<br>
	 * This method checks the internal skill tree for the given {@code Summon}.<br>
	 * It returns an empty list if no skills are assigned.
	 * @param pet The {@code Summon} object to check for skills.
	 * @return A {@code List} of unique skill IDs available to the pet.
	 */
	public List<Integer> getAvailableSkills(Summon pet)
	{
		final List<Integer> skillIds = new ArrayList<>();
		final int petId = pet.getId();
		final Map<Long, SkillHolder> skillTree = _skillTrees.get(petId);
		if (skillTree == null)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": Pet id " + petId + " does not have any skills assigned.");
			return skillIds;
		}
		
		for (SkillHolder skillHolder : skillTree.values())
		{
			final Integer skillHolderId = skillHolder.getSkillId();
			if (skillIds.contains(skillHolderId))
			{
				continue;
			}
			
			skillIds.add(skillHolderId);
		}
		
		return skillIds;
	}
	
	/**
	 * Retrieves all skills known by a specific pet.<br>
	 * This method looks up the skill tree associated with the {@code Summon}.<br>
	 * It returns an empty list if no skills are found.
	 * @param pet The {@code Summon} object to check for skills.
	 * @return A {@code List} of {@code Skill} objects known by the pet.
	 */
	public List<Skill> getKnownSkills(Summon pet)
	{
		final List<Skill> skills = new ArrayList<>();
		final Map<Long, SkillHolder> skillTree = _skillTrees.get(pet.getId());
		if (skillTree == null)
		{
			return skills;
		}
		
		for (SkillHolder skillHolder : skillTree.values())
		{
			final Skill skill = skillHolder.getSkill();
			if (skills.contains(skill))
			{
				continue;
			}
			
			skills.add(skill);
		}
		
		return skills;
	}
	
	/**
	 * Retrieves a specific {@code Skill} for a given {@code Summon}.<br>
	 * It searches the pet's known skills based on the provided ID.
	 * @param pet The {@code Summon} entity to check.
	 * @param skillId The unique identifier of the skill.
	 * @return The {@code Skill} object if found, or {@code null} otherwise.
	 */
	public Skill getKnownSkill(Summon pet, int skillId)
	{
		final Map<Long, SkillHolder> skillTree = _skillTrees.get(pet.getId());
		if (skillTree == null)
		{
			return null;
		}
		
		for (SkillHolder skillHolder : skillTree.values())
		{
			if (skillHolder.getSkillId() == skillId)
			{
				return skillHolder.getSkill();
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the singleton instance of {@code PetSkillData}.<br>
	 * This method provides a global access point to the pet skill data.
	 * @return The single instance of {@code PetSkillData}.
	 */
	public static PetSkillData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetSkillData INSTANCE = new PetSkillData();
	}
}
