/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.SellBuffsConfig;
import org.l2jmobius.gameserver.data.holders.SellBuffHolder;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.holders.TradeList;
import org.l2jmobius.gameserver.network.serverpackets.ExPrivateStoreSetWholeMsg;
import org.l2jmobius.gameserver.util.FormatUtil;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Manages the logic for selling buffs within the game world.<br>
 * It handles data loading and processing for buff sales based on {@code SellBuffsConfig}.<br>
 * This class ensures that players can interact with sellable buffs correctly.
 * @author St3eT
 */
public class SellBuffsManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SellBuffsManager.class.getName());
	
	private static final Set<Integer> ALLOWED_BUFFS = new HashSet<>();
	private static final String HTML_FOLDER = "data/html/mods/SellBuffs/";
	
	/**
	 * Initializes a new instance of the {@code SellBuffsManager}.<br>
	 * This constructor calls the {@code load()} method to prepare the manager.
	 */
	protected SellBuffsManager()
	{
		load();
	}
	
	/**
	 * Loads the sell buffs configuration from the data files.<br>
	 * This method checks if {@code SellBuffsConfig.SELLBUFF_ENABLED} is true.<br>
	 * It clears the existing list and parses the allowed buffs from {@code data/SellBuffData.xml}.
	 */
	@Override
	public void load()
	{
		if (SellBuffsConfig.SELLBUFF_ENABLED)
		{
			ALLOWED_BUFFS.clear();
			parseDatapackFile("data/SellBuffData.xml");
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + ALLOWED_BUFFS.size() + " allowed buffs.");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to identify allowed skills.<br>
	 * It extracts IDs from all {@code skill} tags and adds them to the internal set.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		final NodeList node = document.getDocumentElement().getElementsByTagName("skill");
		for (int i = 0; i < node.getLength(); ++i)
		{
			final Element elem = (Element) node.item(i);
			final int skillId = Integer.parseInt(elem.getAttribute("id"));
			ALLOWED_BUFFS.add(skillId);
		}
	}
	
	/**
	 * Opens the sell buffs menu for a specific player.<br>
	 * It checks if the player is currently selling buffs to determine which HTML file to display.<br>
	 * The resulting HTML content is then sent to the player via the {@code CommunityBoardHandler}.
	 * @param player The {@code Player} who will receive the sell menu.
	 */
	public void sendSellMenu(Player player)
	{
		final String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + (player.isSellingBuffs() ? "BuffMenu_already.html" : "BuffMenu.html"));
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Sends the buff choice menu to a specific player.<br>
	 * This method retrieves the HTML template and replaces the list placeholder with skill data.
	 * @param player The {@code Player} who will receive the menu.
	 * @param index The {@code int} index used to determine which skills to display.
	 */
	public void sendBuffChoiceMenu(Player player, int index)
	{
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "BuffChoice.html");
		html = html.replace("%list%", buildSkillMenu(player, index));
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Opens the buff editing menu for a specific player.<br>
	 * This method retrieves the base HTML and replaces the list placeholder with the generated edit menu.<br>
	 * It then sends the final result to the {@code Player}.
	 * @param player The {@code Player} who will receive the menu.
	 */
	public void sendBuffEditMenu(Player player)
	{
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "BuffChoice.html");
		html = html.replace("%list%", buildEditMenu(player));
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Sends the buff purchase menu to a specific player.<br>
	 * This method displays the list of buffs available from a seller.<br>
	 * It replaces the placeholder in the HTML template with the generated menu content.
	 * @param player The {@code Player} who will receive the menu.
	 * @param seller The {@code Player} who is currently selling the buffs.
	 * @param index The specific {@code int} index of the buff to display.
	 */
	public void sendBuffMenu(Player player, Player seller, int index)
	{
		if (!seller.isSellingBuffs() || seller.getSellingBuffs().isEmpty())
		{
			return;
		}
		
		String html = HtmCache.getInstance().getHtm(player, HTML_FOLDER + "BuffBuyMenu.html");
		html = html.replace("%list%", buildBuffMenu(seller, index));
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Starts the sell buffs process for a specific player.<br>
	 * This method puts the {@code Player} into a sitting position and enables their selling status.<br>
	 * It sets the private store type to {@code PrivateStoreType.PACKAGE_SELL} and updates the list title.<br>
	 * Finally, it broadcasts the updated information and opens the sell menu for the player.
	 * @param player The {@code Player} who will start selling buffs.
	 * @param title The display name for the sell list.
	 */
	public void startSellBuffs(Player player, String title)
	{
		player.sitDown();
		player.setSellingBuffs(true);
		player.setPrivateStoreType(PrivateStoreType.PACKAGE_SELL);
		final TradeList sellList = player.getSellList();
		sellList.setTitle(title);
		sellList.setPackaged(true);
		player.broadcastUserInfo();
		player.broadcastPacket(new ExPrivateStoreSetWholeMsg(player));
		sendSellMenu(player);
	}
	
	/**
	 * Stops the active sell buffs process for a specific player.<br>
	 * This method resets the player's selling status and private store type.<br>
	 * It also forces the player to stand up and updates their information.<br>
	 * Finally, it sends the default sell menu to the player.
	 * @param player The {@code Player} whose sell buffs are being stopped.
	 */
	public void stopSellBuffs(Player player)
	{
		player.setSellingBuffs(false);
		player.setPrivateStoreType(PrivateStoreType.NONE);
		player.standUp();
		player.broadcastUserInfo();
		sendSellMenu(player);
	}
	
	/**
	 * Constructs the HTML string for the buff selling menu.<br>
	 * It generates a paginated table of buffs available from a specific seller.<br>
	 * The method includes MP gauges, skill icons, costs, and navigation buttons.
	 * @param seller The {@code Player} who is currently selling the buffs.
	 * @param index The current page index for pagination.
	 * @return A {@code String} containing the formatted HTML menu.
	 */
	private String buildBuffMenu(Player seller, int index)
	{
		final int ceiling = 10;
		int nextIndex = -1;
		int previousIndex = -1;
		int emptyFields = 0;
		final StringBuilder sb = new StringBuilder();
		final List<SellBuffHolder> sellList = new ArrayList<>();
		
		int count = 0;
		for (SellBuffHolder holder : seller.getSellingBuffs())
		{
			count++;
			if ((count > index) && (count <= (ceiling + index)))
			{
				sellList.add(holder);
			}
		}
		
		if ((count > 10) && (count > (index + 10)))
		{
			nextIndex = index + 10;
		}
		
		if (index >= 10)
		{
			previousIndex = index - 10;
		}
		
		emptyFields = ceiling - sellList.size();
		
		sb.append("<br>");
		sb.append(HtmlUtil.getMpGauge(250, (long) seller.getCurrentMp(), seller.getMaxMp(), false));
		sb.append("<br>");
		
		sb.append("<table border=0 cellpadding=0 cellspacing=0 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
		sb.append("<tr><td><br><br><br></td></tr>");
		sb.append("<tr>");
		sb.append("<td fixwidth=\"10\"></td>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=75 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Icon
		sb.append("<td> <button action=\"\" value=\"Name\" width=175 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Name
		sb.append("<td> <button action=\"\" value=\"Level\" width=85 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Leve
		sb.append("<td> <button action=\"\" value=\"MP Cost\" width=100 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Price
		sb.append("<td> <button action=\"\" value=\"Price\" width=200 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Price
		sb.append("<td> <button action=\"\" value=\"Action\" width=100 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Action
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("</tr>");
		
		final int sellerObjectId = seller.getObjectId();
		for (SellBuffHolder holder : sellList)
		{
			final Skill skill = seller.getKnownSkill(holder.getSkillId());
			if (skill == null)
			{
				emptyFields++;
				continue;
			}
			
			final ItemTemplate item = ItemData.getInstance().getTemplate(SellBuffsConfig.SELLBUFF_PAYMENT_ID);
			final int skillId = skill.getId();
			final int skillLevel = skill.getLevel();
			
			sb.append("<tr>");
			sb.append("<td fixwidth=\"20\"></td>");
			sb.append("<td align=center><img src=\"" + skill.getIcon() + "\" width=\"32\" height=\"32\"></td>");
			sb.append("<td align=left>" + skill.getName() + (skillLevel > 100 ? "<font color=\"LEVEL\"> + " + (skillLevel % 100) + "</font></td>" : "</td>"));
			sb.append("<td align=center>" + ((skillLevel > 100) ? SkillData.getInstance().getMaxLevel(skillId) : skillLevel) + "</td>");
			sb.append("<td align=center> <font color=\"1E90FF\">" + (skill.getMpConsume() * SellBuffsConfig.SELLBUFF_MP_MULTIPLER) + "</font></td>");
			sb.append("<td align=center> " + FormatUtil.formatAdena(holder.getPrice()) + " <font color=\"LEVEL\"> " + (item != null ? item.getName() : "") + "</font> </td>");
			sb.append("<td align=center fixwidth=\"50\"><button value=\"Buy Buff\" action=\"bypass -h sellbuffbuyskill " + sellerObjectId + " " + skillId + " " + index + "\" width=\"85\" height=\"26\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
			sb.append("</tr>");
			sb.append("<tr><td><br><br></td></tr>");
		}
		
		for (int i = 0; i < emptyFields; i++)
		{
			sb.append("<tr>");
			sb.append("<td fixwidth=\"20\" height=\"32\"></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=left></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=center></font></td>");
			sb.append("<td align=center></td>");
			sb.append("<td align=center fixwidth=\"50\"></td>");
			sb.append("</tr>");
			sb.append("<tr><td><br><br></td></tr>");
		}
		
		sb.append("</table>");
		
		sb.append("<table width=\"250\" border=\"0\">");
		sb.append("<tr>");
		
		if (previousIndex > -1)
		{
			sb.append("<td align=left><button value=\"Previous Page\" action=\"bypass -h sellbuffbuymenu " + sellerObjectId + " " + previousIndex + "\" width=\"100\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		}
		
		if (nextIndex > -1)
		{
			sb.append("<td align=right><button value=\"Next Page\" action=\"bypass -h sellbuffbuymenu " + sellerObjectId + " " + nextIndex + "\" width=\"100\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		}
		sb.append("</tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Constructs the HTML string for the buff editing menu.<br>
	 * It displays a table of buffs currently being sold by the player.<br>
	 * If no buffs are found, it shows an empty message.<br>
	 * Otherwise, it lists each skill with its icon, name, level, and price.
	 * @param player The {@code Player} who owns the selling buffs.
	 * @return A {@code String} containing the generated HTML content.
	 */
	private String buildEditMenu(Player player)
	{
		final StringBuilder sb = new StringBuilder();
		
		sb.append("<table border=0 cellpadding=0 cellspacing=0 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
		sb.append("<tr><td><br><br><br></td></tr>");
		sb.append("<tr>");
		sb.append("<td fixwidth=\"10\"></td>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=75 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Icon
		sb.append("<td> <button action=\"\" value=\"Name\" width=150 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Name
		sb.append("<td> <button action=\"\" value=\"Level\" width=75 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Level
		sb.append("<td> <button action=\"\" value=\"Old Price\" width=100 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Old price
		sb.append("<td> <button action=\"\" value=\"New Price\" width=125 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // New price
		sb.append("<td> <button action=\"\" value=\"Action\" width=125 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Change Price
		sb.append("<td> <button action=\"\" value=\"Remove\" width=85 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Remove Buff
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("</tr>");
		
		final List<SellBuffHolder> sellingBuffs = player.getSellingBuffs();
		if (sellingBuffs.isEmpty())
		{
			sb.append("</table>");
			sb.append("<br><br><br>");
			sb.append("You don't have added any buffs yet!");
		}
		else
		{
			for (SellBuffHolder holder : sellingBuffs)
			{
				final Skill skill = player.getKnownSkill(holder.getSkillId());
				if (skill == null)
				{
					continue;
				}
				
				final int skillId = skill.getId();
				final int skillLevel = skill.getLevel();
				
				sb.append("<tr>");
				sb.append("<td fixwidth=\"20\"></td>");
				sb.append("<td align=center><img src=\"" + skill.getIcon() + "\" width=\"32\" height=\"32\"></td>"); // Icon
				sb.append("<td align=left>" + skill.getName() + (skillLevel > 100 ? "<font color=\"LEVEL\"> + " + (skillLevel % 100) + "</font></td>" : "</td>")); // Name + enchant
				sb.append("<td align=center>" + ((skillLevel > 100) ? SkillData.getInstance().getMaxLevel(skillId) : skillLevel) + "</td>"); // Level
				sb.append("<td align=center> " + FormatUtil.formatAdena(holder.getPrice()) + " </td>"); // Price show
				sb.append("<td align=center><edit var=\"price_" + skillId + "\" width=120 type=\"number\"></td>"); // Price edit
				sb.append("<td align=center><button value=\"Edit\" action=\"bypass -h sellbuffchangeprice " + skillId + " $price_" + skillId + "\" width=\"85\" height=\"26\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				sb.append("<td align=center><button value=\" X \" action=\"bypass -h sellbuffremove " + skillId + "\" width=\"26\" height=\"26\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				sb.append("</tr>");
				sb.append("<tr><td><br><br></td></tr>");
			}
			sb.append("</table>");
		}
		
		return sb.toString();
	}
	
	/**
	 * Constructs the HTML string for the skill selection menu.<br>
	 * It filters skills based on allowed buffs and current list status.<br>
	 * The method handles pagination by showing 10 items per page.
	 * @param player The {@code Player} whose skills are being displayed.
	 * @param index The current page index for the skill list.
	 * @return A string containing the formatted HTML for the menu.
	 */
	private String buildSkillMenu(Player player, int index)
	{
		final int ceiling = index + 10;
		int nextIndex = -1;
		int previousIndex = -1;
		final StringBuilder sb = new StringBuilder();
		final List<Skill> skillList = new ArrayList<>();
		
		int count = 0;
		for (Skill skill : player.getAllSkills())
		{
			if (ALLOWED_BUFFS.contains(skill.getId()) && !isInSellList(player, skill))
			{
				count++;
				
				if ((count > index) && (count <= ceiling))
				{
					skillList.add(skill);
				}
			}
		}
		
		final int indexPlusTen = index + 10;
		if ((count > 10) && (count > indexPlusTen))
		{
			nextIndex = indexPlusTen;
		}
		
		if (index >= 10)
		{
			previousIndex = index - 10;
		}
		
		sb.append("<table border=0 cellpadding=0 cellspacing=0 background=\"L2UI_CH3.refinewnd_back_Pattern\">");
		sb.append("<tr><td><br><br><br></td></tr>");
		sb.append("<tr>");
		sb.append("<td fixwidth=\"10\"></td>");
		sb.append("<td> <button action=\"\" value=\"Icon\" width=100 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Icon
		sb.append("<td> <button action=\"\" value=\"Name\" width=175 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Name
		sb.append("<td> <button action=\"\" value=\"Level\" width=150 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Leve
		sb.append("<td> <button action=\"\" value=\"Price\" width=150 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Price
		sb.append("<td> <button action=\"\" value=\"Action\" width=125 height=23 back=\"L2UI_CT1.OlympiadWnd_DF_Watch_Down\" fore=\"L2UI_CT1.OlympiadWnd_DF_Watch\"> </td>"); // Action
		sb.append("<td fixwidth=\"20\"></td>");
		sb.append("</tr>");
		
		if (skillList.isEmpty())
		{
			sb.append("</table>");
			sb.append("<br><br><br>");
			sb.append("At this moment you cannot add any buffs!");
		}
		else
		{
			for (Skill skill : skillList)
			{
				final int skillId = skill.getId();
				final int skillLevel = skill.getLevel();
				sb.append("<tr>");
				sb.append("<td fixwidth=\"20\"></td>");
				sb.append("<td align=center><img src=\"" + skill.getIcon() + "\" width=\"32\" height=\"32\"></td>");
				sb.append("<td align=left>" + skill.getName() + (skillLevel > 100 ? "<font color=\"LEVEL\"> + " + (skillLevel % 100) + "</font></td>" : "</td>"));
				sb.append("<td align=center>" + ((skillLevel > 100) ? SkillData.getInstance().getMaxLevel(skillId) : skillLevel) + "</td>");
				sb.append("<td align=center><edit var=\"price_" + skillId + "\" width=120 type=\"number\"></td>");
				sb.append("<td align=center fixwidth=\"50\"><button value=\"Add Buff\" action=\"bypass -h sellbuffaddskill " + skillId + " $price_" + skillId + "\" width=\"85\" height=\"26\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
				sb.append("</tr>");
				sb.append("<tr><td><br><br></td></tr>");
			}
			sb.append("</table>");
		}
		
		sb.append("<table width=\"250\" border=\"0\">");
		sb.append("<tr>");
		
		if (previousIndex > -1)
		{
			sb.append("<td align=left><button value=\"Previous Page\" action=\"bypass -h sellbuffadd " + previousIndex + "\" width=\"100\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		}
		
		if (nextIndex > -1)
		{
			sb.append("<td align=right><button value=\"Next Page\" action=\"bypass -h sellbuffadd " + nextIndex + "\" width=\"100\" height=\"30\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td>");
		}
		sb.append("</tr>");
		sb.append("</table>");
		return sb.toString();
	}
	
	/**
	 * Checks if a specific {@code Skill} is currently in the player's sell list.<br>
	 * It iterates through all active selling buffs for the given {@code Player}.
	 * @param player The {@code Player} who owns the sell list.
	 * @param skill The {@code Skill} to check against the list.
	 * @return {@code true} if the skill is found in the list, otherwise {@code false}.
	 */
	public boolean isInSellList(Player player, Skill skill)
	{
		for (SellBuffHolder holder : player.getSellingBuffs())
		{
			if (holder.getSkillId() == skill.getId())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a {@code Player} is allowed to start selling buffs.<br>
	 * This method validates the player's current state and location.<br>
	 * It returns {@code false} if any restrictions are met.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player can sell buffs, otherwise {@code false}.
	 */
	public boolean canStartSellBuffs(Player player)
	{
		if (player.isAlikeDead())
		{
			player.sendMessage("You can't sell buffs in fake death!");
			return false;
		}
		else if (player.isInOlympiadMode() || OlympiadManager.getInstance().isRegistered(player))
		{
			player.sendMessage("You can't sell buffs with Olympiad status!");
			return false;
		}
		else if (player.isRegisteredOnEvent())
		{
			player.sendMessage("You can't sell buffs while registered in an event!");
			return false;
		}
		else if (player.isCursedWeaponEquipped() || (player.getReputation() < 0))
		{
			player.sendMessage("You can't sell buffs in Chaotic state!");
			return false;
		}
		else if (player.isInDuel())
		{
			player.sendMessage("You can't sell buffs in Duel state!");
			return false;
		}
		else if (player.isFishing())
		{
			player.sendMessage("You can't sell buffs while fishing.");
			return false;
		}
		else if (player.isMounted() || player.isFlyingMounted() || player.isFlying())
		{
			player.sendMessage("You can't sell buffs in Mount state!");
			return false;
		}
		else if (player.isTransformed())
		{
			player.sendMessage("You can't sell buffs in Transform state!");
			return false;
		}
		else if (player.isInsideZone(ZoneId.NO_STORE) || !player.isInsideZone(ZoneId.PEACE) || player.isJailed())
		{
			player.sendMessage("You can't sell buffs here!");
			return false;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the singleton instance of {@code SellBuffsManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The active {@code SellBuffsManager} instance.
	 */
	public static SellBuffsManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SellBuffsManager INSTANCE = new SellBuffsManager();
	}
}
