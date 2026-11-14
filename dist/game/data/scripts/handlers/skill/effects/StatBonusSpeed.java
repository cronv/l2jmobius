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

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingItemType;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the application of a speed bonus to an actor.<br>
 * This effect modifies the {@code BaseStat} for movement speed.<br>
 * It is used by skills and items to increase character mobility.
 * @author Sdw
 */
public class StatBonusSpeed extends AbstractEffect
{
	private final Double _stat;
	private final Condition _armorTypeCondition;
	
	/**
	 * Creates a new {@code StatBonusSpeed} effect.<br>
	 * This constructor initializes the speed bonus based on provided parameters.<br>
	 * It sets the target stat and any required armor type conditions.
	 * @param params The {@code StatSet} containing configuration data for this effect.
	 */
	public StatBonusSpeed(StatSet params)
	{
		_stat = Double.valueOf(params.getEnum("stat", BaseStat.class, BaseStat.DEX).ordinal());
		
		int armorTypesMask = 0;
		final List<String> armorTypes = params.getList("armorType", String.class);
		if (armorTypes != null)
		{
			for (String armorType : armorTypes)
			{
				try
				{
					armorTypesMask |= ArmorType.valueOf(armorType).mask();
				}
				catch (IllegalArgumentException e)
				{
					final IllegalArgumentException exception = new IllegalArgumentException("armorTypes should contain ArmorType enum value but found " + armorType);
					exception.addSuppressed(e);
					throw exception;
				}
			}
		}
		
		_armorTypeCondition = armorTypesMask != 0 ? new ConditionUsingItemType(armorTypesMask) : null;
	}
	
	/**
	 * Applies a speed bonus to the target creature.<br>
	 * This method checks if the armor type condition is met before applying the effect.<br>
	 * The {@code Stat.STAT_BONUS_SPEED} value is merged into the creature's stats.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		if ((_armorTypeCondition == null) || _armorTypeCondition.test(effected, effected, skill))
		{
			effected.getStat().mergeAdd(Stat.STAT_BONUS_SPEED, _stat);
		}
	}
}
