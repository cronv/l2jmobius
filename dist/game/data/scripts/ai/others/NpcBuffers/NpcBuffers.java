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
package ai.others.NpcBuffers;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * This class manages buffer logic for {@code Npc} entities.<br>
 * It handles specific script behaviors related to NPC interactions and state management.
 * @author UnAfraid
 */
public class NpcBuffers extends Script
{
	private final NpcBuffersData _npcBuffers = new NpcBuffersData();
	
	/**
	 * Private constructor for the {@code NpcBuffers} class.<br>
	 * This constructor initializes the script and registers all necessary NPC IDs.<br>
	 * It populates the internal buffers used by the {@code Script} logic.
	 */
	private NpcBuffers()
	{
		for (int npcId : _npcBuffers.getNpcBufferIds())
		{
			// TODO: Cleanup once npc rework is finished and default html is configurable.
			addFirstTalkId(npcId);
			addSpawnId(npcId);
		}
	}
	
	// TODO: Cleanup once npc rework is finished and default html is configurable.
	/**
	 * This method handles the initial interaction between an {@code Npc} and a {@code Player}.<br>
	 * It is triggered when a player speaks to the NPC for the first time.
	 * @param npc The {@code Npc} instance that is being spoken to.
	 * @param player The {@code Player} instance who initiated the conversation.
	 * @return A {@code String} containing the dialogue response, or {@code null} if no text is returned.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return null;
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It initializes the buffer skills for the specific NPC.<br>
	 * Each skill is scheduled to run after its initial delay.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final NpcBufferData data = _npcBuffers.getNpcBuffer(npc.getId());
		for (NpcBufferSkillData skill : data.getSkills())
		{
			ThreadPool.schedule(new NpcBufferAI(npc, skill), skill.getInitialDelay());
		}
	}
	
	/**
	 * This is the entry point for the {@code NpcBuffers} application.<br>
	 * It initializes a new instance of the {@code NpcBuffers} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new NpcBuffers();
	}
}
