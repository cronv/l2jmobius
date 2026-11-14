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
package custom.Transmog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.custom.TransmogConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemAdd;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Handles the transmogrification system for player equipment.<br>
 * This class manages how items appear on a {@code Player} without affecting their base stats. It processes item data and updates visual representations based on configuration settings.
 * @author Mobius
 */
public class Transmog extends Script
{
	private static final int NPC = 900009;
	private static final Map<Integer, Map<Integer, Set<Integer>>> PLAYER_TRANSMOGS = new ConcurrentHashMap<>();
	private static final String LOAD_SQL = "SELECT itemId FROM character_transmogs WHERE owner=? ORDER BY itemId ASC;";
	private static final String SAVE_SQL = "REPLACE INTO character_transmogs (owner,itemId) VALUE (?,?)";
	
	/**
	 * Initializes the {@code Transmog} class.<br>
	 * Registers necessary NPCs and talk IDs.<br>
	 * Sets up event listeners if {@code TransmogConfig.ENABLE_TRANSMOG} is true.
	 */
	private Transmog()
	{
		addStartNpc(NPC);
		addTalkId(NPC);
		addFirstTalkId(NPC);
		
		if (TransmogConfig.ENABLE_TRANSMOG)
		{
			final ListenersContainer players = Containers.Players();
			players.addListener(new ConsumerEventListener(players, EventType.ON_PLAYER_ITEM_ADD, (OnPlayerItemAdd event) -> onPlayerItemAdd(event), this));
			players.addListener(new ConsumerEventListener(players, EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
			players.addListener(new ConsumerEventListener(players, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
		}
	}
	
	/**
	 * Handles various transmog-related events for players interacting with an {@code Npc}.<br>
	 * It processes menu navigation, removing transmogs, and setting new transmogs.<br>
	 * The method also generates dynamic HTML content to display available transmogs.
	 * @param event The specific action or command string sent by the player.
	 * @param npc The {@code Npc} entity triggering the event.
	 * @param player The {@code Player} performing the action.
	 * @return A {@code String} representing the HTML content to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("menu"))
		{
			return "900009-01.html";
		}
		else if (event.startsWith("rem"))
		{
			final String[] split = event.split("!");
			final String slot = split[1];
			final String page = split[2];
			if (isValidSlot(slot))
			{
				final Item item = player.getInventory().unEquipItemInBodySlot(getBodypart(slot));
				if (item != null)
				{
					final int transmogId = item.getTransmogId();
					if (transmogId > 0)
					{
						if (TransmogConfig.TRANSMOG_REMOVE_COST > 0)
						{
							if (player.getAdena() < TransmogConfig.TRANSMOG_REMOVE_COST)
							{
								player.getInventory().equipItem(item);
								player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
								onEvent(slot + "!" + page, npc, player);
								return null;
							}
							
							player.reduceAdena(ItemProcessType.FEE, TransmogConfig.TRANSMOG_REMOVE_COST, player, true);
						}
						
						item.removeTransmog();
						player.sendMessage("You have removed your " + ItemData.getInstance().getTemplate(transmogId).getName() + " transmog.");
					}
					
					player.getInventory().equipItem(item);
					player.broadcastInfo();
				}
				else
				{
					player.sendMessage("No item equipped in this slot.");
				}
			}
			
			onEvent(slot + "!" + page, npc, player);
		}
		else if (event.startsWith("set"))
		{
			final String[] split = event.split("!");
			final String slot = split[1];
			final String page = split[2];
			final int transmogId = Integer.parseInt(split[3]);
			if (isValidSlot(slot))
			{
				final BodyPart bodypart = getBodypart(slot);
				final Item item = player.getInventory().unEquipItemInBodySlot(bodypart);
				if (item != null)
				{
					final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(transmogId);
					if (itemTemplate != null)
					{
						final Map<Integer, Set<Integer>> playerTransmogs = PLAYER_TRANSMOGS.getOrDefault(player.getObjectId(), Collections.emptyMap());
						final Set<Integer> itemIds = playerTransmogs.getOrDefault((int) bodypart.getMask(), Collections.emptySet());
						if (itemIds.contains(transmogId))
						{
							if (TransmogConfig.TRANSMOG_APPLY_COST > 0)
							{
								if (player.getAdena() < TransmogConfig.TRANSMOG_APPLY_COST)
								{
									player.getInventory().equipItem(item);
									player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA_2);
									onEvent(slot + "!" + page, npc, player);
									return null;
								}
								
								player.reduceAdena(ItemProcessType.FEE, TransmogConfig.TRANSMOG_APPLY_COST, player, true);
							}
							
							item.setTransmogId(transmogId);
							player.sendMessage("You have set your item transmog to " + itemTemplate.getName() + ".");
						}
					}
					
					player.getInventory().equipItem(item);
					player.broadcastInfo();
				}
				else
				{
					player.sendMessage("No item equipped in this slot.");
				}
			}
			
			onEvent(slot + "!" + page, npc, player);
		}
		else
		{
			final String[] split = event.split("!");
			final String slot = split[0];
			
			final Map<Integer, Set<Integer>> playerTransmogs = PLAYER_TRANSMOGS.getOrDefault(player.getObjectId(), Collections.emptyMap());
			final Set<Integer> itemIds = playerTransmogs.getOrDefault((int) getBodypart(slot).getMask(), Collections.emptySet());
			if (itemIds.isEmpty())
			{
				return "900009-02.html";
			}
			
			final int page = split.length > 1 ? Integer.parseInt(split[1]) : 1;
			int total = 0;
			int counter = 0;
			final int pages = Math.ceilDiv(itemIds.size(), 16);
			final int maxItem = page * 16;
			final int minItem = maxItem - 16;
			String content = HtmCache.getInstance().getHtm(player, "data/scripts/custom/Transmog/900009-03.html");
			for (Integer itemId : itemIds)
			{
				final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
				if (itemTemplate != null)
				{
					total++;
					if ((total < minItem) || (total > maxItem))
					{
						continue;
					}
					
					counter++;
					final String itemName = itemTemplate.getName();
					content = content.replace("%" + counter + "%", "<center><img src=\"" + itemTemplate.getIcon() + "\" width=32 height=32><br><a action=\"bypass Script Transmog set!" + slot + "!" + page + "!" + itemTemplate.getDisplayId() + "\">" + itemName + "</a></center>");
				}
			}
			
			for (int i = 1; i < 17; i++)
			{
				content = content.replace("%" + i + "%", "");
			}
			
			content = content.replace("%title%", "Transmog (" + page + " of " + pages + ")");
			content = content.replace("%previous%", "<a action=\"bypass Script Transmog " + slot + "!" + Math.max(1, (page - 1)) + "\">Previous</a>");
			content = content.replace("%next%", "<a action=\"bypass Script Transmog " + slot + "!" + Math.min(pages, (page + 1)) + "\">Next</a>");
			content = content.replace("%remove%", "<a action=\"bypass Script Transmog rem!" + slot + "!" + page + "\">Remove Transmog</a>");
			
			HtmlUtil.sendCBHtml(player, content);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when a player adds an item to their inventory.<br>
	 * This method checks if the item is eligible for the transmog system.<br>
	 * If valid, it adds the item's display ID to the player's collection.<br>
	 * A notification message is sent to the player upon successful addition.
	 * @param event The {@code OnPlayerItemAdd} event containing the player and item data.
	 */
	public void onPlayerItemAdd(OnPlayerItemAdd event)
	{
		final ItemTemplate itemTemplate = event.getItem().getTemplate();
		if ((itemTemplate instanceof EtcItem) || itemTemplate.isForNpc())
		{
			return;
		}
		
		final Integer bodypart = (int) itemTemplate.getBodyPart().getMask();
		if ((bodypart < BodyPart.R_HAND.getMask()) || (bodypart > BodyPart.HAIRALL.getMask()))
		{
			return;
		}
		
		if (TransmogConfig.TRANSMOG_BANNED_ITEM_IDS.contains(itemTemplate.getId()))
		{
			return;
		}
		
		final Player player = event.getPlayer();
		final Integer playerObjectId = player.getObjectId();
		final Map<Integer, Set<Integer>> playerTransmogs = PLAYER_TRANSMOGS.computeIfAbsent(playerObjectId, _ -> new HashMap<>());
		final Set<Integer> itemIds = playerTransmogs.computeIfAbsent(bodypart, _ -> new HashSet<>());
		if (itemIds.add(itemTemplate.getDisplayId()))
		{
			player.sendPacket(new CreatureSay(null, ChatType.WHISPER, "[Transmog]", itemTemplate.getName() + " has been added to your appearance collection."));
		}
	}
	
	/**
	 * Handles the logic when a player logs into the game.<br>
	 * This method loads the transmog data from the database for the current player.<br>
	 * It stores the loaded items in the {@code PLAYER_TRANSMOGS} map.
	 * @param event The {@code OnPlayerLogin} event containing the player information.
	 */
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		final String owner = TransmogConfig.TRANSMOG_SHARE_ACCOUNT ? player.getAccountName() : String.valueOf(player.getObjectId());
		final Map<Integer, Set<Integer>> playerTransmogs = new HashMap<>();
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(LOAD_SQL))
		{
			statement.setString(1, owner);
			try (ResultSet result = statement.executeQuery())
			{
				while (result.next())
				{
					final int itemId = result.getInt(1);
					final ItemTemplate itemTemplate = ItemData.getInstance().getTemplate(itemId);
					if (itemTemplate != null)
					{
						final Integer bodypart = (int) itemTemplate.getBodyPart().getMask();
						playerTransmogs.computeIfAbsent(bodypart, _ -> new HashSet<>()).add(itemId);
					}
				}
			}
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with Transmog: " + e.getMessage());
		}
		
		if (!playerTransmogs.isEmpty())
		{
			PLAYER_TRANSMOGS.put(player.getObjectId(), playerTransmogs);
		}
	}
	
	/**
	 * Handles the logic when a player logs out of the game.<br>
	 * This method saves all active transmogs to the database.<br>
	 * It also removes the player from the temporary memory cache.
	 * @param event The {@code OnPlayerLogout} event containing the player data.
	 */
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		final Integer playerObjectId = player.getObjectId();
		final String owner = TransmogConfig.TRANSMOG_SHARE_ACCOUNT ? player.getAccountName() : String.valueOf(player.getObjectId());
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement(SAVE_SQL))
		{
			for (Set<Integer> itemIds : PLAYER_TRANSMOGS.getOrDefault(playerObjectId, Collections.emptyMap()).values())
			{
				for (int itemId : itemIds)
				{
					statement.setString(1, owner);
					statement.setInt(2, itemId);
					statement.addBatch();
				}
			}
			
			statement.executeBatch();
		}
		catch (SQLException e)
		{
			LOGGER.warning("Problem with Transmog: " + e.getMessage());
		}
		
		PLAYER_TRANSMOGS.remove(playerObjectId);
	}
	
	/**
	 * Converts a string event name into its corresponding {@code BodyPart}.<br>
	 * This method maps specific slot identifiers to the correct enum value.<br>
	 * It returns {@code BodyPart.NONE} if the provided string does not match any known part.
	 * @param event The string identifier for the body part or equipment slot.
	 * @return The corresponding {@code BodyPart} enum constant.
	 */
	private BodyPart getBodypart(String event)
	{
		switch (event)
		{
			case "R_HAND":
			{
				return BodyPart.R_HAND;
			}
			case "L_HAND":
			{
				return BodyPart.L_HAND;
			}
			case "GLOVES":
			{
				return BodyPart.GLOVES;
			}
			case "CHEST":
			{
				return BodyPart.CHEST;
			}
			case "LEGS":
			{
				return BodyPart.LEGS;
			}
			case "FEET":
			{
				return BodyPart.FEET;
			}
			case "BACK":
			{
				return BodyPart.BACK;
			}
			case "LR_HAND":
			{
				return BodyPart.LR_HAND;
			}
			case "FULL_ARMOR":
			{
				return BodyPart.FULL_ARMOR;
			}
			case "HAIRALL":
			{
				return BodyPart.HAIRALL;
			}
			case "HAIR2":
			{
				return BodyPart.HAIR2;
			}
			case "HAIR":
			{
				return BodyPart.HAIR;
			}
			default:
			{
				return BodyPart.NONE;
			}
		}
	}
	
	/**
	 * Checks if the provided slot name is a valid transmog location.<br>
	 * It compares the input against a list of allowed body parts and equipment slots.
	 * @param slot The name of the slot to validate.
	 * @return {@code true} if the slot is valid, otherwise {@code false}.
	 */
	private boolean isValidSlot(String slot)
	{
		switch (slot)
		{
			case "R_HAND":
			case "L_HAND":
			case "GLOVES":
			case "CHEST":
			case "LEGS":
			case "FEET":
			case "BACK":
			case "LR_HAND":
			case "FULL_ARMOR":
			case "HAIRALL":
			case "HAIR2":
			case "HAIR":
			{
				return true;
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Handles the logic for when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * It returns the filename of the dialogue to display.
	 * @param npc The {@code Npc} that is being spoken to.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Transmog} class.<br>
	 * It initializes a new instance of the {@code Transmog} object.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Transmog();
	}
}
