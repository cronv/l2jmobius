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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.DispelSlotType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code StealAbnormal} effect.<br>
 * It manages how abnormal status effects are transferred between entities.
 * @author Adry_85, Zoey76
 */
public class StealAbnormal extends AbstractEffect
{
	private final DispelSlotType _slot;
	private final int _rate;
	private final int _max;
	
	/**
	 * Creates a new {@code StealAbnormal} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the dispel slot, rate, and maximum limits.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public StealAbnormal(StatSet params)
	{
		_slot = params.getEnum("slot", DispelSlotType.class, DispelSlotType.BUFF);
		_rate = params.getInt("rate", 0);
		_max = params.getInt("max", 0);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this specific effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.STEAL_ABNORMAL;
	}
	
	/**
	 * Checks if this effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of stealing abnormal status from a target.<br>
	 * This method transfers specific buffs from the {@code effected} creature to the {@code effector}.<br>
	 * It only applies if the {@code effected} creature is a player and different from the {@code effector}.
	 * @param effector The creature that performs the action.
	 * @param effected The creature from whom the effect is stolen.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer() && (effector != effected))
		{
			final List<BuffInfo> toSteal = Formulas.calcCancelStealEffects(effector, effected, skill, _slot, _rate, _max);
			if (toSteal.isEmpty())
			{
				return;
			}
			
			for (BuffInfo infoToSteal : toSteal)
			{
				// Invert effected and effector.
				final BuffInfo stolen = new BuffInfo(effected, effector, infoToSteal.getSkill(), false, null, null);
				stolen.setAbnormalTime(infoToSteal.getTime()); // Copy the remaining time.
				
				// To include all the effects, it's required to go through the template rather the buff info.
				infoToSteal.getSkill().applyEffectScope(EffectScope.GENERAL, stolen, true, true);
				effected.getEffectList().remove(infoToSteal, SkillFinishType.REMOVED, true, true);
				effector.getEffectList().add(stolen);
			}
		}
	}
}
