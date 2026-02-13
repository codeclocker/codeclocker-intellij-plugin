package com.codeclocker.plugin.intellij.dashboard.ui;

import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.TimePeriod;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.function.Consumer;
import javax.swing.JButton;
import javax.swing.JPanel;

/** Chip-style period selector with styled buttons. */
public class TimePeriodSelectorPanel extends JPanel {

  private static final Color SELECTED_BG = new JBColor(0x7C3AED, 0xA78BFA);
  private static final Color SELECTED_FG = new JBColor(0xFFFFFF, 0x1E1E2E);
  private static final Color UNSELECTED_BG =
      JBColor.namedColor("ActionButton.hoverBackground", new JBColor(0xF0F0F0, 0x3C3F41));
  private static final Color UNSELECTED_FG =
      JBColor.namedColor("Label.foreground", JBColor.foreground());

  private TimePeriod selected = TimePeriod.LAST_7_DAYS;
  private final JButton[] buttons;

  public TimePeriodSelectorPanel(Consumer<TimePeriod> onPeriodChanged) {
    setLayout(new FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(4)));
    setBorder(JBUI.Borders.empty(4, 8));

    TimePeriod[] periods = TimePeriod.values();
    buttons = new JButton[periods.length];

    for (int i = 0; i < periods.length; i++) {
      TimePeriod period = periods[i];
      JButton button = createChipButton(period.getLabel());
      buttons[i] = button;

      button.addActionListener(
          e -> {
            selected = period;
            updateButtonStyles();
            onPeriodChanged.accept(period);
          });

      add(button);
    }

    updateButtonStyles();
  }

  public TimePeriod getSelected() {
    return selected;
  }

  private JButton createChipButton(String text) {
    JButton button =
        new JButton(text) {
          @Override
          protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int arc = JBUI.scale(14);

            if (getModel().isPressed() || isCurrentlySelected()) {
              g2.setColor(SELECTED_BG);
              g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
              g2.dispose();
              setForeground(SELECTED_FG);
            } else {
              g2.setColor(UNSELECTED_BG);
              g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
              g2.dispose();
              setForeground(UNSELECTED_FG);
            }

            super.paintComponent(g);
          }

          private boolean isCurrentlySelected() {
            return getText().equals(selected.getLabel());
          }
        };

    button.setContentAreaFilled(false);
    button.setBorderPainted(false);
    button.setFocusPainted(false);
    button.setOpaque(false);
    button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    button.setFont(JBUI.Fonts.smallFont());
    button.setBorder(JBUI.Borders.empty(4, 12));
    return button;
  }

  private void updateButtonStyles() {
    for (JButton button : buttons) {
      button.repaint();
    }
  }
}
