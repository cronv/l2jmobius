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

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the datapack item, skill, schema, and handler roots from a selected source or distribution path.
 * @author BazookaRpm
 */
public final class ItemProjectLayout
{
	private final Path _selectedRoot;
	private final Path _chronicleRoot;
	private final Path _itemRoot;
	private final Path _itemSchema;
	private final Path _skillRoot;
	private final Path _masterHandler;
	
	private ItemProjectLayout(Path selectedRoot, Path chronicleRoot, Path dataRoot)
	{
		_selectedRoot = selectedRoot;
		_chronicleRoot = chronicleRoot;
		_itemRoot = dataRoot.resolve("stats").resolve("items");
		_itemSchema = dataRoot.resolve("xsd").resolve("items.xsd");
		_skillRoot = dataRoot.resolve("stats").resolve("skills");
		_masterHandler = dataRoot.resolve("scripts").resolve("handlers").resolve("MasterHandler.java");
	}
	
	public static ItemProjectLayout detect(Path selectedPath)
	{
		if (selectedPath == null)
		{
			throw new IllegalArgumentException("A project or datapack path is required.");
		}
		
		final Path selected = selectedPath.toAbsolutePath().normalize();
		final List<Path> candidates = new ArrayList<>();
		candidates.add(selected.resolve("dist").resolve("game").resolve("data"));
		candidates.add(selected.resolve("game").resolve("data"));
		candidates.add(selected.resolve("data"));
		candidates.add(selected);
		for (Path candidate : candidates)
		{
			if (Files.isDirectory(candidate.resolve("stats").resolve("items")))
			{
				return new ItemProjectLayout(selected, resolveChronicleRoot(selected, candidate), candidate.toAbsolutePath().normalize());
			}
		}
		
		throw new IllegalArgumentException("Could not locate stats/items below " + selected + ".");
	}
	
	private static Path resolveChronicleRoot(Path selected, Path dataRoot)
	{
		final Path gameRoot = dataRoot.getParent();
		if ((gameRoot != null) && "game".equalsIgnoreCase(gameRoot.getFileName().toString()))
		{
			final Path distRoot = gameRoot.getParent();
			if ((distRoot != null) && "dist".equalsIgnoreCase(distRoot.getFileName().toString()))
			{
				final Path projectRoot = distRoot.getParent();
				if (projectRoot != null)
				{
					return projectRoot;
				}
			}
		}
		
		return selected;
	}
	
	public Path getSelectedRoot()
	{
		return _selectedRoot;
	}
	
	public Path getChronicleRoot()
	{
		return _chronicleRoot;
	}
	
	public Path getItemRoot()
	{
		return _itemRoot;
	}
	
	public Path getItemSchema()
	{
		return _itemSchema;
	}
	
	public Path getSkillRoot()
	{
		return _skillRoot;
	}
	
	public Path getMasterHandler()
	{
		return _masterHandler;
	}
}
