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
package org.l2jmobius.gameserver.data.holders;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ArmorsetSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Represents a collection of armor items that provide specific bonuses when worn together.<br>
 * This class manages the logic for identifying and applying set effects to a {@code Player}.
 * @author UnAfraid
 */
public class ArmorSet
{
	private final int _id;
	private final int _minimumPieces;
	private final boolean _isVisual;
	
	private final int[] _requiredItems;
	private final int[] _optionalItems;
	
	private final List<ArmorsetSkillHolder> _skills;
	private final Map<BaseStat, Double> _stats;
	
	private static final int[] ARMORSET_SLOTS =
	{
		Inventory.PAPERDOLL_CHEST,
		Inventory.PAPERDOLL_LEGS,
		Inventory.PAPERDOLL_HEAD,
		Inventory.PAPERDOLL_GLOVES,
		Inventory.PAPERDOLL_FEET,
		Inventory.PAPERDOLL_HAIR,
		Inventory.PAPERDOLL_HAIR2,
		Inventory.PAPERDOLL_DECO1,
		Inventory.PAPERDOLL_DECO2,
		Inventory.PAPERDOLL_DECO3,
		Inventory.PAPERDOLL_DECO4,
		Inventory.PAPERDOLL_DECO5,
		Inventory.PAPERDOLL_DECO6
	};
	private static final int[] ARTIFACT_1_SLOTS =
	{
		Inventory.PAPERDOLL_ARTIFACT1,
		Inventory.PAPERDOLL_ARTIFACT2,
		Inventory.PAPERDOLL_ARTIFACT3,
		Inventory.PAPERDOLL_ARTIFACT4,
		Inventory.PAPERDOLL_ARTIFACT13,
		Inventory.PAPERDOLL_ARTIFACT16,
		Inventory.PAPERDOLL_ARTIFACT19,
	
	};
	private static final int[] ARTIFACT_2_SLOTS =
	{
		Inventory.PAPERDOLL_ARTIFACT5,
		Inventory.PAPERDOLL_ARTIFACT6,
		Inventory.PAPERDOLL_ARTIFACT7,
		Inventory.PAPERDOLL_ARTIFACT8,
		Inventory.PAPERDOLL_ARTIFACT14,
		Inventory.PAPERDOLL_ARTIFACT17,
		Inventory.PAPERDOLL_ARTIFACT20,
	
	};
	private static final int[] ARTIFACT_3_SLOTS =
	{
		Inventory.PAPERDOLL_ARTIFACT9,
		Inventory.PAPERDOLL_ARTIFACT10,
		Inventory.PAPERDOLL_ARTIFACT11,
		Inventory.PAPERDOLL_ARTIFACT12,
		Inventory.PAPERDOLL_ARTIFACT15,
		Inventory.PAPERDOLL_ARTIFACT18,
		Inventory.PAPERDOLL_ARTIFACT21,
	
	};
	
	/**
	 * Creates a new {@code ArmorSet} instance with the specified properties.<br>
	 * This constructor initializes all required data for an armor set.
	 * @param id The unique identifier for the armor set.
	 * @param minimumPieces The number of pieces needed to activate the set.
	 * @param isVisual Whether this set is only used for visual effects.
	 * @param requiredItems A {@code Set} of item IDs that must be equipped.
	 * @param optionalItems A {@code Set} of item IDs that can be equipped but are not required.
	 * @param skills A {@code List} of {@code ArmorsetSkillHolder} objects associated with the set.
	 * @param stats A {@code Map} linking {@code BaseStat} types to their bonus values.
	 */
	public ArmorSet(int id, int minimumPieces, boolean isVisual, Set<Integer> requiredItems, Set<Integer> optionalItems, List<ArmorsetSkillHolder> skills, Map<BaseStat, Double> stats)
	{
		_id = id;
		_minimumPieces = minimumPieces;
		_isVisual = isVisual;
		_requiredItems = requiredItems.stream().mapToInt(x -> x).toArray();
		_optionalItems = optionalItems.stream().mapToInt(x -> x).toArray();
		_skills = skills;
		_stats = stats;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code ArmorSet}.<br>
	 * This ID is used to distinguish different armor sets in the game.
	 * @return The integer ID of the armor set.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Returns the minimum number of pieces required to activate this armor set.<br>
	 * This value is retrieved from the internal {@code _minimumPieces} field.
	 * @return The minimum number of pieces as an {@code int}.
	 */
	public int getMinimumPieces()
	{
		return _minimumPieces;
	}
	
	/**
	 * Checks if this armor set is purely for visual purposes.<br>
	 * Returns {@code true} if the set provides no stats or skills.<br>
	 * Returns {@code false} if the set provides active bonuses.
	 * @return the visual status of the armor set.
	 */
	public boolean isVisual()
	{
		return _isVisual;
	}
	
	/**
	 * Retrieves the list of required item IDs for this armor set.<br>
	 * These items must be equipped to activate the set bonuses.
	 * @return an {@code int[]} containing the required item IDs.
	 */
	public int[] getRequiredItems()
	{
		return _requiredItems;
	}
	
	/**
	 * Retrieves the list of optional items for this armor set.<br>
	 * These items provide bonuses but are not required to activate the set.
	 * @return an {@code int[]} containing the IDs of all optional items.
	 */
	public int[] getOptionalItems()
	{
		return _optionalItems;
	}
	
	/**
	 * Retrieves the list of skills associated with this armor set.<br>
	 * These skills are granted when the set requirements are met.
	 * @return a {@code List} of {@code ArmorsetSkillHolder} objects.
	 */
	public List<ArmorsetSkillHolder> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Retrieves the bonus value for a specific statistic.<br>
	 * This method checks the internal stats map of the armor set.<br>
	 * It returns {@code 0.0} if the statistic is not found.
	 * @param stat The {@code BaseStat} to check for a bonus.
	 * @return The bonus value as a {@code double}.
	 */
	public double getStatsBonus(BaseStat stat)
	{
		return _stats.getOrDefault(stat, 0d);
	}
	
	/**
	 * Checks if a specific item ID is part of the optional items for this set.<br>
	 * This method looks through the {@code _optionalItems} array.
	 * @param shieldId The unique identifier of the item to check.
	 * @return {@code true} if the item is found, otherwise {@code false}.
	 */
	public boolean containOptionalItem(int shieldId)
	{
		return ArrayUtil.contains(_optionalItems, shieldId);
	}
	
	/**
	 * Calculates the enchantment level of an armor set for a specific player.<br>
	 * It checks if the player meets the minimum piece requirement first.<br>
	 * The result is the lowest enchantment level among all required items equipped by the player.
	 * @param player The {@code Player} whose equipment is being checked.
	 * @return The lowest enchantment level found, or 0 if no required items are equipped.
	 */
	public int getSetEnchant(Player player)
	{
		// Player don't have full set.
		if (getPieceCount(player) < _minimumPieces)
		{
			return 0;
		}
		
		final PlayerInventory inv = player.getInventory();
		int enchantLevel = Byte.MAX_VALUE;
		for (int armorSlot : ARMORSET_SLOTS)
		{
			final Item itemPart = inv.getPaperdollItem(armorSlot);
			if ((itemPart != null) && ArrayUtil.contains(_requiredItems, itemPart.getId()))
			{
				final int itemEnchantLevel = itemPart.getEnchantLevel();
				if (enchantLevel > itemEnchantLevel)
				{
					enchantLevel = itemEnchantLevel;
				}
			}
		}
		
		if (enchantLevel == Byte.MAX_VALUE)
		{
			enchantLevel = 0;
		}
		
		return enchantLevel;
	}
	
	/**
	 * Calculates a bitmask of equipped artifact slots based on the provided book. <br>
	 * It checks if items in specific inventory slots match the required set items. <br>
	 * The result is a sum of the slot indices for all valid matches found.
	 * @param player The {@code Player} whose inventory will be checked.
	 * @param bookSlot The identifier for the artifact group to check (1, 2, or 3).
	 * @return The calculated bitmask of matching artifact slots.
	 */
	public int getArtifactSlotMask(Player player, int bookSlot)
	{
		final PlayerInventory inv = player.getInventory();
		int slotMask = 0;
		switch (bookSlot)
		{
			case 1:
			{
				for (int artifactSlot : ARTIFACT_1_SLOTS)
				{
					final Item itemPart = inv.getPaperdollItem(artifactSlot);
					if ((itemPart != null) && ArrayUtil.contains(_requiredItems, itemPart.getId()))
					{
						slotMask += artifactSlot;
					}
				}
				break;
			}
			case 2:
			{
				for (int artifactSlot : ARTIFACT_2_SLOTS)
				{
					final Item itemPart = inv.getPaperdollItem(artifactSlot);
					if ((itemPart != null) && ArrayUtil.contains(_requiredItems, itemPart.getId()))
					{
						slotMask += artifactSlot;
					}
				}
				break;
			}
			case 3:
			{
				for (int artifactSlot : ARTIFACT_3_SLOTS)
				{
					final Item itemPart = inv.getPaperdollItem(artifactSlot);
					if ((itemPart != null) && ArrayUtil.contains(_requiredItems, itemPart.getId()))
					{
						slotMask += artifactSlot;
					}
				}
				break;
			}
		}
		
		return slotMask;
	}
	
	/**
	 * Checks if the player is wearing any optional items from this set.<br>
	 * It scans all paperdoll slots for matching IDs.
	 * @param player The {@code Player} to check.
	 * @param idProvider A function to extract the item ID from an {@code Item}.
	 * @return {@code true} if at least one optional item is equipped, otherwise {@code false}.
	 */
	public boolean hasOptionalEquipped(Player player, Function<Item, Integer> idProvider)
	{
		for (Item item : player.getInventory().getPaperdollItems())
		{
			if (ArrayUtil.contains(_optionalItems, idProvider.apply(item)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Calculates the number of required pieces a {@code Player} is wearing.<br>
	 * It checks items in the paperdoll slots against the required item IDs.
	 * @param player The {@code Player} to check.
	 * @param idProvider A function to extract an ID from an {@code Item}.
	 * @return The total count of equipped required pieces.
	 */
	public long getPieceCount(Player player, Function<Item, Integer> idProvider)
	{
		return player.getInventory().getPaperdollItemCount(item -> ArrayUtil.contains(_requiredItems, idProvider.apply(item)));
	}
	
	/**
	 * Calculates how many required pieces of this armor set a player is wearing.<br>
	 * It checks the {@code Player}'s paperdoll inventory for matching items.
	 * @param player The {@code Player} to check.
	 * @return The total count of equipped required pieces.
	 */
	public long getPieceCount(Player player)
	{
		return player.getInventory().getPaperdollItemCount(item -> ArrayUtil.contains(_requiredItems, item.getId()));
	}
}
