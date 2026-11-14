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
package ai.bosses.Freya.Sirra;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for the boss {@code Sirra}.<br>
 * It defines the behaviors and logic for this specific NPC script.
 * @author St3eT
 */
public class Sirra extends Script
{
	// NPC
	private static final int SIRRA = 32762;
	
	// Misc
	private static final int FREYA_INSTID = 139;
	private static final int FREYA_HARD_INSTID = 144;
	
	/**
	 * Initializes the {@code Sirra} instance.<br>
	 * Registers the first talk ID for this NPC.
	 */
	private Sirra()
	{
		addFirstTalkId(SIRRA);
	}
	
	/**
	 * Handles the initial interaction when a {@code Player} talks to an {@code Npc}.<br>
	 * It determines which dialogue file to show based on the current instance.<br>
	 * The method checks if the world is in a specific state to choose between easy or hard content.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the name of the HTML dialogue file to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			final int templateId = world.getTemplateId();
			if ((templateId == FREYA_INSTID))
			{
				return (world.isStatus(0)) ? "32762-easy.html" : "32762-easyfight.html";
			}
			else if ((templateId == FREYA_HARD_INSTID))
			{
				return (world.isStatus(0)) ? "32762-hard.html" : "32762-hardfight.html";
			}
		}
		
		return "32762.html";
	}
	
	/**
	 * This is the entry point for the {@code Sirra} class.<br>
	 * It initializes a new instance of the {@code Sirra} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Sirra();
	}
}
