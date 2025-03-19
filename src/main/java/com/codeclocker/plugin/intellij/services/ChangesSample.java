package com.codeclocker.plugin.intellij.services;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public record ChangesSample(
    long samplingStartedAt,
    AtomicLong additions,
    AtomicLong removals,
    Map<String, String> metadata) {

  public static ChangesSample create(String extension) {
    Map<String, String> metadata = extension == null ? Map.of() : Map.of("extension", extension);

    return new ChangesSample(
        System.currentTimeMillis(), new AtomicLong(0), new AtomicLong(0), metadata);
  }

  public void incrementAdditions(long additions) {
    this.additions.getAndUpdate(prev -> prev + additions);
  }

  public void incrementRemovals(long removals) {
    this.removals.getAndUpdate(prev -> prev + removals);
  }
}
