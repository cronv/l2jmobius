/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.targets.affectscope;

import java.util.function.Consumer;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.handler.AffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.handler.IAffectScopeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;

/**
 * This class provides a single target affect scope implementation.<br>
 * It handles effects that are applied to one specific {@code Creature} or {@code WorldObject}.
 * @author Nik
 */
public class Single implements IAffectScopeHandler
{
	/**
	 * Iterates through all objects affected by a specific skill scope.<br>
	 * It checks if the {@code target} is a creature or an item.<br>
	 * The provided {@code action} is performed on each valid object found.
	 * @param creature The {@code Creature} who cast the skill.
	 * @param target The primary {@code WorldObject} targeted by the skill.
	 * @param skill The {@code Skill} being executed.
	 * @param action A {@code Consumer} to execute on each affected {@code WorldObject}.
	 */
	@Override
	public void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		final IAffectObjectHandler affectObject = AffectObjectHandler.getInstance().getHandler(skill.getAffectObject());
		if (target.isCreature())
		{
			final TargetType targetType = skill.getTargetType();
			if (targetType == TargetType.GROUND)
			{
				action.accept(creature); // Return yourself to mark that effects can use your current skill's world position.
			}
			
			// Only the body skills act on a corpse, anything else drops a target that died during the cast.
			final Creature targetCreature = target.asCreature();
			if (targetCreature.isDead() && (targetType != TargetType.NPC_BODY) && (targetType != TargetType.PC_BODY) && !skill.isStayAfterDeath())
			{
				return;
			}
			
			if (((affectObject == null) || affectObject.checkAffectedObject(creature, targetCreature)))
			{
				action.accept(target); // Return yourself to mark that effects can use your current skill's world position.
			}
		}
		else if (target.isItem())
		{
			action.accept(target); // Return yourself to mark that effects can use your current skill's world position.
		}
	}
	
	/**
	 * Retrieves the scope type for this handler.<br>
	 * It returns the {@code AffectScope} constant associated with this implementation.
	 * @return The {@code AffectScope} type of this handler.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.SINGLE;
	}
}
