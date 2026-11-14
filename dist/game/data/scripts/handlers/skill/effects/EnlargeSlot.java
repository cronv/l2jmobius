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
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StorageType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the effect of increasing a character's equipment slot capacity.<br>
 * This class manages how {@code Stat} values are modified to allow for more items.<br>
 * It is triggered by specific skills that provide this buff.
 * @author Sdw, CostyKiller
 */
public class EnlargeSlot extends AbstractEffect
{
	private final StorageType _type;
	private final double _amount;
	
	/**
	 * Creates a new {@code EnlargeSlot} effect.<br>
	 * This constructor initializes the slot expansion properties.<br>
	 * It extracts values from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the amount and type for this effect.
	 */
	public EnlargeSlot(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
		_type = params.getEnum("type", StorageType.class, StorageType.INVENTORY_NORMAL);
	}
	
	/**
	 * Applies a stat increase to the target creature.<br>
	 * This method updates the specific storage capacity based on the effect type.<br>
	 * The client is refreshed by {@code PlayerStat} once the recalculated limits differ.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		Stat stat = Stat.INVENTORY_NORMAL;
		
		switch (_type)
		{
			case TRADE_BUY:
			{
				stat = Stat.TRADE_BUY;
				break;
			}
			case TRADE_SELL:
			{
				stat = Stat.TRADE_SELL;
				break;
			}
			case RECIPE_DWARVEN:
			{
				stat = Stat.RECIPE_DWARVEN;
				break;
			}
			case RECIPE_COMMON:
			{
				stat = Stat.RECIPE_COMMON;
				break;
			}
			case STORAGE_PRIVATE:
			{
				stat = Stat.STORAGE_PRIVATE;
				break;
			}
		}
		
		effected.getStat().mergeAdd(stat, _amount);
	}
}
