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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for blocking a specific target during a skill effect.<br>
 * This class ensures that the target is prevented from performing certain actions or movements.<br>
 * It extends {@code AbstractEffect} to integrate with the game's skill system.
 * @author UnAfraid
 */
public class BlockTarget extends AbstractEffect
{
	/**
	 * Creates a new {@code BlockTarget} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public BlockTarget(StatSet params)
	{
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It makes the {@code effected} creature untargetable.<br>
	 * It also clears any active targets pointing at the {@code effected} creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} used to trigger this effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.setTargetable(false);
		World.forEachVisibleObject(effected, Creature.class, target ->
		{
			if ((target.getTarget() == effected))
			{
				target.setTarget(null);
			}
		});
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It restores the targetability of the {@code effected} creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The {@code Skill} used to apply this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.setTargetable(true);
	}
}
