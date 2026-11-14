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
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the modification of a creature's {@code CP} value.<br>
 * This effect is primarily utilized for items like potions and for applying static damage.
 * @author Nik
 */
public class Cp extends AbstractEffect
{
	private final int _amount;
	private final StatModifierType _mode;
	
	/**
	 * Creates a new {@code Cp} effect instance.<br>
	 * This constructor initializes the amount and mode from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Cp(StatSet params)
	{
		_amount = params.getInt("amount", 0);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies an instant CP change effect to a target creature.<br>
	 * This method calculates the restoration amount based on the skill mode and item type.<br>
	 * It updates the target's current CP and sends a notification message.
	 * @param effector The {@code Creature} who performed the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to trigger the effect, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor() || effected.isHpBlocked())
		{
			return;
		}
		
		int basicAmount = _amount;
		if ((item != null) && (item.isPotion() || item.isElixir()))
		{
			basicAmount += effected.getStat().getValue(Stat.ADDITIONAL_POTION_CP, 0);
		}
		
		double amount = 0;
		switch (_mode)
		{
			case DIFF:
			{
				amount = Math.min(basicAmount, Math.max(0, effected.getMaxRecoverableCp() - effected.getCurrentCp()));
				break;
			}
			case PER:
			{
				amount = Math.min((effected.getMaxCp() * basicAmount) / 100, Math.max(0, effected.getMaxRecoverableCp() - effected.getCurrentCp()));
				break;
			}
		}
		
		if (amount != 0)
		{
			final double newCp = amount + effected.getCurrentCp();
			effected.setCurrentCp(newCp, false);
			effected.broadcastStatusUpdate(effector);
		}
		
		if (amount >= 0)
		{
			if ((effector != null) && (effector != effected))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOVERED_S2_CP_WITH_C1_S_HELP);
				sm.addString(effector.getName());
				sm.addInt((int) amount);
				effected.sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOVERED_S1_CP);
				sm.addInt((int) amount);
				effected.sendPacket(sm);
			}
		}
	}
}
