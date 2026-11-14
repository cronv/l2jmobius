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
package org.l2jmobius.tools.map;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Builds a lightweight colored terrain preview directly from client G16 heightmaps.
 * @author BazookaRpm
 */
public final class TerrainPreviewRenderer
{
	private static final int TILE_PIXELS = 48;
	private static final double ELEVATION_SCALE = 5000.0;
	
	public record PreviewProgress(int processedRegions, int totalRegions, int failedRegions, String currentRegion)
	{
	}
	
	public record PreviewResult(BufferedImage image, int minimumX, int maximumX, int minimumY, int maximumY, int renderedRegions, List<String> failedRegions)
	{
		public PreviewResult
		{
			failedRegions = List.copyOf(failedRegions);
		}
	}
	
	public PreviewResult render(Path clientRoot, List<String> regions, Set<String> classicRegions, boolean preferClassic, BooleanSupplier cancelled, Consumer<PreviewProgress> progressConsumer)
	{
		final Map<String, String> mapSources = new HashMap<>();
		for (String region : regions)
		{
			mapSources.put(region, preferClassic && classicRegions.contains(region) ? region + "_Classic" : region);
		}
		
		return render(clientRoot, regions, mapSources, cancelled, progressConsumer);
	}
	
	/**
	 * Renders terrain using exact map basenames resolved from the client scan.
	 * @param clientRoot the client root directory
	 * @param regions base XX_YY regions to render
	 * @param mapSources base region to exact map basename
	 * @param cancelled cancellation callback
	 * @param progressConsumer progress callback
	 * @return the rendered terrain preview
	 */
	public PreviewResult render(Path clientRoot, List<String> regions, Map<String, String> mapSources, BooleanSupplier cancelled, Consumer<PreviewProgress> progressConsumer)
	{
		if ((regions == null) || regions.isEmpty())
		{
			throw new IllegalArgumentException("No map regions were supplied for terrain preview.");
		}
		
		int minimumX = Integer.MAX_VALUE;
		int maximumX = Integer.MIN_VALUE;
		int minimumY = Integer.MAX_VALUE;
		int maximumY = Integer.MIN_VALUE;
		for (String region : regions)
		{
			final int[] coordinates = parseRegion(region);
			minimumX = Math.min(minimumX, coordinates[0]);
			maximumX = Math.max(maximumX, coordinates[0]);
			minimumY = Math.min(minimumY, coordinates[1]);
			maximumY = Math.max(maximumY, coordinates[1]);
		}
		
		final int columns = (maximumX - minimumX) + 1;
		final BufferedImage image = new BufferedImage(columns * TILE_PIXELS, (maximumY - minimumY) + 1 * TILE_PIXELS, BufferedImage.TYPE_INT_ARGB);
		final List<String> failedRegions = new ArrayList<>();
		int processed = 0;
		int rendered = 0;
		for (String region : regions)
		{
			if ((cancelled != null) && cancelled.getAsBoolean())
			{
				break;
			}
			
			String mapName = region;
			if (mapSources != null)
			{
				final String scannedSource = mapSources.get(region);
				if ((scannedSource != null) && !scannedSource.isBlank())
				{
					mapName = scannedSource;
				}
			}
			
			try
			{
				final Terrain terrain = Terrain.load(clientRoot, mapName, false);
				final int[] coordinates = parseRegion(region);
				renderTerrain(image, terrain, (coordinates[0] - minimumX) * TILE_PIXELS, (coordinates[1] - minimumY) * TILE_PIXELS);
				rendered++;
			}
			catch (Exception e)
			{
				failedRegions.add(region + ": " + safeMessage(e));
			}
			
			processed++;
			if (progressConsumer != null)
			{
				progressConsumer.accept(new PreviewProgress(processed, regions.size(), failedRegions.size(), region));
			}
		}
		
		return new PreviewResult(image, minimumX, maximumX, minimumY, maximumY, rendered, failedRegions);
	}
	
	private static void renderTerrain(BufferedImage image, Terrain terrain, int offsetX, int offsetY)
	{
		for (int py = 0; py < TILE_PIXELS; py++)
		{
			final int sy = sampleCoordinate(py, TILE_PIXELS, terrain.vSize);
			for (int px = 0; px < TILE_PIXELS; px++)
			{
				final int sx = sampleCoordinate(px, TILE_PIXELS, terrain.uSize);
				if (!isVisible(terrain, sx, sy))
				{
					continue;
				}
				
				final int leftX = Math.max(0, sx - 1);
				final int rightX = Math.min(terrain.uSize - 1, sx + 1);
				final int topY = Math.max(0, sy - 1);
				final int bottomY = Math.min(terrain.vSize - 1, sy + 1);
				final double height = terrain.worldZ(sx, sy);
				final double gradientX = terrain.worldZ(rightX, sy) - terrain.worldZ(leftX, sy);
				final double gradientY = terrain.worldZ(sx, bottomY) - terrain.worldZ(sx, topY);
				final double slopeX = Math.atan(gradientX / Math.max(1.0, Math.abs(terrain.scale[0]) * 2.0));
				final double slopeY = Math.atan(gradientY / Math.max(1.0, Math.abs(terrain.scale[1]) * 2.0));
				final double elevation = 0.5 + (Math.atan(height / ELEVATION_SCALE) / Math.PI);
				final double relief = clamp(0.55 - (slopeX * 0.36) + (slopeY * 0.22));
				final int terrainColor = terrainColor(elevation);
				final double brightness = 0.54 + (relief * 0.58);
				image.setRGB(offsetX + px, offsetY + py, 0xFF000000 | shadeColor(terrainColor, brightness));
			}
		}
	}
	
	private static int terrainColor(double elevation)
	{
		if (elevation < 0.42)
		{
			return interpolateColor(42, 70, 43, 65, 91, 47, elevation / 0.42);
		}
		
		if (elevation < 0.54)
		{
			return interpolateColor(65, 91, 47, 93, 108, 56, (elevation - 0.42) / 0.12);
		}
		
		if (elevation < 0.66)
		{
			return interpolateColor(93, 108, 56, 126, 108, 68, (elevation - 0.54) / 0.12);
		}
		
		if (elevation < 0.78)
		{
			return interpolateColor(126, 108, 68, 139, 118, 88, (elevation - 0.66) / 0.12);
		}
		
		if (elevation < 0.90)
		{
			return interpolateColor(139, 118, 88, 153, 145, 125, (elevation - 0.78) / 0.12);
		}
		
		return interpolateColor(153, 145, 125, 190, 188, 174, (elevation - 0.90) / 0.10);
	}
	
	private static int interpolateColor(int redA, int greenA, int blueA, int redB, int greenB, int blueB, double factor)
	{
		final double amount = clamp(factor);
		final int red = clampColor((int) Math.round(redA + ((redB - redA) * amount)));
		final int green = clampColor((int) Math.round(greenA + ((greenB - greenA) * amount)));
		final int blue = clampColor((int) Math.round(blueA + ((blueB - blueA) * amount)));
		return (red << 16) | (green << 8) | blue;
	}
	
	private static int shadeColor(int color, double brightness)
	{
		final int red = clampColor((int) Math.round(((color >> 16) & 0xFF) * brightness));
		final int green = clampColor((int) Math.round(((color >> 8) & 0xFF) * brightness));
		final int blue = clampColor((int) Math.round((color & 0xFF) * brightness));
		return (red << 16) | (green << 8) | blue;
	}
	
	private static boolean isVisible(Terrain terrain, int x, int y)
	{
		if ((terrain.uSize < 2) || (terrain.vSize < 2))
		{
			return true;
		}
		
		final int qx = Math.min(terrain.uSize - 2, x);
		final int qy = Math.min(terrain.vSize - 2, y);
		return terrain.isQuadVisible(qx, qy);
	}
	
	private static int sampleCoordinate(int pixel, int pixelSize, int terrainSize)
	{
		if ((pixelSize <= 1) || (terrainSize <= 1))
		{
			return 0;
		}
		
		return Math.min(terrainSize - 1, (int) Math.round((pixel * (terrainSize - 1.0)) / (pixelSize - 1.0)));
	}
	
	private static double clamp(double value)
	{
		return Math.max(0.0, Math.min(1.0, value));
	}
	
	private static int clampColor(int value)
	{
		return Math.max(0, Math.min(255, value));
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
	
	private static String safeMessage(Throwable error)
	{
		final String message = error.getMessage();
		return ((message == null) || message.isBlank()) ? error.getClass().getSimpleName() : message;
	}
}
