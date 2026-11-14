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
package org.l2jmobius.tools.editor.drop.service;

import java.nio.file.Path;

import org.l2jmobius.tools.editor.drop.model.DataIndex;
import org.l2jmobius.tools.editor.drop.model.NpcRecord;
import org.l2jmobius.tools.editor.drop.model.ProjectLayout;
import org.l2jmobius.tools.editor.model.NpcMetadataRecord;
import org.l2jmobius.tools.editor.service.NpcMetadataCache;

/**
 * Builds the Drop Editor indexes with a shared streaming NPC scan and lazy item metadata lookup.
 * @author BazookaRpm
 */
public final class ProjectScanner
{
	public DataIndex scan(Path selectedPath) throws Exception
	{
		final ProjectLayout layout = ProjectLayout.detect(selectedPath);
		final DataIndex index = new DataIndex(layout, new LazyItemLookup(layout.getItemRoot()));
		for (NpcMetadataRecord metadata : NpcMetadataCache.load(layout.getNpcRoot()))
		{
			final String type = metadata.getType().isBlank() ? "Folk" : metadata.getType();
			index.addNpc(new NpcRecord(metadata.getId(), metadata.getLevel(85), type, metadata.getName(), metadata.getTitle(), metadata.getFile()));
		}
		
		return index;
	}
}
