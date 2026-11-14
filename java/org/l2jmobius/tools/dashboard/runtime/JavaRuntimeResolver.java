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
package org.l2jmobius.tools.dashboard.runtime;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;

/**
 * Resolves a Java executable from the explicit setting, JAVA_HOME, or the running JVM.
 * @author BazookaRpm
 */
public final class JavaRuntimeResolver
{
	private JavaRuntimeResolver()
	{
	}
	
	public static Path resolve(ControlCenterSettings settings)
	{
		final String override = settings.getJavaExecutableOverride();
		if (!override.isEmpty())
		{
			final Path explicit = Paths.get(override).toAbsolutePath().normalize();
			if (Files.isRegularFile(explicit))
			{
				return explicit;
			}
		}
		
		final String executableName = isWindows() ? "javaw.exe" : "java";
		final String javaHome = System.getenv("JAVA_HOME");
		if ((javaHome != null) && !javaHome.isBlank())
		{
			final Path environmentJava = Paths.get(javaHome, "bin", executableName);
			if (Files.isRegularFile(environmentJava))
			{
				return environmentJava;
			}
		}
		
		final Path currentJava = Paths.get(System.getProperty("java.home"), "bin", executableName);
		if (Files.isRegularFile(currentJava))
		{
			return currentJava;
		}
		
		return Paths.get(isWindows() ? "java.exe" : "java");
	}
	
	private static boolean isWindows()
	{
		return System.getProperty("os.name", "").toLowerCase().contains("win");
	}
}
