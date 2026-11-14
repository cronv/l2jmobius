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
package ai.areas.Gracia.SeedOfHellfire;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the artificial intelligence behavior for the {@code Zofan} NPC.<br>
 * It handles the logic and actions performed by this entity within the Seed of Hellfire area.
 * @author St3eT
 */
public class Zofan extends Script
{
	// NPCs
	private static final int[] ZOFAN =
	{
		23215, // Zofan
		23216, // Zofan
		23227, // Beggar Zofan
		23229, // Zofan
		23237, // Engineer Zofan
	};
	
	// Misc
	private static final String[] MINION_PARAMS =
	{
		"i_adult1_sil",
		"i_adult2_sil",
		"i_adult3_sil",
		"i_child1_sil",
		"i_child2_sil",
		"i_child3_sil",
		"i_child4_sil",
		"i_child5_sil",
		"i_child6_sil",
	};
	
	/**
	 * Private constructor for the {@code Zofan} class.<br>
	 * It initializes the spawn IDs for all {@code Zofan} variants.
	 */
	private Zofan()
	{
		addSpawnId(ZOFAN);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the NPC has a valid spawn location and no world instance.<br>
	 * It also checks for specific parameters to determine if minions should be added.
	 * @param npc The {@code Npc} object that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if ((npc.getInstanceWorld() == null) && (npc.getSpawn() != null))
		{
			final StatSet params = npc.getParameters();
			if (params.getInt("i_childrengarden_guard", 0) == 0)
			{
				for (String param : MINION_PARAMS)
				{
					if (params.getInt(param, -1) != -1)
					{
						addMinion(npc.asMonster(), params.getInt(param));
					}
				}
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code Zofan} class.<br>
	 * It initializes a new instance of the {@code Zofan} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Zofan();
	}
}
