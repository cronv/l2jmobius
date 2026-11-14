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
package ai.areas.Aden.Gallias;

import java.util.Set;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSubChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.clientpackets.RequestAcquireSkill;

/**
 * Defines the artificial intelligence behavior for the {@code Gallias} NPC.<br>
 * This script is based on the {@code Trandon} AI logic.
 * @author Mobius
 */
public class Gallias extends Script
{
	// NPC
	private static final int NPC_ID = 34514;
	
	// Items
	private static final int SUB_CERTIFICATE = 10280;
	private static final int DUAL_CERTIFICATE = 36078;
	private static final int DUAL_CERTIFICATE_ENHANCED = 81731;
	private static final int ENERGY_OF_POWER = 80924;
	
	// Skills
	private static final int DUAL_CLASS_RENEWED_ENERGY_OF_POWER = 30820;
	
	// Misc @formatter:off
	private static final int[] SUB_SKILL_LEVELS = {65, 70, 75, 80};
	private static final int[] DUAL_SKILL_LEVELS = {85, 90, 95, 99, 101, 103, 105, 107, 109, 110};
	// @formatter:on
	private static final String SUB_CERTIFICATE_COUNT_VAR = "SUB_CERTIFICATE_COUNT";
	private static final String DUAL_CERTIFICATE_COUNT_VAR = "DUAL_CERTIFICATE_COUNT";
	
	/**
	 * Private constructor for the {@code Gallias} class.<br>
	 * This constructor initializes the NPC IDs and talk IDs required for the script.
	 */
	private Gallias()
	{
		addStartNpc(NPC_ID);
		addFirstTalkId(NPC_ID);
		addTalkId(NPC_ID);
	}
	
	/**
	 * Handles various events for the Gallias NPC script.<br>
	 * It processes logic for sub-class and dual-class certificates, skills, and item rewards.<br>
	 * The method returns the appropriate HTML text based on the player's status and actions.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The HTML string to display, or {@code null} if no text is applicable.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final String[] substrings = event.split(" ");
		if (substrings.length < 1)
		{
			return null;
		}
		
		String htmltext = substrings[0];
		switch (htmltext)
		{
			case "34514.html":
			case "34514-01.html":
			case "34514-02.html":
			case "34514-04.html":
			case "34514-05.html":
			case "34514-06.html":
			case "34514-07.html":
			case "34514-08.html":
			case "34514-09.html":
			case "34514-10.html":
			case "34514-11.html":
			case "34514-19.html":
			{
				break;
			}
			case "34514-03.html":
			{
				if (!player.hasDualClass())
				{
					htmltext = "34514-05.html";
				}
				else if (hasAtLeastOneQuestItem(player, ENERGY_OF_POWER) || (player.getKnownSkill(DUAL_CLASS_RENEWED_ENERGY_OF_POWER) != null))
				{
					htmltext = "34514-04.html";
				}
				else
				{
					giveItems(player, ENERGY_OF_POWER, 1);
					htmltext = "34514-03.html";
				}
				break;
			}
			case "34514-12.html":
			{
				if (!player.isSubClassActive())
				{
					htmltext = "34514-13.html";
				}
				else if (!player.isInventoryUnder90(false) || (player.getWeightPenalty() >= 2))
				{
					htmltext = "34514-14.html";
				}
				break;
			}
			case "subCertify":
			{
				if ((substrings.length < 2) || !player.isSubClassActive())
				{
					return null;
				}
				
				final int index = Integer.parseInt(substrings[1]);
				if ((index < 0) || (index > 3))
				{
					return null;
				}
				
				final int level = SUB_SKILL_LEVELS[index];
				if (player.getLevel() < level)
				{
					htmltext = getHtm(player, "34514-16.html").replace("%level%", String.valueOf(level));
				}
				else if (player.getVariables().hasVariable(getSubSkillVariableName(player, level)))
				{
					htmltext = "34514-17.html";
				}
				else
				{
					htmltext = getHtm(player, "34514-18.html");
					htmltext = htmltext.replace("%level%", String.valueOf(level));
					htmltext = htmltext.replace("%index%", String.valueOf(index));
				}
				break;
			}
			case "giveSubCertify":
			{
				if ((substrings.length < 2) || !player.isSubClassActive())
				{
					return null;
				}
				
				final int index = Integer.parseInt(substrings[1]);
				if ((index < 0) || (index > 3))
				{
					return null;
				}
				
				final int level = SUB_SKILL_LEVELS[index];
				final PlayerVariables vars = player.getVariables();
				if ((player.getLevel() < level) || vars.hasVariable(getSubSkillVariableName(player, level)))
				{
					htmltext = null;
				}
				else
				{
					final int subId = player.getPlayerClass().getId();
					final int currentCount = player.getVariables().getInt(SUB_CERTIFICATE_COUNT_VAR + subId, 0);
					if (currentCount < SUB_SKILL_LEVELS.length)
					{
						player.getVariables().set(SUB_CERTIFICATE_COUNT_VAR + subId, currentCount + 1);
						vars.set(getSubSkillVariableName(player, level), true);
						giveItems(player, SUB_CERTIFICATE, 1);
					}
					
					htmltext = "34514-20.html";
				}
				break;
			}
			case "learnSubSkill":
			{
				if (player.isSubClassActive() || !ownsAtLeastOneItem(player, SUB_CERTIFICATE))
				{
					htmltext = "34514-21.html";
				}
				else
				{
					RequestAcquireSkill.showSubSkillList(player);
					htmltext = null;
				}
				break;
			}
			case "deleteSubSkill":
			{
				if (player.isSubClassActive())
				{
					htmltext = "34514-21.html";
				}
				else if (player.getAdena() < PlayerConfig.FEE_DELETE_SUBCLASS_SKILLS)
				{
					htmltext = "34514-22.html";
				}
				else if (!hasSubCertificate(player))
				{
					htmltext = "34514-23.html";
				}
				else
				{
					htmltext = null; // TODO: Unknown html
					takeItems(player, SUB_CERTIFICATE, -1);
					player.getWarehouse().destroyItemByItemId(ItemProcessType.QUEST, SUB_CERTIFICATE, -1, player, npc);
					takeItems(player, Inventory.ADENA_ID, PlayerConfig.FEE_DELETE_SUBCLASS_SKILLS);
					for (SubClassHolder subclass : player.getSubClasses().values())
					{
						player.getVariables().remove(SUB_CERTIFICATE_COUNT_VAR + subclass.getId());
					}
					
					final PlayerVariables vars = player.getVariables();
					for (int i = 1; i <= 3; i++)
					{
						for (int lv : SUB_SKILL_LEVELS)
						{
							vars.remove("SubSkill-" + i + "-" + lv);
						}
					}
					
					takeSkills(player, "SubSkillList");
				}
				break;
			}
			case "34514-26.html":
			{
				// TODO: What happens when you have all dual certificates?
				if (!player.isDualClassActive())
				{
					htmltext = "34514-24.html";
				}
				else if (!player.isInventoryUnder90(false) || (player.getWeightPenalty() >= 2))
				{
					htmltext = "34514-25.html";
				}
				break;
			}
			case "34514-35.html":
			{
				// TODO: What happens when you have all dual certificates?
				if (!player.isDualClassActive())
				{
					htmltext = "34514-24.html";
				}
				else if (!player.isInventoryUnder90(false) || (player.getWeightPenalty() >= 2))
				{
					htmltext = "34514-25.html";
				}
				break;
			}
			case "dualCertify":
			{
				if ((substrings.length < 2) || !player.isDualClassActive())
				{
					return null;
				}
				
				final int index = Integer.parseInt(substrings[1]);
				if ((index < 0) || (index > (DUAL_SKILL_LEVELS.length - 1)))
				{
					return null;
				}
				
				final int level = DUAL_SKILL_LEVELS[index];
				final PlayerVariables vars = player.getVariables();
				if (vars.hasVariable(getDualSkillVariableName(level)))
				{
					htmltext = getHtm(player, "34514-27.html");
				}
				else if ((player.getLevel() < level) || (player.getStat().getBaseLevel() < level))
				{
					htmltext = getHtm(player, "34514-28.html");
				}
				else
				{
					final int currentCount = player.getVariables().getInt(DUAL_CERTIFICATE_COUNT_VAR, 0);
					if (currentCount < DUAL_SKILL_LEVELS.length)
					{
						player.getVariables().set(DUAL_CERTIFICATE_COUNT_VAR, currentCount + 1);
						vars.set(getDualSkillVariableName(level), true);
						giveItems(player, level < 107 ? DUAL_CERTIFICATE : DUAL_CERTIFICATE_ENHANCED, 1);
					}
					
					htmltext = getHtm(player, "34514-29.html");
				}
				
				htmltext = htmltext.replace("%level%", String.valueOf(level));
				break;
			}
			case "learnDualSkill":
			{
				// TODO: What happens when you have all dual-certificates used?
				if (player.isSubClassActive())
				{
					htmltext = "34514-30.html";
				}
				else if (!ownsAtLeastOneItem(player, DUAL_CERTIFICATE) && !ownsAtLeastOneItem(player, DUAL_CERTIFICATE_ENHANCED))
				{
					htmltext = "34514-31.html";
				}
				else if ((player.getLevel() < DUAL_SKILL_LEVELS[0]) || (player.getStat().getBaseLevel() < DUAL_SKILL_LEVELS[0]))
				{
					// This case should not happen
					htmltext = null;
				}
				else
				{
					RequestAcquireSkill.showDualSkillList(player);
					htmltext = null;
				}
				break;
			}
			case "deleteDualSkill":
			{
				if (player.isSubClassActive())
				{
					htmltext = "34514-30.html";
				}
				else if (player.getAdena() < PlayerConfig.FEE_DELETE_DUALCLASS_SKILLS)
				{
					htmltext = "34514-32.html";
				}
				else if (!hasDualCertificate(player))
				{
					htmltext = "34514-33.html";
				}
				else
				{
					htmltext = null; // TODO: Unknown html
					takeItems(player, DUAL_CERTIFICATE, -1);
					takeItems(player, DUAL_CERTIFICATE_ENHANCED, -1);
					player.getWarehouse().destroyItemByItemId(ItemProcessType.QUEST, DUAL_CERTIFICATE, -1, player, npc);
					player.getWarehouse().destroyItemByItemId(ItemProcessType.QUEST, DUAL_CERTIFICATE_ENHANCED, -1, player, npc);
					takeItems(player, Inventory.ADENA_ID, PlayerConfig.FEE_DELETE_DUALCLASS_SKILLS);
					player.getVariables().remove(DUAL_CERTIFICATE_COUNT_VAR);
					
					final PlayerVariables vars = player.getVariables();
					for (int lv : DUAL_SKILL_LEVELS)
					{
						vars.remove(getDualSkillVariableName(lv));
					}
					
					takeSkills(player, "DualSkillList");
				}
				break;
			}
			default:
			{
				htmltext = null;
			}
		}
		
		return htmltext;
	}
	
	// TODO: Move this to char skills
	/**
	 * Handles the logic when a player changes their subclass.<br>
	 * This method grants skills based on whether the player is dual-classing or sub-classing.
	 * @param event The {@code OnPlayerSubChange} event containing the affected player.
	 */
	@RegisterEvent(EventType.ON_PLAYER_SUB_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onSubChange(OnPlayerSubChange event)
	{
		final Player player = event.getPlayer();
		if (player.isDualClassActive() || !player.isSubClassActive())
		{
			giveSkills(player, "DualSkillList");
		}
		
		giveSkills(player, "SubSkillList");
	}
	
	/**
	 * Handles the logic when a player logs into the game.<br>
	 * This method checks if the player has active sub-classes or dual-classes.<br>
	 * It grants the appropriate skills based on those statuses.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		if (player.isDualClassActive() || !player.isSubClassActive())
		{
			giveSkills(player, "DualSkillList");
		}
		
		giveSkills(player, "SubSkillList");
	}
	
	/**
	 * Checks if the player possesses any sub-class certificates.<br>
	 * It verifies this by looking for specific skill variables in the {@code PlayerVariables}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if at least one certificate is found, otherwise {@code false}.
	 */
	private boolean hasSubCertificate(Player player)
	{
		final PlayerVariables vars = player.getVariables();
		final Set<Integer> subs = player.getSubClasses().keySet();
		for (int index : subs)
		{
			for (int lv : SUB_SKILL_LEVELS)
			{
				if (vars.hasVariable("SubSkill-" + index + "-" + lv))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the player possesses any dual skills.<br>
	 * It iterates through all required levels to find a matching variable.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if at least one dual skill is found, otherwise {@code false}.
	 */
	private boolean hasDualCertificate(Player player)
	{
		final PlayerVariables vars = player.getVariables();
		for (int lv : DUAL_SKILL_LEVELS)
		{
			if (vars.hasVariable(getDualSkillVariableName(lv)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Generates a unique variable name for a sub-skill.<br>
	 * The name is based on the {@code Player} class index and the skill level.
	 * @param player The {@code Player} object used to retrieve the class index.
	 * @param level The level of the skill.
	 * @return A formatted string representing the sub-skill variable name.
	 */
	private String getSubSkillVariableName(Player player, int level)
	{
		return "SubSkill-" + player.getClassIndex() + "-" + level;
	}
	
	/**
	 * Generates the variable name for a dual skill based on its level.<br>
	 * The result follows the format {@code DualSkill-level}.
	 * @param level The current level of the dual skill.
	 * @return A {@code String} containing the formatted variable name.
	 */
	private String getDualSkillVariableName(int level)
	{
		return "DualSkill-" + level;
	}
	
	/**
	 * Removes specific skills from a {@code Player}.<br>
	 * This method reads a list of skills from the player variables based on the provided type.<br>
	 * It removes each skill found in that list and clears the variable.
	 * @param player The {@code Player} who will have their skills removed.
	 * @param type The variable name used to identify the group of skills to remove.
	 */
	private void takeSkills(Player player, String type)
	{
		final PlayerVariables vars = player.getVariables();
		final String list = vars.getString(type, "");
		if (!list.isEmpty())
		{
			final String[] skills = list.split(";");
			for (String skill : skills)
			{
				final String[] str = skill.split("-");
				final Skill sk = SkillData.getInstance().getSkill(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
				player.removeSkill(sk);
			}
			
			vars.remove(type);
			player.sendSkillList();
		}
	}
	
	/**
	 * Grants skills to a {@code Player} based on a variable type.<br>
	 * This method reads a semicolon-separated list from the player's variables.<br>
	 * It parses each entry and adds the corresponding {@code Skill} to the player.
	 * @param player The {@code Player} who will receive the skills.
	 * @param type The name of the variable containing the skill data.
	 */
	private void giveSkills(Player player, String type)
	{
		final String list = player.getVariables().getString(type, "");
		if (!list.isEmpty())
		{
			final String[] skills = list.split(";");
			for (String skill : skills)
			{
				final String[] str = skill.split("-");
				final Skill sk = SkillData.getInstance().getSkill(Integer.parseInt(str[0]), Integer.parseInt(str[1]));
				player.addSkill(sk, false);
			}
		}
	}
	
	/**
	 * Handles the logic when a {@code Player} speaks to an {@code Npc} for the first time.<br>
	 * It returns the name of the HTML file to display.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the dialogue HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Gallias} class.<br>
	 * It initializes a new instance of the {@code Gallias} script.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Gallias();
	}
}
