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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;

import org.l2jmobius.tools.dashboard.tools.MapExtractorService.ClientScanResult;
import org.l2jmobius.tools.dashboard.tools.MapPreviewLayer;
import org.l2jmobius.tools.dashboard.ui.WorldTerrainPreviewPanel.PreviewMode;
import org.l2jmobius.tools.map.TerrainPreviewRenderer.PreviewProgress;
import org.l2jmobius.tools.map.TerrainPreviewRenderer.PreviewResult;

/**
 * Hosts the layered world-map preview for the Dashboard Map Extractor.
 * @author BazookaRpm
 */
public final class MapPreviewDialog extends JDialog
{
	private final ClientScanResult _scanResult;
	private final Set<String> _availableRegions;
	private final WorldTerrainPreviewPanel _map = new WorldTerrainPreviewPanel();
	private final JProgressBar _progress = new JProgressBar(0, 100);
	private final JLabel _status = DashboardUiFactory.label("Preparing terrain preview...", Font.PLAIN, 10, ControlCenterTheme.MUTED);
	private final JRadioButton _terrain = new JRadioButton("Terrain", true);
	private final JRadioButton _clientMap = new JRadioButton("Client Map");
	private final JRadioButton _blend = new JRadioButton("Blend");
	private final JCheckBox _grid = new JCheckBox("Grid", true);
	private final JCheckBox _regionNames = new JCheckBox("Map XX_XX");
	private MapPreviewLayer _terrainLayer;
	private MapPreviewLayer _clientMapLayer;
	
	public MapPreviewDialog(Window owner, ClientScanResult scanResult, List<String> availableRegions, Set<String> selectedRegions, Consumer<String> selectionListener)
	{
		super(owner, "World Map Preview", ModalityType.MODELESS);
		_scanResult = scanResult;
		_availableRegions = new HashSet<>(availableRegions);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		final JPanel root = DashboardUiFactory.panel(new BorderLayout(0, 8));
		root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		final JPanel header = DashboardUiFactory.panel(new BorderLayout(8, 0));
		header.add(DashboardUiFactory.title("World Map Preview", "Terrain is the extraction reference. Compatible client-map layers can be added without changing tile selection."), BorderLayout.WEST);
		final JPanel actions = DashboardUiFactory.flow(FlowLayout.RIGHT, 6);
		final JButton fit = new DashboardActionButton("Fit World", ControlCenterIcons.create(GlyphIcon.Kind.SEARCH, 13, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, false);
		fit.addActionListener(event -> _map.fitToView());
		configureLayerControls();
		configureOverlayControls();
		actions.add(_terrain);
		actions.add(_clientMap);
		actions.add(_blend);
		actions.add(_grid);
		actions.add(_regionNames);
		actions.add(fit);
		header.add(actions, BorderLayout.EAST);
		root.add(header, BorderLayout.NORTH);
		_map.setSelectionListener(selectionListener);
		_map.setData(scanResult, null, null, _availableRegions, selectedRegions);
		root.add(_map, BorderLayout.CENTER);
		final JPanel footer = DashboardUiFactory.panel(new BorderLayout(8, 0));
		_progress.setStringPainted(true);
		_progress.setString("0%");
		_progress.setPreferredSize(new Dimension(320, 22));
		footer.add(_progress, BorderLayout.WEST);
		footer.add(_status, BorderLayout.CENTER);
		footer.add(DashboardUiFactory.label("Mouse wheel: zoom  |  Right drag: pan  |  Left click: select tile", Font.PLAIN, 9, ControlCenterTheme.MUTED), BorderLayout.EAST);
		root.add(footer, BorderLayout.SOUTH);
		setContentPane(root);
		setMinimumSize(new Dimension(900, 520));
		setSize(1240, 760);
		setLocationRelativeTo(owner);
	}
	
	public void setPreview(PreviewResult result, Set<String> selectedRegions)
	{
		_terrainLayer = new MapPreviewLayer(result.image(), result.minimumX(), result.maximumX(), result.minimumY(), result.maximumY(), "G16 Terrain");
		_map.setData(_scanResult, _terrainLayer, _clientMapLayer, _availableRegions, selectedRegions);
		updateLayerAvailability();
		_progress.setValue(100);
		_progress.setString("100%");
		_status.setText("Terrain: " + result.renderedRegions() + " regions" + (result.failedRegions().isEmpty() ? "." : "; " + result.failedRegions().size() + " without terrain preview."));
		_status.setForeground(result.failedRegions().isEmpty() ? ControlCenterTheme.GREEN : ControlCenterTheme.YELLOW);
	}
	
	public void setClientMapPreview(MapPreviewLayer clientMapLayer)
	{
		_clientMapLayer = clientMapLayer;
		_map.setClientMapLayer(clientMapLayer);
		updateLayerAvailability();
	}
	
	public void setProgress(PreviewProgress progress)
	{
		final int percent = progress.totalRegions() == 0 ? 0 : (int) Math.round((progress.processedRegions() * 100.0) / progress.totalRegions());
		_progress.setValue(percent);
		_progress.setString(percent + "%");
		_status.setText("Building " + progress.currentRegion() + "  (" + progress.processedRegions() + "/" + progress.totalRegions() + ")" + (progress.failedRegions() == 0 ? "" : "  Warnings: " + progress.failedRegions()));
		_status.setForeground(progress.failedRegions() == 0 ? ControlCenterTheme.MUTED : ControlCenterTheme.YELLOW);
	}
	
	public void setSelection(Set<String> selectedRegions)
	{
		_map.setSelection(selectedRegions);
	}
	
	public void setFailure(String message)
	{
		_progress.setString("Failed");
		_status.setText(message);
		_status.setForeground(ControlCenterTheme.RED);
	}
	
	private void configureLayerControls()
	{
		final ButtonGroup layers = new ButtonGroup();
		layers.add(_terrain);
		layers.add(_clientMap);
		layers.add(_blend);
		configureRadio(_terrain);
		configureRadio(_clientMap);
		configureRadio(_blend);
		_clientMap.setEnabled(false);
		_blend.setEnabled(false);
		_clientMap.setToolTipText("No compatible client-map preview provider is available for this client.");
		_blend.setToolTipText("No compatible client-map preview provider is available for this client.");
		_terrain.addActionListener(event -> _map.setPreviewMode(PreviewMode.TERRAIN));
		_clientMap.addActionListener(event -> _map.setPreviewMode(PreviewMode.CLIENT_MAP));
		_blend.addActionListener(event -> _map.setPreviewMode(PreviewMode.BLEND));
	}
	
	private void updateLayerAvailability()
	{
		final boolean clientMapAvailable = _clientMapLayer != null;
		final boolean blendAvailable = (_terrainLayer != null) && clientMapAvailable;
		_clientMap.setEnabled(clientMapAvailable);
		_blend.setEnabled(blendAvailable);
		final String clientTooltip = clientMapAvailable ? _clientMapLayer.source() : "No compatible client-map preview provider is available for this client.";
		_clientMap.setToolTipText(clientTooltip);
		_blend.setToolTipText(blendAvailable ? "Blend terrain and " + _clientMapLayer.source() + "." : "Blend requires both terrain and a compatible client-map layer.");
		if (_blend.isSelected() && !blendAvailable)
		{
			_terrain.setSelected(true);
			_map.setPreviewMode(PreviewMode.TERRAIN);
		}
		else if (_clientMap.isSelected() && !clientMapAvailable)
		{
			_terrain.setSelected(true);
			_map.setPreviewMode(PreviewMode.TERRAIN);
		}
	}
	
	private void configureOverlayControls()
	{
		_grid.setOpaque(false);
		_grid.setForeground(ControlCenterTheme.TEXT);
		_grid.addActionListener(event -> _map.setShowGrid(_grid.isSelected()));
		_regionNames.setOpaque(false);
		_regionNames.setForeground(ControlCenterTheme.TEXT);
		_regionNames.addActionListener(event -> _map.setShowRegionNames(_regionNames.isSelected()));
	}
	
	private static void configureRadio(JRadioButton button)
	{
		button.setOpaque(false);
		button.setForeground(ControlCenterTheme.TEXT);
	}
}
