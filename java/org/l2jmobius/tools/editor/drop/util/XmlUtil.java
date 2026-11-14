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
package org.l2jmobius.tools.editor.drop.util;

import java.io.StringReader;
import java.nio.file.Path;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.xml.sax.InputSource;

/**
 * Creates hardened DOM parsers for Drop Editor XML reads and validation.
 * @author BazookaRpm
 */
public final class XmlUtil
{
	private XmlUtil()
	{
	}
	
	public static Document parse(Path file) throws Exception
	{
		return newBuilder().parse(file.toFile());
	}
	
	public static Document parse(String xml) throws Exception
	{
		return newBuilder().parse(new InputSource(new StringReader(xml)));
	}
	
	private static DocumentBuilder newBuilder() throws Exception
	{
		final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setNamespaceAware(false);
		factory.setExpandEntityReferences(false);
		factory.setXIncludeAware(false);
		setFeature(factory, "http://apache.org/xml/features/disallow-doctype-decl", true);
		setFeature(factory, "http://xml.org/sax/features/external-general-entities", false);
		setFeature(factory, "http://xml.org/sax/features/external-parameter-entities", false);
		setFeature(factory, "http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		return factory.newDocumentBuilder();
	}
	
	private static void setFeature(DocumentBuilderFactory factory, String feature, boolean value) throws Exception
	{
		factory.setFeature(feature, value);
	}
}
