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
package ai.others.Spawns;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.spawns.SpawnGroup;
import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This script prevents an {@code Npc} from performing any random activities.<br>
 * It ensures that the entity remains stationary or follows its primary logic without interruptions.
 * @author UnAfraid
 */
public class NoRandomActivity extends Script
{
	/**
	 * Private constructor for the {@code NoRandomActivity} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private NoRandomActivity()
	{
	}
	
	/**
	 * This method handles the logic when an {@code Npc} is spawned.<br>
	 * It configures random animations and walking based on the {@code SpawnTemplate}.<br>
	 * The settings are applied to both the {@code Npc} and its associated {@code Spawn}.
	 * @param template The {@code SpawnTemplate} containing the configuration parameters.
	 * @param group The {@code SpawnGroup} that the NPC belongs to.
	 * @param npc The {@code Npc} instance being spawned.
	 */
	@Override
	public void onSpawnNpc(SpawnTemplate template, SpawnGroup group, Npc npc)
	{
		final StatSet parameters = template.getParameters();
		if (parameters == null)
		{
			return;
		}
		
		npc.setRandomAnimation(!parameters.getBoolean("disableRandomAnimation", false));
		final boolean randomWalk = !parameters.getBoolean("disableRandomWalk", false);
		npc.setRandomWalking(randomWalk);
		
		final Spawn spawn = npc.getSpawn();
		if (spawn == null)
		{
			return;
		}
		
		spawn.setRandomWalking(randomWalk);
	}
	
	/**
	 * This is the entry point for the {@code NoRandomActivity} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new NoRandomActivity();
	}
}
