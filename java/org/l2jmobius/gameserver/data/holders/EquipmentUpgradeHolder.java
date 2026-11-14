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
package org.l2jmobius.gameserver.data.holders;

import java.util.List;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * This class holds the data related to equipment upgrades.<br>
 * It stores information about the current upgrade level and status of an item.<br>
 * It is used by {@code ItemHolder} to manage enhancement properties.
 * @author Mobius
 */
public class EquipmentUpgradeHolder
{
	private final int _id;
	private final int _requiredItemId;
	private final int _requiredItemEnchant;
	private final List<ItemHolder> _materials;
	private final long _adena;
	private final int _resultItemId;
	private final int _resultItemEnchant;
	private final boolean _announce;
	
	/**
	 * Creates a new {@code EquipmentUpgradeHolder} instance.<br>
	 * This constructor initializes all the requirements and results for an equipment upgrade.
	 * @param id The unique identifier for this upgrade.
	 * @param requiredItemId The item ID needed to start the upgrade.
	 * @param requiredItemEnchant The enchantment level required for the input item.
	 * @param materials A list of {@code ItemHolder} objects used as crafting materials.
	 * @param adena The amount of currency required for the upgrade.
	 * @param resultItemId The item ID that will be received after a successful upgrade.
	 * @param resultItemEnchant The enchantment level of the resulting item.
	 * @param announce Whether to broadcast the upgrade success to other players. Set to {@code true} for announcements or {@code false} to hide them.
	 */
	public EquipmentUpgradeHolder(int id, int requiredItemId, int requiredItemEnchant, List<ItemHolder> materials, long adena, int resultItemId, int resultItemEnchant, boolean announce)
	{
		_id = id;
		_requiredItemId = requiredItemId;
		_requiredItemEnchant = requiredItemEnchant;
		_materials = materials;
		_adena = adena;
		_resultItemId = resultItemId;
		_resultItemEnchant = resultItemEnchant;
		_announce = announce;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code EquipmentUpgradeHolder}.<br>
	 * This value is used to distinguish between different upgrades.
	 * @return The unique integer ID of the upgrade.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the ID of the item required for this upgrade.<br>
	 * This value is used to check if the player has the correct base item.
	 * @return The unique identifier of the required item.
	 */
	public int getRequiredItemId()
	{
		return _requiredItemId;
	}
	
	/**
	 * Retrieves the required enchantment level for an item upgrade.<br>
	 * This value is used to check if the player's item meets the necessary requirements.
	 * @return The {@code int} value of the required enchantment.
	 */
	public int getRequiredItemEnchant()
	{
		return _requiredItemEnchant;
	}
	
	/**
	 * Retrieves the list of materials required for an upgrade.<br>
	 * This method returns all items needed to complete the process.
	 * @return a {@code List} of {@code ItemHolder} objects.
	 */
	public List<ItemHolder> getMaterials()
	{
		return _materials;
	}
	
	/**
	 * Retrieves the amount of {@code adena} required for this upgrade.
	 * @return The total cost in {@code adena}.
	 */
	public long getAdena()
	{
		return _adena;
	}
	
	/**
	 * Returns the unique identifier for the item produced by this upgrade.<br>
	 * This value corresponds to the {@code _resultItemId} field.
	 * @return The ID of the resulting item.
	 */
	public int getResultItemId()
	{
		return _resultItemId;
	}
	
	/**
	 * Retrieves the enchantment level of the resulting item.<br>
	 * This value is defined in the {@code EquipmentUpgradeHolder}.
	 * @return The enchantment level as an {@code int}.
	 */
	public int getResultItemEnchant()
	{
		return _resultItemEnchant;
	}
	
	/**
	 * Checks if the upgrade should be announced to other players.<br>
	 * Returns {@code true} if an announcement is required.<br>
	 * Returns {@code false} otherwise.
	 * @return The announcement status as a {@code boolean}.
	 */
	public boolean isAnnounce()
	{
		return _announce;
	}
}
