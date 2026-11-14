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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.ToolTipManager;

import org.l2jmobius.tools.dashboard.tools.MapExtractorService.ClientScanResult;
import org.l2jmobius.tools.dashboard.tools.MapPreviewLayer;

/**
 * Displays tile-aligned map preview layers with zoom, pan and region selection.
 * @author BazookaRpm
 */
public final class WorldTerrainPreviewPanel extends JPanel
{
	public enum PreviewMode
	{
		TERRAIN,
		CLIENT_MAP,
		BLEND
	}
	
	private static final double MIN_SCALE = 0.15;
	private static final double MAX_SCALE = 8.0;
	private static final int VIEW_MARGIN = 18;
	private static final float BLEND_ALPHA = 0.50f;
	private ClientScanResult _scanResult;
	private MapPreviewLayer _terrainLayer;
	private MapPreviewLayer _clientMapLayer;
	private Set<String> _availableRegions = Collections.emptySet();
	private Set<String> _selectedRegions = Collections.emptySet();
	private Consumer<String> _selectionListener;
	private PreviewMode _previewMode = PreviewMode.TERRAIN;
	private boolean _showGrid = true;
	private boolean _showRegionNames;
	private boolean _fitMode = true;
	private double _scale = 1.0;
	private double _offsetX;
	private double _offsetY;
	private int _dragX;
	private int _dragY;
	private boolean _panning;
	
	public WorldTerrainPreviewPanel()
	{
		setOpaque(true);
		setBackground(ControlCenterTheme.CONSOLE);
		setBorder(BorderFactory.createLineBorder(ControlCenterTheme.BORDER));
		ToolTipManager.sharedInstance().registerComponent(this);
		final MouseAdapter mouse = new MouseAdapter()
		{
			@Override
			public void mousePressed(MouseEvent event)
			{
				if (event.getButton() == MouseEvent.BUTTON3)
				{
					prepareTransform();
					_fitMode = false;
					_panning = true;
					_dragX = event.getX();
					_dragY = event.getY();
				}
			}
			
			@Override
			public void mouseReleased(MouseEvent event)
			{
				if (event.getButton() == MouseEvent.BUTTON3)
				{
					_panning = false;
				}
			}
			
			@Override
			public void mouseDragged(MouseEvent event)
			{
				if (!_panning)
				{
					return;
				}
				
				_offsetX += event.getX() - _dragX;
				_offsetY += event.getY() - _dragY;
				_dragX = event.getX();
				_dragY = event.getY();
				repaint();
			}
			
			@Override
			public void mouseClicked(MouseEvent event)
			{
				if ((event.getButton() != MouseEvent.BUTTON1) || (_selectionListener == null))
				{
					return;
				}
				
				final String region = regionAt(event.getX(), event.getY());
				if ((region != null) && _availableRegions.contains(region))
				{
					_selectionListener.accept(region);
				}
			}
			
			@Override
			public void mouseWheelMoved(MouseWheelEvent event)
			{
				if (getReferenceLayer() == null)
				{
					return;
				}
				
				prepareTransform();
				_fitMode = false;
				final double previousScale = _scale;
				final double factor = event.getWheelRotation() < 0 ? 1.15 : (1.0 / 1.15);
				_scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, _scale * factor));
				final double imageX = (event.getX() - _offsetX) / previousScale;
				final double imageY = (event.getY() - _offsetY) / previousScale;
				_offsetX = event.getX() - (imageX * _scale);
				_offsetY = event.getY() - (imageY * _scale);
				repaint();
			}
		};
		addMouseListener(mouse);
		addMouseMotionListener(mouse);
		addMouseWheelListener(mouse);
	}
	
	public void setSelectionListener(Consumer<String> selectionListener)
	{
		_selectionListener = selectionListener;
	}
	
	public void setData(ClientScanResult scanResult, MapPreviewLayer terrainLayer, MapPreviewLayer clientMapLayer, Set<String> availableRegions, Set<String> selectedRegions)
	{
		_scanResult = scanResult;
		_terrainLayer = terrainLayer;
		_clientMapLayer = clientMapLayer;
		_availableRegions = availableRegions == null ? Collections.emptySet() : new HashSet<>(availableRegions);
		_selectedRegions = selectedRegions == null ? Collections.emptySet() : new HashSet<>(selectedRegions);
		if (!isModeAvailable(_previewMode))
		{
			_previewMode = getFallbackMode();
		}
		
		fitToView();
	}
	
	public void setClientMapLayer(MapPreviewLayer clientMapLayer)
	{
		_clientMapLayer = clientMapLayer;
		if (!isModeAvailable(_previewMode))
		{
			_previewMode = getFallbackMode();
		}
		
		fitToView();
	}
	
	public void setPreviewMode(PreviewMode previewMode)
	{
		final MapPreviewLayer previousReferenceLayer = getReferenceLayer();
		_previewMode = isModeAvailable(previewMode) ? previewMode : getFallbackMode();
		if (previousReferenceLayer != getReferenceLayer())
		{
			fitToView();
			return;
		}
		
		repaint();
	}
	
	public void setSelection(Set<String> selectedRegions)
	{
		_selectedRegions = selectedRegions == null ? Collections.emptySet() : new HashSet<>(selectedRegions);
		repaint();
	}
	
	public void setShowGrid(boolean showGrid)
	{
		_showGrid = showGrid;
		repaint();
	}
	
	public void setShowRegionNames(boolean showRegionNames)
	{
		_showRegionNames = showRegionNames;
		repaint();
	}
	
	public void fitToView()
	{
		_fitMode = true;
		repaint();
	}
	
	@Override
	public String getToolTipText(MouseEvent event)
	{
		final String region = regionAt(event.getX(), event.getY());
		if ((region == null) || !_availableRegions.contains(region))
		{
			return "Scroll to zoom. Hold the right mouse button to pan.";
		}
		
		return region + (_selectedRegions.contains(region) ? " - selected" : " - available");
	}
	
	@Override
	protected void paintComponent(Graphics graphics)
	{
		super.paintComponent(graphics);
		final Graphics2D canvas = (Graphics2D) graphics.create();
		canvas.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		final MapPreviewLayer referenceLayer = getReferenceLayer();
		if ((referenceLayer == null) || (_scanResult == null))
		{
			canvas.setColor(ControlCenterTheme.MUTED);
			canvas.drawString("Map preview is not available.", 16, 24);
			canvas.dispose();
			return;
		}
		
		prepareTransform();
		switch (_previewMode)
		{
			case CLIENT_MAP:
			{
				drawLayer(canvas, _clientMapLayer, referenceLayer, 1.0f);
				break;
			}
			case BLEND:
			{
				drawLayer(canvas, _terrainLayer, referenceLayer, 1.0f);
				drawLayer(canvas, _clientMapLayer, referenceLayer, BLEND_ALPHA);
				break;
			}
			case TERRAIN:
			default:
			{
				drawLayer(canvas, _terrainLayer, referenceLayer, 1.0f);
				break;
			}
		}
		
		paintTileOverlay(canvas, referenceLayer);
		canvas.dispose();
	}
	
	private void drawLayer(Graphics2D canvas, MapPreviewLayer layer, MapPreviewLayer referenceLayer, float alpha)
	{
		if (layer == null)
		{
			return;
		}
		
		final double tileWidth = referenceLayer.image().getWidth() / (double) referenceLayer.columns();
		final double tileHeight = referenceLayer.image().getHeight() / (double) referenceLayer.rows();
		final int drawX = (int) Math.round(_offsetX + ((layer.minimumX() - referenceLayer.minimumX()) * tileWidth * _scale));
		final int drawY = (int) Math.round(_offsetY + ((layer.minimumY() - referenceLayer.minimumY()) * tileHeight * _scale));
		final int drawWidth = Math.max(1, (int) Math.round(layer.columns() * tileWidth * _scale));
		final int drawHeight = Math.max(1, (int) Math.round(layer.rows() * tileHeight * _scale));
		final Composite previousComposite = canvas.getComposite();
		if (alpha < 1.0f)
		{
			canvas.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
		}
		
		canvas.drawImage(layer.image(), drawX, drawY, drawWidth, drawHeight, null);
		canvas.setComposite(previousComposite);
	}
	
	private void paintTileOverlay(Graphics2D canvas, MapPreviewLayer referenceLayer)
	{
		final double tileWidth = (referenceLayer.image().getWidth() * _scale) / referenceLayer.columns();
		final double tileHeight = (referenceLayer.image().getHeight() * _scale) / referenceLayer.rows();
		final Color selectionColor = alpha(ControlCenterTheme.ACCENT, 92);
		final Color selectedBorder = alpha(ControlCenterTheme.ACCENT_HOVER, 220);
		final Color gridColor = alpha(ControlCenterTheme.BORDER, 150);
		final Color regionNameColor = alpha(ControlCenterTheme.TEXT, 230);
		canvas.setFont(canvas.getFont().deriveFont(Font.BOLD, 10f));
		final FontMetrics fontMetrics = canvas.getFontMetrics();
		canvas.setStroke(new BasicStroke(1f));
		for (String region : _availableRegions)
		{
			final int[] coordinates = parseRegion(region);
			final int column = coordinates[0] - referenceLayer.minimumX();
			final int row = coordinates[1] - referenceLayer.minimumY();
			if ((column < 0) || (column >= referenceLayer.columns()) || (row < 0) || (row >= referenceLayer.rows()))
			{
				continue;
			}
			
			final int x = (int) Math.round(_offsetX + (column * tileWidth));
			final int y = (int) Math.round(_offsetY + (row * tileHeight));
			final int width = Math.max(1, (int) Math.ceil(tileWidth));
			final int height = Math.max(1, (int) Math.ceil(tileHeight));
			if (_selectedRegions.contains(region))
			{
				canvas.setColor(selectionColor);
				canvas.fillRect(x, y, width, height);
				canvas.setColor(selectedBorder);
				canvas.drawRect(x, y, width, height);
			}
			else if (_showGrid)
			{
				canvas.setColor(gridColor);
				canvas.drawRect(x, y, width, height);
			}
			
			if (_showRegionNames)
			{
				final int textWidth = fontMetrics.stringWidth(region);
				if ((width >= (textWidth + 4)) && (height >= (fontMetrics.getHeight() + 2)))
				{
					final int textX = x + ((width - textWidth) / 2);
					final int textY = y + ((height - fontMetrics.getHeight()) / 2) + fontMetrics.getAscent();
					canvas.setColor(regionNameColor);
					canvas.drawString(region, textX, textY);
				}
			}
		}
	}
	
	private void prepareTransform()
	{
		final MapPreviewLayer referenceLayer = getReferenceLayer();
		if (!_fitMode || (referenceLayer == null))
		{
			return;
		}
		
		final double availableWidth = Math.max(1.0, getWidth() - (VIEW_MARGIN * 2.0));
		final double availableHeight = Math.max(1.0, getHeight() - (VIEW_MARGIN * 2.0));
		_scale = Math.min(availableWidth / referenceLayer.image().getWidth(), availableHeight / referenceLayer.image().getHeight());
		_scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, _scale));
		_offsetX = (getWidth() - (referenceLayer.image().getWidth() * _scale)) / 2.0;
		_offsetY = (getHeight() - (referenceLayer.image().getHeight() * _scale)) / 2.0;
	}
	
	private String regionAt(int mouseX, int mouseY)
	{
		final MapPreviewLayer referenceLayer = getReferenceLayer();
		if (referenceLayer == null)
		{
			return null;
		}
		
		prepareTransform();
		final double imageX = (mouseX - _offsetX) / _scale;
		final double imageY = (mouseY - _offsetY) / _scale;
		if ((imageX < 0) || (imageY < 0) || (imageX >= referenceLayer.image().getWidth()) || (imageY >= referenceLayer.image().getHeight()))
		{
			return null;
		}
		
		final int column = Math.min(referenceLayer.columns() - 1, (int) ((imageX * referenceLayer.columns()) / referenceLayer.image().getWidth()));
		final int row = Math.min(referenceLayer.rows() - 1, (int) ((imageY * referenceLayer.rows()) / referenceLayer.image().getHeight()));
		return (referenceLayer.minimumX() + column) + "_" + (referenceLayer.minimumY() + row);
	}
	
	private boolean isModeAvailable(PreviewMode previewMode)
	{
		switch (previewMode)
		{
			case CLIENT_MAP:
			{
				return _clientMapLayer != null;
			}
			case BLEND:
			{
				return (_terrainLayer != null) && (_clientMapLayer != null);
			}
			case TERRAIN:
			default:
			{
				return _terrainLayer != null;
			}
		}
	}
	
	private PreviewMode getFallbackMode()
	{
		if (_terrainLayer != null)
		{
			return PreviewMode.TERRAIN;
		}
		
		return _clientMapLayer != null ? PreviewMode.CLIENT_MAP : PreviewMode.TERRAIN;
	}
	
	private MapPreviewLayer getReferenceLayer()
	{
		if ((_previewMode == PreviewMode.CLIENT_MAP) && (_clientMapLayer != null))
		{
			return _clientMapLayer;
		}
		
		return _terrainLayer != null ? _terrainLayer : _clientMapLayer;
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
