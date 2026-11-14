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
package ai.bosses.Lindvior;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;

/**
 * This class handles the artificial intelligence for the {@code Vortex} boss.<br>
 * It defines the behaviors and logic used by this specific NPC during combat.
 * @author Gigi
 * @date 2017-07-23 - [10:32:50]
 */
public class Vortex extends Script
{
	private static final int SMALL_VORTEX = 25898;
	private static final int BIG_VORTEX = 19427;
	
	/**
	 * Initializes a new instance of the {@code Vortex} class.<br>
	 * This constructor registers the required spawn and creature see IDs.<br>
	 * It sets up the initial state for the AI script.
	 */
	public Vortex()
	{
		super();
		addSpawnId(SMALL_VORTEX, BIG_VORTEX);
		addCreatureSeeId(SMALL_VORTEX, BIG_VORTEX);
	}
	
	/**
	 * Handles specific events for the Vortex AI script.<br>
	 * This method processes logic for small and big vortex effects.<br>
	 * It manages player movement, health reduction, and NPC despawning.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance triggering the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "rnd_small":
			{
				World.forEachVisibleObjectInRange(npc, Player.class, 250, attackers ->
				{
					if ((attackers != null) && !attackers.isDead() && !attackers.isAlikeDead())
					{
						attackers.getAI().setIntentionIdle();
						final int radians = (int) Math.toRadians(npc.calculateDirectionTo(attackers));
						final int x = (int) (attackers.getX() + (600 * Math.cos(radians)));
						final int y = (int) (attackers.getY() + (600 * Math.sin(radians)));
						final int z = attackers.getZ();
						final Location loc = GeoEngine.getInstance().getValidLocation(attackers.getX(), attackers.getY(), attackers.getZ(), x, y, z, attackers.getInstanceWorld());
						attackers.broadcastPacket(new FlyToLocation(attackers, x, y, z, FlyType.THROW_UP, 800, 800, 800));
						attackers.setXYZ(loc);
						attackers.broadcastPacket(new ValidateLocation(attackers));
						npc.getAI().setIntentionAttack(player);
						startQuestTimer("stop_knock_down", 5000, npc, attackers);
						startQuestTimer("despawn_small", 5000, npc, null);
					}
				});
				break;
			}
			case "rnd_big":
			{
				World.forEachVisibleObjectInRange(npc, Player.class, 500, attackers ->
				{
					if ((attackers != null) && !attackers.isDead() && !attackers.isAlikeDead())
					{
						attackers.setCurrentHp(attackers.getMaxHp() * 0.2);
						attackers.setCurrentMp(attackers.getMaxMp() * 0.2);
						attackers.setCurrentCp(1.0);
						startQuestTimer("despawn_big", 60000, npc, null);
					}
				});
				break;
			}
			case "despawn_small":
			{
				if (npc != null)
				{
					cancelQuestTimers("rnd_small");
					npc.getSpawn().stopRespawn();
					npc.doDie(null);
				}
				break;
			}
			case "despawn_big":
			{
				if (npc != null)
				{
					cancelQuestTimers("despawn_big");
					npc.getSpawn().stopRespawn();
					npc.deleteMe();
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It triggers specific quest timers based on the NPC's ID.<br>
	 * The behavior differs for {@code SMALL_VORTEX} and {@code BIG_VORTEX} types.
	 * @param npc The {@code Npc} that spotted the creature.
	 * @param creature The {@code Creature} that was seen.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		switch (npc.getId())
		{
			case SMALL_VORTEX:
			{
				startQuestTimer("rnd_small", 5000, npc, null, true);
				break;
			}
			case BIG_VORTEX:
			{
				startQuestTimer("rnd_big", 10000, npc, null, true);
				break;
			}
		}
	}
	
	/**
	 * This method is called when the {@code Npc} first appears in the world.<br>
	 * It triggers an initial attack and sets the movement state.<br>
	 * The {@code Npc} will start running and walking randomly.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		attackRandomTarget(npc);
		npc.setRandomWalking(true);
		npc.setRunning();
	}
	
	/**
	 * This method finds a visible {@code Player} near the {@code Npc}.<br>
	 * It adds an attack desire to the {@code Npc} for each player found.
	 * @param npc The {@code Npc} that will perform the action.
	 */
	private void attackRandomTarget(Npc npc)
	{
		World.forFirstVisibleObject(npc, Player.class, p -> addAttackPlayerDesire(npc, p));
	}
	
	/**
	 * This is the entry point for the {@code Vortex} class.<br>
	 * It initializes a new instance of the {@code Vortex} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Vortex();
	}
}
