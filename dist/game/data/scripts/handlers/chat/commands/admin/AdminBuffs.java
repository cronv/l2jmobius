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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.html.PageBuilder;
import org.l2jmobius.gameserver.mechanics.html.PageResult;
import org.l2jmobius.gameserver.mechanics.html.styles.ButtonsStyle;
import org.l2jmobius.gameserver.mechanics.options.AugmentationInstance;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.util.GMAudit;

/**
 * Handles administrative commands for applying buffs to players.<br>
 * This class allows administrators to grant various effects and skills to characters.<br>
 * It provides a way to manage game mechanics via the {@code AdminBuffs} command.
 * @author Mobius
 */
public class AdminBuffs implements IAdminCommandHandler
{
	private static final int PAGE_LIMIT = 7;
	private static final int EFFECT_PAGE_LIMIT = 5;
	private static final int EFFECT_VALUE_MAX_LENGTH = 60;
	private static final String FONT_RED1 = "<font color=\"FF0000\">";
	private static final String FONT_RED2 = "</font>";
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_buff",
		"admin_getbuffs",
		"admin_getbuffs_ps",
		"admin_getbuffs_items",
		"admin_stopbuff",
		"admin_stopallbuffs",
		"admin_viewblockedeffects",
		"admin_viewskilleffects",
		"admin_viewskilleffects_ps",
		"admin_areacancel",
		"admin_removereuse",
		"admin_switch_gm_buffs"
	};
	
	/**
	 * Processes administrative commands sent by a {@code Player}.<br>
	 * It handles various actions such as buffing, viewing effects, and resetting skills.
	 * @param commandValue The raw string input from the player.
	 * @param activeChar The {@code Player} object who executed the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String commandValue, Player activeChar)
	{
		String command = commandValue;
		if (command.startsWith("admin_buff"))
		{
			if ((activeChar.getTarget() == null) || !activeChar.getTarget().isCreature())
			{
				activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				return false;
			}
			
			final StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken();
			if (!st.hasMoreTokens())
			{
				activeChar.sendSysMessage("Skill Id and level are not specified.");
				activeChar.sendSysMessage("Usage: //buff <skillId> <skillLevel>");
				return false;
			}
			
			try
			{
				final int skillId = Integer.parseInt(st.nextToken());
				final int skillLevel = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : SkillData.getInstance().getMaxLevel(skillId);
				final Creature target = activeChar.getTarget().asCreature();
				final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
				if (skill == null)
				{
					activeChar.sendSysMessage("Skill with id: " + skillId + ", level: " + skillLevel + " not found.");
					return false;
				}
				
				activeChar.sendSysMessage("Admin buffing " + skill.getName() + " (" + skillId + "," + skillLevel + ")");
				skill.applyEffects(activeChar, target);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed buffing: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //buff <skillId> <skillLevel>");
				return false;
			}
		}
		else if (command.startsWith("admin_getbuffs"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			command = st.nextToken();
			final boolean itemsOnly = command.endsWith("_items");
			final boolean passive = itemsOnly || command.endsWith("_ps");
			if (st.hasMoreTokens())
			{
				final String playername = st.nextToken();
				final Player player = World.getPlayer(playername);
				if (player != null)
				{
					int page = 0;
					if (st.hasMoreTokens())
					{
						page = Integer.parseInt(st.nextToken());
					}
					
					showBuffs(activeChar, player, page, passive, itemsOnly);
					return true;
				}
				
				activeChar.sendSysMessage("The player " + playername + " is not online.");
				return false;
			}
			else if ((activeChar.getTarget() != null) && activeChar.getTarget().isCreature())
			{
				showBuffs(activeChar, activeChar.getTarget().asCreature(), 0, passive, itemsOnly);
				return true;
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				return false;
			}
		}
		else if (command.startsWith("admin_stopbuff"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				final int skillId = Integer.parseInt(st.nextToken());
				removeBuff(activeChar, objectId, skillId);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed removing effect: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //stopbuff <objectId> <skillId>");
				return false;
			}
		}
		else if (command.startsWith("admin_stopallbuffs"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				removeAllBuffs(activeChar, objectId);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed removing all effects: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //stopallbuffs <objectId>");
				return false;
			}
		}
		else if (command.startsWith("admin_viewblockedeffects"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				viewBlockedEffects(activeChar, objectId);
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed viewing blocked effects: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //viewblockedeffects <objectId>");
				return false;
			}
		}
		else if (command.startsWith("admin_viewskilleffects"))
		{
			try
			{
				final StringTokenizer st = new StringTokenizer(command, " ");
				st.nextToken();
				final int objectId = Integer.parseInt(st.nextToken());
				final int skillId = Integer.parseInt(st.nextToken());
				final int skillLevel = Integer.parseInt(st.nextToken());
				final int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
				viewSkillEffects(activeChar, objectId, skillId, skillLevel, page, command.contains("_ps"));
				return true;
			}
			catch (Exception e)
			{
				activeChar.sendSysMessage("Failed viewing skill effects: " + e.getMessage());
				activeChar.sendSysMessage("Usage: //viewskilleffects <objectId skillId skillLevel>");
				return false;
			}
		}
		else if (command.startsWith("admin_areacancel"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			if (!st.hasMoreTokens())
			{
				activeChar.sendSysMessage("Usage: //areacancel <radius>");
				return false;
			}
			
			final String val = st.nextToken();
			try
			{
				final int radius = Integer.parseInt(val);
				final int[] affected =
				{
					0
				};
				World.forEachVisibleObjectInRange(activeChar, Player.class, radius, player ->
				{
					player.stopAllEffects();
					affected[0]++;
				});
				
				activeChar.sendSysMessage("Effects canceled for " + affected[0] + " player(s) within radius " + radius);
				return true;
			}
			catch (NumberFormatException e)
			{
				activeChar.sendSysMessage("Invalid radius value.");
				return false;
			}
		}
		else if (command.startsWith("admin_removereuse"))
		{
			final StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken();
			
			Player player = null;
			if (st.hasMoreTokens())
			{
				final String playername = st.nextToken();
				
				try
				{
					player = World.getPlayer(playername);
				}
				catch (Exception e)
				{
				}
				
				if (player == null)
				{
					activeChar.sendSysMessage("The player " + playername + " is not online.");
					return false;
				}
			}
			else if ((activeChar.getTarget() != null) && activeChar.getTarget().isPlayer())
			{
				player = activeChar.getTarget().asPlayer();
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THAT_IS_AN_INCORRECT_TARGET);
				return false;
			}
			
			try
			{
				player.resetTimeStamps();
				player.resetTimeStampsItems();
				player.resetDisabledSkills();
				player.sendPacket(new SkillCoolTime(player));
				activeChar.sendSysMessage("Skill reuse and item reuse was removed from " + player.getName() + ".");
				return true;
			}
			catch (NullPointerException e)
			{
				return false;
			}
		}
		else if (command.startsWith("admin_switch_gm_buffs"))
		{
			if (GeneralConfig.GM_GIVE_SPECIAL_SKILLS != GeneralConfig.GM_GIVE_SPECIAL_AURA_SKILLS)
			{
				final boolean toAuraSkills = activeChar.getKnownSkill(7041) != null;
				switchSkills(activeChar, toAuraSkills);
				activeChar.sendSkillList();
				activeChar.sendSysMessage("You have succefully changed to target " + (toAuraSkills ? "aura" : "one") + " special skills.");
				return true;
			}
			
			activeChar.sendSysMessage("There is nothing to switch.");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Switches the active skill set for a specific player.<br>
	 * This method removes current skills and adds new ones based on the provided boolean.
	 * @param gmchar The {@code Player} whose skills will be modified.
	 * @param toAuraSkills Set to {@code true} to switch to aura skills, or {@code false} for standard skills.
	 */
	private void switchSkills(Player gmchar, boolean toAuraSkills)
	{
		final Collection<Skill> skills = toAuraSkills ? SkillTreeData.getInstance().getGMSkillTree() : SkillTreeData.getInstance().getGMAuraSkillTree();
		for (Skill skill : skills)
		{
			gmchar.removeSkill(skill, false); // Don't Save GM skills to database.
		}
		
		SkillTreeData.getInstance().addSkills(gmchar, toAuraSkills);
	}
	
	/**
	 * Displays the buffs of a specific creature to an administrator.<br>
	 * It generates an HTML page showing active or passive effects.<br>
	 * The method handles pagination and logs the action if auditing is enabled.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @param target The {@code Creature} whose buffs are being viewed.
	 * @param page The current page number to display.
	 * @param passive A boolean indicating if only passive effects should be shown.
	 * @param itemsOnly {@code true} to show only the skills that come from an item.
	 */
	private void showBuffs(Player activeChar, Creature target, int page, boolean passive, boolean itemsOnly)
	{
		final List<BuffInfo> effects = new ArrayList<>();
		if (passive)
		{
			effects.addAll(target.getEffectList().getPassives());
		}
		else
		{
			effects.addAll(target.getEffectList().getEffects());
		}
		
		final Map<Skill, Integer> skills = new HashMap<>();
		final Map<Skill, String> itemNames = new HashMap<>();
		for (BuffInfo info : effects)
		{
			final Skill skill = info.getSkill();
			if (skill != null)
			{
				final String itemName = getItemName(target, info);
				if (itemName != null)
				{
					itemNames.put(skill, itemName);
				}
				else if (itemsOnly)
				{
					continue;
				}
				
				final Integer existing = skills.get(skill);
				if (existing != null)
				{
					skills.put(skill, Math.max(info.getAbnormalTime(), existing));
				}
				else
				{
					skills.put(skill, info.getTime());
				}
			}
		}
		
		int row = 0;
		final int objectId = target.getObjectId();
		final String targetName = target.getName();
		final String pageLink = "bypass admin_getbuffs" + (itemsOnly ? "_items " : passive ? "_ps " : " ") + targetName;
		final PageResult result = PageBuilder.newBuilder(skills.keySet(), PAGE_LIMIT, pageLink).currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, skill, sb) ->
		{
			sb.append((row % 2) == 0 ? "<table width=\"295\" bgcolor=\"000000\">" : "<table width=\"295\">");
			sb.append("<tr><td height=40 width=40><img src=\"");
			sb.append(skill.getIcon());
			sb.append("\" width=32 height=32></td><td width=");
			sb.append(skill.isPassive() ? "850" : "650");
			sb.append("><font color=\"B09878\">");
			sb.append(passive ? "<a action=\"bypass admin_viewskilleffects_ps " : "<a action=\"bypass admin_viewskilleffects ");
			sb.append(objectId);
			sb.append(' ');
			sb.append(skill.getId());
			sb.append(' ');
			sb.append(skill.getLevel());
			sb.append("\">");
			final String skillName = skill.getName();
			sb.append(skillName);
			if (!skillName.contains("Lv") && !skillName.contains("Level"))
			{
				sb.append(" Lv. ");
				sb.append(skill.getLevel());
			}
			sb.append(" (");
			sb.append(skill.getId());
			sb.append(")</a></font>");
			final String itemName = itemNames.get(skill);
			if (itemName != null)
			{
				sb.append("<br1><font color=\"808080\">");
				sb.append(itemName);
				sb.append("</font>");
			}
			sb.append("</td><td fixwidth=");
			sb.append(skill.isPassive() ? "1" : "200");
			sb.append(" height=32 valign=\"center\" align=\"center\">");
			sb.append(skill.isPassive() ? "" : skill.isToggle() ? "T" : (skills.get(skill)) + "s");
			sb.append("</td><td><table><tr><td height=5></td></tr><tr><td fixwidth=360><button value=\"X\" action=\"bypass admin_stopbuff ");
			sb.append(objectId);
			sb.append(' ');
			sb.append(skill.getId());
			sb.append("\" width=30 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table></td></tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
		}).build();
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(activeChar, "data/html/admin/getbuff_skills.htm");
		html.replace("%pages%", buildPager(pageLink, page, result.getPages()));
		
		html.replace("%buffsText%", passive ? "Hide Passives" : "Show Passives");
		html.replace("%passives%", passive ? "" : "_ps");
		html.replace("%itemsText%", itemsOnly ? "Hide Item Passives" : "Item Passives");
		html.replace("%items%", itemsOnly ? "" : "_items");
		
		html.replace("%targetName%", targetName);
		html.replace("%targetObjId%", String.valueOf(objectId));
		html.replace("%buffs%", result.getBodyTemplate().toString());
		html.replace("%skillSize%", skills.size());
		activeChar.sendPacket(html);
		
		if (GeneralConfig.GMAUDIT)
		{
			GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", "getbuffs", targetName + " (" + objectId + ")", "");
		}
	}
	
	/**
	 * Displays the effects of a specific skill on a target creature.<br>
	 * This method filters skills by ID and level to show active or passive statuses.<br>
	 * It generates an HTML message for the administrator to view these details.
	 * @param activeChar The {@code Player} who is executing the command.
	 * @param objectId The unique identifier of the target creature.
	 * @param skillId The ID of the skill to filter by.
	 * @param skillLevel The level of the skill to filter by.
	 * @param page The current page number for the results.
	 * @param passive A boolean indicating if only {@code true} (passive) or {@code false} (active) effects should be shown.
	 */
	private void viewSkillEffects(Player activeChar, int objectId, int skillId, int skillLevel, int page, boolean passive)
	{
		Creature target = null;
		try
		{
			target = World.findObject(objectId).asCreature();
		}
		catch (Exception e)
		{
			activeChar.sendSysMessage("Target with object id " + objectId + " not found.");
			return;
		}
		
		final List<EffectRow> effects = new ArrayList<>();
		for (BuffInfo info : passive ? target.getEffectList().getPassives() : target.getEffectList().getEffects())
		{
			final Skill skill = info.getSkill();
			if ((skill != null) && (skill.getId() == skillId) && (skill.getLevel() == skillLevel))
			{
				for (AbstractEffect effect : info.getEffects())
				{
					effects.add(new EffectRow(info, effect));
				}
			}
		}
		
		final Creature effected = target;
		final String pageLink = "bypass admin_viewskilleffects" + (passive ? "_ps " : " ") + objectId + " " + skillId + " " + skillLevel;
		final PageResult result = PageBuilder.newBuilder(effects, EFFECT_PAGE_LIMIT, pageLink).currentPage(page).style(ButtonsStyle.INSTANCE).bodyHandler((pages, effectRow, sb) ->
		{
			final BuffInfo info = effectRow.info();
			final Skill skill = info.getSkill();
			final boolean isInUse = info.isInUse();
			sb.append("<table width=\"295\" bgcolor=\"000000\"><tr><td width=225>");
			sb.append(!isInUse ? FONT_RED1 : "<font color=\"LEVEL\">");
			sb.append(effectRow.effect().getClass().getSimpleName());
			sb.append(!isInUse ? FONT_RED2 : "</font>");
			sb.append("</td><td width=40 align=center>");
			sb.append(skill.isToggle() ? "T" : skill.isPassive() ? "P" : info.getTime() + "s");
			sb.append("</td><td width=30><button value=\"X\" action=\"bypass admin_stopbuff ");
			sb.append(objectId);
			sb.append(' ');
			sb.append(skill.getId());
			sb.append("\" width=30 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr>");
			sb.append("<tr><td width=295 colspan=3><font color=\"B09878\">");
			final String itemName = getItemName(effected, info);
			if (itemName != null)
			{
				sb.append("item=");
				sb.append(itemName);
				sb.append("<br1>");
			}
			sb.append(getEffectValues(effectRow.effect()));
			sb.append("</font></td></tr></table><img src=\"L2UI.SquareGray\" width=295 height=1>");
		}).build();
		
		final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
		html.setFile(activeChar, "data/html/admin/getbuff_effects.htm");
		html.replace("%pages%", buildPager(pageLink, page, result.getPages()));
		
		html.replace("%buffsText%", passive ? "Hide Passives" : "Show Passives");
		html.replace("%passives%", passive ? "" : "_ps");
		
		html.replace("%targetName%", target.getName());
		html.replace("%targetObjId%", target.getObjectId());
		html.replace("%buffs%", result.getBodyTemplate().toString());
		html.replace("%effectSize%", effects.size());
		activeChar.sendPacket(html);
	}
	
	/**
	 * Builds the Prev / Page x / y / Next pager, laid out like the admin collections pager.
	 * @param pageLink The bypass the page number is appended to.
	 * @param page The current page, 0 based.
	 * @param pages The total page count.
	 * @return The pager table, or an empty string when everything fits on one page.
	 */
	private static String buildPager(String pageLink, int page, int pages)
	{
		if (pages <= 1)
		{
			return "";
		}
		
		final int current = Math.min(Math.max(page, 0), pages - 1);
		final StringBuilder sb = new StringBuilder(700);
		sb.append("<table width=280 bgcolor=1a1a1a><tr><td width=70 align=center>");
		if (current > 0)
		{
			sb.append("<button value=\"◄ Prev\" action=\"").append(pageLink).append(' ').append(current - 1).append("\" width=65 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			sb.append("<button value=\"◄ Prev\" action=\"\" width=65 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF_Grayed\">");
		}
		
		sb.append("</td><td width=140 align=center><font color=\"LEVEL\">Page ").append(current + 1).append(" / ").append(pages).append("</font></td><td width=70 align=center>");
		if (current < (pages - 1))
		{
			sb.append("<button value=\"Next ►\" action=\"").append(pageLink).append(' ').append(current + 1).append("\" width=65 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		}
		else
		{
			sb.append("<button value=\"Next ►\" action=\"\" width=65 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF_Grayed\">");
		}
		
		sb.append("</td></tr></table>");
		return sb.toString();
	}
	
	/**
	 * Lists the configured values of an effect, read from its instance fields up to {@code AbstractEffect}.<br>
	 * Effect handlers parse their XML params into fields and keep no copy of the params, so the fields are the only source.
	 * @param effect The effect to describe.
	 * @return The values as {@code name=value} pairs, or "no values" when the effect has none.
	 */
	private static String getEffectValues(AbstractEffect effect)
	{
		final StringBuilder sb = new StringBuilder();
		for (Class<?> type = effect.getClass(); (type != null) && (type != AbstractEffect.class); type = type.getSuperclass())
		{
			for (Field field : type.getDeclaredFields())
			{
				if (Modifier.isStatic(field.getModifiers()))
				{
					continue;
				}
				
				Object value;
				try
				{
					field.setAccessible(true);
					value = field.get(effect);
				}
				catch (Exception e)
				{
					continue;
				}
				
				String text;
				if (value instanceof int[] ints)
				{
					text = Arrays.toString(ints);
				}
				else if (value instanceof Object[] objects)
				{
					text = Arrays.toString(objects);
				}
				else
				{
					text = String.valueOf(value);
				}
				
				if (text.length() > EFFECT_VALUE_MAX_LENGTH)
				{
					text = text.substring(0, EFFECT_VALUE_MAX_LENGTH) + "...";
				}
				
				if (sb.length() > 0)
				{
					sb.append("<br1>");
				}
				
				final String fieldName = field.getName();
				sb.append(fieldName.startsWith("_") ? fieldName.substring(1) : fieldName);
				sb.append('=');
				sb.append(text.replace("<", "&lt;"));
			}
		}
		
		return sb.length() > 0 ? sb.toString() : "no values";
	}
	
	/**
	 * Finds the name of the item a buff comes from.<br>
	 * Used items (potions, scrolls) are kept on the buff itself. Equipped item skills are added without their item, so for players they are matched against the equipped items instead.
	 * @param target The creature carrying the buff.
	 * @param info The buff to look up.
	 * @return The item name, or {@code null} when the buff does not come from an item.
	 */
	private static String getItemName(Creature target, BuffInfo info)
	{
		final Item item = info.getItem();
		if (item != null)
		{
			return item.getName();
		}
		
		if (!target.isPlayer())
		{
			return null;
		}
		
		final int skillId = info.getSkill().getId();
		for (Item equipped : target.asPlayer().getInventory().getPaperdollItems())
		{
			final List<ItemSkillHolder> itemSkills = equipped.getTemplate().getAllSkills();
			if (itemSkills != null)
			{
				for (ItemSkillHolder itemSkill : itemSkills)
				{
					if (itemSkill.getSkillId() == skillId)
					{
						return equipped.getName();
					}
				}
			}
			
			final AugmentationInstance augmentation = equipped.getAugmentation();
			if ((augmentation != null) && (hasOptionSkill(augmentation.getOption1Id(), skillId) || hasOptionSkill(augmentation.getOption2Id(), skillId) || hasOptionSkill(augmentation.getOption3Id(), skillId)))
			{
				return equipped.getName() + " (augment)";
			}
		}
		
		return null;
	}
	
	/**
	 * Checks whether an option grants the given skill, as a passive or as an active skill.
	 * @param optionId The option to check, 0 or less when the slot is empty.
	 * @param skillId The skill ID to look for.
	 * @return {@code true} if the option grants the skill.
	 */
	private static boolean hasOptionSkill(int optionId, int skillId)
	{
		final Options options = optionId > 0 ? OptionData.getInstance().getOptions(optionId) : null;
		if (options == null)
		{
			return false;
		}
		
		return (options.hasPassiveSkills() && containsSkill(options.getPassiveSkills(), skillId)) || (options.hasActiveSkills() && containsSkill(options.getActiveSkills(), skillId));
	}
	
	/**
	 * Checks whether a skill list holds a skill with the given ID.
	 * @param skills The skills to search.
	 * @param skillId The skill ID to look for.
	 * @return {@code true} if a skill with that ID is in the list.
	 */
	private static boolean containsSkill(List<Skill> skills, int skillId)
	{
		for (Skill skill : skills)
		{
			if (skill.getId() == skillId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * One row of the skill effects page: an effect together with the buff that carries it.
	 * @param info The buff carrying the effect.
	 * @param effect The effect shown in the row.
	 */
	private record EffectRow(BuffInfo info, AbstractEffect effect)
	{
	}
	
	/**
	 * Removes a specific skill effect from a target creature.<br>
	 * This method identifies the target by its {@code objId}.<br>
	 * It stops the effect if the target is currently affected by the given {@code skillId}.<br>
	 * A system message is sent to the {@code activeChar} upon success.
	 * @param activeChar The player executing the command.
	 * @param objId The unique identifier of the target object.
	 * @param skillId The ID of the skill effect to remove.
	 */
	private void removeBuff(Player activeChar, int objId, int skillId)
	{
		Creature target = null;
		try
		{
			target = World.findObject(objId).asCreature();
		}
		catch (Exception e)
		{
			// Checked bellow.
		}
		
		if ((target != null) && (skillId > 0))
		{
			if (target.isAffectedBySkill(skillId))
			{
				target.stopSkillEffects(SkillFinishType.REMOVED, skillId);
				activeChar.sendSysMessage("Removed skill ID: " + skillId + " effects from " + target.getName() + " (" + objId + ").");
			}
			
			showBuffs(activeChar, target, 0, false, false);
			if (GeneralConfig.GMAUDIT)
			{
				GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", "stopbuff", target.getName() + " (" + objId + ")", Integer.toString(skillId));
			}
		}
	}
	
	/**
	 * Removes all active effects from a specific creature.<br>
	 * This method identifies the target by its {@code objId}.<br>
	 * It stops all effects and notifies the {@code activeChar}.<br>
	 * If logging is enabled, it records the action in the audit log.
	 * @param activeChar The player who executes the command.
	 * @param objId The unique identifier of the target creature.
	 */
	private void removeAllBuffs(Player activeChar, int objId)
	{
		Creature target = null;
		try
		{
			target = World.findObject(objId).asCreature();
		}
		catch (Exception e)
		{
			// Checked bellow.
		}
		
		if (target != null)
		{
			target.stopAllEffects();
			final String targetName = target.getName();
			activeChar.sendSysMessage("Removed all effects from " + targetName + " (" + objId + ")");
			showBuffs(activeChar, target, 0, false, false);
			if (GeneralConfig.GMAUDIT)
			{
				GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", "stopallbuffs", targetName + " (" + objId + ")", "");
			}
		}
	}
	
	/**
	 * Displays the blocked abnormal effects of a specific creature.<br>
	 * This method finds the target by its {@code objId}.<br>
	 * It sends an HTML message to the {@code activeChar} showing all blocked slots.<br>
	 * If the object is not found, it sends a system message to the player.
	 * @param activeChar The player who is viewing the effects.
	 * @param objId The unique identifier of the target creature.
	 */
	private void viewBlockedEffects(Player activeChar, int objId)
	{
		Creature target = null;
		try
		{
			target = World.findObject(objId).asCreature();
		}
		catch (Exception e)
		{
			activeChar.sendSysMessage("Target with object id " + objId + " not found.");
			return;
		}
		
		if (target != null)
		{
			final Set<AbnormalType> blockedAbnormals = target.getEffectList().getBlockedAbnormalTypes();
			final int blockedAbnormalSize = blockedAbnormals != null ? blockedAbnormals.size() : 0;
			final StringBuilder html = new StringBuilder(500 + (blockedAbnormalSize * 50));
			html.append("<html><table width=\"100%\"><tr><td width=45><button value=\"Main\" action=\"bypass admin_admin\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=180><center><font color=\"LEVEL\">Blocked effects of ");
			final String targetName = target.getName();
			html.append(targetName);
			html.append("</font></td><td width=45><button value=\"Back\" action=\"bypass admin_getbuffs" + (target.isPlayer() ? (" " + targetName) : "") + "\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><br>");
			if ((blockedAbnormals != null) && !blockedAbnormals.isEmpty())
			{
				html.append("<br>Blocked buff slots: ");
				for (AbnormalType slot : blockedAbnormals)
				{
					html.append("<br>").append(slot.toString());
				}
			}
			
			html.append("</html>");
			
			// Send the packet
			activeChar.sendPacket(new NpcHtmlMessage(0, 1, html.toString()));
			if (GeneralConfig.GMAUDIT)
			{
				GMAudit.logAction(activeChar.getName() + " [" + activeChar.getObjectId() + "]", "viewblockedeffects", targetName + " (" + Integer.toString(target.getObjectId()) + ")", "");
			}
		}
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all registered command strings.
	 * @return an array of {@code String} containing all commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
