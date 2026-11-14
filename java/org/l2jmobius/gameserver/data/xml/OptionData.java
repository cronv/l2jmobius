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
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.handler.EffectHandler;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillHolder;
import org.l2jmobius.gameserver.mechanics.options.OptionSkillType;
import org.l2jmobius.gameserver.mechanics.options.Options;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the parsing and storage of option data from XML files.<br>
 * It maps configuration values to {@code Options} objects for use in the game mechanics.
 * @author UnAfraid, BazookaRpm
 */
public class OptionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(OptionData.class.getName());
	
	private static volatile Options[] _options = new Options[0];
	private static Map<Integer, Options> _optionMap = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code OptionData} class.<br>
	 * This constructor automatically triggers the {@code load()} method.<br>
	 * It ensures that data is ready for use immediately after creation.
	 */
	protected OptionData()
	{
		load();
	}
	
	/**
	 * Loads the augmentation options from the data directory.<br>
	 * This method populates the internal {@code Options} array.<br>
	 * It clears the temporary map after loading is complete.
	 */
	@Override
	public synchronized void load()
	{
		parseDatapackDirectory("data/stats/augmentation/options", false);
		
		if (_optionMap.isEmpty())
		{
			LOGGER.warning(getClass().getSimpleName() + ": No options were loaded. Keeping the active options.");
			return;
		}
		
		// Filled before publishing; readers would otherwise see an all null array while it is populated.
		final Options[] options = new Options[Collections.max(_optionMap.keySet()) + 1];
		for (Entry<Integer, Options> option : _optionMap.entrySet())
		{
			options[option.getKey().intValue()] = option.getValue();
		}
		
		_options = options;
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _optionMap.size() + " options.");
		_optionMap.clear();
	}
	
	/**
	 * Parses the provided {@code Document} to populate the options map.<br>
	 * This method reads XML data from a file and creates new {@code Options} objects.<br>
	 * It handles effects, active skills, passive skills, and various activation skills.
	 * @param document The {@code Document} containing the XML data to be parsed.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "option", optionNode ->
		{
			final int id = parseInteger(optionNode.getAttributes(), "id");
			final Options option = new Options(id);
			
			forEach(optionNode, IXmlReader::isNode, innerNode ->
			{
				switch (innerNode.getNodeName())
				{
					case "effects":
					{
						forEach(innerNode, "effect", effectNode ->
						{
							final String name = parseString(effectNode.getAttributes(), "name");
							final StatSet params = new StatSet();
							forEach(effectNode, IXmlReader::isNode, paramNode -> params.set(paramNode.getNodeName(), SkillData.getInstance().parseValue(paramNode, true, false, Collections.emptyMap())));
							option.addEffect(EffectHandler.getInstance().getHandlerFactory(name).apply(params));
						});
						break;
					}
					case "active_skill":
					{
						final NamedNodeMap activeAttrs = innerNode.getAttributes();
						final int skillId = parseInteger(activeAttrs, "id");
						final int skillLevel = parseInteger(activeAttrs, "level");
						final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
						if (skill != null)
						{
							option.addActiveSkill(skill);
						}
						else
						{
							LOGGER.info(getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
						}
						break;
					}
					case "passive_skill":
					{
						final NamedNodeMap passiveAttrs = innerNode.getAttributes();
						final int skillId = parseInteger(passiveAttrs, "id");
						final int skillLevel = parseInteger(passiveAttrs, "level");
						final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
						if (skill != null)
						{
							option.addPassiveSkill(skill);
						}
						else
						{
							LOGGER.info(getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
						}
						break;
					}
					case "attack_skill":
					{
						final NamedNodeMap attackAttrs = innerNode.getAttributes();
						final int skillId = parseInteger(attackAttrs, "id");
						final int skillLevel = parseInteger(attackAttrs, "level");
						final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
						if (skill != null)
						{
							option.addActivationSkill(new OptionSkillHolder(skill, parseDouble(attackAttrs, "chance"), OptionSkillType.ATTACK));
						}
						else
						{
							LOGGER.info(getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
						}
						break;
					}
					case "magic_skill":
					{
						final NamedNodeMap magicAttrs = innerNode.getAttributes();
						final int skillId = parseInteger(magicAttrs, "id");
						final int skillLevel = parseInteger(magicAttrs, "level");
						final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
						if (skill != null)
						{
							option.addActivationSkill(new OptionSkillHolder(skill, parseDouble(magicAttrs, "chance"), OptionSkillType.MAGIC));
						}
						else
						{
							LOGGER.info(getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
						}
						break;
					}
					case "critical_skill":
					{
						final NamedNodeMap criticalAttrs = innerNode.getAttributes();
						final int skillId = parseInteger(criticalAttrs, "id");
						final int skillLevel = parseInteger(criticalAttrs, "level");
						final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
						if (skill != null)
						{
							option.addActivationSkill(new OptionSkillHolder(skill, parseDouble(criticalAttrs, "chance"), OptionSkillType.CRITICAL));
						}
						else
						{
							LOGGER.info(getClass().getSimpleName() + ": Could not find skill " + skillId + "(" + skillLevel + ") used by option " + id + ".");
						}
						break;
					}
				}
			});
			_optionMap.put(option.getId(), option);
		}));
	}
	
	/**
	 * Retrieves the {@code Options} object for a specific ID.<br>
	 * This method looks up the data in the internal array.<br>
	 * It returns {@code null} if the ID is out of bounds.
	 * @param id The unique identifier for the options to retrieve.
	 * @return The {@code Options} object associated with the ID, or {@code null}.
	 */
	public Options getOptions(int id)
	{
		if ((id > -1) && (_options.length > id))
		{
			return _options[id];
		}
		
		return null;
	}
	
	/**
	 * Retrieves the singleton instance of {@code OptionData}.<br>
	 * This method provides a global access point to the data manager.
	 * @return The single instance of {@code OptionData}.
	 */
	public static OptionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final OptionData INSTANCE = new OptionData();
	}
}
