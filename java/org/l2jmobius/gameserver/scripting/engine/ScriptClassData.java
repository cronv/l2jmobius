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
package org.l2jmobius.gameserver.scripting.engine;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.nio.file.Path;

import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.tools.JavaFileObject;

/**
 * This class holds the metadata and data for a specific script class.<br>
 * It implements {@code JavaFileObject} to facilitate the loading of scripts during the compilation or execution process.
 * @author HorridoJoho, Mobius
 */
public class ScriptClassData implements JavaFileObject
{
	private final Path _sourcePath;
	private final String _javaName;
	private final String _javaSimpleName;
	private final ByteArrayOutputStream _out;
	
	/**
	 * Creates a new instance of {@code ScriptClassData}.<br>
	 * This constructor initializes the source path and names for a script class.<br>
	 * It also prepares an internal output stream for data handling.
	 * @param sourcePath The file system path to the source code.
	 * @param javaName The full name of the Java class.
	 * @param javaSimpleName The simple name of the Java class.
	 */
	public ScriptClassData(Path sourcePath, String javaName, String javaSimpleName)
	{
		_sourcePath = sourcePath;
		_javaName = javaName;
		_javaSimpleName = javaSimpleName;
		_out = new ByteArrayOutputStream();
	}
	
	/**
	 * Retrieves the file path of the source code.<br>
	 * This method returns the {@code Path} object associated with this script class.
	 * @return The {@code Path} representing the source file location.
	 */
	public Path getSourcePath()
	{
		return _sourcePath;
	}
	
	/**
	 * Retrieves the full Java name of the script class.<br>
	 * This includes the package path prefix.
	 * @return The {@code String} representing the full Java name.
	 */
	public String getJavaName()
	{
		return _javaName;
	}
	
	/**
	 * Retrieves the simple name of the Java class.<br>
	 * This is the name used in the source code without the package path.
	 * @return The {@code String} representing the simple name.
	 */
	public String getJavaSimpleName()
	{
		return _javaSimpleName;
	}
	
	/**
	 * Retrieves the raw byte content of the Java file.<br>
	 * This method returns the data stored in the internal {@code ByteArrayOutputStream}.
	 * @return a {@code byte[]} containing the source code data.
	 */
	public byte[] getJavaData()
	{
		return _out.toByteArray();
	}
	
	/**
	 * Converts the current data into a {@code URI} object.<br>
	 * This method currently returns {@code null}.
	 * @return the {@code URI} representation of this data, or {@code null}.
	 */
	@Override
	public URI toUri()
	{
		return null;
	}
	
	/**
	 * Retrieves the name of the script class.<br>
	 * This method currently returns {@code null}.
	 * @return The name as a {@code String}.
	 */
	@Override
	public String getName()
	{
		return null;
	}
	
	/**
	 * Opens an {@code InputStream} for the script data.<br>
	 * This method currently returns {@code null}.
	 * @return a {@code InputStream} or {@code null}.
	 */
	@Override
	public InputStream openInputStream()
	{
		return null;
	}
	
	/**
	 * Opens an {@code OutputStream} for the script data.<br>
	 * This method returns the internal buffer used to store the content.
	 * @return The {@code OutputStream} associated with this class.
	 */
	@Override
	public OutputStream openOutputStream()
	{
		return _out;
	}
	
	/**
	 * Opens a {@code Reader} for the script content.<br>
	 * This method allows you to read the text data of the file.
	 * @param ignoreEncodingErrors Set to {@code true} to skip errors during decoding, or {@code false} to throw an exception.
	 * @return A {@code Reader} object for the script content, or {@code null}.
	 */
	@Override
	public Reader openReader(boolean ignoreEncodingErrors)
	{
		return null;
	}
	
	/**
	 * Retrieves the character content of the script file.<br>
	 * This method currently returns {@code null}.
	 * @param ignoreEncodingErrors Set to {@code true} to skip errors during decoding.
	 * @return The content as a {@code CharSequence}, or {@code null}.
	 */
	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors)
	{
		return null;
	}
	
	/**
	 * Opens a {@code Writer} for the script file.<br>
	 * This method currently returns {@code null}.
	 * @return a {@code Writer} object or {@code null}.
	 */
	@Override
	public Writer openWriter()
	{
		return null;
	}
	
	/**
	 * Returns the last modified time of the script file.<br>
	 * This method currently returns a default value of {@code 0}.
	 * @return The timestamp of the last modification as a {@code long}.
	 */
	@Override
	public long getLastModified()
	{
		return 0;
	}
	
	/**
	 * Attempts to delete the file associated with this {@code ScriptClassData}.<br>
	 * This method currently does not perform any deletion.
	 * @return {@code false} because the deletion is not implemented.
	 */
	@Override
	public boolean delete()
	{
		return false;
	}
	
	/**
	 * Returns the type of the script class.<br>
	 * This method always returns {@code Kind.CLASS}.
	 * @return The {@code Kind} representing the script type.
	 */
	@Override
	public Kind getKind()
	{
		return Kind.CLASS;
	}
	
	/**
	 * Checks if a given name and kind match the current class data.<br>
	 * It returns {@code true} only if the {@code kind} is {@code Kind.CLASS}.<br>
	 * It also verifies that the {@code simpleName} matches the internal Java simple name.
	 * @param simpleName The name to compare against the internal name.
	 * @param kind The type of object to check for compatibility.
	 * @return {@code true} if both the name and kind are compatible, otherwise {@code false}.
	 */
	@Override
	public boolean isNameCompatible(String simpleName, Kind kind)
	{
		return (kind == Kind.CLASS) && (_javaSimpleName.contentEquals(simpleName));
	}
	
	/**
	 * Retrieves the nesting level of this script class.<br>
	 * This method always returns {@code NestingKind.TOP_LEVEL}.
	 * @return the {@code NestingKind} of the class.
	 */
	@Override
	public NestingKind getNestingKind()
	{
		return NestingKind.TOP_LEVEL;
	}
	
	/**
	 * Retrieves the access level of the script class.<br>
	 * This method returns {@code null} for this implementation.
	 * @return the {@code Modifier} representing the access level or {@code null}.
	 */
	@Override
	public Modifier getAccessLevel()
	{
		return null;
	}
}
