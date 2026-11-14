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
package org.l2jmobius.tools.editor.item.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lexically locates XML element spans without serializing or rebuilding source text. It is intentionally package-private and used only after hardened XML parsing succeeds.
 * @author BazookaRpm
 */
final class ItemXmlSpans
{
	private static final Pattern ATTRIBUTE_PATTERN = Pattern.compile("\\b([A-Za-z_][A-Za-z0-9_.:-]*)\\s*=\\s*([\\\"'])(.*?)\\2", Pattern.DOTALL);
	
	private ItemXmlSpans()
	{
	}
	
	static ElementSpan findTopLevelItem(String source, int itemId, int occurrence)
	{
		if (occurrence < 0)
		{
			throw new IllegalArgumentException("Item occurrence cannot be negative.");
		}
		
		final String id = Integer.toString(itemId);
		int currentOccurrence = 0;
		for (ElementSpan span : spans(source))
		{
			if ("item".equals(span.getName()) && "list".equals(span.getParentName()))
			{
				if (id.equals(attribute(span.getOpeningTag(source), "id")))
				{
					if (currentOccurrence == occurrence)
					{
						return span;
					}
					
					currentOccurrence++;
				}
			}
		}
		
		throw new IllegalArgumentException("Could not locate top-level item " + itemId + " occurrence " + (occurrence + 1) + ".");
	}
	
	static ElementSpan rootItem(String itemXml)
	{
		ElementSpan match = null;
		for (ElementSpan span : spans(itemXml))
		{
			if ("item".equals(span.getName()) && (span.getParentName() == null))
			{
				if (match != null)
				{
					throw new IllegalArgumentException("Editor content must contain exactly one top-level <item> element.");
				}
				
				match = span;
			}
		}
		
		if (match == null)
		{
			throw new IllegalArgumentException("Editor content must contain one top-level <item> element.");
		}
		
		return match;
	}
	
	static List<ElementSpan> directChildren(List<ElementSpan> spans, String parentName, String childName)
	{
		final List<ElementSpan> result = new ArrayList<>();
		for (ElementSpan span : spans)
		{
			if (childName.equals(span.getName()) && parentName.equals(span.getParentName()))
			{
				result.add(span);
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	static List<ElementSpan> childrenOfSpan(List<ElementSpan> spans, ElementSpan parent, String childName)
	{
		final List<ElementSpan> result = new ArrayList<>();
		for (ElementSpan span : spans)
		{
			if ((span.getStart() > parent.getStart()) && (span.getEnd() < parent.getEnd()) && childName.equals(span.getName()) && parent.getName().equals(span.getParentName()))
			{
				result.add(span);
			}
		}
		
		return Collections.unmodifiableList(result);
	}
	
	static String attribute(String openingTag, String name)
	{
		final Matcher matcher = ATTRIBUTE_PATTERN.matcher(openingTag);
		while (matcher.find())
		{
			if (name.equals(matcher.group(1)))
			{
				return unescape(matcher.group(3));
			}
		}
		
		return null;
	}
	
	static String replaceAttribute(String openingTag, String name, String value, boolean removeWhenBlank)
	{
		final Matcher matcher = ATTRIBUTE_PATTERN.matcher(openingTag);
		while (matcher.find())
		{
			if (name.equals(matcher.group(1)))
			{
				if (removeWhenBlank && ((value == null) || value.isBlank()))
				{
					int start = matcher.start();
					while ((start > 0) && Character.isWhitespace(openingTag.charAt(start - 1)))
					{
						start--;
					}
					
					return openingTag.substring(0, start) + openingTag.substring(matcher.end());
				}
				
				return openingTag.substring(0, matcher.start(3)) + escapeAttribute(value == null ? "" : value, matcher.group(2).charAt(0)) + openingTag.substring(matcher.end(3));
			}
		}
		
		if (removeWhenBlank && ((value == null) || value.isBlank()))
		{
			return openingTag;
		}
		
		final int length = openingTag.length();
		final int insert = openingTag.endsWith("/>") ? length - 2 : length - 1;
		return openingTag.substring(0, insert) + " " + name + "=\"" + escapeAttribute(value == null ? "" : value, '\"') + "\"" + openingTag.substring(insert);
	}
	
	static String escapeText(String value)
	{
		return (value == null ? "" : value).replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
	
	private static String escapeAttribute(String value, char quote)
	{
		String escaped = value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
		escaped = quote == '\'' ? escaped.replace("'", "&apos;") : escaped.replace("\"", "&quot;");
		return escaped;
	}
	
	private static String unescape(String value)
	{
		return value.replace("&quot;", "\"").replace("&apos;", "'").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&");
	}
	
	static List<ElementSpan> spans(String xml)
	{
		final List<ElementSpan> result = new ArrayList<>();
		final Deque<OpenElement> stack = new ArrayDeque<>();
		final int length = xml.length();
		int offset = 0;
		while (offset < length)
		{
			final int start = xml.indexOf('<', offset);
			if (start < 0)
			{
				break;
			}
			
			if (xml.startsWith("<!--", start))
			{
				final int end = xml.indexOf("-->", start + 4);
				offset = end < 0 ? length : end + 3;
				continue;
			}
			
			if (xml.startsWith("<![CDATA[", start))
			{
				final int end = xml.indexOf("]]>", start + 9);
				offset = end < 0 ? length : end + 3;
				continue;
			}
			
			if (xml.startsWith("<?", start))
			{
				final int end = xml.indexOf("?>", start + 2);
				offset = end < 0 ? length : end + 2;
				continue;
			}
			
			if (xml.startsWith("<!", start))
			{
				final int end = findTagEnd(xml, start + 2);
				offset = end < 0 ? length : end + 1;
				continue;
			}
			
			final int end = findTagEnd(xml, start + 1);
			if (end < 0)
			{
				break;
			}
			
			final String raw = xml.substring(start, end + 1);
			final boolean closing = raw.startsWith("</");
			final boolean selfClosing = !closing && raw.substring(0, raw.length() - 1).trim().endsWith("/");
			final String name = tagName(raw, closing);
			if (!name.isEmpty())
			{
				if (closing)
				{
					if (!stack.isEmpty())
					{
						final OpenElement open = stack.pop();
						if (open._name.equals(name))
						{
							result.add(new ElementSpan(open._name, open._parentName, open._start, end + 1, open._openingEnd));
						}
						else
						{
							throw new IllegalArgumentException("Malformed XML nesting near </" + name + ">.");
						}
					}
				}
				else
				{
					final String parent = stack.isEmpty() ? null : stack.peek()._name;
					if (selfClosing)
					{
						result.add(new ElementSpan(name, parent, start, end + 1, end + 1));
					}
					else
					{
						stack.push(new OpenElement(name, parent, start, end + 1));
					}
				}
			}
			
			offset = end + 1;
		}
		
		if (!stack.isEmpty())
		{
			throw new IllegalArgumentException("Malformed XML: unclosed <" + stack.peek()._name + "> element.");
		}
		
		result.sort((first, second) -> Integer.compare(first._start, second._start));
		return result;
	}
	
	private static int findTagEnd(String xml, int start)
	{
		char quote = 0;
		for (int index = start; index < xml.length(); index++)
		{
			final char value = xml.charAt(index);
			if (quote != 0)
			{
				if (value == quote)
				{
					quote = 0;
				}
			}
			else if ((value == '\'') || (value == '\"'))
			{
				quote = value;
			}
			else if (value == '>')
			{
				return index;
			}
		}
		
		return -1;
	}
	
	private static String tagName(String raw, boolean closing)
	{
		int index = closing ? 2 : 1;
		while ((index < raw.length()) && Character.isWhitespace(raw.charAt(index)))
		{
			index++;
		}
		
		final int start = index;
		while ((index < raw.length()) && (Character.isLetterOrDigit(raw.charAt(index)) || (raw.charAt(index) == '_') || (raw.charAt(index) == '-') || (raw.charAt(index) == ':')))
		{
			index++;
		}
		
		return raw.substring(start, index);
	}
	
	static final class ElementSpan
	{
		private final String _name;
		private final String _parentName;
		private final int _start;
		private final int _end;
		private final int _openingEnd;
		
		private ElementSpan(String name, String parentName, int start, int end, int openingEnd)
		{
			_name = name;
			_parentName = parentName;
			_start = start;
			_end = end;
			_openingEnd = openingEnd;
		}
		
		String getName()
		{
			return _name;
		}
		
		String getParentName()
		{
			return _parentName;
		}
		
		int getStart()
		{
			return _start;
		}
		
		int getEnd()
		{
			return _end;
		}
		
		String getOpeningTag(String xml)
		{
			return xml.substring(_start, _openingEnd);
		}
		
		String getXml(String xml)
		{
			return xml.substring(_start, _end);
		}
		
		int getContentStart()
		{
			return _openingEnd;
		}
	}
	
	private static final class OpenElement
	{
		private final String _name;
		private final String _parentName;
		private final int _start;
		private final int _openingEnd;
		
		private OpenElement(String name, String parentName, int start, int openingEnd)
		{
			_name = name;
			_parentName = parentName;
			_start = start;
			_openingEnd = openingEnd;
		}
	}
}
