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
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.communitybbs.TopicConstructorType;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.BB.Post;
import org.l2jmobius.gameserver.communitybbs.BB.Topic;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Manages the lifecycle and operations of {@code Topic} objects within the community board system.<br>
 * This class handles the creation, retrieval, and organization of forum topics.
 */
public class TopicBBSManager extends BaseBBSManager
{
	private final Collection<Topic> _table = ConcurrentHashMap.newKeySet();
	private final Map<Forum, Integer> _maxId = new ConcurrentHashMap<>();
	
	/**
	 * This is the default constructor for {@code TopicBBSManager}.<br>
	 * It prevents external classes from creating new instances of this manager.<br>
	 * Use {@code getInstance} to get the singleton instance.
	 */
	protected TopicBBSManager()
	{
		// Prevent external initialization.
	}
	
	/**
	 * Adds a new {@code Topic} to the internal collection.<br>
	 * This method registers the provided topic for management.
	 * @param tt The {@code Topic} object to be added.
	 */
	public void addTopic(Topic tt)
	{
		_table.add(tt);
	}
	
	/**
	 * Removes a specific topic from the internal collection.<br>
	 * This method updates the list of active topics by deleting the provided {@code Topic}.
	 * @param topic The {@code Topic} object to be removed.
	 */
	public void delTopic(Topic topic)
	{
		_table.remove(topic);
	}
	
	/**
	 * Updates the maximum ID for a specific forum.<br>
	 * This method stores the highest ID associated with a {@code Forum}.
	 * @param id The new maximum ID value to set.
	 * @param f The {@code Forum} object that will be updated.
	 */
	public void setMaxID(int id, Forum f)
	{
		_maxId.put(f, id);
	}
	
	/**
	 * Retrieves the highest topic ID for a specific forum.<br>
	 * It returns {@code 0} if no maximum ID is currently stored.
	 * @param f The {@code Forum} object to check.
	 * @return The maximum integer ID associated with the forum.
	 */
	public int getMaxID(Forum f)
	{
		final Integer i = _maxId.get(f);
		return i == null ? 0 : i;
	}
	
	/**
	 * Retrieves a {@code Topic} object based on its unique identifier.<br>
	 * This method searches through the internal collection of topics.<br>
	 * It returns the matching topic if found.
	 * @param idf The unique ID of the topic to find.
	 * @return The {@code Topic} with the matching ID, or {@code null} if no match exists.
	 */
	public Topic getTopicByID(int idf)
	{
		for (Topic t : _table)
		{
			if (t.getID() == idf)
			{
				return t;
			}
		}
		
		return null;
	}
	
	/**
	 * Parses and executes community board commands.<br>
	 * This method handles creating or deleting topics based on the provided arguments.
	 * @param ar1 The command type such as "crea" or "del".
	 * @param ar2 The forum identifier used to locate the correct forum.
	 * @param ar3 The topic identifier used for deletion operations.
	 * @param ar4 The content of the post being created.
	 * @param ar5 The title of the new topic.
	 * @param player The {@code Player} object who is executing the command.
	 */
	@Override
	public void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player)
	{
		if (ar1.equals("crea"))
		{
			final Forum f = ForumsBBSManager.getInstance().getForumByID(Integer.parseInt(ar2));
			if (f == null)
			{
				CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + ar2 + " is not implemented yet</center><br><br></body></html>", player);
			}
			// The memo box holds at most 100 memos.
			else if ((f.getType() == Forum.MEMO) && (f.getTopicSize() >= 100))
			{
				player.sendPacket(SystemMessageId.THE_MEMO_BOX_IS_FULL_THERE_IS_A_100_MEMO_LIMIT);
			}
			else
			{
				final long currentTime = System.currentTimeMillis();
				f.vload();
				final String playerName = player.getName();
				final int playerObjId = player.getObjectId();
				final Topic t = new Topic(TopicConstructorType.CREATE, getInstance().getMaxID(f) + 1, Integer.parseInt(ar2), ar5, currentTime, playerName, playerObjId, (f.getType() == Forum.MEMO) ? Topic.MEMO : Topic.NORMAL, 0);
				f.addTopic(t);
				final int topicId = t.getID();
				getInstance().setMaxID(topicId, f);
				final Post p = new Post(playerName, playerObjId, currentTime, topicId, f.getID(), ar4);
				PostBBSManager.getInstance().addPostByTopic(p, t);
				parsecmd(getListBypass(f), player);
			}
		}
		else if (ar1.equals("del"))
		{
			final Forum f = ForumsBBSManager.getInstance().getForumByID(Integer.parseInt(ar2));
			if (f == null)
			{
				CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + ar2 + " does not exist !</center><br><br></body></html>", player);
			}
			else
			{
				final Topic t = f.getTopic(Integer.parseInt(ar3));
				if (t == null)
				{
					CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the topic: " + ar3 + " does not exist !</center><br><br></body></html>", player);
				}
				else
				{
					// CPost cp = null;
					final Post p = PostBBSManager.getInstance().getGPosttByTopic(t);
					if (p != null)
					{
						p.deleteMe(t);
					}
					
					t.deleteme(f);
					parsecmd(getListBypass(f), player);
				}
			}
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the command: " + ar1 + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	/**
	 * Processes a community board command sent by a {@code Player}.<br>
	 * It handles actions like reading, creating, and deleting topics.<br>
	 * If the command is unknown, it sends an error message to the player.
	 * @param command The raw string command received from the client.
	 * @param player The {@code Player} instance who sent the command.
	 */
	@Override
	public void parsecmd(String command, Player player)
	{
		if (command.equals("_bbsmemo"))
		{
			final Forum memo = player.getMemo();
			showTopics(memo, player, 1, memo.getID());
		}
		else if (command.startsWith("_bbstopics;read"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final int idf = Integer.parseInt(st.nextToken());
			final String index = st.hasMoreTokens() ? st.nextToken() : null;
			final int ind = index == null ? 1 : Integer.parseInt(index);
			showTopics(ForumsBBSManager.getInstance().getForumByID(idf), player, ind, idf);
		}
		else if (command.startsWith("_bbstopics;crea"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final int idf = Integer.parseInt(st.nextToken());
			showNewTopic(ForumsBBSManager.getInstance().getForumByID(idf), player, idf);
		}
		else if (command.startsWith("_bbstopics;del"))
		{
			final StringTokenizer st = new StringTokenizer(command, ";");
			st.nextToken();
			st.nextToken();
			final int idf = Integer.parseInt(st.nextToken());
			final int idt = Integer.parseInt(st.nextToken());
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
					// CPost cp = null;
					final Post p = PostBBSManager.getInstance().getGPosttByTopic(t);
					if (p != null)
					{
						p.deleteMe(t);
					}
					
					t.deleteme(f);
					parsecmd(getListBypass(f), player);
				}
			}
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the command: " + command + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	/**
	 * Displays a new topic to the specified {@code Player}.<br>
	 * This method checks if the {@code Forum} exists and determines its type.<br>
	 * It handles different display logic based on whether the forum is a memo or standard type.
	 * @param forum The {@code Forum} object associated with the request.
	 * @param player The {@code Player} who will view the topic.
	 * @param idf The unique identifier for the new topic.
	 */
	private void showNewTopic(Forum forum, Player player, int idf)
	{
		if (forum == null)
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + idf + " is not implemented yet</center><br><br></body></html>", player);
		}
		else if ((forum.getType() == Forum.MEMO) || (forum.getType() == Forum.CLAN))
		{
			showMemoNewTopics(forum, player);
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + forum.getName() + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	/**
	 * Displays the memo form for new topics in a specific forum.<br>
	 * This method sends an HTML interface to the player.
	 * @param forum The {@code Forum} where the memo is displayed.
	 * @param player The {@code Player} who will view the memo.
	 */
	private void showMemoNewTopics(Forum forum, Player player)
	{
		final String html = "<html><body><br><br><table border=0 width=610><tr><td width=10></td><td width=600 align=left><a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass " + getListBypass(forum) + "\">" + getListName(forum) + "</a></td></tr></table><img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=0><tr><td width=610><img src=\"sek.cbui355\" width=\"610\" height=\"1\"><br1><img src=\"sek.cbui355\" width=\"610\" height=\"1\"></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=20></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29>&$413;</td><td FIXWIDTH=540><edit var = \"Title\" width=540 height=13></td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29 valign=top>&$427;</td><td align=center FIXWIDTH=540><MultiEdit var =\"Content\" width=535 height=313></td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr></table><table fixwidth=610 border=0 cellspacing=0 cellpadding=0><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=1></td><td align=center FIXWIDTH=60 height=29>&nbsp;</td><td align=center FIXWIDTH=70><button value=\"&$140;\" action=\"Write Topic crea " + forum.getID() + " Title Content Title\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\" ></td><td align=center FIXWIDTH=70><button value = \"&$141;\" action=\"bypass " + getListBypass(forum) + "\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\"> </td><td align=center FIXWIDTH=400>&nbsp;</td><td><img src=\"l2ui.mini_logo\" width=5 height=1></td></tr></table></center></body></html>";
		send1001(html, player);
		send1002(player);
	}
	
	/**
	 * Displays the list of topics for a specific forum to a player.<br>
	 * This method checks if the forum exists and determines its type.<br>
	 * It handles different display logic based on whether it is a memo or standard forum.
	 * @param forum The {@code Forum} object containing the topic data.
	 * @param player The {@code Player} who is viewing the topics.
	 * @param index The current page or position index for the list.
	 * @param idf The unique identifier of the forum.
	 */
	private void showTopics(Forum forum, Player player, int index, int idf)
	{
		if (forum == null)
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + idf + " is not implemented yet</center><br><br></body></html>", player);
		}
		else if ((forum.getType() == Forum.MEMO) || (forum.getType() == Forum.CLAN))
		{
			showMemoTopics(forum, player, index);
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>the forum: " + forum.getName() + " is not implemented yet</center><br><br></body></html>", player);
		}
	}
	
	/**
	 * Displays a paginated list of memo topics for a specific forum.<br>
	 * This method generates an HTML layout containing the topic names and dates.<br>
	 * It also includes navigation buttons to switch between different pages.
	 * @param forum The {@code Forum} object where the topics are located.
	 * @param player The {@code Player} who is viewing the memo topics.
	 * @param index The page number to display.
	 */
	private void showMemoTopics(Forum forum, Player player, int index)
	{
		forum.vload();
		final int forumId = forum.getID();
		final StringBuilder html = new StringBuilder(2000);
		html.append("<html><body><br><br><table border=0 width=610><tr><td width=10></td><td width=600 align=left><a action=\"bypass _bbshome\">HOME</a>&nbsp;>&nbsp;<a action=\"bypass " + getListBypass(forum) + "\">" + getListName(forum) + "</a></td></tr></table><img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=2 bgcolor=888888 width=610><tr><td FIXWIDTH=5></td><td FIXWIDTH=415 align=center>&$413;</td><td FIXWIDTH=120 align=center></td><td FIXWIDTH=70 align=center>&$418;</td></tr></table>");
		final DateFormat dateFormat = DateFormat.getInstance();
		for (int i = 0, j = getMaxID(forum) + 1; i < (12 * index); j--)
		{
			if (j < 0)
			{
				break;
			}
			
			final Topic t = forum.getTopic(j);
			if ((t != null) && (i++ >= (12 * (index - 1))))
			{
				html.append("<table border=0 cellspacing=0 cellpadding=5 WIDTH=610><tr><td FIXWIDTH=5></td><td FIXWIDTH=415><a action=\"bypass _bbsposts;read;" + forumId + ";" + t.getID() + "\">" + t.getName() + "</a></td><td FIXWIDTH=120 align=center></td><td FIXWIDTH=70 align=center>" + dateFormat.format(new Date(t.getDate())) + "</td></tr></table><img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\">");
			}
		}
		
		html.append("<br><table width=610 cellspace=0 cellpadding=0><tr><td width=50><button value=\"&$422;\" action=\"bypass " + getListBypass(forum) + "\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\"></td><td width=510 align=center><table border=0><tr>");
		if (index == 1)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"bypass _bbstopics;read;" + forumId + ";" + (index - 1) + "\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		
		// Twelve topics per page, at least one page.
		final int nbp = Math.max(1, (forum.getTopicSize() + 11) / 12);
		
		for (int i = 1; i <= nbp; i++)
		{
			if (i == index)
			{
				html.append("<td> " + i + " </td>");
			}
			else
			{
				html.append("<td><a action=\"bypass _bbstopics;read;" + forumId + ";" + i + "\"> " + i + " </a></td>");
			}
		}
		
		if (index == nbp)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"bypass _bbstopics;read;" + forumId + ";" + (index + 1) + "\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		
		html.append("</tr></table> </td> <td align=right><button value = \"&$421;\" action=\"bypass _bbstopics;crea;" + forumId + "\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\" ></td></tr><tr><td><img src=\"l2ui.mini_logo\" width=5 height=10></td></tr><tr> <td></td><td align=center><table border=0><tr><td></td><td><edit var = \"Search\" width=130 height=11></td><td><button value=\"&$420;\" action=\"Write 5 -2 0 Search _ _\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\"> </td> </tr></table> </td></tr></table><br><br><br></center></body></html>");
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}
	
	/**
	 * Retrieves the bypass that lists the topics of a forum.
	 * @param forum The {@code Forum} to list.
	 * @return The memo list bypass for memo forums, otherwise the topic list bypass of the forum.
	 */
	public static String getListBypass(Forum forum)
	{
		return (forum.getType() == Forum.MEMO) ? "_bbsmemo" : "_bbstopics;read;" + forum.getID();
	}
	
	/**
	 * Retrieves the navigation name of a forum.
	 * @param forum The {@code Forum} to name.
	 * @return {@code Memo Form} for memo forums, otherwise the forum name.
	 */
	public static String getListName(Forum forum)
	{
		return (forum.getType() == Forum.MEMO) ? "Memo Form" : forum.getName();
	}
	
	/**
	 * Retrieves the singleton instance of the {@code TopicBBSManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The single instance of {@code TopicBBSManager}.
	 */
	public static TopicBBSManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TopicBBSManager INSTANCE = new TopicBBSManager();
	}
}
