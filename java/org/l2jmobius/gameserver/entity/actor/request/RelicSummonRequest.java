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
 * Handles the request sent by a {@code Player} to summon a relic.<br>
 * This class processes the logic required to initiate the summoning action.
 * @author Mobius
 */
public class RelicSummonRequest extends AbstractRequest
{
	/**
	 * Creates a new {@code RelicSummonRequest} for a specific player.<br>
	 * This constructor initializes the request with the provided {@code Player} object.
	 * @param player The {@code Player} who is initiating the relic summon.
	 */
	public RelicSummonRequest(Player player)
	{
		super(player);
	}
	
	/**
	 * Checks if the request is currently being used by a specific object.<br>
	 * This method always returns {@code false}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code true} if the object is using the request, otherwise {@code false}.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
}
