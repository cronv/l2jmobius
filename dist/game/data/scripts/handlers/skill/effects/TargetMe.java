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
 * This class handles the "Target Me" skill effect.<br>
 * It manages the logic for applying this specific effect to a {@code Creature}.
 * @author -Nemesiss-
 */
public class TargetMe extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code TargetMe} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public TargetMe(StatSet params)
	{
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes any locked target from the {@code effected} creature.<br>
	 * This ensures the player can select new targets again.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who was affected by the skill.
	 * @param skill The {@code Skill} used to apply this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayable())
		{
			effected.asPlayable().setLockedTarget(null);
		}
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It forces the {@code effected} creature to target the {@code effector}.<br>
	 * The target is locked if the {@code effected} creature is playable.
	 * @param effector The creature who cast the skill or caused the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 * @param item The {@code Item} used to trigger the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayable())
		{
			if (effected.getTarget() != effector)
			{
				effected.setTarget(effector);
			}
			
			effected.asPlayable().setLockedTarget(effector);
		}
	}
}
