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
import org.l2jmobius.gameserver.entity.actor.instance.AirShip;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code RefuelAirship} skill effect.<br>
 * It manages the refueling process for an {@code AirShip} entity.
 * @author Adry_85
 */
public class RefuelAirship extends AbstractEffect
{
	private final int _value;
	
	/**
	 * Creates a new instance of the {@code RefuelAirship} effect.<br>
	 * This constructor initializes the effect using provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public RefuelAirship(StatSet params)
	{
		_value = params.getInt("value", 0);
	}
	
	/**
	 * Returns the type of effect associated with this instance.<br>
	 * This is used to identify the {@code RefuelAirship} action.
	 * @return The {@code EffectType} for refuelling an airship.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.REFUEL_AIRSHIP;
	}
	
	/**
	 * Checks if this effect is applied instantly.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the instant effect of a skill to an entity.<br>
	 * This method updates the fuel level of the {@code AirShip} owned by the {@code effector}.<br>
	 * It also refreshes any abnormal visual effects on the ship.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final AirShip ship = effector.asPlayer().getAirShip();
		ship.setFuel(ship.getFuel() + _value);
		ship.updateAbnormalVisualEffects();
	}
}
