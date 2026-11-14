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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;

// This class is here mostly for convenience and for avoidance of hardcoded IDs.
// It refers to Beast (mobs) that can be attacked but can also be fed.
// For example, the Beast Farm's Alpen Buffalo.
// This class is only used by handlers to verify target correctness, as all other tasks are managed by scripted AI.
/**
 * Represents a type of monster that can be fed by players.<br>
 * This class is primarily used by handlers to verify targets for feeding actions.<br>
 * It serves as a convenient way to identify specific creatures like the Alpen Buffalo without using hardcoded IDs.
 */
public class FeedableBeast extends Monster
{
	/**
	 * Creates a new {@code FeedableBeast} instance.<br>
	 * This constructor initializes the beast using a provided template.<br>
	 * It sets the {@code InstanceType} to {@code FeedableBeast}.
	 * @param template The {@code NpcTemplate} used to define the beast.
	 */
	public FeedableBeast(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.FeedableBeast);
	}
}
