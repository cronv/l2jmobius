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
package ai.others;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles {@code Npc} entities that cannot be killed by players.<br>
 * It ensures these NPCs remain active and persistent within the game world.
 * @author UnAfraid
 */
public class NonLethalableNpcs extends Script
{
	private static final int[] NPCS =
	{
		35062, // Headquarters
	};
	
	/**
	 * Initializes a new instance of the {@code NonLethalableNpcs} class.<br>
	 * This constructor registers specific NPC IDs that cannot be killed.
	 */
	public NonLethalableNpcs()
	{
		addSpawnId(NPCS);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It prevents the NPC from being killed by players.<br>
	 * The {@code lethalable} property of the {@code npc} is set to {@code false}.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setLethalable(false);
	}
	
	/**
	 * This is the entry point for the {@code NonLethalableNpcs} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new NonLethalableNpcs();
	}
}
