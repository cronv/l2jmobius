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
package ai.areas.IsleOfSouls;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Defines the artificial intelligence behavior for NPCs in the Hills of Gold area.<br>
 * This {@code HillsOfGold} class handles specific script logic for entities within this zone.
 * @author Mobius
 */
public class HillsOfGold extends Script
{
	// NPCs
	private static final int GOLEM_OF_REPAIRS = 19309;
	private static final int EXCAVATOR_GOLEM = 19312;
	private static final int DRILL_GOLEM = 19310;
	private static final int SPICULA_1 = 23246;
	private static final int SPICULA_2 = 23247;
	private static final int YIN_FRAGMENT = 19308;
	private static final int SPICULA_ELITE_GUARD = 23303;
	private static final int[] GOLEMS =
	{
		23255,
		23257,
		23259,
		23261,
		23263,
		23264,
		23266,
		23267,
	};
	
	/**
	 * Initializes the {@code HillsOfGold} script.<br>
	 * Registers specific attack and spawn IDs for NPCs.<br>
	 * This sets up the behavior for various golems and fragments.
	 */
	public HillsOfGold()
	{
		addAttackId(YIN_FRAGMENT);
		addSpawnId(SPICULA_1, SPICULA_2);
		addSpawnId(GOLEMS);
	}
	
	/**
	 * Handles periodic timer events for the NPC.<br>
	 * This method checks if the {@code Npc} is alive and not in combat.<br>
	 * It identifies nearby specific golems to increase attack desire.<br>
	 * It also manages a recurring aggro timer for the entity.
	 * @param event The name of the timer event being triggered.
	 * @param params Additional parameters associated with the timer event.
	 * @param npc The {@code Npc} instance that owns this script.
	 * @param player The {@code Player} instance involved in the event, if any.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if ((npc != null) && !npc.isDead() && npc.isSpawned())
		{
			if (!npc.isInCombat())
			{
				World.forEachVisibleObjectInRange(npc, Monster.class, npc.getAggroRange(), nearby ->
				{
					final int nearbyId = nearby.getId();
					if ((nearbyId == GOLEM_OF_REPAIRS) || (nearbyId == EXCAVATOR_GOLEM) || (nearbyId == DRILL_GOLEM))
					{
						addAttackDesire(npc, nearby);
						return;
					}
				});
			}
			
			getTimers().addTimer("SPICULA_AGGRO_" + npc.getObjectId(), null, 10000, npc, null);
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method triggers specific events based on the damage dealt.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final int x = npc.getX();
		final int y = npc.getY();
		final int z = npc.getZ();
		final int heading = attacker.getHeading();
		final int respawnDelay = npc.getSpawn().getRespawnDelay();
		final Npc mob1 = addSpawn(SPICULA_ELITE_GUARD, x, y, z, heading + 32500, true, respawnDelay);
		addAttackDesire(mob1, attacker);
		final Npc mob2 = addSpawn(SPICULA_ELITE_GUARD, x, y, z, heading + 32500, true, respawnDelay);
		addAttackDesire(mob2, attacker);
		final Npc mob3 = addSpawn(SPICULA_ELITE_GUARD, x, y, z, heading + 32500, true, respawnDelay);
		addAttackDesire(mob3, attacker);
		final Npc mob4 = addSpawn(SPICULA_ELITE_GUARD, x, y, z, heading + 32500, true, respawnDelay);
		addAttackDesire(mob4, attacker);
		npc.deleteMe();
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It initializes specific behaviors based on the NPC ID.<br>
	 * Spicula types receive a new timer for aggro logic.<br>
	 * Other NPCs are assigned a display effect.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final int npcId = npc.getId();
		if ((npcId == SPICULA_1) || (npcId == SPICULA_2))
		{
			getTimers().addTimer("SPICULA_AGGRO_" + npc.getObjectId(), null, 5000, npc, null);
		}
		else
		{
			npc.setDisplayEffect(1);
		}
	}
	
	/**
	 * This is the entry point for the {@code HillsOfGold} script.<br>
	 * It initializes a new instance of the class to start the AI logic.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new HillsOfGold();
	}
}
