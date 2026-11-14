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
package org.l2jmobius.tools.dashboard.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import javax.swing.Icon;

/**
 * Draws the vector symbols used by the dashboard without external image resources.
 * @author BazookaRpm
 */
public final class GlyphIcon implements Icon
{
	public enum Kind
	{
		HOME,
		SERVICE,
		LOG,
		DATABASE,
		TELEMETRY,
		DIAGNOSTICS,
		SETTINGS,
		PREFERENCES,
		TOOLS,
		DETAILS,
		PLAY,
		PAUSE,
		STOP,
		RESTART,
		REFRESH,
		SAVE,
		FOLDER,
		TRASH,
		SEARCH,
		INFO
	}
	
	private final Kind _kind;
	private final int _size;
	private final Color _color;
	
	public GlyphIcon(Kind kind, int size, Color color)
	{
		_kind = kind;
		_size = Math.max(12, size);
		_color = color;
	}
	
	@Override
	public int getIconWidth()
	{
		return _size;
	}
	
	@Override
	public int getIconHeight()
	{
		return _size;
	}
	
	@Override
	public void paintIcon(Component component, Graphics graphics, int x, int y)
	{
		final Graphics2D canvas = (Graphics2D) graphics.create();
		canvas.translate(x, y);
		canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		canvas.setStroke(new BasicStroke(Math.max(1.4f, _size / 10f), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		canvas.setColor(_color);
		draw(canvas);
		canvas.dispose();
	}
	
	private void draw(Graphics2D canvas)
	{
		final double size = _size;
		switch (_kind)
		{
			case HOME:
			{
				final GeneralPath path = new GeneralPath();
				path.moveTo(size * 0.14, size * 0.48);
				path.lineTo(size * 0.50, size * 0.17);
				path.lineTo(size * 0.86, size * 0.48);
				path.moveTo(size * 0.24, size * 0.43);
				path.lineTo(size * 0.24, size * 0.82);
				path.lineTo(size * 0.76, size * 0.82);
				path.lineTo(size * 0.76, size * 0.43);
				canvas.draw(path);
				break;
			}
			case SERVICE:
			{
				canvas.draw(new Rectangle2D.Double(size * 0.14, size * 0.17, size * 0.72, size * 0.25));
				canvas.draw(new Rectangle2D.Double(size * 0.14, size * 0.58, size * 0.72, size * 0.25));
				canvas.fill(new Ellipse2D.Double(size * 0.68, size * 0.25, size * 0.08, size * 0.08));
				canvas.fill(new Ellipse2D.Double(size * 0.68, size * 0.66, size * 0.08, size * 0.08));
				break;
			}
			case LOG:
			{
				canvas.draw(new Rectangle2D.Double(size * 0.19, size * 0.14, size * 0.62, size * 0.72));
				for (int row = 0; row < 3; row++)
				{
					final double y = size * (0.34 + (row * 0.16));
					canvas.draw(new Line2D.Double(size * 0.30, y, size * 0.70, y));
				}
				break;
			}
			case DATABASE:
			{
				canvas.draw(new Ellipse2D.Double(size * 0.18, size * 0.16, size * 0.64, size * 0.23));
				canvas.draw(new Arc2D.Double(size * 0.18, size * 0.29, size * 0.64, size * 0.25, 180, 180, Arc2D.OPEN));
				canvas.draw(new Arc2D.Double(size * 0.18, size * 0.48, size * 0.64, size * 0.25, 180, 180, Arc2D.OPEN));
				canvas.draw(new Line2D.Double(size * 0.18, size * 0.28, size * 0.18, size * 0.68));
				canvas.draw(new Line2D.Double(size * 0.82, size * 0.28, size * 0.82, size * 0.68));
				break;
			}
			case TELEMETRY:
			{
				canvas.draw(new Line2D.Double(size * 0.17, size * 0.75, size * 0.17, size * 0.24));
				canvas.draw(new Line2D.Double(size * 0.17, size * 0.75, size * 0.84, size * 0.75));
				final GeneralPath graph = new GeneralPath();
				graph.moveTo(size * 0.23, size * 0.63);
				graph.lineTo(size * 0.39, size * 0.48);
				graph.lineTo(size * 0.52, size * 0.58);
				graph.lineTo(size * 0.68, size * 0.31);
				graph.lineTo(size * 0.82, size * 0.39);
				canvas.draw(graph);
				break;
			}
			case DIAGNOSTICS:
			case SEARCH:
			{
				canvas.draw(new Ellipse2D.Double(size * 0.18, size * 0.18, size * 0.48, size * 0.48));
				canvas.draw(new Line2D.Double(size * 0.60, size * 0.60, size * 0.84, size * 0.84));
				break;
			}
			case SETTINGS:
			case PREFERENCES:
			{
				canvas.draw(new Ellipse2D.Double(size * 0.31, size * 0.31, size * 0.38, size * 0.38));
				for (int index = 0; index < 8; index++)
				{
					final double angle = Math.PI * index / 4.0;
					canvas.draw(new Line2D.Double(size * 0.50 + (Math.cos(angle) * size * 0.25), size * 0.50 + (Math.sin(angle) * size * 0.25), size * 0.50 + (Math.cos(angle) * size * 0.38), size * 0.50 + (Math.sin(angle) * size * 0.38)));
				}
				break;
			}
			case TOOLS:
			{
				canvas.draw(new Line2D.Double(size * 0.22, size * 0.78, size * 0.72, size * 0.28));
				canvas.draw(new Arc2D.Double(size * 0.54, size * 0.13, size * 0.32, size * 0.32, 145, 220, Arc2D.OPEN));
				canvas.draw(new Ellipse2D.Double(size * 0.15, size * 0.69, size * 0.18, size * 0.18));
				break;
			}
			case DETAILS:
			case INFO:
			{
				canvas.draw(new Ellipse2D.Double(size * 0.18, size * 0.18, size * 0.64, size * 0.64));
				canvas.draw(new Line2D.Double(size * 0.50, size * 0.43, size * 0.50, size * 0.67));
				canvas.fill(new Ellipse2D.Double(size * 0.46, size * 0.29, size * 0.08, size * 0.08));
				break;
			}
			case PLAY:
			{
				final GeneralPath triangle = new GeneralPath();
				triangle.moveTo(size * 0.30, size * 0.20);
				triangle.lineTo(size * 0.78, size * 0.50);
				triangle.lineTo(size * 0.30, size * 0.80);
				triangle.closePath();
				canvas.fill(triangle);
				break;
			}
			case PAUSE:
			{
				canvas.fill(new Rectangle2D.Double(size * 0.26, size * 0.22, size * 0.15, size * 0.56));
				canvas.fill(new Rectangle2D.Double(size * 0.59, size * 0.22, size * 0.15, size * 0.56));
				break;
			}
			case STOP:
			{
				canvas.fill(new Rectangle2D.Double(size * 0.26, size * 0.26, size * 0.48, size * 0.48));
				break;
			}
			case RESTART:
			case REFRESH:
			{
				canvas.draw(new Arc2D.Double(size * 0.18, size * 0.18, size * 0.64, size * 0.64, 35, 285, Arc2D.OPEN));
				final GeneralPath arrow = new GeneralPath();
				arrow.moveTo(size * 0.70, size * 0.15);
				arrow.lineTo(size * 0.86, size * 0.18);
				arrow.lineTo(size * 0.79, size * 0.32);
				arrow.closePath();
				canvas.fill(arrow);
				break;
			}
			case SAVE:
			{
				canvas.draw(new Rectangle2D.Double(size * 0.20, size * 0.16, size * 0.60, size * 0.68));
				canvas.draw(new Rectangle2D.Double(size * 0.31, size * 0.20, size * 0.37, size * 0.20));
				canvas.draw(new Rectangle2D.Double(size * 0.32, size * 0.57, size * 0.36, size * 0.20));
				break;
			}
			case FOLDER:
			{
				final GeneralPath folder = new GeneralPath();
				folder.moveTo(size * 0.14, size * 0.31);
				folder.lineTo(size * 0.40, size * 0.31);
				folder.lineTo(size * 0.48, size * 0.22);
				folder.lineTo(size * 0.84, size * 0.22);
				folder.lineTo(size * 0.84, size * 0.76);
				folder.lineTo(size * 0.14, size * 0.76);
				folder.closePath();
				canvas.draw(folder);
				break;
			}
			case TRASH:
			{
				canvas.draw(new Rectangle2D.Double(size * 0.28, size * 0.30, size * 0.44, size * 0.50));
				canvas.draw(new Line2D.Double(size * 0.22, size * 0.24, size * 0.78, size * 0.24));
				canvas.draw(new Line2D.Double(size * 0.40, size * 0.17, size * 0.60, size * 0.17));
				canvas.draw(new Line2D.Double(size * 0.41, size * 0.40, size * 0.41, size * 0.69));
				canvas.draw(new Line2D.Double(size * 0.59, size * 0.40, size * 0.59, size * 0.69));
				break;
			}
			default:
			{
				break;
			}
		}
	}
}
