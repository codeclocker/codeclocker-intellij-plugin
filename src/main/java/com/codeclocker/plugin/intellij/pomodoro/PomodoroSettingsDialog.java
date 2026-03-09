package com.codeclocker.plugin.intellij.pomodoro;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import javax.swing.JComponent;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import org.jetbrains.annotations.Nullable;

public class PomodoroSettingsDialog extends DialogWrapper {

  private JSpinner workMinutesSpinner;
  private JSpinner shortBreakSpinner;
  private JSpinner longBreakSpinner;
  private JSpinner cyclesSpinner;
  private JBCheckBox autoStartBreakCheckbox;
  private JBCheckBox useCodingTimeCheckbox;
  private JBCheckBox notificationsCheckbox;

  public PomodoroSettingsDialog() {
    super(true);
    setTitle("Pomodoro Settings");
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    int workMinutes = PomodoroPersistence.getWorkMinutes();
    int shortBreak = PomodoroPersistence.getShortBreakMinutes();
    int longBreak = PomodoroPersistence.getLongBreakMinutes();
    int cycles = PomodoroPersistence.getCyclesBeforeLongBreak();
    boolean autoStartBreak = PomodoroPersistence.isAutoStartBreak();
    boolean useCodingTime = PomodoroPersistence.isUseCodingTime();
    boolean notifications = PomodoroPersistence.isNotificationsEnabled();

    workMinutesSpinner = new JSpinner(new SpinnerNumberModel(workMinutes, 1, 120, 5));
    shortBreakSpinner = new JSpinner(new SpinnerNumberModel(shortBreak, 1, 30, 1));
    longBreakSpinner = new JSpinner(new SpinnerNumberModel(longBreak, 1, 60, 5));
    cyclesSpinner = new JSpinner(new SpinnerNumberModel(cycles, 1, 10, 1));

    autoStartBreakCheckbox =
        new JBCheckBox("Auto-start break timer when work interval ends", autoStartBreak);
    useCodingTimeCheckbox =
        new JBCheckBox("Use coding time for work intervals (instead of real clock)", useCodingTime);
    notificationsCheckbox = new JBCheckBox("Show break notifications", notifications);

    return FormBuilder.createFormBuilder()
        .addLabeledComponent(new JBLabel("Work interval (minutes):"), workMinutesSpinner)
        .addVerticalGap(5)
        .addLabeledComponent(new JBLabel("Short break (minutes):"), shortBreakSpinner)
        .addVerticalGap(5)
        .addLabeledComponent(new JBLabel("Long break (minutes):"), longBreakSpinner)
        .addVerticalGap(5)
        .addLabeledComponent(new JBLabel("Cycles before long break:"), cyclesSpinner)
        .addVerticalGap(15)
        .addComponent(autoStartBreakCheckbox)
        .addComponent(useCodingTimeCheckbox)
        .addComponent(notificationsCheckbox)
        .addComponentFillVertically(new javax.swing.JPanel(), 0)
        .getPanel();
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    int work = (Integer) workMinutesSpinner.getValue();
    int shortBreak = (Integer) shortBreakSpinner.getValue();

    if (work < 1) {
      return new ValidationInfo("Work interval must be at least 1 minute", workMinutesSpinner);
    }
    if (shortBreak < 1) {
      return new ValidationInfo("Short break must be at least 1 minute", shortBreakSpinner);
    }

    return null;
  }

  @Override
  protected void doOKAction() {
    PomodoroPersistence.setWorkMinutes((Integer) workMinutesSpinner.getValue());
    PomodoroPersistence.setShortBreakMinutes((Integer) shortBreakSpinner.getValue());
    PomodoroPersistence.setLongBreakMinutes((Integer) longBreakSpinner.getValue());
    PomodoroPersistence.setCyclesBeforeLongBreak((Integer) cyclesSpinner.getValue());
    PomodoroPersistence.setAutoStartBreak(autoStartBreakCheckbox.isSelected());
    PomodoroPersistence.setUseCodingTime(useCodingTimeCheckbox.isSelected());
    PomodoroPersistence.setNotificationsEnabled(notificationsCheckbox.isSelected());

    super.doOKAction();
  }

  public static void showDialog() {
    new PomodoroSettingsDialog().showAndGet();
  }
}
