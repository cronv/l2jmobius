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
 * This class defines the artificial intelligence behavior for the {@code RagnaOrcCommander} NPC.<br>
 * It handles the specific actions and logic for this commander within the Den of Evil area.
 * @author Zealar, Mobius
 */
public class RagnaOrcCommander extends Script
{
	private static final int RAGNA_ORC_COMMANDER = 22694;
	
	/**
	 * Private constructor for the {@code RagnaOrcCommander} class.<br>
	 * This constructor initializes the spawn ID for the NPC.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private RagnaOrcCommander()
	{
		addSpawnId(RAGNA_ORC_COMMANDER);
	}
	
	/**
	 * This method is called when the {@code Npc} is first created.<br>
	 * It initializes the minions for the commander based on its parameters.<br>
	 * It adds specific groups of minions to the NPC's active list.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		for (MinionHolder minionHolder : npc.getParameters().getMinionList("Privates1"))
		{
			addMinion(npc.asMonster(), minionHolder.getId());
		}
		
		for (MinionHolder minionHolder : npc.getParameters().getMinionList(getRandomBoolean() ? "Privates2" : "Privates3"))
		{
			addMinion(npc.asMonster(), minionHolder.getId());
		}
	}
	
	/**
	 * This is the entry point for the {@code RagnaOrcCommander} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new RagnaOrcCommander();
	}
}
