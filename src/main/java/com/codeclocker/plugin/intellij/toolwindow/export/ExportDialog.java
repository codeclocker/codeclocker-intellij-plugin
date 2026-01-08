package com.codeclocker.plugin.intellij.toolwindow.export;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.FormBuilder;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import org.jetbrains.annotations.Nullable;

/** Dialog for selecting date range for activity export. */
public class ExportDialog extends DialogWrapper {

  private JSpinner fromDateSpinner;
  private JSpinner toDateSpinner;

  private final LocalDate defaultFromDate;
  private final LocalDate defaultToDate;

  public ExportDialog(LocalDate defaultFromDate, LocalDate defaultToDate) {
    super(true);
    this.defaultFromDate = defaultFromDate != null ? defaultFromDate : LocalDate.now().minusDays(7);
    this.defaultToDate = defaultToDate != null ? defaultToDate : LocalDate.now();
    setTitle("Export Activity Report");
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    // From date spinner
    fromDateSpinner = createDateSpinner(defaultFromDate);

    // To date spinner
    toDateSpinner = createDateSpinner(defaultToDate);

    return FormBuilder.createFormBuilder()
        .addLabeledComponent(new JBLabel("From:"), fromDateSpinner)
        .addVerticalGap(10)
        .addLabeledComponent(new JBLabel("To:"), toDateSpinner)
        .addVerticalGap(10)
        .addComponentFillVertically(new JPanel(), 0)
        .getPanel();
  }

  private JSpinner createDateSpinner(LocalDate initialDate) {
    Date date = Date.from(initialDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    SpinnerDateModel model = new SpinnerDateModel(date, null, null, Calendar.DAY_OF_MONTH);
    JSpinner spinner = new JSpinner(model);
    JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "yyyy-MM-dd");
    spinner.setEditor(editor);
    return spinner;
  }

  public LocalDate getFromDate() {
    Date date = (Date) fromDateSpinner.getValue();
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  public LocalDate getToDate() {
    Date date = (Date) toDateSpinner.getValue();
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  @Override
  protected void doOKAction() {
    // Validate that from <= to
    if (getFromDate().isAfter(getToDate())) {
      setErrorText("'From' date must be before or equal to 'To' date");
      return;
    }
    super.doOKAction();
  }
}
