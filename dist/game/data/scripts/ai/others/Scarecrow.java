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
 * This class defines the artificial intelligence behavior for the {@code Scarecrow} NPC.<br>
 * It handles how the scarecrow interacts with players and other entities in the game world.
 * @author ivantotov
 */
public class Scarecrow extends Script
{
	// NPCs
	private static final int TRAINING_DUMMY = 19546;
	private static final int SCARECROW = 27457;
	
	/**
	 * Private constructor for the {@code Scarecrow} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Scarecrow()
	{
		addSpawnId(TRAINING_DUMMY, SCARECROW);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It disables the core AI for the entity.<br>
	 * It sets the entity to an immobilized state.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.disableCoreAI(true);
		npc.setImmobilized(true);
	}
	
	/**
	 * This is the entry point for the {@code Scarecrow} class.<br>
	 * It initializes a new instance of the {@code Scarecrow} object.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Scarecrow();
	}
}
