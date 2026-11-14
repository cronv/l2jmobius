/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to draw points and lines on the client side.<br/>
 * Note that names in these primitives are bugged and may appear even when not looking at them.
 * @author NosBit
 */
public class ExServerPrimitive extends ServerPacket
{
	private final String _name;
	private final int _x;
	private final int _y;
	private final int _z;
	private final List<Point> _points = new ArrayList<>();
	private final List<Line> _lines = new ArrayList<>();
	
	/**
	 * Creates a new {@code ExServerPrimitive} instance.<br/>
	 * This constructor initializes the primitive with a unique name and coordinates.
	 * @param name A unique name used to identify or replace lines for this primitive.
	 * @param x The x coordinate, typically representing the middle of the drawing area.
	 * @param y The y coordinate, typically representing the middle of the drawing area.
	 * @param z The z coordinate, typically representing the middle of the drawing area.
	 */
	public ExServerPrimitive(String name, int x, int y, int z)
	{
		_name = name;
		_x = x;
		_y = y;
		_z = z;
	}
	
	/**
	 * Creates a new {@code ExServerPrimitive} using coordinates from an {@code ILocational}.<br/>
	 * This is a convenience constructor for creating primitives at a specific location.<br/>
	 * @param name The unique name used to identify or replace these primitives.
	 * @param locational The location object providing the x, y, and z coordinates.
	 */
	public ExServerPrimitive(String name, ILocational locational)
	{
		this(name, locational.getX(), locational.getY(), locational.getZ());
	}
	
	/**
	 * Adds a new point to the {@code ExServerPrimitive} object.<br/>
	 * This method stores the coordinates and visual properties for rendering.
	 * @param name The unique identifier for this point.<br/>
	 * @param color The integer color value of the point.<br/>
	 * @param isNameColored A boolean indicating if the name should be colored.<br/>
	 * @param x The x coordinate of the point.<br/>
	 * @param y The y coordinate of the point.<br/>
	 * @param z The z coordinate of the point.
	 */
	public void addPoint(String name, int color, boolean isNameColored, int x, int y, int z)
	{
		_points.add(new Point(name, color, isNameColored, x, y, z));
	}
	
	/**
	 * Adds a point to the primitive collection.<br/>
	 * This method uses the coordinates from an {@code ILocational} object.<br/>
	 * @param name The unique name for this point.<br/>
	 * @param color The integer color value of the point.<br/>
	 * @param isNameColored Whether the name should be colored.<br/>
	 * @param locational The location containing the x, y, and z coordinates.
	 */
	public void addPoint(String name, int color, boolean isNameColored, ILocational locational)
	{
		addPoint(name, color, isNameColored, locational.getX(), locational.getY(), locational.getZ());
	}
	
	/**
	 * Adds a new point to the primitive collection.<br/>
	 * This method uses an empty string for the name.<br/>
	 * It sets the name color property to {@code false}.
	 * @param color The integer color value of the point.
	 * @param x The x coordinate of the point.
	 * @param y The y coordinate of the point.
	 * @param z The z coordinate of the point.
	 */
	public void addPoint(int color, int x, int y, int z)
	{
		addPoint("", color, false, x, y, z);
	}
	
	/**
	 * Adds a point to the primitive collection.<br/>
	 * This method uses an empty name and sets {@code isNameColored} to {@code false}.<br/>
	 * @param color The integer color value for the point.
	 * @param locational The {@code ILocational} object defining the position of the point.
	 */
	public void addPoint(int color, ILocational locational)
	{
		addPoint("", color, false, locational);
	}
	
	/**
	 * Adds a new point to the primitive collection.<br/>
	 * This method uses a {@code Color} object for the visual appearance.<br/>
	 * @param name The unique identifier for this point.<br/>
	 * @param color The {@code Color} used to render the point.<br/>
	 * @param isNameColored A boolean indicating if the name should be colored.<br/>
	 * @param x The x coordinate of the point.<br/>
	 * @param y The y coordinate of the point.<br/>
	 * @param z The z coordinate of the point.
	 */
	public void addPoint(String name, Color color, boolean isNameColored, int x, int y, int z)
	{
		addPoint(name, color.getRGB(), isNameColored, x, y, z);
	}
	
	/**
	 * Adds a new point to the primitive collection.<br/>
	 * This method uses an {@code ILocational} object to determine coordinates.<br/>
	 * @param name The unique identifier for this point.<br/>
	 * @param color The {@code Color} of the point.<br/>
	 * @param isNameColored A boolean indicating if the name should be colored.<br/>
	 * @param locational The location data for the point.
	 */
	public void addPoint(String name, Color color, boolean isNameColored, ILocational locational)
	{
		addPoint(name, color.getRGB(), isNameColored, locational);
	}
	
	/**
	 * Adds a new point to the primitive collection.<br/>
	 * This method uses an empty string for the name and sets {@code isNameColored} to {@code false}.<br/>
	 * @param color The {@code Color} of the point.<br/>
	 * @param x The x coordinate of the point.<br/>
	 * @param y The y coordinate of the point.<br/>
	 * @param z The z coordinate of the point.
	 */
	public void addPoint(Color color, int x, int y, int z)
	{
		addPoint("", color, false, x, y, z);
	}
	
	/**
	 * Adds a point to the primitive collection.<br/>
	 * This method uses an empty name and sets {@code isNameColored} to {@code false}.<br/>
	 * @param color The {@code Color} of the point.<br/>
	 * @param locational The {@code ILocational} position of the point.
	 */
	public void addPoint(Color color, ILocational locational)
	{
		addPoint("", color, false, locational);
	}
	
	/**
	 * Adds a new line to the {@code ExServerPrimitive} object.<br/>
	 * This method defines a line between two sets of 3D coordinates.<br/>
	 * @param name The unique identifier for this line.<br/>
	 * @param color The color value for the line.<br/>
	 * @param isNameColored Whether the name should be colored.<br/>
	 * @param x The x coordinate of the starting point.<br/>
	 * @param y The y coordinate of the starting point.<br/>
	 * @param z The z coordinate of the starting point.<br/>
	 * @param x2 The x coordinate of the ending point.<br/>
	 * @param y2 The y coordinate of the ending point.<br/>
	 * @param z2 The z coordinate of the ending point.
	 */
	public void addLine(String name, int color, boolean isNameColored, int x, int y, int z, int x2, int y2, int z2)
	{
		_lines.add(new Line(name, color, isNameColored, x, y, z, x2, y2, z2));
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method uses the coordinates from an {@code ILocational} object for the starting point.<br/>
	 * @param name The unique identifier for this line.<br/>
	 * @param color The integer color value of the line.<br/>
	 * @param isNameColored Whether the name associated with the line should be colored.<br/>
	 * @param locational The location providing the starting coordinates.<br/>
	 * @param x2 The x coordinate of the end point.<br/>
	 * @param y2 The y coordinate of the end point.<br/>
	 * @param z2 The z coordinate of the end point.
	 */
	public void addLine(String name, int color, boolean isNameColored, ILocational locational, int x2, int y2, int z2)
	{
		addLine(name, color, isNameColored, locational.getX(), locational.getY(), locational.getZ(), x2, y2, z2);
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method uses coordinates from an {@code ILocational} object for the second point.<br/>
	 * @param name The unique identifier for this line.<br/>
	 * @param color The color value of the line.<br/>
	 * @param isNameColored Whether the name should be colored.<br/>
	 * @param x The x coordinate of the first point.<br/>
	 * @param y The y coordinate of the first point.<br/>
	 * @param z The z coordinate of the first point.<br/>
	 * @param locational2 The location providing coordinates for the second point.
	 */
	public void addLine(String name, int color, boolean isNameColored, int x, int y, int z, ILocational locational2)
	{
		addLine(name, color, isNameColored, x, y, z, locational2.getX(), locational2.getY(), locational2.getZ());
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method uses two {@code ILocational} objects to define the start and end points.<br/>
	 * @param name The unique identifier for this line.<br/>
	 * @param color The integer color value of the line.<br/>
	 * @param isNameColored Whether the name associated with the line should be colored.<br/>
	 * @param locational The starting location of the line.<br/>
	 * @param locational2 The ending location of the line.
	 */
	public void addLine(String name, int color, boolean isNameColored, ILocational locational, ILocational locational2)
	{
		addLine(name, color, isNameColored, locational, locational2.getX(), locational2.getY(), locational2.getZ());
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method uses an empty string for the name.<br/>
	 * It defines a line between two sets of coordinates.
	 * @param color The color value of the line.<br/>
	 * @param x The x coordinate of the first point.<br/>
	 * @param y The y coordinate of the first point.<br/>
	 * @param z The z coordinate of the first point.<br/>
	 * @param x2 The x coordinate of the second point.<br/>
	 * @param y2 The y coordinate of the second point.<br/>
	 * @param z2 The z coordinate of the second point.
	 */
	public void addLine(int color, int x, int y, int z, int x2, int y2, int z2)
	{
		addLine("", color, false, x, y, z, x2, y2, z2);
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method uses an empty name for the line.<br/>
	 * @param color The color of the line.<br/>
	 * @param locational The starting location of the line.<br/>
	 * @param x2 The x coordinate of the end point.<br/>
	 * @param y2 The y coordinate of the end point.<br/>
	 * @param z2 The z coordinate of the end point.
	 */
	public void addLine(int color, ILocational locational, int x2, int y2, int z2)
	{
		addLine("", color, false, locational, x2, y2, z2);
	}
	
	/**
	 * Adds a line to the primitive object.<br/>
	 * This method uses an empty name for the line.<br/>
	 * @param color The color of the line.<br/>
	 * @param x The x coordinate of the starting point.<br/>
	 * @param y The y coordinate of the starting point.<br/>
	 * @param z The z coordinate of the starting point.<br/>
	 * @param locational2 The location of the ending point.
	 */
	public void addLine(int color, int x, int y, int z, ILocational locational2)
	{
		addLine("", color, false, x, y, z, locational2);
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method connects two locations with a specific color.
	 * @param color The color value of the line.<br/>
	 * @param locational The starting location of the line.<br/>
	 * @param locational2 The ending location of the line.
	 */
	public void addLine(int color, ILocational locational, ILocational locational2)
	{
		addLine("", color, false, locational, locational2);
	}
	
	/**
	 * Adds a line to the {@code ExServerPrimitive} object.<br/>
	 * This method defines a line between two sets of 3D coordinates.<br/>
	 * @param name The unique identifier for this line.<br/>
	 * @param color The {@code Color} used to render the line.<br/>
	 * @param isNameColored Whether the name should be colored.<br/>
	 * @param x The starting X coordinate.<br/>
	 * @param y The starting Y coordinate.<br/>
	 * @param z The starting Z coordinate.<br/>
	 * @param x2 The ending X coordinate.<br/>
	 * @param y2 The ending Y coordinate.<br/>
	 * @param z2 The ending Z coordinate.
	 */
	public void addLine(String name, Color color, boolean isNameColored, int x, int y, int z, int x2, int y2, int z2)
	{
		addLine(name, color.getRGB(), isNameColored, x, y, z, x2, y2, z2);
	}
	
	/**
	 * Adds a line to the primitive drawing.<br/>
	 * This method uses the location of an {@code ILocational} object for the starting point.<br/>
	 * @param name The unique identifier for this line.<br/>
	 * @param color The {@code Color} used to draw the line.<br/>
	 * @param isNameColored Whether the name associated with the line should be colored.<br/>
	 * @param locational The location object providing the starting coordinates.<br/>
	 * @param x2 The x-coordinate of the end point.<br/>
	 * @param y2 The y-coordinate of the end point.<br/>
	 * @param z2 The z-coordinate of the end point.
	 */
	public void addLine(String name, Color color, boolean isNameColored, ILocational locational, int x2, int y2, int z2)
	{
		addLine(name, color.getRGB(), isNameColored, locational, x2, y2, z2);
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method uses specific coordinates and a location object for positioning.<br/>
	 * @param name The unique identifier for this line.<br/>
	 * @param color The {@code Color} of the line.<br/>
	 * @param isNameColored Whether the name should be colored.<br/>
	 * @param x The starting X coordinate.<br/>
	 * @param y The starting Y coordinate.<br/>
	 * @param z The starting Z coordinate.<br/>
	 * @param locational2 The destination location for the line.
	 */
	public void addLine(String name, Color color, boolean isNameColored, int x, int y, int z, ILocational locational2)
	{
		addLine(name, color.getRGB(), isNameColored, x, y, z, locational2);
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method uses two {@code ILocational} objects to define the start and end points.<br/>
	 * @param name The unique identifier for this line.<br/>
	 * @param color The color of the line.<br/>
	 * @param isNameColored Whether the name should be colored.<br/>
	 * @param locational The starting location of the line.<br/>
	 * @param locational2 The ending location of the line.
	 */
	public void addLine(String name, Color color, boolean isNameColored, ILocational locational, ILocational locational2)
	{
		addLine(name, color.getRGB(), isNameColored, locational, locational2);
	}
	
	/**
	 * Adds a line to the primitive collection.<br/>
	 * This method uses an empty string for the name.<br/>
	 * It defines a line between two sets of coordinates.
	 * @param color The {@code Color} of the line.<br/>
	 * @param x The starting x coordinate.<br/>
	 * @param y The starting y coordinate.<br/>
	 * @param z The starting z coordinate.<br/>
	 * @param x2 The ending x coordinate.<br/>
	 * @param y2 The ending y coordinate.<br/>
	 * @param z2 The ending z coordinate.
	 */
	public void addLine(Color color, int x, int y, int z, int x2, int y2, int z2)
	{
		addLine("", color, false, x, y, z, x2, y2, z2);
	}
	
	/**
	 * Adds a line to the primitive drawing.<br/>
	 * This method uses an empty name for the line.<br/>
	 * @param color The {@code Color} of the line.<br/>
	 * @param locational The starting location of the line.<br/>
	 * @param x2 The x coordinate of the end point.<br/>
	 * @param y2 The y coordinate of the end point.<br/>
	 * @param z2 The z coordinate of the end point.
	 */
	public void addLine(Color color, ILocational locational, int x2, int y2, int z2)
	{
		addLine("", color, false, locational, x2, y2, z2);
	}
	
	/**
	 * Adds a line to the primitive drawing.<br/>
	 * This method uses an empty name for the line.<br/>
	 * @param color The {@code Color} of the line.<br/>
	 * @param x The starting x coordinate.<br/>
	 * @param y The starting y coordinate.<br/>
	 * @param z The starting z coordinate.<br/>
	 * @param locational2 The ending location for the line.
	 */
	public void addLine(Color color, int x, int y, int z, ILocational locational2)
	{
		addLine("", color, false, x, y, z, locational2);
	}
	
	/**
	 * Adds a line to the primitive drawing.<br/>
	 * This method connects two locations with a specific color.
	 * @param color The {@code Color} of the line.<br/>
	 * @param locational The starting location of the line.<br/>
	 * @param locational2 The ending location of the line.
	 */
	public void addLine(Color color, ILocational locational, ILocational locational2)
	{
		addLine("", color, false, locational, locational2);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SERVER_PRIMITIVE.writeId(this, buffer);
		buffer.writeString(_name);
		buffer.writeInt(_x);
		buffer.writeInt(_y);
		buffer.writeInt(_z);
		buffer.writeInt(65535); // Has to do something with display range and angle.
		buffer.writeInt(65535); // Has to do something with display range and angle.
		buffer.writeInt(_points.size() + _lines.size());
		for (Point point : _points)
		{
			buffer.writeByte(1); // It is the type in this case Point.
			buffer.writeString(point.getName());
			final int color = point.getColor();
			buffer.writeInt((color >> 16) & 0xFF); // R
			buffer.writeInt((color >> 8) & 0xFF); // G
			buffer.writeInt(color & 0xFF); // B
			buffer.writeInt(point.isNameColored());
			buffer.writeInt(point.getX());
			buffer.writeInt(point.getY());
			buffer.writeInt(point.getZ());
		}
		
		for (Line line : _lines)
		{
			buffer.writeByte(2); // It is the type in this case Line.
			buffer.writeString(line.getName());
			final int color = line.getColor();
			buffer.writeInt((color >> 16) & 0xFF); // R
			buffer.writeInt((color >> 8) & 0xFF); // G
			buffer.writeInt(color & 0xFF); // B
			buffer.writeInt(line.isNameColored());
			buffer.writeInt(line.getX());
			buffer.writeInt(line.getY());
			buffer.writeInt(line.getZ());
			buffer.writeInt(line.getX2());
			buffer.writeInt(line.getY2());
			buffer.writeInt(line.getZ2());
		}
	}
	
	private static class Point
	{
		private final String _name;
		private final int _color;
		private final boolean _isNameColored;
		private final int _x;
		private final int _y;
		private final int _z;
		
		public Point(String name, int color, boolean isNameColored, int x, int y, int z)
		{
			_name = name;
			_color = color;
			_isNameColored = isNameColored;
			_x = x;
			_y = y;
			_z = z;
		}
		
		/**
		 * @return the name
		 */
		public String getName()
		{
			return _name;
		}
		
		/**
		 * @return the color
		 */
		public int getColor()
		{
			return _color;
		}
		
		/**
		 * @return the isNameColored
		 */
		public boolean isNameColored()
		{
			return _isNameColored;
		}
		
		/**
		 * @return the x
		 */
		public int getX()
		{
			return _x;
		}
		
		/**
		 * @return the y
		 */
		public int getY()
		{
			return _y;
		}
		
		/**
		 * @return the z
		 */
		public int getZ()
		{
			return _z;
		}
	}
	
	private static class Line extends Point
	{
		private final int _x2;
		private final int _y2;
		private final int _z2;
		
		public Line(String name, int color, boolean isNameColored, int x, int y, int z, int x2, int y2, int z2)
		{
			super(name, color, isNameColored, x, y, z);
			_x2 = x2;
			_y2 = y2;
			_z2 = z2;
		}
		
		/**
		 * @return the x2
		 */
		public int getX2()
		{
			return _x2;
		}
		
		/**
		 * @return the y2
		 */
		public int getY2()
		{
			return _y2;
		}
		
		/**
		 * @return the z2
		 */
		public int getZ2()
		{
			return _z2;
		}
	}
}
