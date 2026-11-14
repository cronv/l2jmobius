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
package org.l2jmobius.gameserver.scripting;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.TraceUtil;
import org.l2jmobius.gameserver.config.DevelopmentConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.scripting.engine.ScriptExecutor;

/**
 * This class manages the loading and execution of game scripts.<br>
 * It parses script files from the filesystem and provides them to the {@code ScriptExecutor}.
 * @author Mobius
 */
public class ScriptEngine implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ScriptEngine.class.getName());
	
	public static final Path SCRIPT_FOLDER = ServerConfig.SCRIPT_ROOT.toPath();
	public static final Path MASTER_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "MasterHandler.java");
	public static final Path EFFECT_MASTER_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "EffectMasterHandler.java");
	public static final Path SKILL_CONDITION_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "SkillConditionMasterHandler.java");
	public static final Path CONDITION_HANDLER_FILE = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "ConditionMasterHandler.java");
	public static final Path ONE_DAY_REWARD_MASTER_HANDLER = Paths.get(SCRIPT_FOLDER.toString(), "handlers", "DailyMissionMasterHandler.java");
	
	private static final ScriptExecutor SCRIPT_EXECUTOR = new ScriptExecutor();
	private static final Set<String> EXCLUSIONS = new HashSet<>();
	
	/**
	 * Initializes a new instance of the {@code ScriptEngine}.<br>
	 * This constructor automatically calls the {@code load()} method.<br>
	 * It prepares the engine to read scripts from the server configuration.
	 */
	protected ScriptEngine()
	{
		// Load Scripts.xml
		load();
	}
	
	/**
	 * Loads the script configuration from the server files.<br>
	 * This method clears existing exclusions and parses {@code config/Scripts.xml}.<br>
	 * It logs the total number of excluded files to the logger.
	 */
	@Override
	public void load()
	{
		EXCLUSIONS.clear();
		parseDatapackFile("config/Scripts.xml");
		LOGGER.info("Loaded " + EXCLUSIONS.size() + " files to exclude.");
	}
	
	/**
	 * Parses an XML document to identify files that should be excluded from the script engine.<br>
	 * This method reads exclusion rules and applies them to the files in the {@code SCRIPT_FOLDER}.<br>
	 * It handles nested include paths to determine which scripts are skipped during loading.
	 * @param document The {@code Document} object containing the XML configuration.
	 * @param file The {@code File} associated with the current parsing operation.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		try
		{
			final Map<String, Set<String>> excludePaths = new HashMap<>();
			forEach(document, "list", listNode -> forEach(listNode, "exclude", excludeNode ->
			{
				final String excludeFile = parseString(excludeNode.getAttributes(), "file");
				excludePaths.putIfAbsent(excludeFile, new HashSet<>());
				
				forEach(excludeNode, "include", includeNode -> excludePaths.get(excludeFile).add(parseString(includeNode.getAttributes(), "file")));
			}));
			
			final int nameCount = SCRIPT_FOLDER.getNameCount();
			Files.walkFileTree(SCRIPT_FOLDER, new SimpleFileVisitor<Path>()
			{
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
				{
					final String fileName = file.getFileName().toString();
					if (!fileName.endsWith(".java"))
					{
						return FileVisitResult.CONTINUE;
					}
					
					final List<String> relativePathParts = new ArrayList<>();
					file.subpath(nameCount, file.getNameCount()).forEach(p -> relativePathParts.add(p.toString()));
					for (int i = 0; i < relativePathParts.size(); i++)
					{
						final String currentPart = relativePathParts.get(i);
						final Set<String> includePath = excludePaths.get(currentPart);
						if (includePath != null)
						{
							boolean excludeScript = true;
							
							for (int j = i + 1; j < relativePathParts.size(); j++)
							{
								if (includePath.contains(relativePathParts.get(j)))
								{
									excludeScript = false;
									break;
								}
							}
							
							if (excludeScript)
							{
								EXCLUSIONS.add(file.toUri().getPath());
								break;
							}
						}
					}
					
					return FileVisitResult.CONTINUE;
				}
			});
		}
		catch (IOException e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not initialize. " + e.getMessage());
		}
	}
	
	/**
	 * Recursively scans a directory to find script files.<br>
	 * It adds all {@code .java} files to the provided list.<br>
	 * Files listed in {@code EXCLUSIONS} are ignored.
	 * @param dir The root directory to start scanning from.
	 * @param files The list where discovered file paths will be added.
	 */
	private void processDirectory(File dir, List<Path> files)
	{
		for (File file : dir.listFiles())
		{
			if (file.isFile())
			{
				final String filePath = file.toURI().getPath();
				if (filePath.endsWith(".java") && !EXCLUSIONS.contains(filePath))
				{
					files.add(file.toPath().toAbsolutePath());
				}
			}
			else if (file.isDirectory())
			{
				processDirectory(file, files);
			}
		}
	}
	
	/**
	 * Executes a script from the specified file path.<br>
	 * This method resolves the path and checks if it exists before running.<br>
	 * If an error occurs during execution, it logs the stack trace and throws an exception.
	 * @param sourceFiles The {@code Path} to the script file to be executed.
	 * @throws Exception If the file is missing or if the script fails to run.
	 */
	public void executeScript(Path sourceFiles) throws Exception
	{
		Path path = sourceFiles;
		if (!path.isAbsolute())
		{
			path = SCRIPT_FOLDER.resolve(path);
		}
		
		path = path.toAbsolutePath();
		// System.out.println("Executing script at path: " + path.toString());
		
		// Check if the path exists.
		if (!Files.exists(path))
		{
			throw new Exception("Script file does not exist: " + path.toString());
		}
		
		// Execute the script and check for errors.
		final Entry<Path, Throwable> error = SCRIPT_EXECUTOR.executeScript(path);
		if (error != null)
		{
			final Throwable cause = error.getValue();
			if (cause != null)
			{
				LOGGER.warning(TraceUtil.getStackTrace(cause));
			}
			
			throw new Exception("ScriptEngine: " + error.getKey() + " failed execution!", cause);
		}
	}
	
	/**
	 * Executes all scripts located in the script folder.<br>
	 * This method checks if quests are enabled before starting.<br>
	 * It logs a warning for any script that fails to execute.
	 * @throws Exception If an error occurs during the script execution process.
	 */
	public void executeScriptList() throws Exception
	{
		if (DevelopmentConfig.NO_QUESTS)
		{
			return;
		}
		
		final List<Path> files = new ArrayList<>();
		processDirectory(SCRIPT_FOLDER.toFile(), files);
		
		final Map<Path, Throwable> invokationErrors = SCRIPT_EXECUTOR.executeScripts(files);
		for (Entry<Path, Throwable> entry : invokationErrors.entrySet())
		{
			// Reflection wraps script errors in InvocationTargetException, which has no message, so log the cause.
			final Throwable error = entry.getValue();
			final Throwable cause = (error.getCause() != null) ? error.getCause() : error;
			LOGGER.warning(getClass().getSimpleName() + ": " + entry.getKey() + " failed execution! " + TraceUtil.getStackTrace(cause));
		}
	}
	
	/**
	 * Retrieves the path of the script currently being processed.<br>
	 * This method queries the {@code ScriptExecutor} for the active file.
	 * @return The {@code Path} of the current loading script.
	 */
	public Path getCurrentLoadingScript()
	{
		return SCRIPT_EXECUTOR.getCurrentExecutingScript();
	}
	
	/**
	 * Retrieves the singleton instance of the {@code ScriptEngine}.<br>
	 * Use this method to access the global script engine.
	 * @return The active {@code ScriptEngine} instance.
	 */
	public static ScriptEngine getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ScriptEngine INSTANCE = new ScriptEngine();
	}
}
