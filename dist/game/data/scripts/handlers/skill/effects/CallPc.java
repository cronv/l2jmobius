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

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.SummonRequestHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ConfirmDlg;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code CallPc} skill effect.<br>
 * It manages the summoning of a player character to a specific location.
 * @author Adry_85, Mobius
 */
public class CallPc extends AbstractEffect
{
	private final int _itemId;
	private final int _itemCount;
	
	/**
	 * Creates a new {@code CallPc} effect instance.<br>
	 * This constructor initializes the required item data from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public CallPc(StatSet params)
	{
		_itemId = params.getInt("itemId", 0);
		_itemCount = params.getInt("itemCount", 0);
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a Call Pc skill.<br>
	 * This method handles summoning requests or teleporting targets based on the target type.<br>
	 * It checks for required items and sends confirmation dialogs when necessary.
	 * @param effector The {@code Creature} who is casting the skill.
	 * @param effected The {@code Creature} who is being targeted by the skill.
	 * @param skill The {@code Skill} object used to perform the action.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector == effected)
		{
			return;
		}
		
		final Player target = effected.asPlayer();
		final Player player = effector.asPlayer();
		if (player != null)
		{
			if (SummonRequestHolder.checkSummonTargetStatus(target, player))
			{
				if ((_itemId != 0) && (_itemCount != 0))
				{
					if (target.getInventory().getInventoryItemCount(_itemId, 0) < _itemCount)
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.S1_IS_REQUIRED_FOR_SUMMONING);
						sm.addItemName(_itemId);
						target.sendPacket(sm);
						return;
					}
					
					target.getInventory().destroyItemByItemId(null, _itemId, _itemCount, player, target);
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_DISAPPEARED);
					sm.addItemName(_itemId);
					target.sendPacket(sm);
				}
				
				target.addScript(new SummonRequestHolder(player));
				
				final ConfirmDlg confirm = new ConfirmDlg(SystemMessageId.C1_WANTS_TO_SUMMON_YOU_TO_S2_ACCEPT.getId());
				confirm.getSystemMessage().addString(player.getName());
				confirm.getSystemMessage().addZoneName(player.getX(), player.getY(), player.getZ());
				confirm.addTime(30000);
				confirm.addRequesterId(player.getObjectId());
				target.sendPacket(confirm);
			}
		}
		else if (target != null)
		{
			if (skill.getTargetType() == TargetType.ENEMY)
			{
				effected.abortCast();
				effected.abortAttack();
				effected.stopMove(null);
				effected.sendPacket(new FlyToLocation(effected, effector, FlyType.DUMMY, 0, 0, 0));
				effected.setLocation(effector.getLocation());
			}
			else
			{
				final WorldObject previousTarget = target.getTarget();
				target.teleToLocation(effector);
				target.setTarget(previousTarget);
			}
		}
	}
}
