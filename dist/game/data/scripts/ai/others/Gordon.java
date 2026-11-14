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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code Gordon} NPC.<br>
 * It handles the specific logic and actions performed by this entity within the game world.
 * @author TOFIZ, malyelfik
 */
public class Gordon extends Script
{
	private static final int GORDON = 29095;
	
	/**
	 * Private constructor for the {@code Gordon} class.<br>
	 * This constructor initializes the required spawn and creature see IDs.
	 */
	private Gordon()
	{
		addSpawnId(GORDON);
		addCreatureSeeId(GORDON);
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It checks if the {@code Creature} is a player with a cursed weapon.<br>
	 * If both conditions are met, it increases the {@code Npc} desire to attack that player.
	 * @param npc The {@code Npc} entity that performs the action.
	 * @param creature The {@code Creature} entity that was spotted.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (creature.isPlayer() && creature.asPlayer().isCursedWeaponEquipped())
		{
			addAttackPlayerDesire(npc, creature.asPlayer());
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It prevents the NPC from returning to its spawn point.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.asAttackable().setCanReturnToSpawnPoint(false);
	}
	
	/**
	 * This is the entry point for the {@code Gordon} class.<br>
	 * It initializes a new instance of the {@code Gordon} object.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Gordon();
	}
}
