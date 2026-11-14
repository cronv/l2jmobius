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
package org.l2jmobius.commons.ui;

import java.awt.AWTEvent;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;

import javax.swing.AbstractButton;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.plaf.BorderUIResource;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.ComponentUI;
import javax.swing.plaf.DimensionUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.UIResource;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

/**
 * A flat look and feel built on plain Swing, without any external library.<br>
 * Metal handles the component behavior, everything visible is overridden here from a {@link Palette}.<br>
 * Use {@link DarkTheme} or {@link LightTheme} instead of instantiating this directly.
 * @author Mobius
 */
public class FlatLookAndFeel extends MetalLookAndFeel
{
	/** Custom default key, read back by the border painter. */
	private static final String BORDER_COLOR_KEY = "Component.borderColor";
	
	private static final int FONT_SIZE = 12;
	private static final int ARC = 8;
	private static final int SCROLL_BAR_WIDTH = 15;
	/** Gap between the thumb and the edge of the track, on each side. */
	private static final int THUMB_INSET = 2;
	
	/** Palette the title bar listener reads, replaced by every {@link #install(String, Palette)} call. */
	private static Palette _installed;
	
	private final String _name;
	private final Palette _palette;
	
	/**
	 * Creates a look and feel painting the given palette.
	 * @param name the name reported by {@link #getName()}
	 * @param palette the colors to paint with
	 */
	public FlatLookAndFeel(String name, Palette palette)
	{
		_name = name;
		_palette = palette;
	}
	
	@Override
	public String getName()
	{
		return _name;
	}
	
	@Override
	public String getID()
	{
		return _name;
	}
	
	@Override
	public String getDescription()
	{
		return _name;
	}
	
	@Override
	public boolean isSupportedLookAndFeel()
	{
		return true;
	}
	
	/**
	 * Installs the given palette and keeps the title bar of every window in step with it.<br>
	 * The title bar belongs to the operating system, so it is recolored by {@link WindowsTitleBar}<br>
	 * rather than painted here, which keeps the native window buttons and behavior.
	 * @param name the look and feel name
	 * @param palette the colors to paint with
	 */
	public static void install(String name, Palette palette)
	{
		try
		{
			UIManager.setLookAndFeel(new FlatLookAndFeel(name, palette));
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return;
		}
		
		final boolean first = _installed == null;
		_installed = palette;
		
		for (Window window : Window.getWindows())
		{
			WindowsTitleBar.apply(window, palette.dark, palette.titleBar);
		}
		
		// Windows opened later have no native handle yet, so they are recolored as they appear.
		// The listener lives for the rest of the process, a later install only swaps the palette it reads.
		if (first)
		{
			Toolkit.getDefaultToolkit().addAWTEventListener(event ->
			{
				if (event.getID() == WindowEvent.WINDOW_OPENED)
				{
					WindowsTitleBar.apply(((WindowEvent) event).getWindow(), _installed.dark, _installed.titleBar);
				}
			}, AWTEvent.WINDOW_EVENT_MASK);
		}
	}
	
	@Override
	public UIDefaults getDefaults()
	{
		// Metal paints part of its chrome from the current theme, so align that before reading the defaults.
		setCurrentTheme(new FlatMetalTheme(_palette));
		
		final UIDefaults defaults = super.getDefaults();
		putColor(defaults, _palette.background, "control", "window", "menu", "Panel.background", "OptionPane.background", "Label.background", "Button.background", "ToggleButton.background", "CheckBox.background", "RadioButton.background", "ComboBox.background", "ComboBox.buttonBackground", "ToolBar.background", "TabbedPane.background", "TabbedPane.contentAreaColor", "TabbedPane.tabAreaBackground", "TabbedPane.unselectedBackground", "SplitPane.background", "ScrollPane.background", "Viewport.background", "ScrollBar.background", "ScrollBar.track", "ProgressBar.background", "TableHeader.background", "FileChooser.background", "Slider.background");
		putColor(defaults, _palette.menuBar, "MenuBar.background", "Menu.background", "MenuItem.background", "CheckBoxMenuItem.background", "RadioButtonMenuItem.background", "PopupMenu.background");
		putColor(defaults, _palette.content, "TextField.background", "PasswordField.background", "FormattedTextField.background", "TextArea.background", "TextPane.background", "EditorPane.background", "List.background", "Table.background", "Tree.background", "Tree.textBackground", "ToolTip.background", "info");
		putColor(defaults, _palette.foreground, "controlText", "text", "textText", "windowText", "menuText", "infoText", "Label.foreground", "Panel.foreground", "Button.foreground", "ToggleButton.foreground", "CheckBox.foreground", "RadioButton.foreground", "ComboBox.foreground", "TextField.foreground", "PasswordField.foreground", "FormattedTextField.foreground", "TextArea.foreground", "TextPane.foreground", "EditorPane.foreground", "List.foreground", "Table.foreground", "TableHeader.foreground", "Tree.foreground", "Tree.textForeground", "TabbedPane.foreground", "MenuBar.foreground", "Menu.foreground", "MenuItem.foreground", "CheckBoxMenuItem.foreground", "RadioButtonMenuItem.foreground", "PopupMenu.foreground", "ToolTip.foreground", "TitledBorder.titleColor", "OptionPane.messageForeground", "ProgressBar.selectionBackground", "FileChooser.foreground");
		putColor(defaults, _palette.foreground, "TextField.caretForeground", "PasswordField.caretForeground", "FormattedTextField.caretForeground", "TextArea.caretForeground", "TextPane.caretForeground", "EditorPane.caretForeground");
		putColor(defaults, _palette.selection, "textHighlight", "TextField.selectionBackground", "PasswordField.selectionBackground", "FormattedTextField.selectionBackground", "TextArea.selectionBackground", "TextPane.selectionBackground", "EditorPane.selectionBackground", "List.selectionBackground", "Table.selectionBackground", "Tree.selectionBackground", "ComboBox.selectionBackground", "Menu.selectionBackground", "MenuItem.selectionBackground", "CheckBoxMenuItem.selectionBackground", "RadioButtonMenuItem.selectionBackground", "TabbedPane.selected", "TabbedPane.focus");
		putColor(defaults, _palette.selectionText, "textHighlightText", "TextField.selectionForeground", "PasswordField.selectionForeground", "FormattedTextField.selectionForeground", "TextArea.selectionForeground", "TextPane.selectionForeground", "EditorPane.selectionForeground", "List.selectionForeground", "Table.selectionForeground", "Tree.selectionForeground", "ComboBox.selectionForeground", "Menu.selectionForeground", "MenuItem.selectionForeground", "CheckBoxMenuItem.selectionForeground", "RadioButtonMenuItem.selectionForeground");
		putColor(defaults, _palette.border, BORDER_COLOR_KEY, "controlShadow", "controlDkShadow", "controlHighlight", "controlLtHighlight", "Separator.foreground", "Separator.background", "Table.gridColor", "Tree.line", "TabbedPane.darkShadow", "TabbedPane.light", "TabbedPane.shadow", "TabbedPane.highlight", "TabbedPane.selectHighlight", "TabbedPane.borderHightlightColor");
		putColor(defaults, _palette.hover, "Button.select", "ToggleButton.select", "Tree.selectionBorderColor");
		putColor(defaults, _palette.disabled, "textInactiveText", "Button.disabledText", "Label.disabledForeground", "CheckBox.disabledText", "RadioButton.disabledText", "MenuItem.disabledForeground", "Menu.disabledForeground", "ComboBox.disabledForeground", "TextField.inactiveForeground");
		putColor(defaults, _palette.thumb, "ScrollBar.thumb", "ProgressBar.foreground");
		putColor(defaults, _palette.thumbHover, "ScrollBar.thumbHighlight", "ScrollBar.thumbShadow");
		
		final Border flatBorder = new FlatBorder(4, 8);
		// A scroll pane holds its own content, so the outline hugs it instead of padding it.
		final Border tightBorder = new FlatBorder(1, 1);
		defaults.put("Button.border", flatBorder);
		defaults.put("ToggleButton.border", flatBorder);
		defaults.put("TextField.border", flatBorder);
		defaults.put("PasswordField.border", flatBorder);
		defaults.put("FormattedTextField.border", flatBorder);
		defaults.put("ComboBox.border", flatBorder);
		defaults.put("ScrollPane.border", tightBorder);
		defaults.put("PopupMenu.border", flatBorder);
		defaults.put("TitledBorder.border", flatBorder);
		// Metal outlines every menu item, which shows up as a gutter line down the popup.
		final Border menuItemBorder = new BorderUIResource.EmptyBorderUIResource(4, 6, 4, 6);
		defaults.put("Menu.border", menuItemBorder);
		defaults.put("MenuItem.border", menuItemBorder);
		defaults.put("CheckBoxMenuItem.border", menuItemBorder);
		defaults.put("RadioButtonMenuItem.border", menuItemBorder);
		defaults.put("Menu.borderPainted", Boolean.FALSE);
		defaults.put("MenuItem.borderPainted", Boolean.FALSE);
		defaults.put("CheckBoxMenuItem.borderPainted", Boolean.FALSE);
		defaults.put("RadioButtonMenuItem.borderPainted", Boolean.FALSE);
		
		final Icon checkIcon = new FlatCheckIcon();
		defaults.put("CheckBoxMenuItem.checkIcon", checkIcon);
		defaults.put("RadioButtonMenuItem.checkIcon", checkIcon);
		defaults.put("MenuBar.border", null);
		defaults.put("ToolBar.border", null);
		defaults.put("SplitPane.border", null);
		final Insets textMargin = new Insets(4, 6, 4, 6);
		defaults.put("TextArea.margin", textMargin);
		defaults.put("TextPane.margin", textMargin);
		defaults.put("SplitPane.dividerSize", Integer.valueOf(6));
		defaults.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 0));
		defaults.put("ScrollBar.width", Integer.valueOf(SCROLL_BAR_WIDTH));
		defaults.put("ScrollBar.minimumThumbSize", new DimensionUIResource(SCROLL_BAR_WIDTH, SCROLL_BAR_WIDTH * 3));
		defaults.put("ScrollBarUI", FlatScrollBarUI.class.getName());
		defaults.put("Table.showGrid", Boolean.FALSE);
		defaults.put("Table.intercellSpacing", new DimensionUIResource(0, 0));
		defaults.put("Button.rollover", Boolean.TRUE);
		
		// A logical font is a composite, so Windows and Linux both fall back to a symbol font for the emoji the tools put on their buttons.
		// A physical family such as Segoe UI draws those as boxes.
		// Metal also defaults to bold labels and menus, so this flattens every font in one pass.
		final FontUIResource font = new FontUIResource(Font.SANS_SERIF, Font.PLAIN, FONT_SIZE);
		final Object[] keys = defaults.keySet().toArray();
		for (Object key : keys)
		{
			if ((key instanceof String) && ((String) key).endsWith(".font"))
			{
				defaults.put(key, font);
			}
		}
		
		return defaults;
	}
	
	private static void putColor(UIDefaults defaults, Color color, String... keys)
	{
		final ColorUIResource resource = new ColorUIResource(color);
		for (String key : keys)
		{
			defaults.put(key, resource);
		}
	}
	
	/**
	 * The colors a theme paints with. Every field is required.
	 */
	public static class Palette
	{
		/** Window and container background. */
		public Color background;
		/** Menu bar strip and the menus that drop out of it. */
		public Color menuBar;
		/** Title bar shade, applied by Windows 11 only. */
		public Color titleBar;
		/** Whether the operating system should paint the title bar in its dark mode. */
		public boolean dark;
		/** Background of text areas, lists and tables. */
		public Color content;
		/** Regular text. */
		public Color foreground;
		/** Text of disabled components. */
		public Color disabled;
		/** Component outlines and separators. */
		public Color border;
		/** Fill of a hovered or pressed button. */
		public Color hover;
		/** Background of selected text and rows. */
		public Color selection;
		/** Text drawn on top of {@link #selection}. */
		public Color selectionText;
		/** Scroll bar thumb, also the progress bar fill. */
		public Color thumb;
		/** Scroll bar thumb while hovered. */
		public Color thumbHover;
	}
	
	/**
	 * Keeps the colors Metal reads from its theme rather than from the defaults table in step with the palette.
	 */
	private static class FlatMetalTheme extends DefaultMetalTheme
	{
		private final ColorUIResource _primary;
		private final ColorUIResource _secondary;
		private final ColorUIResource _text;
		
		FlatMetalTheme(Palette palette)
		{
			_primary = new ColorUIResource(palette.selection);
			_secondary = new ColorUIResource(palette.background);
			_text = new ColorUIResource(palette.foreground);
		}
		
		@Override
		public String getName()
		{
			return "Flat";
		}
		
		@Override
		protected ColorUIResource getPrimary1()
		{
			return _primary;
		}
		
		@Override
		protected ColorUIResource getPrimary2()
		{
			return _primary;
		}
		
		@Override
		protected ColorUIResource getPrimary3()
		{
			return _primary;
		}
		
		@Override
		protected ColorUIResource getSecondary1()
		{
			return _secondary;
		}
		
		@Override
		protected ColorUIResource getSecondary2()
		{
			return _secondary;
		}
		
		@Override
		protected ColorUIResource getSecondary3()
		{
			return _secondary;
		}
		
		@Override
		protected ColorUIResource getBlack()
		{
			return _text;
		}
	}
	
	/**
	 * A bare check mark for selected menu items, drawn without the Metal box around it.
	 */
	private static class FlatCheckIcon implements Icon, UIResource
	{
		private static final int SIZE = 12;
		private static final BasicStroke STROKE = new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
		
		@Override
		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			if (!(c instanceof AbstractButton) || !((AbstractButton) c).isSelected())
			{
				return;
			}
			
			final Graphics2D graphics = (Graphics2D) g.create();
			try
			{
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setColor(c.getForeground());
				graphics.setStroke(STROKE);
				graphics.drawLine(x + 2, y + 6, x + 5, y + 9);
				graphics.drawLine(x + 5, y + 9, x + 10, y + 3);
			}
			finally
			{
				graphics.dispose();
			}
		}
		
		@Override
		public int getIconWidth()
		{
			return SIZE;
		}
		
		@Override
		public int getIconHeight()
		{
			return SIZE;
		}
	}
	
	/**
	 * A single pixel rounded outline, replaces the Metal bevels.
	 */
	private static class FlatBorder implements Border, UIResource
	{
		private final Insets _insets;
		
		FlatBorder(int vertical, int horizontal)
		{
			_insets = new Insets(vertical, horizontal, vertical, horizontal);
		}
		
		@Override
		public void paintBorder(Component c, Graphics g, int x, int y, int width, int height)
		{
			final Color color = UIManager.getColor(BORDER_COLOR_KEY);
			if (color == null)
			{
				return;
			}
			
			final Graphics2D graphics = (Graphics2D) g.create();
			try
			{
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setColor(color);
				graphics.drawRoundRect(x, y, width - 1, height - 1, ARC, ARC);
			}
			finally
			{
				graphics.dispose();
			}
		}
		
		@Override
		public Insets getBorderInsets(Component c)
		{
			return _insets;
		}
		
		@Override
		public boolean isBorderOpaque()
		{
			return false;
		}
	}
	
	/**
	 * Thin scroll bar without arrow buttons and with a rounded thumb.<br>
	 * Colors come from the defaults table, so it serves every palette.
	 */
	public static class FlatScrollBarUI extends BasicScrollBarUI
	{
		public static ComponentUI createUI(JComponent c)
		{
			return new FlatScrollBarUI();
		}
		
		@Override
		protected JButton createIncreaseButton(int orientation)
		{
			return createHiddenButton();
		}
		
		@Override
		protected JButton createDecreaseButton(int orientation)
		{
			return createHiddenButton();
		}
		
		private static JButton createHiddenButton()
		{
			final JButton button = new JButton();
			final Dimension zero = new Dimension(0, 0);
			button.setPreferredSize(zero);
			button.setMinimumSize(zero);
			button.setMaximumSize(zero);
			button.setFocusable(false);
			return button;
		}
		
		@Override
		protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds)
		{
			g.setColor(trackColor);
			g.fillRect(trackBounds.x, trackBounds.y, trackBounds.width, trackBounds.height);
		}
		
		@Override
		protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds)
		{
			if (thumbBounds.isEmpty() || !scrollbar.isEnabled())
			{
				return;
			}
			
			final Graphics2D graphics = (Graphics2D) g.create();
			try
			{
				graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				graphics.setColor(isThumbRollover() ? thumbHighlightColor : thumbColor);
				graphics.fillRoundRect(thumbBounds.x + THUMB_INSET, thumbBounds.y + THUMB_INSET, thumbBounds.width - (THUMB_INSET * 2), thumbBounds.height - (THUMB_INSET * 2), ARC, ARC);
			}
			finally
			{
				graphics.dispose();
			}
		}
		
		@Override
		protected void setThumbBounds(int x, int y, int width, int height)
		{
			super.setThumbBounds(x, y, width, height);
			scrollbar.repaint();
		}
	}
}
