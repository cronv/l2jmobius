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
package ai.areas.TalkingIsland;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code TrainingGolem}.<br>
 * It handles the logic for how this NPC interacts with players in its designated area.
 * @author Gladicek
 */
public class TrainingGolem extends Script
{
	// NPCs
	private static final int TRAINING_GOLEM = 27532;
	
	/**
	 * Private constructor for the {@code TrainingGolem} class.<br>
	 * This prevents other classes from creating new instances of this script.<br>
	 * It initializes the spawn ID for the training golem.
	 */
	private TrainingGolem()
	{
		addSpawnId(TRAINING_GOLEM);
	}
	
	/**
	 * This method is called when a new {@code Npc} enters the world.<br>
	 * It sets the state of the {@code npc} to immobilized.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setImmobilized(true);
	}
	
	/**
	 * This is the entry point for the {@code TrainingGolem} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new TrainingGolem();
	}
}
