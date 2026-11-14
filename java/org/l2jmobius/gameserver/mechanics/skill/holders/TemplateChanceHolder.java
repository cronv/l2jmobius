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
package org.l2jmobius.gameserver.mechanics.skill.holders;

/**
 * This class serves as a data container to hold a specific template ID and its associated chance.<br>
 * It is used to simplify passing these two values together throughout the skill mechanics system.
 * @author Nik
 */
public class TemplateChanceHolder
{
	private final int _templateId;
	private final int _minChance;
	private final int _maxChance;
	
	/**
	 * Creates a new {@code TemplateChanceHolder} instance.<br>
	 * This constructor initializes the template ID and the chance range.
	 * @param templateId The unique identifier for the template.
	 * @param minChance The minimum possible chance value.
	 * @param maxChance The maximum possible chance value.
	 */
	public TemplateChanceHolder(int templateId, int minChance, int maxChance)
	{
		_templateId = templateId;
		_minChance = minChance;
		_maxChance = maxChance;
	}
	
	/**
	 * Retrieves the unique identifier for the template.<br>
	 * This value is stored within the {@code TemplateChanceHolder}.
	 * @return The {@code int} value of the template ID.
	 */
	public int getTemplateId()
	{
		return _templateId;
	}
	
	/**
	 * Determines if a given value falls within the allowed range.<br>
	 * It checks if {@code chance} is between {@code _minChance} and {@code _maxChance}.
	 * @param chance The value to check against the limits.
	 * @return {@code true} if the value is within the valid range, otherwise {@code false}.
	 */
	public boolean calcChance(int chance)
	{
		return (_maxChance > chance) && (chance >= _minChance);
	}
	
	/**
	 * Returns a string representation of this {@code TemplateChanceHolder}.<br>
	 * It displays the template ID and the chance range.
	 * @return A formatted string containing the holder's data.
	 */
	@Override
	public String toString()
	{
		return "[TemplateId: " + _templateId + " minChance: " + _minChance + " maxChance: " + _minChance + "]";
	}
}
