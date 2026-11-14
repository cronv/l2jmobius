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
 * This class handles the logic for a mana healing effect based on a percentage.<br>
 * It calculates and applies the heal amount to a {@code Creature} based on their current mana.
 * @author UnAfraid
 */
public class ManaHealPercent extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code ManaHealPercent} effect.<br>
	 * This constructor initializes the healing power from the provided stats.<br>
	 * It ensures that the {@code power} key is used instead of {@code amount}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ManaHealPercent(StatSet params)
	{
		_power = params.getDouble("power", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this specific effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MANAHEAL_PERCENT;
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method returns {@code true} for this specific effect type.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the mana heal effect immediately to a target.<br>
	 * This method calculates the amount based on the power of the effect.<br>
	 * It also checks for potion bonuses and prevents overhealing.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the mana heal.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} used, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || effected.isDead() || effected.isDoor() || effected.isMpBlocked())
		{
			return;
		}
		
		double amount = 0;
		final double power = _power;
		final boolean full = (power == 100.0);
		
		amount = full ? effected.getMaxMp() : (effected.getMaxMp() * power) / 100.0;
		if ((item != null) && (item.isPotion() || item.isElixir()))
		{
			amount += effected.getStat().getValue(Stat.ADDITIONAL_POTION_MP, 0);
		}
		
		// Prevents overheal
		final double currentMp = effected.getCurrentMp();
		amount = Math.min(amount, Math.max(0, effected.getMaxRecoverableMp() - currentMp));
		if (amount != 0)
		{
			final double newMp = amount + currentMp;
			effected.setCurrentMp(newMp, false);
			effected.broadcastStatusUpdate(effector);
		}
		
		SystemMessage sm;
		if (effector.getObjectId() != effected.getObjectId())
		{
			sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOVERED_S2_MP_WITH_C1_S_HELP);
			sm.addString(effector.getName());
		}
		else
		{
			sm = new SystemMessage(SystemMessageId.S1_MP_HAS_BEEN_RESTORED);
		}
		
		sm.addInt((int) amount);
		effected.sendPacket(sm);
	}
}
