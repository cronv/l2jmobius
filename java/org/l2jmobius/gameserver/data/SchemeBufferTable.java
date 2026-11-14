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
package org.l2jmobius.gameserver.data;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.entity.actor.holders.npc.BuffSkillHolder;

/**
 * This class manages the loading and storage of scheme buffer skills and player schemes.<br>
 * It handles data persistence between the database and memory while providing category-aware lookups for skill IDs.<br>
 * It also resolves specific skill holders required for manual pages.
 * @author Mobius, BazookaRpm
 */
public class SchemeBufferTable
{
	private static final Logger LOGGER = Logger.getLogger(SchemeBufferTable.class.getName());
	
	// Constants.
	private static final String SKILLS_XML_PATH = "./data/SchemeBufferSkills.xml";
	private static final String LOAD_SCHEMES = "SELECT * FROM buffer_schemes";
	private static final String DELETE_SCHEMES = "TRUNCATE TABLE buffer_schemes";
	private static final String INSERT_SCHEME = "INSERT INTO buffer_schemes (object_id, scheme_name, skills) VALUES (?,?,?)";
	private static final String TYPE_MAGE_GROUP = "MAGE_GROUP";
	private static final String TYPE_FIGHTER_GROUP = "FIGHTER_GROUP";
	private static final String DB_COLUMN_OBJECT_ID = "object_id";
	private static final String DB_COLUMN_SCHEME_NAME = "scheme_name";
	private static final String DB_COLUMN_SKILLS = "skills";
	private static final String SKILL_SEPARATOR = ",";
	
	// Player schemes storage.
	private final Map<Integer, Map<String, List<Integer>>> _schemesTable = new ConcurrentHashMap<>();
	
	// Skills registry.
	private final Map<Integer, BuffSkillHolder> _availableBuffs = new LinkedHashMap<>();
	private final Map<String, List<Integer>> _skillIdsByType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final Map<String, Map<Integer, BuffSkillHolder>> _availableBuffsByType = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
	private final List<String> _skillTypesOrder = new ArrayList<>();
	
	/**
	 * Initializes the {@code SchemeBufferTable}.<br>
	 * This constructor loads all available buffs and player schemes into memory.
	 */
	public SchemeBufferTable()
	{
		loadAvailableBuffs();
		loadPlayerSchemes();
		LOGGER.info("SchemeBufferTable: Loaded " + _schemesTable.size() + " players and " + _availableBuffs.size() + " available skills.");
	}
	
	/**
	 * Saves all current player schemes to the database.<br>
	 * This method clears existing records and inserts updated data from {@code _schemesTable}.<br>
	 * It handles batch processing for improved performance during the save operation.
	 */
	public void saveSchemes()
	{
		try (Connection connection = DatabaseFactory.getConnection())
		{
			try (PreparedStatement deleteStatement = connection.prepareStatement(DELETE_SCHEMES))
			{
				deleteStatement.execute();
			}
			
			try (PreparedStatement insertStatement = connection.prepareStatement(INSERT_SCHEME))
			{
				for (Entry<Integer, Map<String, List<Integer>>> playerEntry : _schemesTable.entrySet())
				{
					for (Entry<String, List<Integer>> schemeEntry : playerEntry.getValue().entrySet())
					{
						final StringBuilder skillsBuilder = new StringBuilder();
						for (int skillId : schemeEntry.getValue())
						{
							skillsBuilder.append(skillId).append(SKILL_SEPARATOR);
						}
						
						if (!skillsBuilder.isEmpty())
						{
							skillsBuilder.setLength(skillsBuilder.length() - SKILL_SEPARATOR.length());
						}
						
						insertStatement.setInt(1, playerEntry.getKey());
						insertStatement.setString(2, schemeEntry.getKey());
						insertStatement.setString(3, skillsBuilder.toString());
						insertStatement.addBatch();
					}
				}
				insertStatement.executeBatch();
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Error while saving schemes. Players: " + _schemesTable.size() + ".", e);
		}
	}
	
	/**
	 * Sets a new scheme for a specific player.<br>
	 * This method updates the list of skill IDs associated with a scheme name.<br>
	 * It ensures that the maximum number of schemes is not exceeded.
	 * @param playerId The unique identifier of the player.
	 * @param schemeName The name of the scheme to create or update.
	 * @param skillIds The list of skill IDs to include in this scheme.
	 */
	public void setScheme(int playerId, String schemeName, List<Integer> skillIds)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.computeIfAbsent(playerId, key -> new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER));
		if ((schemes.size() >= SchemeBufferConfig.BUFFER_MAX_SCHEMES) && !schemes.containsKey(schemeName))
		{
			return;
		}
		
		final List<Integer> safeList = (skillIds != null) ? List.copyOf(skillIds) : Collections.emptyList();
		schemes.put(schemeName, safeList);
	}
	
	/**
	 * Deletes a specific scheme for a player.<br>
	 * This method removes the entry from the internal memory table.<br>
	 * If no schemes remain for the player, their data is cleared entirely.
	 * @param playerId The unique identifier of the player.
	 * @param schemeName The name of the scheme to be deleted.
	 * @return {@code true} if the scheme was successfully removed, {@code false} otherwise.
	 */
	public boolean deleteScheme(int playerId, String schemeName)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}
		
		final boolean removed = schemes.remove(schemeName) != null;
		if (schemes.isEmpty())
		{
			_schemesTable.remove(playerId);
		}
		return removed;
	}
	
	/**
	 * Adds a specific skill to an existing player scheme.<br>
	 * This method checks if the scheme exists and if the skill is already present.<br>
	 * It returns {@code true} if the skill was successfully added.<br>
	 * It returns {@code false} if the scheme does not exist or the skill is already in the list.
	 * @param playerId The unique identifier of the player.
	 * @param schemeName The name of the scheme to modify.
	 * @param skillId The ID of the skill to add.
	 * @return {@code true} if successful, {@code false} otherwise.
	 */
	public boolean addSkillToScheme(int playerId, String schemeName, int skillId)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}
		
		final List<Integer> current = schemes.get(schemeName);
		if (current == null)
		{
			return false;
		}
		
		if (current.contains(skillId))
		{
			return false;
		}
		
		final List<Integer> updated = new ArrayList<>(current);
		updated.add(skillId);
		schemes.put(schemeName, Collections.unmodifiableList(updated));
		return true;
	}
	
	/**
	 * Removes a specific skill from a player's scheme.<br>
	 * This method checks if the skill exists before attempting to remove it.
	 * @param playerId The unique identifier of the player.
	 * @param schemeName The name of the scheme to modify.
	 * @param skillId The ID of the skill to be removed.
	 * @return {@code true} if the skill was successfully removed, {@code false} otherwise.
	 */
	public boolean removeSkillFromScheme(int playerId, String schemeName, int skillId)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if (schemes == null)
		{
			return false;
		}
		
		final List<Integer> current = schemes.get(schemeName);
		if (current == null)
		{
			return false;
		}
		
		if (!current.contains(skillId))
		{
			return false;
		}
		
		final List<Integer> updated = new ArrayList<>(current);
		updated.remove(Integer.valueOf(skillId));
		schemes.put(schemeName, Collections.unmodifiableList(updated));
		return true;
	}
	
	/**
	 * Retrieves all schemes associated with a specific player.<br>
	 * This method returns an unmodifiable map of scheme names and their skill IDs.
	 * @param playerId The unique identifier of the player.
	 * @return A {@code Map} containing the player's schemes, or {@code null} if none exist.
	 */
	public Map<String, List<Integer>> getPlayerSchemes(int playerId)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		return (schemes != null) ? Collections.unmodifiableMap(schemes) : null;
	}
	
	/**
	 * Retrieves a list of skill IDs for a specific scheme owned by a player.<br>
	 * Returns an empty {@code List} if the player or the scheme does not exist.
	 * @param playerId The unique identifier of the player.
	 * @param schemeName The name of the scheme to retrieve.
	 * @return A {@code List<Integer>} containing the skill IDs for the specified scheme.
	 */
	public List<Integer> getScheme(int playerId, String schemeName)
	{
		final Map<String, List<Integer>> schemes = _schemesTable.get(playerId);
		if ((schemes == null) || (schemes.get(schemeName) == null))
		{
			return Collections.emptyList();
		}
		return schemes.get(schemeName);
	}
	
	/**
	 * Retrieves a list of skill IDs based on the provided group type.<br>
	 * This method looks up the skills associated with a specific category.<br>
	 * If no skills are found for the given {@code groupType}, it returns an empty list.
	 * @param groupType The category name used to filter the skills.
	 * @return A {@code List<Integer>} containing the skill IDs for the specified type.
	 */
	public List<Integer> getSkillsIdsByType(String groupType)
	{
		return _skillIdsByType.getOrDefault(groupType, Collections.emptyList());
	}
	
	/**
	 * Retrieves a list of available skill types.<br>
	 * This method filters out specific group types from the internal order.
	 * @return A {@code List<String>} containing the filtered skill types.
	 */
	public List<String> getSkillTypes()
	{
		final List<String> skillTypes = new ArrayList<>(_skillTypesOrder.size());
		for (String type : _skillTypesOrder)
		{
			if (type.equalsIgnoreCase(TYPE_MAGE_GROUP) || type.equalsIgnoreCase(TYPE_FIGHTER_GROUP))
			{
				continue;
			}
			skillTypes.add(type);
		}
		return skillTypes;
	}
	
	/**
	 * Retrieves the {@code BuffSkillHolder} for a specific skill ID.<br>
	 * This method looks up the buff data from the internal cache.
	 * @param skillId The unique identifier of the skill to retrieve.
	 * @return The {@code BuffSkillHolder} associated with the given {@code skillId}, or {@code null} if not found.
	 */
	public BuffSkillHolder getAvailableBuff(int skillId)
	{
		return _availableBuffs.get(skillId);
	}
	
	/**
	 * Retrieves a {@code BuffSkillHolder} based on the group type and skill ID.<br>
	 * This method looks for a specific buff within a category first.<br>
	 * If it is not found in that category, it returns the default buff holder.
	 * @param groupType The category name of the buff.
	 * @param skillId The unique identifier for the skill.
	 * @return The {@code BuffSkillHolder} associated with the provided parameters.
	 */
	public BuffSkillHolder getAvailableBuff(String groupType, int skillId)
	{
		final Map<Integer, BuffSkillHolder> holdersByType = _availableBuffsByType.get(groupType);
		return (holdersByType != null) ? holdersByType.getOrDefault(skillId, _availableBuffs.get(skillId)) : _availableBuffs.get(skillId);
	}
	
	/**
	 * Provides the singleton instance of the {@code SchemeBufferTable}.<br>
	 * Use this method to access the global manager for scheme buffer skills.
	 * @return The singleton instance of {@code SchemeBufferTable}.
	 */
	public static SchemeBufferTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Loads available buffs from the configuration file.<br>
	 * This method parses the XML file located at {@code SKILLS_XML_PATH}.<br>
	 * It populates the internal maps with skill data and categories.<br>
	 * It ensures that only the highest level version of a skill is kept in the main buffer.
	 */
	private void loadAvailableBuffs()
	{
		int categoryCount = 0;
		int skillCount = 0;
		
		try
		{
			final Node rootNode = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new File(SKILLS_XML_PATH)).getDocumentElement();
			for (Node categoryNode = (rootNode != null) ? rootNode.getFirstChild() : null; categoryNode != null; categoryNode = categoryNode.getNextSibling())
			{
				if (!"category".equalsIgnoreCase(categoryNode.getNodeName()))
				{
					continue;
				}
				
				final NamedNodeMap categoryAttributes = categoryNode.getAttributes();
				if (categoryAttributes == null)
				{
					continue;
				}
				
				final Node typeNode = categoryAttributes.getNamedItem("type");
				if (typeNode == null)
				{
					continue;
				}
				
				final String category = typeNode.getNodeValue();
				if (!_skillIdsByType.containsKey(category))
				{
					_skillIdsByType.put(category, new ArrayList<>());
					_availableBuffsByType.put(category, new LinkedHashMap<>());
					_skillTypesOrder.add(category);
					categoryCount++;
				}
				
				for (Node buffNode = categoryNode.getFirstChild(); buffNode != null; buffNode = buffNode.getNextSibling())
				{
					if (!"buff".equalsIgnoreCase(buffNode.getNodeName()))
					{
						continue;
					}
					
					final NamedNodeMap attributes = buffNode.getAttributes();
					if (attributes == null)
					{
						continue;
					}
					
					final Node idNode = attributes.getNamedItem("id");
					final Node levelNode = attributes.getNamedItem("level");
					final Node priceNode = attributes.getNamedItem("price");
					if ((idNode == null) || (levelNode == null) || (priceNode == null))
					{
						continue;
					}
					
					final int skillId = Integer.parseInt(idNode.getNodeValue());
					final int level = Integer.parseInt(levelNode.getNodeValue());
					final int price = Integer.parseInt(priceNode.getNodeValue());
					final Node descNode = attributes.getNamedItem("desc");
					final String description = (descNode != null) ? descNode.getNodeValue() : "";
					
					final List<Integer> skillIds = _skillIdsByType.get(category);
					if (!skillIds.contains(skillId))
					{
						skillIds.add(skillId);
					}
					
					final BuffSkillHolder holder = new BuffSkillHolder(skillId, level, price, category, description);
					_availableBuffsByType.get(category).put(skillId, holder);
					
					final BuffSkillHolder existing = _availableBuffs.get(skillId);
					if ((existing == null) || (holder.getLevel() > existing.getLevel()))
					{
						_availableBuffs.put(skillId, holder);
					}
					
					skillCount++;
				}
			}
			
			for (Entry<String, List<Integer>> entry : _skillIdsByType.entrySet())
			{
				entry.setValue(Collections.unmodifiableList(entry.getValue()));
			}
			
			LOGGER.info("SchemeBufferTable: Loaded " + categoryCount + " categories and " + skillCount + " entries.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Failed to load skills XML: " + SKILLS_XML_PATH + ".", e);
		}
	}
	
	/**
	 * Loads all player schemes from the database into memory.<br>
	 * This method retrieves scheme names and skill IDs for every object.<br>
	 * It validates each skill ID against the {@code _availableBuffs} map.<br>
	 * Valid entries are then stored using the {@code setScheme} method.
	 */
	private void loadPlayerSchemes()
	{
		int entryCount = 0;
		
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(LOAD_SCHEMES);
			ResultSet resultSet = statement.executeQuery())
		{
			while (resultSet.next())
			{
				final String schemeName = resultSet.getString(DB_COLUMN_SCHEME_NAME);
				final String skills = resultSet.getString(DB_COLUMN_SKILLS);
				if ((schemeName == null) || (skills == null))
				{
					continue;
				}
				
				final int objectId = resultSet.getInt(DB_COLUMN_OBJECT_ID);
				final String[] split = skills.split(SKILL_SEPARATOR);
				final List<Integer> schemeSkillIds = new ArrayList<>(split.length);
				
				for (String token : split)
				{
					if (token.isEmpty())
					{
						continue;
					}
					
					final int skillId;
					try
					{
						skillId = Integer.parseInt(token);
					}
					catch (NumberFormatException e)
					{
						LOGGER.warning("SchemeBufferTable: Invalid skill id '" + token + "' for scheme '" + schemeName + "' (objectId: " + objectId + ").");
						continue;
					}
					
					if (_availableBuffs.containsKey(skillId))
					{
						schemeSkillIds.add(skillId);
					}
				}
				
				setScheme(objectId, schemeName, schemeSkillIds);
				entryCount++;
			}
			
			LOGGER.info("SchemeBufferTable: Loaded " + entryCount + " scheme entries from database.");
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "SchemeBufferTable: Failed to load buff schemes from database.", e);
		}
	}
	
	private static class SingletonHolder
	{
		protected static final SchemeBufferTable INSTANCE = new SchemeBufferTable();
	}
}
