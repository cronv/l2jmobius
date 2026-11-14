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
package handlers.actions.user;

import java.util.List;

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.enums.npc.AISkillScope;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the action where a player commands a {@code Summon} to attack.<br>
 * This class manages the logic for initiating an attack from a servitor.
 * @author Mobius
 */
public class ServitorAttack implements IActionUserHandler
{
	/**
	 * Handles the action where a player commands their servitors to attack.<br>
	 * It checks if the player owns any servitors and identifies a valid target.<br>
	 * Servitors will follow the player if the target is too far away.<br>
	 * Otherwise, they will attempt to attack the target based on the {@code ctrlPressed} state.
	 * @param player The {@code Player} who is performing the action.
	 * @param data The {@code ActionDataHolder} containing information about the action.
	 * @param ctrlPressed A boolean indicating if the control key is held down.
	 * @param shiftPressed A boolean indicating if the shift key is held down.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		if (!player.hasServitors())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
			return;
		}
		
		final WorldObject target = player.getTarget();
		if (target == null)
		{
			return;
		}
		
		final boolean targetOutOfRange = (player.calculateDistance3D(target) > 3000) || !GeoEngine.getInstance().canMoveToTarget(player, target);
		for (Summon summon : player.getServitors().values())
		{
			if (targetOutOfRange)
			{
				summon.getAI().setIntentionFollow(player);
			}
			else if (summon.canAttack(target, ctrlPressed))
			{
				final List<Skill> skills = summon.getTemplate().getAISkills(AISkillScope.ATTACK);
				if (skills.isEmpty())
				{
					summon.doAttack(target);
				}
				else
				{
					summon.setTarget(target);
					summon.useMagic(skills.get(0), null, ctrlPressed, shiftPressed);
				}
			}
		}
	}
	
	/**
	 * Checks if the current action is performed by a pet.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if this is a pet action.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
