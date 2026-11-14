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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.List;

import javax.swing.JComponent;

import org.l2jmobius.tools.dashboard.health.RuntimeSample;
import org.l2jmobius.tools.dashboard.health.SampleSeries;
import org.l2jmobius.tools.dashboard.runtime.ServiceRole;

/**
 * Draws one compact historical runtime metric using the Control Center chart proportions.
 * @author BazookaRpm
 */
public final class TelemetryChart extends JComponent
{
	public enum Metric
	{
		CPU,
		HEAP,
		THREADS,
		ACTIVITY
	}
	
	private static final BasicStroke LINE = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
	
	private final String _title;
	private final SampleSeries _series;
	private final Metric _metric;
	private final ServiceRole _role;
	
	public TelemetryChart(String title, SampleSeries series, Metric metric, ServiceRole role)
	{
		_title = title.toUpperCase();
		_series = series;
		_metric = metric;
		_role = role;
		setPreferredSize(new Dimension(320, 160));
		setOpaque(false);
	}
	
	@Override
	protected void paintComponent(Graphics graphics)
	{
		final Graphics2D canvas = (Graphics2D) graphics.create();
		canvas.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		canvas.setColor(ControlCenterTheme.MUTED);
		canvas.setFont(ControlCenterTheme.font(Font.BOLD, 10));
		canvas.drawString(_title, 12, 18);
		canvas.setColor(ControlCenterTheme.BORDER);
		canvas.drawRoundRect(8, 28, getWidth() - 16, getHeight() - 38, 10, 10);
		final List<RuntimeSample> samples = _series.snapshot();
		if (samples.size() < 2)
		{
			canvas.dispose();
			return;
		}
		
		final int left = 18;
		final int top = 38;
		final int width = Math.max(1, getWidth() - 36);
		final int height = Math.max(1, getHeight() - 58);
		final double maximum = maximum(samples);
		canvas.setColor(ControlCenterTheme.METRIC);
		canvas.setStroke(LINE);
		int previousX = left;
		int previousY = top + height;
		for (int index = 0; index < samples.size(); index++)
		{
			final RuntimeSample sample = samples.get(index);
			final int x = left + (int) Math.round((double) index * width / (samples.size() - 1));
			final double value = Math.max(0, sampleValue(sample));
			final int y = top + height - (int) Math.round((value / maximum) * height);
			if (index > 0)
			{
				canvas.drawLine(previousX, previousY, x, y);
			}
			
			previousX = x;
			previousY = y;
		}
		
		canvas.dispose();
	}
	
	private double maximum(List<RuntimeSample> samples)
	{
		if ((_metric == Metric.CPU) || (_metric == Metric.HEAP))
		{
			return 1.0;
		}
		
		double maximum = 1.0;
		for (RuntimeSample sample : samples)
		{
			maximum = Math.max(maximum, sampleValue(sample));
		}
		
		return maximum;
	}
	
	private double sampleValue(RuntimeSample sample)
	{
		switch (_metric)
		{
			case CPU:
			{
				return sample.processCpuRatio();
			}
			case HEAP:
			{
				return sample.heapRatio();
			}
			case THREADS:
			{
				return sample.threadCount();
			}
			case ACTIVITY:
			default:
			{
				return _role == ServiceRole.GAME ? sample.onlinePlayers() : sample.authenticatedClients();
			}
		}
	}
}
