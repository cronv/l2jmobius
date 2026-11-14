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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.enums.UserInfoType;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.UserInfo;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code VitalityPointUp} skill effect.<br>
 * It manages the logic for increasing a creature's vitality points.
 * @author Adry_85, Mobius
 */
public class VitalityPointUp extends AbstractEffect
{
	private final int _value;
	
	/**
	 * Creates a new instance of the {@code VitalityPointUp} effect.<br>
	 * This constructor initializes the effect using provided statistics.<br>
	 * It extracts the value from the {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public VitalityPointUp(StatSet params)
	{
		_value = params.getInt("value", 0);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return the {@code EffectType} for this specific effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.VITALITY_POINT_UP;
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code true} only if the target is not {@code null}.<br>
	 * It also verifies that the target is an instance of {@code Player}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && effected.isPlayer();
	}
	
	/**
	 * Checks if this effect is applied instantly.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the effect immediately to a target.<br>
	 * This method updates the vitality points of the {@code effected} creature.<br>
	 * It also sends a network packet to update the user interface.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		player.updateVitalityPoints(_value, false, false);
		
		final UserInfo ui = new UserInfo(player);
		ui.addComponentType(UserInfoType.VITA_FAME);
		player.sendPacket(ui);
		
		// Send item list to update vitality items with red icons in inventory.
		ThreadPool.schedule(() ->
		{
			final List<Item> items = new ArrayList<>();
			ITEMS: for (Item i : player.getInventory().getItems())
			{
				if (i.getTemplate().hasSkills())
				{
					for (ItemSkillHolder s : i.getTemplate().getAllSkills())
					{
						if (s.getSkill().hasEffectType(EffectType.VITALITY_POINT_UP))
						{
							items.add(i);
							continue ITEMS;
						}
					}
				}
			}
			
			if (!items.isEmpty())
			{
				final InventoryUpdate iu = new InventoryUpdate();
				iu.addItems(items);
				player.sendInventoryUpdate(iu);
			}
		}, 1000);
	}
}
