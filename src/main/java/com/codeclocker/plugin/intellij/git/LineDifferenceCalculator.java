package com.codeclocker.plugin.intellij.git;

import java.util.ArrayList;
import java.util.List;

public class LineDifferenceCalculator {

  public static LineDifferenceResult calculateLineDifferences(String oldString, String newString) {
    List<String> oldLines = splitLines(oldString);
    List<String> newLines = splitLines(newString);

    int oldSize = oldLines.size();
    int newSize = newLines.size();

    int[][] dp = new int[oldSize + 1][newSize + 1];

    for (int i = 1; i <= oldSize; i++) {
      for (int j = 1; j <= newSize; j++) {
        if (oldLines.get(i - 1).equals(newLines.get(j - 1))) {
          dp[i][j] = dp[i - 1][j - 1] + 1;
        } else {
          dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
        }
      }
    }

    int added = 0;
    int removed = 0;
    int i = oldSize;
    int j = newSize;

    while (i > 0 || j > 0) {
      if (i > 0 && j > 0 && oldLines.get(i - 1).equals(newLines.get(j - 1))) {
        i--;
        j--;
      } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
        added++;
        j--;
      } else {
        removed++;
        i--;
      }
    }

    return new LineDifferenceResult(added, removed);
  }

  private static List<String> splitLines(String text) {
    List<String> lines = new ArrayList<>();
    if (text == null || text.isEmpty()) {
      return lines;
    }
    int start = 0;
    int end;
    while ((end = text.indexOf('\n', start)) != -1) {
      lines.add(text.substring(start, end));
      start = end + 1;
    }
    if (start < text.length()) {
      lines.add(text.substring(start));
    }
    return lines;
  }

  public record LineDifferenceResult(int addedLines, int removedLines) {}
}
