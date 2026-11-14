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
package handlers.items;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.ArenaParticipantsHolder;
import org.l2jmobius.gameserver.entity.actor.instance.Block;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles special items that trigger specific events when used.<br>
 * It allows for custom logic to be executed by a {@code Player} or other entities.<br>
 * It implements the {@code IItemHandler} interface to process item interactions.
 */
public class EventItem implements IItemHandler
{
	/**
	 * Handles the logic for using specific event items.<br>
	 * This method checks if the {@code playable} entity is a {@code Player}.<br>
	 * It processes special items like Handy's Block Checker.
	 * @param playable The entity attempting to use the item.
	 * @param item The {@code Item} instance being used.
	 * @param forceUse A boolean indicating if the use is forced.
	 * @return {@code true} if the item was successfully used, otherwise {@code false}.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		boolean used = false;
		
		final Player player = playable.asPlayer();
		final int itemId = item.getId();
		switch (itemId)
		{
			case 13787: // Handy's Block Checker Bond
			{
				used = useBlockCheckerItem(player, item);
				break;
			}
			case 13788: // Handy's Block Checker Land Mine
			{
				used = useBlockCheckerItem(player, item);
				break;
			}
			default:
			{
				LOGGER.warning("EventItemHandler: Item with id: " + itemId + " is not handled");
				break;
			}
		}
		
		return used;
	}
	
	/**
	 * Checks if the item can be used as a block checker.<br>
	 * It verifies the player is in a valid arena and applies effects to enemies.
	 * @param caster The {@code Player} using the item.
	 * @param item The {@code Item} being used.
	 * @return {@code true} if the effect was successfully applied, {@code false} otherwise.
	 */
	private boolean useBlockCheckerItem(Player caster, Item item)
	{
		final int blockCheckerArena = caster.getBlockCheckerArena();
		if (blockCheckerArena == -1)
		{
			final SystemMessage msg = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			msg.addItemName(item);
			caster.sendPacket(msg);
			return false;
		}
		
		final Skill sk = item.getEtcItem().getSkills(ItemSkillType.NORMAL).get(0).getSkill();
		if (sk == null)
		{
			return false;
		}
		
		if (!caster.destroyItem(null, item, 1, caster, true))
		{
			return false;
		}
		
		final Block block = (Block) caster.getTarget();
		final ArenaParticipantsHolder holder = HandysBlockCheckerManager.getInstance().getHolder(blockCheckerArena);
		if (holder != null)
		{
			final int team = holder.getPlayerTeam(caster);
			World.forEachVisibleObjectInRange(block, Player.class, sk.getEffectRange(), pc ->
			{
				final int enemyTeam = holder.getPlayerTeam(pc);
				if ((enemyTeam != -1) && (enemyTeam != team))
				{
					sk.applyEffects(caster, pc);
				}
			});
			
			return true;
		}
		
		LOGGER.warning("Char: " + caster.getName() + "[" + caster.getObjectId() + "] has unknown block checker arena");
		return false;
	}
}
