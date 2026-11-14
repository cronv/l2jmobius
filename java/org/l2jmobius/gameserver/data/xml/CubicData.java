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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.templates.CubicTemplate;
import org.l2jmobius.gameserver.entity.cubic.CubicSkill;
import org.l2jmobius.gameserver.entity.cubic.ICubicConditionHolder;
import org.l2jmobius.gameserver.entity.cubic.conditions.HealthCondition;
import org.l2jmobius.gameserver.entity.cubic.conditions.HpCondition;
import org.l2jmobius.gameserver.entity.cubic.conditions.HpCondition.HpConditionType;
import org.l2jmobius.gameserver.entity.cubic.conditions.RangeCondition;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of cubic data from XML files.<br>
 * It provides a structured way to initialize {@code CubicTemplate} objects for use in the game server.
 * @author BazookaRpm
 */
public class CubicData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(CubicData.class.getName());
	
	private volatile Map<Integer, Map<Integer, CubicTemplate>> _cubics = Collections.emptyMap();
	private volatile Map<Integer, Map<Integer, CubicTemplate>> _loadingCubics;
	
	/**
	 * Initializes a new instance of the {@code CubicData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected CubicData()
	{
		load();
	}
	
	/**
	 * Loads all cubic data from the configuration files.<br>
	 * The active catalog is replaced only once the whole directory parses, so a failed reload keeps the templates the running cubics are already using.
	 */
	@Override
	public synchronized void load()
	{
		final Map<Integer, Map<Integer, CubicTemplate>> loadedCubics = new ConcurrentHashMap<>();
		_loadingCubics = loadedCubics;
		try
		{
			parseDatapackDirectory("data/stats/cubics", true);
			int count = 0;
			for (Map<Integer, CubicTemplate> levels : loadedCubics.values())
			{
				count += levels.size();
			}
			
			if (count == 0)
			{
				LOGGER.warning(getClass().getSimpleName() + ": No cubic templates loaded. Keeping previous data.");
				return;
			}
			
			_cubics = loadedCubics;
			LOGGER.info(getClass().getSimpleName() + ": Loaded " + count + " cubic templates.");
		}
		finally
		{
			_loadingCubics = null;
		}
	}
	
	/**
	 * Parses the XML content from a {@code Document} to populate cubic templates.<br>
	 * This method iterates through all {@code cubic} nodes within the {@code list} element.<br>
	 * It creates new {@code CubicTemplate} objects for each entry found.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed XML content.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "cubic", cubicNode -> parseTemplate(cubicNode, new CubicTemplate(new StatSet(parseAttributes(cubicNode))))));
	}
	
	/**
	 * Parses the XML data for a specific cubic entry.<br>
	 * It identifies and processes conditions and skills within the {@code Node}.<br>
	 * The resulting {@code CubicTemplate} is then stored in the internal map.
	 * @param cubicNode The XML node containing the cubic data.
	 * @param template The {@code CubicTemplate} object to be populated.
	 */
	private void parseTemplate(Node cubicNode, CubicTemplate template)
	{
		forEach(cubicNode, IXmlReader::isNode, innerNode ->
		{
			switch (innerNode.getNodeName())
			{
				case "conditions":
				{
					parseConditions(innerNode, template, template);
					break;
				}
				case "skills":
				{
					parseSkills(innerNode, template);
					break;
				}
			}
		});
		
		final Map<Integer, Map<Integer, CubicTemplate>> loadingCubics = _loadingCubics;
		if (loadingCubics == null)
		{
			LOGGER.warning("Skipping cubic template outside loading context. cubicId: " + template.getId() + " level: " + template.getLevel());
			return;
		}
		
		loadingCubics.computeIfAbsent(template.getId(), key -> new ConcurrentHashMap<>()).put(template.getLevel(), template);
	}
	
	/**
	 * Parses the conditions defined in an XML node.<br>
	 * It populates the {@code ICubicConditionHolder} with various condition types.<br>
	 * Supported tags include hp, range, and healthPercent.
	 * @param cubicNode The XML node containing the condition definitions.
	 * @param template The {@code CubicTemplate} being processed.
	 * @param holder The holder where new conditions are added.
	 */
	private void parseConditions(Node cubicNode, CubicTemplate template, ICubicConditionHolder holder)
	{
		forEach(cubicNode, IXmlReader::isNode, conditionNode ->
		{
			switch (conditionNode.getNodeName())
			{
				case "hp":
				{
					final NamedNodeMap hpAttrs = conditionNode.getAttributes();
					final HpConditionType type = parseEnum(hpAttrs, HpConditionType.class, "type");
					final int hpPer = parseInteger(hpAttrs, "percent");
					holder.addCondition(new HpCondition(type, hpPer));
					break;
				}
				case "range":
				{
					final int range = parseInteger(conditionNode.getAttributes(), "value");
					holder.addCondition(new RangeCondition(range));
					break;
				}
				case "healthPercent":
				{
					final NamedNodeMap hpAttrs = conditionNode.getAttributes();
					final int min = parseInteger(hpAttrs, "min");
					final int max = parseInteger(hpAttrs, "max");
					holder.addCondition(new HealthCondition(min, max));
					break;
				}
				default:
				{
					LOGGER.warning("Attempting to use not implemented condition: " + conditionNode.getNodeName() + " for cubic id: " + template.getId() + " level: " + template.getLevel());
					break;
				}
			}
		});
	}
	
	/**
	 * Parses the skills defined in an XML node.<br>
	 * It creates new {@code CubicSkill} objects from the data.<br>
	 * These skills are then added to the provided {@code CubicTemplate}.
	 * @param skillsNode The XML {@code Node} containing skill information.
	 * @param template The {@code CubicTemplate} where skills will be stored.
	 */
	private void parseSkills(Node skillsNode, CubicTemplate template)
	{
		forEach(skillsNode, "skill", skillNode ->
		{
			final CubicSkill skill = new CubicSkill(new StatSet(parseAttributes(skillNode)));
			forEach(skillNode, "conditions", conditionsNode -> parseConditions(conditionsNode, template, skill));
			template.getCubicSkills().add(skill);
		});
	}
	
	/**
	 * Retrieves a specific {@code CubicTemplate} based on its ID and level.<br>
	 * This method looks up the template in the internal data map.<br>
	 * It returns {@code null} if no matching template is found.
	 * @param id The unique identifier for the cubic.
	 * @param level The specific level of the cubic.
	 * @return The corresponding {@code CubicTemplate} object or {@code null}.
	 */
	public CubicTemplate getCubicTemplate(int id, int level)
	{
		return _cubics.getOrDefault(id, Collections.emptyMap()).get(level);
	}
	
	/**
	 * Provides access to the singleton instance of {@code CubicData}.<br>
	 * Use this method to retrieve the global data manager.
	 * @return The singleton instance of {@code CubicData}.
	 */
	public static CubicData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CubicData INSTANCE = new CubicData();
	}
}
