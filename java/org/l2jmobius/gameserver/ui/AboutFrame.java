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

import java.awt.Container;
import java.awt.Desktop;
import java.awt.Font;
import java.awt.Window.Type;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Calendar;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import org.l2jmobius.gameserver.config.ServerConfig;

/**
 * This class manages the "About" window for the game server client.<br>
 * It displays information regarding the software version and credits.<br>
 * The {@code AboutFrame} provides a simple graphical interface for users to view project details.
 * @author Mobius
 */
public class AboutFrame
{
	private static final String URL = "www.l2jmobius.org";
	
	private final URI _uri;
	private JFrame _frmAbout;
	
	/**
	 * Constructs a new {@code AboutFrame} instance.<br>
	 * This constructor initializes the UI components and sets the frame to visible.
	 */
	public AboutFrame()
	{
		initialize();
		_uri = createURI(URL);
		_frmAbout.setVisible(true);
	}
	
	/**
	 * Sets up the visual components for the about window.<br>
	 * This method configures the {@code JFrame}, labels, and layout.<br>
	 * It also populates the protocol information from {@code ServerConfig}.
	 */
	private void initialize()
	{
		_frmAbout = new JFrame();
		_frmAbout.setResizable(false);
		_frmAbout.setTitle("About");
		_frmAbout.setBounds(100, 100, 297, 197);
		_frmAbout.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		_frmAbout.setType(Type.UTILITY);
		final Container contentPane = _frmAbout.getContentPane();
		contentPane.setLayout(null);
		
		final JLabel lblLjmobius = new JLabel("L2jMobius");
		lblLjmobius.setFont(new Font("Tahoma", Font.PLAIN, 32));
		lblLjmobius.setHorizontalAlignment(SwingConstants.CENTER);
		lblLjmobius.setBounds(10, 11, 271, 39);
		contentPane.add(lblLjmobius);
		
		final JLabel lblData = new JLabel("2013-" + Calendar.getInstance().get(Calendar.YEAR));
		lblData.setHorizontalAlignment(SwingConstants.CENTER);
		lblData.setBounds(10, 44, 271, 14);
		contentPane.add(lblData);
		
		final JLabel lblSupports = new JLabel("Server Protocol");
		lblSupports.setHorizontalAlignment(SwingConstants.CENTER);
		lblSupports.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblSupports.setBounds(10, 78, 271, 23);
		contentPane.add(lblSupports);
		
		final JLabel lblProtocols = new JLabel("Protocol Number");
		lblProtocols.setHorizontalAlignment(SwingConstants.CENTER);
		lblProtocols.setFont(new Font("Tahoma", Font.PLAIN, 14));
		lblProtocols.setBounds(10, 92, 271, 23);
		contentPane.add(lblProtocols);
		
		final JLabel site = new JLabel(URL);
		site.setText("<html><font color=\"#000099\"><u>" + URL + "</u></font></html>");
		site.setHorizontalAlignment(SwingConstants.CENTER);
		site.setBounds(76, 128, 140, 14);
		site.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent arg0)
			{
				if (Desktop.isDesktopSupported())
				{
					try
					{
						Desktop.getDesktop().browse(_uri);
					}
					catch (IOException e)
					{
					}
				}
			}
		});
		contentPane.add(site);
		
		// Generate protocols text.
		String protocols = "";
		if (ServerConfig.PROTOCOL_LIST.size() > 1)
		{
			for (Integer number : ServerConfig.PROTOCOL_LIST)
			{
				if (!protocols.isEmpty())
				{
					protocols += " - ";
				}
				
				protocols += number;
			}
			
			lblSupports.setText("Server Protocols");
		}
		else
		{
			protocols += ServerConfig.PROTOCOL_LIST.get(0);
		}
		
		lblProtocols.setText(protocols);
		
		// Center frame to screen.
		_frmAbout.setLocationRelativeTo(null);
	}
	
	/**
	 * Converts a string into a {@code URI} object.<br>
	 * This method handles the conversion internally.<br>
	 * It throws an {@code IllegalArgumentException} if the string is invalid.
	 * @param str The string to be converted into a {@code URI}.
	 * @return The resulting {@code URI} object.
	 */
	private static URI createURI(String str)
	{
		try
		{
			return new URI(str);
		}
		catch (URISyntaxException x)
		{
			throw new IllegalArgumentException(x.getMessage(), x);
		}
	}
}
