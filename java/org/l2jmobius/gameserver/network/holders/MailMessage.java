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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.actor.enums.creature.AttributeType;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Mail;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.enums.MailType;

/**
 * This class represents a data holder for mail messages sent to players.<br>
 * It contains the necessary information to process and display mail content in the game client.
 * @author Migi, DS
 */
public class MailMessage
{
	private static final int EXPIRATION = 360; // 15 days
	private static final int COD_EXPIRATION = 12; // 12 hours
	
	// post state
	public static final int DELETED = 0;
	public static final int READED = 1;
	public static final int REJECTED = 2;
	
	private final int _messageId;
	private final int _senderId;
	private final int _receiverId;
	private final long _expiration;
	private String _senderName = null;
	private String _receiverName = null;
	private final String _subject;
	private final String _content;
	private boolean _unread;
	private boolean _returned;
	private MailType _messageType = MailType.REGULAR;
	private boolean _deletedBySender;
	private boolean _deletedByReceiver;
	private final long _reqAdena;
	private boolean _hasAttachments;
	private Mail _attachments = null;
	
	private int _itemId;
	private int _enchantLvl;
	private final int[] _elementals = new int[6];
	
	/*
	 * Constructor for restoring from DB.
	 */
	/**
	 * Creates a new {@code MailMessage} instance from a database result set.<br>
	 * This constructor populates all fields using the data provided in the {@code ResultSet}.
	 * @param rset The {@code ResultSet} containing the mail data.
	 * @throws SQLException If an error occurs while accessing the database results.
	 */
	public MailMessage(ResultSet rset) throws SQLException
	{
		_messageId = rset.getInt("messageId");
		_senderId = rset.getInt("senderId");
		_receiverId = rset.getInt("receiverId");
		_subject = rset.getString("subject");
		_content = rset.getString("content");
		_expiration = rset.getLong("expiration");
		_reqAdena = rset.getLong("reqAdena");
		_hasAttachments = Boolean.parseBoolean(rset.getString("hasAttachments"));
		_unread = Boolean.parseBoolean(rset.getString("isUnread"));
		_deletedBySender = Boolean.parseBoolean(rset.getString("isDeletedBySender"));
		_deletedByReceiver = Boolean.parseBoolean(rset.getString("isDeletedByReceiver"));
		_messageType = MailType.values()[rset.getInt("sendBySystem")];
		_returned = Boolean.parseBoolean(rset.getString("isReturned"));
		_itemId = rset.getInt("itemId");
		_enchantLvl = rset.getInt("enchantLvl");
		final String elemental = rset.getString("elementals");
		if (elemental != null)
		{
			final String[] elemDef = elemental.split(";");
			for (int i = 0; i < 6; i++)
			{
				_elementals[i] = Integer.parseInt(elemDef[i]);
			}
		}
	}
	
	/*
	 * This constructor used for creating new message.
	 */
	/**
	 * Creates a new {@code MailMessage} instance with the specified details.<br>
	 * This constructor initializes the message and calculates its expiration time based on whether it is a COD mail.
	 * @param senderId The unique identifier of the player sending the mail.
	 * @param receiverId The unique identifier of the player receiving the mail.
	 * @param isCod A boolean flag indicating if the mail is a Cash On Delivery message.
	 * @param subject The title or heading of the mail content.
	 * @param text The main body text of the mail.
	 * @param reqAdena The amount of {@code Adena} required for this transaction.
	 */
	public MailMessage(int senderId, int receiverId, boolean isCod, String subject, String text, long reqAdena)
	{
		_messageId = IdManager.getInstance().getNextId();
		_senderId = senderId;
		_receiverId = receiverId;
		_subject = subject;
		_content = text;
		_expiration = (isCod ? System.currentTimeMillis() + (COD_EXPIRATION * 3600000) : System.currentTimeMillis() + (EXPIRATION * 3600000));
		_hasAttachments = false;
		_unread = true;
		_deletedBySender = false;
		_deletedByReceiver = false;
		_reqAdena = reqAdena;
		_messageType = MailType.REGULAR;
	}
	
	/*
	 * This constructor used for System Mails
	 */
	/**
	 * Creates a new {@code MailMessage} sent by the system.<br>
	 * This constructor initializes a message with a default sender ID of -1.<br>
	 * The expiration is set to 15 days from the current time.
	 * @param receiverId The unique identifier for the player receiving the mail.
	 * @param subject The title or heading of the mail content.
	 * @param content The main body text of the mail message.
	 * @param sendBySystem The {@code MailType} used to categorize this system mail.
	 */
	public MailMessage(int receiverId, String subject, String content, MailType sendBySystem)
	{
		_messageId = IdManager.getInstance().getNextId();
		_senderId = -1;
		_receiverId = receiverId;
		_subject = subject;
		_content = content;
		_expiration = System.currentTimeMillis() + (EXPIRATION * 3600000);
		_reqAdena = 0;
		_hasAttachments = false;
		_unread = true;
		_deletedBySender = true;
		_deletedByReceiver = false;
		_messageType = sendBySystem;
		_returned = false;
	}
	
	/*
	 * This constructor is used for creating new System message
	 */
	/**
	 * Creates a new {@code MailMessage} instance with the provided details.<br>
	 * This constructor initializes a standard mail message with an expiration time of 15 days.<br>
	 * It sets the initial status to unread and marks it as deleted by the sender.
	 * @param senderId The unique identifier of the player sending the mail.
	 * @param receiverId The unique identifier of the player receiving the mail.
	 * @param subject The title or heading of the mail message.
	 * @param content The main body text of the mail message.
	 * @param sendBySystem The {@code MailType} used to categorize how the mail was sent.
	 */
	public MailMessage(int senderId, int receiverId, String subject, String content, MailType sendBySystem)
	{
		_messageId = IdManager.getInstance().getNextId();
		_senderId = senderId;
		_receiverId = receiverId;
		_subject = subject;
		_content = content;
		_expiration = System.currentTimeMillis() + (EXPIRATION * 3600000);
		_hasAttachments = false;
		_unread = true;
		_deletedBySender = true;
		_deletedByReceiver = false;
		_reqAdena = 0;
		_messageType = sendBySystem;
	}
	
	/*
	 * This constructor used for auto-generation of the "return attachments" message
	 */
	/**
	 * Creates a new {@code MailMessage} based on an existing one.<br>
	 * This constructor initializes a new message with specific properties from the provided object.<br>
	 * It generates a new unique ID and sets the expiration time.<br>
	 * The attachments are copied to the new message while being removed from the original.
	 * @param msg The source {@code MailMessage} to copy data from.
	 */
	public MailMessage(MailMessage msg)
	{
		_messageId = IdManager.getInstance().getNextId();
		_senderId = msg.getSenderId();
		_receiverId = msg.getSenderId();
		_subject = "";
		_content = "";
		_expiration = System.currentTimeMillis() + (EXPIRATION * 3600000);
		_unread = true;
		_deletedBySender = true;
		_deletedByReceiver = false;
		_messageType = MailType.REGULAR;
		_returned = true;
		_reqAdena = 0;
		_hasAttachments = true;
		_attachments = msg.getAttachments();
		msg.removeAttachments();
		_attachments.setNewMessageId(_messageId);
	}
	
	/**
	 * Creates a new {@code MailMessage} for a specific receiver.<br>
	 * This constructor initializes the mail with an item and sets its expiration time.<br>
	 * It handles different logic based on the provided {@code MailType}.
	 * @param receiverId The unique identifier of the player receiving the mail.
	 * @param item The {@code Item} object to be included in the mail content.
	 * @param mailType The category of the mail being sent.
	 */
	public MailMessage(int receiverId, Item item, MailType mailType)
	{
		_messageId = IdManager.getInstance().getNextId();
		_senderId = -1;
		_receiverId = receiverId;
		_subject = "";
		_content = item.getName();
		_expiration = System.currentTimeMillis() + (EXPIRATION * 3600000);
		_unread = true;
		_deletedBySender = true;
		_messageType = mailType;
		_returned = false;
		_reqAdena = 0;
		if (mailType == MailType.COMMISSION_ITEM_SOLD)
		{
			_hasAttachments = false;
			_itemId = item.getId();
			_enchantLvl = item.getEnchantLevel();
			if (item.isArmor())
			{
				for (AttributeType type : AttributeType.ATTRIBUTE_TYPES)
				{
					_elementals[type.getClientId()] = item.getDefenceAttribute(type);
				}
			}
			else if (item.isWeapon() && (item.getAttackAttributeType() != AttributeType.NONE))
			{
				_elementals[item.getAttackAttributeType().getClientId()] = item.getAttackAttributePower();
			}
		}
		else if (mailType == MailType.COMMISSION_ITEM_RETURNED)
		{
			final Mail attachement = createAttachments();
			attachement.addItem(ItemProcessType.REFUND, item, null, null);
		}
	}
	
	/**
	 * Creates a {@code PreparedStatement} to insert a {@code MailMessage} into the database.<br>
	 * This method populates all necessary fields from the provided message object.
	 * @param msg The {@code MailMessage} object containing the data to save.
	 * @param con The active {@code Connection} used to create the statement.
	 * @return A prepared {@code PreparedStatement} ready for execution.
	 * @throws SQLException If a database access error occurs.
	 */
	public static PreparedStatement getStatement(MailMessage msg, Connection con) throws SQLException
	{
		final PreparedStatement stmt = con.prepareStatement("INSERT INTO messages (messageId, senderId, receiverId, subject, content, expiration, reqAdena, hasAttachments, isUnread, isDeletedBySender, isDeletedByReceiver, sendBySystem, isReturned, itemId, enchantLvl, elementals) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
		stmt.setInt(1, msg._messageId);
		stmt.setInt(2, msg._senderId);
		stmt.setInt(3, msg._receiverId);
		stmt.setString(4, msg._subject);
		stmt.setString(5, msg._content);
		stmt.setLong(6, msg._expiration);
		stmt.setLong(7, msg._reqAdena);
		stmt.setString(8, String.valueOf(msg._hasAttachments));
		stmt.setString(9, String.valueOf(msg._unread));
		stmt.setString(10, String.valueOf(msg._deletedBySender));
		stmt.setString(11, String.valueOf(msg._deletedByReceiver));
		stmt.setInt(12, msg._messageType.ordinal());
		stmt.setString(13, String.valueOf(msg._returned));
		stmt.setInt(14, msg._itemId);
		stmt.setInt(15, msg._enchantLvl);
		stmt.setString(16, msg._elementals[0] + ";" + msg._elementals[1] + ";" + msg._elementals[2] + ";" + msg._elementals[3] + ";" + msg._elementals[4] + ";" + msg._elementals[5]);
		return stmt;
	}
	
	/**
	 * Retrieves the unique identifier of the mail message.<br>
	 * This value corresponds to the {@code _messageId} field.
	 * @return The unique ID of the mail message.
	 */
	public int getId()
	{
		return _messageId;
	}
	
	/**
	 * Retrieves the unique identifier of the sender.<br>
	 * This value corresponds to the {@code _senderId} field.
	 * @return The {@code int} ID of the sender.
	 */
	public int getSenderId()
	{
		return _senderId;
	}
	
	/**
	 * Retrieves the unique identifier of the message receiver.
	 * @return the {@code int} ID of the receiver.
	 */
	public int getReceiverId()
	{
		return _receiverId;
	}
	
	/**
	 * Retrieves the name of the person who sent the mail.<br>
	 * If the message is a regular type, it fetches the name from {@code PlayerInfoTable}.<br>
	 * For other types, it returns the literal value {@code System}.
	 * @return The name of the sender as a {@code String}.
	 */
	public String getSenderName()
	{
		switch (_messageType)
		{
			case REGULAR:
			{
				_senderName = PlayerInfoTable.getInstance().getNameById(_senderId);
				break;
			}
			default:
			{
				_senderName = "System";
				break;
			}
		}
		
		return _senderName;
	}
	
	/**
	 * Retrieves the name of the mail receiver.<br>
	 * If the name is not stored, it fetches it from {@code PlayerInfoTable}.<br>
	 * Returns an empty string if no name is found.
	 * @return The name of the receiver as a {@code String}.
	 */
	public String getReceiverName()
	{
		if (_receiverName == null)
		{
			_receiverName = PlayerInfoTable.getInstance().getNameById(_receiverId);
			if (_receiverName == null)
			{
				_receiverName = "";
			}
		}
		
		return _receiverName;
	}
	
	/**
	 * Retrieves the subject of the mail message.
	 * @return The {@code String} representing the mail subject.
	 */
	public String getSubject()
	{
		return _subject;
	}
	
	/**
	 * Retrieves the main text body of the mail message.
	 * @return The content string of the {@code MailMessage}.
	 */
	public String getContent()
	{
		return _content;
	}
	
	/**
	 * Checks if the mail message is locked.<br>
	 * A message is considered locked if it requires a positive amount of {@code reqAdena}.
	 * @return {@code true} if the message is locked, {@code false} otherwise.
	 */
	public boolean isLocked()
	{
		return _reqAdena > 0;
	}
	
	/**
	 * Retrieves the expiration time of the mail message.<br>
	 * The value represents a timestamp in milliseconds.
	 * @return the expiration time as a {@code long}.
	 */
	public long getExpiration()
	{
		return _expiration;
	}
	
	/**
	 * Gets the expiration time in seconds.<br>
	 * This value is calculated by dividing the internal {@code _expiration} timestamp by {@code 1000}.
	 * @return The number of seconds until the message expires.
	 */
	public int getExpirationSeconds()
	{
		return (int) (_expiration / 1000);
	}
	
	/**
	 * Checks if the mail message has been read.
	 * @return {@code true} if the message is unread, {@code false} otherwise.
	 */
	public boolean isUnread()
	{
		return _unread;
	}
	
	/**
	 * Updates the status of this message to read.<br>
	 * Sets the {@code _unread} flag to {@code false}.<br>
	 * Updates the database via {@code markAsReadInDb}.
	 */
	public void markAsRead()
	{
		if (_unread)
		{
			_unread = false;
			MailManager.getInstance().markAsReadInDb(_messageId);
		}
	}
	
	/**
	 * Checks if the sender has deleted this message.
	 * @return {@code true} if the sender deleted it, {@code false} otherwise.
	 */
	public boolean isDeletedBySender()
	{
		return _deletedBySender;
	}
	
	/**
	 * Marks the message as deleted by the sender.<br>
	 * This method updates the {@code _deletedBySender} flag to {@code true}.<br>
	 * It also synchronizes this change with the database via {@code MailManager}.
	 */
	public void setDeletedBySender()
	{
		if (!_deletedBySender)
		{
			_deletedBySender = true;
			if (_deletedByReceiver)
			{
				MailManager.getInstance().deleteMessageInDb(_messageId);
			}
			else
			{
				MailManager.getInstance().markAsDeletedBySenderInDb(_messageId);
			}
		}
	}
	
	/**
	 * Checks if the receiver has deleted this mail.
	 * @return {@code true} if the message was deleted by the receiver, {@code false} otherwise.
	 */
	public boolean isDeletedByReceiver()
	{
		return _deletedByReceiver;
	}
	
	/**
	 * Marks the mail as deleted by the receiver.<br>
	 * This method updates the {@code _deletedByReceiver} flag to {@code true}.<br>
	 * It also synchronizes this change with the database via {@code MailManager}.
	 */
	public void setDeletedByReceiver()
	{
		if (!_deletedByReceiver)
		{
			_deletedByReceiver = true;
			if (_deletedBySender)
			{
				MailManager.getInstance().deleteMessageInDb(_messageId);
			}
			else
			{
				MailManager.getInstance().markAsDeletedByReceiverInDb(_messageId);
			}
		}
	}
	
	/**
	 * Retrieves the type of the mail message.<br>
	 * This identifies whether the mail is a regular message or another specific category.
	 * @return the {@code MailType} of this message.
	 */
	public MailType getMailType()
	{
		return _messageType;
	}
	
	/**
	 * Checks if the mail message has been returned.
	 * @return {@code true} if the message was returned, {@code false} otherwise.
	 */
	public boolean isReturned()
	{
		return _returned;
	}
	
	/**
	 * Sets whether the mail message has been returned.<br>
	 * This updates the {@code _returned} field of the {@code MailMessage}.
	 * @param value The status to set for the return flag.
	 */
	public void setReturned(boolean value)
	{
		_returned = value;
	}
	
	/**
	 * Retrieves the amount of {@code adena} required for this message.
	 * @return The required {@code adena} as a {@code long}.
	 */
	public long getReqAdena()
	{
		return _reqAdena;
	}
	
	/**
	 * Retrieves the {@code Mail} object associated with this message.<br>
	 * If no attachments exist, it returns {@code null}.<br>
	 * This method will initialize and restore the attachment if it is not already loaded.
	 * @return The {@code Mail} object containing the attachments, or {@code null} if none exist.
	 */
	public synchronized Mail getAttachments()
	{
		if (!_hasAttachments)
		{
			return null;
		}
		
		if (_attachments == null)
		{
			_attachments = new Mail(_senderId, _messageId);
			_attachments.restore();
		}
		
		return _attachments;
	}
	
	/**
	 * Checks if the mail message contains any attachments.
	 * @return {@code true} if there are attachments, {@code false} otherwise.
	 */
	public boolean hasAttachments()
	{
		return _hasAttachments;
	}
	
	/**
	 * Retrieves the unique identifier of the item associated with this message.
	 * @return the {@code int} value of the item ID.
	 */
	public int getItemId()
	{
		return _itemId;
	}
	
	/**
	 * Retrieves the enchantment level of the item.
	 * @return the current {@code int} value of the enchantment level.
	 */
	public int getEnchantLvl()
	{
		return _enchantLvl;
	}
	
	/**
	 * Retrieves the array of elemental values for this message.
	 * @return an {@code int[]} containing the elemental data.
	 */
	public int[] getElementals()
	{
		return _elementals;
	}
	
	/**
	 * Removes all attachments from this mail message.<br>
	 * This method clears the local {@code _attachments} field and updates the database.<br>
	 * It sets the {@code _hasAttachments} flag to {@code false}.
	 */
	public synchronized void removeAttachments()
	{
		if (_attachments != null)
		{
			_attachments = null;
			_hasAttachments = false;
			MailManager.getInstance().removeAttachmentsInDb(_messageId);
		}
	}
	
	/**
	 * Creates a new {@code Mail} object for the attachments of this message.<br>
	 * This method checks if any attachments already exist before creating one.<br>
	 * It returns {@code null} if an attachment is already present.
	 * @return A new {@code Mail} instance or {@code null} if it already exists.
	 */
	public synchronized Mail createAttachments()
	{
		if (_hasAttachments || (_attachments != null))
		{
			return null;
		}
		
		_attachments = new Mail(_senderId, _messageId);
		_hasAttachments = true;
		return _attachments;
	}
	
	/**
	 * Removes the attachments from this message.<br>
	 * It deletes the {@code Mail} object and removes it from the database.<br>
	 * The internal {@code _attachments} field is set to {@code null}.
	 */
	protected synchronized void unloadAttachments()
	{
		if (_attachments != null)
		{
			_attachments.deleteMe();
			MailManager.getInstance().removeAttachmentsInDb(_messageId);
			_attachments = null;
		}
	}
}
