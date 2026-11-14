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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.replacement.SkillReplacementHolder;
import org.l2jmobius.gameserver.mechanics.skill.replacement.SkillReplacementList;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Shows the skill replacements that are active on the administrator right now.<br>
 * Only the chains that start at a skill the player knows are listed, since the rest can never fire.
 * @author fruit
 */
public class AdminReplacementInfo implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_replacement_info"
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final SkillReplacementList manager = activeChar.getSkillReplacementListIfPresent();
		final Set<Integer> reachableSkillIds = manager == null ? Set.of() : getReachableSkillIds(activeChar, manager);
		final StringBuilder html = new StringBuilder();
		html.append("<html><body><center><font color=\"LEVEL\">Skill Replacement</font><br1>");
		html.append(activeChar.getName());
		html.append("</center><br>");
		
		int shown = 0;
		if (manager != null)
		{
			for (SkillReplacementHolder rule : manager.getRules())
			{
				if (!reachableSkillIds.contains(Integer.valueOf(rule.getExistingSkillId())))
				{
					continue;
				}
				
				if (shown == 0)
				{
					html.append("<table width=\"100%\" cellspacing=\"2\" cellpadding=\"3\">");
					html.append("<tr><td width=\"40%\" align=\"center\"><font color=\"LEVEL\">Source</font></td>");
					html.append("<td width=\"40%\" align=\"center\"><font color=\"LEVEL\">Replacement</font></td>");
					html.append("<td width=\"20%\" align=\"center\"><font color=\"LEVEL\">Priority</font></td></tr>");
				}
				
				shown++;
				appendRule(html, manager, rule);
			}
		}
		
		if (shown == 0)
		{
			html.append("<center><font color=\"FF0000\">No active skill replacements for known skills.</font></center>");
		}
		else
		{
			html.append("</table><br><center><font color=\"AAAAAA\">Applicable replacement rules: ");
			html.append(shown);
			html.append("</font></center>");
		}
		
		html.append("<br><center><button value=\"Refresh\" action=\"bypass -h admin_replacement_info\" width=\"80\" height=\"25\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></center>");
		html.append("</body></html>");
		HtmlUtil.sendCBHtml(activeChar, html.toString());
		return true;
	}
	
	/**
	 * Appends one rule as a table row, naming what it resolves to when that differs from its own target.
	 * @param html The page being built.
	 * @param manager The manager holding the rule.
	 * @param rule The rule to describe.
	 */
	private void appendRule(StringBuilder html, SkillReplacementList manager, SkillReplacementHolder rule)
	{
		final int sourceId = rule.getExistingSkillId();
		final int replacementId = rule.getReplacementSkillId();
		final int resolvedId = manager.resolveSkillId(sourceId);
		html.append("<tr><td valign=\"middle\">");
		appendSkill(html, sourceId);
		html.append("</td><td valign=\"middle\">");
		appendSkill(html, replacementId);
		if (resolvedId != replacementId)
		{
			html.append("<br1><font color=\"LEVEL\">Resolved: ");
			html.append(resolvedId);
			html.append(" : ");
			html.append(getSkillName(resolvedId));
			html.append("</font>");
		}
		
		html.append("</td><td align=\"center\" valign=\"middle\">");
		html.append(rule.getPriority());
		html.append("</td></tr>");
	}
	
	/**
	 * Appends the icon, ID and name of one skill.
	 * @param html The page being built.
	 * @param skillId The skill to describe.
	 */
	private void appendSkill(StringBuilder html, int skillId)
	{
		final Skill skill = getSkill(skillId);
		if (skill != null)
		{
			html.append("<img src=\"");
			html.append(skill.getIcon());
			html.append("\" width=32 height=32>");
		}
		
		html.append(skillId);
		html.append(" : ");
		html.append(getSkillName(skillId));
	}
	
	/**
	 * Collects every skill a chain can reach from the skills the player knows.<br>
	 * A rule whose source is not reachable can never fire, so it is not worth showing.
	 * @param player The player whose known skills open the chains.
	 * @param manager The manager holding the rules.
	 * @return The IDs of every skill the chains touch.
	 */
	private Set<Integer> getReachableSkillIds(Player player, SkillReplacementList manager)
	{
		final Set<Integer> result = new HashSet<>();
		for (SkillReplacementHolder rule : manager.getRules())
		{
			final int sourceId = rule.getExistingSkillId();
			if (player.getKnownSkill(sourceId) != null)
			{
				result.add(Integer.valueOf(sourceId));
			}
		}
		
		boolean grown = true;
		while (grown)
		{
			grown = false;
			for (SkillReplacementHolder rule : manager.getRules())
			{
				if (result.contains(Integer.valueOf(rule.getExistingSkillId())) && result.add(Integer.valueOf(rule.getReplacementSkillId())))
				{
					grown = true;
				}
			}
		}
		
		return result;
	}
	
	/**
	 * @param skillId The skill to load.
	 * @return The skill at its highest level, or {@code null} when no such skill exists.
	 */
	private Skill getSkill(int skillId)
	{
		final int maxLevel = SkillData.getInstance().getMaxLevel(skillId);
		return maxLevel <= 0 ? null : SkillData.getInstance().getSkill(skillId, maxLevel);
	}
	
	/**
	 * @param skillId The skill to name.
	 * @return The skill name, or {@code Unknown} when no such skill exists.
	 */
	private String getSkillName(int skillId)
	{
		final Skill skill = getSkill(skillId);
		return skill == null ? "Unknown" : skill.getName();
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
