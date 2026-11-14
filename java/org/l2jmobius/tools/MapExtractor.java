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
package org.l2jmobius.tools;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.text.Document;

import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.LightTheme;
import org.l2jmobius.commons.ui.LineLimitListener;
import org.l2jmobius.commons.ui.WindowSize;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.tools.map.BspModel;
import org.l2jmobius.tools.map.GeoConfig;
import org.l2jmobius.tools.map.GeoLos;
import org.l2jmobius.tools.map.GeoVerifier;
import org.l2jmobius.tools.map.GeoWriter;
import org.l2jmobius.tools.map.GeometryCollector;
import org.l2jmobius.tools.map.GridBuilder;
import org.l2jmobius.tools.map.StaticMeshes;
import org.l2jmobius.tools.map.Terrain;
import org.l2jmobius.tools.map.XmlWriter;

/**
 * Client geodata extractor: converts XX_YY.unr map tiles of an client into L2J geodata region files (terrain + BSP + static-mesh sampling from org.l2jmobius.tools.map).<br>
 * The GUI generates all regions or a single one and answers height/surface queries at world coordinates; the CLI adds probe, conversion and hand-edit commands (see {@code main(String[])}).
 * @author Mobius
 */
public class MapExtractor extends JFrame
{
	private static final Pattern MAP_NAME = Pattern.compile("\\d+_\\d+(_Classic)?");
	private static final String CLASSIC_SUFFIX = "_Classic";
	
	private final Map<String, Terrain> _terrains = new HashMap<>();
	private final JTextField _clientField;
	private final JTextField _outputField;
	private final JTextField _regionField;
	private final JTextField _coordsField;
	private final JCheckBox _geodataBox;
	private final JCheckBox _doorsBox;
	private final JCheckBox _waterBox;
	private final JCheckBox _classicBox;
	private final JTextArea _logArea;
	
	private MapExtractor()
	{
		final String imagesPath = ".." + File.separator + "images";
		if (Files.isDirectory(Paths.get(imagesPath)))
		{
			final List<Image> icons = new ArrayList<>();
			icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_16x16.png").getImage());
			icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_32x32.png").getImage());
			icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_64x64.png").getImage());
			icons.add(new ImageIcon(imagesPath + File.separator + "l2jmobius_128x128.png").getImage());
			setIconImages(icons);
		}
		
		setTitle("Mobius - MapExtractor");
		setMinimumSize(new Dimension(700, 500));
		setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		setLayout(new BorderLayout());
		
		final JPanel inputPanel = new JPanel(new GridBagLayout());
		
		inputPanel.add(new JLabel("Client:"), constraints(0, 0, 1, 0));
		_clientField = new JTextField();
		inputPanel.add(_clientField, constraints(1, 0, 2, 1));
		final JButton clientBrowse = new JButton("Select Folder");
		inputPanel.add(clientBrowse, constraints(3, 0, 1, 0));
		
		inputPanel.add(new JLabel("Output:"), constraints(0, 1, 1, 0));
		_outputField = new JTextField();
		inputPanel.add(_outputField, constraints(1, 1, 2, 1));
		final JButton outputBrowse = new JButton("Select Folder");
		inputPanel.add(outputBrowse, constraints(3, 1, 1, 0));
		
		inputPanel.add(new JLabel("Region:"), constraints(0, 2, 1, 0));
		_regionField = new JTextField();
		_regionField.setToolTipText("Region name like 22_22; empty generates every region.");
		inputPanel.add(_regionField, constraints(1, 2, 2, 1));
		final JButton generateButton = new JButton("Generate");
		inputPanel.add(generateButton, constraints(3, 2, 1, 0));
		
		inputPanel.add(new JLabel("Export:"), constraints(0, 3, 1, 0));
		final JPanel modePanel = new JPanel();
		_geodataBox = new JCheckBox("Geodata", true);
		_doorsBox = new JCheckBox("Doors", false);
		_waterBox = new JCheckBox("Water zones", false);
		_classicBox = new JCheckBox("Classic maps", false);
		modePanel.add(_geodataBox);
		modePanel.add(_doorsBox);
		modePanel.add(_waterBox);
		modePanel.add(_classicBox);
		final GridBagConstraints modeConstraints = constraints(1, 3, 3, 0);
		modeConstraints.fill = GridBagConstraints.NONE;
		modeConstraints.anchor = GridBagConstraints.WEST;
		inputPanel.add(modePanel, modeConstraints);
		
		inputPanel.add(new JLabel("World X Y:"), constraints(0, 4, 1, 0));
		_coordsField = new JTextField();
		inputPanel.add(_coordsField, constraints(1, 4, 1, 1));
		final JButton heightButton = new JButton("Height Info");
		inputPanel.add(heightButton, constraints(2, 4, 1, 0));
		final JButton dumpButton = new JButton("Dump Map...");
		inputPanel.add(dumpButton, constraints(3, 4, 1, 0));
		add(inputPanel, BorderLayout.NORTH);
		
		_logArea = new JTextArea();
		_logArea.setEditable(false);
		_logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		final Document logDocument = _logArea.getDocument();
		logDocument.addDocumentListener(new LineLimitListener(1000));
		add(new JScrollPane(_logArea), BorderLayout.CENTER);
		
		// Send System.out / System.err to the log area, so builder progress prints appear in the GUI.
		final PrintStream logStream = new PrintStream(new OutputStream()
		{
			@Override
			public void write(int b)
			{
				write(new byte[]
				{
					(byte) b
				}, 0, 1);
			}
			
			@Override
			public void write(byte[] b, int off, int len)
			{
				final String text = new String(b, off, len, StandardCharsets.UTF_8);
				SwingUtilities.invokeLater(() ->
				{
					_logArea.append(text);
					_logArea.setCaretPosition(logDocument.getLength());
				});
			}
		}, true);
		System.setOut(logStream);
		System.setErr(logStream);
		
		clientBrowse.addActionListener(_ -> chooseFolder(_clientField, "Select the client folder (contains 'maps' and 'textures')"));
		outputBrowse.addActionListener(_ -> chooseFolder(_outputField, "Select the output folder (region files are written here)"));
		generateButton.addActionListener(_ ->
		{
			generateButton.setEnabled(false);
			new Thread(() ->
			{
				try
				{
					final Path client = Paths.get(_clientField.getText().trim().replace("\"", ""));
					final Path output = Paths.get(_outputField.getText().trim().replace("\"", ""));
					final String region = _regionField.getText().trim();
					final boolean geodata = _geodataBox.isSelected();
					final boolean doors = _doorsBox.isSelected();
					final boolean water = _waterBox.isSelected();
					final boolean classicSelected = _classicBox.isSelected();
					final boolean classic = classicSelected || region.endsWith(CLASSIC_SUFFIX);
					if (region.isEmpty())
					{
						generateAll(client, output, geodata, doors, water, classicSelected);
					}
					else if (MAP_NAME.matcher(region).matches())
					{
						generateOne(client, output, region.replace(CLASSIC_SUFFIX, ""), geodata, doors, water, classic);
					}
					else
					{
						System.out.println("Region must look like 22_22.");
					}
				}
				catch (Exception e)
				{
					System.out.println("Error: " + e.getMessage());
				}
				finally
				{
					SwingUtilities.invokeLater(() -> generateButton.setEnabled(true));
				}
			}).start();
		});
		heightButton.addActionListener(_ -> queryHeight());
		dumpButton.addActionListener(_ -> dumpMap());
		
		System.out.println("Select the client folder and an output folder, then press Generate.");
		System.out.println("Region files are written directly into the chosen output folder.");
		System.out.println("Height Info prints terrain, BSP and mesh surfaces at the given world point.");
		WindowSize.applyDefault(this);
		setLocationRelativeTo(null);
		setVisible(true);
	}
	
	/**
	 * Creates fresh grid constraints: shared mutable constraint objects leak state between rows.
	 * @param x the grid column
	 * @param y the grid row
	 * @param width the column span
	 * @param weight the horizontal weight (1 stretches, 0 keeps preferred size)
	 * @return the constraints
	 */
	private static GridBagConstraints constraints(int x, int y, int width, int weight)
	{
		final GridBagConstraints gbc = new GridBagConstraints();
		gbc.insets = new Insets(4, 4, 4, 4);
		gbc.fill = GridBagConstraints.HORIZONTAL;
		gbc.gridx = x;
		gbc.gridy = y;
		gbc.gridwidth = width;
		gbc.weightx = weight;
		return gbc;
	}
	
	private void chooseFolder(JTextField field, String title)
	{
		final String fieldText = field.getText();
		final JFileChooser chooser = new JFileChooser(fieldText.isBlank() ? "." : fieldText);
		chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		chooser.setDialogTitle(title);
		if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
		{
			field.setText(chooser.getSelectedFile().getAbsolutePath());
		}
	}
	
	/** Prints terrain height plus BSP and mesh surface columns at the world point in the coords field. */
	private void queryHeight()
	{
		final String coords = _coordsField.getText().trim();
		new Thread(() ->
		{
			try
			{
				final String[] p = coords.split("[\\s,]+");
				final double x = Double.parseDouble(p[0]);
				final double y = Double.parseDouble(p[1]);
				final String map = ((int) Math.floor(x / 32768) + 20) + "_" + ((int) Math.floor(y / 32768) + 18) + (_classicBox.isSelected() ? CLASSIC_SUFFIX : "");
				System.out.printf("Terrain height at (%.0f, %.0f) on map %s = %.1f%n", x, y, map, terrainFor(map).heightAt(x, y));
				final Path root = Paths.get(_clientField.getText().trim().replace("\"", ""));
				BspModel.bspSurfaces(root, map, x, y);
				StaticMeshes.meshSurfaces(root, map, x, y);
			}
			catch (NumberFormatException | ArrayIndexOutOfBoundsException e)
			{
				System.out.println("Enter two numbers, e.g. 82698 148638");
			}
			catch (Exception e)
			{
				System.out.println("Error: " + e.getMessage());
			}
		}).start();
	}
	
	/** Writes all heightmap points of the map under the coords field as "worldX worldY worldZ" lines. */
	private void dumpMap()
	{
		final String coords = _coordsField.getText().trim();
		final String map;
		try
		{
			final String[] p = coords.split("[\\s,]+");
			map = ((int) Math.floor(Double.parseDouble(p[0]) / 32768) + 20) + "_" + ((int) Math.floor(Double.parseDouble(p[1]) / 32768) + 18) + (_classicBox.isSelected() ? CLASSIC_SUFFIX : "");
		}
		catch (Exception e)
		{
			System.out.println("Enter world coordinates first, the map tile is taken from them.");
			return;
		}
		
		final JFileChooser chooser = new JFileChooser(".");
		chooser.setSelectedFile(new File(map + "_heights.txt"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		
		new Thread(() ->
		{
			try
			{
				GeoVerifier.dump(terrainFor(map), chooser.getSelectedFile().toPath());
			}
			catch (Exception e)
			{
				System.out.println("Error: " + e.getMessage());
			}
		}).start();
	}
	
	private Terrain terrainFor(String map) throws IOException
	{
		Terrain terrain = _terrains.get(map);
		if (terrain == null)
		{
			final Path root = Paths.get(_clientField.getText().trim().replace("\"", ""));
			if (!Files.isDirectory(root.resolve("maps")))
			{
				throw new IOException("No 'maps' folder under: " + root);
			}
			
			terrain = Terrain.load(root, map);
			_terrains.put(map, terrain);
		}
		
		return terrain;
	}
	
	/**
	 * Exports one region: geodata to outputRoot/XX_YY.l2j, doors to outputRoot/doors/XX_YY.xml and water zones to outputRoot/water/XX_YY.xml, following the selected modes.<br>
	 * Door and water files are only written when the map contains such actors.
	 * @param clientRoot the client folder
	 * @param outputRoot the export folder
	 * @param map the region name (XX_YY)
	 * @param geodata generate the .l2j region file
	 * @param doors export the door list
	 * @param water export the water zones
	 * @param classic true to read the XX_YY_Classic map variant
	 * @throws IOException on write failure
	 */
	private static void generateOne(Path clientRoot, Path outputRoot, String map, boolean geodata, boolean doors, boolean water, boolean classic) throws IOException
	{
		// Classic clients ship a second map set (XX_YY_Classic.unr) beside the main one.
		// The classic variant, when present, is read as the SOURCE while outputs keep the plain region name the server expects.
		final String source = classic && Files.exists(clientRoot.resolve("maps").resolve(map + CLASSIC_SUFFIX + ".unr")) ? map + CLASSIC_SUFFIX : map;
		Files.createDirectories(outputRoot);
		if (geodata)
		{
			GeoWriter.writeL2j(GridBuilder.buildGrid(clientRoot, source), outputRoot.resolve(map + ".l2j"));
		}
		
		if (doors)
		{
			final List<GeometryCollector.DoorInfo> doorList = GeometryCollector.extractDoors(clientRoot, source);
			doorList.removeIf(door -> door.id == 0); // Client-local auto-doors (no L2ServerObjectRealID) exist only to unbake their panel - the server has no object to manage for them.
			if (!doorList.isEmpty())
			{
				Files.createDirectories(outputRoot.resolve("doors"));
				XmlWriter.writeDoorsXml(doorList, map, outputRoot.resolve("doors").resolve(map + ".xml"));
			}
		}
		
		if (water)
		{
			final List<GeometryCollector.WaterInfo> waterList = GeometryCollector.extractWater(clientRoot, source);
			if (!waterList.isEmpty())
			{
				Files.createDirectories(outputRoot.resolve("water"));
				XmlWriter.writeWaterXml(waterList, map, outputRoot.resolve("water").resolve(map + ".xml"));
			}
		}
	}
	
	/**
	 * Exports every XX_YY map found under clientRoot/maps into outputRoot, following the selected modes.
	 * @param clientRoot the client folder
	 * @param outputRoot the export folder
	 * @param geodata generate the .l2j region files
	 * @param doors export the door lists
	 * @param water export the water zones
	 * @param classic read the XX_YY_Classic map set instead of the plain one
	 * @throws IOException on write failure
	 */
	private static void generateAll(Path clientRoot, Path outputRoot, boolean geodata, boolean doors, boolean water, boolean classic) throws IOException
	{
		final Path mapsDir = clientRoot.resolve("maps");
		if (!Files.isDirectory(mapsDir))
		{
			System.out.println("No 'maps' folder under: " + clientRoot);
			return;
		}
		
		final List<String> maps = new ArrayList<>();
		try (DirectoryStream<Path> files = Files.newDirectoryStream(mapsDir))
		{
			for (Path file : files)
			{
				final String fileName = file.getFileName().toString();
				final int fileNameLength = fileName.length();
				if (!fileName.regionMatches(true, fileNameLength - 4, ".unr", 0, 4))
				{
					continue;
				}
				
				final String mapName = fileName.substring(0, fileNameLength - 4);
				
				// Every region is processed; in classic mode generateOne prefers the XX_YY_Classic source when the client ships one.
				if (!MAP_NAME.matcher(mapName).matches())
				{
					continue;
				}
				
				if (!classic && mapName.endsWith(CLASSIC_SUFFIX))
				{
					continue;
				}
				
				final String region = mapName.replace(CLASSIC_SUFFIX, "");
				if (!maps.contains(region))
				{
					maps.add(region);
				}
			}
		}
		
		Collections.sort(maps);
		
		if (maps.isEmpty())
		{
			System.out.println("No XX_YY.unr map tiles found in " + mapsDir);
			return;
		}
		
		Files.createDirectories(outputRoot);
		System.out.println("Found " + maps.size() + " map tiles. Output: " + outputRoot);
		
		final long start = System.currentTimeMillis();
		int done = 0;
		int failed = 0;
		for (String map : maps)
		{
			System.out.println();
			System.out.println("=== [" + (done + failed + 1) + "/" + maps.size() + "] " + map + " ===");
			try
			{
				generateOne(clientRoot, outputRoot, map, geodata, doors, water, classic);
				done++;
			}
			catch (Throwable t)
			{
				failed++;
				System.out.println("FAILED " + map + ": " + t);
			}
		}
		
		System.out.println();
		System.out.printf("Done: %d generated, %d failed, %.1f min.%n", done, failed, (System.currentTimeMillis() - start) / 60000.0);
	}
	
	/**
	 * CLI reference:
	 *
	 * <pre>
	 * MapExtractor                                                     GUI (console when disabled)
	 * MapExtractor [clientRoot] [outputFolder] [--doors] [--water] [--nogeodata] [--classic]   export every region
	 * (--classic reads the XX_YY_Classic map set; region commands also accept XX_YY_Classic directly)
	 * MapExtractor [clientRoot] [XX_YY]                                terrain summary
	 * MapExtractor [clientRoot] [XX_YY] [worldX] [worldY]              height at world point
	 * MapExtractor [clientRoot] [XX_YY] --build out.l2j                generate one region
	 * MapExtractor [clientRoot] [XX_YY] --verify reference.l2j         build and compare vs a reference file
	 * MapExtractor [clientRoot] [XX_YY] --geocompare region.l2j        compare terrain vs a .l2j
	 * MapExtractor [clientRoot] [XX_YY] --dump out.txt                 dump heightmap points
	 * MapExtractor [clientRoot] [XX_YY] --mesh x y                     mesh surfaces at point
	 * MapExtractor [clientRoot] [XX_YY] --bsp x y                      BSP surfaces at point
	 * MapExtractor [clientRoot] [XX_YY] --fixgaps out.l2j x1 y1 x2 y2  build with gap closing in a world box
	 * MapExtractor [clientRoot] [XX_YY] --blockcells f.l2j cx1 cy1 cx2 cy2 [nswe]  set cell NSWE (0 = block)
	 * MapExtractor [clientRoot] [XX_YY] --addlayer f.l2j cx1 cy1 cx2 cy2 z nswe    insert a layer
	 * MapExtractor [clientRoot] [XX_YY] --los f.l2j x1 y1 z1 x2 y2 z2             trace line of sight between two world points
	 * </pre>
	 * 
	 * @param args see the reference above
	 * @throws Exception on any failure
	 */
	public static void main(String[] args) throws Exception
	{
		if (args.length == 0)
		{
			final ConfigReader interfaceConfig = new ConfigReader("." + File.separator + "config" + File.separator + "Interface.ini");
			if (interfaceConfig.getBoolean("EnableGUI", true) && !GraphicsEnvironment.isHeadless())
			{
				// Disable hardware acceleration.
				System.setProperty("sun.java2d.opengl", "false");
				System.setProperty("sun.java2d.d3d", "false");
				System.setProperty("sun.java2d.noddraw", "true");
				
				if (interfaceConfig.getBoolean("DarkTheme", true))
				{
					DarkTheme.activate();
				}
				else
				{
					LightTheme.activate();
				}
				
				SwingUtilities.invokeLater(MapExtractor::new);
			}
			else // Console.
			{
				try (Scanner in = new Scanner(System.in))
				{
					System.out.print("Client path (e.g. C:\\Games\\Lineage 2): ");
					final Path client = Paths.get(in.nextLine().trim().replace("\"", ""));
					System.out.print("Output folder (region files are written here): ");
					generateAll(client, Paths.get(in.nextLine().trim().replace("\"", "")), true, false, false, false);
				}
				catch (IOException e)
				{
					System.out.println("Error: " + e.getMessage());
				}
			}
			return;
		}
		
		if (args.length < 2)
		{
			System.out.println("Usage: java MapExtractor [<clientRoot> <outputFolder|XX_YY> [options]] - see the class javadoc for the full CLI reference.");
			return;
		}
		
		final Path root = Paths.get(args[0]);
		final String map = args[1];
		if (!MAP_NAME.matcher(map).matches())
		{
			// Second argument is not a region name: mass generation into that folder.
			// Optional flags select the export modes (default: geodata only).
			boolean geodata = true;
			boolean doors = false;
			boolean water = false;
			boolean classic = false;
			for (int i = 2; i < args.length; i++)
			{
				if (args[i].equals("--doors"))
				{
					doors = true;
				}
				else if (args[i].equals("--water"))
				{
					water = true;
				}
				else if (args[i].equals("--nogeodata"))
				{
					geodata = false;
				}
				else if (args[i].equals("--classic"))
				{
					classic = true;
				}
			}
			
			generateAll(root, Paths.get(args[1]), geodata, doors, water, classic);
			return;
		}
		
		// The builder handles terrain-less maps itself, so dispatch these before loading terrain.
		if ((args.length == 4) && args[2].equals("--build"))
		{
			final Path out = Paths.get(args[3]);
			GeoWriter.writeL2j(GridBuilder.buildGrid(root, map), out);
			return;
		}
		
		if ((args.length == 8) && args[2].equals("--fixgaps"))
		{
			// World-coordinate box; closes only crossings with no connecting surface (trench gaps).
			final String[] rxy = map.split("_");
			final int rox = (Integer.parseInt(rxy[0]) - 20) * 32768;
			final int roy = (Integer.parseInt(rxy[1]) - 18) * 32768;
			final int gx1 = (Integer.parseInt(args[4]) - rox) / 16;
			final int gy1 = (Integer.parseInt(args[5]) - roy) / 16;
			final int gx2 = (Integer.parseInt(args[6]) - rox) / 16;
			final int gy2 = (Integer.parseInt(args[7]) - roy) / 16;
			GeoConfig.GAPS_BOX = new int[]
			{
				Math.min(gx1, gx2),
				Math.min(gy1, gy2),
				Math.max(gx1, gx2),
				Math.max(gy1, gy2)
			};
			System.out.println("Gap-fix box cells [" + GeoConfig.GAPS_BOX[0] + "," + GeoConfig.GAPS_BOX[1] + "]..[" + GeoConfig.GAPS_BOX[2] + "," + GeoConfig.GAPS_BOX[3] + "]");
			GeoWriter.writeL2j(GridBuilder.buildGrid(root, map), Paths.get(args[3]));
			return;
		}
		
		if (((args.length == 8) || (args.length == 9)) && args[2].equals("--blockcells"))
		{
			GeoWriter.blockCells(Paths.get(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), args.length == 9 ? Integer.parseInt(args[8]) : 0);
			return;
		}
		
		if ((args.length == 10) && args[2].equals("--addlayer"))
		{
			GeoWriter.addLayer(Paths.get(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
			return;
		}
		
		if ((args.length == 10) && args[2].equals("--los"))
		{
			GeoLos.probe(Paths.get(args[3]), Integer.parseInt(args[4]), Integer.parseInt(args[5]), Integer.parseInt(args[6]), Integer.parseInt(args[7]), Integer.parseInt(args[8]), Integer.parseInt(args[9]));
			return;
		}
		
		if ((args.length == 4) && args[2].equals("--verify"))
		{
			GeoVerifier.verify(GridBuilder.buildGrid(root, map), Paths.get(args[3]));
			return;
		}
		
		final Terrain t = Terrain.load(root, map);
		System.out.printf("Terrain covers world X [%.0f .. %.0f], Y [%.0f .. %.0f]%n", t.texelToWorldX(0), t.texelToWorldX(t.uSize - 1), t.texelToWorldY(0), t.texelToWorldY(t.vSize - 1));
		
		if ((args.length == 5) && args[2].equals("--mesh"))
		{
			StaticMeshes.meshSurfaces(root, map, Double.parseDouble(args[3]), Double.parseDouble(args[4]));
		}
		else if ((args.length == 5) && args[2].equals("--bsp"))
		{
			BspModel.bspSurfaces(root, map, Double.parseDouble(args[3]), Double.parseDouble(args[4]));
		}
		else if ((args.length == 4) && args[2].equals("--geocompare"))
		{
			final String[] xy = map.split("_");
			GeoVerifier.geoCompare(t, Paths.get(args[3]), Integer.parseInt(xy[0]), Integer.parseInt(xy[1]));
		}
		else if ((args.length == 4) && args[2].equals("--dump"))
		{
			GeoVerifier.dump(t, Paths.get(args[3]));
		}
		else if (args.length == 4)
		{
			final double wx = Double.parseDouble(args[2]);
			final double wy = Double.parseDouble(args[3]);
			if (!t.contains(wx, wy))
			{
				System.out.printf("Outside map %s. That point is on map %d_%d.%n", map, (int) Math.floor(wx / 32768) + 20, (int) Math.floor(wy / 32768) + 18);
				return;
			}
			
			System.out.printf("Height at (%.0f, %.0f) = %.1f%n", wx, wy, t.heightAt(wx, wy));
		}
		else
		{
			System.out.printf("Center height: %.1f%n", t.worldZ(t.uSize / 2, t.vSize / 2));
		}
	}
}
