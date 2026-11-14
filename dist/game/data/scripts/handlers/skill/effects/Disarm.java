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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code Disarm} effect logic.<br>
 * It manages the removal of weapons or shields from a target's inventory.
 * @author nBd
 */
public class Disarm extends AbstractEffect
{
	private static final Map<Integer, Integer> _disarmedPlayers = new ConcurrentHashMap<>();
	
	/**
	 * Creates a new instance of the {@code Disarm} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration for this effect.
	 */
	public Disarm(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code true} only if the target is a {@code Player}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the target is a player, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer();
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.DISARMED}.
	 * @return The bitmask representing the disarm effect flag.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.DISARMED.getMask();
	}
	
	/**
	 * This method handles the logic when a disarm effect begins.<br>
	 * It removes the weapon from the right hand of the target player.<br>
	 * The removed item is then tracked in the internal map.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the interaction.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		final Item itemToDisarm = player.getInventory().getPaperdollItem(Inventory.PAPERDOLL_RHAND);
		if (itemToDisarm == null)
		{
			return;
		}
		
		final BodyPart bodyPart = BodyPart.fromItem(itemToDisarm);
		player.getInventory().unEquipItemInBodySlot(bodyPart);
		
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(itemToDisarm);
		player.sendInventoryUpdate(iu);
		player.broadcastUserInfo();
		
		_disarmedPlayers.put(player.getObjectId(), itemToDisarm.getObjectId());
	}
	
	/**
	 * Handles the logic when a {@code Disarm} effect ends.<br>
	 * This method restores the item to the player's equipment.<br>
	 * It updates the inventory and sends a packet to the client.
	 * @param effector The creature that applied the effect.
	 * @param effected The creature that was affected by the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		final Integer itemObjectId = _disarmedPlayers.remove(effected.getObjectId());
		if (itemObjectId == null)
		{
			return;
		}
		
		final Item item = player.getInventory().getItemByObjectId(itemObjectId);
		if (item == null)
		{
			return;
		}
		
		player.getInventory().equipItem(item);
		final InventoryUpdate iu = new InventoryUpdate();
		iu.addModifiedItem(item);
		player.sendInventoryUpdate(iu);
	}
}
