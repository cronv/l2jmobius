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
package org.l2jmobius.gameserver.managers.games;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.AerialCleftConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.type.CleftZone;
import org.l2jmobius.gameserver.entity.zone.type.SpawnTerritory;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.cleft.AerialCleftTeam;
import org.l2jmobius.gameserver.mechanics.cleft.CleftListType;
import org.l2jmobius.gameserver.mechanics.cleft.CleftPlayer;
import org.l2jmobius.gameserver.mechanics.cleft.CleftStateType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExCleftList;
import org.l2jmobius.gameserver.network.serverpackets.ExCleftState;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This class runs the Aerial Cleft, the large scale battle two teams fight over the Rombarshya Territory.<br>
 * The battlefield only opens while one of the Gracia seeds is available, it starts once both teams are full and it stays closed for a while once it ends.
 * @author Altur
 */
public class AerialCleftManager
{
	private static final Logger LOGGER = Logger.getLogger(AerialCleftManager.class.getName());
	
	/** The blue team enters from the Vortex Gate of the Keucereus Alliance Base side. */
	private static final int BLUE_TEAM = 0;
	/** The red team enters from the Vortex Gate of the opposite side. */
	private static final int RED_TEAM = 1;
	
	/** Central compressor, worth the most points. */
	private static final int LARGE_COMPRESSOR = 22553;
	/** The Emery Converter trades the spoils of the battle while the winning team gathers them. */
	private static final int EMERY_CONVERTER = 32520;
	/** Where the Emery Converter appears once the battle is over. */
	private static final Location EMERY_CONVERTER_LOCATION = new Location(-213463, 245228, 2123);
	
	/** The stronghold each post guards, in the order of the posts. */
	private static final String[] POISON_ZONES =
	{
		"cleft_center_zone",
		"cleft_side_zone_003",
		"cleft_side_zone_001",
		"cleft_side_zone_002"
	};
	
	/** Every post of the battlefield, as {npc id, x, y, z}, with the compressor first and its garrison behind. */
	private static final int[][][] POSTS =
	{
		// @formatter:off
		{
			{LARGE_COMPRESSOR, -213463, 244896, 2053},
			{22558, -213888, 245344, 2500},
			{22558, -213888, 244464, 2500},
			{22558, -213072, 245344, 2500},
			{22558, -213072, 244448, 2500}
		},
		{
			{22554, -211648, 250400, 3616},
			{22557, -211952, 249408, 3800},
			{22558, -211344, 249584, 3800},
			{22558, -212368, 249904, 3800}
		},
		{
			{22555, -219104, 242704, 3005},
			{22557, -218240, 243280, 3300},
			{22558, -218240, 242656, 3300},
			{22558, -218832, 243536, 3300}
		},
		{
			{22556, -211773, 238864, 1905},
			{22557, -212418, 239482, 2100},
			{22558, -211398, 239681, 2100},
			{22558, -211980, 239936, 2100}
		}
		// @formatter:on
	};
	
	/** Aerial transformations, the only way to move around the battlefield. */
	private static final int[] FLIGHT_TRANSFORMATIONS =
	{
		840,
		841,
		842
	};
	
	/** The client is told the transformation becomes available in about 40 seconds. */
	private static final int FLIGHT_TRANSFORMATION_DELAY = 40000;
	/** A cleared post is rebuilt three minutes later, so a stronghold can be taken more than once. */
	private static final int POST_REBUILD_DELAY = 180000;
	/** Announcements are shown on the upper part of the screen for ten seconds. */
	private static final int SCREEN_MESSAGE_TIME = 10000;
	/** How many times the location of a star stone is drawn again when it lands on a banned pocket. */
	private static final int STAR_STONE_DRAW_ATTEMPTS = 10;
	
	/** The territories the winning team gathers star stones from. */
	private static final String[] STAR_STONE_TERRITORIES =
	{
		"cleft_star_stone_zone_1",
		"cleft_star_stone_zone_2",
		"cleft_star_stone_zone_3",
		"cleft_star_stone_zone_4",
		"cleft_star_stone_zone_5"
	};
	
	/** The pockets of the third territory star stones never float in. */
	private static final String[] STAR_STONE_BANNED_TERRITORIES =
	{
		"cleft_star_stone_zone_3_banned_1",
		"cleft_star_stone_zone_3_banned_2"
	};
	
	/** The height band of every star stone territory. */
	private static final int[][] STAR_STONE_HEIGHTS =
	{
		// @formatter:off
		{-38, 1962},
		{3167, 4667},
		{3291, 5291},
		{4150, 5950},
		{2354, 4354}
		// @formatter:on
	};
	
	/** The star stones of every territory, as {territory, npc id, amount}. */
	private static final int[][] STAR_STONE_SPAWNS =
	{
		// @formatter:off
		{0, 18687, 12},
		{0, 18688, 12},
		{0, 18689, 12},
		{1, 18684, 4},
		{1, 18685, 4},
		{1, 18686, 4},
		{1, 18687, 4},
		{1, 18688, 4},
		{1, 18689, 4},
		{1, 18690, 4},
		{1, 18691, 4},
		{1, 18692, 4},
		{2, 18690, 12},
		{2, 18691, 12},
		{2, 18692, 12},
		{3, 18684, 12},
		{3, 18685, 12},
		{3, 18686, 12},
		{4, 18684, 4},
		{4, 18685, 4},
		{4, 18686, 4},
		{4, 18687, 4},
		{4, 18688, 4},
		{4, 18689, 4},
		{4, 18690, 4},
		{4, 18691, 4},
		{4, 18692, 4}
		// @formatter:on
	};
	
	private final AerialCleftTeam[] _teams = new AerialCleftTeam[2];
	private final List<List<Spawn>> _posts = new ArrayList<>();
	private final List<Spawn> _collectSpawns = new ArrayList<>();
	
	private volatile EventState _state = EventState.DISABLED;
	private ScheduledFuture<?> _battleTask;
	private ScheduledFuture<?> _collectTask;
	private long _battleEndTime;
	
	private enum EventState
	{
		DISABLED,
		REGISTRATION,
		BATTLE,
		COLLECT,
		CLOSED
	}
	
	/**
	 * Creates the manager and reads the battlefield locations from the cleft zone.<br>
	 * The event stays disabled when the zone is missing, since there would be nowhere to send the players.
	 */
	protected AerialCleftManager()
	{
		_teams[BLUE_TEAM] = new AerialCleftTeam(BLUE_TEAM);
		_teams[RED_TEAM] = new AerialCleftTeam(RED_TEAM);
		for (int i = 0; i < POSTS.length; i++)
		{
			_posts.add(new ArrayList<>());
		}
		
		final String simpleName = getClass().getSimpleName();
		if (!AerialCleftConfig.CLEFT_ENABLED)
		{
			LOGGER.info(simpleName + ": Disabled.");
			return;
		}
		
		final CleftZone zone = ZoneManager.getInstance().getZoneByName("cleft_zone_territory", CleftZone.class);
		if (zone == null)
		{
			LOGGER.info(simpleName + ": Event can't be started because the cleft zone was not found.");
			return;
		}
		
		for (Location location : zone.getStartPoints(BLUE_TEAM))
		{
			_teams[BLUE_TEAM].addStartPoint(location);
		}
		
		for (Location location : zone.getBanishPoints(BLUE_TEAM))
		{
			_teams[BLUE_TEAM].addExitPoint(location);
		}
		
		for (Location location : zone.getStartPoints(RED_TEAM))
		{
			_teams[RED_TEAM].addStartPoint(location);
		}
		
		for (Location location : zone.getBanishPoints(RED_TEAM))
		{
			_teams[RED_TEAM].addExitPoint(location);
		}
		
		if (!_teams[BLUE_TEAM].hasLocations() || !_teams[RED_TEAM].hasLocations())
		{
			LOGGER.info(simpleName + ": Event can't be started because the cleft zone is missing start or banish points.");
			return;
		}
		
		_state = EventState.REGISTRATION;
		LOGGER.info(simpleName + ": Registration is open.");
	}
	
	/**
	 * Verifies if players can register for the next battle.
	 * @return {@code true} while registration is open, {@code false} otherwise.
	 */
	public boolean isRegistrationOpen()
	{
		return _state == EventState.REGISTRATION;
	}
	
	/**
	 * Verifies if the battle is being fought.
	 * @return {@code true} while the battle is running, {@code false} otherwise.
	 */
	public boolean isBattleStarted()
	{
		return _state == EventState.BATTLE;
	}
	
	/**
	 * Verifies if the winning team is collecting star stones.
	 * @return {@code true} during the collect period, {@code false} otherwise.
	 */
	public boolean isCollecting()
	{
		return _state == EventState.COLLECT;
	}
	
	/**
	 * Verifies if a player is taking part in the event, either registered or fighting.
	 * @param objectId The object id of the player to look for.
	 * @return {@code true} if the player takes part, {@code false} otherwise.
	 */
	public boolean isParticipant(int objectId)
	{
		return _teams[BLUE_TEAM].containsPlayer(objectId) || _teams[RED_TEAM].containsPlayer(objectId);
	}
	
	/**
	 * Gets the team a player belongs to.
	 * @param objectId The object id of the player to look for.
	 * @return {@code 0} for the blue team, {@code 1} for the red team, {@code -1} if the player does not take part.
	 */
	public int getParticipantTeamId(int objectId)
	{
		if (_teams[BLUE_TEAM].containsPlayer(objectId))
		{
			return BLUE_TEAM;
		}
		
		if (_teams[RED_TEAM].containsPlayer(objectId))
		{
			return RED_TEAM;
		}
		
		return -1;
	}
	
	/**
	 * Gets the amount of players registered for the next battle.
	 * @return The total participant count as an {@code int}.
	 */
	public int getParticipantCount()
	{
		return _teams[BLUE_TEAM].getPlayerCount() + _teams[RED_TEAM].getPlayerCount();
	}
	
	/**
	 * Gets the time left before the battle ends.
	 * @return The remaining time in seconds, or {@code 0} when no battle is running.
	 */
	public int getRemainingTime()
	{
		if (_battleEndTime == 0)
		{
			return 0;
		}
		
		final long remaining = _battleEndTime - System.currentTimeMillis();
		return remaining > 0 ? (int) (remaining / 1000) : 0;
	}
	
	/**
	 * Registers a player for the next battle.<br>
	 * The player joins the team with fewer members and the battle starts once both teams are full.
	 * @param player The {@code Player} to register.
	 */
	public synchronized void registerPlayer(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		final int objectId = player.getObjectId();
		if (!isRegistrationOpen() || isParticipant(objectId))
		{
			return;
		}
		
		final int teamId = _teams[BLUE_TEAM].getPlayerCount() > _teams[RED_TEAM].getPlayerCount() ? RED_TEAM : BLUE_TEAM;
		_teams[teamId].addPlayer(player);
		player.setRegisteredOnEvent(true);
		player.sendPacket(new ExCleftList(CleftListType.TOTAL, _teams[BLUE_TEAM], _teams[RED_TEAM]));
		broadcastPacket(new ExCleftList(CleftListType.ADD, player, teamId), objectId);
		
		if ((_teams[BLUE_TEAM].getPlayerCount() >= AerialCleftConfig.CLEFT_MIN_TEAM_PLAYERS) && (_teams[RED_TEAM].getPlayerCount() >= AerialCleftConfig.CLEFT_MIN_TEAM_PLAYERS))
		{
			startBattle();
		}
	}
	
	/**
	 * Removes a player from the event.<br>
	 * A battle left with an empty team ends right away.
	 * @param objectId The object id of the player to remove.
	 * @param teleport {@code true} to send the player out of the battlefield.
	 */
	public synchronized void removePlayer(int objectId, boolean teleport)
	{
		final int teamId = getParticipantTeamId(objectId);
		if (teamId == -1)
		{
			return;
		}
		
		final AerialCleftTeam team = _teams[teamId];
		final CleftPlayer cleftPlayer = team.removePlayer(objectId);
		if (cleftPlayer == null)
		{
			return;
		}
		
		final boolean wasCombatAerialTarget = cleftPlayer.isCombatAerialTarget();
		final Player player = cleftPlayer.getPlayer();
		if (player != null)
		{
			if (teleport && ((_state == EventState.BATTLE) || (_state == EventState.COLLECT)))
			{
				teleportOut(player, team);
			}
			else
			{
				releasePlayer(player);
			}
		}
		
		if (_state == EventState.REGISTRATION)
		{
			broadcastPacket(new ExCleftList(CleftListType.REMOVE, objectId, teamId), 0);
			return;
		}
		
		if (_state != EventState.BATTLE)
		{
			return;
		}
		
		if ((_teams[BLUE_TEAM].getPlayerCount() == 0) || (_teams[RED_TEAM].getPlayerCount() == 0))
		{
			endBattle();
			return;
		}
		
		if (wasCombatAerialTarget)
		{
			team.selectCombatAerialTarget();
			broadcastPacket(new ExCleftState(CleftStateType.CAT_UPDATE, getRemainingTime(), team), 0);
		}
	}
	
	/**
	 * Starts the battle.<br>
	 * Both teams are sent to their side of the battlefield, the posts are built and the aerial transformations become available shortly after.
	 */
	private synchronized void startBattle()
	{
		_state = EventState.BATTLE;
		_battleEndTime = System.currentTimeMillis() + (AerialCleftConfig.CLEFT_EVENT_TIME * 60000L);
		for (int i = 0; i < POSTS.length; i++)
		{
			spawnPost(i);
		}
		
		for (AerialCleftTeam team : _teams)
		{
			team.selectCombatAerialTarget();
		}
		
		for (AerialCleftTeam team : _teams)
		{
			for (CleftPlayer cleftPlayer : team.getPlayers())
			{
				final Player player = cleftPlayer.getPlayer();
				if (player == null)
				{
					continue;
				}
				
				cleftPlayer.setJoinTime(System.currentTimeMillis());
				player.sendPacket(new ExCleftList(CleftListType.CLOSE));
				teleportIn(player, team);
				player.sendPacket(SystemMessageId.THE_AERIAL_CLEFT_HAS_BEEN_ACTIVATED_FLIGHT_TRANSFORMATION_WILL_BE_POSSIBLE_IN_APPROXIMATELY_40_SECONDS);
				player.sendPacket(new ExCleftState(CleftStateType.TOTAL, getRemainingTime(), _teams[BLUE_TEAM], _teams[RED_TEAM]));
			}
		}
		
		_battleTask = ThreadPool.schedule(this::endBattle, AerialCleftConfig.CLEFT_EVENT_TIME * 60000L);
	}
	
	/**
	 * Ends the battle.<br>
	 * The losing team is sent out, both teams are rewarded and the winning team is left behind to gather star stones.
	 */
	private synchronized void endBattle()
	{
		if (_state != EventState.BATTLE)
		{
			return;
		}
		
		_state = EventState.COLLECT;
		cancelTask(_battleTask);
		_battleTask = null;
		despawnPosts();
		spawnCollectNpcs();
		
		final AerialCleftTeam winnerTeam = _teams[BLUE_TEAM].getPoints() >= _teams[RED_TEAM].getPoints() ? _teams[BLUE_TEAM] : _teams[RED_TEAM];
		final AerialCleftTeam loserTeam = winnerTeam == _teams[BLUE_TEAM] ? _teams[RED_TEAM] : _teams[BLUE_TEAM];
		broadcastPacket(new ExCleftState(CleftStateType.RESULT, winnerTeam, loserTeam), 0);
		rewardTeam(winnerTeam, true);
		rewardTeam(loserTeam, false);
		
		for (CleftPlayer cleftPlayer : loserTeam.getPlayers())
		{
			final Player player = cleftPlayer.getPlayer();
			if (player != null)
			{
				teleportOut(player, loserTeam);
			}
		}
		
		for (CleftPlayer cleftPlayer : winnerTeam.getPlayers())
		{
			final Player player = cleftPlayer.getPlayer();
			if ((player != null) && player.isDead())
			{
				player.doRevive(100.);
				player.teleToLocation(winnerTeam.getStartPoint(), true);
			}
		}
		
		loserTeam.clean();
		_collectTask = ThreadPool.schedule(this::endCollect, AerialCleftConfig.CLEFT_COLLECT_TIME * 60000L);
		ThreadPool.schedule(this::reopenRegistration, AerialCleftConfig.CLEFT_RELOAD_TIME * 60000L);
	}
	
	/**
	 * Closes the battlefield.<br>
	 * The winning team is sent out and the battlefield stays quiet until the cooldown that began with the battle is over.
	 */
	private synchronized void endCollect()
	{
		if (_state != EventState.COLLECT)
		{
			return;
		}
		
		_state = EventState.CLOSED;
		despawnCollectNpcs();
		
		for (AerialCleftTeam team : _teams)
		{
			for (CleftPlayer cleftPlayer : team.getPlayers())
			{
				final Player player = cleftPlayer.getPlayer();
				if (player != null)
				{
					player.sendPacket(SystemMessageId.THE_AERIAL_CLEFT_BATTLEGROUND_HAS_BEEN_CLOSED);
					teleportOut(player, team);
				}
			}
			
			team.clean();
		}
		
		_battleEndTime = 0;
		cancelTask(_collectTask);
		_collectTask = null;
	}
	
	/**
	 * Opens the registration again once the battlefield has cooled down.
	 */
	private synchronized void reopenRegistration()
	{
		if (_state == EventState.CLOSED)
		{
			_state = EventState.REGISTRATION;
		}
	}
	
	/**
	 * Rewards every member of a team that fought long enough.
	 * @param team The team to reward.
	 * @param winner {@code true} for the winning team, {@code false} for the losing one.
	 */
	private void rewardTeam(AerialCleftTeam team, boolean winner)
	{
		final long count = winner ? AerialCleftConfig.CLEFT_REWARD_COUNT_WINNER : AerialCleftConfig.CLEFT_REWARD_COUNT_LOSER;
		if ((AerialCleftConfig.CLEFT_REWARD_ID <= 0) || (count <= 0))
		{
			return;
		}
		
		for (CleftPlayer cleftPlayer : team.getPlayers())
		{
			final Player player = cleftPlayer.getPlayer();
			if ((player == null) || !hasParticipated(cleftPlayer))
			{
				continue;
			}
			
			player.addItem(ItemProcessType.REWARD, AerialCleftConfig.CLEFT_REWARD_ID, count, null, true);
		}
	}
	
	/**
	 * Verifies if a participant stayed long enough on the battlefield to be rewarded.<br>
	 * The clock of every participant starts when the battle does.
	 * @param cleftPlayer The record to check.
	 * @return {@code true} if the player fought for long enough, {@code false} otherwise.
	 */
	private boolean hasParticipated(CleftPlayer cleftPlayer)
	{
		return (System.currentTimeMillis() - cleftPlayer.getJoinTime()) >= (AerialCleftConfig.CLEFT_MIN_PARTICIPATION_TIME * 60000L);
	}
	
	/**
	 * Sends a player into the battlefield.
	 * @param player The {@code Player} to teleport.
	 * @param team The team the player belongs to.
	 */
	private void teleportIn(Player player, AerialCleftTeam team)
	{
		if (player.isTransformed())
		{
			player.untransform();
		}
		
		if (player.isDead())
		{
			player.doRevive(100.);
		}
		
		player.setTeam(team.getId() == BLUE_TEAM ? Team.BLUE : Team.RED);
		player.setOnEvent(true);
		player.setCanRevive(false);
		player.teleToLocation(team.getStartPoint(), true);
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentMp(player.getMaxMp());
		player.broadcastStatusUpdate();
		player.broadcastUserInfo();
		
		for (Skill skill : player.getAllSkills())
		{
			if (isFlightTransformation(skill.getId()))
			{
				player.disableSkill(skill, FLIGHT_TRANSFORMATION_DELAY);
			}
		}
	}
	
	/**
	 * Sends a player out of the battlefield.
	 * @param player The {@code Player} to teleport.
	 * @param team The team the player belonged to.
	 */
	private void teleportOut(Player player, AerialCleftTeam team)
	{
		if (player.isTransformed())
		{
			player.untransform();
		}
		
		if (player.isDead())
		{
			player.doRevive(100.);
		}
		
		releasePlayer(player);
		player.teleToLocation(team.getExitPoint(), true);
		player.broadcastUserInfo();
	}
	
	/**
	 * Clears everything the event sets on a player.
	 * @param player The {@code Player} to release.
	 */
	private void releasePlayer(Player player)
	{
		player.setTeam(Team.NONE);
		player.setOnEvent(false);
		player.setRegisteredOnEvent(false);
		player.setCanRevive(true);
	}
	
	/**
	 * Sends a dead participant back to the starting point of the team.
	 * @param player The {@code Player} to bring back.
	 * @param team The team the player belongs to.
	 */
	private synchronized void respawnPlayer(Player player, AerialCleftTeam team)
	{
		if (!isBattleStarted() || !team.containsPlayer(player.getObjectId()))
		{
			return;
		}
		
		teleportIn(player, team);
	}
	
	/**
	 * Verifies if a skill is one of the aerial transformations of the battlefield.
	 * @param skillId The skill id to check.
	 * @return {@code true} if the skill is an aerial transformation, {@code false} otherwise.
	 */
	private boolean isFlightTransformation(int skillId)
	{
		for (int transformation : FLIGHT_TRANSFORMATIONS)
		{
			if (transformation == skillId)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Handles the death of a participant.<br>
	 * The killer team scores, the victim goes back to its starting point and a new Combat Aerial Target is picked when the previous one falls.
	 * @param killer The {@code Creature} that killed the player.
	 * @param killed The killed {@code Player}.
	 */
	public void onKill(Creature killer, Player killed)
	{
		if ((killed == null) || !isBattleStarted())
		{
			return;
		}
		
		final int killedObjectId = killed.getObjectId();
		final int killedTeamId = getParticipantTeamId(killedObjectId);
		if (killedTeamId == -1)
		{
			return;
		}
		
		final AerialCleftTeam killedTeam = _teams[killedTeamId];
		final CleftPlayer killedPlayer = killedTeam.getPlayer(killedObjectId);
		if (killedPlayer == null)
		{
			return;
		}
		
		final boolean wasCombatAerialTarget = killedPlayer.isCombatAerialTarget();
		killedPlayer.increaseDeaths();
		ThreadPool.schedule(() -> respawnPlayer(killed, killedTeam), AerialCleftConfig.CLEFT_RESPAWN_DELAY * 1000L);
		
		if ((killer != null) && killer.isPlayer())
		{
			final int killerObjectId = killer.asPlayer().getObjectId();
			final int killerTeamId = getParticipantTeamId(killerObjectId);
			if ((killerTeamId != -1) && (killerTeamId != killedTeamId))
			{
				final AerialCleftTeam killerTeam = _teams[killerTeamId];
				final CleftPlayer killerRecord = killerTeam.getPlayer(killerObjectId);
				if (killerRecord != null)
				{
					killerRecord.increaseKills();
					killerTeam.addPoints(wasCombatAerialTarget ? AerialCleftConfig.CLEFT_CAT_POINTS : AerialCleftConfig.CLEFT_PLAYER_POINTS);
					broadcastPacket(new ExCleftState(CleftStateType.PVP_KILL, getRemainingTime(), _teams[BLUE_TEAM], _teams[RED_TEAM], killerRecord, killerTeamId, killedPlayer, killedTeamId), 0);
				}
			}
		}
		
		if (wasCombatAerialTarget)
		{
			killedTeam.selectCombatAerialTarget();
			broadcastPacket(new ExCleftState(CleftStateType.CAT_UPDATE, getRemainingTime(), killedTeam), 0);
		}
	}
	
	/**
	 * Handles the death of one of the npcs of the battlefield.<br>
	 * A destroyed compressor scores for the team of the killer, and a post left without anything standing is rebuilt three minutes later.
	 * @param npc The killed npc.
	 * @param killer The {@code Player} that killed it.
	 */
	public synchronized void onBattleNpcKilled(Npc npc, Player killer)
	{
		if ((npc == null) || !isBattleStarted())
		{
			return;
		}
		
		final int postIndex = getPostIndex(npc);
		if (postIndex == -1)
		{
			return;
		}
		
		if (POSTS[postIndex][0][0] == npc.getId())
		{
			scoreCompressor(npc, killer, postIndex);
		}
		
		if (isPostCleared(postIndex))
		{
			despawnPost(postIndex);
			ThreadPool.schedule(() -> rebuildPost(postIndex), POST_REBUILD_DELAY);
		}
	}
	
	/**
	 * Scores a destroyed compressor for the team of the killer and makes its stronghold leak poison.
	 * @param npc The destroyed compressor.
	 * @param killer The {@code Player} that destroyed it.
	 * @param postIndex The post the compressor belongs to.
	 */
	private void scoreCompressor(Npc npc, Player killer, int postIndex)
	{
		final int compressorId = npc.getId();
		broadcastPacket(new ExShowScreenMessage(getDestroyedMessage(compressorId), ExShowScreenMessage.TOP_CENTER, SCREEN_MESSAGE_TIME), 0);
		setPoisonZone(postIndex, true);
		
		if (killer == null)
		{
			return;
		}
		
		final int objectId = killer.getObjectId();
		final int teamId = getParticipantTeamId(objectId);
		if (teamId == -1)
		{
			return;
		}
		
		final AerialCleftTeam team = _teams[teamId];
		final CleftPlayer cleftPlayer = team.getPlayer(objectId);
		if (cleftPlayer == null)
		{
			return;
		}
		
		cleftPlayer.increaseCompressors();
		team.addPoints(compressorId == LARGE_COMPRESSOR ? AerialCleftConfig.CLEFT_LARGE_COMPRESSOR_POINTS : AerialCleftConfig.CLEFT_SMALL_COMPRESSOR_POINTS);
		broadcastPacket(new ExCleftState(CleftStateType.TOWER_DESTROY, getRemainingTime(), _teams[BLUE_TEAM], _teams[RED_TEAM], team, compressorId, cleftPlayer), 0);
	}
	
	/**
	 * Handles the disconnection of a participant.
	 * @param player The {@code Player} that logged out.
	 */
	public void onLogout(Player player)
	{
		if (player != null)
		{
			removePlayer(player.getObjectId(), false);
		}
	}
	
	/**
	 * Verifies if a player is allowed to leave the battlefield by other means than the Vortex Gate.
	 * @param objectId The object id of the player to check.
	 * @return {@code true} if the player may escape, {@code false} while the player is fighting.
	 */
	public boolean canEscape(int objectId)
	{
		if (!isBattleStarted() && !isCollecting())
		{
			return true;
		}
		
		return !isParticipant(objectId);
	}
	
	/**
	 * Gets the post an npc of the battlefield belongs to.
	 * @param npc The npc to look for.
	 * @return The index of the post, or {@code -1} if the npc is not one of ours.
	 */
	private int getPostIndex(Npc npc)
	{
		for (int i = 0; i < _posts.size(); i++)
		{
			for (Spawn spawn : _posts.get(i))
			{
				if (spawn.getLastSpawn() == npc)
				{
					return i;
				}
			}
		}
		
		return -1;
	}
	
	/**
	 * Verifies if every npc of a post is dead.
	 * @param postIndex The post to check.
	 * @return {@code true} if nothing of the post is left standing, {@code false} otherwise.
	 */
	private boolean isPostCleared(int postIndex)
	{
		for (Spawn spawn : _posts.get(postIndex))
		{
			final Npc npc = spawn.getLastSpawn();
			if ((npc != null) && !npc.isDead())
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Builds a post with its compressor and its garrison.<br>
	 * The stronghold stops leaking poison while its compressor is working.
	 * @param postIndex The post to build.
	 */
	private void spawnPost(int postIndex)
	{
		for (int[] entry : POSTS[postIndex])
		{
			final Spawn spawn = createSpawn(entry[0], entry[1], entry[2], entry[3]);
			if (spawn != null)
			{
				_posts.get(postIndex).add(spawn);
			}
		}
		
		setPoisonZone(postIndex, false);
		broadcastPacket(new ExShowScreenMessage(getWorkingMessage(POSTS[postIndex][0][0]), ExShowScreenMessage.TOP_CENTER, SCREEN_MESSAGE_TIME), 0);
	}
	
	/**
	 * Rebuilds a post that was cleared during the battle.
	 * @param postIndex The post to rebuild.
	 */
	private synchronized void rebuildPost(int postIndex)
	{
		if (isBattleStarted())
		{
			spawnPost(postIndex);
		}
	}
	
	/**
	 * Removes a post from the battlefield.
	 * @param postIndex The post to remove.
	 */
	private void despawnPost(int postIndex)
	{
		for (Spawn spawn : _posts.get(postIndex))
		{
			deleteSpawn(spawn);
		}
		
		_posts.get(postIndex).clear();
	}
	
	/**
	 * Removes every post from the battlefield and stops the poison of every stronghold.
	 */
	private void despawnPosts()
	{
		for (int i = 0; i < POSTS.length; i++)
		{
			despawnPost(i);
			setPoisonZone(i, false);
		}
	}
	
	/**
	 * Turns the poison of a stronghold on or off.<br>
	 * A stronghold left without its compressor leaks poison until the post is rebuilt.
	 * @param postIndex The post of the stronghold.
	 * @param enabled {@code true} to make the stronghold leak poison, {@code false} to stop it.
	 */
	private void setPoisonZone(int postIndex, boolean enabled)
	{
		final ZoneType zone = ZoneManager.getInstance().getZoneByName(POISON_ZONES[postIndex]);
		if (zone != null)
		{
			zone.setEnabled(enabled);
		}
	}
	
	/**
	 * Scatters the star stones the winning team gathers and calls the Emery Converter to trade them.
	 */
	private void spawnCollectNpcs()
	{
		for (int[] entry : STAR_STONE_SPAWNS)
		{
			for (int i = 0; i < entry[2]; i++)
			{
				spawnStarStone(entry[0], entry[1]);
			}
		}
		
		final Spawn converter = createSpawn(EMERY_CONVERTER, EMERY_CONVERTER_LOCATION.getX(), EMERY_CONVERTER_LOCATION.getY(), EMERY_CONVERTER_LOCATION.getZ());
		if (converter != null)
		{
			_collectSpawns.add(converter);
		}
	}
	
	/**
	 * Scatters a single star stone inside one of the territories.
	 * @param territoryIndex The territory the star stone floats in.
	 * @param npcId The npc id of the star stone.
	 */
	private void spawnStarStone(int territoryIndex, int npcId)
	{
		final SpawnTerritory territory = ZoneManager.getInstance().getSpawnTerritory(STAR_STONE_TERRITORIES[territoryIndex]);
		if (territory == null)
		{
			return;
		}
		
		for (int attempt = 0; attempt < STAR_STONE_DRAW_ATTEMPTS; attempt++)
		{
			final Location location = territory.getRandomPoint();
			if ((location == null) || isBannedLocation(location))
			{
				continue;
			}
			
			final Spawn spawn = createSpawn(npcId, location.getX(), location.getY(), Rnd.get(STAR_STONE_HEIGHTS[territoryIndex][0], STAR_STONE_HEIGHTS[territoryIndex][1]));
			if (spawn != null)
			{
				_collectSpawns.add(spawn);
			}
			
			return;
		}
	}
	
	/**
	 * Verifies if a location falls inside one of the pockets star stones never float in.
	 * @param location The location to check.
	 * @return {@code true} if the location is banned, {@code false} otherwise.
	 */
	private boolean isBannedLocation(Location location)
	{
		for (String name : STAR_STONE_BANNED_TERRITORIES)
		{
			final SpawnTerritory territory = ZoneManager.getInstance().getSpawnTerritory(name);
			if ((territory != null) && territory.isInsideZone(location.getX(), location.getY()))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Removes the star stones that were not gathered and the Emery Converter.
	 */
	private void despawnCollectNpcs()
	{
		for (Spawn spawn : _collectSpawns)
		{
			deleteSpawn(spawn);
		}
		
		_collectSpawns.clear();
	}
	
	/**
	 * Spawns one npc of the battlefield.
	 * @param npcId The npc id to spawn.
	 * @param x The x coordinate.
	 * @param y The y coordinate.
	 * @param z The z coordinate.
	 * @return The created {@code Spawn}, or {@code null} if it could not be created.
	 */
	private Spawn createSpawn(int npcId, int x, int y, int z)
	{
		try
		{
			final Spawn spawn = new Spawn(npcId);
			spawn.setXYZ(x, y, z);
			spawn.setAmount(1);
			spawn.setHeading(0);
			spawn.setRespawnDelay(1);
			SpawnTable.getInstance().addSpawn(spawn);
			spawn.init();
			spawn.stopRespawn();
			return spawn;
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not spawn npc " + npcId + "!", e);
			return null;
		}
	}
	
	/**
	 * Removes one npc of the battlefield.
	 * @param spawn The spawn to remove.
	 */
	private void deleteSpawn(Spawn spawn)
	{
		SpawnTable.getInstance().removeSpawn(spawn);
		spawn.stopRespawn();
		
		final Npc npc = spawn.getLastSpawn();
		if (npc != null)
		{
			npc.deleteMe();
		}
	}
	
	/**
	 * Gets the announcement shown when a compressor starts working.
	 * @param npcId The npc id of the compressor.
	 * @return The {@code NpcStringId} of the announcement.
	 */
	private NpcStringId getWorkingMessage(int npcId)
	{
		switch (npcId)
		{
			case 22554:
			{
				return NpcStringId.THE_1ST_COMPRESSOR_IS_WORKING;
			}
			case 22555:
			{
				return NpcStringId.THE_2ND_COMPRESSOR_IS_WORKING;
			}
			case 22556:
			{
				return NpcStringId.THE_3RD_COMPRESSOR_IS_WORKING;
			}
			default:
			{
				return NpcStringId.THE_CENTRAL_COMPRESSOR_IS_WORKING;
			}
		}
	}
	
	/**
	 * Gets the announcement shown when a compressor is destroyed.
	 * @param npcId The npc id of the compressor.
	 * @return The {@code NpcStringId} of the announcement.
	 */
	private NpcStringId getDestroyedMessage(int npcId)
	{
		switch (npcId)
		{
			case 22554:
			{
				return NpcStringId.THE_1ST_COMPRESSOR_HAS_BEEN_DESTROYED;
			}
			case 22555:
			{
				return NpcStringId.THE_2ND_COMPRESSOR_HAS_BEEN_DESTROYED;
			}
			case 22556:
			{
				return NpcStringId.THE_3RD_COMPRESSOR_HAS_BEEN_DESTROYED;
			}
			default:
			{
				return NpcStringId.THE_CENTRAL_COMPRESSOR_HAS_BEEN_DESTROYED;
			}
		}
	}
	
	/**
	 * Sends a packet to every participant of the event.
	 * @param packet The {@code ServerPacket} to send.
	 * @param excludedObjectId The object id of a player to skip, or {@code 0} to send it to everyone.
	 */
	private void broadcastPacket(ServerPacket packet, int excludedObjectId)
	{
		for (AerialCleftTeam team : _teams)
		{
			for (CleftPlayer cleftPlayer : team.getPlayers())
			{
				final Player player = cleftPlayer.getPlayer();
				if ((player != null) && (player.getObjectId() != excludedObjectId))
				{
					player.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Cancels a scheduled task when there is one.
	 * @param task The task to cancel.
	 */
	private void cancelTask(ScheduledFuture<?> task)
	{
		if (task != null)
		{
			task.cancel(false);
		}
	}
	
	public static AerialCleftManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AerialCleftManager INSTANCE = new AerialCleftManager();
	}
}
