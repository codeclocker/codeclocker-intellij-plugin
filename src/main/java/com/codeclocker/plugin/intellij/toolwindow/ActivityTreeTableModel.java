package com.codeclocker.plugin.intellij.toolwindow;

import com.intellij.ui.treeStructure.treetable.TreeTableModel;
import javax.swing.JTree;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

/** Tree table model for displaying collapsible activity data. */
public class ActivityTreeTableModel implements TreeTableModel {

  private static final String[] COLUMNS = {"Date", "Time", "Commits"};
  private static final Class<?>[] COLUMN_CLASSES = {
    TreeTableModel.class, String.class, String.class
  };

  private DefaultMutableTreeNode root;

  public ActivityTreeTableModel() {
    this.root = new DefaultMutableTreeNode("Root");
  }

  public void setRoot(DefaultMutableTreeNode root) {
    this.root = root;
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
  public Class<?> getColumnClass(int column) {
    return COLUMN_CLASSES[column];
  }

  @Override
  public Object getValueAt(Object node, int column) {
    if (node instanceof ActivityTreeNode activityNode) {
      return switch (column) {
        case 0 -> activityNode.getDateOrHourDisplay();
        case 1 -> activityNode.getTimeDisplay();
        case 2 -> activityNode.getCommitsDisplay();
        default -> "";
      };
    }
    return "";
  }

  @Override
  public boolean isCellEditable(Object node, int column) {
    return false;
  }

  @Override
  public void setValueAt(Object value, Object node, int column) {
    // Not editable
  }

  @Override
  public void setTree(JTree tree) {
    // Not needed for our implementation
  }

  // TreeModel methods

  @Override
  public Object getRoot() {
    return root;
  }

  @Override
  public Object getChild(Object parent, int index) {
    if (parent instanceof DefaultMutableTreeNode node) {
      return node.getChildAt(index);
    }
    return null;
  }

  @Override
  public int getChildCount(Object parent) {
    if (parent instanceof DefaultMutableTreeNode node) {
      return node.getChildCount();
    }
    return 0;
  }

  @Override
  public boolean isLeaf(Object node) {
    if (node instanceof DefaultMutableTreeNode mutableNode) {
      return mutableNode.isLeaf();
    }
    return true;
  }

  @Override
  public void valueForPathChanged(TreePath path, Object newValue) {
    // Not editable
  }

  @Override
  public int getIndexOfChild(Object parent, Object child) {
    if (parent instanceof DefaultMutableTreeNode parentNode
        && child instanceof DefaultMutableTreeNode childNode) {
      return parentNode.getIndex(childNode);
    }
    return -1;
  }

  @Override
  public void addTreeModelListener(TreeModelListener l) {
    // Simple implementation - could add listener support if needed
  }

  @Override
  public void removeTreeModelListener(TreeModelListener l) {
    // Simple implementation
  }
}
