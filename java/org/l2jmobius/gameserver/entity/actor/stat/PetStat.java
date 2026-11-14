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

import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SocialAction;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents the statistics and attributes specific to a {@code Pet}.<br>
 * This class handles how pet-related data is processed within the game world.
 */
public class PetStat extends SummonStat
{
	/**
	 * Creates a new {@code PetStat} instance for a specific pet.<br>
	 * This constructor initializes the statistics using the provided {@code Pet} object.
	 * @param activeChar The {@code Pet} character to associate with these statistics.
	 */
	public PetStat(Pet activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Adds a specific amount of experience to the pet.<br>
	 * This method checks if the pet is controllable before updating.<br>
	 * It broadcasts the new status to all players if successful.
	 * @param value The amount of experience to add.
	 * @return {@code true} if the experience was added successfully, {@code false} otherwise.
	 */
	public boolean addExp(int value)
	{
		final Pet pet = getActiveChar();
		if (pet.isUncontrollable() || !super.addExp(value))
		{
			return false;
		}
		
		pet.updateAndBroadcastStatus(1);
		return true;
	}
	
	/**
	 * Adds experience and skill points to the pet.<br>
	 * This method rounds the input value to a {@code long}.<br>
	 * It checks if the pet is controllable before updating.<br>
	 * If successful, it sends a system message to the player.
	 * @param addToExp The amount of experience to add.
	 * @return {@code true} if the experience was added successfully, {@code false} otherwise.
	 */
	public boolean addExpAndSp(double addToExp)
	{
		final long finalExp = Math.round(addToExp);
		final Pet pet = getActiveChar();
		if (pet.isUncontrollable() || !addExp(finalExp))
		{
			return false;
		}
		
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_HAS_ACQUIRED_S1_XP);
		sm.addLong(finalExp);
		pet.updateAndBroadcastStatus(1);
		pet.sendPacket(sm);
		return true;
	}
	
	/**
	 * Increases the current level of the pet by a specific amount.<br>
	 * This method checks if the new level exceeds the maximum allowed limit.<br>
	 * It also broadcasts status updates to other players if the level increases.
	 * @param value The amount to add to the current level.
	 * @return {@code true} if the level was successfully increased, {@code false} otherwise.
	 */
	@Override
	public boolean addLevel(int value)
	{
		final int currentLevel = getLevel();
		if ((currentLevel + value) > (getMaxLevel() - 1))
		{
			return false;
		}
		
		final boolean levelIncreased = super.addLevel(value);
		final Pet pet = getActiveChar();
		pet.broadcastStatusUpdate();
		if (levelIncreased)
		{
			pet.broadcastPacket(new SocialAction(pet.getObjectId(), SocialAction.LEVEL_UP));
		}
		
		// Send a Server->Client packet PetInfo to the Player.
		pet.updateAndBroadcastStatus(1);
		
		if (pet.getControlItem() != null)
		{
			pet.getControlItem().setEnchantLevel(currentLevel);
		}
		
		return levelIncreased;
	}
	
	/**
	 * Retrieves the required experience for a specific level.<br>
	 * This method looks up the maximum experience value in the pet data.
	 * @param level The target level to check.
	 * @return The amount of experience needed for the specified {@code level}.
	 */
	@Override
	public long getExpForLevel(int level)
	{
		final Pet pet = getActiveChar();
		try
		{
			return PetData.getInstance().getPetLevelData(pet.getId(), level).getPetMaxExp();
		}
		catch (NullPointerException e)
		{
			if (pet != null)
			{
				LOGGER.warning("Pet objectId:" + pet.getObjectId() + ", NpcId:" + pet.getId() + ", level:" + level + " is missing data from pets_stats table!");
			}
			
			throw e;
		}
	}
	
	/**
	 * Retrieves the {@code Pet} currently associated with these statistics.<br>
	 * This method calls the parent class to get the active character and converts it.
	 * @return the current {@code Pet} object.
	 */
	@Override
	public Pet getActiveChar()
	{
		return super.getActiveChar().asPet();
	}
	
	/**
	 * Retrieves the battle feed value for the pet.<br>
	 * This value is obtained from the active character's pet level data.
	 * @return the current {@code int} value of the pet's battle feed.
	 */
	public int getFeedBattle()
	{
		return getActiveChar().getPetLevelData().getPetFeedBattle();
	}
	
	/**
	 * Retrieves the normal feed amount for the pet.<br>
	 * This value is fetched from the active character's pet data.
	 * @return The normal feed amount as an {@code int}.
	 */
	public int getFeedNormal()
	{
		return getActiveChar().getPetLevelData().getPetFeedNormal();
	}
	
	/**
	 * Updates the level of the pet.<br>
	 * This method synchronizes the new level with the {@code PetData}.<br>
	 * It also updates any associated control item enchant levels.
	 * @param value The new level to set for the pet.
	 */
	@Override
	public void setLevel(int value)
	{
		final Pet pet = getActiveChar();
		final int templateId = pet.getTemplate().getId();
		pet.setPetData(PetData.getInstance().getPetLevelData(templateId, value));
		if (pet.getPetLevelData() == null)
		{
			throw new IllegalArgumentException("No pet data for npc: " + templateId + " level: " + value);
		}
		
		pet.stopFeed();
		super.setLevel(value);
		
		pet.startFeed();
		
		if (pet.getControlItem() != null)
		{
			pet.getControlItem().setEnchantLevel(getLevel());
		}
	}
	
	/**
	 * Retrieves the maximum feed amount for the pet.<br>
	 * This value is fetched from the {@code Pet} data.
	 * @return The maximum feed capacity as an {@code int}.
	 */
	public int getMaxFeed()
	{
		return getActiveChar().getPetLevelData().getPetMaxFeed();
	}
	
	/**
	 * Retrieves the physical attack speed of the pet.<br>
	 * This value is halved if the active character is hungry.
	 * @return The current physical attack speed as an {@code int}.
	 */
	@Override
	public int getPAtkSpd()
	{
		int val = super.getPAtkSpd();
		if (getActiveChar().isHungry())
		{
			val /= 2;
		}
		
		return val;
	}
	
	/**
	 * Retrieves the magic attack speed of the pet.<br>
	 * This value is halved if the active character is hungry.
	 * @return The current magic attack speed as an {@code int}.
	 */
	@Override
	public int getMAtkSpd()
	{
		int val = super.getMAtkSpd();
		if (getActiveChar().isHungry())
		{
			val /= 2;
		}
		
		return val;
	}
	
	/**
	 * Retrieves the maximum level allowed for a pet.<br>
	 * This value is fetched from {@code ExperienceData}.
	 * @return the maximum pet level as an {@code int}.
	 */
	@Override
	public int getMaxLevel()
	{
		return ExperienceData.getInstance().getMaxPetLevel();
	}
}
