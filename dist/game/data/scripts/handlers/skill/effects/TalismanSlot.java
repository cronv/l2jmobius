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
 * This class handles the logic for the {@code TalismanSlot} skill effect.<br>
 * It manages how talisman slots are applied to an entity.
 * @author Adry_85
 */
public class TalismanSlot extends AbstractEffect
{
	private final int _slots;
	
	/**
	 * Creates a new {@code TalismanSlot} effect.<br>
	 * This constructor initializes the number of available slots from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public TalismanSlot(StatSet params)
	{
		_slots = params.getInt("slots", 0);
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It verifies that both creatures are not {@code null}.<br>
	 * It also ensures that the {@code effected} creature is a player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effector != null) && (effected != null) && effected.isPlayer();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It adds talisman slots to the player who was affected by the skill.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.asPlayer().getStat().addTalismanSlots(_slots);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the talisman slots from the player.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.asPlayer().getStat().addTalismanSlots(-_slots);
	}
}
