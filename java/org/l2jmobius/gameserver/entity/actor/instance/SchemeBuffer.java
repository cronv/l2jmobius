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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.SchemeBufferConfig;
import org.l2jmobius.gameserver.data.SchemeBufferTable;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.holders.npc.BuffSkillHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * This class handles the {@code SchemeBuffer} NPC functionality for managing and casting player schemes.<br>
 * It allows players to create, edit, and delete schemes while providing manual skill casting by category.<br>
 * The handler also supports applying class-based auto-buffs using predefined groups.
 * @author Mobius, BazookaRpm
 * @since Classic_3.5_TalesUntold
 */
public class SchemeBuffer extends Npc
{
	// Constants.
	private static final int PAGE_LIMIT = 6;
	private static final int SCHEME_NAME_MAX_LENGTH = 14;
	private static final int TYPES_PER_ROW = 4;
	
	private static final int UI_WIDTH = 280;
	private static final int ICON_COL_WIDTH = 40;
	private static final int BTN_COL_WIDTH = 40;
	private static final int NAME_COL_WIDTH = UI_WIDTH - ICON_COL_WIDTH - BTN_COL_WIDTH; // 200.
	private static final int FOOT_LEFT_W = 70;
	private static final int FOOT_RIGHT_W = 70;
	private static final int FOOT_CENTER_W = UI_WIDTH - FOOT_LEFT_W - FOOT_RIGHT_W; // 140.
	
	/**
	 * Creates a new {@code SchemeBuffer} instance using the provided template.<br>
	 * This constructor initializes the handler with data from an {@code NpcTemplate}.
	 * @param template The {@code NpcTemplate} used to initialize this object.
	 */
	public SchemeBuffer(NpcTemplate template)
	{
		super(template);
	}
	
	/**
	 * Processes bypass commands sent by the player.<br>
	 * This method handles various actions such as creating, editing, and deleting schemes.<br>
	 * It also manages manual buff casting and automated scheme execution.
	 * @param player The {@code Player} who sent the command.
	 * @param commandValue The string containing the command to execute.
	 */
	@Override
	public void onBypassFeedback(Player player, String commandValue)
	{
		if ((player == null) || (commandValue == null) || commandValue.isEmpty())
		{
			return;
		}
		
		final SchemeBufferTable schemeBufferTable = SchemeBufferTable.getInstance();
		final SkillData skillData = SkillData.getInstance();
		
		final StringTokenizer st = new StringTokenizer(commandValue.replace("createscheme ", "createscheme;"), ";");
		if (!st.hasMoreTokens())
		{
			return;
		}
		
		final String currentCommand = st.nextToken();
		
		if (currentCommand.startsWith("menu"))
		{
			showMainMenu(player);
			return;
		}
		else if (currentCommand.startsWith("cleanup"))
		{
			player.stopAllEffects();
			
			final Summon pet = player.getPet();
			if (pet != null)
			{
				pet.stopAllEffects();
			}
			player.getServitors().values().forEach(servitor -> servitor.stopAllEffects());
			
			showMainMenu(player);
			return;
		}
		else if (currentCommand.startsWith("heal"))
		{
			player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
			player.setCurrentCp(player.getMaxCp());
			
			final Summon pet = player.getPet();
			if (pet != null)
			{
				pet.setCurrentHpMp(pet.getMaxHp(), pet.getMaxMp());
			}
			player.getServitors().values().forEach(servitor -> servitor.setCurrentHpMp(servitor.getMaxHp(), servitor.getMaxMp()));
			
			showMainMenu(player);
			return;
		}
		
		if (currentCommand.startsWith("support"))
		{
			showGiveBuffsWindow(player);
			return;
		}
		else if (currentCommand.startsWith("givebuffs"))
		{
			if (!st.hasMoreTokens())
			{
				return;
			}
			
			final String schemeName = st.nextToken();
			
			// NOTE: Cost from bypass is client-controlled. Always recalculate server-side.
			// Consume the token (if present) to preserve HTML/bypass format compatibility.
			if (st.hasMoreTokens())
			{
				st.nextToken();
			}
			
			final int playerId = player.getObjectId();
			final Map<String, List<Integer>> schemes = schemeBufferTable.getPlayerSchemes(playerId);
			if ((schemes == null) || !schemes.containsKey(schemeName))
			{
				player.sendMessage("This scheme name is invalid.");
				showGiveBuffsWindow(player);
				return;
			}
			
			final boolean buffSummons = st.hasMoreTokens() && "pet".equalsIgnoreCase(st.nextToken());
			if (buffSummons && (player.getPet() == null) && !player.hasServitors())
			{
				player.sendMessage("You don't have a pet.");
				return;
			}
			
			final List<Integer> scheme = schemeBufferTable.getScheme(playerId, schemeName);
			final int cost = getFee(scheme);
			if ((cost == 0) || ((SchemeBufferConfig.BUFFER_ITEM_ID == 57) && player.reduceAdena(ItemProcessType.FEE, cost, this, true)) || ((SchemeBufferConfig.BUFFER_ITEM_ID != 57) && player.destroyItemByItemId(ItemProcessType.FEE, SchemeBufferConfig.BUFFER_ITEM_ID, cost, player, true)))
			{
				for (int skillId : scheme)
				{
					final BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(skillId);
					if (holder == null)
					{
						continue;
					}
					
					final Skill skill = skillData.getSkill(skillId, holder.getLevel());
					if (skill == null)
					{
						continue;
					}
					
					if (buffSummons)
					{
						final Summon pet = player.getPet();
						if (pet != null)
						{
							skill.applyEffects(this, pet);
						}
						player.getServitors().values().forEach(servitor -> skill.applyEffects(this, servitor));
					}
					else
					{
						skill.applyEffects(this, player);
					}
				}
			}
			return;
		}
		else if (currentCommand.startsWith("editschemes"))
		{
			if (st.countTokens() < 3)
			{
				return;
			}
			
			final String groupType = st.nextToken();
			final String schemeName = st.nextToken();
			
			final Integer page = parseUnsignedInt(st.nextToken());
			if (page == null)
			{
				return;
			}
			
			showEditSchemeWindow(player, groupType, schemeName, page);
			return;
		}
		else if (currentCommand.startsWith("skill"))
		{
			if (st.countTokens() < 4)
			{
				return;
			}
			
			final String groupType = st.nextToken();
			final String schemeName = st.nextToken();
			
			final Integer skillId = parseUnsignedInt(st.nextToken());
			final Integer page = parseUnsignedInt(st.nextToken());
			if ((skillId == null) || (page == null))
			{
				return;
			}
			
			final int playerId = player.getObjectId();
			final Map<String, List<Integer>> schemes = schemeBufferTable.getPlayerSchemes(playerId);
			if ((schemes == null) || !schemes.containsKey(schemeName))
			{
				player.sendMessage("This scheme name is invalid.");
				showGiveBuffsWindow(player);
				return;
			}
			
			final List<Integer> schemeSkills = schemeBufferTable.getScheme(playerId, schemeName);
			
			if (currentCommand.startsWith("skillselect") && !schemeName.equalsIgnoreCase("none"))
			{
				final int maxLevel = skillData.getMaxLevel(skillId);
				final Skill skill = (maxLevel > 0) ? skillData.getSkill(skillId, maxLevel) : null;
				if (skill == null)
				{
					showEditSchemeWindow(player, groupType, schemeName, page);
					return;
				}
				
				if (!schemeSkills.contains(skillId))
				{
					if (skill.isDance())
					{
						if (getCountOf(schemeSkills, true) < PlayerConfig.DANCES_MAX_AMOUNT)
						{
							schemeBufferTable.addSkillToScheme(playerId, schemeName, skillId);
						}
						else
						{
							player.sendMessage("This scheme has reached the maximum amount of dances/songs.");
						}
					}
					else
					{
						if (getCountOf(schemeSkills, false) < player.getStat().getMaxBuffCount())
						{
							schemeBufferTable.addSkillToScheme(playerId, schemeName, skillId);
						}
						else
						{
							player.sendMessage("This scheme has reached the maximum amount of buffs.");
						}
					}
				}
			}
			else if (currentCommand.startsWith("skillunselect"))
			{
				schemeBufferTable.removeSkillFromScheme(playerId, schemeName, skillId);
			}
			
			showEditSchemeWindow(player, groupType, schemeName, page);
			return;
		}
		else if (currentCommand.startsWith("createscheme"))
		{
			if (!st.hasMoreTokens())
			{
				player.sendMessage("Scheme's name must contain up to " + SCHEME_NAME_MAX_LENGTH + " chars.");
				return;
			}
			
			final String schemeName = st.nextToken().trim();
			if (schemeName.isEmpty() || (schemeName.length() > SCHEME_NAME_MAX_LENGTH))
			{
				player.sendMessage("Scheme's name must contain up to " + SCHEME_NAME_MAX_LENGTH + " chars.");
				return;
			}
			
			final String normalized = schemeName.replace(" ", "").replace(".", "").replace(",", "").replace(";", "").replace(":", "").replace("-", "").replace("+", "").replace("!", "").replace("?", "");
			if (!StringUtil.isAlphaNumeric(normalized))
			{
				player.sendMessage("Please use plain alphanumeric characters.");
				return;
			}
			
			final int playerId = player.getObjectId();
			final Map<String, List<Integer>> schemes = schemeBufferTable.getPlayerSchemes(playerId);
			if (schemes != null)
			{
				if (schemes.size() >= SchemeBufferConfig.BUFFER_MAX_SCHEMES)
				{
					player.sendMessage("Maximum schemes amount is already reached.");
					return;
				}
				if (schemes.containsKey(schemeName))
				{
					player.sendMessage("The scheme name already exists.");
					return;
				}
			}
			
			schemeBufferTable.setScheme(playerId, schemeName, new ArrayList<>());
			showGiveBuffsWindow(player);
			return;
		}
		else if (currentCommand.startsWith("deletescheme"))
		{
			if (!st.hasMoreTokens())
			{
				player.sendMessage("This scheme name is invalid.");
				showGiveBuffsWindow(player);
				return;
			}
			
			final String schemeName = st.nextToken();
			schemeBufferTable.deleteScheme(player.getObjectId(), schemeName);
			showGiveBuffsWindow(player);
			return;
		}
		
		if (currentCommand.startsWith("manual"))
		{
			final String category = st.hasMoreTokens() ? st.nextToken() : "Buffs";
			
			int page = 1;
			if (st.hasMoreTokens())
			{
				final Integer parsedPage = parseUnsignedInt(st.nextToken());
				if (parsedPage == null)
				{
					showMainMenu(player);
					return;
				}
				page = parsedPage;
			}
			
			final String target = st.hasMoreTokens() ? st.nextToken() : "me";
			showManualWindow(player, category, page, target);
			return;
		}
		else if (currentCommand.startsWith("castbuff"))
		{
			if (st.countTokens() < 4)
			{
				return;
			}
			
			final Integer skillId = parseUnsignedInt(st.nextToken());
			final String category = st.nextToken();
			final Integer page = parseUnsignedInt(st.nextToken());
			final String targetType = st.nextToken();
			if ((skillId == null) || (page == null))
			{
				return;
			}
			
			if ("pet".equalsIgnoreCase(targetType) && (player.getPet() == null) && !player.hasServitors())
			{
				player.sendMessage("You don't have a pet.");
				showManualWindow(player, category, page, targetType);
				return;
			}
			
			BuffSkillHolder holder = schemeBufferTable.getAvailableBuff(category, skillId);
			if (holder == null)
			{
				holder = schemeBufferTable.getAvailableBuff(skillId);
			}
			
			if (holder != null)
			{
				final Skill skill = skillData.getSkill(skillId, holder.getLevel());
				if (skill != null)
				{
					if ("pet".equalsIgnoreCase(targetType))
					{
						final Summon pet = player.getPet();
						if (pet != null)
						{
							skill.applyEffects(this, pet);
						}
						player.getServitors().values().forEach(servitor -> skill.applyEffects(this, servitor));
					}
					else
					{
						skill.applyEffects(this, player);
					}
				}
			}
			
			showManualWindow(player, category, page, targetType);
			return;
		}
		else if (currentCommand.startsWith("autobuff"))
		{
			applyAutoBuff(player, st.hasMoreTokens() ? st.nextToken() : "me");
			return;
		}
		
		super.onBypassFeedback(player, commandValue);
	}
	
	/**
	 * Constructs the file path for a specific NPC HTML page.<br>
	 * It determines the filename based on the provided {@code npcId} and {@code value}.<br>
	 * The result is always located in the SchemeBuffer directory.
	 * @param npcId The unique identifier of the NPC.
	 * @param value The specific page or index value.
	 * @param player The {@code Player} object associated with the request.
	 * @return A {@code String} representing the full path to the .htm file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		final String filename = (value == 0) ? Integer.toString(npcId) : (npcId + "-" + value);
		return "data/html/mods/SchemeBuffer/" + filename + ".htm";
	}
	
	/**
	 * Displays the main menu interface to the specified player.<br>
	 * It sends an {@code NpcHtmlMessage} containing the correct HTML path.<br>
	 * The message replaces the {@code %objectId%} placeholder with the actual ID.
	 * @param player The {@code Player} who will view the menu.
	 */
	private void showMainMenu(Player player)
	{
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, getHtmlPath(getId(), 0, player));
		html.replace("%objectId%", objectId);
		player.sendPacket(html);
	}
	
	/**
	 * Displays the buff schemes window to the player.<br>
	 * It lists all defined schemes with their skill counts and costs.<br>
	 * The window provides options to use, edit, or delete each scheme.
	 * @param player The {@code Player} who will view the window.
	 */
	private void showGiveBuffsWindow(Player player)
	{
		final StringBuilder sb = new StringBuilder(200);
		final Map<String, List<Integer>> schemes = SchemeBufferTable.getInstance().getPlayerSchemes(player.getObjectId());
		
		if ((schemes == null) || schemes.isEmpty())
		{
			sb.append("<font color=\"LEVEL\">You haven't defined any scheme.</font>");
		}
		else
		{
			for (Entry<String, List<Integer>> scheme : schemes.entrySet())
			{
				final String schemeName = scheme.getKey();
				final List<Integer> schemeSkills = scheme.getValue();
				final int cost = getFee(schemeSkills);
				sb.append("<font color=\"LEVEL\">").append(schemeName).append(" [").append(schemeSkills.size()).append(" skill(s)]").append((cost > 0) ? (" - cost: " + NumberFormat.getInstance(Locale.ENGLISH).format(cost)) : "").append("</font><br1>");
				
				sb.append("<a action=\"bypass npc_%objectId%_givebuffs;").append(schemeName).append(';').append(cost).append("\">Use on Me</a>&nbsp;|&nbsp;");
				sb.append("<a action=\"bypass npc_%objectId%_givebuffs;").append(schemeName).append(';').append(cost).append(";pet\">Use on Pet</a>&nbsp;|&nbsp;");
				sb.append("<a action=\"bypass npc_%objectId%_editschemes;Buffs;").append(schemeName).append(";1\">Edit</a>&nbsp;|&nbsp;");
				sb.append("<a action=\"bypass npc_%objectId%_deletescheme;").append(schemeName).append("\">Delete</a><br>");
			}
		}
		
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, getHtmlPath(getId(), 1, player));
		html.replace("%schemes%", sb.toString());
		html.replace("%max_schemes%", SchemeBufferConfig.BUFFER_MAX_SCHEMES);
		html.replace("%objectId%", objectId);
		player.sendPacket(html);
	}
	
	/**
	 * Opens the editing window for a specific player scheme.<br>
	 * It sends an {@code NpcHtmlMessage} to the {@code Player}.<br>
	 * The message contains the skill list and count based on the provided parameters.
	 * @param player The {@code Player} who will view the window.
	 * @param groupType The type of skills to display in the group.
	 * @param schemeName The name of the specific scheme to edit.
	 * @param page The current page number for the skill list.
	 */
	private void showEditSchemeWindow(Player player, String groupType, String schemeName, int page)
	{
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		final List<Integer> schemeSkills = SchemeBufferTable.getInstance().getScheme(player.getObjectId(), schemeName);
		
		html.setFile(player, getHtmlPath(getId(), 2, player));
		html.replace("%schemename%", schemeName);
		html.replace("%count%", getCountOf(schemeSkills, false) + " / " + player.getStat().getMaxBuffCount() + " buffs, " + getCountOf(schemeSkills, true) + " / " + PlayerConfig.DANCES_MAX_AMOUNT + " dances/songs");
		html.replace("%typesframe%", getTypesFrame(groupType, schemeName));
		html.replace("%skilllistframe%", getGroupSkillList(player, groupType, schemeName, page));
		html.replace("%objectId%", objectId);
		player.sendPacket(html);
	}
	
	/**
	 * Displays the manual buff casting window for a specific category.<br>
	 * It handles pagination and filters skills based on the target type.<br>
	 * The method constructs an HTML message containing the skill list and buttons.
	 * @param player The {@code Player} who will view the window.
	 * @param category The name of the skill category to display.
	 * @param pageValue The page number to show in the list.
	 * @param targetType The type of target for the buffs, such as "me" or "pet".
	 */
	private void showManualWindow(Player player, String category, int pageValue, String targetType)
	{
		List<Integer> skills = SchemeBufferTable.getInstance().getSkillsIdsByType(category);
		if (skills.isEmpty())
		{
			player.sendMessage("That category doesn't contain any skills.");
			return;
		}
		
		final int skillSize = skills.size();
		final int max = HtmlUtil.countPageNumber(skillSize, PAGE_LIMIT);
		int page = pageValue;
		if (page > max)
		{
			page = max;
		}
		if (page < 1)
		{
			page = 1;
		}
		
		skills = skills.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, skillSize));
		
		final StringBuilder sb = new StringBuilder(skills.size() * 200);
		
		// Tabs Me/Pet.
		sb.append("<table width=\"").append(UI_WIDTH).append("\"><tr>");
		sb.append("<td width=\"").append(UI_WIDTH / 2).append("\" align=\"center\">");
		final int objectId = getObjectId();
		sb.append("me".equalsIgnoreCase(targetType) ? "<font color=\"LEVEL\">Me</font>" : "<a action=\"bypass npc_" + objectId + "_manual;" + category + ";" + page + ";me\">Me</a>");
		sb.append("</td>");
		sb.append("<td width=\"").append(UI_WIDTH / 2).append("\" align=\"center\">");
		sb.append("pet".equalsIgnoreCase(targetType) ? "<font color=\"LEVEL\">Pet</font>" : "<a action=\"bypass npc_" + objectId + "_manual;" + category + ";" + page + ";pet\">Pet</a>");
		sb.append("</td>");
		sb.append("</tr></table><br1>");
		
		int row = 0;
		for (int skillId : skills)
		{
			final BuffSkillHolder holder = SchemeBufferTable.getInstance().getAvailableBuff(skillId);
			if (holder == null)
			{
				continue;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(skillId, holder.getLevel());
			if (skill == null)
			{
				continue;
			}
			
			sb.append(((row % 2) == 0) ? "<table width=\"" + UI_WIDTH + "\" bgcolor=\"000000\"><tr>" : "<table width=\"" + UI_WIDTH + "\"><tr>");
			sb.append("<td height=40 width=").append(ICON_COL_WIDTH).append(" align=center><img src=\"").append(skill.getIcon()).append("\" width=32 height=32></td>");
			sb.append("<td width=").append(NAME_COL_WIDTH).append('>').append(skill.getName()).append("<br1><font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");
			sb.append("<td width=").append(BTN_COL_WIDTH).append(" align=center>").append("<button value=\"+\" action=\"bypass npc_").append(objectId).append("_castbuff;").append(skillId).append(';').append(category).append(';').append(page).append(';').append(targetType).append("\" ").append("width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">").append("</td>");
			sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
			row++;
		}
		
		// Footer.
		sb.append("<br1><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1><table width=\"").append(UI_WIDTH).append("\" bgcolor=000000><tr>");
		
		if (page > 1)
		{
			sb.append("<td align=left width=").append(FOOT_LEFT_W).append("><a action=\"bypass npc_").append(objectId).append("_manual;").append(category).append(';').append(page - 1).append(';').append(targetType).append("\">Previous</a></td>");
		}
		else
		{
			sb.append("<td align=left width=").append(FOOT_LEFT_W).append(">Previous</td>");
		}
		
		sb.append("<td align=center width=").append(FOOT_CENTER_W).append(">Page ").append(page).append("</td>");
		
		if (page < max)
		{
			sb.append("<td align=right width=").append(FOOT_RIGHT_W).append("><a action=\"bypass npc_").append(objectId).append("_manual;").append(category).append(';').append(page + 1).append(';').append(targetType).append("\">Next</a></td>");
		}
		else
		{
			sb.append("<td align=right width=").append(FOOT_RIGHT_W).append(">Next</td>");
		}
		
		sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
		sb.append("<br1><center><a action=\"bypass npc_").append(objectId).append("_menu\">Back</a></center>");
		
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, getHtmlPath(getId(), 3, player));
		html.replace("%category%", category);
		html.replace("%skills%", sb.toString());
		html.replace("%objectId%", objectId);
		player.sendPacket(html);
	}
	
	/**
	 * Applies auto-buffs to the specified target type.<br>
	 * It checks if the player has a valid pet for summon targets.<br>
	 * The method identifies skills based on the player's class category.<br>
	 * All available buffs are then applied to the player or their summons.
	 * @param player The {@code Player} receiving the buff effects.
	 * @param targetType The type of target to apply buffs to, such as "pet".
	 */
	private void applyAutoBuff(Player player, String targetType)
	{
		final boolean toSummons = "pet".equalsIgnoreCase(targetType);
		if (toSummons && (player.getPet() == null) && !player.hasServitors())
		{
			player.sendMessage("You don't have a pet.");
			showMainMenu(player);
			return;
		}
		
		final String category = player.isMageClass() ? "MAGE_GROUP" : "FIGHTER_GROUP";
		final List<Integer> skills = SchemeBufferTable.getInstance().getSkillsIdsByType(category);
		if (skills.isEmpty())
		{
			player.sendMessage("Auto buff configuration is not available.");
			showMainMenu(player);
			return;
		}
		
		for (int skillId : skills)
		{
			final BuffSkillHolder holder = SchemeBufferTable.getInstance().getAvailableBuff(skillId);
			if (holder == null)
			{
				continue;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(skillId, holder.getLevel());
			if (skill == null)
			{
				continue;
			}
			
			if (toSummons)
			{
				final Summon pet = player.getPet();
				if (pet != null)
				{
					skill.applyEffects(this, pet);
				}
				player.getServitors().values().forEach(servitor -> skill.applyEffects(this, servitor));
			}
			else
			{
				skill.applyEffects(this, player);
			}
		}
		
		player.sendMessage("Auto buff applied successfully!");
		showMainMenu(player);
	}
	
	/**
	 * Generates the HTML content for a list of skills based on the group type and scheme.<br>
	 * It handles pagination and checks if each skill is currently selected in the player's scheme.<br>
	 * The method returns an empty message if no skills are found in the specified group.
	 * @param player The {@code Player} object who is requesting the list.
	 * @param groupType The type of skill group to display.
	 * @param schemeName The name of the specific scheme being edited.
	 * @param pageValue The page number to retrieve from the results.
	 * @return A {@code String} containing the formatted HTML for the skill list.
	 */
	private String getGroupSkillList(Player player, String groupType, String schemeName, int pageValue)
	{
		List<Integer> skills = SchemeBufferTable.getInstance().getSkillsIdsByType(groupType);
		if (skills.isEmpty())
		{
			return "That group doesn't contain any skills.";
		}
		
		final int skillSize = skills.size();
		final int max = HtmlUtil.countPageNumber(skillSize, PAGE_LIMIT);
		int page = pageValue;
		if (page > max)
		{
			page = max;
		}
		if (page < 1)
		{
			page = 1;
		}
		
		skills = skills.subList((page - 1) * PAGE_LIMIT, Math.min(page * PAGE_LIMIT, skillSize));
		
		final List<Integer> schemeSkills = SchemeBufferTable.getInstance().getScheme(player.getObjectId(), schemeName);
		final StringBuilder sb = new StringBuilder(skillSize * 200);
		
		int row = 0;
		for (int skillId : skills)
		{
			final BuffSkillHolder holder = SchemeBufferTable.getInstance().getAvailableBuff(skillId);
			if (holder == null)
			{
				continue;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
			if (skill == null)
			{
				continue;
			}
			
			sb.append(((row % 2) == 0) ? "<table width=\"" + UI_WIDTH + "\" bgcolor=\"000000\"><tr>" : "<table width=\"" + UI_WIDTH + "\"><tr>");
			sb.append("<td height=40 width=").append(ICON_COL_WIDTH).append(" align=center><img src=\"").append(skill.getIcon()).append("\" width=32 height=32></td>");
			sb.append("<td width=").append(NAME_COL_WIDTH).append('>').append(skill.getName()).append("<br1><font color=\"B09878\">").append(holder.getDescription()).append("</font></td>");
			
			if (schemeSkills.contains(skillId))
			{
				sb.append("<td width=").append(BTN_COL_WIDTH).append(" align=center>").append("<button value=\"-\" action=\"bypass npc_%objectId%_skillunselect;").append(groupType).append(';').append(schemeName).append(';').append(skillId).append(';').append(page).append("\" ").append("width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">").append("</td>");
			}
			else
			{
				sb.append("<td width=").append(BTN_COL_WIDTH).append(" align=center>").append("<button value=\"+\" action=\"bypass npc_%objectId%_skillselect;").append(groupType).append(';').append(schemeName).append(';').append(skillId).append(';').append(page).append("\" ").append("width=32 height=25 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">").append("</td>");
			}
			
			sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
			row++;
		}
		
		// Footer.
		sb.append("<br1><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1><table width=\"").append(UI_WIDTH).append("\" bgcolor=000000><tr>");
		if (page > 1)
		{
			final int objectId = getObjectId();
			sb.append("<td align=left width=").append(FOOT_LEFT_W).append("><a action=\"bypass npc_").append(objectId).append("_editschemes;").append(groupType).append(';').append(schemeName).append(';').append(page - 1).append("\">Previous</a></td>");
		}
		else
		{
			sb.append("<td align=left width=").append(FOOT_LEFT_W).append(">Previous</td>");
		}
		
		sb.append("<td align=center width=").append(FOOT_CENTER_W).append(">Page ").append(page).append("</td>");
		
		if (page < max)
		{
			final int objectId = getObjectId();
			sb.append("<td align=right width=").append(FOOT_RIGHT_W).append("><a action=\"bypass npc_").append(objectId).append("_editschemes;").append(groupType).append(';').append(schemeName).append(';').append(page + 1).append("\">Next</a></td>");
		}
		else
		{
			sb.append("<td align=right width=").append(FOOT_RIGHT_W).append(">Next</td>");
		}
		
		sb.append("</tr></table><img src=\"L2UI.SquareGray\" width=").append(UI_WIDTH).append(" height=1>");
		return sb.toString();
	}
	
	/**
	 * Generates an HTML table of skill types for the scheme editor.<br>
	 * It creates links to different schemes based on the provided group and name.
	 * @param groupType The type of group currently being edited.
	 * @param schemeName The name of the specific scheme being viewed.
	 * @return A {@code String} containing the HTML table markup.
	 */
	private static String getTypesFrame(String groupType, String schemeName)
	{
		final StringBuilder sb = new StringBuilder(500);
		sb.append("<table>");
		
		int count = 0;
		for (String type : SchemeBufferTable.getInstance().getSkillTypes())
		{
			if (count == 0)
			{
				sb.append("<tr>");
			}
			
			if (groupType.equalsIgnoreCase(type))
			{
				sb.append("<td width=65>").append(type).append("</td>");
			}
			else
			{
				sb.append("<td width=65><a action=\"bypass npc_%objectId%_editschemes;").append(type).append(';').append(schemeName).append(";1\">").append(type).append("</a></td>");
			}
			
			count++;
			if (count == TYPES_PER_ROW)
			{
				sb.append("</tr>");
				count = 0;
			}
		}
		
		if (count != 0)
		{
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Converts a string into an unsigned integer.<br>
	 * Returns {@code null} if the input is invalid or exceeds the maximum value of an {@code int}.
	 * @param value The string to be parsed.
	 * @return The parsed {@code Integer} value, or {@code null} if parsing fails.
	 */
	
	private static Integer parseUnsignedInt(String value)
	{
		if ((value == null) || value.isEmpty())
		{
			return null;
		}
		
		int result = 0;
		for (int i = 0; i < value.length(); i++)
		{
			final char c = value.charAt(i);
			if ((c < '0') || (c > '9'))
			{
				return null;
			}
			
			final int digit = c - '0';
			if (result > ((Integer.MAX_VALUE - digit) / 10))
			{
				return null;
			}
			result = (result * 10) + digit;
		}
		return result;
	}
	
	/**
	 * Calculates the total cost for a list of skills.<br>
	 * It checks if a static cost is configured in {@code SchemeBufferConfig}.<br>
	 * If no static cost exists, it sums the prices from {@code SchemeBufferTable}.
	 * @param list The list of skill IDs to calculate the fee for.
	 * @return The total calculated fee as an {@code int}.
	 */
	private static int getFee(List<Integer> list)
	{
		if (SchemeBufferConfig.BUFFER_STATIC_BUFF_COST > 0)
		{
			return list.size() * SchemeBufferConfig.BUFFER_STATIC_BUFF_COST;
		}
		
		int fee = 0;
		for (int skillId : list)
		{
			final BuffSkillHolder holder = SchemeBufferTable.getInstance().getAvailableBuff(skillId);
			if (holder != null)
			{
				fee += holder.getPrice();
			}
		}
		return fee;
	}
	
	/**
	 * Calculates the number of skills that match a specific type.<br>
	 * It checks if each skill in the list is a dance based on the {@code dances} flag.
	 * @param skills The list of skill IDs to check.
	 * @param dances A boolean indicating whether to count dance skills as {@code true} or non-dance skills as {@code false}.
	 * @return The total count of matching skills.
	 */
	private static int getCountOf(List<Integer> skills, boolean dances)
	{
		int count = 0;
		for (int skillId : skills)
		{
			final Skill skill = SkillData.getInstance().getSkill(skillId, 1);
			if ((skill != null) && (skill.isDance() == dances))
			{
				count++;
			}
		}
		return count;
	}
}
