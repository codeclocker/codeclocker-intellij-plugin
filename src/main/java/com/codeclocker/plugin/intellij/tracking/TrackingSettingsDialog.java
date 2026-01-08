package com.codeclocker.plugin.intellij.tracking;

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
import org.jetbrains.annotations.Nullable;

/** Dialog for configuring tracking behavior settings. */
public class TrackingSettingsDialog extends DialogWrapper {

  private JBCheckBox pauseOnFocusLostCheckbox;
  private JSpinner minutesSpinner;
  private JSpinner secondsSpinner;

  public TrackingSettingsDialog() {
    super(true);
    setTitle("Tracking Settings");
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    boolean pauseOnFocusLost = TrackingPersistence.isPauseOnFocusLostEnabled();
    int totalSeconds = TrackingPersistence.getInactivityTimeoutSeconds();
    int minutes = totalSeconds / 60;
    int seconds = totalSeconds % 60;

    pauseOnFocusLostCheckbox = new JBCheckBox("Pause when IDE loses focus", pauseOnFocusLost);

    minutesSpinner = new JSpinner(new SpinnerNumberModel(minutes, 0, 60, 1));
    secondsSpinner = new JSpinner(new SpinnerNumberModel(seconds, 0, 59, 5));

    JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
    timePanel.add(minutesSpinner);
    timePanel.add(new JBLabel("min"));
    timePanel.add(secondsSpinner);
    timePanel.add(new JBLabel("sec"));

    return FormBuilder.createFormBuilder()
        .addComponent(pauseOnFocusLostCheckbox)
        .addVerticalGap(10)
        .addLabeledComponent("Pause after inactivity:", timePanel)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();
  }

  @Override
  protected @Nullable ValidationInfo doValidate() {
    int minutes = (Integer) minutesSpinner.getValue();
    int seconds = (Integer) secondsSpinner.getValue();
    int totalSeconds = minutes * 60 + seconds;

    if (totalSeconds < 10) {
      return new ValidationInfo("Inactivity timeout must be at least 10 seconds", secondsSpinner);
    }
    if (totalSeconds > 3600) {
      return new ValidationInfo("Inactivity timeout cannot exceed 60 minutes", minutesSpinner);
    }
    return null;
  }

  @Override
  protected void doOKAction() {
    int minutes = (Integer) minutesSpinner.getValue();
    int seconds = (Integer) secondsSpinner.getValue();
    int totalSeconds = minutes * 60 + seconds;

    TrackingPersistence.setPauseOnFocusLostEnabled(pauseOnFocusLostCheckbox.isSelected());
    TrackingPersistence.setInactivityTimeoutSeconds(totalSeconds);
    super.doOKAction();
  }

  /** Show the tracking settings dialog. */
  public static void showDialog() {
    new TrackingSettingsDialog().showAndGet();
  }
}
