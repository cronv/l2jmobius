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
package org.l2jmobius.gameserver.managers;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.petition.Petition;
import org.l2jmobius.gameserver.mechanics.petition.PetitionState;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Manages the lifecycle and logic of player petitions.<br>
 * This class handles creating, updating, and processing {@code Petition} objects within the game world.
 * @author Tempy
 */
public class PetitionManager
{
	protected static final Logger LOGGER = Logger.getLogger(PetitionManager.class.getName());
	
	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	
	private final Map<Integer, Petition> _pendingPetitions;
	private final Map<Integer, Petition> _completedPetitions;
	
	/**
	 * Initializes the {@code PetitionManager}.<br>
	 * This constructor sets up the internal maps for pending and completed petitions.
	 */
	protected PetitionManager()
	{
		_pendingPetitions = new HashMap<>();
		_completedPetitions = new HashMap<>();
	}
	
	/**
	 * Removes all entries from the completed petitions collection.<br>
	 * This method clears the internal storage of finished petitions.<br>
	 * It logs the number of pending petitions remaining after the clear operation.
	 */
	public void clearCompletedPetitions()
	{
		final int numPetitions = _pendingPetitions.size();
		_completedPetitions.clear();
		LOGGER.info(getClass().getSimpleName() + ": Completed petition data cleared. " + numPetitions + " petitions removed.");
	}
	
	/**
	 * Removes all items from the pending petition queue.<br>
	 * This method clears the internal map of active petitions.<br>
	 * It logs the number of petitions that were removed.
	 */
	public void clearPendingPetitions()
	{
		final int numPetitions = _pendingPetitions.size();
		_pendingPetitions.clear();
		LOGGER.info(getClass().getSimpleName() + ": Pending petition queue cleared. " + numPetitions + " petitions removed.");
	}
	
	/**
	 * Accepts a pending petition for an administrator.<br>
	 * This method updates the petition state to {@code IN_PROCESS}.<br>
	 * It sends confirmation messages to both the petitioner and the responder.
	 * @param respondingAdmin The {@code Player} who is accepting the petition.
	 * @param petitionId The unique identifier of the petition.
	 * @return {@code true} if the petition was successfully accepted, otherwise {@code false}.
	 */
	public boolean acceptPetition(Player respondingAdmin, int petitionId)
	{
		if (!isValidPetition(petitionId))
		{
			return false;
		}
		
		final Petition currPetition = _pendingPetitions.get(petitionId);
		if (currPetition.getResponder() != null)
		{
			return false;
		}
		
		currPetition.setResponder(respondingAdmin);
		currPetition.setState(PetitionState.IN_PROCESS);
		
		// Petition application accepted. (Send to Petitioner).
		currPetition.sendPetitionerPacket(new SystemMessage(SystemMessageId.YOUR_GLOBAL_SUPPORT_REQUEST_WAS_RECEIVED_2));
		
		// Petition application accepted. Reciept No. is <ID>.
		SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_GLOBAL_SUPPORT_REQUEST_WAS_RECEIVED_REQUEST_NO_S1);
		sm.addInt(currPetition.getId());
		currPetition.sendResponderPacket(sm);
		
		// Petition consultation with <Player> underway.
		sm = new SystemMessage(SystemMessageId.A_GLOBAL_SUPPORT_CONSULTATION_C1_HAS_BEEN_STARTED);
		sm.addString(currPetition.getPetitioner().getName());
		currPetition.sendResponderPacket(sm);
		
		// Set responder name on petitioner instance.
		currPetition.getPetitioner().setLastPetitionGmName(currPetition.getResponder().getName());
		return true;
	}
	
	/**
	 * Cancels a petition currently in progress for the given player.<br>
	 * This method checks if the {@code Player} is either the petitioner or the responder.<br>
	 * It then updates the petition state to a cancelled status.
	 * @param player The {@code Player} who wants to cancel their active petition.
	 * @return {@code true} if the petition was successfully cancelled, otherwise {@code false}.
	 */
	public boolean cancelActivePetition(Player player)
	{
		final int playerObjectId = player.getObjectId();
		for (Petition currPetition : _pendingPetitions.values())
		{
			final Player petitioner = currPetition.getPetitioner();
			if ((petitioner != null) && (petitioner.getObjectId() == playerObjectId))
			{
				return (currPetition.endPetitionConsultation(PetitionState.PETITIONER_CANCEL));
			}
			
			final Player responder = currPetition.getResponder();
			if ((responder != null) && (responder.getObjectId() == playerObjectId))
			{
				return (currPetition.endPetitionConsultation(PetitionState.RESPONDER_CANCEL));
			}
		}
		
		return false;
	}
	
	/**
	 * This method checks for any pending petitions belonging to a specific player.<br>
	 * If a petition is found, it sends all associated log messages to the {@code Player}.<br>
	 * The check only proceeds if the {@code petitioner} object is not {@code null}.
	 * @param petitioner The {@code Player} who may have an active petition.
	 */
	public void checkPetitionMessages(Player petitioner)
	{
		if (petitioner != null)
		{
			final int petitionerObjectId = petitioner.getObjectId();
			for (Petition currPetition : _pendingPetitions.values())
			{
				if (currPetition == null)
				{
					continue;
				}
				
				final Player currPetitioner = currPetition.getPetitioner();
				if ((currPetitioner != null) && (currPetitioner.getObjectId() == petitionerObjectId))
				{
					for (CreatureSay logMessage : currPetition.getLogMessages())
					{
						petitioner.sendPacket(logMessage);
					}
					
					return;
				}
			}
		}
	}
	
	/**
	 * Ends an active petition for a specific player.<br>
	 * This method checks if the {@code Player} is a GM and has an active pending petition.<br>
	 * If found, it marks the petition as {@code PetitionState.COMPLETED}.
	 * @param player The {@code Player} who is currently responding to a petition.
	 * @return {@code true} if the petition was successfully ended, otherwise {@code false}.
	 */
	public boolean endActivePetition(Player player)
	{
		if (!player.isGM())
		{
			return false;
		}
		
		final int playerObjectId = player.getObjectId();
		for (Petition currPetition : _pendingPetitions.values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			final Player responder = currPetition.getResponder();
			if ((responder != null) && (responder.getObjectId() == playerObjectId))
			{
				return (currPetition.endPetitionConsultation(PetitionState.COMPLETED));
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves all petitions that have been finished.<br>
	 * This method returns the internal collection of completed items.
	 * @return A {@code Map} where the key is an {@code Integer} ID and the value is a {@code Petition}.
	 */
	public Map<Integer, Petition> getCompletedPetitions()
	{
		return _completedPetitions;
	}
	
	/**
	 * Retrieves all petitions that are currently pending.
	 * @return A {@code Map} where the key is an {@code Integer} ID and the value is a {@code Petition} object.
	 */
	public Map<Integer, Petition> getPendingPetitions()
	{
		return _pendingPetitions;
	}
	
	/**
	 * Retrieves the total number of petitions currently waiting for a response.<br>
	 * This method counts all entries in the {@code _pendingPetitions} map.
	 * @return The count of pending petitions as an {@code int}.
	 */
	public int getPendingPetitionCount()
	{
		return _pendingPetitions.size();
	}
	
	/**
	 * Calculates the total number of petitions associated with a specific player.<br>
	 * This includes both pending and completed petitions.<br>
	 * If the provided {@code Player} is {@code null}, it returns 0.
	 * @param player The {@code Player} to check for petition counts.
	 * @return The total count of petitions for the given player.
	 */
	public int getPlayerTotalPetitionCount(Player player)
	{
		if (player == null)
		{
			return 0;
		}
		
		final int playerObjectId = player.getObjectId();
		int petitionCount = 0;
		for (Petition currPetition : _pendingPetitions.values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			final Player petitioner = currPetition.getPetitioner();
			if ((petitioner != null) && (petitioner.getObjectId() == playerObjectId))
			{
				petitionCount++;
			}
		}
		
		for (Petition currPetition : _completedPetitions.values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			final Player petitioner = currPetition.getPetitioner();
			if ((petitioner != null) && (petitioner.getObjectId() == playerObjectId))
			{
				petitionCount++;
			}
		}
		
		return petitionCount;
	}
	
	/**
	 * Checks if any petition is currently being processed.<br>
	 * It iterates through all pending petitions to find one in the {@code IN_PROCESS} state.
	 * @return {@code true} if at least one petition is in progress, otherwise {@code false}.
	 */
	public boolean isPetitionInProcess()
	{
		for (Petition currPetition : _pendingPetitions.values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			if (currPetition.getState() == PetitionState.IN_PROCESS)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a specific petition is currently being processed.<br>
	 * It verifies the petition exists and checks its current state.
	 * @param petitionId The unique identifier of the petition to check.
	 * @return {@code true} if the petition is in the {@code IN_PROCESS} state, otherwise {@code false}.
	 */
	public boolean isPetitionInProcess(int petitionId)
	{
		if (!isValidPetition(petitionId))
		{
			return false;
		}
		
		final Petition currPetition = _pendingPetitions.get(petitionId);
		return (currPetition.getState() == PetitionState.IN_PROCESS);
	}
	
	/**
	 * Checks if a specific player is currently involved in an active petition.<br>
	 * This method returns {@code true} if the player is either the petitioner or the responder.<br>
	 * It only considers petitions that are currently in the {@code IN_PROCESS} state.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the player is part of a consultation, otherwise {@code false}.
	 */
	public boolean isPlayerInConsultation(Player player)
	{
		if (player != null)
		{
			final int playerObjectId = player.getObjectId();
			for (Petition currPetition : _pendingPetitions.values())
			{
				if (currPetition == null)
				{
					continue;
				}
				
				if (currPetition.getState() != PetitionState.IN_PROCESS)
				{
					continue;
				}
				
				final Player petitioner = currPetition.getPetitioner();
				final Player responder = currPetition.getResponder();
				if (((petitioner != null) && (petitioner.getObjectId() == playerObjectId)) || ((responder != null) && (responder.getObjectId() == playerObjectId)))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the petitioning system is enabled.<br>
	 * This value is retrieved from {@code PlayerConfig}.
	 * @return {@code true} if petitioning is allowed, {@code false} otherwise.
	 */
	public boolean isPetitioningAllowed()
	{
		return PlayerConfig.PETITIONING_ALLOWED;
	}
	
	/**
	 * Checks if a specific player has an active pending petition.<br>
	 * This method iterates through all current petitions to find a match.<br>
	 * It returns {@code true} if the player is found as a petitioner.
	 * @param petitioner The {@code Player} object to check.
	 * @return {@code true} if a petition is pending, otherwise {@code false}.
	 */
	public boolean isPlayerPetitionPending(Player petitioner)
	{
		if (petitioner != null)
		{
			final int petitionerObjectId = petitioner.getObjectId();
			for (Petition currPetition : _pendingPetitions.values())
			{
				if (currPetition == null)
				{
					continue;
				}
				
				final Player currPetitioner = currPetition.getPetitioner();
				if ((currPetitioner != null) && (currPetitioner.getObjectId() == petitionerObjectId))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if a petition is currently pending.<br>
	 * It verifies if the {@code petitionId} exists in the pending map.
	 * @param petitionId The unique identifier of the petition to check.
	 * @return {@code true} if the petition is pending, {@code false} otherwise.
	 */
	private boolean isValidPetition(int petitionId)
	{
		return _pendingPetitions.containsKey(petitionId);
	}
	
	/**
	 * Rejects a pending petition and assigns the admin as the responder.<br>
	 * This method updates the {@code Petition} state to rejected.
	 * @param respondingAdmin The {@code Player} who is rejecting the petition.
	 * @param petitionId The unique identifier of the petition to reject.
	 * @return {@code true} if the petition was successfully rejected, {@code false} otherwise.
	 */
	public boolean rejectPetition(Player respondingAdmin, int petitionId)
	{
		if (!isValidPetition(petitionId))
		{
			return false;
		}
		
		final Petition currPetition = _pendingPetitions.get(petitionId);
		if (currPetition.getResponder() != null)
		{
			return false;
		}
		
		currPetition.setResponder(respondingAdmin);
		return (currPetition.endPetitionConsultation(PetitionState.RESPONDER_REJECT));
	}
	
	/**
	 * Sends a message to both the petitioner and responder of an active petition.<br>
	 * This method identifies if the {@code Player} is involved in any pending petitions.<br>
	 * It logs the message and sends it as a {@code CreatureSay} packet.
	 * @param player The {@code Player} who is sending the message.
	 * @param messageText The content of the message to be sent.
	 * @return {@code true} if the message was successfully sent, or {@code false} otherwise.
	 */
	public boolean sendActivePetitionMessage(Player player, String messageText)
	{
		// if (!isPlayerInConsultation(player))
		// return false;
		final int playerObjectId = player.getObjectId();
		CreatureSay cs;
		for (Petition currPetition : _pendingPetitions.values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			final Player petitioner = currPetition.getPetitioner();
			if ((petitioner != null) && (petitioner.getObjectId() == playerObjectId))
			{
				cs = new CreatureSay(player, ChatType.PETITION_PLAYER, player.getName(), messageText);
				currPetition.addLogMessage(cs);
				
				currPetition.sendResponderPacket(cs);
				currPetition.sendPetitionerPacket(cs);
				return true;
			}
			
			final Player responder = currPetition.getResponder();
			if ((responder != null) && (responder.getObjectId() == playerObjectId))
			{
				cs = new CreatureSay(player, ChatType.PETITION_GM, player.getName(), messageText);
				currPetition.addLogMessage(cs);
				
				currPetition.sendResponderPacket(cs);
				currPetition.sendPetitionerPacket(cs);
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Sends a list of all currently pending petitions to the specified player.<br>
	 * This method generates an HTML message containing details like submission time, petitioner name, and petition type.<br>
	 * It also includes buttons for viewing or rejecting petitions if they are not currently in process.
	 * @param player The {@code Player} who will receive the petition list packet.
	 */
	public void sendPendingPetitionList(Player player)
	{
		final StringBuilder htmlContent = new StringBuilder(600 + (_pendingPetitions.size() * 300));
		htmlContent.append("<html><body><center><table width=270><tr><td width=45><button value=\"Main\" action=\"bypass admin_admin\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td width=180><center>Petition Menu</center></td><td width=45><button value=\"Back\" action=\"bypass admin_admin7\" width=45 height=21 back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><br><table width=\"270\"><tr><td><table width=\"270\"><tr><td><button value=\"Reset\" action=\"bypass -h admin_reset_petitions\" width=\"80\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td align=right><button value=\"Refresh\" action=\"bypass -h admin_view_petitions\" width=\"80\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table><br></td></tr>");
		
		if (_pendingPetitions.isEmpty())
		{
			htmlContent.append("<tr><td>There are no currently pending petitions.</td></tr>");
		}
		else
		{
			htmlContent.append("<tr><td><font color=\"LEVEL\">Current Petitions:</font><br></td></tr>");
		}
		
		boolean color = true;
		int petcount = 0;
		for (Petition currPetition : _pendingPetitions.values())
		{
			if (currPetition == null)
			{
				continue;
			}
			
			final Player petitioner = currPetition.getPetitioner();
			final String typeAsString = currPetition.getTypeAsString();
			htmlContent.append("<tr><td width=\"270\"><table width=\"270\" cellpadding=\"2\" bgcolor=" + (color ? "131210" : "444444") + "><tr><td width=\"130\">" + DATE_FORMAT.format(Instant.ofEpochMilli(currPetition.getSubmitTime()).atZone(ZoneId.systemDefault())));
			htmlContent.append("</td><td width=\"140\" align=right><font color=\"" + (petitioner.isOnline() ? "00FF00" : "999999") + "\">" + petitioner.getName() + "</font></td></tr>");
			htmlContent.append("<tr><td width=\"130\">");
			if (currPetition.getState() != PetitionState.IN_PROCESS)
			{
				final int petitionId = currPetition.getId();
				htmlContent.append("<table width=\"130\" cellpadding=\"2\"><tr><td><button value=\"View\" action=\"bypass -h admin_view_petition " + petitionId + "\" width=\"50\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td><td><button value=\"Reject\" action=\"bypass -h admin_reject_petition " + petitionId + "\" width=\"50\" height=\"21\" back=\"L2UI_ct1.button_df\" fore=\"L2UI_ct1.button_df\"></td></tr></table>");
			}
			else
			{
				final Player responder = currPetition.getResponder();
				htmlContent.append("<font color=\"" + (responder.isOnline() ? "00FF00" : "999999") + "\">" + responder.getName() + "</font>");
			}
			htmlContent.append("</td>" + typeAsString + "<td width=\"140\" align=right>" + typeAsString + "</td></tr></table></td></tr>");
			color = !color;
			petcount++;
			if (petcount > 10)
			{
				htmlContent.append("<tr><td><font color=\"LEVEL\">There is more pending petition...</font><br></td></tr>");
				break;
			}
		}
		
		htmlContent.append("</table></center></body></html>");
		
		final NpcHtmlMessage htmlMsg = new NpcHtmlMessage();
		htmlMsg.setHtml(htmlContent.toString());
		player.sendPacket(htmlMsg);
	}
	
	/**
	 * Submits a new petition from a player.<br>
	 * This method creates a {@code Petition} object and adds it to the pending list.<br>
	 * It also broadcasts a notification message to all GMs.
	 * @param petitioner The {@code Player} who is submitting the petition.
	 * @param petitionText The text content of the petition.
	 * @param petitionType The category or type of the petition.
	 * @return The unique {@code int} ID of the newly created petition.
	 */
	public int submitPetition(Player petitioner, String petitionText, int petitionType)
	{
		// Create a new petition instance and add it to the list of pending petitions.
		final Petition newPetition = new Petition(petitioner, petitionText, petitionType);
		final int newPetitionId = newPetition.getId();
		_pendingPetitions.put(newPetitionId, newPetition);
		
		// Notify all GMs that a new petition has been submitted.
		final String msgContent = petitioner.getName() + " has submitted a new petition."; // (ID: " + newPetitionId + ").";
		AdminData.getInstance().broadcastToGMs(new CreatureSay(petitioner, ChatType.HERO_VOICE, "Petition System", msgContent));
		return newPetitionId;
	}
	
	/**
	 * Displays the details of a specific petition to an administrator.<br>
	 * This method checks if the {@code Player} has GM privileges and if the {@code petitionId} is valid.<br>
	 * It then sends an {@code NpcHtmlMessage} containing the petition content and metadata.
	 * @param player The {@code Player} who will view the petition details.
	 * @param petitionId The unique identifier of the petition to display.
	 */
	public void viewPetition(Player player, int petitionId)
	{
		if (!player.isGM())
		{
			return;
		}
		
		if (!isValidPetition(petitionId))
		{
			return;
		}
		
		final Petition currPetition = _pendingPetitions.get(petitionId);
		final Player petitioner = currPetition.getPetitioner();
		final NpcHtmlMessage html = new NpcHtmlMessage();
		html.setFile(player, "data/html/admin/petition.htm");
		html.replace("%petition%", String.valueOf(currPetition.getId()));
		html.replace("%time%", DATE_FORMAT.format(Instant.ofEpochMilli(currPetition.getSubmitTime()).atZone(ZoneId.systemDefault())));
		html.replace("%type%", currPetition.getTypeAsString());
		html.replace("%petitioner%", petitioner.getName());
		html.replace("%online%", (petitioner.isOnline() ? "00FF00" : "999999"));
		html.replace("%text%", currPetition.getContent());
		player.sendPacket(html);
	}
	
	/**
	 * Retrieves the single instance of the {@code PetitionManager}.<br>
	 * This method follows the singleton pattern.
	 * @return The active {@code PetitionManager} instance.
	 */
	public static PetitionManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetitionManager INSTANCE = new PetitionManager();
	}
}
