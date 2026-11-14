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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the harvesting effect.<br>
 * It manages how items are gathered or processed during a skill execution.
 * @author l3x, Zoey76
 */
public class Harvesting extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Harvesting} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Harvesting(StatSet params)
	{
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
	 * Executes the harvesting logic for a specific creature.<br>
	 * This method checks if the player is authorized to harvest the monster.<br>
	 * It handles item rewards and sends notifications to the player and their party.
	 * @param effector The {@code Creature} performing the action, which must be a {@code Player}.
	 * @param effected The {@code Creature} being harvested, which must be a dead {@code Monster}.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the harvesting result.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isMonster() || !effected.isDead())
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		final Monster monster = effected.asMonster();
		if (player.getObjectId() != monster.getSeederId())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_HARVEST);
		}
		else if (monster.isSeeded())
		{
			if (calcSuccess(player, monster))
			{
				final ItemHolder harvestedItem = monster.takeHarvest();
				if (harvestedItem != null)
				{
					// Add item
					player.getInventory().addItem(ItemProcessType.REWARD, harvestedItem.getId(), harvestedItem.getCount(), player, monster);
					
					// Send system msg
					SystemMessage sm = null;
					final long itemCount = item.getCount();
					if (itemCount == 1)
					{
						sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1);
						sm.addItemName(harvestedItem.getId());
					}
					else
					{
						sm = new SystemMessage(SystemMessageId.YOU_HAVE_OBTAINED_S1_X_S2);
						sm.addItemName(item.getId());
						sm.addLong(harvestedItem.getCount());
					}
					
					player.sendPacket(sm);
					
					// Send msg to party.
					final Party party = player.getParty();
					if (party != null)
					{
						final String playerName = player.getName();
						if (itemCount == 1)
						{
							sm = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S2_2);
							sm.addString(playerName);
							sm.addItemName(harvestedItem.getId());
						}
						else
						{
							sm = new SystemMessage(SystemMessageId.C1_HAS_OBTAINED_S3_X_S2);
							sm.addString(playerName);
							sm.addLong(harvestedItem.getCount());
							sm.addItemName(harvestedItem.getId());
						}
						
						party.broadcastToPartyMembers(player, sm);
					}
				}
			}
			else
			{
				player.sendPacket(SystemMessageId.THE_HARVEST_HAS_FAILED);
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.THE_HARVEST_FAILED_BECAUSE_THE_SEED_WAS_NOT_SOWN);
		}
	}
	
	/**
	 * Calculates the success rate of a harvesting action.<br>
	 * It compares the levels of the {@code Player} and the {@code Monster}.<br>
	 * A penalty is applied if the level difference exceeds 5.<br>
	 * The result is determined by a random roll against the final success chance.
	 * @param player The {@code Player} attempting the action.
	 * @param target The {@code Monster} being targeted.
	 * @return {@code true} if the harvest is successful, {@code false} otherwise.
	 */
	private static boolean calcSuccess(Player player, Monster target)
	{
		final int levelPlayer = player.getLevel();
		final int levelTarget = target.getLevel();
		
		int diff = (levelPlayer - levelTarget);
		if (diff < 0)
		{
			diff = -diff;
		}
		
		// Apply penalty, target <=> player levels.
		// 5% penalty for each level.
		int basicSuccess = 100;
		if (diff > 5)
		{
			basicSuccess -= (diff - 5) * 5;
		}
		
		// Success rate can't be less than 1%.
		if (basicSuccess < 1)
		{
			basicSuccess = 1;
		}
		
		return Rnd.get(99) < basicSuccess;
	}
}
