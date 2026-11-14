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
package org.l2jmobius.gameserver.mechanics.undergroundColiseum;

import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.UndergroundColiseumConfig;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.UCTower;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.managers.games.UndergroundColiseumManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchUserDie;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a group of players participating in the Underground Coliseum.<br>
 * This class manages team-specific data and logic for matches within the {@code UCTower}.
 */
public class UCTeam
{
	public static final byte NOT_DECIDED = 0;
	public static final byte WIN = 1;
	public static final byte FAIL = 2;
	
	private final int _index;
	private final UCArena _baseArena;
	protected final int _x;
	protected final int _y;
	protected final int _z;
	private final int _npcId;
	private UCTower _tower = null;
	private Party _party;
	private int _killCount;
	private byte _status;
	private Party _lastParty;
	private int _consecutiveWins;
	private long _registerTime;
	
	/**
	 * Creates a new {@code UCTeam} instance with the specified coordinates and arena.<br>
	 * This constructor initializes the team status to {@code NOT_DECIDED}.
	 * @param index The unique identifier for this team.
	 * @param baseArena The {@code UCArena} associated with this team.
	 * @param x The X coordinate of the team spawn point.
	 * @param y The Y coordinate of the team spawn point.
	 * @param z The Z coordinate of the team spawn point.
	 * @param npcId The ID of the NPC associated with this team.
	 */
	public UCTeam(int index, UCArena baseArena, int x, int y, int z, int npcId)
	{
		_index = index;
		_baseArena = baseArena;
		_x = x;
		_y = y;
		_z = z;
		_npcId = npcId;
		
		setStatus(NOT_DECIDED);
	}
	
	/**
	 * Retrieves the time when this team was registered.
	 * @return The registration time as a {@code long}.
	 */
	public long getRegisterTime()
	{
		return _registerTime;
	}
	
	/**
	 * Updates the last known {@code Party} for this team.<br>
	 * This method stores the {@code Party} object in the internal field.
	 * @param party The {@code Party} to be set as the last party.
	 */
	public void setLastParty(Party party)
	{
		_lastParty = party;
	}
	
	/**
	 * Sets the registration time for this team.<br>
	 * The value is stored in a {@code long} format.
	 * @param time The new registration time to set.
	 */
	public void setRegisterTime(long time)
	{
		_registerTime = time;
	}
	
	/**
	 * Increments the count of consecutive wins for this team.<br>
	 * Updates the best team in the {@code UndergroundColiseumManager} if the win streak is greater than 1.<br>
	 * This check requires that a {@code Party} and its leader are currently assigned.
	 */
	public void increaseConsecutiveWins()
	{
		_consecutiveWins++;
		if ((_consecutiveWins > 1) && (_party != null) && (_party.getLeader() != null))
		{
			UndergroundColiseumManager.getInstance().updateBestTeam(_baseArena.getId(), _party.getLeader().getName(), _consecutiveWins);
		}
	}
	
	/**
	 * Retrieves the current number of consecutive wins.<br>
	 * This value tracks how many times the team has won in a row.
	 * @return The total count of consecutive wins as an {@code int}.
	 */
	public int getConsecutiveWins()
	{
		return _consecutiveWins;
	}
	
	/**
	 * Spawns the tower entity for this team.<br>
	 * This method checks if a tower already exists before creating one.<br>
	 * It uses the {@code _npcId} to retrieve the correct {@code NpcTemplate}.<br>
	 * The new {@code UCTower} is initialized and spawned at the specified coordinates.
	 */
	public void spawnTower()
	{
		if (_tower != null)
		{
			return;
		}
		
		final NpcTemplate template = NpcData.getInstance().getTemplate(_npcId);
		if (template != null)
		{
			_tower = new UCTower(this, template);
			_tower.setInvul(false);
			_tower.fullRestore();
			_tower.spawnMe(_x, _y, _z);
		}
	}
	
	/**
	 * Removes the tower associated with this team.<br>
	 * This method calls {@code deleteMe()} on the current tower object.<br>
	 * It then sets the internal tower reference to {@code null}.
	 */
	public void deleteTower()
	{
		if (_tower != null)
		{
			_tower.deleteMe();
			_tower = null;
		}
	}
	
	/**
	 * Handles the logic when a player is killed by another player.<br>
	 * Updates kill counts for both players and the opposing team.<br>
	 * Checks if the team has lost all members to determine the match winner.<br>
	 * Schedules a respawn task for the defeated player after a delay.
	 * @param player The player who was killed.
	 * @param killer The player who performed the kill.
	 */
	public void onKill(Player player, Player killer)
	{
		if ((player == null) || (killer == null) || (getParty() == null))
		{
			return;
		}
		
		final Party playerParty = player.getParty();
		if ((playerParty != null) && (playerParty == killer.getParty()))
		{
			return;
		}
		
		final UCTeam otherTeam = getOtherTeam();
		otherTeam.increaseKillCount();
		player.addDeathCountUC();
		killer.addKillCountUC();
		
		_baseArena.broadcastToAll(new ExPVPMatchUserDie(_baseArena));
		
		if (player.getUCState() == Player.UC_STATE_POINT)
		{
			for (UCPoint point : _baseArena.getPoints())
			{
				if (point.checkPlayer(player))
				{
					break;
				}
			}
		}
		
		if (_tower == null)
		{
			boolean flag = true;
			for (Player member : getParty().getMembers())
			{
				if ((member != null) && !member.isDead())
				{
					flag = false;
				}
			}
			
			if (flag)
			{
				setStatus(FAIL);
				otherTeam.setStatus(WIN);
				_baseArena.runTaskNow();
			}
			return;
		}
		
		ThreadPool.schedule(() ->
		{
			if (_tower == null)
			{
				return;
			}
			
			if (player.isDead())
			{
				resPlayer(player);
				player.teleToLocation(_x + Rnd.get(2, 50), _y + Rnd.get(10, 100), _z, player.getHeading(), true);
				player.getSummonedNpcs().forEach(summon ->
				{
					summon.abortAttack();
					summon.abortCast();
					if (!summon.isDead())
					{
						summon.setCurrentHp(summon.getMaxHp());
						summon.setCurrentMp(summon.getMaxMp());
					}
				});
			}
		}, UndergroundColiseumConfig.UC_RESS_TIME * 1000);
	}
	
	/**
	 * Increments the total number of kills for this team.<br>
	 * This method updates the internal {@code _killCount} variable.
	 */
	public void increaseKillCount()
	{
		_killCount++;
	}
	
	/**
	 * Resets the status of a {@code Player}.<br>
	 * This method restores experience, revives the character, and refills all resources.
	 * @param player The {@code Player} to be reset.
	 */
	public static void resPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		player.restoreExp(100.0);
		player.doRevive();
		player.setCurrentHpMp(player.getMaxHp(), player.getMaxMp());
		player.setCurrentCp(player.getMaxCp());
	}
	
	/**
	 * Resets the team data to its default state.<br>
	 * This method clears the associated {@code Party} and resets statistics like {@code _consecutiveWins}.<br>
	 * It also sets the status back to {@code NOT_DECIDED}.
	 */
	public void cleanUp()
	{
		if (getParty() != null)
		{
			getParty().setUCState(null);
			_party = null;
		}
		
		_party = null;
		_lastParty = null;
		_consecutiveWins = 0;
		setStatus(NOT_DECIDED);
		_killCount = 0;
	}
	
	/**
	 * Retrieves the current status of the team.<br>
	 * The status can be {@code NOT_DECIDED}, {@code WIN}, or {@code FAIL}.
	 * @return the current status as a {@code byte}
	 */
	public byte getStatus()
	{
		return _status;
	}
	
	/**
	 * Retrieves the base arena associated with this team.
	 * @return the {@code UCArena} object representing the base arena.
	 */
	public UCArena getBaseArena()
	{
		return _baseArena;
	}
	
	/**
	 * Calculates and distributes rewards to the team members.<br>
	 * This method applies a modifier based on the number of consecutive wins.<br>
	 * It handles PC Cafe points, clan reputation, fame, and items.
	 */
	public void computeReward()
	{
		if ((_lastParty == null) || (_lastParty != getOtherTeam().getParty()))
		{
			final List<UCReward> rewards = _baseArena.getRewards();
			double modifier = 1;
			switch (_consecutiveWins)
			{
				case 1:
				{
					modifier = 1.0;
					break;
				}
				case 2:
				{
					modifier = 1.06;
					break;
				}
				case 3:
				{
					modifier = 1.12;
					break;
				}
				case 4:
				{
					modifier = 1.18;
					break;
				}
				case 5:
				{
					modifier = 1.25;
					break;
				}
				case 6:
				{
					modifier = 1.27;
					break;
				}
				case 7:
				{
					modifier = 1.3;
					break;
				}
				case 8:
				{
					modifier = 1.32;
					break;
				}
				case 9:
				{
					modifier = 1.35;
					break;
				}
				case 10:
				{
					modifier = 1.37;
					break;
				}
				default:
				{
					if (_consecutiveWins > 10)
					{
						modifier = 1.4;
					}
					break;
				}
			}
			
			if ((rewards == null) || rewards.isEmpty())
			{
				return;
			}
			
			for (Player member : getParty().getMembers())
			{
				if (member != null)
				{
					for (UCReward reward : rewards)
					{
						final int rewardId = reward.getId();
						switch (rewardId)
						{
							case -100:
							{
								long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
								final int pcCafePoints = member.getPcCafePoints();
								if ((pcCafePoints + amount) > PremiumSystemConfig.PC_CAFE_MAX_POINTS)
								{
									amount = PremiumSystemConfig.PC_CAFE_MAX_POINTS - pcCafePoints;
								}
								
								final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_PA_POINTS_X_S1);
								sm.addInt((int) amount);
								member.sendPacket(sm);
								member.setPcCafePoints((int) (pcCafePoints + amount));
								member.sendPacket(new ExPCCafePointInfo(member.getPcCafePoints(), (int) amount, 1));
								break;
							}
							case -200:
							{
								if (member.getClan() != null)
								{
									final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
									member.getClan().addReputationScore((int) amount);
								}
								break;
							}
							case -300:
							{
								final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
								member.setFame((int) (member.getFame() + amount));
								final SystemMessage sm = new SystemMessage(SystemMessageId.PERSONAL_REPUTATION_S1);
								sm.addInt((int) amount);
								member.sendPacket(sm);
								member.updateUserInfo();
								break;
							}
							default:
							{
								if (rewardId > 0)
								{
									final long amount = reward.isAllowMidifier() ? (long) (reward.getAmount() * modifier) : reward.getAmount();
									member.addItem(ItemProcessType.REWARD, rewardId, amount, null, true);
								}
								break;
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Updates the current status of the team.<br>
	 * This method handles logic for winning or failing a match.<br>
	 * It triggers rewards and broadcasts messages based on the result.
	 * @param status The new {@code byte} status to set.
	 */
	public void setStatus(byte status)
	{
		_status = status;
		
		if (_status == WIN)
		{
			if (getIndex() == 0)
			{
				_baseArena.broadcastToAll(new SystemMessage(SystemMessageId.THE_BLUE_TEAM_IS_VICTORIOUS));
			}
			else
			{
				_baseArena.broadcastToAll(new SystemMessage(SystemMessageId.THE_RED_TEAM_IS_VICTORIOUS));
			}
		}
		
		switch (_status)
		{
			case NOT_DECIDED:
			{
				break;
			}
			case WIN:
			{
				increaseConsecutiveWins();
				computeReward();
				deleteTower();
				break;
			}
			case FAIL:
			{
				deleteTower();
				break;
			}
		}
	}
	
	/**
	 * Sends a network packet to all members of the current team.<br>
	 * This method checks if the {@code _party} is not {@code null}.<br>
	 * It iterates through every {@code Player} in the party and sends the packet.
	 * @param packet The {@code ServerPacket} to be sent to each team member.
	 */
	public void broadcastToTeam(ServerPacket packet)
	{
		final Party party = _party;
		if (party != null)
		{
			for (Player member : party.getMembers())
			{
				if (member != null)
				{
					member.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Retrieves the opposing team from the current arena.<br>
	 * This method uses the index of the other team to fetch the {@code UCTeam} object.
	 * @return The {@code UCTeam} representing the other side.
	 */
	public UCTeam getOtherTeam()
	{
		return _baseArena.getTeams()[getOtherTeamIndex()];
	}
	
	/**
	 * Retrieves the index of the opposing team.<br>
	 * This method returns {@code 1} if the current team index is {@code 0}.<br>
	 * It returns {@code 0} if the current team index is not {@code 0}.
	 * @return The index of the other team.
	 */
	public int getOtherTeamIndex()
	{
		return _index == 0 ? 1 : 0;
	}
	
	/**
	 * Retrieves the total number of kills for this team.
	 * @return the current {@code killCount} as an {@code int}.
	 */
	public int getKillCount()
	{
		return _killCount;
	}
	
	/**
	 * Sets the {@code Party} associated with this team.<br>
	 * If a previous party existed, its state is cleared.<br>
	 * The new party will be linked to this team if it is not {@code null}.
	 * @param party The {@code Party} to associate with this team.
	 */
	public void setParty(Party party)
	{
		final Party oldParty = _party;
		_party = party;
		if (oldParty != null)
		{
			oldParty.setUCState(null);
		}
		
		if (_party != null)
		{
			_party.setUCState(this);
		}
	}
	
	/**
	 * Retrieves the {@code Party} associated with this team.
	 * @return the {@code Party} object or {@code null} if no party is assigned.
	 */
	public Party getParty()
	{
		return _party;
	}
	
	/**
	 * Retrieves the unique index of this team.
	 * @return the current {@code int} index.
	 */
	public int getIndex()
	{
		return _index;
	}
}
