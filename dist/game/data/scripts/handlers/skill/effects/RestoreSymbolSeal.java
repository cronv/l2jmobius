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
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code RestoreSymbolSeal} effect.<br>
 * This class manages how the seal is restored when triggered by a skill.<br>
 * It ensures that the correct status and properties are applied to the target.
 * @author NviX
 */
public class RestoreSymbolSeal extends AbstractEffect
{
	private final int _amount;
	private final StatModifierType _mode;
	
	/**
	 * Creates a new {@code RestoreSymbolSeal} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the amount and the modification mode for the seal.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public RestoreSymbolSeal(StatSet params)
	{
		_amount = params.getInt("amount", 0);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.PER);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this specific effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.RESTORE_SYMBOL_SEAL;
	}
	
	/**
	 * Checks if the effect is applied instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the instant effect of a skill to a target creature.<br>
	 * This method updates the symbol seal points for the player and refreshes their skills.<br>
	 * It also handles inventory updates if specific items are affected.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final int basicAmount = _amount;
		double amount = 0;
		switch (_mode)
		{
			case DIFF:
			{
				amount = Math.min(basicAmount, PlayerConfig.MAX_SYMBOL_SEAL_POINTS - player.getSymbolSealPoints());
				break;
			}
			case PER:
			{
				amount = Math.min((PlayerConfig.MAX_SYMBOL_SEAL_POINTS * basicAmount) / 100, PlayerConfig.MAX_SYMBOL_SEAL_POINTS - player.getSymbolSealPoints());
				break;
			}
		}
		
		if (amount > 0)
		{
			final double newSymbolSealPoints = amount + player.getSymbolSealPoints();
			player.setSymbolSealPoints((int) newSymbolSealPoints);
			player.updateSymbolSealSkills();
			player.sendSkillList();
			player.broadcastUserInfo();
			
			// Send item list to update Dye Powder with red icon in inventory.
			ThreadPool.schedule(() ->
			{
				final List<Item> items = new ArrayList<>();
				ITEMS: for (Item i : player.getInventory().getItems())
				{
					if (i.getTemplate().hasSkills())
					{
						for (ItemSkillHolder s : i.getTemplate().getAllSkills())
						{
							if (s.getSkill().hasEffectType(EffectType.RESTORE_SYMBOL_SEAL))
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
}
