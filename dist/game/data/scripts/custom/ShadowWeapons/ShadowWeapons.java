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
package custom.ShadowWeapons;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for {@code ShadowWeapons}.<br>
 * It defines the behaviors and actions performed by these entities.
 * @author Nyaran, jurchiks
 */
public class ShadowWeapons extends Script
{
	// @formatter:off
	private static final int[] NPCS =
	{
		30037, 30066, 30070, 30109, 30115, 30120, 30174, 30175, 30176, 30187,
		30191, 30195, 30288, 30289, 30290, 30297, 30373, 30462, 30474, 30498,
		30499, 30500, 30503, 30504, 30505, 30511, 30512, 30513, 30595, 30676,
		30677, 30681, 30685, 30687, 30689, 30694, 30699, 30704, 30845, 30847,
		30849, 30854, 30857, 30862, 30865, 30894, 30897, 30900, 30905, 30910,
		30913, 31269, 31272, 31276, 31285, 31288, 31314, 31317, 31321, 31324,
		31326, 31328, 31331, 31334, 31336, 31958, 31961, 31965, 31968, 31974,
		31977, 31996, 32092, 32093, 32094, 32095, 32096, 32097, 32098, 32193,
		32196, 32199, 32202, 32205, 32206, 32213, 32214, 32221, 32222, 32229,
		32230, 32233, 32234
	};
	// @formatter:on
	/**
	 * Private constructor for the {@code ShadowWeapons} class.<br>
	 * This constructor initializes the script by registering NPCs and talk IDs.
	 */
	private ShadowWeapons()
	{
		addStartNpc(NPCS);
		addTalkId(NPCS);
	}
	
	/**
	 * Handles specific events for the {@code ShadowWeapons} script.<br>
	 * It checks the provided {@code event} name to determine which action to take.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} entity involved in the event.
	 * @param player The {@code Player} entity involved in the event.
	 * @return The HTML text associated with the event or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "ShadowCoupon.html":
			{
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the interaction when a player talks to an {@code Npc}.<br>
	 * It checks for specific quest items and determines which HTML file to display.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the talk.
	 * @return The name of the HTML file to be displayed to the player.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext;
		final boolean hasD = hasQuestItems(player, 8869); // Shadow Item Exchange Coupon (D-Grade)
		final boolean hasC = hasQuestItems(player, 8870); // Shadow Item Exchange Coupon (C-Grade)
		
		if (hasD || hasC)
		{
			if (!hasD)
			{
				htmltext = "exchange_c.html";
			}
			else if (!hasC)
			{
				htmltext = "exchange_d.html";
			}
			else
			{
				htmltext = "exchange_both.html";
			}
		}
		else
		{
			htmltext = "exchange_no.html";
		}
		
		return htmltext;
	}
	
	/**
	 * This is the entry point for the {@code ShadowWeapons} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new ShadowWeapons();
	}
}
