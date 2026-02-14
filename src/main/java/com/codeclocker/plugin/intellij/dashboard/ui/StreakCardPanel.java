package com.codeclocker.plugin.intellij.dashboard.ui;

import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;

/** Streak display card with fire icon and warm gradient background. */
public class StreakCardPanel extends JPanel {

  private static final String FIRE = "\uD83D\uDD25";
  private static final String TROPHY = "\uD83C\uDFC6";

  private final JLabel streakValueLabel;
  private final JLabel bestStreakLabel;
  private boolean hasStreak;

  public StreakCardPanel() {
    setLayout(new BorderLayout(0, JBUI.scale(4)));
    setBorder(
        JBUI.Borders.compound(
            new MetricCardPanel.RoundedBorder(
                JBColor.namedColor("Borders.color", new JBColor(0xD0D0D0, 0x505050))),
            JBUI.Borders.empty(12, 14)));
    setOpaque(false);

    // Main content
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setOpaque(false);

    // Top: fire icon + streak number + "days streak"
    JPanel streakRow = new JPanel();
    streakRow.setLayout(new BoxLayout(streakRow, BoxLayout.X_AXIS));
    streakRow.setOpaque(false);
    streakRow.setAlignmentX(CENTER_ALIGNMENT);

    JLabel fireLabel = new JLabel(FIRE);
    fireLabel.setFont(JBUI.Fonts.label().biggerOn(4));
    streakRow.add(fireLabel);
    streakRow.add(Box.createHorizontalStrut(JBUI.scale(4)));

    streakValueLabel = new JLabel("0");
    streakValueLabel.setFont(JBUI.Fonts.label().biggerOn(6).asBold());
    streakRow.add(streakValueLabel);
    streakRow.add(Box.createHorizontalStrut(JBUI.scale(4)));

    JLabel daysLabel = new JLabel("days");
    daysLabel.setFont(JBUI.Fonts.smallFont());
    daysLabel.setForeground(new JBColor(0x5E6687, 0xA9B1D6));
    streakRow.add(daysLabel);

    contentPanel.add(Box.createVerticalGlue());
    contentPanel.add(streakRow);
    contentPanel.add(Box.createVerticalStrut(JBUI.scale(2)));

    JLabel streakLabel = new JLabel("streak");
    streakLabel.setFont(JBUI.Fonts.smallFont());
    streakLabel.setForeground(new JBColor(0x5E6687, 0xA9B1D6));
    streakLabel.setAlignmentX(CENTER_ALIGNMENT);
    contentPanel.add(streakLabel);
    contentPanel.add(Box.createVerticalGlue());

    add(contentPanel, BorderLayout.CENTER);

    // South: separator + best streak
    JPanel southPanel = new JPanel(new BorderLayout(0, JBUI.scale(4)));
    southPanel.setOpaque(false);

    JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
    southPanel.add(separator, BorderLayout.NORTH);

    bestStreakLabel = new JLabel(TROPHY + " Best: 0 days");
    bestStreakLabel.setFont(JBUI.Fonts.smallFont());
    bestStreakLabel.setForeground(new JBColor(0x5E6687, 0xA9B1D6));
    bestStreakLabel.setHorizontalAlignment(SwingConstants.CENTER);
    southPanel.add(bestStreakLabel, BorderLayout.CENTER);

    add(southPanel, BorderLayout.SOUTH);
  }

  public void update(int currentStreak, int longestStreak) {
    this.hasStreak = currentStreak > 0;
    streakValueLabel.setText(String.valueOf(currentStreak));
    bestStreakLabel.setText(TROPHY + " Best: " + longestStreak + " days");
    repaint();
  }

  @Override
  protected void paintComponent(Graphics g) {
    Graphics2D g2 = (Graphics2D) g.create();
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    int arc = JBUI.scale(10);

    if (hasStreak) {
      Color startColor = JBColor.namedColor("FileColor.Yellow", new JBColor(0xFFF8E1, 0x4E3B00));
      Color endColor = JBColor.namedColor("FileColor.Orange", new JBColor(0xFFF3E0, 0x4A2800));
      g2.setPaint(new GradientPaint(0, 0, startColor, 0, getHeight(), endColor));
    } else {
      g2.setColor(getBackground());
    }
    g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
    g2.dispose();
  }
}
