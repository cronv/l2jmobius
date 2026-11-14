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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ScrollPaneConstants;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator;
import org.l2jmobius.tools.dashboard.runtime.DashboardWorkloadCoordinator.WorkloadPermit;
import org.l2jmobius.tools.dashboard.settings.ControlCenterSettings;
import org.l2jmobius.tools.dashboard.settings.SettingsRepository;
import org.l2jmobius.tools.dashboard.tools.ClassicClientMapPreviewProvider;
import org.l2jmobius.tools.dashboard.tools.EarlyClientMapPreviewProvider;
import org.l2jmobius.tools.dashboard.tools.EssenceClientMapPreviewProvider;
import org.l2jmobius.tools.dashboard.tools.GraciaAdenClientMapPreviewProvider;
import org.l2jmobius.tools.dashboard.tools.LiveClientMapPreviewProvider;
import org.l2jmobius.tools.dashboard.tools.MapExtractorService;
import org.l2jmobius.tools.dashboard.tools.MapExtractorService.ClientScanResult;
import org.l2jmobius.tools.dashboard.tools.MapExtractorService.ExtractionProgress;
import org.l2jmobius.tools.dashboard.tools.MapExtractorService.ExtractionReport;
import org.l2jmobius.tools.dashboard.tools.MapExtractorService.ExtractionRequest;
import org.l2jmobius.tools.dashboard.tools.MapExtractorService.RegionPresets;
import org.l2jmobius.tools.dashboard.tools.MapExtractorService.ValidationResult;
import org.l2jmobius.tools.dashboard.tools.MapPreviewLayer;
import org.l2jmobius.tools.map.TerrainPreviewRenderer;
import org.l2jmobius.tools.map.TerrainPreviewRenderer.PreviewProgress;
import org.l2jmobius.tools.map.TerrainPreviewRenderer.PreviewResult;

/**
 * Provides local Map Extractor scan, preview, extraction, and terrain diagnostic workflows. <br>
 * File-intensive operations participate in the global heavy-work policy without changing the validated map providers or rendering rules.
 * @author BazookaRpm
 */
public final class MapExtractorPage extends JPanel implements DashboardPageLifecycle
{
	private static final DateTimeFormatter REPORT_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HHmmss").withZone(ZoneId.systemDefault());
	private final ControlCenterSettings _settings;
	private final DashboardWorkloadCoordinator _workloads;
	private final MapExtractorService _service = new MapExtractorService();
	private final JTextField _client = new JTextField();
	private final JTextField _reference = new JTextField();
	private final JTextField _output = new JTextField();
	private final JTextField _coordinates = new JTextField("82698 148638");
	private final JCheckBox _geodata = new JCheckBox("Geodata");
	private final JCheckBox _doors = new JCheckBox("Doors");
	private final JCheckBox _water = new JCheckBox("Water zones");
	private final JComboBox<MapVariantOption> _mapVariant = new JComboBox<>();
	private final JSpinner _fromX = new JSpinner(new SpinnerNumberModel(0, -256, 256, 1));
	private final JSpinner _toX = new JSpinner(new SpinnerNumberModel(0, -256, 256, 1));
	private final JSpinner _fromY = new JSpinner(new SpinnerNumberModel(0, -256, 256, 1));
	private final JSpinner _toY = new JSpinner(new SpinnerNumberModel(0, -256, 256, 1));
	private final MapRegionPreviewPanel _preview = new MapRegionPreviewPanel();
	private final JTextArea _log = new LineLimitedTextArea(4000);
	private final JProgressBar _progress = new JProgressBar(0, 100);
	private final ValueCard _processed = new ValueCard("Processed regions", "0 / 0");
	private final ValueCard _outputs = new ValueCard("Outputs generated", "0");
	private final ValueCard _warnings = new ValueCard("Warnings", "0");
	private final ValueCard _errors = new ValueCard("Errors", "0");
	private final ValueCard _elapsed = new ValueCard("Elapsed", "00:00");
	private final ValueCard _eta = new ValueCard("ETA", "--:--");
	private final JLabel _summaryStatus = summaryValue("Not scanned");
	private final JLabel _summaryClient = summaryValue("No");
	private final JLabel _summaryResources = summaryValue("Unknown");
	private final JLabel _summaryOutput = summaryValue("Unknown");
	private final JLabel _summaryMaps = summaryValue("0");
	private final JLabel _summarySelected = summaryValue("0");
	private final JLabel _summaryExisting = summaryValue("0");
	private final JLabel _summaryDisk = summaryValue("-");
	private final JButton _detect = button("Detect", GlyphIcon.Kind.SEARCH, false);
	private final JButton _validate = button("Validate Paths", GlyphIcon.Kind.DIAGNOSTICS, false);
	private final JButton _scan = button("Scan Client", GlyphIcon.Kind.SEARCH, false);
	private final JButton _start = button("Start Extraction", GlyphIcon.Kind.PLAY, true);
	private final JButton _stop = button("Stop", GlyphIcon.Kind.STOP, false);
	private final JButton _export = button("Export Report", GlyphIcon.Kind.SAVE, false);
	private final JButton _advanced = button("Advanced Options", GlyphIcon.Kind.SETTINGS, false);
	private final JButton _previewButton = button("Preview", GlyphIcon.Kind.SEARCH, false);
	private final JButton _save = button("Save Configuration", GlyphIcon.Kind.SAVE, true);
	private final JButton _clearLog = button("Clear Log", GlyphIcon.Kind.TRASH, false);
	private final JButton _openOutput = button("Open Output Folder", GlyphIcon.Kind.FOLDER, false);
	private final AtomicBoolean _stopRequested = new AtomicBoolean();
	private final Runnable _availabilityListener = () -> SwingUtilities.invokeLater(this::refreshMaintenanceAvailability);
	private final Set<String> _selectedRegions = new TreeSet<>();
	private final List<JButton> _configurationButtons = new ArrayList<>();
	private ClientScanResult _scanResult;
	private RegionPresets _presets = new RegionPresets(Set.of(), Set.of(), Set.of(), Set.of());
	private ValidationResult _validation;
	private ExtractionReport _lastReport;
	private PreviewResult _terrainPreview;
	private MapPreviewLayer _clientMapPreview;
	private MapPreviewDialog _previewDialog;
	private boolean _updatingRegionControls;
	private boolean _updatingMapVariants;
	private boolean _busy;
	private boolean _clientMapPreviewLoading;
	
	public MapExtractorPage(ControlCenterSettings settings, DashboardWorkloadCoordinator workloads)
	{
		_settings = settings;
		_workloads = workloads;
		setOpaque(false);
		setLayout(new BorderLayout(0, 8));
		add(DashboardUiFactory.title("Map Extractor", "Extract geodata, doors, water zones and terrain information directly from local Lineage II client data."), BorderLayout.NORTH);
		loadConfiguration();
		final JPanel canvas = new VerticalScrollPanel(new GridBagLayout());
		final GridBagConstraints top = pageConstraints(0, 0);
		canvas.add(buildTopArea(), top);
		final GridBagConstraints execution = pageConstraints(1, 0);
		execution.insets = new Insets(6, 0, 0, 0);
		canvas.add(buildExecutionSection(), execution);
		final GridBagConstraints progress = pageConstraints(2, 1);
		progress.insets = new Insets(6, 0, 0, 0);
		progress.fill = GridBagConstraints.BOTH;
		canvas.add(buildProgressSection(), progress);
		final JScrollPane pageScroll = DashboardUiFactory.scroll(canvas);
		pageScroll.setBorder(BorderFactory.createEmptyBorder());
		pageScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		pageScroll.getVerticalScrollBar().setUnitIncrement(22);
		add(pageScroll, BorderLayout.CENTER);
		wireActions();
		loadPresets();
		updateSummary();
		append("Select a Lineage II client folder, validate the paths, then scan the client to select map regions.");
		refreshMaintenanceAvailability();
	}
	
	@Override
	public void pageShown()
	{
		_workloads.addListener(_availabilityListener);
		refreshMaintenanceAvailability();
	}
	
	@Override
	public void pageHidden()
	{
		_workloads.removeListener(_availabilityListener);
	}
	
	private JPanel buildTopArea()
	{
		final JPanel top = DashboardUiFactory.panel(new GridBagLayout());
		final GridBagConstraints source = topConstraints(0, 0.25);
		top.add(flexibleTopSection(buildSourceSection()), source);
		final GridBagConstraints scope = topConstraints(1, 0.20);
		top.add(flexibleTopSection(buildScopeSection()), scope);
		final GridBagConstraints region = topConstraints(2, 0.35);
		top.add(flexibleTopSection(buildRegionSection()), region);
		final GridBagConstraints summary = topConstraints(3, 0.20);
		top.add(flexibleTopSection(buildSummarySection()), summary);
		return top;
	}
	
	private JPanel buildSourceSection()
	{
		final SurfacePanel section = DashboardUiFactory.section("1. Source configuration");
		configurePathField(_client);
		configurePathField(_reference);
		configurePathField(_output);
		final JPanel form = DashboardUiFactory.panel(new GridBagLayout());
		addFieldRow(form, 0, "Client folder", _client, browseButton(_client));
		addFieldRow(form, 1, "Reference geodata", _reference, browseButton(_reference));
		addFieldRow(form, 2, "Output folder", _output, browseButton(_output));
		final GridBagConstraints detect = fieldConstraints(0, 3, 3, 1);
		detect.insets = new Insets(6, 4, 2, 4);
		form.add(_detect, detect);
		section.add(form, BorderLayout.NORTH);
		return section;
	}
	
	private JPanel buildScopeSection()
	{
		final SurfacePanel section = DashboardUiFactory.section("2. Extraction scope");
		final JPanel options = DashboardUiFactory.panel(new BorderLayout());
		options.setLayout(new BoxLayout(options, BoxLayout.Y_AXIS));
		options.add(option(_geodata, "Generate .l2j navigation geometry."));
		options.add(Box.createVerticalStrut(4));
		options.add(option(_doors, "Extract door locations to XML when present."));
		options.add(Box.createVerticalStrut(4));
		options.add(option(_water, "Extract WaterVolume definitions to XML."));
		options.add(Box.createVerticalStrut(4));
		options.add(mapVariantOption());
		options.add(Box.createVerticalGlue());
		section.add(options, BorderLayout.CENTER);
		return section;
	}
	
	private JPanel buildRegionSection()
	{
		final SurfacePanel section = DashboardUiFactory.section("3. Region selection");
		final JPanel content = DashboardUiFactory.panel(new BorderLayout(0, 5));
		final JPanel ranges = DashboardUiFactory.panel(new GridBagLayout());
		ranges.add(DashboardUiFactory.label("Region grid (X / Y)", Font.BOLD, 10, ControlCenterTheme.MUTED), fieldConstraints(0, 0, 4, 1));
		addSpinnerRow(ranges, 1, "From X", _fromX, "To X", _toX);
		addSpinnerRow(ranges, 2, "From Y", _fromY, "To Y", _toY);
		content.add(ranges, BorderLayout.NORTH);
		final JPanel center = DashboardUiFactory.panel(new BorderLayout(0, 4));
		final JPanel presets = DashboardUiFactory.flow(FlowLayout.LEFT, 5);
		presets.add(presetButton("All"));
		final JButton world = button("World", GlyphIcon.Kind.HOME, false);
		world.addActionListener(event -> selectPreset(_presets.world(), "World"));
		_configurationButtons.add(world);
		final JButton aden = button("Aden", GlyphIcon.Kind.DETAILS, false);
		aden.addActionListener(event -> selectPreset(_presets.aden(), "Aden"));
		_configurationButtons.add(aden);
		final JButton giran = button("Giran", GlyphIcon.Kind.DETAILS, false);
		giran.addActionListener(event -> selectPreset(_presets.giran(), "Giran"));
		_configurationButtons.add(giran);
		final JButton dion = button("Dion", GlyphIcon.Kind.DETAILS, false);
		dion.addActionListener(event -> selectPreset(_presets.dion(), "Dion"));
		_configurationButtons.add(dion);
		presets.add(world);
		presets.add(aden);
		presets.add(giran);
		presets.add(dion);
		center.add(presets, BorderLayout.NORTH);
		final JPanel previewArea = DashboardUiFactory.panel(new BorderLayout(0, 2));
		final JPanel previewHeader = DashboardUiFactory.panel(new BorderLayout(6, 0));
		previewHeader.add(DashboardUiFactory.label("Mini map preview", Font.BOLD, 9, ControlCenterTheme.MUTED), BorderLayout.WEST);
		configureFlexibleActionButton(_previewButton, 72);
		_previewButton.setEnabled(false);
		previewHeader.add(_previewButton, BorderLayout.EAST);
		previewArea.add(previewHeader, BorderLayout.NORTH);
		previewArea.add(_preview, BorderLayout.CENTER);
		center.add(previewArea, BorderLayout.CENTER);
		content.add(center, BorderLayout.CENTER);
		section.add(content, BorderLayout.CENTER);
		return section;
	}
	
	private JPanel buildSummarySection()
	{
		final SurfacePanel section = DashboardUiFactory.section("Summary");
		final JPanel content = DashboardUiFactory.panel(new BorderLayout(0, 8));
		final JPanel rows = DashboardUiFactory.panel(new GridBagLayout());
		addSummaryRow(rows, 0, "Status", _summaryStatus);
		addSummaryRow(rows, 1, "Client detected", _summaryClient);
		addSummaryRow(rows, 2, "Geodata resources", _summaryResources);
		addSummaryRow(rows, 3, "Output writable", _summaryOutput);
		addSummaryRow(rows, 4, "Map tiles", _summaryMaps);
		addSummaryRow(rows, 5, "Selected regions", _summarySelected);
		addSummaryRow(rows, 6, "Existing geodata", _summaryExisting);
		addSummaryRow(rows, 7, "Free disk space", _summaryDisk);
		content.add(rows, BorderLayout.NORTH);
		content.add(_save, BorderLayout.SOUTH);
		section.add(content, BorderLayout.CENTER);
		return section;
	}
	
	private JPanel buildExecutionSection()
	{
		final SurfacePanel section = DashboardUiFactory.section("4. Execution");
		final JPanel row = DashboardUiFactory.panel(new GridBagLayout());
		configureFlexibleActionButton(_validate, 90);
		configureFlexibleActionButton(_scan, 85);
		configureFlexibleActionButton(_start, 100);
		configureFlexibleActionButton(_stop, 70);
		configureFlexibleActionButton(_export, 90);
		configureFlexibleActionButton(_advanced, 110);
		int column = 0;
		row.add(_validate, executionConstraints(column++));
		row.add(_scan, executionConstraints(column++));
		row.add(_start, executionConstraints(column++));
		row.add(_stop, executionConstraints(column++));
		row.add(_export, executionConstraints(column++));
		final GridBagConstraints spacer = executionConstraints(column++);
		spacer.weightx = 1;
		spacer.fill = GridBagConstraints.HORIZONTAL;
		row.add(Box.createHorizontalGlue(), spacer);
		final GridBagConstraints advanced = executionConstraints(column);
		advanced.insets = new Insets(0, 8, 0, 0);
		row.add(_advanced, advanced);
		section.add(row, BorderLayout.CENTER);
		_stop.setEnabled(false);
		_export.setEnabled(false);
		return section;
	}
	
	private JPanel buildProgressSection()
	{
		final SurfacePanel section = DashboardUiFactory.section("5. Progress & Output");
		final JPanel content = DashboardUiFactory.panel(new BorderLayout(0, 5));
		final JPanel progressArea = DashboardUiFactory.panel(new BorderLayout(0, 3));
		progressArea.add(DashboardUiFactory.label("Overall progress", Font.PLAIN, 9, ControlCenterTheme.MUTED), BorderLayout.NORTH);
		_progress.setStringPainted(true);
		_progress.setValue(0);
		progressArea.add(_progress, BorderLayout.CENTER);
		content.add(progressArea, BorderLayout.NORTH);
		final JPanel center = DashboardUiFactory.panel(new BorderLayout(0, 5));
		final JPanel cards = DashboardUiFactory.panel(new GridLayout(1, 6, 5, 0));
		cards.add(_processed);
		cards.add(_outputs);
		cards.add(_warnings);
		cards.add(_errors);
		cards.add(_elapsed);
		cards.add(_eta);
		center.add(cards, BorderLayout.NORTH);
		_log.setEditable(false);
		_log.setFont(ControlCenterTheme.font(Font.PLAIN, 11));
		final JScrollPane logScroll = DashboardUiFactory.scroll(_log);
		logScroll.setPreferredSize(new Dimension(0, 112));
		logScroll.setMinimumSize(new Dimension(0, 82));
		center.add(logScroll, BorderLayout.CENTER);
		content.add(center, BorderLayout.CENTER);
		final JPanel footer = DashboardUiFactory.flow(FlowLayout.LEFT, 8);
		footer.add(_clearLog);
		footer.add(_openOutput);
		content.add(footer, BorderLayout.SOUTH);
		section.add(content, BorderLayout.CENTER);
		return section;
	}
	
	private void wireActions()
	{
		_detect.addActionListener(event -> scanClient("Detecting client"));
		_validate.addActionListener(event -> validatePaths(true));
		_scan.addActionListener(event -> scanClient("Scanning client"));
		_start.addActionListener(event -> startExtraction());
		_stop.addActionListener(event -> requestStop());
		_export.addActionListener(event -> exportReport());
		_advanced.addActionListener(event -> showAdvancedOptions());
		_previewButton.addActionListener(event -> showTerrainPreview());
		_save.addActionListener(event -> saveConfiguration());
		_clearLog.addActionListener(event -> _log.setText(""));
		_openOutput.addActionListener(event -> openOutputFolder());
		_mapVariant.addActionListener(event ->
		{
			if (_updatingMapVariants)
			{
				return;
			}
			
			invalidateTerrainPreview();
			refreshAvailableRegions();
		});
		_geodata.addActionListener(event -> updateSummary());
		_fromX.addChangeListener(event -> selectRange());
		_toX.addChangeListener(event -> selectRange());
		_fromY.addChangeListener(event -> selectRange());
		_toY.addChangeListener(event -> selectRange());
		_preview.setSelectionListener(this::toggleRegion);
	}
	
	private void loadConfiguration()
	{
		_client.setText(_settings.getMapExtractorClientPath());
		_reference.setText(_settings.getMapExtractorReferenceGeodataPath());
		_output.setText(_settings.getMapExtractorOutputPath());
		_geodata.setSelected(_settings.isMapExtractorGeodataEnabled());
		_doors.setSelected(_settings.isMapExtractorDoorsEnabled());
		_water.setSelected(_settings.isMapExtractorWaterEnabled());
		updateMapVariantOptions(null, _settings.getMapExtractorMapVariant());
	}
	
	private void loadPresets()
	{
		final Path mapRegionDirectory = _settings.getDistributionRoot().resolve("game/data/mapregion");
		new SwingWorker<RegionPresets, Void>()
		{
			@Override
			protected RegionPresets doInBackground() throws Exception
			{
				return _service.loadRegionPresets(mapRegionDirectory);
			}
			
			@Override
			protected void done()
			{
				try
				{
					_presets = get();
				}
				catch (Exception e)
				{
					append("[WARN] Server map-region presets could not be loaded: " + safeMessage(e));
				}
			}
		}.execute();
	}
	
	private void validatePaths(boolean showDialog)
	{
		_validation = null;
		try
		{
			_validation = _service.validatePaths(requiredPath(_client.getText(), "Client folder"), requiredPath(_output.getText(), "Output folder"), optionalPath(_reference.getText()), _geodata.isSelected());
			updateSummary();
			for (String issue : _validation.issues())
			{
				append("[WARN] " + issue);
			}
			
			if (_validation.isReady(_geodata.isSelected()))
			{
				append("[INFO] Map Extractor paths validated successfully.");
				if (showDialog)
				{
					JOptionPane.showMessageDialog(this, "Client and output paths are ready.", "Map Extractor", JOptionPane.INFORMATION_MESSAGE);
				}
			}
			else if (showDialog)
			{
				JOptionPane.showMessageDialog(this, _validation.issues().isEmpty() ? "Paths are not ready." : String.join(System.lineSeparator(), _validation.issues()), "Map Extractor validation", JOptionPane.WARNING_MESSAGE);
			}
		}
		catch (Exception e)
		{
			showFailure("Path validation failed", e);
		}
	}
	
	private void scanClient(String status)
	{
		if (_busy)
		{
			return;
		}
		
		final Path clientRoot;
		final Path outputRoot;
		try
		{
			clientRoot = requiredPath(_client.getText(), "Client folder");
			outputRoot = requiredPath(_output.getText(), "Output folder");
		}
		catch (Exception e)
		{
			showFailure("Client scan failed", e);
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Map client scan");
		if (permit == null)
		{
			return;
		}
		
		setBusy(true, false);
		_summaryStatus.setText(status + "...");
		new SwingWorker<ClientScanResult, Void>()
		{
			@Override
			protected ClientScanResult doInBackground() throws Exception
			{
				return _service.scanClient(clientRoot, outputRoot);
			}
			
			@Override
			protected void done()
			{
				try
				{
					invalidateTerrainPreview();
					_scanResult = get();
					updateMapVariantOptions(_scanResult, selectedMapVariant());
					applyAllRegions();
					try
					{
						_validation = _service.validatePaths(clientRoot, outputRoot, optionalPath(_reference.getText()), _geodata.isSelected());
					}
					catch (Exception e)
					{
						_validation = null;
						append("[WARN] Client was scanned, but path validation could not complete: " + safeMessage(e));
					}
					
					updateSummary();
					append("[INFO] Client scan completed: " + _scanResult.regions().size() + " map regions" + variantSummary(_scanResult) + ".");
				}
				catch (Exception e)
				{
					showFailure("Client scan failed", e);
				}
				finally
				{
					setBusy(false, false);
					permit.close();
				}
			}
		}.execute();
	}
	
	private void showTerrainPreview()
	{
		if (_busy)
		{
			return;
		}
		
		if (_scanResult == null)
		{
			JOptionPane.showMessageDialog(this, "Scan the client before building the terrain preview.", "Map Extractor", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		final String mapVariant = selectedMapVariant();
		final List<String> regions = _scanResult.availableRegions(mapVariant);
		if (regions.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "The scanned client does not contain previewable map regions.", "Map Extractor", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		final Path clientRoot;
		try
		{
			clientRoot = requiredPath(_client.getText(), "Client folder");
		}
		catch (Exception e)
		{
			showFailure("Terrain preview failed", e);
			return;
		}
		
		final Window owner = SwingUtilities.getWindowAncestor(this);
		_previewDialog = new MapPreviewDialog(owner, _scanResult, regions, _selectedRegions, this::toggleRegion);
		if (_clientMapPreview != null)
		{
			_previewDialog.setClientMapPreview(_clientMapPreview);
		}
		
		if (_terrainPreview != null)
		{
			_previewDialog.setPreview(_terrainPreview, _selectedRegions);
		}
		
		int taskCount = 0;
		if (_clientMapPreview == null)
		{
			taskCount++;
		}
		
		if (_terrainPreview == null)
		{
			taskCount++;
		}
		
		if (taskCount == 0)
		{
			_previewDialog.setVisible(true);
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Map preview");
		if (permit == null)
		{
			_previewDialog.dispose();
			_previewDialog = null;
			return;
		}
		
		final AtomicInteger remainingTasks = new AtomicInteger(taskCount);
		final Runnable taskCompleted = () ->
		{
			if (remainingTasks.decrementAndGet() == 0)
			{
				setBusy(false, false);
				permit.close();
			}
		};
		setBusy(true, false);
		_previewDialog.setVisible(true);
		
		if (_clientMapPreview == null)
		{
			loadClientMapPreview(clientRoot, _scanResult, taskCompleted);
		}
		
		if (_terrainPreview != null)
		{
			return;
		}
		
		final Map<String, String> mapSources = _scanResult.sourceMapNames(mapVariant);
		append("[INFO] Building terrain preview from " + regions.size() + " client map regions.");
		new SwingWorker<PreviewResult, PreviewProgress>()
		{
			@Override
			protected PreviewResult doInBackground()
			{
				final TerrainPreviewRenderer renderer = new TerrainPreviewRenderer();
				return renderer.render(clientRoot, regions, mapSources, this::isCancelled, this::publish);
			}
			
			@Override
			protected void process(List<PreviewProgress> chunks)
			{
				if ((_previewDialog != null) && _previewDialog.isDisplayable() && !chunks.isEmpty())
				{
					_previewDialog.setProgress(chunks.get(chunks.size() - 1));
				}
			}
			
			@Override
			protected void done()
			{
				try
				{
					final PreviewResult result = get();
					final Path activeClient = requiredPath(_client.getText(), "Client folder");
					if (!activeClient.equals(clientRoot))
					{
						append("[WARN] Terrain preview result was discarded because the client path changed during rendering.");
						if ((_previewDialog != null) && _previewDialog.isDisplayable())
						{
							_previewDialog.setFailure("Client path changed. Build the preview again.");
						}
						return;
					}
					
					_terrainPreview = result;
					_preview.setTerrainPreview(result);
					if ((_previewDialog != null) && _previewDialog.isDisplayable())
					{
						_previewDialog.setPreview(result, _selectedRegions);
					}
					
					append("[INFO] Terrain preview completed: " + result.renderedRegions() + " regions rendered, " + result.failedRegions().size() + " warnings.");
					final int warningLimit = Math.min(5, result.failedRegions().size());
					for (int index = 0; index < warningLimit; index++)
					{
						append("[WARN] Preview " + result.failedRegions().get(index));
					}
					
					if (result.failedRegions().size() > warningLimit)
					{
						append("[WARN] " + (result.failedRegions().size() - warningLimit) + " additional terrain-preview warnings were omitted from the console.");
					}
				}
				catch (Exception e)
				{
					final Throwable cause = e.getCause() == null ? e : e.getCause();
					append("[ERROR] Terrain preview failed: " + safeMessage(cause));
					if ((_previewDialog != null) && _previewDialog.isDisplayable())
					{
						_previewDialog.setFailure(safeMessage(cause));
					}
				}
				finally
				{
					taskCompleted.run();
				}
			}
		}.execute();
	}
	
	private void loadClientMapPreview(Path clientRoot, ClientScanResult scanResult, Runnable completion)
	{
		if (_clientMapPreviewLoading)
		{
			completion.run();
			return;
		}
		
		_clientMapPreviewLoading = true;
		new SwingWorker<MapPreviewLayer, Void>()
		{
			@Override
			protected MapPreviewLayer doInBackground() throws Exception
			{
				MapPreviewLayer result = new EarlyClientMapPreviewProvider().load(clientRoot);
				if (result == null)
				{
					result = new GraciaAdenClientMapPreviewProvider().load(clientRoot);
				}
				
				if (result == null)
				{
					result = new EssenceClientMapPreviewProvider().load(clientRoot);
				}
				
				if (result == null)
				{
					result = new ClassicClientMapPreviewProvider().load(clientRoot);
				}
				
				if (result == null)
				{
					result = new LiveClientMapPreviewProvider().load(clientRoot);
				}
				
				return result;
			}
			
			@Override
			protected void done()
			{
				try
				{
					if (_scanResult != scanResult)
					{
						return;
					}
					
					final Path activeClient = requiredPath(_client.getText(), "Client folder");
					if (!activeClient.equals(clientRoot))
					{
						return;
					}
					
					final MapPreviewLayer result = get();
					if (result == null)
					{
						append("[INFO] Compatible Client Map resources were not detected. Terrain preview remains available.");
						return;
					}
					
					_clientMapPreview = result;
					if ((_previewDialog != null) && _previewDialog.isDisplayable())
					{
						_previewDialog.setClientMapPreview(result);
					}
					
					append("[INFO] Client Map preview loaded: " + result.source() + " (" + result.image().getWidth() + "x" + result.image().getHeight() + ", regions " + result.minimumX() + "_" + result.minimumY() + " to " + result.maximumX() + "_" + result.maximumY() + ").");
				}
				catch (Exception e)
				{
					final Throwable cause = e.getCause() == null ? e : e.getCause();
					append("[WARN] Client Map preview is unavailable: " + safeMessage(cause));
				}
				finally
				{
					_clientMapPreviewLoading = false;
					completion.run();
				}
			}
		}.execute();
	}
	
	private void invalidateTerrainPreview()
	{
		_terrainPreview = null;
		_clientMapPreview = null;
		_preview.setTerrainPreview(null);
		if ((_previewDialog != null) && _previewDialog.isDisplayable())
		{
			_previewDialog.dispose();
		}
		
		_previewDialog = null;
	}
	
	private void startExtraction()
	{
		if (_busy)
		{
			return;
		}
		
		if (_scanResult == null)
		{
			JOptionPane.showMessageDialog(this, "Scan the client before starting extraction.", "Map Extractor", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		if (_selectedRegions.isEmpty())
		{
			JOptionPane.showMessageDialog(this, "Select at least one available map region.", "Map Extractor", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		if (!_geodata.isSelected() && !_doors.isSelected() && !_water.isSelected())
		{
			JOptionPane.showMessageDialog(this, "Select Geodata, Doors or Water zones.", "Map Extractor", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		validatePaths(false);
		if ((_validation == null) || !_validation.isReady(_geodata.isSelected()))
		{
			JOptionPane.showMessageDialog(this, "The configured paths are not ready. Run Validate Paths for details.", "Map Extractor", JOptionPane.WARNING_MESSAGE);
			return;
		}
		
		final ExtractionRequest request;
		try
		{
			request = new ExtractionRequest(requiredPath(_client.getText(), "Client folder"), requiredPath(_output.getText(), "Output folder"), List.copyOf(_selectedRegions), _geodata.isSelected(), _doors.isSelected(), _water.isSelected(), selectedMapVariant());
		}
		catch (Exception e)
		{
			showFailure("Map extraction failed", e);
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Map extraction");
		if (permit == null)
		{
			return;
		}
		
		_stopRequested.set(false);
		_lastReport = null;
		_export.setEnabled(false);
		resetProgress(request.regions().size());
		setBusy(true, true);
		new SwingWorker<ExtractionReport, WorkerUpdate>()
		{
			@Override
			protected ExtractionReport doInBackground() throws Exception
			{
				return _service.extract(request, _stopRequested::get, progress -> publish(new WorkerUpdate(null, progress)), message -> publish(new WorkerUpdate(message, null)));
			}
			
			@Override
			protected void process(List<WorkerUpdate> chunks)
			{
				for (WorkerUpdate update : chunks)
				{
					if (update.message() != null)
					{
						append(update.message());
					}
					
					if (update.progress() != null)
					{
						applyProgress(update.progress());
					}
				}
			}
			
			@Override
			protected void done()
			{
				boolean refreshExisting = false;
				try
				{
					_lastReport = get();
					_export.setEnabled(true);
					if (_lastReport.stopped())
					{
						_summaryStatus.setText("Stopped");
						_summaryStatus.setForeground(ControlCenterTheme.YELLOW);
					}
					else if (_lastReport.errors() > 0)
					{
						_summaryStatus.setText("Completed with errors");
						_summaryStatus.setForeground(ControlCenterTheme.RED);
					}
					else
					{
						_summaryStatus.setText("Completed");
						_summaryStatus.setForeground(ControlCenterTheme.GREEN);
					}
					
					append("[INFO] Map extraction operation finished.");
					refreshExisting = true;
				}
				catch (Exception e)
				{
					showFailure("Map extraction failed", e);
				}
				finally
				{
					permit.close();
					setBusy(false, true);
				}
				
				if (refreshExisting)
				{
					refreshExistingGeodata();
				}
			}
		}.execute();
	}
	
	private void requestStop()
	{
		if (!_busy)
		{
			return;
		}
		
		_stopRequested.set(true);
		_stop.setText("Stopping...");
		_stop.setEnabled(false);
		_summaryStatus.setText("Stopping...");
		_summaryStatus.setForeground(ControlCenterTheme.YELLOW);
		append("[INFO] Stop requested. Cancellation will occur at the next safe extraction checkpoint.");
	}
	
	private void exportReport()
	{
		if (_lastReport == null)
		{
			JOptionPane.showMessageDialog(this, "No extraction report is available yet.", "Map Extractor", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		final JFileChooser chooser = new JFileChooser(_output.getText().isBlank() ? "." : _output.getText());
		chooser.setSelectedFile(new File("MapExtractor_Report_" + REPORT_TIME.format(Instant.now()) + ".txt"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		
		try
		{
			_service.writeReport(_lastReport, chooser.getSelectedFile().toPath());
			append("[INFO] Report exported to " + chooser.getSelectedFile() + ".");
		}
		catch (Exception e)
		{
			showFailure("Report export failed", e);
		}
	}
	
	private void saveConfiguration()
	{
		_settings.set(ControlCenterSettings.MAP_EXTRACTOR_CLIENT_PATH, _client.getText().trim());
		_settings.set(ControlCenterSettings.MAP_EXTRACTOR_REFERENCE_GEODATA_PATH, _reference.getText().trim());
		_settings.set(ControlCenterSettings.MAP_EXTRACTOR_OUTPUT_PATH, _output.getText().trim());
		_settings.set(ControlCenterSettings.MAP_EXTRACTOR_GENERATE_GEODATA, _geodata.isSelected());
		_settings.set(ControlCenterSettings.MAP_EXTRACTOR_GENERATE_DOORS, _doors.isSelected());
		_settings.set(ControlCenterSettings.MAP_EXTRACTOR_GENERATE_WATER, _water.isSelected());
		_settings.set(ControlCenterSettings.MAP_EXTRACTOR_MAP_VARIANT, selectedMapVariant());
		try
		{
			SettingsRepository.save(_settings);
			append("[INFO] Map Extractor configuration saved.");
		}
		catch (Exception e)
		{
			showFailure("Configuration save failed", e);
		}
	}
	
	private void showAdvancedOptions()
	{
		final Window owner = SwingUtilities.getWindowAncestor(this);
		final JDialog dialog = new JDialog(owner, "Map Extractor - Advanced Options", JDialog.ModalityType.MODELESS);
		final JPanel root = DashboardUiFactory.panel(new BorderLayout(0, 8));
		root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
		final SurfacePanel height = DashboardUiFactory.section("Terrain diagnostics");
		final JPanel form = DashboardUiFactory.panel(new GridBagLayout());
		addFieldRow(form, 0, "World X Y", _coordinates, null);
		final JPanel actions = DashboardUiFactory.flow(FlowLayout.LEFT, 8);
		final JButton query = button("Height Info", GlyphIcon.Kind.INFO, false);
		final JButton dump = button("Dump Height Map", GlyphIcon.Kind.SAVE, false);
		query.addActionListener(event -> queryHeight());
		dump.addActionListener(event -> dumpHeightMap());
		actions.add(query);
		actions.add(dump);
		final GridBagConstraints actionConstraints = fieldConstraints(0, 1, 3, 1);
		actionConstraints.insets = new Insets(8, 4, 4, 4);
		form.add(actions, actionConstraints);
		height.add(form, BorderLayout.CENTER);
		root.add(height, BorderLayout.CENTER);
		root.add(DashboardUiFactory.label("Height Info reads terrain, BSP and static-mesh surfaces. Dump Height Map writes the selected terrain map to a text file.", Font.PLAIN, 10, ControlCenterTheme.MUTED), BorderLayout.SOUTH);
		dialog.setContentPane(root);
		dialog.setSize(620, 230);
		dialog.setLocationRelativeTo(this);
		dialog.setVisible(true);
	}
	
	private void queryHeight()
	{
		final Path clientRoot;
		try
		{
			clientRoot = requiredPath(_client.getText(), "Client folder");
		}
		catch (Exception e)
		{
			showFailure("Height query failed", e);
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Terrain height query");
		if (permit == null)
		{
			return;
		}
		
		final String coordinates = _coordinates.getText().trim();
		final String mapVariant = selectedMapVariant();
		new SwingWorker<String, Void>()
		{
			@Override
			protected String doInBackground() throws Exception
			{
				return _service.queryHeight(clientRoot, coordinates, mapVariant);
			}
			
			@Override
			protected void done()
			{
				try
				{
					append(get());
				}
				catch (Exception e)
				{
					showFailure("Height query failed", e);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private void dumpHeightMap()
	{
		final JFileChooser chooser = new JFileChooser(_output.getText().isBlank() ? "." : _output.getText());
		chooser.setSelectedFile(new File("map_heights.txt"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}
		
		final Path clientRoot;
		try
		{
			clientRoot = requiredPath(_client.getText(), "Client folder");
		}
		catch (Exception e)
		{
			showFailure("Map dump failed", e);
			return;
		}
		
		final WorkloadPermit permit = acquireHeavy("Terrain height dump");
		if (permit == null)
		{
			return;
		}
		
		final String coordinates = _coordinates.getText().trim();
		final String mapVariant = selectedMapVariant();
		final Path outputFile = chooser.getSelectedFile().toPath();
		new SwingWorker<Void, Void>()
		{
			@Override
			protected Void doInBackground() throws Exception
			{
				_service.dumpHeightMap(clientRoot, coordinates, mapVariant, outputFile);
				return null;
			}
			
			@Override
			protected void done()
			{
				try
				{
					get();
					append("[INFO] Height map written to " + chooser.getSelectedFile() + ".");
				}
				catch (Exception e)
				{
					showFailure("Map dump failed", e);
				}
				finally
				{
					permit.close();
				}
			}
		}.execute();
	}
	
	private void openOutputFolder()
	{
		try
		{
			final Path output = requiredPath(_output.getText(), "Output folder");
			Files.createDirectories(output);
			if (!Desktop.isDesktopSupported())
			{
				throw new IllegalStateException("Desktop folder opening is not supported by this runtime.");
			}
			
			Desktop.getDesktop().open(output.toFile());
		}
		catch (Exception e)
		{
			showFailure("Open output folder failed", e);
		}
	}
	
	private void applyAllRegions()
	{
		if (_scanResult == null)
		{
			return;
		}
		
		_selectedRegions.clear();
		_selectedRegions.addAll(_scanResult.availableRegions(selectedMapVariant()));
		updateRangeControlsFromSelection();
		updatePreviewAndSummary();
	}
	
	private void refreshAvailableRegions()
	{
		if (_scanResult == null)
		{
			updateSummary();
			return;
		}
		
		final Set<String> available = new HashSet<>(_scanResult.availableRegions(selectedMapVariant()));
		_selectedRegions.retainAll(available);
		if (_selectedRegions.isEmpty())
		{
			_selectedRegions.addAll(available);
		}
		
		updateRangeControlsFromSelection();
		updatePreviewAndSummary();
	}
	
	private void selectPreset(Set<String> preset, String name)
	{
		if (_busy)
		{
			return;
		}
		
		if (_scanResult == null)
		{
			JOptionPane.showMessageDialog(this, "Scan the client before selecting a preset.", "Map Extractor", JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		final Set<String> available = new HashSet<>(_scanResult.availableRegions(selectedMapVariant()));
		_selectedRegions.clear();
		for (String region : preset)
		{
			if (available.contains(region))
			{
				_selectedRegions.add(region);
			}
		}
		
		if (_selectedRegions.isEmpty())
		{
			append("[WARN] Preset " + name + " has no regions available in the scanned client.");
		}
		else
		{
			append("[INFO] Preset " + name + " selected " + _selectedRegions.size() + " regions.");
		}
		
		updateRangeControlsFromSelection();
		updatePreviewAndSummary();
	}
	
	private JButton presetButton(String text)
	{
		final JButton button = button(text, GlyphIcon.Kind.HOME, false);
		button.addActionListener(event -> applyAllRegions());
		_configurationButtons.add(button);
		return button;
	}
	
	private void selectRange()
	{
		if (_busy || _updatingRegionControls || (_scanResult == null))
		{
			return;
		}
		
		int fromX = (Integer) _fromX.getValue();
		int toX = (Integer) _toX.getValue();
		int fromY = (Integer) _fromY.getValue();
		int toY = (Integer) _toY.getValue();
		if (fromX > toX)
		{
			final int swap = fromX;
			fromX = toX;
			toX = swap;
		}
		
		if (fromY > toY)
		{
			final int swap = fromY;
			fromY = toY;
			toY = swap;
		}
		
		_selectedRegions.clear();
		for (String region : _scanResult.availableRegions(selectedMapVariant()))
		{
			final int[] coordinates = parseRegion(region);
			if ((coordinates[0] >= fromX) && (coordinates[0] <= toX) && (coordinates[1] >= fromY) && (coordinates[1] <= toY))
			{
				_selectedRegions.add(region);
			}
		}
		
		updatePreviewAndSummary();
	}
	
	private void toggleRegion(String region)
	{
		if (_busy)
		{
			return;
		}
		
		if (_selectedRegions.contains(region))
		{
			_selectedRegions.remove(region);
		}
		else
		{
			_selectedRegions.add(region);
		}
		
		updateRangeControlsFromSelection();
		updatePreviewAndSummary();
	}
	
	private void updateRangeControlsFromSelection()
	{
		if ((_scanResult == null) || _selectedRegions.isEmpty())
		{
			return;
		}
		
		int minimumX = Integer.MAX_VALUE;
		int maximumX = Integer.MIN_VALUE;
		int minimumY = Integer.MAX_VALUE;
		int maximumY = Integer.MIN_VALUE;
		for (String region : _selectedRegions)
		{
			final int[] coordinates = parseRegion(region);
			minimumX = Math.min(minimumX, coordinates[0]);
			maximumX = Math.max(maximumX, coordinates[0]);
			minimumY = Math.min(minimumY, coordinates[1]);
			maximumY = Math.max(maximumY, coordinates[1]);
		}
		
		_updatingRegionControls = true;
		_fromX.setValue(minimumX);
		_toX.setValue(maximumX);
		_fromY.setValue(minimumY);
		_toY.setValue(maximumY);
		_updatingRegionControls = false;
	}
	
	private void updatePreviewAndSummary()
	{
		if (_scanResult != null)
		{
			_preview.setData(_scanResult, _scanResult.availableRegions(selectedMapVariant()), _selectedRegions);
		}
		
		if ((_previewDialog != null) && _previewDialog.isDisplayable())
		{
			_previewDialog.setSelection(_selectedRegions);
		}
		
		updateSummary();
	}
	
	private void updateSummary()
	{
		if (_validation == null)
		{
			_summaryStatus.setText(_scanResult == null ? "Not scanned" : "Scanned");
			_summaryStatus.setForeground(ControlCenterTheme.TEXT);
			_summaryClient.setText(_scanResult == null ? "No" : "Yes");
			_summaryResources.setText("Unknown");
			_summaryOutput.setText("Unknown");
			_summaryDisk.setText("-");
		}
		else
		{
			final boolean ready = _validation.isReady(_geodata.isSelected());
			_summaryStatus.setText(ready && (_scanResult != null) && !_selectedRegions.isEmpty() ? "Ready" : "Needs attention");
			_summaryStatus.setForeground(ready ? ControlCenterTheme.GREEN : ControlCenterTheme.YELLOW);
			_summaryClient.setText(_validation.mapsFound() ? "Yes" : "No");
			_summaryClient.setForeground(_validation.mapsFound() ? ControlCenterTheme.GREEN : ControlCenterTheme.RED);
			if (_geodata.isSelected())
			{
				final boolean resources = _validation.texturesFound() && _validation.staticMeshesFound();
				_summaryResources.setText(resources ? "Yes" : "No");
				_summaryResources.setForeground(resources ? ControlCenterTheme.GREEN : ControlCenterTheme.RED);
			}
			else
			{
				_summaryResources.setText("Not required");
				_summaryResources.setForeground(ControlCenterTheme.MUTED);
			}
			
			_summaryOutput.setText(_validation.outputWritable() ? "Yes" : "No");
			_summaryOutput.setForeground(_validation.outputWritable() ? ControlCenterTheme.GREEN : ControlCenterTheme.RED);
			_summaryDisk.setText(formatBytes(_validation.freeDiskBytes()));
		}
		
		_summaryMaps.setText(_scanResult == null ? "0" : String.valueOf(_scanResult.availableRegions(selectedMapVariant()).size()));
		_summarySelected.setText(String.valueOf(_selectedRegions.size()));
		_summaryExisting.setText(_scanResult == null ? "0" : String.valueOf(_scanResult.existingGeodata()));
	}
	
	private void refreshExistingGeodata()
	{
		if (_scanResult == null)
		{
			return;
		}
		
		final Path outputRoot;
		try
		{
			outputRoot = requiredPath(_output.getText(), "Output folder");
		}
		catch (Exception e)
		{
			append("[WARN] Existing geodata count could not be refreshed: " + safeMessage(e));
			return;
		}
		
		new SwingWorker<Integer, Void>()
		{
			@Override
			protected Integer doInBackground() throws Exception
			{
				return _service.countExistingGeodata(outputRoot);
			}
			
			@Override
			protected void done()
			{
				try
				{
					_scanResult = _scanResult.withExistingGeodata(get());
					updateSummary();
				}
				catch (Exception e)
				{
					append("[WARN] Existing geodata count could not be refreshed: " + safeMessage(e));
				}
			}
		}.execute();
	}
	
	private void resetProgress(int totalRegions)
	{
		_progress.setValue(0);
		_progress.setString("0%");
		_processed.setValue("0 / " + totalRegions);
		_outputs.setValue("0");
		_warnings.setValue("0");
		_errors.setValue("0");
		_elapsed.setValue("00:00");
		_eta.setValue("--:--");
		_warnings.setAccent(ControlCenterTheme.METRIC);
		_errors.setAccent(ControlCenterTheme.METRIC);
	}
	
	private void applyProgress(ExtractionProgress progress)
	{
		final int percent = progress.totalRegions() == 0 ? 0 : (int) Math.round((progress.processedRegions() * 100.0) / progress.totalRegions());
		_progress.setValue(Math.max(0, Math.min(100, percent)));
		_progress.setString(percent + "%" + (progress.currentRegion().isBlank() ? "" : " - " + progress.currentRegion()));
		_processed.setValue(progress.processedRegions() + " / " + progress.totalRegions());
		_outputs.setValue(String.valueOf(progress.outputsGenerated()));
		_warnings.setValue(String.valueOf(progress.warnings()));
		_errors.setValue(String.valueOf(progress.errors()));
		_elapsed.setValue(formatDuration(progress.elapsedMillis()));
		_eta.setValue(progress.etaMillis() < 0 ? "--:--" : formatDuration(progress.etaMillis()));
		_warnings.setAccent(progress.warnings() > 0 ? ControlCenterTheme.YELLOW : ControlCenterTheme.METRIC);
		_errors.setAccent(progress.errors() > 0 ? ControlCenterTheme.RED : ControlCenterTheme.GREEN);
		_summaryStatus.setText(progress.status() + (progress.currentRegion().isBlank() ? "" : " " + progress.currentRegion()));
		_summaryStatus.setForeground(ControlCenterTheme.ACCENT_HOVER);
	}
	
	private void setBusy(boolean busy, boolean extraction)
	{
		_busy = busy;
		_stop.setText("Stop");
		_stop.setEnabled(busy && extraction);
		setConfigurationEnabled(!busy);
		if (!busy)
		{
			_export.setEnabled(_lastReport != null);
		}
		
		refreshMaintenanceAvailability();
	}
	
	private void setConfigurationEnabled(boolean enabled)
	{
		_client.setEnabled(enabled);
		_reference.setEnabled(enabled);
		_output.setEnabled(enabled);
		_coordinates.setEnabled(enabled);
		_geodata.setEnabled(enabled);
		_doors.setEnabled(enabled);
		_water.setEnabled(enabled);
		_mapVariant.setEnabled(enabled);
		_fromX.setEnabled(enabled);
		_toX.setEnabled(enabled);
		_fromY.setEnabled(enabled);
		_toY.setEnabled(enabled);
		_preview.setEnabled(enabled);
		if ((_previewDialog != null) && _previewDialog.isDisplayable())
		{
			_previewDialog.setEnabled(enabled);
		}
		
		for (JButton button : _configurationButtons)
		{
			button.setEnabled(enabled);
		}
	}
	
	private void refreshMaintenanceAvailability()
	{
		final boolean maintenanceAvailable = !_busy && _workloads.isMaintenanceAvailable();
		_detect.setEnabled(maintenanceAvailable);
		_validate.setEnabled(maintenanceAvailable);
		_scan.setEnabled(maintenanceAvailable);
		_start.setEnabled(maintenanceAvailable);
		_advanced.setEnabled(maintenanceAvailable);
		_previewButton.setEnabled(maintenanceAvailable && (_scanResult != null));
		_save.setEnabled(!_busy);
	}
	
	private WorkloadPermit acquireHeavy(String operation)
	{
		final WorkloadPermit permit = _workloads.tryAcquireHeavy(operation);
		if (permit == null)
		{
			showMaintenanceBlocked();
		}
		
		return permit;
	}
	
	private void showMaintenanceBlocked()
	{
		final String reason = _workloads.maintenanceBlockReason();
		JOptionPane.showMessageDialog(this, reason.isBlank() ? "Another maintenance operation is active." : reason, "Map Extractor unavailable", JOptionPane.WARNING_MESSAGE);
		refreshMaintenanceAvailability();
	}
	
	private JButton browseButton(JTextField target)
	{
		final JButton button = button("Browse", GlyphIcon.Kind.FOLDER, false);
		_configurationButtons.add(button);
		button.addActionListener(event ->
		{
			final JFileChooser chooser = new JFileChooser(target.getText().isBlank() ? "." : target.getText());
			chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
			{
				target.setText(chooser.getSelectedFile().getAbsolutePath());
				_validation = null;
				if (target == _client)
				{
					invalidateTerrainPreview();
					_scanResult = null;
					_selectedRegions.clear();
					updateMapVariantOptions(null, selectedMapVariant());
					_previewButton.setEnabled(false);
				}
				
				updateSummary();
			}
		});
		
		return button;
	}
	
	private JPanel mapVariantOption()
	{
		final JPanel panel = DashboardUiFactory.panel(new BorderLayout(6, 3));
		final JPanel selector = DashboardUiFactory.panel(new BorderLayout(6, 0));
		selector.add(DashboardUiFactory.label("Map variant", Font.PLAIN, 11, ControlCenterTheme.TEXT), BorderLayout.WEST);
		selector.add(_mapVariant, BorderLayout.CENTER);
		panel.add(selector, BorderLayout.NORTH);
		final JLabel detail = DashboardUiFactory.label("Prefer the selected XX_YY_<variant>.unr when present; otherwise use Standard.", Font.PLAIN, 9, ControlCenterTheme.MUTED);
		detail.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 2));
		panel.add(detail, BorderLayout.CENTER);
		return panel;
	}
	
	private void updateMapVariantOptions(ClientScanResult scanResult, String preferredVariant)
	{
		final String normalizedPreferred = preferredVariant == null ? "" : preferredVariant.trim().toLowerCase(Locale.ROOT);
		_updatingMapVariants = true;
		try
		{
			_mapVariant.removeAllItems();
			_mapVariant.addItem(new MapVariantOption("", "Standard"));
			if (scanResult == null)
			{
				if (!normalizedPreferred.isEmpty())
				{
					_mapVariant.addItem(new MapVariantOption(normalizedPreferred, MapExtractorService.displayMapVariant(normalizedPreferred)));
				}
			}
			else
			{
				for (String variant : scanResult.variantNames())
				{
					_mapVariant.addItem(new MapVariantOption(variant, MapExtractorService.displayMapVariant(variant)));
				}
			}
			
			int selectedIndex = 0;
			for (int index = 0; index < _mapVariant.getItemCount(); index++)
			{
				if (_mapVariant.getItemAt(index).id().equals(normalizedPreferred))
				{
					selectedIndex = index;
					break;
				}
			}
			
			_mapVariant.setSelectedIndex(selectedIndex);
		}
		finally
		{
			_updatingMapVariants = false;
		}
	}
	
	private String selectedMapVariant()
	{
		final Object selected = _mapVariant.getSelectedItem();
		if (selected instanceof MapVariantOption)
		{
			return ((MapVariantOption) selected).id();
		}
		
		return "";
	}
	
	private static String variantSummary(ClientScanResult scanResult)
	{
		if (scanResult.variantNames().isEmpty())
		{
			return "";
		}
		
		final StringBuilder text = new StringBuilder(", variants: ");
		boolean first = true;
		for (String variant : scanResult.variantNames())
		{
			if (!first)
			{
				text.append(", ");
			}
			text.append(MapExtractorService.displayMapVariant(variant)).append('=').append(scanResult.variantRegions(variant).size());
			first = false;
		}
		
		return text.toString();
	}
	
	private static JPanel option(JCheckBox checkBox, String description)
	{
		final JPanel panel = DashboardUiFactory.panel(new BorderLayout(6, 0));
		panel.add(checkBox, BorderLayout.NORTH);
		final JLabel detail = DashboardUiFactory.label(description, Font.PLAIN, 9, ControlCenterTheme.MUTED);
		detail.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 2));
		panel.add(detail, BorderLayout.CENTER);
		return panel;
	}
	
	private static JButton button(String text, GlyphIcon.Kind icon, boolean primary)
	{
		return new DashboardActionButton(text, ControlCenterIcons.create(icon, 13, ControlCenterTheme.TEXT), ControlCenterTheme.ACTION_PRIMARY, primary);
	}
	
	private static JLabel summaryValue(String text)
	{
		return DashboardUiFactory.label(text, Font.PLAIN, 9, ControlCenterTheme.TEXT);
	}
	
	private static JPanel flexibleTopSection(JPanel section)
	{
		final int preferredHeight = section.getPreferredSize().height;
		section.setMinimumSize(new Dimension(0, 0));
		section.setPreferredSize(new Dimension(0, preferredHeight));
		return section;
	}
	
	private static void configurePathField(JTextField field)
	{
		field.setColumns(8);
		field.setMinimumSize(new Dimension(0, field.getPreferredSize().height));
	}
	
	private static void configureFlexibleActionButton(JButton button, int minimumWidth)
	{
		button.setMinimumSize(new Dimension(minimumWidth, button.getPreferredSize().height));
	}
	
	private static GridBagConstraints executionConstraints(int x)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.insets = new Insets(0, x == 0 ? 0 : 8, 0, 0);
		return constraints;
	}
	
	private static GridBagConstraints pageConstraints(int y, int weightY)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = y;
		constraints.weightx = 1;
		constraints.weighty = weightY;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.NORTH;
		return constraints;
	}
	
	private static GridBagConstraints topConstraints(int x, double weight)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = 0;
		constraints.weightx = weight;
		constraints.weighty = 1;
		constraints.fill = GridBagConstraints.BOTH;
		constraints.anchor = GridBagConstraints.NORTH;
		constraints.insets = new Insets(0, x == 0 ? 0 : 4, 0, x == 3 ? 0 : 4);
		return constraints;
	}
	
	private static void addFieldRow(JPanel panel, int row, String label, JTextField field, JButton button)
	{
		final GridBagConstraints labelConstraints = fieldConstraints(0, row, 1, 0);
		labelConstraints.anchor = GridBagConstraints.WEST;
		panel.add(DashboardUiFactory.label(label, Font.PLAIN, 10, ControlCenterTheme.MUTED), labelConstraints);
		panel.add(field, fieldConstraints(1, row, 1, 1));
		if (button != null)
		{
			panel.add(button, fieldConstraints(2, row, 1, 0));
		}
	}
	
	private static void addSpinnerRow(JPanel panel, int row, String leftLabel, JSpinner left, String rightLabel, JSpinner right)
	{
		panel.add(DashboardUiFactory.label(leftLabel, Font.PLAIN, 10, ControlCenterTheme.MUTED), fieldConstraints(0, row, 1, 0));
		panel.add(left, fieldConstraints(1, row, 1, 1));
		panel.add(DashboardUiFactory.label(rightLabel, Font.PLAIN, 10, ControlCenterTheme.MUTED), fieldConstraints(2, row, 1, 0));
		panel.add(right, fieldConstraints(3, row, 1, 1));
	}
	
	private static void addSummaryRow(JPanel panel, int row, String label, JLabel value)
	{
		final GridBagConstraints left = fieldConstraints(0, row, 1, 1);
		left.anchor = GridBagConstraints.WEST;
		panel.add(DashboardUiFactory.label(label, Font.PLAIN, 9, ControlCenterTheme.MUTED), left);
		final GridBagConstraints right = fieldConstraints(1, row, 1, 0);
		right.anchor = GridBagConstraints.EAST;
		panel.add(value, right);
	}
	
	private static GridBagConstraints fieldConstraints(int x, int y, int width, int weight)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.gridwidth = width;
		constraints.weightx = weight;
		constraints.fill = weight > 0 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
		constraints.insets = new Insets(3, 4, 3, 4);
		return constraints;
	}
	
	private static Path requiredPath(String value, String label)
	{
		if ((value == null) || value.isBlank())
		{
			throw new IllegalArgumentException(label + " is not configured.");
		}
		
		try
		{
			return Path.of(value.trim()).toAbsolutePath().normalize();
		}
		catch (InvalidPathException e)
		{
			throw new IllegalArgumentException(label + " is invalid: " + e.getMessage(), e);
		}
	}
	
	private static Path optionalPath(String value)
	{
		if ((value == null) || value.isBlank())
		{
			return null;
		}
		
		return Path.of(value.trim()).toAbsolutePath().normalize();
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
	
	private static String formatBytes(long bytes)
	{
		if (bytes <= 0)
		{
			return "-";
		}
		
		final double gibibytes = bytes / (1024.0 * 1024.0 * 1024.0);
		return String.format("%.1f GB", gibibytes);
	}
	
	private static String formatDuration(long millis)
	{
		final long seconds = Math.max(0, millis / 1000);
		final long hours = seconds / 3600;
		final long minutes = (seconds % 3600) / 60;
		final long remainingSeconds = seconds % 60;
		if (hours > 0)
		{
			return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
		}
		
		return String.format("%02d:%02d", minutes, remainingSeconds);
	}
	
	private void append(String text)
	{
		if (text == null)
		{
			return;
		}
		
		final String line = text.endsWith("\n") ? text : text + System.lineSeparator();
		if (SwingUtilities.isEventDispatchThread())
		{
			_log.append(line);
			_log.setCaretPosition(_log.getDocument().getLength());
		}
		else
		{
			SwingUtilities.invokeLater(() -> append(line));
		}
	}
	
	private void showFailure(String title, Exception error)
	{
		final Throwable cause = error.getCause() == null ? error : error.getCause();
		append("[ERROR] " + safeMessage(cause));
		_summaryStatus.setText("Error");
		_summaryStatus.setForeground(ControlCenterTheme.RED);
		JOptionPane.showMessageDialog(this, safeMessage(cause), title, JOptionPane.ERROR_MESSAGE);
	}
	
	private static String safeMessage(Throwable error)
	{
		final String message = error.getMessage();
		return ((message == null) || message.isBlank()) ? error.getClass().getSimpleName() : message;
	}
	
	private static final class VerticalScrollPanel extends JPanel implements Scrollable
	{
		private VerticalScrollPanel(LayoutManager layout)
		{
			super(layout);
			setOpaque(false);
		}
		
		@Override
		public Dimension getPreferredScrollableViewportSize()
		{
			return getPreferredSize();
		}
		
		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return 22;
		}
		
		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction)
		{
			return Math.max(22, visibleRect.height - 22);
		}
		
		@Override
		public boolean getScrollableTracksViewportWidth()
		{
			return true;
		}
		
		@Override
		public boolean getScrollableTracksViewportHeight()
		{
			return false;
		}
	}
	
	private record MapVariantOption(String id, String label)
	{
		@Override
		public String toString()
		{
			return label;
		}
	}
	
	private record WorkerUpdate(String message, ExtractionProgress progress)
	{
	}
}
