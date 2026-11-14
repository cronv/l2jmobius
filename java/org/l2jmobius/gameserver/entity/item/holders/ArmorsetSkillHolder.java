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
package org.l2jmobius.gameserver.entity.item.holders;

import java.util.function.Function;

import org.l2jmobius.gameserver.data.holders.ArmorSet;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class manages skills associated with specific armor sets.<br>
 * It allows {@link org.l2jmobius.gameserver.entity.actor.Player} instances to access set-based abilities.<br>
 * It extends the base functionality of {@code SkillHolder}.
 * @author UnAfraid
 */
public class ArmorsetSkillHolder extends SkillHolder
{
	private final int _minimumPieces;
	private final int _minEnchant;
	private final int _artifactSlotMask;
	private final int _artifactBookSlot;
	private final boolean _isOptional;
	
	/**
	 * Creates a new {@code ArmorsetSkillHolder} instance.<br>
	 * This constructor initializes the skill requirements for an armor set.<br>
	 * It sets all necessary conditions like piece count and enchantment levels.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The required level of the skill.
	 * @param minimumPieces The minimum number of armor pieces needed.
	 * @param minEnchant The minimum enchantment level required.
	 * @param isOptional Whether this skill requirement is optional.
	 * @param artifactSlotMask A bitmask representing required artifact slots.
	 * @param artifactBookSlot The specific slot for the artifact book.
	 */
	public ArmorsetSkillHolder(int skillId, int skillLevel, int minimumPieces, int minEnchant, boolean isOptional, int artifactSlotMask, int artifactBookSlot)
	{
		super(skillId, skillLevel);
		_minimumPieces = minimumPieces;
		_minEnchant = minEnchant;
		_isOptional = isOptional;
		_artifactSlotMask = artifactSlotMask;
		_artifactBookSlot = artifactBookSlot;
	}
	
	/**
	 * Returns the minimum number of pieces required for this skill.<br>
	 * This value is used to validate if an {@code ArmorSet} meets the requirements.
	 * @return The minimum number of pieces as an {@code int}.
	 */
	public int getMinimumPieces()
	{
		return _minimumPieces;
	}
	
	/**
	 * Retrieves the minimum enchantment level required for this skill.<br>
	 * This value is used to validate if an {@code ArmorSet} meets the requirements.
	 * @return The minimum enchantment level as an {@code int}.
	 */
	public int getMinEnchant()
	{
		return _minEnchant;
	}
	
	/**
	 * Checks if the skill is optional.<br>
	 * Returns {@code true} if the skill does not require all conditions to be met.<br>
	 * Returns {@code false} otherwise.
	 * @return The optional status of this skill holder.
	 */
	public boolean isOptional()
	{
		return _isOptional;
	}
	
	/**
	 * Checks if a {@code Player} meets the requirements to use a specific skill.<br>
	 * It verifies piece counts, enchantment levels, and optional items.<br>
	 * The method also ensures the player does not already know the skill.
	 * @param player The {@code Player} attempting to use the skill.
	 * @param armorSet The {@code ArmorSet} data used for validation.
	 * @param idProvider A {@code Function} to map an {@code Item} to its unique ID.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	public boolean validateConditions(Player player, ArmorSet armorSet, Function<Item, Integer> idProvider)
	{
		// Player's doesn't have full busy (1 of 3) artifact real slot
		if (_artifactSlotMask > armorSet.getArtifactSlotMask(player, _artifactBookSlot))
		{
			return false;
		}
		
		// Player doesn't have enough items equipped to use this skill
		if (_minimumPieces > armorSet.getPieceCount(player, idProvider))
		{
			return false;
		}
		
		// Player's set enchantment isn't enough to use this skill
		if (_minEnchant > armorSet.getSetEnchant(player))
		{
			return false;
		}
		
		// Player doesn't have the required item to use this skill
		if (_isOptional && !armorSet.hasOptionalEquipped(player, idProvider))
		{
			return false;
		}
		
		// Player already knows that skill
		if (player.getSkillLevel(getSkillId()) == getSkillLevel())
		{
			return false;
		}
		
		return true;
	}
}
