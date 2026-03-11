package com.codeclocker.plugin.intellij.standup;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class StandupDigestDialog extends DialogWrapper {

  private JTextArea textArea;
  private StandupPeriod currentPeriod = StandupPeriod.TODAY_AND_YESTERDAY;

  public StandupDigestDialog() {
    super(true);
    setTitle("What I Was Doing...");
    setOKButtonText("Close");
    init();
    refreshDigest();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    JPanel panel = new JPanel(new BorderLayout(0, JBUI.scale(8)));
    panel.setPreferredSize(new Dimension(JBUI.scale(650), JBUI.scale(550)));

    // Period selector
    ComboBox<StandupPeriod> periodCombo = new ComboBox<>(StandupPeriod.values());
    periodCombo.setSelectedItem(currentPeriod);
    periodCombo.setRenderer(SimpleListCellRenderer.create("", StandupPeriod::getLabel));
    periodCombo.addActionListener(
        e -> {
          StandupPeriod selected = (StandupPeriod) periodCombo.getSelectedItem();
          if (selected != null && selected != currentPeriod) {
            currentPeriod = selected;
            refreshDigest();
          }
        });

    JPanel selectorPanel = new JPanel();
    selectorPanel.add(periodCombo);

    // Text area
    textArea = new JTextArea();
    textArea.setEditable(false);
    textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, JBUI.scale(12)));
    textArea.setLineWrap(true);
    textArea.setWrapStyleWord(true);
    textArea.setMargin(JBUI.insets(8));
    textArea.setText("Loading...");

    JBScrollPane scrollPane = new JBScrollPane(textArea);

    panel.add(selectorPanel, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);

    return panel;
  }

  @Override
  protected Action @NotNull [] createActions() {
    Action copyAction = new CopyToClipboardAction();
    return new Action[] {copyAction, getOKAction()};
  }

  private void refreshDigest() {
    StandupDigestService service =
        ApplicationManager.getApplication().getService(StandupDigestService.class);
    StandupDigest digest = service.compute(currentPeriod);
    String formatted = StandupDigestFormatter.format(digest);
    textArea.setText(formatted);
    textArea.setCaretPosition(0);
  }

  public static void showDialog() {
    new StandupDigestDialog().show();
  }

  private class CopyToClipboardAction extends AbstractAction {
    CopyToClipboardAction() {
      super("Copy to Clipboard");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      String text = textArea.getText();
      if (text != null && !text.isEmpty()) {
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(text), null);
        putValue(NAME, "Copied!");
        Timer timer = new Timer(2000, ev -> putValue(NAME, "Copy to Clipboard"));
        timer.setRepeats(false);
        timer.start();
      }
    }
  }
}
