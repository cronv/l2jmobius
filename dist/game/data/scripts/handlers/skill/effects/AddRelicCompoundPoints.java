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

import org.l2jmobius.gameserver.data.xml.RelicData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.RelicGrade;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.relics.ExRelicsPointInfo;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the effect of adding relic compound points to a player.<br>
 * This class processes the logic for increasing a character's relic points based on an item effect.
 * @author CostyKiller
 */
public class AddRelicCompoundPoints extends AbstractEffect
{
	private final int _relicGrade;
	private final int _amount;
	
	/**
	 * This constructor initializes the effect using a {@code StatSet}.<br>
	 * It extracts the relic grade and the amount from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public AddRelicCompoundPoints(StatSet params)
	{
		_relicGrade = params.getInt("relicGrade", 3);
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
	 * Applies the relic compound points effect instantly.<br>
	 * This method checks if the target is a {@code Player}.<br>
	 * It updates the player's compounding points based on the {@code _relicGrade}.<br>
	 * If the point limit is reached, the item is refunded to the player.
	 * @param effector The creature who triggered the effect.
	 * @param effected The creature who receives the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item associated with this effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final int pointsLimit = RelicData.getInstance().getGuaranteedCompoundMaxPoints();
		final int currentRelicCompoundingPointsCGrade = player.getVariables().getInt(PlayerVariables.RELICS_COMPOUNDING_POINTS_C_GRADE, 0);
		final int currentRelicCompoundingPointsBGrade = player.getVariables().getInt(PlayerVariables.RELICS_COMPOUNDING_POINTS_B_GRADE, 0);
		
		switch (_relicGrade)
		{
			case 3:
			{
				if (currentRelicCompoundingPointsCGrade == pointsLimit)
				{
					player.getInventory().addItem(ItemProcessType.REFUND, item.getId(), 1, player, player);
					player.sendMessage("You cannot have more than " + pointsLimit + " compounding points.");
					return;
				}
				player.getVariables().set(PlayerVariables.RELICS_COMPOUNDING_POINTS_C_GRADE, currentRelicCompoundingPointsCGrade + _amount);
				break;
			}
			case 4:
			{
				if (currentRelicCompoundingPointsBGrade == pointsLimit)
				{
					player.getInventory().addItem(ItemProcessType.REFUND, item.getId(), 1, player, player);
					player.sendMessage("You cannot have more than " + pointsLimit + " compounding points.");
					return;
				}
				player.getVariables().set(PlayerVariables.RELICS_COMPOUNDING_POINTS_B_GRADE, currentRelicCompoundingPointsBGrade + _amount);
				break;
			}
		}
		player.sendMessage("You have obtained " + _amount + " " + RelicGrade.values()[_relicGrade] + " compounding point(s).");
		player.sendPacket(new ExRelicsPointInfo(player));
	}
}
