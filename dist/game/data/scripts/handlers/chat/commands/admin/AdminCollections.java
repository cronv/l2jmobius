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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicCollectionDataHolder;
import org.l2jmobius.gameserver.data.holders.RelicCollectionInfoHolder;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.data.xml.OptionData;
import org.l2jmobius.gameserver.data.xml.RelicCollectionData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.CollectionList;
import org.l2jmobius.gameserver.entity.actor.holders.player.PlayerCollectionData;
import org.l2jmobius.gameserver.entity.actor.holders.player.PlayerRelicCollectionData;
import org.l2jmobius.gameserver.entity.actor.holders.player.RelicList;
import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionActiveEvent;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionComplete;
import org.l2jmobius.gameserver.network.serverpackets.collection.ExCollectionInfo;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionCompleteAnnounce;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsCollectionInfo;

/**
 * This class handles the admin command for viewing and managing collections.<br>
 * It allows administrators to interact with collection data through a dedicated menu.
 * @author CostyKiller
 */
public class AdminCollections implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_collections_menu",
		"admin_collection_add",
		"admin_collection_remove",
		"admin_collection_switch",
		"admin_collection_type_switch",
		"admin_collection_addall_type",
		"admin_collection_addall_category",
		"admin_collection_removeall_type",
		"admin_collection_removeall_category"
	};
	
	private static final int ITEMS_PER_PAGE = 3;
	
	/**
	 * Processes administrative commands related to player collections.<br>
	 * This method handles adding, removing, and viewing collection data for a targeted {@code Player}.<br>
	 * It validates the target and parses command arguments to update database records and send packets.
	 * @param command The raw command string entered by the administrator.
	 * @param activeChar The {@code Player} object of the administrator executing the command.
	 * @return {@code true} if the command was processed successfully, {@code false} otherwise.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		if ((activeChar.getTarget() == null) || !activeChar.getTarget().isPlayer())
		{
			activeChar.sendMessage("Invalid target! Please target a player.");
			return false;
		}
		
		Player target = activeChar.getTarget().asPlayer();
		
		final CollectionList collectionList = target.getCollectionList();
		final RelicList relicList = target.getRelicList();
		if (command.startsWith("admin_collections_menu"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			int type = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			int category = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			int page = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 0;
			
			showCollectionsMenu(activeChar, target, type, category, page);
		}
		else if (command.startsWith("admin_collection_type_switch"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			int newType = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			showCollectionsMenu(activeChar, target, newType, 1, 0);
		}
		else if (command.startsWith("admin_collection_switch"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			int type = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			int category = st.hasMoreTokens() ? Integer.parseInt(st.nextToken()) : 1;
			
			showCollectionsMenu(activeChar, target, type, category, 0);
		}
		else if (command.startsWith("admin_collection_addall_type"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (st.countTokens() < 3)
			{
				activeChar.sendMessage("Usage: //collection_addall_type <type> <category> <page>");
				return false;
			}
			
			int type = Integer.parseInt(st.nextToken());
			int category = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			
			int addedCount = 0;
			
			if (type == 1) // Collections
			{
				for (CollectionDataHolder collection : CollectionData.getInstance().getCollections())
				{
					final int collectionId = collection.getCollectionId();
					if (!collectionList.isComplete(collectionId))
					{
						for (ItemEnchantHolder item : collection.getItems())
						{
							final int itemIndex = item.getIndex();
							boolean indexExists = collectionList.getCollections().stream().anyMatch(col -> (col.getCollectionId() == collectionId) && (col.getIndex() == itemIndex));
							if (!indexExists)
							{
								collectionList.add(new PlayerCollectionData(collectionId, item.getId(), item.getIndex()));
							}
						}
						
						target.sendPacket(new ExCollectionComplete(collectionId));
						target.sendPacket(new SystemMessage(SystemMessageId.S1_COLLECTION_IS_COMPLETE).addString(collection.getName()));
						
						final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
						if (options != null)
						{
							options.apply(target);
						}
						
						addedCount++;
					}
				}
				collectionList.store();
				
				if (category == 7)
				{
					target.sendPacket(new ExCollectionActiveEvent());
				}
				else
				{
					target.sendPacket(new ExCollectionInfo(target, category));
				}
			}
			else if (type == 2) // Relic Collections
			{
				for (RelicCollectionDataHolder collection : RelicCollectionData.getInstance().getRelicCollections())
				{
					final int collectionId = collection.getCollectionId();
					if (!relicList.isCompleteCollection(collectionId))
					{
						int index = 0;
						for (RelicCollectionInfoHolder relic : collection.getRelics())
						{
							final int relicId = relic.getRelicId();
							final boolean alreadyRegistered = relicList.getCollections().stream().anyMatch(rc -> (rc.getRelicCollectionId() == collectionId) && (rc.getRelicId() == relicId));
							if (!alreadyRegistered)
							{
								relicList.getCollections().add(new PlayerRelicCollectionData(collectionId, relic.getRelicId(), relic.getEnchantLevel(), index));
							}
							index++;
						}
						
						target.sendPacket(new ExRelicsCollectionCompleteAnnounce(collectionId));
						
						if (relicList.isCompleteCollection(collectionId))
						{
							final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
							if (options != null)
							{
								options.apply(target);
							}
						}
						
						addedCount++;
					}
				}
				relicList.storeCollections();
				target.sendPacket(new ExRelicsCollectionInfo(target));
			}
			
			activeChar.sendMessage("Added " + addedCount + " missing collection(s) to " + target.getName());
			if (activeChar != target)
			{
				target.sendMessage("Admin added " + addedCount + " collection(s) to you.");
			}
			
			showCollectionsMenu(activeChar, target, type, category, page);
		}
		else if (command.startsWith("admin_collection_addall_category"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (st.countTokens() < 3)
			{
				activeChar.sendMessage("Usage: //collection_addall_category <type> <category> <page>");
				return false;
			}
			
			int type = Integer.parseInt(st.nextToken());
			int category = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			
			int addedCount = 0;
			
			if (type == 1) // Collections
			{
				for (CollectionDataHolder collection : CollectionData.getInstance().getCollectionsByTabId(category))
				{
					final int collectionId = collection.getCollectionId();
					if (!collectionList.isComplete(collectionId))
					{
						for (ItemEnchantHolder item : collection.getItems())
						{
							final int itemIndex = item.getIndex();
							boolean indexExists = collectionList.getCollections().stream().anyMatch(col -> (col.getCollectionId() == collectionId) && (col.getIndex() == itemIndex));
							if (!indexExists)
							{
								collectionList.add(new PlayerCollectionData(collectionId, item.getId(), item.getIndex()));
							}
						}
						
						target.sendPacket(new ExCollectionComplete(collectionId));
						target.sendPacket(new SystemMessage(SystemMessageId.S1_COLLECTION_IS_COMPLETE).addString(collection.getName()));
						
						final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
						if (options != null)
						{
							options.apply(target);
						}
						
						addedCount++;
					}
				}
				collectionList.store();
				
				if (category == 7)
				{
					target.sendPacket(new ExCollectionActiveEvent());
				}
				else
				{
					target.sendPacket(new ExCollectionInfo(target, category));
				}
			}
			else if (type == 2) // Relic Collections
			{
				for (RelicCollectionDataHolder collection : RelicCollectionData.getInstance().getRelicCategory(category))
				{
					final int collectionId = collection.getCollectionId();
					if (!relicList.isCompleteCollection(collectionId))
					{
						int index = 0;
						for (RelicCollectionInfoHolder relic : collection.getRelics())
						{
							final int relicId = relic.getRelicId();
							final boolean alreadyRegistered = relicList.getCollections().stream().anyMatch(rc -> (rc.getRelicCollectionId() == collectionId) && (rc.getRelicId() == relicId));
							if (!alreadyRegistered)
							{
								relicList.getCollections().add(new PlayerRelicCollectionData(collectionId, relic.getRelicId(), relic.getEnchantLevel(), index));
							}
							index++;
						}
						
						target.sendPacket(new ExRelicsCollectionCompleteAnnounce(collectionId));
						
						if (relicList.isCompleteCollection(collectionId))
						{
							final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
							if (options != null)
							{
								options.apply(target);
							}
						}
						
						addedCount++;
					}
				}
				relicList.storeCollections();
				target.sendPacket(new ExRelicsCollectionInfo(target));
			}
			
			activeChar.sendMessage("Added " + addedCount + " missing collection(s) from Category " + category + " to " + target.getName());
			if (activeChar != target)
			{
				target.sendMessage("Admin added " + addedCount + " collection(s) to you.");
			}
			
			showCollectionsMenu(activeChar, target, type, category, page);
		}
		else if (command.startsWith("admin_collection_add"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (st.countTokens() < 4)
			{
				activeChar.sendMessage("Usage: //collection_add <type> <id> <category> <page>");
				return false;
			}
			
			int type = Integer.parseInt(st.nextToken());
			int id = Integer.parseInt(st.nextToken());
			int category = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			
			if (type == 1) // Collection
			{
				CollectionDataHolder collection = CollectionData.getInstance().getCollection(id);
				if (collection == null)
				{
					activeChar.sendMessage("Invalid collection ID: " + id);
					return false;
				}
				
				// Add all items from the collection with unique index check
				for (ItemEnchantHolder item : collection.getItems())
				{
					// Check if this collection+index combination already exists
					final int itemIndex = item.getIndex();
					boolean indexExists = collectionList.getCollections().stream().anyMatch(col -> (col.getCollectionId() == id) && (col.getIndex() == itemIndex));
					
					// Only add if this specific index doesn't exist for this collection
					if (!indexExists)
					{
						collectionList.add(new PlayerCollectionData(id, item.getId(), item.getIndex()));
					}
				}
				collectionList.store();
				
				// Update client
				if (category == 7)
				{
					target.sendPacket(new ExCollectionActiveEvent());
				}
				else
				{
					target.sendPacket(new ExCollectionInfo(target, category));
				}
				
				// Announce Collection Complete.
				target.sendPacket(new ExCollectionComplete(id));
				target.sendPacket(new SystemMessage(SystemMessageId.S1_COLLECTION_IS_COMPLETE).addString(collection.getName()));
				
				// Apply collection option
				final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
				if (options != null)
				{
					options.apply(target);
				}
				
				activeChar.sendMessage("Added Collection '" + sanitizeText(collection.getName()) + "' to " + target.getName());
			}
			else if (type == 2) // Relic collection
			{
				RelicCollectionDataHolder collection = RelicCollectionData.getInstance().getRelicCollection(id);
				if (collection == null)
				{
					activeChar.sendMessage("Invalid relic collection ID: " + id);
					return false;
				}
				
				// Add all relics from the collection with duplicate index check.
				int index = 0;
				for (RelicCollectionInfoHolder relic : collection.getRelics())
				{
					final int relicId = relic.getRelicId();
					final boolean alreadyRegistered = relicList.getCollections().stream().anyMatch(rc -> (rc.getRelicCollectionId() == id) && (rc.getRelicId() == relicId));
					if (!alreadyRegistered)
					{
						relicList.getCollections().add(new PlayerRelicCollectionData(id, relic.getRelicId(), relic.getEnchantLevel(), index));
					}
					index++;
				}
				relicList.storeCollections();
				
				// Update client.
				target.sendPacket(new ExRelicsCollectionInfo(target));
				// Announce Collection Complete.
				target.sendPacket(new ExRelicsCollectionCompleteAnnounce(id));
				
				// Apply collection option if complete.
				if (relicList.isCompleteCollection(id))
				{
					final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
					if (options != null)
					{
						options.apply(target);
					}
				}
				
				activeChar.sendMessage("Added Relic Collection '" + sanitizeText(collection.getName()) + "' to " + target.getName());
			}
			
			if (activeChar != target)
			{
				target.sendMessage("Admin added a collection to you.");
			}
			
			showCollectionsMenu(activeChar, target, type, category, page);
		}
		else if (command.startsWith("admin_collection_removeall_type"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (st.countTokens() < 3)
			{
				activeChar.sendMessage("Usage: //collection_removeall_type <type> <category> <page>");
				return false;
			}
			
			int type = Integer.parseInt(st.nextToken());
			int category = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			
			int removedCount = 0;
			
			if (type == 1) // Collections
			{
				for (CollectionDataHolder collection : CollectionData.getInstance().getCollections())
				{
					if (collectionList.isComplete(collection.getCollectionId()))
					{
						final int collectionId = collection.getCollectionId();
						collectionList.removeById(collectionId);
						
						final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
						if (options != null)
						{
							options.remove(target);
						}
						
						removedCount++;
					}
				}
				collectionList.store();
				target.sendPacket(new ExCollectionInfo(target, category));
			}
			else if (type == 2) // Relic Collections
			{
				for (RelicCollectionDataHolder collection : RelicCollectionData.getInstance().getRelicCollections())
				{
					if (relicList.isCompleteCollection(collection.getCollectionId()))
					{
						final int collectionId = collection.getCollectionId();
						relicList.removeCollectionById(collectionId);
						
						final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
						if (options != null)
						{
							options.remove(target);
						}
						
						removedCount++;
					}
				}
				relicList.storeCollections();
				target.sendPacket(new ExRelicsCollectionInfo(target));
			}
			
			activeChar.sendMessage("Removed " + removedCount + " completed collection(s) from " + target.getName());
			if (activeChar != target)
			{
				target.sendMessage("Admin removed " + removedCount + " collection(s) from you.");
			}
			
			showCollectionsMenu(activeChar, target, type, category, page);
		}
		else if (command.startsWith("admin_collection_removeall_category"))
		{
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (st.countTokens() < 3)
			{
				activeChar.sendMessage("Usage: //collection_removeall_category <type> <category> <page>");
				return false;
			}
			
			int type = Integer.parseInt(st.nextToken());
			int category = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			
			int removedCount = 0;
			
			if (type == 1) // Collections
			{
				for (CollectionDataHolder collection : CollectionData.getInstance().getCollectionsByTabId(category))
				{
					if (collectionList.isComplete(collection.getCollectionId()))
					{
						final int collectionId = collection.getCollectionId();
						collectionList.removeById(collectionId);
						
						final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
						if (options != null)
						{
							options.remove(target);
						}
						
						removedCount++;
					}
				}
				collectionList.store();
				target.sendPacket(new ExCollectionInfo(target, category));
			}
			else if (type == 2) // Relic Collections
			{
				for (RelicCollectionDataHolder collection : RelicCollectionData.getInstance().getRelicCategory(category))
				{
					if (relicList.isCompleteCollection(collection.getCollectionId()))
					{
						final int collectionId = collection.getCollectionId();
						relicList.removeCollectionById(collectionId);
						
						final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
						if (options != null)
						{
							options.remove(target);
						}
						
						removedCount++;
					}
				}
				relicList.storeCollections();
				target.sendPacket(new ExRelicsCollectionInfo(target));
			}
			
			activeChar.sendMessage("Removed " + removedCount + " completed collection(s) from Category " + category + " for " + target.getName());
			if (activeChar != target)
			{
				target.sendMessage("Admin removed " + removedCount + " collection(s) from you.");
			}
			
			showCollectionsMenu(activeChar, target, type, category, page);
		}
		else if (command.startsWith("admin_collection_remove"))
		{
			// XXX: When you remove a collection ui updates only after relog
			StringTokenizer st = new StringTokenizer(command, " ");
			st.nextToken(); // skip command
			
			if (st.countTokens() < 4)
			{
				activeChar.sendMessage("Usage: //collection_remove <type> <id> <category> <page>");
				return false;
			}
			
			int type = Integer.parseInt(st.nextToken());
			int id = Integer.parseInt(st.nextToken());
			int category = Integer.parseInt(st.nextToken());
			int page = Integer.parseInt(st.nextToken());
			
			if (type == 1) // Collection
			{
				// Remove collection option first
				final CollectionDataHolder collection = CollectionData.getInstance().getCollection(id);
				if (collection != null)
				{
					final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
					if (options != null)
					{
						options.remove(target);
					}
				}
				
				// Delete directly from database
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement ps = con.prepareStatement("DELETE FROM collections WHERE accountName=? AND collectionId=?"))
				{
					ps.setString(1, target.getAccountName());
					ps.setInt(2, id);
					ps.execute();
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Error removing collection from database: " + e.getMessage());
					return false;
				}
				
				// Update cached collections
				collectionList.removeById(id);
				
				// Send update packet
				target.sendPacket(new ExCollectionInfo(target, category));
				
				String collectionName = collection != null ? sanitizeText(collection.getName()) : "Collection #" + id;
				if ((collectionName == null) || collectionName.trim().isEmpty())
				{
					collectionName = "Collection #" + id;
				}
				activeChar.sendMessage("Removed Collection '" + collectionName + "' from " + target.getName());
			}
			else if (type == 2) // Relic collection
			{
				// Remove collection option first
				final RelicCollectionDataHolder collection = RelicCollectionData.getInstance().getRelicCollection(id);
				if (collection != null)
				{
					final Options options = OptionData.getInstance().getOptions(collection.getOptionId());
					if (options != null)
					{
						options.remove(target);
					}
				}
				
				// Delete directly from database
				try (Connection con = DatabaseFactory.getConnection();
					PreparedStatement ps = con.prepareStatement("DELETE FROM relic_collections WHERE accountName=? AND relicCollectionId=?"))
				{
					ps.setString(1, target.getAccountName());
					ps.setInt(2, id);
					ps.execute();
				}
				catch (Exception e)
				{
					activeChar.sendMessage("Error removing relic collection from database: " + e.getMessage());
					return false;
				}
				
				// Update cached collections
				relicList.removeCollectionById(id);
				
				// Send update packet
				target.sendPacket(new ExRelicsCollectionInfo(target));
				
				String collectionName = collection != null ? sanitizeText(collection.getName()) : "Relic Collection #" + id;
				if ((collectionName == null) || collectionName.trim().isEmpty())
				{
					collectionName = "Relic Collection #" + id;
				}
				activeChar.sendMessage("Removed Relic Collection '" + collectionName + "' from " + target.getName());
			}
			
			if (activeChar != target)
			{
				target.sendMessage("Admin removed a collection from you.");
			}
			
			showCollectionsMenu(activeChar, target, type, category, page);
		}
		
		return true;
	}
	
	/**
	 * Displays the collections management menu to an administrator.<br>
	 * This method builds a custom HTML message for the {@code activeChar}.<br>
	 * It populates the template with buttons and statistics based on the provided filters.
	 * @param activeChar The player who will receive the menu packet.
	 * @param target The player whose collection data is being viewed.
	 * @param type The specific collection type to display.
	 * @param category The specific category within that type.
	 * @param page The current page number for navigation.
	 */
	private void showCollectionsMenu(Player activeChar, Player target, int type, int category, int page)
	{
		String content = HtmCache.getInstance().getHtm(activeChar, "data/html/admin/collections_menu.htm");
		
		content = content.replace("%targetName%", target.getName());
		content = content.replace("%typeButtons%", generateTypeButtons(type, category, page));
		content = content.replace("%categoryButtons%", generateCategoryButtons(type, category, page));
		content = content.replace("%collections%", generateCollectionsSection(target, type, category, page));
		content = content.replace("%statistics%", generateStatistics(target, type, category, page));
		
		activeChar.sendPacket(new NpcHtmlMessage(0, 1, content));
	}
	
	/**
	 * Generates the HTML for buttons to switch between collection types.<br>
	 * It creates a table containing buttons for Common and Relic collections.<br>
	 * The button styles change based on the current selection.
	 * @param currentType The ID of the currently selected collection type.
	 * @param category The current category index.
	 * @param page The current page number.
	 * @return A string containing the HTML table with buttons.
	 */
	private String generateTypeButtons(int currentType, int category, int page)
	{
		StringBuilder html = new StringBuilder();
		html.append("<table width=270><tr>");
		
		// Common Collections button
		String commonStyle = currentType == 1 ? "L2UI_CT1.Button_DF_Calculator_Down" : "L2UI_CT1.Button_DF_Down";
		html.append("<td width=135 align=center>");
		html.append("<button value=\"Collections\" action=\"bypass -h admin_collection_type_switch 1\" ");
		html.append("width=130 height=22 back=\"").append(commonStyle).append("\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</td>");
		
		// Relic Collections button
		String relicStyle = currentType == 2 ? "L2UI_CT1.Button_DF_Calculator_Down" : "L2UI_CT1.Button_DF_Down";
		html.append("<td width=135 align=center>");
		html.append("<button value=\"Relic Collections\" action=\"bypass -h admin_collection_type_switch 2\" ");
		html.append("width=130 height=22 back=\"").append(relicStyle).append("\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</td>");
		
		html.append("</tr></table>");
		return html.toString();
	}
	
	/**
	 * Generates the HTML for category selection buttons.<br>
	 * It creates a table of buttons based on the maximum categories allowed for a specific type.<br>
	 * The active button is styled differently to indicate the current selection.
	 * @param type The type of collection being viewed.
	 * @param currentCategory The index of the currently selected category.
	 * @param page The current page number.
	 * @return A string containing the HTML table for the buttons.
	 */
	private String generateCategoryButtons(int type, int currentCategory, int page)
	{
		StringBuilder html = new StringBuilder();
		int maxCategory = type == 1 ? 7 : 4;
		
		html.append("<table width=270><tr>");
		for (int i = 1; i <= maxCategory; i++)
		{
			String buttonStyle = (i == currentCategory) ? "L2UI_CT1.Button_DF_Calculator_Down" : "L2UI_CT1.Button_DF_Down";
			int width = (270 / maxCategory) - 2;
			
			html.append("<td align=center>");
			html.append("<button value=\"Cat ").append(i).append("\" ");
			html.append("action=\"bypass -h admin_collection_switch ").append(type).append(' ').append(i).append("\" ");
			html.append("width=").append(width).append(" height=20 ");
			html.append("back=\"").append(buttonStyle).append("\" fore=\"L2UI_CT1.Button_DF\">");
			html.append("</td>");
		}
		html.append("</tr></table>");
		
		return html.toString();
	}
	
	/**
	 * Generates an HTML table representing a list of collections for the admin menu.<br>
	 * It handles both standard and relic collection types based on the provided parameters.<br>
	 * The method includes logic for pagination, status colors, and action buttons.
	 * @param target The {@code Player} whose collection progress is being checked.
	 * @param type The category type where {@code 1} represents standard collections.
	 * @param category The specific ID of the category to display.
	 * @param page The current page index for pagination.
	 * @return A {@code String} containing the generated HTML content.
	 */
	private String generateCollectionsSection(Player target, int type, int category, int page)
	{
		StringBuilder html = new StringBuilder();
		
		List<?> collections;
		String typeName;
		
		if (type == 1)
		{
			collections = CollectionData.getInstance().getCollectionsByTabId(category);
			typeName = "Collection";
		}
		else
		{
			collections = RelicCollectionData.getInstance().getRelicCategory(category);
			typeName = "Relic Collection";
		}
		
		if (collections.isEmpty())
		{
			html.append("<table width=270 bgcolor=282828 cellpadding=5>");
			html.append("<tr><td align=center><font color=\"FF6B6B\">No collections found in this category</font></td></tr>");
			html.append("</table>");
			return html.toString();
		}
		
		final int totalCollections = collections.size();
		final int totalPages = (int) Math.ceil((double) totalCollections / ITEMS_PER_PAGE);
		final int startIndex = page * ITEMS_PER_PAGE;
		final int endIndex = Math.min(startIndex + ITEMS_PER_PAGE, totalCollections);
		
		boolean categoryAllComplete = totalCollections > 0;
		final CollectionList collectionList = target.getCollectionList();
		final RelicList relicList = target.getRelicList();
		for (Object obj : collections)
		{
			boolean thisComplete;
			if (type == 1)
			{
				thisComplete = collectionList.isComplete(((CollectionDataHolder) obj).getCollectionId());
			}
			else
			{
				thisComplete = relicList.isCompleteCollection(((RelicCollectionDataHolder) obj).getCollectionId());
			}
			
			if (!thisComplete)
			{
				categoryAllComplete = false;
				break;
			}
		}
		
		String categoryButtonValue = categoryAllComplete ? "Remove All" : "Add All";
		String categoryButtonAction = categoryAllComplete ? "admin_collection_removeall_category" : "admin_collection_addall_category";
		
		html.append("<table width=270 bgcolor=282828 cellpadding=2 cellspacing=1>");
		
		// Header
		html.append("<tr>");
		html.append("<td width=190 align=left> <font color=\"LEVEL\">").append(typeName).append(" (Category ").append(category).append(")</font></td>");
		html.append("<td></td>");
		html.append("<td width=80 align=center>");
		html.append("<button value=\"").append(categoryButtonValue).append("\" action=\"bypass -h ").append(categoryButtonAction).append(' ').append(type).append(' ').append(category).append(' ').append(page).append("\" width=65 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</td>");
		html.append("</tr>");
		
		// Collections list
		for (int i = startIndex; i < endIndex; i++)
		{
			int collectionId;
			String collectionName;
			boolean hasCollection;
			
			if (type == 1)
			{
				CollectionDataHolder collection = (CollectionDataHolder) collections.get(i);
				collectionId = collection.getCollectionId();
				collectionName = sanitizeHtmlText(collection.getName());
				hasCollection = collectionList.isComplete(collectionId);
			}
			else
			{
				RelicCollectionDataHolder collection = (RelicCollectionDataHolder) collections.get(i);
				collectionId = collection.getCollectionId();
				collectionName = sanitizeHtmlText(collection.getName());
				hasCollection = relicList.isCompleteCollection(collectionId);
			}
			
			// Fallback to ID if name is empty after sanitization
			if ((collectionName == null) || collectionName.trim().isEmpty())
			{
				collectionName = typeName + " #" + collectionId;
			}
			
			String bgColor = ((i - startIndex) % 2) == 0 ? "171612" : "1f1c18";
			String statusColor = hasCollection ? "4EE44E" : "FF6B6B";
			String statusText = hasCollection ? "✓ Complete" : "✗ Missing";
			String action = hasCollection ? "remove" : "add";
			String buttonValue = hasCollection ? "Remove" : "Add";
			String buttonBack = hasCollection ? "L2UI_CT1.Button_DF_Down" : "L2UI_CT1.Button_DF_Down";
			
			html.append("<tr bgcolor=").append(bgColor).append('>');
			html.append("<td width=110 align=left> ").append(collectionName).append("</td>");
			html.append("<td width=70 align=center><font color=\"").append(statusColor).append("\">").append(statusText).append("</font></td>");
			html.append("<td width=45 align=center>");
			html.append("<button value=\"").append(buttonValue).append("\" ");
			html.append("action=\"bypass -h admin_collection_").append(action).append(' ');
			html.append(type).append(' ').append(collectionId).append(' ').append(category).append(' ').append(page).append("\" ");
			html.append("width=45 height=20 back=\"").append(buttonBack).append("\" fore=\"L2UI_CT1.Button_DF\">");
			html.append("</td>");
			html.append("</tr>");
		}
		
		// Pagination
		if (totalPages > 1)
		{
			html.append("</table><tr>");
			html.append("<td colspan=3 align=center bgcolor=1a1a1a>");
			html.append("<table width=268><tr>");
			
			// Previous button
			html.append("<td width=70 align=center>");
			if (page > 0)
			{
				html.append("<button value=\"◄ Prev\" action=\"bypass -h admin_collections_menu ");
				html.append(type).append(' ').append(category).append(' ').append(page - 1);
				html.append("\" width=65 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			}
			else
			{
				html.append("<button value=\"◄ Prev\" action=\"\" width=65 height=20 ");
				html.append("back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF_Grayed\">");
			}
			html.append("</td>");
			
			// Page info
			html.append("<td width=128 align=center>");
			html.append("<font color=\"LEVEL\">Page ").append(page + 1).append(" / ").append(totalPages).append("</font>");
			html.append("</td>");
			
			// Next button
			html.append("<td width=70 align=center>");
			if (page < (totalPages - 1))
			{
				html.append("<button value=\"Next ►\" action=\"bypass -h admin_collections_menu ");
				html.append(type).append(' ').append(category).append(' ').append(page + 1);
				html.append("\" width=65 height=20 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
			}
			else
			{
				html.append("<button value=\"Next ►\" action=\"\" width=65 height=20 ");
				html.append("back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF_Grayed\">");
			}
			html.append("</td>");
			
			html.append("</tr></table>");
			html.append("</td>");
			html.append("</tr>");
		}
		
		html.append("</table>");
		return html.toString();
	}
	
	/**
	 * Generates an HTML table showing the completion progress of a player's collections.<br>
	 * It calculates the percentage of completed items based on the specified type.<br>
	 * The text color changes dynamically depending on the progress reached.
	 * @param target The {@code Player} whose statistics will be calculated.
	 * @param type The category of collection to check, where {@code 1} represents standard collections.
	 * @param category
	 * @param page
	 * @return A {@code String} containing the HTML table for display.
	 */
	private String generateStatistics(Player target, int type, int category, int page)
	{
		StringBuilder html = new StringBuilder();
		
		int total = 0;
		int completed = 0;
		
		if (type == 1)
		{
			Collection<CollectionDataHolder> allCollections = CollectionData.getInstance().getCollections();
			total = allCollections.size();
			for (CollectionDataHolder collection : allCollections)
			{
				if (target.getCollectionList().isComplete(collection.getCollectionId()))
				{
					completed++;
				}
			}
		}
		else
		{
			Collection<RelicCollectionDataHolder> allCollections = RelicCollectionData.getInstance().getRelicCollections();
			total = allCollections.size();
			for (RelicCollectionDataHolder collection : allCollections)
			{
				if (target.getRelicList().isCompleteCollection(collection.getCollectionId()))
				{
					completed++;
				}
			}
		}
		
		int percentage = total > 0 ? (completed * 100) / total : 0;
		String color = percentage >= 75 ? "4EE44E" : percentage >= 50 ? "FFD700" : percentage >= 25 ? "FFA500" : "FF6B6B";
		
		boolean allComplete = (total > 0) && (completed == total);
		String buttonValue = allComplete ? "Remove All" : "Add All";
		String buttonAction = allComplete ? "admin_collection_removeall_type" : "admin_collection_addall_type";
		
		html.append("<table width=270 bgcolor=282828 cellpadding=3>");
		html.append("<tr>");
		html.append("<td width=120 align=center>Completed: <font color=\"").append(color).append("\">").append(completed).append("/").append(total).append("</font></td>");
		html.append("<td width=95 align=center>Progress: <font color=\"").append(color).append("\">").append(percentage).append("%</font></td>");
		html.append("<td width=80 align=center>");
		html.append("<button value=\"").append(buttonValue).append("\" action=\"bypass -h ").append(buttonAction).append(' ').append(type).append(' ').append(category).append(' ').append(page).append("\" width=65 height=18 back=\"L2UI_CT1.Button_DF_Down\" fore=\"L2UI_CT1.Button_DF\">");
		html.append("</td>");
		html.append("</tr>");
		html.append("</table>");
		
		return html.toString();
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns a static array of command strings.
	 * @return an array of {@code String} containing all registered commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
	
	/**
	 * Cleans the input string to make it safe for HTML display.<br>
	 * It replaces special characters and removes invisible control characters.<br>
	 * The method returns an empty string if the input is {@code null} or empty.
	 * @param text The raw string to be sanitized.
	 * @return The cleaned and trimmed string.
	 */
	private String sanitizeHtmlText(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return "";
		}
		
		// Replace HTML special characters
		text = text.replace("&", "&amp;");
		text = text.replace("<", "&lt;");
		text = text.replace(">", "&gt;");
		text = text.replace("\"", "&quot;");
		
		// Remove control characters and other problematic characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		
		// Remove zero-width spaces and other invisible characters
		text = text.replaceAll("[\u200B-\u200D\uFEFF]", "");
		
		return text.trim();
	}
	
	/**
	 * Cleans the input string by removing hidden or problematic characters.<br>
	 * It handles {@code null} and empty inputs safely.<br>
	 * The result is trimmed of leading and trailing whitespace.
	 * @param text The raw string to be cleaned.
	 * @return A sanitized version of the input string or an empty string if the input was null or empty.
	 */
	private String sanitizeText(String text)
	{
		if ((text == null) || text.isEmpty())
		{
			return "";
		}
		
		// Remove control characters and other problematic characters
		text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");
		
		// Remove zero-width spaces and other invisible characters
		text = text.replaceAll("[\u200B-\u200D\uFEFF]", "");
		
		return text.trim();
	}
}