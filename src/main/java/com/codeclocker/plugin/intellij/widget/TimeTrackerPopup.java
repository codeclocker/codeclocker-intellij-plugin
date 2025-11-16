package com.codeclocker.plugin.intellij.widget;

import static com.codeclocker.plugin.intellij.HubHost.HUB_UI_HOST;
import static com.codeclocker.plugin.intellij.services.ChangesActivityTracker.GLOBAL_ADDITIONS;
import static com.codeclocker.plugin.intellij.services.ChangesActivityTracker.GLOBAL_REMOVALS;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.codeclocker.plugin.intellij.apikey.ApiKeyPersistence;
import com.codeclocker.plugin.intellij.apikey.EnterApiKeyAction;
import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.ui.popup.ListPopup;
import com.intellij.openapi.ui.popup.ListSeparator;
import com.intellij.openapi.ui.popup.PopupStep;
import com.intellij.openapi.ui.popup.util.BaseListPopupStep;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.Nullable;

public class TimeTrackerPopup {

  private static final String OPEN_DETAILED_VIEW = "Web Dashboard →";
  private static final String ADD_API_KEY = "Add API Key";

  public static ListPopup create(Project project, String totalTime, String projectTime) {
    List<String> items = new ArrayList<>();
    items.add("Total: " + totalTime);
    items.add(project.getName() + ": " + projectTime);
    items.add("Committed Lines: " + getFormattedVcsChanges());
    items.add(OPEN_DETAILED_VIEW);

    boolean hasApiKey = isNotBlank(ApiKeyPersistence.getApiKey());
    if (!hasApiKey) {
      items.add(ADD_API_KEY);
    }

    BaseListPopupStep<String> step =
        new BaseListPopupStep<>("Coding Activity Today", items) {
          @Override
          public boolean isSelectable(String value) {
            return OPEN_DETAILED_VIEW.equals(value) || ADD_API_KEY.equals(value);
          }

          @Override
          public PopupStep<?> onChosen(String selectedValue, boolean finalChoice) {
            if (OPEN_DETAILED_VIEW.equals(selectedValue)) {
              BrowserUtil.browse(HUB_UI_HOST);
            } else if (ADD_API_KEY.equals(selectedValue)) {
              EnterApiKeyAction.showAction();
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

  public static String getFormattedVcsChanges() {
    return String.format("+%d / -%d", GLOBAL_ADDITIONS.get(), GLOBAL_REMOVALS.get());
  }
}
