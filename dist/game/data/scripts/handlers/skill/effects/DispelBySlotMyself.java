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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code DispelBySlot} effect applied to the caster.<br>
 * It removes specific status effects from the player based on defined slot constraints.
 * @author Gnacik, Zoey76, Adry_85
 */
public class DispelBySlotMyself extends AbstractEffect
{
	private final Set<AbnormalType> _dispelAbnormals;
	
	/**
	 * Creates a new instance of the {@code DispelBySlotMyself} effect.<br>
	 * It initializes the set of abnormal types to be dispelled based on the provided parameters.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public DispelBySlotMyself(StatSet params)
	{
		final String dispel = params.getString("dispel");
		if ((dispel != null) && !dispel.isEmpty())
		{
			_dispelAbnormals = new HashSet<>();
			for (String slot : dispel.split(";"))
			{
				_dispelAbnormals.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_dispelAbnormals = Collections.<AbnormalType> emptySet();
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return the {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DISPEL_BY_SLOT;
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
	 * Executes the instant effect of a dispel skill.<br>
	 * It removes specific abnormal effects from the target creature.<br>
	 * This method checks if any abnormalities are defined before processing.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used to trigger the dispel.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_dispelAbnormals.isEmpty())
		{
			return;
		}
		
		// The effectlist should already check if it has buff with this abnormal type or not.
		effected.getEffectList().stopEffects(info -> !info.getSkill().isIrreplaceableBuff() && _dispelAbnormals.contains(info.getSkill().getAbnormalType()), true, true);
	}
}
