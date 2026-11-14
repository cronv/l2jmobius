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
package org.l2jmobius.gameserver.entity.clan;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.enums.ClanWarState;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.clan.OnClanWarStart;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExEnemyKillLog;
import org.l2jmobius.gameserver.network.serverpackets.SurrenderPledgeWar;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the logic and state for clan warfare events.<br>
 * This class handles the progression of a war between two clans.<br>
 * It coordinates actions such as starting, ending, and surrendering during a {@code ClanWar}.
 * @author Sdw
 */
public class ClanWar
{
	public static final long TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR = TimeUnit.DAYS.toMillis(7);
	public static final long TIME_TO_DELETION_AFTER_CANCELLATION = TimeUnit.DAYS.toMillis(5);
	public static final long TIME_TO_DELETION_AFTER_DEFEAT = TimeUnit.DAYS.toMillis(21);
	private final int _attackerClanId;
	private final int _attackedClanId;
	private int _winnerClanId = 0;
	private ClanWarState _state;
	private Future<?> _cancelTask;
	private final long _startTime;
	private long _endTime = 0;
	
	public static enum WarProgress
	{
		VERY_LOW,
		LOW,
		NORMAL,
		HIGH,
		VERY_HIGH;
	}
	
	private final AtomicInteger _attackerKillCount = new AtomicInteger();
	private final AtomicInteger _attackedKillCount = new AtomicInteger();
	
	private boolean _attackerDeclared;
	private boolean _attackedDeclared;
	
	/**
	 * Initializes a new {@code ClanWar} between two clans.<br>
	 * This method sets the initial state to {@code BLOOD_DECLARATION}.<br>
	 * It schedules a timeout task and notifies relevant listeners.<br>
	 * System messages are sent to both participating clans.
	 * @param attacker The clan that is initiating the war.
	 * @param attacked The clan that is being attacked.
	 */
	public ClanWar(Clan attacker, Clan attacked)
	{
		_attackerClanId = attacker.getId();
		_attackedClanId = attacked.getId();
		_startTime = System.currentTimeMillis();
		_state = ClanWarState.BLOOD_DECLARATION;
		_cancelTask = ThreadPool.schedule(this::clanWarTimeout, (_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) - System.currentTimeMillis());
		attacker.addWar(attacked.getId(), this);
		attacked.addWar(attacker.getId(), this);
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_CLAN_WAR_START))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnClanWarStart(attacker, attacked));
		}
		
		SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_DECLARED_A_CLAN_WAR_WITH_S1);
		sm.addString(attacked.getName());
		attacker.broadcastToOnlineMembers(sm);
		
		sm = new SystemMessage(SystemMessageId.S1_HAS_DECLARED_A_CLAN_WAR_THE_WAR_WILL_AUTOMATICALLY_START_IF_YOU_KILL_MORE_THAN_3_S1_CLAN_MEMBERS_IN_3_D);
		sm.addString(attacker.getName());
		attacked.broadcastToOnlineMembers(sm);
	}
	
	/**
	 * Creates a new {@code ClanWar} instance with specific progress and timing data.<br>
	 * This constructor initializes the war state and schedules cleanup tasks if necessary.
	 * @param attacker The clan initiating the attack.
	 * @param attacked The clan being targeted by the attack.
	 * @param attackerKillCount The current number of kills for the attacking clan.
	 * @param attackedKillCount The current number of kills for the attacked clan.
	 * @param winnerClan The ID of the winning clan, or {@code 0} if not yet decided.
	 * @param startTime The timestamp when the war began.
	 * @param endTime The timestamp when the war is scheduled to end.
	 * @param state The current {@code ClanWarState} of the conflict.
	 */
	public ClanWar(Clan attacker, Clan attacked, int attackerKillCount, int attackedKillCount, int winnerClan, long startTime, long endTime, ClanWarState state)
	{
		_attackerClanId = attacker.getId();
		_attackedClanId = attacked.getId();
		_startTime = startTime;
		_endTime = endTime;
		_state = state;
		_attackerKillCount.set(attackerKillCount);
		_attackedKillCount.set(attackedKillCount);
		_winnerClanId = winnerClan;
		if ((_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) > System.currentTimeMillis())
		{
			_cancelTask = ThreadPool.schedule(this::clanWarTimeout, _startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR - System.currentTimeMillis());
		}
		
		if (_endTime > 0)
		{
			final long endTimePeriod = _endTime + (_state == ClanWarState.TIE ? TIME_TO_DELETION_AFTER_CANCELLATION : TIME_TO_DELETION_AFTER_DEFEAT);
			if (endTimePeriod > System.currentTimeMillis())
			{
				ThreadPool.schedule(() -> ClanTable.getInstance().deleteClanWars(_attackerClanId, _attackedClanId), 10000);
			}
			else
			{
				ThreadPool.schedule(() -> ClanTable.getInstance().deleteClanWars(_attackerClanId, _attackedClanId), endTimePeriod);
			}
		}
	}
	
	/**
	 * Handles the logic when a player kills another player during a clan war.<br>
	 * It updates kill counts and manages reputation scores based on the current war state.<br>
	 * This method also handles transitions from blood declaration to mutual war states.
	 * @param killer The {@code Player} who performed the kill.
	 * @param victim The {@code Player} who was killed.
	 */
	public void onKill(Player killer, Player victim)
	{
		final Clan victimClan = victim.getClan();
		final Clan killerClan = killer.getClan();
		
		// Reputation increase by killing an enemy (over level 4) in a clan war under the condition of mutual war declaration.
		if ((victim.getLevel() > 4) && (_state == ClanWarState.MUTUAL))
		{
			// However, when the other side reputation score is 0 or below, your clan cannot acquire any reputation points from them.
			if (victimClan.getReputationScore() > 0)
			{
				victimClan.takeReputationScore(FeatureConfig.REPUTATION_SCORE_PER_KILL);
				killerClan.addReputationScore(FeatureConfig.REPUTATION_SCORE_PER_KILL);
			}
			
			if (FeatureConfig.CLAN_WAR_KILL_MESSAGE)
			{
				victim.removePvpStreak();
				killer.increasePvpStreak();
				killerClan.broadcastToOnlineMembers(new ExEnemyKillLog(killer, victim, true));
				victimClan.broadcastToOnlineMembers(new ExEnemyKillLog(killer, victim, false));
			}
			else
			{
				// System Message notification to clan members.
				SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_KILLED_BY_A_MEMBER_OF_THE_S2_CLAN_CLAN_REPUTATION_POINTS_1);
				sm.addPcName(victim);
				sm.addString(killerClan.getName());
				victimClan.broadcastToOnlineMembers(sm);
				sm = new SystemMessage(SystemMessageId.A_MEMBER_OF_THE_S1_CLAN_IS_KILLED_BY_C2_CLAN_REPUTATION_POINTS_1);
				sm.addString(victimClan.getName());
				sm.addPcName(killer);
				killerClan.broadcastToOnlineMembers(sm);
			}
			
			if (killerClan.getId() == _attackerClanId)
			{
				_attackerKillCount.incrementAndGet();
			}
			else
			{
				_attackedKillCount.incrementAndGet();
			}
		}
		else if ((_state == ClanWarState.BLOOD_DECLARATION) && (victimClan.getId() == _attackerClanId) && (victim.getReputation() >= 0))
		{
			final int killCount = _attackedKillCount.incrementAndGet();
			if (killCount >= 3)
			{
				_state = ClanWarState.MUTUAL;
				SystemMessage sm = new SystemMessage(SystemMessageId.A_CLAN_WAR_WITH_S1_HAS_STARTED_THE_CLAN_THAT_WITHDRAWS_FROM_THE_WAR_FIRST_LOSE_10_000_CLAN_REPUTATION_POINTS_ANY_CLAN_THAT_CANCELS_THE_WAR_WILL_BE_UNABLE_TO_DECLARE_A_NEW_WAR_FOR_1_WEEK_IF_YOUR_CLAN_MEMBER_IS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_HUNTING_ZONES);
				sm.addString(victimClan.getName());
				killerClan.broadcastToOnlineMembers(sm);
				
				sm = new SystemMessage(SystemMessageId.A_CLAN_WAR_WITH_S1_HAS_STARTED_THE_CLAN_THAT_WITHDRAWS_FROM_THE_WAR_FIRST_LOSE_10_000_CLAN_REPUTATION_POINTS_ANY_CLAN_THAT_CANCELS_THE_WAR_WILL_BE_UNABLE_TO_DECLARE_A_NEW_WAR_FOR_1_WEEK_IF_YOUR_CLAN_MEMBER_IS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_HUNTING_ZONES);
				sm.addString(killerClan.getName());
				victimClan.broadcastToOnlineMembers(sm);
				
				if (_cancelTask != null)
				{
					_cancelTask.cancel(true);
					_cancelTask = null;
				}
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S1_THE_CLAN_S_MEMBER_HAS_BEEN_KILLED_BY_YOUR_CLAN_MEMBER_IF_YOUR_CLAN_KILLS_S2_MEMBERS_OF_S1_IN_3_D_THE_WAR_WITH_S1_WILL_BEGIN);
				sm.addString(victimClan.getName());
				sm.addInt(5 - killCount);
				killerClan.broadcastToOnlineMembers(sm);
			}
		}
	}
	
	/**
	 * Cancels the current clan war due to a surrender declaration.<br>
	 * The {@code cancelor} loses reputation and is treated as the loser.<br>
	 * The opposing clan is declared the winner of the war.
	 * @param player The {@code Player} who initiated the cancellation.
	 * @param cancelor The {@code Clan} that surrendered the war.
	 */
	public void cancel(Player player, Clan cancelor)
	{
		final Clan winnerClan = cancelor.getId() == _attackerClanId ? ClanTable.getInstance().getClan(_attackedClanId) : ClanTable.getInstance().getClan(_attackerClanId);
		
		// Reduce reputation.
		cancelor.takeReputationScore(5000);
		final String cancelorName = cancelor.getName();
		player.sendPacket(new SurrenderPledgeWar(cancelorName, player.getName()));
		SystemMessage sm = new SystemMessage(SystemMessageId.THE_WAR_IS_OVER_AS_YOU_VE_ADMITTED_DEFEAT_FROM_THE_CLAN_S1_YOU_VE_LOST);
		sm.addString(winnerClan.getName());
		cancelor.broadcastToOnlineMembers(sm);
		
		sm = new SystemMessage(SystemMessageId.THE_WAR_ENDED_BY_THE_S1_CLAN_S_DEFEAT_DECLARATION_YOU_HAVE_WON_THE_CLAN_WAR_OVER_THE_S1_CLAN);
		sm.addString(cancelorName);
		winnerClan.broadcastToOnlineMembers(sm);
		
		_winnerClanId = winnerClan.getId();
		_endTime = System.currentTimeMillis();
		ThreadPool.schedule(() -> ClanTable.getInstance().deleteClanWars(cancelor.getId(), winnerClan.getId()), 5000 /* (_endTime + TIME_TO_DELETION_AFTER_DEFEAT) - System.currentTimeMillis() */);
	}
	
	/**
	 * Handles the timeout logic for a clan war.<br>
	 * This method is called when a war expires because one side failed to respond.<br>
	 * It sets the state to {@code ClanWarState.TIE} and schedules the deletion of the war record.
	 */
	public void clanWarTimeout()
	{
		final Clan attackerClan = ClanTable.getInstance().getClan(_attackerClanId);
		final Clan attackedClan = ClanTable.getInstance().getClan(_attackedClanId);
		if ((attackerClan != null) && (attackedClan != null))
		{
			SystemMessage sm = new SystemMessage(SystemMessageId.THE_WAR_DECLARED_BY_THE_S1_CLAN_HAS_ENDED);
			sm.addString(attackerClan.getName());
			attackedClan.broadcastToOnlineMembers(sm);
			
			sm = new SystemMessage(SystemMessageId.S1_THE_CLAN_HAS_NOT_FOUGHT_BACK_FOR_3_D_THE_WAR_IS_OVER);
			sm.addString(attackedClan.getName());
			attackerClan.broadcastToOnlineMembers(sm);
			
			_state = ClanWarState.TIE;
			_endTime = System.currentTimeMillis();
			ThreadPool.schedule(() -> ClanTable.getInstance().deleteClanWars(attackerClan.getId(), attackedClan.getId()), 5000 /* (_endTime + TIME_TO_DELETION_AFTER_CANCELLATION) - System.currentTimeMillis() */);
		}
	}
	
	/**
	 * Updates the war state to mutual when both clans have declared war.<br>
	 * This method sends a system message to all online members of both clans.<br>
	 * It also cancels any existing cancellation task if one is active.
	 * @param attacker The {@code Clan} that initiated the attack.
	 * @param attacked The {@code Clan} that was targeted by the attack.
	 */
	public void mutualClanWarAccepted(Clan attacker, Clan attacked)
	{
		final int attackerId = attacker.getId();
		if (attackerId == _attackerClanId)
		{
			_attackerDeclared = true;
		}
		else if (attackerId == _attackedClanId)
		{
			_attackedDeclared = true;
		}
		
		if (_attackerDeclared && _attackedDeclared)
		{
			_state = ClanWarState.MUTUAL;
			SystemMessage sm = new SystemMessage(SystemMessageId.A_CLAN_WAR_WITH_S1_HAS_STARTED_THE_CLAN_THAT_WITHDRAWS_FROM_THE_WAR_FIRST_LOSE_10_000_CLAN_REPUTATION_POINTS_ANY_CLAN_THAT_CANCELS_THE_WAR_WILL_BE_UNABLE_TO_DECLARE_A_NEW_WAR_FOR_1_WEEK_IF_YOUR_CLAN_MEMBER_IS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_HUNTING_ZONES);
			sm.addString(attacker.getName());
			attacked.broadcastToOnlineMembers(sm);
			
			sm = new SystemMessage(SystemMessageId.A_CLAN_WAR_WITH_S1_HAS_STARTED_THE_CLAN_THAT_WITHDRAWS_FROM_THE_WAR_FIRST_LOSE_10_000_CLAN_REPUTATION_POINTS_ANY_CLAN_THAT_CANCELS_THE_WAR_WILL_BE_UNABLE_TO_DECLARE_A_NEW_WAR_FOR_1_WEEK_IF_YOUR_CLAN_MEMBER_IS_KILLED_BY_THE_OTHER_CLAN_XP_DECREASES_BY_1_4_OF_THE_AMOUNT_THAT_DECREASES_IN_HUNTING_ZONES);
			sm.addString(attacked.getName());
			attacker.broadcastToOnlineMembers(sm);
			
			if (_cancelTask != null)
			{
				_cancelTask.cancel(true);
				_cancelTask = null;
			}
		}
	}
	
	/**
	 * Calculates the difference in kills between two clans.<br>
	 * It returns a positive value if the provided {@code Clan} is winning.<br>
	 * If the clan is losing, it returns a negative value.
	 * @param clan The {@code Clan} to check for its kill difference.
	 * @return The integer difference of kills between the attacker and attacked clans.
	 */
	public int getKillDifference(Clan clan)
	{
		return _attackerClanId == clan.getId() ? _attackerKillCount.get() - _attackedKillCount.get() : _attackedKillCount.get() - _attackerKillCount.get();
	}
	
	/**
	 * Retrieves the current war status for a specific {@code Clan}.<br>
	 * It checks if a winner has been declared to return {@code WIN} or {@code LOSS}.<br>
	 * If no winner exists, it returns the general {@code ClanWarState}.
	 * @param clan The {@code Clan} to check.
	 * @return The {@code ClanWarState} for the provided clan.
	 */
	public ClanWarState getClanWarState(Clan clan)
	{
		if (_winnerClanId > 0)
		{
			return _winnerClanId == clan.getId() ? ClanWarState.WIN : ClanWarState.LOSS;
		}
		
		return _state;
	}
	
	/**
	 * Calculates the current progress of a war for a specific clan.<br>
	 * It compares the kill difference to determine the {@code WarProgress} level.
	 * @param clan The {@code Clan} to check the progress for.
	 * @return The calculated {@code WarProgress} based on the kill difference.
	 */
	public WarProgress calculateWarProgress(Clan clan)
	{
		int pointDiff = getKillDifference(clan);
		if (pointDiff <= -50)
		{
			return WarProgress.VERY_LOW;
		}
		
		if ((pointDiff > -50) && (pointDiff <= -20))
		{
			return WarProgress.LOW;
		}
		
		if ((pointDiff > -20) && (pointDiff <= 19))
		{
			return WarProgress.NORMAL;
		}
		
		if ((pointDiff > 19) && (pointDiff <= 49))
		{
			return WarProgress.HIGH;
		}
		
		return WarProgress.VERY_HIGH;
	}
	
	/**
	 * Retrieves the unique identifier of the attacking clan.<br>
	 * This value is used to identify which clan initiated the war.
	 * @return The {@code int} ID of the attacker clan.
	 */
	public int getAttackerClanId()
	{
		return _attackerClanId;
	}
	
	/**
	 * Retrieves the unique identifier of the clan being attacked.<br>
	 * This value corresponds to the {@code _attackedClanId} field.
	 * @return The {@code int} ID of the attacked clan.
	 */
	public int getAttackedClanId()
	{
		return _attackedClanId;
	}
	
	/**
	 * Retrieves the current number of kills made by the attacking clan.<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return The total kill count for the attacker as an {@code int}.
	 */
	public int getAttackerKillCount()
	{
		return _attackerKillCount.get();
	}
	
	/**
	 * Retrieves the total number of kills made by the attacked clan.<br>
	 * This value is updated during active combat events.
	 * @return The current kill count for the attacked side as an {@code int}.
	 */
	public int getAttackedKillCount()
	{
		return _attackedKillCount.get();
	}
	
	/**
	 * Retrieves the unique identifier of the winning clan.<br>
	 * Returns 0 if no winner has been determined yet.
	 * @return The {@code int} ID of the winning clan.
	 */
	public int getWinnerClanId()
	{
		return _winnerClanId;
	}
	
	/**
	 * Retrieves the start time of the {@code ClanWar}.<br>
	 * The value is returned in milliseconds.
	 * @return the start time as a {@code long}.
	 */
	public long getStartTime()
	{
		return _startTime;
	}
	
	/**
	 * Retrieves the end time of the {@code ClanWar}.<br>
	 * The value is returned as a {@code long}.
	 * @return The timestamp representing when the war ends.
	 */
	public long getEndTime()
	{
		return _endTime;
	}
	
	/**
	 * Retrieves the current state of the {@code ClanWar}.<br>
	 * This is useful for checking if the war is active or finished.
	 * @return The current {@code ClanWarState} of this war.
	 */
	public ClanWarState getState()
	{
		return _state;
	}
	
	/**
	 * Calculates the number of kills remaining to start a blood declaration.<br>
	 * It returns {@code 0} if the war is not in the {@code BLOOD_DECLARATION} state.<br>
	 * Otherwise, it subtracts the current attacked kill count from {@code 5}.
	 * @return The number of kills needed to trigger the next phase.
	 */
	public int getKillToStart()
	{
		return _state == ClanWarState.BLOOD_DECLARATION ? 5 - _attackedKillCount.get() : 0;
	}
	
	/**
	 * Calculates the time remaining until a non-mutual clan war can be cancelled.<br>
	 * This value is based on the {@code _startTime} and the constant {@code TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR}.
	 * @return The remaining time in seconds.
	 */
	public int getRemainingTime()
	{
		return (int) (((_startTime + TIME_TO_CANCEL_NON_MUTUAL_CLAN_WAR) - System.currentTimeMillis()) / 1000L);
	}
	
	/**
	 * Retrieves the opponent of a given {@code Clan}.<br>
	 * It checks if the provided clan is the attacker or the attacked party.<br>
	 * The method returns the other side of the war.
	 * @param clan The {@code Clan} to check for its opponent.
	 * @return The opposing {@code Clan} object.
	 */
	public Clan getOpposingClan(Clan clan)
	{
		return _attackerClanId == clan.getId() ? ClanTable.getInstance().getClan(_attackedClanId) : ClanTable.getInstance().getClan(_attackerClanId);
	}
}
