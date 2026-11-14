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
 * Handles the logic for skills that target a {@code Creature}.<br>
 * This class is used to process actions performed on specific creature entities.
 * @author Liamxroy
 */
public class RecallCreature implements ITargetTypeHandler
{
	/**
	 * Retrieves the specific target for a recall skill.<br>
	 * This method identifies which {@code WorldObject} the {@code Creature} should interact with.
	 * @param creature The {@code Creature} performing the action.
	 * @param selectedTarget The {@code WorldObject} currently selected by the user.
	 * @param skill The {@code Skill} being executed.
	 * @param forceUse Whether to ignore requirements and force the use of the skill.
	 * @param dontMove Whether to prevent the character from moving during the action.
	 * @param sendMessage Whether to send a confirmation message to the player.
	 * @return The {@code WorldObject} associated with the recall creature.
	 */
	@Override
	public WorldObject getTarget(Creature creature, WorldObject selectedTarget, Skill skill, boolean forceUse, boolean dontMove, boolean sendMessage)
	{
		return creature.getRecallCreature();
	}
	
	/**
	 * Retrieves the target type associated with this handler.<br>
	 * This method returns the {@code TargetType} used for identifying valid targets.
	 * @return The {@code TargetType} enum value.
	 */
	@Override
	public Enum<TargetType> getTargetType()
	{
		return TargetType.RECALL_CREATURE;
	}
}
