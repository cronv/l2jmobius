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
package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.groups.PartyMessageType;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMode;

/**
 * Provides a base framework for managing olympiad game logic.<br>
 * This {@code AbstractOlympiadGame} class handles core mechanics shared by different types of olympiads.
 * @author godson, GodKratos, Pere, DS, Mobius
 */
public abstract class AbstractOlympiadGame
{
	protected static final Logger LOGGER = Logger.getLogger(AbstractOlympiadGame.class.getName());
	protected static final Logger LOGGER_OLYMPIAD = Logger.getLogger("olympiad");
	
	protected static final String POINTS = "olympiad_points";
	protected static final String COMP_DONE = "competitions_done";
	protected static final String COMP_WON = "competitions_won";
	protected static final String COMP_LOST = "competitions_lost";
	protected static final String COMP_DRAWN = "competitions_drawn";
	protected static final String COMP_DONE_WEEK = "competitions_done_week";
	protected static final String COMP_DONE_WEEK_CLASSED = "competitions_done_week_classed";
	protected static final String COMP_DONE_WEEK_NON_CLASSED = "competitions_done_week_non_classed";
	protected static final String COMP_DONE_WEEK_TEAM = "competitions_done_week_team";
	
	// Skill whose reuse is not reset on entering the arena (retail olympiad_reset_skill_excluded).
	private static final int GAIN_LURE = 13849;
	
	protected long _startTime = 0;
	protected boolean _aborted = false;
	protected final int _stadiumId;
	
	/**
	 * Initializes a new instance of an olympiad game.<br>
	 * Sets the unique stadium identifier for this game.
	 * @param id The unique identification number for the stadium.
	 */
	protected AbstractOlympiadGame(int id)
	{
		_stadiumId = id;
	}
	
	/**
	 * Checks if the olympiad game has been cancelled.
	 * @return {@code true} if the game was aborted, {@code false} otherwise.
	 */
	public boolean isAborted()
	{
		return _aborted;
	}
	
	/**
	 * Retrieves the unique identifier for the stadium.<br>
	 * This ID identifies which specific arena is being used.
	 * @return The {@code int} value of the stadium ID.
	 */
	public int getStadiumId()
	{
		return _stadiumId;
	}
	
	/**
	 * Initializes the start time of the competition.<br>
	 * Checks if the competition has been aborted.
	 * @return {@code true} if the competition is not aborted, {@code false} otherwise.
	 */
	protected boolean makeCompetitionStart()
	{
		_startTime = System.currentTimeMillis();
		return !_aborted;
	}
	
	/**
	 * Adds a specific amount of points to a {@code Participant}.<br>
	 * This method updates the participant's statistics.<br>
	 * It also broadcasts a system message to all players.
	 * @param par The {@code Participant} receiving the points.
	 * @param points The number of points to add.
	 */
	protected void addPointsToParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, points);
		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EARNED_OLYMPIAD_POINTS_X_S2);
		sm.addString(par.getName());
		sm.addInt(points);
		broadcastPacket(sm);
	}
	
	/**
	 * Subtracts a specific amount of points from a participant.<br>
	 * This method updates the {@code Participant} statistics and broadcasts a system message to all players.
	 * @param par The {@code Participant} whose points will be reduced.
	 * @param points The number of points to remove from the participant.
	 */
	protected void removePointsFromParticipant(Participant par, int points)
	{
		par.updateStat(POINTS, -points);
		final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_LOST_OLYMPIAD_POINTS_X_S2);
		sm.addString(par.getName());
		sm.addInt(points);
		broadcastPacket(sm);
	}
	
	/**
	 * Checks if a {@code Player} meets the requirements to participate in an olympiad.<br>
	 * It validates the player status, class type, equipment, and inventory weight.<br>
	 * If any requirement is failed, it sends a notification to the player and returns a cancellation message.
	 * @param player The {@code Player} to check.
	 * @return A {@code SystemMessage} if requirements are not met, or {@code null} if they are satisfied.
	 */
	protected static SystemMessage checkDefaulted(Player player)
	{
		if ((player == null) || !player.isOnline())
		{
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		if ((player.getClient() == null) || player.getClient().isDetached())
		{
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_MADE_HASTE_WITH_THEIR_TAIL_BETWEEN_THEIR_LEGS_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		// safety precautions
		if (player.getObserverMode().isActive())
		{
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		SystemMessage sm;
		if (player.isDead())
		{
			sm = new SystemMessage(SystemMessageId.C1_IS_DEAD_AND_CANNOT_TAKE_PART_IN_THE_OLYMPIAD_IF_YOU_DIE_AFTER_REGISTERING_FOR_THE_OLYMPIAD_OR_WHILE_TELEPORTING_TO_THE_ARENA_YOU_LOSE_OLYMPIAD_POINTS_AND_YOUR_MATCH_IS_CANCELLED);
			sm.addPcName(player);
			player.sendPacket(sm);
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		if (player.isSubClassActive())
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_BECAUSE_YOU_HAVE_CHANGED_YOUR_CLASS_TO_SUBCLASS);
			sm.addPcName(player);
			player.sendPacket(sm);
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		if (player.isCursedWeaponEquipped())
		{
			sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_THE_OWNER_OF_S2_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addPcName(player);
			sm.addItemName(player.getCursedWeaponEquippedId());
			player.sendPacket(sm);
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		if (!player.isInventoryUnder90(true) || (player.getWeightPenalty() >= 3))
		{
			sm = new SystemMessage(SystemMessageId.C1_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_AS_THEIR_INVENTORY_IS_FILLED_FOR_MORE_THAN_80);
			sm.addPcName(player);
			player.sendPacket(sm);
			return new SystemMessage(SystemMessageId.YOUR_OPPONENT_DOES_NOT_MEET_THE_REQUIREMENTS_TO_DO_BATTLE_THE_MATCH_HAS_BEEN_CANCELLED);
		}
		
		return null;
	}
	
	/**
	 * Teleports a participant to a specific arena location.<br>
	 * This method updates the player's status and sends the required olympiad packet.
	 * @param par The {@code Participant} being moved.
	 * @param loc The target {@code Location} for teleportation.
	 * @param id The unique identifier for the olympiad game.
	 * @param instance The {@code Instance} where the player will be placed.
	 * @param mode The {@code OlympiadMode} to apply to the player.
	 * @return {@code true} if the teleportation was successful, {@code false} otherwise.
	 */
	protected static boolean portPlayerToArena(Participant par, Location loc, int id, Instance instance, OlympiadMode mode)
	{
		final Player player = par.getPlayer();
		if ((player == null) || !player.isOnline())
		{
			return false;
		}
		
		try
		{
			player.setPvpFlag(0);
			player.setLastLocation();
			if (player.isSitting())
			{
				player.standUp();
			}
			
			player.setTarget(null);
			
			player.setOlympiadGameId(id);
			player.setInOlympiadMode(true);
			player.setOlympiadStart(false);
			player.setOlympiadSide(par.getSide());
			player.teleToLocation(loc, instance);
			player.sendPacket(new ExOlympiadMode(mode));
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Teleports a participant to a specific location for an olympiad spot.<br>
	 * This method ensures the player is standing and clears their current target.
	 * @param par The {@code Participant} to be moved.
	 * @param loc The destination {@code Location}.
	 * @param id The unique identifier for the spot.
	 * @return {@code true} if the teleport was successful, {@code false} otherwise.
	 */
	protected static boolean portPlayerToSpot(Participant par, Location loc, int id)
	{
		final Player player = par.getPlayer();
		if ((player == null) || !player.isOnline())
		{
			return false;
		}
		
		try
		{
			if (player.isSitting())
			{
				player.standUp();
			}
			
			player.setTarget(null);
			player.teleToLocation(loc);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Cleans up the player's status and effects after an olympiad.<br>
	 * This method removes buffs, clan skills, and active casts.<br>
	 * It also restores the player's health and mana resources.
	 * @param player The {@code Player} to be cleaned.
	 * @param removeParty Whether to remove the player from their current {@code Party}.
	 */
	protected void removals(Player player, boolean removeParty)
	{
		try
		{
			if (player == null)
			{
				return;
			}
			
			// Remove Buffs
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			player.getEffectList().stopEffects(info -> info.getSkill().isBlockedInOlympiad(), true, true);
			
			// Remove Clan Skills
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.removeSkillEffects(player);
				if (clan.getCastleId() > 0)
				{
					final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
					if (castle != null)
					{
						castle.removeResidentialSkills(player);
					}
				}
				
				if (clan.getFortId() > 0)
				{
					final Fort fort = FortManager.getInstance().getFortByOwner(clan);
					if (fort != null)
					{
						fort.removeResidentialSkills(player);
					}
				}
			}
			
			// Abort casting if player casting.
			player.abortAttack();
			player.abortCast();
			
			// Force the character to be visible.
			player.setInvisible(false);
			
			// Heal Player fully
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			
			// Remove Summon's Buffs
			if (player.hasSummon())
			{
				final Summon pet = player.getPet();
				if (pet != null)
				{
					pet.unSummon(player);
				}
				
				player.getServitors().values().forEach(s ->
				{
					s.stopAllEffectsExceptThoseThatLastThroughDeath();
					s.getEffectList().stopEffects(info -> info.getSkill().isBlockedInOlympiad(), true, true);
					s.abortAttack();
					s.abortCast();
				});
			}
			
			// stop any cubic that has been given by other player.
			player.getCubicList().stopByOthers();
			
			// Remove player from his party.
			if (removeParty)
			{
				final Party party = player.getParty();
				if (party != null)
				{
					party.removePartyMember(player, PartyMessageType.EXPELLED);
				}
			}
			
			// Remove Agathion
			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
				player.broadcastUserInfo();
			}
			
			player.checkItemRestriction();
			
			// enable skills with cool time <= 15 minutes
			for (Skill skill : player.getAllSkills())
			{
				if ((skill.getReuseDelay() <= 900000) && (skill.getId() != GAIN_LURE))
				{
					player.enableSkill(skill);
					player.removeTimeStamp(skill);
				}
			}
			
			player.sendSkillList();
			player.sendPacket(new SkillCoolTime(player));
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Cleans up the player state for the second round.<br>
	 * This method removes debuffs and cancels active actions.<br>
	 * It also restores the player's health, mana, and skill status.
	 * @param player The {@code Player} object to be cleaned.
	 */
	protected void roundTwoClean(Player player)
	{
		try
		{
			if (player == null)
			{
				return;
			}
			
			// Remove Debuffs
			player.getEffectList().stopEffects(info -> info.getSkill().isDebuff(), true, true);
			
			// Abort casting if player casting.
			player.abortAttack();
			player.abortCast();
			
			// Force the character to be visible.
			player.setInvisible(false);
			
			// Heal Player fully
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			
			// enable skills with cool time <= 15 minutes
			for (Skill skill : player.getAllSkills())
			{
				if ((skill.getReuseDelay() <= 900000) && (skill.getId() != GAIN_LURE))
				{
					player.enableSkill(skill);
					player.removeTimeStamp(skill);
				}
			}
			
			player.sendSkillList();
			player.sendPacket(new SkillCoolTime(player));
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Resets the status and effects of a {@code Player}.<br>
	 * This method stops active attacks, casts, and special effects.<br>
	 * It also restores health, mana, and CP values for the player and their summons.
	 * @param player The {@code Player} instance to be cleaned.
	 */
	protected void cleanEffects(Player player)
	{
		try
		{
			// prevent players kill each other
			player.setOlympiadStart(false);
			player.setTarget(null);
			player.abortAttack();
			player.abortCast();
			player.getAI().setIntentionIdle();
			
			if (player.isDead())
			{
				player.setDead(false);
			}
			
			player.stopAllEffectsExceptThoseThatLastThroughDeath();
			player.getEffectList().stopEffects(info -> info.getSkill().isBlockedInOlympiad(), true, true);
			player.clearSouls();
			player.clearCharges();
			if (player.getAgathionId() > 0)
			{
				player.setAgathionId(0);
			}
			
			final Summon pet = player.getPet();
			if ((pet != null) && !pet.isDead())
			{
				pet.setTarget(null);
				pet.abortAttack();
				pet.abortCast();
				pet.getAI().setIntentionIdle();
				pet.stopAllEffectsExceptThoseThatLastThroughDeath();
				pet.getEffectList().stopEffects(info -> info.getSkill().isBlockedInOlympiad(), true, true);
			}
			
			for (Summon s : player.getServitors().values())
			{
				if (!s.isDead())
				{
					s.setTarget(null);
					s.abortAttack();
					s.abortCast();
					s.getAI().setIntentionIdle();
					s.stopAllEffectsExceptThoseThatLastThroughDeath();
					s.getEffectList().stopEffects(info -> info.getSkill().isBlockedInOlympiad(), true, true);
				}
			}
			
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.getStatus().startHpMpRegeneration();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	/**
	 * Resets the status of a {@code Player} after an olympiad event.<br>
	 * This method removes olympiad modes and restores clan skills.<br>
	 * It also heals the player to their maximum values.
	 * @param player The {@code Player} whose status needs to be reset.
	 */
	protected void playerStatusBack(Player player)
	{
		try
		{
			if (player.isTransformed())
			{
				player.untransform();
			}
			
			player.setInOlympiadMode(false);
			player.setOlympiadStart(false);
			player.setOlympiadSide(-1);
			player.setOlympiadGameId(-1);
			player.sendPacket(new ExOlympiadMode(OlympiadMode.SPECTATOR));
			player.sendPacket(new ExOlympiadMode(OlympiadMode.NONE));
			
			// Add Clan Skills
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.addSkillEffects(player);
				if (clan.getCastleId() > 0)
				{
					final Castle castle = CastleManager.getInstance().getCastleByOwner(clan);
					if (castle != null)
					{
						castle.giveResidentialSkills(player);
					}
				}
				
				if (clan.getFortId() > 0)
				{
					final Fort fort = FortManager.getInstance().getFortByOwner(clan);
					if (fort != null)
					{
						fort.giveResidentialSkills(player);
					}
				}
				
				player.sendSkillList();
			}
			
			// heal again after adding clan skills
			player.setCurrentCp(player.getMaxCp());
			player.setCurrentHp(player.getMaxHp());
			player.setCurrentMp(player.getMaxMp());
			player.getStatus().startHpMpRegeneration();
			
			if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, player);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "playerStatusBack()", e);
		}
	}
	
	/**
	 * Teleports a player back to their last known location.<br>
	 * This method checks if the {@code Player} is not {@code null}.<br>
	 * It also verifies that the {@code Location} data exists before moving them.
	 * @param player The {@code Player} object to teleport back.
	 */
	protected void portPlayerBack(Player player)
	{
		if (player == null)
		{
			return;
		}
		
		final Location loc = player.getLastLocation();
		if (loc != null)
		{
			player.setIsPendingRevive(false);
			player.teleToLocation(loc, null);
			player.unsetLastLocation();
		}
	}
	
	/**
	 * Gives a list of items to a specific player as a reward.<br>
	 * This method updates the player inventory and sends a notification message.<br>
	 * It handles errors internally by logging warnings.
	 * @param player The {@code Player} who will receive the rewards.
	 * @param list The {@code List} of {@code ItemHolder} objects to be given.
	 */
	public static void rewardParticipant(Player player, List<ItemHolder> list)
	{
		if ((player == null) || !player.isOnline() || list.isEmpty())
		{
			return;
		}
		
		try
		{
			final InventoryUpdate iu = new InventoryUpdate();
			list.forEach(holder ->
			{
				final Item item = player.getInventory().addItem(ItemProcessType.REWARD, holder.getId(), holder.getCount(), player, null);
				if (item == null)
				{
					return;
				}
				
				iu.addModifiedItem(item);
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
				sm.addItemName(item);
				sm.addLong(holder.getCount());
				player.sendPacket(sm);
			});
			player.sendInventoryUpdate(iu);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	public abstract CompetitionType getType();
	
	public abstract String[] getPlayerNames();
	
	public abstract boolean containsParticipant(int playerId);
	
	public abstract void sendOlympiadInfo(Creature creature);
	
	public abstract void broadcastOlympiadInfo(OlympiadStadium stadium);
	
	protected abstract void broadcastPacket(ServerPacket packet);
	
	protected abstract boolean needBuffers();
	
	protected abstract boolean checkDefaulted();
	
	protected abstract void removals();
	
	protected abstract boolean portPlayersToArena(List<Location> spawns, Instance instance);
	
	protected abstract boolean portPlayersToSpots(List<Location> spawns, Instance instance);
	
	protected abstract void cleanEffects();
	
	protected abstract void portPlayersBack();
	
	protected abstract void playersStatusBack();
	
	protected abstract void clearPlayers();
	
	protected abstract void matchEnd(boolean value);
	
	protected abstract boolean isMatchEnd();
	
	protected abstract void handleDisconnect(Player player);
	
	protected abstract void resetDamage();
	
	protected abstract void resetDamageFinal();
	
	protected abstract void addDamage(Player player, int damage);
	
	protected abstract boolean checkBattleStatus();
	
	protected abstract boolean haveWinner();
	
	protected abstract boolean roundWinner();
	
	protected abstract void validateWinner(OlympiadStadium stadium);
	
	protected abstract void validateRound1Winner(OlympiadStadium stadium);
	
	protected abstract void validateRound2Winner(OlympiadStadium stadium);
	
	protected abstract void validateRound3Winner(OlympiadStadium stadium);
	
	protected abstract int getDivider();
	
	protected abstract void healPlayers();
	
	protected abstract void buffPlayers();
	
	protected abstract void roundTwoCleanUp();
	
	protected abstract void untransformPlayers();
	
	protected abstract void makePlayersInvul();
	
	protected abstract void removePlayersInvul();
}
