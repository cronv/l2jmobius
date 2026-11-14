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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.entity.item.type.ItemType;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Shows the targeted player's inventory in a paged HTML window and edits it from there.<br>
 * The window lists one category at a time, deletes a whole stack with its x button and changes a stackable item's count with the Add, + and - buttons.<br>
 * Every button sends one of the commands in {@code ADMIN_COMMANDS} back to this handler, carrying the object id, the page and the category, so the window can be redrawn where the admin left it.
 * @author CostyKiller
 */
public class AdminInventory implements IAdminCommandHandler
{
	private static final String COMMAND_SHOW_INV = "admin_show_inv";
	private static final String COMMAND_INVENTORY = "admin_inventory";
	private static final String COMMAND_DELETE_ITEM = "admin_delete_item";
	private static final String COMMAND_ADD_COUNT = "admin_add_item_count";
	private static final String COMMAND_INC_COUNT = "admin_item_count_inc";
	private static final String COMMAND_DEC_COUNT = "admin_item_count_dec";
	
	// Row budgets per page. A stackable row is taller than a non stackable one, it carries a controls line under the name line, so each kind gets its own budget and the window stays under the height where the client adds a scrollbar.
	private static final int MAX_STACKABLE_PER_PAGE = 3;
	private static final int MAX_NON_STACKABLE_PER_PAGE = 5;
	
	// "All Items" is the one category where both kinds of row share a page, so it gets its own smaller budgets.
	private static final int MAX_STACKABLE_PER_PAGE_ALL = 2;
	private static final int MAX_NON_STACKABLE_PER_PAGE_ALL = 3;
	
	// Category codes used in the bypass params, and the labels shown for them. Both arrays are read with the same category index, so they must keep the same length and order.
	private static final String[] CATEGORY_CODES =
	{
		"ALL",
		"EQUIP",
		"SUPPLY",
		"MATERIAL",
		"MISC",
		"QUEST",
		"ARTIFACT"
	};
	private static final String[] CATEGORY_LABELS =
	{
		"All Items",
		"Equipment",
		"Supplies",
		"Materials",
		"Misc",
		"Quest",
		"Artifacts"
	};
	
	private static final String[] ADMIN_COMMANDS =
	{
		COMMAND_SHOW_INV,
		COMMAND_INVENTORY,
		COMMAND_DELETE_ITEM,
		COMMAND_ADD_COUNT,
		COMMAND_INC_COUNT,
		COMMAND_DEC_COUNT
	};
	
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final WorldObject target = activeChar.getTarget();
		
		if (target == null)
		{
			activeChar.sendMessage("Select a target");
			return false;
		}
		
		if (!target.isPlayer())
		{
			activeChar.sendMessage("Target need to be player");
			return false;
		}
		
		final Player player = target.asPlayer();
		
		// Both names open the same page, only the buttons drawn on it keep using COMMAND_SHOW_INV.
		final boolean showInventory = command.startsWith(COMMAND_SHOW_INV);
		if (showInventory || command.startsWith(COMMAND_INVENTORY))
		{
			final String[] args = parseArgs(command, showInventory ? COMMAND_SHOW_INV : COMMAND_INVENTORY);
			final int page = (args.length > 0) ? StringUtil.parseInt(args[0], 0) : 0;
			final String category = (args.length > 1) ? args[1] : "ALL";
			showItemsPage(activeChar, player, page, category);
		}
		else if (command.startsWith(COMMAND_DELETE_ITEM))
		{
			final String[] args = parseArgs(command, COMMAND_DELETE_ITEM);
			final int objectId = (args.length > 0) ? StringUtil.parseInt(args[0], -1) : -1;
			final int page = (args.length > 1) ? StringUtil.parseInt(args[1], 0) : 0;
			final String category = (args.length > 2) ? args[2] : "ALL";
			
			// Object id -1 finds nothing, which is the wanted answer for a command typed without arguments.
			final Item item = player.getInventory().getItemByObjectId(objectId);
			if (item == null)
			{
				activeChar.sendMessage("Item not found, refreshing list.");
			}
			else
			{
				player.destroyItem(ItemProcessType.DESTROY, objectId, item.getCount(), null, true);
			}
			
			showItemsPage(activeChar, player, page, category);
		}
		else if (command.startsWith(COMMAND_ADD_COUNT))
		{
			final String[] args = parseArgs(command, COMMAND_ADD_COUNT);
			
			// An empty quantity box reaches us as a missing argument instead of an empty one, so the page and the category are read from the tail of the list, where a dropped quantity cannot shift them onto the wrong parameter.
			final int page = (args.length > 2) ? StringUtil.parseInt(args[args.length - 2], 0) : 0;
			final String category = (args.length > 2) ? args[args.length - 1] : "ALL";
			
			// Long.MIN_VALUE marks "no number given", a quantity nobody can type, so a typed 0 still reaches addItemCount and gets its own answer.
			final long amount = (args.length < 4) ? Long.MIN_VALUE : StringUtil.parseLong(args[1], Long.MIN_VALUE);
			
			if (amount == Long.MIN_VALUE)
			{
				activeChar.sendMessage("Invalid quantity.");
			}
			else
			{
				addItemCount(activeChar, player, StringUtil.parseInt(args[0], -1), amount);
			}
			
			showItemsPage(activeChar, player, page, category);
		}
		else if (command.startsWith(COMMAND_INC_COUNT))
		{
			final String[] args = parseArgs(command, COMMAND_INC_COUNT);
			final int objectId = (args.length > 0) ? StringUtil.parseInt(args[0], -1) : -1;
			final int page = (args.length > 1) ? StringUtil.parseInt(args[1], 0) : 0;
			final String category = (args.length > 2) ? args[2] : "ALL";
			
			// The + and - buttons move a stack by one.
			adjustItemCount(activeChar, player, objectId, 1);
			showItemsPage(activeChar, player, page, category);
		}
		else if (command.startsWith(COMMAND_DEC_COUNT))
		{
			final String[] args = parseArgs(command, COMMAND_DEC_COUNT);
			final int objectId = (args.length > 0) ? StringUtil.parseInt(args[0], -1) : -1;
			final int page = (args.length > 1) ? StringUtil.parseInt(args[1], 0) : 0;
			final String category = (args.length > 2) ? args[2] : "ALL";
			
			adjustItemCount(activeChar, player, objectId, -1);
			showItemsPage(activeChar, player, page, category);
		}
		
		return true;
	}
	
	/**
	 * Splits the arguments that follow a bypass command name.
	 * @param command The whole bypass line, command name included.
	 * @param prefix The command name to strip.
	 * @return The whitespace separated arguments, an empty array when the line carries none.
	 */
	private static String[] parseArgs(String command, String prefix)
	{
		final int prefixLength = prefix.length();
		if (command.length() <= prefixLength)
		{
			return new String[0];
		}
		
		final String rest = command.substring(prefixLength + 1).trim();
		if (rest.isEmpty())
		{
			return new String[0];
		}
		
		return rest.split("\\s+");
	}
	
	/**
	 * Adds the typed amount to a stackable item's current count.
	 * @param activeChar The admin running the command, also the reference logged for the addition.
	 * @param target The player whose inventory is edited.
	 * @param objectId The object id of the item to grow.
	 * @param amount How many units to add, zero and below are rejected.
	 */
	private void addItemCount(Player activeChar, Player target, int objectId, long amount)
	{
		final Item item = target.getInventory().getItemByObjectId(objectId);
		if (item == null)
		{
			activeChar.sendMessage("Item not found.");
			return;
		}
		
		if (!item.isStackable())
		{
			activeChar.sendMessage("Item is not stackable.");
			return;
		}
		
		if (amount <= 0)
		{
			activeChar.sendMessage("Enter a positive amount to add.");
			return;
		}
		
		target.addItem(ItemProcessType.REWARD, item.getId(), amount, activeChar, true);
	}
	
	/**
	 * Moves a stackable item's count by a relative delta, this is what the + and - buttons call.<br>
	 * A delta that would leave nothing destroys the whole stack instead of leaving a count of zero behind.
	 * @param activeChar The admin running the command, also the reference logged for the addition.
	 * @param target The player whose inventory is edited.
	 * @param objectId The object id of the item to adjust.
	 * @param delta How many units to add, negative to remove.
	 */
	private void adjustItemCount(Player activeChar, Player target, int objectId, long delta)
	{
		final Item item = target.getInventory().getItemByObjectId(objectId);
		if (item == null)
		{
			activeChar.sendMessage("Item not found.");
			return;
		}
		
		if (!item.isStackable())
		{
			activeChar.sendMessage("Item is not stackable.");
			return;
		}
		
		final long count = item.getCount();
		if ((count + delta) <= 0)
		{
			target.destroyItem(ItemProcessType.DESTROY, objectId, count, null, true);
			return;
		}
		
		if (delta > 0)
		{
			target.addItem(ItemProcessType.REWARD, item.getId(), delta, activeChar, true);
		}
		else
		{
			target.destroyItem(ItemProcessType.DESTROY, objectId, -delta, null, true);
		}
	}
	
	/**
	 * Groups items into pages, closing a page as soon as either budget for that page is spent, whichever comes first.<br>
	 * A page can therefore mix both kinds of row while respecting both budgets.
	 * @param filtered The items to lay out, already reduced to one category.
	 * @param stackableCap How many stackable rows fit on a page.
	 * @param nonStackableCap How many non stackable rows fit on a page.
	 * @return One list per page, always at least one page, an empty one when the category holds no item.
	 */
	private static List<List<Item>> paginate(List<Item> filtered, int stackableCap, int nonStackableCap)
	{
		final List<List<Item>> pages = new ArrayList<>();
		List<Item> currentPage = new ArrayList<>();
		int stackableCount = 0;
		int nonStackableCount = 0;
		
		for (Item item : filtered)
		{
			final boolean stackable = item.isStackable();
			if (!currentPage.isEmpty() && (stackable ? (stackableCount >= stackableCap) : (nonStackableCount >= nonStackableCap)))
			{
				pages.add(currentPage);
				currentPage = new ArrayList<>();
				stackableCount = 0;
				nonStackableCount = 0;
			}
			
			currentPage.add(item);
			if (stackable)
			{
				stackableCount++;
			}
			else
			{
				nonStackableCount++;
			}
		}
		
		if (!currentPage.isEmpty() || pages.isEmpty())
		{
			pages.add(currentPage);
		}
		
		return pages;
	}
	
	/**
	 * Draws one page of one category and sends it to the admin.
	 * @param activeChar The admin the window is sent to.
	 * @param player The player whose inventory is listed.
	 * @param page The wanted page, clamped into the pages the category actually has.
	 * @param category A category code, an unknown one falls back to "ALL".
	 */
	private void showItemsPage(Player activeChar, Player player, int page, String category)
	{
		// An unknown code lands on index 0, which is the "ALL" category.
		final int categoryIndex = indexOfCategory(category);
		final String categoryCode = CATEGORY_CODES[categoryIndex];
		
		final List<Item> filtered = new ArrayList<>();
		for (Item item : player.getInventory().getItems())
		{
			if (matchesCategory(item, categoryCode))
			{
				filtered.add(item);
			}
		}
		
		// The inventory is a hash set, so its order is not stable, without a sort the same page would list different items on every refresh.
		filtered.sort((Item item1, Item item2) -> Integer.compare(item1.getObjectId(), item2.getObjectId()));
		
		final boolean allCategory = categoryIndex == 0;
		final List<List<Item>> pages = paginate(filtered, allCategory ? MAX_STACKABLE_PER_PAGE_ALL : MAX_STACKABLE_PER_PAGE, allCategory ? MAX_NON_STACKABLE_PER_PAGE_ALL : MAX_NON_STACKABLE_PER_PAGE);
		final int maxPages = pages.size();
		
		// Deleting the last item of a page leaves the admin on a page that no longer exists.
		if (page >= maxPages)
		{
			page = maxPages - 1;
		}
		
		if (page < 0)
		{
			page = 0;
		}
		
		final NpcHtmlMessage adminReply = new NpcHtmlMessage(0);
		adminReply.setFile(activeChar, "data/html/admin/inventory.htm");
		adminReply.replace("%PLAYER_NAME%", player.getName());
		adminReply.replace("%CAT_HEADER%", getCategoryHeaderHtml(categoryIndex));
		adminReply.replace("%ITEMS%", getItemsHtml(pages.get(page), page, categoryCode));
		
		adminReply.replace("%PREV_BUTTON%", (page > 0) ? "<button value=\"< Prev\" action=\"bypass -h " + COMMAND_SHOW_INV + " " + (page - 1) + " " + categoryCode + "\" width=70 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "");
		adminReply.replace("%NEXT_BUTTON%", (page < (maxPages - 1)) ? "<button value=\"Next >\" action=\"bypass -h " + COMMAND_SHOW_INV + " " + (page + 1) + " " + categoryCode + "\" width=70 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" : "");
		adminReply.replace("%CURRENT_PAGE%", String.valueOf(page + 1));
		adminReply.replace("%MAX_PAGES%", String.valueOf(maxPages));
		
		activeChar.sendPacket(adminReply);
	}
	
	/**
	 * Builds the header row that names the shown category and steps to the one before or after it.<br>
	 * Both steps wrap around the ends of {@code CATEGORY_CODES} and open the new category on its first page.
	 * @param categoryIndex The index of the shown category.
	 * @return The header table.
	 */
	private static String getCategoryHeaderHtml(int categoryIndex)
	{
		return "<table width=280><tr>" + "<td width=90 align=center>" + "<button value=\"< Cat\" action=\"bypass -h " + COMMAND_SHOW_INV + " 0 " + CATEGORY_CODES[((categoryIndex - 1) + CATEGORY_CODES.length) % CATEGORY_CODES.length] + "\" width=70 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" + "</td>" + "<td width=100 align=center><font color=\"LEVEL\">" + CATEGORY_LABELS[categoryIndex] + "</font></td>" + "<td width=90 align=center>" + "<button value=\"Cat >\" action=\"bypass -h " + COMMAND_SHOW_INV + " 0 " + CATEGORY_CODES[(categoryIndex + 1) % CATEGORY_CODES.length] + "\" width=70 height=21 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">" + "</td>" + "</tr></table>";
	}
	
	/**
	 * Resolves a category code coming from a bypass into its index.
	 * @param category The code to look up.
	 * @return The index in {@code CATEGORY_CODES}, 0 for anything unknown, which is the "ALL" category.
	 */
	private static int indexOfCategory(String category)
	{
		for (int i = 0; i < CATEGORY_CODES.length; i++)
		{
			if (CATEGORY_CODES[i].equals(category))
			{
				return i;
			}
		}
		
		return 0;
	}
	
	/**
	 * Tells whether an item belongs in a category.<br>
	 * Equipable, quest and artifact items are recognized by their own flags, in that order: an equipable artifact is listed under artifacts, and an artifact that is also a quest item is listed under quests.<br>
	 * Everything left is an etc item and is placed by its {@link org.l2jmobius.gameserver.entity.item.type.EtcItemType}, the types not named below land in "MISC".
	 * @param item The item to place.
	 * @param category The category code being drawn.
	 * @return {@code true} when the item belongs in that category.
	 */
	private static boolean matchesCategory(Item item, String category)
	{
		if ("ALL".equals(category))
		{
			return true;
		}
		
		final boolean artifact = item.isArtifactItem();
		if (item.isEquipable() && !artifact)
		{
			return "EQUIP".equals(category);
		}
		
		if (item.isQuestItem())
		{
			return "QUEST".equals(category);
		}
		
		if (artifact)
		{
			return "ARTIFACT".equals(category);
		}
		
		final ItemType itemType = item.getTemplate().getItemType();
		if (itemType instanceof EtcItemType)
		{
			switch ((EtcItemType) itemType)
			{
				case POTION:
				case SCROLL:
				case ELIXIR:
				case SOULSHOT:
				{
					return "SUPPLY".equals(category);
				}
				case MATERIAL:
				case RECIPE:
				case SEED:
				case SEED2:
				{
					return "MATERIAL".equals(category);
				}
				default:
				{
					break;
				}
			}
		}
		
		return "MISC".equals(category);
	}
	
	/**
	 * Builds the item rows of one page.<br>
	 * Every button repeats the page and the category, so the window comes back where the admin left it.
	 * @param pageItems The items of the drawn page.
	 * @param page The drawn page, sent back by each button.
	 * @param category The drawn category code, sent back by each button.
	 * @return The item table.
	 */
	private static String getItemsHtml(List<Item> pageItems, int page, String category)
	{
		final StringBuilder sbItems = new StringBuilder();
		sbItems.append("<table width=270 border=0 cellpadding=0 cellspacing=1>");
		
		for (Item item : pageItems)
		{
			final int objectId = item.getObjectId();
			final boolean stackable = item.isStackable();
			final int enchantLevel = item.getEnchantLevel();
			final String countSuffix = stackable ? (" (" + item.getCount() + ")") : ((item.isEquipable() && (enchantLevel > 0)) ? (" (+" + enchantLevel + ")") : "");
			
			// Name row: icon, name with its count or enchant level, delete button at the end.
			sbItems.append("<tr height=36>");
			sbItems.append("<td width=32><img src=\"" + item.getTemplate().getIcon() + "\" width=32 height=32></td>");
			sbItems.append("<td width=200><font color=\"LEVEL\">[" + item.getId() + "] " + item.getName() + countSuffix + "</font></td>");
			sbItems.append("<td width=38 align=right><button value=\"x\" action=\"bypass -h " + COMMAND_DELETE_ITEM + " " + objectId + " " + page + " " + category + "\" width=18 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
			sbItems.append("</tr>");
			
			// Controls row, stackable items only. The nested table keeps these column widths to itself, otherwise they inherit the wide name column above and drift to the right.
			if (stackable)
			{
				sbItems.append("<tr>");
				sbItems.append("<td width=32></td>");
				sbItems.append("<td colspan=2 width=238>");
				sbItems.append("<table cellpadding=0 cellspacing=1 border=0><tr>");
				sbItems.append("<td width=48><edit var=\"qty_" + objectId + "\" width=45 height=12 type=\"number\"></td>");
				sbItems.append("<td width=32><button value=\"Add\" action=\"bypass -h " + COMMAND_ADD_COUNT + " " + objectId + " $qty_" + objectId + " " + page + " " + category + "\" width=30 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sbItems.append("<td width=18><button value=\"+\" action=\"bypass -h " + COMMAND_INC_COUNT + " " + objectId + " " + page + " " + category + "\" width=18 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sbItems.append("<td width=18><button value=\"-\" action=\"bypass -h " + COMMAND_DEC_COUNT + " " + objectId + " " + page + " " + category + "\" width=18 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\"></td>");
				sbItems.append("</tr></table>");
				sbItems.append("</td>");
				sbItems.append("</tr>");
			}
			
			// Spacer row between items.
			sbItems.append("<tr><td colspan=3 height=12></td></tr>");
		}
		
		sbItems.append("</table>");
		return sbItems.toString();
	}
	
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
