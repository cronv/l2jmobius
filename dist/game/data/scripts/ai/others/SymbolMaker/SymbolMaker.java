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
package ai.others.SymbolMaker;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.HennaEquipList;
import org.l2jmobius.gameserver.network.serverpackets.HennaRemoveList;

/**
 * This class handles the artificial intelligence for the {@code SymbolMaker} NPC.<br>
 * It manages the logic required to create and display symbols within the game world.
 * @author Adry_85
 */
public class SymbolMaker extends Script
{
	// NPCs
	private static final int[] NPCS =
	{
		31046, // Marsden
		31047, // Kell
		31048, // McDermott
		31049, // Pepper
		31050, // Thora
		31051, // Keach
		31052, // Heid
		31053, // Kidder
		31264, // Olsun
		31308, // Achim
		31953, // Rankar
	};
	
	/**
	 * Private constructor for the {@code SymbolMaker} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private SymbolMaker()
	{
		addFirstTalkId(NPCS);
		addStartNpc(NPCS);
		addTalkId(NPCS);
	}
	
	/**
	 * Handles specific events for the Symbol Maker script.<br>
	 * It processes HTML requests and henna actions.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The HTML content string or null if no text is returned.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "symbol_maker.htm":
			case "symbol_maker-1.htm":
			case "symbol_maker-2.htm":
			case "symbol_maker-3.htm":
			{
				htmltext = event;
				break;
			}
			case "Draw":
			{
				player.sendPacket(new HennaEquipList(player));
				break;
			}
			case "Remove":
			{
				player.sendPacket(new HennaRemoveList(player));
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method is triggered when a player speaks to the NPC for the first time.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return The name of the HTML file to display to the player.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "symbol_maker.htm";
	}
	
	/**
	 * This is the entry point for the {@code SymbolMaker} application.<br>
	 * It initializes a new instance of the {@code SymbolMaker} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new SymbolMaker();
	}
}
