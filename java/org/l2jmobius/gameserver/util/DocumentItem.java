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
package org.l2jmobius.gameserver.util;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.holders.ExtractableProduct;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.mechanics.stats.functions.FuncTemplate;

/**
 * Reads and parses item data from XML documents into item templates.
 * @author mkizub, JIV, Mobius
 */
public class DocumentItem extends DocumentBase implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DocumentItem.class.getName());
	
	private DocumentItemDataHolder _currentItem = null;
	private final List<ItemTemplate> _itemsInFile = new ArrayList<>();
	
	private class DocumentItemDataHolder
	{
		public DocumentItemDataHolder()
		{
		}
		
		int id;
		String type;
		StatSet set;
		int currentLevel;
		ItemTemplate item;
	}
	
	/**
	 * Creates a parser bound to the specified item XML file.
	 * @param file the XML file containing the item definitions to parse
	 */
	public DocumentItem(File file)
	{
		super(file);
	}
	
	/**
	 * Returns the stat set of the item currently being parsed.
	 * @return the stat set of the current item
	 */
	@Override
	protected StatSet getStatSet()
	{
		return _currentItem.set;
	}
	
	/**
	 * Resolves a named table value using the current item level as index.
	 * @param name the table name to look up
	 * @return the value at the current level for the named table
	 */
	@Override
	protected String getTableValue(String name)
	{
		return _tables.get(name)[_currentItem.currentLevel];
	}
	
	/**
	 * Resolves a named table value at the given one-based position.
	 * @param name the table name to look up
	 * @param idx the one-based index of the value
	 * @return the value at the specified position
	 */
	@Override
	protected String getTableValue(String name, int idx)
	{
		return _tables.get(name)[idx - 1];
	}
	
	/**
	 * Iterates the document and parses each item node into a template.
	 * @param document the XML document to parse
	 */
	@Override
	protected void parseDocument(Document document)
	{
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("item".equalsIgnoreCase(d.getNodeName()))
					{
						try
						{
							_currentItem = new DocumentItemDataHolder();
							parseItem(d);
							_itemsInFile.add(_currentItem.item);
							resetTable();
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, "Cannot create item " + _currentItem.id, e);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Parses a single item node, building its template and attached stats, skills, capsuled items and conditions.
	 * @param node the item node to parse
	 * @throws InvocationTargetException if item instantiation via reflection fails
	 */
	private void parseItem(Node node) throws InvocationTargetException
	{
		Node n = node;
		final NamedNodeMap nodeAttrs = n.getAttributes();
		final int itemId = Integer.parseInt(nodeAttrs.getNamedItem("id").getNodeValue());
		final String className = nodeAttrs.getNamedItem("type").getNodeValue();
		final String itemName = nodeAttrs.getNamedItem("name").getNodeValue();
		final Node additionalNameNode = nodeAttrs.getNamedItem("additionalName");
		final String additionalName = additionalNameNode != null ? additionalNameNode.getNodeValue() : null;
		_currentItem.id = itemId;
		_currentItem.type = className;
		_currentItem.set = new StatSet();
		_currentItem.set.set("item_id", itemId);
		_currentItem.set.set("name", itemName);
		_currentItem.set.set("additionalName", additionalName);
		
		final Node first = n.getFirstChild();
		for (n = first; n != null; n = n.getNextSibling())
		{
			// Check if this is a direct element value node (like <icon>value</icon>).
			if ((n.getNodeType() == Node.ELEMENT_NODE) && (n.getFirstChild() != null) && (n.getAttributes().getLength() == 0))
			{
				// First check if this node has any child ELEMENT nodes (not just text/whitespace).
				boolean hasChildElements = false;
				for (Node child = n.getFirstChild(); child != null; child = child.getNextSibling())
				{
					if (child.getNodeType() == Node.ELEMENT_NODE)
					{
						hasChildElements = true;
						break;
					}
				}
				
				// Only parse as element value if it has NO child elements AND has text content.
				if (!hasChildElements && (n.getFirstChild().getNodeType() == Node.TEXT_NODE))
				{
					// Parse element value, regardless of whether the item is created or not.
					parseElementValue(n, _currentItem.set, 1);
					
					// If the item is already created, update it with the modified StatSet.
					if (_currentItem.item != null)
					{
						_currentItem.item.set(_currentItem.set);
					}
					continue;
				}
			}
			
			final String nodeName = n.getNodeName();
			switch (nodeName)
			{
				case "table":
				{
					if (_currentItem.item != null)
					{
						throw new IllegalStateException("Item created but table node found! Item " + itemId);
					}
					
					parseTable(n);
					break;
				}
				case "set":
				{
					if (_currentItem.item != null)
					{
						throw new IllegalStateException("Item created but set node found! Item " + itemId);
					}
					
					parseBeanSet(n, _currentItem.set, 1);
					break;
				}
				case "stats":
				{
					makeItem();
					for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
					{
						if ("stat".equals(b.getNodeName()))
						{
							final Stat type = Stat.valueOfXml(b.getAttributes().getNamedItem("type").getNodeValue());
							final double value = Double.parseDouble(b.getTextContent());
							_currentItem.item.addFunctionTemplate(new FuncTemplate(null, null, "add", 0x00, type, value));
						}
					}
					break;
				}
				case "skills":
				{
					makeItem();
					for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
					{
						if ("skill".equalsIgnoreCase(b.getNodeName()))
						{
							final NamedNodeMap bAttrs = b.getAttributes();
							final int id = parseInteger(bAttrs, "id");
							final int level = parseInteger(bAttrs, "level");
							final int subLevel = parseInteger(bAttrs, "subLevel", 0);
							final ItemSkillType type = parseEnum(bAttrs, ItemSkillType.class, "type", ItemSkillType.NORMAL);
							final int chance = parseInteger(bAttrs, "type_chance", 100);
							final int value = parseInteger(bAttrs, "type_value", 0);
							if (type == ItemSkillType.ON_ENCHANT)
							{
								final int enchantLimit = _currentItem.item.getEnchantLimit();
								if ((enchantLimit > 0) && (value > enchantLimit))
								{
									LOGGER.warning(getClass().getSimpleName() + ": Item " + itemId + " has ON_ENCHANT value greater than it's enchant limit.");
								}
							}
							
							_currentItem.item.addSkill(new ItemSkillHolder(id, level, subLevel, type, chance, value));
						}
					}
					break;
				}
				case "capsuled_items":
				{
					makeItem();
					for (Node b = n.getFirstChild(); b != null; b = b.getNextSibling())
					{
						if ("item".equals(b.getNodeName()))
						{
							final NamedNodeMap bAttrs = b.getAttributes();
							final int id = parseInteger(bAttrs, "id");
							final long min = parseLong(bAttrs, "min");
							final long max = parseLong(bAttrs, "max");
							final double chance = parseDouble(bAttrs, "chance");
							final int minEnchant = parseInteger(bAttrs, "minEnchant", 0);
							final int maxEnchant = parseInteger(bAttrs, "maxEnchant", 0);
							_currentItem.item.addCapsuledItem(new ExtractableProduct(id, min, max, chance, minEnchant, maxEnchant));
						}
					}
					break;
				}
				case "conditions":
				{
					makeItem();
					final Condition condition = parseCondition(n.getFirstChild(), _currentItem.item);
					final NamedNodeMap condAttrs = n.getAttributes();
					final Node msg = condAttrs.getNamedItem("msg");
					final Node msgId = condAttrs.getNamedItem("msgId");
					if ((condition != null) && (msg != null))
					{
						condition.setMessage(msg.getNodeValue());
					}
					else if ((condition != null) && (msgId != null))
					{
						final String msgIdVal = getValue(msgId.getNodeValue(), null);
						condition.setMessageId(Integer.decode(msgIdVal));
						final Node addName = n.getAttributes().getNamedItem("addName");
						if ((addName != null) && (Integer.decode(msgIdVal) > 0))
						{
							condition.addName();
						}
					}
					
					_currentItem.item.attachCondition(condition);
					break;
				}
			}
		}
		
		// Bah! In this point item doesn't have to be still created.
		makeItem();
	}
	
	/**
	 * Lazily instantiates the item template via reflection, or reloads its stats if already created.
	 * @throws InvocationTargetException if the item class constructor fails
	 */
	private void makeItem() throws InvocationTargetException
	{
		// If item exists just reload the data.
		if (_currentItem.item != null)
		{
			_currentItem.item.set(_currentItem.set);
			return;
		}
		
		try
		{
			final Constructor<?> itemClass = Class.forName("org.l2jmobius.gameserver.entity.item." + _currentItem.type).getConstructor(StatSet.class);
			_currentItem.item = (ItemTemplate) itemClass.newInstance(_currentItem.set);
		}
		catch (Exception e)
		{
			throw new InvocationTargetException(e);
		}
	}
	
	/**
	 * Returns the templates parsed from this document.
	 * @return the parsed item templates
	 */
	public List<ItemTemplate> getItemList()
	{
		return _itemsInFile;
	}
	
	/**
	 * Loads item data from the associated file.
	 */
	@Override
	public void load()
	{
	}
	
	/**
	 * Parses the given document associated with the specified file.
	 * @param document the XML document to parse
	 * @param file the file the document was loaded from
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
	}
}
