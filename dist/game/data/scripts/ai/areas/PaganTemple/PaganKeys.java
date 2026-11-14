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
package ai.areas.PaganTemple;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class is a custom {@code Script} used to remove specific key drops from NPC XML files.<br>
 * It allows for more convenient access to the Pagan Temple area.
 * @author Notorion
 */
public class PaganKeys extends Script
{
	// Item
	private static final int KEY_OF_PAGANS = 36247;
	
	// NPC
	private static final int RESURRECTED_WORKER = 22140;
	
	// Chance
	private static final int DROP_CHANCE = 55;
	
	/**
	 * Private constructor for the {@code PaganKeys} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private PaganKeys()
	{
		addKillId(RESURRECTED_WORKER);
	}
	
	/**
	 * Handles logic when an {@code Npc} is killed.<br>
	 * This method determines if a special key should be dropped based on the NPC type.<br>
	 * It checks for specific drop chances and handles item distribution to the {@code Player}.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who killed the entity.
	 * @param isSummon Indicates if the kill was caused by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (getRandom(100) < DROP_CHANCE)
		{
			if (PlayerConfig.AUTO_LOOT)
			{
				giveItems(killer, KEY_OF_PAGANS, 1);
			}
			else
			{
				npc.dropItem(killer, KEY_OF_PAGANS, 1);
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code PaganKeys} class.<br>
	 * It initializes a new instance of the script to run in the game.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new PaganKeys();
	}
}
