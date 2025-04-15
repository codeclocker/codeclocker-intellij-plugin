package com.codeclocker.plugin.intellij;

import com.intellij.util.concurrency.AppExecutorUtil;
import java.util.concurrent.ScheduledExecutorService;

public class ScheduledExecutor {

  public static final ScheduledExecutorService EXECUTOR =
      AppExecutorUtil.getAppScheduledExecutorService();
}
