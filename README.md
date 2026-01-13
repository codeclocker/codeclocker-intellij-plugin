# CodeClocker – Time & Activity Tracking for JetBrains IDEs

![Build](https://github.com/codeclocker/codeclocker-intellij-plugin/actions/workflows/gradle.yml/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/26962.svg)](https://plugins.jetbrains.com/plugin/26962)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/26962.svg)](https://plugins.jetbrains.com/plugin/26962)

<!-- Plugin description -->

## Automatic Time & Activity Tracker

Plugin automatically tracks your **active coding time per project**, shows your progress in the **IDE status bar**, and helps you build consistent habits with **daily/weekly goals**.  
Use it **locally (offline)** by default, or optionally sync to **[CodeClocker Hub](https://hub.codeclocker.com/)** for **web dashboards** and **team analytics**.

### Local Mode (Default - No account, no API key)

- **Automatic coding time tracking** - records active time while you work, organized by project.
- **Daily & weekly goals** - set targets, monitor progress, and get notified when you reach them.
- **Activity Report** - detailed tree-view of daily activity with project breakdown and commit details.
- **CSV export** - export activity data for invoicing with date range selection.
- **Status bar widget** - see today's tracked time, and goal progress at a glance.
- **Auto-pause settings** - configure when tracking pauses (IDE focus lost, inactivity timeout).
- **Privacy** - all tracking data stays on your machine in Local Mode.
- **VCS / Git insights** - tracks **added & removed lines** from version control activity.

### Hub Mode (Optional - Web dashboards & team reporting)

Enable Hub Mode with an API key to sync activity to **[CodeClocker Hub](https://hub.codeclocker.com/)** and unlock:

- **Personal activity dashboard** - visualize trends, totals, and project breakdowns in the browser.
- **Team dashboard** - monitor team activity and productivity insights:
  - **Team statistics** (totals, daily averages)
  - **Contributor overview** (individual hours and activity)
  - **Project activity charts** (who contributed and when)
  - **Time range filtering** (e.g., last 7 days, custom periods)
  - **Data storage** - Activity is synced to CodeClocker Hub only when you enable Hub Mode and provide an API key.

### Activity Report

Click the status bar widget and select **Activity Report...** to open a detailed view of your coding activity:

- **Tree-table view** - Daily activity organized by project with expandable rows
- **Commit history** - See commits with hash and message for each project
- **Project filter** - View all projects or filter to a specific one
- **CSV export** - Export data for invoicing with customizable date range

The Activity Report auto-refreshes every 10 seconds to show live data.

### Auto-Pause Settings

Click the status bar widget and select **Auto-Pause...** to configure tracking behavior:

- **Pause when IDE loses focus** - Automatically pause tracking when you switch to another application
- **Inactivity timeout** - Set how long to wait before pausing when there's no activity (10 seconds to 60 minutes)

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
