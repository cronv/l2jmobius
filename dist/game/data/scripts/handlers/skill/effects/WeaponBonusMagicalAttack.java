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
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the application of a magical attack bonus from a weapon.<br>
 * This effect modifies the {@code Stat} values for the affected creature.
 * @author Sero
 */
public class WeaponBonusMagicalAttack extends AbstractStatAddEffect
{
	/**
	 * Creates a new {@code WeaponBonusMagicalAttack} effect.<br>
	 * This constructor initializes the effect using the provided stats.<br>
	 * It applies changes to the {@code Stat.WEAPON_BONUS_MAGIC_ATTACK} stat.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public WeaponBonusMagicalAttack(StatSet params)
	{
		super(params, Stat.WEAPON_BONUS_MAGIC_ATTACK);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It updates the inventory for the player receiving the effect.<br>
	 * It ensures the weapon in the right hand is refreshed visually.
	 * @param effector The creature that triggers the effect.
	 * @param effected The creature that receives the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon == null)
		{
			return;
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(weapon);
		player.sendInventoryUpdate(iu);
		player.broadcastUserInfo();
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It updates the inventory for the player who received the effect.<br>
	 * It refreshes the weapon information in the right hand slot.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item weapon = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (weapon == null)
		{
			return;
		}
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(weapon);
		player.sendInventoryUpdate(iu);
		player.broadcastUserInfo();
	}
}
