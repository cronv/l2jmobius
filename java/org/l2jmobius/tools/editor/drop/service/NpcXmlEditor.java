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
package org.l2jmobius.tools.editor.drop.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import org.l2jmobius.tools.editor.drop.model.DataIndex;
import org.l2jmobius.tools.editor.drop.model.DropEditorProfile;
import org.l2jmobius.tools.editor.drop.model.DropGroup;
import org.l2jmobius.tools.editor.drop.model.DropItem;
import org.l2jmobius.tools.editor.drop.model.DropSection;
import org.l2jmobius.tools.editor.drop.model.DropType;
import org.l2jmobius.tools.editor.drop.model.ItemRecord;
import org.l2jmobius.tools.editor.drop.model.NpcDrops;
import org.l2jmobius.tools.editor.drop.model.NpcRecord;
import org.l2jmobius.tools.editor.drop.util.XmlUtil;
import org.l2jmobius.tools.editor.service.FileSnapshot;
import org.l2jmobius.tools.editor.service.FileSnapshotCache;

/**
 * Loads and atomically writes the drop block of a selected NPC XML definition.
 * @author BazookaRpm
 */
public final class NpcXmlEditor
{
	private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
	
	public NpcDrops loadDrops(NpcRecord npc, DataIndex index) throws Exception
	{
		final FileSnapshot snapshot = FileSnapshotCache.get(npc.getFile());
		
		final Document document = XmlUtil.parse(snapshot.getText());
		final Element npcElement = findTopLevelNpc(document, npc.getId());
		if (npcElement == null)
		{
			throw new IllegalStateException("NPC " + npc.getId() + " was not found in " + npc.getFile());
		}
		
		final NpcDrops result = new NpcDrops(npc.getId());
		result.setSourceSha256(snapshot.getSha256());
		final Element dropLists = findDirectChild(npcElement, "dropLists");
		if (dropLists == null)
		{
			return result;
		}
		
		for (Node sectionNode = dropLists.getFirstChild(); sectionNode != null; sectionNode = sectionNode.getNextSibling())
		{
			if (sectionNode.getNodeType() != Node.ELEMENT_NODE)
			{
				continue;
			}
			
			final DropType type;
			try
			{
				type = DropType.fromXmlName(sectionNode.getNodeName());
			}
			catch (IllegalArgumentException e)
			{
				throw new IllegalStateException("NPC " + npc.getId() + " carries an unknown <" + sectionNode.getNodeName() + "> drop section in " + npc.getFile() + ". Saving would delete it, so the editor refuses to open it.");
			}
			
			if (!DropEditorProfile.current().supports(type))
			{
				throw new IllegalStateException("NPC " + npc.getId() + " carries a <" + sectionNode.getNodeName() + "> drop section that " + DropEditorProfile.current().getDisplayName() + " does not support. Saving would delete it, so the editor refuses to open it.");
			}
			
			final DropSection section = new DropSection(type);
			result.getSections().add(section);
			
			for (Node child = sectionNode.getFirstChild(); child != null; child = child.getNextSibling())
			{
				if (child.getNodeType() != Node.ELEMENT_NODE)
				{
					continue;
				}
				
				final Element element = (Element) child;
				if ("group".equalsIgnoreCase(element.getTagName()))
				{
					final DropGroup group = new DropGroup(element.getAttribute("chance"));
					section.getGroups().add(group);
					for (Node itemNode = element.getFirstChild(); itemNode != null; itemNode = itemNode.getNextSibling())
					{
						if ((itemNode.getNodeType() == Node.ELEMENT_NODE) && "item".equalsIgnoreCase(itemNode.getNodeName()))
						{
							group.getItems().add(parseDropItem((Element) itemNode, type, commentAfter(itemNode), index));
						}
					}
				}
				else if ("item".equalsIgnoreCase(element.getTagName()))
				{
					section.getItems().add(parseDropItem(element, type, commentAfter(element), index));
				}
			}
		}
		
		return result;
	}
	
	public Path saveDrops(NpcRecord npc, NpcDrops drops, DataIndex index) throws Exception
	{
		final FileSnapshot original = FileSnapshotCache.readFresh(npc.getFile());
		if ((drops.getSourceSha256() != null) && !drops.getSourceSha256().equals(original.getSha256()))
		{
			throw new IllegalStateException("The NPC XML changed on disk after it was loaded. Reload the NPC before saving to avoid overwriting external changes.");
		}
		
		validateDrops(drops, index);
		final NpcSpan npcSpan = findNpcSpan(original.getText(), npc.getId());
		final String npcText = original.getText().substring(npcSpan._start, npcSpan._end);
		final DropSpan dropSpan = findDropSpan(npcText);
		final String lineEnding = original.getLineEnding();
		final String replacement;
		String updatedNpc;
		
		drops.removeEmptySections();
		if (dropSpan != null)
		{
			if (drops.getSections().isEmpty())
			{
				final int removeStart = lineStart(npcText, dropSpan._start);
				final int removeEnd = includeFollowingLineEnding(npcText, dropSpan._end, lineEnding);
				updatedNpc = npcText.substring(0, removeStart) + npcText.substring(removeEnd);
			}
			else
			{
				final int replaceStart = lineStart(npcText, dropSpan._start);
				final int replaceEnd = lineEnd(npcText, dropSpan._end);
				replacement = buildDropLists(drops, index, npcText.substring(replaceStart, dropSpan._start), lineEnding);
				updatedNpc = npcText.substring(0, replaceStart) + replacement + npcText.substring(replaceEnd);
			}
		}
		else if (!drops.getSections().isEmpty())
		{
			final int closeNpc = npcText.lastIndexOf("</npc>");
			if (closeNpc < 0)
			{
				throw new IllegalStateException("NPC closing tag was not found for id " + npc.getId());
			}
			
			final int closeLineStart = lineStart(npcText, closeNpc);
			final String childIndent = npcText.substring(closeLineStart, closeNpc) + "\t";
			replacement = buildDropLists(drops, index, childIndent, lineEnding) + lineEnding;
			updatedNpc = npcText.substring(0, closeLineStart) + replacement + npcText.substring(closeLineStart);
		}
		else
		{
			return null;
		}
		
		final String updated = original.getText().substring(0, npcSpan._start) + updatedNpc + original.getText().substring(npcSpan._end);
		XmlUtil.parse(updated);
		final Path backup = createBackup(npc.getFile(), index);
		writeAtomic(npc.getFile(), updated, original.hasBom());
		final FileSnapshot saved = FileSnapshotCache.readFresh(npc.getFile());
		XmlUtil.parse(saved.getText());
		drops.setSourceSha256(saved.getSha256());
		return backup;
	}
	
	private DropItem parseDropItem(Element element, DropType type, String comment, DataIndex index)
	{
		final int itemId = Integer.parseInt(element.getAttribute("id"));
		final DropItem item = new DropItem(itemId, element.getAttribute("min"), element.getAttribute("max"), element.getAttribute("chance"));
		final DropEditorProfile profile = DropEditorProfile.current();
		if (profile.supportsExtendedAttributes(type))
		{
			item.setMinLevel(element.hasAttribute("minLevel") ? element.getAttribute("minLevel") : null);
			item.setMaxLevel(element.hasAttribute("maxLevel") ? element.getAttribute("maxLevel") : null);
			item.setDailyLimit(element.hasAttribute("dailyLimit") ? element.getAttribute("dailyLimit") : null);
		}
		
		if ((comment == null) || comment.isBlank())
		{
			final ItemRecord record = index.getItem(itemId);
			item.setComment(record == null ? null : record.getName());
		}
		else
		{
			item.setComment(comment.trim());
		}
		
		return item;
	}
	
	private void validateDrops(NpcDrops drops, DataIndex index)
	{
		final DropEditorProfile profile = DropEditorProfile.current();
		for (DropSection section : drops.getSections())
		{
			if (!profile.supports(section.getType()))
			{
				throw new IllegalArgumentException("Drop type " + section.getType() + " is not enabled for " + profile.getDisplayName() + ".");
			}
			
			if (!profile.supportsGroups(section.getType()) && !section.getGroups().isEmpty())
			{
				throw new IllegalArgumentException(section.getType() + " does not use grouped drops in " + profile.getDisplayName() + ".");
			}
			
			if (!profile.supportsUngrouped(section.getType()) && !section.getItems().isEmpty())
			{
				throw new IllegalArgumentException(section.getType() + " does not use ungrouped drops in " + profile.getDisplayName() + ".");
			}
			
			for (DropGroup group : section.getGroups())
			{
				parseNonNegativeDouble(group.getChance(), "group chance");
				if (group.getItems().isEmpty())
				{
					throw new IllegalArgumentException("Drop groups cannot be empty.");
				}
				
				for (DropItem item : group.getItems())
				{
					validateItem(item, section.getType(), index);
				}
			}
			
			for (DropItem item : section.getItems())
			{
				validateItem(item, section.getType(), index);
			}
		}
	}
	
	private void validateItem(DropItem item, DropType type, DataIndex index)
	{
		if (index.getItem(item.getItemId()) == null)
		{
			throw new IllegalArgumentException("Item id " + item.getItemId() + " does not exist in data/stats/items.");
		}
		
		final DropEditorProfile profile = DropEditorProfile.current();
		final long min;
		final long max;
		if (profile.usesIntAmounts(type))
		{
			min = parseNonNegativeInt(item.getMin(), "minimum amount");
			max = parseNonNegativeInt(item.getMax(), "maximum amount");
		}
		else
		{
			min = parseNonNegativeLong(item.getMin(), "minimum amount");
			max = parseNonNegativeLong(item.getMax(), "maximum amount");
		}
		
		if (max < min)
		{
			throw new IllegalArgumentException("Maximum amount cannot be lower than minimum amount for item " + item.getItemId() + ".");
		}
		
		final double chance = parseNonNegativeDouble(item.getChance(), "item chance");
		if (chance > 100d)
		{
			throw new IllegalArgumentException("Item chance cannot exceed 100 for item " + item.getItemId() + ".");
		}
		
		if (profile.supportsExtendedAttributes(type))
		{
			final String minLevelValue = item.getMinLevel();
			final String maxLevelValue = item.getMaxLevel();
			final String dailyLimitValue = item.getDailyLimit();
			final boolean hasExtended = isPresent(minLevelValue) || isPresent(maxLevelValue) || isPresent(dailyLimitValue);
			if (profile.requiresExtendedAttributes(type) || hasExtended)
			{
				if (!isPresent(minLevelValue) || !isPresent(maxLevelValue) || !isPresent(dailyLimitValue))
				{
					throw new IllegalArgumentException("Min level, max level, and daily limit must be provided together for item " + item.getItemId() + ".");
				}
				
				final int minLevel = parseNonNegativeInt(minLevelValue, "minimum level");
				final int maxLevel = parseNonNegativeInt(maxLevelValue, "maximum level");
				if (maxLevel < minLevel)
				{
					throw new IllegalArgumentException("Drop maxLevel cannot be lower than minLevel for item " + item.getItemId() + ".");
				}
				
				parseNonNegativeInt(dailyLimitValue, "daily limit");
			}
		}
		else if (isPresent(item.getMinLevel()) || isPresent(item.getMaxLevel()) || isPresent(item.getDailyLimit()))
		{
			throw new IllegalArgumentException("Extended level or daily-limit attributes are not used by " + type + " in " + profile.getDisplayName() + ".");
		}
	}
	
	private boolean isPresent(String value)
	{
		return (value != null) && !value.isBlank();
	}
	
	private long parseNonNegativeLong(String value, String field)
	{
		try
		{
			final long parsed = Long.parseLong(value);
			if (parsed < 0)
			{
				throw new IllegalArgumentException(field + " cannot be negative.");
			}
			
			return parsed;
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Invalid " + field + ": " + value, e);
		}
	}
	
	private int parseNonNegativeInt(String value, String field)
	{
		try
		{
			final int parsed = Integer.parseInt(value);
			if (parsed < 0)
			{
				throw new IllegalArgumentException(field + " cannot be negative.");
			}
			
			return parsed;
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Invalid " + field + ": " + value, e);
		}
	}
	
	private double parseNonNegativeDouble(String value, String field)
	{
		try
		{
			final double parsed = Double.parseDouble(value);
			if (!Double.isFinite(parsed) || (parsed < 0d))
			{
				throw new IllegalArgumentException(field + " must be a finite, non-negative number.");
			}
			
			return parsed;
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Invalid " + field + ": " + value, e);
		}
	}
	
	private String buildDropLists(NpcDrops drops, DataIndex index, String indent, String lineEnding)
	{
		final StringBuilder builder = new StringBuilder();
		builder.append(indent).append("<dropLists>").append(lineEnding);
		for (DropSection section : drops.getSections())
		{
			if (section.isEmpty())
			{
				continue;
			}
			
			builder.append(indent).append('\t').append('<').append(section.getType().getXmlName()).append('>').append(lineEnding);
			for (DropGroup group : section.getGroups())
			{
				builder.append(indent).append("\t\t<group chance=\"").append(group.getChance()).append("\">").append(lineEnding);
				for (DropItem item : group.getItems())
				{
					appendItem(builder, item, section.getType(), index, indent + "\t\t\t", lineEnding);
				}
				
				builder.append(indent).append("\t\t</group>").append(lineEnding);
			}
			
			for (DropItem item : section.getItems())
			{
				appendItem(builder, item, section.getType(), index, indent + "\t\t", lineEnding);
			}
			
			builder.append(indent).append("\t</").append(section.getType().getXmlName()).append('>').append(lineEnding);
		}
		builder.append(indent).append("</dropLists>");
		return builder.toString();
	}
	
	private void appendItem(StringBuilder builder, DropItem item, DropType type, DataIndex index, String indent, String lineEnding)
	{
		builder.append(indent).append("<item id=\"").append(item.getItemId()).append("\" min=\"").append(item.getMin()).append("\" max=\"").append(item.getMax()).append('"');
		final DropEditorProfile profile = DropEditorProfile.current();
		if (profile.supportsExtendedAttributes(type) && isPresent(item.getMinLevel()) && isPresent(item.getMaxLevel()) && isPresent(item.getDailyLimit()))
		{
			builder.append(" minLevel=\"").append(item.getMinLevel()).append("\" maxLevel=\"").append(item.getMaxLevel()).append("\" dailyLimit=\"").append(item.getDailyLimit()).append('"');
		}
		builder.append(" chance=\"").append(item.getChance()).append("\" />");
		String comment = item.getComment();
		if ((comment == null) || comment.isBlank())
		{
			final ItemRecord record = index.getItem(item.getItemId());
			comment = record == null ? null : record.getName();
		}
		
		if ((comment != null) && !comment.isBlank())
		{
			builder.append(" <!-- ").append(sanitizeComment(comment)).append(" -->");
		}
		builder.append(lineEnding);
	}
	
	private String sanitizeComment(String comment)
	{
		return comment.replace("--", "-").trim();
	}
	
	private Element findTopLevelNpc(Document document, int npcId)
	{
		final Element root = document.getDocumentElement();
		for (Node node = root.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ((node.getNodeType() != Node.ELEMENT_NODE) || !"npc".equalsIgnoreCase(node.getNodeName()))
			{
				continue;
			}
			
			final Element element = (Element) node;
			if (element.hasAttribute("id") && Integer.toString(npcId).equals(element.getAttribute("id")))
			{
				return element;
			}
		}
		
		return null;
	}
	
	private Element findDirectChild(Element parent, String name)
	{
		for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling())
		{
			if ((node.getNodeType() == Node.ELEMENT_NODE) && name.equalsIgnoreCase(node.getNodeName()))
			{
				return (Element) node;
			}
		}
		
		return null;
	}
	
	/**
	 * Reads the comment written after a drop item.
	 * @param item The item element to look behind.
	 * @return The comment text, or {@code null} when the item carries none.
	 */
	private static String commentAfter(Node item)
	{
		for (Node node = item.getNextSibling(); node != null; node = node.getNextSibling())
		{
			if (node.getNodeType() == Node.COMMENT_NODE)
			{
				return node.getNodeValue().trim();
			}
			
			// Only spacing may sit between the item and its comment, a line break belongs to the next item.
			if ((node.getNodeType() != Node.TEXT_NODE) || node.getNodeValue().contains("\n"))
			{
				return null;
			}
		}
		
		return null;
	}
	
	private NpcSpan findNpcSpan(String text, int npcId)
	{
		final Pattern pattern = Pattern.compile("<npc\\b(?=[^>]*\\bid\\s*=\\s*[\"']" + npcId + "[\"'])[^>]*>", Pattern.CASE_INSENSITIVE);
		final Matcher matcher = pattern.matcher(text);
		if (!matcher.find())
		{
			throw new IllegalStateException("NPC " + npcId + " opening tag was not found.");
		}
		
		final int close = text.indexOf("</npc>", matcher.end());
		if (close < 0)
		{
			throw new IllegalStateException("NPC " + npcId + " closing tag was not found.");
		}
		
		return new NpcSpan(matcher.start(), close + "</npc>".length());
	}
	
	private DropSpan findDropSpan(String npcText)
	{
		final int start = npcText.indexOf("<dropLists>");
		if (start < 0)
		{
			return null;
		}
		
		final int close = npcText.indexOf("</dropLists>", start);
		if (close < 0)
		{
			throw new IllegalStateException("dropLists closing tag was not found.");
		}
		
		return new DropSpan(start, close + "</dropLists>".length());
	}
	
	private int lineStart(String text, int position)
	{
		final int newline = text.lastIndexOf('\n', Math.max(0, position - 1));
		return newline < 0 ? 0 : newline + 1;
	}
	
	private int lineEnd(String text, int position)
	{
		final int newline = text.indexOf('\n', position);
		if (newline < 0)
		{
			return text.length();
		}
		
		return (newline > 0) && (text.charAt(newline - 1) == '\r') ? newline - 1 : newline;
	}
	
	private int includeFollowingLineEnding(String text, int position, String lineEnding)
	{
		int end = lineEnd(text, position);
		if (end < text.length())
		{
			if (lineEnding.equals("\r\n") && text.startsWith("\r\n", end))
			{
				end += 2;
			}
			else if (text.charAt(end) == '\n')
			{
				end++;
			}
		}
		
		return end;
	}
	
	private Path createBackup(Path source, DataIndex index) throws IOException
	{
		final String projectName = safeName(index.getLayout().getSelectedRoot().getFileName() == null ? "project" : index.getLayout().getSelectedRoot().getFileName().toString());
		final Path backupRoot = Path.of(System.getProperty("user.home"), ".l2jmobius-drop-editor", "backups", projectName, BACKUP_FORMAT.format(LocalDateTime.now()));
		Path relative;
		try
		{
			relative = index.getLayout().getSelectedRoot().relativize(source).normalize();
			if (relative.isAbsolute() || relative.startsWith(".."))
			{
				relative = source.getFileName();
			}
		}
		catch (IllegalArgumentException e)
		{
			relative = source.getFileName();
		}
		
		final Path backup = backupRoot.resolve(relative == null ? source.getFileName() : relative).normalize();
		if (!backup.startsWith(backupRoot))
		{
			throw new IOException("Refusing to create a backup outside the backup root.");
		}
		
		Files.createDirectories(backup.getParent());
		Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
		return backup;
	}
	
	private String safeName(String value)
	{
		return value.replaceAll("[^A-Za-z0-9._-]", "_");
	}
	
	private void writeAtomic(Path target, String text, boolean bom) throws IOException
	{
		final byte[] body = text.getBytes(StandardCharsets.UTF_8);
		final byte[] output;
		if (bom)
		{
			output = new byte[body.length + 3];
			output[0] = (byte) 0xEF;
			output[1] = (byte) 0xBB;
			output[2] = (byte) 0xBF;
			System.arraycopy(body, 0, output, 3, body.length);
		}
		else
		{
			output = body;
		}
		
		final Path temp = Files.createTempFile(target.getParent(), target.getFileName().toString() + ".", ".dropeditor.tmp");
		Files.write(temp, output);
		try
		{
			try
			{
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException e)
			{
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			// A move that failed leaves the temporary file behind.
			Files.deleteIfExists(temp);
		}
	}
	
	/**
	 * Stores the source range of the selected NPC element.
	 */
	private static final class NpcSpan
	{
		private final int _start;
		private final int _end;
		
		private NpcSpan(int start, int end)
		{
			_start = start;
			_end = end;
		}
	}
	
	/**
	 * Stores the source range of the selected NPC drop block.
	 */
	private static final class DropSpan
	{
		private final int _start;
		private final int _end;
		
		private DropSpan(int start, int end)
		{
			_start = start;
			_end = end;
		}
	}
}
