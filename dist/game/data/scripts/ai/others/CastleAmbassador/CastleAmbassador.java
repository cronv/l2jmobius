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
package ai.others.CastleAmbassador;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;

/**
 * This class handles the artificial intelligence for the {@code CastleAmbassador} NPC.<br>
 * It manages the behavior and logic of ambassadors during siege events.
 * @author St3eT
 */
public class CastleAmbassador extends Script
{
	// NPCs
	// @formatter:off
	private static final int[] CASTLE_AMBASSADOR =
	{
		36393, 36394, 36437, 36435, // Gludio
		36395, 36436, 36439, 36441, // Dion
		36396, 36440, 36444, 36449, 36451, // Giran
		36397, 36438, 36442, 36443, 36446, // Oren
		36398, 36399, 36445, 36448, // Aden
		36400, 36450, // Innadril
		36401, 36447, 36453, // Goddard
		36433, 36452, 36454, // Rune
		36434, 36455, // Schuttgart
	};
	// @formatter:on
	
	/**
	 * Private constructor for the {@code CastleAmbassador} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private CastleAmbassador()
	{
		addStartNpc(CASTLE_AMBASSADOR);
		addTalkId(CASTLE_AMBASSADOR);
		addFirstTalkId(CASTLE_AMBASSADOR);
		addEventReceivedId(CASTLE_AMBASSADOR);
		addSpawnId(CASTLE_AMBASSADOR);
	}
	
	/**
	 * Handles specific events for the Castle Ambassador NPC.<br>
	 * This method updates the fort state and manages quest timers based on player actions.<br>
	 * It returns the name of the HTML file to display to the player.
	 * @param event The type of event that occurred, such as "signed", "rejected", or "DESPAWN".
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} who interacted with the NPC.
	 * @return The name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		
		if (npc != null)
		{
			final Fort fortresss = npc.getFort();
			
			switch (event)
			{
				case "signed":
				{
					final int fortState = fortresss.getFortState();
					if (fortState == 0)
					{
						fortresss.setFortState(2, fortresss.getCastleIdByAmbassador(npc.getId()));
						cancelQuestTimer("DESPAWN", npc, null);
						startQuestTimer("DESPAWN", 3000, npc, null);
						htmltext = "ambassador-05.html";
					}
					else if (fortState == 1)
					{
						htmltext = "ambassador-04.html";
					}
					break;
				}
				case "rejected":
				{
					final int fortState = fortresss.getFortState();
					if (fortState == 0)
					{
						fortresss.setFortState(1, fortresss.getCastleIdByAmbassador(npc.getId()));
						cancelQuestTimer("DESPAWN", npc, null);
						startQuestTimer("DESPAWN", 3000, npc, null);
						htmltext = "ambassador-02.html";
					}
					else if (fortState == 2)
					{
						htmltext = "ambassador-02.html";
					}
					break;
				}
				case "DESPAWN":
				{
					if (fortresss.getFortState() == 0)
					{
						fortresss.setFortState(1, fortresss.getCastleIdByAmbassador(npc.getId()));
					}
					
					cancelQuestTimer("DESPAWN", npc, null);
					npc.broadcastEvent("DESPAWN", 1000, null);
					npc.deleteMe();
					break;
				}
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles events received between two {@code Npc} entities.<br>
	 * This method checks if the event is a {@code DESPAWN} and deletes the receiver if it exists.
	 * @param eventName The name of the event being triggered.
	 * @param sender The {@code Npc} that sent the event.
	 * @param receiver The {@code Npc} that received the event.
	 * @param reference The {@code WorldObject} associated with the event.
	 * @return The result of the base {@code onEventReceived} method.
	 */
	@Override
	public String onEventReceived(String eventName, Npc sender, Npc receiver, WorldObject reference)
	{
		if ((receiver != null) && eventName.equals("DESPAWN"))
		{
			receiver.deleteMe();
		}
		
		return super.onEventReceived(eventName, sender, receiver, reference);
	}
	
	/**
	 * Handles the initial interaction between a player and an ambassador.<br>
	 * It determines which HTML file to display based on clan ownership.<br>
	 * The result is formatted with the specific castle name.
	 * @param npc The {@code Npc} instance of the ambassador.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The formatted HTML string to be displayed to the player.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Fort fortresss = npc.getFort();
		final int fortOwner = fortresss.getOwnerClan() == null ? 0 : fortresss.getOwnerClan().getId();
		String htmltext = null;
		
		if (player.isClanLeader() && (player.getClan() != null) && (player.getClanId() == fortOwner))
		{
			htmltext = (fortresss.isBorderFortress()) ? "ambassador-01.html" : "ambassador.html";
		}
		else
		{
			htmltext = "ambassador-03.html";
		}
		
		htmltext = getHtm(player, htmltext);
		htmltext = htmltext.replace("%castleName%", fortresss.getCastleByAmbassador(npc.getId()).getName());
		return htmltext;
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It checks if the {@code Npc} belongs to a valid castle and fort.<br>
	 * If the requirements are not met, the {@code Npc} will be deleted.<br>
	 * Otherwise, it starts a despawn timer for the entity.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Fort fort = npc.getFort();
		if (fort == null)
		{
			npc.deleteMe();
			LOGGER.warning("Spawned: " + npc + " location: " + npc.getLocation() + " without fort!");
			return;
		}
		
		final Castle castle = fort.getCastleByAmbassador(npc.getId());
		if ((castle == null) || (castle.getOwnerId() == 0))
		{
			npc.deleteMe();
		}
		else
		{
			startQuestTimer("DESPAWN", 3600000, npc, null);
		}
	}
	
	/**
	 * This is the main entry point for the {@code CastleAmbassador} class.<br>
	 * It initializes a new instance of the script to start the application.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new CastleAmbassador();
	}
}
