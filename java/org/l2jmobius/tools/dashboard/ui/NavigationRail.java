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
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.l2jmobius.tools.dashboard.bridge.DashboardProfile;
import org.l2jmobius.tools.dashboard.runtime.ServiceObserver;
import org.l2jmobius.tools.dashboard.runtime.ServicePhase;
import org.l2jmobius.tools.dashboard.runtime.ServiceSupervisor;

/**
 * Implements the Dashboard navigation rail with source tools and responsive collapse behavior.
 * @author BazookaRpm
 */
public final class NavigationRail extends JPanel
{
	private static final PageEntry[] PAGES =
	{
		new PageEntry("dashboard", "Dashboard", GlyphIcon.Kind.HOME),
		new PageEntry("login", "Login Server", GlyphIcon.Kind.SERVICE, "SYSTEM"),
		new PageEntry("game", "Game Server", GlyphIcon.Kind.SERVICE),
		new PageEntry("observability", "Observability", GlyphIcon.Kind.TELEMETRY),
		new PageEntry("diagnostics", "Diagnostics", GlyphIcon.Kind.DIAGNOSTICS),
		new PageEntry("logs", "Logs", GlyphIcon.Kind.LOG),
		new PageEntry("settings", "Settings", GlyphIcon.Kind.SETTINGS),
		new PageEntry("tools", "Database Tools", GlyphIcon.Kind.TOOLS, "TOOLS"),
		new PageEntry("mapExtractor", "Map Extractor", GlyphIcon.Kind.TOOLS),
		new PageEntry("npcEditor", "NPC Editor", GlyphIcon.Kind.TOOLS),
		new PageEntry("itemEditor", "Item Editor", GlyphIcon.Kind.TOOLS),
		new PageEntry("dropEditor", "Drop Editor", GlyphIcon.Kind.TOOLS),
		new PageEntry("search", "Search", GlyphIcon.Kind.SEARCH),
		new PageEntry("accountManager", "Account Manager", GlyphIcon.Kind.DETAILS, "ADMINISTRATION"),
		new PageEntry("gameServerRegister", "Game Server Register", GlyphIcon.Kind.SERVICE),
		new PageEntry("backups", "Backups", GlyphIcon.Kind.DATABASE),
		new PageEntry("preferences", "Preferences", GlyphIcon.Kind.PREFERENCES)
	};
	private final int _openWidth;
	private final Consumer<String> _navigationAction;
	private final Map<String, NavigationButton> _pageButtons = new LinkedHashMap<>();
	private final List<JLabel> _sectionLabels = new ArrayList<>();
	private final JLabel _summary = DashboardUiFactory.label("OFFLINE", Font.BOLD, 10, ControlCenterTheme.MUTED);
	private final JLabel _summaryDetail = DashboardUiFactory.label("0 of 2 services online", Font.PLAIN, 9, ControlCenterTheme.MUTED);
	private final JPanel _footer;
	private boolean _compact;
	private ServicePhase _loginState = ServicePhase.OFFLINE;
	private ServicePhase _gameState = ServicePhase.OFFLINE;
	
	public NavigationRail(int width, Consumer<String> pageConsumer, ServiceSupervisor login, ServiceSupervisor game)
	{
		_openWidth = width;
		_navigationAction = pageConsumer;
		setBackground(ControlCenterTheme.NAVIGATION);
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, ControlCenterTheme.BORDER));
		resizeRail(width);
		add(buildPageScroll(), BorderLayout.CENTER);
		_footer = buildFooter();
		add(_footer, BorderLayout.PAGE_END);
		login.addObserver(createStateObserver(true));
		game.addObserver(createStateObserver(false));
		activate("dashboard", true);
	}
	
	private JScrollPane buildPageScroll()
	{
		final JScrollPane scroll = new JScrollPane(buildPageList(), ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setBorder(BorderFactory.createEmptyBorder());
		scroll.setOpaque(false);
		scroll.getViewport().setOpaque(false);
		scroll.getVerticalScrollBar().setUnitIncrement(18);
		return scroll;
	}
	
	private JPanel buildPageList()
	{
		final JPanel list = DashboardUiFactory.panel(new GridBagLayout());
		final NavigationButton collapse = new NavigationButton("", GlyphIcon.Kind.HOME, false);
		collapse.setToolTipText("Collapse navigation.");
		collapse.addActionListener(event -> setCollapsed(!_compact));
		addRow(list, collapse, 0, 0);
		int row = 1;
		for (PageEntry page : PAGES)
		{
			if (page.section() != null)
			{
				final JLabel section = DashboardUiFactory.label(page.section(), Font.BOLD, 9, ControlCenterTheme.MUTED);
				_sectionLabels.add(section);
				addSectionRow(list, section, row++);
			}
			
			final NavigationButton button = new NavigationButton(page.label(), page.icon(), true);
			button.setToolTipText(page.label());
			button.addActionListener(event -> activate(page.key(), false));
			_pageButtons.put(page.key(), button);
			addRow(list, button, row++, 2);
		}
		
		final GridBagConstraints filler = new GridBagConstraints();
		filler.gridx = 0;
		filler.gridy = row;
		filler.weighty = 1;
		filler.fill = GridBagConstraints.VERTICAL;
		list.add(DashboardUiFactory.panel(new BorderLayout()), filler);
		return list;
	}
	
	private void addSectionRow(JPanel parent, JLabel section, int row)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(6, 20, 1, 8);
		parent.add(section, constraints);
	}
	
	private void addRow(JPanel parent, NavigationButton button, int row, int topGap)
	{
		final GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.weightx = 1;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(topGap, 8, 0, 8);
		parent.add(button, constraints);
	}
	
	private JPanel buildFooter()
	{
		final SurfacePanel card = new SurfacePanel(10, ControlCenterTheme.SURFACE, ControlCenterTheme.BORDER);
		card.setLayout(new GridBagLayout());
		final JLabel brand = DashboardUiFactory.label("L2J MOBIUS", Font.BOLD, 10, ControlCenterTheme.TEXT_BRIGHT);
		final JLabel edition = DashboardUiFactory.label(DashboardProfile.EDITION_LABEL, Font.PLAIN, 9, ControlCenterTheme.MUTED);
		brand.setHorizontalAlignment(SwingConstants.CENTER);
		edition.setHorizontalAlignment(SwingConstants.CENTER);
		_summary.setHorizontalAlignment(SwingConstants.CENTER);
		_summaryDetail.setHorizontalAlignment(SwingConstants.CENTER);
		final JComponent[] rows =
		{
			brand,
			edition,
			_summary,
			_summaryDetail
		};
		for (int index = 0; index < rows.length; index++)
		{
			final GridBagConstraints line = new GridBagConstraints();
			line.gridx = 0;
			line.gridy = index;
			line.weightx = 1;
			line.fill = GridBagConstraints.HORIZONTAL;
			line.insets = new Insets(index == 2 ? 5 : 1, 6, 1, 6);
			card.add(rows[index], line);
		}
		
		final JPanel holder = DashboardUiFactory.panel(new BorderLayout());
		holder.setBorder(BorderFactory.createEmptyBorder(0, 8, 6, 8));
		holder.add(card);
		return holder;
	}
	
	public int getExpandedWidth()
	{
		return _openWidth;
	}
	
	public boolean isCollapsed()
	{
		return _compact;
	}
	
	public void setCollapsed(boolean collapsed)
	{
		if (_compact != collapsed)
		{
			_compact = collapsed;
			resizeRail(collapsed ? 60 : _openWidth);
			for (NavigationButton button : _pageButtons.values())
			{
				button.showText(!collapsed);
			}
			
			for (JLabel section : _sectionLabels)
			{
				section.setVisible(!collapsed);
			}
			
			// The status card has no room for its text in the narrow rail.
			_footer.setVisible(!collapsed);
			
			revalidate();
			repaint();
		}
	}
	
	public void selectPage(String key)
	{
		activate(key, true);
	}
	
	private void activate(String key, boolean external)
	{
		final NavigationButton selected = _pageButtons.get(key);
		if (selected == null)
		{
			return;
		}
		
		for (Entry<String, NavigationButton> entry : _pageButtons.entrySet())
		{
			entry.getValue().markSelected(entry.getValue() == selected);
		}
		
		if (!external)
		{
			_navigationAction.accept(key);
		}
	}
	
	private void resizeRail(int width)
	{
		setPreferredSize(new Dimension(width, 0));
		setMinimumSize(new Dimension(width, 0));
	}
	
	private ServiceObserver createStateObserver(boolean login)
	{
		return new ServiceObserver()
		{
			@Override
			public void phaseChanged(ServicePhase phase, String detail)
			{
				SwingUtilities.invokeLater(() -> updateState(login, phase));
			}
		};
	}
	
	private void updateState(boolean login, ServicePhase phase)
	{
		if (login)
		{
			_loginState = phase;
		}
		else
		{
			_gameState = phase;
		}
		
		final int count = (_loginState == ServicePhase.ONLINE ? 1 : 0) + (_gameState == ServicePhase.ONLINE ? 1 : 0);
		_summary.setText(count == 2 ? "ONLINE" : count == 0 ? "OFFLINE" : "PARTIAL");
		_summary.setForeground(count == 2 ? ControlCenterTheme.GREEN : count == 0 ? ControlCenterTheme.MUTED : ControlCenterTheme.YELLOW);
		_summaryDetail.setText(count + " of 2 services online");
	}
	
	/**
	 * Defines one navigation rail entry.
	 * @param key internal page routing key
	 * @param label visible navigation label
	 * @param icon navigation glyph
	 * @param section optional visible section heading
	 */
	private record PageEntry(String key, String label, GlyphIcon.Kind icon, String section)
	{
		/**
		 * Creates an entry without a section heading.
		 * @param key internal page routing key
		 * @param label visible navigation label
		 * @param icon navigation glyph
		 */
		private PageEntry(String key, String label, GlyphIcon.Kind icon)
		{
			this(key, label, icon, null);
		}
	}
	
	/**
	 * Renders one navigation rail button with active-state decoration.
	 */
	private static final class NavigationButton extends JButton
	{
		private final String _label;
		private boolean _active;
		
		private NavigationButton(String label, GlyphIcon.Kind icon, boolean textVisible)
		{
			super(textVisible ? label : "", ControlCenterIcons.create(icon, 16, ControlCenterTheme.MUTED));
			_label = label;
			setFont(ControlCenterTheme.font(Font.PLAIN, 12));
			setForeground(ControlCenterTheme.MUTED);
			setHorizontalAlignment(textVisible ? SwingConstants.LEFT : SwingConstants.CENTER);
			setIconTextGap(12);
			// The collapsed rail width minus the row insets; the open rail stretches the row to fill.
			setPreferredSize(new Dimension(44, 34));
			setFocusPainted(false);
			setContentAreaFilled(false);
			setBorderPainted(false);
			setOpaque(false);
			setBorder(BorderFactory.createEmptyBorder(0, textVisible ? 12 : 0, 0, 8));
		}
		
		private void showText(boolean visible)
		{
			setText(visible ? _label : "");
			setHorizontalAlignment(visible ? SwingConstants.LEFT : SwingConstants.CENTER);
			setBorder(BorderFactory.createEmptyBorder(0, visible ? 12 : 0, 0, visible ? 8 : 0));
		}
		
		private void markSelected(boolean selected)
		{
			_active = selected;
			setForeground(selected ? ControlCenterTheme.TEXT_BRIGHT : ControlCenterTheme.MUTED);
			repaint();
		}
		
		@Override
		protected void paintComponent(Graphics graphics)
		{
			if (_active)
			{
				final Graphics2D paint = (Graphics2D) graphics.create();
				paint.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				paint.setColor(ControlCenterTheme.ACCENT_DIM);
				paint.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
				paint.setColor(ControlCenterTheme.ACCENT);
				paint.fillRect(0, 5, 3, Math.max(1, getHeight() - 10));
				paint.dispose();
			}
			
			super.paintComponent(graphics);
		}
	}
}
