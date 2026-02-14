package com.codeclocker.plugin.intellij.dashboard.ui;

import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.ProjectBreakdownEntry;
import com.intellij.ui.JBColor;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/** Paginated table showing per-project breakdown of time, additions and removals. */
public class AllProjectsPanel extends JPanel {

  private static final Color GREEN = new JBColor(0x16A34A, 0x4ADE80);
  private static final Color RED = new JBColor(0xDC2626, 0xF87171);
  private static final Color MUTED_TEXT = new JBColor(0x5E6687, 0xA9B1D6);
  private static final Color ALT_ROW_BG =
      JBColor.namedColor("Table.alternativeRowBackground", new JBColor(0xF5F5F5, 0x2D2F31));

  private final JPanel tableContainer;
  private final JComboBox<Integer> rowsPerPageCombo;
  private final JLabel pageInfoLabel;
  private final JLabel prevButton;
  private final JLabel nextButton;

  private List<ProjectBreakdownEntry> allEntries = new ArrayList<>();
  private int currentPage = 0;
  private int rowsPerPage = 10;

  public AllProjectsPanel() {
    setLayout(new BorderLayout());
    setOpaque(false);
    setBorder(
        JBUI.Borders.compound(
            new MetricCardPanel.RoundedBorder(
                JBColor.namedColor("Borders.color", new JBColor(0xD0D0D0, 0x505050))),
            JBUI.Borders.empty(14, 14)));

    // NORTH: section header
    JPanel headerPanel = new JPanel();
    headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
    headerPanel.setOpaque(false);
    headerPanel.setBorder(JBUI.Borders.emptyBottom(10));

    JLabel titleLabel = new JLabel("All Projects");
    titleLabel.setFont(JBUI.Fonts.label().asBold().deriveFont((float) JBUI.scale(13)));
    titleLabel.setAlignmentX(LEFT_ALIGNMENT);
    headerPanel.add(titleLabel);

    headerPanel.add(Box.createVerticalStrut(JBUI.scale(2)));

    JLabel subtitleLabel = new JLabel("Complete breakdown by project");
    subtitleLabel.setFont(JBUI.Fonts.smallFont());
    subtitleLabel.setForeground(MUTED_TEXT);
    subtitleLabel.setAlignmentX(LEFT_ALIGNMENT);
    headerPanel.add(subtitleLabel);

    add(headerPanel, BorderLayout.NORTH);

    // CENTER: table container
    tableContainer = new JPanel();
    tableContainer.setLayout(new BoxLayout(tableContainer, BoxLayout.Y_AXIS));
    tableContainer.setOpaque(false);
    add(tableContainer, BorderLayout.CENTER);

    // SOUTH: pagination bar
    JPanel paginationBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, JBUI.scale(8), 0));
    paginationBar.setOpaque(false);
    paginationBar.setBorder(JBUI.Borders.emptyTop(8));

    JLabel rowsLabel = new JLabel("Rows per page");
    rowsLabel.setFont(JBUI.Fonts.smallFont());
    rowsLabel.setForeground(MUTED_TEXT);
    paginationBar.add(rowsLabel);

    rowsPerPageCombo = new JComboBox<>(new Integer[] {5, 10, 25});
    rowsPerPageCombo.setSelectedItem(10);
    rowsPerPageCombo.addActionListener(
        e -> {
          Integer selected = (Integer) rowsPerPageCombo.getSelectedItem();
          if (selected != null) {
            rowsPerPage = selected;
            currentPage = 0;
            rebuildRows();
          }
        });
    paginationBar.add(rowsPerPageCombo);

    paginationBar.add(Box.createHorizontalStrut(JBUI.scale(12)));

    pageInfoLabel = new JLabel("");
    pageInfoLabel.setFont(JBUI.Fonts.smallFont());
    pageInfoLabel.setForeground(MUTED_TEXT);
    paginationBar.add(pageInfoLabel);

    paginationBar.add(Box.createHorizontalStrut(JBUI.scale(4)));

    prevButton = createNavButton("<");
    prevButton.addMouseListener(
        new java.awt.event.MouseAdapter() {
          @Override
          public void mouseClicked(java.awt.event.MouseEvent e) {
            if (currentPage > 0) {
              currentPage--;
              rebuildRows();
            }
          }
        });
    paginationBar.add(prevButton);

    nextButton = createNavButton(">");
    nextButton.addMouseListener(
        new java.awt.event.MouseAdapter() {
          @Override
          public void mouseClicked(java.awt.event.MouseEvent e) {
            int totalPages = Math.max(1, (int) Math.ceil((double) allEntries.size() / rowsPerPage));
            if (currentPage < totalPages - 1) {
              currentPage++;
              rebuildRows();
            }
          }
        });
    paginationBar.add(nextButton);

    add(paginationBar, BorderLayout.SOUTH);
  }

  public void update(List<ProjectBreakdownEntry> entries) {
    this.allEntries = entries != null ? entries : new ArrayList<>();
    this.currentPage = 0;
    rebuildRows();
  }

  private void rebuildRows() {
    tableContainer.removeAll();

    int total = allEntries.size();

    if (total == 0) {
      JLabel emptyLabel = new JLabel("No project data for the selected period");
      emptyLabel.setFont(JBUI.Fonts.label());
      emptyLabel.setForeground(MUTED_TEXT);
      emptyLabel.setHorizontalAlignment(SwingConstants.CENTER);
      emptyLabel.setAlignmentX(CENTER_ALIGNMENT);
      emptyLabel.setBorder(JBUI.Borders.empty(20, 0));
      tableContainer.add(emptyLabel);
      pageInfoLabel.setText("0 of 0");
      updateNavButtons();
      tableContainer.revalidate();
      tableContainer.repaint();
      return;
    }

    // Column header row
    tableContainer.add(createHeaderRow());

    // Data rows for current page
    int start = currentPage * rowsPerPage;
    int end = Math.min(start + rowsPerPage, total);
    for (int i = start; i < end; i++) {
      tableContainer.add(createDataRow(allEntries.get(i), (i - start) % 2 == 1));
    }

    // Update pagination info
    pageInfoLabel.setText((start + 1) + "–" + end + " of " + total);
    updateNavButtons();

    tableContainer.revalidate();
    tableContainer.repaint();
  }

  private JPanel createHeaderRow() {
    JPanel row = new JPanel(new GridBagLayout());
    row.setOpaque(false);
    row.setBorder(JBUI.Borders.emptyBottom(4));
    row.setMaximumSize(
        new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + JBUI.scale(24)));
    row.setAlignmentX(LEFT_ALIGNMENT);

    Font headerFont = JBUI.Fonts.smallFont().asBold();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, JBUI.scale(4), 0, JBUI.scale(4));
    gbc.gridy = 0;

    // Project Name column - stretches
    gbc.gridx = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    JLabel projectHeader = new JLabel("Project Name");
    projectHeader.setFont(headerFont);
    projectHeader.setForeground(MUTED_TEXT);
    row.add(projectHeader, gbc);

    // Time Spent column
    gbc.gridx = 1;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.EAST;
    JLabel timeHeader = new JLabel("Time Spent");
    timeHeader.setFont(headerFont);
    timeHeader.setForeground(MUTED_TEXT);
    timeHeader.setPreferredSize(
        new Dimension(JBUI.scale(110), timeHeader.getPreferredSize().height));
    timeHeader.setHorizontalAlignment(SwingConstants.RIGHT);
    row.add(timeHeader, gbc);

    // Lines Added column
    gbc.gridx = 2;
    JLabel addedHeader = new JLabel("Lines Added");
    addedHeader.setFont(headerFont);
    addedHeader.setForeground(MUTED_TEXT);
    addedHeader.setPreferredSize(
        new Dimension(JBUI.scale(90), addedHeader.getPreferredSize().height));
    addedHeader.setHorizontalAlignment(SwingConstants.RIGHT);
    row.add(addedHeader, gbc);

    // Lines Removed column
    gbc.gridx = 3;
    JLabel removedHeader = new JLabel("Lines Removed");
    removedHeader.setFont(headerFont);
    removedHeader.setForeground(MUTED_TEXT);
    removedHeader.setPreferredSize(
        new Dimension(JBUI.scale(100), removedHeader.getPreferredSize().height));
    removedHeader.setHorizontalAlignment(SwingConstants.RIGHT);
    row.add(removedHeader, gbc);

    return row;
  }

  private JPanel createDataRow(ProjectBreakdownEntry entry, boolean alternate) {
    JPanel row = new JPanel(new GridBagLayout());
    row.setAlignmentX(LEFT_ALIGNMENT);
    if (alternate) {
      row.setBackground(ALT_ROW_BG);
      row.setOpaque(true);
    } else {
      row.setOpaque(false);
    }
    row.setBorder(JBUI.Borders.empty(4, 4));
    row.setMaximumSize(
        new Dimension(Integer.MAX_VALUE, row.getPreferredSize().height + JBUI.scale(28)));

    Font dataFont = JBUI.Fonts.label();
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(0, JBUI.scale(4), 0, JBUI.scale(4));
    gbc.gridy = 0;

    // Project Name
    gbc.gridx = 0;
    gbc.weightx = 1.0;
    gbc.fill = GridBagConstraints.HORIZONTAL;
    gbc.anchor = GridBagConstraints.WEST;
    JLabel nameLabel = new JLabel(entry.projectName());
    nameLabel.setFont(dataFont);
    row.add(nameLabel, gbc);

    // Time Spent
    gbc.gridx = 1;
    gbc.weightx = 0;
    gbc.fill = GridBagConstraints.NONE;
    gbc.anchor = GridBagConstraints.EAST;
    JLabel timeLabel = new JLabel(formatTime(entry.timeSpentSeconds()));
    timeLabel.setFont(dataFont);
    timeLabel.setPreferredSize(new Dimension(JBUI.scale(110), timeLabel.getPreferredSize().height));
    timeLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    row.add(timeLabel, gbc);

    // Lines Added
    gbc.gridx = 2;
    JLabel addedLabel = new JLabel("+" + entry.additions());
    addedLabel.setFont(dataFont);
    addedLabel.setForeground(GREEN);
    addedLabel.setPreferredSize(
        new Dimension(JBUI.scale(90), addedLabel.getPreferredSize().height));
    addedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    row.add(addedLabel, gbc);

    // Lines Removed
    gbc.gridx = 3;
    JLabel removedLabel = new JLabel("-" + entry.removals());
    removedLabel.setFont(dataFont);
    removedLabel.setForeground(RED);
    removedLabel.setPreferredSize(
        new Dimension(JBUI.scale(100), removedLabel.getPreferredSize().height));
    removedLabel.setHorizontalAlignment(SwingConstants.RIGHT);
    row.add(removedLabel, gbc);

    return row;
  }

  private void updateNavButtons() {
    int totalPages = Math.max(1, (int) Math.ceil((double) allEntries.size() / rowsPerPage));
    prevButton.setEnabled(currentPage > 0);
    prevButton.setForeground(currentPage > 0 ? getForeground() : MUTED_TEXT);
    nextButton.setEnabled(currentPage < totalPages - 1);
    nextButton.setForeground(currentPage < totalPages - 1 ? getForeground() : MUTED_TEXT);
  }

  private JLabel createNavButton(String text) {
    JLabel label = new JLabel(text);
    label.setFont(JBUI.Fonts.label().asBold());
    label.setForeground(MUTED_TEXT);
    label.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
    label.setBorder(JBUI.Borders.empty(2, 6));
    return label;
  }

  private static String formatTime(long seconds) {
    if (seconds <= 0) {
      return "0 s";
    }
    long h = seconds / 3600;
    long m = (seconds % 3600) / 60;
    long s = seconds % 60;
    if (h > 0) {
      return h + " h " + m + " m " + s + " s";
    }
    if (m > 0) {
      return m + " m " + s + " s";
    }
    return s + " s";
  }
}
