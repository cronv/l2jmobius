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
package ai.areas.FantasyIsle;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameChangeTimeToStart;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameRequestReady;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameTeamList;

/**
 * This class handles the artificial intelligence for the Handys Block Checker event.<br>
 * It manages the logic required to run the event script.
 * @authors BiggBoss, Gigiikun
 */
public class HandysBlockCheckerEvent extends Script
{
	private static final Logger LOGGER = Logger.getLogger(HandysBlockCheckerEvent.class.getName());
	
	// Arena Managers
	private static final int A_MANAGER_1 = 32521;
	private static final int A_MANAGER_2 = 32522;
	private static final int A_MANAGER_3 = 32523;
	private static final int A_MANAGER_4 = 32524;
	
	/**
	 * Initializes the {@code HandysBlockCheckerEvent} script.<br>
	 * It registers the required first talk IDs.<br>
	 * It starts the participant queue in {@code HandysBlockCheckerManager}.
	 */
	public HandysBlockCheckerEvent()
	{
		addFirstTalkId(A_MANAGER_1, A_MANAGER_2, A_MANAGER_3, A_MANAGER_4);
		HandysBlockCheckerManager.getInstance().startUpParticipantsQueue();
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method checks if the arena is full or currently being prepared.<br>
	 * If available, it adds the player to the arena and updates the game state.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return Returns {@code null} after processing the logic.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if ((npc == null) || (player == null))
		{
			return null;
		}
		
		final int arena = npc.getId() - A_MANAGER_1;
		if (eventIsFull(arena))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_BECAUSE_CAPACITY_HAS_BEEN_EXCEEDED);
			return null;
		}
		
		if (HandysBlockCheckerManager.getInstance().arenaIsBeingUsed(arena))
		{
			player.sendPacket(SystemMessageId.THE_MATCH_IS_BEING_PREPARED_PLEASE_TRY_AGAIN_LATER);
			return null;
		}
		
		if (HandysBlockCheckerManager.getInstance().addPlayerToArena(player, arena))
		{
			final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(arena);
			player.sendPacket(new ExCubeGameTeamList(holder.getRedPlayers(), holder.getBluePlayers(), arena));
			
			final int countBlue = holder.getBlueTeamSize();
			final int countRed = holder.getRedTeamSize();
			final int minMembers = GeneralConfig.MIN_BLOCK_CHECKER_TEAM_MEMBERS;
			if ((countBlue >= minMembers) && (countRed >= minMembers))
			{
				holder.updateEvent();
				holder.broadCastPacketToTeam(ExCubeGameRequestReady.STATIC_PACKET);
				holder.broadCastPacketToTeam(new ExCubeGameChangeTimeToStart(10));
			}
		}
		
		return null;
	}
	
	/**
	 * Checks if a specific arena has reached its maximum capacity.<br>
	 * It returns {@code true} if there are exactly 12 players in the arena.<br>
	 * Otherwise, it returns {@code false}.
	 * @param arena The unique identifier for the arena to check.
	 * @return {@code true} if the arena is full, {@code false} otherwise.
	 */
	private boolean eventIsFull(int arena)
	{
		return HandysBlockCheckerManager.getInstance().getHolder(arena).getAllPlayers().size() == 12;
	}
	
	/**
	 * This is the main entry point for the application.<br>
	 * It checks if the block checker event is enabled in {@code GeneralConfig}.<br>
	 * If enabled, it initializes a new {@code HandysBlockCheckerEvent} instance.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		if (GeneralConfig.ENABLE_BLOCK_CHECKER_EVENT)
		{
			new HandysBlockCheckerEvent();
			LOGGER.info("Handy's Block Checker Event is enabled");
		}
		else
		{
			LOGGER.info("Handy's Block Checker Event is disabled");
		}
	}
}
