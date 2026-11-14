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
 * Handles the logic for removing effects from a target.<br>
 * This effect identifies and dispels active status effects on a {@code Creature}.<br>
 * It is triggered when a skill specifically targets an opponent's buffs or debuffs.
 * @author Gnacik, Zoey76, Adry_85
 */
public class DispelCaster extends AbstractEffect
{
	private final Set<AbnormalType> _dispelAbnormals;
	
	/**
	 * Creates a new {@code DispelCaster} effect instance.<br>
	 * This constructor initializes the set of abnormal types to be dispelled.<br>
	 * It reads these values from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public DispelCaster(StatSet params)
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
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DISPEL_BY_SLOT;
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the dispel effect immediately.<br>
	 * This method removes specific abnormal effects from the {@code effector}.<br>
	 * It checks if any active buffs match the types defined in this effect.
	 * @param effector The creature who performs the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_dispelAbnormals.isEmpty())
		{
			return;
		}
		
		// The effectlist should already check if it has buff with this abnormal type or not.
		effector.getEffectList().stopEffects(info -> !info.getSkill().isIrreplaceableBuff() && _dispelAbnormals.contains(info.getSkill().getAbnormalType()), true, true);
	}
}
