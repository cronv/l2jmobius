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
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.DamageReturn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code SphericBarrier} skill effect.<br>
 * It manages how the barrier interacts with damage and stat modifications.<br>
 * This class ensures that the protective properties of the sphere are applied correctly to the target.
 * @author Sdw
 */
public class SphericBarrier extends AbstractStatAddEffect
{
	/**
	 * Creates a new instance of {@code SphericBarrier}.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public SphericBarrier(StatSet params)
	{
		super(params, Stat.SPHERIC_BARRIER_RANGE);
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It sets up the necessary listeners on the target creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedEvent(event), this));
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the damage listener from the {@code effected} creature.<br>
	 * This ensures that the protection no longer applies after the skill expires.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill that triggered this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Handles the logic when a creature receives damage.<br>
	 * It checks if the attacker is within a specific distance.<br>
	 * If the attacker is too far away, it blocks all damage.<br>
	 * Otherwise, it allows the full amount of damage to pass through.
	 * @param event The {@code OnCreatureDamageReceived} event containing damage details.
	 * @return A {@code DamageReturn} object specifying if the damage was blocked or reduced.
	 */
	private DamageReturn onDamageReceivedEvent(OnCreatureDamageReceived event)
	{
		if (event.getAttacker().calculateDistance3D(event.getTarget()) > _amount)
		{
			return new DamageReturn(false, true, false, 0);
		}
		
		return new DamageReturn(false, false, false, event.getDamage());
	}
}
