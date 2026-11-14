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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.MailManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.holders.MailMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles the periodic removal of expired messages from the system.<br>
 * It ensures that {@code MailMessage} objects are cleaned up to maintain server performance.
 * @author Mobius
 */
public class MessageDeletionTaskManager implements Runnable
{
	private static final Map<Integer, Long> PENDING_MESSAGES = new ConcurrentHashMap<>();
	private static final AtomicBoolean WORKING = new AtomicBoolean();
	
	/**
	 * Initializes the {@code MessageDeletionTaskManager}.<br>
	 * This constructor schedules a recurring task to run every 10000 milliseconds.<br>
	 * It handles the cleanup of expired messages in the background.
	 */
	protected MessageDeletionTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this, 10000, 10000);
	}
	
	@Override
	public void run()
	{
		if (!WORKING.compareAndSet(false, true))
		{
			return;
		}
		
		try
		{
			if (!PENDING_MESSAGES.isEmpty())
			{
				final long currentTime = System.currentTimeMillis();
				final Iterator<Integer> iterator = PENDING_MESSAGES.keySet().iterator();
				Integer messageId;
				Long time;
				MailMessage message;
				
				while (iterator.hasNext())
				{
					messageId = iterator.next();
					time = PENDING_MESSAGES.get(messageId);
					if ((time != null) && (currentTime > time.longValue()))
					{
						message = MailManager.getInstance().getMessage(messageId);
						if (message == null)
						{
							iterator.remove();
							continue;
						}
						
						if (message.hasAttachments())
						{
							final Player sender = World.getPlayer(message.getSenderId());
							final Player receiver = World.getPlayer(message.getReceiverId());
							if (sender != null)
							{
								message.getAttachments().returnToWh(sender.getWarehouse());
								sender.sendPacket(SystemMessageId.THE_MAIL_WAS_RETURNED_DUE_TO_THE_EXCEEDED_WAITING_TIME);
							}
							else if (message.getSenderId() == -1) // Action House / Custom Mail
							{
								if (receiver != null)
								{
									message.getAttachments().returnToWh(receiver.getWarehouse());
								}
							}
							else
							{
								message.getAttachments().returnToWh(null);
							}
							
							message.getAttachments().deleteMe();
							message.removeAttachments();
							
							if (receiver != null)
							{
								receiver.sendPacket(new SystemMessage(SystemMessageId.THE_MAIL_WAS_RETURNED_DUE_TO_THE_EXCEEDED_WAITING_TIME));
							}
						}
						
						MailManager.getInstance().deleteMessageInDb(messageId);
						iterator.remove();
					}
				}
			}
		}
		finally
		{
			WORKING.set(false);
		}
	}
	
	/**
	 * Adds a new message to the pending deletion list.<br>
	 * This method stores the message ID and its scheduled removal time.
	 * @param msgId The unique identifier for the system message.
	 * @param deletionTime The timestamp when the message should be removed.
	 */
	public void add(int msgId, long deletionTime)
	{
		PENDING_MESSAGES.put(msgId, deletionTime);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code MessageDeletionTaskManager}.<br>
	 * Use this method to access the global task manager.
	 * @return The active {@code MessageDeletionTaskManager} instance.
	 */
	public static MessageDeletionTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MessageDeletionTaskManager INSTANCE = new MessageDeletionTaskManager();
	}
}
