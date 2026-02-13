package com.codeclocker.plugin.intellij.dashboard.ui;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.jetbrains.annotations.Nullable;

/** Horizontal bar showing lifetime CodeClocker stats. */
public class JourneyBarPanel extends JPanel {

  private static final DateTimeFormatter DISPLAY_FORMAT =
      DateTimeFormatter.ofPattern("MMM d, yyyy");

  private final JLabel statsLabel;
  private final JLabel trackingLabel;

  public JourneyBarPanel() {
    setLayout(new BorderLayout(JBUI.scale(12), 0));
    setBorder(
        JBUI.Borders.compound(
            JBUI.Borders.customLine(
                JBColor.namedColor("Borders.color", new JBColor(0xD0D0D0, 0x505050)), 1),
            JBUI.Borders.empty(10, 14)));

    JLabel titleLabel = new JLabel("Your CodeClocker Journey");
    titleLabel.setFont(JBUI.Fonts.label().asBold());
    add(titleLabel, BorderLayout.WEST);

    statsLabel = new JLabel();
    statsLabel.setFont(JBUI.Fonts.smallFont());
    statsLabel.setForeground(new JBColor(0x5E6687, 0xA9B1D6));
    JPanel centerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    centerPanel.setOpaque(false);
    centerPanel.add(statsLabel);
    add(centerPanel, BorderLayout.CENTER);

    trackingLabel = new JLabel();
    trackingLabel.setFont(JBUI.Fonts.smallFont());
    trackingLabel.setForeground(new JBColor(0x5E6687, 0xA9B1D6));
    add(trackingLabel, BorderLayout.EAST);
  }

  public void update(
      int days, long totalSeconds, int projects, long totalLines, @Nullable LocalDate firstDate) {
    String timeStr = formatTime(totalSeconds);
    String linesStr = formatLargeNumber(totalLines);

    statsLabel.setText(
        days
            + " days  \u00B7  "
            + timeStr
            + "  \u00B7  "
            + projects
            + " projects  \u00B7  "
            + linesStr
            + " lines");

    if (firstDate != null) {
      long daysSinceFirst = ChronoUnit.DAYS.between(firstDate, LocalDate.now());
      trackingLabel.setText(
          "Tracking since " + firstDate.format(DISPLAY_FORMAT) + " (" + daysSinceFirst + " days)");
    } else {
      trackingLabel.setText("");
    }
  }

  private String formatTime(long seconds) {
    if (seconds <= 0) {
      return "0m";
    }
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    if (hours > 0) {
      return hours + "h " + minutes + "m";
    }
    return minutes + "m";
  }

  private String formatLargeNumber(long value) {
    if (value >= 1_000_000) {
      return String.format("%.1fM", value / 1_000_000.0);
    }
    if (value >= 1_000) {
      return String.format("%.1fk", value / 1_000.0);
    }
    return String.valueOf(value);
  }
}
