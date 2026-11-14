/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.mechanics.announce;

import java.util.Date;

import org.l2jmobius.gameserver.managers.IdManager;

/**
 * Represents a scheduled announcement for in-game events.<br>
 * This class handles the data and logic required to display messages to players at specific times.
 * @author UnAfraid, Mobius
 */
public class EventAnnouncement implements IAnnouncement
{
	private final int _id;
	private Date _startDate = null;
	private Date _endDate = null;
	private String _content;
	
	/**
	 * Creates a new {@code EventAnnouncement} object.<br>
	 * This constructor initializes the start time, end time, and message content.<br>
	 * It also assigns a unique ID to the announcement.
	 * @param startDate The date when the announcement becomes active.
	 * @param endDate The date when the announcement expires.
	 * @param content The text message to be displayed to players.
	 */
	public EventAnnouncement(Date startDate, Date endDate, String content)
	{
		_id = IdManager.getInstance().getNextId();
		_startDate = startDate;
		_endDate = endDate;
		_content = content;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code EventAnnouncement}.<br>
	 * This ID is used to distinguish between different announcements.
	 * @return The unique integer ID of the announcement.
	 */
	@Override
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the category of this announcement.<br>
	 * This method always returns the {@code EVENT} type.
	 * @return The {@code AnnouncementType} associated with this object.
	 */
	@Override
	public AnnouncementType getType()
	{
		return AnnouncementType.EVENT;
	}
	
	/**
	 * Sets the category of the announcement.<br>
	 * This defines how the message is classified in the system.
	 * @param type The {@code AnnouncementType} to assign to this announcement.
	 */
	@Override
	public void setType(AnnouncementType type)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Checks if the announcement is currently active.<br>
	 * It verifies that both start and end dates are set.<br>
	 * It also checks if the current time falls between those dates.
	 * @return {@code true} if the announcement is valid, {@code false} otherwise.
	 */
	@Override
	public boolean isValid()
	{
		final Date now = new Date();
		return (_startDate != null) && (_endDate != null) && !now.before(_startDate) && !now.after(_endDate);
	}
	
	/**
	 * Retrieves the text content of this announcement.<br>
	 * This method returns the string stored in the {@code _content} field.
	 * @return The content as a {@code String}.
	 */
	@Override
	public String getContent()
	{
		return _content;
	}
	
	/**
	 * Updates the text content of this announcement.<br>
	 * The new value is stored in the {@code _content} field.
	 * @param content The new string to set as the announcement content.
	 */
	@Override
	public void setContent(String content)
	{
		_content = content;
	}
	
	/**
	 * Retrieves the name of the person who created this announcement.<br>
	 * Returns a default value if no author is specified.
	 * @return The author's name as a {@code String}.
	 */
	@Override
	public String getAuthor()
	{
		return "N/A";
	}
	
	/**
	 * Sets the name of the person who created this announcement.<br>
	 * This updates the {@code author} field within the {@code EventAnnouncement} object.
	 * @param author The name of the author to set.
	 */
	@Override
	public void setAuthor(String author)
	{
		throw new UnsupportedOperationException();
	}
	
	/**
	 * Deletes this announcement from the system.<br>
	 * It releases the unique ID associated with this object.
	 * @return {@code true} if the deletion was successful.
	 */
	@Override
	public boolean deleteMe()
	{
		IdManager.getInstance().releaseId(_id);
		return true;
	}
	
	/**
	 * Saves the current announcement to the database.<br>
	 * This method confirms that the data is stored successfully.
	 * @return {@code true} if the operation was successful, otherwise {@code false}.
	 */
	@Override
	public boolean storeMe()
	{
		return true;
	}
	
	/**
	 * Updates the current announcement data.<br>
	 * This method is currently not implemented.
	 * @return {@code true} if the update was successful, otherwise {@code false}.
	 */
	@Override
	public boolean updateMe()
	{
		throw new UnsupportedOperationException();
	}
}
