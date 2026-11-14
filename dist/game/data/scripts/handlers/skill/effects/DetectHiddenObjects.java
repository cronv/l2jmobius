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
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.DoorOpenType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code DetectHiddenObjects} skill effect.<br>
 * It allows players to perceive objects that are otherwise hidden in the game world.
 * @author UnAfraid
 */
public class DetectHiddenObjects extends AbstractEffect
{
	/**
	 * Initializes a new {@code DetectHiddenObjects} effect.<br>
	 * This constructor sets up the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public DetectHiddenObjects(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a skill on a target.<br>
	 * This method reveals hidden doors if they are affected by the skill.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} or object receiving the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isDoor())
		{
			return;
		}
		
		final Door door = effected.asDoor();
		if (door.getTemplate().isStealth())
		{
			door.setMeshIndex(1);
			door.setTargetable(door.getTemplate().getOpenType() != DoorOpenType.NONE);
			door.broadcastStatusUpdate();
		}
	}
}
