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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code HpByLevel} effect.<br>
 * It calculates and applies health values based on a creature's level.
 * @author Zoey76
 */
public class HpByLevel extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code HpByLevel} effect instance.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts the {@code power} value to determine the health scaling.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public HpByLevel(StatSet params)
	{
		_power = params.getDouble("power", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the effect immediately to a target.<br>
	 * This method restores health to the {@code effected} creature based on the power of this effect.<br>
	 * It also sends a system message to the target showing the amount of health restored.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} used to trigger the effect.
	 * @param item The {@code Item} used to trigger the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Calculation
		final double abs = _power;
		final double currentHp = effector.getCurrentHp();
		final long maxHp = effector.getMaxHp();
		final double absorb = ((currentHp + abs) > maxHp ? maxHp : (currentHp + abs));
		final int restored = (int) (absorb - currentHp);
		effector.setCurrentHp(absorb);
		
		// System message
		final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_VE_RECOVERED_S1_HP);
		sm.addInt(restored);
		effector.sendPacket(sm);
	}
}
