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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a condition based on the instance level.<br>
 * This class is used to check if an entity meets specific requirements related to the {@code InstanceTemplate} level.
 * @author malyelfik
 */
public class ConditionLevel extends Condition
{
	private final int _min;
	private final int _max;
	
	/**
	 * Creates a new {@code ConditionLevel} instance.<br>
	 * This constructor initializes the level requirements for an instance zone.<br>
	 * It sets the minimum and maximum levels based on the provided parameters.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters The {@code StatSet} containing the min and max level values.
	 * @param onlyLeader Determines if the condition applies only to the party leader.
	 * @param showMessageAndHtml Determines if a message should be shown to the player.
	 */
	public ConditionLevel(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, onlyLeader, showMessageAndHtml);
		
		// Load params
		_min = Math.min(PlayerConfig.PLAYER_MAXIMUM_LEVEL, parameters.getInt("min", 1));
		_max = Math.min(PlayerConfig.PLAYER_MAXIMUM_LEVEL, parameters.getInt("max", Integer.MAX_VALUE));
		
		// Set message
		setSystemMessage(SystemMessageId.C1_DOES_NOT_MEET_LEVEL_REQUIREMENTS_AND_CANNOT_ENTER, (msg, player) -> msg.addString(player.getName()));
	}
	
	/**
	 * Checks if the {@code Player} level is within a specific range.<br>
	 * The level must be between the minimum and maximum values defined for this condition.
	 * @param player The {@code Player} whose level will be checked.
	 * @param npc The {@code Npc} associated with the condition.
	 * @return {@code true} if the {@code Player} level is within range, otherwise {@code false}.
	 */
	@Override
	protected boolean test(Player player, Npc npc)
	{
		return (player.getLevel() >= _min) && (player.getLevel() <= _max);
	}
}
