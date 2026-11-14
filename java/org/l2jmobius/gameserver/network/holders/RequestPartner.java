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
package org.l2jmobius.gameserver.network.holders;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class manages transaction requests between two {@code Player} instances.<br>
 * It handles the synchronization of actions performed by one player on another.
 * @author kriau
 */
public class RequestPartner
{
	private static final int REQUEST_TIMEOUT = 15; // in secs
	
	protected Player _player;
	protected Player _partner;
	protected boolean _isRequestor;
	protected boolean _isAnswerer;
	protected ClientPacket _requestPacket;
	
	/**
	 * Creates a new {@code RequestPartner} instance.<br>
	 * This method initializes the object with a specific {@code Player}.
	 * @param player The {@code Player} who is initiating or receiving the request.
	 */
	public RequestPartner(Player player)
	{
		_player = player;
	}
	
	/**
	 * Resets the internal state of this request.<br>
	 * It sets {@code _partner} and {@code _requestPacket} to {@code null}.<br>
	 * It also resets the boolean flags for requestor and answerer status.
	 */
	protected void clear()
	{
		_partner = null;
		_requestPacket = null;
		_isRequestor = false;
		_isAnswerer = false;
	}
	
	/**
	 * Sets the {@code Player} associated with this request.<br>
	 * This method updates the {@code _partner} field.<br>
	 * It is synchronized to ensure thread safety.
	 * @param partner The {@code Player} to be set as the partner.
	 */
	private synchronized void setPartner(Player partner)
	{
		_partner = partner;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this request.<br>
	 * Returns {@code null} if no partner has been assigned yet.
	 * @return The {@code Player} object representing the partner.
	 */
	public Player getPartner()
	{
		return _partner;
	}
	
	/**
	 * Updates the internal request packet.<br>
	 * This method stores the provided {@code ClientPacket} into the {@code _requestPacket} field.
	 * @param packet The {@code ClientPacket} to be stored.
	 */
	private synchronized void setRequestPacket(ClientPacket packet)
	{
		_requestPacket = packet;
	}
	
	/**
	 * Retrieves the {@code ClientPacket} associated with this request.<br>
	 * This packet contains the data sent by the player to initiate a transaction.
	 * @return the {@code ClientPacket} object or {@code null} if no packet exists.
	 */
	public ClientPacket getRequestPacket()
	{
		return _requestPacket;
	}
	
	/**
	 * Sets the partner and the packet for a new request.<br>
	 * This method validates if the target is available to receive a request.<br>
	 * It updates both players involved in the transaction.
	 * @param partner The {@code Player} who will receive the request.
	 * @param packet The {@code ClientPacket} being sent as part of the request.
	 * @return {@code true} if the request was successfully set, otherwise {@code false}.
	 */
	public synchronized boolean setRequest(Player partner, ClientPacket packet)
	{
		if (partner == null)
		{
			_player.sendPacket(SystemMessageId.THE_TARGET_CANNOT_BE_INVITED);
			return false;
		}
		
		if (partner.getRequest().isProcessingRequest())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ON_ANOTHER_TASK_PLEASE_TRY_AGAIN_LATER);
			sm.addString(partner.getName());
			_player.sendPacket(sm);
			return false;
		}
		
		if (isProcessingRequest())
		{
			_player.sendPacket(SystemMessageId.WAITING_FOR_ANOTHER_REPLY);
			return false;
		}
		
		_partner = partner;
		_requestPacket = packet;
		setOnRequestTimer(true);
		_partner.getRequest().setPartner(_player);
		_partner.getRequest().setRequestPacket(packet);
		_partner.getRequest().setOnRequestTimer(false);
		return true;
	}
	
	/**
	 * Sets the roles for the players involved in a request.<br>
	 * It determines who is the requestor and who is the answerer.<br>
	 * This method also starts a timer to clear the request after a timeout.
	 * @param isRequestor The boolean value indicating if the player is the one starting the request.
	 */
	private void setOnRequestTimer(boolean isRequestor)
	{
		_isRequestor = isRequestor;
		_isAnswerer = !isRequestor;
		ThreadPool.schedule(this::clear, REQUEST_TIMEOUT * 1000);
	}
	
	/**
	 * Handles the logic when a response to a request is received.<br>
	 * It clears any existing requests for the {@code _partner}.<br>
	 * This method also calls {@code clear} to reset the current state.
	 */
	public void onRequestResponse()
	{
		if (_partner != null)
		{
			_partner.getRequest().clear();
		}
		
		clear();
	}
	
	/**
	 * Checks if a request is currently being handled.<br>
	 * It returns {@code true} if a partner has been assigned.<br>
	 * It returns {@code false} if no partner exists.
	 * @return {@code true} if the request is active, otherwise {@code false}.
	 */
	public boolean isProcessingRequest()
	{
		return _partner != null;
	}
}
