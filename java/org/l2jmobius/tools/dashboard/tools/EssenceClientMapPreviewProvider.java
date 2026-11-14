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

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.tools.map.Export;
import org.l2jmobius.tools.map.UnrealCrypt;
import org.l2jmobius.tools.map.UnrealPackage;

/**
 * Builds verified Essence ClassicAden world maps from exact retail client profiles. <br>
 * A recognized profile must match its MinimapRegion, HuntingZone, L2GameDataName and L2WorldMap resources before annotations are rendered.
 * @author BazookaRpm
 */
public final class EssenceClientMapPreviewProvider
{
	private static final int WORLD_MAP_COLUMNS = 2;
	private static final int WORLD_MAP_ROWS = 3;
	private static final int WORLD_MAP_TILE_SIZE = 1024;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int MINIMUM_X = 15;
	private static final int MAXIMUM_X = 26;
	private static final int MINIMUM_Y = 10;
	private static final int MAXIMUM_Y = 25;
	private static final int ACTIVE_WIDTH = ((MAXIMUM_X - MINIMUM_X) + 1) * REGION_PIXEL_SIZE;
	private static final int ACTIVE_HEIGHT = ((MAXIMUM_Y - MINIMUM_Y) + 1) * REGION_PIXEL_SIZE;
	private static final int DXT1_FORMAT = 3;
	private static final int WORLD_MAP_PACKAGE_VERSION = 133;
	private static final int WORLD_MAP_PACKAGE_LICENSE = 40;
	private static final String MINIMAP_REGION_FILE = "MinimapRegion_ClassicAden.dat";
	private static final String HUNTING_ZONE_FILE = "HuntingZone_ClassicAden-eu.dat";
	private static final String ZONE_NAME_FILE = "ZoneName_ClassicAden-eu.dat";
	private static final String GAME_DATA_NAME_FILE = "L2GameDataName.dat";
	private static final String WORLD_MAP_FILE = "L2WorldMap.utx";
	private static final int[] CLASSIC_TILE_NUMBERS =
	{
		2,
		3,
		5,
		6,
		8,
		9
	};
	
	/**
	 * Loads an exact verified Essence World Map profile.
	 * @param clientRoot the Lineage II client root
	 * @return the rendered Essence client map, or {@code null} when the client is not one of the verified Essence profiles
	 * @throws IOException if a recognized Essence profile is incomplete, mismatched or malformed
	 */
	public MapPreviewLayer load(Path clientRoot) throws IOException
	{
		final Path systemDirectory = findChildDirectory(clientRoot, "system");
		if (systemDirectory == null)
		{
			return null;
		}
		
		final ProfileSelection selection = selectProfile(systemDirectory);
		if (selection == null)
		{
			return null;
		}
		
		final Profile profile = selection.profile();
		final Path dataDirectory = selection.dataDirectory();
		final Path minimapRegionFile = selection.minimapRegionFile();
		final Path huntingZoneFile = requireProfileFile(dataDirectory, HUNTING_ZONE_FILE, profile);
		final Path gameDataNameFile = requireProfileFile(dataDirectory, GAME_DATA_NAME_FILE, profile);
		validateHash(huntingZoneFile, profile.huntingZoneSha256(), profile, HUNTING_ZONE_FILE);
		validateHash(gameDataNameFile, profile.gameDataNameSha256(), profile, GAME_DATA_NAME_FILE);
		
		Path zoneNameFile = null;
		if (profile.usesZoneNameFallback())
		{
			zoneNameFile = requireProfileFile(dataDirectory, ZONE_NAME_FILE, profile);
			validateHash(zoneNameFile, profile.zoneNameSha256(), profile, ZONE_NAME_FILE);
		}
		
		final Path texturesDirectory = findChildDirectory(clientRoot, "systextures");
		if (texturesDirectory == null)
		{
			throw new IOException("Essence " + profile.version() + " requires the SysTextures directory.");
		}
		
		final Path worldMapPackage = requireProfileFile(texturesDirectory, WORLD_MAP_FILE, profile);
		validateHash(worldMapPackage, profile.worldMapPackageSha256(), profile, WORLD_MAP_FILE);
		
		final MapPreviewLayer layer = loadClassicTiles(worldMapPackage, profile);
		final int icons = EssenceWorldMapIconOverlay.draw(minimapRegionFile, huntingZoneFile, gameDataNameFile, worldMapPackage, profile, layer.image());
		final EssenceHuntingZoneOverlay.AnnotationResult annotations = EssenceHuntingZoneOverlay.draw(minimapRegionFile, huntingZoneFile, zoneNameFile, layer.image(), layer.minimumX(), layer.minimumY(), profile.strictActiveMarkers());
		validateAnnotationCounts(profile, icons, annotations);
		
		final String source = layer.source() + " + Essence world-map annotations (" + icons + " icons, " + annotations.minimapLabels() + " MinimapRegion labels, " + annotations.fallbackLabels() + " fallback labels)";
		return new MapPreviewLayer(layer.image(), layer.minimumX(), layer.maximumX(), layer.minimumY(), layer.maximumY(), source);
	}
	
	private static ProfileSelection selectProfile(Path systemDirectory) throws IOException
	{
		final Path euDirectory = findChildDirectory(systemDirectory, "eu");
		final Path euMinimap = findChildFile(euDirectory, MINIMAP_REGION_FILE);
		if (euMinimap != null)
		{
			final Profile profile = Profile.forMinimapSha256(sha256(euMinimap));
			if ((profile == null) || !profile.euDataRoot())
			{
				return null;
			}
			
			return new ProfileSelection(profile, euDirectory, euMinimap);
		}
		
		final Path flatMinimap = findChildFile(systemDirectory, MINIMAP_REGION_FILE);
		if (flatMinimap == null)
		{
			return null;
		}
		
		final Profile profile = Profile.forMinimapSha256(sha256(flatMinimap));
		if ((profile == null) || profile.euDataRoot())
		{
			return null;
		}
		
		return new ProfileSelection(profile, systemDirectory, flatMinimap);
	}
	
	private static Path requireProfileFile(Path directory, String fileName, Profile profile) throws IOException
	{
		final Path file = findChildFile(directory, fileName);
		if (file == null)
		{
			throw new IOException("Essence " + profile.version() + " requires " + fileName + " in " + directory + ".");
		}
		
		return file;
	}
	
	private static void validateHash(Path file, String expectedSha256, Profile profile, String resourceName) throws IOException
	{
		if (!sha256(file).equals(expectedSha256))
		{
			throw new IOException("Essence " + profile.version() + " " + resourceName + " does not match the verified client profile.");
		}
	}
	
	private static void validateAnnotationCounts(Profile profile, int icons, EssenceHuntingZoneOverlay.AnnotationResult annotations) throws IOException
	{
		if ((icons != profile.expectedIcons()) || (annotations.minimapLabels() != profile.expectedMinimapLabels()) || (annotations.fallbackLabels() != profile.expectedFallbackLabels()))
		{
			throw new IOException("Essence " + profile.version() + " annotation counts do not match the verified profile: " + icons + " icons, " + annotations.minimapLabels() + " MinimapRegion labels, " + annotations.fallbackLabels() + " fallback labels.");
		}
	}
	
	private static MapPreviewLayer loadClassicTiles(Path texturePackage, Profile profile) throws IOException
	{
		final UnrealPackage pkg = new UnrealPackage(UnrealCrypt.decrypt(texturePackage));
		final int packageVersion = Short.toUnsignedInt(pkg.buf.getShort(4));
		final int packageLicense = Short.toUnsignedInt(pkg.buf.getShort(6));
		if ((packageVersion != WORLD_MAP_PACKAGE_VERSION) || (packageLicense != WORLD_MAP_PACKAGE_LICENSE))
		{
			throw new IOException("Essence " + profile.version() + " L2WorldMap.utx uses an unexpected Unreal package version/license: " + packageVersion + "/" + packageLicense + ".");
		}
		
		final MessageDigest compositeDigest = newSha256Digest();
		final byte[][] dxt1Tiles = new byte[WORLD_MAP_COLUMNS * WORLD_MAP_ROWS][];
		for (int tileIndex = 0; tileIndex < dxt1Tiles.length; tileIndex++)
		{
			final String textureName = "WorldMap" + CLASSIC_TILE_NUMBERS[tileIndex] + "_Classic";
			final Export texture = findTexture(pkg, "Minimap", textureName);
			if (texture == null)
			{
				throw new IOException("Essence " + profile.version() + " L2WorldMap.utx is missing Minimap." + textureName + ".");
			}
			
			final Map<String, Object> properties = UnrealPackage.readProps(pkg, texture);
			final int width = intProperty(properties, "USize");
			final int height = intProperty(properties, "VSize");
			final int format = intProperty(properties, "Format");
			if ((width != WORLD_MAP_TILE_SIZE) || (height != WORLD_MAP_TILE_SIZE) || (format != DXT1_FORMAT))
			{
				throw new IOException("Essence " + profile.version() + " " + textureName + " has an unsupported size or format.");
			}
			
			final byte[] dxt1 = readTopMip(pkg, texture, (width * height) / 2);
			dxt1Tiles[tileIndex] = dxt1;
			compositeDigest.update(dxt1);
		}
		
		final String compositeSha256 = HexFormat.of().formatHex(compositeDigest.digest());
		if (!compositeSha256.equals(profile.worldMapCompositeSha256()))
		{
			throw new IOException("Essence " + profile.version() + " WorldMap*_Classic payloads do not match the verified client profile.");
		}
		
		final BufferedImage image = new BufferedImage(ACTIVE_WIDTH, ACTIVE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			for (int tileIndex = 0; tileIndex < dxt1Tiles.length; tileIndex++)
			{
				final BufferedImage tile = decodeDxt1(dxt1Tiles[tileIndex], WORLD_MAP_TILE_SIZE, WORLD_MAP_TILE_SIZE);
				final int column = tileIndex % WORLD_MAP_COLUMNS;
				final int row = tileIndex / WORLD_MAP_COLUMNS;
				graphics.drawImage(tile, column * WORLD_MAP_TILE_SIZE, row * WORLD_MAP_TILE_SIZE, null);
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		return new MapPreviewLayer(image, MINIMUM_X, MAXIMUM_X, MINIMUM_Y, MAXIMUM_Y, "Essence " + profile.version() + " Client Map: L2WorldMap.Minimap.WorldMap [WorldMap*_Classic]");
	}
	
	private static Export findTexture(UnrealPackage pkg, String groupName, String textureName)
	{
		for (Export export : pkg.exports)
		{
			if (!pkg.objName(export.classRef).equalsIgnoreCase("Texture") || !pkg.names[export.nameIdx].equalsIgnoreCase(textureName) || (export.outerRef <= 0))
			{
				continue;
			}
			
			final Export outer = pkg.exports[export.outerRef - 1];
			if (pkg.names[outer.nameIdx].equalsIgnoreCase(groupName))
			{
				return export;
			}
		}
		
		return null;
	}
	
	private static int intProperty(Map<String, Object> properties, String name)
	{
		final Object value = properties.get(name);
		return value instanceof Number ? ((Number) value).intValue() : -1;
	}
	
	private static byte[] readTopMip(UnrealPackage pkg, Export texture, int expectedBytes) throws IOException
	{
		final int start = pkg.buf.position();
		final int end = texture.offset + texture.size;
		final ByteBuffer buffer = pkg.buf.duplicate().order(ByteOrder.LITTLE_ENDIAN);
		for (int skip = 0; (start + skip + 6) < end; skip++)
		{
			buffer.position(start + skip);
			final int mipCount = Byte.toUnsignedInt(buffer.get());
			if ((mipCount < 1) || (mipCount > 16))
			{
				continue;
			}
			
			final int positionAfter = buffer.getInt();
			final int dataSize = UnrealPackage.readIndex(buffer);
			final int dataStart = buffer.position();
			if ((dataSize != expectedBytes) || ((dataStart + dataSize) > end) || (positionAfter != (dataStart + dataSize)))
			{
				continue;
			}
			
			final byte[] data = new byte[dataSize];
			buffer.get(data);
			return data;
		}
		
		throw new IOException("Could not locate DXT1 mip data for " + pkg.names[texture.nameIdx] + ".");
	}
	
	private static BufferedImage decodeDxt1(byte[] data, int width, int height) throws IOException
	{
		final int blocksWide = (width + 3) / 4;
		final int blocksHigh = (height + 3) / 4;
		final int expectedBytes = blocksWide * blocksHigh * 8;
		if (data.length != expectedBytes)
		{
			throw new IOException("Unexpected DXT1 payload size " + data.length + " for " + width + "x" + height + ".");
		}
		
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int offset = 0;
		for (int blockY = 0; blockY < blocksHigh; blockY++)
		{
			for (int blockX = 0; blockX < blocksWide; blockX++)
			{
				final int color0 = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
				final int color1 = (data[offset + 2] & 0xFF) | ((data[offset + 3] & 0xFF) << 8);
				final int[] colors = buildDxt1Palette(color0, color1);
				final int indices = (data[offset + 4] & 0xFF) | ((data[offset + 5] & 0xFF) << 8) | ((data[offset + 6] & 0xFF) << 16) | ((data[offset + 7] & 0xFF) << 24);
				offset += 8;
				for (int pixelY = 0; pixelY < 4; pixelY++)
				{
					final int y = (blockY * 4) + pixelY;
					if (y >= height)
					{
						continue;
					}
					
					for (int pixelX = 0; pixelX < 4; pixelX++)
					{
						final int x = (blockX * 4) + pixelX;
						if (x >= width)
						{
							continue;
						}
						
						final int paletteIndex = (indices >>> (2 * ((pixelY * 4) + pixelX))) & 0x03;
						image.setRGB(x, y, colors[paletteIndex]);
					}
				}
			}
		}
		
		return image;
	}
	
	private static int[] buildDxt1Palette(int color0, int color1)
	{
		final int[] first = decodeRgb565(color0);
		final int[] second = decodeRgb565(color1);
		final int[] palette = new int[4];
		palette[0] = argb(255, first[0], first[1], first[2]);
		palette[1] = argb(255, second[0], second[1], second[2]);
		if (color0 > color1)
		{
			palette[2] = argb(255, ((2 * first[0]) + second[0]) / 3, ((2 * first[1]) + second[1]) / 3, ((2 * first[2]) + second[2]) / 3);
			palette[3] = argb(255, (first[0] + (2 * second[0])) / 3, (first[1] + (2 * second[1])) / 3, (first[2] + (2 * second[2])) / 3);
		}
		else
		{
			palette[2] = argb(255, (first[0] + second[0]) / 2, (first[1] + second[1]) / 2, (first[2] + second[2]) / 2);
			palette[3] = 0;
		}
		
		return palette;
	}
	
	private static int[] decodeRgb565(int color)
	{
		final int red5 = (color >>> 11) & 0x1F;
		final int green6 = (color >>> 5) & 0x3F;
		final int blue5 = color & 0x1F;
		return new int[]
		{
			(red5 << 3) | (red5 >>> 2),
			(green6 << 2) | (green6 >>> 4),
			(blue5 << 3) | (blue5 >>> 2)
		};
	}
	
	private static int argb(int alpha, int red, int green, int blue)
	{
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}
	
	private static Path findChildDirectory(Path parent, String name) throws IOException
	{
		if ((parent == null) || !Files.isDirectory(parent))
		{
			return null;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent))
		{
			for (Path child : stream)
			{
				if (Files.isDirectory(child) && child.getFileName().toString().equalsIgnoreCase(name))
				{
					return child;
				}
			}
		}
		
		return null;
	}
	
	private static Path findChildFile(Path parent, String name) throws IOException
	{
		if ((parent == null) || !Files.isDirectory(parent))
		{
			return null;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent))
		{
			for (Path child : stream)
			{
				if (Files.isRegularFile(child) && child.getFileName().toString().equalsIgnoreCase(name))
				{
					return child;
				}
			}
		}
		
		return null;
	}
	
	private static MessageDigest newSha256Digest()
	{
		try
		{
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 is unavailable.", e);
		}
	}
	
	private static String sha256(Path file) throws IOException
	{
		return HexFormat.of().formatHex(newSha256Digest().digest(Files.readAllBytes(file)));
	}
	
	static enum Profile
	{
		ESSENCE_4_2("4.2", false, true, "b8db9acf3a72a04406f5d7e304b06f755b4e27163f33e827c0722b336d44dc89", "5893b964b7e6657aee5d7d084f1d084f2d0f10b057c9aca297d4884c2567778a", "e710798d00ad334311e1d9b55f9f2f1dc4a74686fb4043cac993e00bc8b710cb", "f04af484959bce0e86f093892a3ddcb0eef274f7e3d2d5d602eb1ff400da2521", "e21c203210a6509352286c73af42a32930d885b9b34be0572d7ca3363684e8c2", "233ab41dbc7b5c6773cbb5d8a3ef036fdbb102449b033af4d1c483ce12a2c518", 28, 90, 0, Set.of("18_24_camp", "19_22_0", "18_23_0", "19_18_0", "19_21_castle", "20_17_0", "20_20_0", "20_21_0", "21_18_0", "21_24_0", "22_17_0", "22_18_0", "22_25_0", "23_16_1", "23_18_0", "23_23_0", "24_12_0", "24_14_1", "24_15_0", "24_18_0", "24_21_0", "25_12_0", "25_14_0", "25_15_0", "25_16_0", "25_17_0", "25_19_0", "25_21_0", "25_23_C0", "16_25_C0", "18_19_C0", "18_20_C0", "20_14_C0", "20_23_C0")),
		ESSENCE_5_2("5.2", false, true, "7973e95b5632dab9245acc4e0cb88d2db9441a7a36c98c48def3031987543a87", "e773a54c110bcec49c82f5058fbf365afa03e0d9ae804fa9dde6afd776d30abb", "51ffb6680984b47290c5c5b314ab734dd484d2a8e7d1c40c3bcc69f611f0ab12", "", "3513e5c67769ce30033276072a6fe98b056b1721c11d7e7fb5f54a5c99bdfd43", "dbefc01c7a25d803d6e316a78f2465f7811d9d74a9fc58a9fe54c381dbb75146", 24, 84, 0, Set.of("18_24_camp", "19_22_0", "18_23_0", "19_18_0", "19_21_castle", "20_17_0", "20_20_0", "20_21_0", "21_18_0", "21_24_0", "22_13_castle", "22_17_0", "22_18_0", "22_25_0", "23_16_1", "23_18_0", "23_23_0", "24_12_0", "24_14_1", "24_15_0", "24_18_0", "24_21_0", "25_12_0", "25_14_0", "25_15_0", "25_16_0", "25_17_0", "25_19_0", "25_21_0", "25_23_C0", "16_25_C0", "18_19_C0", "18_20_C0", "20_14_C0", "20_23_C0")),
		ESSENCE_6_3("6.3", true, true, "5681d2f67b07503f63145ec0c8ad93ad0f766bfae0d682d1b2a2c9b6e75b5ae2", "65ec13fb21b4af171e540791696c27f4e7af4612fc31eb4c0be22509f6b0a432", "62838402e159ec30ecff04ddd7f3e7e987005a8b0f4667aa7f6cad56aae87c43", "", "effdabb608ad3bc5ac9ebf974d4611f4e8dc29502f2bce4fe1daf9f27e53c1c7", "dbefc01c7a25d803d6e316a78f2465f7811d9d74a9fc58a9fe54c381dbb75146", 28, 88, 0, Set.of("18_24_camp", "19_22_0", "18_23_0", "19_18_0", "19_21_castle", "20_17_0", "20_20_0", "20_21_0", "21_18_0", "21_24_0", "22_13_castle", "22_17_0", "22_18_0", "22_25_0", "23_16_1", "23_18_0", "23_23_0", "24_12_0", "24_14_1", "24_15_0", "24_18_0", "24_21_0", "25_12_0", "25_14_0", "25_15_0", "25_16_0", "25_17_0", "25_19_0", "25_21_0", "25_23_C0", "16_25_C0", "18_19_C0", "18_20_C0", "20_14_C0", "20_23_C0")),
		ESSENCE_7_3("7.3", true, true, "7072849ef07693cd47d440c871a76d22bfac11ca4c3b4e138e808db7f4f44bd8", "dfd0bbbf32056078b427b5249d02b3b04fca61c6d51703d6285114100d112388", "a0ebb3aa88fb78eaa6830c35759200f70349d81cb7c54559981faa3426846a8a", "", "849553021a76dd31dd6ffbd7d98193eed77cddf8ddbaeed58067057de3b42be4", "777bdc034b8c42b8749fb3ef8f4e3709f1dc0d7cdaf292191b5fbbbbb5dff746", 30, 79, 0, Set.of()),
		ESSENCE_8_3("8.3", true, true, "657db6895d41800a118ee12000345c4600c159c3ab38b8ac37d9eef5ee714eb6", "9f0a99b194a499328fec1a90f86cedef5fadd194553fda5aecf206fbf1a8e996", "5ecfdb9616d1fe56c6a7c824497b629b903ee15bd5c0ecfe73574927c470b812", "", "87302334102e8cd7a384c90022666cf2e40d6988c5d0899abab09a5f29493ed1", "fe3b3515efcc9d62c6249a825437769f4d2b89fff90699f009446dabcd8d12ff", 29, 77, 0, Set.of()),
		ESSENCE_9_1("9.1", true, true, "2fafea6ffe27eba12c8b6c3b9fd4bb744b89d11e57fcf0c03baaa515092542ec", "63b26693c955e8f3113619a412cddc223d9c7447b7aed7d04fb49b7064148a98", "92a538ee1b0f8badab19073d1226bcc5627d1800e0523ee2e7fc28bcebd8e9cd", "", "9c5f5f6c5b927d993d45e4e842d14185c8dee029cf1bd991e9700011ff7ca488", "322b5953d2a2fa6a3376b9aa2c10a3dc7c3d6d25c9bbd114a198f3178e0f1f9f", 29, 80, 0, Set.of()),
		ESSENCE_9_2("9.2", true, true, "f4b33fb4cef297e795908bf406c48dd9515d55038ca0a9562a9a2699a47f5c2e", "47b6b97b8f9bbb3d42e6178015464036b2081c600d8a8bc65a17e243894ed82a", "957cad3e2a47c7db372dfd10a4a5e1f6e7be5364676da578da8f991fb055ea15", "", "9c5f5f6c5b927d993d45e4e842d14185c8dee029cf1bd991e9700011ff7ca488", "322b5953d2a2fa6a3376b9aa2c10a3dc7c3d6d25c9bbd114a198f3178e0f1f9f", 29, 81, 0, Set.of()),
		ESSENCE_10_1("10.1", true, true, "02de81319444da92bade3a3c7856c95a597b11b4f198840c946dd6f9e4235f78", "1e3686109a022cc33e340686fd1d94ad31e9d446f1b474859724e9db524e2287", "58abbbb0bd2b9aa0be1c0594ebef1ca29c92bba21ededfaabfbe4fb9ddc68e5f", "", "9fc1e776d11e01fd39fd7f907ddfae3457822cb3dacc99f9b87eb2fe74f5006b", "6e4aff9b9f9678f2e53ae728228cd064d1171ebb6c88a02d3580f7ba0760ef34", 30, 85, 0, Set.of()),
		ESSENCE_10_2("10.2", true, true, "a20403055868b98829d876765ad990b188ccbe8d604065ccbca905558e6a3bc4", "f1c05ca990b86dfabf41d1b6eecd27384677dc43d8b8fadd76b2d59b30d90743", "83518649d6f19db0e2562486689f234cf3b178bda2fce4694bacb6f046cc0cce", "", "9fc1e776d11e01fd39fd7f907ddfae3457822cb3dacc99f9b87eb2fe74f5006b", "6e4aff9b9f9678f2e53ae728228cd064d1171ebb6c88a02d3580f7ba0760ef34", 30, 85, 0, Set.of()),
		ESSENCE_10_3("10.3", true, true, "6ca511c7e1fb285c40d45138725dabec105391f81cbf0d5626fe0faf23e25a13", "1ddc11a086fc6098e70973c02db465c1ee705c1956445b2e08294669671b9d72", "e0862db8799b46c5adf20a5776fdf63bfd7dbe15749407478c0ee2e6ff8fb782", "", "4cd535ad142a491feddfbcbd6f4e6213a23e40eeab7e1e5918de9b32b4a1104e", "6e4aff9b9f9678f2e53ae728228cd064d1171ebb6c88a02d3580f7ba0760ef34", 29, 84, 0, Set.of());
		
		private final String _version;
		private final boolean _euDataRoot;
		private final boolean _strictActiveMarkers;
		private final String _minimapSha256;
		private final String _huntingZoneSha256;
		private final String _gameDataNameSha256;
		private final String _zoneNameSha256;
		private final String _worldMapPackageSha256;
		private final String _worldMapCompositeSha256;
		private final int _expectedIcons;
		private final int _expectedMinimapLabels;
		private final int _expectedFallbackLabels;
		private final Set<String> _staticIconNames;
		
		Profile(String version, boolean euDataRoot, boolean strictActiveMarkers, String minimapSha256, String huntingZoneSha256, String gameDataNameSha256, String zoneNameSha256, String worldMapPackageSha256, String worldMapCompositeSha256, int expectedIcons, int expectedMinimapLabels, int expectedFallbackLabels, Set<String> staticIconNames)
		{
			_version = version;
			_euDataRoot = euDataRoot;
			_strictActiveMarkers = strictActiveMarkers;
			_minimapSha256 = minimapSha256;
			_huntingZoneSha256 = huntingZoneSha256;
			_gameDataNameSha256 = gameDataNameSha256;
			_zoneNameSha256 = zoneNameSha256;
			_worldMapPackageSha256 = worldMapPackageSha256;
			_worldMapCompositeSha256 = worldMapCompositeSha256;
			_expectedIcons = expectedIcons;
			_expectedMinimapLabels = expectedMinimapLabels;
			_expectedFallbackLabels = expectedFallbackLabels;
			_staticIconNames = staticIconNames;
		}
		
		String version()
		{
			return _version;
		}
		
		boolean euDataRoot()
		{
			return _euDataRoot;
		}
		
		boolean strictActiveMarkers()
		{
			return _strictActiveMarkers;
		}
		
		boolean usesZoneNameFallback()
		{
			return !_strictActiveMarkers;
		}
		
		String huntingZoneSha256()
		{
			return _huntingZoneSha256;
		}
		
		String gameDataNameSha256()
		{
			return _gameDataNameSha256;
		}
		
		String zoneNameSha256()
		{
			return _zoneNameSha256;
		}
		
		String worldMapPackageSha256()
		{
			return _worldMapPackageSha256;
		}
		
		String worldMapCompositeSha256()
		{
			return _worldMapCompositeSha256;
		}
		
		int expectedIcons()
		{
			return _expectedIcons;
		}
		
		int expectedMinimapLabels()
		{
			return _expectedMinimapLabels;
		}
		
		int expectedFallbackLabels()
		{
			return _expectedFallbackLabels;
		}
		
		Set<String> staticIconNames()
		{
			return _staticIconNames;
		}
		
		static Profile forMinimapSha256(String sha256)
		{
			for (Profile profile : values())
			{
				if (profile._minimapSha256.equals(sha256))
				{
					return profile;
				}
			}
			
			return null;
		}
	}
	
	private record ProfileSelection(Profile profile, Path dataDirectory, Path minimapRegionFile)
	{
	}
}
