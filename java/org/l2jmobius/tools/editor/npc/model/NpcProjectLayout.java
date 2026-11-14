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
package org.l2jmobius.tools.editor.npc.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Resolves the NPC datapack and schema locations from a selected L2JMobius path.
 * @author BazookaRpm
 */
public final class NpcProjectLayout
{
	private final Path _selectedRoot;
	private final Path _dataRoot;
	private final Path _npcRoot;
	private final Path _skillRoot;
	private final Path _npcSchema;
	
	private NpcProjectLayout(Path selectedRoot, Path dataRoot)
	{
		_selectedRoot = selectedRoot;
		_dataRoot = dataRoot;
		_npcRoot = dataRoot.resolve("stats").resolve("npcs");
		_skillRoot = dataRoot.resolve("stats").resolve("skills");
		_npcSchema = dataRoot.resolve("xsd").resolve("npcs.xsd");
	}
	
	public static NpcProjectLayout detect(Path selectedPath)
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
			if (Files.isDirectory(candidate.resolve("stats").resolve("npcs")))
			{
				return new NpcProjectLayout(resolveChronicleRoot(selected, candidate), candidate);
			}
		}
		
		throw new IllegalArgumentException("Could not locate data/stats/npcs below the selected path.");
	}
	
	private static Path resolveChronicleRoot(Path selected, Path dataRoot)
	{
		final Path gameRoot = dataRoot.getParent();
		if ((gameRoot != null) && "game".equalsIgnoreCase(gameRoot.getFileName().toString()))
		{
			final Path distRoot = gameRoot.getParent();
			if ((distRoot != null) && (distRoot.getFileName() != null) && "dist".equalsIgnoreCase(distRoot.getFileName().toString()) && (distRoot.getParent() != null))
			{
				return distRoot.getParent();
			}
		}
		
		return selected;
	}
	
	public Path getSelectedRoot()
	{
		return _selectedRoot;
	}
	
	public Path getDataRoot()
	{
		return _dataRoot;
	}
	
	public Path getNpcRoot()
	{
		return _npcRoot;
	}
	
	public Path getSkillRoot()
	{
		return _skillRoot;
	}
	
	public Path getNpcSchema()
	{
		return _npcSchema;
	}
}
