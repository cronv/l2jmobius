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
package org.l2jmobius.tools.editor.drop.model;

/**
 * Source-specific Drop Editor capabilities for the current chronicle overlay.
 * @author BazookaRpm
 */
public final class DropEditorProfile
{
	private static final DropEditorProfile CURRENT = new DropEditorProfile("14.3 Throne", new DropType[]
	{
		DropType.DROP,
		DropType.SPOIL,
		DropType.LIMITED
	}, new DropType[]
	{
		DropType.DROP
	}, new DropType[]
	{
		DropType.SPOIL,
		DropType.LIMITED
	}, new DropType[]
	{
		DropType.LIMITED
	}, new DropType[]
	{
		DropType.LIMITED
	}, new DropType[]
	{
		DropType.LIMITED
	});
	
	private final String _displayName;
	private final DropType[] _supportedTypes;
	private final DropType[] _groupedTypes;
	private final DropType[] _ungroupedTypes;
	private final DropType[] _extendedTypes;
	private final DropType[] _requiredExtendedTypes;
	private final DropType[] _intAmountTypes;
	
	private DropEditorProfile(String displayName, DropType[] supportedTypes, DropType[] groupedTypes, DropType[] ungroupedTypes, DropType[] extendedTypes, DropType[] requiredExtendedTypes, DropType[] intAmountTypes)
	{
		// The only caller passes private literals, so there is nothing to copy them away from.
		_displayName = displayName;
		_supportedTypes = supportedTypes;
		_groupedTypes = groupedTypes;
		_ungroupedTypes = ungroupedTypes;
		_extendedTypes = extendedTypes;
		_requiredExtendedTypes = requiredExtendedTypes;
		_intAmountTypes = intAmountTypes;
	}
	
	public static DropEditorProfile current()
	{
		return CURRENT;
	}
	
	public String getDisplayName()
	{
		return _displayName;
	}
	
	public DropType[] getSupportedTypes()
	{
		return _supportedTypes.clone();
	}
	
	public boolean supports(DropType type)
	{
		return contains(_supportedTypes, type);
	}
	
	public boolean supportsGroups(DropType type)
	{
		return contains(_groupedTypes, type);
	}
	
	public boolean supportsUngrouped(DropType type)
	{
		return contains(_ungroupedTypes, type);
	}
	
	public boolean supportsExtendedAttributes(DropType type)
	{
		return contains(_extendedTypes, type);
	}
	
	public boolean requiresExtendedAttributes(DropType type)
	{
		return contains(_requiredExtendedTypes, type);
	}
	
	public boolean usesIntAmounts(DropType type)
	{
		return contains(_intAmountTypes, type);
	}
	
	public boolean hasExtendedAttributes()
	{
		return _extendedTypes.length > 0;
	}
	
	public String getSupportedTypesLabel()
	{
		final StringBuilder builder = new StringBuilder();
		for (DropType type : _supportedTypes)
		{
			if (builder.length() > 0)
			{
				builder.append(", ");
			}
			builder.append(type.name());
		}
		
		return builder.toString();
	}
	
	private boolean contains(DropType[] types, DropType type)
	{
		for (DropType value : types)
		{
			if (value == type)
			{
				return true;
			}
		}
		
		return false;
	}
}
