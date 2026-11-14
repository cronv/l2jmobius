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
 * Handles the evasion effect applied by skills.<br>
 * This effect does not stack and takes the value from the most recent application.
 * @author Sdw
 */
public class SkillEvasion extends AbstractEffect
{
	private final int _magicType;
	private final double _amount;
	
	/**
	 * Creates a new {@code SkillEvasion} effect.<br>
	 * This constructor initializes the magic type and amount from a provided set.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public SkillEvasion(StatSet params)
	{
		_magicType = params.getInt("magicType", 0);
		_amount = params.getDouble("amount", 0);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It applies a skill evasion bonus to the target creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.getStat().addSkillEvasionTypeValue(_magicType, _amount);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the evasion value from the {@code effected} creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.getStat().removeSkillEvasionTypeValue(_magicType, _amount);
	}
}
