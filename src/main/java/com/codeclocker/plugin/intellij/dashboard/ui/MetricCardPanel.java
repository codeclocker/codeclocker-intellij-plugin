package com.codeclocker.plugin.intellij.dashboard.ui;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.AbstractBorder;
import org.jetbrains.annotations.Nullable;

/** A metric card panel mimicking the web UI's MetricCard widget. */
public class MetricCardPanel extends JPanel {

  private final JLabel valueLabel;
  private final JLabel subtitleLabel;
  private final Color iconColor;

  public MetricCardPanel(String title, Color iconColor) {
    this.iconColor = iconColor;
    setLayout(new BorderLayout(0, JBUI.scale(4)));
    setBorder(
        JBUI.Borders.compound(
            new RoundedBorder(JBColor.namedColor("Borders.color", new JBColor(0xD0D0D0, 0x505050))),
            JBUI.Borders.empty(12, 14)));
    setOpaque(false);

    // North: title + icon dot
    JPanel headerPanel = new JPanel(new BorderLayout());
    headerPanel.setOpaque(false);
    JLabel titleLabel = new JLabel(title);
    titleLabel.setFont(JBUI.Fonts.smallFont());
    titleLabel.setForeground(new JBColor(0x5E6687, 0xA9B1D6));
    headerPanel.add(titleLabel, BorderLayout.WEST);

    JPanel dotPanel =
        new JPanel() {
          @Override
          protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(iconColor);
            int size = JBUI.scale(10);
            g2.fillOval(0, (getHeight() - size) / 2, size, size);
            g2.dispose();
          }

          @Override
          public Dimension getPreferredSize() {
            int s = JBUI.scale(10);
            return new Dimension(s, s);
          }
        };
    dotPanel.setOpaque(false);
    headerPanel.add(dotPanel, BorderLayout.EAST);
    add(headerPanel, BorderLayout.NORTH);

    // Center: value
    valueLabel = new JLabel("\u2014", SwingConstants.CENTER);
    valueLabel.setFont(JBUI.Fonts.label().biggerOn(6).asBold());
    add(valueLabel, BorderLayout.CENTER);

    // South: trend / subtitle
    subtitleLabel = new JLabel("", SwingConstants.CENTER);
    subtitleLabel.setFont(JBUI.Fonts.smallFont());
    subtitleLabel.setForeground(new JBColor(0x5E6687, 0xA9B1D6));
    JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
    southPanel.setOpaque(false);
    southPanel.add(subtitleLabel);
    add(southPanel, BorderLayout.SOUTH);
  }

  public void update(String value, @Nullable Integer trendPercent, @Nullable String subtitle) {
    valueLabel.setText(value);

    if (trendPercent != null) {
      String arrow = trendPercent >= 0 ? "\u2197" : "\u2198";
      String text = String.format("%s %d%% vs prev", arrow, Math.abs(trendPercent));
      subtitleLabel.setText(text);
      subtitleLabel.setForeground(
          trendPercent >= 0 ? new JBColor(0x1B8A2D, 0x5EC46B) : new JBColor(0xC62828, 0xEF5350));
    } else if (subtitle != null) {
      subtitleLabel.setText(subtitle);
      subtitleLabel.setForeground(new JBColor(0x5E6687, 0xA9B1D6));
    } else {
      subtitleLabel.setText("");
    }
  }

  public void setLoading(boolean loading) {
    if (loading) {
      valueLabel.setText("\u2014");
      subtitleLabel.setText("");
    }
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    g2.setColor(getBackground());
    int arc = JBUI.scale(10);
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
    g2.dispose();
  }

  static class RoundedBorder extends AbstractBorder {
    private final Color color;

    RoundedBorder(Color color) {
      this.color = color;
    }

    @Override
    public void paintBorder(java.awt.Component c, Graphics g, int x, int y, int width, int height) {
      Graphics2D g2 = (Graphics2D) g.create();
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g2.setColor(color);
      int arc = JBUI.scale(10);
      g2.drawRoundRect(x, y, width - 1, height - 1, arc, arc);
      g2.dispose();
    }

    @Override
    public java.awt.Insets getBorderInsets(java.awt.Component c) {
      return JBUI.insets(1);
    }
  }
}
