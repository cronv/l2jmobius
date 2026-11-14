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
package org.l2jmobius.tools.editor.npc.util;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Hardened XML parsing and cached schema validation for the offline NPC editor.
 * @author BazookaRpm
 */
public final class NpcXmlUtil
{
	private static final int MAX_SCHEMAS = 8;
	private static final Map<Path, SchemaEntry> SCHEMAS = new LinkedHashMap<>(16, 0.75f, true)
	{
		private static final long serialVersionUID = 1L;
		
		@Override
		protected boolean removeEldestEntry(Entry<Path, SchemaEntry> eldest)
		{
			return size() > MAX_SCHEMAS;
		}
	};
	
	private NpcXmlUtil()
	{
	}
	
	public static Document parse(Path file) throws Exception
	{
		final DocumentBuilder builder = createBuilder();
		return builder.parse(file.toFile());
	}
	
	public static Document parse(String xml) throws Exception
	{
		final DocumentBuilder builder = createBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
	}
	
	public static void validate(String xml, Path schemaPath) throws Exception
	{
		parse(xml);
		if ((schemaPath == null) || !Files.isRegularFile(schemaPath))
		{
			return;
		}
		
		getSchema(schemaPath).newValidator().validate(new StreamSource(new StringReader(xml)));
	}
	
	private static synchronized Schema getSchema(Path schemaPath) throws Exception
	{
		final Path normalized = schemaPath.toAbsolutePath().normalize();
		final long fingerprint = schemaFingerprint(normalized.getParent());
		final SchemaEntry cached = SCHEMAS.get(normalized);
		if ((cached != null) && (cached._fingerprint == fingerprint))
		{
			return cached._schema;
		}
		
		final SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
		factory.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		factory.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "file");
		final Schema schema = factory.newSchema(normalized.toFile());
		SCHEMAS.put(normalized, new SchemaEntry(fingerprint, schema));
		return schema;
	}
	
	private static long schemaFingerprint(Path directory) throws IOException
	{
		if ((directory == null) || !Files.isDirectory(directory))
		{
			return 0L;
		}
		
		long fingerprint = 1125899906842597L;
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.xsd"))
		{
			for (Path file : stream)
			{
				final BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
				fingerprint = (31 * fingerprint) + file.getFileName().toString().toLowerCase(Locale.ROOT).hashCode();
				fingerprint = (31 * fingerprint) + attributes.size();
				fingerprint = (31 * fingerprint) + attributes.lastModifiedTime().toMillis();
			}
		}
		
		return fingerprint;
	}
	
	private static DocumentBuilder createBuilder() throws Exception
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setXIncludeAware(false);
		factory.setExpandEntityReferences(false);
		return factory.newDocumentBuilder();
	}
	
	/**
	 * Stores one compiled schema and the XSD-directory fingerprint used to invalidate it.
	 */
	private static final class SchemaEntry
	{
		private final long _fingerprint;
		private final Schema _schema;
		
		private SchemaEntry(long fingerprint, Schema schema)
		{
			_fingerprint = fingerprint;
			_schema = schema;
		}
	}
}
