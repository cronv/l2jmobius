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
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Team;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCTeam;

/**
 * Represents a tower structure within the Underground Coliseum.<br>
 * This entity serves as a stationary point for team-based mechanics and objectives.
 */
public class UCTower extends Folk
{
	private UCTeam _team;
	
	/**
	 * Creates a new {@code UCTower} instance.<br>
	 * This constructor initializes the tower with a specific team and template.
	 * @param team The {@code UCTeam} that this tower belongs to.
	 * @param template The {@code NpcTemplate} used to define the tower's properties.
	 */
	public UCTower(UCTeam team, NpcTemplate template)
	{
		super(template);
		_team = team;
	}
	
	/**
	 * Checks if this tower can be targeted by an attack.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the tower is attackable, otherwise {@code false}.
	 */
	@Override
	public boolean canBeAttacked()
	{
		return true;
	}
	
	/**
	 * Checks if a specific creature can be targeted for an auto-attack.<br>
	 * This method currently always returns {@code true}.
	 * @param creature The {@code Creature} to check.
	 * @return {@code true} if the creature can be attacked, otherwise {@code false}.
	 */
	@Override
	public boolean isAutoAttackable(Creature creature)
	{
		return true;
	}
	
	/**
	 * Reduces the current health of this tower based on an attack.<br>
	 * It checks if the attacker is on a different team before applying damage.<br>
	 * If the damage exceeds the remaining health, the tower dies.
	 * @param amountValue The amount of damage to subtract from the health.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param skill The {@code Skill} used for the attack.
	 * @param isDOT Whether the damage is a Damage Over Time effect.
	 * @param directlyToHp Whether the damage applies directly to health.
	 * @param critical Whether the hit is a critical strike.
	 * @param reflect Whether the damage should be reflected back to the attacker.
	 */
	@Override
	public void reduceCurrentHp(double amountValue, Creature attacker, Skill skill, boolean isDOT, boolean directlyToHp, boolean critical, boolean reflect)
	{
		if (attacker.getTeam() == getTeam())
		{
			return;
		}
		
		final double currentHp = getStatus().getCurrentHp();
		if (amountValue < currentHp)
		{
			getStatus().setCurrentHp(currentHp - amountValue);
		}
		else
		{
			doDie(attacker);
		}
	}
	
	/**
	 * Handles the logic for when this tower is defeated.<br>
	 * It checks if the death is valid and removes the tower from its team.
	 * @param killer The {@code Creature} that dealt the final blow.
	 * @return {@code true} if the tower died successfully, otherwise {@code false}.
	 */
	@Override
	public boolean doDie(Creature killer)
	{
		if (!super.doDie(killer))
		{
			return false;
		}
		
		if (_team != null)
		{
			_team.deleteTower();
			_team = null;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the team associated with this tower.<br>
	 * It maps the internal {@code UCTeam} index to a {@code Team} type.
	 * @return The {@code Team} of the tower.
	 */
	@Override
	public Team getTeam()
	{
		return _team.getIndex() == 1 ? Team.BLUE : Team.RED;
	}
}
