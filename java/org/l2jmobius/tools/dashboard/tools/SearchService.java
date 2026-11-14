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
package org.l2jmobius.tools.dashboard.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Searches supported L2J Mobius distribution text files without starting an external Search process.
 * @author BazookaRpm
 */
public final class SearchService
{
	public record SearchMatch(Path file, int matches)
	{
	}
	
	public record SearchSummary(int filesSearched, int totalMatches, int filesWithMatches)
	{
	}
	
	public SearchSummary search(Path root, String expression, Set<String> extensions, boolean caseInsensitive, AtomicBoolean cancelled, Consumer<SearchMatch> resultConsumer, Consumer<Integer> progressConsumer) throws IOException
	{
		if ((root == null) || !Files.isDirectory(root))
		{
			throw new IOException("Search root does not exist: " + root);
		}
		
		if ((expression == null) || expression.isEmpty())
		{
			throw new IllegalArgumentException("Search expression is required.");
		}
		
		final Pattern pattern = Pattern.compile(expression, caseInsensitive ? Pattern.CASE_INSENSITIVE : 0);
		final List<Path> files = collectFiles(root, normalizeExtensions(extensions), cancelled);
		int searched = 0;
		int matches = 0;
		int matchedFiles = 0;
		for (Path file : files)
		{
			if (cancelled.get())
			{
				break;
			}
			
			searched++;
			final int count = countMatches(file, pattern, cancelled);
			if (count > 0)
			{
				matches += count;
				matchedFiles++;
				resultConsumer.accept(new SearchMatch(file, count));
			}
			
			progressConsumer.accept(files.isEmpty() ? 100 : (searched * 100) / files.size());
		}
		
		return new SearchSummary(searched, matches, matchedFiles);
	}
	
	private static List<Path> collectFiles(Path root, Set<String> extensions, AtomicBoolean cancelled) throws IOException
	{
		final List<Path> files = new ArrayList<>();
		Files.walkFileTree(root, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes)
			{
				if (cancelled.get())
				{
					return FileVisitResult.TERMINATE;
				}
				
				final Path name = directory.getFileName();
				if ((name != null) && (name.toString().equals("bin") || name.toString().equals(".svn") || name.toString().equals(".git")))
				{
					return FileVisitResult.SKIP_SUBTREE;
				}
				
				return FileVisitResult.CONTINUE;
			}
			
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes)
			{
				if (cancelled.get())
				{
					return FileVisitResult.TERMINATE;
				}
				
				if (attributes.isRegularFile() && supported(file, extensions))
				{
					files.add(file);
				}
				
				return FileVisitResult.CONTINUE;
			}
		});
		
		return files;
	}
	
	private static boolean supported(Path file, Set<String> extensions)
	{
		if (extensions.isEmpty())
		{
			return true;
		}
		
		final String name = file.getFileName().toString().toLowerCase();
		for (String extension : extensions)
		{
			if (name.endsWith(extension))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static int countMatches(Path file, Pattern pattern, AtomicBoolean cancelled)
	{
		try
		{
			if (cancelled.get())
			{
				return 0;
			}
			
			final String content = Files.readString(file, StandardCharsets.UTF_8);
			if (cancelled.get())
			{
				return 0;
			}
			
			final Matcher matcher = pattern.matcher(content);
			int count = 0;
			while (matcher.find())
			{
				if (cancelled.get())
				{
					return count;
				}
				
				count++;
			}
			
			return count;
		}
		catch (IOException e)
		{
			return 0;
		}
	}
	
	private static Set<String> normalizeExtensions(Set<String> extensions)
	{
		final Set<String> result = new HashSet<>();
		if (extensions == null)
		{
			return result;
		}
		
		for (String extension : extensions)
		{
			if ((extension == null) || extension.isBlank())
			{
				continue;
			}
			
			final String lower = extension.toLowerCase();
			result.add(lower.startsWith(".") ? lower : "." + lower);
		}
		
		return result;
	}
}
