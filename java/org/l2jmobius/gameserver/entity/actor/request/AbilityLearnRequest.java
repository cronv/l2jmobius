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
package org.l2jmobius.gameserver.entity.actor.request;

import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This request handles the process of a player learning a new ability.<br>
 * It validates and processes the {@code Player} action to acquire specific skills.
 * @author Mobius
 */
public class AbilityLearnRequest extends AbstractRequest
{
	/**
	 * Creates a new {@code AbilityLearnRequest} for a specific player.<br>
	 * This request is used when a player tries to learn a new ability.
	 * @param player The {@code Player} who is making the request.
	 */
	public AbilityLearnRequest(Player player)
	{
		super(player);
	}
	
	/**
	 * Checks if the request is currently being used on a specific object.<br>
	 * This method always returns {@code false}.
	 * @param objectId The unique identifier of the target object.
	 * @return {@code true} if the request is in use, otherwise {@code false}.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
}
