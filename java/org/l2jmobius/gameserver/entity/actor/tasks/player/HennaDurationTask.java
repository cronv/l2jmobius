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
package org.l2jmobius.gameserver.entity.actor.tasks.player;

import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Manages the duration of a {@code Henna} effect on a {@code Player}.<br>
 * This task handles the expiration logic for the henna's active time.
 * @author Mobius
 */
public class HennaDurationTask implements Runnable
{
	private final Player _player;
	private final int _slot;
	
	/**
	 * Creates a new task to manage the duration of a henna effect.<br>
	 * This task tracks the specific slot used by the {@code Player}.
	 * @param player The {@code Player} who owns the henna effect.
	 * @param slot The inventory slot where the henna is located.
	 */
	public HennaDurationTask(Player player, int slot)
	{
		_player = player;
		_slot = slot;
	}
	
	@Override
	public void run()
	{
		if (_player != null)
		{
			_player.getHennaList().remove(_slot);
		}
	}
}
