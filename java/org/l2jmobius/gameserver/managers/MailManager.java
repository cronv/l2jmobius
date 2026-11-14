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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.MailType;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.ExNoticePostArrived;
import org.l2jmobius.gameserver.network.serverpackets.ExUnReadMailCount;
import org.l2jmobius.gameserver.taskmanagers.MessageDeletionTaskManager;

/**
 * Manages the in-game mail system for players.<br>
 * This class handles sending, receiving, and tracking {@code MailType} messages.<br>
 * It coordinates interactions between the database and the {@link org.l2jmobius.gameserver.entity.actor.Player} objects.
 * @author Migi, DS
 */
public class MailManager
{
	private static final Logger LOGGER = Logger.getLogger(MailManager.class.getName());
	
	private final Map<Integer, MailMessage> _messages = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code MailManager}.<br>
	 * This constructor calls the {@code load()} method to prepare data.
	 */
	protected MailManager()
	{
		load();
	}
	
	/**
	 * Loads all mail messages from the database into memory.<br>
	 * It populates the internal message map and registers them with the task manager.<br>
	 * Any errors during the database query are logged as warnings.
	 */
	private void load()
	{
		int count = 0;
		try (Connection con = DatabaseFactory.getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT * FROM messages ORDER BY expiration"))
		{
			while (rs.next())
			{
				count++;
				final MailMessage msg = new MailMessage(rs);
				final int msgId = msg.getId();
				_messages.put(msgId, msg);
				MessageDeletionTaskManager.getInstance().add(msgId, msg.getExpiration());
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error loading from database:", e);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + count + " messages.");
	}
	
	/**
	 * Retrieves a specific mail message from the internal cache.<br>
	 * It uses the unique identifier provided in the parameters.
	 * @param msgId The unique ID of the message to retrieve.
	 * @return The {@code MailMessage} object associated with the ID, or {@code null} if not found.
	 */
	public MailMessage getMessage(int msgId)
	{
		return _messages.get(msgId);
	}
	
	/**
	 * Retrieves all mail messages currently stored in the manager.<br>
	 * This method returns a collection of {@code MailMessage} objects.
	 * @return A {@code Collection} containing all {@code MailMessage} instances.
	 */
	public Collection<MailMessage> getMessages()
	{
		return _messages.values();
	}
	
	/**
	 * Checks if a specific player has any unread mail messages.<br>
	 * It searches through all current messages in the system.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if at least one unread message exists, otherwise {@code false}.
	 */
	public boolean hasUnreadPost(Player player)
	{
		final int objectId = player.getObjectId();
		for (MailMessage msg : _messages.values())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && msg.isUnread())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Calculates the total number of messages in the inbox for a specific user.<br>
	 * It counts all {@code MailMessage} objects where the receiver ID matches the provided ID.<br>
	 * Messages marked as deleted by the receiver are not included in this count.
	 * @param objectId The unique identifier of the player whose inbox size is being checked.
	 * @return The total number of active messages for the given {@code objectId}.
	 */
	public int getInboxSize(int objectId)
	{
		int size = 0;
		for (MailMessage msg : _messages.values())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && !msg.isDeletedByReceiver())
			{
				size++;
			}
		}
		
		return size;
	}
	
	/**
	 * Calculates the number of messages in the outbox for a specific user.<br>
	 * It counts all {@code MailMessage} objects where the sender ID matches the provided ID.<br>
	 * Messages marked as deleted by the sender are excluded from this count.
	 * @param objectId The unique identifier of the user whose outbox size is being checked.
	 * @return The total number of active messages in the outbox for the given {@code objectId}.
	 */
	public int getOutboxSize(int objectId)
	{
		int size = 0;
		for (MailMessage msg : _messages.values())
		{
			if ((msg != null) && (msg.getSenderId() == objectId) && !msg.isDeletedBySender())
			{
				size++;
			}
		}
		
		return size;
	}
	
	/**
	 * Retrieves all active mail messages for a specific user.<br>
	 * It filters out any messages that have been deleted by the receiver.
	 * @param objectId The unique identifier of the player receiving the mail.
	 * @return A {@code List} containing all valid {@code MailMessage} objects for the given ID.
	 */
	public List<MailMessage> getInbox(int objectId)
	{
		final List<MailMessage> inbox = new ArrayList<>();
		for (MailMessage msg : _messages.values())
		{
			if ((msg != null) && (msg.getReceiverId() == objectId) && !msg.isDeletedByReceiver())
			{
				inbox.add(msg);
			}
		}
		
		return inbox;
	}
	
	/**
	 * Calculates the number of unread messages for a specific player.<br>
	 * It checks all messages in the player's inbox.
	 * @param player The {@code Player} object to check.
	 * @return The total count of unread messages as a {@code long}.
	 */
	public long getUnreadCount(Player player)
	{
		long count = 0;
		for (MailMessage message : getInbox(player.getObjectId()))
		{
			if (message.isUnread())
			{
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Calculates the number of regular mails currently being processed.<br>
	 * This includes messages with attachments that are not yet deleted or returned.<br>
	 * It checks both incoming and outgoing mail for the specified object.
	 * @param objectId The unique identifier of the player or entity.
	 * @return The total count of mails in progress.
	 */
	public int getMailsInProgress(int objectId)
	{
		int count = 0;
		for (MailMessage msg : _messages.values())
		{
			if ((msg != null) && (msg.getMailType() == MailType.REGULAR))
			{
				if ((msg.getReceiverId() == objectId) && !msg.isDeletedByReceiver() && !msg.isReturned() && msg.hasAttachments())
				{
					count++;
				}
				else if ((msg.getSenderId() == objectId) && !msg.isDeletedBySender() && !msg.isReturned() && msg.hasAttachments())
				{
					count++;
				}
			}
		}
		
		return count;
	}
	
	/**
	 * Retrieves all outgoing messages for a specific user.<br>
	 * This method filters the internal message map to find items sent by the provided {@code objectId}.<br>
	 * It only includes messages that have not been deleted by the sender.
	 * @param objectId The unique identifier of the user whose outbox is being retrieved.
	 * @return A {@code List} of {@code MailMessage} objects belonging to the specified user.
	 */
	public List<MailMessage> getOutbox(int objectId)
	{
		final List<MailMessage> outbox = new ArrayList<>();
		for (MailMessage msg : _messages.values())
		{
			if ((msg != null) && (msg.getSenderId() == objectId) && !msg.isDeletedBySender())
			{
				outbox.add(msg);
			}
		}
		
		return outbox;
	}
	
	/**
	 * Sends a new mail message to the recipient.<br>
	 * This method saves the message to the database and notifies the player.<br>
	 * It also schedules the message for deletion based on its expiration time.
	 * @param msg The {@code MailMessage} object containing the message details.
	 */
	public void sendMessage(MailMessage msg)
	{
		final int msgId = msg.getId();
		_messages.put(msgId, msg);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = MailMessage.getStatement(msg, con))
		{
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error saving message:", e);
		}
		
		final Player receiver = World.getPlayer(msg.getReceiverId());
		if (receiver != null)
		{
			receiver.sendPacket(ExNoticePostArrived.valueOf(true));
			receiver.sendPacket(new ExUnReadMailCount(receiver));
		}
		
		MessageDeletionTaskManager.getInstance().add(msgId, msg.getExpiration());
	}
	
	/**
	 * Updates the database to mark a specific message as read.<br>
	 * This method sets the {@code isUnread} status to {@code false}.<br>
	 * It handles any {@code SQLException} by logging a warning.
	 * @param msgId The unique identifier of the message to update.
	 */
	public void markAsReadInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE messages SET isUnread = 'false' WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error marking as read message:", e);
		}
	}
	
	/**
	 * Updates the database to mark a specific message as deleted by its sender.<br>
	 * This method sets the {@code isDeletedBySender} flag to {@code true}.<br>
	 * It uses the provided {@code msgId} to identify the correct record.
	 * @param msgId The unique identifier of the message to update.
	 */
	public void markAsDeletedBySenderInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE messages SET isDeletedBySender = 'true' WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error marking as deleted by sender message:", e);
		}
	}
	
	/**
	 * Updates the database to mark a specific message as deleted by the receiver.<br>
	 * This method sets the {@code isDeletedByReceiver} flag to {@code true}.
	 * @param msgId The unique identifier of the message to update.
	 */
	public void markAsDeletedByReceiverInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE messages SET isDeletedByReceiver = 'true' WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error marking as deleted by receiver message:", e);
		}
	}
	
	/**
	 * This method removes the attachment status from a specific message in the database.<br>
	 * It updates the {@code hasAttachments} column to {@code false}.<br>
	 * If an error occurs, it logs a warning to the logger.
	 * @param msgId The unique identifier of the message to update.
	 */
	public void removeAttachmentsInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE messages SET hasAttachments = 'false' WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error removing attachments in message:", e);
		}
	}
	
	/**
	 * Removes a specific message from the database.<br>
	 * This method also clears the message from the internal cache.<br>
	 * It releases the associated ID back to the {@code IdManager}.
	 * @param msgId The unique identifier of the message to delete.
	 */
	public void deleteMessageInDb(int msgId)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM messages WHERE messageId = ?"))
		{
			ps.setInt(1, msgId);
			ps.execute();
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Error deleting message:", e);
		}
		
		_messages.remove(msgId);
		IdManager.getInstance().releaseId(msgId);
	}
	
	/**
	 * Provides access to the singleton instance of {@code MailManager}.<br>
	 * Use this method to get the global manager for handling mail operations.
	 * @return The single instance of {@code MailManager}.
	 */
	public static MailManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MailManager INSTANCE = new MailManager();
	}
}
