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
 * This effect provides protection by blocking incoming debuffs.<br>
 * It functions similarly to the Linken Sphere from DOTA.<br>
 * The {@code AbnormalShield} prevents negative status effects from being applied.
 * @author Nik
 */
public class AbnormalShield extends AbstractEffect
{
	private final int _times;
	
	/**
	 * Creates a new {@code AbnormalShield} effect.<br>
	 * This constructor initializes the shield using the provided statistics.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public AbnormalShield(StatSet params)
	{
		_times = params.getInt("times", -1);
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It sets the number of blocked debuffs on the target creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} used to trigger this effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.setAbnormalShieldBlocks(_times);
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * This value is used to identify specific behaviors of the {@code AbnormalShield}.
	 * @return The {@code long} mask representing the {@code EffectFlag.ABNORMAL_SHIELD} flag.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.ABNORMAL_SHIELD.getMask();
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the shield status from the target creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.setAbnormalShieldBlocks(Integer.MIN_VALUE);
	}
	
	/**
	 * Retrieves the type of this effect.<br>
	 * This method returns a constant value representing an abnormal shield.
	 * @return The {@code EffectType} associated with this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.ABNORMAL_SHIELD;
	}
}
