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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.tools.map.UnrealCrypt;

/**
 * Draws the external Essence world-map annotations from the client data.
 * <p>
 * For clients that provide {@code MinimapRegion_ClassicAden.dat}, HuntingZone remains the localized text source while the matching MinimapRegion record is the authoritative presentation source. The HuntingZone map marker is the MinimapRegion {@code regionid}; this provides the retail world location, description offsets and map-font selection without inventing placement rules.
 * </p>
 * <p>
 * MinimapRegion can contain a unique base record set followed by conditional override records that reuse a region ID. The selector for those overrides is client runtime state and is not encoded in the static preview inputs currently available here, so the first record for each region ID is retained. This is a conservative base-map interpretation and avoids silently applying an override without its runtime condition.
 * </p>
 * @author BazookaRpm
 */
final class EssenceHuntingZoneOverlay
{
	private static final Charset CLIENT_TEXT_CHARSET = Charset.forName("windows-1252");
	private static final int MAX_RECORDS = 10000;
	private static final int MAX_MINIMAP_COLORS = 64;
	private static final int ZONE_METADATA_SIZE = 40;
	private static final int HUNTING_TRAILER_SIZE = 11;
	private static final int WORLD_REGION_SIZE = 32768;
	private static final int WORLD_REGION_X_OFFSET = 20;
	private static final int WORLD_REGION_Y_OFFSET = 18;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int FALLBACK_MATCH_DISTANCE = REGION_PIXEL_SIZE * 2;
	private static final int LABEL_COLLISION_PADDING_X = 3;
	private static final int LABEL_COLLISION_PADDING_Y = 2;
	private static final int MAP_FONT_SIZE_8 = 8;
	private static final int MAP_FONT_SIZE_9 = 9;
	private static final int MAP_FONT_SIZE_10 = 10;
	private static final int TOWN_TYPE = 6;
	private static final int CASTLE_TYPE = 8;
	private static final Font MAP_FONT_8 = new Font(Font.SANS_SERIF, Font.BOLD, MAP_FONT_SIZE_8);
	private static final Font MAP_FONT_9 = new Font(Font.SANS_SERIF, Font.BOLD, MAP_FONT_SIZE_9);
	private static final Font MAP_FONT_10 = new Font(Font.SANS_SERIF, Font.BOLD, MAP_FONT_SIZE_10);
	private static final Color NORMAL_LABEL_COLOR = new Color(238, 238, 238);
	private static final Color CASTLE_LABEL_COLOR = new Color(235, 218, 103);
	private static final Color LABEL_OUTLINE_COLOR = new Color(18, 18, 18, 235);
	private static final int[][] FALLBACK_PLACEMENT_OFFSETS =
	{
		{
			0,
			0
		},
		{
			0,
			-10
		},
		{
			0,
			10
		},
		{
			-14,
			0
		},
		{
			14,
			0
		},
		{
			0,
			-20
		},
		{
			0,
			20
		},
		{
			-20,
			-10
		},
		{
			20,
			-10
		},
		{
			-20,
			10
		},
		{
			20,
			10
		}
	};
	private static final Set<String> SEMANTIC_STOP_WORDS = Set.of("the", "of", "to", "and", "north", "northern", "south", "southern", "part", "entrance", "upper", "lower", "level", "region", "side", "front", "right", "left", "siege", "barracks");
	
	private EssenceHuntingZoneOverlay()
	{
	}
	
	static AnnotationResult draw(Path minimapRegionFile, Path huntingZoneFile, Path zoneNameFile, BufferedImage image, int minimumX, int minimumY, boolean strictActiveMarkers) throws IOException
	{
		if ((minimapRegionFile == null) || (huntingZoneFile == null) || (image == null))
		{
			throw new IOException("Essence annotations require verified MinimapRegion and HuntingZone resources.");
		}
		
		final List<HuntingLabel> huntingLabels = readHuntingLabels(huntingZoneFile);
		if (huntingLabels.isEmpty())
		{
			throw new IOException("Essence HuntingZone data contains no usable labels: " + huntingZoneFile);
		}
		
		final List<MinimapRegionLayout> minimapLayouts = readMinimapRegionLayouts(minimapRegionFile);
		if (minimapLayouts.isEmpty())
		{
			throw new IOException("Essence MinimapRegion data contains no layouts: " + minimapRegionFile);
		}
		
		final Map<Integer, MinimapRegionLayout> baseLayouts = firstLayoutByRegionId(minimapLayouts);
		final Map<Integer, Integer> fontSizes = inferMapFontSizes(baseLayouts, huntingLabels);
		
		Set<String> zoneNames = Set.of();
		boolean fallbackClassificationAvailable = false;
		if (!strictActiveMarkers)
		{
			if (zoneNameFile == null)
			{
				throw new IOException("Essence fallback-label classification requires the verified ZoneName_ClassicAden resource.");
			}
			
			zoneNames = readZoneNameIndex(zoneNameFile);
			fallbackClassificationAvailable = true;
		}
		
		final List<Label> labels = selectWorldMapLabels(huntingLabels, baseLayouts, fontSizes, zoneNames, fallbackClassificationAvailable, strictActiveMarkers, image.getWidth(), image.getHeight(), minimumX, minimumY);
		if (labels.isEmpty())
		{
			return AnnotationResult.EMPTY;
		}
		
		final Graphics2D graphics = image.createGraphics();
		int renderedMinimapLabels = 0;
		int renderedFallbackLabels = 0;
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			final List<Rectangle> occupied = new ArrayList<>();
			for (Label label : labels)
			{
				final Font font = fontFor(label.fontSize());
				graphics.setFont(font);
				final FontMetrics metrics = graphics.getFontMetrics(font);
				final PlacedLabel placedLabel;
				if (label.minimapLayout())
				{
					placedLabel = createMinimapPlacement(label, metrics, image.getWidth(), image.getHeight());
					renderedMinimapLabels++;
				}
				else
				{
					placedLabel = createFallbackPlacement(label, metrics, occupied, image.getWidth(), image.getHeight());
					renderedFallbackLabels++;
				}
				
				final int textWidth = metrics.stringWidth(label.text());
				occupied.add(boundsFor(placedLabel, metrics, textWidth));
				drawOutlinedText(graphics, label.text(), placedLabel.x(), placedLabel.baselineY(), colorFor(label));
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		return new AnnotationResult(renderedMinimapLabels, renderedFallbackLabels);
	}
	
	private static List<Label> selectWorldMapLabels(List<HuntingLabel> huntingLabels, Map<Integer, MinimapRegionLayout> baseLayouts, Map<Integer, Integer> fontSizes, Set<String> zoneNames, boolean fallbackClassificationAvailable, boolean strictActiveMarkers, int width, int height, int minimumX, int minimumY)
	{
		final List<HuntingLabel> primaryLabels = new ArrayList<>();
		for (HuntingLabel huntingLabel : huntingLabels)
		{
			if (huntingLabel.mapMarker() != 0)
			{
				primaryLabels.add(huntingLabel);
			}
		}
		
		final List<Label> labels = new ArrayList<>(primaryLabels.size());
		final Set<String> seen = new HashSet<>();
		for (HuntingLabel huntingLabel : primaryLabels)
		{
			final String key = normalizeText(huntingLabel.text());
			if (!seen.add(key))
			{
				continue;
			}
			
			final MinimapRegionLayout layout = baseLayouts.get(huntingLabel.mapMarker());
			if (layout != null)
			{
				final int x = worldToMapPixel(layout.worldX(), minimumX, WORLD_REGION_X_OFFSET);
				final int y = worldToMapPixel(layout.worldY(), minimumY, WORLD_REGION_Y_OFFSET);
				if (isPotentiallyVisible(x + layout.descriptionOffsetX(), y + layout.descriptionOffsetY(), width, height))
				{
					labels.add(new Label(huntingLabel.text(), x, y, huntingLabel.huntingType(), fontSizes.getOrDefault(layout.fontId(), MAP_FONT_SIZE_9), layout.descriptionOffsetX(), layout.descriptionOffsetY(), true, false));
				}
				continue;
			}
			
			if ((huntingLabel.worldX() == 0) && (huntingLabel.worldY() == 0))
			{
				continue;
			}
			
			final int x = worldToMapPixel(huntingLabel.worldX(), minimumX, WORLD_REGION_X_OFFSET);
			final int y = worldToMapPixel(huntingLabel.worldY(), minimumY, WORLD_REGION_Y_OFFSET);
			if (isPotentiallyVisible(x, y, width, height))
			{
				labels.add(new Label(huntingLabel.text(), x, y, huntingLabel.huntingType(), fallbackFontSizeFor(huntingLabel.huntingType()), 0, 0, false, false));
			}
		}
		
		if (strictActiveMarkers || !fallbackClassificationAvailable)
		{
			return labels;
		}
		
		for (HuntingLabel huntingLabel : huntingLabels)
		{
			if ((huntingLabel.mapMarker() != 0) || ((huntingLabel.minimumLevel() <= 0) && (huntingLabel.maximumLevel() <= 0)))
			{
				continue;
			}
			
			final String key = normalizeText(huntingLabel.text());
			if (seen.contains(key) || zoneNames.contains(key) || hasNearbyPrimaryLabel(huntingLabel, primaryLabels))
			{
				continue;
			}
			
			final int x = worldToMapPixel(huntingLabel.worldX(), minimumX, WORLD_REGION_X_OFFSET);
			final int y = worldToMapPixel(huntingLabel.worldY(), minimumY, WORLD_REGION_Y_OFFSET);
			if (!isPotentiallyVisible(x, y, width, height))
			{
				continue;
			}
			
			seen.add(key);
			labels.add(new Label(huntingLabel.text(), x, y, huntingLabel.huntingType(), MAP_FONT_SIZE_9, 0, 0, false, true));
		}
		
		return labels;
	}
	
	private static Map<Integer, MinimapRegionLayout> firstLayoutByRegionId(List<MinimapRegionLayout> layouts)
	{
		final Map<Integer, MinimapRegionLayout> result = new LinkedHashMap<>();
		for (MinimapRegionLayout layout : layouts)
		{
			result.putIfAbsent(layout.regionId(), layout);
		}
		
		return result;
	}
	
	private static Map<Integer, Integer> inferMapFontSizes(Map<Integer, MinimapRegionLayout> layouts, List<HuntingLabel> huntingLabels)
	{
		final Map<Integer, Integer> allFontCounts = new HashMap<>();
		for (MinimapRegionLayout layout : layouts.values())
		{
			allFontCounts.merge(layout.fontId(), 1, Integer::sum);
		}
		
		if (allFontCounts.isEmpty())
		{
			return Map.of();
		}
		
		final Map<Integer, Integer> majorFontCounts = new HashMap<>();
		for (HuntingLabel huntingLabel : huntingLabels)
		{
			if ((huntingLabel.mapMarker() == 0) || ((huntingLabel.huntingType() != TOWN_TYPE) && (huntingLabel.huntingType() != CASTLE_TYPE)))
			{
				continue;
			}
			
			final MinimapRegionLayout layout = layouts.get(huntingLabel.mapMarker());
			if (layout != null)
			{
				majorFontCounts.merge(layout.fontId(), 1, Integer::sum);
			}
		}
		
		final int size10FontId = mostFrequentFontId(!majorFontCounts.isEmpty() ? majorFontCounts : allFontCounts);
		final List<Entry<Integer, Integer>> remaining = new ArrayList<>();
		for (Entry<Integer, Integer> entry : allFontCounts.entrySet())
		{
			if (entry.getKey() != size10FontId)
			{
				remaining.add(entry);
			}
		}
		
		remaining.sort(Entry.<Integer, Integer> comparingByValue(Comparator.reverseOrder()).thenComparing(Entry.comparingByKey()));
		
		final Map<Integer, Integer> result = new HashMap<>();
		result.put(size10FontId, MAP_FONT_SIZE_10);
		if (!remaining.isEmpty())
		{
			result.put(remaining.get(0).getKey(), MAP_FONT_SIZE_9);
		}
		
		for (int i = 1; i < remaining.size(); i++)
		{
			result.put(remaining.get(i).getKey(), MAP_FONT_SIZE_8);
		}
		
		return result;
	}
	
	private static int mostFrequentFontId(Map<Integer, Integer> counts)
	{
		int selectedFontId = 0;
		int selectedCount = Integer.MIN_VALUE;
		for (Entry<Integer, Integer> entry : counts.entrySet())
		{
			if ((entry.getValue() > selectedCount) || ((entry.getValue() == selectedCount) && (entry.getKey() < selectedFontId)))
			{
				selectedFontId = entry.getKey();
				selectedCount = entry.getValue();
			}
		}
		
		return selectedFontId;
	}
	
	private static List<MinimapRegionLayout> readMinimapRegionLayouts(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile);
		try
		{
			if (buffer.remaining() < Integer.BYTES)
			{
				throw new IOException("Essence MinimapRegion data is missing its record count: " + dataFile);
			}
			
			final int count = buffer.getInt();
			if ((count < 0) || (count > MAX_RECORDS))
			{
				throw new IOException("Essence MinimapRegion record count is invalid: " + count + ".");
			}
			
			final List<MinimapRegionLayout> layouts = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, Short.BYTES + (Integer.BYTES * 5) + (Short.BYTES * 4) + Integer.BYTES + 1, dataFile);
				final int regionId = Short.toUnsignedInt(buffer.getShort());
				buffer.getInt(); // IconTexNormal ASCF identifier.
				buffer.getInt(); // IconTexOver ASCF identifier.
				buffer.getInt(); // IconTexPushed ASCF identifier.
				final int worldX = buffer.getInt();
				final int worldY = buffer.getInt();
				buffer.getShort(); // Width.
				buffer.getShort(); // Height.
				final int descriptionOffsetX = buffer.getShort();
				final int descriptionOffsetY = buffer.getShort();
				final int fontId = buffer.getInt();
				final int colorCount = Byte.toUnsignedInt(buffer.get());
				if (colorCount > MAX_MINIMAP_COLORS)
				{
					throw new IOException("Essence MinimapRegion color count is invalid: " + colorCount + ".");
				}
				
				requireRemaining(buffer, colorCount * Integer.BYTES, dataFile);
				buffer.position(buffer.position() + (colorCount * Integer.BYTES));
				layouts.add(new MinimapRegionLayout(regionId, worldX, worldY, descriptionOffsetX, descriptionOffsetY, fontId));
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Essence MinimapRegion data uses an unsupported record layout: " + dataFile);
			}
			
			return layouts;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Essence MinimapRegion data is truncated or malformed: " + dataFile, e);
		}
	}
	
	private static Set<String> readZoneNameIndex(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile);
		try
		{
			if (buffer.remaining() < Integer.BYTES)
			{
				throw new IOException("Essence ZoneName data is missing its record count: " + dataFile);
			}
			
			final int count = buffer.getInt();
			if ((count < 0) || (count > MAX_RECORDS))
			{
				throw new IOException("Essence ZoneName record count is invalid: " + count + ".");
			}
			
			final Set<String> names = new HashSet<>();
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, (Short.BYTES * 3) + (Float.BYTES * 2), dataFile);
				buffer.position(buffer.position() + (Short.BYTES * 3) + (Float.BYTES * 2));
				final String text = readClientString(buffer, dataFile).trim();
				requireRemaining(buffer, ZONE_METADATA_SIZE, dataFile);
				buffer.position(buffer.position() + ZONE_METADATA_SIZE);
				if (!text.isBlank())
				{
					names.add(normalizeText(text));
				}
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Essence ZoneName data uses an unsupported record layout: " + dataFile);
			}
			
			return names;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Essence ZoneName data is truncated or malformed: " + dataFile, e);
		}
	}
	
	static Set<Integer> readActiveMapMarkers(Path huntingZoneFile) throws IOException
	{
		if (huntingZoneFile == null)
		{
			throw new IOException("Essence active-marker resolution requires the verified HuntingZone_ClassicAden resource.");
		}
		
		final Set<Integer> activeMarkers = new HashSet<>();
		for (HuntingLabel huntingLabel : readHuntingLabels(huntingZoneFile))
		{
			if (huntingLabel.mapMarker() != 0)
			{
				activeMarkers.add(huntingLabel.mapMarker());
			}
		}
		
		return activeMarkers;
	}
	
	private static List<HuntingLabel> readHuntingLabels(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile);
		try
		{
			if (buffer.remaining() < Integer.BYTES)
			{
				throw new IOException("Essence HuntingZone data is missing its record count: " + dataFile);
			}
			
			final int count = buffer.getInt();
			if ((count < 0) || (count > MAX_RECORDS))
			{
				throw new IOException("Essence HuntingZone record count is invalid: " + count + ".");
			}
			
			final List<HuntingLabel> labels = new ArrayList<>();
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, (Integer.BYTES * 4) + (Float.BYTES * 3), dataFile);
				buffer.getInt(); // ID.
				final int huntingType = buffer.getInt();
				final int minimumLevel = buffer.getInt();
				final int maximumLevel = buffer.getInt();
				final float worldX = buffer.getFloat();
				final float worldY = buffer.getFloat();
				buffer.getFloat(); // World Z.
				readClientString(buffer, dataFile); // Description.
				requireRemaining(buffer, Integer.BYTES, dataFile);
				buffer.getInt(); // Localized name flag/identifier.
				final String text = readClientString(buffer, dataFile).trim();
				requireRemaining(buffer, HUNTING_TRAILER_SIZE, dataFile);
				final int mapMarker = Byte.toUnsignedInt(buffer.get());
				buffer.position(buffer.position() + HUNTING_TRAILER_SIZE - 1);
				
				if (text.isBlank() || !Float.isFinite(worldX) || !Float.isFinite(worldY) || ((mapMarker == 0) && (worldX == 0) && (worldY == 0)))
				{
					continue;
				}
				
				labels.add(new HuntingLabel(text, worldX, worldY, huntingType, minimumLevel, maximumLevel, mapMarker));
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Essence HuntingZone data uses an unsupported record layout: " + dataFile);
			}
			
			return labels;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Essence HuntingZone data is truncated or malformed: " + dataFile, e);
		}
	}
	
	private static boolean hasNearbyPrimaryLabel(HuntingLabel candidate, List<HuntingLabel> primaryLabels)
	{
		final int candidateX = worldToMapPixel(candidate.worldX(), 0, WORLD_REGION_X_OFFSET);
		final int candidateY = worldToMapPixel(candidate.worldY(), 0, WORLD_REGION_Y_OFFSET);
		for (HuntingLabel primary : primaryLabels)
		{
			if (!similarText(candidate.text(), primary.text()))
			{
				continue;
			}
			
			final int primaryX = worldToMapPixel(primary.worldX(), 0, WORLD_REGION_X_OFFSET);
			final int primaryY = worldToMapPixel(primary.worldY(), 0, WORLD_REGION_Y_OFFSET);
			if (Math.hypot(candidateX - primaryX, candidateY - primaryY) <= FALLBACK_MATCH_DISTANCE)
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static PlacedLabel createMinimapPlacement(Label label, FontMetrics metrics, int width, int height)
	{
		final int textWidth = metrics.stringWidth(label.text());
		final int ascent = metrics.getAscent();
		final int descent = metrics.getDescent();
		int x = label.x() + label.descriptionOffsetX();
		int baselineY = label.y() + label.descriptionOffsetY() + ascent;
		if (x < 0)
		{
			x = 0;
		}
		else if ((x + textWidth) > width)
		{
			x = Math.max(0, width - textWidth);
		}
		
		if ((baselineY - ascent) < 0)
		{
			baselineY = ascent;
		}
		else if ((baselineY + descent) > height)
		{
			baselineY = height - descent;
		}
		
		return new PlacedLabel(label, x, baselineY);
	}
	
	private static PlacedLabel createFallbackPlacement(Label label, FontMetrics metrics, List<Rectangle> occupied, int width, int height)
	{
		final int textWidth = metrics.stringWidth(label.text());
		PlacedLabel placement = null;
		for (int[] offset : FALLBACK_PLACEMENT_OFFSETS)
		{
			final PlacedLabel candidate = createCenteredPlacement(label, offset[0], offset[1], metrics, textWidth, width, height);
			if (!intersects(boundsFor(candidate, metrics, textWidth), occupied))
			{
				placement = candidate;
				break;
			}
		}
		
		return placement != null ? placement : createCenteredPlacement(label, 0, 0, metrics, textWidth, width, height);
	}
	
	private static PlacedLabel createCenteredPlacement(Label label, int offsetX, int offsetY, FontMetrics metrics, int textWidth, int width, int height)
	{
		final int ascent = metrics.getAscent();
		final int descent = metrics.getDescent();
		int x = label.x() - (textWidth / 2) + offsetX;
		int baselineY = label.y() + ((ascent - descent) / 2) + offsetY;
		if (x < 0)
		{
			x = 0;
		}
		else if ((x + textWidth) > width)
		{
			x = width - textWidth;
		}
		
		if ((baselineY - ascent) < 0)
		{
			baselineY = ascent;
		}
		else if ((baselineY + descent) > height)
		{
			baselineY = height - descent;
		}
		
		return new PlacedLabel(label, x, baselineY);
	}
	
	private static Rectangle boundsFor(PlacedLabel placedLabel, FontMetrics metrics, int textWidth)
	{
		final int ascent = metrics.getAscent();
		final int descent = metrics.getDescent();
		return new Rectangle(placedLabel.x() - LABEL_COLLISION_PADDING_X, placedLabel.baselineY() - ascent - LABEL_COLLISION_PADDING_Y, textWidth + (LABEL_COLLISION_PADDING_X * 2), ascent + descent + (LABEL_COLLISION_PADDING_Y * 2));
	}
	
	private static boolean intersects(Rectangle bounds, List<Rectangle> occupied)
	{
		for (Rectangle existing : occupied)
		{
			if (bounds.intersects(existing))
			{
				return true;
			}
		}
		
		return false;
	}
	
	private static Font fontFor(int size)
	{
		if (size == MAP_FONT_SIZE_8)
		{
			return MAP_FONT_8;
		}
		
		if (size == MAP_FONT_SIZE_10)
		{
			return MAP_FONT_10;
		}
		
		return MAP_FONT_9;
	}
	
	private static int fallbackFontSizeFor(int huntingType)
	{
		return ((huntingType == CASTLE_TYPE) || (huntingType == TOWN_TYPE)) ? MAP_FONT_SIZE_10 : MAP_FONT_SIZE_9;
	}
	
	private static Color colorFor(Label label)
	{
		return label.huntingType() == CASTLE_TYPE ? CASTLE_LABEL_COLOR : NORMAL_LABEL_COLOR;
	}
	
	private static void drawOutlinedText(Graphics2D graphics, String text, int x, int baselineY, Color color)
	{
		graphics.setColor(LABEL_OUTLINE_COLOR);
		for (int offsetY = -1; offsetY <= 1; offsetY++)
		{
			for (int offsetX = -1; offsetX <= 1; offsetX++)
			{
				if ((offsetX != 0) || (offsetY != 0))
				{
					graphics.drawString(text, x + offsetX, baselineY + offsetY);
				}
			}
		}
		
		graphics.setColor(color);
		graphics.drawString(text, x, baselineY);
	}
	
	private static boolean similarText(String first, String second)
	{
		final Set<String> firstWords = semanticWords(first);
		final Set<String> secondWords = semanticWords(second);
		if (firstWords.isEmpty() || secondWords.isEmpty())
		{
			return false;
		}
		
		if (firstWords.containsAll(secondWords) || secondWords.containsAll(firstWords))
		{
			return true;
		}
		
		int common = 0;
		for (String word : firstWords)
		{
			if (secondWords.contains(word))
			{
				common++;
			}
		}
		
		return (common >= 2) && (common >= (Math.min(firstWords.size(), secondWords.size()) - 1));
	}
	
	private static Set<String> semanticWords(String text)
	{
		final Set<String> result = new HashSet<>();
		for (String word : normalizeText(text).split(" "))
		{
			if (word.isBlank() || SEMANTIC_STOP_WORDS.contains(word))
			{
				continue;
			}
			
			if ((word.length() > 4) && word.endsWith("s"))
			{
				word = word.substring(0, word.length() - 1);
			}
			
			result.add(word);
		}
		
		return result;
	}
	
	private static String normalizeText(String text)
	{
		return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", " ").trim();
	}
	
	private static boolean isPotentiallyVisible(int x, int y, int width, int height)
	{
		return (x >= -REGION_PIXEL_SIZE) && (y >= -REGION_PIXEL_SIZE) && (x <= (width + REGION_PIXEL_SIZE)) && (y <= (height + REGION_PIXEL_SIZE));
	}
	
	private static int worldToMapPixel(float worldCoordinate, int minimumRegion, int regionOffset)
	{
		final double regionPosition = (worldCoordinate / WORLD_REGION_SIZE) + regionOffset - minimumRegion;
		return (int) Math.round(regionPosition * REGION_PIXEL_SIZE);
	}
	
	private static String readClientString(ByteBuffer buffer, Path dataFile) throws IOException
	{
		final int length = readCompactIndex(buffer, dataFile);
		if (length == 0)
		{
			return "";
		}
		
		if (length > 0)
		{
			requireRemaining(buffer, length, dataFile);
			final byte[] text = new byte[length];
			buffer.get(text);
			final int textLength = (text.length > 0) && (text[text.length - 1] == 0) ? text.length - 1 : text.length;
			return new String(text, 0, textLength, CLIENT_TEXT_CHARSET);
		}
		
		final long byteLengthValue = -(long) length * Character.BYTES;
		if (byteLengthValue > Integer.MAX_VALUE)
		{
			throw new IOException("Essence annotation string is too large: " + dataFile);
		}
		
		final int byteLength = (int) byteLengthValue;
		requireRemaining(buffer, byteLength, dataFile);
		final byte[] text = new byte[byteLength];
		buffer.get(text);
		final int textLength = (text.length >= 2) && (text[text.length - 2] == 0) && (text[text.length - 1] == 0) ? text.length - 2 : text.length;
		return new String(text, 0, textLength, StandardCharsets.UTF_16LE);
	}
	
	private static int readCompactIndex(ByteBuffer buffer, Path dataFile) throws IOException
	{
		requireRemaining(buffer, 1, dataFile);
		int current = Byte.toUnsignedInt(buffer.get());
		final boolean negative = (current & 0x80) != 0;
		int value = current & 0x3F;
		if ((current & 0x40) != 0)
		{
			for (int shift = 6; shift <= 27; shift += 7)
			{
				requireRemaining(buffer, 1, dataFile);
				current = Byte.toUnsignedInt(buffer.get());
				value |= (current & 0x7F) << shift;
				if ((current & 0x80) == 0)
				{
					return negative ? -value : value;
				}
			}
			
			throw new IOException("Essence annotation string length uses an invalid compact index: " + dataFile);
		}
		
		return negative ? -value : value;
	}
	
	private static void requireRemaining(ByteBuffer buffer, int required, Path dataFile) throws IOException
	{
		if ((required < 0) || (buffer.remaining() < required))
		{
			throw new IOException("Essence annotation data is truncated: " + dataFile);
		}
	}
	
	static final class AnnotationResult
	{
		private static final AnnotationResult EMPTY = new AnnotationResult(0, 0);
		private final int _minimapLabels;
		private final int _fallbackLabels;
		
		AnnotationResult(int minimapLabels, int fallbackLabels)
		{
			_minimapLabels = minimapLabels;
			_fallbackLabels = fallbackLabels;
		}
		
		int minimapLabels()
		{
			return _minimapLabels;
		}
		
		int fallbackLabels()
		{
			return _fallbackLabels;
		}
		
		int totalLabels()
		{
			return _minimapLabels + _fallbackLabels;
		}
	}
	
	private record HuntingLabel(String text, float worldX, float worldY, int huntingType, int minimumLevel, int maximumLevel, int mapMarker)
	{
	}
	
	private record MinimapRegionLayout(int regionId, int worldX, int worldY, int descriptionOffsetX, int descriptionOffsetY, int fontId)
	{
	}
	
	private record Label(String text, int x, int y, int huntingType, int fontSize, int descriptionOffsetX, int descriptionOffsetY, boolean minimapLayout, boolean fallback)
	{
	}
	
	private record PlacedLabel(Label label, int x, int baselineY)
	{
	}
}
