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
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Locale;

/**
 * Utility for locating files and resources within the environment.
 */
public class Locator
{
	/**
	 * Prevents instantiation of this utility class.
	 */
	private Locator()
	{
	}
	
	/**
	 * Finds the source file on disk for the given class.
	 * @param c the class whose source file is located
	 * @return the source {@code File}
	 */
	public static File getClassSource(Class<?> c)
	{
		final String classResource = c.getName().replace('.', '/') + ".class";
		return getResourceSource(c.getClassLoader(), classResource);
	}
	
	/**
	 * Locates the {@code File} backing a resource via the given loader.
	 * @param classLoader the loader to search with, or {@code null} for the system loader
	 * @param resource the resource name to find
	 * @return the resource {@code File}, or {@code null} if not found
	 */
	public static File getResourceSource(ClassLoader classLoader, String resource)
	{
		ClassLoader c = classLoader;
		if (c == null)
		{
			c = Locator.class.getClassLoader();
		}
		
		URL url = null;
		if (c == null)
		{
			url = ClassLoader.getSystemResource(resource);
		}
		else
		{
			url = c.getResource(resource);
		}
		
		if (url != null)
		{
			final String u = url.toString();
			if (u.startsWith("jar:file:"))
			{
				final int pling = u.indexOf('!');
				final String jarName = u.substring(4, pling);
				return new File(fromURI(jarName));
			}
			else if (u.startsWith("file:"))
			{
				final int tail = u.indexOf(resource);
				final String dirName = u.substring(0, tail);
				return new File(fromURI(dirName));
			}
		}
		
		return null;
	}
	
	/**
	 * Converts a file URI string into a system-specific file path.
	 * @param uriValue the URI string to convert
	 * @return the decoded file path
	 */
	public static String fromURI(String uriValue)
	{
		String uri = uriValue;
		URL url = null;
		try
		{
			// Java 19
			// url = new URL(uri);
			// Java 20
			url = URI.create(uri).toURL();
		}
		catch (MalformedURLException emYouEarlEx)
		{
			// Ignore malformed exception
		}
		
		if ((url == null) || !("file".equals(url.getProtocol())))
		{
			throw new IllegalArgumentException("Can only handle valid file: URIs");
		}
		
		final StringBuilder buf = new StringBuilder(url.getHost());
		if (!buf.isEmpty())
		{
			buf.insert(0, File.separatorChar).insert(0, File.separatorChar);
		}
		
		final String file = url.getFile();
		final int queryPos = file.indexOf('?');
		buf.append((queryPos < 0) ? file : file.substring(0, queryPos));
		uri = buf.toString().replace('/', File.separatorChar);
		if ((File.pathSeparatorChar == ';') && uri.startsWith("\\") && (uri.length() > 2) && Character.isLetter(uri.charAt(1)) && (uri.lastIndexOf(':') > -1))
		{
			uri = uri.substring(1);
		}
		
		return decodeUri(uri);
	}
	
	/**
	 * Decodes percent-encoded sequences in the given URI string.
	 * @param uri the encoded string
	 * @return the decoded string
	 */
	private static String decodeUri(String uri)
	{
		if (uri.indexOf('%') == -1)
		{
			return uri;
		}
		
		final StringBuilder sb = new StringBuilder();
		final CharacterIterator iter = new StringCharacterIterator(uri);
		for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next())
		{
			if (c == '%')
			{
				final char c1 = iter.next();
				if (c1 != CharacterIterator.DONE)
				{
					final int i1 = Character.digit(c1, 16);
					final char c2 = iter.next();
					if (c2 != CharacterIterator.DONE)
					{
						final int i2 = Character.digit(c2, 16);
						sb.append((char) ((i1 << 4) + i2));
					}
				}
			}
			else
			{
				sb.append(c);
			}
		}
		
		return sb.toString();
	}
	
	/**
	 * Locates the {@code tools.jar} file, checking the classpath then {@code java.home}.
	 * @return the {@code tools.jar} {@code File}, or {@code null} if unavailable
	 */
	public static File getToolsJar()
	{
		// Firstly check if the tools jar is already in the classpath.
		boolean toolsJarAvailable = false;
		try
		{
			// Just check whether this throws an exception.
			Class.forName("com.sun.tools.javac.Main");
			toolsJarAvailable = true;
		}
		catch (Exception e)
		{
			try
			{
				Class.forName("sun.tools.javac.Main");
				toolsJarAvailable = true;
			}
			catch (Exception e2)
			{
				// ignore
			}
		}
		
		if (toolsJarAvailable)
		{
			return null;
		}
		
		// Could not find the compiler; trying to locate tools.jar based on the java.home setting.
		String javaHome = System.getProperty("java.home");
		if (javaHome.toLowerCase(Locale.US).endsWith("jre"))
		{
			javaHome = javaHome.substring(0, javaHome.length() - 4);
		}
		
		final File toolsJar = new File(javaHome + "/lib/tools.jar");
		if (!toolsJar.exists())
		{
			System.out.println("Unable to locate tools.jar. " + "Expected to find it in " + toolsJar.getPath());
			return null;
		}
		
		return toolsJar;
	}
	
	/**
	 * Returns the {@code URL} array of {@code .jar} files at the given location.
	 * @param location the directory to search
	 * @return the matching {@code URL} array
	 * @throws MalformedURLException if a path cannot be converted to a {@code URL}
	 */
	public static URL[] getLocationURLs(File location) throws MalformedURLException
	{
		return getLocationURLs(location, new String[]
		{
			".jar"
		});
	}
	
	/**
	 * Returns the {@code URL} array of files at the given location matching the extensions.
	 * @param location the file or directory to search
	 * @param extensions the file extensions to match
	 * @return the matching {@code URL} array, empty if the location does not exist
	 * @throws MalformedURLException if a path cannot be converted to a {@code URL}
	 */
	public static URL[] getLocationURLs(File location, String[] extensions) throws MalformedURLException
	{
		URL[] urls = new URL[0];
		
		if (!location.exists())
		{
			return urls;
		}
		
		if (!location.isDirectory())
		{
			urls = new URL[1];
			final String path = location.getPath();
			for (String extension : extensions)
			{
				if (path.toLowerCase().endsWith(extension))
				{
					urls[0] = location.toURI().toURL();
					break;
				}
			}
			
			return urls;
		}
		
		final File[] matches = location.listFiles((FilenameFilter) (_, name) ->
		{
			for (String extension : extensions)
			{
				if (name.toLowerCase().endsWith(extension))
				{
					return true;
				}
			}
			
			return false;
		});
		
		final int matchesLength = matches.length;
		urls = new URL[matchesLength];
		for (int i = 0; i < matchesLength; ++i)
		{
			urls[i] = matches[i].toURI().toURL();
		}
		
		return urls;
	}
}
