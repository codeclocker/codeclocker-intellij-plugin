package com.codeclocker.plugin.intellij.toolwindow;

import static com.codeclocker.plugin.intellij.HubHost.HUB_UI_HOST;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyLifecycle;
import com.codeclocker.plugin.intellij.apikey.ApiKeyPersistence;
import com.codeclocker.plugin.intellij.apikey.EnterApiKeyAction;
import com.codeclocker.plugin.intellij.local.CommitRecord;
import com.codeclocker.plugin.intellij.local.LocalActivityDataProvider;
import com.codeclocker.plugin.intellij.local.LocalTrackerState;
import com.codeclocker.plugin.intellij.local.ProjectActivitySnapshot;
import com.codeclocker.plugin.intellij.services.TimeSpentPerProjectLogger;
import com.codeclocker.plugin.intellij.toolwindow.export.ActivityCsvExporter;
import com.codeclocker.plugin.intellij.toolwindow.export.ExportDialog;
import com.intellij.icons.AllIcons;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileChooser.FileChooserFactory;
import com.intellij.openapi.fileChooser.FileSaverDescriptor;
import com.intellij.openapi.fileChooser.FileSaverDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFileWrapper;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.treeStructure.treetable.TreeTable;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.JPanel;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.tree.DefaultMutableTreeNode;
import org.jetbrains.annotations.NotNull;

/** Panel displaying branch activity and commits in a collapsible tree table. */
public class BranchActivityPanel extends JPanel implements Disposable {

  private static final Logger LOG = Logger.getInstance(BranchActivityPanel.class);

  private static final DateTimeFormatter DATE_DISPLAY_FORMATTER =
      DateTimeFormatter.ofPattern("yyyy-MM-dd (EEE)");

  private static final String ALL_PROJECTS = "All Projects";

  private static final int AUTO_REFRESH_INTERVAL_MS = 10_000; // 10 seconds

  private final Project project;
  private final TreeTable treeTable;
  private final ActivityTreeTableModel treeTableModel;
  private final ComboBox<String> projectComboBox;
  private final javax.swing.Timer autoRefreshTimer;
  private final JPanel infoBanner;
  private final javax.swing.JLabel bannerMessageLabel;
  private final HyperlinkLabel bannerLink;

  private String selectedProject;

  public BranchActivityPanel(Project project) {
    this.project = project;
    this.selectedProject = ALL_PROJECTS;
    this.treeTableModel = new ActivityTreeTableModel();
    this.treeTable = new TreeTable(treeTableModel);
    this.projectComboBox = new ComboBox<>();
    this.bannerMessageLabel = new javax.swing.JLabel();
    this.bannerLink = new HyperlinkLabel();
    this.infoBanner = createInfoBanner();

    setLayout(new BorderLayout());

    // Create header with toolbar and info banner
    JPanel headerPanel = new JPanel(new BorderLayout());
    JPanel toolbarPanel = createToolbarPanel();
    headerPanel.add(toolbarPanel, BorderLayout.NORTH);
    headerPanel.add(infoBanner, BorderLayout.SOUTH);
    add(headerPanel, BorderLayout.NORTH);

    // Configure tree table
    configureTreeTable();

    // Add tree table in scroll pane
    JBScrollPane scrollPane = new JBScrollPane(treeTable);
    add(scrollPane, BorderLayout.CENTER);

    // Load initial data
    refreshData();

    // Refresh data when panel becomes visible (tool window opened)
    addAncestorListener(
        new AncestorListener() {
          @Override
          public void ancestorAdded(AncestorEvent event) {
            refreshData();
          }

          @Override
          public void ancestorRemoved(AncestorEvent event) {
            // Not needed
          }

          @Override
          public void ancestorMoved(AncestorEvent event) {
            // Not needed
          }
        });

    // Setup auto-refresh timer
    autoRefreshTimer = new javax.swing.Timer(AUTO_REFRESH_INTERVAL_MS, e -> refreshData());
    autoRefreshTimer.start();
  }

  private JPanel createToolbarPanel() {
    DefaultActionGroup actionGroup = new DefaultActionGroup();

    // Refresh action
    AnAction refreshAction =
        new AnAction("Refresh", "Refresh branch activity data", AllIcons.Actions.Refresh) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            refreshData();
          }
        };
    actionGroup.add(refreshAction);

    // Expand all action
    AnAction expandAllAction =
        new AnAction("Expand All", "Expand all daily nodes", AllIcons.Actions.Expandall) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            expandAll();
          }
        };
    actionGroup.add(expandAllAction);

    // Collapse all action
    AnAction collapseAllAction =
        new AnAction("Collapse All", "Collapse all daily nodes", AllIcons.Actions.Collapseall) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            collapseAll();
          }
        };
    actionGroup.add(collapseAllAction);

    // Export action
    AnAction exportAction =
        new AnAction("Export", "Export activity data to CSV", AllIcons.ToolbarDecorator.Export) {
          @Override
          public void actionPerformed(@NotNull AnActionEvent e) {
            exportActivityData();
          }
        };
    actionGroup.add(exportAction);

    ActionToolbar toolbar =
        ActionManager.getInstance().createActionToolbar("BranchActivityToolbar", actionGroup, true);
    toolbar.setTargetComponent(this);

    JPanel toolbarPanel = new JPanel(new BorderLayout());
    toolbarPanel.add(toolbar.getComponent(), BorderLayout.WEST);

    // Add project selector combo box
    projectComboBox.addActionListener(
        e -> {
          String selected = (String) projectComboBox.getSelectedItem();
          if (selected != null && !selected.equals(selectedProject)) {
            selectedProject = selected;
            refreshData();
          }
        });

    JPanel filterPanel = new JPanel();
    filterPanel.add(projectComboBox);
    toolbarPanel.add(filterPanel, BorderLayout.EAST);

    return toolbarPanel;
  }

  private JPanel createInfoBanner() {
    JPanel banner = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
    banner.setBackground(new Color(230, 243, 255)); // Light blue
    banner.setBorder(JBUI.Borders.customLine(new Color(100, 149, 237), 0, 0, 1, 0));

    javax.swing.JLabel infoIcon = new javax.swing.JLabel(AllIcons.General.Information);
    banner.add(infoIcon);
    banner.add(bannerMessageLabel);
    banner.add(bannerLink);

    // Set up click handler that checks current state
    bannerLink.addHyperlinkListener(
        e -> {
          if (javax.swing.event.HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
            try {
              boolean hasApiKey = isNotBlank(ApiKeyPersistence.getApiKey());
              boolean subscriptionExpired = ApiKeyLifecycle.isActivityDataStoppedBeingCollected();

              if (hasApiKey && subscriptionExpired) {
                BrowserUtil.browse(HUB_UI_HOST + "/payment");
              } else {
                EnterApiKeyAction.showAction();
              }
            } catch (Exception ex) {
              LOG.debug("Failed to handle banner link click", ex);
              EnterApiKeyAction.showAction();
            }
          }
        });

    banner.setVisible(false); // Initially hidden
    return banner;
  }

  private void updateInfoBanner() {
    // Run on EDT to avoid threading issues, and use invokeLater to avoid blocking
    ApplicationManager.getApplication()
        .invokeLater(
            () -> {
              try {
                boolean hasApiKey = isNotBlank(ApiKeyPersistence.getApiKey());
                boolean subscriptionExpired = ApiKeyLifecycle.isActivityDataStoppedBeingCollected();
                boolean hasActiveSubscription = hasApiKey && !subscriptionExpired;

                if (hasActiveSubscription) {
                  // Connected with active subscription - hide banner
                  infoBanner.setVisible(false);
                  return;
                }

                // Calculate days of history
                LocalActivityDataProvider dataProvider =
                    ApplicationManager.getApplication().getService(LocalActivityDataProvider.class);
                int daysOfHistory = calculateDaysOfHistory(dataProvider);

                int maxDays = LocalTrackerState.MAX_SESSIONS;
                String message;
                if (daysOfHistory >= maxDays) {
                  message =
                      String.format(
                          "You have %d days of history. Older data is being rotated.", maxDays);
                } else if (daysOfHistory > 0) {
                  message =
                      String.format(
                          "You have %d day%s of history. Local storage keeps %d days.",
                          daysOfHistory, daysOfHistory == 1 ? "" : "s", maxDays);
                } else {
                  message = "Start coding to build your activity history.";
                }

                bannerMessageLabel.setText(message);
                bannerLink.setHyperlinkText(
                    hasApiKey && subscriptionExpired
                        ? "Renew subscription to keep data forever"
                        : "Connect to Hub to keep data forever");
                infoBanner.setVisible(true);
              } catch (Exception e) {
                // Services may not be ready during early initialization
                LOG.debug("Failed to update info banner, services may not be ready", e);
                infoBanner.setVisible(false);
              }
            });
  }

  /**
   * Count unique days with coding activity (sessions). A session is a day where the user coded.
   */
  private int calculateDaysOfHistory(LocalActivityDataProvider dataProvider) {
    if (dataProvider == null) {
      return 0;
    }

    Map<String, Map<String, ProjectActivitySnapshot>> data =
        dataProvider.getAllDataInLocalTimezone();
    if (data == null || data.isEmpty()) {
      return 0;
    }

    // Count unique dates (hourKey format: yyyy-MM-dd-HH, extract first 10 chars for date)
    Set<String> uniqueDates = new HashSet<>();
    for (String hourKey : data.keySet()) {
      if (hourKey != null && hourKey.length() >= 10) {
        uniqueDates.add(hourKey.substring(0, 10));
      }
    }

    return uniqueDates.size();
  }

  private void configureTreeTable() {
    treeTable.setRootVisible(false);
    treeTable.setRowHeight(25);
    treeTable.getColumnModel().getColumn(0).setPreferredWidth(200); // Date/Hour
    treeTable.getColumnModel().getColumn(1).setPreferredWidth(100); // Time
    treeTable.getColumnModel().getColumn(2).setPreferredWidth(400); // Commits
  }

  private void expandAll() {
    for (int i = 0; i < treeTable.getTree().getRowCount(); i++) {
      treeTable.getTree().expandRow(i);
    }
  }

  private void collapseAll() {
    for (int i = treeTable.getTree().getRowCount() - 1; i >= 0; i--) {
      treeTable.getTree().collapseRow(i);
    }
  }

  public void refreshData() {
    LocalActivityDataProvider dataProvider =
        ApplicationManager.getApplication().getService(LocalActivityDataProvider.class);
    if (dataProvider == null) {
      return;
    }

    // Update info banner based on Hub connection status
    updateInfoBanner();

    // Save expanded state before refresh
    Set<String> expandedNodes = saveExpandedState();

    // Data is returned with hourKeys in local timezone
    Map<String, Map<String, ProjectActivitySnapshot>> allData =
        dataProvider.getAllDataInLocalTimezone();

    // Add unsaved deltas from accumulators to match status bar widget totals
    mergeUnsavedDeltas(allData);

    // Update project dropdown with available projects
    updateProjectDropdown(allData);

    DefaultMutableTreeNode root = buildTreeStructure(allData);
    treeTableModel.setRoot(root);

    // Refresh the tree table
    treeTable.setModel(treeTableModel);
    configureTreeTable();

    // Restore expanded state after refresh
    restoreExpandedState(expandedNodes);
  }

  private Set<String> saveExpandedState() {
    Set<String> expandedNodes = new HashSet<>();
    javax.swing.JTree tree = treeTable.getTree();

    for (int i = 0; i < tree.getRowCount(); i++) {
      if (tree.isExpanded(i)) {
        javax.swing.tree.TreePath path = tree.getPathForRow(i);
        Object node = path.getLastPathComponent();
        if (node instanceof ActivityTreeNode activityNode) {
          expandedNodes.add(activityNode.getDateOrHourDisplay());
        }
      }
    }
    return expandedNodes;
  }

  private void restoreExpandedState(Set<String> expandedNodes) {
    if (expandedNodes.isEmpty()) {
      return;
    }

    javax.swing.JTree tree = treeTable.getTree();

    // Iterate through rows and expand matching nodes
    for (int i = 0; i < tree.getRowCount(); i++) {
      javax.swing.tree.TreePath path = tree.getPathForRow(i);
      Object node = path.getLastPathComponent();
      if (node instanceof ActivityTreeNode activityNode) {
        if (expandedNodes.contains(activityNode.getDateOrHourDisplay())) {
          tree.expandPath(path);
        }
      }
    }
  }

  /**
   * Merge unsaved deltas from accumulators into the data map. This ensures the Activity Report
   * shows the same totals as the status bar widget.
   */
  private void mergeUnsavedDeltas(Map<String, Map<String, ProjectActivitySnapshot>> allData) {
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    if (logger == null) {
      return;
    }

    String currentHourKey =
        java.time.LocalDateTime.now()
            .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd-HH"));

    // Get unsaved deltas for all projects and merge into current hour
    Map<String, ProjectActivitySnapshot> hourData =
        allData.computeIfAbsent(currentHourKey, k -> new LinkedHashMap<>());

    // Check each project that might have unsaved data
    for (String projectName : getAllProjectNames(allData)) {
      long unsavedDelta = logger.getProjectUnsavedDelta(projectName);
      if (unsavedDelta > 0) {
        ProjectActivitySnapshot existing = hourData.get(projectName);
        if (existing != null) {
          // Add unsaved delta to existing snapshot
          ProjectActivitySnapshot updated =
              new ProjectActivitySnapshot(
                  existing.getCodedTimeSeconds() + unsavedDelta,
                  existing.getAdditions(),
                  existing.getRemovals(),
                  existing.isReported());
          updated.setBranchActivity(existing.getBranchActivity());
          updated.setCommits(existing.getCommits());
          hourData.put(projectName, updated);
        } else {
          // Create new snapshot with just the unsaved delta
          hourData.put(projectName, new ProjectActivitySnapshot(unsavedDelta, 0, 0, false));
        }
      }
    }
  }

  private Set<String> getAllProjectNames(
      Map<String, Map<String, ProjectActivitySnapshot>> allData) {
    Set<String> projectNames = new HashSet<>();
    for (Map<String, ProjectActivitySnapshot> hourData : allData.values()) {
      projectNames.addAll(hourData.keySet());
    }
    // Also check accumulator for projects that might only have unsaved data
    TimeSpentPerProjectLogger logger =
        ApplicationManager.getApplication().getService(TimeSpentPerProjectLogger.class);
    if (logger != null) {
      // The logger doesn't expose project names directly, so we rely on local state
      // Projects with only unsaved data will be picked up on next flush
    }
    return projectNames;
  }

  private void updateProjectDropdown(Map<String, Map<String, ProjectActivitySnapshot>> data) {
    // Collect all unique project names from data
    List<String> projects =
        data.values().stream()
            .flatMap(hourData -> hourData.keySet().stream())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

    // Remember current selection
    String currentSelection = selectedProject;

    // Update dropdown items
    projectComboBox.removeAllItems();
    projectComboBox.addItem(ALL_PROJECTS);
    for (String proj : projects) {
      projectComboBox.addItem(proj);
    }

    // Restore selection if it still exists, otherwise default to All Projects
    if (currentSelection != null
        && (ALL_PROJECTS.equals(currentSelection) || projects.contains(currentSelection))) {
      projectComboBox.setSelectedItem(currentSelection);
    } else {
      projectComboBox.setSelectedItem(ALL_PROJECTS);
      selectedProject = ALL_PROJECTS;
    }
  }

  private DefaultMutableTreeNode buildTreeStructure(
      Map<String, Map<String, ProjectActivitySnapshot>> data) {
    DefaultMutableTreeNode root = new DefaultMutableTreeNode("Root");

    if (ALL_PROJECTS.equals(selectedProject)) {
      // All Projects mode: Day → Project → Commits
      buildTreeForAllProjects(root, data);
    } else {
      // Single project mode: Day → Hour → Commits
      buildTreeForSingleProject(root, data);
    }

    return root;
  }

  private void buildTreeForAllProjects(
      DefaultMutableTreeNode root, Map<String, Map<String, ProjectActivitySnapshot>> data) {
    // Group data by date, then by project
    Map<String, Map<String, ProjectDailyAggregate>> dateProjectMap = new LinkedHashMap<>();

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry : data.entrySet()) {
      String hourKey = hourEntry.getKey();
      String date = extractDateFromHourKey(hourKey);

      for (Map.Entry<String, ProjectActivitySnapshot> projEntry : hourEntry.getValue().entrySet()) {
        String projectName = projEntry.getKey();
        ProjectActivitySnapshot snapshot = projEntry.getValue();

        dateProjectMap
            .computeIfAbsent(date, k -> new LinkedHashMap<>())
            .computeIfAbsent(projectName, k -> new ProjectDailyAggregate())
            .add(snapshot);
      }
    }

    // Sort dates descending (newest first)
    List<String> sortedDates =
        dateProjectMap.keySet().stream()
            .sorted(Comparator.reverseOrder())
            .collect(Collectors.toList());

    for (String date : sortedDates) {
      Map<String, ProjectDailyAggregate> projectsForDate = dateProjectMap.get(date);

      // Calculate daily totals
      long totalSeconds =
          projectsForDate.values().stream().mapToLong(ProjectDailyAggregate::getTotalSeconds).sum();
      List<CommitRecord> allCommits =
          projectsForDate.values().stream()
              .flatMap(agg -> agg.getCommits().stream())
              .distinct()
              .collect(Collectors.toList());

      // Create daily parent node
      String dateDisplay = formatDateDisplay(date);
      ActivityTreeNode dailyNode =
          ActivityTreeNode.createDailyNode(
              dateDisplay, totalSeconds, new ArrayList<>(), allCommits);

      // Sort projects by coding time descending (biggest to smallest)
      projectsForDate.entrySet().stream()
          .sorted(
              (e1, e2) ->
                  Long.compare(e2.getValue().getTotalSeconds(), e1.getValue().getTotalSeconds()))
          .forEach(
              entry -> {
                String projectName = entry.getKey();
                ProjectDailyAggregate aggregate = entry.getValue();

                // Create project row using BranchActivityRow
                BranchActivityRow projectRow =
                    new BranchActivityRow(
                        date,
                        projectName,
                        "-",
                        aggregate.getTotalSeconds(),
                        formatTime(aggregate.getTotalSeconds()),
                        aggregate.getCommits(),
                        formatCommitsDisplay(aggregate.getCommits()));

                ActivityTreeNode projectNode = ActivityTreeNode.createHourlyNode(projectRow);

                // Add commits as children (no project prefix since parent is the project)
                for (CommitRecord commit : aggregate.getCommits()) {
                  ActivityTreeNode commitNode = ActivityTreeNode.createCommitNode(commit, null);
                  projectNode.add(commitNode);
                }

                dailyNode.add(projectNode);
              });

      root.add(dailyNode);
    }
  }

  private void buildTreeForSingleProject(
      DefaultMutableTreeNode root, Map<String, Map<String, ProjectActivitySnapshot>> data) {
    // Group data by date
    Map<String, ProjectDailyAggregate> dateMap = new LinkedHashMap<>();

    for (Map.Entry<String, Map<String, ProjectActivitySnapshot>> hourEntry : data.entrySet()) {
      String hourKey = hourEntry.getKey();
      String date = extractDateFromHourKey(hourKey);

      ProjectActivitySnapshot snapshot = hourEntry.getValue().get(selectedProject);
      if (snapshot != null) {
        dateMap.computeIfAbsent(date, k -> new ProjectDailyAggregate()).add(snapshot);
      }
    }

    // Sort dates descending (newest first)
    List<String> sortedDates =
        dateMap.keySet().stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList());

    for (String date : sortedDates) {
      ProjectDailyAggregate aggregate = dateMap.get(date);

      // Create daily parent node
      String dateDisplay = formatDateDisplay(date);
      ActivityTreeNode dailyNode =
          ActivityTreeNode.createDailyNode(
              dateDisplay, aggregate.getTotalSeconds(), new ArrayList<>(), aggregate.getCommits());

      // Add commits directly as children of daily node
      for (CommitRecord commit : aggregate.getCommits()) {
        ActivityTreeNode commitNode = ActivityTreeNode.createCommitNode(commit, null);
        dailyNode.add(commitNode);
      }

      root.add(dailyNode);
    }
  }

  /** Helper class to aggregate project data for a day. */
  private static class ProjectDailyAggregate {
    private long totalSeconds = 0;
    private final List<CommitRecord> commits = new ArrayList<>();

    void add(ProjectActivitySnapshot snapshot) {
      totalSeconds += snapshot.getCodedTimeSeconds();
      for (CommitRecord commit : snapshot.getCommits()) {
        if (commits.stream().noneMatch(c -> c.getHash().equals(commit.getHash()))) {
          commits.add(commit);
        }
      }
    }

    long getTotalSeconds() {
      return totalSeconds;
    }

    List<CommitRecord> getCommits() {
      return commits;
    }
  }

  private String extractDateFromHourKey(String hourKey) {
    // hourKey format: yyyy-MM-dd-HH
    if (hourKey != null && hourKey.length() >= 10) {
      return hourKey.substring(0, 10);
    }
    return hourKey;
  }

  private String formatDateDisplay(String date) {
    try {
      LocalDate localDate = LocalDate.parse(date);
      String formatted = localDate.format(DATE_DISPLAY_FORMATTER);
      if (localDate.equals(LocalDate.now())) {
        return formatted + " - Today";
      }
      return formatted;
    } catch (Exception e) {
      return date;
    }
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

  private String formatCommitsDisplay(List<CommitRecord> commits) {
    if (commits.isEmpty()) {
      return "-";
    }
    if (commits.size() == 1) {
      CommitRecord c = commits.get(0);
      String msg = c.getMessage();
      if (msg.length() > 40) {
        msg = msg.substring(0, 37) + "...";
      }
      return c.getHash() + ": " + msg;
    }
    // Multiple commits - show count and hashes
    String hashes =
        commits.stream().map(CommitRecord::getHash).limit(3).collect(Collectors.joining(", "));
    if (commits.size() > 3) {
      hashes += "...";
    }
    return commits.size() + " commits: " + hashes;
  }

  private void exportActivityData() {
    LocalActivityDataProvider dataProvider =
        ApplicationManager.getApplication().getService(LocalActivityDataProvider.class);
    if (dataProvider == null) {
      Messages.showErrorDialog(project, "Failed to load activity data", "Export Error");
      return;
    }

    Map<String, Map<String, ProjectActivitySnapshot>> data =
        dataProvider.getAllDataInLocalTimezone();
    if (data.isEmpty()) {
      Messages.showInfoMessage(project, "No activity data to export", "Export");
      return;
    }

    // Get date range from data
    ActivityCsvExporter exporter = new ActivityCsvExporter();
    LocalDate[] dateRange = exporter.getDateRange(data);
    LocalDate defaultFrom = dateRange != null ? dateRange[0] : LocalDate.now().minusDays(7);
    LocalDate defaultTo = dateRange != null ? dateRange[1] : LocalDate.now();

    // Show export dialog
    ExportDialog dialog = new ExportDialog(defaultFrom, defaultTo);
    if (!dialog.showAndGet()) {
      return;
    }

    LocalDate fromDate = dialog.getFromDate();
    LocalDate toDate = dialog.getToDate();

    // Generate CSV content
    String csvContent = exporter.exportToCsv(data, fromDate, toDate);

    // Show file save dialog
    String defaultFileName = "activity-report-" + fromDate + "-to-" + toDate + ".csv";
    FileSaverDescriptor descriptor =
        new FileSaverDescriptor("Export Activity Report", "Save activity report as CSV", "csv");
    FileSaverDialog saveDialog =
        FileChooserFactory.getInstance().createSaveFileDialog(descriptor, project);
    VirtualFileWrapper fileWrapper = saveDialog.save(defaultFileName);

    if (fileWrapper == null) {
      return;
    }

    // Write CSV to file
    try {
      Files.writeString(fileWrapper.getFile().toPath(), csvContent, StandardCharsets.UTF_8);
      Messages.showInfoMessage(
          project,
          "Activity report exported to:\n" + fileWrapper.getFile().getAbsolutePath(),
          "Export Successful");
    } catch (IOException e) {
      LOG.error("Failed to export activity report", e);
      Messages.showErrorDialog(project, "Failed to write file: " + e.getMessage(), "Export Error");
    }
  }

  @Override
  public void dispose() {
    if (autoRefreshTimer != null) {
      autoRefreshTimer.stop();
    }
  }
}
