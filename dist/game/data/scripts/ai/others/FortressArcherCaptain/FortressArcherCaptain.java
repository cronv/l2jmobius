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
package ai.others.FortressArcherCaptain;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence for the {@code FortressArcherCaptain} NPC.<br>
 * It handles the behavior and logic for this specific unit within the game world.
 * @author St3eT
 */
public class FortressArcherCaptain extends Script
{
	// NPCs
	private static final int[] ARCHER_CAPTAIN =
	{
		35661, // Shanty Fortress
		35692, // Southern Fortress
		35730, // Hive Fortress
		35761, // Valley Fortress
		35799, // Ivory Fortress
		35830, // Narsell Fortress
		35861, // Bayou Fortress
		35899, // White Sands Fortress
		35930, // Borderland Fortress
		35968, // Swamp Fortress
		36006, // Archaic Fortress
		36037, // Floran Fortress
		36075, // Cloud Mountain
		36113, // Tanor Fortress
		36144, // Dragonspine Fortress
		36175, // Antharas's Fortress
		36213, // Western Fortress
		36251, // Hunter's Fortress
		36289, // Aaru Fortress
		36320, // Demon Fortress
		36358, // Monastic Fortress
	};
	
	/**
	 * Initializes the {@code FortressArcherCaptain} script.<br>
	 * Registers the required NPC IDs for starting and talking.
	 */
	private FortressArcherCaptain()
	{
		addStartNpc(ARCHER_CAPTAIN);
		addFirstTalkId(ARCHER_CAPTAIN);
	}
	
	/**
	 * Determines the correct dialogue file to show when a player talks to an {@code Npc} for the first time.<br>
	 * It checks if the {@code Player} belongs to the clan that owns the fortress.<br>
	 * If they are in the owner clan, it returns the specific owner message.<br>
	 * Otherwise, it returns the default greeting.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the name of the HTML dialogue file to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final int fortOwner = npc.getFort().getOwnerClan() == null ? 0 : npc.getFort().getOwnerClan().getId();
		return ((player.getClan() != null) && (player.getClanId() == fortOwner)) ? "FortressArcherCaptain.html" : "FortressArcherCaptain-01.html";
	}
	
	/**
	 * This is the entry point for the {@code FortressArcherCaptain} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new FortressArcherCaptain();
	}
}
