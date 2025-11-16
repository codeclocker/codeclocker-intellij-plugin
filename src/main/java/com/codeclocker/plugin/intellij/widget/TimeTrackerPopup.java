package com.codeclocker.plugin.intellij.widget;

import static com.codeclocker.plugin.intellij.HubHost.HUB_UI_HOST;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class TimeTrackerPopup {

  private static final String OPEN_DETAILED_VIEW = "Web Dashboard →";

  public static ListPopup create(Project project, String totalTime, String projectTime) {
    List<String> items =
        List.of("Total: " + totalTime, project.getName() + ": " + projectTime, OPEN_DETAILED_VIEW);

    BaseListPopupStep<String> step =
        new BaseListPopupStep<>("Coding Time Today", items) {
          @Override
          public boolean isSelectable(String value) {
            return OPEN_DETAILED_VIEW.equals(value);
          }

          @Override
          public PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
            if (OPEN_DETAILED_VIEW.equals(selectedValue)) {
              BrowserUtil.browse(HUB_UI_HOST);
            }
            return FINAL_CHOICE;
          }

          @Override
          public boolean hasSubstep(String selectedValue) {
            return false;
          }

          @Override
          public @Nullable ListSeparator getSeparatorAbove(String value) {
            return OPEN_DETAILED_VIEW.equals(value) ? new ListSeparator() : null;
          }
        };

    return JBPopupFactory.getInstance().createListPopup(step);
  }
}
