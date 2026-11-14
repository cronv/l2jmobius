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
package org.l2jmobius.commons.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import org.l2jmobius.commons.config.ThreadConfig;

/**
 * Contract for XML parsing utilities, providing consistent read and typed-extraction helpers.
 * @author Zoey76, Mobius
 */
public interface IXmlReader
{
	static final Logger LOGGER = Logger.getLogger(IXmlReader.class.getName());
	
	static final String JAXP_SCHEMA_LANGUAGE = "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
	static final String W3C_XML_SCHEMA = "http://www.w3.org/2001/XMLSchema";
	static final String DEFER_NODE_EXPANSION = "http://apache.org/xml/features/dom/defer-node-expansion";
	
	/** Pattern used to detect the schema referenced by a document without parsing it. */
	static final Pattern SCHEMA_LOCATION_PATTERN = Pattern.compile("noNamespaceSchemaLocation\\s*=\\s*\"([^\"]+)\"");
	
	/** Compiled schemas cached by XSD file path, avoiding schema recompilation for every parsed file. */
	static final Map<String, Schema> SCHEMA_CACHE = new ConcurrentHashMap<>();
	
	/** Reusable document builders cached per thread and schema, avoiding factory and builder creation for every parsed file. */
	static final ThreadLocal<Map<String, DocumentBuilder>> BUILDER_CACHE = ThreadLocal.withInitial(HashMap::new);
	
	/** Reports validation problems through the logger instead of the standard error stream. */
	static final ErrorHandler XML_ERROR_HANDLER = new ErrorHandler()
	{
		@Override
		public void warning(SAXParseException e)
		{
			LOGGER.warning("XML warning in " + e.getSystemId() + " at line " + e.getLineNumber() + ": " + e.getMessage());
		}
		
		@Override
		public void error(SAXParseException e)
		{
			LOGGER.warning("XML error in " + e.getSystemId() + " at line " + e.getLineNumber() + ": " + e.getMessage());
		}
		
		@Override
		public void fatalError(SAXParseException e) throws SAXException
		{
			throw e;
		}
	};
	
	/**
	 * Loads or reloads the data; clear the backing list or map before loading.
	 */
	void load();
	
	/**
	 * Parses a datapack file resolved relative to the current directory.
	 * @param path The relative path to the file to parse.
	 */
	default void parseDatapackFile(String path)
	{
		parseFile(new File(".", path));
	}
	
	/**
	 * Validates and parses an XML file, logging a warning on invalid input or parse error.
	 * @param file The {@code File} to parse.
	 */
	default void parseFile(File file)
	{
		if (!isValidXmlFile(file))
		{
			LOGGER.warning("Cannot parse " + file.getName() + ": file does not exist or is not valid.");
			return;
		}
		
		try
		{
			final byte[] content = Files.readAllBytes(file.toPath());
			final InputSource source = new InputSource(new ByteArrayInputStream(content));
			source.setSystemId(file.toURI().toASCIIString());
			parseDocument(getDocumentBuilder(file, content).parse(source), file);
		}
		catch (SAXParseException e)
		{
			LOGGER.log(Level.WARNING, "Error parsing " + file.getName() + " at line " + e.getLineNumber() + ", column " + e.getColumnNumber() + ".", e);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Error parsing " + file.getName(), e);
		}
	}
	
	/**
	 * Provides a cached, reusable {@code DocumentBuilder} configured for the schema referenced by the given file.<br>
	 * Compiled schemas are cached by XSD path and document builders per thread, so parsing many files sharing a schema compiles it only once.
	 * @param file The {@code File} being parsed.
	 * @param content The raw bytes of the file, scanned for the referenced schema location.
	 * @return A configured {@code DocumentBuilder} ready to parse.
	 * @throws Exception If no document builder could be created.
	 */
	private DocumentBuilder getDocumentBuilder(File file, byte[] content) throws Exception
	{
		// Determine the builder cache key: "-" when not validating, the XSD path when found, "" as the validating fallback.
		String key = "-";
		Schema schema = null;
		if (isValidating())
		{
			key = "";
			final Matcher matcher = SCHEMA_LOCATION_PATTERN.matcher(new String(content, 0, Math.min(content.length, 4096), StandardCharsets.ISO_8859_1));
			if (matcher.find())
			{
				final File schemaFile = new File(file.getParentFile(), matcher.group(1));
				if (schemaFile.isFile())
				{
					final String schemaPath = schemaFile.getPath();
					schema = SCHEMA_CACHE.get(schemaPath);
					if (schema == null)
					{
						try
						{
							schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(schemaFile);
							SCHEMA_CACHE.put(schemaPath, schema);
						}
						catch (Exception e)
						{
							LOGGER.warning("Could not compile schema " + schemaPath + ": " + e.getMessage());
						}
					}
					if (schema != null)
					{
						key = schemaPath;
					}
				}
			}
		}
		
		final Map<String, DocumentBuilder> builders = BUILDER_CACHE.get();
		DocumentBuilder builder = builders.get(key);
		if (builder == null)
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setIgnoringComments(true);
			try
			{
				// Fully expanded nodes parse faster than deferred expansion for small, fully traversed documents.
				factory.setAttribute(DEFER_NODE_EXPANSION, Boolean.FALSE);
			}
			catch (IllegalArgumentException e)
			{
				// The attribute is Xerces specific; ignore it when another parser implementation is used.
			}
			
			if (schema != null)
			{
				factory.setSchema(schema);
			}
			else if (key.isEmpty())
			{
				factory.setValidating(true);
				factory.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
			}
			
			builder = factory.newDocumentBuilder();
			builders.put(key, builder);
		}
		else
		{
			builder.reset();
		}
		
		builder.setErrorHandler(XML_ERROR_HANDLER);
		return builder;
	}
	
	/**
	 * Parses all files in the given directory, without recursing into subdirectories.
	 * @param directory The {@code File} folder to parse.
	 * @return {@code true} if the directory exists and was processed, {@code false} otherwise.
	 */
	default boolean parseDirectory(File directory)
	{
		return parseDirectory(directory, false);
	}
	
	/**
	 * Parses all datapack files in a directory resolved relative to the current directory.
	 * @param path The relative directory path.
	 * @param recursive Whether to search subdirectories.
	 * @return {@code true} if the directory exists and was processed, {@code false} otherwise.
	 */
	default boolean parseDatapackDirectory(String path, boolean recursive)
	{
		return parseDirectory(new File(".", path), recursive);
	}
	
	/**
	 * Parses all valid XML files in the given directory, sequentially or multithreaded per configuration.
	 * @param directory The {@code File} directory to parse.
	 * @param recursive Whether to search subdirectories.
	 * @return {@code true} if the directory exists and was processed, {@code false} otherwise.
	 */
	default boolean parseDirectory(File directory, boolean recursive)
	{
		if (!directory.exists())
		{
			LOGGER.warning("Directory not found: " + directory.getAbsolutePath());
			return false;
		}
		
		// If multithreading is enabled, use a thread pool to parse files.
		if (ThreadConfig.THREADS_FOR_LOADING)
		{
			final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
			final List<Future<?>> tasks = new ArrayList<>();
			
			final File[] files = directory.listFiles();
			if (files != null)
			{
				for (File file : files)
				{
					if (recursive && file.isDirectory())
					{
						parseDirectory(file, true);
					}
					else if (isValidXmlFile(file))
					{
						tasks.add(executorService.schedule(() -> parseFile(file), 0, TimeUnit.MILLISECONDS));
					}
				}
			}
			
			for (Future<?> task : tasks)
			{
				try
				{
					task.get();
				}
				catch (Exception e)
				{
					LOGGER.warning("Failed to parse file: " + e.getMessage());
				}
			}
			
			executorService.shutdown();
			try
			{
				executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
			}
			catch (InterruptedException e)
			{
				Thread.currentThread().interrupt();
				LOGGER.warning("Parsing process was interrupted: " + e.getMessage());
			}
		}
		else // Parse files sequentially if multithreading is not enabled.
		{
			final File[] files = directory.listFiles();
			if (files != null)
			{
				for (File file : files)
				{
					if (recursive && file.isDirectory())
					{
						parseDirectory(file, true);
					}
					else if (isValidXmlFile(file))
					{
						parseFile(file);
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Parses the given document; called from parseFile.
	 * @param document The {@code Document} to parse.
	 * @param file The {@code File} being processed.
	 */
	void parseDocument(Document document, File file);
	
	/**
	 * Parses a {@code Node} value into a {@code Boolean}.
	 * @param node The {@code Node} to parse.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The parsed {@code Boolean}, or {@code defaultValue}.
	 */
	default Boolean parseBoolean(Node node, Boolean defaultValue)
	{
		return node != null ? Boolean.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Boolean}, defaulting to {@code null}.
	 * @param node The {@code Node} to parse.
	 * @return The parsed {@code Boolean}, or {@code null} if {@code node} is {@code null}.
	 */
	default Boolean parseBoolean(Node node)
	{
		return parseBoolean(node, null);
	}
	
	/**
	 * Parses the named attribute into a {@code Boolean}.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @return The parsed {@code Boolean}, or {@code null} if absent.
	 */
	default Boolean parseBoolean(NamedNodeMap attributes, String name)
	{
		return parseBoolean(attributes.getNamedItem(name));
	}
	
	/**
	 * Parses the named attribute into a {@code Boolean}, returning a default when absent.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent.
	 * @return The parsed {@code Boolean}, or {@code defaultValue}.
	 */
	default Boolean parseBoolean(NamedNodeMap attributes, String name, Boolean defaultValue)
	{
		return parseBoolean(attributes.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Byte}.
	 * @param node The {@code Node} to parse.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The parsed {@code Byte}, or {@code defaultValue}.
	 */
	default Byte parseByte(Node node, Byte defaultValue)
	{
		return node != null ? Byte.decode(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Byte}, defaulting to {@code null}.
	 * @param node The {@code Node} to parse.
	 * @return The parsed {@code Byte}, or {@code null} if {@code node} is {@code null}.
	 */
	default Byte parseByte(Node node)
	{
		return parseByte(node, null);
	}
	
	/**
	 * Parses the named attribute into a {@code Byte}.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @return The parsed {@code Byte}, or {@code null} if absent.
	 */
	default Byte parseByte(NamedNodeMap attributes, String name)
	{
		return parseByte(attributes.getNamedItem(name));
	}
	
	/**
	 * Parses the named attribute into a {@code Byte}, returning a default when absent.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent.
	 * @return The parsed {@code Byte}, or {@code defaultValue}.
	 */
	default Byte parseByte(NamedNodeMap attributes, String name, Byte defaultValue)
	{
		return parseByte(attributes.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Short}.
	 * @param node The {@code Node} to parse.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The parsed {@code Short}, or {@code defaultValue}.
	 */
	default Short parseShort(Node node, Short defaultValue)
	{
		return node != null ? Short.decode(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Short}, defaulting to {@code null}.
	 * @param node The {@code Node} to parse.
	 * @return The parsed {@code Short}, or {@code null} if {@code node} is {@code null}.
	 */
	default Short parseShort(Node node)
	{
		return parseShort(node, null);
	}
	
	/**
	 * Parses the named attribute into a {@code Short}.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @return The parsed {@code Short}, or {@code null} if absent.
	 */
	default Short parseShort(NamedNodeMap attributes, String name)
	{
		return parseShort(attributes.getNamedItem(name));
	}
	
	/**
	 * Parses the named attribute into a {@code Short}, returning a default when absent.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent.
	 * @return The parsed {@code Short}, or {@code defaultValue}.
	 */
	default Short parseShort(NamedNodeMap attributes, String name, Short defaultValue)
	{
		return parseShort(attributes.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a {@code Node} value into an {@code int}.
	 * @param node The {@code Node} to parse.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The parsed {@code int}, or {@code defaultValue}.
	 */
	default int parseInt(Node node, Integer defaultValue)
	{
		return node != null ? Integer.decode(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a {@code Node} value into an {@code int}, defaulting to {@code -1}.
	 * @param node The {@code Node} to parse.
	 * @return The parsed {@code int}, or {@code -1} if {@code node} is {@code null}.
	 */
	default int parseInt(Node node)
	{
		return parseInt(node, -1);
	}
	
	/**
	 * Parses a {@code Node} value into an {@code Integer}.
	 * @param node The {@code Node} to parse.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The parsed {@code Integer}, or {@code defaultValue}.
	 */
	default Integer parseInteger(Node node, Integer defaultValue)
	{
		return node != null ? Integer.decode(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a {@code Node} value into an {@code Integer}, defaulting to {@code null}.
	 * @param node The {@code Node} to parse.
	 * @return The parsed {@code Integer}, or {@code null} if {@code node} is {@code null}.
	 */
	default Integer parseInteger(Node node)
	{
		return parseInteger(node, null);
	}
	
	/**
	 * Parses the named attribute into an {@code Integer}.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @return The parsed {@code Integer}, or {@code null} if absent.
	 */
	default Integer parseInteger(NamedNodeMap attributes, String name)
	{
		return parseInteger(attributes.getNamedItem(name));
	}
	
	/**
	 * Parses the named attribute into an {@code Integer}, returning a default when absent.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent.
	 * @return The parsed {@code Integer}, or {@code defaultValue}.
	 */
	default Integer parseInteger(NamedNodeMap attributes, String name, Integer defaultValue)
	{
		return parseInteger(attributes.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Long}.
	 * @param node The {@code Node} to parse.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The parsed {@code Long}, or {@code defaultValue}.
	 */
	default Long parseLong(Node node, Long defaultValue)
	{
		return node != null ? Long.decode(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Long}, defaulting to {@code null}.
	 * @param node The {@code Node} to parse.
	 * @return The parsed {@code Long}, or {@code null} if {@code node} is {@code null}.
	 */
	default Long parseLong(Node node)
	{
		return parseLong(node, null);
	}
	
	/**
	 * Parses the named attribute into a {@code Long}.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @return The parsed {@code Long}, or {@code null} if absent.
	 */
	default Long parseLong(NamedNodeMap attributes, String name)
	{
		return parseLong(attributes.getNamedItem(name));
	}
	
	/**
	 * Parses the named attribute into a {@code Long}, returning a default when absent.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent.
	 * @return The parsed {@code Long}, or {@code defaultValue}.
	 */
	default Long parseLong(NamedNodeMap attributes, String name, Long defaultValue)
	{
		return parseLong(attributes.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Float}.
	 * @param node The {@code Node} to parse.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The parsed {@code Float}, or {@code defaultValue}.
	 */
	default Float parseFloat(Node node, Float defaultValue)
	{
		return node != null ? Float.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Float}, defaulting to {@code null}.
	 * @param node The {@code Node} to parse.
	 * @return The parsed {@code Float}, or {@code null} if {@code node} is {@code null}.
	 */
	default Float parseFloat(Node node)
	{
		return parseFloat(node, null);
	}
	
	/**
	 * Parses the named attribute into a {@code Float}.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @return The parsed {@code Float}, or {@code null} if absent.
	 */
	default Float parseFloat(NamedNodeMap attributes, String name)
	{
		return parseFloat(attributes.getNamedItem(name));
	}
	
	/**
	 * Parses the named attribute into a {@code Float}, returning a default when absent.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent.
	 * @return The parsed {@code Float}, or {@code defaultValue}.
	 */
	default Float parseFloat(NamedNodeMap attributes, String name, Float defaultValue)
	{
		return parseFloat(attributes.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Double}.
	 * @param node The {@code Node} to parse.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The parsed {@code Double}, or {@code defaultValue}.
	 */
	default Double parseDouble(Node node, Double defaultValue)
	{
		return node != null ? Double.valueOf(node.getNodeValue()) : defaultValue;
	}
	
	/**
	 * Parses a {@code Node} value into a {@code Double}, defaulting to {@code null}.
	 * @param node The {@code Node} to parse.
	 * @return The parsed {@code Double}, or {@code null} if {@code node} is {@code null}.
	 */
	default Double parseDouble(Node node)
	{
		return parseDouble(node, null);
	}
	
	/**
	 * Parses the named attribute into a {@code Double}.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @return The parsed {@code Double}, or {@code null} if absent.
	 */
	default Double parseDouble(NamedNodeMap attributes, String name)
	{
		return parseDouble(attributes.getNamedItem(name));
	}
	
	/**
	 * Parses the named attribute into a {@code Double}, returning a default when absent.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent.
	 * @return The parsed {@code Double}, or {@code defaultValue}.
	 */
	default Double parseDouble(NamedNodeMap attributes, String name, Double defaultValue)
	{
		return parseDouble(attributes.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Retrieves the text value of a {@code Node}, or a default when it is {@code null}.
	 * @param node The {@code Node} to read.
	 * @param defaultValue The value returned if {@code node} is {@code null}.
	 * @return The node value, or {@code defaultValue}.
	 */
	default String parseString(Node node, String defaultValue)
	{
		return node != null ? node.getNodeValue() : defaultValue;
	}
	
	/**
	 * Retrieves the text value of a {@code Node}, defaulting to {@code null}.
	 * @param node The {@code Node} to read.
	 * @return The node value, or {@code null} if {@code node} is {@code null}.
	 */
	default String parseString(Node node)
	{
		return parseString(node, null);
	}
	
	/**
	 * Retrieves the value of the named attribute.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @return The attribute value, or {@code null} if absent.
	 */
	default String parseString(NamedNodeMap attributes, String name)
	{
		return parseString(attributes.getNamedItem(name));
	}
	
	/**
	 * Retrieves the value of the named attribute, or a default when absent.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent.
	 * @return The attribute value, or {@code defaultValue}.
	 */
	default String parseString(NamedNodeMap attributes, String name, String defaultValue)
	{
		return parseString(attributes.getNamedItem(name), defaultValue);
	}
	
	/**
	 * Parses a {@code Node} value into a constant of the enum, returning a default on null or invalid input.
	 * @param <T> The enum type.
	 * @param node The {@code Node} to parse.
	 * @param enumClass The enum class.
	 * @param defaultValue The value returned if {@code node} is {@code null} or invalid.
	 * @return The parsed enum constant, or {@code defaultValue}.
	 */
	default <T extends Enum<T>> T parseEnum(Node node, Class<T> enumClass, T defaultValue)
	{
		if (node == null)
		{
			return defaultValue;
		}
		
		try
		{
			return Enum.valueOf(enumClass, node.getNodeValue());
		}
		catch (IllegalArgumentException e)
		{
			LOGGER.warning("Invalid value for node: " + node.getNodeName() + ", specified value: " + node.getNodeValue() + " should be an enum of type \"" + enumClass.getSimpleName() + "\". Using default value: " + defaultValue);
			return defaultValue;
		}
	}
	
	/**
	 * Parses a {@code Node} value into a constant of the enum, defaulting to {@code null}.
	 * @param <T> The enum type.
	 * @param node The {@code Node} to parse.
	 * @param enumClass The enum class.
	 * @return The parsed enum constant, or {@code null} if {@code node} is {@code null} or invalid.
	 */
	default <T extends Enum<T>> T parseEnum(Node node, Class<T> enumClass)
	{
		return parseEnum(node, enumClass, null);
	}
	
	/**
	 * Parses the named attribute into a constant of the enum.
	 * @param <T> The enum type.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param enumClass The enum class.
	 * @param name The attribute name.
	 * @return The parsed enum constant, or {@code null} if absent or invalid.
	 */
	default <T extends Enum<T>> T parseEnum(NamedNodeMap attributes, Class<T> enumClass, String name)
	{
		return parseEnum(attributes.getNamedItem(name), enumClass);
	}
	
	/**
	 * Parses the named attribute into a constant of the enum, returning a default when absent or invalid.
	 * @param <T> The enum type.
	 * @param attributes The {@code NamedNodeMap} of attributes.
	 * @param enumClass The enum class.
	 * @param name The attribute name.
	 * @param defaultValue The value returned if absent or invalid.
	 * @return The parsed enum constant, or {@code defaultValue}.
	 */
	default <T extends Enum<T>> T parseEnum(NamedNodeMap attributes, Class<T> enumClass, String name, T defaultValue)
	{
		return parseEnum(attributes.getNamedItem(name), enumClass, defaultValue);
	}
	
	/**
	 * Collects all attributes of a {@code Node} into a name-to-value {@code Map}.
	 * @param node The {@code Node} to read.
	 * @return A {@code Map} of attribute names to values.
	 */
	default Map<String, Object> parseAttributes(Node node)
	{
		final NamedNodeMap attributes = node.getAttributes();
		final Map<String, Object> attributeMap = new LinkedHashMap<>();
		for (int i = 0; i < attributes.getLength(); i++)
		{
			final Node attribute = attributes.item(i);
			attributeMap.put(attribute.getNodeName(), attribute.getNodeValue());
		}
		
		return attributeMap;
	}
	
	/**
	 * Applies the action to every child of the given {@code Node}.
	 * @param node The parent {@code Node}.
	 * @param action The {@code Consumer} applied to each child.
	 */
	default void forEach(Node node, Consumer<Node> action)
	{
		forEach(node, _ -> true, action);
	}
	
	/**
	 * Applies the action to each child whose name matches, case-insensitively.
	 * @param node The parent {@code Node}.
	 * @param nodeName The child name to match.
	 * @param action The {@code Consumer} applied to each match.
	 */
	default void forEach(Node node, String nodeName, Consumer<Node> action)
	{
		forEach(node, child -> nodeName.equalsIgnoreCase(child.getNodeName()), action);
	}
	
	/**
	 * Applies the action to each child of the given {@code Node} that satisfies the filter.
	 * @param node The parent {@code Node}.
	 * @param filter The {@code Predicate} deciding whether a child is processed.
	 * @param action The {@code Consumer} applied to each accepted child.
	 */
	default void forEach(Node node, Predicate<Node> filter, Consumer<Node> action)
	{
		final NodeList children = node.getChildNodes();
		for (int i = 0; i < children.getLength(); i++)
		{
			final Node childNode = children.item(i);
			if (filter.test(childNode))
			{
				action.accept(childNode);
			}
		}
	}
	
	/**
	 * Checks whether the {@code File} exists and has an {@code .xml} extension.
	 * @param file The {@code File} to check.
	 * @return {@code true} if valid, {@code false} otherwise.
	 */
	default boolean isValidXmlFile(File file)
	{
		return (file != null) && file.isFile() && file.getName().toLowerCase().endsWith(".xml");
	}
	
	/**
	 * Indicates whether parsing validates against a schema; defaults to {@code true}, override to disable.
	 * @return {@code true} if validating, {@code false} otherwise.
	 */
	default boolean isValidating()
	{
		return true;
	}
	
	/**
	 * Checks whether the {@code Node} is an element node.
	 * @param node The {@code Node} to check.
	 * @return {@code true} if an element, {@code false} otherwise.
	 */
	static boolean isNode(Node node)
	{
		return node.getNodeType() == Node.ELEMENT_NODE;
	}
}
