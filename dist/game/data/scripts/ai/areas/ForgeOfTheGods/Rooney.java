/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package ai.areas.ForgeOfTheGods;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Defines the artificial intelligence behavior for the {@code Rooney} NPC.<br>
 * This class handles how the entity interacts with players and other creatures within its area.
 * @author malyelfik, Mobius
 */
public class Rooney extends Script
{
	// NPC
	private static final int ROONEY = 32049;
	
	// Locations
	private static final Location SPAWN_LOCATION = new Location(179680, -115472, -3613);
	private static final Location[] LOCATIONS =
	{
		new Location(179221, -115743, -3600),
		new Location(177668, -118775, -4080),
		new Location(179906, -108469, -5832),
		new Location(181285, -113798, -6064),
		new Location(181805, -108718, -5832),
		new Location(184131, -117511, -3336)
	};
	
	// Misc
	private static final int TELEPORT_DELAY = 60 * 60 * 1000; // Teleports 60 minutes after first seeing a player.
	private static final int SPEECH_DELAY = 60 * 1000;
	
	/**
	 * Initializes the {@code Rooney} script.<br>
	 * Registers the creature ID and sets up spawn locations.
	 */
	private Rooney()
	{
		addCreatureSeeId(ROONEY);
		addSpawn(ROONEY, SPAWN_LOCATION, false, 0);
	}
	
	/**
	 * Handles specific events for the {@code Rooney} NPC.<br>
	 * This method processes teleport logic and dialogue based on script values.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the event processing, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("TELEPORT") && !npc.isDecayed())
		{
			final int aiVal = npc.getScriptValue();
			switch (aiVal)
			{
				case 1:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.HURRY);
					break;
				}
				case 2:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.I_AM_NOT_THAT_TYPE_OF_PERSON_WHO_STAYS_IN_ONE_PLACE_FOR_A_LONG_TIME);
					break;
				}
				case 3:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.IT_S_HARD_FOR_ME_TO_KEEP_STANDING_LIKE_THIS);
					break;
				}
				case 4:
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WHY_DON_T_I_GO_THAT_WAY_THIS_TIME);
					break;
				}
				default:
				{
					npc.teleToLocation(getRandomEntry(LOCATIONS), false);
					npc.setScriptValue(0);
					return null;
				}
			}
			
			npc.setScriptValue(aiVal + 1);
			
			// Four speeches one minute apart, then the teleport at the 60th minute.
			startQuestTimer("TELEPORT", aiVal < 4 ? SPEECH_DELAY : TELEPORT_DELAY - (4 * SPEECH_DELAY), npc, null);
		}
		
		return null;
	}
	
	/**
	 * This method is called when the {@code Npc} sees a {@code Creature}.<br>
	 * It checks if the creature is a {@code Player} and if the NPC has a specific script value.<br>
	 * If both conditions are met, it welcomes the player and starts a teleport timer.
	 * @param npc The {@code Npc} instance that sees the creature.
	 * @param creature The {@code Creature} instance that was seen.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() && npc.isScriptValue(0))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.WELCOME);
			startQuestTimer("TELEPORT", SPEECH_DELAY, npc, null);
			npc.setScriptValue(1);
		}
	}
	
	/**
	 * This is the entry point for the {@code Rooney} class.<br>
	 * It initializes a new instance of the {@code Rooney} script.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Rooney();
	}
}
