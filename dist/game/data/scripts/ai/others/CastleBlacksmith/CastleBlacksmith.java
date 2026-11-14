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
package ai.others.CastleBlacksmith;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for the {@code CastleBlacksmith} NPC.<br>
 * It defines the behaviors and logic for this specific script.
 * @author malyelfik
 */
public class CastleBlacksmith extends Script
{
	// Blacksmith IDs
	private static final int[] NPCS =
	{
		35098, // Blacksmith (Gludio)
		35140, // Blacksmith (Dion)
		35182, // Blacksmith (Giran)
		35224, // Blacksmith (Oren)
		35272, // Blacksmith (Aden)
		35314, // Blacksmith (Innadril)
		35361, // Blacksmith (Goddard)
		35507, // Blacksmith (Rune)
		35553, // Blacksmith (Schuttgart)
	};
	
	/**
	 * Private constructor for the {@code CastleBlacksmith} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private CastleBlacksmith()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
		addFirstTalkId(NPCS);
	}
	
	/**
	 * Checks if a {@code Player} has permission to interact with an {@code Npc}.<br>
	 * This includes checking for administrator status, clan leadership, or specific castle access.
	 * @param player The {@code Player} attempting the action.
	 * @param npc The {@code Npc} being interacted with.
	 * @return {@code true} if the player has the required rights; {@code false} otherwise.
	 */
	private boolean hasRights(Player player, Npc npc)
	{
		final boolean isMyLord = player.isClanLeader() && (player.getClan().getCastleId() == (npc.getCastle() != null ? npc.getCastle().getResidenceId() : -1));
		return player.isGM() || isMyLord || ((player.getClanId() == npc.getCastle().getOwnerId()) && player.hasAccess(ClanAccess.CASTLE_MANOR));
	}
	
	/**
	 * Handles specific events triggered by the {@code CastleBlacksmith}.<br>
	 * It checks if the player has the required rights for the action.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return The event string if successful, or {@code null} otherwise.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		return (event.equalsIgnoreCase(npc.getId() + "-02.html") && hasRights(player, npc)) ? event : null;
	}
	
	/**
	 * Determines the initial dialogue for a player talking to an {@code Npc}.<br>
	 * It checks if the {@code Player} has the required rights.<br>
	 * If they have rights, it returns a specific HTML file based on the NPC ID.<br>
	 * Otherwise, it returns "no.html".
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A string representing the path to the dialogue HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return (hasRights(player, npc)) ? npc.getId() + "-01.html" : "no.html";
	}
	
	/**
	 * This is the entry point for the {@code CastleBlacksmith} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new CastleBlacksmith();
	}
}
