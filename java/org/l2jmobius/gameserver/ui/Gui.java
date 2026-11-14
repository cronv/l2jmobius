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
package org.l2jmobius.gameserver.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DropMode;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.LightTheme;
import org.l2jmobius.commons.ui.LineLimitListener;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.commons.ui.WindowSize;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.Shutdown;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.config.ConfigLoader;
import org.l2jmobius.gameserver.data.xml.AdminData;
import org.l2jmobius.gameserver.data.xml.BuyListData;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.data.xml.PrimeShopData;
import org.l2jmobius.gameserver.entity.World;

/**
 * This class manages the graphical user interface for the game server.<br>
 * It handles the creation and lifecycle of {@code JFrame} components and various UI elements.
 * @author Mobius
 */
public class Gui
{
	private static final String[] SHUTDOWN_OPTIONS =
	{
		"Shutdown",
		"Cancel"
	};
	private static final String[] RESTART_OPTIONS =
	{
		"Restart",
		"Cancel"
	};
	private static final String[] ABORT_OPTIONS =
	{
		"Abort",
		"Cancel"
	};
	private static final String[] CONFIRM_OPTIONS =
	{
		"Confirm",
		"Cancel"
	};
	
	private static Gui _instance;
	
	private final JTextArea _txtrConsole;
	private final StringBuilder _pendingConsole = new StringBuilder();
	
	/**
	 * Initializes the graphical user interface for the game server.<br>
	 * This constructor sets up system properties to disable hardware acceleration.<br>
	 * It activates the dark theme if configured and initializes the console text area.<br>
	 * It also builds the main menu bar with actions, reload options, and log controls.
	 */
	public Gui()
	{
		_instance = this;
		
		// Disable hardware acceleration.
		System.setProperty("sun.java2d.opengl", "false");
		System.setProperty("sun.java2d.d3d", "false");
		System.setProperty("sun.java2d.noddraw", "true");
		
		if (InterfaceConfig.DARK_THEME)
		{
			DarkTheme.activate();
		}
		else
		{
			LightTheme.activate();
		}
		
		// Initialize console.
		_txtrConsole = new JTextArea();
		_txtrConsole.setEditable(false);
		_txtrConsole.setLineWrap(true);
		_txtrConsole.setWrapStyleWord(true);
		_txtrConsole.setDropMode(DropMode.INSERT);
		_txtrConsole.setFont(new Font("Monospaced", Font.PLAIN, 16));
		_txtrConsole.getDocument().addDocumentListener(new LineLimitListener(500));
		
		// Add right-click copy functionality.
		addRightClickCopyMenu();
		
		// Initialize menu items.
		final JMenuBar menuBar = new JMenuBar();
		menuBar.setFont(new Font("Segoe UI", Font.PLAIN, 14));
		
		final JMenu mnActions = new JMenu("Actions");
		mnActions.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnActions);
		
		final JMenuItem mntmShutdown = new JMenuItem("Shutdown");
		mntmShutdown.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmShutdown.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Shutdown GameServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0)
			{
				final Object answer = JOptionPane.showInputDialog(null, "Shutdown delay in seconds", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "600");
				if (answer != null)
				{
					final String input = ((String) answer).trim();
					if (StringUtil.isNumeric(input))
					{
						final int delay = Integer.parseInt(input);
						if (delay > 0)
						{
							Shutdown.getInstance().startShutdown(null, delay, false);
						}
					}
				}
			}
		});
		mnActions.add(mntmShutdown);
		
		final JMenuItem mntmRestart = new JMenuItem("Restart");
		mntmRestart.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmRestart.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Restart GameServer?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, RESTART_OPTIONS, RESTART_OPTIONS[1]) == 0)
			{
				final Object answer = JOptionPane.showInputDialog(null, "Restart delay in seconds", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "600");
				if (answer != null)
				{
					final String input = ((String) answer).trim();
					if (StringUtil.isNumeric(input))
					{
						final int delay = Integer.parseInt(input);
						if (delay > 0)
						{
							Shutdown.getInstance().startShutdown(null, delay, true);
						}
					}
				}
			}
		});
		mnActions.add(mntmRestart);
		
		final JMenuItem mntmAbort = new JMenuItem("Abort");
		mntmAbort.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAbort.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Abort server shutdown?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, ABORT_OPTIONS, ABORT_OPTIONS[1]) == 0)
			{
				Shutdown.getInstance().abort(null);
			}
		});
		mnActions.add(mntmAbort);
		
		final JMenu mnReload = new JMenu("Reload");
		mnReload.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnReload);
		
		final JMenuItem mntmConfigs = new JMenuItem("Configs");
		mntmConfigs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmConfigs.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload configs?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				ConfigLoader.init();
			}
		});
		mnReload.add(mntmConfigs);
		
		final JMenuItem mntmAccess = new JMenuItem("Access");
		mntmAccess.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAccess.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload admin access levels?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				AdminData.getInstance().load();
			}
		});
		mnReload.add(mntmAccess);
		
		final JMenuItem mntmHtml = new JMenuItem("HTML");
		mntmHtml.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmHtml.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload HTML files?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				HtmCache.getInstance().reload();
			}
		});
		mnReload.add(mntmHtml);
		
		final JMenuItem mntmMultisells = new JMenuItem("Multisells");
		mntmMultisells.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmMultisells.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload multisells?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				MultisellData.getInstance().load();
			}
		});
		mnReload.add(mntmMultisells);
		
		final JMenuItem mntmBuylists = new JMenuItem("Buylists");
		mntmBuylists.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmBuylists.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload buylists?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				BuyListData.getInstance().load();
			}
		});
		mnReload.add(mntmBuylists);
		
		final JMenuItem mntmPrimeShop = new JMenuItem("PrimeShop");
		mntmPrimeShop.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmPrimeShop.addActionListener(_ ->
		{
			if (JOptionPane.showOptionDialog(null, "Reload PrimeShop?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, CONFIRM_OPTIONS, CONFIRM_OPTIONS[1]) == 0)
			{
				PrimeShopData.getInstance().load();
			}
		});
		mnReload.add(mntmPrimeShop);
		
		final JMenu mnAnnounce = new JMenu("Announce");
		mnAnnounce.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnAnnounce);
		
		final JMenuItem mntmNormal = new JMenuItem("Normal");
		mntmNormal.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmNormal.addActionListener(_ ->
		{
			final Object input = JOptionPane.showInputDialog(null, "Announce message", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "");
			if (input != null)
			{
				final String message = ((String) input).trim();
				if (!message.isEmpty())
				{
					World.broadcastToAllOnlinePlayers(message, false);
				}
			}
		});
		mnAnnounce.add(mntmNormal);
		
		final JMenuItem mntmCritical = new JMenuItem("Critical");
		mntmCritical.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmCritical.addActionListener(_ ->
		{
			final Object input = JOptionPane.showInputDialog(null, "Critical announce message", "Input", JOptionPane.INFORMATION_MESSAGE, null, null, "");
			if (input != null)
			{
				final String message = ((String) input).trim();
				if (!message.isEmpty())
				{
					World.broadcastToAllOnlinePlayers(message, true);
				}
			}
		});
		mnAnnounce.add(mntmCritical);
		
		final JMenu mnLogs = new JMenu("Logs");
		mnLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnLogs);
		
		final JMenuItem mntmLogs = new JMenuItem("View");
		mntmLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmLogs.addActionListener(_ -> new LogPanel(false));
		mnLogs.add(mntmLogs);
		
		final JMenuItem mntmDeleteLogs = new JMenuItem("Delete");
		mntmDeleteLogs.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmDeleteLogs.addActionListener(_ -> new LogPanel(true));
		mnLogs.add(mntmDeleteLogs);
		
		final JMenu mnFont = new JMenu("Font");
		mnFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnFont);
		
		final String[] fonts =
		{
			"16",
			"21",
			"27",
			"33"
		};
		for (String font : fonts)
		{
			final JMenuItem mntmFont = new JMenuItem(font);
			mntmFont.setFont(new Font("Segoe UI", Font.PLAIN, 13));
			mntmFont.addActionListener(_ -> _txtrConsole.setFont(new Font("Monospaced", Font.PLAIN, Integer.parseInt(font))));
			mnFont.add(mntmFont);
		}
		
		final JMenu mnHelp = new JMenu("Help");
		mnHelp.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		menuBar.add(mnHelp);
		
		final JMenuItem mntmAbout = new JMenuItem("About");
		mntmAbout.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		mntmAbout.addActionListener(_ -> new AboutFrame());
		mnHelp.add(mntmAbout);
		
		// Set icons.
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_16x16.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_32x32.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_64x64.png").getImage());
		icons.add(new ImageIcon(".." + File.separator + "images" + File.separator + "l2jmobius_128x128.png").getImage());
		
		// Set Panels.
		final JPanel systemPanel = new SystemPanel();
		final JScrollPane scrollPanel = new JScrollPane(_txtrConsole);
		// The scrollbar is always shown, so the console width never changes and printed lines keep their wrapping.
		scrollPanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scrollPanel.setBounds(0, 0, 800, 550);
		final JLayeredPane layeredPanel = new JLayeredPane();
		layeredPanel.add(scrollPanel, 0, 0);
		layeredPanel.add(systemPanel, 1, 0);
		
		// Set frame.
		final JFrame frame = new JFrame("Mobius - GameServer");
		frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		frame.addWindowListener(new WindowAdapter()
		{
			@Override
			public void windowClosing(WindowEvent ev)
			{
				if (JOptionPane.showOptionDialog(null, "Shutdown server immediately?", "Select an option", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE, null, SHUTDOWN_OPTIONS, SHUTDOWN_OPTIONS[1]) == 0)
				{
					Shutdown.getInstance().startShutdown(null, 1, false);
				}
			}
		});
		frame.addComponentListener(new ComponentAdapter()
		{
			@Override
			public void componentResized(ComponentEvent ev)
			{
				scrollPanel.setSize(frame.getContentPane().getSize());
				systemPanel.setLocation(frame.getContentPane().getWidth() - systemPanel.getWidth() - 34, systemPanel.getY());
			}
		});
		frame.setJMenuBar(menuBar);
		frame.setIconImages(icons);
		frame.add(layeredPanel, BorderLayout.CENTER);
		// The scroll bar sits beside the 800 wide panel, and both themes size it the same.
		frame.getContentPane().setPreferredSize(new Dimension(800 + UIManager.getInt("ScrollBar.width"), 550));
		frame.pack();
		WindowSize.applyDefault(frame);
		frame.setLocationRelativeTo(null);
		
		// Redirect output to text area.
		redirectSystemStreams();
		
		// Show SplashScreen.
		new SplashScreen(".." + File.separator + "images" + File.separator + "splash.png", 5000, frame);
	}
	
	/**
	 * Creates and attaches a right-click context menu to the console text area.<br>
	 * This menu allows users to copy selected or all messages to the system clipboard.<br>
	 * It dynamically updates the menu item text based on the current content.
	 */
	private void addRightClickCopyMenu()
	{
		final JPopupMenu popupMenu = new JPopupMenu();
		final JMenuItem copyItem = new JMenuItem("Copy all messages"); // Set as initial text.
		copyItem.setFont(new Font("Segoe UI", Font.PLAIN, 13));
		
		// Calculate width based on the longer text.
		popupMenu.addPopupMenuListener(new PopupMenuListener()
		{
			@Override
			public void popupMenuWillBecomeVisible(PopupMenuEvent e)
			{
				// Update text before menu becomes visible.
				updateCopyMenuItemText(copyItem);
				
				// Set preferred width based on the current text.
				popupMenu.pack();
			}
			
			@Override
			public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
			{
			}
			
			@Override
			public void popupMenuCanceled(PopupMenuEvent e)
			{
			}
		});
		
		copyItem.addActionListener(_ ->
		{
			String textToCopy = _txtrConsole.getSelectedText();
			if ((textToCopy == null) || textToCopy.isEmpty())
			{
				// No selection, copy all text.
				textToCopy = _txtrConsole.getText();
			}
			
			if ((textToCopy != null) && !textToCopy.isEmpty())
			{
				// Use StringSelection to copy to clipboard.
				final StringSelection selection = new StringSelection(textToCopy);
				final Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
				clipboard.setContents(selection, selection);
			}
		});
		
		popupMenu.add(copyItem);
		
		// Also set as component popup menu for compatibility.
		_txtrConsole.setComponentPopupMenu(popupMenu);
	}
	
	/**
	 * Updates the text of a {@code JMenuItem} based on current selection.<br>
	 * It checks if any text is selected in the console area.<br>
	 * If text is selected, it sets the label to "Copy selection".<br>
	 * Otherwise, it sets the label to "Copy all messages".
	 * @param copyItem The {@code JMenuItem} to be updated.
	 */
	private void updateCopyMenuItemText(JMenuItem copyItem)
	{
		final String selectedText = _txtrConsole.getSelectedText();
		if ((selectedText != null) && !selectedText.isEmpty())
		{
			copyItem.setText("Copy selection");
		}
		else
		{
			copyItem.setText("Copy all messages");
		}
	}
	
	/**
	 * Updates the console text area with new content.<br>
	 * This method ensures the UI updates on the correct thread.<br>
	 * It appends the provided {@code String} to the end of the text.<br>
	 * The cursor position is moved to the very end of the text.
	 * @param text The new text to add to the console.
	 */
	void updateTextArea(String text)
	{
		// Startup prints faster than the event queue drains, so text waits in a buffer and one event flushes all of it.
		// One event per print buries the queue behind thousands of them, and everything else on it waits that long too.
		final boolean pending;
		synchronized (_pendingConsole)
		{
			pending = _pendingConsole.length() > 0;
			_pendingConsole.append(text);
		}
		
		if (pending)
		{
			return;
		}
		
		SwingUtilities.invokeLater(() ->
		{
			final String buffered;
			synchronized (_pendingConsole)
			{
				buffered = _pendingConsole.toString();
				_pendingConsole.setLength(0);
			}
			
			_txtrConsole.append(buffered);
			// Asking the document for its length avoids copying the whole console on every print.
			_txtrConsole.setCaretPosition(_txtrConsole.getDocument().getLength());
		});
	}
	
	/**
	 * Redirects the standard output and error streams to the UI text area.<br>
	 * This ensures that system messages are displayed in the application window.
	 */
	private void redirectSystemStreams()
	{
		final OutputStream out = new OutputStream()
		{
			@Override
			public void write(int b)
			{
				updateTextArea(String.valueOf((char) b));
			}
			
			@Override
			public void write(byte[] b, int off, int len)
			{
				updateTextArea(new String(b, off, len));
			}
			
			@Override
			public void write(byte[] b)
			{
				write(b, 0, b.length);
			}
		};
		
		System.setOut(new PrintStream(out, true));
		System.setErr(new PrintStream(out, true));
	}
	
	/**
	 * Counts how many characters fit on one line of the console.<br>
	 * Used to draw section headers the full width of the window.
	 * @return the column count, or zero when there is no console to measure
	 */
	public static int getConsoleColumns()
	{
		if (_instance == null)
		{
			return 0;
		}
		
		// The text area is inset by its margin, which is not part of the reported width.
		final JTextArea console = _instance._txtrConsole;
		final Insets insets = console.getInsets();
		final int width = console.getWidth() - insets.left - insets.right;
		final int characterWidth = console.getFontMetrics(console.getFont()).charWidth('-');
		if ((width <= 0) || (characterWidth <= 0))
		{
			return 0;
		}
		
		// One column is kept in reserve, since a line that fills the area exactly is wrapped.
		return (width / characterWidth) - 1;
	}
}
