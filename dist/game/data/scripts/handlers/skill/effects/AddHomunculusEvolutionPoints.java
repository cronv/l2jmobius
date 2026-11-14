/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.skill.effects;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExHomunculusPointInfo;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the permanent increase of Homunculus evolution points.<br>
 * It is triggered as an item effect to modify a player's progression stats.
 * @author Tanatos
 */
public class AddHomunculusEvolutionPoints extends AbstractEffect
{
	private final int _amount;
	
	/**
	 * This constructor initializes the effect with specific point values.<br>
	 * It extracts the evolution amount from the provided {@code StatSet}.<br>
	 * The default value is set to {@code 0} if no amount is found.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public AddHomunculusEvolutionPoints(StatSet params)
	{
		_amount = params.getInt("amount", 0);
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
	 * Applies the effect immediately to a target creature.<br>
	 * This method checks if the target is a {@code Player}.<br>
	 * It adds evolution points and sends notification packets.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with this effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final int upgradePoints = player.getVariables().getInt(PlayerVariables.HOMUNCULUS_EVOLUTION_POINTS, 0) + _amount;
		player.getVariables().set(PlayerVariables.HOMUNCULUS_EVOLUTION_POINTS, upgradePoints);
		player.sendPacket(SystemMessageId.YOU_HAVE_ACQUIRED_UPGRADE_POINTS);
		player.sendPacket(new ExHomunculusPointInfo(player));
	}
}
