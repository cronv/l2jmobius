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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

import org.l2jmobius.gameserver.scripting.annotations.Disabled;

/**
 * Executes game-server scripts by compiling and running script source files.
 * @author HorridoJoho, Mobius
 */
public class ScriptExecutor
{
	private static final Logger LOGGER = Logger.getLogger(ScriptExecutor.class.getName());
	
	private static final JavaCompiler COMPILER = ToolProvider.getSystemJavaCompiler();
	private static final ScriptClassLoader SCRIPT_CLASS_LOADER = new ScriptClassLoader(ClassLoader.getSystemClassLoader());
	private static final List<String> OPTIONS = new ArrayList<>();
	
	private static Path _currentExecutingScript;
	
	/**
	 * Creates a {@code ScriptExecutor} and configures the default compiler options for source paths and debugging information.
	 */
	public ScriptExecutor()
	{
		addOptionIfNotNull(OPTIONS, "data/scripts", "-sourcepath");
		addOptionIfNotNull(OPTIONS, "source,lines,vars", "-g:");
	}
	
	/**
	 * Adds a compiler option to a {@code List} when the supplied value is not {@code null}, formatting by colon-suffixed prefix.
	 * @param list target {@code List} to receive the option
	 * @param nullChecked value checked for {@code null} and appended
	 * @param before prefix placed before the value
	 * @return {@code true} if the option was added, {@code false} if the value was {@code null}
	 */
	private boolean addOptionIfNotNull(List<String> list, String nullChecked, String before)
	{
		if (nullChecked == null)
		{
			return false;
		}
		
		if (before.endsWith(":"))
		{
			list.add(before + nullChecked);
		}
		else
		{
			list.add(before);
			list.add(nullChecked);
		}
		
		return true;
	}
	
	/**
	 * Compiles and executes the scripts at the given paths, returning the failures encountered.
	 * @param sourcePaths {@code Path} objects to compile and execute
	 * @return a {@code Map} of script {@code Path} to the {@code Throwable} thrown during execution
	 * @throws Exception if compilation fails or file management errors occur
	 */
	public Map<Path, Throwable> executeScripts(Iterable<Path> sourcePaths) throws Exception
	{
		final DiagnosticCollector<JavaFileObject> fileManagerDiagnostics = new DiagnosticCollector<>();
		final DiagnosticCollector<JavaFileObject> compilationDiagnostics = new DiagnosticCollector<>();
		
		try (ScriptFileManager fileManager = new ScriptFileManager(COMPILER.getStandardFileManager(fileManagerDiagnostics, null, StandardCharsets.UTF_8)))
		{
			// We really need an iterable of files or strings.
			final List<String> sourcePathStrings = new ArrayList<>();
			for (Path sourcePath : sourcePaths)
			{
				sourcePathStrings.add(sourcePath.toAbsolutePath().toString());
			}
			
			final StringWriter strOut = new StringWriter();
			final PrintWriter out = new PrintWriter(strOut);
			final boolean compilationSuccess = COMPILER.getTask(out, fileManager, compilationDiagnostics, OPTIONS, null, fileManager.getJavaFileObjectsFromStrings(sourcePathStrings)).call();
			if (!compilationSuccess)
			{
				logDiagnostics(out, fileManagerDiagnostics, compilationDiagnostics);
				throw new RuntimeException(strOut.toString());
			}
			
			final Map<Path, Throwable> executionFailures = new HashMap<>();
			final Iterable<ScriptClassData> compiledClasses = fileManager.getCompiledClasses();
			
			// Add all newly compiled classes to the script class loader.
			SCRIPT_CLASS_LOADER.addCompiledClasses(compiledClasses);
			
			for (Path sourcePath : sourcePaths)
			{
				boolean found = false;
				for (ScriptClassData compiledClass : compiledClasses)
				{
					final Path compiledSourcePath = compiledClass.getSourcePath();
					
					// sourcePath can be relative, so we have to use endsWith.
					if ((compiledSourcePath != null) && (compiledSourcePath.equals(sourcePath) || compiledSourcePath.endsWith(sourcePath)))
					{
						final String javaName = compiledClass.getJavaName();
						if (javaName.indexOf('$') != -1)
						{
							continue;
						}
						
						found = true;
						_currentExecutingScript = compiledSourcePath;
						try
						{
							final Class<?> javaClass = SCRIPT_CLASS_LOADER.loadClass(javaName);
							executeMainMethod(javaClass, compiledSourcePath);
						}
						catch (Exception e)
						{
							executionFailures.put(compiledSourcePath, e);
						}
						finally
						{
							_currentExecutingScript = null;
						}
						break;
					}
				}
				
				if (!found)
				{
					LOGGER.severe("Compilation successful, but class corresponding to " + sourcePath.toString() + " not found!");
				}
			}
			
			return executionFailures;
		}
	}
	
	/**
	 * Writes the file-management and compilation diagnostics to the given output.
	 * @param out {@code PrintWriter} receiving the log messages
	 * @param fileManagerDiagnostics collector of file-management diagnostics
	 * @param compilationDiagnostics collector of compilation diagnostics
	 */
	private void logDiagnostics(PrintWriter out, DiagnosticCollector<JavaFileObject> fileManagerDiagnostics, DiagnosticCollector<JavaFileObject> compilationDiagnostics)
	{
		out.println();
		out.println("----------------");
		out.println("File diagnostics");
		out.println("----------------");
		for (Diagnostic<? extends JavaFileObject> diagnostic : fileManagerDiagnostics.getDiagnostics())
		{
			logDiagnostic(out, diagnostic);
		}
		
		out.println();
		out.println("-----------------------");
		out.println("Compilation diagnostics");
		out.println("-----------------------");
		for (Diagnostic<? extends JavaFileObject> diagnostic : compilationDiagnostics.getDiagnostics())
		{
			logDiagnostic(out, diagnostic);
		}
	}
	
	/**
	 * Writes a single {@code Diagnostic} with its source name, line, column, code and message to the given output.
	 * @param out {@code PrintWriter} receiving the log entry
	 * @param diagnostic {@code Diagnostic} to write
	 */
	private void logDiagnostic(PrintWriter out, Diagnostic<? extends JavaFileObject> diagnostic)
	{
		String sourceName = (diagnostic.getSource() != null) ? diagnostic.getSource().getName() : "Unknown Source";
		out.println("\t" + diagnostic.getKind() + ": " + sourceName + ", Line " + diagnostic.getLineNumber() + ", Column " + diagnostic.getColumnNumber());
		out.println("\t\tcode: " + diagnostic.getCode());
		out.println("\t\tmessage: " + diagnostic.getMessage(null));
	}
	
	/**
	 * Invokes the static {@code main(String[])} of the given class unless it is annotated {@code Disabled}.
	 * @param javaClass {@code Class} whose main method is invoked
	 * @param compiledSourcePath {@code Path} passed as the sole argument to main
	 * @throws Exception if reflection or the invoked method fails
	 */
	private void executeMainMethod(Class<?> javaClass, Path compiledSourcePath) throws Exception
	{
		if (javaClass.isAnnotationPresent(Disabled.class))
		{
			return;
		}
		
		for (Method method : javaClass.getMethods())
		{
			if (method.getName().equals("main") && Modifier.isStatic(method.getModifiers()) && (method.getParameterCount() == 1) && (method.getParameterTypes()[0] == String[].class))
			{
				method.invoke(null, (Object) new String[]
				{
					compiledSourcePath.toString()
				});
				break;
			}
		}
	}
	
	/**
	 * Compiles and executes a single script, returning the first failure or {@code null} on success.
	 * @param sourcePath {@code Path} of the script to execute
	 * @return an {@code Entry} of the failed {@code Path} and its {@code Throwable}, or {@code null} if successful
	 * @throws Exception if compilation fails or file management errors occur
	 */
	public Entry<Path, Throwable> executeScript(Path sourcePath) throws Exception
	{
		final Map<Path, Throwable> executionFailures = executeScripts(Arrays.asList(sourcePath));
		if (!executionFailures.isEmpty())
		{
			return executionFailures.entrySet().iterator().next();
		}
		
		return null;
	}
	
	/**
	 * Returns the {@code Path} of the script currently executing.
	 * @return the current script {@code Path}, or {@code null} if none is running
	 */
	public Path getCurrentExecutingScript()
	{
		return _currentExecutingScript;
	}
}
