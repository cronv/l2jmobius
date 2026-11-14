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

import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.Weapon;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayableExpChanged;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.ExNewSkillToLearnByLevelUp;

/**
 * Represents the statistics specific to a {@code Playable} entity.<br>
 * This class handles attributes that are unique to players and other playable characters in the game world.
 */
public class PlayableStat extends CreatureStat
{
	protected static final Logger LOGGER = Logger.getLogger(PlayableStat.class.getName());
	
	/**
	 * Creates a new {@code PlayableStat} instance for a specific player.<br>
	 * This constructor initializes the statistics based on the provided {@code Playable} object.
	 * @param player The {@code Playable} entity whose statistics are being initialized.
	 */
	public PlayableStat(Playable player)
	{
		super(player);
	}
	
	/**
	 * Adds a specific amount of experience to the character.<br>
	 * This method updates the level if the new experience threshold is met.<br>
	 * It also handles special logic for pets and sends packets to players when they level up.
	 * @param amount The amount of experience to add.
	 * @return {@code true} if the experience was added successfully, or {@code false} if the action was terminated by an event.
	 */
	public boolean addExp(long amount)
	{
		final Playable playable = getActiveChar();
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYABLE_EXP_CHANGED, playable))
		{
			final TerminateReturn term = EventDispatcher.getInstance().notifyEvent(new OnPlayableExpChanged(playable, getExp(), getExp() + amount), playable, TerminateReturn.class);
			if ((term != null) && term.terminate())
			{
				return false;
			}
		}
		
		if (((getExp() + amount) < 0) || ((amount > 0) && (getExp() == (getExpForLevel(getMaxLevel()) - 1))))
		{
			return true;
		}
		
		long value = amount;
		if ((getExp() + value) >= getExpForLevel(getMaxLevel()))
		{
			value = getExpForLevel(getMaxLevel()) - 1 - getExp();
		}
		
		final int oldLevel = getLevel();
		setExp(getExp() + value);
		int minimumLevel = 1;
		if (playable.isPet())
		{
			// Get minimum level from NpcTemplate.
			minimumLevel = PetData.getInstance().getPetMinLevel(playable.asPet().getTemplate().getId());
		}
		
		int level = minimumLevel; // minimum level
		for (int tmp = level; tmp <= getMaxLevel(); tmp++)
		{
			if (getExp() >= getExpForLevel(tmp))
			{
				continue;
			}
			
			level = --tmp;
			break;
		}
		
		if ((level != getLevel()) && (level >= minimumLevel))
		{
			addLevel(level - getLevel());
		}
		
		if ((getLevel() > oldLevel) && playable.isPlayer())
		{
			final Player player = playable.asPlayer();
			if (SkillTreeData.getInstance().hasAvailableSkills(player, player.getPlayerClass()))
			{
				player.sendPacket(ExNewSkillToLearnByLevelUp.STATIC_PACKET);
			}
			
			// Send quest list.
			if (!PlayerConfig.DISABLE_TUTORIAL)
			{
				player.sendQuestList();
			}
		}
		
		return true;
	}
	
	/**
	 * Removes a specific amount of experience from the actor.<br>
	 * This method ensures that the remaining experience does not drop below the required level threshold.<br>
	 * It also handles level reduction if the experience loss causes a level down.
	 * @param amount The amount of experience to remove.
	 * @return {@code true} if the operation was successful.
	 */
	public boolean removeExp(long amount)
	{
		long value = amount;
		if (((getExp() - value) < getExpForLevel(getLevel())) && (!PlayerConfig.PLAYER_DELEVEL || (PlayerConfig.PLAYER_DELEVEL && (getLevel() <= PlayerConfig.DELEVEL_MINIMUM))))
		{
			value = getExp() - getExpForLevel(getLevel());
		}
		
		if ((getExp() - value) < 0)
		{
			value = getExp() - 1;
		}
		
		setExp(getExp() - value);
		int minimumLevel = 1;
		final Playable playable = getActiveChar();
		if (playable.isPet())
		{
			// Get minimum level from NpcTemplate.
			minimumLevel = PetData.getInstance().getPetMinLevel(playable.asPet().getTemplate().getId());
		}
		
		int level = minimumLevel;
		for (int tmp = level; tmp <= getMaxLevel(); tmp++)
		{
			if (getExp() >= getExpForLevel(tmp))
			{
				continue;
			}
			
			level = --tmp;
			break;
		}
		
		if ((level != getLevel()) && (level >= minimumLevel))
		{
			addLevel(level - getLevel());
		}
		
		return true;
	}
	
	/**
	 * Removes a specified amount of experience and skill points.<br>
	 * This method checks if the provided values are greater than {@code 0}.<br>
	 * It attempts to subtract both values from the current totals.
	 * @param removeExp The amount of experience to remove.
	 * @param removeSp The amount of skill points to remove.
	 * @return {@code true} if either experience or skill points were successfully removed, otherwise {@code false}.
	 */
	public boolean removeExpAndSp(long removeExp, long removeSp)
	{
		boolean expRemoved = false;
		boolean spRemoved = false;
		if (removeExp > 0)
		{
			expRemoved = removeExp(removeExp);
		}
		
		if (removeSp > 0)
		{
			spRemoved = removeSp(removeSp);
		}
		
		return expRemoved || spRemoved;
	}
	
	/**
	 * Increases the level of the playable character by a specific amount.<br>
	 * This method ensures the new level does not exceed the maximum allowed limit.<br>
	 * It also updates the experience points and restores health and mana if the level increases.
	 * @param amount The number of levels to add.
	 * @return {@code true} if the level was successfully increased, otherwise {@code false}.
	 */
	public boolean addLevel(int amount)
	{
		int value = amount;
		if ((getLevel() + value) > (getMaxLevel() - 1))
		{
			if (getLevel() < (getMaxLevel() - 1))
			{
				value = getMaxLevel() - 1 - getLevel();
			}
			else
			{
				return false;
			}
		}
		
		final boolean levelIncreased = (getLevel() + value) > getLevel();
		value += getLevel();
		setLevel(value);
		
		// Sync up exp with current level.
		if ((getExp() >= getExpForLevel(getLevel() + 1)) || (getExpForLevel(getLevel()) > getExp()))
		{
			setExp(getExpForLevel(getLevel()));
		}
		
		final Playable playable = getActiveChar();
		if (!levelIncreased && playable.isPlayer() && !playable.isGM() && PlayerConfig.DECREASE_SKILL_LEVEL)
		{
			playable.asPlayer().checkPlayerSkills();
		}
		
		if (!levelIncreased)
		{
			return false;
		}
		
		playable.getStatus().setCurrentHp(playable.getStat().getMaxHp());
		playable.getStatus().setCurrentMp(playable.getStat().getMaxMp());
		
		return true;
	}
	
	/**
	 * Adds a specific amount of SP to the character.<br>
	 * This method ensures that the total SP does not exceed {@code PlayerConfig.MAX_SP}.<br>
	 * It returns {@code false} if the amount is negative or if the character is already at maximum SP.
	 * @param amount The amount of SP to add.
	 * @return {@code true} if the SP was successfully updated, and {@code false} otherwise.
	 */
	public boolean addSp(long amount)
	{
		if (amount < 0)
		{
			LOGGER.warning("wrong usage");
			return false;
		}
		
		final long currentSp = getSp();
		if (currentSp >= PlayerConfig.MAX_SP)
		{
			return false;
		}
		
		long value = amount;
		if (currentSp > (PlayerConfig.MAX_SP - value))
		{
			value = PlayerConfig.MAX_SP - currentSp;
		}
		
		setSp(currentSp + value);
		return true;
	}
	
	/**
	 * Reduces the current SP of the actor.<br>
	 * If the requested {@code amount} is greater than the current SP, it sets the SP to 0.
	 * @param amount The amount of SP to remove.
	 * @return Always returns {@code true}.
	 */
	public boolean removeSp(long amount)
	{
		final long currentSp = getSp();
		if (currentSp < amount)
		{
			setSp(getSp() - currentSp);
			return true;
		}
		
		setSp(getSp() - amount);
		return true;
	}
	
	/**
	 * Retrieves the total experience required to reach a specific level.<br>
	 * This value is fetched from the {@code ExperienceData} configuration.
	 * @param level The target level to check.
	 * @return The amount of experience needed for the given {@code level}.
	 */
	public long getExpForLevel(int level)
	{
		return ExperienceData.getInstance().getExpForLevel(level);
	}
	
	/**
	 * Retrieves the current active character.<br>
	 * This method returns the {@code Playable} entity associated with these stats.
	 * @return The active {@code Playable} object.
	 */
	@Override
	public Playable getActiveChar()
	{
		return super.getActiveChar().asPlayable();
	}
	
	/**
	 * Retrieves the maximum level allowed for a character.<br>
	 * This value is fetched from the {@code ExperienceData} configuration.
	 * @return The maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return ExperienceData.getInstance().getMaxLevel();
	}
	
	/**
	 * Returns the current physical attack radius.<br>
	 * It checks if the {@code Playable} actor has an active {@code Weapon}.<br>
	 * If a weapon exists, it returns the base radius from that weapon.<br>
	 * Otherwise, it returns the default value from the parent class.
	 * @return The physical attack radius as an {@code int}.
	 */
	@Override
	public int getPhysicalAttackRadius()
	{
		final Weapon weapon = getActiveChar().getActiveWeaponItem();
		return weapon != null ? weapon.getBaseAttackRadius() : super.getPhysicalAttackRadius();
	}
	
	/**
	 * Retrieves the physical attack angle for the character.<br>
	 * It checks if the character is holding a {@code Weapon}.<br>
	 * If a weapon exists, it returns the base attack angle from that weapon plus any bonuses.<br>
	 * Otherwise, it returns the default value from the parent class.
	 * @return The integer value of the physical attack angle.
	 */
	@Override
	public int getPhysicalAttackAngle()
	{
		final Playable playable = getActiveChar();
		final Weapon weapon = playable.getActiveWeaponItem();
		return (weapon != null ? weapon.getBaseAttackAngle() + (int) playable.getStat().getValue(Stat.WEAPON_ATTACK_ANGLE_BONUS, 0) : super.getPhysicalAttackAngle());
	}
}
