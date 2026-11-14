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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarFile;

import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.tools.dashboard.runtime.JavaRuntimeResolver;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Launches the official utility scripts supplied by the active L2J Mobius distribution.<br>
 * Using the source-provided launcher preserves each tool's working directory, JVM flags and GUI behavior.
 * @author BazookaRpm
 */
public final class OfficialToolLauncher
{
	private static final DateTimeFormatter LOG_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private final ControlCenterSettings _settings;
	private final Map<OfficialTool, Process> _processes = new EnumMap<>(OfficialTool.class);
	
	public OfficialToolLauncher(ControlCenterSettings settings)
	{
		_settings = settings;
	}
	
	public ToolAvailability inspect(OfficialTool tool)
	{
		final Path workingDirectory = workingDirectory(tool);
		if (!Files.isDirectory(workingDirectory))
		{
			return new ToolAvailability(false, "Working directory not found: " + workingDirectory);
		}
		
		final Path interfaceConfig = workingDirectory.resolve("config/Interface.ini");
		if (!Files.isRegularFile(interfaceConfig))
		{
			return new ToolAvailability(false, "Interface.ini not found: " + interfaceConfig);
		}
		
		if (!isGuiEnabled(interfaceConfig))
		{
			return new ToolAvailability(false, "EnableGUI must be true in " + interfaceConfig);
		}
		
		final Path script = launcherScript(tool);
		if (!Files.isRegularFile(script))
		{
			return new ToolAvailability(false, "Official launcher script not found: " + script);
		}
		
		final Path jar = _settings.getDistributionRoot().resolve("libs").resolve(tool.jarName());
		if (!Files.isRegularFile(jar))
		{
			return new ToolAvailability(false, tool.jarName() + " not found: " + jar);
		}
		
		try (JarFile archive = new JarFile(jar.toFile()))
		{
			if (archive.getJarEntry(tool.classEntry()) == null)
			{
				return new ToolAvailability(false, tool.mainClass() + " is not present in " + tool.jarName());
			}
		}
		catch (IOException e)
		{
			return new ToolAvailability(false, "Unable to inspect " + jar.getFileName() + ": " + e.getMessage());
		}
		
		return new ToolAvailability(true, "Ready. Official launcher: " + script.getFileName());
	}
	
	public synchronized boolean isRunning(OfficialTool tool)
	{
		final Process process = _processes.get(tool);
		if ((process != null) && process.isAlive())
		{
			return true;
		}
		
		_processes.remove(tool);
		return false;
	}
	
	public synchronized Process launch(OfficialTool tool) throws IOException
	{
		if (isRunning(tool))
		{
			throw new IOException(tool.label() + " is already running.");
		}
		
		final ToolAvailability availability = inspect(tool);
		if (!availability.available())
		{
			throw new IOException(availability.detail());
		}
		
		final Path logFile = logFile(tool);
		Files.createDirectories(logFile.getParent());
		appendLog(logFile, "Launching " + tool.label() + " with " + launcherScript(tool));
		final ProcessBuilder builder = new ProcessBuilder(buildCommand(tool));
		builder.directory(workingDirectory(tool).toFile());
		builder.redirectErrorStream(true);
		builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));
		configureJavaHome(builder);
		final Process process = builder.start();
		_processes.put(tool, process);
		return process;
	}
	
	public synchronized void release(OfficialTool tool, Process process)
	{
		if (_processes.get(tool) == process)
		{
			_processes.remove(tool);
		}
	}
	
	public List<String> buildCommand(OfficialTool tool)
	{
		final List<String> command = new ArrayList<>();
		final Path script = launcherScript(tool);
		if (isWindows())
		{
			command.add(resolveWindowsScriptHost());
			command.add("//nologo");
			command.add(script.toString());
		}
		else
		{
			command.add("/bin/sh");
			command.add(script.toString());
		}
		
		return command;
	}
	
	public Path launcherScript(OfficialTool tool)
	{
		return workingDirectory(tool).resolve(isWindows() ? tool.windowsScript() : tool.unixScript()).toAbsolutePath().normalize();
	}
	
	public Path workingDirectory(OfficialTool tool)
	{
		return _settings.getServiceDirectory(tool.workingRole()).toAbsolutePath().normalize();
	}
	
	public Path logFile(OfficialTool tool)
	{
		return _settings.getDistributionRoot().resolve("log/tools").resolve(tool.name().toLowerCase() + ".log").toAbsolutePath().normalize();
	}
	
	private void configureJavaHome(ProcessBuilder builder)
	{
		final Path binDirectory = JavaRuntimeResolver.resolve(_settings).toAbsolutePath().normalize().getParent();
		if (binDirectory != null)
		{
			final Path javaHome = binDirectory.getParent();
			if ((javaHome != null) && Files.isDirectory(javaHome))
			{
				builder.environment().put("JAVA_HOME", javaHome.toString());
			}
		}
	}
	
	private static void appendLog(Path logFile, String text) throws IOException
	{
		Files.writeString(logFile, "[" + LOG_TIMESTAMP.format(LocalDateTime.now()) + "] " + text + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
	}
	
	private static String resolveWindowsScriptHost()
	{
		final String systemRoot = System.getenv("SystemRoot");
		if ((systemRoot != null) && !systemRoot.isBlank())
		{
			final Path host = Paths.get(systemRoot, "System32", "wscript.exe");
			if (Files.isRegularFile(host))
			{
				return host.toString();
			}
		}
		
		return "wscript.exe";
	}
	
	private static boolean isGuiEnabled(Path interfaceConfig)
	{
		if (!Files.isRegularFile(interfaceConfig))
		{
			return true;
		}
		
		return new ConfigReader(interfaceConfig.toString()).getBoolean("EnableGUI", true);
	}
	
	private static boolean isWindows()
	{
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}
}
