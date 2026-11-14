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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.l2jmobius.tools.dashboard.tools.MapExtractorService;
import org.l2jmobius.tools.dashboard.tools.MapExtractorService.ClientScanResult;
import org.l2jmobius.tools.map.TerrainPreviewRenderer.PreviewResult;

/**
 * Renders the detected client map-tile grid and optional colored terrain preview.
 * @author BazookaRpm
 */
public final class MapRegionPreviewPanel extends JPanel
{
	private ClientScanResult _scanResult;
	private PreviewResult _terrainPreview;
	private Set<String> _selectedRegions = Collections.emptySet();
	private Set<String> _availableRegions = Collections.emptySet();
	private Consumer<String> _selectionListener;
	
	public MapRegionPreviewPanel()
	{
		setOpaque(true);
		setBackground(ControlCenterTheme.CONSOLE);
		setBorder(BorderFactory.createLineBorder(ControlCenterTheme.BORDER));
		setPreferredSize(new Dimension(240, 96));
		setMinimumSize(new Dimension(0, 82));
		ToolTipManager.sharedInstance().registerComponent(this);
		addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if (!isEnabled() || (event.getButton() != MouseEvent.BUTTON1) || (_selectionListener == null))
				{
					return;
				}
				
				final String region = regionAt(event.getX(), event.getY());
				if ((region != null) && _availableRegions.contains(region))
				{
					_selectionListener.accept(region);
				}
			}
		});
	}
	
	public void setSelectionListener(Consumer<String> selectionListener)
	{
		_selectionListener = selectionListener;
	}
	
	public void setData(ClientScanResult scanResult, List<String> availableRegions, Set<String> selectedRegions)
	{
		_scanResult = scanResult;
		_availableRegions = new HashSet<>(availableRegions);
		_selectedRegions = new HashSet<>(selectedRegions);
		repaint();
	}
	
	public void setTerrainPreview(PreviewResult terrainPreview)
	{
		_terrainPreview = terrainPreview;
		repaint();
	}
	
	@Override
	public String getToolTipText(MouseEvent event)
	{
		final String region = regionAt(event.getX(), event.getY());
		if ((region == null) || !_availableRegions.contains(region))
		{
			return null;
		}
		
		final boolean selected = _selectedRegions.contains(region);
		final Set<String> variants = _scanResult == null ? Collections.emptySet() : _scanResult.variantsFor(region);
		return region + (selected ? " - selected" : " - available") + variantTooltip(variants);
	}
	
	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		final Graphics2D canvas = (Graphics2D) graphics.create();
		canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		if (_scanResult == null)
		{
			paintEmpty(canvas);
			canvas.dispose();
			return;
		}
		
		final int columns = Math.max(1, (_scanResult.maximumX() - _scanResult.minimumX()) + 1);
		final int rows = Math.max(1, (_scanResult.maximumY() - _scanResult.minimumY()) + 1);
		final int margin = 7;
		final double cellWidth = Math.max(2.0, (getWidth() - (margin * 2.0)) / columns);
		final double cellHeight = Math.max(2.0, (getHeight() - (margin * 2.0)) / rows);
		paintTerrain(canvas, margin, columns, rows, cellWidth, cellHeight);
		paintRegions(canvas, margin, cellWidth, cellHeight);
		canvas.dispose();
	}
	
	private void paintTerrain(Graphics2D canvas, int margin, int columns, int rows, double cellWidth, double cellHeight)
	{
		if ((_terrainPreview == null) || (_terrainPreview.image() == null))
		{
			return;
		}
		
		final BufferedImage image = _terrainPreview.image();
		final int x = margin + (int) Math.floor((_terrainPreview.minimumX() - _scanResult.minimumX()) * cellWidth);
		final int y = margin + (int) Math.floor((_terrainPreview.minimumY() - _scanResult.minimumY()) * cellHeight);
		final int previewColumns = (_terrainPreview.maximumX() - _terrainPreview.minimumX()) + 1;
		final int previewRows = (_terrainPreview.maximumY() - _terrainPreview.minimumY()) + 1;
		final int width = Math.max(1, (int) Math.ceil(previewColumns * cellWidth));
		canvas.drawImage(image, x, y, width, Math.max(1, (int) Math.ceil(previewRows * cellHeight)), null);
	}
	
	private void paintRegions(Graphics2D canvas, int margin, double cellWidth, double cellHeight)
	{
		final Color selectedFill = alpha(ControlCenterTheme.ACCENT, _terrainPreview == null ? 255 : 92);
		final Color selectedBorder = alpha(ControlCenterTheme.ACCENT_HOVER, 220);
		final Color availableFill = _terrainPreview == null ? ControlCenterTheme.SURFACE_HOVER : alpha(ControlCenterTheme.SURFACE_HOVER, 35);
		final Color grid = alpha(ControlCenterTheme.BORDER, 165);
		canvas.setStroke(new BasicStroke(1f));
		for (String region : _scanResult.regions())
		{
			if (!_availableRegions.contains(region))
			{
				continue;
			}
			
			final int[] coordinates = parseRegion(region);
			final int column = coordinates[0] - _scanResult.minimumX();
			final int row = coordinates[1] - _scanResult.minimumY();
			final int x = margin + (int) Math.floor(column * cellWidth);
			final int y = margin + (int) Math.floor(row * cellHeight);
			final int width = Math.max(2, (int) Math.ceil(cellWidth));
			final int height = Math.max(2, (int) Math.ceil(cellHeight));
			if (_selectedRegions.contains(region))
			{
				canvas.setColor(selectedFill);
				canvas.fillRect(x, y, width, height);
				canvas.setColor(selectedBorder);
				canvas.drawRect(x, y, width, height);
			}
			else
			{
				canvas.setColor(availableFill);
				canvas.fillRect(x, y, width, height);
				canvas.setColor(grid);
				canvas.drawRect(x, y, width, height);
			}
			
			if (_scanResult.hasVariants(region))
			{
				canvas.setColor(ControlCenterTheme.YELLOW);
				canvas.fillOval(x + Math.max(1, width - 4), y + 1, 3, 3);
			}
		}
	}
	
	private void paintEmpty(Graphics2D canvas)
	{
		final String message = "Scan client to build the region preview.";
		canvas.setFont(ControlCenterTheme.font(Font.PLAIN, 10));
		canvas.setColor(ControlCenterTheme.MUTED);
		final FontMetrics metrics = canvas.getFontMetrics();
		canvas.drawString(message, Math.max(8, (getWidth() - metrics.stringWidth(message)) / 2), Math.max(18, getHeight() / 2));
	}
	
	private String regionAt(int mouseX, int mouseY)
	{
		if (_scanResult == null)
		{
			return null;
		}
		
		final int columns = Math.max(1, (_scanResult.maximumX() - _scanResult.minimumX()) + 1);
		final int rows = Math.max(1, (_scanResult.maximumY() - _scanResult.minimumY()) + 1);
		final int margin = 7;
		final double cellWidth = Math.max(2.0, (getWidth() - (margin * 2.0)) / columns);
		final double cellHeight = Math.max(2.0, (getHeight() - (margin * 2.0)) / rows);
		if ((mouseX < margin) || (mouseY < margin))
		{
			return null;
		}
		
		final int column = (int) ((mouseX - margin) / cellWidth);
		final int row = (int) ((mouseY - margin) / cellHeight);
		if ((column < 0) || (column >= columns) || (row < 0) || (row >= rows))
		{
			return null;
		}
		
		return (_scanResult.minimumX() + column) + "_" + (_scanResult.minimumY() + row);
	}
	
	private static String variantTooltip(Set<String> variants)
	{
		if ((variants == null) || variants.isEmpty())
		{
			return "";
		}
		
		final StringBuilder text = new StringBuilder(" - variants: ");
		boolean first = true;
		for (String variant : variants)
		{
			if (!first)
			{
				text.append(", ");
			}
			text.append(MapExtractorService.displayMapVariant(variant));
			first = false;
		}
		
		return text.toString();
	}
	
	private static Color alpha(Color color, int alpha)
	{
		return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
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
}
