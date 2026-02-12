package com.codeclocker.plugin.intellij.goal;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import java.awt.FlowLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Dialog for configuring per-project daily and weekly coding time goals. */
public class ProjectGoalSettingsDialog extends DialogWrapper {

  private final String projectName;
  private JBCheckBox customGoalsCheckbox;
  private JSpinner dailyHoursSpinner;
  private JSpinner dailyMinutesSpinner;
  private JSpinner weeklyHoursSpinner;
  private JSpinner weeklyMinutesSpinner;
  private JBCheckBox notificationsCheckbox;

  public ProjectGoalSettingsDialog(@NotNull Project project) {
    super(project, true);
    this.projectName = project.getName();
    setTitle("Coding Time Goals: " + projectName);
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    boolean hasCustomGoals = ProjectGoalPersistence.hasCustomGoals(projectName);
    int dailyMinutes =
        hasCustomGoals
            ? ProjectGoalPersistence.getProjectDailyGoalMinutes(projectName)
            : GoalPersistence.getDailyGoalMinutes();
    int weeklyMinutes =
        hasCustomGoals
            ? ProjectGoalPersistence.getProjectWeeklyGoalMinutes(projectName)
            : GoalPersistence.getWeeklyGoalMinutes();
    boolean notificationsEnabled =
        ProjectGoalPersistence.isProjectNotificationsEnabled(projectName);

    // Custom goals checkbox
    customGoalsCheckbox = new JBCheckBox("Use custom goals for this project", hasCustomGoals);
    customGoalsCheckbox.addChangeListener(e -> updateSpinnersEnabled());

    // Daily goal spinners
    dailyHoursSpinner = new JSpinner(new SpinnerNumberModel(dailyMinutes / 60, 0, 24, 1));
    dailyMinutesSpinner = new JSpinner(new SpinnerNumberModel(dailyMinutes % 60, 0, 59, 5));

    JPanel dailyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    dailyPanel.add(dailyHoursSpinner);
    dailyPanel.add(new JBLabel("hours"));
    dailyPanel.add(dailyMinutesSpinner);
    dailyPanel.add(new JBLabel("minutes"));

    // Weekly goal spinners
    weeklyHoursSpinner = new JSpinner(new SpinnerNumberModel(weeklyMinutes / 60, 0, 168, 1));
    weeklyMinutesSpinner = new JSpinner(new SpinnerNumberModel(weeklyMinutes % 60, 0, 59, 5));

    JPanel weeklyPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
    weeklyPanel.add(weeklyHoursSpinner);
    weeklyPanel.add(new JBLabel("hours"));
    weeklyPanel.add(weeklyMinutesSpinner);
    weeklyPanel.add(new JBLabel("minutes"));

    // Notifications checkbox
    notificationsCheckbox =
        new JBCheckBox("Show notification when goal is reached", notificationsEnabled);

    // Note label
    JBLabel noteLabel = new JBLabel("Uncheck to use global goals for the selected project");
    noteLabel.setEnabled(false);

    // Set initial state
    updateSpinnersEnabled();

    return FormBuilder.createFormBuilder()
        .addComponent(customGoalsCheckbox)
        .addVerticalGap(15)
        .addLabeledComponent(new JBLabel("Daily goal:"), dailyPanel)
        .addVerticalGap(10)
        .addLabeledComponent(new JBLabel("Weekly goal:"), weeklyPanel)
        .addVerticalGap(15)
        .addComponent(notificationsCheckbox)
        .addVerticalGap(10)
        .addComponent(noteLabel)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();
  }

  private void updateSpinnersEnabled() {
    boolean enabled = customGoalsCheckbox.isSelected();
    dailyHoursSpinner.setEnabled(enabled);
    dailyMinutesSpinner.setEnabled(enabled);
    weeklyHoursSpinner.setEnabled(enabled);
    weeklyMinutesSpinner.setEnabled(enabled);
    notificationsCheckbox.setEnabled(enabled);

    // When unchecked, show global values
    if (!enabled) {
      int globalDaily = GoalPersistence.getDailyGoalMinutes();
      int globalWeekly = GoalPersistence.getWeeklyGoalMinutes();
      dailyHoursSpinner.setValue(globalDaily / 60);
      dailyMinutesSpinner.setValue(globalDaily % 60);
      weeklyHoursSpinner.setValue(globalWeekly / 60);
      weeklyMinutesSpinner.setValue(globalWeekly % 60);
    }
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    if (!customGoalsCheckbox.isSelected()) {
      return null;
    }

    int dailyHours = (Integer) dailyHoursSpinner.getValue();
    int dailyMins = (Integer) dailyMinutesSpinner.getValue();
    int dailyTotal = dailyHours * 60 + dailyMins;

    int weeklyHours = (Integer) weeklyHoursSpinner.getValue();
    int weeklyMins = (Integer) weeklyMinutesSpinner.getValue();
    int weeklyTotal = weeklyHours * 60 + weeklyMins;

    if (dailyTotal > 1440) {
      return new ValidationInfo("Daily goal cannot exceed 24 hours", dailyHoursSpinner);
    }

    if (weeklyTotal > 10080) {
      return new ValidationInfo("Weekly goal cannot exceed 168 hours", weeklyHoursSpinner);
    }

    return null;
  }

  @Override
  protected void doOKAction() {
    boolean useCustomGoals = customGoalsCheckbox.isSelected();
    ProjectGoalPersistence.setCustomGoalsEnabled(projectName, useCustomGoals);

    if (useCustomGoals) {
      int dailyHours = (Integer) dailyHoursSpinner.getValue();
      int dailyMins = (Integer) dailyMinutesSpinner.getValue();
      int dailyTotal = dailyHours * 60 + dailyMins;

      int weeklyHours = (Integer) weeklyHoursSpinner.getValue();
      int weeklyMins = (Integer) weeklyMinutesSpinner.getValue();
      int weeklyTotal = weeklyHours * 60 + weeklyMins;

      ProjectGoalPersistence.setProjectDailyGoalMinutes(projectName, dailyTotal);
      ProjectGoalPersistence.setProjectWeeklyGoalMinutes(projectName, weeklyTotal);
      ProjectGoalPersistence.setProjectNotificationsEnabled(
          projectName, notificationsCheckbox.isSelected());
    }

    super.doOKAction();
  }

  /** Show the project goal settings dialog for the given project. */
  public static void showDialog(@NotNull Project project) {
    new ProjectGoalSettingsDialog(project).showAndGet();
  }
}
