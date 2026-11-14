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
import org.l2jmobius.gameserver.handler.IAffectScopeHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;

/**
 * Handles the specific area of effect for Wyvern-related skills.<br>
 * This class determines which targets are affected based on the {@code Wyvern} scope logic.
 * @author Nik
 */
public class WyvernScope implements IAffectScopeHandler
{
	/**
	 * Iterates through all objects affected by a specific skill.<br>
	 * This method applies an action to each valid {@code WorldObject}.
	 * @param creature The {@code Creature} who cast the skill.
	 * @param target The primary {@code WorldObject} targeted by the skill.
	 * @param skill The {@code Skill} being executed.
	 * @param action The {@code Consumer} to execute on each affected object.
	 */
	@Override
	public void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action)
	{
		// TODO Unknown affect scope.
	}
	
	/**
	 * Retrieves the specific type of affect scope for this handler.<br>
	 * This method identifies how the effect is applied to targets.
	 * @return The {@code AffectScope} enum value representing the wyvern scope.
	 */
	@Override
	public Enum<AffectScope> getAffectScopeType()
	{
		return AffectScope.WYVERN_SCOPE;
	}
}
