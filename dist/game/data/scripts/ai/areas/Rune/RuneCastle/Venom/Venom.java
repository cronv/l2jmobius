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
package ai.areas.Rune.RuneCastle.Venom;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeFinish;
import org.l2jmobius.gameserver.mechanics.events.holders.sieges.OnCastleSiegeStart;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class defines the artificial intelligence behavior for {@code Venom} NPCs located in Rune Castle.<br>
 * It handles the specific logic and actions these entities perform within their designated area.
 * @author nonom, MELERIX
 */
public class Venom extends Script
{
	private static final int CASTLE = 8; // Rune
	
	private static final int VENOM = 29054;
	private static final int TELEPORT_CUBE = 29055;
	private static final int DUNGEON_KEEPER = 35506;
	
	private static final byte ALIVE = 0;
	private static final byte DEAD = 1;
	
	private static final int HOURS_BEFORE = 24;
	
	private static final Location[] TARGET_TELEPORTS =
	{
		new Location(12860, -49158, 976),
		new Location(14878, -51339, 1024),
		new Location(15674, -49970, 864),
		new Location(15696, -48326, 864),
		new Location(14873, -46956, 1024),
		new Location(12157, -49135, -1088),
		new Location(12875, -46392, -288),
		new Location(14087, -46706, -288),
		new Location(14086, -51593, -288),
		new Location(12864, -51898, -288),
		new Location(15538, -49153, -1056),
		new Location(17001, -49149, -1064)
	};
	
	private static final Location TRHONE = new Location(11025, -49152, -537);
	private static final Location DUNGEON = new Location(11882, -49216, -3008);
	private static final Location TELEPORT = new Location(12589, -49044, -3008);
	private static final Location CUBE = new Location(12047, -49211, -3009);
	
	private static final SkillHolder VENOM_STRIKE = new SkillHolder(4993, 1);
	private static final SkillHolder SONIC_STORM = new SkillHolder(4994, 1);
	private static final SkillHolder VENOM_TELEPORT = new SkillHolder(4995, 1);
	private static final SkillHolder RANGE_TELEPORT = new SkillHolder(4996, 1);
	
	private Npc _venom;
	private Npc _massymore;
	
	private Location _loc;
	
	private boolean _aggroMode = false;
	private boolean _prisonIsOpen = false;
	
	// @formatter:off
	private static final int[] TARGET_TELEPORTS_OFFSET =
	{
		650, 100, 100, 100, 100, 650, 200, 200, 200, 200, 200, 650
	};
	// @formatter:on
	
	private static List<Player> _targets = new ArrayList<>();
	
	/**
	 * Private constructor for the {@code Venom} class.<br>
	 * This constructor initializes all necessary NPC IDs and event listeners.<br>
	 * It also checks if the prison is open based on the current siege date.
	 */
	private Venom()
	{
		addStartNpc(DUNGEON_KEEPER, TELEPORT_CUBE);
		addFirstTalkId(DUNGEON_KEEPER, TELEPORT_CUBE);
		addTalkId(DUNGEON_KEEPER, TELEPORT_CUBE);
		addSpawnId(VENOM, DUNGEON_KEEPER);
		addSpellFinishedId(VENOM);
		addAttackId(VENOM);
		addKillId(VENOM);
		addAggroRangeEnterId(VENOM);
		setCastleSiegeStartId(this::onSiegeStart, CASTLE);
		setCastleSiegeFinishId(this::onSiegeFinish, CASTLE);
		
		final long currentTime = System.currentTimeMillis();
		final long startSiegeDate = CastleManager.getInstance().getCastleById(CASTLE).getSiegeDate().getTimeInMillis();
		final long openingDungeonDate = startSiegeDate - (HOURS_BEFORE * 360000);
		if ((currentTime > openingDungeonDate) && (currentTime < startSiegeDate))
		{
			_prisonIsOpen = true;
		}
	}
	
	/**
	 * Handles the initial interaction between a player and an {@code Npc}.<br>
	 * It returns the path to the dialogue file based on the {@code Npc} ID.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the HTML file path for the dialogue.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * Handles the interaction between a {@code Player} and an {@code Npc}.<br>
	 * It checks the {@code Npc} ID to perform specific actions like teleporting.
	 * @param npc The {@code Npc} being interacted with.
	 * @param talker The {@code Player} who initiated the conversation.
	 * @return A string representing the dialogue or action result.
	 */
	@Override
	public String onTalk(Npc npc, Player talker)
	{
		switch (npc.getId())
		{
			case TELEPORT_CUBE:
			{
				talker.teleToLocation(TeleportWhereType.TOWN);
				break;
			}
			case DUNGEON_KEEPER:
			{
				if (_prisonIsOpen)
				{
					talker.teleToLocation(TELEPORT);
				}
				else
				{
					return "35506-02.html";
				}
				break;
			}
		}
		
		return super.onTalk(npc, talker);
	}
	
	/**
	 * Handles specific script events for the Venom NPC.<br>
	 * This method processes logic for tower checks, raid status, and cube despawning.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The name of the processed {@code event}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "tower_check":
			{
				if (CastleManager.getInstance().getCastleById(CASTLE).getSiege().getControlTowerCount() <= 1)
				{
					changeLocation(MoveTo.THRONE);
					_massymore.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.OH_NO_THE_DEFENSES_HAVE_FAILED_IT_IS_TOO_DANGEROUS_TO_REMAIN_INSIDE_THE_CASTLE_FLEE_EVERY_MAN_FOR_HIMSELF);
					cancelQuestTimer("tower_check", npc, null);
					startQuestTimer("raid_check", 10000, npc, null, true);
				}
				break;
			}
			case "raid_check":
			{
				if (!npc.isInsideZone(ZoneId.SIEGE) && !npc.isTeleporting())
				{
					npc.teleToLocation(_loc);
				}
				break;
			}
			case "cube_despawn":
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
				break;
			}
		}
		
		return event;
	}
	
	/**
	 * Handles logic when an {@code Npc} enters the aggro range of a {@code Player}.<br>
	 * This method determines if the {@code Player} should be added to the target list.<br>
	 * It ignores actions if {@code isSummon} is {@code true}.
	 * @param npc The {@code Npc} entity that entered the aggro range.
	 * @param player The {@code Player} who triggered the aggro.
	 * @param isSummon A boolean indicating if the action was triggered by a summon.
	 */
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (isSummon)
		{
			return;
		}
		
		if (_aggroMode && (_targets.size() < 10) && (getRandom(3) < 1) && !player.isDead())
		{
			_targets.add(player);
		}
	}
	
	/**
	 * This method is called when a castle siege begins.<br>
	 * It resets the AI state and prepares the {@code _venom} entity.<br>
	 * The {@code _venom} entity is healed and its skills are enabled.
	 * @param event The {@code OnCastleSiegeStart} event data.
	 */
	public void onSiegeStart(OnCastleSiegeStart event)
	{
		_aggroMode = true;
		_prisonIsOpen = false;
		if ((_venom != null) && !_venom.isDead())
		{
			_venom.setCurrentHp(_venom.getMaxHp());
			_venom.setCurrentMp(_venom.getMaxMp());
			_venom.enableSkill(VENOM_TELEPORT.getSkill());
			_venom.enableSkill(RANGE_TELEPORT.getSkill());
			startQuestTimer("tower_check", 30000, _venom, null, true);
		}
	}
	
	/**
	 * This method is called when a castle siege ends.<br>
	 * It resets the {@code _aggroMode} to {@code false}.<br>
	 * If the {@code _venom} entity is alive, it moves to the prison and disables specific skills.<br>
	 * It also updates the status to {@code ALIVE} and cancels active quest timers.
	 * @param event The {@code OnCastleSiegeFinish} event data.
	 */
	public void onSiegeFinish(OnCastleSiegeFinish event)
	{
		_aggroMode = false;
		if ((_venom != null) && !_venom.isDead())
		{
			changeLocation(MoveTo.PRISON);
			_venom.disableSkill(VENOM_TELEPORT.getSkill(), -1);
			_venom.disableSkill(RANGE_TELEPORT.getSkill(), -1);
		}
		
		updateStatus(ALIVE);
		cancelQuestTimer("tower_check", _venom, null);
		cancelQuestTimer("raid_check", _venom, null);
	}
	
	/**
	 * Handles logic after a specific skill is cast by an {@code Npc}.<br>
	 * This method checks the {@code Skill} ID to trigger teleports or stop aggression.
	 * @param npc The {@code Npc} that performed the action.
	 * @param player The {@code Player} who was targeted or involved in the skill.
	 * @param skill The {@code Skill} object used for the calculation.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		switch (skill.getId())
		{
			case 4222:
			{
				npc.teleToLocation(_loc);
				break;
			}
			case 4995:
			{
				teleportTarget(player);
				npc.asAttackable().stopHating(player);
				break;
			}
			case 4996:
			{
				teleportTarget(player);
				npc.asAttackable().stopHating(player);
				if ((_targets != null) && !_targets.isEmpty())
				{
					for (Player target : _targets)
					{
						final long x = player.getX() - target.getX();
						final long y = player.getY() - target.getY();
						final long z = player.getZ() - target.getZ();
						final long range = 250;
						if (((x * x) + (y * y) + (z * z)) <= (range * range))
						{
							teleportTarget(target);
							npc.asAttackable().stopHating(target);
						}
					}
					
					_targets.clear();
				}
				break;
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It initializes specific behaviors for the {@code DUNGEON_KEEPER} and {@code VENOM} NPCs.<br>
	 * It also handles reviving the NPC or deleting it if its status is {@code DEAD}.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		switch (npc.getId())
		{
			case DUNGEON_KEEPER:
			{
				_massymore = npc;
				break;
			}
			case VENOM:
			{
				_venom = npc;
				_loc = _venom.getLocation();
				_venom.disableSkill(VENOM_TELEPORT.getSkill(), -1);
				_venom.disableSkill(RANGE_TELEPORT.getSkill(), -1);
				_venom.doRevive();
				npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.WHO_DARES_TO_COVET_THE_THRONE_OF_OUR_CASTLE_LEAVE_IMMEDIATELY_OR_YOU_WILL_PAY_THE_PRICE_OF_YOUR_AUDACITY_WITH_YOUR_VERY_OWN_BLOOD);
				_venom.asAttackable().setCanReturnToSpawnPoint(false);
				if (checkStatus() == DEAD)
				{
					_venom.deleteMe();
				}
				break;
			}
		}
		
		if (checkStatus() == DEAD)
		{
			npc.deleteMe();
		}
		else
		{
			npc.doRevive();
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method determines which skill the {@code Npc} should cast based on health and distance.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summoned unit.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final double distance = npc.calculateDistance2D(attacker);
		if (_aggroMode && (getRandom(100) < 25))
		{
			npc.setTarget(attacker);
			npc.doCast(VENOM_TELEPORT.getSkill());
		}
		else if (_aggroMode && (npc.getCurrentHp() < (npc.getMaxHp() / 3)) && (getRandom(100) < 25) && !npc.isCastingNow(SkillCaster::isAnyNormalType))
		{
			npc.setTarget(attacker);
			npc.doCast(RANGE_TELEPORT.getSkill());
		}
		else if ((distance > 300) && (getRandom(100) < 10) && !npc.isCastingNow(SkillCaster::isAnyNormalType))
		{
			npc.setTarget(attacker);
			npc.doCast(VENOM_STRIKE.getSkill());
		}
		else if ((getRandom(100) < 10) && !npc.isCastingNow(SkillCaster::isAnyNormalType))
		{
			npc.setTarget(attacker);
			npc.doCast(SONIC_STORM.getSkill());
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is killed.<br>
	 * Updates the status to {@code DEAD}.<br>
	 * Broadcasts a specific message to all players.<br>
	 * Spawns a teleport cube if the siege is not active.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who performed the kill.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		updateStatus(DEAD);
		npc.broadcastSay(ChatType.NPC_SHOUT, NpcStringId.IT_S_NOT_OVER_YET_IT_WON_T_BE_OVER_LIKE_THIS_NEVER);
		if (!CastleManager.getInstance().getCastleById(CASTLE).getSiege().isInProgress())
		{
			final Npc cube = addSpawn(TELEPORT_CUBE, CUBE, false, 0);
			startQuestTimer("cube_despawn", 120000, cube, null);
		}
		
		cancelQuestTimer("raid_check", npc, null);
	}
	
	/**
	 * Updates the current location of the venom entity.<br>
	 * This method handles moving the venom to specific areas like the throne or prison.<br>
	 * It also manages spawning a new venom if the current one is dead or missing.
	 * @param loc The {@code MoveTo} destination type to determine the target area.
	 */
	private void changeLocation(MoveTo loc)
	{
		switch (loc)
		{
			case THRONE:
			{
				_venom.teleToLocation(TRHONE, false);
				break;
			}
			case PRISON:
			{
				if ((_venom == null) || _venom.isDead() || _venom.isDecayed())
				{
					_venom = addSpawn(VENOM, DUNGEON, false, 0);
				}
				else
				{
					_venom.teleToLocation(DUNGEON, false);
				}
				
				cancelQuestTimer("raid_check", _venom, null);
				cancelQuestTimer("tower_check", _venom, null);
				break;
			}
		}
		
		_loc.setLocation(_venom.getLocation());
	}
	
	/**
	 * Moves a target player to a random location.<br>
	 * This method checks if the {@code Player} is alive before teleporting.<br>
	 * It also resets the player's AI intention to idle.
	 * @param player The {@code Player} object to be teleported.
	 */
	private void teleportTarget(Player player)
	{
		if ((player != null) && !player.isDead())
		{
			final int rnd = getRandom(11);
			player.teleToLocation(TARGET_TELEPORTS[rnd], TARGET_TELEPORTS_OFFSET[rnd]);
			player.getAI().setIntentionIdle();
		}
	}
	
	/**
	 * Checks the current status of the Venom.<br>
	 * It retrieves the value from {@code GlobalVariablesManager}.<br>
	 * If no variable exists, it initializes it to {@code 0}.
	 * @return The current status as an {@code int}.
	 */
	private int checkStatus()
	{
		int checkStatus = ALIVE;
		if (GlobalVariablesManager.getInstance().hasVariable("VenomStatus"))
		{
			checkStatus = GlobalVariablesManager.getInstance().getInt("VenomStatus");
		}
		else
		{
			GlobalVariablesManager.getInstance().set("VenomStatus", 0);
		}
		
		return checkStatus;
	}
	
	/**
	 * Updates the global status of the Venom.<br>
	 * This method saves the new value to the {@code GlobalVariablesManager}.
	 * @param status The new status value to be stored as a string.
	 */
	private void updateStatus(int status)
	{
		GlobalVariablesManager.getInstance().set("VenomStatus", Integer.toString(status));
	}
	
	private enum MoveTo
	{
		THRONE,
		PRISON
	}
	
	/**
	 * This is the entry point for the {@code Venom} script.<br>
	 * It initializes a new instance of the {@code Venom} class.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Venom();
	}
}
