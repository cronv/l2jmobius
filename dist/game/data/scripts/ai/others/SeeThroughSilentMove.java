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
 * This class implements an AI script for NPCs that can see through silent moves.<br>
 * It allows entities to detect and react to players moving while using the {@code SilentMove} skill.
 * @author Gigiikun
 */
public class SeeThroughSilentMove extends Script
{
	// @formatter:off
	private static final int[] MONSTERS =
	{
		18001, 22215, 22216, 22217, /*22327,*/ 22746, 22747, 22748,
		22749, 22750, 22751, 22752, 22753, 22754, 22755, 22756, 22757, 22758,
		22759, 22760, 22761, 22762, 22763, 22764, 22765, 22794, 22795, 22796,
		22797, 22798, 22799, 22800, 22843, 22857, 25725, 25726, 25727, 29009,
		29010, 29011, 29012, 29013,
		19253, // Zellaka (Solo 85)
		25882, // Zellaka (Group 85)
		19254, // Pelline (Solo 90)
		25883, // Pelline (Group 90)
		19255, // Kalios (Solo 95)
		25884, // Kalios (Group 95)
	};
	// @formatter:on
	
	/**
	 * Initializes the script with specific monster IDs.<br>
	 * This method registers the {@code MONSTERS} array to the spawn system.
	 */
	private SeeThroughSilentMove()
	{
		addSpawnId(MONSTERS);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the {@code Npc} can be attacked.<br>
	 * If it is attackable, it enables the see through silent move property.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.isAttackable())
		{
			npc.asAttackable().setSeeThroughSilentMove(true);
		}
	}
	
	/**
	 * This is the entry point for the {@code SeeThroughSilentMove} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new SeeThroughSilentMove();
	}
}
