package com.codeclocker.plugin.intellij.dashboard.ui;

import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.TimePeriod;
import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.TimelineDataPoint;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

/** Custom area chart panel that visualizes coding activity over time. */
public class ActivityTimelinePanel extends javax.swing.JPanel {

  private static final Color PRIMARY = new JBColor(0x7C3AED, 0xA78BFA);
  private static final Color MUTED_TEXT = new JBColor(0x5E6687, 0xA9B1D6);
  private static final Color GRID_LINE = new JBColor(0xE0E0E0, 0x3C3F41);

  private List<TimelineDataPoint> dataPoints = new ArrayList<>();
  private boolean hourlyMode = true;

  private int[] px = new int[0];
  private int[] py = new int[0];
  private int chartX;
  private int chartY;
  private int chartWidth;
  private int chartHeight;
  private long yMax;
  private int hoveredIndex = -1;

  public ActivityTimelinePanel() {
    setOpaque(false);
    setBorder(
        JBUI.Borders.compound(
            new MetricCardPanel.RoundedBorder(
                JBColor.namedColor("Borders.color", new JBColor(0xD0D0D0, 0x505050))),
            JBUI.Borders.empty(14, 14)));

    addMouseMotionListener(
        new MouseMotionAdapter() {
          @Override
          public void mouseMoved(MouseEvent e) {
            updateHoveredIndex(e.getX(), e.getY());
          }
        });
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseExited(MouseEvent e) {
            if (hoveredIndex != -1) {
              hoveredIndex = -1;
              repaint();
            }
          }
        });
  }

  private void updateHoveredIndex(int mx, int my) {
    if (px.length == 0
        || mx < chartX
        || mx > chartX + chartWidth
        || my < chartY
        || my > chartY + chartHeight) {
      if (hoveredIndex != -1) {
        hoveredIndex = -1;
        repaint();
      }
      return;
    }
    int closest = -1;
    int minDist = Integer.MAX_VALUE;
    for (int i = 0; i < px.length; i++) {
      int dist = Math.abs(mx - px[i]);
      if (dist < minDist) {
        minDist = dist;
        closest = i;
      }
    }
    int halfStep = px.length > 1 ? Math.abs(px[1] - px[0]) / 2 : chartWidth / 2;
    if (minDist > halfStep) {
      closest = -1;
    }
    if (closest != hoveredIndex) {
      hoveredIndex = closest;
      repaint();
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(super.getPreferredSize().width, JBUI.scale(280));
  }

  public void update(List<TimelineDataPoint> points, TimePeriod period) {
    this.dataPoints = points != null ? points : new ArrayList<>();
    this.hourlyMode = period == TimePeriod.LAST_24_HOURS;
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    super.paintComponent(g);
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

    // Background
    g2.setColor(getBackground());
    int arc = JBUI.scale(10);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);

    java.awt.Insets insets = getInsets();
    int x0 = insets.left;
    int y0 = insets.top;
    int totalWidth = getWidth() - insets.left - insets.right;
    int totalHeight = getHeight() - insets.top - insets.bottom;

    // Title area
    int titleY = y0;
    Font boldFont = JBUI.Fonts.label().asBold().deriveFont((float) JBUI.scale(13));
    Font smallFont = JBUI.Fonts.smallFont();

    g2.setFont(boldFont);
    g2.setColor(getForeground());
    g2.drawString("Activity Timeline", x0, titleY + g2.getFontMetrics().getAscent());
    titleY += g2.getFontMetrics().getHeight() + JBUI.scale(2);

    g2.setFont(smallFont);
    g2.setColor(MUTED_TEXT);
    g2.drawString(
        "Your coding activity over the selected period",
        x0,
        titleY + g2.getFontMetrics().getAscent());
    titleY += g2.getFontMetrics().getHeight() + JBUI.scale(10);

    // Chart margins
    int leftMargin = JBUI.scale(55);
    int bottomMargin = JBUI.scale(40);
    int rightMargin = JBUI.scale(10);
    int topMargin = JBUI.scale(5);

    chartX = x0 + leftMargin;
    chartY = titleY + topMargin;
    chartWidth = totalWidth - leftMargin - rightMargin;
    chartHeight = y0 + totalHeight - chartY - bottomMargin;

    if (chartWidth <= 0 || chartHeight <= 0) {
      g2.dispose();
      return;
    }

    // Empty state
    if (dataPoints.isEmpty() || dataPoints.stream().allMatch(p -> p.seconds() == 0)) {
      px = new int[0];
      py = new int[0];
      g2.setFont(JBUI.Fonts.label());
      g2.setColor(MUTED_TEXT);
      String msg = "No activity data for the selected period";
      int msgWidth = g2.getFontMetrics().stringWidth(msg);
      g2.drawString(msg, chartX + (chartWidth - msgWidth) / 2, chartY + chartHeight / 2);
      g2.dispose();
      return;
    }

    // Y-axis scale
    long maxData = dataPoints.stream().mapToLong(TimelineDataPoint::seconds).max().orElse(0);
    long[] ticks;

    if (hourlyMode) {
      yMax = 3600;
      ticks = new long[] {900, 1800, 2700, 3600};
    } else {
      if (maxData <= 28800) {
        yMax = 28800;
        ticks = new long[] {7200, 14400, 21600, 28800};
      } else if (maxData <= 43200) {
        yMax = 43200;
        ticks = new long[] {10800, 21600, 32400, 43200};
      } else if (maxData <= 57600) {
        yMax = 57600;
        ticks = new long[] {14400, 28800, 43200, 57600};
      } else if (maxData <= 72000) {
        yMax = 72000;
        ticks = new long[] {18000, 36000, 54000, 72000};
      } else {
        yMax = 86400;
        ticks = new long[] {21600, 43200, 64800, 86400};
      }
    }

    // Draw horizontal grid lines + Y-axis labels
    g2.setFont(smallFont);
    BasicStroke dashedStroke =
        new BasicStroke(
            1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {4, 4}, 0.0f);
    BasicStroke solidStroke = new BasicStroke(1);

    // Baseline
    g2.setStroke(solidStroke);
    g2.setColor(GRID_LINE);
    g2.drawLine(chartX, chartY + chartHeight, chartX + chartWidth, chartY + chartHeight);

    for (long tick : ticks) {
      int ty = chartY + chartHeight - (int) ((double) tick / yMax * chartHeight);
      g2.setStroke(dashedStroke);
      g2.setColor(GRID_LINE);
      g2.drawLine(chartX, ty, chartX + chartWidth, ty);

      g2.setStroke(solidStroke);
      g2.setColor(MUTED_TEXT);
      String label = formatYLabel(tick);
      int labelWidth = g2.getFontMetrics().stringWidth(label);
      g2.drawString(
          label, chartX - labelWidth - JBUI.scale(6), ty + g2.getFontMetrics().getAscent() / 2);
    }

    // Draw "0" at baseline
    g2.setColor(MUTED_TEXT);
    String zeroLabel = "0";
    int zeroWidth = g2.getFontMetrics().stringWidth(zeroLabel);
    g2.drawString(
        zeroLabel,
        chartX - zeroWidth - JBUI.scale(6),
        chartY + chartHeight + g2.getFontMetrics().getAscent() / 2);

    // Compute data point positions
    int n = dataPoints.size();
    px = new int[n];
    py = new int[n];

    for (int i = 0; i < n; i++) {
      px[i] = chartX + (n > 1 ? (int) ((double) i / (n - 1) * chartWidth) : chartWidth / 2);
      double ratio = Math.min((double) dataPoints.get(i).seconds() / yMax, 1.0);
      py[i] = chartY + chartHeight - (int) (ratio * chartHeight);
    }

    // Build smooth curve path
    GeneralPath curvePath = new GeneralPath();
    curvePath.moveTo(px[0], py[0]);

    if (n == 1) {
      // Single point - just a dot, no curve
    } else if (n == 2) {
      curvePath.lineTo(px[1], py[1]);
    } else {
      for (int i = 0; i < n - 1; i++) {
        double midX = (px[i] + px[i + 1]) / 2.0;
        curvePath.curveTo(midX, py[i], midX, py[i + 1], px[i + 1], py[i + 1]);
      }
    }

    // Gradient fill area
    if (n > 1) {
      GeneralPath areaPath = (GeneralPath) curvePath.clone();
      areaPath.lineTo(px[n - 1], chartY + chartHeight);
      areaPath.lineTo(px[0], chartY + chartHeight);
      areaPath.closePath();

      Color primaryResolved = PRIMARY;
      Color topColor =
          new Color(
              primaryResolved.getRed(), primaryResolved.getGreen(), primaryResolved.getBlue(), 102);
      Color bottomColor =
          new Color(
              primaryResolved.getRed(), primaryResolved.getGreen(), primaryResolved.getBlue(), 13);
      GradientPaint gradient =
          new GradientPaint(0, chartY, topColor, 0, chartY + chartHeight, bottomColor);
      g2.setPaint(gradient);
      g2.fill(areaPath);
    }

    // Draw curve line
    if (n > 1) {
      g2.setColor(PRIMARY);
      g2.setStroke(new BasicStroke(JBUI.scale(2), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
      g2.draw(curvePath);
    }

    // Data dots (only when <= 31 points)
    if (n <= 31) {
      int dotRadius = JBUI.scale(3);
      g2.setColor(PRIMARY);
      for (int i = 0; i < n; i++) {
        if (dataPoints.get(i).seconds() > 0) {
          g2.fillOval(px[i] - dotRadius, py[i] - dotRadius, dotRadius * 2, dotRadius * 2);
        }
      }
    }

    // X-axis labels
    g2.setFont(smallFont);
    g2.setColor(MUTED_TEXT);
    int labelInterval = computeLabelInterval(n, chartWidth, g2);
    int xLabelY = chartY + chartHeight + JBUI.scale(16);

    for (int i = 0; i < n; i += labelInterval) {
      String label = dataPoints.get(i).label();
      int labelWidth = g2.getFontMetrics().stringWidth(label);
      g2.drawString(label, px[i] - labelWidth / 2, xLabelY);
    }

    // Hover tooltip overlay
    if (hoveredIndex >= 0 && hoveredIndex < n) {
      paintTooltip(g2, boldFont, smallFont);
    }

    g2.dispose();
  }

  private void paintTooltip(Graphics2D g2, Font boldFont, Font smallFont) {
    int hx = px[hoveredIndex];
    int hy = py[hoveredIndex];

    // Vertical crosshair line
    Color mutedResolved = MUTED_TEXT;
    g2.setColor(
        new Color(mutedResolved.getRed(), mutedResolved.getGreen(), mutedResolved.getBlue(), 102));
    g2.setStroke(
        new BasicStroke(
            1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10.0f, new float[] {4, 4}, 0.0f));
    g2.drawLine(hx, chartY, hx, chartY + chartHeight);

    // Highlighted dot
    int dotR = JBUI.scale(5);
    g2.setStroke(new BasicStroke(JBUI.scale(2)));
    g2.setColor(getBackground());
    g2.fillOval(hx - dotR, hy - dotR, dotR * 2, dotR * 2);
    g2.setColor(PRIMARY);
    g2.drawOval(hx - dotR, hy - dotR, dotR * 2, dotR * 2);
    g2.fillOval(hx - JBUI.scale(3), hy - JBUI.scale(3), JBUI.scale(6), JBUI.scale(6));

    // Tooltip box
    String line1 = dataPoints.get(hoveredIndex).label();
    String line2 = formatTooltipValue(dataPoints.get(hoveredIndex).seconds());

    g2.setFont(boldFont);
    FontMetrics fmBold = g2.getFontMetrics();
    int line1Width = fmBold.stringWidth(line1);
    int line1Height = fmBold.getHeight();

    g2.setFont(smallFont);
    FontMetrics fmSmall = g2.getFontMetrics();
    int line2Width = fmSmall.stringWidth(line2);
    int line2Height = fmSmall.getHeight();

    int pad = JBUI.scale(6);
    int gap = JBUI.scale(2);
    int boxWidth = Math.max(line1Width, line2Width) + pad * 2;
    int boxHeight = line1Height + gap + line2Height + pad * 2;

    int boxX = hx - boxWidth / 2;
    int boxY = hy - boxHeight - JBUI.scale(12);

    // Clamp horizontally
    if (boxX < chartX) {
      boxX = chartX;
    } else if (boxX + boxWidth > chartX + chartWidth) {
      boxX = chartX + chartWidth - boxWidth;
    }
    // If too close to top, show below
    if (boxY < chartY) {
      boxY = hy + JBUI.scale(12);
    }

    int boxArc = JBUI.scale(6);
    g2.setColor(getBackground());
    g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, boxArc, boxArc);
    g2.setStroke(new BasicStroke(1));
    g2.setColor(GRID_LINE);
    g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, boxArc, boxArc);

    g2.setFont(boldFont);
    g2.setColor(getForeground());
    g2.drawString(line1, boxX + pad, boxY + pad + fmBold.getAscent());

    g2.setFont(smallFont);
    g2.setColor(MUTED_TEXT);
    g2.drawString(line2, boxX + pad, boxY + pad + line1Height + gap + fmSmall.getAscent());
  }

  private String formatTooltipValue(long seconds) {
    if (seconds <= 0) return "No activity";
    long h = seconds / 3600;
    long m = (seconds % 3600) / 60;
    long s = seconds % 60;
    if (h > 0) return h + "h " + m + "m " + s + "s";
    if (m > 0) return m + "m " + s + "s";
    return s + "s";
  }

  private int computeLabelInterval(int n, int chartWidth, Graphics2D g2) {
    if (n <= 1) return 1;
    int avgLabelWidth = g2.getFontMetrics().stringWidth("MMM 00") + JBUI.scale(10);
    int maxLabels = Math.max(1, chartWidth / avgLabelWidth);
    return Math.max(1, (int) Math.ceil((double) n / maxLabels));
  }

  private String formatYLabel(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    if (hours > 0 && minutes == 0) {
      return hours + "h";
    }
    if (hours > 0) {
      return hours + "h " + minutes + "m";
    }
    return minutes + "m";
  }
}
