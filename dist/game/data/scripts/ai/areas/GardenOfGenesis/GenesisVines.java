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
package ai.areas.GardenOfGenesis;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Handles the artificial intelligence for the {@code GenesisVines} entities.<br>
 * This script defines the behaviors and actions performed by these vines within the Garden of Genesis area.
 * @author St3eT
 */
public class GenesisVines extends Script
{
	// NPCs
	private static final int VINE = 18987; // Vine
	private static final int ROSE_VINE = 18988; // Rose Vine
	
	// Skills
	private static final SkillHolder VINE_SKILL = new SkillHolder(14092, 1);
	private static final SkillHolder ROSE_VINE_SKILL = new SkillHolder(14091, 1);
	
	/**
	 * Private constructor for the {@code GenesisVines} class.<br>
	 * This constructor initializes the script and registers the required spawn IDs.
	 */
	private GenesisVines()
	{
		addSpawnId(VINE, ROSE_VINE);
	}
	
	/**
	 * Handles specific events for the {@code GenesisVines} script.<br>
	 * It manages skill casting logic and NPC deletion based on event types.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class event handler.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("CAST_SKILL") && npc.isScriptValue(1))
		{
			final SkillHolder skill = npc.getId() == VINE ? VINE_SKILL : ROSE_VINE_SKILL;
			if (SkillCaster.checkUseConditions(npc, skill.getSkill()))
			{
				addSkillCastDesire(npc, npc, skill, 23);
			}
			
			startQuestTimer("CAST_SKILL", 3000, npc, null);
		}
		else if (event.equals("DELETE"))
		{
			npc.setScriptValue(0);
			npc.deleteMe();
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} first appears in the world.<br>
	 * It initializes the NPC by disabling its core AI.<br>
	 * It sets specific script values and starts new quest timers.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.disableCoreAI(true);
		npc.setScriptValue(1);
		cancelQuestTimer("CAST_SKILL", npc, null);
		cancelQuestTimer("DELETE", npc, null);
		startQuestTimer("CAST_SKILL", 3000, npc, null);
		startQuestTimer("DELETE", 150000, npc, null);
	}
	
	/**
	 * This is the main entry point for the {@code GenesisVines} class.<br>
	 * It initializes a new instance of the script to start the program.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new GenesisVines();
	}
}
