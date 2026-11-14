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
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code RebalanceHP} effect specifically for {@code Summon} entities.<br>
 * It ensures that health points are adjusted correctly when this effect is applied to a summon.
 * @author Adry_85, earendil
 */
public class RebalanceHPSummon extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code RebalanceHPSummon} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public RebalanceHPSummon(StatSet params)
	{
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.REBALANCE_HP;
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, {@code false} otherwise.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a skill.<br>
	 * This method rebalances the health of the {@code effector} and their nearby summons based on their total HP percentage.
	 * @param effector The creature who casts the skill.
	 * @param effected The creature who is affected by the skill.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer())
		{
			return;
		}
		
		double fullHP = 0;
		double currentHPs = 0;
		
		for (Summon summon : effector.getServitors().values())
		{
			if (!summon.isDead() && LocationUtil.checkIfInRange(skill.getAffectRange(), effector, summon, true))
			{
				fullHP += summon.getMaxHp();
				currentHPs += summon.getCurrentHp();
			}
		}
		
		fullHP += effector.getMaxHp();
		currentHPs += effector.getCurrentHp();
		
		final double percentHP = currentHPs / fullHP;
		for (Summon summon : effector.getServitors().values())
		{
			if (!summon.isDead() && LocationUtil.checkIfInRange(skill.getAffectRange(), effector, summon, true))
			{
				double newHP = summon.getMaxHp() * percentHP;
				if (newHP > summon.getCurrentHp()) // The target gets healed.
				{
					// The heal will be blocked if the current hp passes the limit.
					if (summon.getCurrentHp() > summon.getMaxRecoverableHp())
					{
						newHP = summon.getCurrentHp();
					}
					else if (newHP > summon.getMaxRecoverableHp())
					{
						newHP = summon.getMaxRecoverableHp();
					}
				}
				
				summon.setCurrentHp(newHP);
			}
		}
		
		double newHP = effector.getMaxHp() * percentHP;
		if (newHP > effector.getCurrentHp()) // The target gets healed.
		{
			// The heal will be blocked if the current hp passes the limit.
			final double currentEffectorHp = effector.getCurrentHp();
			if (currentEffectorHp > effector.getMaxRecoverableHp())
			{
				newHP = currentEffectorHp;
			}
			else if (newHP > effector.getMaxRecoverableHp())
			{
				newHP = effector.getMaxRecoverableHp();
			}
		}
		
		effector.setCurrentHp(newHP);
	}
}
