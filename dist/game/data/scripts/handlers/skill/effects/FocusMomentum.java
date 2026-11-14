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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code FocusEnergy} effect.<br>
 * It manages the logic for applying focus momentum to a creature.
 * @author DS
 */
public class FocusMomentum extends AbstractEffect
{
	private final int _amount;
	private final int _maxCharges;
	
	/**
	 * Creates a new {@code FocusMomentum} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the amount and maximum charges based on provided parameters.
	 * @param params The {@code StatSet} containing configuration data for this effect.
	 */
	public FocusMomentum(StatSet params)
	{
		_amount = params.getInt("amount", 1);
		_maxCharges = params.getInt("maxCharges", 0);
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
	 * Applies the instant effect of this skill to a target.<br>
	 * This method updates the player's charges based on the defined amount.<br>
	 * It sends system messages if the charge limit is reached or increased.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final int currentCharges = player.getCharges();
		final int maxCharges = Math.min(_maxCharges, (int) effected.getStat().getValue(Stat.MAX_MOMENTUM));
		
		if (currentCharges >= maxCharges)
		{
			if (!skill.isTriggeredSkill())
			{
				player.sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
			}
			return;
		}
		
		final int newCharge = Math.min(currentCharges + _amount, maxCharges);
		
		player.setCharges(newCharge);
		
		if (newCharge == maxCharges)
		{
			player.sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
		}
		else
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_FORCE_HAS_INCREASED_TO_LEVEL_S1);
			sm.addInt(newCharge);
			player.sendPacket(sm);
		}
		
		player.sendPacket(new EtcStatusUpdate(player));
	}
}
