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
package ai.areas.CrumaTower;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class handles the artificial intelligence for the {@code CrumaTower} area.<br>
 * It defines specific behaviors and logic for NPCs within this zone.
 * @author malyelfik
 */
public class CrumaTower extends Script
{
	// NPCs
	private static final int CARSUS = 30483;
	private static final int TELEPORT_DEVICE = 33157;
	
	/**
	 * Initializes the {@code CrumaTower} script.<br>
	 * This constructor registers the required spawn and attack IDs.
	 */
	public CrumaTower()
	{
		addSpawnId(CARSUS);
		addAttackId(TELEPORT_DEVICE);
	}
	
	/**
	 * Handles specific events triggered by the {@code CrumaTower} script.<br>
	 * This method processes logic for interactions between an {@code Npc} and a {@code Player}.<br>
	 * It checks if the {@code event} matches specific criteria to trigger dialogue or timers.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("MESSAGE") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.USE_THE_ELEVATOR_TO_GO_TO_THE_UNDERGROUND_S_THIRD_FLOOR);
			startQuestTimer(event, 15000, npc, player);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} first appears in the world.<br>
	 * It initializes the quest timer for the NPC.
	 * @param npc The {@code Npc} instance that has just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("MESSAGE", 15000, npc, null);
	}
	
	/**
	 * This method handles the logic when a creature takes damage.<br>
	 * It checks if the target is an {@code Npc} with a specific teleport parameter.<br>
	 * If valid, it teleports the attacker to that location.
	 * @param event The {@code OnCreatureDamageReceived} event containing the target and attacker details.
	 */
	@RegisterEvent(EventType.ON_CREATURE_DAMAGE_RECEIVED)
	@RegisterType(ListenerRegisterType.NPC)
	@Id(TELEPORT_DEVICE)
	public void onCreatureDamageReceived(OnCreatureDamageReceived event)
	{
		try
		{
			final Npc npc = event.getTarget().asNpc();
			final int[] location = npc.getParameters().getIntArray("teleport", ";");
			event.getAttacker().teleToLocation(location[0], location[1], location[2]);
		}
		catch (Exception e)
		{
			LOGGER.warning("Invalid location for Cruma Tower teleport device.");
		}
	}
	
	/**
	 * This is the main entry point for the {@code CrumaTower} script.<br>
	 * It initializes a new instance of the {@code CrumaTower} class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CrumaTower();
	}
}
