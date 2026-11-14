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
package org.l2jmobius.tools.editor.item.model;

/**
 * Mutable editor state for one item while preserving its original source snapshot identity.
 * @author BazookaRpm
 */
public final class ItemDefinition
{
	private final ItemMetadataRecord _item;
	private String _originalXml;
	private String _xml;
	private String _sourceSha256;
	
	public ItemDefinition(ItemMetadataRecord item, String xml, String sourceSha256)
	{
		_item = item;
		_originalXml = xml == null ? "" : xml;
		_xml = _originalXml;
		_sourceSha256 = sourceSha256;
	}
	
	public ItemMetadataRecord getItem()
	{
		return _item;
	}
	
	public String getOriginalXml()
	{
		return _originalXml;
	}
	
	public String getXml()
	{
		return _xml;
	}
	
	public void setXml(String xml)
	{
		_xml = xml == null ? "" : xml;
	}
	
	public String getSourceSha256()
	{
		return _sourceSha256;
	}
	
	public boolean isDirty()
	{
		return !_xml.equals(_originalXml);
	}
	
	public void revert()
	{
		_xml = _originalXml;
	}
	
	public void markSaved(String xml, String sourceSha256)
	{
		_originalXml = xml == null ? "" : xml;
		_xml = _originalXml;
		_sourceSha256 = sourceSha256;
	}
}
