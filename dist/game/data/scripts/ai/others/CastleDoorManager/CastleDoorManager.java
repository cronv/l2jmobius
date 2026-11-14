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
package ai.others.CastleDoorManager;

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * This class manages the artificial intelligence for castle doors.<br>
 * It handles door behaviors and interactions within the game world.
 * @author St3eT
 */
public class CastleDoorManager extends Script
{
	// NPCs
	// @formatter:off
	private static final int[] DOORMEN_OUTTER =
	{
		35096, // Gludio
		35138, // Dion
		35180, // Giran
		35222, // Oren
		35267, // Aden
		35312, // Innadril
		35356, // Goddard
		35503, // Rune
		35548, // Schuttgart
	};
	private static final int[] DOORMEN_INNER =
	{
		35097, // Gludio
		35139, // Dion
		35181, // Giran
		35223, // Oren
		35268, 35269, 35270, 35271, // Aden
		35313, // Innadril
		35357, 35358, 35359, 35360, // Goddard
		35504, 35505, // Rune
		35549, 35550, 35551, 35552, // Schuttgart
	};
	// @formatter:on
	
	/**
	 * Private constructor for the {@code CastleDoorManager} class.<br>
	 * This constructor initializes the required NPCs and talk IDs.<br>
	 * It registers both outer and inner doormen to the script.
	 */
	private CastleDoorManager()
	{
		addStartNpc(DOORMEN_OUTTER);
		addStartNpc(DOORMEN_INNER);
		addTalkId(DOORMEN_OUTTER);
		addTalkId(DOORMEN_INNER);
		addFirstTalkId(DOORMEN_OUTTER);
		addFirstTalkId(DOORMEN_INNER);
	}
	
	/**
	 * Handles specific events for the castle door management system.<br>
	 * This method processes actions like managing doors or teleporting players.<br>
	 * It checks if the {@code Player} belongs to the owning clan before executing commands.
	 * @param event The event string containing the action and parameters.
	 * @param npc The {@code Npc} entity triggering the event.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final StringTokenizer st = new StringTokenizer(event, " ");
		final String action = st.nextToken();
		String htmltext = null;
		switch (action)
		{
			case "manageDoors":
			{
				if (isOwningClan(player, npc) && st.hasMoreTokens())
				{
					if (npc.getCastle().getSiege().isInProgress())
					{
						htmltext = "CastleDoorManager-siege.html";
					}
					else
					{
						final Castle castle = npc.getCastle();
						final boolean open = st.nextToken().equals("1");
						final int doorId1 = npc.getParameters().getInt("DoorId1", 0);
						final int doorId2 = npc.getParameters().getInt("DoorId2", 0);
						castle.openCloseDoor(player, doorId1, open);
						castle.openCloseDoor(player, doorId2, open);
					}
				}
				else
				{
					htmltext = getHtmlName(npc) + "-no.html";
				}
				break;
			}
			case "teleport":
			{
				if (isOwningClan(player, npc) && st.hasMoreTokens())
				{
					final int param = Integer.parseInt(st.nextToken());
					if (param == 1)
					{
						final int x = npc.getParameters().getInt("pos_x01");
						final int y = npc.getParameters().getInt("pos_y01");
						final int z = npc.getParameters().getInt("pos_z01");
						player.teleToLocation(x, y, z);
					}
					else
					{
						final int x = npc.getParameters().getInt("pos_x02");
						final int y = npc.getParameters().getInt("pos_y02");
						final int z = npc.getParameters().getInt("pos_z02");
						player.teleToLocation(x, y, z);
					}
				}
				else
				{
					htmltext = getHtmlName(npc) + "-no.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the correct HTML file to display when a player talks to an {@code Npc} for the first time.<br>
	 * It checks if the {@code Player} belongs to the owning clan and has the required permissions.<br>
	 * If the requirements are met, it returns the standard name.<br>
	 * Otherwise, it returns the restricted version of the name.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A string representing the path to the HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return isOwningClan(player, npc) && player.hasAccess(ClanAccess.CASTLE_OPEN_DOOR) ? getHtmlName(npc) + ".html" : getHtmlName(npc) + "-no.html";
	}
	
	/**
	 * Determines the HTML name for a specific {@code Npc}.<br>
	 * It checks if the {@code Npc} ID is in the inner doorman list.<br>
	 * Returns "CastleDoorManager-Inner" or "CastleDoorManager-Outter".
	 * @param npc The {@code Npc} to check.
	 * @return A string representing the HTML name.
	 */
	private String getHtmlName(Npc npc)
	{
		return ArrayUtil.contains(DOORMEN_INNER, npc.getId()) ? "CastleDoorManager-Inner" : "CastleDoorManager-Outter";
	}
	
	/**
	 * Checks if a {@code Player} belongs to the clan that owns the castle of an {@code Npc}.<br>
	 * This check returns {@code true} if the player is a Game Master.<br>
	 * It also returns {@code true} if the player has a valid clan ID and it matches the owner ID.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} associated with the castle.
	 * @return {@code true} if the player is an owner or a Game Master, otherwise {@code false}.
	 */
	private boolean isOwningClan(Player player, Npc npc)
	{
		return player.isGM() || ((npc.getCastle().getOwnerId() == player.getClanId()) && (player.getClanId() != 0));
	}
	
	/**
	 * This is the entry point for the {@code CastleDoorManager} class.<br>
	 * It initializes a new instance of the manager to start the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new CastleDoorManager();
	}
}
