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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchInfo;

/**
 * Handles the background execution of olympiad-related game logic.<br>
 * This class implements {@code Runnable} to process periodic tasks for the olympiad system.
 * @author DS
 */
public class OlympiadGameTask implements Runnable
{
	protected static final Logger LOGGER = Logger.getLogger(OlympiadGameTask.class.getName());
	
	private static final int[] TELEPORT_TO_ARENA_TIMES =
	{
		120,
		60,
		30,
		15,
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	
	private static final int[] BATTLE_START_TIME_FIRST =
	{
		60,
		55,
		50,
		40,
		30,
		20,
		10,
		0
	};
	
	private static final int[] BATTLE_START_TIME_SECOND =
	{
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	
	private static final int[] BATTLE_END_TIME_SECOND =
	{
		120,
		60,
		30,
		10,
		5
	};
	
	private static final int[] TELEPORT_TO_TOWN_TIMES =
	{
		40,
		30,
		20,
		10,
		5,
		4,
		3,
		2,
		1,
		0
	};
	
	private final OlympiadStadium _stadium;
	private AbstractOlympiadGame _game;
	private OlympiadGameState _state = OlympiadGameState.IDLE;
	private boolean _needAnnounce = false;
	private int _countDown = 0;
	
	/**
	 * Creates a new {@code OlympiadGameTask} for a specific stadium.<br>
	 * This constructor initializes the task and registers it with the provided {@code OlympiadStadium}.
	 * @param stadium The {@code OlympiadStadium} where this game task will run.
	 */
	public OlympiadGameTask(OlympiadStadium stadium)
	{
		_stadium = stadium;
		_stadium.registerTask(this);
	}
	
	/**
	 * Checks if the olympiad game is currently active.<br>
	 * It returns {@code true} if the state is not {@code IDLE}.<br>
	 * It returns {@code false} if the game is in an idle state.
	 * @return {@code true} if the task is running, {@code false} otherwise.
	 */
	public boolean isRunning()
	{
		return _state != OlympiadGameState.IDLE;
	}
	
	/**
	 * Checks if the olympiad game is currently active.<br>
	 * It returns {@code true} if the state is between {@code GAME_STARTED} and {@code CLEANUP}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the game has started, {@code false} otherwise.
	 */
	public boolean isGameStarted()
	{
		return (_state.ordinal() >= OlympiadGameState.GAME_STARTED.ordinal()) && (_state.ordinal() <= OlympiadGameState.CLEANUP.ordinal());
	}
	
	/**
	 * Checks if the current olympiad battle is active.<br>
	 * This method returns {@code true} if the game state is currently set to {@code BATTLE_IN_PROGRESS}, {@code ROUND_2}, or {@code ROUND_3}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the battle has started; {@code false} otherwise.
	 */
	public boolean isBattleStarted()
	{
		return (_state == OlympiadGameState.BATTLE_IN_PROGRESS) || (_state == OlympiadGameState.ROUND_2) || (_state == OlympiadGameState.ROUND_3);
	}
	
	/**
	 * Checks if the current battle has ended.<br>
	 * It returns {@code true} if the game state is {@code TELEPORT_TO_TOWN}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if the battle is finished, {@code false} otherwise.
	 */
	public boolean isBattleFinished()
	{
		return _state == OlympiadGameState.TELEPORT_TO_TOWN;
	}
	
	/**
	 * Checks if a system announcement is pending.<br>
	 * This method resets the {@code _needAnnounce} flag to {@code false} if it was set to {@code true}.
	 * @return {@code true} if an announcement was successfully triggered, otherwise {@code false}.
	 */
	public boolean needAnnounce()
	{
		if (_needAnnounce)
		{
			_needAnnounce = false;
			return true;
		}
		
		return false;
	}
	
	/**
	 * Retrieves the {@code OlympiadStadium} associated with this task.
	 * @return the current {@code OlympiadStadium} object.
	 */
	public OlympiadStadium getStadium()
	{
		return _stadium;
	}
	
	/**
	 * Retrieves the current olympiad game instance.<br>
	 * This method returns the {@code AbstractOlympiadGame} associated with this task.
	 * @return The current {@code AbstractOlympiadGame} object or {@code null} if no game is attached.
	 */
	public AbstractOlympiadGame getGame()
	{
		return _game;
	}
	
	/**
	 * Links a new {@code AbstractOlympiadGame} to this task.<br>
	 * This method initializes the game state and starts the execution thread.<br>
	 * It prevents overwriting an active game that is not in the {@code IDLE} state.
	 * @param game The {@code AbstractOlympiadGame} instance to attach.
	 */
	public void attachGame(AbstractOlympiadGame game)
	{
		if ((game != null) && (_state != OlympiadGameState.IDLE))
		{
			LOGGER.warning("Attempt to overwrite non-finished game in state " + _state);
			return;
		}
		
		_game = game;
		_state = OlympiadGameState.BEGIN;
		_needAnnounce = false;
		ThreadPool.execute(this);
	}
	
	@Override
	public void run()
	{
		try
		{
			final String player1 = _game.getPlayerNames()[0];
			final String player2 = _game.getPlayerNames()[1];
			int delay = 1; // Schedule next call after 1s.
			switch (_state)
			{
				// Game created
				case BEGIN:
				{
					_state = OlympiadGameState.TELEPORT_TO_ARENA;
					_countDown = OlympiadConfig.OLYMPIAD_WAIT_TIME;
					break;
				}
				// Teleport to arena countdown.
				case TELEPORT_TO_ARENA:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_TAKEN_TO_THE_OLYMPIC_STADIUM_IN_S1_SEC);
						sm.addInt(_countDown);
						_game.broadcastPacket(sm);
					}
					
					if (_countDown == 1)
					{
						_game.untransformPlayers();
					}
					
					delay = getDelay(TELEPORT_TO_ARENA_TIMES);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.GAME_STARTED;
					}
					break;
				}
				// Game start, port players to arena.
				case GAME_STARTED:
				{
					_stadium.makeZonePvPForCharsInside(false);
					if (!startGame())
					{
						_state = OlympiadGameState.GAME_STOPPED;
						break;
					}
					
					_state = OlympiadGameState.BATTLE_COUNTDOWN_FIRST;
					_countDown = BATTLE_START_TIME_FIRST[0];
					_stadium.updateZoneInfoForObservers(); // TODO lion temp hack for remove old info from client about prevoius match
					delay = 5;
					break;
				}
				// Battle start countdown, first part (60-10).
				case BATTLE_COUNTDOWN_FIRST:
				{
					if (_countDown > 0)
					{
						if (_countDown == 10)
						{
							_game.healPlayers();
						}
						else
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.THE_MATCH_BEGINS_IN_S1_SEC);
							sm.addInt(_countDown);
							_stadium.broadcastPacket(sm);
						}
					}
					
					delay = getDelay(BATTLE_START_TIME_FIRST);
					if (_countDown <= 0)
					{
						_game.makePlayersInvul();
						_game.resetDamage();
						_game.resetDamageFinal();
						_stadium.openDoors();
						
						_state = OlympiadGameState.BATTLE_COUNTDOWN_SECOND;
						_countDown = BATTLE_START_TIME_SECOND[0];
						delay = getDelay(BATTLE_START_TIME_SECOND);
					}
					break;
				}
				// Battle start countdown, second part (10-0).
				case BATTLE_COUNTDOWN_SECOND:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.THE_MATCH_BEGINS_IN_S1_SEC);
						sm.addInt(_countDown);
						_stadium.broadcastPacket(sm);
					}
					
					delay = getDelay(BATTLE_START_TIME_SECOND);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.BATTLE_STARTED;
						_game.removePlayersInvul();
					}
					break;
				}
				// Beginning of the battle.
				case BATTLE_STARTED:
				{
					_countDown = (int) OlympiadConfig.OLYMPIAD_BATTLE / 1000;
					
					_game.broadcastPacket(new ExOlympiadMatchInfo(player1, player2, 0, 0, 1, 100));
					
					final SystemMessage round1 = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_ROUND_1);
					_stadium.broadcastPacket(round1);
					final SystemMessage start = new SystemMessage(SystemMessageId.HIDDEN_MSG_START_OLYMPIAD);
					_stadium.broadcastPacket(start);
					_game.broadcastOlympiadInfo(_stadium);
					_state = OlympiadGameState.ROUND_1; // Set state first, used in zone update.
					if (!startBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				case ROUND_1:
				{
					_state = OlympiadGameState.BATTLE_IN_PROGRESS; // Set state first, used in zone update.
					_countDown = (int) OlympiadConfig.OLYMPIAD_BATTLE / 1000;
					_stadium.makeZonePvPForCharsInside(true);
					if (!startBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				// Checks during battle
				case BATTLE_IN_PROGRESS:
				{
					_countDown -= 1;
					final int remaining = (int) OlympiadConfig.OLYMPIAD_BATTLE / 1000;
					for (int announceTime : BATTLE_END_TIME_SECOND)
					{
						if (announceTime == remaining)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.THE_GAME_ENDS_IN_S1_SEC);
							sm.addInt(announceTime);
							_stadium.broadcastPacket(sm);
							break;
						}
					}
					
					if (roundCheck() || (_countDown <= 0))
					{
						round1();
						_game.makePlayersInvul();
						_state = OlympiadGameState.WAIT_TIME_1;
						_stadium.makeZonePvPForCharsInside(false);
						_countDown = 20;
					}
					else if (checkBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				case WAIT_TIME_1:
				{
					_countDown -= 1;
					if (_countDown == 14)
					{
						_game.buffPlayers();
						_game.portPlayersToSpots(_stadium.getZone().getSpawns(), _stadium.getInstance());
						_game.broadcastOlympiadInfo(_stadium);
					}
					
					if (_countDown <= 0)
					{
						_game.removePlayersInvul();
						_state = OlympiadGameState.ROUND_2;
						_countDown = (int) OlympiadConfig.OLYMPIAD_BATTLE / 1000;
						final SystemMessage round2 = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_ROUND_2);
						_stadium.broadcastPacket(round2);
						final SystemMessage start = new SystemMessage(SystemMessageId.HIDDEN_MSG_START_OLYMPIAD);
						_stadium.broadcastPacket(start);
						_stadium.makeZonePvPForCharsInside(true);
						_game.broadcastOlympiadInfo(_stadium);
					}
					break;
				}
				case ROUND_2:
				{
					_countDown -= 1;
					final int remaining = (int) OlympiadConfig.OLYMPIAD_BATTLE / 1000;
					for (int announceTime : BATTLE_END_TIME_SECOND)
					{
						if (announceTime == remaining)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.THE_GAME_ENDS_IN_S1_SEC);
							sm.addInt(announceTime);
							_stadium.broadcastPacket(sm);
							break;
						}
					}
					
					if (roundCheck() || (_countDown <= 0))
					{
						round2();
						if (_game.isMatchEnd())
						{
							_state = OlympiadGameState.GAME_STOPPED;
							break;
						}
						
						_state = OlympiadGameState.WAIT_TIME_2;
						_game.makePlayersInvul();
						_stadium.makeZonePvPForCharsInside(false);
						_countDown = 20;
					}
					else if (checkBattle())
					{
						_state = OlympiadGameState.GAME_STOPPED;
					}
					break;
				}
				case WAIT_TIME_2:
				{
					_countDown -= 1;
					if (_countDown == 14)
					{
						_game.buffPlayers();
						_game.roundTwoCleanUp();
						_game.broadcastOlympiadInfo(_stadium);
						_game.portPlayersToSpots(_stadium.getZone().getSpawns(), _stadium.getInstance());
					}
					
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.ROUND_3;
						_game.removePlayersInvul();
						_countDown = (int) OlympiadConfig.OLYMPIAD_BATTLE / 1000;
						final SystemMessage round2 = new SystemMessage(SystemMessageId.HIDDEN_MSG_OLYMPIAD_ROUND_3);
						_stadium.broadcastPacket(round2);
						final SystemMessage start = new SystemMessage(SystemMessageId.HIDDEN_MSG_START_OLYMPIAD);
						_stadium.broadcastPacket(start);
						_stadium.makeZonePvPForCharsInside(true);
						_game.broadcastOlympiadInfo(_stadium);
					}
					break;
				}
				case ROUND_3:
				{
					_countDown -= 1;
					final int remaining = (int) OlympiadConfig.OLYMPIAD_BATTLE / 1000;
					for (int announceTime : BATTLE_END_TIME_SECOND)
					{
						if (announceTime == remaining)
						{
							final SystemMessage sm = new SystemMessage(SystemMessageId.THE_GAME_ENDS_IN_S1_SEC);
							sm.addInt(announceTime);
							_stadium.broadcastPacket(sm);
							break;
						}
					}
					
					if (roundCheck() || (_countDown <= 0) || checkBattle())
					{
						round3();
						_game.makePlayersInvul();
						_stadium.makeZonePvPForCharsInside(false);
					}
					break;
				}
				// End of the battle.
				case GAME_STOPPED:
				{
					_state = OlympiadGameState.TELEPORT_TO_TOWN;
					_countDown = TELEPORT_TO_TOWN_TIMES[0];
					stopGame();
					delay = getDelay(TELEPORT_TO_TOWN_TIMES);
					break;
				}
				// Teleport to town countdown.
				case TELEPORT_TO_TOWN:
				{
					if (_countDown > 0)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_WILL_BE_MOVED_BACK_TO_TOWN_IN_S1_SECOND_S);
						sm.addInt(_countDown);
						_game.broadcastPacket(sm);
					}
					
					delay = getDelay(TELEPORT_TO_TOWN_TIMES);
					if (_countDown <= 0)
					{
						_state = OlympiadGameState.CLEANUP;
					}
					break;
				}
				// Removals
				case CLEANUP:
				{
					cleanupGame();
					_state = OlympiadGameState.IDLE;
					_game = null;
					return;
				}
			}
			
			ThreadPool.schedule(this, delay * 1000);
		}
		catch (Exception e)
		{
			switch (_state)
			{
				case GAME_STOPPED:
				case TELEPORT_TO_TOWN:
				case CLEANUP:
				case IDLE:
				{
					LOGGER.warning("Unable to return players back in town, exception: " + e.getMessage());
					_state = OlympiadGameState.IDLE;
					_game = null;
					return;
				}
			}
			
			LOGGER.log(Level.WARNING, "Exception in " + _state + ", trying to port players back: " + e.getMessage(), e);
			_state = OlympiadGameState.GAME_STOPPED;
			ThreadPool.schedule(this, 1000);
		}
	}
	
	/**
	 * Calculates the required delay based on a sequence of times.<br>
	 * It compares the current {@code _countDown} value against the provided array.<br>
	 * The method updates the internal countdown state during calculation.
	 * @param times An array of integers representing the time sequence.
	 * @return The calculated delay as an integer.
	 */
	private int getDelay(int[] times)
	{
		int time;
		for (int i = 0; i < (times.length - 1); i++)
		{
			time = times[i];
			if (time >= _countDown)
			{
				continue;
			}
			
			final int delay = _countDown - time;
			_countDown = time;
			return delay;
		}
		
		// should not happens
		_countDown = -1;
		return 1;
	}
	
	/**
	 * Initializes the start of an olympiad game.<br>
	 * This method closes the stadium doors and teleports players to the arena.<br>
	 * It also handles buffer spawning and notifies the {@code OlympiadGameManager}.
	 * @return {@code true} if the game started successfully, or {@code false} otherwise.
	 */
	private boolean startGame()
	{
		try
		{
			// Checking for opponents and teleporting to arena.
			if (_game.checkDefaulted())
			{
				return false;
			}
			
			_stadium.closeDoors();
			if (_game.needBuffers())
			{
				_stadium.spawnBuffers();
			}
			
			if (!_game.portPlayersToArena(_stadium.getZone().getSpawns(), _stadium.getInstance()))
			{
				return false;
			}
			
			_game.removals();
			_needAnnounce = true;
			OlympiadGameManager.getInstance().startBattle(); // inform manager
			return true;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		return false;
	}
	
	/**
	 * Initiates the battle phase of the olympiad.<br>
	 * This method checks if buffers need to be deleted and starts the competition.<br>
	 * It broadcasts information to players if the start is successful.
	 * @return {@code true} if the battle started successfully, {@code false} otherwise.
	 */
	private boolean startBattle()
	{
		try
		{
			if (_game.needBuffers())
			{
				_stadium.deleteBuffers();
			}
			
			if (_game.checkBattleStatus() && _game.makeCompetitionStart())
			{
				// game successfully started
				_game.broadcastOlympiadInfo(_stadium);
				// _stadium.broadcastPacket(new SystemMessage(SystemMessageId.THE_MATCH_HAS_BEGUN_FIGHT));
				_stadium.updateZoneStatusForCharactersInside();
				return true;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		return false;
	}
	
	/**
	 * Checks if a round winner has been determined.<br>
	 * It calls the {@code roundWinner()} method on the current game instance.<br>
	 * If an error occurs, it logs a warning and returns {@code true}.
	 * @return {@code true} if a winner is found or if an exception occurs, {@code false} otherwise.
	 */
	private boolean roundCheck()
	{
		try
		{
			return _game.roundWinner();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		return true;
	}
	
	/**
	 * Handles the logic for checking the winner of round 1.<br>
	 * It calls {@code validateRound1Winner} on the current game instance.<br>
	 * Any exceptions during validation are logged as warnings.
	 */
	private void round1()
	{
		try
		{
			_game.validateRound1Winner(_stadium);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Handles the logic for the second round of the olympiad.<br>
	 * It validates the winner of {@code round2} using the current stadium.<br>
	 * Any errors during validation are logged as a warning.
	 */
	private void round2()
	{
		try
		{
			_game.validateRound2Winner(_stadium);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Handles the final logic for round 3 of the olympiad.<br>
	 * It validates the winner and makes players invulnerable.<br>
	 * The method also updates the zone status and stops the game state.
	 */
	private void round3()
	{
		try
		{
			_game.validateRound3Winner(_stadium);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.makePlayersInvul();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_stadium.updateZoneStatusForCharactersInside();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_state = OlympiadGameState.GAME_STOPPED;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Checks if the current battle has a winner.<br>
	 * It calls {@code haveWinner()} on the {@code _game} object.<br>
	 * If an error occurs, it logs a warning and returns {@code true}.
	 * @return {@code true} if there is a winner or if an exception occurred.
	 */
	private boolean checkBattle()
	{
		try
		{
			return _game.haveWinner();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		return true;
	}
	
	/**
	 * Stops the current game and performs cleanup.<br>
	 * This method validates the winner and clears active effects.<br>
	 * It also makes players invulnerable and updates the stadium zone status.
	 */
	private void stopGame()
	{
		try
		{
			_game.validateWinner(_stadium);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.cleanEffects();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.makePlayersInvul();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_stadium.updateZoneStatusForCharactersInside();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Resets the game state after it has finished.<br>
	 * This method removes invulnerability from players.<br>
	 * It restores player statuses and teleports them back to their original locations.<br>
	 * Finally, it clears all players and closes the stadium doors.
	 */
	private void cleanupGame()
	{
		try
		{
			_game.removePlayersInvul();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.playersStatusBack();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.portPlayersBack();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_game.clearPlayers();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
		
		try
		{
			_stadium.closeDoors();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
}
