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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.config.custom.RebirthConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;

/**
 * Manages the complete rebirth system lifecycle for players.<br>
 * This class handles rebirth requests, skill selection, persistence, rewards, and visual effects.<br>
 * It also builds dynamic HTML windows for rebirth menus and manages selected rebirth skills.
 * @author BazookaRpm
 */
public final class RebirthManager
{
	// Logger.
	private static final Logger LOGGER = Logger.getLogger(RebirthManager.class.getName());
	
	// Constants.
	private static final int SKILLS_PER_ROW = 3;
	private static final int SKILL_ICON_PADDING_THRESHOLD = 1000;
	
	private static final String REBIRTH_BUSY_MESSAGE = "A rebirth operation is already in progress.";
	private static final String SELECT_REBIRTH_COUNT = "SELECT rebirthCount FROM rebirth_system WHERE charId=?";
	private static final String INSERT_FIRST_REBIRTH = "INSERT INTO rebirth_system (charId, rebirthCount) VALUES (?,1)";
	private static final String UPDATE_REBIRTH_COUNT = "UPDATE rebirth_system SET rebirthCount=? WHERE charId=?";
	private static final String SELECT_SELECTED_SKILLS = "SELECT selectedSkills FROM rebirth_system WHERE charId=?";
	private static final String UPDATE_SELECTED_SKILLS = "UPDATE rebirth_system SET selectedSkills=? WHERE charId=?";
	
	private static final String CLASS_LIST_FILE = "data/stats/players/classList.xml";
	
	private static final Map<Integer, Integer> ROOT_CLASS_IDS = new ConcurrentHashMap<>();
	private static volatile boolean ROOT_CLASS_IDS_LOADED = false;
	
	// Operation guards.
	private static final Set<Integer> REBIRTH_OPERATIONS_IN_PROGRESS = ConcurrentHashMap.newKeySet();
	private static final Set<Integer> REBIRTH_SKILL_OPERATIONS_IN_PROGRESS = ConcurrentHashMap.newKeySet();
	
	/**
	 * Initializes a new instance of the {@code RebirthManager}.<br>
	 * This constructor is used to set up the manager for rebirth mechanics.
	 */
	protected RebirthManager()
	{
	}
	
	/**
	 * Opens the skill selection window for a player who has performed at least one Rebirth.<br>
	 * This method generates an HTML interface showing available skills, their costs, and current ownership status.<br>
	 * It validates the player's rebirth count to determine the maximum number of skills they can select.
	 * @param player The {@code Player} who will view the selection window.
	 * @param objectId The unique identifier for the NPC or object sending the message.
	 */
	public void displaySkillSelectionWindow(Player player, int objectId)
	{
		final int rebirthCount = getRebirthLevel(player);
		if (rebirthCount <= 0)
		{
			player.sendMessage("You must perform at least one Rebirth before selecting skills.");
			return;
		}
		
		final List<String> skillDefinitions = getSkillPool(player);
		final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(skillDefinitions, player);
		if (skillDefinitionMap.isEmpty())
		{
			player.sendMessage("There are no skills configured for your class. Contact an administrator.");
			return;
		}
		
		final List<Integer> selectedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
		final Set<Integer> selectedSkillSet = createSkillSet(selectedSkills);
		final int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		final StringBuilder htmlBuilder = new StringBuilder();
		
		htmlBuilder.append("<html><body><center>");
		htmlBuilder.append("<title>Select your Rebirth Skills</title>");
		htmlBuilder.append("<br><font color=LEVEL>Maximum allowed: ").append(allowedSkills).append(" skill(s)</font><br1>");
		htmlBuilder.append("<font color=99FF99>Tokens available: ").append(getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID)).append("</font><br><br>");
		htmlBuilder.append("<table width=256 cellpadding=1 cellspacing=2><tr>");
		
		int column = 0;
		for (Entry<Integer, String> entry : skillDefinitionMap.entrySet())
		{
			final String definition = entry.getValue();
			final int skillLevel = getSkillLevel(definition);
			if (skillLevel <= 0)
			{
				final int playerId = player.getObjectId();
				final String playerName = player.getName();
				LOGGER.warning("Skipping invalid rebirth skill definition in selection window: playerId=" + playerId + " playerName=" + playerName + " definition=" + definition);
				continue;
			}
			
			final int skillId = entry.getKey().intValue();
			final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
			if (skill == null)
			{
				final int playerId = player.getObjectId();
				final String playerName = player.getName();
				LOGGER.warning("Configured rebirth skill was not found in selection window: playerId=" + playerId + " playerName=" + playerName + " skillId=" + skillId + " skillLevel=" + skillLevel + " definition=" + definition);
				continue;
			}
			
			final int itemCost = getSkillCost(definition);
			if (column == SKILLS_PER_ROW)
			{
				htmlBuilder.append("</tr><tr>");
				column = 0;
			}
			
			final String icon = (skillId < SKILL_ICON_PADDING_THRESHOLD) ? ("0" + skillId) : String.valueOf(skillId);
			htmlBuilder.append("<td width=90 align=center>");
			htmlBuilder.append("<img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1>");
			htmlBuilder.append("<font color=\"FFDD99\">").append(skill.getName()).append("</font><br1>");
			htmlBuilder.append("<font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1>");
			htmlBuilder.append("<font color=\"99FF99\">Cost: ").append(itemCost).append("</font><br1>");
			
			if (selectedSkillSet.contains(Integer.valueOf(skillId)))
			{
				htmlBuilder.append("<font color=AAAAAA>Owned</font>");
			}
			else if (selectedSkills.size() >= allowedSkills)
			{
				htmlBuilder.append("<font color=AAAAAA>Limit reached</font>");
			}
			else
			{
				htmlBuilder.append("<button value=\"Choose\" action=\"bypass -h rebirth_previewSkill ").append(skillId).append("\" width=55 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			}
			
			htmlBuilder.append("</td>");
			column++;
		}
		
		htmlBuilder.append("</tr></table><br>");
		htmlBuilder.append("<button value=\"Return\" action=\"bypass -h rebirth_openmenu\" width=95 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		htmlBuilder.append("</center></body></html>");
		
		html.setHtml(htmlBuilder.toString());
		player.sendPacket(html);
	}
	
	/**
	 * Opens a preview window for a specific skill.<br>
	 * This method allows the player to see details about a skill before selecting it.
	 * @param player The {@code Player} who will view the window.
	 * @param objectId The unique identifier of the NPC or object triggering the action.
	 * @param skillId The specific ID of the skill to be previewed.
	 */
	public void displaySkillPreviewWindow(Player player, int objectId, int skillId)
	{
		final List<String> skillDefinitions = getSkillPool(player);
		final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(skillDefinitions, player);
		final List<Integer> savedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
		final Set<Integer> savedSkillSet = createSkillSet(savedSkills);
		final int rebirthCount = getRebirthLevel(player);
		displaySkillPreviewWindow(player, objectId, skillId, skillDefinitionMap, savedSkills, savedSkillSet, rebirthCount);
	}
	
	/**
	 * Opens the main rebirth skills window for a specific player.<br>
	 * This method prepares the necessary data and displays the UI to the user.
	 * @param player The {@code Player} who will view the window.
	 * @param objectId The unique identifier of the NPC or object triggering the window.
	 */
	public void displayMainWindow(Player player, int objectId)
	{
		final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
		final List<Integer> selectedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
		final int currentRebirthCount = getRebirthLevel(player);
		displayMainWindow(player, objectId, selectedSkills, currentRebirthCount, skillDefinitionMap);
	}
	
	/**
	 * Grants a specific rebirth skill to a player.<br>
	 * This method validates the player's rebirth count and available skills.<br>
	 * It consumes the required amount of reward items if the purchase is successful.
	 * @param player The {@code Player} who is attempting to acquire the skill.
	 * @param objectId The unique identifier for the NPC or object handling the transaction.
	 * @param skillId The unique identifier of the rebirth skill to be acquired.
	 */
	public void acquireRebirthSkill(Player player, int objectId, int skillId)
	{
		final int playerId = player.getObjectId();
		if (!REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.add(Integer.valueOf(playerId)))
		{
			player.sendMessage(REBIRTH_BUSY_MESSAGE);
			return;
		}
		
		try
		{
			final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
			final List<Integer> savedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
			final int rebirthCount = getRebirthLevel(player);
			if (rebirthCount <= 0)
			{
				player.sendMessage("You must perform at least one Rebirth before selecting skills.");
				displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
				return;
			}
			
			final Set<Integer> savedSkillSet = createSkillSet(savedSkills);
			if (savedSkillSet.contains(Integer.valueOf(skillId)))
			{
				player.sendMessage("You already own this rebirth skill.");
				displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
				return;
			}
			
			final int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
			if (savedSkills.size() >= allowedSkills)
			{
				player.sendMessage("You have reached the maximum number of skills allowed. (" + allowedSkills + ").");
				displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
				return;
			}
			
			final String definition = getSkillDefinition(skillDefinitionMap, skillId);
			if (definition == null)
			{
				player.sendMessage("This skill is not configured for your class.");
				displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
				return;
			}
			
			final int itemCost = getSkillCost(definition);
			if (!consumeItem(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID, itemCost, ItemProcessType.DESTROY, "rebirth token"))
			{
				displaySkillPreviewWindow(player, objectId, skillId, skillDefinitionMap, savedSkills, savedSkillSet, rebirthCount);
				return;
			}
			
			savedSkills.add(Integer.valueOf(skillId));
			saveSelectedSkills(playerId, savedSkills);
			grantRebirthSkills(player, skillDefinitionMap, savedSkills);
			playRebirthSelectFx(player);
			player.sendMessage("Rebirth skill purchased successfully.");
			displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
		}
		finally
		{
			REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.remove(Integer.valueOf(playerId));
		}
	}
	
	/**
	 * Removes a specific rebirth skill from the player.<br>
	 * This method checks if the player owns the skill before removing it.<br>
	 * It also handles item refunds based on the configuration.<br>
	 * Finally, it updates the UI to show the current skills.
	 * @param player The {@code Player} who is performing the reset.
	 * @param objectId The ID of the NPC or object used for the interaction.
	 * @param skillId The unique identifier of the rebirth skill to remove.
	 */
	public void resetRebirthSkill(Player player, int objectId, int skillId)
	{
		final int playerId = player.getObjectId();
		if (!REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.add(Integer.valueOf(playerId)))
		{
			player.sendMessage(REBIRTH_BUSY_MESSAGE);
			return;
		}
		
		try
		{
			final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
			final List<Integer> savedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
			final Set<Integer> savedSkillSet = createSkillSet(savedSkills);
			final int rebirthCount = getRebirthLevel(player);
			if (!savedSkillSet.contains(Integer.valueOf(skillId)))
			{
				player.sendMessage("You do not own this rebirth skill.");
				displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
				return;
			}
			
			final String definition = getSkillDefinition(skillDefinitionMap, skillId);
			if (definition == null)
			{
				player.sendMessage("The configured skill data is invalid.");
				displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
				return;
			}
			
			final int skillLevel = getSkillLevel(definition);
			final int itemCost = getSkillCost(definition);
			final int refundAmount = getRefundAmount(itemCost);
			
			savedSkills.remove(Integer.valueOf(skillId));
			saveSelectedSkills(playerId, savedSkills);
			
			final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
			if (skill != null)
			{
				player.removeSkill(skill, true);
			}
			else
			{
				LOGGER.warning("Configured rebirth skill could not be removed because it was not found: playerId=" + playerId + " playerName=" + player.getName() + " skillId=" + skillId + " skillLevel=" + skillLevel + " definition=" + definition);
			}
			player.sendSkillList();
			
			if (refundAmount > 0)
			{
				player.addItem(ItemProcessType.REFUND, RebirthConfig.REBIRTH_REWARD_ITEM_ID, refundAmount, player, true);
				player.sendMessage("Refunded " + refundAmount + " rebirth token(s).");
			}
			else
			{
				player.sendMessage("Skill removed. Refund mode " + RebirthConfig.REBIRTH_SKILL_REFUND_MODE + " returned no tokens.");
			}
			
			displayMainWindow(player, objectId, savedSkills, rebirthCount, skillDefinitionMap);
		}
		finally
		{
			REBIRTH_SKILL_OPERATIONS_IN_PROGRESS.remove(Integer.valueOf(playerId));
		}
	}
	
	/**
	 * Generates an HTML string representing the list of acquired skills for a player.<br>
	 * It creates a table layout showing skill icons, names, levels, and costs.<br>
	 * If {@code showResetButton} is {@code true}, it includes a reset button for each skill.
	 * @param player The {@code Player} object whose skills are being displayed.
	 * @param selectedSkills A {@code List} of {@code Integer} IDs representing the chosen skills.
	 * @param skillDefinitionMap A {@code Map} linking skill IDs to their definition strings.
	 * @param showResetButton A {@code boolean} indicating whether to include a reset button in the HTML.
	 * @return A formatted HTML string containing the skill table or a default message if no skills are selected.
	 */
	private String buildAcquiredSkillsHtml(Player player, List<Integer> selectedSkills, Map<Integer, String> skillDefinitionMap, boolean showResetButton)
	{
		if (selectedSkills.isEmpty())
		{
			return "<center><font color=\"AAAAAA\">You don't have any skills selected yet.</font></center>";
		}
		
		final StringBuilder htmlBuilder = new StringBuilder();
		htmlBuilder.append("<table width=270 border=0 cellpadding=2 cellspacing=2><tr>");
		int column = 0;
		for (int skillId : selectedSkills)
		{
			final String definition = getSkillDefinition(skillDefinitionMap, skillId);
			if (definition == null)
			{
				LOGGER.warning("Selected rebirth skill definition was not found while building acquired skills HTML: playerId=" + player.getObjectId() + " playerName=" + player.getName() + " skillId=" + skillId);
				continue;
			}
			
			final int skillLevel = getSkillLevel(definition);
			final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
			if (skill == null)
			{
				LOGGER.warning("Selected rebirth skill was not found while building acquired skills HTML: playerId=" + player.getObjectId() + " playerName=" + player.getName() + " skillId=" + skillId + " skillLevel=" + skillLevel + " definition=" + definition);
				continue;
			}
			
			final int itemCost = getSkillCost(definition);
			if (column == SKILLS_PER_ROW)
			{
				htmlBuilder.append("</tr><tr>");
				column = 0;
			}
			
			final String icon = (skillId < SKILL_ICON_PADDING_THRESHOLD) ? ("0" + skillId) : String.valueOf(skillId);
			htmlBuilder.append("<td width=90 align=center>");
			htmlBuilder.append("<img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1>");
			htmlBuilder.append("<font color=\"FFDD99\">").append(skill.getName()).append("</font><br1>");
			htmlBuilder.append("<font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1>");
			htmlBuilder.append("<font color=\"99FF99\">Cost: ").append(itemCost).append("</font><br1>");
			
			if (showResetButton)
			{
				htmlBuilder.append("<button value=\"Reset\" action=\"bypass -h rebirth_resetSkill ").append(skillId).append("\" width=65 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>");
			}
			
			htmlBuilder.append("</td>");
			column++;
		}
		
		htmlBuilder.append("</tr></table>");
		return htmlBuilder.toString();
	}
	
	/**
	 * Retrieves the list of available rebirth skills for a specific player.<br>
	 * It checks if the {@code Player} is a mage to determine which skill set to return.
	 * @param player The {@code Player} object used to check the character class.
	 * @return A {@code List<String>} containing the relevant skill definitions.
	 */
	private List<String> getSkillPool(Player player)
	{
		return player.getPlayerClass().isMage() ? RebirthConfig.REBIRTH_MAGE_SKILLS : RebirthConfig.REBIRTH_FIGHTER_SKILLS;
	}
	
	/**
	 * Converts a list of raw skill definitions into a map indexed by skill ID.<br>
	 * This method validates each definition and logs warnings for invalid or duplicate entries.
	 * @param skillDefinitions The list of raw strings representing the skills.
	 * @param player The {@code Player} object associated with these skills.
	 * @return A {@code Map<Integer, String>} where keys are IDs and values are definitions.
	 */
	private Map<Integer, String> createSkillDefinitionMap(List<String> skillDefinitions, Player player)
	{
		final Map<Integer, String> skillDefinitionMap = new HashMap<>();
		for (String definition : skillDefinitions)
		{
			final int skillId = getSkillId(definition);
			if (skillId <= 0)
			{
				LOGGER.warning("Skipping invalid rebirth skill definition while building index: playerId=" + player.getObjectId() + " playerName=" + player.getName() + " definition=" + definition);
				continue;
			}
			
			final String previous = skillDefinitionMap.put(Integer.valueOf(skillId), definition);
			if (previous != null)
			{
				LOGGER.warning("Duplicate rebirth skill definition detected while building index: playerId=" + player.getObjectId() + " playerName=" + player.getName() + " skillId=" + skillId + " previousDefinition=" + previous + " newDefinition=" + definition);
			}
		}
		return skillDefinitionMap;
	}
	
	/**
	 * Converts a {@code List} of skill IDs into a {@code Set}.<br>
	 * This ensures that all skill IDs in the collection are unique.
	 * @param skills The list of skill IDs to convert.
	 * @return A new {@code HashSet} containing the unique skill IDs.
	 */
	private Set<Integer> createSkillSet(List<Integer> skills)
	{
		return new HashSet<>(skills);
	}
	
	/**
	 * Retrieves and validates the list of selected rebirth skills for a player.<br>
	 * It checks if each stored skill exists within the provided {@code skillDefinitionMap}.<br>
	 * If any stale skills are found, they are removed from the database.
	 * @param player The {@code Player} object whose skills are being loaded.
	 * @param skillDefinitionMap A map containing valid skill IDs and their definitions.
	 * @return A list of validated skill IDs for the player.
	 */
	private List<Integer> loadValidatedSelectedSkills(Player player, Map<Integer, String> skillDefinitionMap)
	{
		final int playerId = player.getObjectId();
		final List<Integer> storedSkills = getSelectedSkills(playerId);
		if (storedSkills.isEmpty())
		{
			return storedSkills;
		}
		
		final List<Integer> validSkills = new ArrayList<>(storedSkills.size());
		boolean changed = false;
		for (int skillId : storedSkills)
		{
			if (skillDefinitionMap.containsKey(Integer.valueOf(skillId)))
			{
				validSkills.add(Integer.valueOf(skillId));
			}
			else
			{
				changed = true;
				LOGGER.warning("Removing stale rebirth skill from persisted selection: playerId=" + playerId + " playerName=" + player.getName() + " skillId=" + skillId);
			}
		}
		
		if (changed)
		{
			saveSelectedSkills(playerId, validSkills);
		}
		return validSkills;
	}
	
	/**
	 * Retrieves the description for a specific skill.<br>
	 * It looks up the definition using the provided map and ID.
	 * @param skillDefinitionMap The map containing all skill definitions.
	 * @param skillId The unique identifier of the skill to find.
	 * @return The string definition of the skill or {@code null} if not found.
	 */
	private String getSkillDefinition(Map<Integer, String> skillDefinitionMap, int skillId)
	{
		return skillDefinitionMap.get(Integer.valueOf(skillId));
	}
	
	/**
	 * Retrieves the unique identifier for a skill from its definition string.<br>
	 * The method parses the first part of a comma-separated value.<br>
	 * It returns {@code 0} if the format is invalid or parsing fails.
	 * @param definition The raw string containing the skill data.
	 * @return The parsed integer ID of the skill, or {@code 0} on error.
	 */
	private int getSkillId(String definition)
	{
		final String[] parts = definition.split(",");
		if (parts.length < 2)
		{
			return 0;
		}
		
		try
		{
			return Integer.parseInt(parts[0].trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.log(Level.WARNING, "Failed to parse rebirth skill id from definition: definition=" + definition, e);
			return 0;
		}
	}
	
	/**
	 * Retrieves the skill level from a given definition string.<br>
	 * It splits the {@code definition} by commas and parses the second part.<br>
	 * If parsing fails or the format is incorrect, it returns 1.
	 * @param definition The raw skill definition string to parse.
	 * @return The parsed integer level of the skill.
	 */
	private int getSkillLevel(String definition)
	{
		final String[] parts = definition.split(",");
		if (parts.length < 2)
		{
			return 1;
		}
		
		try
		{
			return Integer.parseInt(parts[1].trim());
		}
		catch (NumberFormatException e)
		{
			LOGGER.log(Level.WARNING, "Failed to parse rebirth skill level from definition: definition=" + definition, e);
			return 1;
		}
	}
	
	/**
	 * Retrieves the cost of a specific skill from its definition string.<br>
	 * It parses the third part of a comma-separated value.<br>
	 * Returns {@code 1} if the format is invalid or parsing fails.
	 * @param definition The raw string containing the skill data.
	 * @return The integer cost of the skill.
	 */
	private int getSkillCost(String definition)
	{
		final String[] parts = definition.split(",");
		if (parts.length < 3)
		{
			return 1;
		}
		
		try
		{
			return Math.max(1, Integer.parseInt(parts[2].trim()));
		}
		catch (NumberFormatException e)
		{
			LOGGER.log(Level.WARNING, "Failed to parse rebirth skill cost from definition: definition=" + definition, e);
			return 1;
		}
	}
	
	/**
	 * Calculates the amount of currency to refund based on the configuration.<br>
	 * It checks the {@code RebirthConfig.REBIRTH_SKILL_REFUND_MODE} value.
	 * @param itemCost The original cost of the item.
	 * @return The calculated refund amount as an {@code int}.
	 */
	private int getRefundAmount(int itemCost)
	{
		switch (RebirthConfig.REBIRTH_SKILL_REFUND_MODE)
		{
			case "FULL":
			{
				return itemCost;
			}
			case "MID":
			{
				return itemCost / 2;
			}
			default:
			{
				return 0;
			}
		}
	}
	
	/**
	 * Retrieves the quantity of a specific item in a player's inventory.<br>
	 * It checks if the item exists and returns its count.
	 * @param player The {@code Player} whose inventory is being checked.
	 * @param itemId The unique identifier for the item to count.
	 * @return The number of items found, or 0 if the item does not exist.
	 */
	private long getItemCount(Player player, int itemId)
	{
		final Item item = player.getInventory().getItemByItemId(itemId);
		return item != null ? item.getCount() : 0;
	}
	
	/**
	 * Initiates the rebirth process for a specific player.<br>
	 * This method checks all requirements such as level, class status, and item availability.<br>
	 * If successful, it consumes the required items and increments the player's rebirth count.
	 * @param player The {@code Player} who is requesting to be reborn.
	 */
	public void requestRebirth(Player player)
	{
		final int playerId = player.getObjectId();
		if (!REBIRTH_OPERATIONS_IN_PROGRESS.add(Integer.valueOf(playerId)))
		{
			player.sendMessage(REBIRTH_BUSY_MESSAGE);
			return;
		}
		
		try
		{
			if (!RebirthConfig.REBIRTH_ALLOW_REBIRTH)
			{
				player.sendMessage("Rebirth is disabled by configuration.");
				return;
			}
			if (player.getLevel() < RebirthConfig.REBIRTH_MIN_LEVEL)
			{
				player.sendMessage("You do not meet the minimum level for Rebirth (" + RebirthConfig.REBIRTH_MIN_LEVEL + ").");
				return;
			}
			if (player.isSubClassActive())
			{
				player.sendMessage("Switch to your main class to request Rebirth.");
				return;
			}
			if (player.isAlikeDead() || player.isInDuel() || player.isCastingNow() || player.isAttackingNow())
			{
				player.sendMessage("You cannot request Rebirth right now.");
				return;
			}
			
			final int currentRebirthCount = getRebirthLevel(player);
			if (currentRebirthCount >= RebirthConfig.REBIRTH_MAX_COUNT)
			{
				player.sendMessage("You are currently at your maximum rebirth count.");
				return;
			}
			
			if (!consumeItem(player, RebirthConfig.REBIRTH_ITEM_ID, RebirthConfig.REBIRTH_ITEM_AMOUNT, ItemProcessType.DESTROY, "rebirth request"))
			{
				return;
			}
			
			grantRebirth(player, currentRebirthCount + 1, currentRebirthCount == 0);
		}
		finally
		{
			REBIRTH_OPERATIONS_IN_PROGRESS.remove(Integer.valueOf(playerId));
		}
	}
	
	/**
	 * Processes the rebirth logic for a player.<br>
	 * This method updates the database, resets character stats, and handles skill management.<br>
	 * It also grants rewards and updates the UI after a successful rebirth.
	 * @param player The {@code Player} instance to perform the rebirth on.
	 * @param newCount The new rebirth count to assign to the player.
	 * @param firstBirth A boolean indicating if this is the player's first rebirth.
	 */
	private void grantRebirth(Player player, int newCount, boolean firstBirth)
	{
		final int playerId = player.getObjectId();
		final String playerName = player.getName();
		
		try
		{
			// Critical persistence step.
			final boolean updated = firstBirth ? insertFirst(playerId) : updateCount(playerId, newCount);
			if (!updated)
			{
				player.sendMessage("Rebirth could not be completed due to a database error.");
				LOGGER.severe("Aborting rebirth because rebirth count persistence failed: playerId=" + playerId + " playerName=" + playerName + " newCount=" + newCount + " firstBirth=" + firstBirth);
				return;
			}
			
			// Critical player mutation step.
			final long currentExp = player.getExp();
			final long targetExp = ExperienceData.getInstance().getExpForLevel(1);
			if (currentExp > targetExp)
			{
				player.getStat().setLevel(1);
				player.removeExpAndSp(currentExp - targetExp, 0);
				player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
				player.setCurrentCp(player.getMaxCp());
				player.broadcastUserInfo();
			}
			
			player.setPlayerClass(player.getBaseClass());
			if (RebirthConfig.REBIRTH_DELETE_ALL_SKILLS)
			{
				for (Skill skill : player.getAllSkills())
				{
					if (skill == null)
					{
						continue;
					}
					
					if (RebirthConfig.REBIRTH_SKILL_DELETE_WHITELIST.contains(Integer.valueOf(skill.getId())))
					{
						continue;
					}
					if (isProtectedByGroup(player, skill))
					{
						continue;
					}
					
					player.removeSkill(skill, true);
				}
			}
			player.giveAvailableSkills(true, true, true, false);
			if (RebirthConfig.REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS)
			{
				player.regiveTemporarySkills();
			}
			player.storeMe();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed during critical rebirth section: playerId=" + playerId + " playerName=" + playerName + " newCount=" + newCount + " firstBirth=" + firstBirth, e);
			player.sendMessage("Rebirth could not be completed due to an internal error.");
			return;
		}
		
		try
		{
			// Post-commit section.
			final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
			final List<Integer> selectedSkills = loadValidatedSelectedSkills(player, skillDefinitionMap);
			grantRewardTokens(player);
			if (!RebirthConfig.REBIRTH_DELETE_RESTORE_TEMPORARY_SKILLS)
			{
				grantRebirthSkills(player, skillDefinitionMap, selectedSkills);
			}
			player.sendSkillList();
			player.broadcastUserInfo();
			player.broadcastStatusUpdate();
			displayCongrats(player, newCount);
			player.sendMessage("Rebirth applied successfully. You have been reborn!");
			routePostRebirthUi(player, selectedSkills, newCount, skillDefinitionMap);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed during post-commit rebirth section: playerId=" + playerId + " playerName=" + playerName + " newCount=" + newCount, e);
		}
	}
	
	/**
	 * Gives reward tokens to the specified player.<br>
	 * This method checks if rewards are enabled in {@code RebirthConfig}.<br>
	 * It adds the items to the inventory and sends a confirmation message.
	 * @param player The {@code Player} who will receive the reward.
	 */
	private void grantRewardTokens(Player player)
	{
		if ((RebirthConfig.REBIRTH_REWARD_ITEM_ID <= 0) || (RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT <= 0))
		{
			return;
		}
		
		player.addItem(ItemProcessType.REWARD, RebirthConfig.REBIRTH_REWARD_ITEM_ID, RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT, player, true);
		player.sendMessage("You received " + RebirthConfig.REBIRTH_REWARD_ITEM_AMOUNT + " rebirth token(s).");
	}
	
	/**
	 * Checks if a specific skill belongs to a protected group.<br>
	 * This method verifies if the {@code Skill} should be excluded from deletion based on configuration.<br>
	 * It evaluates various categories such as starting classes, common skills, and special trees.
	 * @param player The {@code Player} instance to check.
	 * @param skill The {@code Skill} object to verify.
	 * @return {@code true} if the skill is protected; {@code false} otherwise.
	 */
	private boolean isProtectedByGroup(Player player, Skill skill)
	{
		if ((player == null) || (skill == null) || RebirthConfig.REBIRTH_DELETE_PROTECTED_SKILL_GROUPS.isEmpty())
		{
			return false;
		}
		
		if (isProtectedSkillGroupEnabled("STARTING_CLASS") && isStartingClassSkill(player, skill))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("COMMON") && isSkillInTree(skill, SkillTreeData.getInstance().getCommonSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("FISHING") && isSkillInTree(skill, SkillTreeData.getInstance().getFishingSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("NOBLE") && isSkillInList(skill, SkillTreeData.getInstance().getNobleSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("HERO") && isSkillInList(skill, SkillTreeData.getInstance().getHeroSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("CLAN") && isSkillInTree(skill, SkillTreeData.getInstance().getPledgeSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("SUBPLEDGE") && isSkillInTree(skill, SkillTreeData.getInstance().getSubPledgeSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("COLLECT") && isSkillInTree(skill, SkillTreeData.getInstance().getCollectSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("SUBCLASS") && isSkillInTree(skill, SkillTreeData.getInstance().getSubClassSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("TRANSFORM") && isSkillInTree(skill, SkillTreeData.getInstance().getTransformSkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("TRANSFER") && isTransferSkill(player, skill))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("RACE") && isRaceSkill(player, skill))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("REVELATION") && isRevelationSkill(player, skill))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("ABILITY") && isSkillInTree(skill, SkillTreeData.getInstance().getAbilitySkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("ALCHEMY") && isSkillInTree(skill, SkillTreeData.getInstance().getAlchemySkillTree()))
		{
			return true;
		}
		if (isProtectedSkillGroupEnabled("DUAL_CLASS") && isDualClassSkill(skill))
		{
			return true;
		}
		return false;
	}
	
	/**
	 * Checks if a specific skill group is protected from deletion.<br>
	 * This method looks up the group name in the {@code RebirthConfig}.
	 * @param groupName The name of the skill group to check.
	 * @return {@code true} if the group is protected, {@code false} otherwise.
	 */
	private boolean isProtectedSkillGroupEnabled(String groupName)
	{
		return RebirthConfig.REBIRTH_DELETE_PROTECTED_SKILL_GROUPS.contains(groupName);
	}
	
	/**
	 * Checks if a specific {@code Skill} exists within the provided {@code Map} tree.<br>
	 * It validates that neither input is {@code null} and that the tree is not empty.<br>
	 * The method looks for the unique hash code of the skill based on its ID and level.
	 * @param skill The {@code Skill} object to check.
	 * @param tree A {@code Map} representing the skill tree structure.
	 * @return {@code true} if the skill is found in the tree, otherwise {@code false}.
	 */
	private boolean isSkillInTree(Skill skill, Map<Long, ?> tree)
	{
		if ((skill == null) || (tree == null) || tree.isEmpty())
		{
			return false;
		}
		
		return tree.containsKey(Long.valueOf(SkillData.getSkillHashCode(skill.getId(), skill.getLevel())));
	}
	
	/**
	 * Checks if a specific {@code Skill} exists within a provided list.<br>
	 * It compares both the ID and the level of the skill.<br>
	 * Returns {@code false} if either input is {@code null} or the list is empty.
	 * @param skill The {@code Skill} object to search for.
	 * @param tree The {@code List} of {@code Skill} objects to check against.
	 * @return {@code true} if a matching skill is found, otherwise {@code false}.
	 */
	private boolean isSkillInList(Skill skill, List<Skill> tree)
	{
		if ((skill == null) || (tree == null) || tree.isEmpty())
		{
			return false;
		}
		
		for (Skill treeSkill : tree)
		{
			if ((treeSkill != null) && (treeSkill.getId() == skill.getId()) && (treeSkill.getLevel() == skill.getLevel()))
			{
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Checks if a specific skill belongs to the player's starting class tree.<br>
	 * It identifies the root class of the {@code Player}.<br>
	 * It then verifies if the {@code Skill} exists within that class's complete skill tree.
	 * @param player The {@code Player} object to check.
	 * @param skill The {@code Skill} instance to verify.
	 * @return {@code true} if the skill is part of the starting class tree, {@code false} otherwise.
	 */
	private boolean isStartingClassSkill(Player player, Skill skill)
	{
		if ((player == null) || (skill == null))
		{
			return false;
		}
		
		final int rootClassId = getRootClassId(player.getBaseClass());
		final PlayerClass rootClass = PlayerClass.getPlayerClass(rootClassId);
		if (rootClass == null)
		{
			return false;
		}
		
		return isSkillInTree(skill, SkillTreeData.getInstance().getCompleteClassSkillTree(rootClass));
	}
	
	/**
	 * Checks if a specific skill belongs to the transfer tree of the player's class.<br>
	 * This method validates both the {@code Player} and the {@code Skill} objects.<br>
	 * It returns {@code true} if the skill is part of the allowed tree for that class.
	 * @param player The {@code Player} object to check.
	 * @param skill The {@code Skill} object to verify.
	 * @return {@code true} if the skill is a valid transfer skill, otherwise {@code false}.
	 */
	private boolean isTransferSkill(Player player, Skill skill)
	{
		if ((player == null) || (skill == null))
		{
			return false;
		}
		
		final PlayerClass playerClass = PlayerClass.getPlayerClass(player.getBaseClass());
		if (playerClass == null)
		{
			return false;
		}
		
		return isSkillInTree(skill, SkillTreeData.getInstance().getTransferSkillTree(playerClass));
	}
	
	/**
	 * Checks if a specific skill belongs to the player's race.<br>
	 * It compares the skill ID and level against the race skill tree.
	 * @param player The {@code Player} object to check.
	 * @param skill The {@code Skill} object to verify.
	 * @return {@code true} if the skill is part of the race tree, otherwise {@code false}.
	 */
	private boolean isRaceSkill(Player player, Skill skill)
	{
		if ((player == null) || (skill == null))
		{
			return false;
		}
		
		return SkillTreeData.getInstance().getRaceSkillTree(player.getRace()).stream().anyMatch(skillLearn -> (skillLearn != null) && (skillLearn.getSkillId() == skill.getId()) && (skillLearn.getSkillLevel() == skill.getLevel()));
	}
	
	/**
	 * Checks if a specific skill is classified as a revelation skill.<br>
	 * It verifies the skill against both base and dual class types.
	 * @param player The {@code Player} object to check.
	 * @param skill The {@code Skill} object to verify.
	 * @return {@code true} if the skill is a revelation skill, otherwise {@code false}.
	 */
	private boolean isRevelationSkill(Player player, Skill skill)
	{
		if ((player == null) || (skill == null))
		{
			return false;
		}
		
		return (SkillTreeData.getInstance().getRevelationSkill(org.l2jmobius.gameserver.entity.actor.enums.player.SubclassType.BASECLASS, skill.getId(), skill.getLevel()) != null) || (SkillTreeData.getInstance().getRevelationSkill(org.l2jmobius.gameserver.entity.actor.enums.player.SubclassType.DUALCLASS, skill.getId(), skill.getLevel()) != null);
	}
	
	/**
	 * Checks if a specific skill is designated as a dual class skill.<br>
	 * It verifies the skill exists in the {@code SkillTreeData} for the given level.
	 * @param skill The {@code Skill} object to check.
	 * @return {@code true} if the skill is a dual class skill, otherwise {@code false}.
	 */
	private boolean isDualClassSkill(Skill skill)
	{
		if (skill == null)
		{
			return false;
		}
		
		return SkillTreeData.getInstance().getDualClassSkill(skill.getId(), skill.getLevel()) != null;
	}
	
	/**
	 * Finds the root class ID for a given class ID.<br>
	 * It first checks the {@code ROOT_CLASS_IDS} map.<br>
	 * If not found, it attempts to retrieve the ID from {@code PlayerClass}.<br>
	 * Returns the original {@code classId} if no mapping exists.
	 * @param classId The unique identifier of the class.
	 * @return The root class ID associated with the provided class.
	 */
	private int getRootClassId(int classId)
	{
		ensureRootClassIdsLoaded();
		
		final Integer rootClassId = ROOT_CLASS_IDS.get(Integer.valueOf(classId));
		if (rootClassId != null)
		{
			return rootClassId.intValue();
		}
		
		final PlayerClass playerClass = PlayerClass.getPlayerClass(classId);
		if (playerClass != null)
		{
			return playerClass.getRootClass().getId();
		}
		return classId;
	}
	
	/**
	 * Checks if the root class IDs have been loaded into memory.<br>
	 * If they are not loaded, this method calls {@code loadRootClassIds()} to initialize them.<br>
	 * It uses a synchronized block to ensure thread safety during the loading process.
	 */
	private void ensureRootClassIdsLoaded()
	{
		if (ROOT_CLASS_IDS_LOADED)
		{
			return;
		}
		
		synchronized (ROOT_CLASS_IDS)
		{
			if (ROOT_CLASS_IDS_LOADED)
			{
				return;
			}
			
			loadRootClassIds();
			ROOT_CLASS_IDS_LOADED = true;
		}
	}
	
	/**
	 * Loads the root class IDs from the configuration file.<br>
	 * This method parses the XML file to map each class to its ultimate parent.<br>
	 * It populates the {@code ROOT_CLASS_IDS} map for use in rebirth logic.
	 */
	private void loadRootClassIds()
	{
		final File file = new File(CLASS_LIST_FILE);
		if (!file.exists())
		{
			LOGGER.warning("Rebirth class list file not found: " + CLASS_LIST_FILE);
			return;
		}
		
		final Map<Integer, Integer> parentClassIds = new HashMap<>();
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			final NodeList classNodes = factory.newDocumentBuilder().parse(file).getElementsByTagName("class");
			for (int i = 0; i < classNodes.getLength(); i++)
			{
				final Node classNode = classNodes.item(i);
				final int classId = parseIntAttribute(classNode, "classId", -1);
				if (classId < 0)
				{
					continue;
				}
				
				parentClassIds.put(Integer.valueOf(classId), Integer.valueOf(parseIntAttribute(classNode, "parentClassId", -1)));
			}
			
			for (Integer classId : parentClassIds.keySet())
			{
				ROOT_CLASS_IDS.put(classId, Integer.valueOf(resolveRootClassId(classId.intValue(), parentClassIds)));
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to parse rebirth class list file: " + CLASS_LIST_FILE, e);
		}
	}
	
	/**
	 * Finds the root class ID by traversing up the inheritance tree.<br>
	 * It follows parents until it reaches a null or negative ID.<br>
	 * This method prevents infinite loops using a visited set.
	 * @param classId The starting class ID to check.
	 * @param parentClassIds A map linking child class IDs to their parent class IDs.
	 * @return The final root class ID found during traversal.
	 */
	private int resolveRootClassId(int classId, Map<Integer, Integer> parentClassIds)
	{
		int currentClassId = classId;
		final Set<Integer> visited = new HashSet<>();
		while (visited.add(Integer.valueOf(currentClassId)))
		{
			final Integer parentClassId = parentClassIds.get(Integer.valueOf(currentClassId));
			if ((parentClassId == null) || (parentClassId.intValue() < 0))
			{
				return currentClassId;
			}
			currentClassId = parentClassId.intValue();
		}
		return classId;
	}
	
	/**
	 * Retrieves an integer value from a specific attribute of a {@code Node}.<br>
	 * It returns the {@code defaultValue} if the node, attribute, or value is missing.<br>
	 * If the attribute value is not a valid number, it also returns the {@code defaultValue}.
	 * @param node The {@code Node} to search for an attribute.
	 * @param attributeName The name of the attribute to retrieve.
	 * @param defaultValue The value to return if parsing fails or data is missing.
	 * @return The parsed integer value or the provided default.
	 */
	private int parseIntAttribute(Node node, String attributeName, int defaultValue)
	{
		if ((node == null) || (attributeName == null) || !node.hasAttributes() || (node.getAttributes().getNamedItem(attributeName) == null))
		{
			return defaultValue;
		}
		
		try
		{
			return Integer.parseInt(node.getAttributes().getNamedItem(attributeName).getNodeValue());
		}
		catch (NumberFormatException e)
		{
			return defaultValue;
		}
	}
	
	/**
	 * Displays a congratulatory message to the player after a successful rebirth.<br>
	 * This method handles social actions, visual effects, and screen messages based on configuration.<br>
	 * It also broadcasts a global announcement if enabled in the settings.
	 * @param player The {@code Player} who completed the rebirth process.
	 * @param rebirthCount The total number of rebirths the player has achieved.
	 */
	private void displayCongrats(Player player, int rebirthCount)
	{
		if (RebirthConfig.REBIRTH_FINISH_SOCIAL_ID > 0)
		{
			player.broadcastPacket(new SocialAction(player.getObjectId(), RebirthConfig.REBIRTH_FINISH_SOCIAL_ID));
		}
		if (RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_ID > 0)
		{
			player.broadcastPacket(new MagicSkillUse(player, player, RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_ID, RebirthConfig.REBIRTH_FINISH_EFFECT_SKILL_LVL, 1, 0));
		}
		
		final String name = player.getName();
		final String screenMessage = RebirthConfig.REBIRTH_SCREEN_MESSAGE.replace("%player%", name).replace("%rebirth_count%", String.valueOf(rebirthCount)).replace("%max_rebirth%", String.valueOf(RebirthConfig.REBIRTH_MAX_COUNT));
		final String globalAnnouncement = RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT.replace("%player%", name).replace("%rebirth_count%", String.valueOf(rebirthCount)).replace("%max_rebirth%", String.valueOf(RebirthConfig.REBIRTH_MAX_COUNT));
		int screenPosition = ExShowScreenMessage.TOP_CENTER;
		
		switch (RebirthConfig.REBIRTH_SCREEN_MESSAGE_POSITION)
		{
			case "TOP_LEFT":
			{
				screenPosition = ExShowScreenMessage.TOP_LEFT;
				break;
			}
			case "TOP_CENTER":
			{
				screenPosition = ExShowScreenMessage.TOP_CENTER;
				break;
			}
			case "TOP_RIGHT":
			{
				screenPosition = ExShowScreenMessage.TOP_RIGHT;
				break;
			}
			case "MIDDLE_LEFT":
			{
				screenPosition = ExShowScreenMessage.MIDDLE_LEFT;
				break;
			}
			case "MIDDLE_CENTER":
			{
				screenPosition = ExShowScreenMessage.MIDDLE_CENTER;
				break;
			}
			case "MIDDLE_RIGHT":
			{
				screenPosition = ExShowScreenMessage.MIDDLE_RIGHT;
				break;
			}
			case "BOTTOM_CENTER":
			{
				screenPosition = ExShowScreenMessage.BOTTOM_CENTER;
				break;
			}
			case "BOTTOM_RIGHT":
			{
				screenPosition = ExShowScreenMessage.BOTTOM_RIGHT;
				break;
			}
		}
		
		if (RebirthConfig.REBIRTH_SCREEN_MESSAGE_ENABLED)
		{
			player.sendPacket(new ExShowScreenMessage(screenMessage, screenPosition, RebirthConfig.REBIRTH_SCREEN_MESSAGE_TIME));
		}
		
		if (RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT_ENABLED)
		{
			World.broadcastToAllOnlinePlayers(globalAnnouncement, RebirthConfig.REBIRTH_GLOBAL_ANNOUNCEMENT_CRITICAL);
		}
		
		player.sendMessage("Congratulations " + name + "! You have been Reborn!");
	}
	
	/**
	 * Handles the UI logic for a player's rebirth process.<br>
	 * It displays the main window and sends a notification message.
	 * @param player The {@code Player} who is performing the rebirth.
	 * @param selectedSkills A {@code List} of IDs for the skills chosen by the player.
	 * @param rebirthCount The total number of rebirths performed by the player.
	 * @param skillDefinitionMap A {@code Map} containing the definitions for each skill.
	 */
	private void routePostRebirthUi(Player player, List<Integer> selectedSkills, int rebirthCount, Map<Integer, String> skillDefinitionMap)
	{
		displayMainWindow(player, 0, selectedSkills, rebirthCount, skillDefinitionMap);
		player.sendMessage("Use your rebirth tokens to purchase skills.");
	}
	
	/**
	 * Plays the visual effects for the rebirth selection screen.<br>
	 * This method broadcasts a social action and an optional magic skill effect to other players.<br>
	 * It uses values defined in {@code RebirthConfig}.
	 * @param player The {@code Player} who is performing the rebirth selection.
	 */
	public void playRebirthSelectFx(Player player)
	{
		if (RebirthConfig.REBIRTH_SELECT_SOCIAL_ID > 0)
		{
			player.broadcastPacket(new SocialAction(player.getObjectId(), RebirthConfig.REBIRTH_SELECT_SOCIAL_ID));
		}
		if (RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_ID > 0)
		{
			player.broadcastPacket(new MagicSkillUse(player, player, RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_ID, RebirthConfig.REBIRTH_SELECT_EFFECT_SKILL_LVL, 1, 0));
		}
	}
	
	/**
	 * Removes a specific quantity of an item from the player's inventory.<br>
	 * This method validates if the player has enough items before processing.<br>
	 * It logs a warning if the input parameters are invalid.
	 * @param player The {@code Player} who will lose the items.
	 * @param itemId The unique identifier of the item to consume.
	 * @param amount The quantity of the item to remove.
	 * @param processType The type of processing used for destruction.
	 * @param reason A description of why the items are being consumed.
	 * @return {@code true} if the items were successfully removed, {@code false} otherwise.
	 */
	private boolean consumeItem(Player player, int itemId, long amount, ItemProcessType processType, String reason)
	{
		if ((player == null) || (itemId <= 0) || (amount <= 0) || (amount > Integer.MAX_VALUE))
		{
			LOGGER.warning("Invalid consumeItem request: player=" + player + " itemId=" + itemId + " amount=" + amount + " processType=" + processType + " reason=" + reason);
			return false;
		}
		
		final Item item = player.getInventory().getItemByItemId(itemId);
		if ((item == null) || (item.getCount() < amount))
		{
			final String itemName = ((item != null) && (item.getTemplate() != null)) ? item.getTemplate().getName() : ("Item " + itemId);
			player.sendMessage("You need at least " + amount + " [ " + itemName + " ] for " + reason + ".");
			return false;
		}
		
		player.getInventory().destroyItem(processType, item, (int) amount, player, null);
		return true;
	}
	
	/**
	 * Grants the selected rebirth skills to a specific {@code Player}.<br>
	 * This method processes the validated skill list for the user.
	 * @param player The {@code Player} receiving the skills.
	 */
	public void grantRebirthSkills(Player player)
	{
		final Map<Integer, String> skillDefinitionMap = createSkillDefinitionMap(getSkillPool(player), player);
		grantRebirthSkills(player, skillDefinitionMap, loadValidatedSelectedSkills(player, skillDefinitionMap));
	}
	
	/**
	 * Grants selected rebirth skills to a {@code Player}.<br>
	 * This method checks if the player is eligible based on configuration.<br>
	 * It iterates through the provided IDs and adds valid skills to the player.<br>
	 * Finally, it sends an updated skill list to the client.
	 * @param player The {@code Player} receiving the skills.
	 * @param skillDefinitionMap A map containing the definitions for each skill ID.
	 * @param selectedSkills A list of skill IDs that the player has chosen to acquire.
	 */
	private void grantRebirthSkills(Player player, Map<Integer, String> skillDefinitionMap, List<Integer> selectedSkills)
	{
		if (!RebirthConfig.REBIRTH_INHERIT_SKILLS_TO_SUBCLASSES && (player.getClassIndex() > 0))
		{
			return;
		}
		
		for (int skillId : selectedSkills)
		{
			final String definition = getSkillDefinition(skillDefinitionMap, skillId);
			if (definition == null)
			{
				LOGGER.warning("Skipping selected rebirth skill because definition was not found: playerId=" + player.getObjectId() + " playerName=" + player.getName() + " skillId=" + skillId);
				continue;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(skillId, getSkillLevel(definition));
			if (skill != null)
			{
				player.addSkill(skill, true);
			}
			else
			{
				LOGGER.warning("Skipping selected rebirth skill because skill data was not found: playerId=" + player.getObjectId() + " playerName=" + player.getName() + " skillId=" + skillId + " definition=" + definition);
			}
		}
		player.sendSkillList();
	}
	
	/**
	 * Retrieves the current rebirth level of a specific player.<br>
	 * This method counts how many times the player has undergone the rebirth process.
	 * @param player The {@code Player} object to check.
	 * @return The total number of rebirths for the player as an {@code int}.
	 */
	public int getRebirthLevel(Player player)
	{
		return getCount(player.getObjectId());
	}
	
	/**
	 * Retrieves the number of rebirths for a specific character.<br>
	 * This method queries the database using the provided {@code charId}.<br>
	 * It returns 0 if no record is found or if an error occurs.
	 * @param charId The unique identifier of the character.
	 * @return The total count of rebirths for the character.
	 */
	public int getCount(int charId)
	{
		int count = 0;
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(SELECT_REBIRTH_COUNT))
		{
			statement.setInt(1, charId);
			try (ResultSet resultSet = statement.executeQuery())
			{
				if (resultSet.next())
				{
					count = resultSet.getInt(1);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to load rebirth count: charId=" + charId, e);
		}
		return count;
	}
	
	/**
	 * Inserts a new record for the first rebirth into the database.<br>
	 * This method uses the provided {@code charId} to identify the character.
	 * @param charId The unique identifier of the character.
	 * @return {@code true} if the insertion was successful, {@code false} otherwise.
	 */
	public boolean insertFirst(int charId)
	{
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(INSERT_FIRST_REBIRTH))
		{
			statement.setInt(1, charId);
			return statement.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to insert first rebirth record: charId=" + charId, e);
		}
		return false;
	}
	
	/**
	 * Updates the rebirth count for a specific character in the database.<br>
	 * This method attempts to save the {@code newCount} value associated with the given {@code charId}.
	 * @param charId The unique identifier of the character.
	 * @param newCount The new rebirth count value to be saved.
	 * @return {@code true} if the database update was successful, otherwise {@code false}.
	 */
	public boolean updateCount(int charId, int newCount)
	{
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(UPDATE_REBIRTH_COUNT))
		{
			statement.setInt(1, newCount);
			statement.setInt(2, charId);
			return statement.executeUpdate() > 0;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to update rebirth count: charId=" + charId + " newCount=" + newCount, e);
		}
		return false;
	}
	
	/**
	 * Retrieves the list of selected rebirth skills for a specific character.<br>
	 * This method fetches data from the database using the provided {@code charId}.<br>
	 * It parses the comma-separated string into a list of integers.
	 * @param charId The unique identifier of the character.
	 * @return A {@code List<Integer>} containing the selected skill IDs.
	 */
	public List<Integer> getSelectedSkills(int charId)
	{
		final List<Integer> skills = new ArrayList<>();
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(SELECT_SELECTED_SKILLS))
		{
			statement.setInt(1, charId);
			try (ResultSet resultSet = statement.executeQuery())
			{
				if (resultSet.next())
				{
					final String data = resultSet.getString(1);
					if ((data != null) && !data.isEmpty())
					{
						for (String skillValue : data.split(","))
						{
							try
							{
								skills.add(Integer.valueOf(Integer.parseInt(skillValue.trim())));
							}
							catch (NumberFormatException e)
							{
								LOGGER.log(Level.WARNING, "Failed to parse persisted rebirth skill id: charId=" + charId + " rawValue=" + skillValue + " data=" + data, e);
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to load selected rebirth skills: charId=" + charId, e);
		}
		return skills;
	}
	
	/**
	 * Saves the list of selected rebirth skills for a specific character.<br>
	 * This method updates the database with the provided skill IDs.
	 * @param charId The unique identifier of the character.
	 * @param skills A {@code List} of integers representing the selected skill IDs.
	 */
	public void saveSelectedSkills(int charId, List<Integer> skills)
	{
		try (Connection connection = DatabaseFactory.getConnection();
			PreparedStatement statement = connection.prepareStatement(UPDATE_SELECTED_SKILLS))
		{
			final StringBuilder skillBuilder = new StringBuilder();
			for (int i = 0; i < skills.size(); i++)
			{
				if (i > 0)
				{
					skillBuilder.append(',');
				}
				skillBuilder.append(skills.get(i));
			}
			
			statement.setString(1, skillBuilder.toString());
			statement.setInt(2, charId);
			statement.executeUpdate();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, "Failed to save selected rebirth skills: charId=" + charId + " skills=" + skills, e);
		}
	}
	
	/**
	 * Displays a preview window for a specific rebirth skill to the player.<br>
	 * It checks if the player has enough tokens and meets the selection limits.<br>
	 * The method generates an HTML message containing skill details and purchase buttons.
	 * @param player The {@code Player} who will view the preview.
	 * @param objectId The {@code int} ID of the NPC or object sending the window.
	 * @param skillId The {@code int} ID of the skill to preview.
	 * @param skillDefinitionMap A {@code Map<Integer, String>} containing skill definitions.
	 * @param savedSkills A {@code List<Integer>} of skills already selected by the player.
	 * @param savedSkillSet A {@code Set<Integer>} for quick lookup of selected skills.
	 * @param rebirthCount The current number of rebirths the player has completed.
	 */
	private void displaySkillPreviewWindow(Player player, int objectId, int skillId, Map<Integer, String> skillDefinitionMap, List<Integer> savedSkills, Set<Integer> savedSkillSet, int rebirthCount)
	{
		final String definition = getSkillDefinition(skillDefinitionMap, skillId);
		if (definition == null)
		{
			player.sendMessage("Invalid skill.");
			return;
		}
		
		final int targetLevel = getSkillLevel(definition);
		final Skill skill = SkillData.getInstance().getSkill(skillId, targetLevel);
		if (skill == null)
		{
			LOGGER.warning("Configured rebirth skill preview was not found: playerId=" + player.getObjectId() + " playerName=" + player.getName() + " skillId=" + skillId + " targetLevel=" + targetLevel + " definition=" + definition);
			player.sendMessage("Invalid skill.");
			return;
		}
		
		final int itemCost = getSkillCost(definition);
		final int allowedSkills = Math.min(rebirthCount, RebirthConfig.REBIRTH_MAX_SELECTED_SKILLS);
		final long availableTokens = getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID);
		final boolean alreadySelected = savedSkillSet.contains(Integer.valueOf(skillId));
		final boolean limitReached = savedSkills.size() >= allowedSkills;
		final boolean hasEnoughTokens = availableTokens >= itemCost;
		
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		final StringBuilder htmlBuilder = new StringBuilder();
		
		htmlBuilder.append("<html><body><center>");
		htmlBuilder.append("<br>");
		htmlBuilder.append("<font color=\"LEVEL\">Rebirth Manager</font><br>");
		htmlBuilder.append("<table width=256 border=0 cellpadding=1 cellspacing=2>");
		htmlBuilder.append("<tr><td width=95 align=center>");
		htmlBuilder.append("<font color=\"66CC66\">Your Rebirths</font><br1>");
		htmlBuilder.append("<font color=\"FFFF99\">").append(rebirthCount).append('/').append(RebirthConfig.REBIRTH_MAX_COUNT).append("</font><br1>");
		htmlBuilder.append("<font color=\"99FF99\">Tokens: ").append(availableTokens).append("</font>");
		htmlBuilder.append("</td><td width=95 align=center>");
		final String icon = (skillId < SKILL_ICON_PADDING_THRESHOLD) ? ("0" + skillId) : String.valueOf(skillId);
		htmlBuilder.append("<img src=\"icon.skill").append(icon).append("\" width=32 height=32><br1>");
		htmlBuilder.append("<font color=\"FFDD99\">").append(skill.getName()).append("</font><br1>");
		htmlBuilder.append("<font color=\"FFFF99\">Lv. ").append(skill.getLevel()).append("</font><br1>");
		htmlBuilder.append("<font color=\"99FF99\">Cost: ").append(itemCost).append("</font>");
		htmlBuilder.append("</td></tr></table>");
		htmlBuilder.append("<br1>");
		htmlBuilder.append("<font color=\"LEVEL\">This purchase consumes ").append(itemCost).append(" token(s).</font><br1>");
		htmlBuilder.append("<font color=\"LEVEL\">Refund: ").append(RebirthConfig.REBIRTH_SKILL_REFUND_MODE).append("</font><br1>");
		htmlBuilder.append("<font color=\"66CC66\">Rebirth Skills Obtained</font><br1>");
		htmlBuilder.append(buildAcquiredSkillsHtml(player, savedSkills, skillDefinitionMap, false));
		htmlBuilder.append("<br1>");
		if (!alreadySelected && !limitReached && hasEnoughTokens)
		{
			htmlBuilder.append("<button value=\"Confirm\" action=\"bypass -h rebirth_confirmSkill ").append(skillId).append("\" width=95 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"><br1>");
		}
		else
		{
			if (alreadySelected)
			{
				htmlBuilder.append("<font color=AAAAAA>Already selected.</font><br1>");
			}
			if (limitReached)
			{
				htmlBuilder.append("<font color=AAAAAA>Skill limit reached.</font><br1>");
			}
			if (!hasEnoughTokens)
			{
				htmlBuilder.append("<font color=FF6666>Not enough tokens.</font><br1>");
			}
		}
		
		htmlBuilder.append("<button value=\"Return\" action=\"bypass -h rebirth_selectskills\" width=95 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		htmlBuilder.append("</center></body></html>");
		
		html.setHtml(htmlBuilder.toString());
		player.sendPacket(html);
	}
	
	/**
	 * Displays the main rebirth information window to a player.<br>
	 * This method generates dynamic HTML content based on the player's current progress.<br>
	 * It updates the UI with rebirth counts, acquired skills, and available tokens.
	 * @param player The {@code Player} who will view the window.
	 * @param objectId The unique identifier for the NPC or object triggering the window.
	 * @param selectedSkills A {@code List} of integers representing the IDs of chosen skills.
	 * @param rebirthCount The current number of rebirths completed by the player.
	 * @param skillDefinitionMap A {@code Map} linking skill IDs to their display names.
	 */
	private void displayMainWindow(Player player, int objectId, List<Integer> selectedSkills, int rebirthCount, Map<Integer, String> skillDefinitionMap)
	{
		final long tokenCount = getItemCount(player, RebirthConfig.REBIRTH_REWARD_ITEM_ID);
		
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, "data/html/default/70001_dynamic.htm");
		html.replace("%rebirth_count%", rebirthCount + "/" + RebirthConfig.REBIRTH_MAX_COUNT);
		html.replace("%rebirth_icons%", buildAcquiredSkillsHtml(player, selectedSkills, skillDefinitionMap, true));
		html.replace("%next_skill_icon%", "<img src=\"icon.etc_coins_gold_i00\" width=32 height=32>");
		html.replace("%next_skill_name%", "Rebirth Tokens");
		html.replace("%next_skill_lvl%", String.valueOf(tokenCount));
		html.replace("%next_skill_desc%", "Refund mode: " + RebirthConfig.REBIRTH_SKILL_REFUND_MODE);
		
		if (rebirthCount < RebirthConfig.REBIRTH_MAX_COUNT)
		{
			html.replace("%rebirth_button%", "<button value=\"Request Rebirth\" action=\"bypass -h rebirth_confirmrequest\" width=130 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			html.replace("%rebirth_button%", "<font color=AAAAAA>Max Rebirth reached.</font>");
		}
		
		player.sendPacket(html);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code RebirthManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code RebirthManager} instance.
	 */
	public static RebirthManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	/**
	 * Holds the singleton rebirth manager instance.
	 */
	private static class SingletonHolder
	{
		protected static final RebirthManager INSTANCE = new RebirthManager();
	}
}