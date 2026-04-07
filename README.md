# CodeClocker — Auto-Generated Timesheets for Dev Teams | JetBrains Plugin

![Build](https://github.com/codeclocker/codeclocker-intellij-plugin/actions/workflows/gradle.yml/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/26962.svg)](https://plugins.jetbrains.com/plugin/26962)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/26962.svg)](https://plugins.jetbrains.com/plugin/26962)

**Auto-generate timesheets. Approve in minutes. Export for billing.**

CodeClocker automatically tracks active coding time in your JetBrains IDE and turns it into weekly timesheets — no manual timers needed. 
Use it locally for personal tracking, or connect to CodeClocker Hub for team submissions, approvals, and billing exports.
Managers approve with one click, finance exports to CSV or PDF.

<a href="https://hub.codeclocker.com/">
  <img src="https://site.codeclocker.com/screenshots/team-dashboard.jpg" alt="CodeClocker Team Dashboard — timesheet approvals, submission status, and tracked hours" width="100%" />
</a>

<p>
  <a href="https://site.codeclocker.com/">Website</a> &bull;
  <a href="https://hub.codeclocker.com/">CodeClocker Hub</a> &bull;
  <a href="https://plugins.jetbrains.com/plugin/26962">JetBrains Marketplace</a> &bull;
  <a href="https://github.com/codeclocker/codeclocker-intellij-plugin/issues">Report Issue</a>
</p>

---

<!-- Plugin description -->

## Auto-Generated Timesheets & Team Activity Tracking for JetBrains IDEs

CodeClocker automatically tracks coding time and generates weekly timesheets from real IDE activity.  
It gives developers a pre-filled draft they can review and adjust instead of reconstructing the whole week from scratch.  
Use it locally for personal productivity, or connect to [CodeClocker Hub](https://hub.codeclocker.com/) to unlock team timesheets, approval workflows, team awareness updates, and billing exports.

### For Teams & Managers

- **Auto-generated weekly timesheets** — pre-filled from actual IDE activity with per-project and per-branch breakdowns.
- **Evidence-linked worklogs** — connect timesheet entries to branches and commits for easier review and client reporting.
- **One-click approvals** — managers review and approve team timesheets from a single dashboard.
- **CSV & PDF exports** — export approved timesheets for payroll, accounting, or client billing.
- **Automated reminders** — nudge developers to submit and managers to approve on time.
- **Team activity dashboard** — see weekly hours, submission status, and project activity across the team.
- **Team awareness updates** — daily pulse emails and Slack summaries help teammates stay aware of what the team is working on, what moved forward, and what may need coordination.
- **Slack integrations** — post team activity summaries to Slack channels on a schedule.
- **Role-based access** — owners, managers, and members with appropriate permissions.
- **Anonymous mode** — teams can enable anonymous activity tracking so managers see aggregated team stats without individual breakdowns. Non-anonymous mode shows per-member contributions for teams that prefer full transparency. **To prevent spying, once a team is created with anonymous mode enabled, the setting cannot be changed.**

Built for software teams that need weekly timesheets, manager approvals, team awareness, and invoice-ready exports without asking developers to run timers.  
Start a free team trial at [hub.codeclocker.com](https://hub.codeclocker.com/).

### For Developers

- **Automatic time tracking** — records active coding time per project automatically in the background.
- **Daily & weekly goals** — set targets globally or per project, and get notified when you hit them.
- **Pomodoro Timer** — built-in timer with configurable work/break intervals and status bar countdown.
- **What I Was Doing** — generate standup-ready summaries with time breakdowns, branch details, and commits.
- **Activity Report** — tree-view of daily activity with project breakdown, commit history, and CSV export.
- **Teammate awareness** — daily pulse updates help you stay aware of what your teammates are working on and what moved forward across the team.
- **VCS / Git insights** — tracks added and removed lines from version control activity.
- **Auto-pause** — pauses tracking when the IDE loses focus or on inactivity timeout.
- **Privacy first** — all data stays on your machine in Local Mode. Hub sync is opt-in.
- **Team privacy controls** — your team can run in anonymous mode where managers only see team-level totals, not individual activity.

### Supported IDEs

IntelliJ IDEA, PyCharm, Android Studio, WebStorm, PhpStorm, Rider, CLion, GoLand, DataGrip, RubyMine, AppCode, RustRover — and all other JetBrains IDEs.

Learn more at [site.codeclocker.com](https://site.codeclocker.com/).

<!-- Plugin description end -->

---

## Screenshots

### Team Timesheet Approvals

Review submission status, tracked hours, and flags for every team member. Approve timesheets and export the week — all from one screen.

<img src="https://site.codeclocker.com/screenshots/team-dashboard.jpg" alt="CodeClocker auto-generated weekly timesheet with commit evidence" width="100%" />

### Auto-Generated Timesheets

Weekly timesheets are filled automatically from IDE activity. Developers review, adjust if needed, and submit in seconds. Every row links to commits and branches.

<img src="https://site.codeclocker.com/screenshots/timesheet.png" alt="CodeClocker team timesheet approval dashboard" width="100%" />

### Aggregated Activity

See aggregated overall team activity. Filter by time range to spot trends.

<img src="https://site.codeclocker.com/screenshots/team-activity.jpg" alt="CodeClocker real-time team activity tracking dashboard" width="100%" />

### Reports & Exports

Export approved timesheets as CSV for payroll or PDF for clients. Close the week once everything is approved.

<img src="https://site.codeclocker.com/screenshots/export-everywhere.png" alt="CodeClocker reports and exports — CSV, PDF, close week" width="100%" />

### In-IDE Activity Dashboard

The status bar widget shows today's tracked time, goal progress, coding trends, and per-project breakdown — without leaving your IDE.

<img src="https://site.codeclocker.com/screenshots/status-bar-popup.png" alt="CodeClocker IDE status bar widget with goals and coding trends" width="400" />

### Activity Report

Detailed tree-view of daily coding activity organized by project. Expand any day to see commits with hashes and messages. Filter by project and export to CSV.

<img src="https://site.codeclocker.com/screenshots/activity-report.png" alt="CodeClocker activity report tool window with commit details" width="100%" />

---

## Installation

- **From your IDE:**

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "CodeClocker"</kbd> > <kbd>Install</kbd>

- **From JetBrains Marketplace:**

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26962) and click <kbd>Install to ...</kbd>.

  Or download the [latest release](https://plugins.jetbrains.com/plugin/26962/versions) and install manually via <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---

## Getting Started with Teams

1. **Install the plugin** — from JetBrains Marketplace or directly in your IDE.
2. **Sign up on [CodeClocker Hub](https://hub.codeclocker.com/)** — create your account and get an API key.
3. **Connect the plugin** — paste the API key via <kbd>Tools</kbd> > <kbd>CodeClocker API Key</kbd>.
4. **Create a team** — invite developers via a shareable link.
5. **Approve & export** — review auto-generated timesheets each week, export to CSV or PDF.

Developers only need the plugin installed and connected. CodeClocker builds draft timesheets automatically from IDE activity.

<a href="https://hub.codeclocker.com/"><strong>Start your free team trial →</strong></a>

---

## Links

- [CodeClocker Website](https://site.codeclocker.com/) — features, pricing, and how it works
- [CodeClocker Hub](https://hub.codeclocker.com/) — web dashboard for timesheets, approvals, and exports
- [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/26962) — plugin page with reviews and ratings
- [GitHub Issues](https://github.com/codeclocker/codeclocker-intellij-plugin/issues) — report bugs or request features
- [Changelog](CHANGELOG.md) — release history and what's new
