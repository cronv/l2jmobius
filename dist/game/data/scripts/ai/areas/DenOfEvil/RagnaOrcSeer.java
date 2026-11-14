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
package ai.areas.DenOfEvil;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class defines the artificial intelligence behavior for the {@code RagnaOrcSeer} NPC.<br>
 * It handles the specific actions and logic for this entity within the {@code DenOfEvil} area.
 * @author Zealar, Mobius
 */
public class RagnaOrcSeer extends Script
{
	private static final int RAGNA_ORC_SEER = 22697;
	
	/**
	 * Private constructor for the {@code RagnaOrcSeer} class.<br>
	 * It initializes the spawn ID for this NPC script.
	 */
	private RagnaOrcSeer()
	{
		addSpawnId(RAGNA_ORC_SEER);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It initializes the minions for the spawned entity.<br>
	 * It selects a random group of minions based on the NPC's parameters.
	 * @param npc The {@code Npc} instance that was just created.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		for (MinionHolder minionHolder : npc.getParameters().getMinionList("Privates" + getRandom(1, 2)))
		{
			addMinion(npc.asMonster(), minionHolder.getId());
		}
	}
	
	/**
	 * This is the entry point for the {@code RagnaOrcSeer} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new RagnaOrcSeer();
	}
}
