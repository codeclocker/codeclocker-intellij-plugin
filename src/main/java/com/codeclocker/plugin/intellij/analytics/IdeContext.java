package com.codeclocker.plugin.intellij.analytics;

import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.extensions.PluginId;
import java.util.Locale;

/** Contains information about the IDE environment. */
public record IdeContext(
    String ideName,
    String ideVersion,
    String ideBuild,
    String ideProductCode,
    String pluginVersion,
    String osName,
    String osVersion,
    String osArch,
    String javaVersion,
    String locale) {

  private static final String PLUGIN_ID = "com.codeclocker";

  /** Creates an IdeContext with current IDE and system information. */
  public static IdeContext current() {
    ApplicationInfo appInfo = ApplicationInfo.getInstance();

    return new IdeContext(
        appInfo.getFullApplicationName(),
        appInfo.getFullVersion(),
        appInfo.getBuild().asString(),
        appInfo.getBuild().getProductCode(),
        getPluginVersion(),
        System.getProperty("os.name"),
        System.getProperty("os.version"),
        System.getProperty("os.arch"),
        System.getProperty("java.version"),
        Locale.getDefault().toString());
  }

  private static String getPluginVersion() {
    IdeaPluginDescriptor plugin = PluginManagerCore.getPlugin(PluginId.getId(PLUGIN_ID));
    if (plugin != null) {
      return plugin.getVersion();
    }
    return "unknown";
  }
}
