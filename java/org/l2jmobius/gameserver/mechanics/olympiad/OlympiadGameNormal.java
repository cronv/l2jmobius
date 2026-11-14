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
package org.l2jmobius.gameserver.mechanics.olympiad;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchInfo;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchResult;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadUserInfo;

/**
 * Handles the core logic for standard olympiad matches.<br>
 * This class manages the progression and rules of a typical olympiad event. It serves as the base implementation for {@code OlympiadGame} types that follow normal gameplay flow.
 * @author GodKratos, Pere, DS
 */
public abstract class OlympiadGameNormal extends AbstractOlympiadGame
{
	protected int _damageP1 = 0;
	protected int _damageP2 = 0;
	protected int _damageP1Final = 0;
	protected int _damageP2Final = 0;
	
	protected Participant _playerOne;
	protected Participant _playerTwo;
	
	String _winnerRound1 = "";
	String _winnerRound2 = "";
	String _winnerRound3 = "";
	int _player1Wins = 0;
	int _player2Wins = 0;
	boolean _matchEnd;
	
	/**
	 * Initializes a new normal olympiad game instance.<br>
	 * This constructor assigns the first two participants to the players.<br>
	 * It also sets the unique game ID for both players.
	 * @param id The unique identifier for this olympiad game.
	 * @param opponents An array containing the {@code Participant} objects for the match.
	 */
	protected OlympiadGameNormal(int id, Participant[] opponents)
	{
		super(id);
		
		_playerOne = opponents[0];
		_playerTwo = opponents[1];
		
		_playerOne.getPlayer().setOlympiadGameId(id);
		_playerTwo.getPlayer().setOlympiadGameId(id);
	}
	
	/**
	 * Creates an array of {@code Participant} objects from a set of player IDs.<br>
	 * This method randomly selects two online players to form a match.<br>
	 * It returns {@code null} if the input is invalid or no valid pair is found.
	 * @param set A {@code Set} of unique player IDs to choose from.
	 * @return An array containing two {@code Participant} objects, or {@code null}.
	 */
	protected static Participant[] createListOfParticipants(Set<Integer> set)
	{
		if ((set == null) || set.isEmpty() || (set.size() < 2))
		{
			return null;
		}
		
		int playerOneObjectId = 0;
		int playerTwoObjectId = 0;
		Player playerOne = null;
		Player playerTwo = null;
		
		while (set.size() > 1)
		{
			int random = Rnd.get(set.size());
			Iterator<Integer> iter = set.iterator();
			while (iter.hasNext())
			{
				playerOneObjectId = iter.next();
				if (--random < 0)
				{
					iter.remove();
					break;
				}
			}
			
			playerOne = World.getPlayer(playerOneObjectId);
			if ((playerOne == null) || !playerOne.isOnline())
			{
				continue;
			}
			
			random = Rnd.get(set.size());
			iter = set.iterator();
			while (iter.hasNext())
			{
				playerTwoObjectId = iter.next();
				if (--random < 0)
				{
					iter.remove();
					break;
				}
			}
			
			playerTwo = World.getPlayer(playerTwoObjectId);
			if ((playerTwo == null) || !playerTwo.isOnline())
			{
				set.add(playerOneObjectId);
				continue;
			}
			
			final Participant[] result = new Participant[2];
			result[0] = new Participant(playerOne, 1);
			result[1] = new Participant(playerTwo, 2);
			
			return result;
		}
		
		return null;
	}
	
	/**
	 * Checks if a specific player is participating in the current olympiad.<br>
	 * It verifies if the {@code playerId} matches either of the two active participants.
	 * @param playerId The unique ID of the player to check.
	 * @return {@code true} if the player is a participant, {@code false} otherwise.
	 */
	@Override
	public boolean containsParticipant(int playerId)
	{
		return ((_playerOne != null) && (_playerOne.getObjectId() == playerId)) || ((_playerTwo != null) && (_playerTwo.getObjectId() == playerId));
	}
	
	/**
	 * Sends the information of both participants to a specific creature.<br>
	 * This method sends {@code ExOlympiadUserInfo} packets for both {@code _playerOne} and {@code _playerTwo}.
	 * @param creature The {@code Creature} that will receive the olympiad data.
	 */
	@Override
	public void sendOlympiadInfo(Creature creature)
	{
		creature.sendPacket(new ExOlympiadUserInfo(_playerOne));
		creature.sendPacket(new ExOlympiadUserInfo(_playerTwo));
	}
	
	/**
	 * Sends the information of both participants to all players in the stadium.<br>
	 * This method uses {@code ExOlympiadUserInfo} packets for each player.
	 * @param stadium The {@code OlympiadStadium} where the broadcast will occur.
	 */
	@Override
	public void broadcastOlympiadInfo(OlympiadStadium stadium)
	{
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerOne));
		stadium.broadcastPacket(new ExOlympiadUserInfo(_playerTwo));
	}
	
	/**
	 * Sends a {@code ServerPacket} to both players in the match.<br>
	 * This method checks if each player is updated before sending the packet.
	 * @param packet The {@code ServerPacket} to be sent to the participants.
	 */
	@Override
	protected void broadcastPacket(ServerPacket packet)
	{
		if (_playerOne.updatePlayer())
		{
			_playerOne.getPlayer().sendPacket(packet);
		}
		
		if (_playerTwo.updatePlayer())
		{
			_playerTwo.getPlayer().sendPacket(packet);
		}
	}
	
	/**
	 * Moves the players to their designated spawn locations within an arena.<br>
	 * This method handles teleporting both participants to specific coordinates.<br>
	 * It returns {@code true} if all players were moved successfully.
	 * @param spawns A {@code List} of {@code Location} objects containing the target coordinates.
	 * @param instance The {@code Instance} where the movement should occur.
	 * @return {@code true} if both players were teleported, otherwise {@code false}.
	 */
	@Override
	protected boolean portPlayersToArena(List<Location> spawns, Instance instance)
	{
		boolean result = true;
		try
		{
			result &= portPlayerToArena(_playerOne, spawns.get(0), _stadiumId, instance, OlympiadMode.BLUE);
			result &= portPlayerToArena(_playerTwo, spawns.get(spawns.size() / 2), _stadiumId, instance, OlympiadMode.RED);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "portPlayersToArena: ", e);
			return false;
		}
		
		return result;
	}
	
	/**
	 * Teleports the primary players to their designated starting positions.<br>
	 * This method uses specific indices from the provided list of locations.<br>
	 * It returns {@code true} if both teleportations succeed, otherwise {@code false}.
	 * @param spawns A {@code List} of {@code Location} objects containing valid spawn points.
	 * @param instance The {@code Instance} where the teleportation occurs.
	 * @return {@code true} if all players were moved successfully, {@code false} otherwise.
	 */
	@Override
	protected boolean portPlayersToSpots(List<Location> spawns, Instance instance)
	{
		boolean result = true;
		try
		{
			result &= portPlayerToSpot(_playerOne, spawns.get(0), _stadiumId);
			result &= portPlayerToSpot(_playerTwo, spawns.get(spawns.size() / 2), _stadiumId);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "portPlayersToSpots: ", e);
			return false;
		}
		
		return result;
	}
	
	/**
	 * Checks if the game requires buffers.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if buffers are needed, {@code false} otherwise.
	 */
	@Override
	protected boolean needBuffers()
	{
		return true;
	}
	
	/**
	 * Handles the removal of items or effects for participants.<br>
	 * This method is called when a match is finished.<br>
	 * It checks if the game was aborted before proceeding.
	 */
	@Override
	protected void removals()
	{
		if (_aborted)
		{
			return;
		}
		
		removals(_playerOne.getPlayer(), true);
		removals(_playerTwo.getPlayer(), true);
	}
	
	/**
	 * Initializes the start of a competition.<br>
	 * This method checks if the base game can start.<br>
	 * It verifies that both players are not {@code null}.<br>
	 * If successful, it sets the olympiad status to {@code true} for both participants.
	 * @return {@code true} if the competition started successfully, otherwise {@code false}.
	 */
	@Override
	protected boolean makeCompetitionStart()
	{
		if (!super.makeCompetitionStart())
		{
			return false;
		}
		
		final Player p1 = _playerOne.getPlayer();
		final Player p2 = _playerTwo.getPlayer();
		if ((p1 == null) || (p2 == null))
		{
			return false;
		}
		
		p1.setOlympiadStart(true);
		p1.updateEffectIcons();
		p2.setOlympiadStart(true);
		p2.updateEffectIcons();
		return true;
	}
	
	/**
	 * Removes active effects from the participants of the olympiad.<br>
	 * This method checks if both players are still connected and active.<br>
	 * It then calls {@code cleanEffects} for each valid player.
	 */
	@Override
	protected void cleanEffects()
	{
		final Player p1 = _playerOne.getPlayer();
		if ((p1 != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected() && (p1.getOlympiadGameId() == _stadiumId))
		{
			cleanEffects(p1);
		}
		
		final Player p2 = _playerTwo.getPlayer();
		if ((p2 != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected() && (p2.getOlympiadGameId() == _stadiumId))
		{
			cleanEffects(p2);
		}
	}
	
	/**
	 * Moves the players back to their original locations.<br>
	 * This method checks if both {@code _playerOne} and {@code _playerTwo} are still active.<br>
	 * It ensures they have not defaulted or disconnected before teleporting them.
	 */
	@Override
	protected void portPlayersBack()
	{
		final Player p1 = _playerOne.getPlayer();
		if ((p1 != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected())
		{
			portPlayerBack(p1);
		}
		
		final Player p2 = _playerTwo.getPlayer();
		if ((p2 != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected())
		{
			portPlayerBack(p2);
		}
	}
	
	/**
	 * Restores the status of players for both participants.<br>
	 * This method checks if each player is active and belongs to the current stadium.<br>
	 * If valid, it calls {@code playerStatusBack(Player)} for that specific player.
	 */
	@Override
	protected void playersStatusBack()
	{
		final Player p1 = _playerOne.getPlayer();
		if ((p1 != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected() && (p1.getOlympiadGameId() == _stadiumId))
		{
			playerStatusBack(p1);
		}
		
		final Player p2 = _playerTwo.getPlayer();
		if ((p2 != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected() && (p2.getOlympiadGameId() == _stadiumId))
		{
			playerStatusBack(p2);
		}
	}
	
	/**
	 * Resets the participants for this olympiad game.<br>
	 * It sets both {@code _playerOne} and {@code _playerTwo} to {@code null}.<br>
	 * This method is used to clear the current players from the game state.
	 */
	@Override
	protected void clearPlayers()
	{
		_playerOne.setPlayer(null);
		_playerOne = null;
		_playerTwo.setPlayer(null);
		_playerTwo = null;
	}
	
	/**
	 * Handles the disconnection of a player from an olympiad match.<br>
	 * It updates the disconnected status for either {@code _playerOne} or {@code _playerTwo}.
	 * @param player The {@code Player} object that has disconnected.
	 */
	@Override
	protected void handleDisconnect(Player player)
	{
		final int objectId = player.getObjectId();
		if (objectId == _playerOne.getObjectId())
		{
			_playerOne.setDisconnected(true);
		}
		else if (objectId == _playerTwo.getObjectId())
		{
			_playerTwo.setDisconnected(true);
		}
	}
	
	/**
	 * Checks if the battle is currently active and valid.<br>
	 * It verifies that the game was not aborted.<br>
	 * It also ensures both players are still connected to the server.
	 * @return {@code true} if the battle is ongoing, {@code false} otherwise.
	 */
	@Override
	protected boolean checkBattleStatus()
	{
		if (_aborted)
		{
			return false;
		}
		
		if ((_playerOne.getPlayer() == null) || _playerOne.isDisconnected())
		{
			return false;
		}
		
		if ((_playerTwo.getPlayer() == null) || _playerTwo.isDisconnected())
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Checks if the match has ended because a winner was determined.<br>
	 * It verifies if either {@code _playerOne} or {@code _playerTwo} is dead.<br>
	 * This method also returns {@code true} if the battle status is invalid.
	 * @return {@code true} if there is a winner, {@code false} otherwise.
	 */
	@Override
	protected boolean haveWinner()
	{
		if (!checkBattleStatus())
		{
			return true;
		}
		
		boolean playerOneLost = true;
		try
		{
			final Player p1 = _playerOne.getPlayer();
			if (p1.getOlympiadGameId() == _stadiumId)
			{
				playerOneLost = p1.isDead();
			}
		}
		catch (Exception e)
		{
			playerOneLost = true;
		}
		
		boolean playerTwoLost = true;
		try
		{
			final Player p2 = _playerTwo.getPlayer();
			if (p2.getOlympiadGameId() == _stadiumId)
			{
				playerTwoLost = p2.isDead();
			}
		}
		catch (Exception e)
		{
			playerTwoLost = true;
		}
		
		return playerOneLost || playerTwoLost;
	}
	
	/**
	 * Checks if a winner has been determined for the current round.<br>
	 * This method returns {@code true} if either of the two players is dead or if the battle status is invalid.<br>
	 * It ensures that the game can proceed to the next phase by verifying the loss condition.
	 * @return {@code true} if a winner exists, {@code false} otherwise.
	 */
	@Override
	public boolean roundWinner()
	{
		if (!checkBattleStatus())
		{
			return true;
		}
		
		boolean playerOneLost = true;
		try
		{
			final Player p1 = _playerOne.getPlayer();
			if (p1.getOlympiadGameId() == _stadiumId)
			{
				playerOneLost = p1.isDead();
			}
		}
		catch (Exception e)
		{
			playerOneLost = true;
		}
		
		boolean playerTwoLost = true;
		try
		{
			final Player p2 = _playerTwo.getPlayer();
			if (p2.getOlympiadGameId() == _stadiumId)
			{
				playerTwoLost = p2.isDead();
			}
		}
		catch (Exception e)
		{
			playerTwoLost = true;
		}
		
		return playerOneLost || playerTwoLost;
	}
	
	/**
	 * Updates the status of whether the match has ended.<br>
	 * Sets the internal {@code _matchEnd} variable to the provided value.
	 * @param value The boolean state indicating if the match is finished.
	 */
	@Override
	public void matchEnd(boolean value)
	{
		_matchEnd = value;
	}
	
	/**
	 * Checks if the current olympiad match has finished.
	 * @return {@code true} if the match is over, {@code false} otherwise.
	 */
	@Override
	public boolean isMatchEnd()
	{
		return _matchEnd;
	}
	
	/**
	 * Validates the winner of the first round of an Olympiad match.<br>
	 * This method checks player health and damage to determine if a player won, lost, or tied.<br>
	 * It updates statistics and sends victory or defeat packets to the participants.
	 * @param stadium The {@code OlympiadStadium} where the match is taking place.
	 */
	@Override
	protected void validateRound1Winner(OlympiadStadium stadium)
	{
		if (_aborted)
		{
			return;
		}
		
		SystemMessage sm;
		
		try
		{
			double playerOneHp = 0;
			if ((_playerOne.getPlayer() != null) && !_playerOne.getPlayer().isDead())
			{
				final Player p = _playerOne.getPlayer();
				playerOneHp = p.getCurrentHp() + p.getCurrentCp();
				if (playerOneHp < 0.5)
				{
					playerOneHp = 0;
				}
			}
			
			double playerTwoHp = 0;
			if ((_playerTwo.getPlayer() != null) && !_playerTwo.getPlayer().isDead())
			{
				final Player p2 = _playerTwo.getPlayer();
				playerTwoHp = p2.getCurrentHp() + p2.getCurrentCp();
				if (playerTwoHp < 0.5)
				{
					playerTwoHp = 0;
				}
			}
			
			// If players crashed, search if they've relogged.
			_playerOne.updatePlayer();
			_playerTwo.updatePlayer();
			
			_damageP1Final += _damageP1;
			_damageP2Final += _damageP2;
			
			final Player p = _playerOne.getPlayer();
			final Player p2 = _playerTwo.getPlayer();
			if (((p == null) || !p.isOnline()) && ((p2 == null) || !p2.isOnline()))
			{
				_playerOne.updateStat(COMP_DRAWN, 1);
				_playerTwo.updateStat(COMP_DRAWN, 1);
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				stadium.broadcastPacket(sm);
			}
			else if ((p2 == null) || !p2.isOnline() || ((playerTwoHp == 0) && (playerOneHp != 0)))
			{
				final String p1Name = _playerOne.getName();
				final String p2Name = _playerTwo.getName();
				_winnerRound1 = p1Name;
				_player1Wins += 1;
				
				final SystemMessage ko = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_KNOCK_DOWN);
				
				if (p != null)
				{
					p.sendPacket(ko);
					p.sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_VICTORY));
					p.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 1, 0, 1, 20));
					ThreadPool.schedule(() -> SkillCaster.triggerCast(p, p, CommonSkill.OLYMPIAD_WIN.getSkill()), 2000);
					ThreadPool.schedule(() -> p.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 1, 0, 2, 100)), 20000);
				}
				
				if (p2 != null)
				{
					p2.sendPacket(ko);
					p2.setCurrentCp(p2.getMaxCp() * 0.7);
					p2.setCurrentHp(p2.getMaxHp() * 0.7);
					p2.setCurrentMp(p2.getMaxMp() * 0.7);
					p2.sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_DEFEAT));
					p2.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 1, 0, 1, 20));
					ThreadPool.schedule(() -> p2.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 1, 0, 2, 100)), 20000);
				}
			}
			else if ((p == null) || !p.isOnline() || ((playerOneHp == 0) && (playerTwoHp != 0)))
			{
				final String p1Name = _playerOne.getName();
				final String p2Name = _playerTwo.getName();
				_winnerRound1 = p2Name;
				_player2Wins += 1;
				
				final SystemMessage ko = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_KNOCK_DOWN);
				// if (p2 != null)
				// {
				p2.sendPacket(ko);
				p2.sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_VICTORY));
				p2.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 0, 1, 1, 20));
				ThreadPool.schedule(() -> SkillCaster.triggerCast(p2, p2, CommonSkill.OLYMPIAD_WIN.getSkill()), 2000);
				ThreadPool.schedule(() -> p2.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 0, 1, 2, 100)), 20000);
				// }
				
				if (p != null)
				{
					p.sendPacket(ko);
					p.sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_DEFEAT));
					p.setCurrentCp(p.getMaxCp() * 0.7);
					p.setCurrentHp(p.getMaxHp() * 0.7);
					p.setCurrentMp(p.getMaxMp() * 0.7);
					p.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 0, 1, 1, 20));
					ThreadPool.schedule(() -> p.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 0, 1, 2, 100)), 20000);
				}
			}
			else
			{
				final String p1Name = _playerOne.getName();
				final String p2Name = _playerTwo.getName();
				_winnerRound1 = "";
				final SystemMessage tie = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_TIE);
				p2.broadcastPacket(tie);
				p.broadcastPacket(tie);
				
				p.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 0, 0, 1, 20));
				ThreadPool.schedule(() -> p.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 0, 0, 2, 100)), 20000);
				p2.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 0, 0, 1, 20));
				ThreadPool.schedule(() -> p2.sendPacket(new ExOlympiadMatchInfo(p1Name, p2Name, 0, 0, 2, 100)), 20000);
			}
			
			resetDamage();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on validateRound1Winner(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Validates the winner of the second round of an olympiad match.<br>
	 * This method checks player health and damage to determine the victor.<br>
	 * It handles victory conditions, ties, and sends relevant packets to players.
	 * @param stadium The {@code OlympiadStadium} where the match is taking place.
	 */
	@Override
	protected void validateRound2Winner(OlympiadStadium stadium)
	{
		if (_aborted)
		{
			return;
		}
		
		SystemMessage sm;
		
		try
		{
			double playerOneHp = 0;
			if ((_playerOne.getPlayer() != null) && !_playerOne.getPlayer().isDead())
			{
				playerOneHp = _playerOne.getPlayer().getCurrentHp() + _playerOne.getPlayer().getCurrentCp();
				if (playerOneHp < 0.5)
				{
					playerOneHp = 0;
				}
			}
			
			double playerTwoHp = 0;
			if ((_playerTwo.getPlayer() != null) && !_playerTwo.getPlayer().isDead())
			{
				playerTwoHp = _playerTwo.getPlayer().getCurrentHp() + _playerTwo.getPlayer().getCurrentCp();
				if (playerTwoHp < 0.5)
				{
					playerTwoHp = 0;
				}
			}
			
			// If players crashed, search if they've relogged.
			_playerOne.updatePlayer();
			_playerTwo.updatePlayer();
			
			_damageP1Final += _damageP1;
			_damageP2Final += _damageP2;
			
			if (((_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline()) && ((_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline()))
			{
				_playerOne.updateStat(COMP_DRAWN, 1);
				_playerTwo.updateStat(COMP_DRAWN, 1);
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				stadium.broadcastPacket(sm);
			}
			else if ((_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline() || ((playerTwoHp == 0) && (playerOneHp != 0)))
			{
				_winnerRound2 = _playerOne.getName();
				_player1Wins += 1;
				
				final SystemMessage ko = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_KNOCK_DOWN);
				
				_playerOne.getPlayer().sendPacket(ko);
				_playerOne.getPlayer().sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_VICTORY));
				if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerOne.getName()))
				{
					_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 2, 0, 2, 20));
					matchEnd(true);
				}
				else if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerTwo.getName()))
				{
					_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 1, 2, 20));
					ThreadPool.schedule(() -> _playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 1, 3, 100)), 20000);
				}
				else
				{
					_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 0, 2, 20));
					ThreadPool.schedule(() -> _playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 0, 3, 100)), 20000);
				}
				
				ThreadPool.schedule(() -> SkillCaster.triggerCast(_playerOne.getPlayer(), _playerOne.getPlayer(), CommonSkill.OLYMPIAD_WIN.getSkill()), 2000);
				
				if (_playerTwo.getPlayer() != null)
				{
					_playerTwo.getPlayer().sendPacket(ko);
					_playerTwo.getPlayer().sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_DEFEAT));
					if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerOne.getName()))
					{
						_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 2, 0, 2, 20));
					}
					else if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerTwo.getName()))
					{
						_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 1, 2, 20));
						ThreadPool.schedule(() -> _playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 1, 3, 100)), 20000);
					}
					else
					{
						_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 0, 2, 20));
						ThreadPool.schedule(() -> _playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 0, 3, 100)), 20000);
					}
				}
			}
			else if ((_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline() || ((playerOneHp == 0) && (playerTwoHp != 0)))
			{
				_winnerRound2 = _playerTwo.getName();
				_player2Wins += 1;
				
				final SystemMessage ko = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_KNOCK_DOWN);
				
				_playerTwo.getPlayer().sendPacket(ko);
				_playerTwo.getPlayer().sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_VICTORY));
				if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerTwo.getName()))
				{
					_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 2, 2, 20));
					matchEnd(true);
				}
				else if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerOne.getName()))
				{
					_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 1, 2, 20));
					ThreadPool.schedule(() -> _playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 1, 3, 100)), 20000);
				}
				else
				{
					_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 1, 2, 20));
					ThreadPool.schedule(() -> _playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 1, 3, 100)), 20000);
				}
				
				ThreadPool.schedule(() -> SkillCaster.triggerCast(_playerTwo.getPlayer(), _playerTwo.getPlayer(), CommonSkill.OLYMPIAD_WIN.getSkill()), 2000);
				
				if (_playerOne.getPlayer() != null)
				{
					_playerOne.getPlayer().sendPacket(ko);
					_playerOne.getPlayer().sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_DEFEAT));
					if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerTwo.getName()))
					{
						_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 2, 2, 20));
					}
					else if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerOne.getName()))
					{
						_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 1, 2, 20));
						ThreadPool.schedule(() -> _playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 1, 3, 100)), 20000);
					}
					else
					{
						_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 1, 2, 20));
						ThreadPool.schedule(() -> _playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 1, 3, 100)), 20000);
					}
				}
			}
			else
			{
				_winnerRound2 = "";
				final SystemMessage tie = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_TIE);
				_playerTwo.getPlayer().broadcastPacket(tie);
				_playerOne.getPlayer().broadcastPacket(tie);
				if (_winnerRound1.toLowerCase().equalsIgnoreCase(""))
				{
					_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 0, 2, 20));
					ThreadPool.schedule(() -> _playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 0, 3, 100)), 20000);
					_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 0, 2, 20));
					ThreadPool.schedule(() -> _playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 0, 3, 100)), 20000);
				}
				else if (_winnerRound1.toLowerCase().equalsIgnoreCase(_playerOne.getName()))
				{
					_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 0, 2, 20));
					ThreadPool.schedule(() -> _playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 0, 3, 100)), 20000);
					_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 0, 2, 20));
					ThreadPool.schedule(() -> _playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 1, 0, 3, 100)), 20000);
				}
				else
				{
					_playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 1, 2, 20));
					ThreadPool.schedule(() -> _playerOne.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 1, 3, 100)), 20000);
					_playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 1, 2, 20));
					ThreadPool.schedule(() -> _playerTwo.getPlayer().sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), _playerTwo.getName(), 0, 1, 3, 100)), 20000);
				}
			}
			
			resetDamage();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on validateRound2Winner(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Validates the winner of the third round in an olympiad match.<br>
	 * This method checks player health and damage to determine if a player won, lost, or tied.<br>
	 * It updates statistics and sends victory or defeat packets to the participants.
	 * @param stadium The {@code OlympiadStadium} where the match is taking place.
	 */
	@Override
	protected void validateRound3Winner(OlympiadStadium stadium)
	{
		if (_aborted)
		{
			return;
		}
		
		SystemMessage sm;
		
		try
		{
			double playerOneHp = 0;
			if ((_playerOne.getPlayer() != null) && !_playerOne.getPlayer().isDead())
			{
				final Player p = _playerOne.getPlayer();
				playerOneHp = p.getCurrentHp() + p.getCurrentCp();
				if (playerOneHp < 0.5)
				{
					playerOneHp = 0;
				}
			}
			
			double playerTwoHp = 0;
			if ((_playerTwo.getPlayer() != null) && !_playerTwo.getPlayer().isDead())
			{
				final Player p2 = _playerTwo.getPlayer();
				playerTwoHp = p2.getCurrentHp() + p2.getCurrentCp();
				if (playerTwoHp < 0.5)
				{
					playerTwoHp = 0;
				}
			}
			
			// If players crashed, search if they've relogged.
			_playerOne.updatePlayer();
			_playerTwo.updatePlayer();
			
			_damageP1Final += _damageP1;
			_damageP2Final += _damageP2;
			
			if (((_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline()) && ((_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline()))
			{
				_playerOne.updateStat(COMP_DRAWN, 1);
				_playerTwo.updateStat(COMP_DRAWN, 1);
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				stadium.broadcastPacket(sm);
			}
			else if ((_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline() || ((playerTwoHp == 0) && (playerOneHp != 0)))
			{
				final String p1Name = _playerOne.getName();
				_winnerRound3 = p1Name;
				_player1Wins += 1;
				
				final SystemMessage ko = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_KNOCK_DOWN);
				
				final Player p1Obj = _playerOne.getPlayer();
				p1Obj.sendPacket(ko);
				p1Obj.sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_VICTORY));
				if (p1Name.toLowerCase().equalsIgnoreCase(p1Name))
				{
					p1Obj.sendPacket(new ExOlympiadMatchInfo(p1Name, _playerTwo.getName(), 2, 1, 3, 0));
				}
				else
				{
					p1Obj.sendPacket(new ExOlympiadMatchInfo(p1Name, _playerTwo.getName(), 1, 2, 3, 0));
				}
				
				ThreadPool.schedule(() -> SkillCaster.triggerCast(p1Obj, p1Obj, CommonSkill.OLYMPIAD_WIN.getSkill()), 2000);
				
				if (_playerTwo.getPlayer() != null)
				{
					final Player p2Obj = _playerTwo.getPlayer();
					p2Obj.sendPacket(ko);
					p2Obj.sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_DEFEAT));
					if (p1Name.toLowerCase().equalsIgnoreCase(p1Name))
					{
						p2Obj.sendPacket(new ExOlympiadMatchInfo(p1Name, _playerTwo.getName(), 2, 1, 3, 0));
					}
					else
					{
						p2Obj.sendPacket(new ExOlympiadMatchInfo(p1Name, _playerTwo.getName(), 1, 2, 3, 0));
					}
				}
			}
			else if ((_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline() || ((playerOneHp == 0) && (playerTwoHp != 0)))
			{
				final String p2Name = _playerTwo.getName();
				_winnerRound3 = p2Name;
				_player2Wins += 1;
				
				final SystemMessage ko = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_KNOCK_DOWN);
				
				final Player p2Obj = _playerTwo.getPlayer();
				p2Obj.sendPacket(ko);
				p2Obj.sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_VICTORY));
				if (p2Name.toLowerCase().equalsIgnoreCase(p2Name))
				{
					p2Obj.sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), p2Name, 1, 2, 3, 0));
				}
				else
				{
					p2Obj.sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), p2Name, 2, 1, 3, 0));
				}
				
				ThreadPool.schedule(() -> SkillCaster.triggerCast(p2Obj, p2Obj, CommonSkill.OLYMPIAD_WIN.getSkill()), 2000);
				
				if (_playerOne.getPlayer() != null)
				{
					final Player p1Obj = _playerOne.getPlayer();
					p1Obj.sendPacket(ko);
					p1Obj.sendPacket(new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_DEFEAT));
					if (p2Name.toLowerCase().equalsIgnoreCase(p2Name))
					{
						p1Obj.sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), p2Name, 1, 2, 3, 0));
					}
					else
					{
						p1Obj.sendPacket(new ExOlympiadMatchInfo(_playerOne.getName(), p2Name, 2, 1, 3, 0));
					}
				}
			}
			else
			{
				_winnerRound3 = "";
				final SystemMessage tie = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_TIE);
				_playerTwo.getPlayer().broadcastPacket(tie);
				_playerOne.getPlayer().broadcastPacket(tie);
			}
			
			resetDamage();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on validateRound3Winner(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Validates the match results and determines the winner of an olympiad.<br>
	 * This method handles scenarios such as player defaults, crashes, or normal completion.<br>
	 * It updates participant statistics and broadcasts the final result to the stadium.
	 * @param stadium The {@code OlympiadStadium} where the match took place.
	 */
	@Override
	protected void validateWinner(OlympiadStadium stadium)
	{
		if (_aborted)
		{
			return;
		}
		
		ExOlympiadMatchResult resultP1 = null;
		ExOlympiadMatchResult resultP2 = null;
		
		int winside = 0;
		
		final List<OlympiadInfo> list1 = new ArrayList<>(1);
		final List<OlympiadInfo> list2 = new ArrayList<>(1);
		
		final boolean pOneCrash = ((_playerOne.getPlayer() == null) || _playerOne.isDisconnected());
		final boolean pTwoCrash = ((_playerTwo.getPlayer() == null) || _playerTwo.isDisconnected());
		
		final int playerOnePoints = _playerOne.getStats().getInt(POINTS);
		final int playerTwoPoints = _playerTwo.getStats().getInt(POINTS);
		int pointDiff = Math.min(playerOnePoints, playerTwoPoints) / getDivider();
		if (pointDiff <= 0)
		{
			pointDiff = 1;
		}
		else if (pointDiff > OlympiadConfig.OLYMPIAD_MAX_POINTS)
		{
			pointDiff = OlympiadConfig.OLYMPIAD_MAX_POINTS;
		}
		
		int points;
		SystemMessage sm;
		
		// Check for if a player defaulted before battle started.
		if (_playerOne.isDefaulted() || _playerTwo.isDefaulted())
		{
			try
			{
				if (_playerOne.isDefaulted())
				{
					try
					{
						points = Math.min(playerOnePoints / 3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
						removePointsFromParticipant(_playerOne, points);
						list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1, playerOnePoints - points, -points));
						
						winside = 2;
						
						if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
						{
							LOGGER_OLYMPIAD.info(_playerOne.getName() + " default," + _playerOne + "," + _playerTwo + ",0,0,0,0," + points + "," + getType());
						}
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				
				if (_playerTwo.isDefaulted())
				{
					try
					{
						points = Math.min(playerTwoPoints / 3, OlympiadConfig.OLYMPIAD_MAX_POINTS);
						removePointsFromParticipant(_playerTwo, points);
						list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2, playerTwoPoints - points, -points));
						
						winside = 1;
						
						if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
						{
							LOGGER_OLYMPIAD.info(_playerTwo.getName() + " default," + _playerOne + "," + _playerTwo + ",0,0,0,0," + points + "," + getType());
						}
					}
					catch (Exception e)
					{
						LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
					}
				}
				return;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}
		
		// Create results for players if a player crashed.
		if (pOneCrash || pTwoCrash)
		{
			try
			{
				if (pTwoCrash && !pOneCrash)
				{
					sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
					sm.addString(_playerOne.getName());
					stadium.broadcastPacket(sm);
					
					_playerOne.updateStat(COMP_WON, 1);
					addPointsToParticipant(_playerOne, pointDiff);
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1Final, playerOnePoints + pointDiff, pointDiff));
					
					_playerTwo.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerTwo, pointDiff);
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2Final, playerTwoPoints - pointDiff, -pointDiff));
					
					winside = 1;
					
					rewardParticipant(_playerOne.getPlayer(), OlympiadConfig.OLYMPIAD_WINNER_REWARD); // Winner
					
					if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
					{
						LOGGER_OLYMPIAD.info(_playerTwo.getName() + " crash," + _playerOne + "," + _playerTwo + ",0,0,0,0," + pointDiff + "," + getType());
					}
					
					// Notify to scripts
					if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerOne, _playerTwo, getType()), Olympiad.getInstance());
					}
				}
				else if (pOneCrash && !pTwoCrash)
				{
					sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
					sm.addString(_playerTwo.getName());
					stadium.broadcastPacket(sm);
					
					_playerTwo.updateStat(COMP_WON, 1);
					addPointsToParticipant(_playerTwo, pointDiff);
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2Final, playerTwoPoints + pointDiff, pointDiff));
					
					_playerOne.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerOne, pointDiff);
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1Final, playerOnePoints - pointDiff, -pointDiff));
					
					winside = 2;
					
					rewardParticipant(_playerTwo.getPlayer(), OlympiadConfig.OLYMPIAD_WINNER_REWARD); // Winner
					
					if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
					{
						LOGGER_OLYMPIAD.info(_playerOne.getName() + " crash," + _playerOne + "," + _playerTwo + ",0,0,0,0," + pointDiff + "," + getType());
					}
					
					// Notify to scripts
					if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
					{
						EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerTwo, _playerOne, getType()), Olympiad.getInstance());
					}
				}
				else if (pOneCrash && pTwoCrash)
				{
					stadium.broadcastPacket(new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE));
					
					_playerOne.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerOne, pointDiff);
					list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1Final, playerOnePoints - pointDiff, -pointDiff));
					
					_playerTwo.updateStat(COMP_LOST, 1);
					removePointsFromParticipant(_playerTwo, pointDiff);
					list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2Final, playerTwoPoints - pointDiff, -pointDiff));
					
					if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
					{
						LOGGER_OLYMPIAD.info("both crash," + _playerOne.getName() + "," + _playerOne + ",0,0,0,0," + _playerTwo + "," + pointDiff + "," + getType());
					}
				}
				
				_playerOne.updateStat(COMP_DONE, 1);
				_playerTwo.updateStat(COMP_DONE, 1);
				_playerOne.updateStat(COMP_DONE_WEEK, 1);
				_playerTwo.updateStat(COMP_DONE_WEEK, 1);
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, _playerOne, getType()), Olympiad.getInstance());
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(null, _playerTwo, getType()), Olympiad.getInstance());
				}
				return;
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
				return;
			}
		}
		
		try
		{
			String winner = "draw";
			
			// Calculate Fight time
			final long fightTime = (System.currentTimeMillis() - _startTime);
			
			double playerOneHp = 0;
			if ((_playerOne.getPlayer() != null) && !_playerOne.getPlayer().isDead())
			{
				playerOneHp = _playerOne.getPlayer().getCurrentHp() + _playerOne.getPlayer().getCurrentCp();
				if (playerOneHp < 0.5)
				{
					playerOneHp = 0;
				}
			}
			
			double playerTwoHp = 0;
			if ((_playerTwo.getPlayer() != null) && !_playerTwo.getPlayer().isDead())
			{
				playerTwoHp = _playerTwo.getPlayer().getCurrentHp() + _playerTwo.getPlayer().getCurrentCp();
				if (playerTwoHp < 0.5)
				{
					playerTwoHp = 0;
				}
			}
			
			// If players crashed, search if they've relogged.
			_playerOne.updatePlayer();
			_playerTwo.updatePlayer();
			
			if (((_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline()) && ((_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline()))
			{
				_playerOne.updateStat(COMP_DRAWN, 1);
				_playerTwo.updateStat(COMP_DRAWN, 1);
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				stadium.broadcastPacket(sm);
			}
			else if ((_player1Wins == 2) || (_playerTwo.getPlayer() == null) || !_playerTwo.getPlayer().isOnline())
			{
				sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
				sm.addString(_playerOne.getName());
				stadium.broadcastPacket(sm);
				
				_playerOne.updateStat(COMP_WON, 1);
				_playerTwo.updateStat(COMP_LOST, 1);
				
				addPointsToParticipant(_playerOne, pointDiff);
				list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1Final, playerOnePoints + pointDiff, pointDiff));
				
				removePointsFromParticipant(_playerTwo, pointDiff);
				list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2Final, playerTwoPoints - pointDiff, -pointDiff));
				
				winner = _playerOne.getName() + " won";
				winside = 1;
				
				// Save Fight Result
				saveResults(_playerOne, _playerTwo, 1, _startTime, fightTime, getType());
				
				rewardParticipant(_playerOne.getPlayer(), OlympiadConfig.OLYMPIAD_WINNER_REWARD); // Winner
				rewardParticipant(_playerTwo.getPlayer(), OlympiadConfig.OLYMPIAD_LOSER_REWARD); // Loser
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerOne, _playerTwo, getType()), Olympiad.getInstance());
				}
			}
			else if ((_player2Wins == 2) || (_playerOne.getPlayer() == null) || !_playerOne.getPlayer().isOnline())
			{
				sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
				sm.addString(_playerTwo.getName());
				stadium.broadcastPacket(sm);
				
				_playerTwo.updateStat(COMP_WON, 1);
				_playerOne.updateStat(COMP_LOST, 1);
				
				addPointsToParticipant(_playerTwo, pointDiff);
				list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2Final, playerTwoPoints + pointDiff, pointDiff));
				
				removePointsFromParticipant(_playerOne, pointDiff);
				list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1Final, playerOnePoints - pointDiff, -pointDiff));
				
				winner = _playerTwo.getName() + " won";
				winside = 2;
				
				// Save Fight Result
				saveResults(_playerOne, _playerTwo, 2, _startTime, fightTime, getType());
				
				rewardParticipant(_playerTwo.getPlayer(), OlympiadConfig.OLYMPIAD_WINNER_REWARD); // Winner
				rewardParticipant(_playerOne.getPlayer(), OlympiadConfig.OLYMPIAD_LOSER_REWARD); // Loser
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerTwo, _playerOne, getType()), Olympiad.getInstance());
				}
			}
			else if ((_player1Wins == 1) && (_player2Wins == 0)) // One player 1 win, two draws.
			{
				sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
				sm.addString(_playerOne.getName());
				stadium.broadcastPacket(sm);
				
				_playerOne.updateStat(COMP_WON, 1);
				_playerTwo.updateStat(COMP_LOST, 1);
				
				addPointsToParticipant(_playerOne, pointDiff);
				list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1Final, playerOnePoints + pointDiff, pointDiff));
				
				removePointsFromParticipant(_playerTwo, pointDiff);
				list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2Final, playerTwoPoints - pointDiff, -pointDiff));
				
				winner = _playerOne.getName() + " won";
				winside = 1;
				
				// Save Fight Result
				saveResults(_playerOne, _playerTwo, 1, _startTime, fightTime, getType());
				
				rewardParticipant(_playerOne.getPlayer(), OlympiadConfig.OLYMPIAD_WINNER_REWARD); // Winner
				rewardParticipant(_playerTwo.getPlayer(), OlympiadConfig.OLYMPIAD_LOSER_REWARD); // Loser
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerOne, _playerTwo, getType()), Olympiad.getInstance());
				}
			}
			else if ((_player2Wins == 1) && (_player1Wins == 0)) // One player 2 win, two draws.
			{
				sm = new SystemMessage(SystemMessageId.CONGRATULATIONS_C1_YOU_WIN_THE_MATCH);
				sm.addString(_playerTwo.getName());
				stadium.broadcastPacket(sm);
				
				_playerTwo.updateStat(COMP_WON, 1);
				_playerOne.updateStat(COMP_LOST, 1);
				
				addPointsToParticipant(_playerTwo, pointDiff);
				list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2Final, playerTwoPoints + pointDiff, pointDiff));
				
				removePointsFromParticipant(_playerOne, pointDiff);
				list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1Final, playerOnePoints - pointDiff, -pointDiff));
				
				winner = _playerTwo.getName() + " won";
				winside = 2;
				
				// Save Fight Result
				saveResults(_playerOne, _playerTwo, 2, _startTime, fightTime, getType());
				
				rewardParticipant(_playerTwo.getPlayer(), OlympiadConfig.OLYMPIAD_WINNER_REWARD); // Winner
				rewardParticipant(_playerOne.getPlayer(), OlympiadConfig.OLYMPIAD_LOSER_REWARD); // Loser
				
				// Notify to scripts
				if (EventDispatcher.getInstance().hasListener(EventType.ON_OLYMPIAD_MATCH_RESULT, Olympiad.getInstance()))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnOlympiadMatchResult(_playerTwo, _playerOne, getType()), Olympiad.getInstance());
				}
			}
			else
			{
				// Save Fight Result
				_playerTwo.updateStat(COMP_DRAWN, 1);
				_playerOne.updateStat(COMP_DRAWN, 1);
				saveResults(_playerOne, _playerTwo, 0, _startTime, fightTime, getType());
				
				sm = new SystemMessage(SystemMessageId.THE_DUEL_HAS_ENDED_IN_A_TIE);
				stadium.broadcastPacket(sm);
				
				int value = Math.min(playerOnePoints / getDivider(), OlympiadConfig.OLYMPIAD_MAX_POINTS);
				
				removePointsFromParticipant(_playerOne, value);
				list1.add(new OlympiadInfo(_playerOne.getName(), _playerOne.getClanName(), _playerOne.getClanId(), _playerOne.getBaseClass(), _damageP1Final, playerOnePoints - value, -value));
				
				value = Math.min(playerTwoPoints / getDivider(), OlympiadConfig.OLYMPIAD_MAX_POINTS);
				removePointsFromParticipant(_playerTwo, value);
				list2.add(new OlympiadInfo(_playerTwo.getName(), _playerTwo.getClanName(), _playerTwo.getClanId(), _playerTwo.getBaseClass(), _damageP2Final, playerTwoPoints - value, -value));
			}
			
			_playerOne.updateStat(COMP_DONE, 1);
			_playerTwo.updateStat(COMP_DONE, 1);
			_playerOne.updateStat(COMP_DONE_WEEK, 1);
			_playerTwo.updateStat(COMP_DONE_WEEK, 1);
			
			if (winside == 1)
			{
				resultP1 = new ExOlympiadMatchResult(false, winside, list1, list2, _winnerRound1.equalsIgnoreCase(_playerOne.getName()) ? 2 : _winnerRound1.isEmpty() ? 0 : 3, _winnerRound2.equalsIgnoreCase(_playerOne.getName()) ? 2 : _winnerRound2.isEmpty() ? 0 : 3, !_winnerRound3.isEmpty() && _winnerRound3.equalsIgnoreCase(_playerOne.getName()) ? 2 : _winnerRound3.isEmpty() ? 0 : 3, list1.get(0).getCurrentPoints(), list1.get(0).getDiffPoints());
			}
			else if (winside == 2)
			{
				resultP1 = new ExOlympiadMatchResult(false, winside, list2, list1, _winnerRound1.equalsIgnoreCase(_playerTwo.getName()) ? 3 : _winnerRound1.isEmpty() ? 0 : 2, _winnerRound2.equalsIgnoreCase(_playerTwo.getName()) ? 3 : _winnerRound2.isEmpty() ? 0 : 2, !_winnerRound3.isEmpty() && _winnerRound3.equalsIgnoreCase(_playerTwo.getName()) ? 3 : _winnerRound3.isEmpty() ? 0 : 2, list1.get(0).getCurrentPoints(), list1.get(0).getDiffPoints());
			}
			else
			{
				resultP1 = new ExOlympiadMatchResult(true, winside, list1, list2, _winnerRound1.isEmpty() ? 0 : (_winnerRound1.equalsIgnoreCase(_playerOne.getName()) ? 1 : 2), _winnerRound2.isEmpty() ? 0 : (_winnerRound2.equalsIgnoreCase(_playerOne.getName()) ? 1 : 2), _winnerRound3.isEmpty() ? 0 : (_winnerRound3.equalsIgnoreCase(_playerOne.getName()) ? 1 : 2), list1.get(0).getCurrentPoints(), list1.get(0).getDiffPoints());
			}
			
			if (winside == 1)
			{
				resultP2 = new ExOlympiadMatchResult(false, winside, list1, list2, _winnerRound1.equalsIgnoreCase(_playerOne.getName()) ? 2 : _winnerRound1.isEmpty() ? 0 : 3, _winnerRound2.equalsIgnoreCase(_playerOne.getName()) ? 2 : _winnerRound2.isEmpty() ? 0 : 3, !_winnerRound3.isEmpty() && _winnerRound3.equalsIgnoreCase(_playerOne.getName()) ? 2 : _winnerRound3.isEmpty() ? 0 : 3, list2.get(0).getCurrentPoints(), list2.get(0).getDiffPoints());
			}
			else if (winside == 2)
			{
				resultP2 = new ExOlympiadMatchResult(false, winside, list2, list1, _winnerRound1.equalsIgnoreCase(_playerTwo.getName()) ? 3 : _winnerRound1.isEmpty() ? 0 : 2, _winnerRound2.equalsIgnoreCase(_playerTwo.getName()) ? 3 : _winnerRound2.isEmpty() ? 0 : 2, !_winnerRound3.isEmpty() && _winnerRound3.equalsIgnoreCase(_playerTwo.getName()) ? 3 : _winnerRound3.isEmpty() ? 0 : 2, list2.get(0).getCurrentPoints(), list2.get(0).getDiffPoints());
			}
			else
			{
				resultP2 = new ExOlympiadMatchResult(true, winside, list1, list2, _winnerRound1.isEmpty() ? 0 : (_winnerRound1.equalsIgnoreCase(_playerTwo.getName()) ? 2 : 1), _winnerRound2.isEmpty() ? 0 : (_winnerRound2.equalsIgnoreCase(_playerTwo.getName()) ? 2 : 1), _winnerRound3.isEmpty() ? 0 : (_winnerRound3.equalsIgnoreCase(_playerTwo.getName()) ? 2 : 1), list2.get(0).getCurrentPoints(), list2.get(0).getDiffPoints());
			}
			
			if (_playerOne.getPlayer() != null)
			{
				_playerOne.getPlayer().sendPacket(resultP1);
			}
			
			if (_playerTwo.getPlayer() != null)
			{
				_playerTwo.getPlayer().sendPacket(resultP2);
			}
			
			if (OlympiadConfig.OLYMPIAD_LOG_FIGHTS)
			{
				LOGGER_OLYMPIAD.info(winner + "," + _playerOne.getName() + "," + _playerOne + "," + _playerTwo + "," + playerOneHp + "," + playerTwoHp + "," + _damageP1 + "," + _damageP2 + "," + pointDiff + "," + getType());
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception on validateWinner(): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Adds a specific amount of damage to the total score for a player.<br>
	 * This method checks if the target is not invincible or protected before adding the value.<br>
	 * It updates either {@code _damageP1} or {@code _damageP2} based on which participant is hit.
	 * @param player The {@code Player} who received the damage.
	 * @param damage The amount of damage to add to the score.
	 */
	@Override
	protected void addDamage(Player player, int damage)
	{
		final Player player1 = _playerOne.getPlayer();
		final Player player2 = _playerTwo.getPlayer();
		if ((player1 == null) || (player2 == null))
		{
			return;
		}
		
		if (player == player1)
		{
			if (!player2.isInvul() && !player2.isHpBlocked())
			{
				_damageP1 += damage;
			}
		}
		else if (player == player2)
		{
			if (!player1.isInvul() && !player1.isHpBlocked())
			{
				_damageP2 += damage;
			}
		}
	}
	
	/**
	 * Retrieves the names of the players in this match.<br>
	 * It returns an array containing two names.
	 * @return a {@code String[]} containing the name of player one and player two.
	 */
	@Override
	public String[] getPlayerNames()
	{
		return new String[]
		{
			_playerOne.getName(),
			_playerTwo.getName()
		};
	}
	
	/**
	 * Checks if either participant has defaulted during the match.<br>
	 * This method updates player statuses and sends notification packets if a default occurs.
	 * @return {@code true} if at least one player has defaulted, otherwise {@code false}.
	 */
	@Override
	public boolean checkDefaulted()
	{
		SystemMessage reason;
		_playerOne.updatePlayer();
		_playerTwo.updatePlayer();
		
		reason = checkDefaulted(_playerOne.getPlayer());
		if (reason != null)
		{
			_playerOne.setDefaulted(true);
			final Player playerTwo = _playerTwo.getPlayer();
			if (playerTwo != null)
			{
				playerTwo.sendPacket(reason);
			}
		}
		
		reason = checkDefaulted(_playerTwo.getPlayer());
		if (reason != null)
		{
			_playerTwo.setDefaulted(true);
			final Player playerOne = _playerOne.getPlayer();
			if (playerOne != null)
			{
				playerOne.sendPacket(reason);
			}
		}
		
		return _playerOne.isDefaulted() || _playerTwo.isDefaulted();
	}
	
	/**
	 * Resets the damage values for both participants.<br>
	 * Sets {@code _damageP1} and {@code _damageP2} to {@code 0}.
	 */
	@Override
	public void resetDamage()
	{
		_damageP1 = 0;
		_damageP2 = 0;
	}
	
	/**
	 * Resets the final damage values for both participants.<br>
	 * This method sets {@code _damageP1Final} and {@code _damageP2Final} to {@code 0}.
	 */
	@Override
	public void resetDamageFinal()
	{
		_damageP1Final = 0;
		_damageP2Final = 0;
	}
	
	/**
	 * Saves the results of an olympiad fight to the database.<br>
	 * This method records participant details and match statistics.<br>
	 * It handles the SQL insertion into the {@code olympiad_fights} table.
	 * @param one The first participant in the fight.
	 * @param two The second participant in the fight.
	 * @param winner The ID of the winning participant.
	 * @param startTime The timestamp when the fight began.
	 * @param fightTime The total duration of the fight.
	 * @param type The category of the competition.
	 */
	protected void saveResults(Participant one, Participant two, int winner, long startTime, long fightTime, CompetitionType type)
	{
		final int winnerFormat1 = one.getPlayer().getOlympiadFightHistory().getWinnerFormat(one, two, winner);
		one.getPlayer().getOlympiadFightHistory().addOlympiadFight(two, winnerFormat1);
		final int winnerFormat2 = two.getPlayer().getOlympiadFightHistory().getWinnerFormat(one, two, winner);
		two.getPlayer().getOlympiadFightHistory().addOlympiadFight(one, winnerFormat2);
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO olympiad_fights (charOneId, charTwoId, charOneClass, charTwoClass, charOneLevel, charTwoLevel, winner, start, time, classed) values(?,?,?,?,?,?,?,?,?,?)"))
		{
			statement.setInt(1, one.getObjectId());
			statement.setInt(2, two.getObjectId());
			statement.setInt(3, one.getBaseClass());
			statement.setInt(4, two.getBaseClass());
			statement.setInt(5, one.getLevel());
			statement.setInt(6, two.getLevel());
			statement.setInt(7, winner);
			statement.setLong(8, startTime != 0 ? startTime : System.currentTimeMillis());
			statement.setLong(9, fightTime);
			statement.setInt(10, (type == CompetitionType.CLASSED ? 1 : 0));
			statement.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.SEVERE, "SQL exception while saving olympiad fight.", e);
		}
	}
	
	/**
	 * Restores the health and mana of all active players.<br>
	 * This method sets the current {@code Cp}, {@code Hp}, and {@code Mp} to their maximum values.<br>
	 * It checks if both {@code _playerOne} and {@code _playerTwo} are not {@code null} before applying changes.
	 */
	@Override
	protected void healPlayers()
	{
		final Player player1 = _playerOne.getPlayer();
		if (player1 != null)
		{
			player1.setCurrentCp(player1.getMaxCp());
			player1.setCurrentHp(player1.getMaxHp());
			player1.setCurrentMp(player1.getMaxMp());
		}
		
		final Player player2 = _playerTwo.getPlayer();
		if (player2 != null)
		{
			player2.setCurrentCp(player2.getMaxCp());
			player2.setCurrentHp(player2.getMaxHp());
			player2.setCurrentMp(player2.getMaxMp());
		}
	}
	
	/**
	 * Triggers various olympiad music skills for both players.<br>
	 * This method checks if {@code _playerOne} and {@code _playerTwo} are active.<br>
	 * It applies multiple {@code CommonSkill} effects to each player.
	 */
	@Override
	protected void buffPlayers()
	{
		final Player player1 = _playerOne.getPlayer();
		if (player1 != null)
		{
			SkillCaster.triggerCast(player1, player1, CommonSkill.OLYMPIAD_FEVER_MELODY.getSkill());
			SkillCaster.triggerCast(player1, player1, CommonSkill.OLYMPIAD_DRUMBEAT_OF_SERENITY.getSkill());
			SkillCaster.triggerCast(player1, player1, CommonSkill.OLYMPIAD_FANTASIA_HARMONY.getSkill());
		}
		
		final Player player2 = _playerTwo.getPlayer();
		if (player2 != null)
		{
			SkillCaster.triggerCast(player2, player2, CommonSkill.OLYMPIAD_FEVER_MELODY.getSkill());
			SkillCaster.triggerCast(player2, player2, CommonSkill.OLYMPIAD_DRUMBEAT_OF_SERENITY.getSkill());
			SkillCaster.triggerCast(player2, player2, CommonSkill.OLYMPIAD_FANTASIA_HARMONY.getSkill());
		}
	}
	
	/**
	 * Cleans up the state for players after round two.<br>
	 * This method schedules a task to run after 1000 milliseconds.<br>
	 * It checks if both {@code _playerOne} and {@code _playerTwo} are active and connected.<br>
	 * If they are valid, it calls {@code roundTwoClean} for each player.
	 */
	@Override
	protected void roundTwoCleanUp()
	{
		ThreadPool.schedule(() ->
		{
			final Player p = _playerOne.getPlayer();
			if ((p != null) && !_playerOne.isDefaulted() && !_playerOne.isDisconnected() && (p.getOlympiadGameId() == _stadiumId))
			{
				roundTwoClean(p);
			}
			
			final Player p2 = _playerTwo.getPlayer();
			if ((p2 != null) && !_playerTwo.isDefaulted() && !_playerTwo.isDisconnected() && (p2.getOlympiadGameId() == _stadiumId))
			{
				roundTwoClean(p2);
			}
		}, 1000);
	}
	
	/**
	 * Reverts the transformation state of players in a match.<br>
	 * This method checks if {@code _playerOne} or {@code _playerTwo} are transformed.<br>
	 * If they are, it calls {@code stopTransformation(true)} on their respective {@code Player} objects.
	 */
	@Override
	protected void untransformPlayers()
	{
		final Player player1 = _playerOne.getPlayer();
		if ((player1 != null) && player1.isTransformed())
		{
			player1.stopTransformation(true);
		}
		
		final Player player2 = _playerTwo.getPlayer();
		if ((player2 != null) && player2.isTransformed())
		{
			player2.stopTransformation(true);
		}
	}
	
	/**
	 * Sets the invulnerability status for all participants.<br>
	 * This method checks if {@code _playerOne} and {@code _playerTwo} are not {@code null}.<br>
	 * If they exist, it sets their invulnerability to {@code true}.
	 */
	@Override
	public void makePlayersInvul()
	{
		if (_playerOne.getPlayer() != null)
		{
			_playerOne.getPlayer().setInvul(true);
		}
		
		if (_playerTwo.getPlayer() != null)
		{
			_playerTwo.getPlayer().setInvul(true);
		}
	}
	
	/**
	 * Removes the invulnerability status from all players in the match.<br>
	 * This method checks if {@code _playerOne} and {@code _playerTwo} are active.<br>
	 * It sets their invulnerability state to {@code false}.
	 */
	@Override
	public void removePlayersInvul()
	{
		if (_playerOne.getPlayer() != null)
		{
			_playerOne.getPlayer().setInvul(false);
		}
		
		if (_playerTwo.getPlayer() != null)
		{
			_playerTwo.getPlayer().setInvul(false);
		}
	}
}
