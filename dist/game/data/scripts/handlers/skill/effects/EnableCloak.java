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
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for applying the {@code EnableCloak} effect.<br>
 * It manages how a creature becomes cloaked when triggered by a skill.
 * @author Adry_85
 */
public class EnableCloak extends AbstractEffect
{
	/**
	 * Initializes a new {@code EnableCloak} effect.<br>
	 * This constructor sets up the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public EnableCloak(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It verifies that both creatures are not {@code null}.<br>
	 * The target must be a player character.
	 * @param effector The creature casting the skill.
	 * @param effected The creature receiving the skill.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effector != null) && (effected != null) && effected.isPlayer();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It enables the cloak slot status for the target player.
	 * @param effector The creature that triggers the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.asPlayer().getStat().setCloakSlotStatus(true);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the cloak status from the target creature.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} who received the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.asPlayer().getStat().setCloakSlotStatus(false);
	}
}
