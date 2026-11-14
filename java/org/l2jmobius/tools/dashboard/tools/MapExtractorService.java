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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.CancellationException;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.l2jmobius.tools.map.BspModel;
import org.l2jmobius.tools.map.GeoVerifier;
import org.l2jmobius.tools.map.GeoWriter;
import org.l2jmobius.tools.map.GeometryCollector;
import org.l2jmobius.tools.map.GridBuilder;
import org.l2jmobius.tools.map.StaticMeshes;
import org.l2jmobius.tools.map.Terrain;
import org.l2jmobius.tools.map.XmlWriter;

/**
 * Provides the MapExtractor operations used by the integrated Dashboard page.
 * @author BazookaRpm
 */
public final class MapExtractorService
{
	private static final Pattern MAP_NAME = Pattern.compile("\\d+_\\d+");
	private static final Pattern MAP_SOURCE_NAME = Pattern.compile("(\\d+_\\d+)(?:_(.+))?");
	private static final String CLASSIC_VARIANT = "classic";
	private static final Object CAPTURE_LOCK = new Object();
	private final Map<String, Terrain> _terrains = new HashMap<>();
	
	public record ValidationResult(boolean clientDetected, boolean mapsFound, boolean texturesFound, boolean staticMeshesFound, boolean outputWritable, boolean referenceValid, long freeDiskBytes, List<String> issues)
	{
		public ValidationResult
		{
			issues = List.copyOf(issues);
		}
		
		public boolean isReady(boolean geodata)
		{
			return mapsFound && outputWritable && referenceValid && (!geodata || (texturesFound && staticMeshesFound));
		}
	}
	
	public record ClientScanResult(List<String> regions, Set<String> standardRegions, Map<String, Map<String, String>> variantSources, int minimumX, int maximumX, int minimumY, int maximumY, long mapBytes, int existingGeodata)
	{
		public ClientScanResult
		{
			regions = List.copyOf(regions);
			standardRegions = Set.copyOf(standardRegions);
			final Map<String, Map<String, String>> copiedSources = new TreeMap<>();
			for (Entry<String, Map<String, String>> entry : variantSources.entrySet())
			{
				copiedSources.put(normalizeMapVariant(entry.getKey()), Map.copyOf(entry.getValue()));
			}
			
			variantSources = Collections.unmodifiableMap(copiedSources);
		}
		
		public ClientScanResult withExistingGeodata(int count)
		{
			return new ClientScanResult(regions, standardRegions, variantSources, minimumX, maximumX, minimumY, maximumY, mapBytes, count);
		}
		
		/**
		 * Returns the regions carrying the requested map variant.
		 * @param mapVariant the variant suffix, without the leading underscore
		 * @return the matching base region names
		 */
		public Set<String> variantRegions(String mapVariant)
		{
			final Map<String, String> sources = variantSources.get(normalizeMapVariant(mapVariant));
			return sources == null ? Collections.emptySet() : sources.keySet();
		}
		
		/**
		 * Returns all detected variant names.
		 * @return normalized map variant names
		 */
		public Set<String> variantNames()
		{
			return variantSources.keySet();
		}
		
		/**
		 * Returns every detected variant available for a region.
		 * @param region the base XX_YY region
		 * @return normalized variant names
		 */
		public Set<String> variantsFor(String region)
		{
			final Set<String> variants = new TreeSet<>();
			for (Entry<String, Map<String, String>> entry : variantSources.entrySet())
			{
				if (entry.getValue().containsKey(region))
				{
					variants.add(entry.getKey());
				}
			}
			
			return Collections.unmodifiableSet(variants);
		}
		
		/**
		 * Returns whether a region has at least one detected variant.
		 * @param region the base XX_YY region
		 * @return {@code true} when a variant source exists
		 */
		public boolean hasVariants(String region)
		{
			for (Map<String, String> sources : variantSources.values())
			{
				if (sources.containsKey(region))
				{
					return true;
				}
			}
			
			return false;
		}
		
		/**
		 * Returns base regions usable with the requested variant preference. Standard maps remain the fallback when a region has no preferred variant.
		 * @param mapVariant the preferred variant suffix, or empty for Standard
		 * @return selectable base region names
		 */
		public List<String> availableRegions(String mapVariant)
		{
			final Map<String, String> preferredSources = variantSources.get(normalizeMapVariant(mapVariant));
			final List<String> result = new ArrayList<>();
			for (String region : regions)
			{
				if (standardRegions.contains(region) || ((preferredSources != null) && preferredSources.containsKey(region)))
				{
					result.add(region);
				}
			}
			
			return result;
		}
		
		/**
		 * Resolves the exact scanned map basenames used by terrain preview.
		 * @param mapVariant the preferred variant suffix, or empty for Standard
		 * @return base region to actual map basename
		 */
		public Map<String, String> sourceMapNames(String mapVariant)
		{
			final Map<String, String> preferredSources = variantSources.get(normalizeMapVariant(mapVariant));
			final Map<String, String> result = new TreeMap<>();
			for (String region : regions)
			{
				String source = null;
				if (preferredSources != null)
				{
					source = preferredSources.get(region);
				}
				
				if ((source == null) && standardRegions.contains(region))
				{
					source = region;
				}
				
				if (source != null)
				{
					result.put(region, source);
				}
			}
			
			return Collections.unmodifiableMap(result);
		}
	}
	
	public record RegionPresets(Set<String> world, Set<String> aden, Set<String> giran, Set<String> dion)
	{
		public RegionPresets
		{
			world = Set.copyOf(world);
			aden = Set.copyOf(aden);
			giran = Set.copyOf(giran);
			dion = Set.copyOf(dion);
		}
	}
	
	public record ExtractionRequest(Path clientRoot, Path outputRoot, List<String> regions, boolean geodata, boolean doors, boolean water, String mapVariant)
	{
		public ExtractionRequest
		{
			regions = List.copyOf(regions);
			mapVariant = normalizeMapVariant(mapVariant);
		}
	}
	
	public record ExtractionProgress(int processedRegions, int totalRegions, int outputsGenerated, int warnings, int errors, long elapsedMillis, long etaMillis, String currentRegion, String status)
	{
	}
	
	public record ExtractionReport(Path clientRoot, Path outputRoot, boolean geodata, boolean doors, boolean water, String mapVariant, int requestedRegions, int processedRegions, int successfulRegions, int outputsGenerated, int warnings, int errors, boolean stopped, Instant startedAt, Instant finishedAt, List<String> failedRegions)
	{
		public ExtractionReport
		{
			mapVariant = normalizeMapVariant(mapVariant);
			failedRegions = List.copyOf(failedRegions);
		}
	}
	
	public ValidationResult validatePaths(Path clientRoot, Path outputRoot, Path referenceRoot, boolean geodata) throws IOException
	{
		final List<String> issues = new ArrayList<>();
		final boolean clientDetected;
		final boolean mapsFound;
		final boolean texturesFound;
		final boolean staticMeshesFound;
		if (clientRoot == null)
		{
			clientDetected = false;
			mapsFound = false;
			texturesFound = false;
			staticMeshesFound = false;
		}
		else
		{
			clientDetected = Files.isDirectory(clientRoot);
			mapsFound = clientDetected && Files.isDirectory(clientRoot.resolve("maps"));
			texturesFound = clientDetected && Files.isDirectory(clientRoot.resolve("textures"));
			staticMeshesFound = clientDetected && Files.isDirectory(clientRoot.resolve("staticmeshes"));
		}
		
		if (!clientDetected)
		{
			issues.add("Client folder was not found.");
		}
		else if (!mapsFound)
		{
			issues.add("Client maps directory was not found.");
		}
		
		if (geodata && !texturesFound)
		{
			issues.add("Client textures directory was not found and is required for geodata extraction.");
		}
		
		if (geodata && !staticMeshesFound)
		{
			issues.add("Client staticmeshes directory was not found and is required for full geodata extraction.");
		}
		
		boolean outputWritable = false;
		long freeDiskBytes = 0;
		if (outputRoot == null)
		{
			issues.add("Output folder is not configured.");
		}
		else
		{
			Files.createDirectories(outputRoot);
			outputWritable = Files.isDirectory(outputRoot) && Files.isWritable(outputRoot);
			if (!outputWritable)
			{
				issues.add("Output folder is not writable.");
			}
			else
			{
				freeDiskBytes = Files.getFileStore(outputRoot).getUsableSpace();
			}
		}
		
		boolean referenceValid = true;
		if (referenceRoot != null)
		{
			referenceValid = Files.isDirectory(referenceRoot);
			if (!referenceValid)
			{
				issues.add("Reference geodata folder was not found.");
			}
		}
		
		return new ValidationResult(clientDetected, mapsFound, texturesFound, staticMeshesFound, outputWritable, referenceValid, freeDiskBytes, issues);
	}
	
	public ClientScanResult scanClient(Path clientRoot, Path outputRoot) throws IOException
	{
		validateMapDirectory(clientRoot);
		final Path mapsDirectory = clientRoot.resolve("maps");
		final Set<String> standardRegions = new HashSet<>();
		final Map<String, Map<String, String>> variantSources = new HashMap<>();
		long mapBytes = 0;
		try (DirectoryStream<Path> files = Files.newDirectoryStream(mapsDirectory))
		{
			for (Path file : files)
			{
				if (!Files.isRegularFile(file))
				{
					continue;
				}
				
				final String fileName = file.getFileName().toString();
				if (!fileName.toLowerCase(Locale.ROOT).endsWith(".unr"))
				{
					continue;
				}
				
				final String mapName = fileName.substring(0, fileName.length() - 4);
				final Matcher matcher = MAP_SOURCE_NAME.matcher(mapName);
				if (!matcher.matches())
				{
					continue;
				}
				
				final String region = matcher.group(1);
				final String variant = matcher.group(2);
				if (variant == null)
				{
					standardRegions.add(region);
				}
				else
				{
					final String normalizedVariant = normalizeMapVariant(variant);
					Map<String, String> sources = variantSources.get(normalizedVariant);
					if (sources == null)
					{
						sources = new HashMap<>();
						variantSources.put(normalizedVariant, sources);
					}
					
					final String previous = sources.putIfAbsent(region, mapName);
					if ((previous != null) && !previous.equals(mapName))
					{
						throw new IOException("Ambiguous " + normalizedVariant + " map variants for region " + region + ": " + previous + ".unr and " + mapName + ".unr.");
					}
				}
				
				mapBytes += Files.size(file);
			}
		}
		
		final Set<String> allRegions = new TreeSet<>();
		allRegions.addAll(standardRegions);
		for (Map<String, String> sources : variantSources.values())
		{
			allRegions.addAll(sources.keySet());
		}
		
		if (allRegions.isEmpty())
		{
			throw new IOException("No XX_YY.unr map tiles were found under " + mapsDirectory + ".");
		}
		
		int minimumX = Integer.MAX_VALUE;
		int maximumX = Integer.MIN_VALUE;
		int minimumY = Integer.MAX_VALUE;
		int maximumY = Integer.MIN_VALUE;
		for (String region : allRegions)
		{
			final int[] coordinates = parseRegion(region);
			minimumX = Math.min(minimumX, coordinates[0]);
			maximumX = Math.max(maximumX, coordinates[0]);
			minimumY = Math.min(minimumY, coordinates[1]);
			maximumY = Math.max(maximumY, coordinates[1]);
		}
		
		return new ClientScanResult(new ArrayList<>(allRegions), standardRegions, variantSources, minimumX, maximumX, minimumY, maximumY, mapBytes, countExistingGeodata(outputRoot));
	}
	
	public RegionPresets loadRegionPresets(Path mapRegionDirectory) throws IOException
	{
		if ((mapRegionDirectory == null) || !Files.isDirectory(mapRegionDirectory))
		{
			return new RegionPresets(Collections.emptySet(), Collections.emptySet(), Collections.emptySet(), Collections.emptySet());
		}
		
		final Set<String> world = new LinkedHashSet<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(mapRegionDirectory, "*.xml"))
		{
			for (Path file : files)
			{
				world.addAll(loadMapRegions(file));
			}
		}
		
		final Set<String> aden = loadMapRegionsIfPresent(mapRegionDirectory.resolve("aden_town.xml"));
		final Set<String> giran = loadMapRegionsIfPresent(mapRegionDirectory.resolve("giran_castle_town.xml"));
		final Set<String> dion = loadMapRegionsIfPresent(mapRegionDirectory.resolve("dion_castle_town.xml"));
		return new RegionPresets(world, aden, giran, dion);
	}
	
	public ExtractionReport extract(ExtractionRequest request, BooleanSupplier stopRequested, Consumer<ExtractionProgress> progressConsumer, Consumer<String> logger) throws IOException
	{
		if (!request.geodata() && !request.doors() && !request.water())
		{
			throw new IllegalArgumentException("Select at least one extraction output.");
		}
		
		final ValidationResult validation = validatePaths(request.clientRoot(), request.outputRoot(), null, request.geodata());
		if (!validation.isReady(request.geodata()))
		{
			throw new IOException(validation.issues().isEmpty() ? "Map Extractor paths are not ready." : validation.issues().get(0));
		}
		
		if (request.regions().isEmpty())
		{
			throw new IllegalArgumentException("Select at least one map region.");
		}
		
		final List<String> regions = new ArrayList<>(request.regions());
		Collections.sort(regions);
		for (String region : regions)
		{
			if (!MAP_NAME.matcher(region).matches())
			{
				throw new IllegalArgumentException("Invalid region " + region + ". Expected XX_YY.");
			}
		}
		
		final Instant startedAt = Instant.now();
		final long startedNanos = System.nanoTime();
		final int total = regions.size();
		final List<String> failedRegions = new ArrayList<>();
		int processed = 0;
		int successful = 0;
		int outputs = 0;
		int warnings = 0;
		int errors = 0;
		boolean stopped = false;
		
		logger.accept("[INFO] Starting Map Extractor.");
		logger.accept("[INFO] Client path: " + request.clientRoot());
		logger.accept("[INFO] Output path: " + request.outputRoot());
		logger.accept("[INFO] Selected regions: " + total + ".");
		logger.accept("[INFO] Map variant: " + displayMapVariant(request.mapVariant()) + ".");
		publishProgress(progressConsumer, processed, total, outputs, warnings, errors, startedNanos, "", "Starting");
		
		for (String region : regions)
		{
			if ((stopRequested != null) && stopRequested.getAsBoolean())
			{
				stopped = true;
				logger.accept("[INFO] Stop request accepted before region " + region + ".");
				break;
			}
			
			logger.accept("[INFO] [" + processed + 1 + "/" + total + "] Region " + region + ".");
			publishProgress(progressConsumer, processed, total, outputs, warnings, errors, startedNanos, region, "Extracting");
			try
			{
				outputs += generateOne(request.clientRoot(), request.outputRoot(), region, request.geodata(), request.doors(), request.water(), request.mapVariant(), stopRequested, logger);
				successful++;
			}
			catch (CancellationException e)
			{
				stopped = true;
				logger.accept("[INFO] Stop request accepted during region " + region + ".");
				break;
			}
			catch (Exception e)
			{
				errors++;
				failedRegions.add(region + ": " + safeMessage(e));
				logger.accept("[ERROR] Region " + region + " failed: " + safeMessage(e));
			}
			
			processed++;
			publishProgress(progressConsumer, processed, total, outputs, warnings, errors, startedNanos, region, "Completed region");
		}
		
		final Instant finishedAt = Instant.now();
		logger.accept("[INFO] Extraction finished: " + successful + " successful, " + errors + " failed, " + outputs + " output files.");
		if (stopped)
		{
			logger.accept("[INFO] Extraction stopped safely.");
		}
		
		return new ExtractionReport(request.clientRoot(), request.outputRoot(), request.geodata(), request.doors(), request.water(), request.mapVariant(), total, processed, successful, outputs, warnings, errors, stopped, startedAt, finishedAt, failedRegions);
	}
	
	public void writeReport(ExtractionReport report, Path outputFile) throws IOException
	{
		final Path parent = outputFile.toAbsolutePath().normalize().getParent();
		if (parent != null)
		{
			Files.createDirectories(parent);
		}
		
		final StringBuilder text = new StringBuilder(2048);
		text.append("L2J Mobius - Map Extractor Report\n");
		text.append("================================================\n");
		text.append("Started: ").append(DateTimeFormatter.ISO_INSTANT.format(report.startedAt())).append('\n');
		text.append("Finished: ").append(DateTimeFormatter.ISO_INSTANT.format(report.finishedAt())).append('\n');
		text.append("Elapsed: ").append(formatDuration(Duration.between(report.startedAt(), report.finishedAt()))).append('\n');
		text.append("Client: ").append(report.clientRoot()).append('\n');
		text.append("Output: ").append(report.outputRoot()).append('\n');
		text.append("Geodata: ").append(report.geodata()).append('\n');
		text.append("Doors: ").append(report.doors()).append('\n');
		text.append("Water zones: ").append(report.water()).append('\n');
		text.append("Map variant: ").append(displayMapVariant(report.mapVariant())).append('\n');
		text.append("Requested regions: ").append(report.requestedRegions()).append('\n');
		text.append("Processed regions: ").append(report.processedRegions()).append('\n');
		text.append("Successful regions: ").append(report.successfulRegions()).append('\n');
		text.append("Output files: ").append(report.outputsGenerated()).append('\n');
		text.append("Warnings: ").append(report.warnings()).append('\n');
		text.append("Errors: ").append(report.errors()).append('\n');
		text.append("Stopped: ").append(report.stopped()).append('\n');
		if (!report.failedRegions().isEmpty())
		{
			text.append("\nFailed regions\n");
			text.append("--------------\n");
			for (String failure : report.failedRegions())
			{
				text.append(failure).append('\n');
			}
		}
		
		Files.writeString(outputFile, text, StandardCharsets.UTF_8);
	}
	
	public void generate(Path clientRoot, Path outputRoot, String region, boolean geodata, boolean doors, boolean water, boolean classic, Consumer<String> logger) throws IOException
	{
		generate(clientRoot, outputRoot, region, geodata, doors, water, classic ? CLASSIC_VARIANT : "", logger);
	}
	
	/**
	 * Generates the requested map outputs using a preferred detected map variant.
	 * @param clientRoot the client root directory
	 * @param outputRoot the output directory
	 * @param region an optional XX_YY region or XX_YY_variant map name
	 * @param geodata whether geodata output is enabled
	 * @param doors whether door XML output is enabled
	 * @param water whether water-zone XML output is enabled
	 * @param mapVariant the preferred map variant suffix, or empty for Standard
	 * @param logger the extraction log consumer
	 * @throws IOException when client data cannot be read or written
	 */
	public void generate(Path clientRoot, Path outputRoot, String region, boolean geodata, boolean doors, boolean water, String mapVariant, Consumer<String> logger) throws IOException
	{
		final List<String> regions = new ArrayList<>();
		String preferredVariant = normalizeMapVariant(mapVariant);
		if ((region == null) || region.isBlank())
		{
			regions.addAll(scanClient(clientRoot, outputRoot).availableRegions(preferredVariant));
		}
		else
		{
			final Matcher matcher = MAP_SOURCE_NAME.matcher(region.trim());
			if (!matcher.matches())
			{
				throw new IllegalArgumentException("Region must use the XX_YY format, for example 22_22.");
			}
			
			regions.add(matcher.group(1));
			if (preferredVariant.isEmpty() && (matcher.group(2) != null))
			{
				preferredVariant = normalizeMapVariant(matcher.group(2));
			}
		}
		
		extract(new ExtractionRequest(clientRoot, outputRoot, regions, geodata, doors, water, preferredVariant), () -> false, null, logger);
	}
	
	public String queryHeight(Path clientRoot, String coordinates, boolean classic) throws IOException
	{
		return queryHeight(clientRoot, coordinates, classic ? CLASSIC_VARIANT : "");
	}
	
	/**
	 * Queries terrain and surface information using a preferred detected map variant.
	 * @param clientRoot the client root directory
	 * @param coordinates the world X/Y coordinates
	 * @param mapVariant the preferred map variant suffix, or empty for Standard
	 * @return the terrain and surface report
	 * @throws IOException when the map data cannot be read
	 */
	public String queryHeight(Path clientRoot, String coordinates, String mapVariant) throws IOException
	{
		validateMapDirectory(clientRoot);
		final double[] point = parseCoordinates(coordinates);
		final String map = selectSource(clientRoot, mapFor(point[0], point[1]), mapVariant);
		final StringBuilder result = new StringBuilder();
		result.append(String.format("Terrain height at (%.0f, %.0f) on map %s = %.1f%n", point[0], point[1], map, terrainFor(clientRoot, map).heightAt(point[0], point[1])));
		result.append(captureSurfaceOutput(clientRoot, map, point[0], point[1]));
		return result.toString();
	}
	
	public void dumpHeightMap(Path clientRoot, String coordinates, boolean classic, Path outputFile) throws IOException
	{
		dumpHeightMap(clientRoot, coordinates, classic ? CLASSIC_VARIANT : "", outputFile);
	}
	
	/**
	 * Dumps a terrain heightmap using a preferred detected map variant.
	 * @param clientRoot the client root directory
	 * @param coordinates the world X/Y coordinates
	 * @param mapVariant the preferred map variant suffix, or empty for Standard
	 * @param outputFile the destination text file
	 * @throws IOException when the map data cannot be read or written
	 */
	public void dumpHeightMap(Path clientRoot, String coordinates, String mapVariant, Path outputFile) throws IOException
	{
		validateMapDirectory(clientRoot);
		final double[] point = parseCoordinates(coordinates);
		final String map = selectSource(clientRoot, mapFor(point[0], point[1]), mapVariant);
		GeoVerifier.dump(terrainFor(clientRoot, map), outputFile);
	}
	
	private synchronized Terrain terrainFor(Path clientRoot, String map) throws IOException
	{
		Terrain terrain = _terrains.get(map);
		if (terrain == null)
		{
			terrain = Terrain.load(clientRoot, map);
			_terrains.put(map, terrain);
		}
		
		return terrain;
	}
	
	private static String captureSurfaceOutput(Path clientRoot, String map, double x, double y) throws IOException
	{
		synchronized (CAPTURE_LOCK)
		{
			final PrintStream previousOut = System.out;
			final PrintStream previousErr = System.err;
			final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
			final PrintStream capture = new PrintStream(buffer, true, StandardCharsets.UTF_8);
			try
			{
				System.setOut(capture);
				System.setErr(capture);
				BspModel.bspSurfaces(clientRoot, map, x, y);
				StaticMeshes.meshSurfaces(clientRoot, map, x, y);
			}
			finally
			{
				// The streams go back before the capture closes, otherwise System.out points at a closed stream in between.
				System.setOut(previousOut);
				System.setErr(previousErr);
				capture.close();
			}
			
			return buffer.toString(StandardCharsets.UTF_8);
		}
	}
	
	private static int generateOne(Path clientRoot, Path outputRoot, String map, boolean geodata, boolean doors, boolean water, String mapVariant, BooleanSupplier stopRequested, Consumer<String> logger) throws IOException
	{
		final String source = selectSource(clientRoot, map, mapVariant);
		Files.createDirectories(outputRoot);
		int outputs = 0;
		checkCancelled(stopRequested);
		if (geodata)
		{
			logger.accept("[INFO] " + map + ": extracting geodata from " + source + ".unr.");
			GeoWriter.writeL2j(GridBuilder.buildGrid(clientRoot, source, stopRequested), outputRoot.resolve(map + ".l2j"), stopRequested);
			outputs++;
			checkCancelled(stopRequested);
		}
		
		if (doors)
		{
			logger.accept("[INFO] " + map + ": extracting doors.");
			final List<GeometryCollector.DoorInfo> list = GeometryCollector.extractDoors(clientRoot, source, stopRequested);
			final Iterator<GeometryCollector.DoorInfo> iterator = list.iterator();
			while (iterator.hasNext())
			{
				if (iterator.next().id == 0)
				{
					iterator.remove();
				}
			}
			
			if (!list.isEmpty())
			{
				checkCancelled(stopRequested);
				Files.createDirectories(outputRoot.resolve("doors"));
				XmlWriter.writeDoorsXml(list, map, outputRoot.resolve("doors").resolve(map + ".xml"));
				outputs++;
			}
		}
		
		if (water)
		{
			logger.accept("[INFO] " + map + ": extracting water zones.");
			final List<GeometryCollector.WaterInfo> list = GeometryCollector.extractWater(clientRoot, source, stopRequested);
			if (!list.isEmpty())
			{
				checkCancelled(stopRequested);
				Files.createDirectories(outputRoot.resolve("water"));
				XmlWriter.writeWaterXml(list, map, outputRoot.resolve("water").resolve(map + ".xml"));
				outputs++;
			}
		}
		
		return outputs;
	}
	
	private static String selectSource(Path clientRoot, String map, String mapVariant) throws IOException
	{
		final Path maps = clientRoot.resolve("maps");
		final String normalizedVariant = normalizeMapVariant(mapVariant);
		if (!normalizedVariant.isEmpty())
		{
			final String variantSource = findVariantSource(maps, map, normalizedVariant);
			if (variantSource != null)
			{
				return variantSource;
			}
		}
		
		final Path standard = maps.resolve(map + ".unr");
		if (Files.isRegularFile(standard))
		{
			return map;
		}
		
		throw new IOException("No usable map file was found for region " + map + " using variant " + displayMapVariant(normalizedVariant) + ".");
	}
	
	private static String findVariantSource(Path mapsDirectory, String region, String mapVariant) throws IOException
	{
		String source = null;
		try (DirectoryStream<Path> files = Files.newDirectoryStream(mapsDirectory, region + "_*"))
		{
			for (Path file : files)
			{
				if (!Files.isRegularFile(file))
				{
					continue;
				}
				
				final String fileName = file.getFileName().toString();
				if (!fileName.toLowerCase(Locale.ROOT).endsWith(".unr"))
				{
					continue;
				}
				
				final String mapName = fileName.substring(0, fileName.length() - 4);
				final Matcher matcher = MAP_SOURCE_NAME.matcher(mapName);
				if (!matcher.matches() || !region.equals(matcher.group(1)) || !mapVariant.equals(normalizeMapVariant(matcher.group(2))))
				{
					continue;
				}
				
				if ((source != null) && !source.equals(mapName))
				{
					throw new IOException("Ambiguous " + mapVariant + " map variants for region " + region + ": " + source + ".unr and " + mapName + ".unr.");
				}
				
				source = mapName;
			}
		}
		
		return source;
	}
	
	/**
	 * Returns a stable display label for a map variant identifier.
	 * @param mapVariant the variant suffix, without the leading underscore
	 * @return Standard for the base map or a title-cased variant label
	 */
	public static String displayMapVariant(String mapVariant)
	{
		final String normalizedVariant = normalizeMapVariant(mapVariant);
		if (normalizedVariant.isEmpty())
		{
			return "Standard";
		}
		
		return Character.toUpperCase(normalizedVariant.charAt(0)) + normalizedVariant.substring(1);
	}
	
	private static String normalizeMapVariant(String mapVariant)
	{
		if (mapVariant == null)
		{
			return "";
		}
		
		String normalized = mapVariant.trim();
		while (normalized.startsWith("_"))
		{
			normalized = normalized.substring(1);
		}
		
		return normalized.toLowerCase(Locale.ROOT);
	}
	
	private static void validateMapDirectory(Path clientRoot) throws IOException
	{
		if ((clientRoot == null) || !Files.isDirectory(clientRoot.resolve("maps")))
		{
			throw new IOException("The selected client folder does not contain a maps directory.");
		}
	}
	
	private static Set<String> loadMapRegionsIfPresent(Path file) throws IOException
	{
		return Files.isRegularFile(file) ? loadMapRegions(file) : Collections.emptySet();
	}
	
	private static Set<String> loadMapRegions(Path file) throws IOException
	{
		final Set<String> result = new LinkedHashSet<>();
		try
		{
			final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setXIncludeAware(false);
			factory.setExpandEntityReferences(false);
			final Document document = factory.newDocumentBuilder().parse(file.toFile());
			final NodeList nodes = document.getElementsByTagName("map");
			for (int index = 0; index < nodes.getLength(); index++)
			{
				final Node node = nodes.item(index);
				if (!(node instanceof Element))
				{
					continue;
				}
				
				final Element map = (Element) node;
				final String region = map.getAttribute("X") + "_" + map.getAttribute("Y");
				if (MAP_NAME.matcher(region).matches())
				{
					result.add(region);
				}
			}
		}
		catch (Exception e)
		{
			throw new IOException("Could not read map regions from " + file + ": " + safeMessage(e), e);
		}
		
		return result;
	}
	
	private static void checkCancelled(BooleanSupplier stopRequested)
	{
		if ((stopRequested != null) && stopRequested.getAsBoolean())
		{
			throw new CancellationException("Map extraction stopped.");
		}
	}
	
	public int countExistingGeodata(Path outputRoot) throws IOException
	{
		if ((outputRoot == null) || !Files.isDirectory(outputRoot))
		{
			return 0;
		}
		
		int count = 0;
		try (DirectoryStream<Path> files = Files.newDirectoryStream(outputRoot, "*.l2j"))
		{
			for (Path file : files)
			{
				if (Files.isRegularFile(file))
				{
					count++;
				}
			}
		}
		
		return count;
	}
	
	private static void publishProgress(Consumer<ExtractionProgress> progressConsumer, int processed, int total, int outputs, int warnings, int errors, long startedNanos, String currentRegion, String status)
	{
		if (progressConsumer == null)
		{
			return;
		}
		
		final long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
		long etaMillis = -1;
		if ((processed > 0) && (processed < total))
		{
			etaMillis = Math.round((elapsedMillis / (double) processed) * (total - processed));
		}
		else if (processed >= total)
		{
			etaMillis = 0;
		}
		
		progressConsumer.accept(new ExtractionProgress(processed, total, outputs, warnings, errors, elapsedMillis, etaMillis, currentRegion, status));
	}
	
	private static double[] parseCoordinates(String coordinates)
	{
		if ((coordinates == null) || coordinates.isBlank())
		{
			throw new IllegalArgumentException("Enter world X and Y coordinates.");
		}
		
		final String[] values = coordinates.trim().split("[\\s,]+");
		if (values.length < 2)
		{
			throw new IllegalArgumentException("Enter two coordinates, for example 82698 148638.");
		}
		
		return new double[]
		{
			Double.parseDouble(values[0]),
			Double.parseDouble(values[1])
		};
	}
	
	private static int[] parseRegion(String region)
	{
		final String[] values = region.split("_");
		return new int[]
		{
			Integer.parseInt(values[0]),
			Integer.parseInt(values[1])
		};
	}
	
	private static String mapFor(double x, double y)
	{
		return ((int) Math.floor(x / 32768) + 20) + "_" + ((int) Math.floor(y / 32768) + 18);
	}
	
	private static String formatDuration(Duration duration)
	{
		final long seconds = Math.max(0, duration.toSeconds());
		final long hours = seconds / 3600;
		final long minutes = (seconds % 3600) / 60;
		final long remainingSeconds = seconds % 60;
		if (hours > 0)
		{
			return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
		}
		
		return String.format("%02d:%02d", minutes, remainingSeconds);
	}
	
	private static String safeMessage(Throwable error)
	{
		final String message = error.getMessage();
		return ((message == null) || message.isBlank()) ? error.getClass().getSimpleName() : message;
	}
}
