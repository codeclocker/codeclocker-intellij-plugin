package com.codeclocker.plugin.intellij.toolwindow;

import java.util.ArrayList;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/** Table model for displaying branch activity data. */
public class BranchActivityTableModel extends AbstractTableModel {

  private static final String[] COLUMNS = {"Date + Hour", "Branch", "Time", "Commits"};

  private List<BranchActivityRow> rows = new ArrayList<>();

  @Override
  public int getRowCount() {
    return rows.size();
  }

  @Override
  public int getColumnCount() {
    return COLUMNS.length;
  }

  @Override
  public String getColumnName(int column) {
    return COLUMNS[column];
  }

  @Override
  public Object getValueAt(int row, int column) {
    if (row >= rows.size()) {
      return "";
    }
    BranchActivityRow r = rows.get(row);
    return switch (column) {
      case 0 -> r.hourDisplay();
      case 1 -> r.branchName();
      case 2 -> r.timeDisplay();
      case 3 -> r.commitsDisplay();
      default -> "";
    };
  }

  public void setData(List<BranchActivityRow> rows) {
    this.rows = rows != null ? rows : new ArrayList<>();
    fireTableDataChanged();
  }

  public BranchActivityRow getRowAt(int row) {
    if (row >= 0 && row < rows.size()) {
      return rows.get(row);
    }
    return null;
  }
}
