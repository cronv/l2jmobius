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

import java.util.List;

import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class serves as a template for handling item-based skills.<br>
 * It provides the base logic for executing skills associated with specific items.
 * @author Zoey76, Skache, Mobius
 */
public class ItemSkillsTemplate implements IItemHandler
{
	/**
	 * Handles the logic for using an item by a playable entity.<br>
	 * It checks permissions, reuse delays, and skill conditions before execution.
	 * @param playable The {@code Playable} entity attempting to use the item.
	 * @param item The {@code Item} instance being used.
	 * @param forceUse A boolean indicating if the use should be forced.
	 * @return {@code true} if the item was successfully used, otherwise {@code false}.
	 */
	@Override
	public boolean onItemUse(Playable playable, Item item, boolean forceUse)
	{
		if (!playable.isPlayer() && !playable.isPet())
		{
			return false;
		}
		
		// Pets can use items only when they are tradable.
		if (playable.isPet() && !item.isTradeable())
		{
			playable.sendPacket(SystemMessageId.THIS_ITEM_CANNOT_BE_TRANSFERRED_TO_A_PET);
			return false;
		}
		
		// Verify that item is not under reuse.
		if (!checkReuse(playable, null, item))
		{
			return false;
		}
		
		final List<ItemSkillHolder> skills = item.getTemplate().getSkills(ItemSkillType.NORMAL);
		if (skills == null)
		{
			LOGGER.info("Item " + item + " does not have registered any skill for handler.");
			return false;
		}
		
		boolean hasConsumeSkill = false;
		for (SkillHolder skillInfo : skills)
		{
			if ((skillInfo != null) && (skillInfo.getSkill() != null) && (skillInfo.getSkill().getItemConsumeId() > 0))
			{
				hasConsumeSkill = true;
			}
		}
		
		boolean successfulUse = false;
		boolean consumed = false;
		for (SkillHolder skillInfo : skills)
		{
			if (skillInfo == null)
			{
				continue;
			}
			
			final Skill itemSkill = skillInfo.getSkill();
			if (itemSkill != null)
			{
				if (itemSkill.hasEffectType(EffectType.EXTRACT_ITEM))
				{
					final Player player = playable.asPlayer();
					if ((player != null) && !player.isInventoryUnder80(false))
					{
						player.sendPacket(SystemMessageId.YOU_HAVE_EXCEEDED_THE_ITEM_OWNERSHIP_LIMIT_AND_YOU_CANNOT_TAKE_THE_ITEM_CHECK_ITEM_OWNERSHIP_TIME_LIMITS_FOR_THE_INVENTORY_PLEASE);
						return false;
					}
				}
				
				final WorldObject target = playable.getTarget();
				if (!itemSkill.hasEffectType(EffectType.SUMMON_PET) && !itemSkill.checkCondition(playable, target, true))
				{
					continue;
				}
				
				// Verify that skill is not under reuse.
				if (!checkReuse(playable, itemSkill, item))
				{
					continue;
				}
				
				if (playable.isSkillDisabled(itemSkill))
				{
					continue;
				}
				
				if (!item.isPotion() && !item.isElixir() && !item.isScroll() && playable.isCastingNow())
				{
					continue;
				}
				
				if (playable.isPlayer() && itemSkill.hasEffectType(EffectType.SUMMON_PET))
				{
					playable.doCast(itemSkill);
					successfulUse = true;
				}
				else if (itemSkill.isWithoutAction() || item.getTemplate().hasImmediateEffect() || item.getTemplate().hasExImmediateEffect())
				{
					// An instant effect cannot be undone, so the item is taken first.
					if (!consumed && checkConsume(item, hasConsumeSkill))
					{
						if (!playable.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, playable, false))
						{
							playable.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
							return false;
						}
						
						consumed = true;
					}
					
					SkillCaster.triggerCast(playable, itemSkill.getTargetType() == TargetType.OTHERS ? target : null, itemSkill, item, false);
					successfulUse = true;
				}
				else
				{
					playable.getAI().setIntentionIdle();
					if (playable.useMagic(itemSkill, item, forceUse, false))
					{
						successfulUse = true;
					}
					else
					{
						continue;
					}
				}
				
				// Send message to the master only once the pet actually uses the skill.
				if (playable.isPet())
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_PET_USES_S1);
					sm.addSkillName(itemSkill);
					playable.sendPacket(sm);
				}
			}
		}
		
		if (successfulUse && !consumed && checkConsume(item, hasConsumeSkill) && !playable.destroyItem(ItemProcessType.NONE, item.getObjectId(), 1, playable, false))
		{
			playable.sendPacket(SystemMessageId.INCORRECT_ITEM_COUNT_2);
			return false;
		}
		
		return successfulUse;
	}
	
	/**
	 * Determines if an {@code Item} should be consumed after use.<br>
	 * This method checks the item's default action and effect types.<br>
	 * It evaluates whether the item is a one-time use object.
	 * @param item The {@code Item} instance to check.
	 * @param hasConsumeSkill A boolean indicating if the skill allows consumption.
	 * @return {@code true} if the item should be removed, {@code false} otherwise.
	 */
	private boolean checkConsume(Item item, boolean hasConsumeSkill)
	{
		switch (item.getTemplate().getDefaultAction())
		{
			case CAPSULE:
			case SKILL_REDUCE:
			{
				if (!hasConsumeSkill && item.getTemplate().hasImmediateEffect())
				{
					return true;
				}
				break;
			}
			case SKILL_REDUCE_ON_SKILL_SUCCESS:
			{
				return false;
			}
		}
		
		return hasConsumeSkill;
	}
	
	/**
	 * Checks if a skill or item can be used again based on the remaining reuse time.<br>
	 * If the action is not ready, it sends a message to the {@code Player}.
	 * @param playable The entity attempting to use the skill or item.
	 * @param skill The {@code Skill} being checked for reuse.
	 * @param item The {@code Item} being checked for reuse.
	 * @return {@code true} if the action is ready to be used, {@code false} otherwise.
	 */
	private boolean checkReuse(Playable playable, Skill skill, Item item)
	{
		final long remainingTime = (skill != null) ? playable.getSkillRemainingReuseTime(skill.getReuseHashCode()) : playable.getItemRemainingReuseTime(item.getObjectId());
		final boolean isAvailable = remainingTime <= 0;
		if (playable.isPlayer() && !isAvailable)
		{
			final long roundedTime = TimeUtil.roundUpToSecond(remainingTime);
			final int hours = (int) (roundedTime / 3600000);
			final int minutes = (int) (roundedTime % 3600000) / 60000;
			final int seconds = (int) ((roundedTime / 1000) % 60);
			SystemMessage sm = null;
			if (hours > 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_AGAIN_IN_S2_H_S3_MIN_S4_SEC);
				if ((skill == null) || skill.isStatic())
				{
					sm.addItemName(item);
				}
				else
				{
					sm.addSkillName(skill);
				}
				
				sm.addInt(hours);
				sm.addInt(minutes);
			}
			else if (minutes > 0)
			{
				sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_AGAIN_IN_S2_MIN_S3_SEC);
				if ((skill == null) || skill.isStatic())
				{
					sm.addItemName(item);
				}
				else
				{
					sm.addSkillName(skill);
				}
				
				sm.addInt(minutes);
			}
			else
			{
				sm = new SystemMessage(SystemMessageId.S1_WILL_BE_AVAILABLE_AGAIN_IN_S2_SEC);
				if ((skill == null) || skill.isStatic())
				{
					sm.addItemName(item);
				}
				else
				{
					sm.addSkillName(skill);
				}
			}
			
			sm.addInt(seconds);
			playable.sendPacket(sm);
		}
		
		return isAvailable;
	}
}
