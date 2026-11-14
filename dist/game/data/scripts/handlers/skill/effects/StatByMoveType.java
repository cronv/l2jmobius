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
import org.l2jmobius.gameserver.mechanics.stats.MoveType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements a skill effect that modifies stats based on the {@code MoveType}.<br>
 * It allows for dynamic stat adjustments depending on how a creature is moving.
 * @author UnAfraid
 */
public class StatByMoveType extends AbstractEffect
{
	private final Stat _stat;
	private final MoveType _type;
	private final double _value;
	
	/**
	 * Creates a new {@code StatByMoveType} effect instance.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts the required stat, move type, and numerical value.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public StatByMoveType(StatSet params)
	{
		_stat = params.getEnum("stat", Stat.class);
		_type = params.getEnum("type", MoveType.class);
		_value = params.getDouble("value");
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It applies a stat modification to the target creature.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} used to trigger this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.getStat().mergeMoveTypeValue(_stat, _type, _value);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the stat modification from the {@code effected} creature.<br>
	 * The value is subtracted to revert the change made during the effect.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.getStat().mergeMoveTypeValue(_stat, _type, -_value);
	}
	
	/**
	 * Determines if the effect should trigger during an action.<br>
	 * This method checks if the {@code Skill} is a passive or toggle type.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill associated with this effect.
	 * @param item The item used to trigger the skill, if any.
	 * @return {@code true} if the skill is passive or toggle, otherwise {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		return skill.isPassive() || skill.isToggle();
	}
}
