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
package org.l2jmobius.tools.editor.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Small shared LRU cache that avoids repeated reads of recently opened XML files while preserving fresh reads before writes.
 * @author BazookaRpm
 */
public final class FileSnapshotCache
{
	private static final int MAX_ENTRIES = 8;
	private static final Map<Path, CacheEntry> CACHE = new LinkedHashMap<>(16, 0.75f, true)
	{
		private static final long serialVersionUID = 1L;
		
		@Override
		protected boolean removeEldestEntry(Entry<Path, CacheEntry> eldest)
		{
			return size() > MAX_ENTRIES;
		}
	};
	
	private FileSnapshotCache()
	{
	}
	
	public static synchronized FileSnapshot get(Path file) throws IOException
	{
		final Path normalized = file.toAbsolutePath().normalize();
		final BasicFileAttributes attributes = Files.readAttributes(normalized, BasicFileAttributes.class);
		final CacheEntry cached = CACHE.get(normalized);
		if ((cached != null) && cached.matches(attributes))
		{
			return cached._snapshot;
		}
		
		return readAndCache(normalized, attributes);
	}
	
	public static synchronized FileSnapshot readFresh(Path file) throws IOException
	{
		final Path normalized = file.toAbsolutePath().normalize();
		return readAndCache(normalized, Files.readAttributes(normalized, BasicFileAttributes.class));
	}
	
	public static synchronized void invalidate(Path file)
	{
		if (file != null)
		{
			CACHE.remove(file.toAbsolutePath().normalize());
		}
	}
	
	private static FileSnapshot readAndCache(Path file, BasicFileAttributes attributes) throws IOException
	{
		final byte[] bytes = Files.readAllBytes(file);
		final boolean bom = (bytes.length >= 3) && ((bytes[0] & 0xFF) == 0xEF) && ((bytes[1] & 0xFF) == 0xBB) && ((bytes[2] & 0xFF) == 0xBF);
		final String text = new String(bytes, bom ? 3 : 0, bytes.length - (bom ? 3 : 0), StandardCharsets.UTF_8);
		final String lineEnding = text.contains("\r\n") ? "\r\n" : "\n";
		final FileSnapshot snapshot = new FileSnapshot(text, lineEnding, bom, sha256(bytes));
		CACHE.put(file, new CacheEntry(attributes.size(), attributes.lastModifiedTime().toMillis(), snapshot));
		return snapshot;
	}
	
	private static String sha256(byte[] bytes)
	{
		try
		{
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 is not available in this JDK.", e);
		}
	}
	
	/**
	 * Stores the file metadata used to validate one cached snapshot.
	 */
	private static final class CacheEntry
	{
		private final long _size;
		private final long _lastModified;
		private final FileSnapshot _snapshot;
		
		private CacheEntry(long size, long lastModified, FileSnapshot snapshot)
		{
			_size = size;
			_lastModified = lastModified;
			_snapshot = snapshot;
		}
		
		private boolean matches(BasicFileAttributes attributes)
		{
			return (_size == attributes.size()) && (_lastModified == attributes.lastModifiedTime().toMillis());
		}
	}
}
