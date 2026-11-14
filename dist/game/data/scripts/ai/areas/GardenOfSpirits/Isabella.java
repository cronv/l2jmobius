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
package ai.areas.GardenOfSpirits;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code Isabella} raid boss.<br>
 * It handles the specific combat logic and movements for this entity within the Garden of Spirits area.
 * @URL https://www.youtube.com/watch?v=3M73b6Kre6Y
 * @author Gigi, Mobius
 */
public class Isabella extends Script
{
	// NPC
	private static final int ISABELLA = 26131;
	private static final int CROA = 26132;
	private static final int AMIS = 26133;
	private static final int CROAMIS = 23563;
	
	// Doors
	private static final int DOOR1 = 18200001;
	private static final int DOOR2 = 18200002;
	
	// Location
	private static final Location CROAMIS_SPAWN_LOCATION = new Location(-51033, 82405, -4882, 44107);
	
	/**
	 * Initializes the {@code Isabella} script.<br>
	 * Registers the required attack, spawn, and kill IDs for the boss.
	 */
	public Isabella()
	{
		addAttackId(ISABELLA);
		addSpawnId(ISABELLA, CROAMIS);
		addKillId(ISABELLA, CROAMIS);
	}
	
	/**
	 * Handles specific events for the Isabella boss.<br>
	 * This method triggers minion spawns when a {@code SPAWN} event occurs.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return Always returns {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("SPAWN"))
		{
			final Npc minion1 = addSpawn(CROA, -51104, 83436, -5120, 61567, false, 300000, false);
			addAttackPlayerDesire(minion1, player);
			final Npc minion2 = addSpawn(AMIS, -50307, 83662, -5120, 45183, false, 300000, false);
			addAttackPlayerDesire(minion2, player);
			final Npc minion3 = addSpawn(CROA, -50259, 82825, -5120, 23684, false, 300000, false);
			addAttackPlayerDesire(minion3, player);
			final Npc minion4 = addSpawn(AMIS, -50183, 82901, -5120, 28180, false, 300000, false);
			addAttackPlayerDesire(minion4, player);
			final Npc minion5 = addSpawn(CROA, -50387, 83732, -5112, 45183, false, 300000, false);
			addAttackPlayerDesire(minion5, player);
			final Npc minion6 = addSpawn(AMIS, -51157, 83298, -5112, 64987, true, 300000, false);
			addAttackPlayerDesire(minion6, player);
		}
		
		return null;
	}
	
	/**
	 * Handles the logic triggered when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks the health of the {@code Npc} to trigger specific quest events.<br>
	 * It updates internal script values based on damage thresholds.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt in the attack.
	 * @param isSummon Whether the attack was performed by a summoned entity.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (!npc.isDead())
		{
			final int currentHpPercent = npc.getCurrentHpPercent();
			if ((currentHpPercent <= 80) && npc.isScriptValue(0))
			{
				startQuestTimer("SPAWN", 500, npc, attacker);
				npc.setScriptValue(1);
			}
			else if ((currentHpPercent <= 50) && npc.isScriptValue(1))
			{
				startQuestTimer("SPAWN", 500, npc, attacker);
				npc.setScriptValue(2);
			}
			else if ((currentHpPercent <= 10) && npc.isScriptValue(2))
			{
				startQuestTimer("SPAWN", 500, npc, attacker);
				npc.setScriptValue(3);
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It handles the initial setup for specific NPCs.<br>
	 * If the NPC is Isabella, it triggers a spawn event.<br>
	 * Otherwise, it moves the NPC to a fixed location and closes doors.
	 * @param npc The {@code Npc} instance that just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == ISABELLA)
		{
			addSpawn(CROAMIS, CROAMIS_SPAWN_LOCATION, false, 0);
		}
		else // CROAMIS
		{
			npc.setRandomWalking(false);
			npc.teleToLocation(CROAMIS_SPAWN_LOCATION); // No random spawn.
			closeDoor(DOOR1, 0);
			closeDoor(DOOR2, 0);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles specific logic for the boss and its minions.<br>
	 * If the boss dies, it removes nearby minions from the world.<br>
	 * If a different NPC dies, it opens specific doors.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (npc.getId() == ISABELLA)
		{
			World.forEachVisibleObjectInRange(npc, Monster.class, 1500, minion ->
			{
				if ((minion != null) && !minion.isAlikeDead() && ((minion.getId() == CROA) || (minion.getId() == AMIS)))
				{
					minion.deleteMe();
				}
			});
		}
		else // CROAMIS
		{
			openDoor(DOOR1, 0);
			openDoor(DOOR2, 0);
		}
	}
	
	/**
	 * This is the main entry point for the {@code Isabella} class.<br>
	 * It initializes a new instance of the {@code Isabella} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Isabella();
	}
}
