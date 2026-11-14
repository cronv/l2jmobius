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
package org.l2jmobius.gameserver.managers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameAddPlayer;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameChangeTeam;
import org.l2jmobius.gameserver.network.serverpackets.ExCubeGameRemovePlayer;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages player additions, removals, and team changes for specific game events.<br>
 * It handles the status of event arenas by clearing participant lists or liberating the arena.
 * @author BiggBoss
 */
public class HandysBlockCheckerManager
{
	// All the participants and their team classified by arena.
	private static final ArenaParticipantsHolder[] _arenaPlayers = new ArenaParticipantsHolder[4];
	
	// Arena votes to start the game.
	private static final Map<Integer, Integer> _arenaVotes = new HashMap<>();
	
	// Arena Status, True = is being used, otherwise, False
	private static final Map<Integer, Boolean> _arenaStatus = new HashMap<>();
	
	// Registration request penalty (10 seconds)
	protected static Set<Integer> _registrationPenalty = Collections.synchronizedSet(new HashSet<>());
	
	/**
	 * Retrieves the current number of votes for a specific arena.<br>
	 * This method is thread-safe and uses {@code arenaId} to look up the value.
	 * @param arenaId The unique identifier of the arena.
	 * @return The total number of votes as an {@code int}.
	 */
	public synchronized int getArenaVotes(int arenaId)
	{
		return _arenaVotes.get(arenaId);
	}
	
	/**
	 * Increases the vote count for a specific arena.<br>
	 * This method checks if enough votes have been gathered to start the event.<br>
	 * If the requirements are met, it triggers the start of the event.
	 * @param arena The unique identifier for the arena.
	 */
	public synchronized void increaseArenaVotes(int arena)
	{
		final int newVotes = _arenaVotes.get(arena) + 1;
		final ArenaParticipantsHolder holder = _arenaPlayers[arena];
		if ((newVotes > (holder.getAllPlayers().size() / 2)) && !holder.getEvent().isStarted())
		{
			clearArenaVotes(arena);
			if ((holder.getBlueTeamSize() == 0) || (holder.getRedTeamSize() == 0))
			{
				return;
			}
			
			if (GeneralConfig.HBCE_FAIR_PLAY)
			{
				holder.checkAndShuffle();
			}
			
			ThreadPool.execute(holder.getEvent().new StartEvent());
		}
		else
		{
			_arenaVotes.put(arena, newVotes);
		}
	}
	
	/**
	 * Resets the vote count for a specific arena.<br>
	 * This method sets the votes for the given {@code arena} to {@code 0}.
	 * @param arena The unique identifier of the arena to clear.
	 */
	public synchronized void clearArenaVotes(int arena)
	{
		_arenaVotes.put(arena, 0);
	}
	
	/**
	 * Initializes the default values for arena statuses and votes.<br>
	 * Sets all four arenas to a status of {@code false}.<br>
	 * Sets all initial vote counts to {@code 0}.
	 */
	protected HandysBlockCheckerManager()
	{
		// Initialize arena status
		_arenaStatus.put(0, false);
		_arenaStatus.put(1, false);
		_arenaStatus.put(2, false);
		_arenaStatus.put(3, false);
		
		// Initialize arena votes
		_arenaVotes.put(0, 0);
		_arenaVotes.put(1, 0);
		_arenaVotes.put(2, 0);
		_arenaVotes.put(3, 0);
	}
	
	/**
	 * Retrieves the participant holder for a specific arena.<br>
	 * This method returns the {@code ArenaParticipantsHolder} associated with the provided index.
	 * @param arena The unique identifier of the arena.
	 * @return The {@code ArenaParticipantsHolder} for the specified arena.
	 */
	public ArenaParticipantsHolder getHolder(int arena)
	{
		return _arenaPlayers[arena];
	}
	
	/**
	 * Initializes the internal array of participants holders.<br>
	 * This method creates a new {@code ArenaParticipantsHolder} for each arena index.<br>
	 * It ensures that all four arenas have valid holder objects ready for use.
	 */
	public void startUpParticipantsQueue()
	{
		for (int i = 0; i < 4; ++i)
		{
			_arenaPlayers[i] = new ArenaParticipantsHolder(i);
		}
	}
	
	/**
	 * Adds a {@code Player} to a specific arena.<br>
	 * This method checks if the player is eligible to join based on several conditions.<br>
	 * It validates that the player is not already registered, does not have a cursed weapon, and is not in another event.<br>
	 * If successful, it assigns the player to the team with fewer members.
	 * @param player The {@code Player} instance to add to the arena.
	 * @param arenaId The unique identifier for the target arena.
	 * @return {@code true} if the player was successfully added; {@code false} otherwise.
	 */
	public boolean addPlayerToArena(Player player, int arenaId)
	{
		final ArenaParticipantsHolder holder = _arenaPlayers[arenaId];
		
		synchronized (holder)
		{
			boolean isRed;
			for (int i = 0; i < 4; i++)
			{
				if (_arenaPlayers[i].getAllPlayers().contains(player))
				{
					final SystemMessage msg = new SystemMessage(SystemMessageId.THE_MATCH_C1_PREVIOUSLY_PARTICIPATED_IN_SHOULD_END_BEFORE_YOU_APPLY_PLEASE_TRY_AGAIN_LATER);
					msg.addString(player.getName());
					player.sendPacket(msg);
					return false;
				}
			}
			
			if (player.isCursedWeaponEquipped())
			{
				player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_WHILE_IN_POSSESSION_OF_A_CURSED_WEAPON);
				return false;
			}
			
			if (player.isRegisteredOnEvent() || player.isInOlympiadMode())
			{
				player.sendMessage("Could not register you due other event participation.");
				return false;
			}
			
			if (OlympiadManager.getInstance().isRegistered(player))
			{
				OlympiadManager.getInstance().unRegisterNoble(player);
				player.sendPacket(SystemMessageId.APPLICANTS_FOR_THE_OLYMPIAD_UNDERGROUND_COLISEUM_OR_KRATEI_S_CUBE_MATCHES_CANNOT_REGISTER);
			}
			
			// if(UnderGroundColiseum.getInstance().isRegisteredPlayer(player))
			// {
			// UngerGroundColiseum.getInstance().removeParticipant(player);
			// player.sendPacket(SystemMessageId.APPLICANTS_FOR_THE_OLYMPIAD_UNDERGROUND_COLISEUM_OR_KRATEI_S_CUBE_MATCHES_CANNOT_REGISTER));
			// }
			// if(KrateiCubeManager.getInstance().isRegisteredPlayer(player))
			// {
			// KrateiCubeManager.getInstance().removeParticipant(player);
			// player.sendPacket(SystemMessageId.APPLICANTS_FOR_THE_OLYMPIAD_UNDERGROUND_COLISEUM_OR_KRATEI_S_CUBE_MATCHES_CANNOT_REGISTER));
			// }
			
			if (_registrationPenalty.contains(player.getObjectId()))
			{
				player.sendPacket(SystemMessageId.YOU_MUST_WAIT_10_SECONDS_BEFORE_ATTEMPTING_TO_REGISTER_AGAIN);
				return false;
			}
			
			if (holder.getBlueTeamSize() < holder.getRedTeamSize())
			{
				holder.addPlayer(player, 1);
				isRed = false;
			}
			else
			{
				holder.addPlayer(player, 0);
				isRed = true;
			}
			
			holder.broadCastPacketToTeam(new ExCubeGameAddPlayer(player, isRed));
			return true;
		}
	}
	
	/**
	 * Removes a {@code Player} from a specific arena and team.<br>
	 * This method also broadcasts the removal to other players on the same team.<br>
	 * If a team becomes empty after removal, the event ends abnormally.<br>
	 * A registration penalty is applied to the removed player.
	 * @param player The {@code Player} to be removed from the arena.
	 * @param arenaId The unique identifier of the arena.
	 * @param team The team index of the player being removed.
	 */
	public void removePlayer(Player player, int arenaId, int team)
	{
		final ArenaParticipantsHolder holder = _arenaPlayers[arenaId];
		synchronized (holder)
		{
			final boolean isRed = team == 0;
			holder.removePlayer(player, team);
			holder.broadCastPacketToTeam(new ExCubeGameRemovePlayer(player, isRed));
			
			// End event if theres an empty team.
			final int teamSize = isRed ? holder.getRedTeamSize() : holder.getBlueTeamSize();
			if (teamSize == 0)
			{
				holder.getEvent().endEventAbnormally();
			}
			
			final int objectId = player.getObjectId();
			_registrationPenalty.add(objectId);
			schedulePenaltyRemoval(objectId);
		}
	}
	
	/**
	 * Switches a {@code Player} to the opposite team in a specific arena.<br>
	 * This method checks if the target team has space before moving the player.<br>
	 * If the team is full, it sends a message to the {@code Player}.<br>
	 * It updates the participant holder and broadcasts the change to the team.
	 * @param player The {@code Player} who will change teams.
	 * @param arena The ID of the arena where the change occurs.
	 */
	public void changePlayerToTeam(Player player, int arena)
	{
		final ArenaParticipantsHolder holder = _arenaPlayers[arena];
		
		synchronized (holder)
		{
			final boolean isFromRed = holder.getRedPlayers().contains(player);
			if (isFromRed && (holder.getBlueTeamSize() == 6))
			{
				player.sendMessage("The team is full");
				return;
			}
			else if (!isFromRed && (holder.getRedTeamSize() == 6))
			{
				player.sendMessage("The team is full");
				return;
			}
			
			final int futureTeam = isFromRed ? 1 : 0;
			holder.addPlayer(player, futureTeam);
			if (isFromRed)
			{
				holder.removePlayer(player, 0);
			}
			else
			{
				holder.removePlayer(player, 1);
			}
			
			holder.broadCastPacketToTeam(new ExCubeGameChangeTeam(player, isFromRed));
		}
	}
	
	/**
	 * Clears all participants from a specific arena queue.<br>
	 * This method resets the player list for the given {@code arenaId}.
	 * @param arenaId The unique identifier of the arena to clear.
	 */
	public synchronized void clearPaticipantQueueByArenaId(int arenaId)
	{
		_arenaPlayers[arenaId].clearPlayers();
	}
	
	/**
	 * Checks if a specific arena is currently active.<br>
	 * It returns {@code true} if the arena is in use and {@code false} otherwise.<br>
	 * This method also validates that the {@code arenaId} is within the allowed range.
	 * @param arenaId The unique identifier of the arena to check.
	 * @return {@code true} if the arena is being used, {@code false} if it is free or invalid.
	 */
	public boolean arenaIsBeingUsed(int arenaId)
	{
		if ((arenaId < 0) || (arenaId > 3))
		{
			return false;
		}
		
		return _arenaStatus.get(arenaId);
	}
	
	/**
	 * Sets the status of a specific arena to being used.<br>
	 * This updates the internal state for the given {@code arenaId}.
	 * @param arenaId The unique identifier of the arena.
	 */
	public void setArenaBeingUsed(int arenaId)
	{
		_arenaStatus.put(arenaId, true);
	}
	
	/**
	 * Marks a specific arena as available for use.<br>
	 * This method updates the status of the arena to {@code false}.
	 * @param arenaId The unique identifier of the arena to free.
	 */
	public void setArenaFree(int arenaId)
	{
		_arenaStatus.put(arenaId, false);
	}
	
	/**
	 * Handles the cleanup logic when a {@code Player} disconnects.<br>
	 * It removes the player from their current arena and team.<br>
	 * If the player was in a team, it clears their effects and items.<br>
	 * Finally, it resets their zone status and teleports them back to a safe location.
	 * @param player The {@code Player} who is disconnecting.
	 */
	public void onDisconnect(Player player)
	{
		final int arena = player.getBlockCheckerArena();
		final int team = getHolder(arena).getPlayerTeam(player);
		getInstance().removePlayer(player, arena, team);
		if (player.getTeam() != Team.NONE)
		{
			player.stopAllEffects();
			
			// Remove team aura
			player.setTeam(Team.NONE);
			
			// Remove the event items.
			final PlayerInventory inv = player.getInventory();
			if (inv.getItemByItemId(13787) != null)
			{
				final long count = inv.getInventoryItemCount(13787, 0);
				inv.destroyItemByItemId(ItemProcessType.DESTROY, 13787, count, player, player);
			}
			
			if (inv.getItemByItemId(13788) != null)
			{
				final long count = inv.getInventoryItemCount(13788, 0);
				inv.destroyItemByItemId(ItemProcessType.DESTROY, 13788, count, player, player);
			}
			
			player.setInsideZone(ZoneId.PVP, false);
			
			// Teleport Back
			player.teleToLocation(-57478, -60367, -2370);
		}
	}
	
	/**
	 * Removes a specific penalty from the registration list.<br>
	 * This method clears the entry associated with the provided {@code objectId}.
	 * @param objectId The unique identifier of the object to remove.
	 */
	public void removePenalty(int objectId)
	{
		_registrationPenalty.remove(objectId);
	}
	
	/**
	 * Schedules the removal of a registration penalty for a specific object.<br>
	 * This method waits for 10000 milliseconds before calling {@code removePenalty}.
	 * @param objectId The unique identifier of the object to be processed.
	 */
	private void schedulePenaltyRemoval(int objectId)
	{
		ThreadPool.schedule(() -> removePenalty(objectId), 10000);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code HandysBlockCheckerManager}.<br>
	 * This provides a global access point to manage arena participants and statuses.
	 * @return The active instance of {@code HandysBlockCheckerManager}.
	 */
	public static HandysBlockCheckerManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HandysBlockCheckerManager INSTANCE = new HandysBlockCheckerManager();
	}
}
