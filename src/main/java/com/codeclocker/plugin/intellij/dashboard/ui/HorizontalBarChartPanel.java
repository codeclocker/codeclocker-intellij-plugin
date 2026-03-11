package com.codeclocker.plugin.intellij.dashboard.ui;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JPanel;

/** Horizontal bar chart panel showing ranked items by time spent. */
public class HorizontalBarChartPanel extends JPanel {

  private static final Color MUTED_TEXT = new JBColor(0x5E6687, 0xA9B1D6);
  private static final Color GRID_LINE = new JBColor(0xE0E0E0, 0x3C3F41);

  private static final Color[] PALETTE = {
    new Color(0x3B82F6),
    new Color(0x22D3EE),
    new Color(0x10B981),
    new Color(0xEC4899),
    new Color(0x8B5CF6),
    new Color(0xF97316),
    new Color(0x14B8A6),
  };

  private static final int ROW_HEIGHT = 32;
  private static final int BAR_LEFT_MARGIN = 8;
  private static final int LABEL_PADDING = 12;
  private static final double MAX_LABEL_RATIO = 0.4;

  public record BarEntry(String label, long seconds) {}

  private final String title;
  private final String subtitle;
  private final int maxItems;
  private List<BarEntry> entries = new ArrayList<>();

  public HorizontalBarChartPanel(String title, String subtitle, int maxItems) {
    this.title = title;
    this.subtitle = subtitle;
    this.maxItems = maxItems;
    setOpaque(false);
    setBorder(
        JBUI.Borders.compound(
            new MetricCardPanel.RoundedBorder(
                JBColor.namedColor("Borders.color", new JBColor(0xD0D0D0, 0x505050))),
            JBUI.Borders.empty(14, 14)));
  }

  public void update(List<BarEntry> newEntries) {
    if (newEntries == null || newEntries.isEmpty()) {
      this.entries = new ArrayList<>();
    } else {
      this.entries = newEntries.size() > maxItems ? newEntries.subList(0, maxItems) : newEntries;
    }
    revalidate();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    Insets insets = getInsets();
    int titleHeight = JBUI.scale(45);
    int axisHeight = JBUI.scale(24);
    int itemCount = Math.max(maxItems, 3);
    int rowsHeight = itemCount * JBUI.scale(ROW_HEIGHT);
    return new Dimension(
        super.getPreferredSize().width,
        insets.top + titleHeight + rowsHeight + axisHeight + insets.bottom);
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

    Insets insets = getInsets();
    int x0 = insets.left;
    int y0 = insets.top;
    int totalWidth = getWidth() - insets.left - insets.right;

    Font boldFont = JBUI.Fonts.label().asBold().deriveFont((float) JBUI.scale(13));
    Font smallFont = JBUI.Fonts.smallFont();

    // Title
    int titleY = y0;
    g2.setFont(boldFont);
    g2.setColor(getForeground());
    g2.drawString(title, x0, titleY + g2.getFontMetrics().getAscent());
    titleY += g2.getFontMetrics().getHeight() + JBUI.scale(2);

    g2.setFont(smallFont);
    g2.setColor(MUTED_TEXT);
    g2.drawString(subtitle, x0, titleY + g2.getFontMetrics().getAscent());
    titleY += g2.getFontMetrics().getHeight() + JBUI.scale(10);

    // Empty state
    if (entries.isEmpty()) {
      g2.setFont(JBUI.Fonts.label());
      g2.setColor(MUTED_TEXT);
      String msg = "No data for the selected period";
      int msgWidth = g2.getFontMetrics().stringWidth(msg);
      g2.drawString(msg, x0 + (totalWidth - msgWidth) / 2, titleY + JBUI.scale(30));
      g2.dispose();
      return;
    }

    int scaledBarLeftMargin = JBUI.scale(BAR_LEFT_MARGIN);
    int scaledRowHeight = JBUI.scale(ROW_HEIGHT);
    int scaledPadding = JBUI.scale(LABEL_PADDING);
    int maxLabelWidth = (int) (totalWidth * MAX_LABEL_RATIO);
    int minLabelWidth = JBUI.scale(60);

    Font labelFont = JBUI.Fonts.label().deriveFont((float) JBUI.scale(12));
    g2.setFont(labelFont);
    FontMetrics labelFm = g2.getFontMetrics();

    // Compute label width from actual text
    int naturalLabelWidth = 0;
    for (BarEntry entry : entries) {
      naturalLabelWidth = Math.max(naturalLabelWidth, labelFm.stringWidth(entry.label()));
    }
    int scaledLabelWidth =
        Math.max(minLabelWidth, Math.min(naturalLabelWidth + scaledPadding, maxLabelWidth));

    int barAreaX = x0 + scaledLabelWidth + scaledBarLeftMargin;
    int barAreaWidth = totalWidth - scaledLabelWidth - scaledBarLeftMargin;

    // Find max value for scaling
    long maxSeconds = 0;
    for (BarEntry entry : entries) {
      maxSeconds = Math.max(maxSeconds, entry.seconds());
    }
    if (maxSeconds <= 0) {
      maxSeconds = 1;
    }

    // Compute nice axis max and tick interval
    long axisMax = computeNiceMax(maxSeconds);
    long tickInterval = computeTickInterval(axisMax);

    // Draw bars
    int barHeight = JBUI.scale(20);
    int barArc = JBUI.scale(4);
    g2.setFont(labelFont);

    for (int i = 0; i < entries.size(); i++) {
      BarEntry entry = entries.get(i);
      int rowY = titleY + i * scaledRowHeight;
      int barY = rowY + (scaledRowHeight - barHeight) / 2;

      // Label on the left
      g2.setColor(getForeground());
      String label = truncateText(entry.label(), labelFm, scaledLabelWidth - JBUI.scale(8));
      int labelY = rowY + scaledRowHeight / 2 + labelFm.getAscent() / 2 - 1;
      int labelW = labelFm.stringWidth(label);
      g2.drawString(label, x0 + scaledLabelWidth - labelW - JBUI.scale(4), labelY);

      // Bar
      double ratio = (double) entry.seconds() / axisMax;
      int barWidth = Math.max(JBUI.scale(4), (int) (ratio * barAreaWidth));
      Color barColor = PALETTE[i % PALETTE.length];
      g2.setColor(barColor);
      g2.fillRoundRect(barAreaX, barY, barWidth, barHeight, barArc, barArc);
    }

    // X-axis ticks and labels
    int axisY = titleY + entries.size() * scaledRowHeight;
    g2.setFont(smallFont);
    FontMetrics smallFm = g2.getFontMetrics();
    g2.setColor(GRID_LINE);
    g2.drawLine(barAreaX, axisY, barAreaX + barAreaWidth, axisY);

    g2.setColor(MUTED_TEXT);
    for (long tick = 0; tick <= axisMax; tick += tickInterval) {
      double ratio = (double) tick / axisMax;
      int tickX = barAreaX + (int) (ratio * barAreaWidth);
      g2.setColor(GRID_LINE);
      g2.drawLine(tickX, axisY, tickX, axisY + JBUI.scale(4));
      g2.setColor(MUTED_TEXT);
      String tickLabel = formatTimeShort(tick);
      int tickLabelW = smallFm.stringWidth(tickLabel);
      g2.drawString(tickLabel, tickX - tickLabelW / 2, axisY + JBUI.scale(4) + smallFm.getAscent());
    }

    g2.dispose();
  }

  private static long computeNiceMax(long maxSeconds) {
    if (maxSeconds <= 60) {
      return ceilTo(maxSeconds, 10);
    }
    if (maxSeconds <= 300) {
      return ceilTo(maxSeconds, 60);
    }
    if (maxSeconds <= 3600) {
      return ceilTo(maxSeconds, 300);
    }
    return ceilTo(maxSeconds, 3600);
  }

  private static long computeTickInterval(long axisMax) {
    if (axisMax <= 60) {
      return Math.max(10, axisMax / 4);
    }
    if (axisMax <= 300) {
      return 60;
    }
    if (axisMax <= 3600) {
      return Math.max(300, axisMax / 5);
    }
    long hours = axisMax / 3600;
    if (hours <= 5) {
      return 3600;
    }
    return ceilTo(hours / 4, 1) * 3600;
  }

  private static long ceilTo(long value, long step) {
    return ((value + step - 1) / step) * step;
  }

  private static String formatTimeShort(long seconds) {
    if (seconds <= 0) {
      return "0m";
    }
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    if (hours > 0 && minutes > 0) {
      return hours + "h" + minutes + "m";
    }
    if (hours > 0) {
      return hours + "h";
    }
    return minutes + "m";
  }

  private static String truncateText(String text, FontMetrics fm, int maxWidth) {
    if (fm.stringWidth(text) <= maxWidth) {
      return text;
    }
    String ellipsis = "...";
    int ellipsisWidth = fm.stringWidth(ellipsis);
    for (int i = text.length() - 1; i > 0; i--) {
      if (fm.stringWidth(text.substring(0, i)) + ellipsisWidth <= maxWidth) {
        return text.substring(0, i) + ellipsis;
      }
    }
    return ellipsis;
  }
}
