# CodeClocker – IntelliJ Plugin for Tracking Developer Activity

![Build](https://github.com/codeclocker/codeclocker-intellij-plugin/actions/workflows/gradle.yml/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/26962)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/26962)

<!-- Plugin description -->

## CodeClocker Time Tracker

Plugin automatically tracks your coding time and activity, displaying it in the IDE status bar. 
Optionally, sync your data to the [CodeClocker Hub](https://hub.codeclocker.com/) for web dashboards and team features.

### Features

#### Local Features (No API Key Required)

* **Coding Time Tracking** – Automatically records time spent on each project.
* **Daily & Weekly Goals** – Set personalized coding time goals and track your progress. Get notified when you reach your targets.
* **VCS Added & Removed Lines** – Tracks added and removed lines in your version control system.
* **IDE Status Bar Widget** – Displays your current coding activity and goal progress directly in the IDE status bar for quick, at-a-glance monitoring.

#### Hub Features (Optional – Requires API Key)

* **Personal Activity Web Dashboard** – Visualizes your coding activity in a web dashboard on [CodeClocker Hub](https://hub.codeclocker.com/).
* **Team Dashboard** – Provides team management and monitoring on [CodeClocker Hub](https://hub.codeclocker.com/) for collective coding activity:
  * **Team Statistics** – Presents aggregated metrics, including total time and daily averages.
  * **Contributors Overview** – Displays individual contributor statistics, including hours coded.
  * **Project Activity Charts** – Renders project-level activity with stacked bar charts showing each contributor’s participation.
  * **Time Period Filtering** – Supports analysis of team performance across different time ranges (e.g., last 7 days).

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "CodeClocker"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26962) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/26962/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/codeclocker/codeclocker-intellij-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## API Key Activation (Optional)

**Note:** The API key is only required if you want to sync your data to [CodeClocker Hub](https://hub.codeclocker.com/) for web dashboards and team features. All local features (time tracking, goals, status bar widget) work without an API key.

To enable Hub integration:

1. After [installation](#installation), a dialog will prompt you to enter your CodeClocker API key.
  Click "Get API Key" to be redirected to the [API Key page on CodeClocker Hub](https://hub.codeclocker.com/api-key).
  Copy your API key from this page.
2. Return to your running IntelliJ IDE, paste the copied API key into the dialog window, and click "OK".

Once set up, CodeClocker will automatically track your coding activity and report it to [CodeClocker Hub](https://hub.codeclocker.com).
If your API key status initially shows "Inactive: No received data yet," don't worry—it may take up to a minute for activity tracking to start.
Once active, it will update to "Active: Receiving data".

If you skipped the initial prompt or need to add/update your API key later, go to **Tools > CodeClocker API Key** at the top of the dropdown list.

![API key menu](docs/media/api-key-menu.jpg)
