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
 * This class handles the {@code Flag} effect implementation.<br>
 * It defines how the flag mechanic behaves when applied to an entity.
 * @author BiggBoss
 */
public class Flag extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Flag} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Flag(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It returns {@code true} only if the {@code effected} creature is not {@code null}.<br>
	 * The target must also be a player character.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && effected.isPlayer();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It updates the PvP flag for the target creature.
	 * @param effector The {@code Creature} who cast the skill.
	 * @param effected The {@code Creature} who received the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 * @param item The {@code Item} used to trigger the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.updatePvPFlag(1);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the PvP flag from the affected player.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.asPlayer().updatePvPFlag(0);
	}
}
