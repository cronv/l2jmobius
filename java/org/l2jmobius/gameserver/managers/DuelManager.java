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
package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Duel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Manages the lifecycle and logic of player duels within the game world.<br>
 * This class handles the creation, tracking, and resolution of {@code Duel} instances between players.
 */
public class DuelManager
{
	private static final int[] ARENAS =
	{
		147, // OlympiadGrassyArena.xml
		148, // OlympiadThreeBridgesArena.xml
		149, // OlympiadHerossVestigesArena.xml
		150, // OlympiadOrbisArena.xml
	};
	private final Map<Integer, Duel> _duels = new ConcurrentHashMap<>();
	private final AtomicInteger _currentDuelId = new AtomicInteger();
	
	/**
	 * Initializes a new instance of the {@code DuelManager}.<br>
	 * This constructor is protected to restrict instantiation.<br>
	 * Use {@code getInstance} to access the manager.
	 */
	protected DuelManager()
	{
	}
	
	/**
	 * Retrieves a {@code Duel} object based on its unique identifier.<br>
	 * This method looks up the duel in the internal map.<br>
	 * It returns {@code null} if no duel exists with the given ID.
	 * @param duelId The unique ID of the duel to find.
	 * @return The {@code Duel} object, or {@code null} if not found.
	 */
	public Duel getDuel(int duelId)
	{
		return _duels.get(duelId);
	}
	
	/**
	 * Registers a new duel between two players.<br>
	 * This method checks if either player is currently engaged in PvP combat before creating the duel.<br>
	 * If a conflict is detected, it cancels the request and notifies the parties involved.
	 * @param playerA The first participant of the duel.
	 * @param playerB The second participant of the duel.
	 * @param partyDuel A flag indicating if this is a party-based duel.
	 */
	public void addDuel(Player playerA, Player playerB, int partyDuel)
	{
		if ((playerA == null) || (playerB == null))
		{
			return;
		}
		
		// Return if a player has PvPFlag.
		final String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
		if (partyDuel == 1)
		{
			final Party playerAParty = playerA.getParty();
			final Party playerBParty = playerB.getParty();
			boolean playerInPvP = false;
			for (Player temp : playerAParty.getMembers())
			{
				if (temp.getPvpFlag() != 0)
				{
					playerInPvP = true;
					break;
				}
			}
			
			if (!playerInPvP)
			{
				for (Player temp : playerBParty.getMembers())
				{
					if (temp.getPvpFlag() != 0)
					{
						playerInPvP = true;
						break;
					}
				}
			}
			
			// A player has PvP flag.
			if (playerInPvP)
			{
				for (Player temp : playerAParty.getMembers())
				{
					temp.sendMessage(engagedInPvP);
				}
				
				for (Player temp : playerBParty.getMembers())
				{
					temp.sendMessage(engagedInPvP);
				}
				return;
			}
		}
		else if ((playerA.getPvpFlag() != 0) || (playerB.getPvpFlag() != 0))
		{
			playerA.sendMessage(engagedInPvP);
			playerB.sendMessage(engagedInPvP);
			return;
		}
		
		final int duelId = _currentDuelId.incrementAndGet();
		_duels.put(duelId, new Duel(playerA, playerB, partyDuel, duelId));
	}
	
	/**
	 * Removes a specific {@code Duel} from the active duels list.<br>
	 * This method uses the unique ID of the provided {@code duel} object to perform the removal.
	 * @param duel The {@code Duel} instance to be removed.
	 */
	public void removeDuel(Duel duel)
	{
		_duels.remove(duel.getId());
	}
	
	/**
	 * Processes a surrender request from a player.<br>
	 * This method checks if the {@code Player} is currently in a duel.<br>
	 * If valid, it calls the surrender logic on the associated {@code Duel} object.
	 * @param player The {@code Player} who wants to surrender.
	 */
	public void doSurrender(Player player)
	{
		if ((player == null) || !player.isInDuel())
		{
			return;
		}
		
		final Duel duel = getDuel(player.getDuelId());
		duel.doSurrender(player);
	}
	
	/**
	 * Handles the logic when a {@code Player} is defeated in a duel.<br>
	 * This method checks if the player is currently in an active duel.<br>
	 * It then triggers the defeat logic for that specific duel.
	 * @param player The {@code Player} who was defeated.
	 */
	public void onPlayerDefeat(Player player)
	{
		if ((player == null) || !player.isInDuel())
		{
			return;
		}
		
		final Duel duel = getDuel(player.getDuelId());
		if (duel != null)
		{
			duel.onPlayerDefeat(player);
		}
	}
	
	/**
	 * Handles a {@code Skill} buff applied to a {@code Player}.<br>
	 * This method checks if the player is currently in a duel.<br>
	 * If valid, it passes the buff information to the active {@code Duel} object.
	 * @param player The {@code Player} receiving the buff.
	 * @param buff The {@code Skill} buff being applied.
	 */
	public void onBuff(Player player, Skill buff)
	{
		if ((player == null) || !player.isInDuel() || (buff == null))
		{
			return;
		}
		
		final Duel duel = getDuel(player.getDuelId());
		if (duel != null)
		{
			duel.onBuff(player, buff);
		}
	}
	
	/**
	 * This method is called when a {@code Player} leaves their party.<br>
	 * It checks if the {@code Player} is currently in a duel.<br>
	 * If they are, it updates the active {@code Duel} state accordingly.
	 * @param player The {@code Player} who was removed from the party.
	 */
	public void onRemoveFromParty(Player player)
	{
		if ((player == null) || !player.isInDuel())
		{
			return;
		}
		
		final Duel duel = getDuel(player.getDuelId());
		if (duel != null)
		{
			duel.onRemoveFromParty(player);
		}
	}
	
	/**
	 * Sends a network packet to the opposing team in a duel.<br>
	 * This method identifies the correct team based on the {@code Player} status.<br>
	 * It handles both individual and party duels automatically.
	 * @param player The {@code Player} who is initiating the broadcast.
	 * @param packet The {@code ServerPacket} to be sent to the other team.
	 */
	public void broadcastToOppositTeam(Player player, ServerPacket packet)
	{
		if ((player == null) || !player.isInDuel())
		{
			return;
		}
		
		final Duel duel = getDuel(player.getDuelId());
		if (duel == null)
		{
			return;
		}
		
		final Player playerA = duel.getPlayerA();
		final Player playerB = duel.getPlayerB();
		if ((playerA == null) || (playerB == null))
		{
			return;
		}
		
		if (playerA == player)
		{
			duel.broadcastToTeam2(packet);
		}
		else if (playerB == player)
		{
			duel.broadcastToTeam1(packet);
		}
		else if (duel.isPartyDuel())
		{
			final Party playerAParty = playerA.getParty();
			if ((playerAParty != null) && playerAParty.getMembers().contains(player))
			{
				duel.broadcastToTeam2(packet);
			}
			else
			{
				final Party playerBParty = playerB.getParty();
				if ((playerBParty != null) && playerBParty.getMembers().contains(player))
				{
					duel.broadcastToTeam1(packet);
				}
			}
		}
	}
	
	/**
	 * Retrieves a random arena ID for a duel.<br>
	 * This method selects an index from the internal {@code ARENAS} array.
	 * @return The randomly selected integer representing the arena ID.
	 */
	public int getDuelArena()
	{
		return ARENAS[Rnd.get(ARENAS.length)];
	}
	
	/**
	 * Retrieves the single global instance of {@code DuelManager}.<br>
	 * This method follows the singleton pattern.
	 * @return The active {@code DuelManager} instance.
	 */
	public static DuelManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DuelManager INSTANCE = new DuelManager();
	}
}
