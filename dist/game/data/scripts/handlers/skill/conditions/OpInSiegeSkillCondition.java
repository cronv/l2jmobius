/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.skill.conditions;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.managers.SiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.Siege;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player is currently participating in a siege.<br>
 * This condition validates whether the {@code Creature} is involved in an active {@code Siege} or {@code FortSiege}.
 * @author dontknowdontcare
 */
public class OpInSiegeSkillCondition implements ISkillCondition
{
	private final Set<Integer> _residenceIds = new HashSet<>();
	
	/**
	 * Creates a new condition to check if an action occurs during a siege.<br>
	 * It initializes the required residence IDs from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpInSiegeSkillCondition(StatSet params)
	{
		_residenceIds.addAll(params.getList("residenceIds", Integer.class));
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This method verifies if the {@code caster} meets the required siege conditions.
	 * @param caster The creature attempting to cast the skill.
	 * @param skill The skill being used.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the condition is met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		for (int id : _residenceIds)
		{
			if (valid(caster, id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the {@code caster} is currently inside a valid active siege zone.<br>
	 * This method verifies both fort and castle sieges based on the provided {@code id}.
	 * @param caster The {@code Creature} performing the action.
	 * @param id The unique identifier for the siege.
	 * @return {@code true} if the {@code caster} is inside an active siege zone, otherwise {@code false}.
	 */
	private boolean valid(Creature caster, int id)
	{
		final FortSiege fortSiege = FortSiegeManager.getInstance().getSiege(id);
		if (fortSiege != null)
		{
			return fortSiege.isInProgress() && fortSiege.getFort().getZone().isInsideZone(caster);
		}
		
		final Siege castleSiege = SiegeManager.getInstance().getSiege(id);
		if (castleSiege != null)
		{
			return castleSiege.isInProgress() && castleSiege.getCastle().getZone().isInsideZone(caster);
		}
		
		// TODO: Check for clan hall siege
		
		return false;
	}
}
