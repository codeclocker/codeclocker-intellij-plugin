package com.codeclocker.plugin.intellij.goal;

/** Immutable record representing current progress towards a coding time goal. */
public record GoalProgress(
    long currentSeconds,
    long goalSeconds,
    int percentage,
    long remainingSeconds,
    boolean isComplete) {

  /** Create a GoalProgress from current and goal seconds. */
  public static GoalProgress of(long currentSeconds, long goalSeconds) {
    int percentage = goalSeconds > 0 ? (int) ((currentSeconds * 100) / goalSeconds) : 0;
    long remaining = Math.max(0, goalSeconds - currentSeconds);
    boolean complete = currentSeconds >= goalSeconds;
    return new GoalProgress(currentSeconds, goalSeconds, percentage, remaining, complete);
  }

  /** Format progress as "1h 30m / 2h 00m" */
  public String formatProgress() {
    return formatTime(currentSeconds) + " / " + formatTime(goalSeconds);
  }

  /** Format remaining time as "30m remaining" or "Goal reached!" */
  public String formatRemaining() {
    if (isComplete) {
      return "Goal reached!";
    }
    return formatTime(remainingSeconds) + " remaining";
  }

  /** Format percentage as "75%" */
  public String formatPercentage() {
    return percentage + "%";
  }

  /**
   * Render an ASCII progress bar.
   *
   * @param width number of characters for the bar
   * @return progress bar string like "██████████───────"
   */
  public String renderProgressBar(int width) {
    int filled = (int) ((percentage / 100.0) * width);
    filled = Math.min(filled, width);

    StringBuilder bar = new StringBuilder();
    for (int i = 0; i < width; i++) {
      bar.append(i < filled ? "█" : "─");
    }
    return bar.toString();
  }

  private static String formatTime(long seconds) {
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;

    if (hours > 0) {
      return String.format("%dh %02dm", hours, minutes);
    } else {
      return String.format("%dm", minutes);
    }
  }
}
