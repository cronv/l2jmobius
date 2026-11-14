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
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code Betray} skill effect.<br>
 * It defines the logic for how this specific effect behaves on a target.<br>
 * It extends {@code AbstractEffect} to provide its functionality.
 * @author decad
 */
public class Betray extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Betray} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the initial values for this effect.
	 */
	public Betray(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code Betray} effect can be applied.<br>
	 * This method verifies if the requirements for the effect are met.<br>
	 * It returns {@code true} only if the {@code effector} is a player and the {@code effected} is a summon.
	 * @param effector The creature casting or triggering the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effector.isPlayer() && effected.isSummon();
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.BETRAYED}.
	 * @return The long value representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.BETRAYED.getMask();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It changes the behavior of the {@code effected} creature.<br>
	 * The target will now attack players.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.getAI().setIntentionAttack(effected.asPlayer());
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It resets the target creature's AI state to idle.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} affected by the skill.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.getAI().setIntentionIdle();
	}
}
