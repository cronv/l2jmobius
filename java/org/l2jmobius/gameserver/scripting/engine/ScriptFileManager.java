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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.StandardJavaFileManager;

/**
 * This class manages the loading and handling of script files for the scripting engine.<br>
 * It extends {@code StandardJavaFileManager} to provide custom file access logic during compilation.
 * @author HorridoJoho, Mobius
 */
public class ScriptFileManager implements StandardJavaFileManager
{
	private final StandardJavaFileManager _wrapped;
	private final List<ScriptClassData> _classOutputs = new ArrayList<>();
	
	/**
	 * Creates a new instance of {@code ScriptFileManager}.<br>
	 * This constructor initializes the manager with an existing file manager.<br>
	 * It wraps the provided {@code StandardJavaFileManager} to extend its functionality.
	 * @param wrapped The {@code StandardJavaFileManager} to be used by this manager.
	 */
	public ScriptFileManager(StandardJavaFileManager wrapped)
	{
		_wrapped = wrapped;
	}
	
	/**
	 * Retrieves all classes that have been compiled.<br>
	 * This method returns an unmodifiable collection of {@code ScriptClassData}.
	 * @return An {@code Iterable} containing the compiled {@code ScriptClassData} objects.
	 */
	Iterable<ScriptClassData> getCompiledClasses()
	{
		return Collections.unmodifiableCollection(_classOutputs);
	}
	
	/**
	 * Checks if a specific compiler option is supported.<br>
	 * This method delegates the check to the wrapped {@code StandardJavaFileManager}.
	 * @param option The name of the option to check.
	 * @return An integer representing the status of the option.
	 */
	@Override
	public int isSupportedOption(String option)
	{
		return _wrapped.isSupportedOption(option);
	}
	
	/**
	 * Retrieves the {@code ClassLoader} for a specific location.<br>
	 * This method delegates the request to the wrapped manager.
	 * @param location The {@code Location} to retrieve the loader from.
	 * @return The {@code ClassLoader} associated with the provided {@code Location}.
	 */
	@Override
	public ClassLoader getClassLoader(Location location)
	{
		return _wrapped.getClassLoader(location);
	}
	
	/**
	 * Lists the files found at a specific location.<br>
	 * It filters results based on the package name and file types.<br>
	 * The method can search through subdirectories if requested.
	 * @param location The starting directory for the search.
	 * @param packageName The package name to filter by.
	 * @param kinds The set of {@code Kind} values to include.
	 * @param recurse Set to {@code true} to search subdirectories, or {@code false} to stay in the root.
	 * @return An {@code Iterable} containing the matching {@code JavaFileObject} instances.
	 * @throws IOException If an error occurs while accessing the file system.
	 */
	@Override
	public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException
	{
		return _wrapped.list(location, packageName, kinds, recurse);
	}
	
	/**
	 * Determines the binary name for a given file at a specific location.<br>
	 * This method delegates the logic to the wrapped {@code StandardJavaFileManager}.
	 * @param location The {@code Location} where the file is stored.
	 * @param file The {@code JavaFileObject} representing the source file.
	 * @return The inferred binary name as a {@code String}.
	 */
	@Override
	public String inferBinaryName(Location location, JavaFileObject file)
	{
		return _wrapped.inferBinaryName(location, file);
	}
	
	/**
	 * Checks if two {@code FileObject} instances represent the same file.<br>
	 * This method delegates the check to the wrapped manager.
	 * @param a The first file object to compare.
	 * @param b The second file object to compare.
	 * @return {@code true} if both objects refer to the same file, otherwise {@code false}.
	 */
	@Override
	public boolean isSameFile(FileObject a, FileObject b)
	{
		return _wrapped.isSameFile(a, b);
	}
	
	/**
	 * Processes a compiler option and its subsequent arguments.<br>
	 * This method delegates the handling to the wrapped {@code StandardJavaFileManager}.
	 * @param current The current option string being processed.
	 * @param remaining An iterator containing any additional arguments for the option.
	 * @return {@code true} if the option was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean handleOption(String current, Iterator<String> remaining)
	{
		return _wrapped.handleOption(current, remaining);
	}
	
	/**
	 * Checks if the manager recognizes a specific location.<br>
	 * This method delegates the check to the wrapped {@code StandardJavaFileManager}.
	 * @param location The {@code Location} to verify.
	 * @return {@code true} if the location exists, {@code false} otherwise.
	 */
	@Override
	public boolean hasLocation(Location location)
	{
		return _wrapped.hasLocation(location);
	}
	
	/**
	 * Retrieves a {@code JavaFileObject} for a specific input file.<br>
	 * This method delegates the request to the wrapped manager.
	 * @param location The {@code Location} where the file is stored.
	 * @param className The name of the {@code ClassName} to find.
	 * @param kind The {@code Kind} of the file object requested.
	 * @return The corresponding {@code JavaFileObject}.
	 * @throws IOException If an error occurs while accessing the file.
	 */
	@Override
	public JavaFileObject getJavaFileForInput(Location location, String className, Kind kind) throws IOException
	{
		return _wrapped.getJavaFileForInput(location, className, kind);
	}
	
	/**
	 * Retrieves the {@code JavaFileObject} for a specific output location.<br>
	 * This method handles the creation of script class data objects.<br>
	 * It uses the provided {@code className} and {@code kind} to determine the file details.
	 * @param location The base location where the file will be stored.
	 * @param className The name of the class to generate.
	 * @param kind The type of Java file object to create.
	 * @param sibling An optional sibling {@code FileObject} used for path resolution.
	 * @return The generated {@code JavaFileObject}.
	 * @throws IOException If an error occurs during file access.
	 */
	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException
	{
		if (kind != Kind.CLASS)
		{
			return _wrapped.getJavaFileForOutput(location, className, kind, sibling);
		}
		
		String javaName = className;
		if (javaName.contains("/"))
		{
			javaName = javaName.replace('/', '.');
		}
		
		ScriptClassData fileObject;
		if (sibling != null)
		{
			fileObject = new ScriptClassData(Paths.get(sibling.getName()), javaName, javaName.substring(javaName.lastIndexOf('.') + 1));
		}
		else
		{
			fileObject = new ScriptClassData(null, javaName, javaName.substring(javaName.lastIndexOf('.') + 1));
		}
		
		_classOutputs.add(fileObject);
		return fileObject;
	}
	
	/**
	 * Retrieves a {@code FileObject} based on the provided location and name.<br>
	 * This method delegates the request to the wrapped {@code StandardJavaFileManager}.
	 * @param location The base {@code Location} where the file is stored.
	 * @param packageName The package name associated with the file.
	 * @param relativeName The name of the file relative to the package path.
	 * @return The {@code FileObject} corresponding to the input parameters.
	 * @throws IOException If an error occurs while accessing the file.
	 */
	@Override
	public FileObject getFileForInput(Location location, String packageName, String relativeName) throws IOException
	{
		return _wrapped.getFileForInput(location, packageName, relativeName);
	}
	
	/**
	 * Retrieves a {@code FileObject} for an output file.<br>
	 * This method uses the wrapped manager to find the correct path.
	 * @param location The base {@code Location} for the files.
	 * @param packageName The name of the package containing the file.
	 * @param relativeName The name of the file relative to the package.
	 * @param sibling A {@code FileObject} used as a reference for siblings.
	 * @return The resulting {@code FileObject} for the output.
	 * @throws IOException If an error occurs while accessing the file system.
	 */
	@Override
	public FileObject getFileForOutput(Location location, String packageName, String relativeName, FileObject sibling) throws IOException
	{
		return _wrapped.getFileForOutput(location, packageName, relativeName, sibling);
	}
	
	/**
	 * Flushes the underlying file manager.<br>
	 * This method ensures all buffered data is written to the destination.<br>
	 * It delegates the operation to the wrapped {@code StandardJavaFileManager}.
	 * @throws IOException If an error occurs during the flush process.
	 */
	@Override
	public void flush() throws IOException
	{
		_wrapped.flush();
	}
	
	/**
	 * Closes the underlying file manager.<br>
	 * This method releases any resources held by the {@code ScriptFileManager}.<br>
	 * It calls the {@code close()} method on the wrapped manager.
	 * @throws IOException If an error occurs while closing the resources.
	 */
	@Override
	public void close() throws IOException
	{
		_wrapped.close();
	}
	
	/**
	 * Converts a collection of {@code File} objects into {@code JavaFileObject} instances.<br>
	 * This method delegates the conversion to the wrapped {@code StandardJavaFileManager}.
	 * @param files The collection of {@code File} objects to convert.
	 * @return An {@code Iterable} containing the converted {@code JavaFileObject} items.
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromFiles(Iterable<? extends File> files)
	{
		return _wrapped.getJavaFileObjectsFromFiles(files);
	}
	
	/**
	 * Retrieves {@code JavaFileObject} instances for the provided files.<br>
	 * This method delegates the request to the wrapped {@code StandardJavaFileManager}.
	 * @param files The array of {@code File} objects to process.
	 * @return An {@code Iterable} containing the corresponding {@code JavaFileObject}s.
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(File... files)
	{
		return _wrapped.getJavaFileObjects(files);
	}
	
	/**
	 * Converts a collection of file names into {@code JavaFileObject} instances.<br>
	 * This method delegates the conversion to the wrapped {@code StandardJavaFileManager}.
	 * @param names A collection of strings representing the paths to the files.
	 * @return An {@code Iterable} containing the corresponding {@code JavaFileObject} objects.
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromStrings(Iterable<String> names)
	{
		return _wrapped.getJavaFileObjectsFromStrings(names);
	}
	
	/**
	 * Retrieves the {@code JavaFileObject} instances for the given names.<br>
	 * This method delegates the request to the wrapped {@code StandardJavaFileManager}.
	 * @param names The names of the files to retrieve.
	 * @return An {@code Iterable} containing the requested {@code JavaFileObject} instances.
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(String... names)
	{
		return _wrapped.getJavaFileObjects(names);
	}
	
	/**
	 * Sets the current location for the file manager.<br>
	 * This method updates the internal state using the provided {@code Location} and {@code Iterable}.
	 * @param location The new {@code Location} to set.
	 * @param path The {@code Iterable} of files associated with the path.
	 * @throws IOException If an error occurs while setting the location.
	 */
	@Override
	public void setLocation(Location location, Iterable<? extends File> path) throws IOException
	{
		_wrapped.setLocation(location, path);
	}
	
	/**
	 * Retrieves the files associated with a specific {@code Location}.<br>
	 * This method delegates the request to the wrapped {@code StandardJavaFileManager}.
	 * @param location The {@code Location} to search for files.
	 * @return An {@code Iterable} containing the {@code File} objects found at the location.
	 */
	@Override
	public Iterable<? extends File> getLocation(Location location)
	{
		return _wrapped.getLocation(location);
	}
	
	/**
	 * Retrieves the specific {@code Location} for a given module.<br>
	 * This method delegates the request to the wrapped manager.
	 * @param location The base {@code Location} to search from.
	 * @param moduleName The name of the module to find.
	 * @return The {@code Location} associated with the specified module.
	 * @throws IOException If an error occurs while accessing the file system.
	 */
	@Override
	public Location getLocationForModule(Location location, String moduleName) throws IOException
	{
		return _wrapped.getLocationForModule(location, moduleName);
	}
	
	/**
	 * This method retrieves the {@code Location} for a specific module.<br>
	 * It uses the provided {@code location} and {@code JavaFileObject} to find the correct path.<br>
	 * The result is obtained from the wrapped manager.
	 * @param location The base {@code Location} used as a starting point.
	 * @param fo The {@code JavaFileObject} representing the module file.
	 * @return The {@code Location} associated with the module.
	 * @throws IOException If an error occurs while accessing the file system.
	 */
	@Override
	public Location getLocationForModule(Location location, JavaFileObject fo) throws IOException
	{
		return _wrapped.getLocationForModule(location, fo);
	}
	
	/**
	 * Retrieves a {@code ServiceLoader} for the specified service type from a given location.<br>
	 * This method delegates the request to the wrapped {@code StandardJavaFileManager}.
	 * @param location The {@code Location} where the services are located.
	 * @param service The {@code Class} of the service to load.
	 * @return A {@code ServiceLoader} for the requested service type.
	 * @throws IOException If an error occurs while accessing the location.
	 */
	@Override
	public <S> ServiceLoader<S> getServiceLoader(Location location, Class<S> service) throws IOException
	{
		return _wrapped.getServiceLoader(location, service);
	}
	
	/**
	 * Determines the module name based on a given {@code Location}.<br>
	 * This method delegates the logic to the wrapped manager.
	 * @param location The {@code Location} used to identify the module.
	 * @return The inferred module name as a {@code String}.
	 * @throws IOException If an error occurs while accessing the file system.
	 */
	@Override
	public String inferModuleName(Location location) throws IOException
	{
		return _wrapped.inferModuleName(location);
	}
	
	/**
	 * Retrieves a list of locations associated with modules for a specific location.<br>
	 * This method delegates the request to the wrapped {@code StandardJavaFileManager}.
	 * @param location The base {@code Location} to search from.
	 * @return An {@code Iterable} containing sets of {@code Location} objects.
	 * @throws IOException If an error occurs while accessing the file system.
	 */
	@Override
	public Iterable<Set<Location>> listLocationsForModules(Location location) throws IOException
	{
		return _wrapped.listLocationsForModules(location);
	}
	
	/**
	 * Checks if a specific {@code FileObject} exists at the given {@code Location}.<br>
	 * This method delegates the check to the wrapped manager.
	 * @param location The {@code Location} where the file is expected to be found.
	 * @param fo The {@code FileObject} to search for.
	 * @return {@code true} if the file exists at the location, otherwise {@code false}.
	 * @throws IOException If an error occurs during the check.
	 */
	@Override
	public boolean contains(Location location, FileObject fo) throws IOException
	{
		return _wrapped.contains(location, fo);
	}
	
	/**
	 * Updates the {@code Location} based on a collection of {@code Path} objects.<br>
	 * This method delegates the operation to the wrapped manager.
	 * @param location The {@code Location} object to be updated.
	 * @param paths A collection of {@code Path} objects used to determine the new location.
	 * @throws IOException If an error occurs while processing the paths.
	 */
	@Override
	public void setLocationFromPaths(Location location, Collection<? extends Path> paths) throws IOException
	{
		_wrapped.setLocationFromPaths(location, paths);
	}
	
	/**
	 * Sets the file location for a specific module.<br>
	 * This method registers where a module's files are stored.<br>
	 * It updates the internal configuration of the wrapped manager.
	 * @param location The {@code Location} object representing the base directory.
	 * @param moduleName The name of the module to associate with this path.
	 * @param paths A collection of {@code Path} objects defining the specific file locations.
	 * @throws IOException If an error occurs while accessing the file system.
	 */
	@Override
	public void setLocationForModule(Location location, String moduleName, Collection<? extends Path> paths) throws IOException
	{
		_wrapped.setLocationForModule(location, moduleName, paths);
	}
	
	/**
	 * Converts a {@code Location} into a collection of {@code Path} objects.<br>
	 * This method delegates the conversion to the wrapped file manager.
	 * @param location The source {@code Location} to convert.
	 * @return An {@code Iterable} containing the resulting {@code Path} objects.
	 */
	@Override
	public Iterable<? extends Path> getLocationAsPaths(Location location)
	{
		return _wrapped.getLocationAsPaths(location);
	}
	
	/**
	 * Converts a {@code FileObject} into a {@code Path}.<br>
	 * This method delegates the conversion to the wrapped manager.
	 * @param file The {@code FileObject} to convert.
	 * @return The resulting {@code Path} object.
	 */
	@Override
	public Path asPath(FileObject file)
	{
		return _wrapped.asPath(file);
	}
	
	/**
	 * Converts a collection of file paths into {@code JavaFileObject} instances.<br>
	 * This method delegates the conversion to the wrapped {@code StandardJavaFileManager}.
	 * @param paths The collection of {@code Path} objects to process.
	 * @return An {@code Iterable} containing the corresponding {@code JavaFileObject} items.
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjectsFromPaths(Collection<? extends Path> paths)
	{
		return _wrapped.getJavaFileObjectsFromPaths(paths);
	}
	
	/**
	 * Retrieves {@code JavaFileObject} instances for the specified paths.<br>
	 * This method delegates the request to the wrapped {@code StandardJavaFileManager}.
	 * @param paths The file paths to retrieve objects from.
	 * @return An {@code Iterable} containing the requested {@code JavaFileObject} instances.
	 */
	@Override
	public Iterable<? extends JavaFileObject> getJavaFileObjects(Path... paths)
	{
		return _wrapped.getJavaFileObjects(paths);
	}
}
