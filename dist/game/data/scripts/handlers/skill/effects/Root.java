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
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for root effects in the game.<br>
 * It manages how creatures are immobilized when affected by a {@code Skill}.
 * @author mkizub
 */
public class Root extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Root} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Root(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for the effect flags.<br>
	 * This method returns the value associated with {@code EffectFlag.ROOTED}.
	 * @return The {@code long} bitmask representing the root status.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.ROOTED.getMask();
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value representing a root effect.
	 * @return the {@code EffectType} of this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.ROOT;
	}
	
	/**
	 * This method is called when a root effect ends.<br>
	 * It notifies the AI of the {@code effected} creature to rethink its actions.<br>
	 * This logic only runs if the {@code effected} creature is not a player.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature affected by the root.
	 * @param skill The skill that caused the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (!effected.isPlayer())
		{
			effected.getAI().notifyActionThink();
		}
	}
	
	/**
	 * Checks if the effect can be applied to the target.<br>
	 * Raid bosses are immune, checked here so that the effect flag is not applied to them either.
	 * @param effector The {@code Creature} casting the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && !effected.isRaid();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It stops the movement of the target creature.<br>
	 * It notifies the AI that the creature is rooted.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill used to apply this effect.
	 * @param item The item used to trigger the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.stopMove(null);
		effected.getAI().notifyActionRooted();
	}
}
