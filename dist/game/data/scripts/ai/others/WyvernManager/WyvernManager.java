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
package ai.others.WyvernManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * This class manages the logic and behavior for {@code Wyvern} entities.<br>
 * It handles various interactions related to wyverns within the game world.
 * @author xban1x, Mobius
 */
public class WyvernManager extends Script
{
	private enum ManagerType
	{
		CASTLE,
		CLAN_HALL,
		AGIT,
		FORT,
	}
	
	// Misc
	private static final int CRYSTAL_B_GRADE = 1460;
	private static final int WYVERN = 12621;
	private static final int WYVERN_FEE = 25;
	private static final int STRIDER_LEVEL = 55;
	private static final int[] STRIDERS =
	{
		12526,
		12527,
		12528,
		16038,
		16039,
		16040,
		16068,
		13197
	};
	
	// NPCS
	private static final Map<Integer, ManagerType> MANAGERS = new HashMap<>();
	static
	{
		MANAGERS.put(35101, ManagerType.CASTLE);
		MANAGERS.put(35143, ManagerType.CASTLE);
		MANAGERS.put(35185, ManagerType.CASTLE);
		MANAGERS.put(35227, ManagerType.CASTLE);
		MANAGERS.put(35275, ManagerType.CASTLE);
		MANAGERS.put(35317, ManagerType.CASTLE);
		MANAGERS.put(35364, ManagerType.CASTLE);
		MANAGERS.put(35510, ManagerType.CASTLE);
		MANAGERS.put(35536, ManagerType.CASTLE);
		MANAGERS.put(35556, ManagerType.CASTLE);
		MANAGERS.put(35419, ManagerType.CLAN_HALL);
		MANAGERS.put(35638, ManagerType.CLAN_HALL);
		
		// Aden clan hall gatekeepers, reached from their own dialog.
		MANAGERS.put(35440, ManagerType.AGIT);
		MANAGERS.put(35442, ManagerType.AGIT);
		MANAGERS.put(35444, ManagerType.AGIT);
		MANAGERS.put(35446, ManagerType.AGIT);
		MANAGERS.put(35448, ManagerType.AGIT);
		MANAGERS.put(35450, ManagerType.AGIT);
		MANAGERS.put(36457, ManagerType.FORT);
		MANAGERS.put(36458, ManagerType.FORT);
		MANAGERS.put(36459, ManagerType.FORT);
		MANAGERS.put(36460, ManagerType.FORT);
		MANAGERS.put(36461, ManagerType.FORT);
		MANAGERS.put(36462, ManagerType.FORT);
		MANAGERS.put(36463, ManagerType.FORT);
		MANAGERS.put(36464, ManagerType.FORT);
		MANAGERS.put(36465, ManagerType.FORT);
		MANAGERS.put(36466, ManagerType.FORT);
		MANAGERS.put(36467, ManagerType.FORT);
		MANAGERS.put(36468, ManagerType.FORT);
		MANAGERS.put(36469, ManagerType.FORT);
		MANAGERS.put(36470, ManagerType.FORT);
		MANAGERS.put(36471, ManagerType.FORT);
		MANAGERS.put(36472, ManagerType.FORT);
		MANAGERS.put(36473, ManagerType.FORT);
		MANAGERS.put(36474, ManagerType.FORT);
		MANAGERS.put(36475, ManagerType.FORT);
		MANAGERS.put(36476, ManagerType.FORT);
		MANAGERS.put(36477, ManagerType.FORT);
	}
	
	/**
	 * Private constructor for the {@code WyvernManager} class.<br>
	 * This prevents other classes from creating new instances of this manager.<br>
	 * It initializes the required NPC and talk IDs during class loading.
	 */
	private WyvernManager()
	{
		final Set<Integer> managerKeys = MANAGERS.keySet();
		addStartNpc(managerKeys);
		addTalkId(managerKeys);
		for (Entry<Integer, ManagerType> manager : MANAGERS.entrySet())
		{
			if (manager.getValue() != ManagerType.AGIT)
			{
				addFirstTalkId(manager.getKey());
			}
		}
	}
	
	/**
	 * Handles the logic for a player to mount a wyvern.<br>
	 * It checks if the player is currently riding a valid strider and has enough crystals.<br>
	 * If requirements are met, it removes the old mount and provides the new one.
	 * @param npc The {@code Npc} interacting with the player.
	 * @param player The {@code Player} attempting to mount the wyvern.
	 * @return A string representing the HTML file to display to the player.
	 */
	private String mountWyvern(Npc npc, Player player)
	{
		if (player.isMounted() && (player.getMountLevel() >= STRIDER_LEVEL) && ArrayUtil.contains(STRIDERS, player.getMountNpcId()))
		{
			if (isOwnerClan(npc, player) && (getQuestItemsCount(player, CRYSTAL_B_GRADE) >= WYVERN_FEE))
			{
				takeItems(player, CRYSTAL_B_GRADE, WYVERN_FEE);
				player.dismount();
				player.mount(WYVERN, 0, true);
				
				return "wyvernmanager-04.html";
			}
			
			return replacePart(player, "wyvernmanager-06.html");
		}
		
		if (player.getMountType() != MountType.STRIDER)
		{
			player.sendPacket(SystemMessageId.YOU_MAY_ONLY_RIDE_A_WYVERN_WHILE_YOU_ARE_RIDING_A_STRIDER);
		}
		
		return replacePart(player, "wyvernmanager-05.html");
	}
	
	/**
	 * Checks if the {@code Player} is the leader of the clan that owns the area managed by the {@code Npc}.<br>
	 * This method validates ownership for castles, clan halls, and forts.
	 * @param npc The {@code Npc} representing the manager or location.
	 * @param player The {@code Player} attempting to access the area.
	 * @return {@code true} if the player is the leader of the owning clan; {@code false} otherwise.
	 */
	private boolean isOwnerClan(Npc npc, Player player)
	{
		final Clan clan = player.getClan();
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				if ((clan != null) && (npc.getCastle() != null))
				{
					return (player.isClanLeader() && (player.getClanId() == npc.getCastle().getOwnerId()));
				}
				
				return false;
			}
			case CLAN_HALL:
			{
				if ((clan != null) && (npc.getClanHall() != null))
				{
					return (player.isClanLeader() && (player.getClanId() == npc.getClanHall().getOwnerId()));
				}
				
				return false;
			}
			case FORT:
			{
				final Fort fort = npc.getFort();
				if ((clan != null) && (fort != null) && (fort.getOwnerClan() != null))
				{
					return (player.isClanLeader() && (player.getClanId() == npc.getFort().getOwnerClan().getId()));
				}
				
				return false;
			}
			case AGIT:
			{
				final ClanHall clanHall = npc.getClanHall();
				return (clanHall != null) && (player.getClanId() == clanHall.getOwnerId());
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Checks if the given {@code Npc} is currently located in an active siege zone.<br>
	 * It determines this based on whether the {@code Castle} or {@code Fort} associated with the NPC is active.
	 * @param npc The {@code Npc} to check.
	 * @return {@code true} if the NPC is in an active siege, otherwise {@code false}.
	 */
	private boolean isInSiege(Npc npc)
	{
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				return npc.getCastle().getZone().isActive();
			}
			case FORT:
			{
				return npc.getFort().getZone().isActive();
			}
			default:
			{
				return false;
			}
		}
	}
	
	/**
	 * Retrieves the name of the residence associated with an {@code Npc}.<br>
	 * It checks if the {@code Npc} belongs to a castle, clan hall, or fort.
	 * @param npc The {@code Npc} object to check.
	 * @return The name of the residence as a {@code String}, or {@code null} if no residence is found.
	 */
	private String getResidenceName(Npc npc)
	{
		final ManagerType type = MANAGERS.get(npc.getId());
		switch (type)
		{
			case CASTLE:
			{
				return npc.getCastle().getName();
			}
			case CLAN_HALL:
			{
				return npc.getClanHall().getName();
			}
			case FORT:
			{
				return npc.getFort().getName();
			}
			case AGIT:
			{
				return npc.getClanHall().getName();
			}
			default:
			{
				return null;
			}
		}
	}
	
	/**
	 * This method generates a formatted HTML string for the player.<br>
	 * It replaces specific placeholders with the residence name of the {@code Npc}.
	 * @param npc The {@code Npc} entity associated with the residence.
	 * @param player The {@code Player} receiving the message.
	 * @return A formatted string containing the final HTML content.
	 */
	private String replaceAll(Npc npc, Player player)
	{
		return replacePart(player, "wyvernmanager-01.html").replace("%residence_name%", getResidenceName(npc));
	}
	
	/**
	 * Replaces specific placeholders in an HTML template with constant values.<br>
	 * It replaces {@code %wyvern_fee%} and {@code %strider_level%} with their respective numeric values.
	 * @param player The {@code Player} object associated with the action.
	 * @param htmlFile The path to the HTML file template to be processed.
	 * @return A formatted string containing the final HTML content.
	 */
	private String replacePart(Player player, String htmlFile)
	{
		return getHtm(player, htmlFile).replace("%wyvern_fee%", String.valueOf(WYVERN_FEE)).replace("%strider_level%", String.valueOf(STRIDER_LEVEL));
	}
	
	/**
	 * Handles specific interaction events for the Wyvern Manager.<br>
	 * This method determines which HTML response to return based on the event type and player permissions.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} entity involved in the interaction.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The HTML content to display or {@code null} if the action is denied.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "Return":
			{
				if (!isOwnerClan(npc, player))
				{
					htmltext = "wyvernmanager-02.html";
				}
				else if (FeatureConfig.ALLOW_WYVERN_ALWAYS)
				{
					htmltext = replaceAll(npc, player);
				}
				else
				{
					final ManagerType type = MANAGERS.get(npc.getId());
					if (type == ManagerType.CASTLE)
					{
						htmltext = "wyvernmanager-dusk.html";
					}
					else
					{
						htmltext = replaceAll(npc, player);
					}
				}
				break;
			}
			case "Help":
			{
				final ManagerType type = MANAGERS.get(npc.getId());
				htmltext = type == ManagerType.CASTLE ? replacePart(player, "wyvernmanager-03.html") : replacePart(player, "wyvernmanager-03b.html");
				break;
			}
			case "RideWyvern":
			{
				if (!FeatureConfig.ALLOW_WYVERN_ALWAYS)
				{
					// If in siege and config blocks wyvern riding, allow only castle lord.
					if (!FeatureConfig.ALLOW_WYVERN_DURING_SIEGE && player.isClanLeader() && (player.isInSiege() || isInSiege(npc)))
					{
						// Determine if the player is the castle lord: must be clan leader and own the castle.
						final Castle castle = CastleManager.getInstance().getCastle(npc);
						final boolean isCastleLord = (castle != null) && (player.getClanId() == castle.getOwnerId());
						if (!isCastleLord)
						{
							player.sendMessage("You cannot summon a wyvern during a siege.");
							return null;
						}
					}
					
					final ManagerType type = MANAGERS.get(npc.getId());
					if (type == ManagerType.CASTLE)
					{
						htmltext = "wyvernmanager-dusk.html";
					}
					else
					{
						htmltext = mountWyvern(npc, player);
					}
				}
				else
				{
					htmltext = mountWyvern(npc, player);
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the correct HTML file to display when a player talks to an NPC for the first time.<br>
	 * It checks ownership and configuration settings to decide which message to show.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return The name of the HTML file to be displayed as a String.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (!isOwnerClan(npc, player))
		{
			htmltext = "wyvernmanager-02.html";
		}
		else if (FeatureConfig.ALLOW_WYVERN_ALWAYS)
		{
			htmltext = replaceAll(npc, player);
		}
		else
		{
			final ManagerType type = MANAGERS.get(npc.getId());
			if (type == ManagerType.CASTLE)
			{
				htmltext = "wyvernmanager-dusk.html";
			}
			else
			{
				htmltext = replaceAll(npc, player);
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This is the entry point for the {@code WyvernManager} class.<br>
	 * It initializes a new instance of the manager to start the system.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new WyvernManager();
	}
}
