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
package handlers.skill.targets;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

/**
 * This class handles skill targets for a player's pet.<br>
 * It identifies the specific pet owned by a {@code Creature} when a skill is cast.
 * @author Mobius
 */
public class OwnerPet implements ITargetTypeHandler
{
	/**
	 * Returns the specific target type for this handler.<br>
	 * This method identifies which {@code TargetType} is associated with the owner's pet.
	 * @return The {@code TargetType} enum value.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.OWNER_PET;
	}
	
	/**
	 * Retrieves the target object for a specific skill action.<br>
	 * This method identifies which {@code WorldObject} should be affected.<br>
	 * It returns the player performing the action as the target.
	 * @param creature The {@code Creature} who is initiating the action.
	 * @param selectedTarget The {@code WorldObject} currently selected by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to bypass normal usage requirements.
	 * @param dontMove Whether to prevent the character from moving during the action.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return The {@code WorldObject} representing the target.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		return creature.asPlayer();
	}
}
