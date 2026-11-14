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
package org.l2jmobius.gameserver.communitybbs.BB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.TopicConstructorType;
import org.l2jmobius.gameserver.communitybbs.Manager.TopicBBSManager;

/**
 * Represents a single discussion topic within the community bulletin board.<br>
 * This class handles the data and logic for individual forum threads.
 */
public class Topic
{
	private static final Logger LOGGER = Logger.getLogger(Topic.class.getName());
	
	public static final int NORMAL = 0;
	public static final int MEMO = 1;
	
	private final int _id;
	private final int _forumId;
	private final String _topicName;
	private final long _date;
	private final String _ownerName;
	private final int _ownerId;
	private final int _type;
	private final int _cReply;
	
	/**
	 * Creates a new {@code Topic} instance and registers it with the manager.<br>
	 * If the constructor type is set to {@code CREATE}, the topic is saved to the database.
	 * @param ct The {@code TopicConstructorType} used to determine if the topic should be saved.
	 * @param id The unique identifier for the topic.
	 * @param fid The identifier of the forum where this topic belongs.
	 * @param name The display name of the topic.
	 * @param date The timestamp of when the topic was created.
	 * @param oname The name of the user who owns the topic.
	 * @param oid The unique identifier of the owner.
	 * @param type The category or classification type of the topic.
	 * @param cReply The current count of replies for this topic.
	 */
	public Topic(TopicConstructorType ct, int id, int fid, String name, long date, String oname, int oid, int type, int cReply)
	{
		_id = id;
		_forumId = fid;
		_topicName = name;
		_date = date;
		_ownerName = oname;
		_ownerId = oid;
		_type = type;
		_cReply = cReply;
		TopicBBSManager.getInstance().addTopic(this);
		
		if (ct == TopicConstructorType.CREATE)
		{
			insertindb();
		}
	}
	
	/**
	 * Saves the current {@code Topic} data into the database.<br>
	 * This method uses a {@code PreparedStatement} to execute an {@code INSERT} query.<br>
	 * It logs a warning if any error occurs during the database operation.
	 */
	private void insertindb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO topic (topic_id,topic_forum_id,topic_name,topic_date,topic_ownername,topic_ownerid,topic_type,topic_reply) values (?,?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, _id);
			ps.setInt(2, _forumId);
			ps.setString(3, _topicName);
			ps.setLong(4, _date);
			ps.setString(5, _ownerName);
			ps.setInt(6, _ownerId);
			ps.setInt(7, _type);
			ps.setInt(8, _cReply);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while saving new Topic to db " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the unique identifier of this {@code Topic}.
	 * @return The {@code int} value of the ID.
	 */
	public int getID()
	{
		return _id;
	}
	
	/**
	 * Retrieves the unique identifier for the forum. <br>
	 * This value links the {@code Topic} to its parent forum.
	 * @return The integer ID of the forum.
	 */
	public int getForumID()
	{
		return _forumId;
	}
	
	/**
	 * Retrieves the name of the topic.<br>
	 * This method returns the {@code _topicName} field.
	 * @return The name of the topic as a {@code String}.
	 */
	public String getName()
	{
		return _topicName;
	}
	
	/**
	 * Retrieves the name of the owner for this {@code Topic}.
	 * @return The name of the owner as a {@code String}.
	 */
	public String getOwnerName()
	{
		return _ownerName;
	}
	
	/**
	 * Retrieves the object id of the owner for this {@code Topic}.
	 * @return The object id of the owner as an {@code int}.
	 */
	public int getOwnerId()
	{
		return _ownerId;
	}
	
	/**
	 * Deletes the current {@code Topic} from the database.<br>
	 * This method removes the topic from the manager and the associated forum.<br>
	 * It also executes a SQL query to delete the record from the table.
	 * @param f The {@code Forum} object where the topic is located.
	 */
	public void deleteme(Forum f)
	{
		TopicBBSManager.getInstance().delTopic(this);
		f.rmTopicByID(_id);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM topic WHERE topic_id=? AND topic_forum_id=?"))
		{
			ps.setInt(1, _id);
			ps.setInt(2, f.getID());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while deleting topic: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the date associated with this {@code Topic}.<br>
	 * The value is returned as a {@code long}.
	 * @return The date of the topic.
	 */
	public long getDate()
	{
		return _date;
	}
}
