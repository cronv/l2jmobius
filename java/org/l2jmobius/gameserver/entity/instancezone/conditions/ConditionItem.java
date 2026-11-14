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
package org.l2jmobius.gameserver.entity.instancezone.conditions;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a condition based on an item within an instance zone.<br>
 * It checks for specific requirements related to items before allowing actions.
 * @author malyelfik
 */
public class ConditionItem extends Condition
{
	private final int _itemId;
	private final long _count;
	private final boolean _take;
	
	/**
	 * Creates a new {@code ConditionItem} instance.<br>
	 * This constructor initializes the item requirements for an instance zone.<br>
	 * It sets up the required item ID, count, and whether to remove the item.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters A {@code StatSet} containing the "id", "count", and "take" values.
	 * @param onlyLeader Determines if this condition applies only to the party leader.
	 * @param showMessageAndHtml Determines if a system message and HTML should be displayed.
	 */
	public ConditionItem(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, onlyLeader, showMessageAndHtml);
		
		// Load params
		_itemId = parameters.getInt("id");
		_count = parameters.getLong("count");
		_take = parameters.getBoolean("take", false);
		
		// Set message
		setSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_ITEM_REQUIREMENTS_AND_CANNOT_ENTER, (msg, player) -> msg.addString(player.getName()));
	}
	
	/**
	 * Checks if the {@code Player} has enough items in their inventory.<br>
	 * It compares the current count of a specific item ID against the required amount.
	 * @param player The {@code Player} whose inventory will be checked.
	 * @param npc The {@code Npc} involved in the interaction.
	 * @return {@code true} if the player has at least the required number of items, otherwise {@code false}.
	 */
	@Override
	protected boolean test(Player player, Npc npc)
	{
		return player.getInventory().getInventoryItemCount(_itemId, -1) >= _count;
	}
	
	/**
	 * This method is called when the condition is successfully met.<br>
	 * It handles any actions that should occur after success.<br>
	 * If {@code _take} is true, it removes the required item from the player.
	 * @param player The {@code Player} who triggered the successful condition.
	 */
	@Override
	protected void onSuccess(Player player)
	{
		if (_take)
		{
			player.destroyItemByItemId(ItemProcessType.FEE, _itemId, _count, null, true);
		}
	}
}
