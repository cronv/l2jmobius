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
package ai.others.CastleTeleporter;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the artificial intelligence for the {@code CastleTeleporter} NPC.<br>
 * It manages the logic required to teleport players between different castle locations.
 * @author malyelfik, St3eT
 */
public class CastleTeleporter extends Script
{
	// NPCs
	private static final int[] MASS_TELEPORTERS =
	{
		35095, // Mass Gatekeeper (Gludio)
		35137, // Mass Gatekeeper (Dion)
		35179, // Mass Gatekeeper (Giran)
		35221, // Mass Gatekeeper (Oren)
		35266, // Mass Gatekeeper (Aden)
		35311, // Mass Gatekeeper (Innadril)
		35355, // Mass Gatekeeper (Goddard)
		35502, // Mass Gatekeeper (Rune)
		35547, // Mass Gatekeeper (Schuttgart)
	};
	// @formatter:off
	private static final int[] SIEGE_TELEPORTERS =
	{
		35092, 35093, 35094, // Gludio
		35134, 35135, 35136, // Dion
		35176, 35177, 35178, // Giran
		35218, 35219, 35220, // Oren
		35261, 35262, 35263, 35264, 35265, // Aden
		35308, 35309, 35310, // Innadril
		35352, 35353, 35354, // Goddard
		35497, 35498, 35499, 35500, 35501, // Rune
		35544, 35545, 35546, // Schuttgart
	};
	// @formatter:on
	
	/**
	 * Private constructor for the {@code CastleTeleporter} class.<br>
	 * This constructor initializes the NPC IDs and talk IDs required for the script.
	 */
	private CastleTeleporter()
	{
		addStartNpc(MASS_TELEPORTERS);
		addStartNpc(SIEGE_TELEPORTERS);
		addTalkId(MASS_TELEPORTERS);
		addTalkId(SIEGE_TELEPORTERS);
		addFirstTalkId(MASS_TELEPORTERS);
		addFirstTalkId(SIEGE_TELEPORTERS);
	}
	
	/**
	 * Handles specific events for the {@code CastleTeleporter} script.<br>
	 * It processes teleport requests and mass teleportation logic based on the event string.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The resulting event string or the result of the super class method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String action = st.nextToken();
		
		switch (action)
		{
			case "CastleTeleporter-06.html":
			{
				if (npc.isScriptValue(0))
				{
					final Siege siege = npc.getCastle().getSiege();
					final int time = (siege.isInProgress() && (siege.getControlTowerCount() == 0)) ? 480000 : 30000;
					startQuestTimer("MASS_TELEPORT", time, npc, null);
					npc.setScriptValue(1);
				}
				
				return event;
			}
			case "teleportMe":
			{
				if (st.hasMoreTokens())
				{
					final int unknowInt = Integer.parseInt(st.nextToken());
					final StatSet npcParams = npc.getParameters();
					Location teleLoc = null;
					switch (unknowInt)
					{
						case 0:
						{
							teleLoc = getTeleportLocation(npcParams, "01", "02", "03");
							break;
						}
						case 1:
						{
							teleLoc = getTeleportLocation(npcParams, "11", "12", "13");
							break;
						}
						case 2:
						{
							teleLoc = getTeleportLocation(npcParams, "21", "22", "23");
							break;
						}
						case 3:
						{
							teleLoc = getTeleportLocation(npcParams, "31", "32", "33");
							break;
						}
						case 4:
						{
							teleLoc = getTeleportLocation(npcParams, "41", "42", "43");
							break;
						}
						case 5:
						{
							if (isOwner(player, npc))
							{
								teleLoc = new Location(npcParams.getInt("pos_x51"), npcParams.getInt("pos_y51"), npcParams.getInt("pos_z51"));
							}
							else
							{
								return "CastleTeleporter-noAuthority.html";
							}
							break;
						}
					}
					
					if (teleLoc != null)
					{
						player.teleToLocation(teleLoc);
					}
				}
				break;
			}
			case "MASS_TELEPORT":
			{
				final int region = MapRegionData.getInstance().getMapRegionLocId(npc.getX(), npc.getY());
				final NpcSay msg = new NpcSay(npc, ChatType.NPC_SHOUT, NpcStringId.THE_DEFENDERS_OF_S1_CASTLE_WILL_BE_TELEPORTED_INSIDE);
				msg.addStringParameter(npc.getCastle().getName());
				npc.getCastle().oustAllPlayers();
				npc.setScriptValue(0);
				for (Player pl : World.getPlayers()) // TODO: Is it possible to get all the players for that region, instead of all players?
				{
					if (region == MapRegionData.getInstance().getMapRegionLocId(pl))
					{
						pl.sendPacket(msg);
					}
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Determines the correct HTML file to display when a player talks to an {@code Npc} for the first time.<br>
	 * This method checks if the {@code Npc} is a mass teleporter or if the {@code Player} is a defender.<br>
	 * It returns the filename based on the current siege state and ownership permissions.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} entity initiating the conversation.
	 * @return A {@code String} representing the name of the HTML file to be displayed.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (ArrayUtil.contains(MASS_TELEPORTERS, npc.getId()))
		{
			final Siege siege = npc.getCastle().getSiege();
			htmltext = npc.isScriptValue(0) ? (siege.isInProgress() && (siege.getControlTowerCount() == 0)) ? "CastleTeleporter-05.html" : "CastleTeleporter-04.html" : "CastleTeleporter-06.html";
		}
		else if ((npc.getCastle().getOwnerId() == player.getClanId()) && (player.getClanId() != 0) && (player.getSiegeState() == 2)) // Deffender
		{
			htmltext = getHtmlName(npc) + ".html";
		}
		else
		{
			htmltext = getHtmlName(npc) + "-no.html";
		}
		
		return htmltext;
	}
	
	/**
	 * Calculates a random teleport destination based on provided parameters.<br>
	 * It selects one of three possible locations using a weighted random chance.
	 * @param npcParams The {@code StatSet} containing the coordinate data.
	 * @param paramName1 The identifier for the first set of coordinates.
	 * @param paramName2 The identifier for the second set of coordinates.
	 * @param paramName3 The identifier for the third set of coordinates.
	 * @return A {@code Location} object representing the chosen destination.
	 */
	private Location getTeleportLocation(StatSet npcParams, String paramName1, String paramName2, String paramName3)
	{
		final Location loc;
		if (getRandom(100) < 33)
		{
			loc = new Location(npcParams.getInt("pos_x" + paramName1), npcParams.getInt("pos_y" + paramName1), npcParams.getInt("pos_z" + paramName1));
		}
		else if (getRandom(100) < 66)
		{
			loc = new Location(npcParams.getInt("pos_x" + paramName2), npcParams.getInt("pos_y" + paramName2), npcParams.getInt("pos_z" + paramName2));
		}
		else
		{
			loc = new Location(npcParams.getInt("pos_x" + paramName3), npcParams.getInt("pos_y" + paramName3), npcParams.getInt("pos_z" + paramName3));
		}
		
		return loc;
	}
	
	/**
	 * Retrieves the specific HTML name for a given {@code Npc}.<br>
	 * It maps certain NPC IDs to predefined teleporter names.<br>
	 * If no match is found, it returns the ID as a string.
	 * @param npc The {@code Npc} object to check.
	 * @return The HTML name or the NPC ID as a string.
	 */
	private String getHtmlName(Npc npc)
	{
		final int npcId = npc.getId();
		switch (npcId)
		{
			case 35092:
			case 35134:
			case 35176:
			case 35218:
			case 35308:
			case 35352:
			case 35544:
			{
				return "CastleTeleporter-01";
			}
			case 35093:
			case 35135:
			case 35177:
			case 35219:
			case 35309:
			case 35353:
			case 35545:
			{
				return "CastleTeleporter-02";
			}
			case 35094:
			case 35136:
			case 35178:
			case 35220:
			case 35310:
			case 35354:
			case 35546:
			{
				return "CastleTeleporter-03";
			}
		}
		
		return String.valueOf(npcId);
	}
	
	/**
	 * Checks if the {@code Player} has permission to use the {@code Npc}.<br>
	 * This returns {@code true} if the player is a GM.<br>
	 * It also returns {@code true} if the player is the leader of the clan that owns the castle.
	 * @param player The {@code Player} checking for permission.
	 * @param npc The {@code Npc} being interacted with.
	 * @return {@code true} if the player is an owner or GM, {@code false} otherwise.
	 */
	private boolean isOwner(Player player, Npc npc)
	{
		return player.isGM() || ((player.getClan() != null) && (player.getClanId() == npc.getCastle().getOwnerId()) && player.isClanLeader());
	}
	
	/**
	 * This is the main entry point for the {@code CastleTeleporter} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new CastleTeleporter();
	}
}
