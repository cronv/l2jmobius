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
package handlers.bypass.communityboard;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.BB.Topic;
import org.l2jmobius.gameserver.communitybbs.Manager.PostBBSManager;
import org.l2jmobius.gameserver.communitybbs.Manager.TopicBBSManager;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;

/**
 * Handles the logic for the memo board system.<br>
 * This class allows players to interact with and write messages on a community board.<br>
 * It implements {@code IWriteBoardHandler} to manage board updates.
 * @author Zoey76, Mobius
 */
public class MemoBoard implements IWriteBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsmemo",
		"_bbstopics",
		"_bbsposts"
	};
	
	private static final String[] CLAN_BOARDS =
	{
		Clan.ANNOUNCEMENT_BOARD,
		Clan.FREE_BOARD
	};
	
	/**
	 * Retrieves the list of valid commands for this board.<br>
	 * These commands are used to interact with the {@code MemoBoard}.
	 * @return An array of {@code String} containing all available command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	/**
	 * Handles the execution of a specific bypass command.<br>
	 * It shows the account memo or a board of the {@code Player} clan.
	 * @param command The name of the command being executed.
	 * @param player The {@code Player} who sent the command.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player)
	{
		CommunityBoardHandler.getInstance().addBypass(player, "Memo Command", command);
		
		// Players can only browse their account memo and the boards of their clan.
		final String[] params = command.split(";");
		final Forum forum = (params.length > 2) ? getForum(player, params[2]) : null;
		if (forum == null)
		{
			TopicBBSManager.getInstance().parsecmd("_bbsmemo", player);
		}
		else if (!isAllowed(player, command, forum, (params.length > 3) ? getTopic(forum, params[3]) : null))
		{
			TopicBBSManager.getInstance().parsecmd(TopicBBSManager.getListBypass(forum), player);
		}
		else if (command.startsWith("_bbsposts"))
		{
			PostBBSManager.getInstance().parsecmd(command, player);
		}
		else
		{
			TopicBBSManager.getInstance().parsecmd(command, player);
		}
		
		return true;
	}
	
	/**
	 * Processes a command to write content to the community board.<br>
	 * This method handles the input provided by a {@code Player}.<br>
	 * Topic writes create or delete topics, post writes edit them.
	 * @param player The {@code Player} who sent the command.
	 * @param arg1 The first argument of the command.
	 * @param arg2 The second argument of the command.
	 * @param arg3 The third argument of the command.
	 * @param arg4 The fourth argument of the command.
	 * @param arg5 The fifth argument of the command.
	 * @return {@code true} if the command was successful, otherwise {@code false}.
	 */
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		// Players can only write in their account memo and the boards of their clan.
		if (arg1.equals("crea") || arg1.equals("del"))
		{
			final Forum forum = getForum(player, arg2);
			if ((forum != null) && canWrite(player, forum, arg1.equals("del") ? getTopic(forum, arg3) : null))
			{
				TopicBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, player);
			}
		}
		else
		{
			final String[] ids = arg1.split(";");
			final Forum forum = getForum(player, ids[0]);
			if ((forum != null) && (ids.length > 1) && canWrite(player, forum, getTopic(forum, ids[1])))
			{
				PostBBSManager.getInstance().parsewrite(arg1, arg2, arg3, arg4, arg5, player);
			}
		}
		
		return true;
	}
	
	/**
	 * Resolves a forum id from a command to a forum the {@code Player} may use.
	 * @param player The {@code Player} who sent the command.
	 * @param forumId The forum id taken from the command.
	 * @return The account memo or a board of the player clan, otherwise {@code null}.
	 */
	private Forum getForum(Player player, String forumId)
	{
		final Forum memo = player.getMemo();
		if (forumId.equals(String.valueOf(memo.getID())))
		{
			return memo;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return null;
		}
		
		for (String name : CLAN_BOARDS)
		{
			final Forum board = clan.getBoard(name);
			if ((board != null) && forumId.equals(String.valueOf(board.getID())))
			{
				return board;
			}
		}
		
		return null;
	}
	
	/**
	 * Resolves a topic id from a command inside the given forum.
	 * @param forum The {@code Forum} that must hold the topic.
	 * @param topicId The topic id taken from the command.
	 * @return The {@code Topic}, or {@code null} if the forum has no such topic.
	 */
	private Topic getTopic(Forum forum, String topicId)
	{
		return StringUtil.isNumeric(topicId) ? forum.getTopic(Integer.parseInt(topicId)) : null;
	}
	
	/**
	 * Checks whether a bypass command may run in the given forum.
	 * @param player The {@code Player} who sent the command.
	 * @param command The bypass command.
	 * @param forum The {@code Forum} of the command.
	 * @param topic The {@code Topic} of the command, or {@code null} if it has none.
	 * @return {@code true} if the command may run, otherwise {@code false}.
	 */
	private boolean isAllowed(Player player, String command, Forum forum, Topic topic)
	{
		if (command.startsWith("_bbstopics;read"))
		{
			return true;
		}
		
		if (command.startsWith("_bbstopics;crea"))
		{
			return canWrite(player, forum, null);
		}
		
		// Posts are found by topic id alone, so the topic must belong to the forum as well.
		return (topic != null) && (command.startsWith("_bbsposts;read") || canWrite(player, forum, topic));
	}
	
	/**
	 * Checks whether the {@code Player} may create a topic, or edit and delete the given one.<br>
	 * The clan leader moderates both clan boards, members post on the free board and change only their own topics.
	 * @param player The {@code Player} who sent the command.
	 * @param forum The {@code Forum} to write in.
	 * @param topic The {@code Topic} to edit or delete, or {@code null} to create one.
	 * @return {@code true} if the player may write, otherwise {@code false}.
	 */
	private boolean canWrite(Player player, Forum forum, Topic topic)
	{
		if ((forum.getType() == Forum.MEMO) || player.isClanLeader())
		{
			return true;
		}
		
		return forum.getName().equals(Clan.FREE_BOARD) && ((topic == null) || (topic.getOwnerId() == player.getObjectId()));
	}
}
