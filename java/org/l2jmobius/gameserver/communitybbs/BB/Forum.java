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
import java.sql.ResultSet;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.TopicConstructorType;
import org.l2jmobius.gameserver.communitybbs.Manager.ForumsBBSManager;
import org.l2jmobius.gameserver.communitybbs.Manager.TopicBBSManager;

/**
 * Represents a forum category within the community bulletin board system.<br>
 * This class manages the collection of topics associated with a specific forum.<br>
 * It provides methods to handle data retrieval and organization for the {@code ForumsBBSManager}.
 */
public class Forum
{
	private static final Logger LOGGER = Logger.getLogger(Forum.class.getName());
	
	// type
	public static final int ROOT = 0;
	public static final int NORMAL = 1;
	public static final int CLAN = 2;
	public static final int MEMO = 3;
	public static final int MAIL = 4;
	
	// perm
	public static final int INVISIBLE = 0;
	public static final int ALL = 1;
	public static final int CLANMEMBERONLY = 2;
	public static final int OWNERONLY = 3;
	
	private final Collection<Forum> _children;
	private final Map<Integer, Topic> _topic = new ConcurrentHashMap<>();
	private final int _forumId;
	private String _forumName;
	private int _forumType;
	private int _forumPost;
	private int _forumPerm;
	private final Forum _fParent;
	private int _ownerID;
	private boolean _loaded = false;
	
	/**
	 * Creates a new instance of the {@code Forum} class.<br>
	 * This constructor initializes the forum with its unique identifier and parent reference.
	 * @param forumId The unique identification number for this forum.
	 * @param fParent The {@code Forum} object that acts as the parent of this forum.
	 */
	public Forum(int forumId, Forum fParent)
	{
		_forumId = forumId;
		_fParent = fParent;
		_children = ConcurrentHashMap.newKeySet();
	}
	
	/**
	 * Creates a new {@code Forum} instance and adds it to the manager.<br>
	 * This constructor initializes all forum properties and sets up parent-child relationships.
	 * @param name The display name of the forum.
	 * @param parent The {@code Forum} object that acts as the parent.
	 * @param type The category type of the forum.
	 * @param perm The access permission level for the forum.
	 * @param ownerId The unique identifier of the forum owner.
	 */
	public Forum(String name, Forum parent, int type, int perm, int ownerId)
	{
		_forumName = name;
		_forumId = ForumsBBSManager.getInstance().getANewID();
		_forumType = type;
		_forumPost = 0;
		_forumPerm = perm;
		_fParent = parent;
		_ownerID = ownerId;
		_children = ConcurrentHashMap.newKeySet();
		parent._children.add(this);
		ForumsBBSManager.getInstance().addForum(this);
		_loaded = true;
	}
	
	/**
	 * Loads the forum data from the database.<br>
	 * This method retrieves the forum details and all associated topics.<br>
	 * It populates the internal fields and the {@code _topic} map.
	 */
	private void load()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM forums WHERE forum_id=?"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				if (rs.next())
				{
					_forumName = rs.getString("forum_name");
					_forumPost = rs.getInt("forum_post");
					_forumType = rs.getInt("forum_type");
					_forumPerm = rs.getInt("forum_perm");
					_ownerID = rs.getInt("forum_owner_id");
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Data error on Forum " + _forumId + " : " + e.getMessage(), e);
		}
		
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM topic WHERE topic_forum_id=? ORDER BY topic_id DESC"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Topic t = new Topic(TopicConstructorType.RESTORE, rs.getInt("topic_id"), rs.getInt("topic_forum_id"), rs.getString("topic_name"), rs.getLong("topic_date"), rs.getString("topic_ownername"), rs.getInt("topic_ownerid"), rs.getInt("topic_type"), rs.getInt("topic_reply"));
					final Integer topicId = t.getID();
					_topic.put(topicId, t);
					if (topicId > TopicBBSManager.getInstance().getMaxID(this))
					{
						TopicBBSManager.getInstance().setMaxID(topicId, this);
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Data error on Forum " + _forumId + " : " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves all child forums from the database.<br>
	 * It populates the {@code _children} collection with new {@code Forum} objects.<br>
	 * Each found forum is registered with the {@code ForumsBBSManager}.
	 */
	private void getChildren()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT forum_id FROM forums WHERE forum_parent=?"))
		{
			ps.setInt(1, _forumId);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final Forum f = new Forum(rs.getInt("forum_id"), this);
					_children.add(f);
					ForumsBBSManager.getInstance().addForum(f);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Data error on Forum (children): " + e.getMessage(), e);
		}
	}
	
	/**
	 * Retrieves the total number of topics in this forum.<br>
	 * This method calls {@code vload()} to refresh the data before counting.
	 * @return The size of the topic collection as an {@code int}.
	 */
	public int getTopicSize()
	{
		vload();
		return _topic.size();
	}
	
	/**
	 * Retrieves a {@code Topic} based on the provided ID.<br>
	 * This method reloads data from the database before searching.
	 * @param j The unique identifier of the topic to retrieve.
	 * @return The {@code Topic} object associated with the ID, or {@code null} if not found.
	 */
	public Topic getTopic(int j)
	{
		vload();
		return _topic.get(j);
	}
	
	/**
	 * Adds a new {@code Topic} to this forum.<br>
	 * This method calls {@code vload()} before saving the topic.<br>
	 * The topic is stored in the internal map using its unique ID.
	 * @param t The {@code Topic} object to be added.
	 */
	public void addTopic(Topic t)
	{
		vload();
		_topic.put(t.getID(), t);
	}
	
	/**
	 * Retrieves the unique identifier for this forum.
	 * @return The {@code int} value of the forum ID.
	 */
	public int getID()
	{
		return _forumId;
	}
	
	/**
	 * Retrieves the name of the forum.<br>
	 * This method calls {@code vload()} before returning the value.
	 * @return The name of the forum as a {@code String}.
	 */
	public String getName()
	{
		vload();
		return _forumName;
	}
	
	/**
	 * Retrieves the type of the forum.<br>
	 * This method calls {@code vload()} before returning the value.
	 * @return The integer value representing the forum type.
	 */
	public int getType()
	{
		vload();
		return _forumType;
	}
	
	/**
	 * Searches for a child {@code Forum} by its name.<br>
	 * This method returns the first matching child found in the collection.<br>
	 * It returns {@code null} if no match is found.
	 * @param name The name of the forum to search for.
	 * @return The matching {@code Forum} object or {@code null}.
	 */
	public Forum getChildByName(String name)
	{
		vload();
		for (Forum forum : _children)
		{
			if (forum.getName().equals(name))
			{
				return forum;
			}
		}
		
		return null;
	}
	
	/**
	 * Removes a topic from the internal collection.<br>
	 * This method uses the provided {@code id} to locate and delete the entry.
	 * @param id The unique identifier of the topic to remove.
	 */
	public void rmTopicByID(int id)
	{
		_topic.remove(id);
	}
	
	/**
	 * Saves the current {@code Forum} data into the database.<br>
	 * This method executes an {@code INSERT} query for the forums table.<br>
	 * It handles any exceptions by logging a warning message.
	 */
	public void insertIntoDb()
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO forums (forum_id,forum_name,forum_parent,forum_post,forum_type,forum_perm,forum_owner_id) VALUES (?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, _forumId);
			ps.setString(2, _forumName);
			ps.setInt(3, _fParent.getID());
			ps.setInt(4, _forumPost);
			ps.setInt(5, _forumType);
			ps.setInt(6, _forumPerm);
			ps.setInt(7, _ownerID);
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while saving new Forum to db " + e.getMessage(), e);
		}
	}
	
	/**
	 * Verifies if the forum data is loaded into memory.<br>
	 * It calls {@code load()} and {@code getChildren()} if the {@code _loaded} flag is {@code false}.<br>
	 * This ensures that all necessary data is available for use.
	 */
	public void vload()
	{
		if (!_loaded)
		{
			load();
			getChildren();
			_loaded = true;
		}
	}
}
