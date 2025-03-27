package com.codeclocker.plugin.intellij;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

public class ScheduledExecutor {

  public static final ScheduledExecutorService EXECUTOR =
      Executors.newSingleThreadScheduledExecutor();
}
