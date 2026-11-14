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
package ai.bosses.QueenShyeed;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.type.EffectZone;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class handles the artificial intelligence for the {@code QueenShyeed} boss.<br>
 * It manages the boss's behaviors and interactions within the game world.
 * @author malyelfik, Mobius
 */
public class QueenShyeed extends Script
{
	// NPC
	private static final int SHYEED = 25671;
	private static final Location SHYEED_LOC = new Location(79634, -55428, -6104, 0);
	
	// Respawn
	private static final int RESPAWN = 86400000; // 24 h
	private static final int RANDOM_RESPAWN = 43200000; // 12 h
	
	// Zones
	private static final EffectZone MOB_BUFF_ZONE = ZoneManager.getInstance().getZoneByName("22_16_stakato_mob_buff", EffectZone.class);
	private static final EffectZone MOB_BUFF_DISPLAY_ZONE = ZoneManager.getInstance().getZoneByName("22_16_stakato_mob_buff_display", EffectZone.class);
	private static final EffectZone PC_BUFF_ZONE = ZoneManager.getInstance().getZoneByName("22_16_stakato_pc_buff", EffectZone.class);
	
	/**
	 * Private constructor for the {@code QueenShyeed} class.<br>
	 * It initializes the NPC by adding its kill ID and spawning it.
	 */
	private QueenShyeed()
	{
		addKillId(SHYEED);
		spawnShyeed();
	}
	
	/**
	 * Handles specific events for the {@code QueenShyeed} script.<br>
	 * It manages the respawn and despawn logic for the NPC.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return Always returns {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "respawn":
			{
				spawnShyeed();
				break;
			}
			case "despawn":
			{
				if (!npc.isDead())
				{
					startRespawn();
					npc.deleteMe();
				}
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the logic for restarting the respawn timer.<br>
	 * It also enables specific zone effects and broadcasts a message.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was triggered by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.SHYEED_S_CRY_IS_STEADILY_DYING_DOWN);
		startRespawn();
		PC_BUFF_ZONE.setEnabled(true);
	}
	
	/**
	 * Handles the spawning logic for Queen Shyeed.<br>
	 * Checks if the respawn timer has expired before creating the {@code Npc}.<br>
	 * Updates the status of various {@code EffectZone} instances during spawn.
	 */
	private void spawnShyeed()
	{
		final long respawn = GlobalVariablesManager.getInstance().getLong("QueenShyeedRespawn", 0);
		final long remain = respawn != 0 ? respawn - System.currentTimeMillis() : 0;
		if (remain > 0)
		{
			PC_BUFF_ZONE.setEnabled(true);
			startQuestTimer("respawn", remain, null, null);
			return;
		}
		
		final Npc npc = addSpawn(SHYEED, SHYEED_LOC, false, 0);
		startQuestTimer("despawn", 10800000, npc, null);
		PC_BUFF_ZONE.setEnabled(false);
		
		MOB_BUFF_ZONE.setEnabled(true);
		MOB_BUFF_DISPLAY_ZONE.setEnabled(true);
	}
	
	/**
	 * Initializes the timer for the next boss appearance.<br>
	 * Calculates a random respawn time based on {@code RESPAWN} and {@code RANDOM_RESPAWN}.<br>
	 * Updates the global variable {@code QueenShyeedRespawn} with the new timestamp.<br>
	 * Disables the {@code MOB_BUFF_ZONE} and {@code MOB_BUFF_DISPLAY_ZONE} effects.
	 */
	private void startRespawn()
	{
		final int respawnTime = RESPAWN - getRandom(RANDOM_RESPAWN);
		GlobalVariablesManager.getInstance().set("QueenShyeedRespawn", Long.toString(System.currentTimeMillis() + respawnTime));
		startQuestTimer("respawn", respawnTime, null, null);
		
		MOB_BUFF_ZONE.setEnabled(false);
		MOB_BUFF_DISPLAY_ZONE.setEnabled(false);
	}
	
	/**
	 * This is the entry point for the {@code QueenShyeed} class.<br>
	 * It initializes a new instance of the {@code QueenShyeed} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new QueenShyeed();
	}
}
