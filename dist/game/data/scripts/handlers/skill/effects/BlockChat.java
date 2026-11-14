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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code BlockChat} effect.<br>
 * It prevents a creature from sending messages in the chat system.
 * @author BiggBoss
 */
public class BlockChat extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code BlockChat} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public BlockChat(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code true} only if the {@code effected} creature is not {@code null}.<br>
	 * It also verifies that the {@code effected} creature is a player.
	 * @param effector The creature casting the skill.
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
	 * Retrieves the bitmask for the chat block effect.<br>
	 * This value is used to identify specific behavior flags.
	 * @return The {@code long} mask representing the {@code EffectFlag}.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.CHAT_BLOCK.getMask();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It applies a chat ban punishment to the target creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		PunishmentManager.getInstance().startPunishment(new PunishmentTask(0, effected.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN, 0, "Chat banned bot report", "system", true));
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the chat ban from the affected creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who was affected by the skill.
	 * @param skill The skill used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		PunishmentManager.getInstance().stopPunishment(effected.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.CHAT_BAN);
	}
}
