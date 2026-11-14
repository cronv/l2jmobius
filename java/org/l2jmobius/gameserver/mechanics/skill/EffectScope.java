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
 * Defines the spatial range and target limitations for skill effects.<br>
 * It determines which entities are affected by a specific {@code Skill} action.
 * @author NosBit
 */
public enum EffectScope
{
	GENERAL("effects"),
	START("startEffects"),
	SELF("selfEffects"),
	CHANNELING("channelingEffects"),
	PVP("pvpEffects"),
	PVE("pveEffects"),
	END("endEffects");
	
	private static final Map<String, EffectScope> XML_NODE_NAME_TO_EFFECT_SCOPE;
	
	static
	{
		XML_NODE_NAME_TO_EFFECT_SCOPE = Arrays.stream(values()).collect(Collectors.toMap(e -> e.getXmlNodeName(), e -> e));
	}
	
	private final String _xmlNodeName;
	
	/**
	 * Creates a new {@code EffectScope} instance.<br>
	 * This constructor maps the scope to a specific XML node name.
	 * @param xmlNodeName The name of the XML node associated with this scope.
	 */
	EffectScope(String xmlNodeName)
	{
		_xmlNodeName = xmlNodeName;
	}
	
	/**
	 * Returns the name of the XML node associated with this {@code EffectScope}.<br>
	 * This is used to map configuration files to specific effect types.
	 * @return The string name of the XML node.
	 */
	public String getXmlNodeName()
	{
		return _xmlNodeName;
	}
	
	/**
	 * Finds an {@code EffectScope} based on its corresponding XML node name.<br>
	 * This method looks up the provided string in the internal mapping.
	 * @param xmlNodeName The name of the XML node to search for.
	 * @return The matching {@code EffectScope} or {@code null} if no match is found.
	 */
	public static EffectScope findByXmlNodeName(String xmlNodeName)
	{
		return XML_NODE_NAME_TO_EFFECT_SCOPE.get(xmlNodeName);
	}
}
