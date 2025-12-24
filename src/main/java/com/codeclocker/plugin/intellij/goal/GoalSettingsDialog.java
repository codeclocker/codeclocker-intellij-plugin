package com.codeclocker.plugin.intellij.goal;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import java.awt.FlowLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.jetbrains.annotations.Nullable;

/** Dialog for configuring daily and weekly coding time goals. */
public class GoalSettingsDialog extends DialogWrapper {

  private JSpinner dailyHoursSpinner;
  private JSpinner dailyMinutesSpinner;
  private JSpinner weeklyHoursSpinner;
  private JSpinner weeklyMinutesSpinner;
  private JBCheckBox enabledCheckbox;
  private JBCheckBox notificationsCheckbox;

  public GoalSettingsDialog() {
    super(true);
    setTitle("Coding Time Goals");
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    int dailyMinutes = GoalPersistence.getDailyGoalMinutes();
    int weeklyMinutes = GoalPersistence.getWeeklyGoalMinutes();
    boolean enabled = GoalPersistence.isGoalsEnabled();
    boolean notificationsEnabled = GoalPersistence.isNotificationsEnabled();

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

    // Enable checkbox
    enabledCheckbox = new JBCheckBox("Show goal progress in widget", enabled);

    // Notifications checkbox
    notificationsCheckbox =
        new JBCheckBox("Show notification when goal is reached", notificationsEnabled);

    return FormBuilder.createFormBuilder()
        .addLabeledComponent(new JBLabel("Daily goal:"), dailyPanel)
        .addVerticalGap(10)
        .addLabeledComponent(new JBLabel("Weekly goal:"), weeklyPanel)
        .addVerticalGap(15)
        .addComponent(enabledCheckbox)
        .addComponent(notificationsCheckbox)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();
  }

  @Override
  protected void doOKAction() {
    int dailyHours = (Integer) dailyHoursSpinner.getValue();
    int dailyMins = (Integer) dailyMinutesSpinner.getValue();
    int dailyTotal = dailyHours * 60 + dailyMins;

    int weeklyHours = (Integer) weeklyHoursSpinner.getValue();
    int weeklyMins = (Integer) weeklyMinutesSpinner.getValue();
    int weeklyTotal = weeklyHours * 60 + weeklyMins;

    GoalPersistence.setDailyGoalMinutes(dailyTotal);
    GoalPersistence.setWeeklyGoalMinutes(weeklyTotal);
    GoalPersistence.setGoalsEnabled(enabledCheckbox.isSelected());
    GoalPersistence.setNotificationsEnabled(notificationsCheckbox.isSelected());

    super.doOKAction();
  }

  /** Show the goal settings dialog. */
  public static void showDialog() {
    new GoalSettingsDialog().showAndGet();
  }
}
