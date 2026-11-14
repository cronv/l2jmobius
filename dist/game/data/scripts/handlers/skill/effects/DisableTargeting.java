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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the targeting disable effect.<br>
 * Affected players will lose their current target and become unable to select new targets for the duration of the effect.
 * @author Nik
 */
public class DisableTargeting extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code DisableTargeting} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration for this effect.
	 */
	public DisableTargeting(StatSet params)
	{
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It clears the target from the {@code effected} creature.<br>
	 * It also stops any active attacks or casts.<br>
	 * The AI intention is set to idle.
	 * @param effector The creature that caused the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply this effect.
	 * @param item The item used to apply this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.setTarget(null);
		effected.abortAttack();
		effected.abortCast();
		effected.getAI().setIntentionIdle();
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * It returns the value associated with {@code EffectFlag.TARGETING_DISABLED}.
	 * @return The {@code long} mask representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.TARGETING_DISABLED.getMask();
	}
}
