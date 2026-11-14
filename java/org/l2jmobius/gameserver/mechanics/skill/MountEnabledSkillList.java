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
package org.l2jmobius.gameserver.mechanics.skill;

/**
 * This class manages a list of skills that can be used while on a mount.<br>
 * It helps the game engine determine which abilities are valid in this state.
 * @author Mobius
 */
public class MountEnabledSkillList
{
	private static final int STRIDER_SIEGE_ASSAULT = 325;
	private static final int WYVERN_BREATH = 4289;
	
	/**
	 * Checks if a specific skill is part of the allowed mount list.<br>
	 * This method verifies the provided {@code skillId}.
	 * @param skillId The unique identifier of the skill to check.
	 * @return {@code true} if the skill is in the list, otherwise {@code false}.
	 */
	public static boolean contains(int skillId)
	{
		return (skillId == STRIDER_SIEGE_ASSAULT) || (skillId == WYVERN_BREATH);
	}
}
