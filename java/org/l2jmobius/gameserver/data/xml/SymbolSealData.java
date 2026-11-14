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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.SymbolSealHolder;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class handles the loading and parsing of {@code SymbolSeal} data from XML files.<br>
 * It maps configuration values to {@link org.l2jmobius.gameserver.data.holders.SymbolSealHolder} objects.
 * @author NviX
 */
public class SymbolSealData implements IXmlReader
{
	private final Map<Integer, List<SymbolSealHolder>> _data = new HashMap<>();
	
	/**
	 * Initializes a new instance of the {@code SymbolSealData} class.<br>
	 * This constructor automatically triggers the {@code load()} method.<br>
	 * It prepares the internal data structures for use.
	 */
	protected SymbolSealData()
	{
		load();
	}
	
	/**
	 * Loads the data from the XML file.<br>
	 * This method calls {@code parseDatapackFile} to initialize the internal map.
	 */
	@Override
	public void load()
	{
		parseDatapackFile("data/SymbolSealData.xml");
	}
	
	/**
	 * Parses the XML content from a file into the internal data structure.<br>
	 * This method iterates through all nodes to find class and symbol information.
	 * @param document The {@code Document} object containing the parsed XML tree.
	 * @param file The {@code File} object representing the source of the data.
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
					if ("class".equalsIgnoreCase(d.getNodeName()))
					{
						final int classId = parseInteger(d.getAttributes(), "id");
						final List<SymbolSealHolder> classData = _data.computeIfAbsent(classId, k -> new ArrayList<>());
						
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("symbol".equalsIgnoreCase(cd.getNodeName()))
							{
								final NamedNodeMap cdAttrs = cd.getAttributes();
								final int symbolId = parseInteger(cdAttrs, "id");
								final int skillId = parseInteger(cdAttrs, "skillId");
								classData.add(new SymbolSealHolder(symbolId, SkillData.getInstance().getSkill(skillId, 1)));
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a {@code Skill} based on the provided class and symbol identifiers.<br>
	 * This method searches the internal data map for a matching entry.<br>
	 * It returns {@code null} if no matching skill is found.
	 * @param classId The unique identifier for the character class.
	 * @param symbolId The unique identifier for the specific symbol.
	 * @return The {@code Skill} object associated with the IDs, or {@code null}.
	 */
	public Skill getSkill(int classId, int symbolId)
	{
		final List<SymbolSealHolder> data = _data.get(classId);
		if (data != null)
		{
			final SymbolSealHolder symbol = data.get(symbolId);
			if (symbol != null)
			{
				return symbol.getSkill();
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the singleton instance of {@code SymbolSealData}.<br>
	 * This method provides a global access point to the data.
	 * @return The single instance of {@code SymbolSealData}.
	 */
	public static SymbolSealData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SymbolSealData INSTANCE = new SymbolSealData();
	}
}
