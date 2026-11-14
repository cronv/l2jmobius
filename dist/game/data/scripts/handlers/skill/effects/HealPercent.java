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
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for applying a healing effect based on a percentage.<br>
 * It calculates and applies health restoration to a {@code Creature} based on its current maximum health.
 * @author UnAfraid
 */
public class HealPercent extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code HealPercent} effect.<br>
	 * This constructor initializes the healing power from a {@code StatSet}.<br>
	 * It ensures that the {@code power} value is used instead of an {@code amount} value.
	 * @param params The {@code StatSet} containing the effect configuration.
	 */
	public HealPercent(StatSet params)
	{
		_power = params.getDouble("power", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Returns the type of effect associated with this instance.<br>
	 * This method identifies the effect as a healing action.
	 * @return the {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HEAL;
	}
	
	/**
	 * Checks if the effect is applied instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect happens immediately.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the heal effect to a target creature immediately.<br>
	 * This method calculates the healing amount based on the power of this effect.<br>
	 * It also accounts for potion bonuses and specific character stats.<br>
	 * If the result is negative, it applies damage instead.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} used to trigger this effect, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor() || effected.isHpBlocked() || effected.isRaid())
		{
			return;
		}
		
		double amount = 0;
		final double power = _power;
		final boolean full = (power == 100.0);
		
		amount = full ? effected.getMaxHp() : (effected.getMaxHp() * power) / 100.0;
		if ((item != null) && (item.isPotion() || item.isElixir()))
		{
			amount += effected.getStat().getValue(Stat.ADDITIONAL_POTION_HP, 0);
		}
		
		if (amount > 0)
		{
			final double healEffectVal = effected.getStat().getValue(Stat.HEAL_EFFECT, 1);
			if (healEffectVal > 1)
			{
				amount *= healEffectVal;
			}
			
			final double healEffectAddVal = effected.getStat().getValue(Stat.HEAL_EFFECT_ADD, 0);
			if (healEffectAddVal > 0)
			{
				amount += healEffectAddVal;
			}
		}
		
		// Prevents overheal
		final double currentHp = effected.getCurrentHp();
		amount = Math.min(amount, Math.max(0, effected.getMaxRecoverableHp() - currentHp));
		if (amount >= 0)
		{
			if (amount != 0)
			{
				final double newHp = amount + currentHp;
				effected.setCurrentHp(newHp, false);
				effected.broadcastStatusUpdate(effector);
			}
			
			SystemMessage sm;
			if (effector.getObjectId() != effected.getObjectId())
			{
				sm = new SystemMessage(SystemMessageId.S2_HP_HAS_BEEN_RECOVERED_BY_C1);
				sm.addString(effector.getName());
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.YOU_VE_RECOVERED_S1_HP);
			}
			
			sm.addInt((int) amount);
			effected.sendPacket(sm);
		}
		else
		{
			final double damage = -amount;
			effected.reduceCurrentHp(damage, effector, skill, false, false, false, false);
			effector.sendDamageMessage(effected, skill, (int) damage, false, false);
		}
	}
}
