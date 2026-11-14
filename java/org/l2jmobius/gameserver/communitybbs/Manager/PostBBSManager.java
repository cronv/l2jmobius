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
package org.l2jmobius.gameserver.communitybbs.Manager;

import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.BB.Post;
import org.l2jmobius.gameserver.communitybbs.BB.Post.CPost;
import org.l2jmobius.gameserver.communitybbs.BB.Topic;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;

/**
 * Manages the lifecycle and operations of {@code Post} objects within the community board system.<br>
 * This class handles actions such as creating, retrieving, and organizing posts across different forums.
 */
public class PostBBSManager extends BaseBBSManager
{
	private final Map<Topic, Post> _postByTopic = new ConcurrentHashMap<>();
	
	/**
	 * Retrieves a {@code Post} associated with a specific {@code Topic}.<br>
	 * If no {@code Post} exists for the given {@code Topic}, a new one is created.
	 * @param t The {@code Topic} to look up.
	 * @return The {@code Post} linked to the {@code Topic}.
	 */
	public Post getGPosttByTopic(Topic t)
	{
		Post post = _postByTopic.get(t);
		if (post == null)
		{
			post = new Post(t);
			_postByTopic.put(t, post);
		}
		
		return post;
	}
	
	/**
	 * Removes a {@code Post} associated with a specific {@code Topic}.<br>
	 * This method updates the internal map by deleting the entry for the provided topic.
	 * @param t The {@code Topic} object whose posts should be removed.
	 */
	public void delPostByTopic(Topic t)
	{
		_postByTopic.remove(t);
	}
	
	/**
	 * Adds a {@code Post} to a specific {@code Topic}.<br>
	 * This method only adds the post if the topic does not already have one.
	 * @param p The {@code Post} object to be added.
	 * @param t The {@code Topic} where the post will be stored.
	 */
	public void addPostByTopic(Post p, Topic t)
	{
		if (_postByTopic.get(t) == null)
		{
			_postByTopic.put(t, p);
		}
	}
	
	/**
	 * Processes incoming chat commands for the community board system.<br>
	 * This method identifies specific actions like reading or editing posts.<br>
	 * It handles command parsing and triggers the appropriate display logic.
	 * @param command The raw string command sent by the player.
	 * @param player The {@code Player} object who executed the command.
	 */
	@Override
	public void parsecmd(String command, Player player)
	{
		if (command.startsWith("_bbsposts;read;"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final int idf = Integer.parseInt(st.nextToken());
			final int idp = Integer.parseInt(st.nextToken());
			final String index = st.hasMoreTokens() ? st.nextToken() : null;
			final int ind = index == null ? 1 : Integer.parseInt(index);
			showPost(TopicBBSManager.getInstance().getTopicByID(idp), ForumsBBSManager.getInstance().getForumByID(idf), player, ind);
		}
		else if (command.startsWith("_bbsposts;edit;"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final int idf = Integer.parseInt(st.nextToken());
			final int idt = Integer.parseInt(st.nextToken());
			final int idp = Integer.parseInt(st.nextToken());
			showEditPost(TopicBBSManager.getInstance().getTopicByID(idt), ForumsBBSManager.getInstance().getForumByID(idf), player, idp);
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	/**
	 * Displays the editing interface for a specific post.<br>
	 * It checks if the provided {@code Forum}, {@code Topic}, and post exist.<br>
	 * If any are missing, it sends an error message to the {@code Player}.<br>
	 * Otherwise, it calls {@code showHtmlEditPost} to display the editor.
	 * @param topic The {@code Topic} containing the post.
	 * @param forum The {@code Forum} where the topic is located.
	 * @param player The {@code Player} who is viewing the edit screen.
	 * @param idp The unique identifier for the post.
	 */
	private void showEditPost(Topic topic, Forum forum, Player player, int idp)
	{
		final Post p = getGPosttByTopic(topic);
		if ((forum == null) || (topic == null) || (p == null))
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>Error, this forum, topic or post does not exist!</center><br><br></body></html>", player);
		}
		else
		{
			showHtmlEditPost(topic, player, forum, p);
		}
	}
	
	/**
	 * Displays a specific post to the player.<br>
	 * It checks if the {@code Forum} and {@code Topic} are valid.<br>
	 * It handles different forum types like {@code MEMO}.<br>
	 * If the forum is not implemented, it sends an error message.
	 * @param topic The {@code Topic} where the post is located.
	 * @param forum The {@code Forum} containing the topic.
	 * @param player The {@code Player} who is viewing the post.
	 * @param ind The index of the post to display.
	 */
	private void showPost(Topic topic, Forum forum, Player player, int ind)
	{
		if ((forum == null) || (topic == null))
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>Error: This forum is not implemented yet!</center></body></html>", player);
		}
		else if ((forum.getType() == Forum.MEMO) || (forum.getType() == Forum.CLAN))
		{
			showMemoPost(topic, player, forum);
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>The forum: " + forum.getName() + " is not implemented yet!</center></body></html>", player);
		}
	}
	
	/**
	 * Displays the HTML interface for editing a specific post.<br>
	 * This method constructs an HTML string containing a multi-edit box.<br>
	 * It sends the layout to the player and provides the current content.
	 * @param topic The {@code Topic} where the post is located.
	 * @param player The {@code Player} who will view the edit screen.
	 * @param forum The {@code Forum} associated with the topic.
	 * @param p The {@code Post} object being edited.
	 */
	private void showHtmlEditPost(Topic topic, Player player, Forum forum, Post p)
	{
		final String topicName = topic.getName();
		final String html = "<html><body><br><br><table border=0 width=610><tr><td width=10></td><td width=600 align=left><a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass " + TopicBBSManager.getListBypass(forum) + "\">" + TopicBBSManager.getListName(forum) + "</a></td></tr></table><img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=0><tr><td width=610><img src=\"sek.cbui355\" width=\"610\" height=\"1\"><br1><img src=\"sek.cbui355\" width=\"610\" height=\"1\"></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=20></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29>&$413;</td><td FIXWIDTH=540>" + topicName + "</td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29 valign=top>&$427;</td><td align=center FIXWIDTH=540><MultiEdit var =\"Content\" width=535 height=313></td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29>&nbsp;</td><td align=center FIXWIDTH=70><button value=\"&$140;\" action=\"Write Post " + forum.getID() + ";" + topic.getID() + ";0 _ Content Content Content\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\" ></td><td align=center FIXWIDTH=70><button value = \"&$141;\" action=\"bypass " + TopicBBSManager.getListBypass(forum) + "\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\"> </td><td align=center FIXWIDTH=400>&nbsp;</td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr></table></center></body></html>";
		send1001(html, player);
		send1002(player, p.getCPost(0).getPostText(), topicName, DateFormat.getInstance().format(new Date(topic.getDate())));
	}
	
	/**
	 * Displays the memo post content to a specific player.<br>
	 * This method retrieves the first post of a {@code Topic}.<br>
	 * It formats the text and wraps it in an HTML layout.<br>
	 * Finally, it sends the formatted message to the {@code Player}.
	 * @param topic The {@code Topic} containing the memo post.
	 * @param player The {@code Player} who will view the memo.
	 * @param forum The {@code Forum} where the topic is located.
	 */
	private void showMemoPost(Topic topic, Player player, Forum forum)
	{
		final Post p = getGPosttByTopic(topic);
		final Locale locale = Locale.getDefault();
		final DateFormat dateFormat = DateFormat.getDateInstance(DateFormat.FULL, locale);
		String mes = p.getCPost(0).getPostText().replace(">", "&gt;");
		mes = mes.replace("<", "&lt;");
		
		final int forumId = forum.getID();
		final int topicId = topic.getID();
		final String html = "<html><body><br><br><table border=0 width=610><tr><td width=10></td><td width=600 align=left><a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass " + TopicBBSManager.getListBypass(forum) + "\">" + TopicBBSManager.getListName(forum) + "</a></td></tr></table><img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=0 bgcolor=333333><tr><td height=10></td></tr><tr><td fixWIDTH=55 align=right valign=top>&$413; : &nbsp;</td><td fixWIDTH=380 valign=top>" + topic.getName() + "</td><td fixwidth=5></td><td fixwidth=50></td><td fixWIDTH=120></td></tr><tr><td height=10></td></tr><tr><td align=right><font color=\"AAAAAA\" >&$417; : &nbsp;</font></td><td><font color=\"AAAAAA\">" + topic.getOwnerName() + "</font></td><td></td><td><font color=\"AAAAAA\">&$418; :</font></td><td><font color=\"AAAAAA\">" + dateFormat.format(p.getCPost(0).getPostDate()) + "</font></td></tr><tr><td height=10></td></tr></table><br><table border=0 cellspacing=0 cellpadding=0><tr><td fixwidth=5></td><td FIXWIDTH=600 align=left>" + mes + "</td><td fixqqwidth=5></td></tr></table><br><img src=\"L2UI.squareblank\" width=\"1\" height=\"5\"><img src=\"L2UI.squaregray\" width=\"610\" height=\"1\"><img src=\"L2UI.squareblank\" width=\"1\" height=\"5\"><table border=0 cellspacing=0 cellpadding=0 FIXWIDTH=610><tr><td width=50><button value=\"&$422;\" action=\"bypass " + TopicBBSManager.getListBypass(forum) + "\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\"></td><td width=560 align=right><table border=0 cellspacing=0><tr><td FIXWIDTH=300></td><td><button value = \"&$424;\" action=\"bypass _bbsposts;edit;" + forumId + ";" + topicId + ";0\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\" ></td>&nbsp;<td><button value = \"&$425;\" action=\"bypass _bbstopics;del;" + forumId + ";" + topicId + "\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\" ></td>&nbsp;<td><button value = \"&$421;\" action=\"bypass _bbstopics;crea;" + forumId + "\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\" ></td>&nbsp;</tr></table></td></tr></table><br><br><br></center></body></html>";
		CommunityBoardHandler.separateAndSend(html, player);
	}
	
	/**
	 * Parses and updates a specific post's text content.<br>
	 * This method validates the existence of the forum, topic, and post before applying changes.<br>
	 * It then refreshes the view for the {@code Player}.
	 * @param ar1 The semicolon-separated string containing forum ID, topic ID, and post ID.
	 * @param ar2 An unused argument.
	 * @param ar3 An unused argument.
	 * @param ar4 The new text content for the post.
	 * @param ar5 An unused argument.
	 * @param player The {@code Player} who is performing the action.
	 */
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		final StringTokenizer st = new StringTokenizer(ar1, ";");
		final int idf = Integer.parseInt(st.nextToken());
		final int idt = Integer.parseInt(st.nextToken());
		final int idp = Integer.parseInt(st.nextToken());
		final Forum f = ForumsBBSManager.getInstance().getForumByID(idf);
		if (f == null)
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + idf + " does not exist !</center><br><br></body></html>", player);
		}
		else
		{
			final Topic t = f.getTopic(idt);
			if (t == null)
			{
				CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the topic: " + idt + " does not exist !</center><br><br></body></html>", player);
			}
			else
			{
				final Post p = getGPosttByTopic(t);
				if (p != null)
				{
					final CPost postObj = p.getCPost(idp);
					if (postObj == null)
					{
						CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the post: " + idp + " does not exist !</center><br><br></body></html>", player);
					}
					else
					{
						postObj.setPostText(ar4);
						p.updateText(idp);
						parsecmd("_bbsposts;read;" + f.getID() + ";" + t.getID(), player);
					}
				}
			}
		}
	}
	
	/**
	 * Provides access to the singleton instance of {@code PostBBSManager}.<br>
	 * Use this method to get the global manager for BBS posts.
	 * @return The single instance of {@code PostBBSManager}.
	 */
	public static PostBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PostBBSManager INSTANCE = new PostBBSManager();
	}
}
