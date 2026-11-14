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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.taskmanagers.DecayTaskManager;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the resurrection effect.<br>
 * This class manages how a creature is revived when affected by this skill.<br>
 * It extends {@code AbstractEffect} to provide specific behavior for resurrection.
 * @author Adry_85
 */
public class Resurrection extends AbstractEffect
{
	private final int _power;
	private final int _hpPercent;
	private final int _mpPercent;
	private final int _cpPercent;
	
	/**
	 * Creates a new {@code Resurrection} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the power, health percentage, mana percentage, and cp percentage.
	 * @param params The {@code StatSet} containing the configuration values for this effect.
	 */
	public Resurrection(StatSet params)
	{
		_power = params.getInt("power", 0);
		_hpPercent = params.getInt("hpPercent", 0);
		_mpPercent = params.getInt("mpPercent", 0);
		_cpPercent = params.getInt("cpPercent", 0);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this resurrection effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.RESURRECTION;
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
	 * Executes the resurrection logic for a specific creature.<br>
	 * This method handles both player and non-player revival requests.<br>
	 * It determines whether to send a request or apply direct restoration.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the resurrection.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isPlayer())
		{
			final Player player = effected.asPlayer();
			if (!player.isResurrectionBlocked() && !player.isReviveRequested())
			{
				player.reviveRequest(effector.asPlayer(), effected.isPet(), _power, _hpPercent, _mpPercent, _cpPercent);
			}
		}
		else
		{
			DecayTaskManager.getInstance().cancel(effected);
			effected.doRevive(Formulas.calculateSkillResurrectRestorePercent(_power, effector));
		}
	}
}
