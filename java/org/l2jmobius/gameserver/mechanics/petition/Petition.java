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
package org.l2jmobius.gameserver.mechanics.petition;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.PetitionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.PetitionVotePacket;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Represents a community petition within the game world.<br>
 * It manages the collection of votes and handles the logic for public requests.
 * @author xban1x
 */
public class Petition
{
	private final long _submitTime = System.currentTimeMillis();
	private final int _id;
	private final PetitionType _type;
	private PetitionState _state = PetitionState.PENDING;
	private final String _content;
	private final Collection<CreatureSay> _messageLog = ConcurrentHashMap.newKeySet();
	private final Player _petitioner;
	private Player _responder;
	
	/**
	 * Creates a new {@code Petition} instance.<br>
	 * This constructor initializes the petition with unique data.<br>
	 * It sets the content, type, and the player who started it.
	 * @param petitioner The {@code Player} who is submitting the petition.
	 * @param petitionText The text content of the petition.
	 * @param petitionType The integer ID representing the {@code PetitionType}.
	 */
	public Petition(Player petitioner, String petitionText, int petitionType)
	{
		_id = IdManager.getInstance().getNextId();
		_type = PetitionType.values()[petitionType - 1];
		_content = petitionText;
		_petitioner = petitioner;
	}
	
	/**
	 * Adds a new message to the petition log.<br>
	 * This method stores the provided {@code CreatureSay} object in the internal collection.
	 * @param cs The {@code CreatureSay} message to be added.
	 * @return {@code true} if the message was successfully added, or {@code false} otherwise.
	 */
	public boolean addLogMessage(CreatureSay cs)
	{
		return _messageLog.add(cs);
	}
	
	/**
	 * Retrieves all messages associated with this petition.<br>
	 * These messages are stored in a {@code Collection}.
	 * @return A {@code Collection} of {@code CreatureSay} objects.
	 */
	public Collection<CreatureSay> getLogMessages()
	{
		return _messageLog;
	}
	
	/**
	 * Ends the consultation phase of a petition.<br>
	 * Updates the state and notifies both the petitioner and responder.<br>
	 * Moves the petition to the completed list if successful.
	 * @param endState The {@code PetitionState} to set for this petition.
	 * @return {@code true} if the petition was successfully removed from the pending list, {@code false} otherwise.
	 */
	public boolean endPetitionConsultation(PetitionState endState)
	{
		setState(endState);
		
		if ((_responder != null) && _responder.isOnline())
		{
			if (endState == PetitionState.RESPONDER_REJECT)
			{
				_petitioner.sendMessage("Your petition was rejected. Please try again later.");
			}
			else
			{
				// Ending petition consultation with <Player>.
				SystemMessage sm = new SystemMessage(SystemMessageId.A_GLOBAL_SUPPORT_CONSULTATION_C1_HAS_BEEN_FINISHED);
				sm.addString(_petitioner.getName());
				_responder.sendPacket(sm);
				
				if (endState == PetitionState.PETITIONER_CANCEL)
				{
					// Receipt No. <ID> petition cancelled.
					sm = new SystemMessage(SystemMessageId.REQUEST_NO_S1_TO_THE_GLOBAL_SUPPORT_WAS_CANCELLED);
					sm.addInt(_id);
					_responder.sendPacket(sm);
				}
			}
		}
		
		// End petition consultation and inform them, if they are still online. And if petitioner is online, enable Evaluation button.
		if ((_petitioner != null) && _petitioner.isOnline())
		{
			_petitioner.sendPacket(SystemMessageId.GLOBAL_SUPPORT_HAS_ALREADY_RESPONDED_TO_YOUR_REQUEST_PLEASE_GIVE_US_FEEDBACK_ON_THE_SERVICE_QUALITY);
			_petitioner.sendPacket(PetitionVotePacket.STATIC_PACKET);
		}
		
		final int id = getId();
		PetitionManager.getInstance().getCompletedPetitions().put(id, this);
		return PetitionManager.getInstance().getPendingPetitions().remove(id) != null;
	}
	
	/**
	 * Retrieves the text content of this petition.<br>
	 * This method returns the original message provided by the petitioner.
	 * @return The {@code String} content of the petition.
	 */
	public String getContent()
	{
		return _content;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Petition}.<br>
	 * This ID is assigned when the petition is created.
	 * @return The unique integer ID of the petition.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the player who started this petition.
	 * @return the {@code Player} object representing the petitioner.
	 */
	public Player getPetitioner()
	{
		return _petitioner;
	}
	
	/**
	 * Retrieves the player currently acting as the responder.<br>
	 * This may return {@code null} if no responder has been assigned yet.
	 * @return The {@code Player} object representing the responder.
	 */
	public Player getResponder()
	{
		return _responder;
	}
	
	/**
	 * Retrieves the time when this petition was submitted.<br>
	 * The value is returned in milliseconds.
	 * @return the submission time as a {@code long}.
	 */
	public long getSubmitTime()
	{
		return _submitTime;
	}
	
	/**
	 * Retrieves the current status of the petition.<br>
	 * This method returns the {@code PetitionState} of this object.
	 * @return The current {@code PetitionState}.
	 */
	public PetitionState getState()
	{
		return _state;
	}
	
	/**
	 * Returns the human-readable name of the petition type.<br>
	 * This method converts the {@code PetitionType} into a string.<br>
	 * It replaces underscores with spaces for better readability.
	 * @return The formatted string representation of the petition type.
	 */
	public String getTypeAsString()
	{
		return _type.toString().replace('_', ' ');
	}
	
	/**
	 * Sends a network packet to the player who started the petition.<br>
	 * This method checks if the {@code _petitioner} is currently online.<br>
	 * If the petitioner is offline, no packet is sent.
	 * @param responsePacket The {@code ServerPacket} to be delivered to the petitioner.
	 */
	public void sendPetitionerPacket(ServerPacket responsePacket)
	{
		if ((_petitioner == null) || !_petitioner.isOnline())
		{
			// Allows petitioners to see their petition results upon logging back into the game.
			
			// endPetitionConsultation(PetitionState.Petitioner_Missing);
			return;
		}
		
		_petitioner.sendPacket(responsePacket);
	}
	
	/**
	 * Sends a network packet to the responder of this petition.<br>
	 * It checks if the {@code _responder} is online before sending.<br>
	 * If the responder is offline or null, it ends the consultation with {@code PetitionState.RESPONDER_MISSING}.
	 * @param responsePacket The {@code ServerPacket} to be sent to the responder.
	 */
	public void sendResponderPacket(ServerPacket responsePacket)
	{
		if ((_responder == null) || !_responder.isOnline())
		{
			endPetitionConsultation(PetitionState.RESPONDER_MISSING);
			return;
		}
		
		_responder.sendPacket(responsePacket);
	}
	
	/**
	 * Updates the current status of the petition.<br>
	 * This method changes the {@code _state} field to a new value.
	 * @param state The new {@code PetitionState} to apply.
	 */
	public void setState(PetitionState state)
	{
		_state = state;
	}
	
	/**
	 * Sets the administrator who will respond to this petition.<br>
	 * This method does nothing if a responder is already assigned.
	 * @param respondingAdmin The {@code Player} object representing the admin.
	 */
	public void setResponder(Player respondingAdmin)
	{
		if (_responder != null)
		{
			return;
		}
		
		_responder = respondingAdmin;
	}
}
