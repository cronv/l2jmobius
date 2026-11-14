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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for unsummoning a player's servitors.<br>
 * This {@code UnsummonServitors} class manages the removal of active summons.
 * @author Nik
 */
public class UnsummonServitors extends AbstractEffect
{
	/**
	 * This constructor initializes the {@code UnsummonServitors} effect.<br>
	 * It uses the provided {@code StatSet} to configure the effect properties.
	 * @param params The {@code StatSet} containing the necessary parameters for this effect.
	 */
	public UnsummonServitors(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for unsummoning servitors.<br>
	 * This method stops all actions and effects on the effector's servitors.<br>
	 * It then removes them from the owner.
	 * @param effector The {@code Creature} who owns the servitors.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.hasServitors())
		{
			effector.getServitors().values().forEach(servitor ->
			{
				servitor.abortAttack();
				servitor.abortCast();
				servitor.stopAllEffects();
				
				servitor.unSummon(effector.asPlayer());
			});
		}
	}
}
