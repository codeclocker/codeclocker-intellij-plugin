package com.codeclocker.plugin.intellij.analytics;

import java.util.List;
import java.util.Map;

/** DTO for sending analytics report to the backend. */
public record AnalyticsReportDto(
    String installationId,
    IdeContextDto ideContext,
    List<AnalyticsEventDto> events,
    long reportedAt) {

  public static AnalyticsReportDto from(
      String installationId, IdeContext context, List<AnalyticsEvent> events) {
    return new AnalyticsReportDto(
        installationId,
        IdeContextDto.from(context),
        events.stream().map(AnalyticsEventDto::from).toList(),
        System.currentTimeMillis());
  }

  public record IdeContextDto(
      String ideName,
      String ideVersion,
      String ideBuild,
      String ideProductCode,
      String pluginVersion,
      String osName,
      String osVersion,
      String osArch,
      String javaVersion,
      String locale) {
    public static IdeContextDto from(IdeContext context) {
      return new IdeContextDto(
          context.ideName(),
          context.ideVersion(),
          context.ideBuild(),
          context.ideProductCode(),
          context.pluginVersion(),
          context.osName(),
          context.osVersion(),
          context.osArch(),
          context.javaVersion(),
          context.locale());
    }
  }

  public record AnalyticsEventDto(
      String eventType, long timestamp, Map<String, Object> properties) {
    public static AnalyticsEventDto from(AnalyticsEvent event) {
      return new AnalyticsEventDto(event.eventType(), event.timestamp(), event.properties());
    }
  }
}
