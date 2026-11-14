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

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.l2jmobius.tools.map.GeometryCollector.DoorInfo;
import org.l2jmobius.tools.map.GeometryCollector.WaterInfo;

/**
 * Server-XML writers for client-extracted reference data: doors and water zones.
 * @author Mobius
 */
public class XmlWriter
{
	/**
	 * Writes the map's openable doors as DoorData.xml entries (id, full name, oriented footprint nodes, location).<br>
	 * Combat stats are intentionally omitted: the client carries no hp or defence values, so this is reference data meant for comparing against or completing the server's DoorData.xml.
	 * @param doors the doors collected from the map package
	 * @param map the region name (XX_YY)
	 * @param out the XML file to write
	 * @throws IOException on write failure
	 */
	public static void writeDoorsXml(List<DoorInfo> doors, String map, Path out) throws IOException
	{
		try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8)))
		{
			w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			w.println("<list xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"xsd/DoorData.xsd\">");
			w.println("	<!-- Doors extracted from client map " + map + " (Mover actors with L2ServerObjectRealID). Reference data: merge stats manually. -->");
			for (DoorInfo door : doors)
			{
				w.println("	<door id=\"" + door.id + "\" name=\"" + door.name + "\">");
				w.println("		<nodes nodeZ=\"" + (int) Math.round(door.nodeZ) + "\">");
				for (int i = 0; i < 4; i++)
				{
					w.println("			<node x=\"" + (int) Math.round(door.nodeX[i]) + "\" y=\"" + (int) Math.round(door.nodeY[i]) + "\" />");
				}
				
				w.println("		</nodes>");
				w.println("		<location x=\"" + Math.round(door.pos[0]) + "\" y=\"" + Math.round(door.pos[1]) + "\" z=\"" + Math.round(door.pos[2]) + "\" />");
				w.println("	</door>");
			}
			
			w.println("</list>");
		}
		
		System.out.println("Wrote " + doors.size() + " doors to " + out);
	}
	
	/**
	 * Writes the map's WaterVolumes as WaterZone cuboid entries in the server's zones format.
	 * @param volumes the water volumes collected from the map package
	 * @param map the region name (XX_YY)
	 * @param out the XML file to write
	 * @throws IOException on write failure
	 */
	public static void writeWaterXml(List<WaterInfo> volumes, String map, Path out) throws IOException
	{
		try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8)))
		{
			w.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
			w.println("<list enabled=\"true\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"../xsd/zones.xsd\">");
			w.println("	<!-- Water zones extracted from client map " + map + " (WaterVolume brush bounds). -->");
			int index = 0;
			for (WaterInfo water : volumes)
			{
				index++;
				w.println("	<zone name=\"" + map + "_water" + index + "\" type=\"WaterZone\" shape=\"Cuboid\" minZ=\"" + (int) Math.round(water.box[4] + GeoConfig.GEODATA_Z_OFFSET) + "\" maxZ=\"" + (int) Math.round(water.box[5] + GeoConfig.GEODATA_Z_OFFSET) + "\">");
				w.println("		<node X=\"" + (int) Math.round(water.box[0]) + "\" Y=\"" + (int) Math.round(water.box[1]) + "\" />");
				w.println("		<node X=\"" + (int) Math.round(water.box[2]) + "\" Y=\"" + (int) Math.round(water.box[3]) + "\" />");
				w.println("	</zone>");
			}
			
			w.println("</list>");
		}
		
		System.out.println("Wrote " + volumes.size() + " water zones to " + out);
	}
}
