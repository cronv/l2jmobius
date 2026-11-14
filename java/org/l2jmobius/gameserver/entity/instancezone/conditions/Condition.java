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

import java.util.List;
import java.util.function.BiConsumer;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents an abstract condition for an {@code InstanceTemplate}.<br>
 * It defines the logic used to determine if specific requirements are met within an instance zone.
 * @author malyelfik
 */
public abstract class Condition
{
	private final InstanceTemplate _template;
	private final StatSet _parameters;
	private final boolean _leaderOnly;
	private final boolean _showMessageAndHtml;
	private SystemMessageId _systemMsg = null;
	private BiConsumer<SystemMessage, Player> _systemMsgParams = null;
	
	/**
	 * Creates a new instance of a {@code Condition}.<br>
	 * This constructor initializes the required settings for checking instance requirements.
	 * @param template The {@code InstanceTemplate} where this condition is registered.
	 * @param parameters The {@code StatSet} containing specific values for this condition.
	 * @param onlyLeader Set to {@code true} if the check should only apply to the group leader.
	 * @param showMessageAndHtml Set to {@code true} to display both a system message and HTML content.
	 */
	public Condition(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		_template = template;
		_parameters = parameters;
		_leaderOnly = onlyLeader;
		_showMessageAndHtml = showMessageAndHtml;
	}
	
	/**
	 * Retrieves the parameters associated with this condition.<br>
	 * These values are used to define specific logic for the instance condition.
	 * @return the {@code StatSet} containing the condition parameters.
	 */
	protected StatSet getParameters()
	{
		return _parameters;
	}
	
	/**
	 * Retrieves the {@code InstanceTemplate} associated with this condition.<br>
	 * This template defines the instance where the condition is registered.
	 * @return the {@code InstanceTemplate} for this condition.
	 */
	public InstanceTemplate getInstanceTemplate()
	{
		return _template;
	}
	
	/**
	 * Checks if all members in a group meet the required conditions.<br>
	 * If any member fails, a message is sent to the group.<br>
	 * The check stops early if only the leader needs validation.
	 * @param npc The {@code Npc} involved in the condition.
	 * @param group The list of {@code Player} objects to validate.
	 * @param htmlCallback A {@code BiConsumer} used to send HTML messages to players.
	 * @return {@code true} if all requirements are met, {@code false} otherwise.
	 */
	public boolean validate(Npc npc, List<Player> group, BiConsumer<Player, String> htmlCallback)
	{
		for (Player member : group)
		{
			if (!test(member, npc, group))
			{
				sendMessage(group, member, htmlCallback);
				return false;
			}
			
			if (_leaderOnly)
			{
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Sends various types of messages to players based on the condition parameters.<br>
	 * It handles HTML content, text messages, and system messages.<br>
	 * The method determines whether to send messages to a single member or the entire group.
	 * @param group The list of {@code Player} objects in the current group.
	 * @param member The specific {@code Player} who triggered the action.
	 * @param htmlCallback A {@code BiConsumer} used to send HTML content to the first player in the group.
	 */
	private void sendMessage(List<Player> group, Player member, BiConsumer<Player, String> htmlCallback)
	{
		// Send HTML message if condition has any.
		final String html = _parameters.getString("html", null);
		if ((html != null) && (htmlCallback != null))
		{
			// Send HTML only to player who make request to enter.
			htmlCallback.accept(group.get(0), html);
			
			// Stop execution if only one message is allowed.
			if (!_showMessageAndHtml)
			{
				return;
			}
		}
		
		// Send text message if condition has any.
		final String message = _parameters.getString("message", null);
		if (message != null)
		{
			if (_leaderOnly)
			{
				member.sendMessage(message);
			}
			else
			{
				group.forEach(p -> p.sendMessage(message));
			}
			return;
		}
		
		// Send system message if condition has any.
		if (_systemMsg != null)
		{
			final SystemMessage msg = new SystemMessage(_systemMsg);
			if (_systemMsgParams != null)
			{
				_systemMsgParams.accept(msg, member);
			}
			
			if (_leaderOnly)
			{
				member.sendPacket(msg);
			}
			else
			{
				group.forEach(p -> p.sendPacket(msg));
			}
		}
	}
	
	/**
	 * Applies the condition effect to a list of players.<br>
	 * It calls {@code onSuccess} for each member in the group.<br>
	 * If {@code _leaderOnly} is true, it stops after the first player.
	 * @param group The list of {@code Player} objects to receive the effect.
	 */
	public void applyEffect(List<Player> group)
	{
		for (Player member : group)
		{
			onSuccess(member);
			if (_leaderOnly)
			{
				break;
			}
		}
	}
	
	/**
	 * Sets the system message for this condition.<br>
	 * This method updates the {@code _systemMsg} field.
	 * @param msg The unique identifier of the system message to set.
	 */
	protected void setSystemMessage(SystemMessageId msg)
	{
		_systemMsg = msg;
	}
	
	/**
	 * Sets the system message and its associated parameters.<br>
	 * This method defines what message to send and how to process it.
	 * @param msg The unique identifier for the system message.
	 * @param params A consumer that handles the logic between a {@code SystemMessage} and a {@code Player}.
	 */
	protected void setSystemMessage(SystemMessageId msg, BiConsumer<SystemMessage, Player> params)
	{
		setSystemMessage(msg);
		_systemMsgParams = params;
	}
	
	/**
	 * Checks if the {@code Player} meets the requirements for an action involving an {@code Npc}.<br>
	 * This method delegates the check to the version that does not require a group.
	 * @param player The {@code Player} who is performing the action.
	 * @param npc The {@code Npc} involved in the interaction.
	 * @param group The list of {@code Player} objects in the current group.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	protected boolean test(Player player, Npc npc, List<Player> group)
	{
		return test(player, npc);
	}
	
	/**
	 * Performs a basic check between a {@code Player} and an {@code Npc}.<br>
	 * This method currently always returns {@code true}.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} to check against.
	 * @return Always returns {@code true}.
	 */
	protected boolean test(Player player, Npc npc)
	{
		return true;
	}
	
	/**
	 * This method is called when a condition is successfully met.<br>
	 * It handles the logic that should occur after a successful check.
	 * @param player The {@code Player} who triggered the success.
	 */
	protected void onSuccess(Player player)
	{
	}
}
