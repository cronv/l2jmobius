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
package org.l2jmobius.gameserver.entity.actor.stat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.FishingData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.holders.player.SubClassHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLevelChanged;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.PartySmallWindowUpdateType;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.AcquireSkillList;
import org.l2jmobius.gameserver.network.serverpackets.ExVitalityPointInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExVoteSystemInfo;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PartySmallWindowUpdate;
import org.l2jmobius.gameserver.network.serverpackets.PledgeShowMemberListUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.network.serverpackets.friend.FriendStatus;
import org.l2jmobius.gameserver.network.serverpackets.pledgeV2.ExPledgeMissionRewardCount;

/**
 * Represents the statistics and attributes associated with a {@code Player}.<br>
 * This class handles the calculation and management of various player stats.<br>
 * It serves as the primary data container for character-specific numerical values.
 */
public class PlayerStat extends PlayableStat
{
	public static final int MAX_VITALITY_POINTS = 140000;
	public static final int MIN_VITALITY_POINTS = 0;
	
	// Stats behind the limits ExStorageMaxCount carries.
	private static final Stat[] STORAGE_STATS =
	{
		Stat.INVENTORY_NORMAL,
		Stat.STORAGE_PRIVATE,
		Stat.TRADE_SELL,
		Stat.TRADE_BUY,
		Stat.RECIPE_DWARVEN,
		Stat.RECIPE_COMMON
	};
	
	private long _startingXp;
	private final AtomicInteger _talismanSlots = new AtomicInteger();
	private boolean _cloakSlot = false;
	private int _vitalityPoints = 0;
	private ScheduledFuture<?> _onRecalculateStatsTask;
	private final double[] _storageStatValues = new double[STORAGE_STATS.length];
	
	/**
	 * Creates a new {@code PlayerStat} instance for a specific player.<br>
	 * This constructor initializes the statistics using the provided {@code Player} object.
	 * @param player The {@code Player} associated with these statistics.
	 */
	public PlayerStat(Player player)
	{
		super(player);
	}
	
	/**
	 * Adds a specific amount of experience to the player.<br>
	 * This method checks if the player is allowed to gain experience first.<br>
	 * It also handles karma loss based on reputation and zone rules.
	 * @param value The amount of experience to add.
	 * @return {@code true} if the experience was successfully added, {@code false} otherwise.
	 */
	@Override
	public boolean addExp(long value)
	{
		final Player player = getActiveChar();
		
		// Allowed to gain exp?
		if (!player.getAccessLevel().canGainExp())
		{
			return false;
		}
		
		if (!super.addExp(value))
		{
			return false;
		}
		
		// Set new karma
		final int reputation = player.getReputation();
		if (!player.isCursedWeaponEquipped() && (reputation < 0) && (player.isGM() || !player.isInsideZone(ZoneId.PVP)))
		{
			final int karmaLost = Formulas.calculateKarmaLost(player, value);
			if (karmaLost > 0)
			{
				player.setReputation(Math.min((reputation + karmaLost), 0));
			}
		}
		
		// EXP status update currently not used in retail.
		player.updateUserInfo();
		return true;
	}
	
	/**
	 * Adds experience and skill points to the character.<br>
	 * This method calculates bonuses if {@code useBonuses} is {@code true}.<br>
	 * It also handles experience sharing with pets if they are nearby.
	 * @param addToExpValue The amount of experience to add.
	 * @param addToSpValue The amount of skill points to add.
	 * @param useBonuses Whether to apply experience and skill point multipliers.
	 */
	public void addExpAndSp(double addToExpValue, double addToSpValue, boolean useBonuses)
	{
		final Player player = getActiveChar();
		
		// Allowed to gain exp/sp?
		if (!player.getAccessLevel().canGainExp())
		{
			return;
		}
		
		double addToExp = addToExpValue;
		double addToSp = addToSpValue;
		
		final double baseExp = addToExp;
		final double baseSp = addToSp;
		double bonusExp = 1;
		double bonusSp = 1;
		if (useBonuses)
		{
			if (player.isFishing())
			{
				// rod fishing bonus
				final Item rod = player.getActiveWeaponInstance();
				if ((rod != null) && (rod.getItemType() == WeaponType.FISHINGROD))
				{
					final int templateId = rod.getTemplate().getId();
					bonusExp *= FishingData.getInstance().getRodData(templateId).getXpMultiplier();
					bonusSp *= FishingData.getInstance().getRodData(templateId).getSpMultiplier();
				}
			}
			else
			{
				bonusExp = getExpBonusMultiplier();
				bonusSp = getSpBonusMultiplier();
			}
		}
		
		addToExp *= bonusExp;
		addToSp *= bonusSp;
		double ratioTakenByPlayer = 0;
		
		// if this player has a pet and it is in his range he takes from the owner's Exp, give the pet Exp now
		final Summon sPet = player.getPet();
		if ((sPet != null) && (player.calculateDistance3D(sPet) < PlayerConfig.ALT_PARTY_RANGE))
		{
			final Pet pet = sPet.asPet();
			ratioTakenByPlayer = pet.getPetLevelData().getOwnerExpTaken() / 100f;
			
			// Only grant experience and skill points to the pet from the owner if the pet has a positive ratio, allowing for customizations where the pet earns more than 100% of the owner's total.
			if (ratioTakenByPlayer > 1)
			{
				ratioTakenByPlayer = 1;
			}
			
			if (!pet.isDead())
			{
				pet.addExpAndSp(addToExp * (1 - ratioTakenByPlayer), addToSp * (1 - ratioTakenByPlayer));
			}
			
			// now adjust the max ratio to avoid the owner earning negative exp/sp
			addToExp *= ratioTakenByPlayer;
			addToSp *= ratioTakenByPlayer;
		}
		
		final long finalExp = Math.round(addToExp);
		final long finalSp = Math.round(addToSp);
		final boolean expAdded = addExp(finalExp);
		final boolean spAdded = addSp(finalSp);
		if (!expAdded && spAdded)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_SP);
			sm.addLong(finalSp);
			player.sendPacket(sm);
		}
		else if (expAdded && !spAdded)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_XP);
			sm.addLong(finalExp);
			player.sendPacket(sm);
		}
		else if ((finalExp > 0) || (finalSp > 0))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_ACQUIRED_S1_XP_BONUS_S2_AND_S3_SP_BONUS_S4);
			sm.addLong(finalExp);
			sm.addLong(Math.round(addToExp - baseExp));
			sm.addLong(finalSp);
			sm.addLong(Math.round(addToSp - baseSp));
			player.sendPacket(sm);
		}
	}
	
	/**
	 * Removes a specific amount of experience and skill points from the actor.<br>
	 * This method automatically sends a message to the player after the operation.
	 * @param addToExp The amount of experience to remove.
	 * @param addToSp The amount of skill points to remove.
	 * @return {@code true} if the removal was successful, {@code false} otherwise.
	 */
	@Override
	public boolean removeExpAndSp(long addToExp, long addToSp)
	{
		return removeExpAndSp(addToExp, addToSp, true);
	}
	
	/**
	 * Removes a specific amount of experience and skill points from the actor.<br>
	 * This method also handles sending system messages to the player if requested.<br>
	 * It returns {@code false} if the removal operation fails.
	 * @param addToExp The amount of experience to remove.
	 * @param addToSp The amount of skill points to remove.
	 * @param sendMessage Whether to send a notification message to the player.
	 * @return {@code true} if the values were successfully removed, {@code false} otherwise.
	 */
	public boolean removeExpAndSp(long addToExp, long addToSp, boolean sendMessage)
	{
		final int level = getLevel();
		if (!super.removeExpAndSp(addToExp, addToSp))
		{
			return false;
		}
		
		if (sendMessage)
		{
			// Send a Server->Client System Message to the Player.
			SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_XP_HAS_DECREASED_BY_S1);
			sm.addLong(addToExp);
			final Player player = getActiveChar();
			player.sendPacket(sm);
			sm = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
			sm.addLong(addToSp);
			player.sendPacket(sm);
			if (getLevel() < level)
			{
				player.broadcastStatusUpdate();
			}
		}
		
		return true;
	}
	
	/**
	 * Increases the character level by a specified amount.<br>
	 * This method checks if the new level exceeds the maximum allowed limit.<br>
	 * It handles all side effects such as skill rewards, clan updates, and pet synchronization.
	 * @param value The amount to increase the level by.
	 * @return {@code true} if the level was successfully increased, {@code false} otherwise.
	 */
	@Override
	public boolean addLevel(int value)
	{
		final int currentLevel = getLevel();
		if ((currentLevel + value) > (ExperienceData.getInstance().getMaxLevel() - 1))
		{
			return false;
		}
		
		final Player player = getActiveChar();
		final boolean levelIncreased = super.addLevel(value);
		if (levelIncreased)
		{
			player.setCurrentCp(getMaxCp());
			player.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.LEVEL_UP));
			player.sendPacket(SystemMessageId.YOUR_LEVEL_HAS_INCREASED);
			player.getFriends().notifyStatus(FriendStatus.MODE_LEVEL);
		}
		
		// Notify to scripts
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_LEVEL_CHANGED, player))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerLevelChanged(player, currentLevel - value, currentLevel), player);
		}
		
		// Update daily mission count.
		player.sendPacket(new ExPledgeMissionRewardCount(player));
		
		// Give AutoGet skills and all normal skills if Auto-Learn is activated.
		player.rewardSkills();
		
		final Clan clan = player.getClan();
		if (clan != null)
		{
			clan.updateClanMember(player);
			clan.broadcastToOnlineMembers(new PledgeShowMemberListUpdate(player));
		}
		
		if (player.isInParty())
		{
			player.getParty().recalculatePartyLevel(); // Recalculate the party level.
		}
		
		// Maybe add some skills when player levels up in transformation.
		final Transform transform = player.getTransformation();
		if (transform != null)
		{
			transform.onLevelUp(player);
		}
		
		// Synchronize level with pet if possible.
		final Summon sPet = player.getPet();
		if (sPet != null)
		{
			final Pet pet = sPet.asPet();
			if (pet.getPetData().isSynchLevel() && (pet.getLevel() != currentLevel))
			{
				final int availableLevel = Math.min(pet.getPetData().getMaxLevel(), currentLevel);
				pet.getStat().setLevel(availableLevel);
				pet.getStat().getExpForLevel(availableLevel);
				pet.setCurrentHp(pet.getMaxHp());
				pet.setCurrentMp(pet.getMaxMp());
				pet.broadcastPacket(new SocialAction(player.getObjectId(), SocialAction.LEVEL_UP));
				pet.updateAndBroadcastStatus(1);
			}
		}
		
		player.broadcastStatusUpdate();
		
		// Update the overloaded status of the Player.
		player.refreshOverloaded(true);
		
		// Send a Server->Client packet UserInfo to the Player.
		player.updateUserInfo();
		
		// Send acquirable skill list.
		player.sendPacket(new AcquireSkillList(player));
		player.sendPacket(new ExVoteSystemInfo(player));
		
		// Removed used by new Clan system.
		// player.sendPacket(new ExOneDayReceiveRewardList(player, true));
		return levelIncreased;
	}
	
	/**
	 * Adds a specific amount of SP to the character.<br>
	 * This method updates the current SP and broadcasts the new information to other players.
	 * @param value The amount of SP to add.
	 * @return {@code true} if the SP was successfully added, {@code false} otherwise.
	 */
	@Override
	public boolean addSp(long value)
	{
		if (!super.addSp(value))
		{
			return false;
		}
		
		getActiveChar().broadcastUserInfo(UserInfoType.CURRENT_HPMPCP_EXP_SP);
		
		return true;
	}
	
	/**
	 * Retrieves the total experience required to reach a specific level.<br>
	 * This method fetches data from the {@code ExperienceData} instance.
	 * @param level The target level to check.
	 * @return The amount of experience needed for the specified level.
	 */
	@Override
	public long getExpForLevel(int level)
	{
		return ExperienceData.getInstance().getExpForLevel(level);
	}
	
	/**
	 * Retrieves the {@code Player} object associated with the active character.<br>
	 * This method calls the parent class to get the character and converts it to a {@code Player}.
	 * @return The current {@code Player} instance.
	 */
	@Override
	public Player getActiveChar()
	{
		return super.getActiveChar().asPlayer();
	}
	
	/**
	 * Retrieves the current experience points of the actor.<br>
	 * This method checks if a sub-class is active and returns its experience if applicable.<br>
	 * Otherwise, it returns the base experience value.
	 * @return The current experience points as a {@code long}.
	 */
	@Override
	public long getExp()
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			return player.getSubClasses().get(player.getClassIndex()).getExp();
		}
		
		return super.getExp();
	}
	
	/**
	 * Retrieves the base experience value.<br>
	 * This method returns the raw experience amount without any modifiers.
	 * @return The base experience as a {@code long}.
	 */
	public long getBaseExp()
	{
		return super.getExp();
	}
	
	/**
	 * Sets the experience points for the character.<br>
	 * This method updates the {@code exp} value of the active player or their active sub-class.
	 * @param value The new experience value to set.
	 */
	@Override
	public void setExp(long value)
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setExp(value);
		}
		else
		{
			super.setExp(value);
		}
	}
	
	/**
	 * Sets the starting experience value for the character.<br>
	 * This value is only updated if {@code GeneralConfig.BOTREPORT_ENABLE} is true.
	 * @param value The new starting experience amount to set.
	 */
	public void setStartingExp(long value)
	{
		if (GeneralConfig.BOTREPORT_ENABLE)
		{
			_startingXp = value;
		}
	}
	
	/**
	 * Retrieves the initial experience value for the character.<br>
	 * This value is typically set when a character is first created.
	 * @return The starting experience as a {@code long}.
	 */
	public long getStartingExp()
	{
		return _startingXp;
	}
	
	/**
	 * Retrieves the current number of talisman slots available.<br>
	 * If the character has not entered the world, it returns a default value of {@code 6}.<br>
	 * Otherwise, it returns the value stored in the {@code _talismanSlots} variable.
	 * @return The total count of talisman slots.
	 */
	public int getTalismanSlots()
	{
		if (!getActiveChar().hasEnteredWorld())
		{
			return 6;
		}
		
		return _talismanSlots.get();
	}
	
	/**
	 * Increases the number of available talisman slots.<br>
	 * This method updates the {@code _talismanSlots} value by the specified amount.
	 * @param count The number of slots to add.
	 */
	public void addTalismanSlots(int count)
	{
		_talismanSlots.addAndGet(count);
	}
	
	/**
	 * Checks if the player is allowed to equip a cloak.<br>
	 * This method returns {@code true} if the character has not entered the world yet.<br>
	 * Otherwise, it returns the current status of the cloak slot.
	 * @return {@code true} if the cloak can be equipped, {@code false} otherwise.
	 */
	public boolean canEquipCloak()
	{
		if (!getActiveChar().hasEnteredWorld())
		{
			return true;
		}
		
		return _cloakSlot;
	}
	
	/**
	 * Updates the status of the cloak slot.<br>
	 * This method sets whether a cloak can be equipped based on the provided value.
	 * @param cloakSlot The new status for the cloak slot. Use {@code true} to enable or {@code false} to disable.
	 */
	public void setCloakSlotStatus(boolean cloakSlot)
	{
		_cloakSlot = cloakSlot;
	}
	
	/**
	 * Retrieves the current level of the character.<br>
	 * This method checks for active dual classes or sub-classes first.<br>
	 * If neither are active, it returns the base level.
	 * @return The current level as an {@code int}.
	 */
	@Override
	public int getLevel()
	{
		final Player player = getActiveChar();
		if (player.isDualClassActive())
		{
			return player.getDualClass().getLevel();
		}
		
		if (player.isSubClassActive())
		{
			final SubClassHolder holder = player.getSubClasses().get(player.getClassIndex());
			if (holder != null)
			{
				return holder.getLevel();
			}
		}
		
		return super.getLevel();
	}
	
	/**
	 * Retrieves the base level of the actor.<br>
	 * This value is obtained from the parent class.
	 * @return The base level as an {@code int}.
	 */
	public int getBaseLevel()
	{
		return super.getLevel();
	}
	
	/**
	 * Sets the level of the character.<br>
	 * This method updates the level in the database and for the active sub-class if applicable.<br>
	 * The value is capped at the maximum level defined in {@code ExperienceData}.
	 * @param value The new level to set.
	 */
	@Override
	public void setLevel(int value)
	{
		int level = value;
		final int maxLevel = ExperienceData.getInstance().getMaxLevel();
		if (level > (maxLevel - 1))
		{
			level = maxLevel - 1;
		}
		
		final Player player = getActiveChar();
		PlayerInfoTable.getInstance().setLevel(player.getObjectId(), level);
		
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setLevel(level);
		}
		else
		{
			super.setLevel(level);
		}
		
		// Removed used by new Clan system.
		// if (!player.isDead())
		// {
		// player.sendPacket(new ExOneDayReceiveRewardList(player, false));
		// }
	}
	
	/**
	 * Retrieves the current SP value of the actor.<br>
	 * This method checks if a sub-class is active and returns its SP if applicable.<br>
	 * Otherwise, it returns the base SP from the parent class.
	 * @return The current SP as a {@code long}.
	 */
	@Override
	public long getSp()
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			return player.getSubClasses().get(player.getClassIndex()).getSp();
		}
		
		return super.getSp();
	}
	
	/**
	 * Retrieves the base SP value of the actor.<br>
	 * This method returns the original SP amount before any modifiers are applied.
	 * @return The base SP as a {@code long}.
	 */
	public long getBaseSp()
	{
		return super.getSp();
	}
	
	/**
	 * Sets the current SP value for the character.<br>
	 * This method updates the SP of the active subclass if one is active.<br>
	 * Otherwise, it updates the base SP of the player.
	 * @param value The new SP value to set.
	 */
	@Override
	public void setSp(long value)
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setSp(value);
		}
		else
		{
			super.setSp(value);
		}
	}
	
	/*
	 * Return current vitality points in integer format
	 */
	/**
	 * Retrieves the current vitality points for the character.<br>
	 * This method accounts for active sub-classes if they are enabled.<br>
	 * The result is clamped between {@code MIN_VITALITY_POINTS} and {@code MAX_VITALITY_POINTS}.
	 * @return the total number of vitality points as an {@code int}.
	 */
	public int getVitalityPoints()
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			final SubClassHolder subClassHolder = player.getSubClasses().get(player.getClassIndex());
			if (subClassHolder == null)
			{
				return 0;
			}
			
			return Math.min(MAX_VITALITY_POINTS, subClassHolder.getVitalityPoints());
		}
		
		return Math.clamp(_vitalityPoints, MIN_VITALITY_POINTS, MAX_VITALITY_POINTS);
	}
	
	/**
	 * Retrieves the base vitality points for the character.<br>
	 * This value is clamped between {@code MIN_VITALITY_POINTS} and {@code MAX_VITALITY_POINTS}.
	 * @return The current base vitality points as an {@code int}.
	 */
	public int getBaseVitalityPoints()
	{
		return Math.clamp(_vitalityPoints, MIN_VITALITY_POINTS, MAX_VITALITY_POINTS);
	}
	
	/**
	 * Calculates the experience bonus for vitality.<br>
	 * This value is only returned if the actor has more than {@code 0} vitality points.<br>
	 * It applies multipliers based on premium status and configuration settings.
	 * @return The calculated vitality experience bonus as a {@code double}.
	 */
	public double getVitalityExpBonus()
	{
		if (getVitalityPoints() > 0)
		{
			return getMul(Stat.VITALITY_EXP_RATE, 1) * (getActiveChar().hasPremiumStatus() ? RatesConfig.RATE_VITALITY_EXP_PREMIUM_MULTIPLIER : RatesConfig.RATE_VITALITY_EXP_MULTIPLIER);
		}
		
		return 0;
	}
	
	/**
	 * Sets the vitality points for the character.<br>
	 * If a subclass is active, it updates the value on the active subclass.<br>
	 * Otherwise, it clamps the provided {@code value} between the minimum and maximum limits.
	 * @param value The new vitality points to set.
	 */
	public void setVitalityPoints(int value)
	{
		final Player player = getActiveChar();
		if (player.isSubClassActive())
		{
			player.getSubClasses().get(player.getClassIndex()).setVitalityPoints(value);
			return;
		}
		
		_vitalityPoints = Math.clamp(value, MIN_VITALITY_POINTS, MAX_VITALITY_POINTS);
	}
	
	/*
	 * Set current vitality points to this value if quiet = true - does not send system messages
	 */
	/**
	 * Sets the current vitality points for the character.<br>
	 * The value is clamped between {@code MIN_VITALITY_POINTS} and {@code MAX_VITALITY_POINTS}.<br>
	 * If {@code quiet} is {@code false}, a system message is sent to notify the player of the change.<br>
	 * This method also updates the party window and broadcasts information to other players.
	 * @param value The new vitality points to set.
	 * @param quiet Whether to suppress the notification messages for the change.
	 */
	public void setVitalityPoints(int value, boolean quiet)
	{
		final int points = Math.clamp(value, MIN_VITALITY_POINTS, MAX_VITALITY_POINTS);
		final int currentVitalityPoints = getVitalityPoints();
		if (points == currentVitalityPoints)
		{
			return;
		}
		
		if (!quiet)
		{
			if (points < currentVitalityPoints)
			{
				getActiveChar().sendPacket(SystemMessageId.YOUR_VITALITY_HAS_DECREASED);
			}
			else
			{
				getActiveChar().sendPacket(SystemMessageId.YOUR_VITALITY_HAS_INCREASED);
			}
		}
		
		setVitalityPoints(points);
		
		if (points == 0)
		{
			getActiveChar().sendPacket(SystemMessageId.YOUR_VITALITY_IS_FULLY_EXHAUSTED);
		}
		else if (points == MAX_VITALITY_POINTS)
		{
			getActiveChar().sendPacket(SystemMessageId.YOUR_VITALITY_IS_AT_MAXIMUM);
		}
		
		final Player player = getActiveChar();
		player.sendPacket(new ExVitalityPointInfo(getVitalityPoints()));
		player.broadcastUserInfo(UserInfoType.VITA_FAME);
		final Party party = player.getParty();
		if (party != null)
		{
			final PartySmallWindowUpdate partyWindow = new PartySmallWindowUpdate(player, false);
			partyWindow.addComponentType(PartySmallWindowUpdateType.VITALITY_POINTS);
			party.broadcastToPartyMembers(player, partyWindow);
		}
		
		// Send item list to update vitality items with red icons in inventory.
		final List<Item> items = new ArrayList<>();
		ITEMS: for (Item item : player.getInventory().getItems())
		{
			final ItemTemplate template = item.getTemplate();
			if (template.hasSkills())
			{
				for (ItemSkillHolder holder : template.getAllSkills())
				{
					if (holder.getSkill().hasEffectType(EffectType.VITALITY_POINT_UP))
					{
						items.add(item);
						continue ITEMS;
					}
				}
			}
		}
		
		if (!items.isEmpty())
		{
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addItems(items);
			player.sendInventoryUpdate(iu);
		}
	}
	
	/**
	 * Updates the current vitality points for the character.<br>
	 * This method checks if vitality is enabled before applying changes.<br>
	 * It handles point gains and losses while respecting minimum and maximum limits.
	 * @param value The amount of vitality points to add or subtract.
	 * @param useRates Whether to apply global configuration rates to the change.
	 * @param quiet Whether to suppress notifications during the update.
	 */
	public synchronized void updateVitalityPoints(int value, boolean useRates, boolean quiet)
	{
		if ((value == 0) || !PlayerConfig.ENABLE_VITALITY)
		{
			return;
		}
		
		int points = value;
		if (useRates)
		{
			if (getActiveChar().isLucky())
			{
				return;
			}
			
			if (points < 0) // vitality consumed
			{
				double consumeRate = getMul(Stat.VITALITY_CONSUME_RATE, 1);
				if (consumeRate <= 0)
				{
					return;
				}
				
				points *= consumeRate;
			}
			
			if (points > 0)
			{
				// vitality increased
				points *= RatesConfig.RATE_VITALITY_GAIN;
			}
			else
			{
				// vitality decreased
				points *= RatesConfig.RATE_VITALITY_LOST;
			}
		}
		
		if (points > 0)
		{
			points = Math.min(getVitalityPoints() + points, MAX_VITALITY_POINTS);
		}
		else
		{
			points = Math.max(getVitalityPoints() + points, MIN_VITALITY_POINTS);
		}
		
		if (Math.abs(points - getVitalityPoints()) <= 1e-6)
		{
			return;
		}
		
		setVitalityPoints(points);
	}
	
	/**
	 * Calculates the total experience bonus multiplier for the player.<br>
	 * This includes bonuses from the Vitality system and skills.<br>
	 * The result is capped by {@code PlayerConfig.MAX_BONUS_EXP}.
	 * @return the final calculated experience bonus as a {@code double}.
	 */
	public double getExpBonusMultiplier()
	{
		double bonus = 1.0;
		double vitality = 1.0;
		double bonusExp = 1.0;
		
		// Bonus from Vitality System.
		vitality = getVitalityExpBonus();
		
		// Bonus exp from skills.
		bonusExp = 1 + (getValue(Stat.BONUS_EXP, 0) / 100);
		if (vitality > 1.0)
		{
			bonus += (vitality - 1);
		}
		
		if (bonusExp > 1)
		{
			bonus += (bonusExp - 1);
		}
		
		// Check for abnormal bonuses.
		bonus = Math.max(bonus, 1);
		if (PlayerConfig.MAX_BONUS_EXP > 0)
		{
			bonus = Math.min(bonus, PlayerConfig.MAX_BONUS_EXP);
		}
		
		return bonus;
	}
	
	/**
	 * Calculates the total multiplier for Skill Points (SP).<br>
	 * This value combines bonuses from the Vitality system and skill stats.<br>
	 * The result is clamped between 1.0 and the maximum allowed in {@code PlayerConfig}.
	 * @return The calculated SP bonus multiplier as a {@code double}.
	 */
	public double getSpBonusMultiplier()
	{
		double bonus = 1.0;
		double vitality = 1.0;
		double bonusSp = 1.0;
		
		// Bonus from Vitality System.
		vitality = getVitalityExpBonus();
		
		// Bonus sp from skills.
		bonusSp = 1 + (getValue(Stat.BONUS_SP, 0) / 100);
		if (vitality > 1.0)
		{
			bonus += (vitality - 1);
		}
		
		if (bonusSp > 1)
		{
			bonus += (bonusSp - 1);
		}
		
		// Check for abnormal bonuses.
		bonus = Math.max(bonus, 1);
		if (PlayerConfig.MAX_BONUS_SP > 0)
		{
			bonus = Math.min(bonus, PlayerConfig.MAX_BONUS_SP);
		}
		
		return bonus;
	}
	
	/**
	 * Retrieves the number of jewel slots available for a brooch.<br>
	 * Returns a default value of {@code 6} if the character has not entered the world.<br>
	 * Otherwise, it returns the specific value from the {@code Stat.BROOCH_JEWELS} stat.
	 * @return The number of jewel slots as an {@code int}.
	 */
	public int getBroochJewelSlots()
	{
		if (!getActiveChar().hasEnteredWorld())
		{
			return 6;
		}
		
		return (int) getValue(Stat.BROOCH_JEWELS, 0);
	}
	
	/**
	 * Retrieves the current number of Agathion slots available.<br>
	 * Returns a default value of {@code 5} if the character has not entered the world.<br>
	 * Otherwise, it returns the value stored in the {@code Stat.AGATHION_SLOTS} stat.
	 * @return The total number of Agathion slots.
	 */
	public int getAgathionSlots()
	{
		if (!getActiveChar().hasEnteredWorld())
		{
			return 5;
		}
		
		return (int) getValue(Stat.AGATHION_SLOTS, 0);
	}
	
	/**
	 * Retrieves the current number of artifact slots available.<br>
	 * Returns a default value of {@code 21} if the character has not entered the world.<br>
	 * Otherwise, it returns the value stored in the {@code Stat.ARTIFACT_SLOTS} stat.
	 * @return The total number of artifact slots.
	 */
	public int getArtifactSlots()
	{
		if (!getActiveChar().hasEnteredWorld())
		{
			return 21;
		}
		
		return (int) getValue(Stat.ARTIFACT_SLOTS, 0);
	}
	
	/**
	 * Updates the character statistics based on current attributes.<br>
	 * This method skips calculation if the character is currently changing classes.
	 * @param broadcast Determines whether to send a network packet to notify clients of the change.
	 */
	@Override
	public void recalculateStats(boolean broadcast)
	{
		if (!getActiveChar().isChangingClass())
		{
			super.recalculateStats(broadcast);
		}
	}
	
	/**
	 * Recalculates the statistics for the actor and its associated entities.<br>
	 * This method schedules a task to update stats after a short delay.<br>
	 * It also updates servitor stats if the player has specific ability changes.
	 * @param broadcast Determines whether to send a network packet to notify clients of the change.
	 */
	@Override
	protected void onRecalculateStats(boolean broadcast)
	{
		if (_onRecalculateStatsTask == null)
		{
			_onRecalculateStatsTask = ThreadPool.schedule(() ->
			{
				super.onRecalculateStats(broadcast);
				
				_onRecalculateStatsTask = null;
			}, 50);
		}
		
		final Player player = getActiveChar();
		
		// Storage limits reach the client only through ExStorageMaxCount, so send it whenever a slot bonus was gained or lost.
		boolean storageChanged = false;
		for (int i = 0; i < STORAGE_STATS.length; i++)
		{
			final double value = getValue(STORAGE_STATS[i], 0);
			if (value != _storageStatValues[i])
			{
				_storageStatValues[i] = value;
				storageChanged = true;
			}
		}
		
		if (storageChanged)
		{
			player.sendStorageMaxCount();
		}
		
		if (player.hasAbnormalType(AbnormalType.ABILITY_CHANGE) && player.hasServitors())
		{
			player.getServitors().values().forEach(servitor -> servitor.getStat().recalculateStats(broadcast));
		}
	}
}
