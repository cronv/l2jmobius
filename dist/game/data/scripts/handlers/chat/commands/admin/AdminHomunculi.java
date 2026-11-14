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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.HomunculusData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.homunculus.Homunculus;
import org.l2jmobius.gameserver.mechanics.homunculus.HomunculusList;
import org.l2jmobius.gameserver.mechanics.homunculus.HomunculusTemplate;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExShowHomunculusList;

/**
 * Handles administrative commands related to homunculi.<br>
 * This class allows administrators to manage and view homunculus data using chat commands.
 * @author CostyKiller
 */
public class AdminHomunculi implements IAdminCommandHandler
{
	// @formatter:off	
	// Homunculi template IDs by type - sourced from HomunculusData.xml
	private static final int[][] HOMUNCULI_BY_TYPE =
	{
		{ 1, 4, 7, 10, 13, 16, 19, 22, 25, 28, 31, 34, 37 },   // Type 0 - Basic
		{ 2, 5, 8, 11, 14, 17, 20, 23, 26, 29, 32, 35, 38 },   // Type 1 - Water/Earth
		{ 3, 6, 9, 12, 15, 18, 21, 24, 27, 30, 33, 36, 39 },   // Type 2 - Luminous
		{ 40, 41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, 52 } // Type 3 - Radiant
	};
	
	private static final String[] TYPE_NAMES =
	{
		"Basic",
		"Water/Earth",
		"Luminous",
		"Radiant"
	};
	
	// Add section: rows per page - 5 rows x 5 buttons.
	private static final int ADD_PAGE_SIZE = 5;
	
	// List section: entries per page.
	private static final int LIST_PAGE_SIZE = 5;
	
	// Skill icon textures.
	private static final String[] SKILL_ICONS =
	{
		"",
		"icon.homun_cupon_s",
		"icon.homun_cupon_s",
		"icon.homun_coupon_etc",
		"icon.homun_coupon_m",
		"icon.homun_coupon_l",
	};
	
	// Level overlay: when level > 0 use etc_level_panel_lv{N}, else icon itself.
	private static final String[] LEVEL_PANEL =
	{
		"",                         // 0 - no overlay, falls back to icon
		"icon.etc_level_panel_lv1", // 1
		"icon.etc_level_panel_lv2", // 2
		"icon.etc_level_panel_lv3", // 3 - max
	};
	// @formatter:on
	
	private static final int MAX_SKILL_LEVEL = 3;
	private static final int MAX_HOM_LEVEL = 5;
	
	private static final String B = "L2UI_CT1.Button_DF_Down";
	private static final String F = "L2UI_CT1.Button_DF";
	
	private static final String HTM_MENU = "data/html/admin/homunculi_menu.htm";
	
	private static final String SEC_SLOTS = "slots";
	private static final String SEC_LIST = "list";
	private static final String SEC_ADD = "add";
	private static final String SEC_SKILLS = "skills";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_homunculi"
	};
	
	// -----------------------------------------------------------------------
	// Dispatch
	// -----------------------------------------------------------------------
	/**
	 * Processes admin commands for managing homunculi.<br>
	 * This method parses the command string and applies changes to a target player's homunculi.<br>
	 * It validates that the active character has a valid player target before execution.
	 * @param command The full command string starting with {@code admin_homunculi}.
	 * @param activeChar The {@code Player} object who is executing the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		st.nextToken(); // consume "admin_homunculi"
		
		if ((activeChar.getTarget() == null) || !activeChar.getTarget().isPlayer())
		{
			activeChar.sendMessage("Invalid target!");
			return false;
		}
		final Player target = activeChar.getTarget().asPlayer();
		
		if (!st.hasMoreTokens())
		{
			showMenu(activeChar, target, SEC_SLOTS, 0, 0);
			return true;
		}
		
		switch (st.nextToken().toLowerCase())
		{
			case "section":
			{
				// Section <sec> [arg1] [arg2] skills: set arg1 to slot and add arg1 as type on page arg2.
				final String sec = st.hasMoreTokens() ? st.nextToken().toLowerCase() : SEC_SLOTS;
				final int arg1 = nextInt(st, 0);
				final int arg2 = nextInt(st, 0);
				showMenu(activeChar, target, sec, arg1, arg2);
				break;
			}
			case "slot_add":
			{
				cmdSlotAdd(activeChar, target);
				break;
			}
			case "slot_remove":
			{
				cmdSlotRemove(activeChar, target);
				break;
			}
			case "give":
			{
				final int templateId = nextInt(st, -1);
				final int level = nextInt(st, 1);
				cmdGive(activeChar, target, templateId, level);
				break;
			}
			case "remove":
			{
				cmdRemove(activeChar, target, nextInt(st, -1));
				break;
			}
			case "hom_level_up":
			{
				cmdHomLevel(activeChar, target, nextInt(st, -1), true);
				break;
			}
			case "hom_level_down":
			{
				cmdHomLevel(activeChar, target, nextInt(st, -1), false);
				break;
			}
			case "skill_set":
			{
				final int slot = nextInt(st, 0);
				final int skillIdx = nextInt(st, 1);
				final int level = nextInt(st, 0);
				cmdSkillSet(activeChar, target, slot, skillIdx, level);
				break;
			}
			default:
			{
				showMenu(activeChar, target, SEC_SLOTS, 0, 0);
				break;
			}
		}
		return true;
	}
	
	// -----------------------------------------------------------------------
	// CMD: add slot
	// -----------------------------------------------------------------------
	/**
	 * Adds a new homunculus slot to the specified player.<br>
	 * Checks if the player has reached the maximum allowed slots defined in {@code PlayerConfig}.<br>
	 * Updates the player variables and synchronizes the client state.
	 * @param activeChar The admin player executing the command.
	 * @param target The player receiving the new homunculus slot.
	 */
	private void cmdSlotAdd(Player activeChar, Player target)
	{
		final int current = target.getAvailableHomunculusSlotCount();
		final int max = PlayerConfig.MAX_HOMUNCULUS_COUNT;
		if (current >= max)
		{
			activeChar.sendMessage(target.getName() + " already at max slots (" + max + ").");
			showMenu(activeChar, target, SEC_SLOTS, 0, 0);
			return;
		}
		
		target.getVariables().set(PlayerVariables.HOMUNCULUS_OPENED_SLOT_COUNT, current + 1);
		target.getVariables().storeMe();
		syncClient(target);
		activeChar.sendMessage("Slot " + (current + 1) + "/" + max + " opened for " + target.getName());
		target.sendMessage("An admin opened an extra homunculi slot for you.");
		showMenu(activeChar, target, SEC_SLOTS, 0, 0);
	}
	
	// -----------------------------------------------------------------------
	// CMD: remove slot
	// -----------------------------------------------------------------------
	/**
	 * Removes the last available homunculus slot from a player.<br>
	 * This method checks if there is more than one slot remaining.<br>
	 * It also ensures the last slot is empty before closing it.
	 * @param activeChar The admin player executing the command.
	 * @param target The player whose homunculus slots are being modified.
	 */
	private void cmdSlotRemove(Player activeChar, Player target)
	{
		final int current = target.getAvailableHomunculusSlotCount();
		if (current <= 1)
		{
			activeChar.sendMessage("Cannot reduce below 1 slot.");
			showMenu(activeChar, target, SEC_SLOTS, 0, 0);
			return;
		}
		
		final Homunculus inLastSlot = target.getHomunculusList().get(current - 1);
		if (inLastSlot != null)
		{
			activeChar.sendMessage("Cannot close slot " + current + " - it contains [" + inLastSlot.getName() + "]. Remove that homunculi first.");
			showMenu(activeChar, target, SEC_SLOTS, 0, 0);
			return;
		}
		
		target.getVariables().set(PlayerVariables.HOMUNCULUS_OPENED_SLOT_COUNT, current - 1);
		target.getVariables().storeMe();
		syncClient(target);
		activeChar.sendMessage("Slot " + current + " closed for " + target.getName() + ". Opened: " + (current - 1));
		showMenu(activeChar, target, SEC_SLOTS, 0, 0);
	}
	
	// -----------------------------------------------------------------------
	// CMD: give
	// -----------------------------------------------------------------------
	/**
	 * Grants a new homunculus to a specific player.<br>
	 * This method checks for valid templates and available slots before adding the entity.<br>
	 * It also clamps the provided level to the allowed maximum range.
	 * @param activeChar The admin player executing the command.
	 * @param target The player receiving the homunculus.
	 * @param templateId The unique ID of the homunculus template.
	 * @param level The desired level for the new homunculus.
	 */
	private void cmdGive(Player activeChar, Player target, int templateId, int level)
	{
		if (templateId < 1)
		{
			activeChar.sendMessage("Invalid template id.");
			return;
		}
		
		final HomunculusTemplate template = HomunculusData.getInstance().getTemplate(templateId);
		if (template == null)
		{
			activeChar.sendMessage("Template " + templateId + " not found.");
			return;
		}
		
		final HomunculusList list = target.getHomunculusList();
		if (list.hasHomunculus(templateId))
		{
			activeChar.sendMessage(target.getName() + " already owns id=" + templateId);
			showMenu(activeChar, target, SEC_ADD, typeOfTemplate(templateId), 0);
			return;
		}
		
		if (list.isFull())
		{
			activeChar.sendMessage("All slots full (max " + PlayerConfig.MAX_HOMUNCULUS_COUNT + ").");
			showMenu(activeChar, target, SEC_ADD, typeOfTemplate(templateId), 0);
			return;
		}
		
		final int listSize = list.size();
		if (listSize >= target.getAvailableHomunculusSlotCount())
		{
			activeChar.sendMessage("No opened slot. Use Slots section to add one first.");
			showMenu(activeChar, target, SEC_ADD, typeOfTemplate(templateId), 0);
			return;
		}
		
		final int clampedLevel = Math.max(1, Math.min(MAX_HOM_LEVEL, level));
		final int slot = listSize;
		final Homunculus hom = new Homunculus(template, slot, clampedLevel, 0, 0, 0, 0, 0, 0, false);
		if (list.add(hom))
		{
			syncClient(target);
			final String templateName = template.getName();
			activeChar.sendMessage("Gave [" + templateName + "] lv=" + clampedLevel + " slot=" + slot + " to " + target.getName());
			target.sendMessage("Admin granted you Homunculi: " + templateName + " (level " + clampedLevel + ").");
		}
		else
		{
			activeChar.sendMessage("Failed to add (DB error).");
		}
		showMenu(activeChar, target, SEC_ADD, typeOfTemplate(templateId), 0);
	}
	
	// -----------------------------------------------------------------------
	// CMD: remove homunculi
	// -----------------------------------------------------------------------
	/**
	 * Removes a homunculus from a specific slot for the target player.<br>
	 * Validates that the slot index is non-negative and contains an item.<br>
	 * Sends confirmation messages to both the admin and the target player.
	 * @param activeChar The {@code Player} executing the command.
	 * @param target The {@code Player} whose homunculus will be removed.
	 * @param slot The index of the homunculus slot to clear.
	 */
	private void cmdRemove(Player activeChar, Player target, int slot)
	{
		if (slot < 0)
		{
			activeChar.sendMessage("Invalid slot.");
			return;
		}
		
		final Homunculus hom = target.getHomunculusList().get(slot);
		if (hom == null)
		{
			activeChar.sendMessage("No homunculi in slot " + slot);
			showMenu(activeChar, target, SEC_LIST, 0, 0);
			return;
		}
		
		final String name = hom.getName();
		if (target.getHomunculusList().remove(hom))
		{
			syncClient(target);
			activeChar.sendMessage("Removed [" + name + "] from " + target.getName());
			target.sendMessage("Admin removed your Homunculi: " + name);
		}
		else
		{
			activeChar.sendMessage("Failed to remove (DB error).");
		}
		
		showMenu(activeChar, target, SEC_LIST, 0, 0);
	}
	
	// -----------------------------------------------------------------------
	// Adjust the homunculi level by removing any skills above the new level when deleveling.
	// -----------------------------------------------------------------------
	/**
	 * Changes the level of a specific homunculus for a player.<br>
	 * This method updates the level and experience based on the {@code increase} flag.<br>
	 * It also handles skill removal if the level is decreased.
	 * @param activeChar The admin player executing the command.
	 * @param target The player whose homunculus will be modified.
	 * @param slot The index of the homunculus in the target's list.
	 * @param increase Set to {@code true} to level up and {@code false} to level down.
	 */
	private void cmdHomLevel(Player activeChar, Player target, int slot, boolean increase)
	{
		if (slot < 0)
		{
			activeChar.sendMessage("Invalid slot.");
			return;
		}
		
		final Homunculus hom = target.getHomunculusList().get(slot);
		if (hom == null)
		{
			activeChar.sendMessage("No homunculi in slot " + slot);
			showMenu(activeChar, target, SEC_LIST, 0, 0);
			return;
		}
		
		final int current = hom.getLevel();
		final int next = increase ? Math.min(MAX_HOM_LEVEL, current + 1) : Math.max(1, current - 1);
		if (next == current)
		{
			activeChar.sendMessage("Level already at " + (increase ? "max (" + MAX_HOM_LEVEL + ")" : "min (1)") + ".");
			showMenu(activeChar, target, SEC_LIST, 0, 0);
			return;
		}
		
		hom.setLevel(next);
		// Set exp to the minimum for the new level so the client shows 0% into that level.
		// expToLevel2 = exp needed to reach lv2 (i.e. minimum exp while at lv2), etc.
		// Level 1 = 0 exp, Level N = expToLevel{N} from template.
		final int newExp;
		switch (next)
		{
			case 2:
			{
				newExp = hom.getTemplate().getExpToLevel2();
				break;
			}
			case 3:
			{
				newExp = hom.getTemplate().getExpToLevel3();
				break;
			}
			case 4:
			{
				newExp = hom.getTemplate().getExpToLevel4();
				break;
			}
			case 5:
			{
				newExp = hom.getTemplate().getExpToLevel5();
				break;
			}
			default:
			{
				newExp = 0; // level 1
				break;
			}
		}
		hom.setExp(newExp);
		
		// When deleveling, remove skills that are no longer unlocked (sk index > new level).
		if (!increase)
		{
			for (int sk = next + 1; sk <= MAX_HOM_LEVEL; sk++)
			{
				if (getSkillLevel(hom, sk) > 0)
				{
					setSkillLevel(hom, sk, 0);
				}
			}
		}
		
		final HomunculusList homList = target.getHomunculusList();
		if (homList.update(hom))
		{
			homList.refreshStats(true);
			target.sendSkillList();
			syncClient(target);
			activeChar.sendMessage(hom.getName() + " level " + current + " -> " + next + " for " + target.getName());
		}
		else
		{
			activeChar.sendMessage("DB update failed.");
		}
		
		showMenu(activeChar, target, SEC_LIST, 0, 0);
	}
	
	// -----------------------------------------------------------------------
	// CMD: set skill level
	// -----------------------------------------------------------------------
	/**
	 * Sets the skill level for a specific homunculus.<br>
	 * This method updates the skill and refreshes the stats for the target player.
	 * @param activeChar The admin player executing the command.
	 * @param target The player who owns the homunculus.
	 * @param slot The index of the homunculus slot to modify.
	 * @param skillIdx The index of the skill to change, which must be between 1 and 5.
	 * @param level The new level to set for the chosen skill.
	 */
	private void cmdSkillSet(Player activeChar, Player target, int slot, int skillIdx, int level)
	{
		final Homunculus hom = target.getHomunculusList().get(slot);
		if (hom == null)
		{
			activeChar.sendMessage("No homunculi in slot " + slot);
			return;
		}
		
		if ((skillIdx < 1) || (skillIdx > 5))
		{
			activeChar.sendMessage("Skill index must be 1-5.");
			return;
		}
		
		final int homLevel = hom.getLevel();
		if (skillIdx > homLevel)
		{
			activeChar.sendMessage("Skill " + skillIdx + " requires level " + skillIdx + " (current: " + homLevel + ").");
			showMenu(activeChar, target, SEC_SKILLS, slot, 0);
			return;
		}
		
		final int clamped = Math.max(0, Math.min(MAX_SKILL_LEVEL, level));
		setSkillLevel(hom, skillIdx, clamped);
		if (target.getHomunculusList().update(hom))
		{
			target.getHomunculusList().refreshStats(true);
			target.sendSkillList();
			syncClient(target);
			activeChar.sendMessage(hom.getName() + " skill " + skillIdx + " -> " + clamped + " for " + target.getName());
		}
		else
		{
			activeChar.sendMessage("DB update failed.");
		}
		
		showMenu(activeChar, target, SEC_SKILLS, slot, 0);
	}
	
	// -----------------------------------------------------------------------
	// MAIN PAGE
	// arg1 = slot (skills) | type (add)
	// arg2 = page (add)
	// -----------------------------------------------------------------------
	/**
	 * Displays a specific menu section to the active player.<br>
	 * It builds the HTML content based on the provided section type and arguments.<br>
	 * The final message is sent as an {@code NpcHtmlMessage}.
	 * @param activeChar The player who will view the menu.
	 * @param target The player whose homunculus data will be displayed.
	 * @param section The specific menu category to display.
	 * @param arg1 The first additional argument for building the section content.
	 * @param arg2 The second additional argument for building the section content.
	 */
	private void showMenu(Player activeChar, Player target, String section, int arg1, int arg2)
	{
		String content = HtmCache.getInstance().getHtm(activeChar, HTM_MENU);
		if (content == null)
		{
			activeChar.sendMessage("Missing file: " + HTM_MENU);
			return;
		}
		
		final String sectionHtml;
		switch (section)
		{
			case SEC_LIST:
			{
				sectionHtml = buildSectionList(target, arg2);
				break;
			}
			case SEC_ADD:
			{
				sectionHtml = buildSectionAdd(target, arg1, arg2);
				break;
			}
			case SEC_SKILLS:
			{
				sectionHtml = buildSectionSkills(target, arg1);
				break;
			}
			default:
			{
				sectionHtml = buildSectionSlots(target);
				break;
			}
		}
		
		content = content.replace("%target_name%", target.getName()).replace("%section_content%", sectionHtml);
		
		activeChar.sendPacket(new NpcHtmlMessage(content));
	}
	
	// -----------------------------------------------------------------------
	// SECTION: Slots
	// -----------------------------------------------------------------------
	/**
	 * Generates an HTML table representing the homunculus slot information.<br>
	 * It displays the count of used, opened, and maximum slots for a player.<br>
	 * The output includes buttons to add or remove slots based on current availability.
	 * @param target The {@code Player} whose homunculus slots are being processed.
	 * @return A {@code String} containing the formatted HTML table.
	 */
	private String buildSectionSlots(Player target)
	{
		final int opened = target.getAvailableHomunculusSlotCount();
		final int max = PlayerConfig.MAX_HOMUNCULUS_COUNT;
		final int used = target.getHomunculusList().size();
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<br><table border=0 bgcolor=282828 align=center cellpadding=3 cellspacing=2 width=270>");
		
		// Info row - three separate td, no colspan.
		sb.append("<tr>");
		sb.append("<td width=90 align=center><font color=\"999999\">Used: </font><font color=\"00FFFF\">").append(used).append("</font></td>");
		sb.append("<td width=90 align=center><font color=\"999999\">Opened: </font><font color=\"00FFFF\">").append(opened).append("</font></td>");
		sb.append("<td width=90 align=center><font color=\"999999\">Max: </font><font color=\"00FFFF\">").append(max).append("</font></td>");
		sb.append("</tr>");
		
		// Spacer row - single td full width, no colspan.
		sb.append("<tr><td><br1></td><td></td><td></td></tr>");
		
		// Button row.
		sb.append("<tr>");
		sb.append("<td width=90 align=center>");
		
		if (opened > 1)
		{
			sb.append("<button value=\"Close Slot\" action=\"bypass -h admin_homunculi slot_remove\" width=88 height=21 back=\"").append(B).append("\" fore=\"").append(F).append("\">");
		}
		else
		{
			sb.append("<font color=\"444444\">Close Slot</font>");
		}
		
		sb.append("</td>");
		sb.append("<td width=90 align=center><font color=\"00FFFF\">").append(opened).append(" / ").append(max).append("</font></td>");
		sb.append("<td width=90 align=center>");
		
		if (opened < max)
		{
			sb.append("<button value=\"Open Slot\" action=\"bypass -h admin_homunculi slot_add\" width=88 height=21 back=\"").append(B).append("\" fore=\"").append(F).append("\">");
		}
		else
		{
			sb.append("<font color=\"444444\">Open Slot</font>");
		}
		
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("<tr><td><br1></td><td></td><td></td></tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	// -----------------------------------------------------------------------
	// SECTION: Current List - paginated, 5 entries per page
	// -----------------------------------------------------------------------
	/**
	 * Builds an HTML table representing the homunculus list for a specific page.<br>
	 * It calculates the correct range of slots based on the provided page index.<br>
	 * The table includes names, levels, and action buttons for each slot.
	 * @param target The {@code Player} whose homunculi are being listed.
	 * @param page The zero-based page number to display.
	 * @return A string containing the formatted HTML table.
	 */
	private String buildSectionList(Player target, int page)
	{
		final HomunculusList list = target.getHomunculusList();
		final int openedSlots = target.getAvailableHomunculusSlotCount();
		final int maxSlots = PlayerConfig.MAX_HOMUNCULUS_COUNT;
		
		final int totalPages = ((maxSlots + LIST_PAGE_SIZE) - 1) / LIST_PAGE_SIZE;
		final int p = Math.max(0, Math.min(totalPages - 1, page));
		final int start = p * LIST_PAGE_SIZE;
		final int end = Math.min(start + LIST_PAGE_SIZE, maxSlots);
		
		final StringBuilder sb = new StringBuilder();
		sb.append("<br><table border=0 bgcolor=282828 align=center cellpadding=2 cellspacing=2 width=270>");
		
		// Header row.
		sb.append("<tr>");
		sb.append("<td width=20 align=center><font color=\"999999\">#</font></td>");
		sb.append("<td width=105><font color=\"999999\">Name</font></td>");
		sb.append("<td width=25 align=center><font color=\"999999\">Lv</font></td>");
		sb.append("<td width=22 align=center></td>");
		sb.append("<td width=22 align=center></td>");
		sb.append("<td width=60 align=center><font color=\"999999\">Remove</font></td>");
		sb.append("</tr>");
		
		// Data rows for current page.
		for (int s = start; s < end; s++)
		{
			final Homunculus hom = list.get(s);
			sb.append("<tr>");
			sb.append("<td width=20 align=center><font color=\"777777\">").append(s + 1).append("</font></td>");
			
			if (hom != null)
			{
				sb.append("<td width=105><font color=\"00FF88\">").append(hom.getName()).append("</font></td>");
				sb.append("<td width=25 align=center><font color=\"00FFFF\">").append(hom.getLevel()).append("</font></td>");
				sb.append("<td width=22 align=center><button value=\"-\" action=\"bypass -h admin_homunculi hom_level_down ").append(s).append("\" width=20 height=18 back=\"").append(B).append("\" fore=\"").append(F).append("\"></td>");
				sb.append("<td width=22 align=center><button value=\"+\" action=\"bypass -h admin_homunculi hom_level_up ").append(s).append("\" width=20 height=18 back=\"").append(B).append("\" fore=\"").append(F).append("\"></td>");
				sb.append("<td width=60 align=center><button value=\"Remove\" action=\"bypass -h admin_homunculi remove ").append(s).append("\" width=56 height=18 back=\"").append(B).append("\" fore=\"").append(F).append("\"></td>");
			}
			else if (s < openedSlots)
			{
				sb.append("<td width=105><font color=\"444444\">[empty]</font></td>");
				sb.append("<td width=25></td><td width=22></td><td width=22></td><td width=60></td>");
			}
			else
			{
				sb.append("<td width=105><font color=\"333333\">[locked]</font></td>");
				sb.append("<td width=25></td><td width=22></td><td width=22></td><td width=60></td>");
			}
			
			sb.append("</tr>\n");
		}
		
		// Pagination row.
		final String prev = (p > 0) ? "<button value=\"<<\" action=\"bypass -h admin_homunculi section list 0 " + (p - 1) + "\" width=25 height=20 back=\"" + B + "\" fore=\"" + F + "\">" : "<font color=\"444444\"><</font>";
		final String next = (p < (totalPages - 1)) ? "<button value=\">>\" action=\"bypass -h admin_homunculi section list 0 " + (p + 1) + "\" width=25 height=20 back=\"" + B + "\" fore=\"" + F + "\">" : "<font color=\"444444\">></font>";
		sb.append("<tr>");
		sb.append("<td width=20 align=center>").append(prev).append("</td>");
		sb.append("<td width=105 align=center><font color=\"999999\">").append(p + 1).append("/").append(totalPages).append("</font></td>");
		sb.append("<td width=25 align=center>").append(next).append("</td>");
		sb.append("<td width=22></td><td width=22></td><td width=60></td>");
		sb.append("</tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	// -----------------------------------------------------------------------
	// SECTION: Add - type tabs + paginated give grid
	// -----------------------------------------------------------------------
	/**
	 * Builds the HTML content for the homunculus addition section.<br>
	 * It generates a grid of available templates based on the specified type and page.<br>
	 * The output includes buttons to give specific levels or navigate between pages.
	 * @param target The {@code Player} whose homunculus list is being checked.
	 * @param type The category index for the homunculi types.
	 * @param page The current page number for pagination.
	 * @return A {@code String} containing the formatted HTML table.
	 */
	private String buildSectionAdd(Player target, int type, int page)
	{
		final int t = Math.max(0, Math.min(TYPE_NAMES.length - 1, type));
		final HomunculusList list = target.getHomunculusList();
		final int[] typeIds = HOMUNCULI_BY_TYPE[t];
		final int totalPages = ((typeIds.length + ADD_PAGE_SIZE) - 1) / ADD_PAGE_SIZE;
		final int p = Math.max(0, Math.min(totalPages - 1, page));
		final int start = p * ADD_PAGE_SIZE;
		final int end = Math.min(start + ADD_PAGE_SIZE, typeIds.length);
		
		final StringBuilder sb = new StringBuilder();
		
		// Type tabs.
		sb.append("<table border=0 cellpadding=0 cellspacing=1 align=center><tr>");
		for (int i = 0; i < TYPE_NAMES.length; i++)
		{
			sb.append("<td><button value=\"").append(TYPE_NAMES[i]).append("\" action=\"bypass -h admin_homunculi section add ").append(i).append(" 0").append("\" width=70 height=21 back=\"").append(B).append("\" fore=\"").append(F).append("\"></td>");
		}
		sb.append("</tr></table><br1>");
		
		// Give grid.
		sb.append("<table border=0 bgcolor=282828 align=center cellpadding=2 cellspacing=2 width=270>");
		
		// Column header.
		sb.append("<tr>");
		sb.append("<td width=105></td>");
		for (int lvl = 1; lvl <= 5; lvl++)
		{
			sb.append("<td width=30 align=center><font color=\"999999\">Lv").append(lvl).append("</font></td>");
		}
		sb.append("</tr>");
		
		// Data rows.
		for (int i = start; i < end; i++)
		{
			final int tid = typeIds[i];
			final HomunculusTemplate tmpl = HomunculusData.getInstance().getTemplate(tid);
			final String name = (tmpl != null) ? tmpl.getName() : ("ID " + tid);
			final boolean isOwned = list.hasHomunculus(tid);
			sb.append("<tr><td width=105><font color=\"").append(isOwned ? "00FF88" : "AAAAFF").append("\">").append(name).append("</font></td>");
			for (int lvl = 1; lvl <= 5; lvl++)
			{
				sb.append("<td width=30 align=center>");
				if (isOwned)
				{
					sb.append("<font color=\"444444\">").append(lvl).append("</font>");
				}
				else
				{
					sb.append("<button value=\"").append(lvl).append("\" action=\"bypass -h admin_homunculi give ").append(tid).append(" ").append(lvl).append("\" width=27 height=20 back=\"").append(B).append("\" fore=\"").append(F).append("\">");
				}
				sb.append("</td>");
			}
			sb.append("</tr>\n");
		}
		
		// Pagination row.
		final String prev = (p > 0) ? "<button value=\"<<\" action=\"bypass -h admin_homunculi section add " + t + " " + (p - 1) + "\" width=25 height=20 back=\"" + B + "\" fore=\"" + F + "\">" : "<font color=\"444444\"><</font>";
		final String next = (p < (totalPages - 1)) ? "<button value=\">>\" action=\"bypass -h admin_homunculi section add " + t + " " + (p + 1) + "\" width=25 height=20 back=\"" + B + "\" fore=\"" + F + "\">" : "<font color=\"444444\">></font>";
		sb.append("<tr>");
		sb.append("<td width=105 align=right>").append(prev).append("</td>");
		sb.append("<td width=30 align=center><font color=\"999999\">").append(p + 1).append("/").append(totalPages).append("</font></td>");
		sb.append("<td width=30 align=left>").append(next).append("</td>");
		sb.append("<td width=30></td><td width=30></td><td width=30></td>");
		sb.append("</tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	// -----------------------------------------------------------------------
	// SECTION: Skills
	// -----------------------------------------------------------------------
	/**
	 * Generates the HTML content for the homunculus skills section.<br>
	 * It builds a navigation bar and a skill grid based on the target's homunculi.<br>
	 * The method handles buttons for setting levels, resetting skills, and navigating slots.
	 * @param target The {@code Player} whose homunculi are being displayed.
	 * @param activeSlot The index of the currently selected homunculus slot.
	 * @return A {@code String} containing the formatted HTML for the skill section.
	 */
	private String buildSectionSkills(Player target, int activeSlot)
	{
		final HomunculusList list = target.getHomunculusList();
		final int maxSlots = PlayerConfig.MAX_HOMUNCULUS_COUNT;
		int slot = activeSlot;
		if (list.get(slot) == null)
		{
			slot = 0;
		}
		final Homunculus hom = list.get(slot);
		
		int prevSlot = -1;
		int nextSlot = -1;
		for (int s = slot - 1; s >= 0; s--)
		{
			if (list.get(s) != null)
			{
				prevSlot = s;
				break;
			}
		}
		for (int s = slot + 1; s < maxSlots; s++)
		{
			if (list.get(s) != null)
			{
				nextSlot = s;
				break;
			}
		}
		
		final StringBuilder sb = new StringBuilder();
		
		// Nav bar.
		sb.append("<br><table border=0 bgcolor=282828 align=center cellpadding=2 cellspacing=2 width=270>");
		sb.append("<tr>");
		sb.append("<td width=30 align=center>");
		
		if (prevSlot >= 0)
		{
			sb.append("<button value=\"<<\" action=\"bypass -h admin_homunculi section skills ").append(prevSlot).append("\" width=25 height=20 back=\"").append(B).append("\" fore=\"").append(F).append("\">");
		}
		else
		{
			sb.append("<font color=\"444444\"><</font>");
		}
		
		sb.append("</td>");
		sb.append("<td align=center>");
		
		if (hom != null)
		{
			final int level = hom.getLevel();
			sb.append("<font color=\"LEVEL\">").append(hom.getName()).append("</font> <font color=\"00FFFF\">(Lv.").append(level).append(")</font>");
		}
		else
		{
			sb.append("<font color=\"777777\">No homunculi owned</font>");
		}
		
		sb.append("</td>");
		sb.append("<td width=30 align=center>");
		
		if (nextSlot >= 0)
		{
			sb.append("<button value=\">>\" action=\"bypass -h admin_homunculi section skills ").append(nextSlot).append("\" width=25 height=20 back=\"").append(B).append("\" fore=\"").append(F).append("\">");
		}
		else
		{
			sb.append("<font color=\"444444\">></font>");
		}
		
		sb.append("</td>");
		sb.append("</tr>");
		sb.append("</table>");
		
		if (hom == null)
		{
			return sb.toString();
		}
		
		// Edit box row.
		sb.append("<br><table width=270 align=center><tr>");
		sb.append("<td align=right><font color=\"999999\">Set level 0-3: </font></td>");
		sb.append("<td align=left><edit var=\"qbox\" width=40 height=14></td>");
		sb.append("</tr></table><br>");
		
		// Skill grid.
		final int homLevel = hom.getLevel();
		sb.append("<table border=0 bgcolor=282828 align=center cellpadding=2 cellspacing=2>");
		
		// Spacer.
		sb.append("<tr><td height=3></td><td></td><td></td><td></td><td></td></tr>");
		
		// MAX row.
		sb.append("<tr>");
		for (int sk = 1; sk <= 5; sk++)
		{
			sb.append("<td align=center>");
			if (sk <= homLevel)
			{
				sb.append("<button value=\"MAX\" action=\"bypass -h admin_homunculi skill_set ").append(slot).append(" ").append(sk).append(" ").append(MAX_SKILL_LEVEL).append("\" width=45 height=20 back=\"").append(B).append("\" fore=\"").append(F).append("\">");
			}
			else
			{
				sb.append("<font color=\"444444\">-</font>");
			}
			sb.append("</td>");
		}
		sb.append("</tr>");
		
		// Icon row.
		sb.append("<tr>");
		for (int sk = 1; sk <= 5; sk++)
		{
			final int skLvl = getSkillLevel(hom, sk);
			final String icon = SKILL_ICONS[sk];
			final String overlay = ((skLvl > 0) && (skLvl < LEVEL_PANEL.length)) ? LEVEL_PANEL[skLvl] : icon;
			sb.append("<td align=center>");
			
			if (sk <= homLevel)
			{
				sb.append("<table width=38 height=38 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\" cellpadding=3 cellspacing=0><tr><td align=center valign=middle>");
				sb.append("<table width=32 height=32 cellpadding=0 cellspacing=0><tr><td align=center background=\"").append(icon).append("\">");
				sb.append("<button value=\" \" action=\"bypass -h admin_homunculi skill_set ").append(slot).append(" ").append(sk).append(" $qbox\"");
				sb.append(" width=32 height=32 back=\"").append(overlay).append("\" fore=\"").append(overlay).append("\">");
				sb.append("</td></tr></table></td></tr></table>");
			}
			else
			{
				sb.append("<table width=38 height=38 background=\"L2UI_CT1.Windows.Windows_DF_TooltipBG\" cellpadding=3 cellspacing=0><tr><td align=center valign=middle>");
				sb.append("<font color=\"444444\">Lv").append(sk).append("</font>");
				sb.append("</td></tr></table>");
			}
			
			sb.append("</td>");
		}
		sb.append("</tr>");
		
		// Spacer.
		sb.append("<tr><td height=3></td><td></td><td></td><td></td><td></td></tr>");
		
		// RES row.
		sb.append("<tr>");
		for (int sk = 1; sk <= 5; sk++)
		{
			sb.append("<td align=center>");
			if (sk <= homLevel)
			{
				sb.append("<button value=\"RES\" action=\"bypass -h admin_homunculi skill_set ").append(slot).append(" ").append(sk).append(" 0\" width=45 height=20 back=\"").append(B).append("\" fore=\"").append(F).append("\">");
			}
			else
			{
				sb.append("<font color=\"444444\">-</font>");
			}
			sb.append("</td>");
		}
		sb.append("</tr>");
		
		// Spacer.
		sb.append("<tr><td height=3></td><td></td><td></td><td></td><td></td></tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	// -----------------------------------------------------------------------
	// Helpers
	// -----------------------------------------------------------------------
	/**
	 * Updates the client with the latest player information.<br>
	 * This method broadcasts user info and sends a new {@code ExShowHomunculusList} packet.
	 * @param player The {@code Player} instance to synchronize.
	 */
	private void syncClient(Player player)
	{
		player.broadcastUserInfo();
		player.sendPacket(new ExShowHomunculusList(player));
	}
	
	/**
	 * Retrieves the skill level for a specific skill index.<br>
	 * This method maps an integer index to the corresponding skill level of a {@code Homunculus}.
	 * @param hom The {@code Homunculus} object to check.
	 * @param index The zero-based index of the skill.
	 * @return The level of the skill or 0 if the index is invalid.
	 */
	private int getSkillLevel(Homunculus hom, int index)
	{
		switch (index)
		{
			case 1:
			{
				return hom.getSkillLevel1();
			}
			case 2:
			{
				return hom.getSkillLevel2();
			}
			case 3:
			{
				return hom.getSkillLevel3();
			}
			case 4:
			{
				return hom.getSkillLevel4();
			}
			case 5:
			{
				return hom.getSkillLevel5();
			}
			default:
			{
				return 0;
			}
		}
	}
	
	/**
	 * Updates the skill level for a specific skill on a {@code Homunculus}.<br>
	 * The method maps the provided index to the corresponding skill setter.
	 * @param hom The {@code Homunculus} object to modify.
	 * @param index The zero-based index of the skill to update.
	 * @param level The new level value to assign to the skill.
	 */
	private void setSkillLevel(Homunculus hom, int index, int level)
	{
		switch (index)
		{
			case 1:
			{
				hom.setSkillLevel1(level);
				break;
			}
			case 2:
			{
				hom.setSkillLevel2(level);
				break;
			}
			case 3:
			{
				hom.setSkillLevel3(level);
				break;
			}
			case 4:
			{
				hom.setSkillLevel4(level);
				break;
			}
			case 5:
			{
				hom.setSkillLevel5(level);
				break;
			}
		}
	}
	
	/**
	 * Retrieves the next integer from a {@code StringTokenizer}.<br>
	 * Returns the default value if no tokens remain or if parsing fails.
	 * @param st The {@code StringTokenizer} to read from.
	 * @param def The default value to return in case of an error.
	 * @return The parsed integer or the default value.
	 */
	private int nextInt(StringTokenizer st, int def)
	{
		if (!st.hasMoreTokens())
		{
			return def;
		}
		
		try
		{
			return Integer.parseInt(st.nextToken());
		}
		catch (NumberFormatException e)
		{
			return def;
		}
	}
	
	/**
	 * Determines the category type of a specific homunculus. <br>
	 * It searches through the {@code HOMUNCULI_BY_TYPE} array to find a match. <br>
	 * If no match is found, it defaults to {@code 0}.
	 * @param templateId The unique ID of the homunculus template.
	 * @return The integer index representing the type of the template.
	 */
	private int typeOfTemplate(int templateId)
	{
		for (int t = 0; t < HOMUNCULI_BY_TYPE.length; t++)
		{
			for (int id : HOMUNCULI_BY_TYPE[t])
			{
				if (id == templateId)
				{
					return t;
				}
			}
		}
		
		return 0;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns a static array of command strings.
	 * @return an array of {@code String} containing all valid commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
