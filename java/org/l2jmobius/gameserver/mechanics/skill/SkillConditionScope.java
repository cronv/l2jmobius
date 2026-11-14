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
package org.l2jmobius.gameserver.mechanics.skill;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Defines the spatial scope for skill conditions.<br>
 * It determines which entities are affected by a specific condition check.<br>
 * This helps in filtering targets based on their proximity or relationship to the caster.
 * @author NosBit
 */
public enum SkillConditionScope
{
	GENERAL("conditions"),
	TARGET("targetConditions"),
	PASSIVE("passiveConditions");
	
	private static final Map<String, SkillConditionScope> XML_NODE_NAME_TO_SKILL_CONDITION_SCOPE;
	
	static
	{
		XML_NODE_NAME_TO_SKILL_CONDITION_SCOPE = Arrays.stream(values()).collect(Collectors.toMap(e -> e.getXmlNodeName(), e -> e));
	}
	
	private final String _xmlNodeName;
	
	/**
	 * Creates a new {@code SkillConditionScope} instance.<br>
	 * This constructor maps the scope to a specific XML node name.
	 * @param xmlNodeName The name of the XML node associated with this scope.
	 */
	SkillConditionScope(String xmlNodeName)
	{
		_xmlNodeName = xmlNodeName;
	}
	
	/**
	 * Returns the name of the XML node associated with this scope.<br>
	 * This value is used to identify the correct configuration in the data files.
	 * @return The {@code String} representing the XML node name.
	 */
	public String getXmlNodeName()
	{
		return _xmlNodeName;
	}
	
	/**
	 * Finds a {@code SkillConditionScope} based on its XML node name.<br>
	 * This method looks up the provided string in the internal mapping.<br>
	 * It returns {@code null} if no matching node name is found.
	 * @param xmlNodeName The name of the XML node to search for.
	 * @return The corresponding {@code SkillConditionScope} or {@code null}.
	 */
	public static SkillConditionScope findByXmlNodeName(String xmlNodeName)
	{
		return XML_NODE_NAME_TO_SKILL_CONDITION_SCOPE.get(xmlNodeName);
	}
}
