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

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.adenadistribution.ExDivideAdenaCancel;

/**
 * Handles the request to distribute {@code adena} among multiple players.<br>
 * This class processes the logic for dividing currency and sending the corresponding packets.
 * @author Sdw
 */
public class AdenaDistributionRequest extends AbstractRequest
{
	private final Player _distributor;
	private final List<Player> _players;
	private final int _adenaObjectId;
	private final long _adenaCount;
	
	/**
	 * Creates a new request to distribute adena to multiple players.<br>
	 * This object stores all necessary data for the distribution process.
	 * @param player The player who initiated the request.
	 * @param distributor The player who is providing the adena.
	 * @param players The list of players who will receive the adena.
	 * @param adenaObjectId The unique identifier for the adena object.
	 * @param adenaCount The total amount of adena to be distributed.
	 */
	public AdenaDistributionRequest(Player player, Player distributor, List<Player> players, int adenaObjectId, long adenaCount)
	{
		super(player);
		_distributor = distributor;
		_adenaObjectId = adenaObjectId;
		_players = players;
		_adenaCount = adenaCount;
	}
	
	/**
	 * Retrieves the player who initiated the distribution.<br>
	 * This returns the {@code Player} responsible for sending the request.
	 * @return the {@code Player} acting as the distributor.
	 */
	public Player getDistributor()
	{
		return _distributor;
	}
	
	/**
	 * Retrieves the list of players involved in this request.<br>
	 * This method returns all recipients for the adena distribution.
	 * @return a {@code List} of {@code Player} objects.
	 */
	public List<Player> getPlayers()
	{
		return _players;
	}
	
	/**
	 * Retrieves the unique identifier for the adena object.<br>
	 * This value corresponds to the {@code adenaObjectId} provided during request creation.
	 * @return The {@code int} ID of the adena object.
	 */
	public int getAdenaObjectId()
	{
		return _adenaObjectId;
	}
	
	/**
	 * Retrieves the total amount of adena to be distributed.<br>
	 * This value is stored in the {@code _adenaCount} field.
	 * @return The number of adena as a {@code long}.
	 */
	public long getAdenaCount()
	{
		return _adenaCount;
	}
	
	/**
	 * Checks if the request is associated with a specific object ID.<br>
	 * It compares the provided {@code objectId} against the internal {@code adenaObjectId}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the IDs match, otherwise {@code false}.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return objectId == _adenaObjectId;
	}
	
	/**
	 * This method is called when the request takes too long to complete.<br>
	 * It cancels the request for all involved players.<br>
	 * Each player receives a cancellation packet and has the request removed.
	 */
	@Override
	public void onTimeout()
	{
		super.onTimeout();
		_players.forEach(p ->
		{
			p.removeRequest(AdenaDistributionRequest.class);
			p.sendPacket(ExDivideAdenaCancel.STATIC_PACKET);
		});
	}
}
