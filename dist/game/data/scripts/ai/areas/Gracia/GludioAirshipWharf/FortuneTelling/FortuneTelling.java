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
package ai.areas.Gracia.GludioAirshipWharf.FortuneTelling;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for the fortune telling script.<br>
 * It manages how NPCs interact with players during the fortune telling process.
 * @author Nyaran
 */
public class FortuneTelling extends Script
{
	// NPC
	private static final int MINE = 32616;
	
	// Misc
	private static final int COST = 1000;
	
	/**
	 * Initializes the {@code FortuneTelling} script.<br>
	 * Registers the required NPC and talk ID.
	 */
	public FortuneTelling()
	{
		addStartNpc(MINE);
		addTalkId(MINE);
	}
	
	/**
	 * Handles the interaction when a {@code Player} talks to an {@code Npc}.<br>
	 * It checks if the player has enough adena to perform fortune telling.<br>
	 * If they have enough, it deducts the cost and returns a random result.<br>
	 * Otherwise, it returns a message about low funds.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} entity initiating the conversation.
	 * @return A {@code String} containing the HTML content to display to the player.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (player.getAdena() < COST)
		{
			htmltext = "lowadena.htm";
		}
		else
		{
			takeItems(player, Inventory.ADENA_ID, COST);
			htmltext = getHtm(player, "fortune.htm").replace("%fortune%", String.valueOf(getRandom(1800309, 1800695)));
		}
		
		return htmltext;
	}
	
	/**
	 * This is the main entry point for the {@code FortuneTelling} script.<br>
	 * It initializes a new instance of the {@code FortuneTelling} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new FortuneTelling();
	}
}
