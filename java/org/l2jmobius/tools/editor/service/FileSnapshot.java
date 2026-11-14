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

/**
 * Immutable single-read snapshot of one UTF-8 XML source file.
 * @author BazookaRpm
 */
public final class FileSnapshot
{
	private final String _text;
	private final String _lineEnding;
	private final boolean _bom;
	private final String _sha256;
	
	FileSnapshot(String text, String lineEnding, boolean bom, String sha256)
	{
		_text = text;
		_lineEnding = lineEnding;
		_bom = bom;
		_sha256 = sha256;
	}
	
	public String getText()
	{
		return _text;
	}
	
	public String getLineEnding()
	{
		return _lineEnding;
	}
	
	public boolean hasBom()
	{
		return _bom;
	}
	
	public String getSha256()
	{
		return _sha256;
	}
}
