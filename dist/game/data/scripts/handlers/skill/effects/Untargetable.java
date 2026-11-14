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

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code Untargetable} effect.<br>
 * It prevents other entities from targeting a creature while the effect is active.
 * @author UnAfraid
 */
public class Untargetable extends AbstractEffect
{
	/**
	 * Creates a new {@code Untargetable} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Untargetable(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a specific target.<br>
	 * This method returns {@code true} only if the {@code effected} creature is a player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the target is a player, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It removes any creatures that are currently targeting the {@code effected} creature.
	 * @param effector The creature who triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply this effect.
	 * @param item The item used to apply this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Remove target from those that have the untargetable creature on target.
		World.forEachVisibleObject(effected, Creature.class, c ->
		{
			if (c.getTarget() == effected)
			{
				c.setTarget(null);
			}
		});
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * It returns the value associated with {@code EffectFlag.UNTARGETABLE}.
	 * @return The long value representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.UNTARGETABLE.getMask();
	}
}
