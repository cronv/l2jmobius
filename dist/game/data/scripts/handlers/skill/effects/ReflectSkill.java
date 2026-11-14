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
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.BasicProperty;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for skills that reflect damage back to the attacker.<br>
 * This {@code ReflectSkill} effect determines how much of an incoming hit is returned based on specific stats.
 * @author Sdw
 */
public class ReflectSkill extends AbstractEffect
{
	private final Stat _stat;
	private final double _amount;
	
	/**
	 * Creates a new instance of {@code ReflectSkill}.<br>
	 * This constructor initializes the skill properties from a provided set.<br>
	 * It determines whether to reflect physical or magic damage based on the input.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ReflectSkill(StatSet params)
	{
		_stat = params.getEnum("type", BasicProperty.class, BasicProperty.PHYSICAL) == BasicProperty.PHYSICAL ? Stat.REFLECT_SKILL_PHYSIC : Stat.REFLECT_SKILL_MAGIC;
		_amount = params.getDouble("amount", 0);
	}
	
	/**
	 * Applies a stat modification to the target creature.<br>
	 * This method updates the {@code Creature} stats based on the provided {@code Skill}.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		effected.getStat().mergeAdd(_stat, _amount);
	}
}
