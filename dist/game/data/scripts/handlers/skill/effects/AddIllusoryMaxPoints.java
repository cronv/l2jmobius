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

import org.l2jmobius.gameserver.config.IllusoryEquipmentConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.virtualItem.ExVirtualItemSystemBaseInfo;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Item Effect: Raise the illusory equipment point limit.<br>
 * The points a character may spend are capped by level, coupons lift that cap.
 * @author CostyKiller
 */
public class AddIllusoryMaxPoints extends AbstractEffect
{
	private final int _amount;
	
	public AddIllusoryMaxPoints(StatSet params)
	{
		_amount = params.getInt("amount", 0);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final PlayerVariables variables = player.getVariables();
		final int bonus = variables.getInt(PlayerVariables.ILLUSORY_POINTS_BONUS, 0);
		final int limit = IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_POINTS_BONUS_LIMIT;
		
		// A limit of zero lets coupons raise the cap without end, otherwise they only fill it up to the limit.
		final int raise = limit > 0 ? Math.min(_amount, limit - bonus) : _amount;
		if (raise <= 0)
		{
			player.sendMessage("Your illusory equipment point limit is already as high as it goes.");
			return;
		}
		
		variables.set(PlayerVariables.ILLUSORY_POINTS_BONUS, bonus + raise);
		player.sendMessage("Illusory equipment point limit raised by " + raise + ".");
		player.sendPacket(new ExVirtualItemSystemBaseInfo(player));
	}
}
