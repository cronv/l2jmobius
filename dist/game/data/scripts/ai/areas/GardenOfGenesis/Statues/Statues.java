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
package ai.areas.GardenOfGenesis.Statues;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class handles the artificial intelligence for statues in the Garden of Genesis.<br>
 * It defines the behaviors and logic specific to these entities within the game world.
 * @author St3eT
 */
public class Statues extends Script
{
	// NPCs
	private static final int STATUE_1 = 33138; // Genesis Angel Statue
	private static final int STATUE_2 = 33139; // Fountain of Genesis
	private static final int STATUE_3 = 33140; // Genesis Goddess Statue
	private static final int STATUE_KEEPER_1 = 23038; // Angel Statue Keeper
	private static final int STATUE_KEEPER_2 = 23039; // Fountain Keeper
	private static final int STATUE_KEEPER_3 = 23040; // Goddess Statue Keeper
	private static final int BUFF_NPC = 19073; // Genesis Transparent
	
	// Skills
	private static final SkillHolder REWARD_BUFF = new SkillHolder(14200, 1); // Blessing of Garden
	// Locations
	private static final Location LOC_1 = new Location(210049, 119367, -1352);
	private static final Location LOC_2 = new Location(217785, 110621, -1344);
	private static final Location LOC_3 = new Location(217792, 118844, -1760);
	
	/**
	 * Initializes the {@code Statues} script.<br>
	 * Registers all required NPCs and interaction IDs.<br>
	 * Sets up the starting conditions for the statues.
	 */
	private Statues()
	{
		addStartNpc(STATUE_1, STATUE_2, STATUE_3);
		addFirstTalkId(STATUE_1, STATUE_2, STATUE_3);
		addTalkId(STATUE_1, STATUE_2, STATUE_3);
		addKillId(STATUE_KEEPER_1, STATUE_KEEPER_2, STATUE_KEEPER_3);
	}
	
	/**
	 * Handles specific events for the statues.<br>
	 * This method manages combat triggers and quest timers.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} who triggered the event.
	 * @return A string containing HTML text or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.equals("fight"))
		{
			if (npc.isScriptValue(0))
			{
				Location loc = null;
				int npcId = -1;
				switch (npc.getId())
				{
					case STATUE_1:
					{
						loc = LOC_1;
						npcId = STATUE_KEEPER_1;
						break;
					}
					case STATUE_2:
					{
						loc = LOC_2;
						npcId = STATUE_KEEPER_2;
						break;
					}
					case STATUE_3:
					{
						loc = LOC_3;
						npcId = STATUE_KEEPER_3;
						break;
					}
				}
				
				if ((loc != null) && (npcId != -1))
				{
					npc.setScriptValue(1);
					startQuestTimer("FUNCTION_REUSE", 900000, npc, null);
					final Npc keeper = addSpawn(npcId, loc, false, 900000);
					addAttackPlayerDesire(keeper, player);
				}
			}
			else
			{
				htmltext = npc.getId() + "-no.html";
			}
		}
		else if (event.equals("FUNCTION_REUSE"))
		{
			npc.setScriptValue(0);
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the dialogue for a player's first interaction with an {@code Npc}.<br>
	 * It returns the filename of the specific NPC's dialogue.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the logic for spawning a reward NPC.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who killed the entity.
	 * @param isSummon Whether the killed entity was a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Npc buffNpc = addSpawn(BUFF_NPC, npc, false, 5000);
		buffNpc.setInvul(true);
		addSkillCastDesire(buffNpc, buffNpc, REWARD_BUFF, 23);
	}
	
	/**
	 * This is the entry point for the {@code Statues} class.<br>
	 * It initializes a new instance of the {@code Statues} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Statues();
	}
}
