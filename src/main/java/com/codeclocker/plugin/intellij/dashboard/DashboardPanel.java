package com.codeclocker.plugin.intellij.dashboard;

import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.DashboardData;
import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.ProjectBreakdownEntry;
import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.ProjectTimelineData;
import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.TimePeriod;
import com.codeclocker.plugin.intellij.dashboard.DashboardDataService.TimelineDataPoint;
import com.codeclocker.plugin.intellij.dashboard.ui.ActivityTimelinePanel;
import com.codeclocker.plugin.intellij.dashboard.ui.AllProjectsPanel;
import com.codeclocker.plugin.intellij.dashboard.ui.JourneyBarPanel;
import com.codeclocker.plugin.intellij.dashboard.ui.MetricCardPanel;
import com.codeclocker.plugin.intellij.dashboard.ui.ProjectTimelineGanttPanel;
import com.codeclocker.plugin.intellij.dashboard.ui.StreakCardPanel;
import com.codeclocker.plugin.intellij.dashboard.ui.TimePeriodSelectorPanel;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.List;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import org.jetbrains.annotations.NotNull;

/** Main dashboard panel displayed as a tab in the CodeClocker tool window. */
public class DashboardPanel extends JPanel implements Disposable {

  private static final Color PURPLE = new JBColor(0x7C3AED, 0xA78BFA);
  private static final Color CYAN = new JBColor(0x0891B2, 0x22D3EE);
  private static final Color GREEN = new JBColor(0x16A34A, 0x4ADE80);
  private static final Color RED = new JBColor(0xDC2626, 0xF87171);

  private final TimePeriodSelectorPanel periodSelector;
  private final MetricCardPanel totalTimeCard;
  private final MetricCardPanel dailyAvgCard;
  private final MetricCardPanel linesAddedCard;
  private final MetricCardPanel linesRemovedCard;
  private final StreakCardPanel streakCard;
  private final JourneyBarPanel journeyBar;
  private final ActivityTimelinePanel activityTimeline;
  private final ProjectTimelineGanttPanel projectTimelinePanel;
  private final AllProjectsPanel allProjectsPanel;

  public DashboardPanel() {
    setLayout(new BorderLayout());

    // Header: period selector + refresh button
    JPanel headerPanel = new JPanel(new BorderLayout());
    periodSelector = new TimePeriodSelectorPanel(this::onPeriodChanged);
    headerPanel.add(periodSelector, BorderLayout.CENTER);

    DefaultActionGroup actionGroup = new DefaultActionGroup();
    actionGroup.add(
        new AnAction("Refresh", "Refresh dashboard data", AllIcons.Actions.Refresh) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            refreshData();
          }
        });
    ActionToolbar toolbar =
        ActionManager.getInstance().createActionToolbar("DashboardToolbar", actionGroup, true);
    toolbar.setTargetComponent(this);
    headerPanel.add(toolbar.getComponent(), BorderLayout.EAST);

    add(headerPanel, BorderLayout.NORTH);

    // Scrollable content
    JPanel contentPanel = new JPanel();
    contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
    contentPanel.setBorder(JBUI.Borders.empty(8, 12));

    // Cards row
    JPanel cardsRow = new JPanel(new GridLayout(1, 5, JBUI.scale(8), 0));

    totalTimeCard = new MetricCardPanel("Total Time", PURPLE);
    dailyAvgCard = new MetricCardPanel("Daily Average", CYAN);
    linesAddedCard = new MetricCardPanel("Lines Added", GREEN);
    linesRemovedCard = new MetricCardPanel("Lines Removed", RED);
    streakCard = new StreakCardPanel();

    cardsRow.add(totalTimeCard);
    cardsRow.add(dailyAvgCard);
    cardsRow.add(linesAddedCard);
    cardsRow.add(linesRemovedCard);
    cardsRow.add(streakCard);

    cardsRow.setMaximumSize(
        new java.awt.Dimension(Integer.MAX_VALUE, cardsRow.getPreferredSize().height));
    contentPanel.add(cardsRow);
    contentPanel.add(javax.swing.Box.createVerticalStrut(JBUI.scale(8)));

    // Journey bar
    journeyBar = new JourneyBarPanel();
    journeyBar.setMaximumSize(
        new java.awt.Dimension(Integer.MAX_VALUE, journeyBar.getPreferredSize().height));
    contentPanel.add(journeyBar);
    contentPanel.add(javax.swing.Box.createVerticalStrut(JBUI.scale(8)));

    // Activity timeline chart
    activityTimeline = new ActivityTimelinePanel();
    activityTimeline.setMaximumSize(
        new java.awt.Dimension(Integer.MAX_VALUE, activityTimeline.getPreferredSize().height));
    contentPanel.add(activityTimeline);
    contentPanel.add(javax.swing.Box.createVerticalStrut(JBUI.scale(8)));

    // Project Timeline Gantt chart
    projectTimelinePanel = new ProjectTimelineGanttPanel();
    projectTimelinePanel.setMaximumSize(
        new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    contentPanel.add(projectTimelinePanel);
    contentPanel.add(javax.swing.Box.createVerticalStrut(JBUI.scale(8)));

    // All Projects breakdown table
    allProjectsPanel = new AllProjectsPanel();
    allProjectsPanel.setMaximumSize(new java.awt.Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
    contentPanel.add(allProjectsPanel);

    // Push everything to the top
    contentPanel.add(javax.swing.Box.createVerticalGlue());

    JBScrollPane scrollPane = new JBScrollPane(contentPanel);
    scrollPane.setBorder(JBUI.Borders.empty());
    add(scrollPane, BorderLayout.CENTER);

    // Refresh on visibility
    addAncestorListener(
        new AncestorListener() {
          @Override
          public void ancestorAdded(AncestorEvent event) {
            refreshData();
          }

          @Override
          public void ancestorRemoved(AncestorEvent event) {}

          @Override
          public void ancestorMoved(AncestorEvent event) {}
        });

    // Initial load
    refreshData();
  }

  private void onPeriodChanged(TimePeriod period) {
    refreshData();
  }

  private void refreshData() {
    totalTimeCard.setLoading(true);
    dailyAvgCard.setLoading(true);
    linesAddedCard.setLoading(true);
    linesRemovedCard.setLoading(true);

    ApplicationManager.getApplication()
        .executeOnPooledThread(
            () -> {
              DashboardDataService service =
                  ApplicationManager.getApplication().getService(DashboardDataService.class);
              if (service == null) {
                return;
              }

              TimePeriod period = periodSelector.getSelected();
              DashboardData data = service.computeForPeriod(period);
              List<TimelineDataPoint> timelineData = service.computeTimelineData(period);
              List<ProjectBreakdownEntry> breakdown = service.computeProjectBreakdown(period);
              ProjectTimelineData timelineGanttData = service.computeProjectTimeline(period);

              ApplicationManager.getApplication()
                  .invokeLater(
                      () -> {
                        totalTimeCard.update(
                            formatTimeWithSeconds(data.totalTimeSpent()),
                            data.trendPercentage(),
                            null);
                        dailyAvgCard.update(formatTime(data.dailyAverage()), null, "per day");
                        linesAddedCard.update("+" + formatNumber(data.additions()), null, null);
                        linesRemovedCard.update("-" + formatNumber(data.removals()), null, null);
                        streakCard.update(data.currentStreak(), data.longestStreak());
                        journeyBar.update(
                            data.lifetimeDays(),
                            data.lifetimeTimeSpent(),
                            data.lifetimeProjects(),
                            data.lifetimeLines(),
                            data.firstActivityDate());
                        activityTimeline.update(timelineData, period);
                        projectTimelinePanel.update(timelineGanttData);
                        allProjectsPanel.update(breakdown);
                      });
            });
  }

  private String formatTimeWithSeconds(long seconds) {
    if (seconds <= 0) {
      return "0m";
    }
    long hours = seconds / 3600;
    long minutes = (seconds % 3600) / 60;
    long secs = seconds % 60;
    if (hours > 0) {
      return hours + "h " + minutes + "m " + secs + "s";
    }
    if (minutes > 0) {
      return minutes + "m " + secs + "s";
    }
    return secs + "s";
  }

  private String formatTime(long seconds) {
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

  private String formatNumber(long value) {
    if (value >= 1_000_000) {
      return String.format("%.1fM", value / 1_000_000.0);
    }
    if (value >= 1_000) {
      return String.format("%.1fk", value / 1_000.0);
    }
    return String.valueOf(value);
  }

  @Override
  public void dispose() {}
}
