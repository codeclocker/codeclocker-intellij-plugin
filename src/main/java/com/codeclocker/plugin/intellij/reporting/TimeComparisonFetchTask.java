package com.codeclocker.plugin.intellij.reporting;

import static com.codeclocker.plugin.intellij.ScheduledExecutor.EXECUTOR;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.reporting.TimeComparisonHttpClient.TimeComparisonResponse;
import com.codeclocker.plugin.intellij.reporting.TimeComparisonHttpClient.TimePeriodComparisonDto;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicReference;

@Service
public final class TimeComparisonFetchTask implements Disposable {

  private static final Logger LOG = Logger.getInstance(TimeComparisonFetchTask.class);
  private static final int FETCH_INTERVAL_SECONDS = 60;

  private final TimeComparisonHttpClient httpClient;

  private final AtomicReference<TimePeriodComparisonDto> todayVsYesterday = new AtomicReference<>();
  private final AtomicReference<TimePeriodComparisonDto> thisWeekVsLastWeek =
      new AtomicReference<>();
  private ScheduledFuture<?> task;

  public TimeComparisonFetchTask() {
    this.httpClient = new TimeComparisonHttpClient();
  }

  public void schedule() {
    if (task != null && !task.isCancelled()) {
      return;
    }

    task = EXECUTOR.scheduleWithFixedDelay(this::fetchData, 0, FETCH_INTERVAL_SECONDS, SECONDS);
  }

  private void fetchData() {
    try {
      String apiKey = ApiKeyLifecycle.getActiveApiKey();
      if (!isNotBlank(apiKey)) {
        LOG.debug("No API key available, skipping time comparison fetch");
        return;
      }

      ZoneId timeZone = ZoneId.systemDefault();
      fetchTodayVsYesterday(apiKey, timeZone);
      fetchThisWeekVsLastWeek(apiKey, timeZone);
    } catch (Exception ex) {
      LOG.debug("Error fetching time comparison data: {}", ex.getMessage());
    }
  }

  /**
   * Triggers an immediate refetch of time comparison data. Called after local data is synced to the
   * server to update trends.
   */
  public void refetch() {
    LOG.debug("Triggering immediate refetch of time comparison data");
    fetchData();
  }

  private void fetchTodayVsYesterday(String apiKey, ZoneId timeZone) {
    LocalDate today = LocalDate.now(timeZone);
    LocalDate yesterday = today.minusDays(1);

    Instant todayStart = toStartOfDay(today, timeZone);
    Instant todayEnd = Instant.now();
    Instant yesterdayStart = toStartOfDay(yesterday, timeZone);
    Instant yesterdayEnd = toEndOfDay(yesterday, timeZone);

    TimeComparisonResponse response =
        httpClient.fetchTimeComparison(apiKey, todayStart, todayEnd, yesterdayStart, yesterdayEnd);

    if (response.isSuccess()) {
      todayVsYesterday.set(response.getComparison());
      LOG.debug("Updated today vs yesterday comparison: {}", todayVsYesterday.get());
    }
  }

  private void fetchThisWeekVsLastWeek(String apiKey, ZoneId timeZone) {
    LocalDate today = LocalDate.now(timeZone);
    LocalDate startOfThisWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    LocalDate startOfLastWeek = startOfThisWeek.minusWeeks(1);
    LocalDate endOfLastWeek = startOfThisWeek.minusDays(1);

    Instant thisWeekStart = toStartOfDay(startOfThisWeek, timeZone);
    Instant thisWeekEnd = Instant.now();
    Instant lastWeekStart = toStartOfDay(startOfLastWeek, timeZone);
    Instant lastWeekEnd = toEndOfDay(endOfLastWeek, timeZone);

    TimeComparisonResponse response =
        httpClient.fetchTimeComparison(
            apiKey, thisWeekStart, thisWeekEnd, lastWeekStart, lastWeekEnd);

    if (response.isSuccess()) {
      thisWeekVsLastWeek.set(response.getComparison());
      LOG.debug("Updated this week vs last week comparison: {}", thisWeekVsLastWeek.get());
    }
  }

  private static Instant toStartOfDay(LocalDate date, ZoneId timeZone) {
    return ZonedDateTime.of(date.atStartOfDay(), timeZone).toInstant();
  }

  private static Instant toEndOfDay(LocalDate date, ZoneId timeZone) {
    return ZonedDateTime.of(date.atTime(23, 59, 59), timeZone).toInstant();
  }

  public TimePeriodComparisonDto getTodayVsYesterday() {
    return todayVsYesterday.get();
  }

  public TimePeriodComparisonDto getThisWeekVsLastWeek() {
    return thisWeekVsLastWeek.get();
  }

  public static TimeComparisonFetchTask getInstance() {
    return ApplicationManager.getApplication().getService(TimeComparisonFetchTask.class);
  }

  @Override
  public void dispose() {
    if (task != null) {
      task.cancel(false);
    }
  }
}
