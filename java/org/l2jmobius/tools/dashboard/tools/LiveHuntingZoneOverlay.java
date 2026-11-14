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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.l2jmobius.tools.map.UnrealCrypt;

/**
 * Draws verified Live-client world-map labels from retail HuntingZone and MinimapRegion metadata.
 * <p>
 * Live 4.0 Grand Crusade moves the visible world-map labels out of the nine baked world-map textures. The HuntingZone map marker identifies the matching MinimapRegion {@code regionId}, while MinimapRegion provides the retail world position, description offsets and font reference. Only active map markers are rendered; no ZoneName fallback or runtime override is guessed.
 * </p>
 * <p>
 * MinimapRegion can contain conditional records that reuse a region ID. Their selector depends on runtime client state that is unavailable to the static preview, so the first record for each region ID is retained as the conservative base-map representation.
 * </p>
 * @author BazookaRpm
 */
final class LiveHuntingZoneOverlay
{
	private static final Charset CLIENT_TEXT_CHARSET = Charset.forName("windows-1252");
	private static final int MAX_RECORDS = 10000;
	private static final int MAX_MINIMAP_COLORS = 64;
	private static final int MAX_HUNTING_EXTRA_IDS = 64;
	private static final int WORLD_REGION_SIZE = 32768;
	private static final int WORLD_REGION_X_OFFSET = 20;
	private static final int WORLD_REGION_Y_OFFSET = 18;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int MAP_FONT_SIZE_9 = 9;
	private static final int MAP_FONT_SIZE_10 = 10;
	private static final int TOWN_TYPE = 6;
	private static final int CASTLE_TYPE = 8;
	private static final Font MAP_FONT_9 = new Font(Font.SANS_SERIF, Font.BOLD, MAP_FONT_SIZE_9);
	private static final Font MAP_FONT_10 = new Font(Font.SANS_SERIF, Font.BOLD, MAP_FONT_SIZE_10);
	private static final Color NORMAL_LABEL_COLOR = new Color(238, 238, 238);
	private static final Color CASTLE_LABEL_COLOR = new Color(235, 218, 103);
	private static final Color LABEL_OUTLINE_COLOR = new Color(18, 18, 18, 235);
	
	private LiveHuntingZoneOverlay()
	{
	}
	
	/**
	 * Draws the verified Live world-map annotation layer.
	 * @param systemDirectory the client system directory
	 * @param image the already-rendered Live world map
	 * @param minimumX the minimum map-region X coordinate represented by the image
	 * @param minimumY the minimum map-region Y coordinate represented by the image
	 * @param expectedMinimapRegionSha256 the exact encrypted MinimapRegion DAT SHA-256 for the profile
	 * @param expectedHuntingZoneSha256 the exact encrypted localized HuntingZone DAT SHA-256 for the profile
	 * @return annotation statistics, or {@code null} when the required metadata does not match the verified profile
	 * @throws IOException if verified client metadata is malformed or cannot be read
	 */
	static AnnotationResult draw(Path systemDirectory, BufferedImage image, int minimumX, int minimumY, String expectedMinimapRegionSha256, String expectedHuntingZoneSha256) throws IOException
	{
		final Path minimapRegionFile = findChildFile(systemDirectory, "MinimapRegion.dat");
		final Path huntingZoneFile = findLocalizedLiveData(systemDirectory, "HuntingZone");
		if ((minimapRegionFile == null) || (huntingZoneFile == null))
		{
			return null;
		}
		
		if (!sha256(minimapRegionFile).equals(expectedMinimapRegionSha256) || !sha256(huntingZoneFile).equals(expectedHuntingZoneSha256))
		{
			return null;
		}
		
		final List<HuntingLabel> huntingLabels = readHuntingLabels(huntingZoneFile);
		final Map<Integer, MinimapRegionLayout> baseLayouts = firstLayoutByRegionId(readMinimapRegionLayouts(minimapRegionFile));
		final Map<Integer, Integer> fontSizes = inferMapFontSizes(baseLayouts, huntingLabels);
		final List<Label> labels = new ArrayList<>();
		for (HuntingLabel huntingLabel : huntingLabels)
		{
			if (huntingLabel.mapMarker() == 0)
			{
				continue;
			}
			
			final MinimapRegionLayout layout = baseLayouts.get(huntingLabel.mapMarker());
			if (layout == null)
			{
				throw new IOException("Live HuntingZone marker " + huntingLabel.mapMarker() + " has no matching MinimapRegion record.");
			}
			
			if (huntingLabel.text().isBlank())
			{
				throw new IOException("Live HuntingZone marker " + huntingLabel.mapMarker() + " has no display text.");
			}
			
			final int x = worldToMapPixel(layout.worldX(), minimumX, WORLD_REGION_X_OFFSET);
			final int y = worldToMapPixel(layout.worldY(), minimumY, WORLD_REGION_Y_OFFSET);
			labels.add(new Label(huntingLabel.text(), x, y, huntingLabel.huntingType(), fontSizes.getOrDefault(layout.fontId(), MAP_FONT_SIZE_9), layout.descriptionOffsetX(), layout.descriptionOffsetY()));
		}
		
		final Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			for (Label label : labels)
			{
				final Font font = label.fontSize() == MAP_FONT_SIZE_10 ? MAP_FONT_10 : MAP_FONT_9;
				graphics.setFont(font);
				final FontMetrics metrics = graphics.getFontMetrics(font);
				final PlacedLabel placed = createPlacement(label, metrics, image.getWidth(), image.getHeight());
				drawOutlinedText(graphics, label.text(), placed.x(), placed.baselineY(), label.huntingType() == CASTLE_TYPE ? CASTLE_LABEL_COLOR : NORMAL_LABEL_COLOR);
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		return new AnnotationResult(labels.size());
	}
	
	/**
	 * Reads the active retail map-marker IDs from the verified localized HuntingZone DAT.
	 * @param systemDirectory the client system directory
	 * @param expectedHuntingZoneSha256 the exact encrypted localized HuntingZone DAT SHA-256 for the profile
	 * @return the active non-zero map-marker IDs, or {@code null} when the required metadata does not match the verified profile
	 * @throws IOException if the verified HuntingZone DAT is malformed or cannot be read
	 */
	static Set<Integer> readActiveMapMarkers(Path systemDirectory, String expectedHuntingZoneSha256) throws IOException
	{
		final Path huntingZoneFile = findLocalizedLiveData(systemDirectory, "HuntingZone");
		if ((huntingZoneFile == null) || !sha256(huntingZoneFile).equals(expectedHuntingZoneSha256))
		{
			return null;
		}
		
		final Set<Integer> result = new HashSet<>();
		for (HuntingLabel huntingLabel : readHuntingLabels(huntingZoneFile))
		{
			if (huntingLabel.mapMarker() != 0)
			{
				result.add(huntingLabel.mapMarker());
			}
		}
		
		return result;
	}
	
	private static List<MinimapRegionLayout> readMinimapRegionLayouts(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile);
		try
		{
			requireRemaining(buffer, Integer.BYTES, dataFile);
			final int count = buffer.getInt();
			if ((count < 0) || (count > MAX_RECORDS))
			{
				throw new IOException("Live MinimapRegion record count is invalid: " + count + ".");
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
					throw new IOException("Live MinimapRegion color count is invalid: " + colorCount + ".");
				}
				
				requireRemaining(buffer, colorCount * Integer.BYTES, dataFile);
				buffer.position(buffer.position() + (colorCount * Integer.BYTES));
				layouts.add(new MinimapRegionLayout(regionId, worldX, worldY, descriptionOffsetX, descriptionOffsetY, fontId));
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Live MinimapRegion data uses an unsupported record layout: " + dataFile);
			}
			
			return layouts;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Live MinimapRegion data is truncated or malformed: " + dataFile, e);
		}
	}
	
	private static List<HuntingLabel> readHuntingLabels(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile);
		try
		{
			requireRemaining(buffer, Integer.BYTES, dataFile);
			final int count = buffer.getInt();
			if ((count < 0) || (count > MAX_RECORDS))
			{
				throw new IOException("Live HuntingZone record count is invalid: " + count + ".");
			}
			
			final List<HuntingLabel> labels = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, (Integer.BYTES * 4) + (Float.BYTES * 3), dataFile);
				buffer.getInt(); // ID.
				final int huntingType = buffer.getInt();
				buffer.getInt(); // Minimum level.
				buffer.getInt(); // Maximum level.
				buffer.getFloat(); // World X. MinimapRegion is the presentation authority.
				buffer.getFloat(); // World Y.
				buffer.getFloat(); // World Z.
				readClientString(buffer, dataFile); // Description.
				requireRemaining(buffer, Integer.BYTES, dataFile);
				buffer.getInt(); // Localized name flag/identifier.
				final String text = readClientString(buffer, dataFile).trim();
				
				requireRemaining(buffer, 1 + 1 + Integer.BYTES + 1, dataFile);
				final int mapMarker = Byte.toUnsignedInt(buffer.get());
				buffer.get(); // Reserved byte.
				buffer.getInt(); // Runtime metadata.
				final int extraCount = Byte.toUnsignedInt(buffer.get());
				if (extraCount > MAX_HUNTING_EXTRA_IDS)
				{
					throw new IOException("Live HuntingZone extra ID count is invalid: " + extraCount + ".");
				}
				
				requireRemaining(buffer, (extraCount * Short.BYTES) + Integer.BYTES, dataFile);
				buffer.position(buffer.position() + (extraCount * Short.BYTES));
				buffer.getInt(); // Runtime metadata.
				labels.add(new HuntingLabel(text, huntingType, mapMarker));
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Live HuntingZone data uses an unsupported record layout: " + dataFile);
			}
			
			return labels;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Live HuntingZone data is truncated or malformed: " + dataFile, e);
		}
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
		for (Entry<Integer, Integer> entry : remaining)
		{
			result.put(entry.getKey(), MAP_FONT_SIZE_9);
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
	
	private static PlacedLabel createPlacement(Label label, FontMetrics metrics, int width, int height)
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
		
		return new PlacedLabel(x, baselineY);
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
	
	private static int worldToMapPixel(int worldCoordinate, int minimumRegion, int regionOffset)
	{
		final double regionPosition = ((double) worldCoordinate / WORLD_REGION_SIZE) + regionOffset - minimumRegion;
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
			throw new IOException("Live annotation string is too large: " + dataFile);
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
			
			throw new IOException("Live annotation string length uses an invalid compact index: " + dataFile);
		}
		
		return negative ? -value : value;
	}
	
	private static void requireRemaining(ByteBuffer buffer, int required, Path dataFile) throws IOException
	{
		if ((required < 0) || (buffer.remaining() < required))
		{
			throw new IOException("Live annotation data is truncated: " + dataFile);
		}
	}
	
	private static Path findLocalizedLiveData(Path systemDirectory, String prefix) throws IOException
	{
		if ((systemDirectory == null) || !Files.isDirectory(systemDirectory))
		{
			return null;
		}
		
		final String lowerPrefix = prefix.toLowerCase(Locale.ROOT) + "-";
		final List<Path> candidates = new ArrayList<>();
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(systemDirectory))
		{
			for (Path child : stream)
			{
				if (!Files.isRegularFile(child))
				{
					continue;
				}
				
				final String name = child.getFileName().toString().toLowerCase(Locale.ROOT);
				if (name.startsWith(lowerPrefix) && name.endsWith(".dat"))
				{
					candidates.add(child);
				}
			}
		}
		
		if (candidates.isEmpty())
		{
			return null;
		}
		
		final String languageCode = resolveLanguageCode(systemDirectory);
		if (languageCode != null)
		{
			final String expectedName = (prefix + "-" + languageCode + ".dat").toLowerCase(Locale.ROOT);
			for (Path candidate : candidates)
			{
				if (candidate.getFileName().toString().toLowerCase(Locale.ROOT).equals(expectedName))
				{
					return candidate;
				}
			}
		}
		
		Path first = candidates.get(0);
		for (int i = 1; i < candidates.size(); i++)
		{
			final Path candidate = candidates.get(i);
			if (candidate.getFileName().toString().compareToIgnoreCase(first.getFileName().toString()) < 0)
			{
				first = candidate;
			}
		}
		
		return first;
	}
	
	private static String resolveLanguageCode(Path systemDirectory) throws IOException
	{
		final Path localization = findChildFile(systemDirectory, "Localization.ini");
		if (localization == null)
		{
			return null;
		}
		
		final ByteBuffer decrypted = UnrealCrypt.decrypt(localization);
		final byte[] bytes = new byte[decrypted.remaining()];
		decrypted.get(bytes);
		final String[] lines = new String(bytes, StandardCharsets.ISO_8859_1).split("\\r?\\n");
		String section = "";
		String languageId = null;
		String languageName = null;
		for (String line : lines)
		{
			final String value = line.trim();
			if (value.startsWith("[") && value.endsWith("]"))
			{
				section = value.substring(1, value.length() - 1).trim();
				continue;
			}
			
			if (!section.equalsIgnoreCase("LanguageSet"))
			{
				continue;
			}
			
			final int separator = value.indexOf('=');
			if (separator <= 0)
			{
				continue;
			}
			
			final String key = value.substring(0, separator).trim();
			final String entry = value.substring(separator + 1).trim();
			if (key.equalsIgnoreCase("Language"))
			{
				languageId = entry;
			}
			else if ((languageId != null) && key.equalsIgnoreCase(languageId))
			{
				languageName = entry;
			}
		}
		
		if (languageName == null)
		{
			return null;
		}
		
		final String languageSection = "[" + languageName + "]";
		boolean inLanguageSection = false;
		for (String line : lines)
		{
			final String value = line.trim();
			if (value.startsWith("[") && value.endsWith("]"))
			{
				inLanguageSection = value.equalsIgnoreCase(languageSection);
				continue;
			}
			
			if (!inLanguageSection)
			{
				continue;
			}
			
			final int separator = value.indexOf('=');
			if (separator <= 0)
			{
				continue;
			}
			
			final String key = value.substring(0, separator).trim();
			if (key.equalsIgnoreCase(languageName + "_Code"))
			{
				return value.substring(separator + 1).trim();
			}
		}
		
		return null;
	}
	
	private static Path findChildFile(Path directory, String fileName) throws IOException
	{
		if ((directory == null) || !Files.isDirectory(directory))
		{
			return null;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory))
		{
			for (Path child : stream)
			{
				if (Files.isRegularFile(child) && child.getFileName().toString().equalsIgnoreCase(fileName))
				{
					return child;
				}
			}
		}
		
		return null;
	}
	
	private static String sha256(Path file) throws IOException
	{
		final MessageDigest digest;
		try
		{
			digest = MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 is unavailable.", e);
		}
		
		final byte[] buffer = new byte[8192];
		try (InputStream input = Files.newInputStream(file))
		{
			int length;
			while ((length = input.read(buffer)) != -1)
			{
				if (length > 0)
				{
					digest.update(buffer, 0, length);
				}
			}
		}
		
		return HexFormat.of().formatHex(digest.digest());
	}
	
	static final class AnnotationResult
	{
		private final int _minimapLabels;
		
		AnnotationResult(int minimapLabels)
		{
			_minimapLabels = minimapLabels;
		}
		
		int minimapLabels()
		{
			return _minimapLabels;
		}
	}
	
	private record HuntingLabel(String text, int huntingType, int mapMarker)
	{
	}
	
	private record MinimapRegionLayout(int regionId, int worldX, int worldY, int descriptionOffsetX, int descriptionOffsetY, int fontId)
	{
	}
	
	private record Label(String text, int x, int y, int huntingType, int fontSize, int descriptionOffsetX, int descriptionOffsetY)
	{
	}
	
	private record PlacedLabel(int x, int baselineY)
	{
	}
}
