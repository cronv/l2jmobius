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
package org.l2jmobius.gameserver.communitybbs.BB;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.gameserver.communitybbs.Manager.PostBBSManager;

/**
 * Represents a single post within the community bulletin board system.<br>
 * This class holds all data related to a user's submission, such as content and metadata.
 * @author Maktakien
 */
public class Post
{
	private static final Logger LOGGER = Logger.getLogger(Post.class.getName());
	
	public static class CPost
	{
		private int _postId;
		private String _postOwner;
		private int _postOwnerId;
		private long _postDate;
		private int _postTopicId;
		private int _postForumId;
		private String _postText;
		
		public void setPostId(int postId)
		{
			_postId = postId;
		}
		
		public int getPostId()
		{
			return _postId;
		}
		
		public void setPostOwner(String postOwner)
		{
			_postOwner = postOwner;
		}
		
		public String getPostOwner()
		{
			return _postOwner;
		}
		
		public void setPostOwnerId(int postOwnerId)
		{
			_postOwnerId = postOwnerId;
		}
		
		public int getPostOwnerId()
		{
			return _postOwnerId;
		}
		
		public void setPostDate(long postDate)
		{
			_postDate = postDate;
		}
		
		public long getPostDate()
		{
			return _postDate;
		}
		
		public void setPostTopicId(int postTopicId)
		{
			_postTopicId = postTopicId;
		}
		
		public int getPostTopicId()
		{
			return _postTopicId;
		}
		
		public void setPostForumId(int postForumId)
		{
			_postForumId = postForumId;
		}
		
		public int getPostForumId()
		{
			return _postForumId;
		}
		
		public void setPostText(String postText)
		{
			_postText = postText;
		}
		
		public String getPostText()
		{
			if (_postText == null)
			{
				return "";
			}
			
			// Bypass exploit check.
			final String text = _postText.toLowerCase();
			if (text.contains("action") && text.contains("bypass"))
			{
				return "";
			}
			
			// Returns text without tags.
			return _postText.replaceAll("<.*?>", "");
		}
	}
	
	private final Collection<CPost> _post;
	
	/**
	 * Creates a new {@code Post} object and saves it to the database.<br>
	 * This constructor initializes all required fields for a forum post.
	 * @param postOwner The name of the user who created the post.
	 * @param postOwnerId The unique identifier of the user who created the post.
	 * @param date The timestamp when the post was created.
	 * @param tid The unique identifier for the topic this post belongs to.
	 * @param postForumId The unique identifier for the forum where the post is located.
	 * @param txt The actual content text of the post.
	 */
	public Post(String postOwner, int postOwnerId, long date, int tid, int postForumId, String txt)
	{
		_post = ConcurrentHashMap.newKeySet();
		final CPost cp = new CPost();
		cp.setPostId(0);
		cp.setPostOwner(postOwner);
		cp.setPostOwnerId(postOwnerId);
		cp.setPostDate(date);
		cp.setPostTopicId(tid);
		cp.setPostForumId(postForumId);
		cp.setPostText(txt);
		_post.add(cp);
		insertindb(cp);
	}
	
	/**
	 * Saves a new post record into the database.<br>
	 * This method uses a {@code PreparedStatement} to execute an {@code INSERT} query.<br>
	 * It handles any exceptions by logging a warning message.
	 * @param cp The {@code CPost} object containing the data to be saved.
	 */
	private void insertindb(CPost cp)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("INSERT INTO posts (post_id,post_owner_name,post_ownerid,post_date,post_topic_id,post_forum_id,post_txt) values (?,?,?,?,?,?,?)"))
		{
			ps.setInt(1, cp.getPostId());
			ps.setString(2, cp.getPostOwner());
			ps.setInt(3, cp.getPostOwnerId());
			ps.setLong(4, cp.getPostDate());
			ps.setInt(5, cp.getPostTopicId());
			ps.setInt(6, cp.getPostForumId());
			ps.setString(7, cp.getPostText());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while saving new Post to db " + e.getMessage(), e);
		}
	}
	
	/**
	 * Creates a new {@code Post} object based on a specific topic.<br>
	 * This constructor initializes the post data by loading it from the provided {@code Topic}.
	 * @param t The {@code Topic} used to load the post information.
	 */
	public Post(Topic t)
	{
		_post = ConcurrentHashMap.newKeySet();
		load(t);
	}
	
	/**
	 * Retrieves a specific post from the internal list.<br>
	 * It searches for the post at the given index.
	 * @param id The zero-based index of the post to retrieve.
	 * @return The {@code CPost} object if found, or {@code null} if it does not exist.
	 */
	public CPost getCPost(int id)
	{
		int i = 0;
		for (CPost cp : _post)
		{
			if (i++ == id)
			{
				return cp;
			}
		}
		
		return null;
	}
	
	/**
	 * Deletes the current post from the database.<br>
	 * This method removes all posts associated with a specific {@code Topic}.<br>
	 * It also updates the {@code PostBBSManager} instance.
	 * @param t The {@code Topic} object used to identify which posts to delete.
	 */
	public void deleteMe(Topic t)
	{
		PostBBSManager.getInstance().delPostByTopic(t);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("DELETE FROM posts WHERE post_forum_id=? AND post_topic_id=?"))
		{
			ps.setInt(1, t.getForumID());
			ps.setInt(2, t.getID());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while deleting post: " + e.getMessage(), e);
		}
	}
	
	/**
	 * Loads all posts associated with a specific {@code Topic} from the database.<br>
	 * It retrieves records based on the forum and topic IDs.<br>
	 * Each retrieved post is added to the internal collection.
	 * @param t The {@code Topic} object containing the required IDs for loading.
	 */
	private void load(Topic t)
	{
		final int forumId = t.getForumID();
		final int id = t.getID();
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("SELECT * FROM posts WHERE post_forum_id=? AND post_topic_id=? ORDER BY post_id ASC"))
		{
			ps.setInt(1, forumId);
			ps.setInt(2, id);
			try (ResultSet rs = ps.executeQuery())
			{
				while (rs.next())
				{
					final CPost cp = new CPost();
					cp.setPostId(rs.getInt("post_id"));
					cp.setPostOwner(rs.getString("post_owner_name"));
					cp.setPostOwnerId(rs.getInt("post_ownerid"));
					cp.setPostDate(rs.getLong("post_date"));
					cp.setPostTopicId(rs.getInt("post_topic_id"));
					cp.setPostForumId(rs.getInt("post_forum_id"));
					cp.setPostText(rs.getString("post_txt"));
					_post.add(cp);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Data error on Post " + forumId + "/" + id + " : " + e.getMessage(), e);
		}
	}
	
	/**
	 * Updates the text of a specific post in the database.<br>
	 * This method retrieves the post data using the provided ID.<br>
	 * It then saves the current text to the database table.
	 * @param i The unique identifier for the post to be updated.
	 */
	public void updateText(int i)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement("UPDATE posts SET post_txt=? WHERE post_id=? AND post_topic_id=? AND post_forum_id=?"))
		{
			final CPost cp = getCPost(i);
			ps.setString(1, cp.getPostText());
			ps.setInt(2, cp.getPostId());
			ps.setInt(3, cp.getPostTopicId());
			ps.setInt(4, cp.getPostForumId());
			ps.execute();
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error while saving new Post to db " + e.getMessage(), e);
		}
	}
}
