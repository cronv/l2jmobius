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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.transform.AdditionalItemHolder;
import org.l2jmobius.gameserver.entity.actor.transform.AdditionalSkillHolder;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.actor.transform.TransformLevelData;
import org.l2jmobius.gameserver.entity.actor.transform.TransformTemplate;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the parsing and storage of {@code Transform} data from XML files.<br>
 * It maps configuration values to {@link org.l2jmobius.gameserver.entity.actor.transform.TransformTemplate} objects.
 * @author UnAfraid, BazookaRpm
 */
public class TransformData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(TransformData.class.getName());
	
	private final Map<Integer, Transform> _transformData = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of {@code TransformData}.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected TransformData()
	{
		load();
	}
	
	/**
	 * Loads all transformation data from the configuration files.<br>
	 * This method clears existing data before parsing new entries.<br>
	 * It logs the total number of loaded templates to the console.
	 */
	@Override
	public synchronized void load()
	{
		_transformData.clear();
		parseDatapackDirectory("data/stats/transformations", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _transformData.size() + " transform templates.");
	}
	
	/**
	 * Parses the provided {@code Document} to populate the internal transform data.<br>
	 * This method iterates through the XML structure to extract stats, skills, and items.<br>
	 * It maps each found transform into the internal storage based on its unique ID.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("transform".equalsIgnoreCase(d.getNodeName()))
					{
						final StatSet set = new StatSet(parseAttributes(d));
						final Transform transform = new Transform(set);
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							final String cdName = cd.getNodeName();
							final boolean isMale = "Male".equalsIgnoreCase(cdName);
							if (isMale || "Female".equalsIgnoreCase(cdName))
							{
								TransformTemplate templateData = null;
								for (Node z = cd.getFirstChild(); z != null; z = z.getNextSibling())
								{
									switch (z.getNodeName())
									{
										case "common":
										{
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												switch (s.getNodeName())
												{
													case "base":
													case "stats":
													case "defense":
													case "magicDefense":
													case "collision":
													case "moving":
													{
														final NamedNodeMap commonAttrs = s.getAttributes();
														for (int i = 0; i < commonAttrs.getLength(); i++)
														{
															final Node att = commonAttrs.item(i);
															set.set(att.getNodeName(), att.getNodeValue());
														}
														break;
													}
												}
											}
											
											templateData = new TransformTemplate(set);
											transform.setTemplate(isMale, templateData);
											break;
										}
										case "skills":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												if ("skill".equals(s.getNodeName()))
												{
													final NamedNodeMap skillAttrs = s.getAttributes();
													final int skillId = parseInteger(skillAttrs, "id");
													final int skillLevel = parseInteger(skillAttrs, "level");
													templateData.addSkill(new SkillHolder(skillId, skillLevel));
												}
											}
											break;
										}
										case "actions":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											
											set.set("actions", z.getTextContent());
											final int[] actions = set.getIntArray("actions", " ");
											templateData.setBasicActionList(actions);
											break;
										}
										case "additionalSkills":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												if ("skill".equals(s.getNodeName()))
												{
													final NamedNodeMap skillAttrs = s.getAttributes();
													final int skillId = parseInteger(skillAttrs, "id");
													final int skillLevel = parseInteger(skillAttrs, "level");
													final int minLevel = parseInteger(skillAttrs, "minLevel");
													templateData.addAdditionalSkill(new AdditionalSkillHolder(skillId, skillLevel, minLevel));
												}
											}
											break;
										}
										case "items":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												if ("item".equals(s.getNodeName()))
												{
													final NamedNodeMap itemAttrs = s.getAttributes();
													final int itemId = parseInteger(itemAttrs, "id");
													final boolean allowed = parseBoolean(itemAttrs, "allowed");
													templateData.addAdditionalItem(new AdditionalItemHolder(itemId, allowed));
												}
											}
											break;
										}
										case "levels":
										{
											if (templateData == null)
											{
												templateData = new TransformTemplate(set);
												transform.setTemplate(isMale, templateData);
											}
											
											for (Node s = z.getFirstChild(); s != null; s = s.getNextSibling())
											{
												if ("level".equals(s.getNodeName()))
												{
													templateData.addLevelData(new TransformLevelData(new StatSet(parseAttributes(s))));
												}
											}
											break;
										}
									}
								}
							}
						}
						
						_transformData.put(transform.getId(), transform);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a {@code Transform} object based on its unique identifier.<br>
	 * This method looks up the data in the internal cache.
	 * @param id The unique integer ID of the transform to find.
	 * @return The corresponding {@code Transform} object, or {@code null} if not found.
	 */
	public Transform getTransform(int id)
	{
		return _transformData.get(id);
	}
	
	/**
	 * Provides access to the singleton instance of {@code TransformData}.<br>
	 * Use this method to get the global data manager for transforms.
	 * @return The single instance of {@code TransformData}.
	 */
	public static TransformData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final TransformData INSTANCE = new TransformData();
	}
}
