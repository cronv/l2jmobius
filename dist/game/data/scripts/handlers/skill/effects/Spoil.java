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
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code Spoil} skill effect.<br>
 * It manages the logic for applying loot drops to a target.<br>
 * It is an implementation of {@code AbstractEffect}.
 * @author _drunk_, Ahmed, Zoey76
 */
public class Spoil extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Spoil} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public Spoil(StatSet params)
	{
	}
	
	/**
	 * Determines if the skill effect succeeds.<br>
	 * It uses the standard magic success formula.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect succeeds, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcMagicSuccess(effector, effected, skill);
	}
	
	/**
	 * Checks if this effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a spoil action.<br>
	 * It checks if the target is a valid monster and has not been spoiled yet.<br>
	 * If successful, it links the effector to the monster and notifies the AI.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isMonster() || effected.isDead())
		{
			effector.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Monster target = effected.asMonster();
		if (target.isSpoiled())
		{
			effector.sendPacket(SystemMessageId.IT_HAS_ALREADY_BEEN_SPOILED);
			return;
		}
		
		target.setSpoilerObjectId(effector.getObjectId());
		effector.sendPacket(SystemMessageId.THE_SPOIL_CONDITION_HAS_BEEN_ACTIVATED);
		target.getAI().notifyActionAttacked(effector);
	}
}
