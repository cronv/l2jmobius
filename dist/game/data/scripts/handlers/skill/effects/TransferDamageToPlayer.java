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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code TransferDamage} effect.<br>
 * It handles the logic for transferring damage from one entity to a player.
 * @author UnAfraid
 */
public class TransferDamageToPlayer extends AbstractStatAddEffect
{
	/**
	 * This method initializes the {@code TransferDamageToPlayer} effect.<br>
	 * It uses the provided {@code StatSet} to configure the initial values.<br>
	 * The effect is linked to the {@code Stat.TRANSFER_DAMAGE_TO_PLAYER} stat.
	 * @param params The set of statistics used to initialize the effect.
	 */
	public TransferDamageToPlayer(StatSet params)
	{
		super(params, Stat.TRANSFER_DAMAGE_TO_PLAYER);
	}
	
	/**
	 * Handles the logic when a skill effect ends.<br>
	 * This method clears the damage transfer status for playable targets.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} that was affected by the skill.
	 * @param skill The {@code Skill} being executed.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayable() && effector.isPlayer())
		{
			effected.asPlayable().setTransferDamageTo(null);
		}
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It checks if the {@code effected} creature is playable and the {@code effector} is a player.<br>
	 * If both conditions are met, it sets the damage transfer target on the {@code effected} creature.
	 * @param effector The creature that initiated the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayable() && effector.isPlayer())
		{
			effected.asPlayable().setTransferDamageTo(effector.asPlayer());
		}
	}
}
