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
package org.l2jmobius.gameserver.entity.actor.tasks.attackable;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * This task manages the periodic timing for command channel updates.<br>
 * It ensures that {@code Attackable} entities process their respective commands at regular intervals.
 * @author xban1x, Mobius
 */
public class CommandChannelTimer implements Runnable
{
	private final Attackable _attackable;
	
	/**
	 * Creates a new {@code CommandChannelTimer} for a specific target.<br>
	 * This timer will manage actions related to the provided {@code Attackable}.
	 * @param attackable The {@code Attackable} entity to be tracked by this timer.
	 */
	public CommandChannelTimer(Attackable attackable)
	{
		_attackable = attackable;
	}
	
	@Override
	public void run()
	{
		if (_attackable == null)
		{
			return;
		}
		
		if ((System.currentTimeMillis() - _attackable.getCommandChannelLastAttack()) > PlayerConfig.LOOT_RAIDS_PRIVILEGE_INTERVAL)
		{
			_attackable.getFirstCommandChannelAttacked().broadcastPacket(new ExShowScreenMessage(NpcStringId.LOOTING_RULES_ARE_NO_LONGER_ACTIVE, ExShowScreenMessage.TOP_CENTER, 10000));
			_attackable.setCommandChannelTimer(null);
			_attackable.setFirstCommandChannelAttacked(null);
			_attackable.setCommandChannelLastAttack(0);
		}
		else
		{
			ThreadPool.schedule(this, 10000); // 10sec
		}
	}
}
