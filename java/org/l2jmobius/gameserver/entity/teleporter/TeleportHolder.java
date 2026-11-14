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
package org.l2jmobius.gameserver.entity.teleporter;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportType;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcTeleportRequest;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class acts as a container for teleportation data.<br>
 * It manages the logic and information required to handle teleports within the game world.
 * @author UnAfraid
 */
public class TeleportHolder
{
	private static final Logger LOGGER = Logger.getLogger(TeleportHolder.class.getName());
	
	private final String _name;
	private final TeleportType _type;
	private final List<TeleportLocation> _teleportData = new ArrayList<>();
	
	/**
	 * Creates a new instance of {@code TeleportHolder}.<br>
	 * This constructor initializes the holder with a specific name and teleport type.
	 * @param name The display name for this teleport list.
	 * @param type The category or type of teleport assigned to this holder.
	 */
	public TeleportHolder(String name, TeleportType type)
	{
		_name = name;
		_type = type;
	}
	
	/**
	 * Retrieves the name of this teleport holder.
	 * @return The {@code String} name of the teleport list.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the teleport type of this holder.
	 * @return the {@code TeleportType} associated with this instance.
	 */
	public TeleportType getType()
	{
		return _type;
	}
	
	/**
	 * Adds a new location to the teleport list.<br>
	 * The method creates a {@code TeleportLocation} object using the provided data.
	 * @param locData the statistics containing the location information
	 */
	public void registerLocation(StatSet locData)
	{
		_teleportData.add(new TeleportLocation(_teleportData.size(), locData));
	}
	
	/**
	 * Retrieves a specific teleport location from the list.<br>
	 * The method uses the provided ID to find the correct data.
	 * @param locationId The unique identifier for the teleport location.
	 * @return The {@code TeleportLocation} object corresponding to the ID.
	 */
	public TeleportLocation getLocation(int locationId)
	{
		return _teleportData.get(locationId);
	}
	
	/**
	 * Retrieves the list of all registered teleport locations.
	 * @return a {@code List} containing all {@code TeleportLocation} objects.
	 */
	public List<TeleportLocation> getLocations()
	{
		return _teleportData;
	}
	
	/**
	 * Displays the list of available teleports to a specific player.<br>
	 * This method sends the teleport data associated with an {@code Npc}.
	 * @param player The {@code Player} who will see the teleport list.
	 * @param npc The {@code Npc} that owns the teleport list.
	 */
	public void showTeleportList(Player player, Npc npc)
	{
		showTeleportList(player, npc, "npc_" + npc.getObjectId() + "_teleport");
	}
	
	/**
	 * Displays a list of available teleports to the player.<br>
	 * This method generates an HTML message containing teleport buttons.<br>
	 * It handles fee calculations and quest zone filtering.
	 * @param player The {@code Player} who will view the teleport list.
	 * @param npc The {@code Npc} that triggers the display.
	 * @param bypass A {@code String} used as a unique identifier for the bypass action.
	 */
	public void showTeleportList(Player player, Npc npc, String bypass)
	{
		if (isNoblesse() && (player.getNobleLevel() < 1))
		{
			LOGGER.warning(player + " requested noblesse teleport without being noble!");
			return;
		}
		
		// Load variables
		final int questZoneId = isNormalTeleport() ? player.getQuestZoneId() : -1;
		
		// Build html
		final StringBuilder sb = new StringBuilder();
		final StringBuilder sbF = new StringBuilder();
		for (TeleportLocation loc : _teleportData)
		{
			final String name = loc.getName();
			String finalName = name;
			String confirmDesc = name;
			if (loc.getNpcStringId() != null)
			{
				final int stringId = loc.getNpcStringId().getId();
				finalName = "<fstring>" + stringId + "</fstring>";
				confirmDesc = "F;" + stringId;
			}
			
			if (shouldPayFee(player, loc))
			{
				final long fee = calculateFee(player, loc);
				if (fee != 0)
				{
					finalName += " - " + fee + " " + getItemName(loc.getFeeId(), true);
				}
			}
			
			final boolean isQuestTeleport = (questZoneId >= 0) && (loc.getQuestZoneId() == questZoneId);
			if (isQuestTeleport)
			{
				sbF.append("<button align=left icon=\"quest\" action=\"bypass -h " + bypass + " " + _name + " " + loc.getId() + "\" msg=\"811;" + confirmDesc + "\">" + finalName + "</button>");
			}
			else
			{
				sb.append("<button align=left icon=\"teleport\" action=\"bypass -h " + bypass + " " + _name + " " + loc.getId() + "\" msg=\"811;" + confirmDesc + "\">" + finalName + "</button>");
			}
		}
		
		final String sbString = sb.toString();
		sbF.append(sbString);
		
		// Send html message
		final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
		msg.setFile(player, "data/html/teleporter/teleports.htm");
		msg.replace("%locations%", sbString);
		player.sendPacket(msg);
	}
	
	/**
	 * Teleports a {@code Player} to a specific location via an {@code Npc}.<br>
	 * This method validates the player status, siege conditions, and required fees.<br>
	 * It also checks for necessary permissions like noblesse status.
	 * @param player The {@code Player} who wants to teleport.
	 * @param npc The {@code Npc} providing the teleport service.
	 * @param locId The unique identifier for the destination location.
	 */
	public void doTeleport(Player player, Npc npc, int locId)
	{
		// Check if player is dead before other checks.
		if (player.isAlikeDead())
		{
			return;
		}
		
		if (isNoblesse() && (player.getNobleLevel() < 1))
		{
			LOGGER.warning(player + " requested noblesse teleport without being noble!");
			return;
		}
		
		final TeleportLocation loc = getLocation(locId);
		if (loc == null)
		{
			LOGGER.warning(player + " requested unknown teleport location " + locId + " within list " + _name + "!");
			return;
		}
		
		// Check if castle is in siege.
		if (!PlayerConfig.TELEPORT_WHILE_SIEGE_IN_PROGRESS)
		{
			for (int castleId : loc.getCastleId())
			{
				if (CastleManager.getInstance().getCastleById(castleId).getSiege().isInProgress())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_TO_A_VILLAGE_THAT_IS_IN_A_SIEGE);
					return;
				}
			}
		}
		
		// Validate conditions for NORMAL teleport.
		if (isNormalTeleport())
		{
			if (!PlayerConfig.TELEPORT_WHILE_SIEGE_IN_PROGRESS && npc.getCastle().getSiege().isInProgress())
			{
				final NpcHtmlMessage msg = new NpcHtmlMessage(npc.getObjectId());
				msg.setFile(player, "data/html/teleporter/castleteleporter-busy.htm");
				msg.replace("%npcname%", npc.getName());
				player.sendPacket(msg);
				return;
			}
			else if (!PlayerConfig.ALT_GAME_KARMA_PLAYER_CAN_USE_GK && (player.getReputation() < 0))
			{
				player.sendMessage("Go away, you're not welcome here.");
				return;
			}
			else if (player.isCombatFlagEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_TELEPORT_WHILE_IN_POSSESSION_OF_A_WARD);
				return;
			}
		}
		
		// Notify listeners.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_TELEPORT_REQUEST, npc))
		{
			final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnNpcTeleportRequest(player, npc, loc), npc, TerminateReturn.class);
			if ((term != null) && term.terminate())
			{
				return;
			}
		}
		
		// Check if player should pay fee.
		final int feeId = loc.getFeeId();
		if (shouldPayFee(player, loc) && !player.destroyItemByItemId(ItemProcessType.FEE, feeId, calculateFee(player, loc), npc, true))
		{
			if (feeId == Inventory.ADENA_ID)
			{
				player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
			}
			else
			{
				player.sendMessage("You do not have enough " + getItemName(feeId, true) + ".");
			}
			return;
		}
		
		player.teleToLocation(loc);
	}
	
	/**
	 * Determines if the {@code Player} must pay a fee to use a specific teleport.<br>
	 * It checks if the teleport is not normal or if the player exceeds the free level limit.<br>
	 * The check also considers active subclasses and the location's fee settings.
	 * @param player The {@code Player} attempting to teleport.
	 * @param loc The {@code TeleportLocation} being accessed.
	 * @return {@code true} if a fee is required, {@code false} otherwise.
	 */
	private boolean shouldPayFee(Player player, TeleportLocation loc)
	{
		return !isNormalTeleport() || (((player.getLevel() > PlayerConfig.MAX_FREE_TELEPORT_LEVEL) || player.isSubClassActive()) && ((loc.getFeeId() != 0) && (loc.getFeeCount() > 0)));
	}
	
	/**
	 * Calculates the cost required for a player to use a teleport.<br>
	 * It checks if the player qualifies for a free teleport based on their level and subclass.<br>
	 * It also applies a discount during specific hours on certain days of the week.
	 * @param player The {@code Player} attempting to teleport.
	 * @param loc The {@code TeleportLocation} being accessed.
	 * @return The total fee amount as a {@code long}.
	 */
	private long calculateFee(Player player, TeleportLocation loc)
	{
		if (isNormalTeleport())
		{
			if (!player.isSubClassActive() && (player.getLevel() <= PlayerConfig.MAX_FREE_TELEPORT_LEVEL))
			{
				return 0;
			}
			
			final Calendar cal = Calendar.getInstance();
			final int hour = cal.get(Calendar.HOUR_OF_DAY);
			final int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
			if ((hour >= 20) && ((dayOfWeek >= Calendar.MONDAY) && (dayOfWeek <= Calendar.TUESDAY)))
			{
				return loc.getFeeCount() / 2;
			}
		}
		
		return loc.getFeeCount();
	}
	
	/**
	 * Checks if the teleport type is considered a standard movement.<br>
	 * This returns {@code true} for both {@code NORMAL} and {@code HUNTING} types.
	 * @return {@code true} if it is a normal or hunting teleport, {@code false} otherwise.
	 */
	private boolean isNormalTeleport()
	{
		return (_type == TeleportType.NORMAL) || (_type == TeleportType.HUNTING);
	}
	
	/**
	 * Checks if the teleport type belongs to a Noblesse category.<br>
	 * It returns {@code true} for both {@code NOBLES_ADENA} and {@code NOBLES_TOKEN}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the teleport is a Noblesse type, {@code false} otherwise.
	 */
	public boolean isNoblesse()
	{
		return (_type == TeleportType.NOBLES_ADENA) || (_type == TeleportType.NOBLES_TOKEN);
	}
	
	/**
	 * Retrieves the display name for a specific item ID.<br>
	 * It handles special cases for currency and unique point types.
	 * @param itemId The unique identifier of the item to look up.
	 * @param fstring If {@code true}, it returns formatted strings for specific currencies.
	 * @return The name of the item as a {@code String}.
	 */
	private String getItemName(int itemId, boolean fstring)
	{
		if (fstring)
		{
			if (itemId == Inventory.ADENA_ID)
			{
				return "<fstring>1000308</fstring>";
			}
			else if (itemId == Inventory.ANCIENT_ADENA_ID)
			{
				return "<fstring>1000309</fstring>";
			}
		}
		
		final ItemTemplate item = ItemData.getInstance().getTemplate(itemId);
		if (item != null)
		{
			return item.getName();
		}
		
		final SpecialItemType specialItem = SpecialItemType.getByClientId(itemId);
		if (specialItem != null)
		{
			switch (specialItem)
			{
				case PC_CAFE_POINTS:
				{
					return "Player Commendation Points";
				}
				case CLAN_REPUTATION:
				{
					return "Clan Reputation Points";
				}
				case FAME:
				{
					return "Fame";
				}
				case FIELD_CYCLE_POINTS:
				{
					return "Field Cycle Points";
				}
				case RAIDBOSS_POINTS:
				{
					return "Raid Points";
				}
				case HONOR_COINS:
				{
					return "Honor Points";
				}
			}
		}
		
		return "Unknown item: " + itemId;
	}
}
