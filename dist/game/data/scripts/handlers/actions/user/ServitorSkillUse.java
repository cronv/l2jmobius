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
package handlers.actions.user;

import org.l2jmobius.gameserver.data.holders.ActionDataHolder;
import org.l2jmobius.gameserver.data.xml.PetSkillData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.handler.IActionUserHandler;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles the logic for a player using a skill on their servitor.<br>
 * This class processes the interaction between a {@code Player} and a {@code Summon}.
 * @author Nik
 */
public class ServitorSkillUse implements IActionUserHandler
{
	/**
	 * Handles the logic for a player using a skill with their servitor.<br>
	 * It checks if the player has an active and obedient servitor.<br>
	 * If valid, it commands all servitors to use the specified skill on the current target.
	 * @param player The {@code Player} who is performing the action.
	 * @param data The {@code ActionDataHolder} containing the skill option ID.
	 * @param ctrlPressed A boolean indicating if the {@code Ctrl} key is held down.
	 * @param shiftPressed A boolean indicating if the {@code Shift} key is held down.
	 */
	@Override
	public void onAction(Player player, ActionDataHolder data, boolean ctrlPressed, boolean shiftPressed)
	{
		final Summon summon = player.getAnyServitor();
		if ((summon == null) || !summon.isServitor())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_SERVITOR);
			return;
		}
		
		player.getServitors().values().forEach(servitor ->
		{
			if (summon.isBetrayed())
			{
				player.sendPacket(SystemMessageId.YOUR_PET_SERVITOR_IS_UNRESPONSIVE_AND_WILL_NOT_OBEY_ANY_ORDERS);
				return;
			}
			
			final int skillLevel = PetSkillData.getInstance().getAvailableLevel(servitor, data.getOptionId());
			if (skillLevel > 0)
			{
				final Skill skill = SkillData.getInstance().getSkill(data.getOptionId(), skillLevel);
				if (skill != null)
				{
					servitor.setTarget(player.getTarget());
					servitor.useMagic(skill, null, (skill.getTargetType() == TargetType.SELF) || ctrlPressed, shiftPressed);
				}
			}
		});
	}
	
	/**
	 * Checks if the current action is related to a pet.<br>
	 * This method returns {@code true} for this specific handler.
	 * @return {@code true} if the action involves a pet, otherwise {@code false}.
	 */
	@Override
	public boolean isPetAction()
	{
		return true;
	}
}
