package com.codeclocker.plugin.intellij.dashboard.ui;

import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.ProjectTimelineData;
import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.ProjectTimelineEntry;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.JPanel;

/** Gantt-style heatmap showing when each project was worked on. */
public class ProjectTimelineGanttPanel extends JPanel {

  private static final Color MUTED_TEXT = new JBColor(0x5E6687, 0xA9B1D6);
  private static final Color GRID_LINE = new JBColor(0xE0E0E0, 0x3C3F41);

  private static final Color[] PALETTE = {
    new Color(0x27ADE2),
    new Color(0xC22572),
    new Color(0x8A50BE),
    new Color(0x1E88E5),
    new Color(0x26A69A),
    new Color(0x7E57C2),
    new Color(0xEC407A),
    new Color(0x42A5F5),
    new Color(0xFF7043),
  };

  private static final Color LEGEND_COLOR = new Color(0x8A50BE);
  private static final double[] LEGEND_OPACITIES = {0.0, 0.4, 0.65, 0.85, 1.0};

  private static final int ROW_HEIGHT = 36;
  private static final int LABEL_WIDTH = 160;
  private static final int TIME_WIDTH = 110;
  private static final int CELL_GAP = 2;
  private static final int CELL_MIN_WIDTH = 12;

  private static final DateTimeFormatter HOUR_KEY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");

  private ProjectTimelineData data;
  private Map<String, Color> colorMap = new HashMap<>();
  private long maxValue;

  // Tooltip state
  private int hoverRow = -1;
  private int hoverCol = -1;
  private int mouseX;
  private int mouseY;

  public ProjectTimelineGanttPanel() {
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
            mouseX = e.getX();
            mouseY = e.getY();
            updateHover(e.getX(), e.getY());
          }
        });
    addMouseListener(
        new MouseAdapter() {
          @Override
          public void mouseExited(MouseEvent e) {
            if (hoverRow != -1 || hoverCol != -1) {
              hoverRow = -1;
              hoverCol = -1;
              repaint();
            }
          }
        });
  }

  public void update(ProjectTimelineData newData) {
    this.data = newData;
    this.hoverRow = -1;
    this.hoverCol = -1;

    if (newData != null && !newData.entries().isEmpty()) {
      // Assign colors
      colorMap.clear();
      List<String> usedIndices = new ArrayList<>();
      for (ProjectTimelineEntry entry : newData.entries()) {
        int hash = 0;
        for (char c : entry.projectName().toCharArray()) {
          hash = hash * 31 + c;
        }
        int idx = Math.abs(hash) % PALETTE.length;
        // Handle collisions
        while (usedIndices.contains(String.valueOf(idx)) && usedIndices.size() < PALETTE.length) {
          idx = (idx + 1) % PALETTE.length;
        }
        usedIndices.add(String.valueOf(idx));
        colorMap.put(entry.projectName(), PALETTE[idx]);
      }

      // Find max value across all cells
      maxValue = 0;
      for (ProjectTimelineEntry entry : newData.entries()) {
        for (long val : entry.dailySeconds().values()) {
          maxValue = Math.max(maxValue, val);
        }
      }
    }

    revalidate();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    int numProjects = data != null ? data.entries().size() : 0;
    int titleHeight = JBUI.scale(45);
    int headerRowHeight = JBUI.scale(24);
    int legendHeight = JBUI.scale(30);
    int rowsHeight = numProjects > 0 ? numProjects * JBUI.scale(ROW_HEIGHT) : JBUI.scale(60);
    return new Dimension(
        super.getPreferredSize().width,
        titleHeight + headerRowHeight + rowsHeight + legendHeight + JBUI.scale(10));
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

    // Title
    Font boldFont = JBUI.Fonts.label().asBold().deriveFont((float) JBUI.scale(13));
    Font smallFont = JBUI.Fonts.smallFont();

    int titleY = y0;
    g2.setFont(boldFont);
    g2.setColor(getForeground());
    g2.drawString("Project Timeline", x0, titleY + g2.getFontMetrics().getAscent());
    titleY += g2.getFontMetrics().getHeight() + JBUI.scale(2);

    g2.setFont(smallFont);
    g2.setColor(MUTED_TEXT);
    g2.drawString("When you worked on each project", x0, titleY + g2.getFontMetrics().getAscent());
    titleY += g2.getFontMetrics().getHeight() + JBUI.scale(10);

    // Empty state
    if (data == null || data.entries().isEmpty()) {
      g2.setFont(JBUI.Fonts.label());
      g2.setColor(MUTED_TEXT);
      String msg = "No project timeline data yet";
      int msgWidth = g2.getFontMetrics().stringWidth(msg);
      g2.drawString(msg, x0 + (totalWidth - msgWidth) / 2, titleY + JBUI.scale(30));
      g2.dispose();
      return;
    }

    int scaledLabelWidth = JBUI.scale(LABEL_WIDTH);
    int scaledTimeWidth = JBUI.scale(TIME_WIDTH);
    int scaledRowHeight = JBUI.scale(ROW_HEIGHT);
    int scaledCellGap = JBUI.scale(CELL_GAP);
    int scaledCellMin = JBUI.scale(CELL_MIN_WIDTH);
    int headerHeight = JBUI.scale(24);

    int numBuckets = data.buckets().size();
    int gridAreaWidth = totalWidth - scaledLabelWidth - scaledTimeWidth;
    int cellWidth =
        numBuckets > 0
            ? Math.max(
                scaledCellMin, (gridAreaWidth - (numBuckets - 1) * scaledCellGap) / numBuckets)
            : scaledCellMin;

    int gridX = x0 + scaledLabelWidth;

    // Date headers
    g2.setFont(smallFont);
    g2.setColor(MUTED_TEXT);
    FontMetrics smallFm = g2.getFontMetrics();
    int headerY = titleY;
    List<String> dateLabels = computeDateLabels(data.buckets(), data.hourly());
    for (int i = 0; i < numBuckets; i++) {
      String label = dateLabels.get(i);
      if (label != null && !label.isEmpty()) {
        int cx = gridX + i * (cellWidth + scaledCellGap);
        int labelW = smallFm.stringWidth(label);
        int drawX = cx + (cellWidth - labelW) / 2;
        g2.drawString(label, drawX, headerY + smallFm.getAscent());
      }
    }
    int rowStartY = headerY + headerHeight;

    // Per-project rows
    long totalAllProjects = 0;
    for (ProjectTimelineEntry entry : data.entries()) {
      totalAllProjects += entry.totalTimeSeconds();
    }

    Font labelFont = JBUI.Fonts.label().deriveFont((float) JBUI.scale(12));
    g2.setFont(labelFont);
    FontMetrics labelFm = g2.getFontMetrics();

    for (int row = 0; row < data.entries().size(); row++) {
      ProjectTimelineEntry entry = data.entries().get(row);
      Color baseColor = colorMap.getOrDefault(entry.projectName(), PALETTE[0]);
      int rowY = rowStartY + row * scaledRowHeight;
      int cellCenterY = rowY + scaledRowHeight / 2;

      // Left zone: dot + name + percentage
      int dotSize = JBUI.scale(8);
      int dotY = cellCenterY - dotSize / 2;
      g2.setColor(baseColor);
      g2.fillOval(x0, dotY, dotSize, dotSize);

      g2.setFont(labelFont);
      g2.setColor(getForeground());
      String projectName =
          truncateText(entry.projectName(), labelFm, scaledLabelWidth - JBUI.scale(55));
      g2.drawString(
          projectName, x0 + dotSize + JBUI.scale(6), cellCenterY + labelFm.getAscent() / 2 - 1);

      // Percentage
      int pct =
          totalAllProjects > 0
              ? (int) Math.round(100.0 * entry.totalTimeSeconds() / totalAllProjects)
              : 0;
      g2.setFont(smallFont);
      g2.setColor(MUTED_TEXT);
      String pctStr = pct + "%";
      g2.drawString(
          pctStr,
          x0 + scaledLabelWidth - smallFm.stringWidth(pctStr) - JBUI.scale(8),
          cellCenterY + smallFm.getAscent() / 2 - 1);

      // Center zone: cells
      int cellHeight = scaledRowHeight - JBUI.scale(8);
      int cellY = rowY + JBUI.scale(4);
      int cellArc = JBUI.scale(3);

      for (int col = 0; col < numBuckets; col++) {
        int cx = gridX + col * (cellWidth + scaledCellGap);
        String bucket = data.buckets().get(col);
        long val = entry.dailySeconds().getOrDefault(bucket, 0L);

        if (val > 0 && maxValue > 0) {
          double opacity = 0.15 + ((double) val / maxValue) * 0.85;
          g2.setColor(withOpacity(baseColor, opacity));
        } else {
          // Zero-value cell
          boolean dark = !JBColor.isBright();
          g2.setColor(dark ? new Color(255, 255, 255, 13) : new Color(0, 0, 0, 10));
        }
        g2.fillRoundRect(cx, cellY, cellWidth, cellHeight, cellArc, cellArc);
      }

      // Right zone: total time
      g2.setFont(smallFont);
      g2.setColor(MUTED_TEXT);
      String timeStr = formatTime(entry.totalTimeSeconds());
      int timeX = x0 + totalWidth - smallFm.stringWidth(timeStr);
      g2.drawString(timeStr, timeX, cellCenterY + smallFm.getAscent() / 2 - 1);
    }

    // Legend at bottom right
    int legendY = rowStartY + data.entries().size() * scaledRowHeight + JBUI.scale(8);
    paintLegend(g2, smallFont, x0 + totalWidth, legendY);

    // Tooltip
    if (hoverRow >= 0
        && hoverRow < data.entries().size()
        && hoverCol >= 0
        && hoverCol < numBuckets) {
      paintTooltip(g2, boldFont, smallFont, gridX, cellWidth, scaledCellGap);
    }

    g2.dispose();
  }

  private void paintLegend(Graphics2D g2, Font smallFont, int rightEdge, int y) {
    g2.setFont(smallFont);
    FontMetrics fm = g2.getFontMetrics();
    int boxSize = JBUI.scale(12);
    int boxGap = JBUI.scale(3);

    String moreLabel = "More";
    String lessLabel = "Less";
    int moreWidth = fm.stringWidth(moreLabel);
    int lessWidth = fm.stringWidth(lessLabel);
    int totalLegendWidth =
        lessWidth
            + JBUI.scale(6)
            + LEGEND_OPACITIES.length * (boxSize + boxGap)
            + JBUI.scale(6)
            + moreWidth;

    int lx = rightEdge - totalLegendWidth;
    int textY = y + fm.getAscent();

    g2.setColor(MUTED_TEXT);
    g2.drawString(lessLabel, lx, textY);
    lx += lessWidth + JBUI.scale(6);

    for (double opacity : LEGEND_OPACITIES) {
      if (opacity <= 0.001) {
        boolean dark = !JBColor.isBright();
        g2.setColor(dark ? new Color(255, 255, 255, 13) : new Color(0, 0, 0, 10));
      } else {
        g2.setColor(withOpacity(LEGEND_COLOR, opacity));
      }
      g2.fillRoundRect(lx, y, boxSize, boxSize, JBUI.scale(2), JBUI.scale(2));
      lx += boxSize + boxGap;
    }

    lx += JBUI.scale(3);
    g2.setColor(MUTED_TEXT);
    g2.drawString(moreLabel, lx, textY);
  }

  private void paintTooltip(
      Graphics2D g2, Font boldFont, Font smallFont, int gridX, int cellWidth, int cellGap) {
    ProjectTimelineEntry entry = data.entries().get(hoverRow);
    String bucket = data.buckets().get(hoverCol);
    long val = entry.dailySeconds().getOrDefault(bucket, 0L);

    String line1 = entry.projectName();
    String line2 = formatBucketLabel(bucket, data.hourly());
    String line3 = val > 0 ? formatTime(val) : "No activity";

    g2.setFont(boldFont);
    FontMetrics fmBold = g2.getFontMetrics();
    int line1Width = fmBold.stringWidth(line1);

    g2.setFont(smallFont);
    FontMetrics fmSmall = g2.getFontMetrics();
    int line2Width = fmSmall.stringWidth(line2);
    int line3Width = fmSmall.stringWidth(line3);

    int pad = JBUI.scale(8);
    int gap = JBUI.scale(3);
    int boxWidth = Math.max(line1Width, Math.max(line2Width, line3Width)) + pad * 2;
    int boxHeight =
        fmBold.getHeight() + gap + fmSmall.getHeight() + gap + fmSmall.getHeight() + pad * 2;

    int boxX = mouseX + JBUI.scale(12);
    int boxY = mouseY - boxHeight - JBUI.scale(4);

    // Clamp to panel bounds
    Insets insets = getInsets();
    if (boxX + boxWidth > getWidth() - insets.right) {
      boxX = mouseX - boxWidth - JBUI.scale(12);
    }
    if (boxY < insets.top) {
      boxY = mouseY + JBUI.scale(12);
    }

    int boxArc = JBUI.scale(6);
    g2.setColor(getBackground());
    g2.fillRoundRect(boxX, boxY, boxWidth, boxHeight, boxArc, boxArc);
    g2.setColor(GRID_LINE);
    g2.drawRoundRect(boxX, boxY, boxWidth, boxHeight, boxArc, boxArc);

    int textY = boxY + pad;
    g2.setFont(boldFont);
    g2.setColor(getForeground());
    g2.drawString(line1, boxX + pad, textY + fmBold.getAscent());
    textY += fmBold.getHeight() + gap;

    g2.setFont(smallFont);
    g2.setColor(MUTED_TEXT);
    g2.drawString(line2, boxX + pad, textY + fmSmall.getAscent());
    textY += fmSmall.getHeight() + gap;

    Color baseColor = colorMap.getOrDefault(entry.projectName(), PALETTE[0]);
    g2.setColor(val > 0 ? baseColor : MUTED_TEXT);
    g2.drawString(line3, boxX + pad, textY + fmSmall.getAscent());
  }

  private void updateHover(int mx, int my) {
    if (data == null || data.entries().isEmpty()) {
      if (hoverRow != -1) {
        hoverRow = -1;
        hoverCol = -1;
        repaint();
      }
      return;
    }

    Insets insets = getInsets();
    int x0 = insets.left;
    int totalWidth = getWidth() - insets.left - insets.right;

    int scaledLabelWidth = JBUI.scale(LABEL_WIDTH);
    int scaledTimeWidth = JBUI.scale(TIME_WIDTH);
    int scaledRowHeight = JBUI.scale(ROW_HEIGHT);
    int scaledCellGap = JBUI.scale(CELL_GAP);
    int scaledCellMin = JBUI.scale(CELL_MIN_WIDTH);

    int numBuckets = data.buckets().size();
    int gridAreaWidth = totalWidth - scaledLabelWidth - scaledTimeWidth;
    int cellWidth =
        numBuckets > 0
            ? Math.max(
                scaledCellMin, (gridAreaWidth - (numBuckets - 1) * scaledCellGap) / numBuckets)
            : scaledCellMin;

    int gridX = x0 + scaledLabelWidth;

    // Compute title area height (must match paintComponent)
    Font boldFont = JBUI.Fonts.label().asBold().deriveFont((float) JBUI.scale(13));
    Font smallFont = JBUI.Fonts.smallFont();
    FontMetrics fmBold = getFontMetrics(boldFont);
    FontMetrics fmSmall = getFontMetrics(smallFont);
    int titleHeight =
        insets.top + fmBold.getHeight() + JBUI.scale(2) + fmSmall.getHeight() + JBUI.scale(10);
    int headerHeight = JBUI.scale(24);
    int rowStartY = titleHeight + headerHeight;

    int newRow = -1;
    int newCol = -1;

    if (my >= rowStartY && my < rowStartY + data.entries().size() * scaledRowHeight) {
      newRow = (my - rowStartY) / scaledRowHeight;
      if (newRow >= data.entries().size()) {
        newRow = -1;
      }
    }

    if (mx >= gridX && newRow >= 0) {
      int relX = mx - gridX;
      int step = cellWidth + scaledCellGap;
      if (step > 0) {
        int col = relX / step;
        if (col >= 0 && col < numBuckets) {
          int cellStart = col * step;
          if (relX >= cellStart && relX < cellStart + cellWidth) {
            newCol = col;
          }
        }
      }
    }

    if (newRow != hoverRow || newCol != hoverCol) {
      hoverRow = newRow;
      hoverCol = newCol;
      repaint();
    }
  }

  private List<String> computeDateLabels(List<String> buckets, boolean hourly) {
    int n = buckets.size();
    List<String> labels = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      labels.add(null);
    }

    if (hourly) {
      // Show "HH" every 2nd or 4th hour
      int step = n > 12 ? 4 : 2;
      DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("HH");
      for (int i = 0; i < n; i += step) {
        try {
          LocalDateTime dt = LocalDateTime.parse(buckets.get(i), HOUR_KEY_FORMATTER);
          labels.set(i, dt.format(hourFmt));
        } catch (Exception ignored) {
        }
      }
    } else if (n <= 7) {
      DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("EEE");
      for (int i = 0; i < n; i++) {
        try {
          labels.set(i, LocalDate.parse(buckets.get(i)).format(dayFmt));
        } catch (Exception ignored) {
        }
      }
    } else if (n <= 14) {
      DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM d");
      for (int i = 0; i < n; i++) {
        try {
          labels.set(i, LocalDate.parse(buckets.get(i)).format(dateFmt));
        } catch (Exception ignored) {
        }
      }
    } else {
      // 15+ days: show every Nth
      int step;
      if (n <= 21) {
        step = 3;
      } else if (n <= 42) {
        step = 5;
      } else {
        step = 7;
      }
      DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("MMM d");
      for (int i = 0; i < n; i += step) {
        try {
          labels.set(i, LocalDate.parse(buckets.get(i)).format(dateFmt));
        } catch (Exception ignored) {
        }
      }
    }
    return labels;
  }

  private String formatBucketLabel(String bucket, boolean hourly) {
    try {
      if (hourly) {
        LocalDateTime dt = LocalDateTime.parse(bucket, HOUR_KEY_FORMATTER);
        return dt.format(DateTimeFormatter.ofPattern("MMM d, HH:00"));
      } else {
        LocalDate date = LocalDate.parse(bucket);
        return date.format(DateTimeFormatter.ofPattern("EEE, MMM d"));
      }
    } catch (Exception e) {
      return bucket;
    }
  }

  private static Color withOpacity(Color base, double opacity) {
    int alpha = (int) Math.round(opacity * 255);
    alpha = Math.max(0, Math.min(255, alpha));
    return new Color(base.getRed(), base.getGreen(), base.getBlue(), alpha);
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

  private static String formatTime(long seconds) {
    if (seconds <= 0) return "0s";
    long h = seconds / 3600;
    long m = (seconds % 3600) / 60;
    long s = seconds % 60;
    if (h > 0) return h + " h " + m + " m " + s + " s";
    if (m > 0) return m + " m " + s + " s";
    return s + " s";
  }
}
