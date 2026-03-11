package com.codeclocker.plugin.intellij.standup;

public enum StandupPeriod {
  TODAY_AND_YESTERDAY("Today + Yesterday", 2),
  YESTERDAY("Yesterday", 1),
  LAST_2_DAYS("Last 2 Days", 2),
  LAST_3_DAYS("Last 3 Days", 3),
  LAST_4_DAYS("Last 4 Days", 4),
  LAST_5_DAYS("Last 5 Days", 5),
  LAST_6_DAYS("Last 6 Days", 6),
  LAST_7_DAYS("Last 7 Days", 7);

  private final String label;
  private final int days;

  StandupPeriod(String label, int days) {
    this.label = label;
    this.days = days;
  }

  public String getLabel() {
    return label;
  }

  public int getDays() {
    return days;
  }
}
