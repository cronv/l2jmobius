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
package org.l2jmobius.gameserver.handler;

import java.util.function.Consumer;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectScope;

/**
 * This interface defines the logic for handling effects based on a specific {@code AffectScope}.<br>
 * It allows for custom behavior when applying skills or mechanics to targets within a defined area.<br>
 * Implementations determine how an effect interacts with different types of entities.
 * @author Nik
 */
public interface IAffectScopeHandler
{
	void forEachAffected(Creature creature, WorldObject target, Skill skill, Consumer<? super WorldObject> action);
	
	Enum<AffectScope> getAffectScopeType();
}
