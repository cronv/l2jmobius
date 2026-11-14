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
 * Defines the artificial intelligence behavior for the Ragna Orc Hero.<br>
 * This class handles the specific actions and logic for this NPC within the {@code DenOfEvil} area.
 * @author Zealar, Mobius
 */
public class RagnaOrcHero extends Script
{
	private static final int RAGNA_ORC_HERO = 22693;
	
	/**
	 * Private constructor for the {@code RagnaOrcHero} class.<br>
	 * This constructor initializes the spawn ID for this script.<br>
	 * It prevents other classes from creating new instances of this class.
	 */
	private RagnaOrcHero()
	{
		addSpawnId(RAGNA_ORC_HERO);
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It initializes the minions for the NPC based on its parameters.<br>
	 * It selects a random group of minions to add to the entity.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		for (MinionHolder minionHolder : npc.getParameters().getMinionList(getRandom(100) < 70 ? "Privates1" : "Privates2"))
		{
			addMinion(npc.asMonster(), minionHolder.getId());
		}
	}
	
	/**
	 * This is the main entry point for the {@code RagnaOrcHero} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new RagnaOrcHero();
	}
}
