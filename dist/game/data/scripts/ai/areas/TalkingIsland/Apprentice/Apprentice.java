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
package ai.areas.TalkingIsland.Apprentice;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class defines the artificial intelligence behavior for the {@code Apprentice} NPC.<br>
 * It handles the logic and interactions for this specific entity on Talking Island.
 * @author St3eT
 */
public class Apprentice extends Script
{
	// NPCs
	private static final int APPRENTICE = 33124;
	
	// Skill
	private static final SkillHolder KUKURU = new SkillHolder(9204, 1); // Kukuru
	
	/**
	 * Private constructor for the {@code Apprentice} class.<br>
	 * This constructor initializes the required IDs for the script.<br>
	 * It registers spawn, start, and talk identifiers.
	 */
	private Apprentice()
	{
		addSpawnId(APPRENTICE);
		addStartNpc(APPRENTICE);
		addTalkId(APPRENTICE);
		addFirstTalkId(APPRENTICE);
	}
	
	/**
	 * Handles specific events for the apprentice NPC.<br>
	 * This method processes actions like riding a Kukuru or displaying text.<br>
	 * It also updates quest progress when required conditions are met.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("rideKukuru"))
		{
			if (!player.isTransformed())
			{
				KUKURU.getSkill().applyEffects(npc, player);
			}
			else
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_CAN_T_RIDE_KUKURU_NOW);
			}
		}
		else if (event.equals("SPAM_TEXT") && (npc != null))
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.TRY_RIDING_A_KUKURU, 1000);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when the {@code Npc} first appears in the world.<br>
	 * It initializes the quest timer for the NPC.
	 * @param npc The {@code Npc} instance that has just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("SPAM_TEXT", 12000, npc, null, true);
	}
	
	/**
	 * Handles the dialogue when a {@code Player} speaks to an {@code Npc} for the first time.<br>
	 * This method determines which HTML file to display based on the NPC's unique ID.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the dialogue HTML file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code Apprentice} class.<br>
	 * It initializes a new instance of the {@code Apprentice} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Apprentice();
	}
}
