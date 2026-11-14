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
package ai.areas.FairySettlement;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Defines the artificial intelligence behavior for the {@code Wisp} entity.<br>
 * This class handles how these creatures interact with players and their environment within the Fairy Settlement area.
 * @author St3eT
 */
public class Wisp extends Script
{
	// NPCs
	private static final int WISP = 32915;
	private static final int LARGE_WISP = 32916;
	
	// Skills
	private static final SkillHolder WISP_HEAL = new SkillHolder(14064, 1);
	private static final SkillHolder LARGE_WISP_HEAL = new SkillHolder(14065, 1);
	
	// Misc
	private static final int RESPAWN_MIN = 60000;
	private static final int RESPAWN_MAX = 120000;
	
	/**
	 * Private constructor for the {@code Wisp} class.<br>
	 * This constructor initializes the required spawn and vision IDs.
	 */
	private Wisp()
	{
		addSpawnId(WISP);
		addCreatureSeeId(WISP, LARGE_WISP);
	}
	
	/**
	 * Handles periodic timer events for the {@code Wisp} script.<br>
	 * It manages NPC deletion and respawn logic based on the event type.
	 * @param event The name of the timer event to process.
	 * @param params A {@code StatSet} containing data like location objects.
	 * @param npc The {@code Npc} instance associated with the timer.
	 * @param player The {@code Player} instance involved in the event.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.equals("DELETE_NPC"))
		{
			final StatSet statSet = new StatSet();
			statSet.set("LOCATION_OBJECT", npc.getLocation());
			getTimers().addTimer("RESPAWN_WISP_" + npc.getObjectId(), statSet, getRandom(RESPAWN_MIN, RESPAWN_MAX), null, null);
			npc.deleteMe();
		}
		else
		{
			addSpawn(WISP, params.getObject("LOCATION_OBJECT", Location.class));
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It stops the respawn timer for the current spawn point.<br>
	 * It also handles the logic to occasionally replace a {@code WISP} with a {@code LARGE_WISP}.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Spawn spawn = npc.getSpawn();
		spawn.stopRespawn();
		if ((npc.getId() == WISP) && (getRandom(100) < 10))
		{
			addSpawn(LARGE_WISP, npc);
			npc.deleteMe();
		}
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It checks if the creature is a player or a fake player.<br>
	 * If true, the NPC targets the creature and casts a healing skill.<br>
	 * The NPC then schedules its own removal after 5000 milliseconds.
	 * @param npc The {@code Npc} that sees the target.
	 * @param creature The {@code Creature} being seen by the NPC.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() || creature.isFakePlayer())
		{
			npc.setTarget(creature);
			npc.doCast(npc.getId() == WISP ? WISP_HEAL.getSkill() : LARGE_WISP_HEAL.getSkill());
			getTimers().addTimer("DELETE_NPC", 5000, npc, null);
		}
	}
	
	/**
	 * This is the entry point for the {@code Wisp} class.<br>
	 * It initializes a new instance of the {@code Wisp} object.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Wisp();
	}
}
