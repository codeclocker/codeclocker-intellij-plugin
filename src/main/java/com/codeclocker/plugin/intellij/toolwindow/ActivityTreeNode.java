package com.codeclocker.plugin.intellij.toolwindow;

import com.codeclocker.plugin.intellij.local.CommitRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.tree.DefaultMutableTreeNode;

/** Tree node for activity data - can represent daily summary, hourly detail, or commit. */
public class ActivityTreeNode extends DefaultMutableTreeNode {

  public enum NodeType {
    DAILY,
    HOURLY,
    COMMIT
  }

  private final NodeType nodeType;
  private final String dateDisplay;
  private final String hourDisplay;
  private final String branchName;
  private final long seconds;
  private final String timeDisplay;
  private final List<CommitRecord> commits;
  private final String commitsDisplay;
  private final String commitMessage;

  /** Creates a daily summary node (parent). */
  public static ActivityTreeNode createDailyNode(
      String dateDisplay, long totalSeconds, List<String> branches, List<CommitRecord> allCommits) {
    String branchesDisplay = formatBranches(branches);
    String timeDisplay = formatTime(totalSeconds);
    String commitsDisplay = formatCommitsCount(allCommits);
    return new ActivityTreeNode(
        NodeType.DAILY,
        dateDisplay,
        null,
        branchesDisplay,
        totalSeconds,
        timeDisplay,
        allCommits,
        commitsDisplay,
        null);
  }

  /** Creates an hourly detail node (child of daily). */
  public static ActivityTreeNode createHourlyNode(BranchActivityRow row) {
    return new ActivityTreeNode(
        NodeType.HOURLY,
        null,
        row.hourDisplay(),
        row.branchName(),
        row.seconds(),
        row.timeDisplay(),
        row.commits(),
        row.commitsDisplay(),
        null);
  }

  /** Creates a commit node (child of hourly). */
  public static ActivityTreeNode createCommitNode(CommitRecord commit, String projectName) {
    String message;
    if (projectName != null && !projectName.isEmpty()) {
      message = "[" + projectName + "] " + commit.getHash() + ": " + commit.getMessage();
    } else {
      message = commit.getHash() + ": " + commit.getMessage();
    }
    return new ActivityTreeNode(NodeType.COMMIT, null, null, null, 0, null, null, null, message);
  }

  private ActivityTreeNode(
      NodeType nodeType,
      String dateDisplay,
      String hourDisplay,
      String branchName,
      long seconds,
      String timeDisplay,
      List<CommitRecord> commits,
      String commitsDisplay,
      String commitMessage) {
    this.nodeType = nodeType;
    this.dateDisplay = dateDisplay;
    this.hourDisplay = hourDisplay;
    this.branchName = branchName;
    this.seconds = seconds;
    this.timeDisplay = timeDisplay;
    this.commits = commits != null ? commits : new ArrayList<>();
    this.commitsDisplay = commitsDisplay;
    this.commitMessage = commitMessage;
  }

  public boolean isDailyNode() {
    return nodeType == NodeType.DAILY;
  }

  public boolean isCommitNode() {
    return nodeType == NodeType.COMMIT;
  }

  public NodeType getNodeType() {
    return nodeType;
  }

  public String getDateOrHourDisplay() {
    return switch (nodeType) {
      case DAILY -> dateDisplay;
      case HOURLY -> hourDisplay;
      case COMMIT -> commitMessage;
    };
  }

  public String getBranchName() {
    return branchName;
  }

  public long getSeconds() {
    return seconds;
  }

  public String getTimeDisplay() {
    return nodeType == NodeType.COMMIT ? "" : timeDisplay;
  }

  public List<CommitRecord> getCommits() {
    return commits;
  }

  public String getCommitsDisplay() {
    return nodeType == NodeType.COMMIT ? "" : commitsDisplay;
  }

  private static String formatBranches(List<String> branches) {
    if (branches == null || branches.isEmpty()) {
      return "-";
    }
    List<String> uniqueBranches =
        branches.stream().distinct().filter(b -> !"-".equals(b)).collect(Collectors.toList());
    if (uniqueBranches.isEmpty()) {
      return "-";
    }
    if (uniqueBranches.size() <= 3) {
      return String.join(", ", uniqueBranches);
    }
    return uniqueBranches.stream().limit(3).collect(Collectors.joining(", "))
        + " (+"
        + (uniqueBranches.size() - 3)
        + " more)";
  }

  private static String formatTime(long seconds) {
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

  private static String formatCommitsCount(List<CommitRecord> commits) {
    if (commits == null || commits.isEmpty()) {
      return "-";
    }
    int count = commits.size();
    return count == 1 ? "1 commit" : count + " commits";
  }

  @Override
  public String toString() {
    return getDateOrHourDisplay();
  }
}
