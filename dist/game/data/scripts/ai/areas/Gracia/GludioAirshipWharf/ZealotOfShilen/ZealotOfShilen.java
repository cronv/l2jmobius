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
package ai.areas.Gracia.GludioAirshipWharf.ZealotOfShilen;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Defines the artificial intelligence behavior for the {@code ZealotOfShilen} NPC.<br>
 * This class handles how these enemies interact with players and other entities in their area.
 * @author nonom, Mobius
 */
public class ZealotOfShilen extends Script
{
	// NPCs
	private static final int ZEALOT = 18782;
	private static final int[] GUARDS =
	{
		32628,
		32629
	};
	
	/**
	 * Initializes the {@code ZealotOfShilen} script.<br>
	 * Registers spawn IDs for both zealots and guards.<br>
	 * Sets up the first talk ID for guard NPCs.
	 */
	public ZealotOfShilen()
	{
		addSpawnId(ZEALOT);
		addSpawnId(GUARDS);
		addFirstTalkId(GUARDS);
	}
	
	/**
	 * Handles specific events for the {@code ZealotOfShilen} script.<br>
	 * This method checks if the {@code Npc} should attack a nearby {@code Monster}.<br>
	 * It updates the {@code Npc} state to running and sets its intention to attack.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc == null)
		{
			return null;
		}
		
		if (!npc.isAttackingNow() && !npc.isAlikeDead())
		{
			final Monster nearby = World.getNearestVisibleObject(npc, Monster.class, m -> !m.isDead() && !m.isDecayed());
			if (nearby != null)
			{
				npc.setRunning();
				npc.asAttackable().addDamageHate(nearby, 0, 999);
				npc.getAI().setIntentionAttack(nearby);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Determines the dialogue file to show when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * It checks if the {@code Npc} is currently attacking.<br>
	 * If it is attacking, it returns a specific combat dialogue.<br>
	 * Otherwise, it returns the default dialogue based on the {@code Npc} ID.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the name of the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return (npc.isAttackingNow()) ? "32628-01.html" : npc.getId() + ".html";
	}
	
	/**
	 * This method is called when an {@code Npc} is spawned.<br>
	 * It configures the behavior of the NPC based on its ID.<br>
	 * Specific settings are applied to handle walking, invulnerability, and quest timers.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == ZEALOT)
		{
			npc.setRandomWalking(false);
		}
		else
		{
			npc.setInvul(true);
			npc.asAttackable().setCanReturnToSpawnPoint(false);
			cancelQuestTimer("WATCHING", npc, null);
			startQuestTimer("WATCHING", 10000, npc, null, true);
		}
	}
	
	/**
	 * This is the main entry point for the {@code ZealotOfShilen} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new ZealotOfShilen();
	}
}
