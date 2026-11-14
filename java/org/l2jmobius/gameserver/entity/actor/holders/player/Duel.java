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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.enums.player.DuelResult;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoom;
import org.l2jmobius.gameserver.entity.groups.matching.MatchingRoomType;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.OlympiadStadiumZone;
import org.l2jmobius.gameserver.managers.DuelManager;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ActionFailed;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelEnd;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelReady;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelStart;
import org.l2jmobius.gameserver.network.serverpackets.ExDuelUpdateUserInfo;
import org.l2jmobius.gameserver.network.serverpackets.PlaySound;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a duel session between two players.<br>
 * This class manages the state, rules, and lifecycle of a 1v1 combat encounter.<br>
 * It handles data such as participants, results, and instance information for the {@code Duel} process.
 */
public class Duel
{
	protected static final Logger LOGGER = Logger.getLogger(Duel.class.getName());
	
	public static final int DUELSTATE_NODUEL = 0;
	public static final int DUELSTATE_DUELLING = 1;
	public static final int DUELSTATE_DEAD = 2;
	public static final int DUELSTATE_WINNER = 3;
	public static final int DUELSTATE_INTERRUPTED = 4;
	
	private static final PlaySound B04_S01 = new PlaySound(1, "B04_S01", 0, 0, 0, 0, 0);
	
	private static final int PARTY_DUEL_DURATION = 300;
	private static final int PLAYER_DUEL_DURATION = 120;
	
	private final int _duelId;
	private Player _playerA;
	private Player _playerB;
	private final boolean _partyDuel;
	private final Calendar _duelEndTime;
	private int _surrenderRequest = 0;
	private int _countdown = 5;
	private boolean _finished = false;
	
	private final Map<Integer, PlayerCondition> _playerConditions = new ConcurrentHashMap<>();
	Instance _duelInstance;
	
	/**
	 * Initializes a new {@code Duel} instance between two players.<br>
	 * This method sets up the duel parameters and handles starting states for both participants.<br>
	 * It also determines if the match is a party duel and schedules the start task.
	 * @param playerA The first participant in the duel.
	 * @param playerB The second participant in the duel.
	 * @param partyDuel A flag where {@code 1} indicates a party duel and other values indicate a standard duel.
	 * @param duelId The unique identifier for this specific duel.
	 */
	public Duel(Player playerA, Player playerB, int partyDuel, int duelId)
	{
		_duelId = duelId;
		_playerA = playerA;
		_playerB = playerB;
		_partyDuel = partyDuel == 1;
		if (_partyDuel)
		{
			for (Player member : _playerA.getParty().getMembers())
			{
				member.setStartingDuel();
			}
			
			for (Player member : _playerB.getParty().getMembers())
			{
				member.setStartingDuel();
			}
		}
		else
		{
			_playerA.setStartingDuel();
			_playerB.setStartingDuel();
		}
		
		_duelEndTime = Calendar.getInstance();
		_duelEndTime.add(Calendar.SECOND, _partyDuel ? PARTY_DUEL_DURATION : PLAYER_DUEL_DURATION);
		setFinished(false);
		
		if (_partyDuel)
		{
			// Inform players that they will be ported shortly.
			final SystemMessage sm = new SystemMessage(SystemMessageId.IN_A_MOMENT_YOU_WILL_BE_TRANSPORTED_TO_THE_SITE_WHERE_THE_DUEL_WILL_TAKE_PLACE);
			broadcastToTeam1(sm);
			broadcastToTeam2(sm);
		}
		
		// Schedule duel start
		ThreadPool.schedule(new ScheduleStartDuelTask(this), 3000);
	}
	
	public static class PlayerCondition
	{
		private Player _player;
		private double _hp;
		private double _mp;
		private double _cp;
		private boolean _paDuel;
		private int _x;
		private int _y;
		private int _z;
		private Set<Skill> _debuffs;
		
		public PlayerCondition(Player player, boolean partyDuel)
		{
			if (player == null)
			{
				return;
			}
			
			_player = player;
			_hp = _player.getCurrentHp();
			_mp = _player.getCurrentMp();
			_cp = _player.getCurrentCp();
			_paDuel = partyDuel;
			if (_paDuel)
			{
				_x = _player.getX();
				_y = _player.getY();
				_z = _player.getZ();
			}
		}
		
		public void restoreCondition()
		{
			if (_player == null)
			{
				return;
			}
			
			_player.setCurrentHp(_hp);
			_player.setCurrentMp(_mp);
			_player.setCurrentCp(_cp);
			
			if (_paDuel)
			{
				teleportBack();
			}
			
			if (_debuffs != null) // Debuff removal
			{
				for (Skill skill : _debuffs)
				{
					if (skill != null)
					{
						_player.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
					}
				}
			}
		}
		
		public void registerDebuff(Skill debuff)
		{
			if (_debuffs == null)
			{
				_debuffs = ConcurrentHashMap.newKeySet();
			}
			
			_debuffs.add(debuff);
		}
		
		public void teleportBack()
		{
			if (_paDuel)
			{
				_player.teleToLocation(_x, _y, _z);
			}
		}
		
		public Player getPlayer()
		{
			return _player;
		}
	}
	
	public class ScheduleDuelTask implements Runnable
	{
		private final Duel _duel;
		
		public ScheduleDuelTask(Duel duel)
		{
			_duel = duel;
		}
		
		@Override
		public void run()
		{
			try
			{
				switch (_duel.checkEndDuelCondition())
				{
					case CANCELED:
					{
						// do not schedule duel end if it was interrupted
						setFinished(true);
						_duel.endDuel(DuelResult.CANCELED);
						break;
					}
					case CONTINUE:
					{
						ThreadPool.schedule(this, 1000);
						break;
					}
					default:
					{
						setFinished(true);
						playKneelAnimation();
						ThreadPool.schedule(new ScheduleEndDuelTask(_duel, _duel.checkEndDuelCondition()), 5000);
						if (_duelInstance != null)
						{
							_duelInstance.destroy();
						}
						break;
					}
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "There has been a problem while runing a duel task!", e);
			}
		}
	}
	
	public static class ScheduleStartDuelTask implements Runnable
	{
		private final Duel _duel;
		
		public ScheduleStartDuelTask(Duel duel)
		{
			_duel = duel;
		}
		
		@Override
		public void run()
		{
			try
			{
				// start/continue countdown
				final int count = _duel.countdown();
				if (count == 4)
				{
					// Save player conditions before teleporting players.
					_duel.savePlayerConditions();
					
					_duel.teleportPlayers();
					
					// Give players 20 seconds to complete teleport and get ready (its ought to be 30 on offical..).
					ThreadPool.schedule(this, 20000);
				}
				else if (count > 0) // Duel not started yet - continue countdown.
				{
					ThreadPool.schedule(this, 1000);
				}
				else
				{
					_duel.startDuel();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "There has been a problem while runing a duel start task!", e);
			}
		}
	}
	
	public static class ScheduleEndDuelTask implements Runnable
	{
		private final Duel _duel;
		private final DuelResult _result;
		
		public ScheduleEndDuelTask(Duel duel, DuelResult result)
		{
			_duel = duel;
			_result = result;
		}
		
		@Override
		public void run()
		{
			try
			{
				_duel.endDuel(_result);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.SEVERE, "There has been a problem while runing a duel end task!", e);
			}
		}
	}
	
	/**
	 * Retrieves the {@code Instance} associated with this duel.
	 * @return the current {@code Instance} object.
	 */
	public Instance getDueldInstance()
	{
		return _duelInstance;
	}
	
	/**
	 * Stops all active combat actions for the players involved in the duel.<br>
	 * This method clears targets and cancels casts for both individual players or their parties.<br>
	 * It also ensures that any pets or servitors are forced to stop attacking.
	 */
	private void stopFighting()
	{
		final ActionFailed af = ActionFailed.STATIC_PACKET;
		if (_partyDuel)
		{
			for (Player temp : _playerA.getParty().getMembers())
			{
				temp.abortCast();
				temp.getAI().setIntentionActive();
				temp.setTarget(null);
				temp.sendPacket(af);
				temp.getServitorsAndPets().forEach(s ->
				{
					s.abortCast();
					s.abortAttack();
					s.setTarget(null);
					s.getAI().setIntentionActive();
				});
			}
			
			for (Player temp : _playerB.getParty().getMembers())
			{
				temp.abortCast();
				temp.getAI().setIntentionActive();
				temp.setTarget(null);
				temp.sendPacket(af);
				temp.getServitorsAndPets().forEach(s ->
				{
					s.abortCast();
					s.abortAttack();
					s.setTarget(null);
					s.getAI().setIntentionActive();
				});
			}
		}
		else
		{
			_playerA.abortCast();
			_playerB.abortCast();
			_playerA.getAI().setIntentionActive();
			_playerA.setTarget(null);
			_playerB.getAI().setIntentionActive();
			_playerB.setTarget(null);
			_playerA.sendPacket(af);
			_playerB.sendPacket(af);
			_playerA.getServitorsAndPets().forEach(s ->
			{
				s.abortCast();
				s.abortAttack();
				s.setTarget(null);
				s.getAI().setIntentionActive();
			});
			_playerB.getServitorsAndPets().forEach(s ->
			{
				s.abortCast();
				s.abortAttack();
				s.setTarget(null);
				s.getAI().setIntentionActive();
			});
		}
	}
	
	/**
	 * Checks if any duelist is currently engaged in PvP combat.<br>
	 * This check is skipped for party duels.<br>
	 * If a conflict is found, an optional message can be sent to both players.
	 * @param sendMessage Determines whether to send a cancellation message to the players.
	 * @return {@code true} if a duelist is in PvP, {@code false} otherwise.
	 */
	public boolean isDuelistInPvp(boolean sendMessage)
	{
		if (_partyDuel)
		{
			// Party duels take place in arenas - should be no other players there.
			return false;
		}
		else if ((_playerA.getPvpFlag() != 0) || (_playerB.getPvpFlag() != 0))
		{
			if (sendMessage)
			{
				final String engagedInPvP = "The duel was canceled because a duelist engaged in PvP combat.";
				_playerA.sendMessage(engagedInPvP);
				_playerB.sendMessage(engagedInPvP);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Initializes and starts the duel between players.<br>
	 * It validates that both players are available for a match.<br>
	 * The method handles logic for both individual and party duels.<br>
	 * It updates player states, sends network packets, and opens doors.<br>
	 * Finally, it schedules the main duel task using {@code ThreadPool}.
	 */
	public void startDuel()
	{
		if ((_playerA == null) || (_playerB == null) || _playerA.isInDuel() || _playerB.isInDuel())
		{
			_playerConditions.clear();
			DuelManager.getInstance().removeDuel(this);
			return;
		}
		
		if (_partyDuel)
		{
			withdrawFromGroupings(_playerA.getParty());
			withdrawFromGroupings(_playerB.getParty());
			
			// Set duel state and team.
			for (Player temp : _playerA.getParty().getMembers())
			{
				temp.cancelActiveTrade();
				temp.setInDuel(_duelId);
				temp.setTeam(Team.BLUE);
				temp.broadcastUserInfo();
				broadcastToTeam2(new ExDuelUpdateUserInfo(temp));
			}
			
			for (Player temp : _playerB.getParty().getMembers())
			{
				temp.cancelActiveTrade();
				temp.setInDuel(_duelId);
				temp.setTeam(Team.RED);
				temp.broadcastUserInfo();
				broadcastToTeam1(new ExDuelUpdateUserInfo(temp));
			}
			
			// Send duel packets
			broadcastToTeam1(ExDuelReady.PARTY_DUEL);
			broadcastToTeam2(ExDuelReady.PARTY_DUEL);
			broadcastToTeam1(ExDuelStart.PARTY_DUEL);
			broadcastToTeam2(ExDuelStart.PARTY_DUEL);
			
			for (Door door : _duelInstance.getDoors())
			{
				if ((door != null) && !door.isOpen())
				{
					door.openMe();
				}
			}
		}
		else
		{
			// set isInDuel() state
			_playerA.setInDuel(_duelId);
			_playerA.setTeam(Team.BLUE);
			_playerB.setInDuel(_duelId);
			_playerB.setTeam(Team.RED);
			
			// Send duel packets
			broadcastToTeam1(ExDuelReady.PLAYER_DUEL);
			broadcastToTeam2(ExDuelReady.PLAYER_DUEL);
			broadcastToTeam1(ExDuelStart.PLAYER_DUEL);
			broadcastToTeam2(ExDuelStart.PLAYER_DUEL);
			
			broadcastToTeam1(new ExDuelUpdateUserInfo(_playerB));
			broadcastToTeam2(new ExDuelUpdateUserInfo(_playerA));
			_playerA.broadcastUserInfo();
			_playerB.broadcastUserInfo();
		}
		
		// play sound
		broadcastToTeam1(B04_S01);
		broadcastToTeam2(B04_S01);
		
		// start duelling task
		ThreadPool.schedule(new ScheduleDuelTask(this), 1000);
	}
	
	/**
	 * Withdraws a dueling party from its command channel and closes the party matching room of its leader.
	 * @param party The {@code Party} entering the duel.
	 */
	private static void withdrawFromGroupings(Party party)
	{
		if (party.isInCommandChannel())
		{
			party.getCommandChannel().removeParty(party);
		}
		
		final Player leader = party.getLeader();
		final MatchingRoom room = leader.getMatchingRoom();
		if ((room != null) && (room.getRoomType() == MatchingRoomType.PARTY) && room.isLeader(leader))
		{
			room.disbandRoom();
		}
	}
	
	/**
	 * Saves the current conditions for players involved in a duel.<br>
	 * This method identifies all relevant participants based on whether it is a party duel.<br>
	 * It populates the {@code _playerConditions} map with new {@code PlayerCondition} objects.
	 */
	public void savePlayerConditions()
	{
		if (_partyDuel)
		{
			for (Player player : _playerA.getParty().getMembers())
			{
				_playerConditions.put(player.getObjectId(), new PlayerCondition(player, _partyDuel));
			}
			
			for (Player player : _playerB.getParty().getMembers())
			{
				_playerConditions.put(player.getObjectId(), new PlayerCondition(player, _partyDuel));
			}
		}
		else
		{
			_playerConditions.put(_playerA.getObjectId(), new PlayerCondition(_playerA, _partyDuel));
			_playerConditions.put(_playerB.getObjectId(), new PlayerCondition(_playerB, _partyDuel));
		}
	}
	
	/**
	 * Resets the duel status for all involved players.<br>
	 * Updates their team and duel state in the game world.<br>
	 * Restores player stats like HP and MP unless the end is abnormal.
	 * @param abnormalDuelEnd Determines if player conditions should be skipped.
	 */
	public void restorePlayerConditions(boolean abnormalDuelEnd)
	{
		// update isInDuel() state for all players
		if (_partyDuel)
		{
			for (Player temp : _playerA.getParty().getMembers())
			{
				temp.setInDuel(0);
				temp.setTeam(Team.NONE);
				temp.broadcastUserInfo();
			}
			
			for (Player temp : _playerB.getParty().getMembers())
			{
				temp.setInDuel(0);
				temp.setTeam(Team.NONE);
				temp.broadcastUserInfo();
			}
		}
		else
		{
			_playerA.setInDuel(0);
			_playerA.setTeam(Team.NONE);
			_playerA.broadcastUserInfo();
			_playerB.setInDuel(0);
			_playerB.setTeam(Team.NONE);
			_playerB.broadcastUserInfo();
		}
		
		// if it is an abnormal DuelEnd do not restore hp, mp, cp
		if (abnormalDuelEnd)
		{
			return;
		}
		
		// restore player conditions
		_playerConditions.values().forEach(PlayerCondition::restoreCondition);
	}
	
	/**
	 * Retrieves the unique identifier for this duel.
	 * @return the {@code int} value of the duel ID.
	 */
	public int getId()
	{
		return _duelId;
	}
	
	/**
	 * Calculates the time left until the duel ends.<br>
	 * The result is returned in milliseconds.
	 * @return the remaining time as an {@code int}.
	 */
	public int getRemainingTime()
	{
		return (int) (_duelEndTime.getTimeInMillis() - System.currentTimeMillis());
	}
	
	/**
	 * Retrieves the first player involved in this duel.
	 * @return the {@code Player} object for player A.
	 */
	public Player getPlayerA()
	{
		return _playerA;
	}
	
	/**
	 * Retrieves the second player involved in the duel.
	 * @return the {@code Player} object for player B.
	 */
	public Player getPlayerB()
	{
		return _playerB;
	}
	
	/**
	 * Checks if the current duel is a party duel.
	 * @return {@code true} if it is a party duel, {@code false} otherwise.
	 */
	public boolean isPartyDuel()
	{
		return _partyDuel;
	}
	
	/**
	 * Updates the finished status of the duel.<br>
	 * Sets the {@code _finished} field to the provided value.
	 * @param mode The new finished state to set.
	 */
	public void setFinished(boolean mode)
	{
		_finished = mode;
	}
	
	/**
	 * Checks if the duel has ended.
	 * @return {@code true} if the duel is finished, {@code false} otherwise.
	 */
	public boolean getFinished()
	{
		return _finished;
	}
	
	/**
	 * Teleports all members of both parties to the duel arena.<br>
	 * This method is only called for party duels.<br>
	 * It finds a valid {@code OlympiadStadiumZone} and creates a new instance.<br>
	 * Players are moved to random spawn locations within that instance.
	 */
	public void teleportPlayers()
	{
		if (!_partyDuel)
		{
			return;
		}
		
		final int instanceId = DuelManager.getInstance().getDuelArena();
		OlympiadStadiumZone zone = null;
		for (OlympiadStadiumZone z : ZoneManager.getInstance().getAllZones(OlympiadStadiumZone.class))
		{
			if (z.getInstanceTemplateId() == instanceId)
			{
				zone = z;
				break;
			}
		}
		
		if (zone == null)
		{
			throw new RuntimeException("Unable to find a party duel arena!");
		}
		
		final List<Location> spawns = zone.getSpawns();
		_duelInstance = InstanceManager.getInstance().createInstance(InstanceManager.getInstance().getInstanceTemplate(instanceId), null);
		
		final int spawnSize = spawns.size();
		final Location spawn1 = spawns.get(Rnd.get(spawnSize / 2));
		for (Player temp : _playerA.getParty().getMembers())
		{
			temp.teleToLocation(spawn1.getX(), spawn1.getY(), spawn1.getZ(), 0, 0, _duelInstance);
		}
		
		final Location spawn2 = spawns.get(Rnd.get(spawnSize / 2, spawnSize));
		for (Player temp : _playerB.getParty().getMembers())
		{
			temp.teleToLocation(spawn2.getX(), spawn2.getY(), spawn2.getZ(), 0, 0, _duelInstance);
		}
	}
	
	/**
	 * Sends a packet to all members of the first team.<br>
	 * If it is a party duel, every member in {@code _playerA.getParty()} receives the packet.<br>
	 * Otherwise, only {@code _playerA} receives the packet.<br>
	 * This method returns early if {@code _playerA} is {@code null}.
	 * @param packet The {@code ServerPacket} to be sent to the team members.
	 */
	public void broadcastToTeam1(ServerPacket packet)
	{
		if (_playerA == null)
		{
			return;
		}
		
		final Party partyA = _partyDuel ? _playerA.getParty() : null;
		if (partyA != null)
		{
			for (Player temp : partyA.getMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerA.sendPacket(packet);
		}
	}
	
	/**
	 * Sends a {@code ServerPacket} to all members of team 2.<br>
	 * If the duel is a party duel, it sends the packet to every member in the party.<br>
	 * If it is not a party duel, it only sends the packet to {@code _playerB}.<br>
	 * The method returns early if {@code _playerB} is {@code null}.
	 * @param packet The {@code ServerPacket} to be sent to the team.
	 */
	public void broadcastToTeam2(ServerPacket packet)
	{
		if (_playerB == null)
		{
			return;
		}
		
		final Party partyB = _partyDuel ? _playerB.getParty() : null;
		if (partyB != null)
		{
			for (Player temp : partyB.getMembers())
			{
				temp.sendPacket(packet);
			}
		}
		else
		{
			_playerB.sendPacket(packet);
		}
	}
	
	/**
	 * Retrieves the winner of the current duel.<br>
	 * This method returns {@code null} if the duel is not finished or no winner exists.
	 * @return the {@code Player} who won the duel, or {@code null} otherwise.
	 */
	public Player getWinner()
	{
		if (!_finished || (_playerA == null) || (_playerB == null))
		{
			return null;
		}
		
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		
		if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		
		return null;
	}
	
	/**
	 * Retrieves the player who lost the duel.<br>
	 * This method returns {@code null} if the duel is not finished or no winner was determined.
	 * @return the {@code Player} who lost the match, or {@code null} if applicable.
	 */
	public Player getLooser()
	{
		if (!_finished || (_playerA == null) || (_playerB == null))
		{
			return null;
		}
		
		if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerB;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			return _playerA;
		}
		
		return null;
	}
	
	/**
	 * Plays the kneeling animation for the loser of a duel.<br>
	 * This method identifies the losing player and broadcasts a {@code SocialAction} packet.<br>
	 * If it is a party duel, all members of the losing party will receive the animation.
	 */
	public void playKneelAnimation()
	{
		final Player looser = getLooser();
		if (looser == null)
		{
			return;
		}
		
		final Party looserParty = _partyDuel ? looser.getParty() : null;
		if (looserParty != null)
		{
			for (Player temp : looserParty.getMembers())
			{
				temp.broadcastPacket(new SocialAction(temp.getObjectId(), 7));
			}
		}
		else
		{
			looser.broadcastPacket(new SocialAction(looser.getObjectId(), 7));
		}
	}
	
	/**
	 * Decrements the duel countdown timer.<br>
	 * Sends a system message to both teams when the time is low or finished.
	 * @return The current value of {@code _countdown}.
	 */
	public int countdown()
	{
		_countdown--;
		
		if (_countdown > 3)
		{
			return _countdown;
		}
		
		// Broadcast countdown to duelists.
		SystemMessage sm = null;
		if (_countdown > 0)
		{
			sm = new SystemMessage(SystemMessageId.THE_DUEL_STARTS_IN_S1_SEC);
			sm.addInt(_countdown);
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.LET_THE_DUEL_BEGIN);
		}
		
		broadcastToTeam1(sm);
		broadcastToTeam2(sm);
		
		return _countdown;
	}
	
	/**
	 * Finalizes the duel and handles all necessary cleanup.<br>
	 * It determines which messages to send based on the {@code DuelResult}.<br>
	 * Player conditions are restored depending on how the duel ended.<br>
	 * The duel is removed from the {@code DuelManager} instance.
	 * @param result The final outcome of the duel.
	 */
	public void endDuel(DuelResult result)
	{
		if ((_playerA == null) || (_playerB == null))
		{
			// clean up
			_playerConditions.clear();
			DuelManager.getInstance().removeDuel(this);
			return;
		}
		
		// inform players of the result
		SystemMessage sm = null;
		switch (result)
		{
			case TEAM_1_WIN:
			case TEAM_2_SURRENDER:
			{
				restorePlayerConditions(false);
				sm = _partyDuel ? new SystemMessage(SystemMessageId.C1_S_PARTY_HAS_WON_THE_DUEL) : new SystemMessage(SystemMessageId.C1_HAS_WON_THE_DUEL);
				sm.addString(_playerA.getName());
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			}
			case TEAM_1_SURRENDER:
			case TEAM_2_WIN:
			{
				restorePlayerConditions(false);
				sm = _partyDuel ? new SystemMessage(SystemMessageId.C1_S_PARTY_HAS_WON_THE_DUEL) : new SystemMessage(SystemMessageId.C1_HAS_WON_THE_DUEL);
				sm.addString(_playerB.getName());
				
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			}
			case CANCELED:
			{
				stopFighting();
				
				// Don't restore hp, mp, cp.
				restorePlayerConditions(true);
				
				// TODO: is there no other message for a canceled duel?
				// send SystemMessage
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			}
			case TIMEOUT:
			{
				stopFighting();
				
				// hp,mp,cp seem to be restored in a timeout too...
				restorePlayerConditions(false);
				
				// send SystemMessage
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				broadcastToTeam1(sm);
				broadcastToTeam2(sm);
				break;
			}
		}
		
		// Send end duel packet.
		final ExDuelEnd duelEnd = _partyDuel ? ExDuelEnd.PARTY_DUEL : ExDuelEnd.PLAYER_DUEL;
		broadcastToTeam1(duelEnd);
		broadcastToTeam2(duelEnd);
		
		// clean up
		_playerConditions.clear();
		DuelManager.getInstance().removeDuel(this);
	}
	
	/**
	 * Checks if the current duel has met any conditions to end.<br>
	 * It evaluates player status, surrender requests, and time limits.<br>
	 * It also checks for interruptions or invalid zone locations in 1v1 duels.
	 * @return The {@code DuelResult} representing the current state of the duel.
	 */
	public DuelResult checkEndDuelCondition()
	{
		// one of the players might leave during duel
		if ((_playerA == null) || (_playerB == null) || !_playerA.isOnline() || !_playerB.isOnline())
		{
			return DuelResult.CANCELED;
		}
		
		// got a duel surrender request?
		if (_surrenderRequest != 0)
		{
			return _surrenderRequest == 1 ? DuelResult.TEAM_1_SURRENDER : DuelResult.TEAM_2_SURRENDER;
		}
		// duel timed out
		else if (getRemainingTime() <= 0)
		{
			return DuelResult.TIMEOUT;
		}
		// Has a player been declared winner yet?
		else if (_playerA.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on.
			stopFighting();
			return DuelResult.TEAM_1_WIN;
		}
		else if (_playerB.getDuelState() == DUELSTATE_WINNER)
		{
			// If there is a Winner already there should be no more fighting going on.
			stopFighting();
			return DuelResult.TEAM_2_WIN;
		}
		
		// More end duel conditions for 1on1 duels.
		else if (!_partyDuel)
		{
			// Duel was interrupted e.g.: player was attacked by mobs / other players.
			if ((_playerA.getDuelState() == DUELSTATE_INTERRUPTED) || (_playerB.getDuelState() == DUELSTATE_INTERRUPTED))
			{
				return DuelResult.CANCELED;
			}
			
			// Are the players too far apart?
			if (!_playerA.isInsideRadius2D(_playerB, 1600))
			{
				return DuelResult.CANCELED;
			}
			
			// Did one of the players engage in PvP combat?
			if (isDuelistInPvp(true))
			{
				return DuelResult.CANCELED;
			}
			
			// Is one of the players in a Siege, Peace or PvP zone?
			if (_playerA.isInsideZone(ZoneId.PEACE) || _playerB.isInsideZone(ZoneId.PEACE) || _playerA.isInsideZone(ZoneId.NO_PVP) || _playerB.isInsideZone(ZoneId.NO_PVP) || _playerA.isInsideZone(ZoneId.SIEGE) || _playerB.isInsideZone(ZoneId.SIEGE) || _playerA.isInsideZone(ZoneId.PVP) || _playerB.isInsideZone(ZoneId.PVP))
			{
				return DuelResult.CANCELED;
			}
		}
		
		return DuelResult.CONTINUE;
	}
	
	/**
	 * Processes a surrender request from a specific player.<br>
	 * This method stops the current fight and updates the duel status for all participants.<br>
	 * It handles both individual duels and party duels by assigning winner and loser states.
	 * @param player The {@code Player} who is initiating the surrender.
	 */
	public void doSurrender(Player player)
	{
		// Already received a surrender request.
		if (_surrenderRequest != 0)
		{
			return;
		}
		
		// stop the fight
		stopFighting();
		
		// TODO: Can every party member cancel a party duel? or only the party leaders?
		if (_partyDuel)
		{
			final Party partyA = _playerA.getParty();
			final Party partyB = _playerB.getParty();
			final List<Player> membersA = partyA.getMembers();
			final List<Player> membersB = partyB.getMembers();
			if (membersA.contains(player))
			{
				_surrenderRequest = 1;
				for (Player temp : membersA)
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				
				for (Player temp : membersB)
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
			else if (membersB.contains(player))
			{
				_surrenderRequest = 2;
				for (Player temp : membersB)
				{
					temp.setDuelState(DUELSTATE_DEAD);
				}
				
				for (Player temp : membersA)
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
		}
		else if (player == _playerA)
		{
			_surrenderRequest = 1;
			_playerA.setDuelState(DUELSTATE_DEAD);
			_playerB.setDuelState(DUELSTATE_WINNER);
		}
		else if (player == _playerB)
		{
			_surrenderRequest = 2;
			_playerB.setDuelState(DUELSTATE_DEAD);
			_playerA.setDuelState(DUELSTATE_WINNER);
		}
	}
	
	/**
	 * Handles the logic when a player is defeated during a duel.<br>
	 * It updates the {@code DuelState} of the defeated player.<br>
	 * If it is a party duel, it checks if any team members are still active to determine the winner.<br>
	 * For 1vs1 duels, it sets the opposing player as the winner.
	 * @param player The {@code Player} who has been defeated.
	 */
	public void onPlayerDefeat(Player player)
	{
		// Set player as defeated.
		player.setDuelState(DUELSTATE_DEAD);
		
		if (_partyDuel)
		{
			final boolean teamdefeated = player.getParty().getMembers().stream().anyMatch(member -> member.getDuelState() == DUELSTATE_DUELLING);
			if (teamdefeated)
			{
				final Player winner = _playerA.getParty().getMembers().contains(player) ? _playerB : _playerA;
				for (Player temp : winner.getParty().getMembers())
				{
					temp.setDuelState(DUELSTATE_WINNER);
				}
			}
		}
		else
		{
			if ((player != _playerA) && (player != _playerB))
			{
				LOGGER.warning("Error in onPlayerDefeat(): player is not part of this 1vs1 duel");
			}
			
			if (_playerA == player)
			{
				_playerB.setDuelState(DUELSTATE_WINNER);
			}
			else
			{
				_playerA.setDuelState(DUELSTATE_WINNER);
			}
		}
	}
	
	/**
	 * Handles the logic when a player leaves their party during an active party duel.<br>
	 * If one of the main duelists leaves, the entire duel is cancelled and players are teleported back.<br>
	 * Otherwise, only the specific player who left is removed from the duel state.
	 * @param player The {@code Player} object that is being removed from the party.
	 */
	public void onRemoveFromParty(Player player)
	{
		// If it isn't a party duel ignore this.
		if (!_partyDuel)
		{
			return;
		}
		
		// This player is leaving his party during party duel.
		// If he's either playerA or playerB cancel the duel and port the players back.
		if ((player == _playerA) || (player == _playerB))
		{
			for (PlayerCondition cond : _playerConditions.values())
			{
				cond.teleportBack();
				cond.getPlayer().setInDuel(0);
			}
			
			_playerA = null;
			_playerB = null;
		}
		else
		// Teleport the player back & delete his PlayerCondition record.
		{
			final PlayerCondition cond = _playerConditions.remove(player.getObjectId());
			if (cond != null)
			{
				cond.teleportBack();
			}
			
			player.setInDuel(0);
		}
	}
	
	/**
	 * Applies a specific debuff to the player's conditions.<br>
	 * This method checks if the {@code Player} has an existing condition record.<br>
	 * If found, it registers the provided {@code Skill} as a debuff.
	 * @param player The {@code Player} who will receive the effect.
	 * @param debuff The {@code Skill} to be applied as a debuff.
	 */
	public void onBuff(Player player, Skill debuff)
	{
		final PlayerCondition cond = _playerConditions.get(player.getObjectId());
		if (cond != null)
		{
			cond.registerDebuff(debuff);
		}
	}
}
