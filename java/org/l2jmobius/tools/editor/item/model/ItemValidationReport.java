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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable semantic validation report for one item definition.
 * @author BazookaRpm
 */
public final class ItemValidationReport
{
	private final List<ItemValidationIssue> _issues;
	
	public ItemValidationReport(List<ItemValidationIssue> issues)
	{
		_issues = Collections.unmodifiableList(new ArrayList<>(issues));
	}
	
	public List<ItemValidationIssue> getIssues()
	{
		return _issues;
	}
	
	public boolean hasErrors()
	{
		for (ItemValidationIssue issue : _issues)
		{
			if (issue.getSeverity() == ItemValidationIssue.Severity.ERROR)
			{
				return true;
			}
		}
		
		return false;
	}
	
	public int getErrorCount()
	{
		int count = 0;
		for (ItemValidationIssue issue : _issues)
		{
			if (issue.getSeverity() == ItemValidationIssue.Severity.ERROR)
			{
				count++;
			}
		}
		
		return count;
	}
	
	public int getWarningCount()
	{
		return _issues.size() - getErrorCount();
	}
}
