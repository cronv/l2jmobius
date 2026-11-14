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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the special effects for resurrection skills.<br>
 * This class manages how the {@code Resurrection} mechanic behaves when triggered.<br>
 * It extends {@code AbstractEffect} to provide specific logic for this skill type.
 * @author Zealar
 */
public class ResurrectionSpecial extends AbstractEffect
{
	private final int _power;
	private final int _hpPercent;
	private final int _mpPercent;
	private final int _cpPercent;
	private final Set<Integer> _instanceId;
	
	/**
	 * Creates a new {@code ResurrectionSpecial} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets power, health, mana, and cp percentages.<br>
	 * It also parses instance IDs if they are provided.
	 * @param params The {@code StatSet} containing all required configuration values.
	 */
	public ResurrectionSpecial(StatSet params)
	{
		_power = params.getInt("power", 0);
		_hpPercent = params.getInt("hpPercent", 0);
		_mpPercent = params.getInt("mpPercent", 0);
		_cpPercent = params.getInt("cpPercent", 0);
		
		final String instanceIds = params.getString("instanceId", null);
		if ((instanceIds != null) && !instanceIds.isEmpty())
		{
			_instanceId = new HashSet<>();
			for (String id : instanceIds.split(";"))
			{
				_instanceId.add(Integer.parseInt(id));
			}
		}
		else
		{
			_instanceId = Collections.<Integer> emptySet();
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this special effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.RESURRECTION_SPECIAL;
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * This value identifies specific properties of the {@code ResurrectionSpecial} effect.
	 * @return The bitmask associated with {@code EffectFlag.RESURRECTION_SPECIAL}.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.RESURRECTION_SPECIAL.getMask();
	}
	
	/**
	 * This method is called when the effect expires.<br>
	 * It handles the logic for reviving a player or their pet.<br>
	 * The action only occurs if specific conditions are met.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (!effected.isPlayer() && !effected.isPet())
		{
			return;
		}
		
		final Player target = effected.asPlayer();
		if (target.isInOlympiadMode())
		{
			return;
		}
		
		final Player caster = effector.asPlayer();
		final Instance instance = caster.getInstanceWorld();
		if (!_instanceId.isEmpty() && ((instance == null) || !_instanceId.contains(instance.getTemplateId())))
		{
			return;
		}
		
		if (effected.isPlayer())
		{
			target.reviveRequest(caster, false, _power, _hpPercent, _mpPercent, _cpPercent);
		}
		else if (effected.isPet())
		{
			target.reviveRequest(target, true, _power, _hpPercent, _mpPercent, _cpPercent);
		}
	}
}
