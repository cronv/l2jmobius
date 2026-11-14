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
package org.l2jmobius.gameserver.entity.actor.stat;

import org.l2jmobius.gameserver.entity.actor.Npc;

/**
 * Represents the statistics for an {@code Npc} entity.<br>
 * This class handles specific attributes and data related to non-player characters.
 */
public class NpcStat extends CreatureStat
{
	/**
	 * Creates a new {@code NpcStat} instance for a specific NPC.<br>
	 * This constructor initializes the statistics based on the provided character.
	 * @param activeChar The {@code Npc} object to associate with these stats.
	 */
	public NpcStat(Npc activeChar)
	{
		super(activeChar);
	}
	
	/**
	 * Retrieves the level of the active character.<br>
	 * This value is fetched from the {@code Npc} template.
	 * @return The current level as an {@code int}.
	 */
	@Override
	public int getLevel()
	{
		return getActiveChar().getTemplate().getLevel();
	}
	
	/**
	 * Retrieves the {@code Npc} associated with this stat.<br>
	 * This method returns the active character cast as an {@code Npc}.
	 * @return The current {@code Npc} object.
	 */
	@Override
	public Npc getActiveChar()
	{
		return super.getActiveChar().asNpc();
	}
	
	/**
	 * Retrieves the physical attack angle for the NPC.<br>
	 * This value is taken from the base template of the active character.
	 * @return The physical attack angle as an {@code int}.
	 */
	@Override
	public int getPhysicalAttackAngle()
	{
		return getActiveChar().getTemplate().getBaseAttackAngle();
	}
}
