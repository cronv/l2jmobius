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
package handlers.skill.conditions;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.sql.SummonInfoTable;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a player meets the requirements to use a summon skill.<br>
 * It verifies that the player is allowed to summon a pet based on current game rules.
 * @author Sdw
 */
public class CanSummonPetSkillCondition implements ISkillCondition
{
	/**
	 * Creates a new instance of {@code CanSummonPetSkillCondition}.<br>
	 * This constructor initializes the condition using specific statistics.
	 * @param params The {@code StatSet} containing the required stats.
	 */
	public CanSummonPetSkillCondition(StatSet params)
	{
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This method validates various conditions such as protection status, active trades, and movement states.<br>
	 * It ensures that pets cannot be summoned during combat or while in restricted areas.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Player player = caster.asPlayer();
		if ((player == null) || player.isSpawnProtected() || player.isTeleportProtected())
		{
			return false;
		}
		
		boolean canSummon = true;
		if (PlayerConfig.RESTORE_PET_ON_RECONNECT && SummonInfoTable.getInstance().getPets().containsKey(player.getObjectId()))
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_MULTIPLE_PETS_AT_THE_SAME_TIME);
			canSummon = false;
		}
		else if (player.hasPet())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_MULTIPLE_PETS_AT_THE_SAME_TIME);
			canSummon = false;
		}
		else if ((player.getActiveTradeList() != null) || player.hasItemRequest() || player.isInStoreMode())
		{
			player.sendPacket(SystemMessageId.CANNOT_BE_SUMMONED_WHILE_TRADING);
			canSummon = false;
		}
		else if (AttackStanceTaskManager.getInstance().hasAttackStanceTask(player))
		{
			player.sendPacket(SystemMessageId.CANNOT_BE_SUMMONED_WHILE_IN_COMBAT);
			canSummon = false;
		}
		else if (player.isInAirShip())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SUMMON_A_SERVITOR_WHILE_MOUNTED);
			canSummon = false;
		}
		else if (player.isFlyingMounted() || player.isMounted() || player.getObserverMode().isActive() || player.isTeleporting())
		{
			canSummon = false;
		}
		
		return canSummon;
	}
}
