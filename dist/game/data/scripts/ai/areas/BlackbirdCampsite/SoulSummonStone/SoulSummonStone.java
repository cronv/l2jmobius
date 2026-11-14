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
package ai.areas.BlackbirdCampsite.SoulSummonStone;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class handles the artificial intelligence for the {@code SoulSummonStone} entity.<br>
 * It defines the behaviors and logic for this specific scriptable NPC.
 * @author NviX
 */
public class SoulSummonStone extends Script
{
	// NPCs
	private static final int SOUL_SUMMON_STONE = 34434;
	
	// Bosses
	private static final int SUMMONED_HARPAS = 26347;
	private static final int SUMMONED_GARP = 26348;
	private static final int SUMMONED_MORICKS = 26349;
	
	// Items
	private static final int SOUL_QUARTZ = 48536;
	
	// Misc
	private Npc _boss;
	
	/**
	 * Private constructor for the {@code SoulSummonStone} class.<br>
	 * It initializes the NPC IDs and script events.<br>
	 * This method is not intended to be called from outside this class.
	 */
	private SoulSummonStone()
	{
		addStartNpc(SOUL_SUMMON_STONE);
		addFirstTalkId(SOUL_SUMMON_STONE);
		addTalkId(SOUL_SUMMON_STONE);
		addKillId(SUMMONED_HARPAS, SUMMONED_GARP, SUMMONED_MORICKS);
	}
	
	/**
	 * Handles specific events for the Soul Summon Stone script.<br>
	 * It manages boss spawning logic based on player items and current state.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A {@code String} representing the HTML file to display or the result of the super method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "request_boss":
			{
				if ((_boss != null) && !_boss.isDead())
				{
					return "34434-04.html";
				}
				
				if (hasQuestItems(player, SOUL_QUARTZ))
				{
					takeItems(player, SOUL_QUARTZ, 1);
					int i = getRandom(100);
					if (i < 40)
					{
						_boss = addSpawn(SUMMONED_HARPAS, player.getX() + getRandom(-300, 300), player.getY() + getRandom(-300, 300), player.getZ() + 20, getRandom(64000), false, 0, true);
						return "34434-01.html";
					}
					else if (i < 80)
					{
						_boss = addSpawn(SUMMONED_GARP, player.getX() + getRandom(-300, 300), player.getY() + getRandom(-300, 300), player.getZ() + 20, getRandom(64000), false, 0, true);
						return "34434-02.html";
					}
					else
					{
						_boss = addSpawn(SUMMONED_MORICKS, player.getX() + getRandom(-300, 300), player.getY() + getRandom(-300, 300), player.getZ() + 20, getRandom(64000), false, 0, true);
						return "34434-03.html";
					}
				}
				
				return "34434-05.html";
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the interaction when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method returns the name of the HTML file to display.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the talk.
	 * @return A {@code String} representing the path to the dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "34434.html";
	}
	
	/**
	 * This is the entry point for the {@code SoulSummonStone} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new SoulSummonStone();
	}
}
