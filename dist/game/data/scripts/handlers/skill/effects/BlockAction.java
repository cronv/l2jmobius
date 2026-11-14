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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code BlockAction} skill effect.<br>
 * It manages how actions are restricted or blocked when this effect is active on a creature.
 * @author BiggBoss
 */
public class BlockAction extends AbstractEffect
{
	private final Set<Integer> _blockedActions = new HashSet<>();
	
	/**
	 * Creates a new {@code BlockAction} effect.<br>
	 * This constructor initializes the blocked actions from the provided data.<br>
	 * It parses a comma-separated list of integers from the {@code params} object.
	 * @param params The {@code StatSet} containing the configuration for this effect.
	 */
	public BlockAction(StatSet params)
	{
		final String[] actions = params.getString("blockedActions").split(",");
		for (String action : actions)
		{
			_blockedActions.add(Integer.parseInt(action));
		}
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code true} only if the {@code effected} creature is not {@code null}.<br>
	 * It also verifies that the {@code effected} creature is a player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the target is a valid player, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && effected.isPlayer();
	}
	
	/**
	 * Checks if a specific action is currently blocked.<br>
	 * This method verifies the provided {@code id} against the internal set of blocked actions.
	 * @param id The unique identifier for the action to check.
	 * @return {@code true} if the action is not blocked, and {@code false} otherwise.
	 */
	@Override
	public boolean checkCondition(Object id)
	{
		return !_blockedActions.contains(id);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It checks if specific actions are blocked by the current effect.<br>
	 * If they are blocked, it applies a punishment to the {@code effected} creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_blockedActions.contains(BotReportTable.PARTY_ACTION_BLOCK_ID))
		{
			PunishmentManager.getInstance().startPunishment(new PunishmentTask(0, effected.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN, 0, "block action debuff", "system", true));
		}
		
		if (_blockedActions.contains(BotReportTable.CHAT_BLOCK_ID))
		{
			PunishmentManager.getInstance().startPunishment(new PunishmentTask(0, effected.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN, 0, "block action debuff", "system", true));
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It checks if any specific actions were blocked during the effect.<br>
	 * If they were, it removes the corresponding punishments from the {@code effected} creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who was affected by the skill.
	 * @param skill The skill used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (_blockedActions.contains(BotReportTable.PARTY_ACTION_BLOCK_ID))
		{
			PunishmentManager.getInstance().stopPunishment(effected.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.PARTY_BAN);
		}
		
		if (_blockedActions.contains(BotReportTable.CHAT_BLOCK_ID))
		{
			PunishmentManager.getInstance().stopPunishment(effected.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN);
		}
	}
}
