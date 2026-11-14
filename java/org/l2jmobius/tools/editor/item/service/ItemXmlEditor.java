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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.w3c.dom.Element;

import org.l2jmobius.tools.editor.item.model.ItemDefinition;
import org.l2jmobius.tools.editor.item.model.ItemMetadataIndex;
import org.l2jmobius.tools.editor.item.model.ItemMetadataRecord;
import org.l2jmobius.tools.editor.item.model.ItemProjectLayout;
import org.l2jmobius.tools.editor.npc.util.NpcXmlUtil;
import org.l2jmobius.tools.editor.service.FileSnapshot;
import org.l2jmobius.tools.editor.service.FileSnapshotCache;

/**
 * Loads and safely persists one top-level item block while preserving unrelated XML source text byte-for-byte.
 * @author BazookaRpm
 */
public final class ItemXmlEditor
{
	private static final DateTimeFormatter BACKUP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");
	private final ItemSemanticValidator _semanticValidator;
	
	public ItemXmlEditor(ItemSemanticValidator semanticValidator)
	{
		_semanticValidator = semanticValidator;
	}
	
	public ItemDefinition load(ItemMetadataRecord item) throws Exception
	{
		final FileSnapshot snapshot = FileSnapshotCache.get(item.getFile());
		final String text = snapshot.getText();
		
		// Swing text areas edit with LF, so the definition keeps LF and saving restores the file line ending.
		final String xml = toLf(ItemXmlSpans.findTopLevelItem(text, item.getId(), item.getIdOccurrence()).getXml(text));
		validateIdentity(item, xml);
		return new ItemDefinition(item, xml, snapshot.getSha256());
	}
	
	public void validate(ItemProjectLayout layout, ItemMetadataIndex itemIndex, ItemDefinition definition) throws Exception
	{
		buildCandidate(layout, itemIndex, definition, FileSnapshotCache.readFresh(definition.getItem().getFile()));
	}
	
	/**
	 * Validates and writes the item. The caller applies the result to the definition on its own thread.
	 * @param layout the project layout
	 * @param itemIndex the item metadata index
	 * @param definition the edited item
	 * @return the saved state, or {@code null} when the item has no changes
	 * @throws Exception when validation, backup or writing fails
	 */
	public SavedItem save(ItemProjectLayout layout, ItemMetadataIndex itemIndex, ItemDefinition definition) throws Exception
	{
		if (!definition.isDirty())
		{
			return null;
		}
		
		final ItemMetadataRecord item = definition.getItem();
		final Path file = item.getFile();
		final FileSnapshot snapshot = FileSnapshotCache.readFresh(file);
		final String candidate = buildCandidate(layout, itemIndex, definition, snapshot);
		final Path backup = createBackup(layout, file);
		writeAtomic(file, candidate, snapshot.hasBom());
		final FileSnapshot saved = FileSnapshotCache.readFresh(file);
		final String savedText = saved.getText();
		return new SavedItem(backup, toLf(ItemXmlSpans.findTopLevelItem(savedText, item.getId(), item.getIdOccurrence()).getXml(savedText)), saved.getSha256());
	}
	
	private static void requireSchema(ItemProjectLayout layout)
	{
		if ((layout == null) || !Files.isRegularFile(layout.getItemSchema()))
		{
			throw new IllegalStateException("items.xsd is required for validation and saving.");
		}
	}
	
	private static void ensureSourceUnchanged(ItemDefinition definition, FileSnapshot snapshot)
	{
		if (!definition.getSourceSha256().equals(snapshot.getSha256()))
		{
			throw new IllegalStateException("The item XML changed on disk after it was opened. Reload the item before saving.");
		}
	}
	
	private String buildCandidate(ItemProjectLayout layout, ItemMetadataIndex itemIndex, ItemDefinition definition, FileSnapshot snapshot) throws Exception
	{
		requireSchema(layout);
		ensureSourceUnchanged(definition, snapshot);
		final ItemMetadataRecord item = definition.getItem();
		final String xml = definition.getXml();
		validateIdentity(item, xml);
		_semanticValidator.validateOrThrow(layout, itemIndex, definition.getOriginalXml(), xml);
		final String source = snapshot.getText();
		final ItemXmlSpans.ElementSpan span = ItemXmlSpans.findTopLevelItem(source, item.getId(), item.getIdOccurrence());
		final String candidate = source.substring(0, span.getStart()) + toLf(xml).replace("\n", snapshot.getLineEnding()) + source.substring(span.getEnd());
		NpcXmlUtil.validate(candidate, layout.getItemSchema());
		return candidate;
	}
	
	private static String toLf(String xml)
	{
		return xml.replace("\r\n", "\n");
	}
	
	private static void validateIdentity(ItemMetadataRecord item, String itemXml) throws Exception
	{
		final Element root = NpcXmlUtil.parse(itemXml).getDocumentElement();
		if (!"item".equals(root.getNodeName()))
		{
			throw new IllegalArgumentException("Editor content must contain exactly one top-level <item> element.");
		}
		
		final int editedId;
		try
		{
			editedId = Integer.parseInt(root.getAttribute("id"));
		}
		catch (NumberFormatException e)
		{
			throw new IllegalArgumentException("Item id must be a valid integer.", e);
		}
		
		final int id = item.getId();
		if (editedId != id)
		{
			throw new IllegalArgumentException("Item id is immutable. Expected " + id + " but found " + editedId + ".");
		}
		
		final String type = root.getAttribute("type");
		final String expectedType = item.getType();
		if (!expectedType.equals(type))
		{
			throw new IllegalArgumentException("Item type is immutable in Item Editor V1. Expected " + expectedType + " but found " + type + ".");
		}
	}
	
	private static Path createBackup(ItemProjectLayout layout, Path source) throws IOException
	{
		final Path chronicleName = layout.getChronicleRoot().getFileName();
		final Path backupRoot = Path.of(System.getProperty("user.home"), ".l2jmobius-item-editor", "backups", safeName(chronicleName == null ? "project" : chronicleName.toString()), BACKUP_FORMAT.format(LocalDateTime.now()));
		Path relative;
		try
		{
			relative = layout.getSelectedRoot().toAbsolutePath().normalize().relativize(source.toAbsolutePath().normalize());
		}
		catch (IllegalArgumentException e)
		{
			// A source on another root falls back to its file name below.
			relative = null;
		}
		
		if ((relative == null) || relative.isAbsolute() || relative.startsWith(".."))
		{
			relative = source.getFileName();
		}
		
		final Path backup = backupRoot.resolve(relative).normalize();
		if (!backup.startsWith(backupRoot))
		{
			throw new IOException("Backup path escaped its configured root.");
		}
		
		Files.createDirectories(backup.getParent());
		Files.copy(source, backup, StandardCopyOption.COPY_ATTRIBUTES);
		return backup;
	}
	
	private static String safeName(String value)
	{
		final String safe = value.replaceAll("[^A-Za-z0-9._-]", "_");
		return safe.isBlank() ? "project" : safe;
	}
	
	private static void writeAtomic(Path file, String text, boolean bom) throws IOException
	{
		final Path temp = Files.createTempFile(file.toAbsolutePath().normalize().getParent(), file.getFileName().toString(), ".item-editor.tmp");
		try
		{
			final byte[] content = text.getBytes(StandardCharsets.UTF_8);
			if (bom)
			{
				final byte[] bytes = new byte[content.length + 3];
				bytes[0] = (byte) 0xEF;
				bytes[1] = (byte) 0xBB;
				bytes[2] = (byte) 0xBF;
				System.arraycopy(content, 0, bytes, 3, content.length);
				Files.write(temp, bytes);
			}
			else
			{
				Files.write(temp, content);
			}
			
			try
			{
				Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			}
			catch (AtomicMoveNotSupportedException e)
			{
				Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally
		{
			Files.deleteIfExists(temp);
		}
		
		FileSnapshotCache.invalidate(file);
	}
	
	public static final class SavedItem
	{
		private final Path _backup;
		private final String _xml;
		private final String _sha256;
		
		private SavedItem(Path backup, String xml, String sha256)
		{
			_backup = backup;
			_xml = xml;
			_sha256 = sha256;
		}
		
		public Path getBackup()
		{
			return _backup;
		}
		
		public String getXml()
		{
			return _xml;
		}
		
		public String getSha256()
		{
			return _sha256;
		}
	}
}
