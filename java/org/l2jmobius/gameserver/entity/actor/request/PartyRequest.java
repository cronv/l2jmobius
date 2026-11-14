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
package org.l2jmobius.gameserver.entity.actor.request;

import java.util.Objects;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.groups.Party;

/**
 * Handles requests related to party management actions.<br>
 * This class processes interactions between {@code Player} entities and {@code Party} groups.
 * @author UnAfraid
 */
public class PartyRequest extends AbstractRequest
{
	private final Player _targetPlayer;
	private final Party _party;
	
	/**
	 * Creates a new {@code PartyRequest} object.<br>
	 * This request is used when a player wants to join or interact with a {@code Party}.<br>
	 * It requires the initiating player, the target player, and the specific party.
	 * @param player The player who is sending the request.
	 * @param targetPlayer The player who is being targeted by the request.
	 * @param party The {@code Party} associated with this request.
	 */
	public PartyRequest(Player player, Player targetPlayer, Party party)
	{
		super(player);
		Objects.requireNonNull(targetPlayer);
		Objects.requireNonNull(party);
		_targetPlayer = targetPlayer;
		_party = party;
	}
	
	/**
	 * Retrieves the player who is the target of this request.
	 * @return the {@code Player} object representing the target.
	 */
	public Player getTargetPlayer()
	{
		return _targetPlayer;
	}
	
	/**
	 * Retrieves the {@code Party} associated with this request.<br>
	 * This method returns the party that was passed during construction.
	 * @return the {@code Party} object.
	 */
	public Party getParty()
	{
		return _party;
	}
	
	/**
	 * Checks if the request is currently using a specific object.<br>
	 * This method always returns {@code false}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code false} regardless of the input.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
	
	/**
	 * This method is called when the request expires.<br>
	 * It removes this request from both the player and the target player.<br>
	 * The {@code Player} objects will no longer see this specific request.
	 */
	@Override
	public void onTimeout()
	{
		super.onTimeout();
		getPlayer().removeRequest(getClass());
		_targetPlayer.removeRequest(getClass());
	}
}
